# 2026-10-26 — 引入自然语言语义找歌、曲库智能管家、实时伴奏消除、音频特征分析与音乐品味画像

- 修改日期：2026-10-26（Asia/Shanghai）
- 类型：功能 / AI 与音频增强
- 适用版本：0.5.0（工作区基线，发布时升至 0.6.0）
- 影响平台：共享 (commonMain) / JVM (jvmMain) / Android (androidMain) / Windows (desktopMain)
- 关联记录或需求：用户明确要求重点开发 AI 功能，包含自然语言语义搜歌、本地曲库智能管家、伴奏与人声分离、本地音频特征提取、音乐品味雷达与听歌性格分析、一键生成精美歌单分享卡片，并在完成后构建发布至 GitHub 与官网。

## 需求或问题

在现有的多模型助手与工具体系基础上，AI 能力主要集中在独立的助手聊天界面中，听歌日常流程与本地曲库管理缺少场景化智能协同：
1. **搜歌检索**：原有搜索仅支持关键词与首字母模糊匹配，当用户只记得歌曲氛围、情绪、节拍速度或场景（如“雨天听的慢歌”、“140 BPM 跑步燃歌”）时无法命中。
2. **曲库杂乱**：用户本地歌曲常存在网络下载垃圾前缀（`kw_`、`320k_`、`【无损音质】`）、标题混杂歌手名或未知专辑，缺乏成体系的一键体检、标签清洗与多版本智能去重。
3. **伴奏与人声处理**：缺乏端侧低延迟、轻量化的人声消除伴奏（K歌）模式。
4. **音频特征提取**：曲库缺乏纯本地离线 BPM 节奏与能量感知能力。
5. **用户画像与社交分享**：用户曲库缺乏深度品味提炼，缺少可视化品味雷达图与精美海报分享能力。

## 原因与依据

1. **实时伴奏消除（Karaoke Filter）**：在立体声音乐中，人声主要录制在正中心（Mid 通道）。采用立体声中置信号提取，并结合单极点一阶 IIR 低通滤波器（截止频率 160 Hz）保护鼓点与贝斯低音，将中心人声抵消（$L' = L - v \cdot M_{vocal}$，$R' = R - v \cdot M_{vocal}$），可以在极低 CPU 负载下实现毫秒级实时伴奏切换，且不增加安装包体积。
2. **纯本地音频特征提取 (AudioFeatureAnalyzer)**：利用短时能量（RMS）结合起音包络（Onset Detection Function）与自相关（Autocorrelation），快速计算整曲 BPM（55~210 BPM）与能量等级（10~99），并将结果持久化在磁盘缓存，为后续运动、助眠等节奏筛选提供坚实基础。
3. **语义搜索意图解析 (SemanticSearchIntentParser)**：离线解析用户自然语言中的场景（雨天、跑步、深夜等）、情绪（治愈、伤感、燃等）、曲风（民谣、摇滚、纯乐等）与 BPM 意图，打分召回本地歌曲与在线歌源，输出带有生动理由的匹配结果。
4. **曲库智能体检 (SmartLibraryAuditEngine)**：自动匹配垃圾正则、拆解混入标题的歌手字段、规范未知专辑，并基于标准化元数据识别同名不同版本（优先推荐保留无损 FLAC、收藏歌曲与完整封面曲目），生成安全预览草稿。
5. **六维音乐品味雷达 (TasteAnalyticsEngine & TasteRadarChart)**：基于收藏比例、高频歌手、情绪与曲风标签，计算流行敏锐度、复古情怀、小众探索度、情绪共鸣度、节奏活力感、流派多样性，Canvas 绘制原生动态雷达，并由性格规则提炼听歌人格（如“光影留声机”、“深夜白日梦想家”）。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| `composeApp/.../audio/PcmGainProcessor.kt` | `PcmGainProcessor` | 增加 `vocalSuppression` 强度参数与 160Hz 低通低音保留滤波器，支持实时伴奏人声消除 |
| `composeApp/.../audio/AudioFeatureAnalyzer.kt` | `AudioFeatureAnalyzer` | 实现 PCM 快速降采样、短时能量、起音通量与自相关 BPM 检测，输出 BPM 与 Energy |
| `composeApp/.../audio/AudioFeatureStore.kt` | `AudioFeatureStore` | 磁盘原子 JSON 持久化与内存并发缓存歌曲音频特征 |
| `composeApp/.../model/SemanticSearchModels.kt` | `SemanticSearchQueryIntent`, `SemanticTrackMatch` | 定义语义意图与歌曲匹配结果数据结构 |
| `composeApp/.../model/SemanticSearchIntentParser.kt` | `SemanticSearchIntentParser` | 解析自然语言意图中的场景、情绪、曲风、BPM 区间与核心关键词 |
| `composeApp/.../model/SmartLibraryModels.kt` | `CleanTagDraft`, `DuplicateGroup`, `SmartLibraryAuditReport` | 定义曲库体检报告与标签清洗草稿模型 |
| `composeApp/.../model/SmartLibraryAuditEngine.kt` | `SmartLibraryAuditEngine` | 实现前缀垃圾清洗、歌手分离、未知专辑规范化与多版本音质去重打分 |
| `composeApp/.../model/MusicTasteModels.kt` | `TasteDimensions`, `TastePersona` | 定义六维品味指标与听歌性格画像模型 |
| `composeApp/.../model/TasteAnalyticsEngine.kt` | `TasteAnalyticsEngine` | 分析用户曲库与偏好，计算六维品味得分并分配个性化听歌人格评语 |
| `composeApp/.../ui/SemanticSongSearch.kt` | `SemanticSongSearch` | 搜索栏 AI 语义找歌视图，展示意图胶囊与带高亮匹配理由的歌曲卡片 |
| `composeApp/.../ui/SmartManagerScreen.kt` | `SmartManagerScreen` | 曲库智能管家页面，包含体检概览、标签清洗建议列表与版本去重释放 |
| `composeApp/.../ui/TasteRadarScreen.kt` | `TasteRadarScreen`, `TasteRadarChart` | 音乐品味画像页面，Canvas 绘制动态六角雷达图与性格细则 |
| `composeApp/.../ui/ShareCardDialog.kt` | `ShareCardDialog` | 高质感极光暗夜分享卡片对话框，支持歌单海报与品味画像海报生成及复制 |
| `composeApp/.../platform/PlatformServices.kt` | `setVocalSuppression`, `isVocalSuppressionEnabled` | 跨平台服务增加伴奏模式状态流与控制接口 |
| `composeApp/.../desktopMain/.../DesktopPcmPlayer.kt` | `DesktopPcmPlayer` | Windows 端 PCM 解码管线接入 `vocalSuppression` |
| `composeApp/.../desktopMain/.../DesktopPlatformServices.kt` | `DesktopPlatformServices` | 桌面平台服务接通伴奏模式状态 |
| `composeApp/.../androidMain/.../PreampAudioProcessor.kt` | `PreampAudioProcessor` | Android 端 Media3 AudioProcessor 接入伴奏消音处理 |
| `composeApp/.../androidMain/.../PlaybackService.kt` | `PlaybackService` | Android 原生后台播放服务接通伴奏模式广播与设置 |
| `composeApp/.../androidMain/.../AndroidPlatformServices.kt` | `AndroidPlatformServices` | Android 平台服务暴露伴奏模式状态 |
| `composeApp/.../ui/LibraryShell.kt` | `DiscoverScreen`, `DestinationContent` | 搜索栏增加「AI 语义」Tab，主导航路由接入智能管家与品味画像，挂载全局分享对话框 |
| `composeApp/.../ui/ToolsScreens.kt` | `ProfileScreen` | 工具箱添加「曲库智能管家」与「音乐品味画像」入口 |
| `composeApp/.../ui/NowPlayingOverlay.kt` | `DropdownMenu` | 完整播放器下拉菜单新增「伴奏模式 (人声消除)」开关 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 搜索歌曲 | 只能按准确歌名、歌手搜索，输入“适合雨天的歌”或“140 BPM”无结果 | 可切至「AI 语义」Tab，输入一句话精准召回曲库与歌源，并呈现智能匹配理由 |
| 本地歌曲标签脏乱 | 需手动逐首编辑，无法批量识别前缀垃圾和多版本重复 | 进入「曲库智能管家」，一键全库体检，可勾选批量规范化标签，并提示保留最佳音质版本 |
| 播放伴奏/K歌 | 无法消弱人声，只能依赖外部伴奏文件 | 播放菜单一键开启「伴奏模式」，实时中置人声消除并保留低频律动 |
| 查看音乐品味与分享 | 无专属品味雷达，无法直观生成分享图 | 进入「音乐品味画像」查看六维雷达与性格标签，并可一键生成精美暗夜海报分享 |

