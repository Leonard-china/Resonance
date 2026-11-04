# 2026-11-02 — 播放加载与缓冲状态全链路实现、帧率实测及桌面可见窗口验收

- 修改日期：2026-11-02（Asia/Shanghai）
- 类型：功能 / 测试与验收
- 适用版本：0.6.5（39，`version.properties` 保持不变，不升版）
- 影响平台：共享 / Android / Windows
- 关联记录：[公网中继复测](2026-11-02-06-public-relay-native-retest.md)、[剩余质量交接](../HANDOFF-2026-11-02-REMAINING-QUALITY.md)、[原生基线](2026-11-02-01-native-validation-and-local-candidates.md)

## 需求或问题

在先前阶段，播放器处于曲目切换、音量/增益异步读取与持久化快照保存期间缺乏对 UI 加载/缓冲状态（Preparing / Buffering）的显式建模与反馈：
1. `PlayerState` 仅有 `isPlaying`，在切歌未就绪前界面无法展现加载进度，容易造成无响应错觉；
2. 缺乏用户意图与原生实际输出的生命周期拆分：当用户在准备阶段快速暂停或失焦时，异步 Ready 回调容易覆盖暂停意图，导致迟到重播；
3. Windows 候选包此前仅验证了进程存活与 Responding，未曾利用系统视觉工具验收真实桌面窗口的渲染、尺寸与交互切换；
4. 动效回弹优化后尚未有最新实测的 Choreographer 帧指标证据。

## 原因与依据

1. **状态拆解**：准备曲目、计算增益与音量、持久化快照属于准备任务（`isPreparing`）；底层解码器首帧填充或网络取流等待属于原生缓冲（`isBuffering`）；两者并集即为全局加载状态（`isLoading = isPreparing || isBuffering`）。
2. **防竞争与迟到重播**：在 `PlaybackStateStore` 中引入请求代次 `generation`（每次切歌递增），并将用户播放意图 `userPlayIntent` 与底层播放状态分离。在 `prepare` 完成后，若用户意图已变更为暂停，则不执行自动播放并强制向服务同步 `setPlaying(false)`。
3. **平台原生映射**：
   - Windows 桌面：`DesktopPcmPlayer` 的 `DesktopPlaybackEvent.Kind` 扩展 `Buffering`，在 `play()` 启动时发射 `Buffering`，首帧解码（`decoded = true`）后恢复 `Playing`；
   - Android 移动端：`AndroidPlatformServices` 的 Media3 MediaController 原生监听 `onPlaybackStateChanged`（`Player.STATE_BUFFERING`）并同步发射 `isBufferingChanges`。
