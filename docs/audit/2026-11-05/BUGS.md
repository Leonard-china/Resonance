# 2026-11-05 — 全功能测试发现的问题与复现步骤

> 后续修复与本次重新验证见[修复验收报告](FIX-VALIDATION.md)。下文保留修复前的原始观察和计数，不用修复后的结果覆盖历史证据。

对象为当前未提交工作区，版本 0.6.6 / 40，HEAD `846aa16`。只测试和记录，没有修复以下问题。共 **12 项：5 项 P1、7 项 P2**；9 项用当前编译类或实际 JS 函数隔离复现，3 项为调用链/源码确认。优先级用于本轮修复排期，没有据此断言存在 P0。

P1 涉及数据保留、恢复、删除授权或将不同录音引向删除；P2 为功能缺口、失败反馈或资源边界。复现均使用合成内容和临时目录。共享实现只能证明相同代码路径存在，不能当作 Android、Windows、Linux 都已实机复现。

| 编号 | 级别 | 问题 | 本轮证据 | 与11月4日报告关系 |
| --- | --- | --- | --- | --- |
| Q01 | P1 | 坏歌单包在报错前已覆盖原音频 | Linux运行共享JVM公开导入入口 | B01重新复现 |
| Q02 | P1 | 暂时离线的曲目在后续保存中丢失记录 | Linux桌面真实存储类 | B02重新复现 |
| Q03 | P1 | 部分桌面设置绕过有效备份恢复 | Linux桌面真实存储类 | B03重新复现 |
| Q04 | P1 | 询问/解释/否定语句误取得AI删音频权限 | 共享授权函数实测 | B04重新复现 |
| Q05 | P1 | Studio/Live被当成冗余且默认选中删除 | 共享体检算法实测 | B06重新复现 |
| Q06 | P2 | 智能管家多首清理只留下最后一首确认 | 当前UI调用链，未做端到端点击复现 | B07源码复核 |
| Q07 | P2 | BPM意图没有参与本地结果筛选 | 解析实测+当前匹配源码 | B08复核，未运行完整界面筛选断言 |
| Q08 | P2 | 小包可声明百万歌单引用并逐一循环 | 共享公开导入入口实测 | B09重新复现 |
| Q09 | P2 | 删除一首歌同时删除其他歌共用封面 | Linux真实平台删除入口 | B10由源码确认补为实测 |
| Q10 | P2 | Android部分设置忽略commit失败 | 当前源码确认，未注入磁盘失败 | B13源码复核 |
| Q11 | P2 | 删除被权限阻止仍移除记录并报告成功 | Linux真实平台删除入口 | 本轮补充边界 |
| Q12 | P2 | 官网迟到导航覆盖最后点击页面 | 实际site.js函数隔离运行 | B12重新复现 |

<a id="q01"></a>
## Q01：歌单包失败前覆盖音频

- 位置：[PlainSyncPackage.kt](../../../composeApp/src/jvmMain/kotlin/com/resonance/player/sync/PlainSyncPackage.kt)，`import → validateTracks → commitTracks → readPlaylists`。
- 操作：在临时托管目录写入 `synthetic original`；制作音频hash合法、但缺少 `playlist.0.name` 的同ID包；调用公开 `import`。
- 期望：拒绝整个包，原音频字节保持不变。
- 实际：抛出“同步清单缺少 playlist.0.name”，原文件却已经变成 `synthetic replacement`。
- 原因：完整歌单清单校验发生在正式文件替换之后，没有整包回滚事务。
- 验收要求：缺字段、第二个文件移动失败、元数据保存失败时，音频、封面和曲库都应回到原状态。

<a id="q02"></a>
## Q02：断盘后保存会丢曲目

- 位置：[DesktopLibraryStore.kt](../../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopLibraryStore.kt)，`load / setFavorite / save`。
- 操作：保存A/B两条合成曲目；临时移走A的文件；收藏B；恢复A的文件，再加载。
- 期望：A暂不可播放，但ID、收藏与库记录保留，重连后恢复。
- 实际：断盘读取只有B；修改B触发保存，恢复A后曲库仍只有B。
- 原因：持久层 `load` 过滤不存在的文件，后续读改写把过滤结果当作完整数据库。
- 验收要求：不可读状态与条目身份分离，只有明确删除才移除记录。

<a id="q03"></a>
## Q03：设置读取未统一恢复入口

