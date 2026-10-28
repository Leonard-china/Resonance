# 2026-10-27 — 将 18 号画廊展台方案落实为原生 UI

- 修改日期：2026-10-27（Asia/Shanghai）
- 类型：原生 UI / 动效反馈 / 回归验证
- 适用版本：`0.6.0 / 34`，不升版
- 影响平台：Compose Multiplatform 共享界面 / Android / Windows
- 关联需求：用户从 20 张概念预览中选择「18 画廊展台」，并要求继续
- 关联记录：[20 张概念预览](2026-10-27-03-dimensional-ui-concept-previews.md)、[上一轮 UI 与动效整理](2026-10-27-02-native-ui-and-motion-polish.md)

## 需求或问题

此前的轻阴影、圆角封面和玻璃面板未达到用户期望的立体感。18 号方案通过方正的封面装裱、底部展架、定向投影、白色播放台与留白表现层次。本轮将这些结构转为实际原生组件，并保留播放、歌单、歌词、音量和导航的现有操作。

## 原因与依据

以用户明确选中的预览为视觉依据，应用本地 frontend-design、material-design-3-ui、animate 与 jetpack-compose-performance 技能。使用现有 Compose 依赖中的 `dropShadow`，具体参数与修饰符顺序核对 [Android 官方阴影文档](https://developer.android.com/develop/ui/compose/graphics/draw/shadows)。没有升级依赖或引入 Web UI。

视觉规范：浅色墙面 `#F3F4F2`、台面 `#FCFDFC`、底边 `#D6DAD6`、青绿主色 `#2F7467`、正文 `#1C302B`；深色墙面沿用 `#111A1D`，台面 `#273236`。冰蓝配色仍可选。文字沿用原生 Sans Serif 字阶。封面为 2dp 小圆角、展架 9dp 高；台面默认 6dp 圆角，底部倒角在布局范围内绘制。定向阴影统一向右下偏移，并辅以小范围接触阴影。大片文字区域保持安静，不给每个文本块叠加阴影。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [GallerySurfaces.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/GallerySurfaces.kt) | `GalleryTokens`、`galleryShadow`、`galleryPlinth`、`GalleryArtworkMount` | 新增共享展架、台面、边缘高光和两层定向阴影；使用原生阴影渲染，倒角画刷通过 `drawWithCache` 缓存；展架预留投影空间 |
| [ResonanceTheme.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt) | `colors`、`ResonanceShapes` | 浅色背景改为淡灰白墙面，深色台面提高明度以区分层级；保留青绿/冰蓝、主题首帧和默认内容色；公共封面/面板圆角收敛 |
| [PlaylistGrid.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistGrid.kt) | `PlaylistListTab` | 每日推荐、自建歌单、新建入口使用封面展架；标题对齐画面，保留网格 key、滚动状态、教程目标和打开/创建回调 |
| [ResonanceSelectionTab.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ResonanceSelectionTab.kt) | `ResonanceSelectionTab` | 选中态改为青绿文字和短下划线；保留 48dp 高度、Tab 语义、焦点描边与原生 ripple，取消标签整体按压缩放 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `ArtworkPane`、`PlaybackControls`、`VolumeAndSpeedRow`、`NowPlayingTopBar`、`PaneSwitcher` | 主封面放上展架，播放/音量控制放入实体台面，歌词/队列表面改为不透明阅读区；减少背景光强度，移除播放器内部玻璃采样；「歌曲与来源」进入播放选项菜单 |
| [SoftNeumorphicButtons.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt) | `SoftHeroPlayButton`、`SoftUtilityButton`、`RefinedSlider` | 播放键使用收敛的青绿渐变和定向投影；次要按钮去掉各自圆盘，保留焦点和选中标记；滑块手柄有接触阴影，继续由原生 Slider 处理输入 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `MiniPlayer`、`BottomNavigationBar`、`DesktopSidebar` | 迷你播放器改为有底边厚度的窄台面，封面小圆角与整体一致；导航和桌面侧栏安静处理，维持原生选中语义与自适应结构 |
| [ToolsScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) | `SettingRow` | 公共工具/设置图标落在小台面上，文字行继续以留白和分隔组织 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `AssistantScreen` | 输入框和发送操作归入同一台面，保留聊天宽度、IME 布局、建议草稿及显式发送流程 |
| [UiVisualContractTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt) | `galleryPlayerKeepsTransportReachableWithLargeTextInShortWindow`、`songInformationRemainsReachableFromPlayerMenu` | 增加小屏大字号主控件边界、播放设置入口和歌曲信息入口回归；原主题对比度检查扩展到台面；截图改存 `build/ui-gallery/screenshots/` |

### 布局与动效

- 手机保留纵向结构；宽窗口继续使用封面与信息双栏；短横屏右栏继续可滚动。
- 大字号且窗口不足 840dp 高，或窗口低于 560dp 时采用紧凑播放布局。手机隐藏内联音量区后仍可通过「播放选项 → 播放设置 · 音量与速度」进入；操作由回归实际调用验证。
- 封面仅在换曲时做 220ms 短位移与淡入，展架不随歌曲一起切换；减少动效时使用 90ms 淡入淡出。取消暂停时封面整体缩小，避免画作离开展架。
- 播放键按压采用已有 120ms ease-out，缩放到 0.98 并轻降 1.5dp；次要按钮仅 0.98 缩放。减少动效和键盘输入时不触发这些位移或缩放。
- 阴影保持静态，按压只更新图层变换，不逐帧改变模糊半径。没有新增自动循环动效或列表入场动画；既有导航、手势、歌词跟随与减少动效规则继续保留。
- 原生截图复查发现固定正方形外容器会压缩展架，已改为按可用高度约束宽度，让封面、9dp 展架及投影留白共同参与测量。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 歌单与播放封面 | 圆角封面、轻阴影 | 小圆角装裱、底部展架、右下投影 |
| 主要播放操作 | 独立圆盘按钮 | 一个实体控制台承载操作，主播放键突出 |
| 曲库/播放器分栏 | 胶囊选中背景和轻缩放 | 短下划线和文字强调，保留选择及焦点语义 |
| 歌曲来源入口 | 标题下方常驻按钮 | 播放选项菜单内「歌曲与来源」 |
| 大字号手机 | 内联音量区占用封面空间 | 使用紧凑布局，音量/倍速从播放设置可达 |
| 助手与设置 | 较平的输入区和图标底衬 | 输入操作共用台面，设置图标用小台面形成一致材质 |

## 平台与数据影响

仅修改共享原生 UI、桌面 UI 回归和文档。没有修改平台接口、音频引擎、任务防竞争、文件转换、用户歌曲、删除/标签写回、同步协议、配置字段或持久化顺序。没有增加权限、依赖或网络调用。生成式概念图没有打包进应用；真实封面仍来自原有异步加载与缓存链路。

保持版本 `0.6.0 / 34`。保留用户已有的 `HorizontalPageSwipeUnitTest.kt` 改动，以及前序技能安装、其他 UI 调整和文档。没有提交、推送或发布。

## 验证

| 命令或操作 | 结果 |
| --- | --- |
| `:composeApp:desktopTest --tests 'com.resonance.player.ui.UiVisualContractTest' :composeApp:compileDebugKotlinAndroid --no-configuration-cache --console=plain` | 通过，1m30s；桌面原生 UI 6 项检查及 Android 编译通过 |
| `:composeApp:allTests :composeApp:lintRelease :composeApp:assembleDebug :composeApp:compileDebugAndroidTestKotlin --no-configuration-cache --console=plain` | 通过，4m29s；桌面 191 项，Android Debug/Release JVM 各 39 项，均 0 失败 / 0 错误 / 0 跳过；Android instrumentation 源码编译通过。Release Lint 0 错误、105 个警告；调试 APK 已重新构建 |
| `:composeApp:desktopTest --tests 'com.resonance.player.ui.UiVisualContractTest' :composeApp:lintRelease :composeApp:assembleDebug --no-configuration-cache --console=plain` | 展架测量修正后通过，1m29s；6 项 UI 检查、Release Lint 与 Debug APK 重建完成，40 张截图已重新生成，并复查手机封面展架 |
| 共享 Compose 原生截图 | 生成 40 张：曲库、播放器、工具、助手、设置 × 深浅模式 × 手机/桌面/150% 字号/短横屏。已目视核对主要手机页、深色设置、桌面曲库与播放器、大字号及短横屏；使用仓库示例数据，缺失封面显示原有几何占位图 |
| `adb devices` | 环境受阻：设备列表为空；设备交互测试和帧率未执行 |
| 原有文件保护 | 用户已有手势测试、`version.properties`、`skills-lock.json` 与本次开始时哈希一致 |

验证日志、初始差异和 SHA-256 摘要在忽略的 `build/gallery-ui-2026-10-27/`。首轮编译遇到动画 `using null` 的 Kotlin 表达式语法错误，修正后进入上述通过结果；没有将中间失败记为通过。全量测试计数在后续定点执行覆盖报告前记录。

## 限制与后续

- 采用的是 18 号的布局与材质方向，生成图中的任意文字、图标和投影不会作为像素级规范照搬。系统菜单、文件选择器和平台窗口保持原有行为。
- 40 张图片是共享 Compose 的实际离屏渲染，不是 Android 系统窗口截图。未验证 Android 真机手势、系统栏、屏幕阅读器、硬件键盘、帧率与能耗；静态阴影的缓存方式不等同于实测性能提升。
- 深色/大字号可读性、控件语义、滑块回调和所调整的菜单可达性已有自动检查；真实设备可访问性仍需设备验收。

## 同步更新的文档

- [开发说明](../DEVELOPMENT.md)：第 11 节更新画廊材质、标签、按压反馈、播放信息入口、大字号布局和当前渲染回归位置。
- [文档入口](../README.md)：新增本次独立变更索引。
- AI 协作规则未变，`AI-WORKFLOW.md` 无需修改。