## 平台与数据影响

- **跨平台共享**：新增模块均使用纯 Kotlin / Compose Multiplatform，双端逻辑无缝统一。
- **存储与格式兼容**：`AudioFeatures` 独立持久化至 `audio-features.json`，不改动 `Track` 与 `PlainSyncPackage` 格式；曲库标签规范化修改沿用原生曲库更新机制。
- **音频安全边界**：去重清理严格遵循已有删除机制，需用户在体检草稿中手动确认，不擅自删除任何物理文件。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat :composeApp:desktopTest` | Windows 11, JDK 17 | 通过（覆盖 PcmGainProcessor、AudioFeatureAnalyzer、SemanticSearchIntentParser、TasteAnalyticsEngine、SmartLibraryAuditEngine 及全部历史回归，15 个任务全绿） |
| `.\gradlew.bat :composeApp:compileKotlinDesktop :composeApp:compileTestKotlinDesktop` | Windows 11, JDK 17 | 通过（0 错误，warnings 已妥善处理） |
| `.\gradlew.bat :composeApp:assembleRelease :composeApp:packageReleaseExe` | Windows 11, JDK 17, Android SDK 36 | 通过（成功构建双端 Release：`Resonance-0.6.0-release.apk` 159.13 MiB 与 `Resonance-0.6.0.exe` 306.41 MiB） |
| `powershell.exe -File scripts\release-manifest.ps1 -Version 0.6.0` | Windows 11 | 通过（生成 `release/0.6.0/` 产物及 `SHA256SUMS.txt`，自动回填 `RELEASE_NOTES-0.6.0.md`） |

## 限制与后续

- 伴奏消除算法基于立体声中置相位抵消，对于单声道录音或人声严重偏离正中的特殊立体声录制效果会相应减弱。
- 离线 BPM 分析对复杂变速交响乐采用平均节奏估计。

## 同步更新的文档

- `docs/README.md`：新增本次 2026-10-26 变更记录索引。
- `docs/DEVELOPMENT.md`：更新第 1 节能力表、第 11 节 UI 章节与第 13 节 AI 工具能力说明。
