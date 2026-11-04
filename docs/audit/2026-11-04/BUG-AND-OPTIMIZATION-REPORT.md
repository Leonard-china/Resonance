# 2026-11-04 — Resonance Bug 与优化排查报告

本次发现 **13 项问题：6 项 P1、7 项 P2**，另列 **9 项优化建议**。优先处理音频导入原子性、临时离线曲目的记录丢失、AI 删除授权和版本去重规则。现有自动测试通过，仍有这些测试没有覆盖的异常输入和跨模块问题。本次只交付审计、复现实验和文档勘误，**问题尚未修复**。

## 审计对象与证据边界

- 日期：2026-11-04，Asia/Shanghai；版本 `0.6.5 / 39`；Git HEAD `fdf4735`。
- 对象为当前未提交工作区，并非仅 HEAD。开始时 `git status --short` 有 372 条；保留全部既有修改。39 个重点源码文件的摘要见 [source-sha256.json](source-sha256.json)，不是声称只有这些文件接受过检查。
- 范围：共享状态/持久队列、Android 播放服务/权限/生命周期、Windows/Linux 共用桌面播放与资料存储、转换/同步包、推荐/助手/清理、一起听传输、原生 UI，以及独立官网和 Python 服务。
- 方法：按开发文档定位，审阅实现和既有回归，重新执行桌面全量/Lint/Python 测试，使用临时目录、合成文件、模拟服务做隔离复现。没有读取真实签名配置/API Key、用户音频或线上数据库；没有发布、安装或修复产品。
- “隔离复现”表示直接运行当前编译类或实际 JS 函数；“源码确认”表示调用链与条件可定位，但没有设备端复现。底层抛异常不等于已经观察到手机崩溃，二者在各条中分别说明。
- 没有发现可据现有证据定为 P0 的项目；这不构成“无其他 Bug”的保证。P1 为数据/删除授权/可用性的重要缺陷，P2 为受条件限制的功能缺陷和健壮性问题。

## 问题总表

| 编号 | 优先级 | 问题 | 主要平台 | 证据 |
| --- | --- | --- | --- | --- |
| B01 | P1 | 导入失败时可能已经覆盖原有托管音频 | Android / Windows / Linux | 隔离复现 |
| B02 | P1 | 暂时不可读的桌面曲目在后续保存时丢失记录 | Windows / Linux | 隔离复现 |
| B03 | P1 | 部分桌面设置读写绕过有效备份恢复 | Windows / Linux | 隔离复现 |
| B04 | P1 | 询问、解释及“不要删文件”会误开启 AI 音频删除权限 | 共享 JVM | 授权函数隔离复现 |
| B05 | P1 | 统计损坏可让歌曲详情的同步读取抛出未处理异常 | Android / Windows / Linux | 存储异常复现 + UI 调用链 |
| B06 | P1 | 去重把现场等不同录音预选为可删除的冗余文件 | 共享 UI | 算法隔离复现 |
| B07 | P2 | 智能管家选择多首清理，只保留最后一首的确认 | 共享 UI | 源码确认 |
| B08 | P2 | BPM 意图只显示标签，没有参与本地结果匹配 | 共享 UI | 解析复现 + 匹配源码 |
| B09 | P2 | 歌单包内引用数量缺少上限，极小输入可放大工作量 | 共享 JVM | 公开导入入口隔离复现 |
| B10 | P2 | 删除单曲会直接删除其他歌曲共用的专辑封面 | Android / Windows / Linux | 源码确认 |
| B11 | P2 | 桌面端在首段 PCM 到达之前就清除缓冲状态 | Windows / Linux | 源码确认 |
| B12 | P2 | 官网连续切页时，迟到请求会覆盖最后点击的页面 | 独立官网 | 实际 JS 函数隔离复现 |
| B13 | P2 | Android 部分设置写盘失败仍正常返回成功 | Android | 源码确认 |

## P1 问题与修复验收建议

### B01 — 歌单包提交发生在完整校验之前

