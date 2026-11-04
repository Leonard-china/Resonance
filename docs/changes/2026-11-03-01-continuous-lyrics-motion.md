# 2026-11-03 — 将歌词闪现切换改为连续滚动，并稳定高亮排版与手动跟随

- 修改日期：2026-11-03（Asia/Shanghai）
- 类型：Bug 修复 / 交互调整
- 适用版本：`0.6.5 / 39`，未升版
- 影响平台：共享 Compose UI（Android 优先、Windows 共用）
- 关联需求：用户认可 macOS 毛玻璃播放器图片，同时反馈上一版歌词滚动呈闪现式、不流畅。
- 关联记录：[毛玻璃图片方向](2026-11-02-10-ui-redesign-skills-and-frosted-material.md)、[先前歌词居中修复](2026-10-29-02-lyrics-centered-follow-and-manual-browsing.md)。

## 需求或问题

歌词换句应能看见同一句文字连贯地滑入焦点位置，阅读过程中不应整组消失后重新出现。当前图片材质方向已得到用户认可；本次只处理歌词动效和直接相关的跟随交互，整体毛玻璃 UI 尚未实施。

## 原因与依据

本次以工作区实际源码为依据，发现与 10 月 29 日文档存在差异，不能将历史验收视为当前实现已通过：

- `HeroSyncedLyricsPreview` 对整组「上一句／当前句／下一句」使用 `AnimatedContent`，换句会同时替换并淡出／淡入两组三行文字。同一句歌词在两组中字号、位置不同，容易产生重影和闪现观感。
- `LyricsContent` 仅监听 `activeIndex`，目标行离屏时直接 `scrollToItem` 后 `scrollBy`，这条路径没有动画。居中计算又以 `viewportSize.height` 对比包含内容留白的 item offset，坐标原点不同。
- `LyricRow` 高亮时瞬时切换字号、字重，可能改变行高和换行；滚动目标随之偏移。
- 当前 `LyricScroll.kt` helper 存在但没有接入。手动浏览从拖动开始计时，清空标记不会触发同一句重新定位；暂停时也不会独立恢复。

坐标与滚动接口依据：[LazyListLayoutInfo](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListLayoutInfo)、[LazyListState](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState)。以上是源码确认的路径；尚未将用户使用的旧安装包与当前源码做版本一致性复现。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [SyncedLyricsPreview.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SyncedLyricsPreview.kt) | `SyncedLyricsPreview`、`PreviewLyricRow` | 用一份稳定 key 的 LazyColumn 保留每句身份，按实际行高滚入焦点；保留前奏、译文、舞台字阶和大字号紧凑预览；仅颜色与绘制层 0.94～1 缩放过渡，不整体淡出、不重排文字 |
| [LyricScroll.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LyricScroll.kt) | `rememberLyricListState`、`FollowLyric`、`centerLyric` | 曲目与原文时间轴决定列表身份，译文到达不重建；观察目标、视口和实测行高，避免每次播放毫秒更新重启动画；同一目标的布局变化也重新定位；可见行复用现有 280ms `PlayerSpec`，离屏目标动画进入测量范围再校正 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `HeroSyncedLyricsPreview`、`LyricsContent`、`LyricRow` | 接入两处连续跟随；固定字号／字重，平滑高亮；手动输入暂停跟随，拖动／惯性结束 5 秒后恢复；返回和点击同句立即请求定位；首尾半视口留白，纯文本从顶部浏览，seek 夹在 `0..1` |
| 同上 | 歌词工具／测试语义 | 恢复先前文档和已有回归约定的收起工具入口，避免小屏工具挤占歌词区域；保留原翻译和校准回调，恢复列表、行、返回按钮的测试标识与选中语义，返回按钮至少 48dp 高 |
| [LyricsMotionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/LyricsMotionTest.kt) | 原生逐帧回归 | 检查换句存在中间位置、长句不重排、快速重定向、预览行身份不替换、减少动效、首尾行与译文到达 |
| [LyricsScrollDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsScrollDeviceTest.kt)、[LyricsPreviewMotionDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsPreviewMotionDeviceTest.kt) | Android 回归 | 保留原有 7 项交互用例，新增换句中间帧、快速反向、离屏 seek、预览连续性／减少动效用例；测试使用合成文本，不接触真实曲库 |
| [PRODUCT.md](../../PRODUCT.md) | 材质方向与用户反馈 | 记录用户认可当前毛玻璃图片方案及歌词流畅度要求；不宣称全项目视觉重构已完成 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 封面歌词换句 | 整组三行更换、交叉淡入淡出 | 同一句保留在列表中，连续经过焦点位置 |
| 完整歌词页切到离屏行 | 直接跳位，再即时校正 | 动画接近目标并按实际高度居中 |
| 长句进入高亮 | 字号／字重改变，可能换行 | 排版尺寸保持稳定，仅颜色过渡 |
| 快速前后 seek | 旧转场可能重放整组内容 | 当前滚动可被取消，从当前可见位置重新定位 |
| 手动浏览／暂停后返回 | 依赖下一句时间戳变化 | 按钮立即返回，停止手动滚动 5 秒后独立恢复 |
| 翻译异步到达 | 列表行高变化缺乏可靠重定位 | 保留列表身份并按新行高修正焦点 |
| 减少动效 | 分散处理 | 定位即时完成，取消预览缩放和颜色动画 |

