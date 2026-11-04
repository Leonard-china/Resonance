# 2026-11-03 — 补齐全应用磨砂页面、列表与弹窗材质

- 修改日期：2026-11-03（Asia/Shanghai）
- 类型：UI 修复 / 本地构建
- 适用版本：0.6.5 / 39，版本来自 `version.properties`，不升版
- 影响平台：共享 Compose；Android、Windows、Linux
- 关联需求：用户反馈 macOS 毛玻璃只在歌词详情明显，没有覆盖整体应用
- 关联记录：[首轮材质](2026-11-03-02-frosted-ui-and-release-apk.md)、[本次歌词焦点](2026-11-03-10-restore-lyric-focus.md)

## 需求或问题

曲库、助手、设置与部分功能页仍呈近乎实色的平面，原先记录的“整体磨砂”没有充分体现到实际界面。需要保持现有功能与原生交互，同时将认可的材质方向应用到各页面和弹窗。

## 原因与依据

- 主壳 `FrostedBackdrop` 使用 90% 画布遮罩，目的地又叠加 38% 遮罩，再覆盖面板，背景颜色几乎不可见；无封面种子色也经过多层衰减。
- 智能管理、品味雷达绘制整页不透明 Canvas；推荐、历史、任务、歌源、存储和搜索列表仍使用不透明 surfaceContainer。
- 普通 Material 确认弹窗没有统一材质；`LocalGlassState` 仅存在于 LibraryShell，App 级弹窗无法访问采样源。
- 本次先检查当前源码及已有原生截图，再新增实际主壳目的地截图。旧截图只作为定位依据，不算本次验收。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [FrostedBackdrop.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/FrostedBackdrop.kt) | `FrostedBackdrop` | 主壳遮罩调整为浅色 52%／深色 48%，播放器为 58%／54%；无封面使用已有种子色，保留柔焦与静态背景 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt)、[LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `LocalGlassState`、`DestinationContent` | 采样状态提升到 App，主壳继续绘制唯一背景源；所有目的地使用带 6dp 外留白的共享玻璃外层，保留导航／返回／滚动状态 |
| [FrostedDialogs.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/FrostedDialogs.kt) | `FrostedAlertDialog` | 包装 Material AlertDialog，仅统一材质、细边与阴影；原按钮、内容、语义、DialogProperties 和关闭回调保持 |
| [MusicTasksScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicTasksScreen.kt)、[RecommendationHistoryScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/RecommendationHistoryScreen.kt)、[MusicSourcesScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicSourcesScreen.kt)、[OnlineSongSearch.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/OnlineSongSearch.kt) | 中性 Surface | 推荐／历史／任务／歌源／存储／在线搜索改用 GlassLight，让背景透过；列表行不单独计算模糊 |
| [SmartManagerScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SmartManagerScreen.kt)、[TasteRadarScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TasteRadarScreen.kt)、[SemanticSongSearch.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SemanticSongSearch.kt)、[TutorialScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TutorialScreens.kt) | 页面背景／Card | 移除两处整页实色遮挡；中性卡片透色，保留图表、交互与业务动作 |
| [TogetherRoomComponents.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherRoomComponents.kt)、[ScreenshotImportDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ScreenshotImportDialog.kt)、[ShareCardDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ShareCardDialog.kt) | 自定义弹窗 Surface | 共享队列／截图核对／分享预览的窗口表面接入材质；分享内容画布仍保留原设计 |
| [UiVisualContractTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt) | `renderFrostedDestinationsThroughTheRealShell` | 经真实 LibraryShell 渲染 14 个目的地×深浅主题；使用合成曲库和空服务替身，不接触真实数据 |

Material 弹窗调用点通过显式 import alias 接入，包括助手、推荐、历史、歌源、工具、教程、一起听／伙伴／邀请／AI 队列、登录、同步包、播放错误和播放器弹窗。仅改变公共容器，不复制各个业务弹窗的动作逻辑。

Haze 官方说明支持对话框使用源采样；本项目沿用已安装的 2.0.0-alpha02 API，没有按新版本文档迁移依赖。参考：[官方源采样与 Dialog 说明](https://chrisbanes.github.io/haze/dev/core-concepts/)。Android／跨窗口的实际性能仍需设备验证。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 曲库／助手／工具／设置 | 多层实色令磨砂接近不可见 | 共享透色背景、页面面板与既有磨砂页头／底座 |
| 推荐／任务／歌源／存储／搜索 | 中性列表卡片遮住背景 | 玻璃色轻表面，原滚动和按钮继续可用 |
| 智能管理／品味雷达 | 独立 Canvas 覆盖整个页面 | 使用主壳玻璃底，卡片与页面统一 |
| App 根级确认或恢复弹窗 | 取不到主壳采样源 | 与主壳共享状态并使用统一 Material 弹窗容器 |
| 模糊不支持 | 原有回退 | 继续使用可读的高不透明度回退，不强制低端设备逐行 blur |

## 平台与数据影响

共享原生 Compose UI 影响三端。本次不改变播放任务、音量状态、同步协议、推荐生命周期、配置格式、权限、文件删除或音频标签。不新增联网、凭据或依赖。保留前一轮每日推荐切歌修复；本地 APK 包含当前工作区已有内容，不把所有既有改动归为本次成果。

## 验证

环境为本机 Linux / JDK 17 / Android SDK 36，离屏截图使用原生 Compose `ImageComposeScene`。工具路径仅在忽略的本机目录中，未作为项目资源提交。

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 首次整类 `UiVisualContractTest` 与 `LyricsMotionTest` | Linux / JVM | 中断：包含无关的 360 帧影片生成，在软件渲染中耗时过长，主动终止；不记为通过 |
| 首次定向 10 项回归 | Linux / JVM | 9 通过、1 失败：新截图夹具缺少 `getImportProgress`，已补齐替身；不是生产代码异常 |
| 定向 10 项＋Android debug 编译＋release 构建 | Linux / JVM / SDK 36 | 通过，5m15s；日志 `build/glass-coverage-2026-11-03/final-verification.log` |
| 完善存储／听歌统计替身后，定向回归并增加双主题播放错误弹窗检查 | Linux / JVM | 11 项全部通过，29s：`LyricsMotionTest` 4 项、`UiVisualContractTest` 7 项；日志 `build/glass-coverage-2026-11-03/render-verification.log` |
| 14 个真实主壳目的地×深浅主题截图，歌词焦点截图及错误弹窗 | 原生 Compose 离屏渲染 | 生成并人工核对 28 张页面截图，重点检查曲库／助手／设置／推荐／一起听／下载空间／品味雷达；歌词与错误弹窗可读。截图在 `composeApp/build/glass-coverage/`，错误弹窗在 `composeApp/build/ui-gallery/screenshots/playback-error-{light,dark}.png` |
| `apksigner verify --verbose` | SDK Build Tools 36.0.0 | 通过：APK Signature Scheme v2，1 个签名者 |
| `git diff --check`（启用 `cr-at-eol`）与新增行空白检查、记录与索引链接校验 | Git / Python | 通过：本轮新增行无尾随空格，25 个相对链接存在，两份记录模板章节与索引均齐全 |
| Android Lint | 未执行 | 本次构建显式排除三个 lintVital 任务 |
| Android 真机、Windows 原生窗口、实际 GPU 性能 | 未执行 | 未连接 Android 设备，离屏验证不代替设备验收 |

实际最终构建命令（省略本机 JDK／SDK 环境变量赋值）：

```bash
"$JAVA_HOME/bin/java" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain \
  :composeApp:desktopTest \
  --tests 'com.resonance.player.ui.LyricsMotionTest' \
  --tests 'com.resonance.player.ui.UiVisualContractTest.renderFrostedDestinationsThroughTheRealShell' \
  --tests 'com.resonance.player.ui.UiVisualContractTest.galleryPlayerKeepsTransportReachableWithLargeTextInShortWindow' \
  --tests 'com.resonance.player.ui.UiVisualContractTest.shortLandscapeKeepsPaneTabsAndTransportInsideViewport' \
  --tests 'com.resonance.player.ui.UiVisualContractTest.wideStageKeepsLyricsQueueAndTransportConnected' \
  --tests 'com.resonance.player.ui.UiVisualContractTest.textAndSelectedRolesStayReadableAcrossAllThemes' \
  --tests 'com.resonance.player.ui.UiVisualContractTest.songInformationRemainsReachableFromPlayerMenu' \
  :composeApp:compileDebugKotlinAndroid :composeApp:assembleRelease \
  -x lintVitalAnalyzeRelease -x lintVitalReportRelease -x lintVitalRelease \
  --no-configuration-cache --console=plain --continue
```

随后仅修改测试替身，使用上述相同测试筛选再加 `--tests 'com.resonance.player.ui.UiVisualContractTest.playbackErrorOffersRetryInBothThemes'`，移除 Android 构建任务、lint 排除项与 `--continue` 后执行，得到最终 11 项通过结果。生产源码与已构建 APK 一致。

本次实际产物：`release/0.6.5/2026-11-03-glass-coverage/Resonance-0.6.5-release.apk`，167,349,953 字节；同目录保存 `README.md` 和 `SHA256SUMS.txt`。这是本地试用包，未发布到外部服务，也未覆盖之前的试用包。

SHA-256（由本次产物计算）：`c9d5266d51c43e4210106ac074228296f9f88ccb774e759045c9a9844b58f276`。

## 限制与后续

没有连接 Android 设备，真实手机的背景模糊、跨窗口效果、帧率／发热尚未验收。离屏 Compose/JVM 截图验证共享布局，不能代替 Android 渲染器或 Windows 实机。系统文件选择器、系统授权和内嵌第三方登录页面仍使用平台自身界面。

## 同步更新的文档

`docs/README.md` 变更索引、`docs/DEVELOPMENT.md` 当前材质与定位说明、`DESIGN.md` 全应用材质约定、`PRODUCT.md` 本轮反馈及历史验证范围。