- **位置**：[PlainSyncPackage.kt](../../../composeApp/src/jvmMain/kotlin/com/resonance/player/sync/PlainSyncPackage.kt)，`import` 第 64–67 行、`commitTracks` 第 196 行、`moveIntoPlace` 第 303 行。
- **触发**：音频及 SHA-256 合法，歌单部分缺少必填字段，或后续文件移动失败。
- **原因**：音频先以 `REPLACE_EXISTING` 移入正式目录，再调用 `readPlaylists`；没有文件级事务回滚。单次文件原子移动不等于整包原子提交。
- **实测**：目标文件预置 `synthetic original`；导入缺少 `playlist.0.name` 的合成包。导入抛“同步清单缺少 playlist.0.name”，目标已经变成 `synthetic replacement`。原文件名按当前 `safeId` 算法生成，没有接触真实歌曲。
- **影响**：界面显示同步失败，正式目录却已改变；原曲目、封面和曲库记录可能不一致。
- **建议**：在 staging 内校验完整清单、唯一 ID、所有目标及资源预算；提交时保留原文件并记录回滚日志，元数据持久化成功后再回收备份。
- **验收**：缺少歌单名/重复 ID/第二个文件提交失败/元数据保存失败，均应保持原音频字节和原曲库不变。

### B02 — 临时离线会变成永久丢失曲目记录

- **位置**：[DesktopPlatformServices.kt](../../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt)，`DesktopLibraryStore.load` 第 875 行，`merge`、`updateTracks`、`setFavorite` 第 922、934、969 行附近。
- **触发**：移动盘断开、网络盘短暂不可用或文件暂时改名，然后对另一个曲目收藏/编辑/导入。
- **原因**：`load()` 丢弃 `!Files.isRegularFile` 的记录；后续读改写以这个过滤后的集合覆盖 `library.properties`。
- **实测**：保存 A/B 两条记录，临时移除 A 文件后读取只剩 B；收藏 B，再恢复 A 文件，曲库仍只有 B。
- **影响**：A 的曲库身份、收藏等信息从主记录中消失。已有歌单可能保有快照，不能据此保证全部曲目信息可恢复。
- **建议**：持久层加载完整目录记录，单独计算文件可用状态；仅显式删除操作才移除条目。
- **验收**：断盘期间修改 B，恢复盘后 A 的 ID/收藏/歌单和历史关联全部保留；兼容旧存储。

### B03 — 备份恢复只对部分设置入口生效

- **位置**：同一文件的 `DesktopLibraryStore.readUiValue` 第 834 行与 `loadThemeMode` 第 1048 行、`loadLastSelectedPlaylistId`、`loadPlaybackMode` 等。
- **原因**：通用读取使用 `AtomicPropertiesFile.read`，部分专用方法仍直接 `Properties.load`；专用写入也直接读取旧主文件。
- **实测**：创建有效 `.bak`，将主文件写成无效 Unicode 转义。`loadThemeMode()` 抛 `IllegalArgumentException`；同一状态下 `readUiValue("app.themeMode")` 能恢复出备份中的 `Dark`。
- **影响**：相同损坏状态会因入口不同而得到恢复或失败。启动阶段还会读取上次歌单/播放模式，可能表现为默认设置或曲库加载失败。
- **建议**：所有 UI 设置读改写统一经过现有恢复入口和串行事务，禁止坏主文件进入下一次备份。
- **验收**：对主题、播放模式、上次歌单、歌词目录分别注入坏主文件，验证恢复后继续写入仍保留其他键。

### B04 — 音频删除授权不能依靠关键词命中

- **位置**：[JvmMusicAssistant.kt](../../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt)，`allowsAudioDeletion` 第 1351 行、聊天执行第 921/935 行、`delete_tracks` 第 1334 行。
- **实测**：以下输入均返回 `true`：`如何删除歌曲？`、`解释一下删除音频有什么风险`、`只删除歌单里的歌曲，不删除音频文件`。
- **原因**：只要匹配“删除”等词就授权；否定规则没有覆盖“不删除”，也没有区分询问、引用、说明和执行指令。
- **影响边界**：并非这些话一定会删文件；但是模型一旦生成 `delete_tracks`，本地执行层会放行，且没有该工具专用的文件删除确认。系统提示词不能替代执行层授权。
- **建议**：模型提出具体曲目及删除范围后，由本地 UI 确认一次性操作；明确分开“从歌单移除”与“删除音频文件”，确认绑定本轮请求和曲目 ID。
- **验收**：上述输入、引用的歌词/歌名、英文否定、混合否定均不得获得文件删除能力；用户确认具体文件后才允许执行。

