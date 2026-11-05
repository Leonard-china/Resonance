# 2026-11-05 — 按 AM++ 源码和截图重做三端播放器、歌词及玻璃导航

- 修改日期：2026-11-05（Asia/Shanghai；实现开始于 11 月 4 日，跨日验证交付）
- 类型：UI 重构
- 适用版本：`0.6.6 / 40`；版本不变
- 影响平台：Android、Linux、Windows 的共享原生 Compose UI
- 关联需求：用户指定 [AM++](https://github.com/Zennmn/AM-plus-plus)，要求查看源码复刻，随后确认三端一起改并授权自主推进。

## 需求或问题

原界面采用矿物绿/冰蓝、整体磨砂背景、底部共用控制台与歌词焦点带。用户要求替换为 AM++ 展示的 Apple Music 界面。此次以实际源码/截图为依据；没有把已知参考重新改成其他视觉方向。

## 原因与依据

只读核对 AM++ commit `b112af969789c5d1da04255830d6700062968b95`：`EmbeddedSettingsPalette.kt` 给出浅/深色角色，`TabletPlayerComponents.kt` 和 `docs/images/tablet-dual-pane-player-open-source-blur.jpg` 给出左封面/播放控制、右歌词结构，`glass/.../LiquidBottomTabs.kt` 给出胶囊导航。AM++ 是 Apple Music 增强模块，宿主曲库等原版页面的完整实现并不在该仓库。

本项目继续以 Kotlin/Compose 独立实现这些可核对的视觉与布局，不复制 GPL 模块代码或 Apple 专有图标、封面和字体，不更改现有 MIT 许可证。Haze 磨砂沿用现有跨平台依赖；它不等于 AM++ Android 专用 Backdrop 的完整光学折射渲染，因此不宣称逐像素或着色器一比一相同。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [主题](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt) | `colors`、`ImmersivePlayerColors` | 浅色 #F2F2F7/白色、#1C1C1E 正文与 #C91D40 强调；深色 #0B0B0C/#1C1C1E/#FF6B83；播放器白色前景独立于主壳主题，保留冰蓝/天气偏好。 |
| [背景](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/FrostedBackdrop.kt) | `FrostedBackdrop` | 主壳改为稳定平整画布，播放器保留真实封面透色，64dp 模糊和受控深色遮罩。 |
| [播放页](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `NowPlayingOverlay`、`PaneSwitcher`、`PlayerVolume`、`LyricRow` | 宽屏左约 46% 为封面和全部播放控制，右约 54% 为歌词/队列；窄屏上下排列；移除跨页底部控制台、改白色无底播放符号、底部图标分栏及平直音量。首次宽屏直接初始化歌词，避免旧封面页叠影。 |
| [进度控件](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt) | `SoftRefinedPlaybackScrubber(flat)` | 播放器直线样式复用既有拖动预览/松手提交，旧波形调用可继续使用默认样式；平直模式不运行播放波形时钟。 |
| [主壳](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `BottomNavigationBar`、`MiniPlayer`、`DesktopSidebar` | 手机迷你播放器和导航分成独立胶囊，选中背景连续平移；桌面保留自适应 rail/侧栏与固定播放条并统一表面。 |
| [工具与设置](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) | `SettingRow`、`SettingsScreen` | 参考白色分组行、玫红图标和灰色正文，保留原设置分组、保存、登录、测试及返回回调。 |
| [原生回归](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt) | 拖动与图集 | 直线/原波形均检查拖动期间不重复 seek；已有原生导航、短横屏/大字、播放器/歌词/队列、浅深主题图集重新执行。 |

歌词保持真实时间轴及跟随算法，当前句用清晰加粗白字、上下句按距离轻微模糊；手动浏览和减少动态时取消上下句模糊。焦点不改变字重/字号/行高；当前与非当前行使用同样的版式，原文/译文保持组合。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 桌面展开播放器 | 上方内容 + 跨栏控制台 | 左侧封面/曲名/进度/播放/音量，右侧歌词/队列 |
| 迷你播放与主导航 | 同一个合并底座 | 两个独立胶囊表面 |
| 播放按钮/进度 | 实心圆钮、波浪进度 | 白色无底播放符号、平直进度 |
| 浅深主题 | 蓝灰/矿物绿 | 参考灰白/黑灰/玫红角色；可选冰蓝仍有效 |
| 当前歌词 | 强调色焦点带 | 沉浸播放器清晰白字，邻近行渐弱/轻微模糊；独立普通歌词组件继续保留原焦点策略 |

## 平台与数据影响

共享原生 UI 同时影响三端，不引入 WebView 或网页，不改独立官网。保留系统安全区/IME、返回、键盘、减少动态及 48dp 交互占位。保留原播放请求代次、队列、音频转换、配置写入顺序、原子保存、同步校验和删除授权，不改变曲库/配置/同步包格式或用户凭据。

## 验证

本轮在 Linux 配置 JDK17 / SDK36，构建通用参数：`--no-daemon --max-workers=2 -Dorg.gradle.jvmargs='-Xmx3g -Dfile.encoding=UTF-8' -Pkotlin.compiler.execution.strategy=in-process --no-configuration-cache --console=plain`；最终纯桌面复测使用 2g 堆。临时日志/图片/合成数据在忽略目录 `build/am-reference-2026-11-04/`，不读取用户歌曲/签名私钥/API Key。

| 命令/操作 | 结果与实际范围 |
| --- | --- |
| 首轮编译/回归 | 初次编译发现新增 API opt-in/重复导入，已修正；首轮 UI 有一项旧文案断言失败，恢复原文。全量首轮 364 项中 353 通过、10 跳过、1 失败，为新 slider 与设置按钮同名；改为「播放音量」。失败日志/报告保留，不能冒充通过。 |
| `:composeApp:desktopTest --tests '*PlaybackSettingsDialogTest' --tests '*UiVisualContractTest.songInformationRemainsReachableFromPlayerMenu'` | **3 项通过**；音量打开不改变设置、调节/关闭/播放仍可用；菜单和歌曲详情覆盖浅深两主题。初次截图抓到过渡，改为单调时钟逐帧推进后重抓，最终图片已查看，无旧页/弹窗叠影。 |
| 最终 `:composeApp:desktopTest` | **364 项：354 通过、10 显式跳过、0 失败/错误**；5m36s，`final-desktop-tests.log`、`final-results/`、`final-test-summary.json`。包含6项新导入、转换/持久化/播放及原生 UI/歌词回归；跳过不算外部联测通过。 |
| `:composeApp:assembleRelease :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest :composeApp:lintRelease :composeApp:packageLinuxPortable` | **通过**，6m24s、140任务；`review-fixes-build.log`。Android Release/Debug/设备测试 APK、完整 Linux 便携包均本次生成；Release Lint 0 Error / 107 Warning。 |
| `apksigner verify --verbose`、`zipalign -c -v 4` | **通过**：正式 APK v2=true、1 signer，ZIP alignment successful；v1/v3/v4=false，不宣称这些签名方案已启用。第一次签名命令缺 java PATH，补齐后实际验证成功。 |
| JVM 原生图集、360帧播放器分栏/换曲检查、浅/深色、短横屏/大字 | **通过**，本次实际 `ImageComposeScene`；选定证据及修正菜单在 `.impeccable/review/am-reference-2026-11-04/` 和本地交付图集。fixture 不是用户音乐。离屏时间不作为真机性能结论。 |
| 独立视觉终审 | 初审 `fix`：要求完整沉浸暗色 Material3 容器以避免浅底白字，并同步规范；已改整套 `darkColorScheme` 和全部 `surfaceContainer`、更新设计文档，最终复评 **`ship`**：原两项必修均 `resolved`，原六图未见本批修复引入的可见回退；仅关闭这两项，不扩大为设备或全业务认证。初审/复评存于 `.impeccable/review/am-reference-2026-11-04/REVIEW-initial.md` / `REVIEW.md`。 |
| Linux 成品启动与原生导入事件 | **通过**：真实可见窗口、原生 DropTarget 注入、入库/UI 重载、Copy 接收与源文件哈希；物理鼠标拖动在当前 Wayland 自动化会话受阻，见[导入记录](2026-11-05-01-desktop-files-and-drop-import.md)，不称手动文件管理器验收。 |
| 本机完整安装及桌面入口 | **通过**：从本次 TAR 解压，392 个文件逐一与原生验收的 app image 比对 SHA-256；原子切换 `current` 到 `0.6.6-20261105-am-ui/`，两份 desktop 文件校验通过，旧版本保留。`installation.json`。未终止旧播放器；需从托盘「退出」后重开入口使用新版。 |

本次构建中曾发现前次残留 Gradle 守护占用约4GB；确认其无已连接构建客户端后停止，当前构建独立运行，未停止用户播放器。第一次安装断言误要求 jpackage 的 `runtime/bin/java`，在切换前中止；改为验证 `libjvm.so` 并比对完整392文件后才切换，未把中止计为安装成功。

### 本地交付

[本地交付目录](../../build/am-reference-2026-11-04/delivery/) 含两包、`SHA256SUMS.txt`、`README.txt`、`gallery.html` 和已查看的原生截图；均未公开上传。哈希从本次复制的真实文件生成，`sha256sum -c SHA256SUMS.txt` 两项通过。

| 文件 | 字节 | SHA-256 |
| --- | ---: | --- |
| `Resonance-0.6.6-release.apk` | 167418724 | `f2b19f76b36fe21c70f4f38d681414143b8644afd9e02ae6e3997b13fd6965c0` |
| `Resonance-0.6.6-linux-x86_64.tar.gz` | 266703401 | `d51a38f5f565589ffe62f066e5587081f8dceedabc5d516682621db3b9c8a087` |

## 限制与后续

Android 当前无连接设备；Windows 未提供本轮原生环境。共享 JVM 原生渲染与 Android 编译不能替代这些设备验证，也不代表高刷新率性能已验收。参考中属于 Apple Music 宿主而非 AM++ 源码的页面按 Resonance 实际业务重排；不假装拥有完整宿主源码或像素级相同的着色器。未发布 GitHub/官网，不自动升版。

## 同步更新的文档

文档索引、开发说明 UI 当前基线、PRODUCT.md、DESIGN.md 及设计 token sidecar；第三方视觉参考来源。
