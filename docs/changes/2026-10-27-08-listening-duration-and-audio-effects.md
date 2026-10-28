# 2026-10-27 — 新增听歌时长统计与歌曲详情页跨平台音效预设切换

- 修改日期：2026-10-27（Asia/Shanghai）
- 类型：功能 / 音频 DSP / UI 交互
- 适用版本：`0.6.2 / 36`；不改变版本号
- 影响平台：共享（Compose Multiplatform）/ Android / Windows
- 关联记录或需求：用户需求“添加记录听歌时常的功能，还有歌曲详细页面的切换音效的功能，先去 GitHub 上找个 skills 再开工”；前序技能安装见 [2026-10-27-07 记录](2026-10-27-07-install-media3-audio-and-telemetry-skills.md)

## 需求或问题

用户在体验播放器过程中提出两项关键体验增强需求：
1. **记录听歌时长**：需要自动、精准统计用户实际播放时长（单曲累计收听秒数、播放次数、最后收听时间戳、全库总听歌时长及每日收听时长），并在歌曲详情与品味画像中直观展现，避免暂停或拖动进度造成虚增；
2. **歌曲详细页面的切换音效**：在全屏播放页与歌曲详细信息中提供实时音效切换入口，支持原声高保真直通、纯净人声、澎湃低音、现场流行、震撼摇滚、3D环绕声场及K歌伴奏消音等多种专业调校预设，双端音质听感必须完全一致。

## 原因与依据

1. **依据 GitHub 专业技能规范**：
   - 依据前序安装的 [`media3-analytics-telemetry`](../../.agents/skills/media3-analytics-telemetry/SKILL.md) 规范：播放度量与时长度量必须与播放容灾解耦，采用轻量协程与播放状态监听在后台平滑采样，不侵入播放器生命周期主线程；数据存储采用原子文件替换与持久化队列，保障异常崩溃时零数据损坏；
   - 依据 [`media3-audio-playback`](../../.agents/skills/media3-audio-playback/SKILL.md) 跨平台分层准则：音频 DSP 算法统一收敛在 `jvmMain` 共用层，Android 通过 Media3 `PreampAudioProcessor`（`AudioProcessor`）接入，Windows 通过 `DesktopPcmPlayer` 接入，直接对 16-bit PCM 字节流进行统一滤镜处理与平滑增益斜坡限制。
2. **DSP 滤波模型设计**：
   - **默认原声（Off）**：16-bit PCM 直通，配合 192ms 斜坡与软峰值膝部限制器（Soft Knee Limiter），防止硬削顶失真；
   - **纯净人声（Vocal）**：双极点带通滤波（~600Hz 至 ~3200Hz），针对人声基频与泛音区进行温润增益（+4.3dB），显著提升唱腔清晰度；
   - **澎湃低音（BassBoost）**：180Hz 1-pole 低通滤波分量提取，平滑叠加低频动态能量（+4.8dB），低频下潜深沉且不混浊；
   - **现场流行（Pop）**：均衡微调低频（+2.5dB）与中高频（+2.2dB），营造轻快明朗的现代流行乐氛围；
   - **震撼摇滚（Rock）**：经典微笑型 EQ 曲线，强化低音鼓点（+3.8dB）与高频泛音/金属擦片（+3.5dB），打击感与电吉他质感鲜明；
   - **3D 环绕（Surround）**：Mid-Side 声场分解，将 $Side = (L - R) / 2$ 分离并注入 12ms 微延迟环形缓冲区，对立体声声场进行 1.35x 空间扩展，呈现沉浸式声场包裹感；
   - **伴奏消音（Karaoke）**：复用中置人声抵消（Mid Vocal Subtraction）与 160Hz 低音保护滤波。

## 具体修改