### B05 — 损坏统计的保护没有贯穿到歌曲详情

- **位置**：[NowPlayingOverlay.kt](../../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) 第 474 行；[ListeningStatsRepository.kt](../../../composeApp/src/jvmMain/kotlin/com/resonance/player/playback/ListeningStatsRepository.kt) 第 16 行；[ListeningStatsStore.kt](../../../composeApp/src/jvmMain/kotlin/com/resonance/player/playback/ListeningStatsStore.kt) 的初始化/`loadFromDisk`；两端 `getTrackListeningStats`。
- **触发**：统计主文件损坏，且没有有效备份，再打开“歌曲与来源”。
- **实测与限制**：存储构造在合成坏文件上抛 `IllegalStateException`。UI 在 composition 内直接调用 getter，缺少像品味页那样的 `runCatching`。本次没有在手机上制造损坏并观察崩溃。
- **影响**：异常可以逃出详情 composition；同时首次读取会在 UI 线程解析整个统计文件。后台写入错误流无法保护这条直接读取路径。
- **建议**：IO 初始化后发布内存状态；详情以可恢复错误状态展示。保留“坏数据不得覆盖”的约束，不能靠清空统计消除异常。
- **验收**：坏主文件/坏备份/读权限失败时仍可播放和关闭详情，看到统计不可用提示，原始文件不变。

### B06 — 不同版本被当作冗余音频预选

- **位置**：[SmartLibraryAuditEngine.kt](../../../composeApp/src/commonMain/kotlin/com/resonance/player/model/SmartLibraryAuditEngine.kt)，`normalizeTrackKey` 第 109 行、重复分组第 79 行附近；[SmartManagerScreen.kt](../../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SmartManagerScreen.kt) 的默认选择与“安全释放”文案。
- **实测**：同歌手的 `Test Song`（3 分钟）与 `Test Song (Live)`（8 分钟）被归为一组，`live` 被默认选中清理。
- **原因**：去掉括号内容后仅按歌手/标题分组；没有核对录音版本、时长、内容摘要或声学指纹。“最佳版本”排序也不是实际码率/母带质量比较。
- **影响边界**：现有单曲删除仍需确认，不能写成自动误删；但推荐和“安全释放”会把有价值的不同录音引向删除。
- **建议**：将元数据相似标为“疑似多版本”，保留 Live/Remix/伴奏等差异，默认不选；字节完全重复可单列。真正版本筛选需明确可解释证据。
- **验收**：录音室/现场/翻唱/伴奏、同名异曲、未知歌手不应默认互删；完全重复文件单独覆盖。

## P2 问题

### B07 — 多首清理只留下最后一首

[LibraryShell.kt](../../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) 第 932 行遍历 `tracksToClean` 调用 `onDeleteLocalTrack`；[App.kt](../../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) 第 854 行只做 `trackPendingDeletion = track`。同一事件内 A/B/C 连续赋值，最终只弹 C 的单曲确认；确认也仅处理 C。应接入已有批量确认/删除通道，统一显示本次列表、成功和失败明细。回归至少选择三个不同组的条目，并验证取消不删除任何文件。

### B08 — BPM 查询没有实际筛选能力

[SemanticSongSearch.kt](../../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SemanticSongSearch.kt) 第 45–92 行的评分只看标题/歌手/专辑文本，`bpmRange` 只在第 114 行显示。`140 BPM` 解析结果为 `125..155` 且关键词为空，因此没有任何曲目能从 BPM 获得分数。`AudioFeatureAnalyzer`/`AudioFeatureStore` 目前没有生产调用点，不存在已接通的音频特征匹配链。建议补齐平台解码→后台分析→缓存→筛选，或先准确说明当前只做文本规则匹配；验证纯 BPM 查询与“BPM + 歌手/曲风”组合。本次同步修正文档中“特征提取已接入”的描述。

### B09 — 歌单引用数量缺乏独立预算

[PlainSyncPackage.kt](../../../composeApp/src/jvmMain/kotlin/com/resonance/player/sync/PlainSyncPackage.kt) 第 213–217 行直接使用 `playlist.*.track.count` 循环，未验证每单/总引用数，也未在此循环检查取消。实测公开导入入口接受 **100 万个不存在的引用**并逐一查找；本机约 101ms，仅说明输入放大，不能外推手机性能。字段可达 Int 上限，压缩包很小也能制造大量工作。另有解压总上限 512 GiB、单条目 4 GiB，未按设备剩余空间调整。建议限制 manifest 字节数、每单和总引用数，分段取消并预检磁盘；测试越界输入应在移动正式文件前拒绝。