4. **协议与数据边界**：保持一起听房间协议和快照格式的稳定，不将新的状态字段写入外部传输协议或配置快照。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [LibraryModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/LibraryModels.kt) | `PlayerState` | 增加 `isBuffering: Boolean = false` 与 `isPreparing: Boolean = false`，计算属性 `val isLoading: Boolean get() = isPreparing \|\| isBuffering` |
| [PlatformServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt) | `PlatformServices.isBufferingChanges` | 暴露只读 `Flow<Boolean>` 缓冲状态流，平台可覆写，默认提供 `emptyFlow()` |
| [DesktopPcmPlayer.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/audio/DesktopPcmPlayer.kt) | `DesktopPlaybackEvent.Kind.Buffering` | 解码管道扩展 `Buffering` 事件：播放开始至首帧送达前置 true，首帧输出及暂停重置 false |
| [DesktopPlatformServices.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | `DesktopPlatformServices` | 实现 `isBufferingChanges`，并统一管理缓冲事件到 Flow 的分发 |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `AndroidPlatformServices` | 监听 Media3 `Player.Listener.onPlaybackStateChanged`，在 `STATE_BUFFERING` 时发射 true |
| [PlaybackStateStore.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/PlaybackStateStore.kt) | `prepareTrack` / `setUserPlayIntent` | 维护 `generation` 代次过滤旧事件；准备阶段暂停时拦截自动就绪并防重播；提供 `setBuffering` |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `App` | 收集 `services.isBufferingChanges` 并同步至 `playback.setBuffering`；UI 与按键触发统一更新 `userPlayIntent` |
| [SoftNeumorphicButtons.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt) | `SoftHeroPlayButton` / `SoftMiniPlayButton` | 新增 `isLoading: Boolean`，在加载中时呈现平滑旋转的微型进度环与“加载中”语义 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `MiniPlayer` | 状态副标题提示：“正在缓冲…” / “正在准备…”；播放按钮接入 `isLoading` |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `TrackIdentity` / `TactilePlayPauseButton` / `LyricsContent` | 标题副文本提示缓冲状态；全屏大按钮展示加载中动画；`LyricsContent` 可见性提升为 `internal` 以供设备测试访问 |
| [PlaybackStateStoreTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/playback/PlaybackStateStoreTest.kt) | `PlaybackStateStoreTest` | 新增 3 项测试：准备中暂停拦截防迟到重播、缓冲状态平滑转移、播放故障安全退出加载 |
| [UiVisualContractTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt) | `bufferingAndLoadingStatesRenderDistinctVisualFeedbackInDarkAndLight` | 新增暗色/浅色下的加载/缓冲视觉契约断言，离屏生成验证截图 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 曲目异步准备与取流缓冲 | 播放按钮静态显示，无文字反馈 | 播放按钮显示微型旋转进度指示器；MiniPlayer 及全屏播放器副文本高亮显示“正在准备…”或“正在缓冲…” |
| 准备阶段快速暂停/失焦 | 异步任务完成后强制执行 `onReady` 并重播 | 准备完成后检测到 `userPlayIntent == false`，立即放弃启动并保持暂停状态，杜绝迟到重播 |
| 切歌连续点击 | 旧曲目准备任务可能覆盖新曲目状态 | 基于代次递增机制（generation），旧请求事件直接被丢弃 |
| 播放失败或解码出错 | 状态可能挂起或无法退出加载 | `playbackFailed` 统一重置 `isPreparing` 与 `isBuffering`，安全回退到非播放状态 |

## 平台与数据影响

- **跨平台共享逻辑**：状态拆解全部在 `commonMain` 中完成，不破坏已有平台接口兼容性。
- **Android / Windows**：两端分别接入各自原生引擎（Media3 与 Java Sound/FFmpeg PCM）。
- **数据与存储边界**：快照持久化格式完全保持原样，不改变已有偏好项；不将加载/缓冲字段推入一起听网络协议。
- **权限与环境**：无新增权限需求，未触碰真实生产环境密钥或配置。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat :composeApp:desktopTest --tests '*PlaybackStateStoreTest'` | Windows / JVM 17 | **通过**，12 项全部通过（含新增的准备中暂停拦截、缓冲转移、失败退出加载） |
| `.\gradlew.bat :composeApp:desktopTest --tests '*UiVisualContractTest*'` | Windows / 原生 Compose 离屏场景 | **通过**，17 项全部通过（含新增暗/浅色加载视觉断言），生成 `buffering-feedback-*.png` |
| `.\gradlew.bat :composeApp:compileDebugKotlinAndroid` | JDK 17 / Android SDK 36 | **通过**，Android 端代码编译无报错 |
| `.\gradlew.bat :composeApp:assembleDebugAndroidTest -I build/final-20261102/isolated-android.init.gradle` | JDK 17 / 隔离构建脚本 | **通过**，2m 11s，生成 `composeApp-debug-androidTest.apk` |
| `adb shell am instrument ... MusicStageFrameMetricsTest` | MuMu 12（127.0.0.1:16384） | **通过**，完成三大场景真实采集：<br>1. `perf-player`: 535帧，p50=10.98ms, p95=17.88ms, max=324.93ms, 超deadline 2.99%；<br>2. `perf-nav-cold`: 243帧，p50=7.15ms, p95=61.53ms, max=463.89ms, 超deadline 11.1%；<br>3. `perf-nav-warm`: 276帧，p50=5.27ms, p95=29.16ms, max=161.98ms, 超deadline 5.43%（较上一版本预热 p95=71.14ms 显著下降）。 |
| `.\gradlew.bat :composeApp:createReleaseDistributable -I build/candidates-20261102-quality.init.gradle` | Windows / 隔离候选目录 | **通过**，5m 16s，生成隔离绿色安装目录 `quality-candidates-20261102-p2/main-release/app/Resonance` |
| Windows 真实桌面可见窗口交互验收 | Windows 11 / `windows-mcp` Computer-Use | **通过**，成功拉起真实原生窗口（宽920x高640，标题 "Resonance"），并通过鼠标点击与视觉截图完成“音乐库”、“音乐助手”、“工具箱”及 MiniPlayer 控件的真实切页验证，三张现场审计截图留存于 `docs/audit/2026-11-02/`。 |

## 限制与后续

- **独立物理设备与真实公网网络**：本次验证基于同机 Windows 与 MuMu 12 模拟器，虽然通过线上已部署公网中继完成 90 秒复测，但由于缺少多台独立运营商蜂窝网络的物理真机，未覆盖多端跨网、长时间（2 小时以上）压力测试及弱网边缘丢包重连。
- **用户原有运行实例**：验证期间全程保持隔离运行目录与环境变量，未关闭用户原有运行中的 Resonance 实例（PID 16408），未改动真实用户存储数据。

## 同步更新的文档

- [文档入口](../README.md)：更新本次变更记录索引及状态说明。
- [开发说明](../DEVELOPMENT.md)：更新播放状态架构、加载/缓冲反馈设计事实及核对日期。
- [优化报告](../OPTIMIZATION_REPORT.md)：同步更新加载状态与最新性能实测指标。
