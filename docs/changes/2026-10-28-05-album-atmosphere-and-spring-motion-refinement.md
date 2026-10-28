# 2026-10-28 — 增加封面取色氛围与流动波浪，恢复跳跃回弹切换

- 修改日期：2026-10-28（Asia/Shanghai）
- 类型：功能 / 界面与动效调整 / 测试
- 适用版本：`0.6.3 / 37`（`version.properties`，未升版）
- 影响平台：共享 / Android / Windows
- 关联需求：在 [音乐舞台 UI](2026-10-28-03-mineradio-inspired-native-music-stage.md) 和 [手机布局验证](2026-10-28-04-android16-device-ui-validation.md) 基础上进一步打磨，用户特别要求动画流畅、有特色，并恢复之前“有跳跃感”的切换。

## 需求或问题

上一轮舞台主要依赖种子光场，真实封面的颜色没有参与背景；波浪进度条虽然保留形状，但播放时没有流动。页面横移和曲库栏目使用定时减速动画，用户希望恢复弹簧越过后回落的切换感。空曲库和助手空会话仍较空泛，与播放器的新视觉层级不一致。

## 原因与依据

`PageSlideSpec` / `DetailSlideSpec` 与曲库入场原本为 tween，不能表现位移超调；仅保留容器的 SizeTransform 弹性，不能等同于内容本身回弹。`WavySlider` 的曲线只随进度更新，缺少播放相位。背景原先未读取 ArtworkMemoryCache 的封面像素。

本次使用 Compose 原生 spring、graphicsLayer 与绘制 API，沿用现有主题、状态和 Slider。性能实现选择包括绘制阶段读取相位、复用 Path、后台取色；这些措施本身不构成真机性能提升的证明。

## 具体修改

| 文件 | 关键类/函数 | 具体修改与目的 |
| --- | --- | --- |
| [AlbumVisuals.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/AlbumVisuals.kt) | `AlbumAtmosphere`、`rememberAlbumAtmosphere`、`atmosphereFromBitmap`、`GenerativeAlbumSleeve` | 复用封面缓存及 revision，在 IO 加载、Default 上取色；采样 8×8 网格，左右半幅分别按饱和度加权，忽略透明、近黑和近灰像素。没有有效色彩时使用四组确定性种子配色。原生太阳/等高线插画替代旧几何缺省封面，无新增图片资源。 |
| [MusicStage.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/MusicStage.kt) | `rememberPlaybackVisualPhase`、`MusicStageBackdrop`、`musicSleevePerspective` | 粒子与波浪共用播放/前台/减少动效门控；取消时保留相位。舞台双色光场随封面以 220ms 过渡；保持 64 个粒子和 48s 周期。封面静态透视为 Y -5°、Z -1.2°。 |
| [ResonanceTheme.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt) | `ResonanceMotionTokens` | 页面、详情和选择位移共享原生低回弹阻尼 / 中等刚度 spring；既有 Spring 别名指向对应 Spec，避免并行配置。播放器 drawer 仍用原有 280ms 曲线。 |
| [LibraryTabTransition.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryTabTransition.kt) | `LibraryTabTransition` | 保持单棵 Lazy 树及提升的滚动状态；32dp 方向位移叠加约 6dp 跃起，弹簧落位，alpha 限制为 0.15～1。减少动效/键盘输入即时切换。 |
| [LibraryDetailTransition.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryDetailTransition.kt) | `LibraryDetailTransition` | 详情入退使用共享弹簧，键盘切换无过渡；保留移动期间不透明 Canvas，减少动效保持顺序淡入淡出。 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `PaneSwitcher`、`PaneContent`、`PlaybackProgress` | 播放器提供当前封面氛围；分栏改为有图标的分段底座，底块在绘制时读取弹簧位置，连续选择可重新定向。分栏内容继承页面 spring。手机正常字号的焦点歌词为 22sp / 30sp，邻行减弱；大字号紧凑布局优先控制区。传递真实 isPlaying 给波浪。 |
| [SoftNeumorphicButtons.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt) | `SoftRefinedPlaybackScrubber`、`WavySlider` | 播放波浪以 3600ms 周期流动，振幅在手柄及轨道两端归零；缓存并重置 Path。保留原生 Slider 至少 48dp 交互区域、预览及 seek 回调；音量仍为直线轨道。 |
| [ResonanceSelectionTab.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ResonanceSelectionTab.kt) | `ResonanceSelectionTab` | 新增默认 false 的 contained 选项供播放器分段标签使用，保留原生选中语义、焦点和点击；曲库继续使用原来的下划线。 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `AlbumArtwork`、`EmptyLibrary`、`DestinationContent` | 缺省封面使用原生插画；空曲库加入叠层封面与搜索、导入、新建入口，短屏隐藏插画。主导航和工具二级页通过共享 Spec 获得回弹，保留 Canvas 视口和 SizeTransform。 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `AssistantWelcome`、`AssistantScreen` | 空会话显示欢迎语与三张可点示例，仅填草稿；输入框、附件和发送组成统一圆角输入台，移除用量横条但不改统计/请求逻辑。保留窄屏标题与操作分行。 |
| [UiVisualContractTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt) | 取色、空曲库、波浪测试及产物生成 | 检查有效色调/透明回退、100%/150% 字号入口、播放/暂停图像变化及 seek；扩充到 60 张六页面截图，并生成 360 帧原生动效预览。 |
| [LibraryNavigationTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LibraryNavigationTest.kt)、[UiInteractionContractTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/UiInteractionContractTest.kt) | 新增草稿不发送与 Android 取色断言 | 校验示例只填输入框且聊天调用为零，Android Bitmap 转 ImageBitmap 后的取色可用；已编译，尚未在新版设备包执行。 |
| [MusicStageFrameMetricsTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/MusicStageFrameMetricsTest.kt) | `playingStageReportsActualWindowFrameDurations` | 使用真实 Choreographer / Window FrameMetrics，播放器或导航两场景；预热 1.5s 后采集约 8s，JSON/CSV 写入自身 Debug 外部文件目录。不使用 Compose 人工测试时钟，不连接真实音频、曲库或网络。 |