### B10 — 删除音频时误删共享封面

[JvmMusicAssistant.kt](../../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) 第 784 行可将 `AlbumCoverRepository` 按歌手/专辑缓存的同一路径写给多首歌曲；两端 `deleteLocalTrack` 却直接删除 `track.artworkPath`（[桌面](../../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) 第 557 行、[Android](../../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) 第 1162 行）。删除其中一首会使其余歌曲的路径失效；内存缓存可能暂时遮住问题。建议只回收应用拥有且没有其他引用的文件，或独立做封面垃圾回收。用 A/B 共用封面的隔离夹具验证删 A、清内存缓存、重启后 B 仍正常。

### B11 — 桌面缓冲结束过早

[DesktopPcmPlayer.kt](../../../composeApp/src/desktopMain/kotlin/com/resonance/player/audio/DesktopPcmPlayer.kt) 第 82 行附近打开音频行后立即发布 `Playing`，随后才从 FFmpeg 读取数据；[DesktopAudioPlayer.event](../../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) 随即清除缓冲标记。慢盘/慢解码时可能已经显示播放，却没有 PCM 输出。本次未做慢盘设备复现。建议首段有效 PCM 写入后结束初始缓冲，后续输出饥饿另设判定；加入延迟首段输出的解码替身，验证此前始终处于加载状态。

### B12 — 官网迟到导航覆盖最后一次点击

[site.js](../../../website/static/site.js) 第 282–308 行的 `navigate` 没有请求代次/取消机制。用文件中实际函数执行“先点论坛、再点下载”，令下载先返回、论坛后返回，最后页面和 history 都回到论坛。结果见 [navigation-probe.json](navigation-probe.json)。建议导航代次在 `await` 后和页面应用前核对，失效响应仅作缓存；同时处理后退/前进和过渡完成顺序。此项是 JS 逻辑复现，没有宣称已做真实浏览器联调。

### B13 — 部分 Android 配置写盘失败不传播

[AndroidPlatformServices.kt](../../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) 第 614、633、866、873、994、1011 行等处丢弃 `SharedPreferences.commit()` 的布尔结果，主题、上次歌单、外观/天气、旧 AI 配置和歌词目录等写入可能失败但正常返回。UI 即使捕获保存异常也收不到失败。建议沿用已正确处理的教程/播放偏好保存契约：保留 IO 上的同步提交并检查结果，再向用户报告。不能为消除 Lint 而机械替换成 `apply()`，否则会丢失确认写盘和顺序语义。需注入返回 false 的存储替身，并校验重启后的值。

## 优化清单

以下是有源码依据的优化入口，收益需要测量，未给出未经测试的加速比例。