初次进入允许直接定位；跨越大量歌词的 seek 沿用 Compose 的虚拟化滚动，不承诺逐行播放整首歌词。所有动效仅服务于定位与阅读连续性。

## 平台与数据影响

Android、Windows 共用原生 UI，没有新增 Web 页面、动画依赖、网络请求、权限或收费调用。不改音频播放后端、歌词时间戳／偏移持久化、音频文件、元数据、配置和同步包格式；不读写真实用户音乐。临时验证工具与工作副本放在忽略的 `build/lyrics-motion-2026-11-03/`，不作为提交资源。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `bash ./gradlew :composeApp:desktopTest --tests com.resonance.player.LyricScrollTest --no-configuration-cache --console=plain` | Linux 初始环境 | 失败：现有 Wrapper 脚本为 CRLF，Bash 解析报错；本机也没有现成 JDK。保留原脚本，后续通过临时 JDK 直接调用仓库 Wrapper JAR |
| Gradle Wrapper `:composeApp:desktopTest --tests com.resonance.player.LyricScrollTest` | Linux / 临时 Temurin JDK 17 | 通过；共享 UI、桌面代码与测试编译通过，3 项数学回归通过。日志 `build/lyrics-motion-2026-11-03/desktop-test.log` |
| Gradle Wrapper `:composeApp:desktopTest`，筛选 `LyricScrollTest` 与 `ui.LyricsMotionTest` | 同上 / Compose 原生离屏渲染 | 通过，6 项，0 失败／错误／跳过。逐帧确认中间位置、稳定行高／行身份、快速重新定位和减少动效。日志 `build/lyrics-motion-2026-11-03/motion-tests.log` |
| 最终歌词与播放器布局定点回归（完整命令如下） | 同上 | 通过，9 项，0 失败／错误／跳过；额外检查大字号短窗口、宽屏舞台歌词／队列／seek、短横屏控制可达。日志 `build/lyrics-motion-2026-11-03/player-tests.log` |
| Gradle Wrapper `:composeApp:assembleDebugAndroidTest --no-configuration-cache --console=plain` | Linux | 环境受阻：Android SDK 未安装，原 `local.properties` 指向目录不存在，报 `SDK location not found`。本次未编译 Android 测试包、未安装或执行设备用例。日志 `build/lyrics-motion-2026-11-03/android-test-build.log` |
| 增量差异、文档链接与 CRLF 兼容 whitespace 检查 | 当前工作区 | 通过；仅核对本次文件，保留原有未提交修改与换行格式 |

最终执行命令（工具链位于忽略的临时构建目录）：

```bash
build/lyrics-motion-2026-11-03/tooling/jdk/bin/java \
  -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain \
  :composeApp:desktopTest \
  --tests com.resonance.player.LyricScrollTest \
  --tests com.resonance.player.ui.LyricsMotionTest \
  --tests com.resonance.player.ui.UiVisualContractTest.galleryPlayerKeepsTransportReachableWithLargeTextInShortWindow \
  --tests com.resonance.player.ui.UiVisualContractTest.wideStageKeepsLyricsQueueAndTransportConnected \
  --tests com.resonance.player.ui.UiVisualContractTest.shortLandscapeKeepsPaneTabsAndTransportInsideViewport \
  --no-configuration-cache --console=plain
```

本次报告来自 `composeApp/build/test-results/desktopTest/` 中三份对应 XML，各 3 项通过。不是全量测试，也不是 Windows 实机／Android 帧率实测。新增 Android 专项用例未执行；历史 MuMu 通过记录不计入本次。

## 限制与后续

- 整体毛玻璃视觉重构仍待实施；此次未打包或发布新版。
- Android 真机／模拟器帧率、触摸中断、Windows 鼠标滚轮手感需要在原生设备上验收；离屏渲染断言不能替代设备帧率数据。
- 不将工作区中已有的其他未提交改动纳入本次修复成果。

## 同步更新的文档

- `docs/README.md`：确认材质方向、歌词修复入口与变更索引。
- `docs/DEVELOPMENT.md` 第 8 节和第 12.3 节：当前滚动行为、源码与测试定位，核对日期 2026-11-03。
- `PRODUCT.md`：用户已认可的方向和本次反馈。
