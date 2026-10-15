# 2026-10-15 — Resonance 详细开发说明：曲库、音乐助手、推荐与歌词

源码核对日期：2026-10-15（Asia/Shanghai）；功能改造基准 HEAD `cdf7ee3`，当前版本为 `0.4.0 / 23`，发布标记 `v0.4.0`。功能回归与限制见 [深度优化验证](changes/2026-10-15-12-verification-and-handoff.md)，本次正式 APK 的构建、签名与交付见 [发布记录](changes/2026-10-15-13-release-0.4.0-android.md) 和 [版本说明](RELEASE_NOTES-0.4.0.md)。

不同 AI 接手时先读本页相关章节与变更记录，再读必要源码。每个独立功能/修复必须新增带日期和具体内容的文档，见 [协作流程](AI-WORKFLOW.md)。

## 目录

- [范围](#scope)、[工程](#architecture)、[模块入口](#modules)
- [状态与并发](#startup)、[模型与兼容](#models)
- [扫描/转换/歌单](#library)、[播放与音量](#playback)、[歌词/翻译/补全](#lyrics)
- [歌单文件](#sync)、[存储/凭据](#storage)、[UI/封面/悬浮歌词](#ui)
- [构建与发布](#build)、[AI/截图/推荐](#assistant)、[任务定位与限制](#gaps)、[维护](#maintenance)

<a id="scope"></a>

## 1. 范围与当前能力

Kotlin / Compose Multiplatform 原生工程，目标 Android、Windows。没有 Web 前端、自有服务器、云端音乐库或账号系统。音频保存在本机；歌词、补全、天气、AI、B 站会联网。扫描/转换保留输入；主动删除、不喜欢、临时推荐清理会删除相关音频。

| 功能 | Android | Windows | 边界 |
| --- | --- | --- | --- |
| 本地库、搜索、收藏、歌单、专辑/歌手、多选 | 已接入 | 已接入 | Discover 是本地搜索，支持拼音首字母 |
| KGMA v3 / slot 1 转 MP3 | 已接入 | 已接入 | 进度、取消、保留源文件；支持载荷转 320k MP3 |
| KGG v5 转 MP3 | 不支持，明确提示 | 已接入 | 需要本机酷狗数据库中的匹配密钥 |
| 播放/队列/随机/循环/变速/恢复 | Media3 服务 | FFmpeg/Java Sound | 原生后台播放与快照恢复 |
| 自动音量平衡 | 单开关 | 单开关 | 全局音量、整曲分析/缓存，独立帮助页 |
| 歌词与双向翻译 | 已接入 | 已接入 | 本地 LRC/KRC、LRCLIB、显式 AI、任意目标语言和双语模式 |
| 后台悬浮歌词 | 系统 overlay | 透明置顶窗口 | 拖动、字号、锁定/点击穿透，系统权限/进程存活限制 |
| 歌单文件导出/导入 | 已接入 | 已接入 | .resonance 明文 ZIP，含音频、封面、元数据和关系 |
| B 站音频导入 | 已接入 | 已接入 | 链接/BV/分 P，编辑歌名歌手专辑与封面，转码入库 |
| 多模型音乐助手 | 已接入 | 已接入 | 千问、DeepSeek、豆包、自定义 HTTPS 接口，文本/视觉角色分开 |
| 长截图歌单识别 | 已接入 | 已接入 | 区域分片、视觉 OCR、本地匹配、原版候选搜索、下载与歌单整理 |
| 每日推荐 | 前台/后台可选 | 前台/托盘后台可选 | 自定义间隔/数量；临时曲目清理；收藏/入自建歌单保留 |
| 元数据/封面/歌词补全 | 已接入 | 已接入 | 公共来源优先，显式 AI 兜底；歌曲页无常驻缺失计数 |
| 歌曲信息编辑 | 曲库覆盖值 | 原有入口可写标签 | B 站新 MP3 两端都真实写入标签与封面 |
| 设备/LAN/二维码同步、酷狗歌单链接 | 已删除 | 已删除 | 不删除历史歌单；酷狗补全和 KGG 密钥读取仍在 |
| 睡眠定时、完整歌词偏移/写回 UI | 未完整接入 | 未完整接入 | 不能凭已有接口或组件宣称主流程完成 |

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

只有 `:composeApp` 一个应用模块。源码简写 C/J/A/D 均指 `composeApp/src/<源集>/kotlin/com/resonance/player/`。`scripts` 管理构建/显式升版/产物，`docs` 管理当前基线与逐次记录。

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
| C/ui/LibraryShell.kt、LibraryDetailTransition.kt | 响应式曲库/搜索/详情、多选、封面和迷你播放器；标签单树切换，详情独立过渡 |
| C/ui/NowPlayingOverlay.kt | 完整播放器、歌词/队列、单个平衡开关、翻译/显示、悬浮开关与编辑信息 |
| C/ui/MusicFeatureController.kt、MusicFeatureScreens.kt | 共享设置、助手、B 站表单、音量帮助、任务进度、拒绝原因 |
| C/ui/ToolsScreens.kt、SyncPackageDialog.kt、BatchEnrichDialog.kt | 导入、歌单文件、设置、导出范围、补全选项/报告 |
| C/platform/ArtworkMemoryCache.kt、SyncArtwork.kt | 位图 LRU/并发限制/同路径合并，曲库与歌单封面回退 |
| C/design/*、ui/AppearanceSettings.kt | 主题、天气配色、减少动态效果、外观设置 |
| [J/features/JvmMusicAssistant.kt](../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | 音乐工具 Agent、OCR、视频入库、翻译、推荐生命周期、预算/稳定上下文 |
| J/ai/MusicAiClient.kt | MusicAiTransport 可注入接口，HTTPS Chat Completions、tools、图片、用量、取消/超时 |
| J/features/MusicJsonStore.kt、MusicFeatureCodec.kt | 原子 JSON/备份恢复、设置/快照编解码、OCR/译文磁盘缓存 |
| J/features/MusicLibraryAccess.kt | AI/Worker 所需的原生曲库读写/删除/收藏/转码桥接 |
| [J/media/BilibiliClient.kt](../composeApp/src/jvmMain/kotlin/com/resonance/player/media/BilibiliClient.kt) | 公共 metadata、WBI、候选打分、音轨/CDN、封面；MusicVideoSource 可注入 |
| J/media/Mp3Import.kt | FFmpeg 参数数组、320k MP3、真实标签/封面、流式 hash |
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
| MusicFeatureSettings | 连接列表/文本视觉角色、推荐开关/后台/间隔/数量、月预算、悬浮开关/锁/字号、翻译语言/显示、B 站 Cookie |
| MusicAssistantState | 最近 40 条消息、任务状态/进度、月输入/缓存/输出 Token、推荐批 |
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
| music-assistant/music-assistant.json + .bak | 新连接、消息、月用量、批次/pending、拒绝原因、来源映射 |
| music-assistant/cache/、artwork/ | hash 命名 OCR/译文、AI 封面 |
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
| 截图 OCR / 翻译 | 用户图片分片 / 歌词文本及目标语言 |
| B 站 | 搜索词、视频 ID、WBI、可选自有 Cookie |
| 天气 | 城市、粗坐标，现有天气/IP 定位接口 |

AI 仅 HTTPS，无携 Key 重定向；15 秒连接、90 秒读取、有界响应和取消断开。B 站限制页面/CDN 主机、音频 1 GiB/封面 10 MB、下载超时/取消；官方登录/验证/412 要报真实原因，不无限重试或绕过限制。

Android 权限：音频/SAF、网络、粗定位、通知、媒体前台服务；新增 SYSTEM_ALERT_WINDOW 和 WorkManager dataSync 前台服务，无管理所有文件权限。悬浮权限由系统页面授予；拒绝不会阻断播放。

<a id="ui"></a>

## 11. UI、封面与悬浮歌词

LibraryShell：<720 dp 底栏、720–1099 rail、>=1100 侧栏。LibraryDestination 新增 PlaylistArchive/Assistant/AudioHelp；主工具入口可进入歌单文件、助手、设置。

曲库常用标签直接切换单棵 Lazy 树，避免 AnimatedContent 双树/透明滑动；详情仍用 LibraryDetailTransition 的裁剪和不透明过渡底层。保留稳定 ID、减少动态效果，别再给高频标签添加重叠模糊层。

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

最多16轮/单轮20个工具，结果回传后再答复。无效 ID/工具报错，不伪称完成；否定删除、只删歌单等有回归。音乐任务可取消，已完成入库保留。

### B 站链路

手动：链接/BV/短链接/分 P → metadata → 编辑歌名/歌手/专辑/封面/目标歌单 → DASH音轨或单段 fallback → staging转 MP3 → 原生入库/关系。自动按 SongRequest 完成相同链路。来源 bvid:cid 去重。

candidateScore 要求标题命中、偏好歌手/官方/原版，强烈扣分现场/live/翻唱/cover/伴奏/混音/变速/合集。无可信候选就报告失败，不随便替换；规则不保证真伪。B 站需要官方登录/验证时显示错误，可设置自有 Cookie。

### 截图流水线与缓存

图片选择 → 区域分片 → 内容 hash 查 OCR 缓存 → 视觉返回歌单名/歌名/歌手 JSON → 重叠去重 → 确定 ID 截图歌单 → 本地匹配 → B 站搜索/下载 → 逐曲保存。单歌单最多300首，一次20歌单，错误列出具体歌曲。当前是用户发送后直接导入，不等待逐首批准。

固定 SYSTEM/工具 schema、稳定排序库摘要在消息前缀，聊天在后；摘要含最多500首、歌手统计和歌单，工具可搜索大库。聊天最多40条落盘、取最近16条请求；OCR/翻译持久缓存避免重复成本。

按 YearMonth 累计输入/缓存命中/输出 Token；统一请求包括连接测试计预算。默认月上限1000000，可配置，前置估算含图片和输出；这不是人民币账单保证，供应商失败请求/图片计费可能不同。云端缓存时效由供应商控制，不承诺隔天命中，也不付费发心跳保活。

### 推荐生命周期

默认关闭，1–168小时/1–30首；前台到期生成或允许后台。Android WorkManager 受节电/网络调度限制，下载有 dataSync 前台通知；Windows 托盘每分钟检查，完全退出后不运行。

按曲库/歌手分布/收藏/拒绝理由找真实候选，过滤已有/拒绝歌曲并自动下载。至少一首成功才提交新批；总失败保留旧批/时间。每首完成写 recommendationPending，中断待提交音频在下次成功批统一处理。

新批成功时，未收藏且未在自建歌单的旧曲物理删除；当前播放延后。保护按当前收藏/成员关系判断，取消收藏并移出歌单后不永久保护；retained 兼容记录不是清理依据。

不喜欢立即取消收藏、删音频、移出所有歌单，即使先前保存也执行；可选原因弹窗/跳过。删除失败显示实际错误。原因参与下次推荐，不自动封禁整位歌手。

<a id="gaps"></a>

## 14. 任务定位与已知限制

| 任务 | 先读 | 回归/检查 |
| --- | --- | --- |
| 音量、快速切歌竞争 | PlaybackStateStore、PlaybackService、平台响度 | PlaybackStateStoreTest、AudioSessionIntegrationTest |
| 转换慢/取消 | ImportControl、Scanner/scanDocumentTree、两端 FFmpeg | DesktopManagedMp3ConverterTest，源文件/staging/阶段 |
| 列表封面 | DynamicArtwork、audioArtwork、ArtworkMemoryCache、SyncArtwork | MusicFeaturesIntegrationTest、CatalogArtworkRematchTest |
| 切标签/详情撕裂 | LibraryShell、LibraryDetailTransition、Lazy key | LibraryDetailTransitionTest，实际大库帧率另测 |
| 包格式/FLAC | PlainSyncPackage、平台导入 | PlainSyncPackageTest、PlaylistArchiveFormatTest |
| B 站412/匹配错误 | BilibiliClient.json/inspect/search/candidateScore | 官方验证/网络/Cookie，MusicAssistantTest |
| AI工具/截图/预算 | JvmMusicAssistant、MusicAiClient、AssistantImages、Codec | MusicAssistantTest、MusicFeaturesIntegrationTest |
| 推荐误删/清理失败 | recommendInternal/pending、原生 delete/activeMusicId | rollover/failure/current-playing/dislike 回归 |
| 翻译/时间轴 | translate/cachedTranslation、LyricDisplay、FloatingLyricsHost | MusicAssistantTest、FloatingLyricsTest |
| 后台歌词 | PlaybackService/AndroidLyricsOverlay、DesktopFloatingLyrics/Main | overlay权限/原生播放/进程/锁定/托盘 |
| 数据恢复/旧值覆盖 | PersistenceQueue、Store锁、AtomicPropertiesFile、MusicJsonStore | PersistenceQueueTest、LibraryPersistenceTest、MusicJsonStoreTest |
| 补全未命中 | MetadataEnricher、当前模型/公共源 | MetadataEnricherTest；未命中无常驻横幅 |
| 构建/签名/FFmpeg | Gradle、version.properties、scripts、CI | SDK/JDK/实际二进制/不自动升版 |

当前网络 B 站公共 metadata 探测 HTTP 412；真实账号下载链未验通，没有用户 Key，未做付费 AI 服务实测。可注入音源/AI测试不等于服务商在线验证。

Android 当前设备 API32，不能代表 API36 后台调度/节电或所有国产 ROM；系统周期不是精确闹钟。后台歌词使用现有译文缓存不自动付费；纯文本无可靠同步轴。桌面悬浮需要实际图形会话/进程存活。

AI 元数据工具改曲库覆盖值，不宣称所有平台均写回原文件；新下载 MP3 转码时真实写标签。旧明文配置可能残留。睡眠/完整歌词偏移写回 UI 本轮未扩展。公开来源、OCR、AI事实准确性和B站源匹配仍需用户实际环境核对。

<a id="navigation"></a>

本节就是“任务到代码定位表”；先定点读实现/测试，文档不足再扩大范围，不必每轮全库分析。新增字段、平台能力、存储/权限或删除语义时同步更新上述章节。

<a id="maintenance"></a>

## 15. 维护要求

独立任务新增 docs/changes/YYYY-MM-DD-NN-主题.md，标题含日期和主要修改；写具体原因、文件/符号、前后行为、平台/数据、实际验证及限制；更新索引与当前说明，历史记录不覆盖。发布说明另外维护。

不读/输出真实签名私钥、API Key。普通构建不升版，不手改生成 BuildVersion。删除/导入测试隔离数据和音乐目录；临时夹具/截图/日志放忽略的 build，不能提交用户音频或本机工具。