| 编号 | 顺序 | 当前成本与定位 | 建议与验收指标 |
| --- | --- | --- | --- |
| O01 | 高 | 统计 getter 在 composition 中首读 JSON；`ListeningStatsStore` 的同步 getter 与全量 `flush` 共用 monitor | IO 初始化，发布不持磁盘锁的内存快照；测首开详情/画像的 UI 线程耗时与万首统计文件下的锁等待。与 B05 一并处理 |
| O02 | 高 | `ManagedMusicStorage.report` 在 legacy 循环里反复 `store.read()`；`cleanup` 对每首重新生成完整 report/曲库/歌单 | 一批读取一次快照、索引路径引用、逐项提交仍保留失败回滚；以 1k/10k 记录及 300 首清理测解析/刷盘次数 |
| O03 | 中 | `LibraryShell.TrackRow` 第 2336 行起，每行分别订阅整个助手状态和前台状态；助手任务变化会广播给所有可见行 | 父级订阅一次，下传该行所需的 released/available 等值；测带任务更新滚动时的 CPU/帧耗时，避免只用重组次数判断收益 |
| O04 | 中 | 歌曲 Lazy 列表 key 含 index（第 1802 行附近）；排序/插入会改变同一歌曲身份 | 确保数据 ID 唯一后使用稳定 ID；异构列表补准确 contentType。验证排序/插入时滚动和行状态，测滚动成本 |
| O05 | 中 | Release `isMinifyEnabled=false`；工程未配置独立 Baseline Profile 生成链 | 单独验证 R8/资源裁剪和启动/曲库/播放器配置文件；先覆盖反射、Rhino、OCR、Media3、JNI，测包体与冷启动，不能直接打开开关交付 |
| O06 | 中 | `App` 的主题/上次歌单/歌词目录仍各自 `scope.launch` 保存，与播放偏好已有的 `PersistenceQueue` 不一致 | 复用保序队列和合并键，统一失败反馈；用可控延迟验证 A→B→C 最后落盘 C，并保留有序操作屏障 |
| O07 | 中 | `DailyBudget.consume` 在 asyncio 中继转发路径同步 JSON 写盘、文件/目录 fsync，每预留新块可阻塞整个事件循环 | 预算预留用串行异步持久队列/线程执行，必须持久成功后才转发；测并发传输下心跳 p95/p99，继续验证重启预算不清零 |
| O08 | 中 | 普通 `desktopTest` 包含 `renderNativeMotionPreviewFrames`，每次渲染/编码 360 张 720×1600 PNG | 将预览视频帧输出做成显式任务，保留少量关键帧和语义/中间态断言；本次线程采样确认测试在 Skia PNG 编码，不应把测试耗时误判为产品 ANR |
| O09 | 低 | 关键大文件职责密集：LibraryShell 2757 行、JvmListenTogetherService 2096 行、AndroidPlatformServices 2017 行、App 1363 行 | 在修复对应模块时拆分存储事务、删除协调、协议传输/房间状态和 UI 接线；保持协议/接口/旧数据，优先补本报告的跨模块回归，不做一次性全库改写 |

## 原生 UI 专项结论

平台形态检查通过：播放器是 Kotlin/Compose 原生实现，并非 Web 页面移植。本次保留已确认的浅色磨砂视觉方向。下面只是源码和当前离屏夹具的预评分，**不代表 TalkBack、折叠屏、手机帧率已验收**。

| 维度 | 预评分 / 4 | 依据与缺口 |
| --- | --- | --- |
| 无障碍 | 3 | 有按钮标签、滑块语义及目标尺寸断言；真实 TalkBack/焦点顺序、系统减少动态效果仍需设备核验 |
| 性能 | 2 | 有懒列表、封面限并发/LRU/下采样；同步统计 IO 和逐行共享 Flow 仍需处理 |
| 主题 | 3 | 深浅主题和对比角色有测试；未逐屏验收所有真实封面背景和系统对比度设置 |
| 平台交互 | 3 | 原生返回、播放/进度语义已有回归；真实预测返回/系统后台限制尚未本轮验证 |
| 自适应 | 3 | 本次执行短横屏、大字号、桌面壳层的离屏回归；未验证折叠姿态/所有设备 |
| 合计 | **14 / 20** | 基础可用，先处理数据与交互链路，再做针对性性能测量 |

人工查看了本次生成的 `Settings-short-1.5.png` 与 `SmartManager-390.png`，没有把旧图当本次验收。原生问题优先用 `$impeccable harden` 处理异常反馈，`$impeccable optimize` 处理 O01/O03/O04，最后 `$impeccable polish` 检查修复后的交互和文案；这些后续实施本次未执行。

## 本次验证结果

机器摘要及全部 77 个 JVM suite 计数见 [verification.json](verification.json)。原始日志在忽略目录 `build/audit-2026-11-04/`，不是历史构建输出。

| 验证 | 本次结果 |
| --- | --- |
| `:composeApp:desktopTest :composeApp:lintRelease --rerun-tasks` | 成功，8m56s；JVM 331 项：**322 通过、9 跳过、0 失败/错误**；两端 Kotlin 相关编译任务实际执行 |
| Android Release Lint | **0 Error，107 Warning**；包含 UseKtx 36、依赖/版本目录等提示；不是 107 个已确认 Bug |
| Linux 原生音频 + 本地 TLS/WSS/寻址补测 | `DesktopAudioOutputIntegrationTest`、`TogetherWebSocketTest`、`TogetherRendezvousTest` 共 8 项，**7 通过、1 跳过**；合成音频静音输出，独立临时资料目录 |
| `python -m unittest discover -s website/tests -v` | **42 通过**；临时数据库与模拟 SMTP |
| `python -m unittest discover -s services/relay -p 'test_*.py' -v` | **11 通过** |
| `python -m unittest discover -s services/rendezvous -p 'test_*.py' -v` | **7 通过** |
| `python -m unittest discover -s services/deploy/aliyun -p 'test_*.py' -v` | **2 通过**；仅回环夹具，未压测生产主机 |
| `AuditProbe.java` + `navigation-probe.cjs` | 当前编译类/实际导航函数的隔离证据已输出；[JVM 结果](probes.json)、[JS 结果](navigation-probe.json) |

