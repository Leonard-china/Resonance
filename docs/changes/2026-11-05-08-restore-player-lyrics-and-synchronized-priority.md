# 2026-11-05 — 恢复封面歌词预览、同步歌词优先和当前句高亮

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.6 / 40，版本号不变
- 影响平台：Android、Windows、Linux 的共享播放器与 JVM 查词链路
- 关联需求：用户截图反馈封面没有歌词，歌词页被识别成纯文本，没有同步滚动或当前句高亮。
- 关联记录：[AM++ 参考界面](2026-11-05-02-am-reference-native-ui.md)、[歌词查找](2026-11-04-09-lyrics-source-recovery-and-compact-tools.md)、[此前高亮修复](2026-11-03-10-restore-lyric-focus.md)。

## 需求或问题

手机封面页只显示封面；进入歌词页时，《双截棍》／周杰伦／范特西／3:21 显示“纯文本歌词 · 可手动滚动”，无法随实际播放进度高亮当前句。需要恢复封面歌词、实际时间轴检索和清晰的当前句提示，保留现有原生播放器结构。

## 原因与依据

1. `PaneContent` 在 `showArtwork=true` 时不再组合 `HeroSyncedLyricsPreview`，所有窄屏封面页因此失去歌词。
2. `LrclibLyricsRepository.fetchOriginal` 立即接受本地或缓存的任何 `Found`；LRCLIB 返回纯文本时也不调用 `sourceFallback`。纯文本成功缓存有效期为 30 天，缺少时间轴仍被视为查询已完成。
3. LRCLIB 搜索按元数据总分只选一条，纯文本或无效时间戳可能压过已经匹配歌名、歌手和合理时长的同步版本。`MusicSourceRegistry.findLyrics` 在每个源和多个源之间也接受第一份非空文本，没有优先选择可用时间轴。
4. 沉浸播放器的 `LyricRow` 跳过 `lyricFocus`；手动浏览时 `contextDistance=0` 又让所有行变成同样白色。此前高亮测试只渲染独立 `LyricsContent` 的非沉浸状态，没有覆盖真正的 `NowPlayingOverlay`。

截图能证明手机收到纯文本，无法单凭截图确定那份文本最初来自本地、LRCLIB、某歌源还是 AI 缓存；本次针对以上能从代码复现的分支修复，不将推测的来源写成设备取证结论。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `PaneContent`、`HeroSyncedLyricsPreview` | 恢复封面下方预览。封面使用紧凑预览字阶，避免采用独立舞台预览的 264dp 高度挤占封面和控制区；宽屏原有左右分栏保留。 |
| 同上 | `LyricsContent`、`LyricRow` | 沉浸模式也绘制当前句淡底和细边；同步歌词的普通行继续减弱，手动浏览清除模糊时仍保留当前行颜色差异。字号、字重、行高不随当前句切换。 |
| [LrclibLyricsRepository.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/lyrics/LrclibLyricsRepository.kt) | `fetchOriginal`、`preferSynchronizedSource`、`fetchRemote` | 本地/缓存纯文本作为兜底，继续查找同步版本；有效同步缓存、本地同步和已生成同步歌词继续直接复用。LRCLIB 搜索先校验匹配与时间轴，再优先同步结果。源未命中、异常或只返回另一份纯文本时保留已有文本；取消继续向上传递。单曲偏移仍由最后的 `LyricOffsetStore.apply` 合并。 |
| [MusicSourceRegistry.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sources/MusicSourceRegistry.kt) | `findLyrics` | 同一来源最多两条匹配候选、最多八个启用来源内优先真实同步歌词；已有纯文本在后续候选失败或超时后仍可返回。沿用严格身份、版本、时长和时间轴校验。 |
| [LyricsAcquisitionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/lyrics/LyricsAcquisitionTest.kt) | 查词与显式公网回归 | 覆盖新/旧纯文本缓存、本地文本、LRCLIB 候选、歌源优先级、异常、取消、偏移及文本不被丢弃；新增所报歌曲纯文本缓存恢复的 opt-in 公网验证。 |
| [LyricsPlayerRegressionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/LyricsPlayerRegressionTest.kt) | 实际播放器原生离屏回归 | 在普通手机、小屏大字、短横屏和桌面窗口实际渲染 `NowPlayingOverlay`，检查预览/控制区可见、普通高度封面可见、换句选中状态、沉浸高亮像素及行高稳定；极短横屏沿用优先控制、允许封面收缩的布局。 |
| [LyricsPlayerRegressionDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsPlayerRegressionDeviceTest.kt) | Android 原生播放器回归 | 使用合成歌词和独立验证应用，检查真实手机的封面预览、换句、沉浸淡底及大字号控制可达。 |
| [LyricsScrollDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsScrollDeviceTest.kt) | `rapidlyReversingTheTargetDoesNotReplayOrSnap` | 手动测试时钟先确认新目标已参与组合再采样首帧，避免将旧动画最后一帧误报为跳变；仍检查不瞬移到终点、原 8dp 首帧阈值及最终居中。 |