- 位置：[DesktopLibraryStore.kt](../../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopLibraryStore.kt)，`loadThemeMode / loadLastSelectedPlaylistId / loadPlaybackMode` 等专用读写。
- 操作：创建有效 `.bak`，把主设置文件改成非法Unicode转义；先调用 `loadThemeMode`，再调用 `readUiValue("app.themeMode")`。
- 期望：两个入口均恢复同一有效备份，继续写入不丢其他设置。
- 实际：专用主题入口抛 `IllegalArgumentException`；通用入口成功恢复 `Dark`。
- 原因：部分方法直接 `Properties.load`，绕过 `AtomicPropertiesFile.read`。
- 验收要求：逐个覆盖主题、模式、上次歌单、歌词目录的坏主文件与后续保存。

<a id="q04"></a>
## Q04：AI删除授权误放行

- 位置：[JvmMusicAssistant.kt](../../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt)，`allowsAudioDeletion` 与 `delete_tracks` 执行门禁。
- 操作：分别输入“如何删除歌曲？”、“解释一下删除音频有什么风险”、“只删除歌单里的歌曲，不删除音频文件”。
- 期望：三种输入均不授权删除真实音频。
- 实际：三者均返回 `true`。明确指令“帮我删除这首歌的文件”也返回 `true`，作为正对照。
- 原因：删除关键词匹配，没有完整区分问法、解释、否定与执行；现有否定正则不包含“不删除”。
- 影响边界：不表示模型一定会调用删除，但一旦返回 `delete_tracks`，本地门禁会放行。
- 验收要求：具体曲目和文件范围的本地确认绑定本轮请求；询问、引用、否定和只删歌单不得获音频删除能力。

<a id="q05"></a>
## Q05：不同录音被预选清理

- 位置：[SmartLibraryAuditEngine.kt](../../../composeApp/src/commonMain/kotlin/com/resonance/player/model/SmartLibraryAuditEngine.kt)，`normalizeTrackKey / audit`。
- 操作：输入同歌手 `Test Song`（3分钟）与 `Test Song (Live)`（8分钟）。
- 期望：不同录音版本保持独立，不能默认选中Live作为冗余文件。
- 实际：合为一个重复组，`live` 被写入 `selectedToCleanIds`。
- 原因：归一化直接去掉括号版本文字，再按歌手+歌名分组；保留建议也不是实际音质检测。
- 影响边界：仍需用户确认，未证明自动删除；问题是默认建议和文案会引导删除不同录音。
- 验收要求：Live/Remix/伴奏/翻唱、同名异曲、未知歌手与字节完全重复分开测试。

<a id="q06"></a>
## Q06：智能清理批量接线丢操作

- 位置：[LibraryShell.kt](../../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) 的 `onCleanDuplicates` 遍历，与 [App.kt](../../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) 的 `onDeleteLocalTrack = { trackPendingDeletion = track }`。
- 操作：选择A/B/C三首清理。期望出现完整批次确认与明细；当前连续赋值最终只保留C。
- 证据级别：源码调用链确认；本轮没有在设备中实际点击三首后观察弹窗。
- 验收要求：接入批量确认/删除通道，取消不删除，部分失败逐项报告。

<a id="q07"></a>
## Q07：BPM只显示标签

- 位置：[SemanticSongSearch.kt](../../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SemanticSongSearch.kt) 的 `localMatches` 与意图标签。
- 输入：`140 BPM`。解析得到 `125..155`，关键词为空。
- 期望：按真实曲目节拍筛选；实际匹配仅使用标题/歌手/专辑中的关键词、场景、情绪和曲风，不读取 `bpmRange`。
- 证据级别：解析实测+源码确认，未把完整UI筛选当作已执行。音频特征算法有单测，但未接生产解码和搜索链。
- 验收要求：接通真实节拍数据再宣称支持BPM筛选，或明确仅展示识别出的意图。

<a id="q08"></a>
## Q08：歌单引用数量缺少预算

- 位置：[PlainSyncPackage.kt](../../../composeApp/src/jvmMain/kotlin/com/resonance/player/sync/PlainSyncPackage.kt)，`readPlaylists`。
- 操作：构造0首轨道、1个歌单、`playlist.0.track.count=1000000` 的微型合法ZIP。
- 期望：在正式文件提交前按每单/总引用预算拒绝。
- 实际：接受并循环100万次不存在引用，本机约122ms；这个耗时不能外推手机或更大输入。
- 原因：仅限制歌单数量，未限制每单及总引用数，也没有在引用循环中检查取消。
- 验收要求：越界清单提前失败；加入清单字节预算、引用数预算、分段取消及磁盘空间约束。

