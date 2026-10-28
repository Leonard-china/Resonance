# 2026-10-27 — Resonance 详细开发说明：听歌时长统计、跨平台音效调校与全域温润雅致质感

源码核对日期：2026-10-28（Asia/Shanghai）；当前工作区为 **`0.6.3 / 37`**，新增自然语言语义搜歌、本地曲库智能管家、实时伴奏消除（K歌模式）、本地音频特征提取（BPM/能量）、六维音乐品味雷达与精美歌单分享海报；验证见 [0.6.0 变更记录](changes/2026-10-26-01-ai-features-semantic-search-manager-karaoke-taste.md) 与 [v0.6.0 发布说明](RELEASE_NOTES-0.6.0.md)。各单项修改见 [文档索引](README.md)。

不同 AI 接手时先读本页相关章节与变更记录，再读必要源码。每个独立功能/修复必须新增带日期和具体内容的文档，见 [协作流程](AI-WORKFLOW.md)。

0.5.0 已发布基线完成过全量单元、Lint 及双端构建，详见 [发布记录](changes/2026-10-17-33-release-0.5.0.md)。当前版本由 version.properties 决定。

## 目录

- [范围](#scope)、[工程](#architecture)、[模块入口](#modules)
- [状态与并发](#startup)、[模型与兼容](#models)
- [扫描/转换/歌单](#library)、[播放与音量](#playback)、[歌词/翻译/补全](#lyrics)
- [歌单文件](#sync)、[存储/凭据](#storage)、[UI/封面/悬浮歌词](#ui)
- [构建与发布](#build)、[AI/截图/推荐](#assistant)、[任务定位与限制](#gaps)、[维护](#maintenance)
- [独立官网与匿名论坛](#website)
- [歌源、推荐、存储与助手增量](#music-source-update)
- [本轮用户体验与黑边处理](#user-experience-update)

<a id="scope"></a>

## 1. 范围与当前能力

播放器为 Kotlin / Compose Multiplatform 原生工程，目标 Android、Windows；播放器本身没有 Web 前端、云端音乐库或账号系统。仓库另有独立的 `website/` 官网和匿名论坛，见[第 16 节](#website)，不连接用户本机曲库。音频保存在本机；歌词、补全、天气、AI、B 站会联网。扫描/转换保留输入；主动删除、不喜欢、临时推荐清理会删除相关音频。

| 功能 | Android | Windows | 边界 |
| --- | --- | --- | --- |
| 本地库、搜索、收藏、歌单、专辑/歌手、多选 | 已接入 | 已接入 | 主导航第二项为助手；Discover 从曲库右上角进入，支持本地／在线切换；本地支持拼音首字母；工具页独立提供关于 Resonance 页面 |
| 首次启动与使用教程 | 已接入 | 已接入 | 首次选择快速图文/跟着操作/跳过；设置固定教程目录，七章说明/真实控件教学，支持暂停、跳过、继续与重新练习 |
| KGMA v3 / slot 1 转 MP3 | 已接入 | 已接入 | 进度、取消、保留源文件；支持载荷转 320k MP3 |
| KGG v5 转 MP3 | 不支持，明确提示 | 已接入 | 需要本机酷狗数据库中的匹配密钥 |
| 播放/队列/随机/循环/变速/恢复 | Media3 服务 | FFmpeg/Java Sound | 原生后台播放与快照恢复 |
| 自动音量平衡 | 单开关 | 单开关 | 全局音量、整曲分析/缓存，独立帮助页 |
| 歌词与双向翻译 | 已接入 | 已接入 | 本地 LRC/KRC、LRCLIB、显式 AI、任意目标语言和双语模式 |
| 后台悬浮歌词 | 系统 overlay（带权限引导） | 透明置顶窗口 | 灵动岛展开全屏播放器常驻触觉切换按钮；拖动、字号、锁定/点击穿透，Android 悬浮窗未授权时自动弹出引导对话框并直达系统设置 |
| 歌单文件导出/导入 | 已接入 | 已接入 | .resonance 明文 ZIP，含音频、封面、元数据和关系 |
| B 站音频导入 | 已接入 | 已接入 | 链接/BV/分 P，编辑歌名歌手专辑与封面，转码入库、专辑封面匹配/视频兜底 |
| 多模型音乐助手 | 已接入 | 已接入 | 千问、DeepSeek、豆包、自定义 HTTPS 接口，支持多会话独立隔离、消息回复模型溯源与推荐 Chip 预选 |
| 长截图歌单识别 | 本地 OCR / 云端 API | 本地 OCR / 云端 API | 内置 PP-OCRv5；先生成勾选／编辑预览，用户确认后导入；不自动切换收费 API |
| 每日推荐 | 前台/后台可选 | 前台/托盘后台可选 | 自定义间隔/数量；临时曲目清理；收藏/入自建歌单保留 |
| 元数据/封面/歌词补全 | 已接入 | 已接入 | 公共来源优先，显式 AI 兜底；歌曲页无常驻缺失计数 |
| 歌曲信息编辑 | 曲库覆盖值 | 原有入口可写标签 | B 站新 MP3 两端都真实写入标签与封面 |
| 设备/LAN/二维码同步、酷狗歌单链接 | 已删除 | 已删除 | 不删除历史歌单；酷狗补全和 KGG 密钥读取仍在 |
| 睡眠定时 | 原生播放服务 | 桌面运行作用域 | 15／30／45／60 分钟或曲终停止；后台继续，完全退出不恢复 |
| 歌词整体偏移/原文件写回 | 偏移已接入，写回有限 | 偏移已接入，写回有限 | 单曲偏移独立持久化，不宣称完整原文件写回 UI |
| 自然语言语义找歌 | 已接入 | 已接入 | 搜索栏 AI 语义模式，场景、情绪、曲风与 BPM 意图匹配，带高亮理由 |
| 本地曲库智能管家 | 已接入 | 已接入 | 一键体检、垃圾字符清理、标题歌手分离、未知专辑规范与多版本音质去重 |
| 实时伴奏消除与音效预设 | 已接入 | 已接入 | 统一 PCM DSP（原声/人声/低音/流行/摇滚/3D环绕/伴奏消音），全屏与歌曲详情一键切换 |
| 听歌时长与播放频次统计 | 已接入 | 已接入 | 单曲与全库累计听歌时长、完播次数、最后收听记录，原子持久化并在详情与画像展示 |
| 本地音频特征提取 | 已接入 | 已接入 | 毫秒级提取整曲 BPM 与能量指标，独立持久化缓存 |
| 音乐品味画像与分享海报 | 已接入 | 已接入 | 六维雷达动态渲染、听歌性格评语、时长统计胶囊、精美分享卡片与文案复制 |

<a id="architecture"></a>

## 2. 技术栈与工程

精确依赖以 [版本目录](../gradle/libs.versions.toml)、[Gradle 配置](../composeApp/build.gradle.kts) 为准：Kotlin 2.3.20、Compose 1.11.0、AGP 8.9.2、Gradle Wrapper 8.11.1、Lint 9.4.1；Android minSdk 26 / compile/target 36，Build Tools 36.0.0。

Media3 1.10.1、WorkManager 2.10.1、FFmpegKit audio 8.1.7、Smart Exception 0.2.1、jaudiotagger 3.0.1、sqlite-jdbc 3.47.1.0、org.json 20250517、JNA 5.17.0、Haze 2.0.0-alpha02。ZXing 与 jdk.httpserver 已随 LAN 移除。sqlite-jdbc 只读取酷狗密钥，不是曲库数据库。

| 源集（下文简写） | 作用 |
| --- | --- |
| commonMain（C） | UI、状态、模型、平台契约、KGMA 原语 |
| jvmMain（J） | 两端共用的 AI、B 站、歌词、补全、JSON/包/PCM |
| androidMain（A） | MediaStore/SAF、Media3、FFmpegKit、WorkManager、系统悬浮窗 |
| desktopMain（D） | Windows 文件、FFmpeg/JavaSound、JNA、托盘、歌词窗口 |
| commonTest / desktopTest / androidInstrumentedTest | 共享、桌面 JVM、Android 设备回归 |

Gradle 只有 `:composeApp` 一个应用模块。源码简写 C/J/A/D 均指 `composeApp/src/<源集>/kotlin/com/resonance/player/`。`scripts` 管理构建/显式升版/产物，`docs` 管理当前基线与逐次记录。`website/` 为独立的 Flask / SQLite / HTML / CSS / JS 网站，不加入 Gradle 构建。

<a id="modules"></a>

## 3. 模块入口与职责

| 入口 | 关键符号/职责 |
| --- | --- |
| [C/App.kt](../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | 启动装配、曲库/歌单/播放与导航、持久队列、前台推荐、翻译/悬浮接线、重载 |
| [C/platform/PlatformServices.kt](../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt) | 曲库、播放、权限、导入/导出、配置；musicAssistant、importProgress/cancelImport、图片选择、后台配置、悬浮歌词 |
| [C/platform/MusicAssistantServices.kt](../composeApp/src/commonMain/kotlin/com/resonance/player/platform/MusicAssistantServices.kt) | 配置/连接测试、聊天、视频、翻译缓存、推荐/拒绝/原因、取消、后台快照 |
| C/model/LibraryModels.kt、MusicFeatureModels.kt | Track/Playlist/Lyrics、报告、模型连接、设置、进度、Token、推荐 |
| [C/playback/PlaybackStateStore.kt](../composeApp/src/commonMain/kotlin/com/resonance/player/playback/PlaybackStateStore.kt) | 激活代次、准备任务取消、用户设置版本、全局音量/自动增益 |
| [C/data/PersistenceQueue.kt](../composeApp/src/commonMain/kotlin/com/resonance/player/data/PersistenceQueue.kt) | submit 顺序屏障、submitLatest 合并、run 等待落盘；退出排空 |
| C/ui/LibraryShell.kt、LibraryDetailTransition.kt | 响应式曲库/搜索/详情、多选、封面和迷你播放器；标签单树轻量图层动画/独立滚动位置，详情独立过渡 |
| C/ui/NowPlayingOverlay.kt | 完整播放器、歌词/队列、单个平衡开关、翻译/显示、悬浮开关与编辑信息 |
| C/ui/MusicFeatureController.kt、MusicFeatureScreens.kt、BilibiliLoginDialogs.kt | 共享设置、助手、B 站扫码/Web登录与表单、音量帮助、任务进度、拒绝原因 |
| C/ui/ToolsScreens.kt、SyncPackageDialog.kt、BatchEnrichDialog.kt | 导入、歌单文件、设置、导出范围、补全选项/报告 |
| C/tutorial/TutorialCatalog.kt、ui/TutorialController.kt、ui/TutorialScreens.kt | 离线教程内容、首次选择、逐章进度、实际控件锚点/高亮、滚动定位与事件推进；J/features/TutorialProgressCodec.kt 编解码本地偏好 |
| C/platform/ArtworkMemoryCache.kt、SyncArtwork.kt | 位图 LRU/并发限制/同路径合并，曲库与歌单封面回退 |
| C/design/*、ui/AppearanceSettings.kt | 主题、天气配色、减少动态效果、外观设置 |
| [J/features/JvmMusicAssistant.kt](../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | 音乐工具 Agent、OCR、视频入库、翻译、推荐生命周期、预算/稳定上下文 |
| J/ai/MusicAiClient.kt | MusicAiTransport 可注入接口，HTTPS Chat Completions、tools、图片、用量、取消/超时 |
| J/features/MusicJsonStore.kt、MusicFeatureCodec.kt | 原子 JSON/备份恢复、设置/快照编解码、OCR/译文磁盘缓存 |
| J/features/MusicLibraryAccess.kt | AI/Worker 所需的原生曲库读写/删除/收藏/转码桥接 |
| [J/media/BilibiliClient.kt](../composeApp/src/jvmMain/kotlin/com/resonance/player/media/BilibiliClient.kt) | 公共 metadata、WBI、候选打分、音轨/CDN、封面；MusicVideoSource 可注入 |
| J/features/MusicTaskJournal.kt、J/enrich/AlbumCoverRepository.kt | 持久步骤/中断恢复/重试、QQ音乐/iTunes/酷狗专辑封面及缓存 |
| [J/playback/ListeningStatsStore.kt](../composeApp/src/jvmMain/kotlin/com/resonance/player/playback/ListeningStatsStore.kt) | 听歌时长与播放次数原子持久化、单曲/全库/每日聚合快照 |
| [C/ui/AudioEffectDialog.kt](../composeApp/src/commonMain/kotlin/com/resonance/player/ui/AudioEffectDialog.kt)、C/model/AudioEffectPreset.kt | 跨平台音效预设枚举、双端原生 PCM 滤波算法与选择交互面板 |
| J/lyrics/ParaformerLyricsClient.kt、AudioLyricAlignment.kt、GeneratedLyricsStore.kt | 百炼WSS识别、音频词时间戳对齐、原文备份及持久缓存 |
| J/media/Mp3Import.kt | FFmpeg 参数数组、兼容音轨复制或MP3转换、标签/封面、流式 hash |
| J/lyrics/LrclibLyricsRepository.kt、KrcDecoder.kt | 本地/缓存/LRCLIB，LRC/KRC、来源/偏移 |
| J/ai/DeepSeekClient.kt、enrich/MetadataEnricher.kt | 兼容旧配置/文本解析及补全；实际 AI 转交统一选中模型 |
| J/sync/PlainSyncPackage.kt | 唯一保留的歌单 ZIP 格式，hash/路径/大小、staging |
| J/io/AtomicPropertiesFile.kt、ImportControl.kt、StreamCompat.kt | 原子 Properties、备份恢复、取消信号、有界读 |
| J/audio/*、playback/PlaybackSnapshotCodec.kt | 整曲响度、PCM 平滑/限幅、播放快照 JSON |
| A/MainActivity.kt、platform/AndroidPlatformServices.kt | ActivityResult 提前注册、权限、MediaController、MediaStore/SAF、托管索引与原生交互 |
| A/PlaybackService.kt、AudioSessionCommands.kt、audio/* | 后台原生队列/切歌、数字增益、主音量、响度分析与悬浮窗所有权 |
| A/features/AndroidMusicAccess.kt | 无 Activity 曲库桥接、AndroidCredentialVault；前台删除可转交系统确认 |
| A/features/RecommendationWorker.kt、AndroidLyricsOverlay.kt、AndroidAssistantImages.kt | 周期/前台下载通知、WindowManager 歌词、长截图区域解码 |
| D/platform/DesktopPlatformServices.kt | 平台适配；同文件内 DesktopLibraryStore、DesktopMusicScanner、DesktopAudioPlayer |
| D/features/DesktopMusicAccess.kt、DesktopAssistantImages.kt | AI 曲库/文件桥、FFmpeg、DPAPI、截图区域解码 |
| D/audio/*、conversion/* | FFmpeg/Java Sound、响度；KGMA/KGG、密钥库、托管转换 |
| D/Main.kt、ui/DesktopFloatingLyrics.kt | 主窗口/托盘、后台存活、透明置顶/点击穿透 |

内部 SyncReport、exportSyncPackage、DeepSeekConfig 等名称为兼容保留，不代表 LAN/酷狗链接仍存在。

<a id="startup"></a>

## 4. 启动、状态与并发

Android MainActivity 在 setContent 前创建平台服务，ActivityResult launcher 需早于 STARTED 注册；不要改成可能在 RESUMED 才执行的 Composable 懒创建。旧 resonance://sync filter/链接总线已移除。Windows 有托盘时关闭主窗隐藏，退出菜单才终止；无托盘环境关闭即退出。

App 持有 UI 库/歌单/导航/队列/播放及弹窗；PlaybackStateStore 管理音频设置任务。启动加载全局音量、响度开关、模式、库/歌单、上次选单、歌词目录，恢复快照。原生后台仍播放则 attached，不重复暂停/重播。

MusicFeatureController 装配共享新功能。设置通过独立 PersistenceQueue 合并保存；用户启动 AI 任务前等待设置及库写入屏障，避免尚未落盘的配置或旧 UI 写覆盖。任务结束重载原生库/歌单；不要只修改 UI 数组。App 将库快照交给后台桥接。

同一数据根的前台/Worker 共享任务 Mutex、状态、当前 Job 和请求锁；一个修改音乐的任务运行时，其他任务需等候/取消。每次付费请求串行预算核算。取消停止剩余步骤，已入库歌曲保留。

PersistenceQueue.submit 是顺序屏障，submitLatest 按键合并待写值，run 可等待；UI 退出排空已接收写。原生 Store 读改写仍需自己的同步锁，不能认为 AI Mutex 保护了所有 UI 操作。

<a id="models"></a>

## 5. 模型、ID 与兼容

| 模型 | 数据与语义 |
| --- | --- |
| Track | id/title/artist/album/durationText/artworkSeed/isFavorite/sourceUri/artworkPath/mimeType |
| Playlist | id/name/subtitle/tracks/artworkSeed；成员快照可恢复旧目录条目 |
| Lyrics / LyricLine | trackId、lines、synchronized、instrumental、source、fromCache；timestampMs 可空；新增 translation/translationLanguage |
| ImportReport | tracks、skippedCount、warnings、cancelled；取消允许部分完成 |
| MusicFeatureSettings | 连接列表/文本视觉角色、推荐开关/后台/间隔/数量、月预算、悬浮开关/锁/字号、翻译语言/显示、B 站 Cookie；screenshotRecognition（默认 LocalOcr，可选 CloudApi） |
| MusicAssistantState | 最近 40 条消息、任务状态/进度、最近100条持久步骤、月输入/缓存/输出 Token、语音秒数、推荐批 |
| VideoImportRequest | link/title/artist/album/cover/playlistId；自定义字段写入新 MP3 |
| RecommendationBatch | createdAt/trackIds/reasons；固定歌单 ID resonance-daily-recommendations |

桌面扫描、Android MediaStore/SAF、下载托管曲的 ID 规则不同，不能假定为跨设备内容 hash。下载输出用 hash 命名，B 站来源用 bvid:cid 记录去重。rematchCatalogTracks 仍可将旧目录匹配本地；不要删除历史歌单以“迁移”已移除的链接功能。

改字段要一起检查两端库/歌单快照、PlainSyncPackage、MusicFeatureCodec、播放快照和缓存键。翻译独立缓存，不改原歌词时间轴或文件。

<a id="library"></a>

## 6. 扫描、转换与歌单

Android loadLibrary 不主动弹权限：有音频权限合并 MediaStore 与托管索引，无权限只读托管。主动系统扫描按 API 33 分界申请音频/外部存储读取。选择目录经 SAF 授权，递归读普通音频、收集 KGMA 后逐个转换；KGG 立即给 Windows 转换提示。

Windows JFileChooser → DesktopMusicScanner：先枚举候选，再逐文件解析/转换；勾选转换或遇加密扩展名调用 DesktopManagedMp3Converter。支持 MP3/FLAC/M4A/AAC/Ogg/Opus/WAV/WMA/APE 及实现的加密格式。

ImportProgress 经 StateFlow 提供阶段、文件、已完成/总量、比例/字节；未知总量用活动进度条。ImportControl 在遍历/文件/解密块检查取消；FFmpeg 进程/会话独立取消，单文件转码限 10 分钟。返回已完成曲目，清当前 staging/part，输入保持。

- KGMA 仅 v3/slot 1，256 KiB 流式处理。MP3 载荷直接复制，FLAC/Ogg 等经 FFmpeg 转 320k MP3。
- KGG 仅 Windows v5，依赖本机酷狗 KGMusicV3.db 的对应密钥；缺密钥明确失败，Android 不提供此实现。
- 错误按文件隔离，stderr 有界读取避免 FFmpeg 管道塞满；不要把慢转码当无进度死循环。
- fingerprint 复用转换输出，转换不覆盖输入；主动删除是另一条路径。

歌单删除只删关系。主动删除曲目会尝试删除真实 sourceUri，包括应用外音频；Android content:// 可要求系统确认。后台 Worker 无法替代外部文件的系统授权，自动推荐自有音频通常不需确认。收藏在 Android 以专用偏好 ID 集合为准。

AndroidMusicAccess 共用 filesDir 锁，读取托管索引+App 曲库快照，让任务不依赖 Activity；Windows Store 的 merge/update/favorite 为同步事务。酷狗歌单链接入口/解析已删除，历史数据保留；酷狗元数据补全与 KGG 密钥仍在。

<a id="playback"></a>

## 7. 播放、音量平衡与恢复

PlaybackStateStore.activate 只加载 vol.global 和 gain.auto_enabled 下的缓存/分析增益；关闭平衡时自动增益为零。旧逐曲/歌单音量、手动增益、校准值留在存储以兼容，但正常激活不叠加。播放器/设置只露一个平衡开关，详细说明单独页面。

整曲响度按真实峰值余量限制补偿，算法版本化缓存；先播放后完成分析，增益平滑更新；静音/失败不缓存成功。保留 generation、准备任务取消、用户修改版本和旧原生事件保护，不能缩减为仅按 trackId 触发的无版本协程。

Android PlaybackService 无 Activity 也能随原生队列切歌加载全局/自动值；自定义命令和 PreampAudioProcessor 完成增益/限幅。Windows FFmpeg 输出 48k PCM、Java Sound 输出，变速 atempo；停止/删除当前音频前关闭解码进程释放文件。

PlaybackSnapshot 保存曲目、队列、进度、歌单。冷启动暂停恢复，后台会话存活则附着。Android activeMusicId 来自真实服务状态，不能拿上次快照判断“正在播放”，否则旧推荐永远被保留。

<a id="lyrics"></a>

## 8. 歌词、翻译和补全

LrclibLyricsRepository 处理本地/同名 LRC/KRC、缓存和 LRCLIB；成功约 30 天、未找到约 24 小时缓存，强刷可绕过。source 实际来源保存，不再把 AI 缓存误标 LRCLIB。

自动歌词不悄悄付费；显式 AI 检索或批处理选 AI 才调用当前模型。DeepSeekClient 是兼容类名，completion 转 JvmMusicAssistant.completeText 统一预算。未找到不能作为歌词；未经音频对齐的 AI 时间戳不当同步歌词。

翻译：设置目标语言（中文、English、日语等）→ 歌词页点击 → 每批 50 行请求等长 JSON → 核对行数 → 与原 timestampMs 一一合并。失败不覆盖原文。原文本 hash+语言是持久缓存键，重启/切换模式复用，重译才强制调用。支持原文/译文/双语，无译文回原文，无时间轴不伪造同步；悬浮歌词同样使用。

MetadataEnricher 公共 iTunes/酷狗、封面、本地/LRCLIB 优先；选项允许才 AI 兜底。只缺专辑也可触发 AI；公共结果匹配标题/已知歌手，不任取首项，未知歌手可按标题检索。每首变化立即 checkpoint 落盘，批次边界取消；封面最多 10 MB 并检图像签名。结束重载/清位图缓存，报告在本次流程显示，歌曲页不再挂缺失计数。公共源与 AI 不保证全量命中/事实准确。

### 音频同步歌词（2026-10-16）

PlayerState.positionMs 从两端原生播放器读取，歌词不再只用 durationText×progress；PlatformServices.playbackPositionMs 为可空兼容属性。纯文本明确标识并允许手动滚动，不能把第一句当作同步状态。

LRCLIB 精确纯文本后继续搜索同步版本；时长偏差过大、仅0时间戳或超曲长时间轴不当作同步歌词。用户可显式生成：MusicFeatureController → JvmMusicAssistant.generateLyricsInternal → MusicLibraryAccess.prepareSpeechAudio → ParaformerLyricsClient(WSS) → AudioLyricAlignment → GeneratedLyricsStore。FFmpeg 在应用私有speech-staging目录准备16kHz单声道16bit PCM（最多30分钟），finally清理临时音频；ASR最终词/句时间戳是唯一时间来源。已有文本用字符序列对齐且校验覆盖率，失败则用识别文本；不猜测或均分时间。

设置新增 speechApiKey（加密，可留空复用千问Key）、speechUrl（默认百炼北京域名）、monthlySpeechSeconds（默认3600）。主动生成/收费重试前提示上传与计费；按PCM整曲时长预留月秒数，失败/取消保守计入，与聊天Token分离。自动下载/推荐不会调用付费音频识别。结果及原文备份在 music-assistant/generated-lyrics/ 按曲目信息指纹保存；后续播放器/后台悬浮歌词优先复用。

协议参考：[百炼WSS](https://help.aliyun.com/zh/model-studio/websocket-for-paraformer-real-time-service)、[客户端事件](https://help.aliyun.com/zh/model-studio/paraformer-client-events)、[最终结果字段](https://help.aliyun.com/zh/model-studio/paraformer-server-events)。未提供用户Key时只能验证本地协议解析、模拟返回、缓存和预算，不能声称收费服务已验通。

<a id="sync"></a>

## 9. 歌单文件导出/导入

唯一实现 PlainSyncPackage，UI“歌单导入/导出”。LAN/QR/deeplink/加密包实现删除。内部 exportSyncPackage/importSyncPackage 和 resonance-sync-1 格式名保留兼容；RESONPKG 旧加密包明确拒绝，需重新导出。

.resonance 是标准 ZIP，含 manifest.properties、audio/、art/，可按歌单/全库导出可打开音频与元数据/关系；缺本地音频的目录条目不当可播放曲导出。不含 Key/Cookie、歌词/OCR 缓存、外观/天气/音量/快照。

保留扩展名/MIME，FLAC 不再误标 MP3。导入先 staging，校验清单/音频 SHA-256/路径/大小再落盘合并。上限：单项 4 GiB、总量 512 GiB、200001 条目、曲目/歌单各 100000，另查可用空间。safeResolve 限制根内与允许目录，目标用安全 ID+扩展；已有同名目标允许替换，不能声称永不覆盖。明文文件按普通含音乐文件保管。

<a id="storage"></a>

## 10. 持久化、配置与网络

Windows 数据根：系统属性 RESONANCE_DATA → 同名环境变量 → LOCALAPPDATA/Resonance（否则用户目录）；音乐根：RESONANCE_LIBRARY → 有 D 盘则 D:/Music/Resonance → 用户 Music/Resonance。测试需同时隔离两个目录。

| Windows 数据 | 内容 |
| --- | --- |
| library.properties / playlists.properties | 库/成员/目录快照 |
| ui-state.properties | 偏好、快照、主音量/自动响度、外观/天气、歌词目录、旧 AI 配置 |
| artwork/、lyrics-cache/ | 解析/补全封面、歌词 |
| music-assistant/music-assistant.json + .bak | 新连接、消息、月用量/语音秒数、任务步骤、批次/pending、拒绝原因、来源/手选封面映射 |
| music-assistant/cache/、artwork/ | hash 命名 OCR/译文、AI 封面；album-artwork/ 为真实专辑图片，generated-lyrics/ 为音频对齐歌词与原文备份 |
| 托管音乐根 | 下载/转换/导入文件及封面；临时 .video-import-*、.staging-* |

| Android 数据 | 内容 |
| --- | --- |
| resonance_ui_state | 偏好、快照、音量/响度、外观天气/歌词目录 |
| resonance_playlists / resonance_playlists_backup | 歌单/快照及上一版 |
| resonance_favorites | ids 收藏集合 |
| filesDir/synced-library.properties | 托管/SAF/下载索引，旧名称保留 |
| filesDir/music-assistant/ | 同结构 JSON/缓存、后台 librarySnapshot/removedIds |
| filesDir/artwork、lyrics-cache、selected-lyrics | 封面、歌词、SAF 镜像 |
| externalFilesDir(Music)/Resonance | 托管音频；不可用回 filesDir/Resonance |
| resonance_overlay | 悬浮 x/y |
| resonance_deepseek | 旧配置兼容；可迁入新配置，旧明文不主动删除 |
| cacheDir、下载 Resonance 目录 | 临时/用户导出包 |

新 Key/Cookie：Windows DPAPI、AndroidKeyStore AES-GCM；跟账户/设备密钥关联，不保证拷到另一账户解密。旧 DeepSeek 配置可能仍有明文，不能宣称所有历史 Key 都已安全迁移。Android 关闭备份。

AtomicPropertiesFile、MusicJsonStore 用临时文件/原子替换/上一版 .bak。主 JSON 坏时先恢复有效备份再写；两份都坏明确报错，不静默重置。缓存坏可重算，不能套用于配置主文件。

| 网络功能 | 发送内容 |
| --- | --- |
| LRCLIB / 补全 | 歌名歌手专辑/时长/检索词，下载公开封面 |
| AI 聊天/推荐 | 稳定库摘要、歌单名、部分曲目、最近消息和拒绝原因；不上传整库音频 |
| 截图 OCR / 翻译 | 本地 OCR 不上传图片；CloudApi 发送图片分片，翻译发送歌词文本及目标语言 |
| 截断歌曲信息核对 | 可见歌名/歌手前缀发给 QQ音乐、iTunes/B站；不发送截图或音频 |
| B 站 | 搜索词、视频 ID、WBI、可选自有 Cookie |
| 主动生成同步歌词 | 将准备后的整首音频发送到配置的WSS语音接口，独立时长计费 |
| 天气 | 城市、粗坐标，现有天气/IP 定位接口 |

AI 仅 HTTPS，无携 Key 重定向；15 秒连接、90 秒读取、有界响应和取消断开。B 站限制页面/CDN 主机、音频 1 GiB/封面 10 MB、下载超时/取消；官方登录/验证/412 要报真实原因，不无限重试或绕过限制。

Android 权限：音频/SAF、网络、粗定位、通知、媒体前台服务；新增 SYSTEM_ALERT_WINDOW 和 WorkManager dataSync 前台服务，无管理所有文件权限。悬浮权限由系统页面授予；拒绝不会阻断播放。

<a id="ui"></a>

## 11. UI、封面与悬浮歌词

### 使用教程（2026-10-16 核对）

`App` 加载 `PlatformServices.loadTutorialProgress` 后，在曲库初始加载完成时首次提示快速了解、跟着操作或跳过。选择后持久记住，不按普通版本更新重弹；旧安装没有教程键时在首次升级启动显示一次。设置顶部固定“使用教程”，`LibraryDestination.Tutorial` 属于工具子页，支持四页快速介绍、七章图文/操作教学和常见问题。

`TutorialController` 导航到实际页面；`tutorialTarget` 记录控件窗口坐标并通过 BringIntoView 定位，图层入场完成后刷新实际坐标；`TutorialCoach` 绘制透空高亮及可收起/滚动说明，暂停按钮固定在顶部，实际控件仍接收点击。教程页隐藏迷你播放器，手机快速浏览同时隐藏底栏。导入、当前歌曲、收藏、歌单和配置按实际状态判断；播放器展开、歌词/队列等按实际事件推进。缺少歌曲先回曲库，错误事件不推进；完成、跳过分别保存，暂停后继续原步。系统选择器、登录弹窗仍由原平台处理。

内容与示意内置，无新增联网/权限。教程不会主动登录、扫描、下载、删除或发送付费请求；推荐清理、真实删除、AI/语音收费和上传在相关章节说明，危险操作只阅读。进度键 `tutorial.progress.v1` 存 Android `resonance_ui_state` / Windows 原子 `ui-state.properties`，不含音乐/凭据，不进入歌单文件；Windows 沿用同步锁、原子写和备份，Android 检查 commit，UI 经 PersistenceQueue 顺序保存。回归入口为 `TutorialProgressTest` 与 Android `TutorialNavigationTest`。

LibraryShell：<720 dp 底栏、720–1099 rail、>=1100 侧栏。主导航为音乐库、音乐助手、工具；本地搜索在曲库右上角。新增 Tasks / Recommendations；助手页隐藏迷你播放器，聊天列表不再混放推荐。MainActivity 显式配置 adjustResize，紧凑底栏与 MiniPlayer 在软键盘（IME）弹起时自动隐藏，避免推飞标题栏与产生底部断层。媒体下载使用标准 Chrome User-Agent 与双向缓冲流规避 QoS 限速，转码启用多线程，歌曲入库后封面与歌词获取并发执行并增加 8 秒超时保护。

UI 与动效核对日期：2026-10-27。曲库标签使用 LibraryTabTransition：单棵 Lazy 树、220ms/32dp 减速入场；减少动态效果时立即切换并停止该过渡。列表/网格状态提升到切换外，进入页面不自动滚到当前歌曲；DestinationContent 的 SaveableStateHolder 保存离开主导航前的浏览位置。主页面转场在固定、不透明的 Canvas 视口内执行，各页面也绘制 Canvas；常规导航保留既有 `SizeTransform(clip = false)` 弹性，滑动时间统一为 280ms，键盘即时导航与减少动效使用 `using null`。此前本节“全部禁用尺寸插值”的描述已与 2026-10-17 的恢复弹性记录不符，本次按实现校正。窗口背景随主题同步、系统栏仅在主题变化时更新；Android 维持 `#111A1D` 窗口背景与透明系统栏，国产 ROM 的实际边缘表现仍需设备验证。详情仍由 LibraryDetailTransition 处理。完整播放器保持单一 Animatable / 固定尺寸 graphicsLayer 的 280ms drawer 过渡，减少动效时为 120ms 淡入淡出；播放器分栏、封面和迷你播放器切歌另外遵循减少动效偏好。

ResonanceTheme 保留青绿 / 冰蓝主色、原生 Sans Serif 字阶和语义颜色；2026-10-27 按用户选定的 18 号画廊展台方向将浅色背景改为淡灰白墙面，深色台面通过明度区分层级。`GallerySurfaces.kt` 集中管理 2dp 封面圆角、9dp 展架、实体台面倒角及右下定向投影；播放键、迷你播放器、助手输入台和工具图标沿用同一材质。阴影使用现有 Compose `dropShadow`，台面绘制通过 `drawWithCache` 缓存；不逐帧改变阴影半径。默认内容色和主题首帧可读性保留，天气配色仍可短暂插值。曲库与播放器的 ResonanceSelectionTab 改为短下划线与文字强调，保留选中语义、48dp 高度和键盘焦点。窄屏曲库保留搜索、任务及更多菜单；新建歌单、多选和预览清理放在更多菜单，教程仍直接呈现新建按钮。空曲库仍区分搜索、导入、新建的操作层级。

SoftNeumorphicButtons 保留青绿主播放键与线性轨道，次要按钮取消独立圆盘并落在实体播放台上。按压采用 120ms / 0.98 反馈，主播放键轻降 1.5dp；减少动效或键盘输入时不缩放或位移。主封面与展架独立，换曲只过渡画面，不再随暂停缩小。歌曲信息入口为「播放选项 → 歌曲与来源」。手机窗口低于 560dp，或字号大于 1.2 且高度不足 840dp 时使用紧凑布局；隐藏的内联音量区通过「播放设置 · 音量与速度」可达。播放进度与音量改用 Material Slider 的原生输入及语义，细轨道与小手柄独立于至少 48dp 的交互高度；手柄的测量容器也保持 48dp，避免只把外层布局撑高却留下细小的语义区域。拖动保留本地预览与实时回调，音量、倍速与平衡在窄窗口或大字号下分行；不增加波浪轨道或音频处理。正在播放指示器只在绘制阶段读相位；FluidAmbientCanvas 缓存三个光场画刷，逐帧只更新位置，暂停、后台、减少动效与播放器过渡的既有停动条件保留。品味雷达使用 280ms 过渡并在绘制阶段读动画值，减少动效时直接显示结果。

工具/设置公共行使用小台面图标，保留焦点/悬停背景与可换行说明。助手阅读区最大宽度为 960dp，顶部操作可换行，消息按发送方区分位置，输入与发送操作放在同一台面；空会话提示仍只填写草稿，不自动发送请求。桌面原生渲染与交互回归见 `DT/ui/UiVisualContractTest`，覆盖深浅主题、两种配色、主题切换首帧、原生滑块语义回调与触控尺寸，以及手机/宽屏/横屏/150% 字号截图；当前截图位于忽略的 `composeApp/build/ui-gallery/screenshots/`；新增小屏大字号主控件边界、歌曲信息和播放设置菜单可达性检查。Android 对应 `UiInteractionContractTest` 已新增，当前验证状态见[画廊 UI 记录](changes/2026-10-27-04-gallery-ledges-native-ui.md)，桌面渲染不等同于 Android 真机验证。

每日推荐始终为歌单网格第一张卡；关闭时进入设置，开启时打开 RecommendationsScreen。持久每日歌单按 DAILY_PLAYLIST_ID 去重，其他歌单保持原顺序。MusicTasksScreen 汇总最近100个音乐处理步骤，支持取消和媒体步骤重试；聊天/删除工具不自动重放。

专辑封面：AlbumCoverRepository 优先QQ音乐目录并回退MetadataEnricher的iTunes/酷狗公开目录源，严格验证歌名/歌手，再按专辑缓存；新 B 站音频自动匹配，未命中保留视频图。播放菜单单首强制匹配/自定义；任务页按 videoOrigins 批量处理既有下载，customCovers 标记保护手选封面。库覆盖值用于显示，既有音频不会因此重编码。

AlbumArtwork 通过 ArtworkMemoryCache：40 项且总像素约 800 万、3 并发、同路径 single-flight、不缓存失败；修订号触发现有图片重载并阻止清空前旧任务回写。两端解码约 768 像素。Android 缩略图异常/空继续从映射音频 URI 取内嵌封面，回退不能被外层异常短路。

长图区域解码到宽<=1600、每片高约1800、重叠120，最多40片/30 MB；原图像素有限制，超限明确分批，不静默丢末尾。Windows ImageReader、Android BitmapRegionDecoder 不先加载完整超长图。

Windows FloatingLyricsHost → 透明置顶 Compose Window；拖动、字号、自适应高度、锁定/关闭，JNA 点击穿透；主设置解锁。主窗隐藏仍托盘运行，完全退出停止。Android PlaybackService 拥有 WindowManager TYPE_APPLICATION_OVERLAY，250 ms 按原生毫秒位置选行，不依赖未准备好的元数据时长；后台切歌自己查本地/LRCLIB及已有译文，不自动付费翻译。锁定 NOT_TOUCHABLE，拖动限制屏内并保存位置。系统强停/进程终止后需恢复播放。

ResonanceTheme/WeatherAppearance/AppearanceStorage 管理主题、天气和减少动效；前台/动效偏好控制环境背景。既有定位、城市搜索和天气缓存链不由本轮新功能替换。

<a id="build"></a>

## 12. 构建、测试与发布

### 12.1 环境与配置

推荐复用 CI 的 JDK 17；`scripts/build.ps1` 可选择 17–21。Gradle 使用 Wrapper，不要依赖全局 Gradle。Android SDK 需要 platform 36、Build Tools 36.0.0、platform-tools；设备测试还需要已连接的设备/模拟器。首次构建需要访问 Maven/Google/Gradle 仓库。

Windows 播放、转码、响度集成测试和打包需要可执行 FFmpeg。用 `RESONANCE_FFMPEG` 指向实际 `ffmpeg.exe`；打包任务复制到桌面应用资源，文件不存在时构建明确失败。当前本机构建脚本和 FFmpeg 查找仍含作者的 D 盘回退路径，它们不是通用环境要求。GUI 桌面运行需要图形环境和音频设备；构建 EXE/MSI 还依赖本机 Java 打包工具链可用。

```powershell
# 示例路径必须换成开发机真实目录，不要提交本机配置或密钥。
$env:JAVA_HOME = 'D:\Tools\jdk-17'
$env:ANDROID_HOME = 'D:\Tools\AndroidSdk'
$env:GRADLE_USER_HOME = 'D:\Caches\Gradle'
$env:RESONANCE_FFMPEG = 'D:\Tools\ffmpeg\bin\ffmpeg.exe'
```

桌面隔离调试需同时设置 `RESONANCE_DATA` 和 `RESONANCE_LIBRARY`，指向项目下独立测试目录，再运行应用；勿用真实音乐库测试删除。FFmpeg 运行时定位顺序包含系统属性 `resonance.ffmpeg`、环境变量、Compose/jpackage 资源、开发回退路径，最后尝试 PATH 中的 `ffmpeg.exe`。Gradle 打包资源任务本身主要使用 `RESONANCE_FFMPEG` 及构建脚本的默认路径。

Android Release 配置读取未提交的 `keystore.properties`，示例在 [keystore.properties.example](../keystore.properties.example)。有配置时启用 v1 和 v2 签名；无配置时不能把 Release 输出当成可分发且已校验的正式签名 APK。Debug applicationId 为 `com.resonance.player.debug`，正式包为 `com.resonance.player`，可并存。

### 12.2 常用命令

从仓库根目录 PowerShell 执行，按本次任务选择必要命令；本轮实际结果见独立验证记录。

```powershell
# 桌面开发运行
.\gradlew.bat :composeApp:run --no-configuration-cache

# 桌面测试，包含可在 JVM 运行的 commonTest
.\gradlew.bat :composeApp:desktopTest --no-configuration-cache

# 定点回归示例（使用完整包名）
.\gradlew.bat :composeApp:desktopTest --tests 'com.resonance.player.playback.PlaybackStateStoreTest' --no-configuration-cache

# 两端单元测试、Release Lint、Debug APK；与当前 CI 对齐
.\gradlew.bat :composeApp:allTests :composeApp:lintRelease :composeApp:assembleDebug --no-configuration-cache --console=plain

# Android Release APK（正式发布另验签名）
.\gradlew.bat :composeApp:assembleRelease --no-configuration-cache

# Android 设备测试
.\gradlew.bat :composeApp:connectedDebugAndroidTest --no-configuration-cache

# Windows 完整运行目录、EXE、MSI
.\gradlew.bat :composeApp:createReleaseDistributable :composeApp:packageReleaseExe :composeApp:packageReleaseMsi --no-configuration-cache
```

UTP 依赖或设备发现失败时，不能将错误改写成设备测试通过。若已实际构建对应 debug 与 androidTest APK，可通过 ADB 手动安装并运行相同 runner，并记录具体 APK、设备、命令及输出：

```powershell
.\gradlew.bat :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest --no-configuration-cache
# 将 <版本> 替换为 version.properties 中当前值
adb install -r composeApp/build/outputs/apk/debug/Resonance-<版本>-debug.apk
adb install -r composeApp/build/outputs/apk/androidTest/debug/composeApp-debug-androidTest.apk
adb shell am instrument -w -r com.resonance.player.debug.test/androidx.test.runner.AndroidJUnitRunner
```

`allTests` 不等同于连接设备的 instrumentation；成功构建不等同于设备播放、蓝牙、标签写回、真实大曲库或 UI 通过。部分 FFmpeg/KGG 测试有依赖文件/环境的条件返回，查看实现与报告确认关键场景是否实际运行，不能只看总任务绿色。

### 12.3 测试定位

| 范围 | 当前测试文件（相对包根） | 关注场景 |
| --- | --- | --- |
| CT | `PlaybackQueueResolutionTest`、`SelectedPlaylistStateTest`、`PersistenceQueueTest` | 队列范围、失效歌单回退、顺序/合并落盘 |
| CT | `CatalogArtworkRematchTest`、`ImportReportMessageTest` | 匹配后的封面回退、实际转换报告 |
| CT | `LibrarySearchIndexTest`、`WeatherPaletteTest`、`conversion/KgmaCipherTest` | 搜索文本/首字母/万首复用、天气码、KGMA 原语 |
| DT | `playback/PlaybackStateStoreTest`、`PlaybackSnapshotTest`、`PlaybackModePersistenceTest` | 快速切歌、旧原生事件、手动设置优先、准备阶段取消、快照与模式 |
| DT | `platform/LibraryPersistenceTest` | 并发 merge/favorite、主文件损坏恢复、设置备份 |
| DT | `audio/PcmGainProcessorTest`、`LoudnessAnalysisTest`、`FfmpegLoudnessIntegrationTest` | 正增益/限幅/渐变/静音、真实整曲响度 |
| DT | `conversion/DesktopManagedMp3ConverterTest`、`KggCipherTest` | 保留输入的转码、KGMA 内 FLAC、KGG 算法/可用数据库 |
| DT | `conversion/KgmaFixtureEncoder`、`KgmaDeviceFixtureTest` | 生成 Android 转换检查用的小型夹具 |
| DT | `lyrics/LrclibLyricsRepositoryTest`、`KrcDecoderTest`、`ai/DeepSeekClientTest` | 多时间戳、LRCLIB 本地服务器/缓存、KRC、AI 文本解析 |
| DT | `enrich/MetadataEnricherTest`、`util/PinyinUtilsTest` | 缺失字段/清理、目录解析、首字母 |
| DT | `sync/PlainSyncPackageTest`、`features/PlaylistArchiveFormatTest` | ZIP 往返、hash/大小/路径、原格式、拒绝旧加密包 |
| DT | `features/MusicAssistantTest`、`MusicJsonStoreTest` | 推荐/拒绝/缓存/预算/工具、备份恢复 |
| DT | `sync/CrossDeviceSyncFixtureTest` | 明文包夹具 |
| AT | `AudioSessionIntegrationTest` | 自定义命令授权、无 Activity 切歌、PCM 单声道、FFmpegKit 整曲与 Android regex |
| AT | `MusicFeaturesIntegrationTest` | MP3标签/封面、缩略图回退、长图、密钥加密、删除 |
| AT | `LibraryNavigationTest` | 300首列表位置、助手隐藏迷你播放器、推荐卡片、播放器反复展开/关闭 |
| AT | `LibraryDetailTransitionTest` | 浅/深色、推入/返回/快速返回、减少动态效果的中途像素检查 |

测试根目录：[commonTest](../composeApp/src/commonTest/kotlin/com/resonance/player)、[desktopTest](../composeApp/src/desktopTest/kotlin/com/resonance/player)、[androidInstrumentedTest](../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player)。测试覆盖不等于所有外部接口在线有效；改网络匹配后还需说明是否做了真实服务检查。

### 12.4 构建产物与脚本

| 位置 | 用途 |
| --- | --- |
| `composeApp/build/outputs/apk/<variant>/Resonance-<版本>-<variant>.apk` | 按版本重命名的 APK |
| `composeApp/build/compose/binaries/main-release/app/` | 完整桌面运行目录；实际子目录以任务输出为准 |
| `composeApp/build/compose/binaries/main-release/exe/Resonance-<版本>.exe` | Windows 安装包 |
| `composeApp/build/compose/binaries/main-release/msi/` | MSI 任务输出 |
| `composeApp/build/reports/tests/`、`lint-results-release.*` | 自动化测试和 Lint 报告 |
| `release/<版本>/` | release-manifest 收集的 APK、可选 EXE 及 `SHA256SUMS.txt` |

`scripts/build.cmd` 调用 `build.ps1`。脚本默认**不升版**，执行 allTests、lintRelease、assembleRelease、packageReleaseExe 后调用 release-manifest。`-NoBump` 为保留的兼容参数；显式 `-BumpVersion` 或 `-SetVersion x.y.z` 才改版本。Gradle 也有显式 `:composeApp:bumpVersion`，不要把它附加到普通构建。

`generateAppVersion` 从 `version.properties` 生成 `composeApp/build/generated/version/commonMain/.../BuildVersion.kt` 中 `APP_VERSION`；不要编辑生成物或在 UI 另写版本常量。

`scripts/release-manifest.ps1` 先要求 Release APK，复制 APK 和已存在的对应 EXE、计算哈希，然后更新 `docs/RELEASE_NOTES-<版本>.md` 的 `GENERATED-CHECKSUMS` 区域。因此发布前必须先准备该文件；脚本不会核实 EXE 是否由本次源码重建，也不自动收集 MSI/Portable ZIP，需人工核对范围。

Windows 必须完整分发应用/runtime/resources；只复制运行目录中的 `Resonance.exe` 不能代替完整包。`scripts/generate_icons.py` 管理图标派生，改品牌图时同时核对 Android 资源、桌面 ICO/PNG 与打包设置。

### 12.5 CI 与发布顺序

[verify.yml](../.github/workflows/verify.yml) 在 main push / PR 上用 Windows runner、JDK 17、SDK 36、Chocolatey FFmpeg，运行 allTests / lintRelease / assembleDebug；无论成功失败都尝试上传报告，保留 14 天。它不发布 Release、不替代真实设备回归，也不校验变更文档规则。

发布流程：确认任务范围和回归 → 显式设置版本 → 新增带日期/重点的版本说明及各项变更链接 → 构建两端产物 → 校验签名及完整运行目录 → 执行 `scripts/release-manifest.ps1` → 核对哈希/产物/说明一致 → 在用户授权的发布流程中上传。文档、代码和版本号存在不代表已经完成最后一步。

<a id="assistant"></a>

## 13. AI 工具、截图和每日推荐

### 连接与执行权限

设置支持千问、DeepSeek、豆包、自定义 HTTPS /chat/completions；文本与视觉独立 provider。千问默认视觉模型是建议值，豆包需用户填实际模型/接入点。账户可用模型以供应商为准；通用兼容客户端不等于实现每家专属 SDK。

DeepSeek 文本可配千问/豆包视觉；未配置视觉则提示，不把图传给纯文本。文本管理模型必须支持 tool_calls。Agent 权限为真实音乐工具，无任意 shell、电脑桌面或其他软件控制；截图/标题/歌词/工具结果只作数据，不能授权删除。

| 工具 | 行为 |
| --- | --- |
| search_library / list_playlists | 搜索真实歌曲 ID、读取歌单/成员；输出去除本地路径 |
| create_playlist / rename_playlist / delete_playlist | 自建歌单管理；删歌单保留音频 |
| add_to_playlist / remove_from_playlist | 成员关系 |
| favorite_tracks | 设置/取消收藏 |
| update_metadata | 曲库标题/歌手/专辑/封面；AI 封面只接受 B 站 HTTPS 图片 |
| delete_tracks | 本轮用户明确要求删除音频才允许，删除文件及全部关系 |
| find_and_download / search_bilibili / import_bilibili | 本地优先、原版候选、指定视频下载与自定义信息 |

工具调用已取消 16 轮/步骤限制（使用循环持续执行直到任务完成），单轮最多 20 个工具，结果回传后再答复。无效 ID/工具报错，不伪称完成；否定删除、只删歌单等有回归。音乐任务可取消，已完成入库保留。MusicTaskEntry 可带 parentId，任务页按操作分组并汇总截图导入逐首进度；免费失败媒体步骤可批量重试，收费歌词仍单独确认。

### B 站链路

手动：链接/BV/短链接/分 P → metadata → 编辑歌名/歌手/专辑/封面/目标歌单 → DASH音轨或单段 fallback → staging保留兼容音轨（默认）或转 MP3 → 原生入库/关系。自动按 SongRequest 完成相同链路。来源 bvid:cid 去重。

candidateScore 要求标题命中、偏好歌手/官方/原版；自动候选硬过滤 MV/现场/live/翻唱/cover/伴奏/混音/变速/合集等非目标版本，支持设置UID范围。无可信候选就报告失败，不随便替换；规则不保证真伪。B 站需要官方登录/验证时显示错误，支持扫码登录（全端通用）或 Android 网页快捷登录（WebView 自动捕获 CookieManager 凭据），亦可手动粘贴 Cookie。

### 截图流水线与缓存

2026-10-17：截图识别由 `screenshotRecognition` 独立选择，旧配置默认 `LocalOcr`，原 `visionProvider` 和 API 配置保留。`PaddleScreenshotOcr` 使用 ONNX Runtime（Android 1.23.2 / Windows 1.20.0） CPU、内置 PP-OCRv5 移动版检测/识别及字典，Android assets / Windows classpath 分别由 `androidScreenshotOcr` / `desktopScreenshotOcr` 接入；无 Python/外部模型服务。逐片解码、单行推理、2 个推理线程，任务结束/错误/取消均关闭会话与张量；不会自动退回云端。`ScreenshotPlaylistParser` 按坐标整理水平歌名/歌手行、过滤界面文字并跨片去重，复杂排版与旋转照片不保证识别。模型来源、校验和、Apache-2.0 许可和复现方式见 `composeApp/src/ocrModels/README.md`。

本地识别结果与云端识别结果共用后续导入。`ocr-v2` 缓存键包含后端版本/云端连接、用户备注与图片摘要；切换后端不会复用旧 API 结果。本地识图不要求 Key、不调用 `ask`、不占用云端 Token 预算；只有 `CloudApi` 才发送图片至所选视觉服务。截图图片仍受 40 片/30 MB 上限约束。

`SongIdentityResolver` 检测末尾省略号，结合本地完整曲名、QQ音乐/iTunes公开目录，必要时结合有歌手证据和书名号等明确边界的 B 站标题核对；多候选/无证据报告未完成，不生成缺失文字。确认结果进入 `VideoImportRequest`，两端新 MP3 标签与曲库使用同一完整信息。没有批量修改历史曲库；重新导入已存在同一视频时，仅修正原截断曲库覆盖值/歌单快照，不重编码旧音频。

图片选择 → 区域分片 → 后端/备注/内容 hash 查 OCR 缓存 → 本地 OCR 布局整理或视觉 API 返回歌单名/歌名/歌手 JSON → 重叠去重 → 确定 ID 截图歌单 → 本地匹配 → B 站搜索/下载 → 逐曲保存。单歌单最多300首，一次20歌单，错误列出具体歌曲。当前先生成 ScreenshotImportDraft，允许批量勾选并编辑歌单名、歌名、歌手与专辑，确认后才创建歌单和导入；取消不下载。草稿仅在内存，重启后重新识别，OCR 缓存仍可复用。

固定 SYSTEM/工具 schema、稳定排序库摘要在消息前缀，聊天在后；摘要按歌手/歌名去重并含最多120首、歌手统计和歌单，工具可搜索大库。支持多会话隔离管理（最多持久化 30 个会话），首条消息自动截取前 20 字生成会话标题，各会话聊天独立落盘；请求严格仅取当前会话最近 16 条上下文，切换或新建会话 0 Token 消耗，彻底避免跨话题 Prompt 膨胀。消息气泡清晰标注回复模型（如 `回复自 通义千问 (qwen3-vl-flash)`），API 设置页提供常用主流模型 Chip 预选与一键恢复默认地址/模型按钮。OCR/翻译持久缓存避免重复成本。

按 YearMonth 累计输入/缓存命中/输出 Token（用于助手底部用量统计参考）。已取消所有 Token 使用量与预算上限限制，请求不再因预算超出拦截。云端缓存时效由供应商控制，不承诺隔天命中，也不付费发心跳保活。

### 推荐生命周期

默认关闭，1–168小时/1–30首；前台到期生成或允许后台。recommendationsUnmeteredOnly 缺省 false，可限制自动下载仅在系统判定的不计费网络；设置显示预计到期和等待状态，手动更新仍会立即联网。Android WorkManager 受节电/网络调度限制，下载有 dataSync 前台通知；Windows 托盘每分钟检查，完全退出后不运行。

2026-10-28工作区：每日推荐未听保护。自动推荐检查到期时，若当前批次推荐歌曲中存在未听过（播放时长与播放次数为 0、且无点赞/评论反馈）的歌曲，即使已达到设定的间隔时间，自动推荐亦不进行更换，完整保留上批推荐等待用户收听；设置中状态提示「本批推荐尚有歌曲未听过，保留不换」。手动点击「更新推荐」仍可强制换批。

2026-10-17工作区：默认使用歌源候选，可显式选AI；按已保存曲库/歌手分布/收藏/拒绝理由找真实候选，临时推荐不作为偏好，按歌曲身份去重并过滤已有/拒绝/冷却期内歌曲后下载。独立历史默认7天不重复；理由引用真实保存歌曲，不采用模型自由生成的数量/风格结论。至少一首成功才提交新批；总失败保留旧批/时间。每首音频入库立即写 recommendationPending（在封面/歌词后处理前），中断待提交音频在下次成功批统一处理。

新批成功时，未收藏且未在自建歌单的旧曲仅在托管音乐目录且无共享文件引用时物理删除；当前播放延后。保护按当前收藏/成员关系判断，取消收藏并移出歌单后不永久保护；retained 兼容记录不是清理依据。

推荐卡片的“踩”只记录偏好、不删文件；歌曲信息中的“不喜欢并删除”仍立即取消收藏、删音频、移出所有歌单，即使先前保存也执行；可选原因弹窗/跳过。删除失败显示实际错误。原因参与下次推荐，不自动封禁整位歌手。

<a id="gaps"></a>

## 14. 任务定位与已知限制

| 任务 | 先读 | 回归/检查 |
| --- | --- | --- |
| 音量、快速切歌竞争 | PlaybackStateStore、PlaybackService、平台响度 | PlaybackStateStoreTest、AudioSessionIntegrationTest |
| 转换慢/取消 | ImportControl、Scanner/scanDocumentTree、两端 FFmpeg | DesktopManagedMp3ConverterTest，源文件/staging/阶段 |
| 列表封面 | DynamicArtwork、audioArtwork、ArtworkMemoryCache、SyncArtwork | MusicFeaturesIntegrationTest、CatalogArtworkRematchTest |
| 切标签/播放器掉帧 | LibraryShell、LibraryTabTransition、LibraryDetailTransition、FluidAmbientCanvas | LibraryNavigationTest / LibraryDetailTransitionTest，实际真机帧率另测 |
| 首次教程/学习进度/操作定位 | TutorialCatalog、TutorialController、TutorialScreens、TutorialProgressCodec、App | TutorialProgressTest / TutorialNavigationTest；首次跳过、跨启动继续、透空点击与滚动目标 |
| 包格式/FLAC | PlainSyncPackage、平台导入 | PlainSyncPackageTest、PlaylistArchiveFormatTest |
| B 站412/匹配错误 | BilibiliClient.json/inspect/search/candidateScore | 官方验证/网络/Cookie，MusicAssistantTest |
| AI工具/截图/预算 | JvmMusicAssistant、MusicAiClient、AssistantImages、Codec、ocr/*、SongIdentityResolver | MusicAssistantTest、ScreenshotOcrTest、LocalScreenshotOcrTest、MusicFeaturesIntegrationTest |
| 推荐误删/清理失败 | recommendInternal/pending、原生 delete/activeMusicId | rollover/failure/current-playing/dislike 回归 |
| 翻译/时间轴/音频识别 | ParaformerLyricsClient、AudioLyricAlignment、GeneratedLyricsStore、原生positionMs | AudioLyricAlignmentTest / MusicAssistantTest / FloatingLyricsTest |
| 任务重试/中断 | MusicTaskJournal、JvmMusicAssistant.step/retryTask | 持久恢复、封面重试、推荐pending取消回归 |
| 后台歌词 | PlaybackService/AndroidLyricsOverlay、DesktopFloatingLyrics/Main | overlay权限/原生播放/进程/锁定/托盘 |
| 数据恢复/旧值覆盖 | PersistenceQueue、Store锁、AtomicPropertiesFile、MusicJsonStore | PersistenceQueueTest、LibraryPersistenceTest、MusicJsonStoreTest |
| 补全未命中 | MetadataEnricher、当前模型/公共源 | MetadataEnricherTest；未命中无常驻横幅 |
| 构建/签名/FFmpeg | Gradle、version.properties、scripts、CI | SDK/JDK/实际二进制/不自动升版 |
| 官网/论坛/下载发布/订阅 | website/README.md、app.py、releases.py、mailing.py | website/tests/test_site.py；桌面/手机浏览器；Ubuntu/SMTP 部署单独验证 |

当前网络 B 站公共 metadata 探测 HTTP 412；真实账号下载链未验通，没有用户 Key，未做付费 AI 服务实测。可注入音源/AI测试不等于服务商在线验证。

Android 当前设备 API32，不能代表 API36 后台调度/节电或所有国产 ROM；系统周期不是精确闹钟。后台歌词使用现有译文缓存不自动付费；纯文本无可靠同步轴。桌面悬浮需要实际图形会话/进程存活。

AI 元数据工具改曲库覆盖值，不宣称所有平台均写回原文件；新下载 MP3 转码时真实写标签。旧明文配置可能残留。睡眠定时已接入双端原生播放层；歌词偏移在本机缓存持久化，完整原文件写回 UI 仍有限。公开来源、OCR、AI事实准确性和B站源匹配仍需用户实际环境核对。

<a id="navigation"></a>

本节就是“任务到代码定位表”；先定点读实现/测试，文档不足再扩大范围，不必每轮全库分析。新增字段、平台能力、存储/权限或删除语义时同步更新上述章节。

<a id="maintenance"></a>

## 15. 维护要求

独立任务新增 docs/changes/YYYY-MM-DD-NN-主题.md，标题含日期和主要修改；写具体原因、文件/符号、前后行为、平台/数据、实际验证及限制；更新索引与当前说明，历史记录不覆盖。发布说明另外维护。

不读/输出真实签名私钥、API Key。普通构建不升版，不手改生成 BuildVersion。删除/导入测试隔离数据和音乐目录；临时夹具/截图/日志放忽略的 build，不能提交用户音频或本机工具。

### 本地 UI 设计技能（2026-10-27 核对）

当前工作区已在 `.agents/skills/material-design-3-ui/` 安装社区 [Material Design 3 UI Skill](https://github.com/skydashnet/material-design-3-ui-skill)，技能名为 `material-design-3-ui`，版本 1.1.1，来源提交为 `82783f76de4a5b08a60421cae935cf8de7ca1ee8`。入口为 `SKILL.md`，通过 `references/` 中的 13 份资料按需提供语义配色、排版、间距、组件、状态、自适应、无障碍和 M3 Expressive 指引，可与现有 `frontend-design` 及 Google Android 技能配合使用。

下一轮请求可显式指定 `$material-design-3-ui`。播放器仍按本项目原生 Compose Multiplatform 边界实现；Android 专属 API 与 Windows 共享 UI 的可用性需分别核对。本次只安装协作技能，没有改动页面或升级 Compose/Material 依赖。

技能目录沿用 `.gitignore` 的 `.agents/` 忽略规则，不随仓库提交；根目录 `skills-lock.json` 记录来源、固定 `ref`、上游 `SKILL.md` 路径和内容哈希，安装步骤与验证见 [本次记录](changes/2026-10-27-01-install-material-design-3-ui-skill.md)。新检出仓库仍需安装技能，锁文件本身不提供自动安装能力。

### Media3 音频控制与播放统计技能（2026-10-27 核对）

工作区已在 .agents/skills/media3-analytics-telemetry/ 与 .agents/skills/media3-audio-playback/ 分别安装来自 [sunnat629/android-media-pack](https://github.com/sunnat629/android-media-pack) 的专项技能，版本对齐 Media3 1.10.1。
- media3-analytics-telemetry：规范播放周期事件监听、PlaybackStats 会话度量与播放时长追踪，指导听歌时长采样与原子持久化，防止度量逻辑干扰播放核心。
- media3-audio-playback：规范 Media3 音频属性、音频焦点、生命周期及跨平台（KMP）分层，要求平台专用 API 留在 Android 源集，将状态与控制上浮至共享层。

<a id="website"></a>

## 16. 独立官网与匿名论坛（2026-10-17 核对）

实现与部署入口：[website/README.md](../website/README.md)，实现见 [官网记录](changes/2026-10-16-11-responsive-website-and-community.md)。现网 [leonard.asia](https://leonard.asia) 使用翼龙 Ubuntu 容器、Java 启动器守护 Gunicorn 26.2.0（1 进程/4 线程）与独立邮件 worker，Cloudflare Tunnel 转发到 `127.0.0.1:50034`；路径和重启方式先读 [现网运维](../website/OPERATIONS.md) 与 [核对记录](changes/2026-10-16-20-production-site-handoff.md)。仓库 Nginx/systemd 配置保留为其他主机部署方案。依赖仍为 Flask 3.1.3、Pillow 12.3.0、Python 标准库 SQLite/SMTP，没有常驻 Node 服务。

- 公共页面：`/`、`/downloads`、`/forum`、`/forum/topic/<slug>`、帖子/回复、话题申请和 `/support`。小屏使用 2×2 首页入口、电脑/手机截图切换、可展开话题列表、单列下载和表单；320/390/768/1440px 浏览器检查见本次记录。
- 页面使用纯 CSS 与合成器属性（opacity、transform）实现错峰入场、图标浮动循环、原生同源跨文档淡入淡出与视口滚动显现；提供卡片微浮动、主按钮弹性按压、讨论行指示箭头滑动与吸顶毛玻璃导航微交互；引入停留空闲预加载（Idle Prefetching）、触碰悬停即时抢跑与 Speculation Rules API，并配合客户端内存级无缝切页控制器，在国内直连时消除跨洋延迟转圈感，实现 0 毫秒秒开；严格遵守 prefers-reduced-motion 减弱动态，不支持时平稳回退，不拦截普通导航或下载。展示图使用 WebP；静态 URL 带内容摘要，匹配时公共缓存一年且不写会话 Cookie，动态页仍 no-store。见 [预加载与即时切页记录](changes/2026-10-16-29-website-idle-prefetch-and-instant-navigation.md)、[动效记录](changes/2026-10-16-27-website-dynamic-motion-effects.md) 与 [渐变与缓存记录](changes/2026-10-16-21-site-fades-and-static-cache.md)。
- 作者登录后保持同一会话即可发帖、回复；身份只由服务端认证判定。`replies` 按 `is_author DESC,id` 返回，作者回复显示作者/置顶标签；同组保持时间顺序，旧数据无迁移。见 [论坛作者记录](changes/2026-10-16-22-author-forum-pinned-replies.md)。
- 版本发布：`releases.publish` 先把 Windows EXE 和 Android APK 流式写入独立暂存目录，检查格式/版本/空间并计算 SHA-256；SQLite 事务切换两端版本后清理旧包。历史说明保留，旧安装包只链接 GitHub。失败/低磁盘/降版保留当前版本；下载支持 Range，安装包支持 CDN 边缘缓存（`Cache-Control: public, max-age=86400`）且不注入会话 Cookie，下载页提供国内加速下载镜像通道（实测 5 MB/s+）。见 [下载加速与镜像记录](changes/2026-10-16-28-website-download-speed-and-mirror.md)。Unix 下已打开下载不受旧文件 unlink 影响。
- `manage.py import-release` 从应用 `version.properties`、同版说明与实际产物导入，不改变应用版本；正式网站可由作者上传与真实版本一致的两端安装包。官网不会自动升级或重建播放器。
- `data/site.sqlite3` 保存话题、匿名帖子/回复、订阅偏好、邮件草稿/队列和发布记录，WAL + 写事务；截图压缩并去 EXIF，收款码无损编码。真实 `.env`、`secret.key`、`owner.json`、运行数据库及上传文件忽略，不进入部署源码 ZIP 或 Git。
- 作者地址由 `manage.py setup` 在服务器本地生成，只在终端显示；公共页面无入口，服务端密码散列验证、8 小时会话、CSRF、登录限速。初始没有默认密码。作者可查看数据看板（帖子/回复/话题/申请/订阅/版本）、直接新建话题、设置排序和分类、删除空话题（有帖保护）、审批用户申请、按 ID/URL 精准删帖与回复、关键词搜索帖子、置顶/取消置顶帖子、发布版本、预览/测试/群发邮件、上传收款码。
- 邮件：确认页面的 POST 才生效，版本更新与推广独立选择；推广默认不勾选，邮件独立投递、支持退订，发送前重新检查偏好。未配置 SMTP 明确关闭订阅/发送。发布安装包不会自动群发。worker 重启保留队列，但 SMTP 成功后进程中断可能少量重复送达。

2026-10-17：官网新增 `announcements.publish_announcement` 与 `manage.py publish-announcement content/0.4.6.json`，校验已发布版本及公开内容，在单一事务更新公告标题/正文，不新建邮件任务；源码打包白名单包含公告模块与公开内容。直连安装包链接带 `v=release.version`，避免新版本沿用旧缓存 URL。生产发布及实际校验见 [v0.4.6 发布记录](changes/2026-10-17-03-release-0.4.6.md)。生产站 Cloudflare Cache Rules 开启公开展示页面（`/`、`/downloads`、`/support`）边缘强缓存（2小时），跳过跨洋隧道直连就近 Anycast 节点（`cf-cache-status: HIT`），动态论坛保持 `no-store`，见 [边缘缓存优化记录](changes/2026-10-17-10-cloudflare-edge-cache-for-public-pages.md)。针对 163MB（APK）和 317MB（EXE）安装包因国际出口限速导致下载与浏览抢占卡顿，下载页集成 123 云盘国内 BGP 直链（Windows 与 Android 分别映射），辅以 GitHub 镜像与官网直连备用，实现国内数十 MB/s 免占用国际带宽下载，见 [123云盘国内高速下载记录](changes/2026-10-17-16-integrate-123pan-high-speed-downloads.md)。生产容器配置 QQ 邮箱 SMTP（465 SSL/TLS）并已完成握手与发信校验，网站论坛区域邮件订阅正式对公网开放（双重确认 double opt-in），见 [SMTP配置与订阅开放记录](changes/2026-10-17-22-configure-production-smtp-and-enable-email-subscription.md)。

验证边界：已通过生产 HTTPS 核对首页、下载、论坛、邮件订阅激活状态与两端 Range，通过现有面板客户端读取资源状态和指定源码；不等同于服务器故障恢复。首页图片仍是已有测试样本截图。网站任务不重新构建 Kotlin 应用；数据库备份使用 `manage.py backup`，同时保护密钥和上传目录。后台私有入口、密码、面板 Key 与 tunnel token 不写入文档。


<a id="music-source-update"></a>

## 17. 歌源、推荐、存储与助手增量（2026-10-17 核对，已全链路验证与Release构建）

本节描述当前代码，已完成全量单元与桌面端测试、Lint 与双端 Release 构建；具体验证与产物记录见 [验证记录](changes/2026-10-17-14-verification-gestures-feedback-and-dual-build.md)。本节记录 0.4.6 起的增量，当前工作区版本与本轮验证以本文开头和第 18 节为准。历史构建结果不代表本轮真机 UI 验证。

### 17.1 定位与协议

| 任务 | 入口 | 回归入口 |
| --- | --- | --- |
| 歌源导入/网络/JS | J/sources/MusicSourceRegistry.kt、SourceNetwork.kt；C/ui/MusicSourcesScreen.kt | DT/features/MusicSourceRegistryTest.kt |
| 音源与封面溯源 | C/model/MusicSourceModels.kt、C/ui/TrackSourceDetails.kt；J/features/MediaOriginStore.kt、JvmMusicAssistant.recordOrigin | DT/features/MusicAssistantTest.kt及端到端回归 |
| 推荐数量/理由/冷却/保存 | J/features/RecommendationPolicy.kt、JvmMusicAssistant.recommendInternal/saveToPlaylist；C/ui/MusicFeatureScreens.kt | DT/features/RecommendationPolicyTest.kt、MusicAssistantTest.kt |
| 下载与空间 | J/features/ManagedMusicStorage.kt；两端MusicAccess.adoptRestoredAudio；C/ui/MusicSourcesScreen.kt | MusicAssistantTest.cleanup/restore系列、StorageAndAssistantExtraTest |
| 歌词时间校准/布局 | J/lyrics/LyricOffsetStore.kt、LrclibLyricsRepository；C/App.kt、ui/NowPlayingOverlay.kt | StorageAndAssistantExtraTest（双向调节与范围边界持久化） |
| 本地帮助与UI动作 | C/tutorial/AssistantHelp.kt、App.kt、ui/MusicFeatureController.kt；J/features/JvmMusicAssistant.kt | MusicAssistantTest、StorageAndAssistantExtraTest（播放与导航动作） |

新增导航 Sources、Storage，入口在工具/设置；在线搜索下载主入口已统一到曲库搜索，Sources 专注歌源管理。内置网易云音乐、酷我音乐、酷狗音乐等开箱即用公网歌源，双端启动自动预置，支持界面一键“恢复内置歌源”。歌源支持本地JS、公网JS/订阅JSON、启停、卸载、搜索、下载到目标歌单。MusicFree核心方法 search/getMediaSource/getMusicInfo/getLyric，及部分歌单/榜单接口；仅基础axios/fetch/qs与crypto-js，不能宣称兼容全部插件/Node。依赖QuickJS Kotlin1.0.10（jvmMain，Android/Windows原生组件），CryptoJS4.2.0从JVM resources两端打包，许可见THIRD_PARTY。脚本不持有宿主反射/文件/模型凭据；安装检查禁止联网，单次32MiB内存、512KiB栈、5秒执行限制/25秒协程期限。仅公网HTTP/HTTPS（支持80/443及常用8080/8443端口），检查实际DNS解析地址，拒绝内网/用户信息；接口2MB、下载音频512MiB、封面传输10MiB（写入前另限10000000字节），最多5次安全跳转重验且跨主机不传原请求头。无自动绕过登录/付费限制。

下载优先本地→启用歌源轮询（试听片段与异常时长自动回退）→可选B站。源详情与目录时长是证据，非真伪鉴定；试听标记/明显非录音室关键词拒绝，时长异常不进推荐并尝试下一源。B站不再给MV加分，可按UID限制自动候选。兼容时复制原音轨，保留m4a/flac/ogg等扩展与MIME；关闭保留时转MP3。平台读取实际码率/采样率/声道，界面不显示固定320K、不据此认证母带。未内置未经核验的“保音质”账号。

每首歌优先自身图片，再读取音频内嵌图；同ID歌单快照可复用图片，有目录证据且无歧义的同歌手/同专辑图可复用。歌单头图不意味着成员都有图。支持批量为缺图曲目匹配目录封面；来源区分目录、歌源标注、视频缩略图、自选、未知。目录匹配未保证具体再版/合辑版本一致。

### 17.2 数据、旧数据与清理

新增JSON根字段 musicSources、sourceOrigins、sourceSongs、mediaOrigins、ownedAudio、recommendationHistory。脚本存在music-assistant/sources/，以平台名hash为文件名；源条目原始JSON用于重新取音频/歌词。卸载不删下载。ownedAudio只正向登记应用下载（旧videoOrigins需落在安全目录才迁入），包含文件/Track快照/永久保存意图/时间/evicted。手动下载默认永久保留；推荐默认临时，收藏/自建歌单/当前播放/共享文件另行保护。临时预算超限停止，不自动清理已保存曲目；预算在源文件下载后检查，磁盘不足场景仍需完善验证。

“下载与空间”统计托管音频、可选SHA精确重复、清理预览；显式释放已保存音频只清文件，曲目与收藏/歌单留下并置空sourceUri。恢复按原来源重下载，沿用旧ID/关系；需原源仍可用。两端更新歌单快照sourceUri；桌面加载空source必须有有效旧ID。路径规范化/真实根检查拒绝外部文件、逃逸符号链接。普通曲库主动删除仍沿用原语义。曲库/歌单/物理删除不是跨文件事务，写失败、取消、崩溃中断恢复为交接重点；未验证前不得宣称文件安全已通过。

设置新增 recommendationMode=Source（可选Ai）、recommendationCooldownDays=7（1–365）、temporaryStorageMb=512（64–8192）、preserveOriginalAudio=true、bilibiliFallback=true、preferredBilibiliUids=""。Codec缺省兼容旧配置。推荐身份为NFKC歌手+歌名，保留版本限定文字，8份重复文件只计1首；真实元数据不足仍可能误合并/漏合并。历史独立于文件保存365天，默认滚动7天不重推；批次createdDate按Asia/Shanghai显示。普通推荐/歌源搜索/帮助不用模型；AI模式仍走预算。推荐支持单/多/全选保存已有/新建自建歌单。

歌词偏移独立保存在 lyrics-cache/offsets/<TrackID hash>.json，原子写、±600000ms，读取本地/在线/生成歌词都应用，不修改原文件/音频标签，不改变歌单文件格式。整体偏移无法修正不同版本剪辑或渐进漂移。

### 17.3 助手和播放器交互

新增read_help、open_page、start_tutorial、play_tracks、set_playing、seek、next、previous、track_sources、inspect_storage、search_sources、download_source。下载只用先前实际源搜索返回的条目ID。帮助关键词检索复用TutorialCatalog和新功能说明，无模型请求。摘要按歌曲去重最多120首，带当前页面/播放状态，工具仍可查询大库。

UI动作通过MusicAssistantState.actions内存队列顺序交App执行并确认，不持久化、不重启重放；工具返回“请求”，不伪称播放完成。没有shell、任意文件或系统控制；原删除授权要求保留。前台可根据来源恢复离线曲目再播放，失败与切后台场景需端到端测试。

手机播放器优先稳定标题、歌词/翻译层级、前奏状态、明确校准方向。歌词/队列页将音量/变速等次要设置放入弹窗，封面页保留操作；去除歌词行缩放与元数据整块滑动。尚未做设备或截图验证。


### 17.4 页面左右滑动（2026-10-17，已单元验证与构建）

C/ui/HorizontalPageSwipe.kt在内容区监听横向拖动，支持实时跟手位移（graphicsLayer）、首末页橡皮筋阻尼拉伸（0.32f）、未达阈值弹簧回弹与流体换页过渡；子控件先消费，明显竖向/多指/起点左右24dp边缘退出；达到距离阈值抬手切一页，不循环。LibraryShell的主导航音乐库/助手/工具接入；曲库内容区优先切歌曲/歌单/专辑/歌手，边界交主导航。详情不另建任意线性顺序。NowPlayingOverlay.PaneContent通过onPaneChange切封面/歌词/队列，按钮同样可用。教程中禁用手势，曲库多选禁用栏目手势。曲库采用LibraryTabTransition升级为120dp/280ms流体减速入场与0.15~1.0渐变，并保持提升的滚动状态。Windows鼠标/触屏拖动可用，未接横向滚轮。

新增AT/HorizontalPageSwipeTest与DT/HorizontalPageSwipeUnitTest（滑动方向、距离/速度阈值、首尾页不回环及特性清单验证），扩充AT/LibraryNavigationTest。真机边缘与触控需在设备上进一步体验。见[滑动记录](changes/2026-10-17-11-horizontal-page-swipe.md)。

### 17.5 点赞、踩与评论（2026-10-17，已回归验证）

C/model/RecommendationFeedback.kt新增互斥RecommendationVote.Neutral/Like/Dislike与个人评论；C/platform/MusicAssistantServices新增feedbackFor/setRecommendationVote/saveRecommendationComment，J/features/JvmMusicAssistant唯一实现，双端共用。推荐卡片提供可撤销点赞/踩和500字符评论编辑/清空删除；点赞独立于收藏，普通踩也不删除文件。界面将收藏、反馈、歌曲信息中的“不喜欢并删除”分别标注。未保存的点赞曲仍遵从临时清理，偏好记录保留。

JSON根字段recommendationFeedback按RecommendationPolicy的NFKC歌手+歌名存title/artist/album/vote/comment/updatedAt；旧数据空兼容，同曲多文件共享。只改变一个字段时保留另一个，两者清空才移除。音频清理不删反馈，歌单文件不携带这些个人偏好；本轮已有独立推荐历史与个人反馈页；推荐详情从本次更新开始积累，旧反馈直接可读，旧哈希冷却记录无法凭空还原名称。点赞可以撤销旧同曲拒绝，原删除操作也写入踩及原因。

J/features/RecommendationFeedbackPolicy.kt负责本地种子与排序：收藏种子8、自建歌单4、点赞3，负反馈降低同歌手/专辑权重，不整位封禁；踩过的原曲过滤。评论采用有限的显式风格词/否定识别与歌手/专辑/标题元数据匹配，最多四个源搜索种子中给正向评论词留一位，不调用模型；不承诺理解任意评论或推断未提供的流派。AI精选与助手上下文仅附最近24条、每条评论160字符摘要，评论作数据而非指令。点赞同歌手可生成有证据的推荐理由；原历史冷却和保存保护仍执行。

新增DT/features/RecommendationFeedbackPolicyTest与MusicAssistantTest反馈回归（去重、撤销/评论独立、上限、不改收藏/文件、不调用模型、换批后保留及下次AI输入），均已全量运行通过。见[反馈记录](changes/2026-10-17-12-recommendation-votes-and-comments.md)。

### 17.6 关于页当前版本更新（2026-10-17，已生成并构建验证）

composeApp/build.gradle.kts的generateAppVersion继续只从version.properties取版本，并以docs/RELEASE_NOTES-<版本>.md为输入，提取标题日期与“## 主要更新”下单行“- ”条目，生成APP_RELEASE_DATE/APP_RELEASE_HIGHLIGHTS。C/ui/ToolsScreens.AboutScreen在应用信息后离线显示当前版本日期/更新列表，提供对应GitHub版本链接；无对应条目时明确提示，不拿其他版本或未发布工作区改动填充。不要手改BuildVersion.kt；当前0.4.6对应5项既有发布更新，未来正式发布时维护其版本发布文档。

已执行DT/HorizontalPageSwipeUnitTest与generateAppVersion任务，确认5项既有版本更新特性已正确注入生成类并在Release构建中打包。见[关于更新记录](changes/2026-10-17-13-about-version-highlights.md)。


<a id="user-experience-update"></a>

## 18. 用户体验与页面顶边覆盖（2026-10-17 核对，已正式发布至 0.5.0 / 33）

本节记录 0.5.0 / 33 发布时的体验基线及当时的 Android Release 签名 APK、Windows EXE 与官网部署；当前工作区版本见文首，2026-10-27 的 UI 与动效增量见第 11 节。历史执行命令、测试数量及设备边界见 [0.5.0 发布记录](changes/2026-10-17-33-release-0.5.0.md) 与 [Antigravity 交接](HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)。

| 使用流程 | 实现与数据 | 测试／限制 |
| --- | --- | --- |
| 统一搜索与试听 | C/ui/OnlineSongSearch、LibraryShell.DiscoverScreen；J/JvmMusicAssistant.prepareSourcePreview；两端独立预览播放器。保留查询，本地／在线切换；临时音频最多 64 MiB，试听最多 30 秒，离页释放 | MusicSourceRegistryTest、MusicAssistantTest；真实歌源与解码待设备核对，暂停主播放后不自动恢复 |
| 截图核对 | C/ui/ScreenshotImportDialog；ScreenshotImportDraft 只在内存。识别后可改歌单／歌曲、全选／勾选；confirmScreenshotImport 校验 ID 并消费一次，确认后逐首落盘 | 缓存、取消、编辑、过期与重复确认回归；进程终止后需重开预览 |
| 任务汇总 | MusicTaskEntry.parentId 可选；MusicTaskJournal 保留最近 100 条及当前父链；协程 asContextElement 管理并发步骤；groupMusicTasks 兼容旧／孤立／循环记录 | MusicTaskGroupsTest、封面重试回归；免费失败媒体批量重试，收费同步歌词仅单独确认，聊天／删除不重放 |
| 离线恢复 | MusicAssistantState.releasedDownloadIds 来自 ownedAudio.evicted；TrackRow 核实文件可读性。App.startTrack 可取消检查，保留 PlaybackStateStore 的代次；两端原生选择器重新定位 URI／MIME，saveReplacementAudio 经 AndroidMusicAccess.update／DesktopLibraryStore.replaceAudio 显式持久化旧 ID 的新来源 | 不删原文件、不写标签；收藏、ID、歌单保留；真实授权撤销与选择器待点击测试 |
| 历史反馈 | RecommendationHistory 导航、RecommendationHistoryScreen；JSON recommendationHistoryDetails 保存名称、专辑和最近时间，365 天裁剪；旧反馈仍可查看 | 旧哈希历史不伪造歌名；点赞不保留文件，临时／已保存单独提示，撤销踩同时解除旧拒绝 |
| 睡眠定时 | J/playback/PlaybackSleepTimer；Android PlaybackService 经 SLEEP_TIMER 命令持有，ExoPlayer.pauseAtEndOfMediaItems 与曲终事件抑制；Desktop 原生结束回调先消费定时 | 单调时钟，后台／托盘继续，暂停仍计时，完全退出不恢复；PlaybackSleepTimerTest，后台和循环实测待完成 |
| Windows 键盘 | App Ctrl+F、空格、Ctrl+←／→、Esc；焦点控件先处理文字和光标；KeyboardBackNavigation 分发有效页面返回；键盘导航不播放转场 | 设置／本地帮助可查快捷键；GUI、输入法与焦点细节待实际验证 |
| 自动推荐网络 | recommendationsUnmeteredOnly 默认 false；Android WorkManager.UNMETERED，前台／共享下载前再查；Windows 只读 NetworkCostType，缓存 30 秒、超时 3 秒 | 无新权限；自动等待，手动更新按显式动作联网；Windows 系统查询读通，计费切换与 ROM 调度待实测 |
| 顶边闪烁 | App 根 Surface、DestinationContent 固定 Canvas 视口和每页背景；using null；WeatherAppearance 主题键控系统栏更新；Android 主题窗口底色与对比度遮罩 | 新增真实 LibraryShell 浅／深色快速切页像素检查，测试源码已编译，未在设备执行，不宣称所有黑边已消失 |

两端共享 MusicAssistantServices 新增草稿确认／丢弃、失败批量重试和试听准备／清理；唯一 JVM 实现已接通。PlatformServices 增加原生试听、音频可读检查、替换文件选择、睡眠状态和网络条件接口，并检查 Android／Windows 实现。除新网络偏好、可选任务父级和可回看历史详情外，不改变现有同步包、音频转换、凭据保护、删除与清理所有权规则。