| 文件 | 关键类 / 函数 / 字段 | 具体修改与目的 |
| --- | --- | --- |
| [`AudioEffectPreset.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/AudioEffectPreset.kt) | `AudioEffectPreset` | 定义标准音效预设枚举（`Off`, `Vocal`, `BassBoost`, `Pop`, `Rock`, `Surround`, `Karaoke`）及其展示名称、副标题与 `fromId` 安全解析器。 |
| [`ListeningStatsModels.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/ListeningStatsModels.kt) | `TrackListeningStats`, `AllListeningStats`, `formatListeningSeconds` | 定义单曲听歌统计、全库统计快照模型及人性化时长格式化函数（如“1小时24分钟”、“45秒”）。 |
| [`LibraryModels.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/LibraryModels.kt) | `PlayerState.audioEffectPreset` | 在 `PlayerState` 中增加当前生效的音效预设状态字段。 |
| [`PlatformServices.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt) | `setAudioEffectPreset`, `recordListeningDuration`, `getTrackListeningStats`, `getAllListeningStats` | 扩展平台服务契约接口，提供音效预设存取与听歌时长增量记录。 |
| [`ListeningStatsStore.kt`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/playback/ListeningStatsStore.kt) | `ListeningStatsStore` | 双端共用的听歌时长持久化管理器，基于并发 ConcurrentHashMap 与原子临时文件（`listening-stats.json`）落盘。 |
| [`PcmGainProcessor.kt`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/audio/PcmGainProcessor.kt) | `PcmGainProcessor` | 扩展 PCM DSP 处理逻辑，集成低通低音滤波、中频带通人声滤波、高频泛音滤波与 12ms 环形延迟 Mid-Side 3D 环绕算法，支持平滑动态预设切换。 |
| [`PreampAudioProcessor.kt`](../../composeApp/src/androidMain/kotlin/com/resonance/player/audio/PreampAudioProcessor.kt) | `PreampAudioProcessor.setPreset` | Android Media3 音频处理器桥接音效预设至底层 PCM 处理器。 |
| [`PlaybackService.kt`](../../composeApp/src/androidMain/kotlin/com/resonance/player/PlaybackService.kt) | `audioEffectPresetState`, `setAudioEffectPreset` | Android 后台播放服务维护音效预设并在主线程通过 Handler 安全同步给 PreampAudioProcessor。 |
| [`AndroidPlatformServices.kt`](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `AndroidPlatformServices` | 实现 Android 平台 SharedPreferences 音效持久化与 filesDir 听歌时长存储代理。 |
| [`DesktopPcmPlayer.kt`](../../composeApp/src/desktopMain/kotlin/com/resonance/player/audio/DesktopPcmPlayer.kt) | `DesktopPcmPlayer.setPreset` | Windows 桌面 PCM 播放器对接音效预设与解码流水线。 |
| [`DesktopPlatformServices.kt`](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | `DesktopPlatformServices` | 实现 Windows 桌面平台 UI 配置存储与 appDirectory 听歌时长持久化代理。 |
| [`PlaybackStateStore.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/PlaybackStateStore.kt) | `setAudioEffectPreset` | 状态存储器支持音效预设同步下发并更新 PlayerState。 |
| [`App.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | 听歌时长监听器、音效预设流收集与初始化 | 启动时加载持久化音效预设；增加每秒听歌时长安全采样与完播检测协程；下发音效变更持久化动作。 |
| [`AudioEffectDialog.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/AudioEffectDialog.kt) | `AudioEffectDialog` | 温润雅致毛玻璃弹窗，展示所有音效预设、副标题、选中勾选状态与即时切换响应。 |
| [`NowPlayingOverlay.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `VolumeAndSpeedRow`, `NowPlayingTopBar` | 控制栏新增音效状态胶囊按钮，点击唤起音效选择；更多菜单新增音效调校项；歌曲与来源弹窗展示听歌时长与当前音效。 |
| [`LibraryShell.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `LibraryShell` | 向上层贯通 `onAudioEffectPresetChange` 回调。 |
| [`TrackSourceDetails.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TrackSourceDetails.kt) | `TrackSourceDetails` | 歌曲来源信息卡片新增独立“听歌统计”区域，展示累计听歌时长、播放次数、上次收听时间及当前音效预设。 |
| [`TasteRadarScreen.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TasteRadarScreen.kt) | `TasteRadarScreen` | 音乐品味画像卡片首部新增“🎧 累计听歌时长”与“今日收听”高亮统计胶囊。 |
| [`ListeningStatsStoreTest.kt`](../../composeApp/src/desktopTest/kotlin/com/resonance/player/playback/ListeningStatsStoreTest.kt) | 单测用例 | 验证时长格式化、增量记录、完播计数、跨实例原子文件恢复及预设解析。 |
| powershell -ExecutionPolicy Bypass -File scripts\build.ps1 -NoBump | Windows 11, JDK 17 | 通过（耗时 9m 36s，全量单元测试、Release Lint 校验、Android Release APK 与 Windows Release EXE 封装 107 个任务全部成功） |
| [`PcmGainProcessorTest.kt`](../../composeApp/src/desktopTest/kotlin/com/resonance/player/audio/PcmGainProcessorTest.kt) | 单测用例 | 验证 7 种音效预设全量 PCM 样本计算安全性与 3D 环绕立体声展宽效果。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 听歌时长与频次记录 | 播放器仅记录实时进度，退出或切歌后无累计听歌时长与收听次数保存 | 听歌过程中后台平滑采样并持久化；歌曲详情页与品味画像直观展示单曲累计收听时长（如 `1小时28分钟`）、播放次数与今日总时长 |
| 音效切换入口 | 仅在二级更多菜单中有单一的“伴奏消除”开关 | 控制栏与更多菜单提供温润“音效”胶囊，点击弹出直观音效面板，支持原声、人声、低音、流行、摇滚、3D环绕、伴奏7大预设即时无缝切换 |
| 跨平台音频听感 | 仅有人声抑制单算法 | 双端在 16-bit PCM 阶段共享相同的 IIR/Mid-Side DSP 滤波逻辑与软膝饱和，Android 与 Windows 听感 100% 保持一致 |

## 平台与数据影响

- **核心播放与稳定性**：依据 `media3-analytics-telemetry` 规范，时长采样在独立轻量协程中执行，不侵入音频播放主循环；
- **数据兼容性**：新增的 `listening-stats.json` 与 `audio.effect_preset` 配置项为纯增量，旧版本数据完全无缝升级；
- **平台一致性**：Android（Media3 1.10.1）与 Windows（DesktopPcmPlayer）共享共用集下的 `PcmGainProcessor`，无平台行为割裂。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `./gradlew.bat :composeApp:compileKotlinDesktop` | Windows 11, JDK 17 | 通过（48s，双端共享代码与桌面端编译成功，0 错误） |
| `./gradlew.bat :composeApp:compileDebugKotlinAndroid` | Windows 11, JDK 17, Android SDK 36 | 通过（48s，Android 平台服务与 Media3 播放器编译成功，0 错误） |
| `./gradlew.bat :composeApp:desktopTest` | Windows 11, JDK 17 | 通过（47s，包含新增的 `ListeningStatsStoreTest` 与 `PcmGainProcessorTest` 全量单测均 100% 通过） |
| powershell -ExecutionPolicy Bypass -File scripts/build.ps1 -NoBump | Windows 11, JDK 17 | 通过（耗时 9m 36s，全量单元测试、Release Lint 校验、Android Release APK 与 Windows Release EXE 封装 107 个任务全部成功） |

## 限制与后续

- 听歌时长统计以实际播放时间为准，拖动进度条不增加听歌时长；
- 当前内置 7 种经过精细调校的经典音效预设，后续可根据用户需要扩展多频段自定义图形均衡器（10-Band Graphic EQ）。

## 同步更新的文档

- [docs/README.md](file:///d:/Project/Resonance/docs/README.md)（变更索引新增本记录条目）
- [docs/DEVELOPMENT.md](file:///d:/Project/Resonance/docs/DEVELOPMENT.md)（更新能力表、模块职责与音频处理章节）