## 修改前后行为

| Before | After | Why |
| --- | --- | --- |
| 窄屏封面页无歌词 | 封面下方显示随真实进度更新的紧凑歌词预览，点击可进入完整歌词 | 恢复可见的歌词入口和预览 |
| 旧纯文本缓存直接结束查找 | 自动继续尝试同步版本，成功后更新原格式缓存 | 无需清应用数据或删除原歌词 |
| 前面的纯文本覆盖后面有效同步候选 | 匹配身份和时长后，优先选择经校验的同步时间轴 | 恢复滚动与高亮的实际时间依据 |
| 沉浸当前句没有淡底；浏览时全白 | 当前句白字、淡底和细边，其他同步行减弱；浏览清晰但焦点保留 | 当前句始终可辨识，换句不重排 |
| 后续歌源查询失败可能丢弃文本 | 保留已经取得的纯文本，不伪造时间戳 | 离线和服务不完整时仍能阅读 |

## 平台与数据影响

共享 UI 和 JVM 检索影响 Android、Windows、Linux；两端 `loadLyrics` 仍接入同一仓库和歌源回退，未改变平台接口。没有修改播放后端、转换、队列防竞争、偏好写入顺序、曲库/歌单/同步包格式、权限或 API Key。

原歌词文件保持不变。缓存沿用现有 JSON，查到同步结果时替换该歌曲缓存，已有单曲偏移沿用独立存储。纯文本缓存不再阻断免费歌词查询，因此在仍只有纯文本的歌曲再次加载时可能额外联网；同步缓存继续离线命中。请求仍只包含歌曲公开元数据与歌词，不上传/下载音频，不自动调用模型或收费识别。真正没有时间轴时继续标识纯文本，不按时长猜测时间戳。

## 验证

本机重量级任务串行，systemd 用户服务对整个进程树设置 MemoryMax=2G、MemoryHigh=1536M、MemorySwapMax=0；日志位于忽略目录 `build/lyrics-regression-2026-11-05/`。不使用历史通过结果替代。

| 实际命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `python3 build/lyrics-regression-2026-11-05/compile_desktop.py` | Linux；Kotlin/Compose compiler 2.3.20，完整 common/JVM/desktop 源码 | 通过：198 个主源码文件全部重编译，未以旧应用 class 局部替换。 |
| `python3 build/lyrics-regression-2026-11-05/test_targeted.py` | 10 个定向测试源码，原生 Compose 离屏与本地 HTTP/QuickJS 夹具 | 44 项：42 通过、2 个 opt-in 公网入口按默认条件跳过、0 失败。包含四种窗口大小的真实播放器高亮像素与换句检查，以及查词、缓存、取消、偏移、翻译和滚动回归。 |
| `RESONANCE_LYRICS_ACCEPTANCE=1` 的 `run_public.py` | 隔离临时资料目录，真实 LRCLIB/网易/QQ 公开服务，只查元数据和歌词 | 13 项全部通过、0 跳过。《双截棍》/周杰伦/范特西/3:21 的旧纯文本缓存恢复为 QQ 歌源的 **91 行同步歌词**；42 秒位置可定位到真实时间戳。《夏天的风》原公网回归同样通过，46 行同步歌词。日志只记录来源、行数和同步标志，不输出原词。 |

原生 UI 测试最初三轮分别遇到测试夹具的合并语义树、被视口裁剪后的行高和极短横屏封面高度断言失败。改为读取未合并子节点、实际测量尺寸及现有紧凑布局约束后，最终 44 项零失败；原始日志保留为 `targeted-tests-first.log`、`targeted-tests-clipped-bounds.log`、`targeted-tests-compact-artwork.log`。没有为使测试通过去除高亮像素或播放控制可达断言。

本次人工核对普通手机、小屏大字、短横屏和桌面的原生渲染截图，通过：封面预览可见、当前行淡底清晰、邻行减弱；短横屏继续优先显示歌词与控制。截图为合成歌词，位于 `composeApp/build/lyrics-regression/`，不包含用户音频或原词。Android 编译、打包和设备检查使用同一份完整主源码输出，结果如下。