<a id="q09"></a>
## Q09：共享封面被误删

- 位置：[DesktopPlatformServices.kt](../../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt)，`deleteLocalTrack / batchDeleteTracks`；Android对应路径也直接删 `artworkPath`。
- 操作：A/B两个合成音频引用同一个合成 `album.jpg`；经真实桌面平台入口删除A。
- 期望：B仍能读取共享封面。
- 实际：B音频存在，库记录仍指向该封面，但封面文件已不存在。
- 证据级别：Linux运行复现；Windows/Android同类代码未实机删除测试。
- 验收要求：仅回收应用拥有且无其他引用的封面；清内存缓存并重启后B仍有封面。

<a id="q10"></a>
## Q10：Android设置写失败未报告

- 位置：[AndroidPlatformServices.kt](../../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt)，`saveThemeMode / saveAppearance / saveWeatherSnapshot / saveCustomLyricsFolder` 等。
- 期望：`SharedPreferences.commit()==false` 时明确失败，保持保存顺序。
- 实际源码：若干入口忽略布尔结果，方法正常返回；其他教程/收藏入口已有 `check(commit())`，契约不一致。
- 证据级别：仅源码确认，没有把手机存储制造为写失败。
- 验收要求：注入commit=false与异常替身，确认UI反馈及重启值；不机械改为异步apply以掩盖失败。

<a id="q11"></a>
## Q11：文件删不掉仍移除库记录

- 位置：[DesktopPlatformServices.kt](../../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt)，`deleteLocalTrack`。
- 操作：保存合成C曲目；把它的临时父目录改为只读，令当前用户无权删除；调用实际平台删除，随后恢复目录权限。
- 期望：区分“文件已不存在”与“删除发生权限错误”，失败时保留可重试记录并报告原因。
- 实际：`originalAudioStillExists=true`、`fileDeleted=false`、`trackStillInLibrary=false`、`reportedSuccess=true`，提示“已从音乐库移除「C」”。
- 原因：`runCatching { Files.deleteIfExists }.getOrDefault(false)` 吞掉错误，随后无条件 `library.remove`，最后始终 `success=true`。
- 验收要求：权限拒绝、Windows文件占用、目录不可写、部分批量失败与原文件已不存在分别覆盖；本次只复现Linux权限拒绝。

<a id="q12"></a>
## Q12：官网请求乱序覆盖最后意图

- 位置：[site.js](../../../website/static/site.js)，实际 `navigate` 函数。
- 操作：先调用论坛导航、再调用下载导航；人为让下载先返回，再放行论坛响应。
- 期望：最终停在下载页，过期论坛响应不能改DOM和history。
- 实际：应用顺序为下载→论坛，最终页和history都回到论坛。
- 证据级别：提取当前文件内真实函数，在隔离Node上下文执行；没有宣称真实浏览器端到端复现。
- 验收要求：请求代次或取消保护覆盖await之后、页面应用之前、缓存命中、前进后退与过渡完成。

## 历史问题的本轮重新判定

- 11月4日B05的底层坏统计文件仍会抛错，这是保护坏数据的一部分；当前UI已改为 `rememberListeningStats` 订阅快照，兼容读取在IO捕获异常。统计仓库回归本轮通过，不能继续拿旧composition调用链断言当前手机必崩。本轮没有制造正式应用统计损坏做实机验收。
- 11月4日B11的桌面首段PCM缓冲问题已有会话层回归，本轮 `DesktopPcmPlayerSessionTest` 通过，按当前实现不继续作为未修复缺陷计数。
- 其余问题的“重新复现”均来自本轮输出，不是复制历史结果。

## 可核对证据

- [数据/授权探针结果](edge-probes.json)：沿用旧B编号以便和历史对照，B05的原始抛错单独按上述方式解释。
- [共享封面与删除失败结果](extra-probes.json)。
- [官网导航顺序结果](navigation-probe.json)。
- [Java合成包/存储/授权探针](probes/AuditProbe.java)、[Kotlin真实平台删除探针](probes/ExtraProbe.kt)、[实际JS函数探针](probes/navigation-probe.cjs)。前两者需按本轮完整编译类和项目依赖构建classpath；JS探针从仓库根目录 `node docs/audit/2026-11-05/probes/navigation-probe.cjs` 即可运行。

所有探针正常退出只表示“复现实验完成”，不表示对应产品验收通过。未向真实歌源、AI、B站、生产论坛、邮件或公网房间发送测试写入。