门控仅停止持续粒子/波浪；切歌的有限颜色过渡遵循减少动效。取色读取已有封面或内嵌封面，不新增联网来源、磁盘取色缓存或权限。缺省插画仍按 seed 稳定生成。进度波形是视觉表现，不读取音频振幅。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 主导航、详情、封面/歌词/队列切换 | 定时滑动减速 | 弹簧轻微越过后回落；曲库栏目另有小幅跃起，标签底块随选择回弹 |
| 连续选中播放器分栏 | 文字/下划线变化 | 原生选中语义即时更新，移动底块的 spring 从当前值重新定向 |
| 播放 / 暂停 | 波浪随进度变化但无相位流动 | 播放时缓慢流动，暂停/后台/减少动效时定住；真实 seek 操作保持 |
| 切换不同颜色封面 | 种子背景 | 有有效封面色时过渡到封面双色光场，取色失败/无色彩时回退 |
| 空曲库 / 空会话 | 简要提示，视觉层次较弱 | 原生封面插画、明确的入口层级与草稿示例，保持可访问操作 |

## 平台与数据影响

共享 Compose 改动同时影响 Android 与 Windows；新增 FrameMetrics 仅属于 Android 测试源码。没有修改平台接口、音频处理/转换、任务防竞争、持久化/同步格式、文件删除、元数据写回、账号或付费请求。版本与正式发布包不变。调试包与正式包并存；本次最新包尚未安装到手机。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat :composeApp:assembleDebugAndroidTest :composeApp:desktopTest --tests 'com.resonance.player.ui.UiVisualContractTest' --no-configuration-cache --console=plain` | Windows / JDK 17 / SDK 36 | 回弹与测量夹具修正后通过，53s；测试 APK 编译通过。日志 `build/stage-refinement-2026-10-28/motion-validation-build.log`。此时桌面为 13 项，后续加入预览生成后以下行最终 14 项为准。 |
| `.\gradlew.bat :composeApp:assembleDebug --no-configuration-cache --console=plain` | 同上 | 通过，4s；最新 APK 与本次回弹源码对应。日志 `final-apk-build.log`。 |
| `.\gradlew.bat :composeApp:desktopTest --tests 'com.resonance.player.ui.UiVisualContractTest' --no-configuration-cache --console=plain` | Windows / 原生 Compose 离屏场景 | 最终通过，3m45s，14 项、0 失败/错误。包括功能断言、截图及视频帧生成，不等同于 14 项设备用例。日志 `motion-preview-build.log`；报告为 `composeApp/build/test-results/desktopTest/TEST-com.resonance.player.ui.UiVisualContractTest.xml`。 |
| 原生截图与动效目视核对 | ImageComposeScene，深浅主题及五种窗口/字号组合 | 生成 60 PNG，检查手机播放器、歌词、助手欢迎与空曲库；动效 720×1600、360 帧，合成 6s / 60fps H.264 MP4。影片是离屏合成，不是手机录屏或设备帧率证明。 |
| `adb devices -l` 与真机测量准备 | 先前 API 36 手机 | 早期旧夹具曾对上一包运行播放器测量；发现采样时间受 onActivity 等待空闲影响后改为 runOnMainSync，并新增导航场景。旧采样不参与前后对比。随后设备从 ADB 断开，最终设备列表为空；最新主包安装、修正后两包同夹具对比及新增设备断言均因环境受阻未执行。 |
| Markdown 本地链接检查、`git diff --check` | Windows / PowerShell | 本记录、索引和开发说明共 189 个本地文件链接均存在，diff 检查通过；Git 的 LF/CRLF 提示不属于空白错误。 |

最新 Debug APK：`composeApp/build/outputs/apk/debug/Resonance-0.6.3-debug.apk`，176,111,928 字节，SHA-256：`142331DAAB19C1BA1EBC9CBDA69EBC1FBA28D0F44249AC17544A1EC177C834DD`。预览：`build/stage-refinement-2026-10-28/native-motion-preview.mp4`。日志、截图、测试音频与本机工具路径均不作为提交资源。

设备测量命令为 `adb shell am instrument -w -r -e class com.resonance.player.MusicStageFrameMetricsTest -e metricsLabel <label> -e metricsScenario <player|navigation> com.resonance.player.debug.test/androidx.test.runner.AndroidJUnitRunner`；label 仅允许小写字母和连字符。输出位于自身 `files/stage-frame-metrics/` 的 JSON/CSV。报告排除 FIRST_DRAW_FRAME，记录 TOTAL_DURATION、可用的 DEADLINE、分位耗时、超过 deadline 的样本数及 metrics 回调丢弃数；回调丢弃数不是渲染掉帧数。口径参考 [Android FrameMetrics](https://developer.android.com/reference/android/view/FrameMetrics)；Debug 夹具不能代替 Release 基准，帧预算应随刷新率判断，见 [Android 慢渲染说明](https://developer.android.com/topic/performance/issues/render)。

## 限制与后续

- 手机重连后，先在前一包上安装最终测试夹具并执行 baseline-player / baseline-navigation，再安装当前主包执行 refined-player / refined-navigation；同设备、同旋转、同夹具、相近温度比较，不能复用旧采样宣称提升。
- 新版取色与草稿设备断言，以及此前核心与横竖屏用例，需在最新主包重新运行；前一轮 35 次执行仅属于记录 04 的包。
- 未运行本次全量 allTests、Release Lint、Release 性能基准、Perfetto 或 Windows 安装包/真实听音验收。没有发布、提交或推送；不宣称新版已在手机完成验收。
- 当前已知未完成项为设备断开导致的最新包安装、设备回归与帧耗时对比；代码/测试夹具与离屏预览已就绪。

## 同步更新的文档

- [文档入口](../README.md)：新增本记录并明确新版设备复测状态。
- [开发说明](../DEVELOPMENT.md)：更新第 11 节视觉、回弹、波浪、取色与回归口径，以及任务定位表和手势章节的旧参数。
- 记录 03 / 04 保留各自当时的实现与验证事实。