| 实际命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `python3 .../compile_android.py` | Android 36 classpath，完整 common/JVM/Android/Debug 主源码 | 通过：190 个主源码文件、30 个设备测试源码；后续只修改设备测试采样/语义读取时单独重编译完整设备测试集。 |
| `python3 .../package_current.py android` | 同一内存限制，Gradle 单 worker/短生命周期、进程内 Kotlin 策略 | `assembleDebug` / `assembleDebugAndroidTest` 通过；通过临时 init 的任务动作接入完整 CLI 编译输出，资源/Manifest/D8 正常执行。不是默认 Gradle Kotlin 编译或正式 Release 构建通过。 |
| `verify_artifacts.py` | 本次 APK，aapt / apksigner | 通过：主包版本 0.6.6 / 40、独立验证应用 ID、两份 APK 签名有效。首轮脚本在 systemd 环境找不到 PATH 中的 Java，改为显式调用已配置 JDK 的 apksigner.jar 后通过，没有读取或输出签名私钥。 |

设备首轮 14 项中 3 项断言失败：封面标签位于合并语义子树导致 2 项可见性误判，改用未合并树后对应 2 项通过；旧反向滚动用例在新目标尚未参与组合前取样，记录到旧动画最后一帧移动 50px。测试改为先确认 selected 目标再比较首帧，仍保留原 8dp 阈值并新增“未跳到最终中心”断言，没有改变生产滚动曲线、时长或算法。首轮和定点诊断日志分别保存在 `device-tests-first.log` / `device-tests-diagnosis.log`。最终重新编译/打包后，完整设备 14 项全部通过，0 跳过，21.659 秒。

| 最终设备命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `adb ... am instrument -w -r -e class com.resonance.player.LyricsPlayerRegressionDeviceTest,com.resonance.player.LyricsScrollDeviceTest,com.resonance.player.LyricsPreviewMotionDeviceTest com.resonance.player.debug.validation20261105lyrics.test/androidx.test.runner.AndroidJUnitRunner` | 已连接 Honor Android 16 / API 36，1264×2728 / 560dpi，独立验证应用 | **14 项全部通过**，0 跳过；包括封面预览、当前行淡底像素、原生位置换句、1.5 倍字号、手动浏览/返回、偏移 seek、减少动态、快速反向与预览连续运动。 |
| 验证包清理 | 同一手机，仅本轮验证 ID | 主 APK 与 androidTest APK 均已卸载；未覆盖日常正式应用、未申请音频权限、未读取或改动用户曲库。 |
| `git -c core.whitespace=cr-at-eol diff --check` 及新增文档链接核对 | 保留已有未提交改动的工作区 | 通过。 |

本次本地产物均为隔离 Debug 验证包，未公开上传。校验值来自本次实际文件：

| 文件 | SHA-256 |
| --- | --- |
| `build/lyrics-regression-2026-11-05/artifacts/Resonance-0.6.6-lyrics-validation.apk` | `b83c6c2bfb5052d505e38191e482e1c00124dd06250d0c4af026829bf82a9f7f` |
| `build/lyrics-regression-2026-11-05/artifacts/Resonance-0.6.6-lyrics-androidTest.apk` | `72d0cc8897444388cd69fc09aea484e8a4e41500b0ac659dccf1625c5cf2e8ca` |

原生手机的合成歌词封面截图保存在 `build/lyrics-regression-2026-11-05/device-screenshots/`；完整高亮通过手机像素断言验证，不将离屏截图冒称手机截图。编译/打包服务峰值约 1.5 GiB，所有相关服务均未超过 2 GiB 进程树限制。

## 限制与后续

已完成共享逻辑、原生离屏、公开歌曲检索及 Android 独立测试包验收；未构建正式签名 Release、未覆盖安装日常手机/桌面程序、未公开发布，Windows 实机未执行。手机使用合成歌词验证播放器，所报歌曲的公开服务/缓存恢复在本次 JVM 网络环境验证，没有声称在用户正式应用里重新播放真实歌曲通过。公开歌词服务仍可能缺少特定版本或受网络影响，真实缺失时间轴时不能承诺同步高亮。

## 同步更新的文档

- [文档索引](../README.md)
- [开发说明第 8、11 节](../DEVELOPMENT.md#lyrics)：同步检索优先级、纯文本缓存恢复及沉浸高亮

后续安装：用户随后明确要求替换手机与电脑，正式包构建及实际安装状态另见 [2026-11-05 本机安装记录](2026-11-05-09-install-lyrics-fix-on-phone-and-linux.md)。本记录的独立测试包状态保留为修复阶段的历史事实。