补测与全量有重叠，不能把两者简单相加为“独立测试总数”。全量中跳过的 2 项原生音频和 2 项本地服务联测，已在补测开启对应环境执行；生产地址、双端原生桥和真实模型夹具仍未验收。

本次环境初始化先发现旧 `website/.venv` 无 Linux 可执行文件，随后在忽略的审计目录新建 venv 并安装固定依赖，网站测试才成功。首次临时 JVM 探针曾因 Java `Map.of()` 拒绝 null 查询失败，改用真实公开导入入口后重跑成功；该失败属于审计夹具，未计为产品缺陷。

### 排除的常见误报

- `PlaybackService` 为 `exported=false`；主 Activity 的邀请入口有动作/长度/协议限制且清除原 Intent，没有仅凭导出 Activity 就认定越权。
- Lint 的 `StaticFieldLeak` 指向 `AndroidTogetherRuntime`，其持有的是 `applicationContext`，本次不作为 Activity 泄漏证据。
- 多数扫描/转换/保存已切到 IO；协程 `Mutex`、`delay` 不等于主线程阻塞。ANR 风险只保留能追到 UI composition → getter → 磁盘/monitor 的统计读取链，未观察到真实 ANR。
- 清理托管下载已有真实路径边界、符号链接和引用保护；转换保留输入、播放代次与取消、同步包 SHA-256/路径校验的已有回归本次仍通过，后续修复必须保留。
- 不依据版本更新提示认定依赖有已知漏洞；本次未做外部漏洞情报或真实第三方服务可用性核验。

## 后续处理顺序与缺口

1. 先修 B01/B02/B03：失败导入回滚、离线记录、统一备份恢复，补旧数据和失败注入回归。
2. 修 B04/B06/B07/B10：删除授权、录音版本区分、真正批量确认和共享封面回收，全部使用合成夹具。
3. 修 B05/B13，再测 O01/O02/O03，避免扩大异常时的 UI/保存问题。
4. 补 B08/B09/B11/B12，并逐项验收，不用全量测试通过替代具体复现关闭。
5. 设备矩阵：Android 真机后台/重建/通知与权限撤销、TalkBack/预测返回；Windows 原生输出；Linux 不同桌面环境；独立蜂窝网络、多成员、断线恢复、至少 2 小时一起听；真实歌源/模型/SMTP 另行明确环境后验收。

本次检测到在线 Android 设备，但只做了连接状态检查，没有安装测试包、操作日常曲库或改变设备设置。没有执行 Windows 原生测试、生产中继探测、远程部署、发布打包或付费模型调用；本报告不扩大这些验证结论。

### 重跑隔离探针

仓库根目录执行，按开发说明配置 `JAVA_HOME`、`ANDROID_HOME`；不提交本机工具路径或 classpath 输出。首次测试及后续实验的完整命令见关联变更记录。

```bash
"$JAVA_HOME/bin/java" -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain \
  -I docs/audit/2026-11-04/classpath.init.gradle :composeApp:auditRuntimeClasspath \
  --no-configuration-cache --console=plain
"$JAVA_HOME/bin/javac" -d build/audit-2026-11-04 \
  -cp "$(cat build/audit-2026-11-04/classpath.txt)" docs/audit/2026-11-04/AuditProbe.java
"$JAVA_HOME/bin/java" -cp "build/audit-2026-11-04:$(cat build/audit-2026-11-04/classpath.txt)" AuditProbe
node docs/audit/2026-11-04/navigation-probe.cjs
```

classpath 任务本身不编译应用；首次需先完成桌面编译/测试。探针只使用临时文件，报告的是当前缺陷行为，修复后应转换为对应“应拒绝/应保留”的正式回归断言。
