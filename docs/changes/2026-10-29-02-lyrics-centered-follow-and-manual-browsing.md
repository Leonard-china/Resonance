# 2026-10-29 — 修正歌词居中坐标、平滑跟随与手动浏览后的立即返回

- 修改日期：2026-10-29（Asia/Shanghai）
- 类型：Bug 修复 / 交互改进
- 适用版本：`0.6.5 / 39`，不升版
- 影响平台：共享 UI（Android、Windows）
- 关联需求：用户提供手机歌词页截图，要求改进滚动体验；[上一轮歌词入口修改](2026-10-29-01-queue-play-next-library-sorting-and-lyrics-polish.md)

## 需求或问题

当前高亮歌词偏向视口底部；手动翻阅后按「回到当前播放」不立即滚动。生成、翻译、显示和校准工具长期占据歌词卡片上部，小屏可读歌词减少。长句和双语行高不同，高亮时字号变化也会改变排版。

## 原因与依据

- 旧 `LyricsContent` 以 `viewportSize.height / 2` 对比 `visibleItemsInfo.offset`，没有计入上半屏内容留白带来的负 `viewportStartOffset`，两者不在同一个坐标原点。依据见 [Compose LazyListLayoutInfo 官方文档](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListLayoutInfo)。
- 自动滚动的 `LaunchedEffect` 只监听 `activeIndex`。清空 `lastUserDragMark` 不会触发该 effect；播放暂停或长时间没有下一句时也不会自动恢复。
- 旧浏览暂停从拖动开始计时，不从拖动和惯性滚动结束计时。列表外的播放器更新会间接影响基于 `TimeMark` 的按钮显示。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [LyricScroll.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LyricScroll.kt) | `lyricCenterOffset`、`LazyListState.centerLyric` | 使用真实 viewport 起止坐标；离屏行先进入测量范围，再按实际行高修正；可见行使用既有曲线的 280ms 滚动；减少动效直接定位；超过视口高度的长行优先显示开头 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `LyricsContent`、`LyricRow` | 区分自动跟随与手动浏览；嵌套滚动只捕捉用户输入，不把程序滚动当拖动；拖动和惯性结束后等待 5 秒恢复，按钮/点句立即请求定位；窗口、字号、译文模式变化时重新定位；换歌隔离列表和状态 |
| 同上 | 歌词工具、行样式 | 默认用 48dp 单行工具栏，点「歌词工具」展开现有翻译/生成/校准；行字号和字重固定，仅过渡颜色，避免高亮引起换行；同步首尾均有半视口留白，纯文本从顶部正常排版 |
| [LyricScrollTest.kt](../../composeApp/src/commonTest/kotlin/com/resonance/player/LyricScrollTest.kt) | 居中数学回归 | 覆盖负留白、多种行高与超高行 |
| [LyricsScrollDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsScrollDeviceTest.kt) | 设备 UI 回归 | 使用原创合成歌词验证实际 Compose 坐标、拖动、立即返回、计时恢复、纯文本、偏移、大字号、窗口变化，以及换歌后歌词和当前行号相同的重新定位 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 同步歌词切到下一行 | 内容留白没有参与中心计算，可能偏到下半屏 | 当前行按实测高度居中；平滑过渡可被手势打断 |
| 手动浏览后点返回 | 只清标记，通常等下一句变化才滚动 | 同一句/暂停时也立即回到当前行 |
| 浏览后等待 | 仅在下一句到来时检查是否已过 3 秒 | 结束拖动和惯性后等待 5 秒，无新时间戳也恢复 |
| 长句与双语 | 高亮字号变化可能导致重排 | 高亮与普通行保持相同排版尺度；译文和长句按实际高度定位 |
| 小屏打开歌词 | 工具按钮和校准入口占据多行 | 默认仅一行来源和工具按钮，展开后保留原操作 |

## 平台与数据影响

Android 与 Windows 共用 UI 和滚动逻辑。只改界面与瞬时跟随状态，不改歌词存储、偏移文件、播放后端、同步包、音频或元数据。没有新增网络请求、收费调用和权限。点击歌词继续使用现有时间偏移换算，新增进度 `0..1` 边界。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `gradlew.bat :composeApp:desktopTest --tests com.resonance.player.LyricScrollTest` | Windows / JDK 17 | 通过，3 项数学回归；共享 UI 编译通过 |
| `gradlew.bat :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest` | Windows / SDK 36 | 通过，Debug 与测试 APK 本次构建 |
| `desktopTest --tests com.resonance.player.LyricScrollTest --tests com.resonance.player.lyrics.LrclibLyricsRepositoryTest` | Windows / JDK 17 | 通过，3 项居中数学 + 3 项歌词仓库回归，0 失败/跳过 |
| ADB `am instrument -w -r -e class 'com.resonance.player.LyricsScrollDeviceTest,com.resonance.player.MusicStageDeviceTest' -e stageTheme light .../AndroidJUnitRunner` | MuMu 12 / Android 12 | 通过，最终 `OK (10 tests)`：7 项歌词专项（含换歌同句重定位）+ 3 项完整播放器歌词/队列/seek/关闭、大字号控制可达、播放/切歌；日志 `output/promo/device-final-10-tests.log` |

最终构建与定点桌面测试合并运行命令：`gradlew.bat :composeApp:desktopTest --tests com.resonance.player.LyricScrollTest --tests com.resonance.player.lyrics.LrclibLyricsRepositoryTest :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest --no-configuration-cache --console=plain`，`BUILD SUCCESSFUL`，日志 `output/promo/build-final.log`。本次未运行全量桌面测试或 Release Lint。

完整播放器回归首次有 1 项在截图前因 MuMu 输入设备切换而丢失默认测试 Activity，另 2 项通过。将 `MusicStageDeviceTest` 改用同一 Debug 稳定宿主后，3 项全部通过；没有降低原断言。该宿主与拍摄相关配置见宣传片记录。

## 限制与后续

用户当前没有可用手机，本次使用 MuMu，不将模拟器结果描述为真机验证。Windows 已编译共享代码和运行定点测试，未执行 Windows 鼠标/触屏的交互验收。此次没有发布新版本或替换正式安装包。拍摄宿主与视频另见 [宣传片记录](2026-10-29-03-native-mumu-promo-film.md)。

## 同步更新的文档

- `docs/README.md` 变更索引。
- `docs/DEVELOPMENT.md` 歌词滚动行为、测试定位及拍摄说明。
