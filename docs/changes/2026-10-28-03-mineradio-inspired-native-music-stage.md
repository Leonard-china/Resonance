# 2026-10-28 — 参考 Mineradio 重塑原生音乐舞台、歌词与歌单界面

- 修改日期：2026-10-28（Asia/Shanghai）
- 类型：UI 功能 / 重构
- 适用版本：`0.6.3 / 37`，不升版
- 影响平台：共享 Compose UI / Android / Windows
- 关联需求：用户指定 [Mineradio-paused](https://github.com/XxHuberrr/Mineradio-paused) 作为播放器界面参考，并撤回先前的网盘链接。

## 需求或问题

用户希望 Resonance 界面采用 Mineradio 的沉浸式音乐空间方向。现有 UI 仍以青绿台面、小字号歌词和普通歌单网格为主，桌面播放控制与歌词挤在右侧栏，缺少独立的舞台布局。

## 原因与依据

阅读参考仓库 `d43de565acabfdc1a9c9820a27e81a98ccbebcef` 的 README、`public/css/index.css` 与 `docs/3D_PLAYLIST_SHELF_MEMORY.md`，查看仓库的 `cinema-beat-smoke.png`（该图是开场画面）。依据其深色空间、歌词舞台、黑玻璃控件和透视歌单设计，在本项目独立实现 Compose 版本。没有将开场图当作完整播放器截图，也没有运行参考软件来宣称像素级还原。

参考源码放在忽略的 `build/reference-mineradio/`，不进入应用。没有复制其 JS/CSS、Logo、品牌或图片资源，没有引入 Electron/WebView。使用本地 `frontend-design` 与 `animate` 技能，沿用项目的导航、按压和减少动效规范。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [MusicStage.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/MusicStage.kt) | `MusicStageBackdrop`、`musicSleevePerspective`、`musicDock` | 缓存光场与 64 个确定性粒子，绘制舞台纵深；只在绘制阶段读取动画，使用前台/播放/减少动效条件控制。封面使用静态透视，播放台统一圆角与投影。 |
| [ResonanceTheme.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt) | `colors` | 深色改为墨蓝黑画布与分层冷灰表面，浅色保留高对比适配；继续支持青绿/冰蓝强调色。 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `NowPlayingOverlay`、`HeroSyncedLyricsPreview`、`LyricRow`、`ArtworkPane` | 正常高度的宽屏使用左封面、右歌词舞台、底部统一控制台；短横屏使用左侧可切换封面/歌词/队列、右侧固定进度与控制布局。歌词预览采用大字、原文/译文层级；封面静态倾斜。 |
| [PlaylistGrid.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistGrid.kt) | `PlaylistListTab` | 置顶推荐改为跨列主卡，保留关闭时去设置/开启时去推荐页的原回调与教程锚点；普通歌单封面增加透视。 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `DesktopSidebar`、`SidebarDestination`、`MiniPlayer` | 侧栏与舞台统一底色、选中层级和紧凑宽度；迷你播放器使用渐变深色浮层。 |
| [SoftNeumorphicButtons.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt) | `SoftHeroPlayButton` | 主播放键采用随主题反差的单色盘面，保留 48dp 最小目标、焦点、按压和真实播放回调。 |
| [styles.xml](../../composeApp/src/androidMain/res/values/styles.xml) | `AppTheme.windowBackground` | 同步 Android 初始窗口深色底为 `#0B0E14`，与共享主题一致。 |
| [UiVisualContractTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt) | `wideStageKeepsLyricsQueueAndTransportConnected`、`stageAtmosphereStopsWhenPausedBackgroundedOrReduced`、`shortLandscapeKeepsPaneTabsAndTransportInsideViewport`、截图夹具 | 检查宽屏进度回调、歌词/队列切换、队列选歌，粒子播放/暂停/后台/减少动效行为，短横屏主控制和标签边界；使用原创歌词测试文本渲染。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 正常高度桌面播放器 | 大封面 + 右侧元信息、歌词和播放控制 | 倾斜唱片封面 + 大字歌词舞台 + 底部悬浮播放台 |
| 手机与短横屏 | 原有纵向/紧凑布局 | 继续按窗口及字号选择布局，应用统一舞台配色、光场与封面材质 |
| 歌单列表 | 推荐与普通歌单同尺寸卡片 | 推荐跨列主卡，普通歌单保留顺序及点击行为并增加静态景深 |
| 主播放键 | 青绿渐变盘面 | 随主题反差的单色盘面，语义与输入回调保留 |

## 平台与数据影响

两端共享原生 Compose 实现。没有更改音频引擎、任务竞争保护、配置写入队列、音量处理、听歌统计、持久化字段或歌单包格式。没有删除/写回音频、增加网络请求、权限、模型调用或凭据。主题偏好和减少动效沿用原键，旧数据无需迁移。

粒子属于装饰性缓慢漂移，未接音频信号，不宣称真实频谱、节拍同步、Three.js 粒子重建或 3D 摄像机。已有波浪进度条继续作为可交互进度显示。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat :composeApp:compileKotlinDesktop :composeApp:compileDebugKotlinAndroid --no-configuration-cache --console=plain` | Windows / JDK 17 | 初轮两端编译通过，2m37s；最终源码由下行命令重新编译验证。 |
| `.\gradlew.bat :composeApp:desktopTest --tests 'com.resonance.player.ui.UiVisualContractTest' :composeApp:lintRelease :composeApp:assembleDebug :composeApp:compileDebugAndroidTestKotlin --no-configuration-cache --console=plain` | Windows / JDK 17 / Android SDK 36 | 最终通过，2m23s；9 项原生 UI 测试，0 失败 / 错误 / 跳过；Release Lint 为 0 错误、106 警告；Debug APK 与 Android 设备测试源码编译完成。日志：忽略的 `build/mineradio-delivery-verification.log`。 |
| 共享 Compose 截图检查 | `ImageComposeScene`，隔离夹具与原创歌词文本 | 生成 50 张：五页 × 深浅主题 × 手机、桌面、手机 150% 字号、短横屏、短横屏 150% 字号。目视检查桌面/手机深色播放器与曲库、浅色播放器/大字号曲库/设置、深色桌面助手、短横屏及横屏大字号。未把未逐张查看的图片写成全部人工验收。 |
| Android SDK 的 `adb devices` | 本机 platform-tools | 环境受阻：设备列表为空，未运行 instrumentation、真机触控、系统栏与帧率检查。 |
| Markdown 本地链接检查 | PowerShell，索引/开发说明/本记录 | 176 个文件链接存在；`git diff --check` 通过。 |

调试产物：[Resonance-0.6.3-debug.apk](../../composeApp/build/outputs/apk/debug/Resonance-0.6.3-debug.apk)，174,984,042 字节。本次产物实际 SHA-256：`03F396BAA0380D02556E848EFF441097DA444108F60952E21BC7A00378DAEE4B`。这是并行安装用的 Debug 包，未构建新的 Windows 安装包、签名 Release APK，也没有发布到 GitHub 或官网。

## 限制与后续

- 本次是将参考的空间、歌词与封面设计转译到原生播放器，未移植其可编辑粒子预设、Wallpaper Engine、摄像头手势或 3D 歌单管理器。
- 原生离屏渲染不等同于 Android 真机系统栏、触控、屏幕阅读器或帧率/能耗验证。
- 本次未运行全量 `allTests`、真实音源/账号播放或 Windows 安装包运行检查；播放引擎、存储与转换代码没有修改。布局和回调由上述定点 UI 回归验证。

## 同步更新的文档

- [文档入口](../README.md)：新增本次独立记录索引。
- [开发说明](../DEVELOPMENT.md)：第 11 节更新舞台布局、主题、播放台、歌词和推荐主卡说明。
- 协作规则不变。
