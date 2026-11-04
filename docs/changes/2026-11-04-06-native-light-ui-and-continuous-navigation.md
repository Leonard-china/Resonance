# 2026-11-04 — 将双端浅色方案落地到原生界面与连续导航

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：UI 重构、导航修复、回归验证
- 适用版本：0.6.5 / 39；不修改版本号
- 影响平台：共享 Compose UI；Android、Windows、Linux
- 关联需求：用户确认[双端浅色方案](../../design-system/resonance/proposals/2026-11-04-light-glass/PLAN.md)后要求开工；承接[原型连续转场](2026-11-04-05-continuous-prototype-navigation.md)。

## 需求或问题

把已确认的浅色音乐播放器方案落实到原生应用，桌面与手机均保留完整业务入口。原生仍使用前轮灰色磨砂布局：桌面缺少统一工具栏和完整播放底座，设置为长表单，专辑/歌手以列表为主；曲库标签原转场直接替换移动中的内容树，无法保留退场页面的真实内容。

## 原因与依据

HTML 方案只影响预览文件，不会自动更新 Kotlin。原生主题、壳层、工具页、曲库集合和播放器需共用新的布局与色板。原 `LibraryTabTransition` 在同一容器内读取最新标签；新旧标签没有独立生命周期。改为按目标保留两棵真实页面树，退场后释放，反向切换复用尚未退场的树。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [ResonanceTheme.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt) | `colors`、`ResonanceMotionTokens` | 雾白画布、深灰文字、玫瑰强调色；统一 280ms / 12dp 转场与圆角。保留深色、系统主题和天气配色。 |
| [FrostedBackdrop.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/FrostedBackdrop.kt) | `FrostedBackdrop` | 浅色主壳改为静态柔和底色，避免每页重复采样大封面；沉浸播放器继续读取真实封面。 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `DesktopTopbar`、`DesktopSidebar`、`LibraryScreen`、`CollectionGrid`、`MiniPlayer` | 桌面侧栏/搜索/任务入口、真实歌单与收藏入口；专辑/歌手自适应网格；横向详情头；固定三段式播放条与真实音量/队列/收藏回调。手机保留合并底座。 |
| [PlaylistGrid.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistGrid.kt) | `PlaylistListTab` | 2/3/4 列封面网格，收藏固定入口；推荐开启或教程进行时保留推荐卡。 |
| [LibraryTabTransition.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryTabTransition.kt)、[LibraryDetailTransition.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryDetailTransition.kt) | `ContinuousPageTransition` | 主导航、曲库、详情、设置、播放器分栏共用可中断转场；固定布局尺寸，旧层禁止点击并退出无障碍语义；键盘/减少动效即时切换。 |
| [ToolsScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt)、[MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `SettingsSection`、`SettingsScreen`、`MusicFeatureSettingsSection`、`ToolPage` | 设置索引与八个分组页，保留原表单/保存/测试/登录行为；返回先回设置索引，教程按目标自动进入分组。工具正文不再套整页玻璃。 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `PaneContent`、`PaneSwitcher` | 顶部分栏与连续切换；宽屏补齐音量、音效、定时快捷入口，保留手机和短屏控制路径。 |
| [AppearanceSettings.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/AppearanceSettings.kt) | `AppearanceSettings` | 默认色板文案改为“默认配色”，减少动态说明与当前页面行为一致；保留 `AccentMode.Sage` 存储值。 |
| [UiBackDispatcher.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/UiBackDispatcher.kt)、[Android 返回](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/ResonanceBackHandler.android.kt)、[桌面返回](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/ResonanceBackHandler.desktop.kt) | `LocalUiBackDispatcher`、`ResonanceBackHandler` | 窗口内桥接原生返回回调给顶部返回按钮；优先退出当前详情/设置组，根级才导航到上一级。旧页面的停用与回收同步注销，系统返回/Escape 继续原路径。 |
| [TogetherRoomComponents.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherRoomComponents.kt) 等 | 房间头、助手/任务/品味/管家内容边距 | 统一手机 20dp / 桌面 40dp 内容边距与开放式页面结构；不改房间连接和业务回调。 |
| [UiVisualContractTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt) | `renderApprovedLightNativePagesAndSettings`、`nativePagesReverseWithoutRecreatingTheDepartingContent`、`renderApprovedPlayerPanes` | 原生双尺寸截图、设置逐组进入/返回、固定播放条、宽屏播放器控件、转场反向与打断后的生命周期回归。 |

原生视觉复核临时截图保存于 `.impeccable/review/`；`.gitignore` 仅新增该生成目录，设计规范 `.impeccable/design.json` 仍可提交。测试加入真实 `TutorialController` 的模拟持久化，避免缺少控制器导致教程画板为空；桌面侧栏歌单/收藏进入详情亦有断言。

独立复核指出短横屏曲库被标题与合并底座挤占，且桌面全局返回未读取当前嵌套状态。修正为高度 <480dp 且宽度 ≥480dp 时使用 72dp 图标 rail 与 60dp 紧凑播放条，短高曲库省略副标题/减小竖向留白；四项导航的触摸尺寸保留。返回按钮优先调用同一窗口内活动的原生返回处理器；覆盖 Android 大屏与共享桌面，不使用仅 desktop 可用的全局对象。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 进入设置 | 全部参数在同一长表单中 | 索引选择八个分组；组内返回索引，再返回工具 |
| 桌面切换曲库/助手/工具 | 内容头与操作分散 | 公共工具栏、侧栏、播放条固定，只有正文转场 |
| 点击侧栏歌单或收藏 | 无直接入口 | 打开真实歌单/收藏详情，沿用原播放和管理操作 |
| 快速反向切页 | 标签内容直接替换 | 退场页面继续存在；反向切换保留同一实例，结束后回收其他页面 |
| 教程目录浏览 | 桌面隐藏迷你播放器 | 普通教程可控制播放；快速引导仍隐藏底座 |
| 宽屏播放器 | 部分快捷操作仅从菜单打开 | 音量、音效、定时快捷入口与手机一致 |

## 平台与数据影响

调整共享 UI、两端返回处理器注册及桌面 UI 回归；`ResonanceBackHandler` 调用签名不变，新增窗口内 UI 回调桥接。不改变播放代次、音频转换、配置保存顺序、文件路径边界、同步包格式、删除/元数据写回和用户凭据。原深色/系统主题偏好继续生效，不强制覆盖用户设置。未修改独立官网、论坛或部署；未自动安装到用户设备或替换现有 Linux 安装。版本不变。

## 验证

本轮使用本地 JDK 17 和 Android SDK，Gradle 参数：`--no-daemon --max-workers=2 -Dorg.gradle.jvmargs='-Xmx3g -Dfile.encoding=UTF-8' -Pkotlin.compiler.execution.strategy=in-process --no-configuration-cache --console=plain`。日志位于 `build/light-ui-native-2026-11-04/`，均为本次执行。

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `:composeApp:desktopTest --tests '*UiVisualContractTest' --tests '*LyricsMotionTest'` | Linux / 原生 `ImageComposeScene` | 首轮 26 项中 23 项通过、3 项失败；发现桌面教程播放条、宽屏快捷入口缺失，以及测试默认键盘输入跳过动画。失败日志保留为 `native-tests-1.log`。 |
| 同上两类完整回归 | Linux / 原生 `ImageComposeScene` | 26 项通过，无失败/跳过；`native-final-2.log`。一次工具会话中断的 `native-final-verification.log` 未完成，不计通过；改用临时 systemd 用户服务运行并保存独立退出码。 |
| `:composeApp:assembleDebug :composeApp:assembleDebugAndroidTest :composeApp:packageLinuxPortable` | Linux / JDK17 / Android SDK | 构建通过，`native-build.log`、`native-final-2.log`。产物未安装用户设备。 |
| 独立复核修正后的 `UiVisualContractTest` 定向 4 项 | Linux / 原生离屏 | 通过：主页面/设置全局返回矩阵、响应式图集、连续反向与键盘即时、短横屏内容空间/播放/导航检查；`verification/review-fixes-4.xml`。 |
| 修正后 `assembleDebug assembleDebugAndroidTest packageLinuxPortable` | Linux | 通过，`native-review-fixes.log`，退出码 0；Android Debug APK、测试 APK、Linux Release 便携包均成功构建。 |
| `shortLandscapeRetainsCollectionViewportAndTransport` 扩展复测 | Linux / 原生离屏 | 1 项通过，内部覆盖曲库/设置/助手/一起听/工具 × 正常/1.5 字号，共 10 组真实壳层场景；两种字号均实际滚动并打开歌单。`native-short-shell.log`、`verification/short-shell.xml`，退出码 0。 |
| 独立视觉复核及限定复评 | 原生截图 + 源码 + 测试报告 | 初评 F1 短横屏、F2 全局返回；修正后审阅者逐项判定 **resolved**，`disposition: ship`。结论只覆盖这两项修正，不扩大为设备/全部业务认证。 |
| 文档与差异检查 | 本地 | YAML / JSON、源码 token 引用、文档链接及本次源码差异检查通过（`git -c core.whitespace=blank-at-eol,blank-at-eof,space-before-tab,cr-at-eol diff --check`，保留仓库原有 CRLF；默认检查曾将 CRLF 报为尾部空白）。 |

截图使用测试曲库及模拟服务，由 Compose 原生渲染，不读取真实用户音乐、不调用付费模型。不以离屏渲染时间推断真机帧率。

### 本地交付物

均位于忽略的 [delivery 目录](../../build/light-ui-native-2026-11-04/delivery/)，不是线上发布：

- [浅色原生图集](../../build/light-ui-native-2026-11-04/delivery/gallery.html)：110 张测试截图，包含双端页面、短横屏、大字、空态；数量不是独立路由数。
- [截图 ZIP](../../build/light-ui-native-2026-11-04/delivery/Resonance-native-light-screenshots.zip)。
- [Android Debug APK](../../build/light-ui-native-2026-11-04/delivery/Resonance-0.6.5-debug.apk)：调试签名，不能覆盖签名不同的正式版；不要为试用删除原数据。
- [Linux x86_64 便携包](../../build/light-ui-native-2026-11-04/delivery/Resonance-0.6.5-linux-x86_64.tar.gz)：包含运行时，系统需提供 FFmpeg，独立试用目录命令见 [README](../../build/light-ui-native-2026-11-04/delivery/README.txt)。
- [SHA256SUMS](../../build/light-ui-native-2026-11-04/delivery/SHA256SUMS.txt)：从本次实际交付文件生成并重新校验。
- [独立审阅与复评](../../build/light-ui-native-2026-11-04/delivery/REVIEW.md)、构建日志和测试 XML 存于本次 build 目录。

## 限制与后续

Android 真机系统栏/IME/返回手势、真实屏幕刷新率，以及 Windows 运行时未在本轮执行。既有联网、同步与付费服务流程保持原实现，离屏视觉测试不代表这些外部服务端到端通过。原型的 78 项是页面/关键状态映射，不是 78 个新增原生路由，详见[原生映射](../../design-system/resonance/proposals/2026-11-04-light-glass/NATIVE-IMPLEMENTATION.md)。

## 同步更新的文档

- `docs/README.md` 最新交付与变更索引。
- `docs/DEVELOPMENT.md` 第 11 节当前原生布局、设置导航、材质与转场。
- `DESIGN.md` 与 `.impeccable/design.json`：按本次原生实现合并更新设计规范。
- 方案目录新增 `NATIVE-IMPLEMENTATION.md`，保留 HTML 提案和原截图作为决策记录。
