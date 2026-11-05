# 2026-11-05 — Resonance逐项功能与验收清单

> 后续修复与本次重新验证见[修复验收报告](FIX-VALIDATION.md)。下文保留修复前的原始观察和计数，不用修复后的结果覆盖历史证据。

共 **191 个功能/检查点**，按当前文档、路由、平台接口及实现整理。版本0.6.6 / 40。每一行包含操作/期望、实际证据和剩余范围；“局部通过”只覆盖链接中的真实测试断言，不表示整行所有平台/系统入口均已验收。

结果说明：**发现缺陷**为本轮异常场景未满足期望；**源码确认**有具体调用链但未运行该失败场景；**局部通过**为关联断言通过；**相关回归有失败**需读用例和总报告判定；**未执行**不记为通过。公网、付费接口、真实选择器和Windows系统验收不会被JVM单测替代。

## 启动、教程与导航

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F001 | 首次启动选择 | 快速图文/跟着操作/跳过；升级后不重复弹出 | 相关回归有失败 | [TutorialProgressTest](TEST-RESULTS.md#tutorialprogresstest)、[TutorialNavigationTest](TEST-RESULTS.md#tutorialnavigationtest) | 独立测试包已实机执行；首次教程的2项UI断言失败，见Android续测报告 |
| F002 | 七章使用教程与常见问题 | 逐章打开、完成、跳过、重新练习 | 相关回归有失败 | [TutorialProgressTest](TEST-RESULTS.md#tutorialprogresstest)、[TutorialNavigationTest](TEST-RESULTS.md#tutorialnavigationtest) | 正文逐句准确性未人工审稿 |
| F003 | 操作教学高亮与定位 | 滚动到真实控件、点击推进、错误事件不推进 | 相关回归有失败 | [TutorialNavigationTest](TEST-RESULTS.md#tutorialnavigationtest) | 设备操作教学主路径通过；设置教程入口定位相关断言仍失败 |
| F004 | 教程暂停和继续 | 暂停后保留步骤，重启恢复已保存进度 | 相关回归有失败 | [TutorialProgressTest](TEST-RESULTS.md#tutorialprogresstest)、[TutorialNavigationTest](TEST-RESULTS.md#tutorialnavigationtest) | 不能代替每个章节实机走通 |
| F005 | 四项主导航 | 音乐库/音乐助手/一起听/工具来回切换 | 相关回归有失败 | [LibraryNavigationTest](TEST-RESULTS.md#librarynavigationtest)、[StatePreservationTest](TEST-RESULTS.md#statepreservationtest) | 跨页保留滚动和播放器分别核对 |
| F006 | 返回与键盘快捷键 | 返回关闭最上层；Ctrl+F、空格、Ctrl+方向键 | 局部通过 | [StatePreservationTest](TEST-RESULTS.md#statepreservationtest) | 真实输入法/全部快捷键未逐键验收 |
| F007 | 小屏、横屏、宽屏布局 | 底栏/导航轨/侧栏切换，大字体控件可达 | 相关回归有失败 | [UiVisualContractTest](TEST-RESULTS.md#uivisualcontracttest)、[LibraryDetailTransitionTest](TEST-RESULTS.md#librarydetailtransitiontest)、[MusicStageDeviceTest](TEST-RESULTS.md#musicstagedevicetest) | Windows/折叠屏/屏幕阅读器未验收 |
| F008 | 页面横向滑动 | 方向/距离阈值、首末页、子控件与多选冲突 | 局部通过 | [HorizontalPageSwipeUnitTest](TEST-RESULTS.md#horizontalpageswipeunittest)、[HorizontalPageSwipeTest](TEST-RESULTS.md#horizontalpageswipetest) | 系统边缘手势仍需不同设备验证 |
| F009 | 页面与详情连续切换 | 快速反向返回、减少动态、旧页不可交互 | 相关回归有失败 | [LibraryDetailTransitionTest](TEST-RESULTS.md#librarydetailtransitiontest)、[UiVisualContractTest](TEST-RESULTS.md#uivisualcontracttest) | 断言通过不代表满帧 |
| F010 | 应用退出与任务清理 | 停止生产者、等待最后写入、释放音频与平台资源 | 局部通过 | [ApplicationSessionTest](TEST-RESULTS.md#applicationsessiontest)、[ApplicationUseCaseTest](TEST-RESULTS.md#applicationusecasetest)、[ApplicationLifecycleDeviceTest](TEST-RESULTS.md#applicationlifecycledevicetest) | 断电/强杀时多文件一致性另列风险 |
## 曲库与歌单

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F011 | 本地曲库加载 | 空库/已有库/托管与系统扫描数据合并 | 发现缺陷（隔离复现） | [LibraryPersistenceTest](TEST-RESULTS.md#librarypersistencetest)、[DesktopMusicImportTest](TEST-RESULTS.md#desktopmusicimporttest) | 离线文件丢记录见[Q02](BUGS.md#q02) |
| F012 | 歌曲、歌单、专辑和歌手视图 | 分组浏览、点开详情、返回保留位置 | 相关回归有失败 | [LibraryNavigationTest](TEST-RESULTS.md#librarynavigationtest)、[LibraryDetailTransitionTest](TEST-RESULTS.md#librarydetailtransitiontest) | 每种混合语言排序未全量覆盖 |
| F013 | 歌名/歌手/专辑文本搜索 | 大小写、空输入、中文、万首索引复用 | 局部通过 | [LibrarySearchIndexTest](TEST-RESULTS.md#librarysearchindextest) | 首屏真实性不依赖联网 |
| F014 | 拼音首字母搜索 | 中文转写、首字母、非中文混合输入 | 局部通过 | [LibrarySearchIndexTest](TEST-RESULTS.md#librarysearchindextest)、[PinyinUtilsTest](TEST-RESULTS.md#pinyinutilstest) | 多音字非穷举 |
| F015 | 自然语言意图找歌 | 场景/情绪/曲风/关键词按元数据评分 | 局部通过 | [SemanticSearchIntentParserTest](TEST-RESULTS.md#semanticsearchintentparsertest) | 仅文本规则；不是音频理解模型 |
| F016 | BPM意图筛选 | 输入140 BPM应按节拍范围匹配曲目 | 源码确认缺口 | [SemanticSearchIntentParserTest](TEST-RESULTS.md#semanticsearchintentparsertest) | [Q07](BUGS.md#q07)：只显示标签，未接筛选 |
| F017 | 歌曲排序 | 默认/标题/歌手/专辑/时长升降序 | 局部通过 | [PlaybackQueueAndFilterTest](TEST-RESULTS.md#playbackqueueandfiltertest) | 字符串与异常时长边界见测试清单 |
| F018 | 曲库筛选 | FLAC/高品质/缺封面/缺歌词/缺元数据 | 局部通过 | [PlaybackQueueAndFilterTest](TEST-RESULTS.md#playbackqueueandfiltertest) | 品质条件不等于测量真实母带 |
| F019 | 收藏和取消收藏 | 相同ID变更收藏，列表数量与歌单快照一致 | 局部通过 | [LibraryPersistenceTest](TEST-RESULTS.md#librarypersistencetest)、[MusicFeaturesIntegrationTest](TEST-RESULTS.md#musicfeaturesintegrationtest) | 收藏与推荐点赞独立 |
| F020 | 新建自建歌单 | 命名、空歌单、保存后重新读取 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest)、[LibraryPersistenceTest](TEST-RESULTS.md#librarypersistencetest) | 系统文件选择不是该项测试范围 |
| F021 | 重命名和删除歌单 | 只改关系与名称，删除歌单不删音频 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | AI工具与手工入口分别看覆盖 |
| F022 | 添加和移出歌单成员 | 保序、去重、跨重启恢复成员 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest)、[ApplicationUseCaseTest](TEST-RESULTS.md#applicationusecasetest) | 多文件落盘没有整库事务 |
| F023 | 多选、全选与批量操作 | 选中歌曲后收藏/加歌单/删除确认 | 相关回归有失败 | [LibraryNavigationTest](TEST-RESULTS.md#librarynavigationtest) | 系统删除授权弹窗未全覆盖 |
| F024 | 歌曲信息与来源详情 | 显示元数据、真实码率/来源/听歌统计 | 局部通过 | [UiVisualContractTest](TEST-RESULTS.md#uivisualcontracttest)、[ListeningStatsRepositoryTest](TEST-RESULTS.md#listeningstatsrepositorytest) | 不能仅凭标题认证无损 |
| F025 | 标题、歌手、专辑编辑 | 编辑后曲库与歌单快照一致 | 局部通过 | [MusicFeaturesIntegrationTest](TEST-RESULTS.md#musicfeaturesintegrationtest)、[LibraryPersistenceTest](TEST-RESULTS.md#librarypersistencetest) | 旧文件真实标签写回按平台区分 |
| F026 | 单曲与批量真实删除 | 确认后删文件及关系；失败应保留明确原因 | 发现缺陷（隔离复现） | [MusicFeaturesIntegrationTest](TEST-RESULTS.md#musicfeaturesintegrationtest) | [Q09](BUGS.md#q09)共享封面、[Q11](BUGS.md#q11)失败仍移除记录 |
| F027 | 临时离线与外部磁盘恢复 | 断盘时修改另一歌曲；重连保留原ID/收藏 | 发现缺陷（隔离复现） | [LibraryPersistenceTest](TEST-RESULTS.md#librarypersistencetest) | [Q02](BUGS.md#q02)隔离场景失败 |
## 导入与格式转换

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F028 | Android媒体扫描和权限 | 未授权只读托管；主动扫描才申请权限 | 局部通过 | [MusicFeaturesIntegrationTest](TEST-RESULTS.md#musicfeaturesintegrationtest) | 系统权限拒绝/撤销/重新授权未逐步实测 |
| F029 | Android SAF递归目录导入 | 读取授权目录、保留输入、显示进度 | 未执行专门验收 | 源码/系统入口 | 系统目录选择器未人工操作 |
| F030 | 桌面多选文件与目录 | 混合根目录、中文路径、错误文件隔离 | 局部通过 | [DesktopMusicImportTest](TEST-RESULTS.md#desktopmusicimporttest) | JFileChooser真实点击未逐项执行 |
| F031 | 桌面整窗拖入 | 文件列表/URI列表、拒绝URL、Copy语义 | 局部通过 | [DesktopMusicImportTest](TEST-RESULTS.md#desktopmusicimporttest) | 真实文件管理器拖放与Wayland未人工验收 |
| F032 | 递归扫描与真实路径去重 | 重复根/符号链接/坏路径不重复入库或死循环 | 局部通过 | [DesktopMusicImportTest](TEST-RESULTS.md#desktopmusicimporttest) | 共享JVM实现不代替Windows系统验证 |
| F033 | 普通音频导入 | MP3/FLAC/M4A/AAC/Ogg/Opus/WAV/WMA/APE | 局部通过 | [DesktopMusicImportTest](TEST-RESULTS.md#desktopmusicimporttest)、[DesktopManagedMp3ConverterTest](TEST-RESULTS.md#desktopmanagedmp3convertertest) | 没有为每种容器每种编码制作全矩阵 |
| F034 | 可选转换MP3与保留原音轨 | 兼容音轨保留格式；转换不覆盖原文件 | 局部通过 | [DesktopManagedMp3ConverterTest](TEST-RESULTS.md#desktopmanagedmp3convertertest)、[MusicFeaturesIntegrationTest](TEST-RESULTS.md#musicfeaturesintegrationtest) | 生成音频夹具，未读取用户歌曲 |
| F035 | KGMA v3/slot1解密 | MP3载荷、非MP3载荷转换与异常头 | 局部通过 | [KgmaCipherTest](TEST-RESULTS.md#kgmaciphertest)、[DesktopManagedMp3ConverterTest](TEST-RESULTS.md#desktopmanagedmp3convertertest) | 其他KGMA版本不支持 |
| F036 | KGG v5解密 | 匹配密钥/缺密钥/异常数据 | 局部通过 | [KggCipherTest](TEST-RESULTS.md#kggciphertest) | Windows酷狗数据库真实密钥环境未执行；Android不支持 |
| F037 | 导入进度、取消与残留清理 | 已完成曲目保留，取消停止当前转码并清part | 局部通过 | [DesktopManagedMp3ConverterTest](TEST-RESULTS.md#desktopmanagedmp3convertertest)、[DesktopMusicImportTest](TEST-RESULTS.md#desktopmusicimporttest) | 单包解压内取消粒度受限 |
| F038 | 重复导入与内容ID | 内容hash复用，路径去重，不重复创建音频 | 局部通过 | [DesktopMusicImportTest](TEST-RESULTS.md#desktopmusicimporttest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 跨来源相同录音身份并非声学识别 |
| F039 | 导入与偏好/歌单并发屏障 | 等已接收写入后重载，避免旧UI覆盖新歌单 | 局部通过 | [ApplicationUseCaseTest](TEST-RESULTS.md#applicationusecasetest)、[DesktopSyncPackageImportTest](TEST-RESULTS.md#desktopsyncpackageimporttest) | 崩溃中的跨文件提交没有回滚保证 |
## 播放与音效

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F040 | 播放、暂停和继续 | 真实PCM推进；暂停后位置停止、继续可推进 | 局部通过 | [DesktopAudioOutputIntegrationTest](TEST-RESULTS.md#desktopaudiooutputintegrationtest)、[AudioSessionIntegrationTest](TEST-RESULTS.md#audiosessionintegrationtest) | Linux静音输出与Android生成静音夹具 |
| F041 | 上一首与下一首 | 只在有效当前队列切换，错误音频有反馈 | 局部通过 | [PlaybackQueueResolutionTest](TEST-RESULTS.md#playbackqueueresolutiontest)、[PlaybackStateStoreTest](TEST-RESULTS.md#playbackstatestoretest) | 真实蓝牙耳机按键未操作 |
| F042 | 队列选择、追加和去重 | 按顺序追加，不自动开播；过滤缺音源ID | 局部通过 | [PlaybackQueueResolutionTest](TEST-RESULTS.md#playbackqueueresolutiontest)、[AssistantMusicLinksTest](TEST-RESULTS.md#assistantmusiclinkstest) | 一起听中使用房间队列 |
| F043 | 随机、顺序和单曲/列表循环 | 模式组合持久化，重启恢复设置 | 局部通过 | [PlaybackModePersistenceTest](TEST-RESULTS.md#playbackmodepersistencetest)、[PlaybackQueueAndFilterTest](TEST-RESULTS.md#playbackqueueandfiltertest) | 设备长时随机播放未执行 |
| F044 | 拖动进度和歌词seek | 松手提交、原生跳转到指定位置 | 局部通过 | [DesktopAudioOutputIntegrationTest](TEST-RESULTS.md#desktopaudiooutputintegrationtest)、[UiInteractionContractTest](TEST-RESULTS.md#uiinteractioncontracttest) | 暂停/后台seek另外有服务测试 |
| F045 | 播放倍速 | 新解码器就绪，状态与实际输出继续推进 | 局部通过 | [DesktopAudioOutputIntegrationTest](TEST-RESULTS.md#desktopaudiooutputintegrationtest)、[PlaybackSettingsDialogTest](TEST-RESULTS.md#playbacksettingsdialogtest) | 音质听感未盲测 |
| F046 | 全局音量 | 持久化并跨曲生效，不叠加旧逐曲音量 | 局部通过 | [PlaybackStateStoreTest](TEST-RESULTS.md#playbackstatestoretest)、[AudioSessionIntegrationTest](TEST-RESULTS.md#audiosessionintegrationtest) | 系统音量与蓝牙硬件音量未全组合 |
| F047 | 自动音量平衡 | 整曲响度、峰值余量、缓存、关闭归零 | 局部通过 | [LoudnessAnalysisTest](TEST-RESULTS.md#loudnessanalysistest)、[FfmpegLoudnessIntegrationTest](TEST-RESULTS.md#ffmpegloudnessintegrationtest)、[AudioSessionIntegrationTest](TEST-RESULTS.md#audiosessionintegrationtest) | 听感一致性未做主观评审 |
| F048 | PCM增益与限幅平滑 | 正/负增益、单声道、渐变和防削波 | 局部通过 | [PcmGainProcessorTest](TEST-RESULTS.md#pcmgainprocessortest)、[AudioSessionIntegrationTest](TEST-RESULTS.md#audiosessionintegrationtest) | 无专用音频分析仪验收 |
| F049 | 七类音效预设和伴奏消音 | 打开不改值，选择后关闭并继续操作播放 | 局部通过 | [UiVisualContractTest](TEST-RESULTS.md#uivisualcontracttest)、[PlayerDialogsDeviceTest](TEST-RESULTS.md#playerdialogsdevicetest) | 预设DSP频响与人声消除效果未逐个量化 |
| F050 | 加载、缓冲和播放错误 | 首段PCM之前保持缓冲；旧任务错误不污染新曲 | 局部通过 | [DesktopPcmPlayerSessionTest](TEST-RESULTS.md#desktoppcmplayersessiontest)、[DesktopPlaybackFailureTest](TEST-RESULTS.md#desktopplaybackfailuretest) | 极端native驱动永久阻塞仍有限制 |
| F051 | 快速A-B-A切歌竞争 | 旧解码/响度/文件检查不能复活过期歌曲 | 局部通过 | [PlaybackStateStoreTest](TEST-RESULTS.md#playbackstatestoretest)、[PlaybackSessionTest](TEST-RESULTS.md#playbacksessiontest)、[DesktopPcmPlayerSessionTest](TEST-RESULTS.md#desktoppcmplayersessiontest) | 短回归不能代替长时压力 |
| F052 | 暂停恢复快照 | 冷启动暂停恢复曲目/进度/队列，后台服务存活时附着 | 局部通过 | [PlaybackSnapshotTest](TEST-RESULTS.md#playbacksnapshottest)、[PlaybackSessionTest](TEST-RESULTS.md#playbacksessiontest) | 跨版本真实升级资料未迁移试验 |
| F053 | 后台服务与生命周期 | 无Activity切歌、重建后命令归属与退出排空 | 局部通过 | [AudioControllerLifecycleDeviceTest](TEST-RESULTS.md#audiocontrollerlifecycledevicetest)、[ApplicationLifecycleDeviceTest](TEST-RESULTS.md#applicationlifecycledevicetest) | 各厂商省电策略与锁屏长时未验收 |
| F054 | 睡眠定时 | 15/30/45/60分钟/曲终；单调时钟、暂停仍计时 | 局部通过 | [PlaybackSleepTimerTest](TEST-RESULTS.md#playbacksleeptimertest) | 整小时墙钟测试未等待；退出不恢复 |
| F055 | 播放页展开收起和拖动回弹 | 短拉回弹、长拉收起、反向操作、减少动态 | 相关回归有失败 | [PlayerExpansionTest](TEST-RESULTS.md#playerexpansiontest)、[LibraryNavigationTest](TEST-RESULTS.md#librarynavigationtest) | Debug帧数据不代表Release性能 |
| F056 | 听歌时长、次数和最后收听 | 后台原生计数、95%完播只计一次、暂停不累计 | 局部通过 | [ListeningStatsStoreTest](TEST-RESULTS.md#listeningstatsstoretest)、[ListeningStatsRepositoryTest](TEST-RESULTS.md#listeningstatsrepositorytest)、[ListeningSessionDeviceTest](TEST-RESULTS.md#listeningsessiondevicetest) | 两小时持续计数未执行 |
| F057 | 统计坏文件和备份恢复 | 损坏不覆盖原始数据，UI读取内存错误快照 | 局部通过 | [ListeningStatsRepositoryTest](TEST-RESULTS.md#listeningstatsrepositorytest)、[ListeningStatsStoreTest](TEST-RESULTS.md#listeningstatsstoretest) | 历史底层抛错不再直接等同现行UI崩溃 |
## 歌词与翻译

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F058 | 本地LRC多时间戳解析 | 空文件、偏移、重复时间、多时间戳 | 局部通过 | [LrclibLyricsRepositoryTest](TEST-RESULTS.md#lrcliblyricsrepositorytest)、[FloatingLyricsTest](TEST-RESULTS.md#floatinglyricstest) | 未穷举各编辑器的私有格式 |
| F059 | 本地KRC解码 | 解压、字词时间、异常数据 | 局部通过 | [KrcDecoderTest](TEST-RESULTS.md#krcdecodertest) | 非KRC格式按未支持处理 |
| F060 | 自定义歌词目录 | 保存目录、扫描同名歌词、取消选择 | 局部通过 | [LrclibLyricsRepositoryTest](TEST-RESULTS.md#lrcliblyricsrepositorytest) | 系统目录选择器与长期授权未实测 |
| F061 | LRCLIB自动查词和缓存 | 标题/歌手/版本/时长校验，正负缓存与强刷 | 局部通过 | [LrclibLyricsRepositoryTest](TEST-RESULTS.md#lrcliblyricsrepositorytest)、[LyricsAcquisitionTest](TEST-RESULTS.md#lyricsacquisitiontest) | 公网查词opt-in未执行 |
| F062 | 歌源歌词回退 | 最多8源、单源超时、纯文本兜底保留 | 局部通过 | [LyricsAcquisitionTest](TEST-RESULTS.md#lyricsacquisitiontest)、[MusicSourceRegistryTest](TEST-RESULTS.md#musicsourceregistrytest) | 真实公开服务可用率未在线测量 |
| F063 | 同步歌词优先 | 纯文本缓存不能阻断后续有效同步轴 | 局部通过 | [LyricsAcquisitionTest](TEST-RESULTS.md#lyricsacquisitiontest)、[LyricsPlayerRegressionTest](TEST-RESULTS.md#lyricsplayerregressiontest) | 零轴/超曲长不伪称同步 |
| F064 | 切歌与偏移代次保护 | A-B-A迟到结果拒绝；偏移不被旧请求重置 | 局部通过 | [LyricsSessionTest](TEST-RESULTS.md#lyricssessiontest) | 翻译另有独立代次 |
| F065 | 封面歌词预览和当前句高亮 | 预览可见、当前句淡底细边、稳定行高 | 局部通过 | [LyricsPlayerRegressionTest](TEST-RESULTS.md#lyricsplayerregressiontest)、[LyricsPlayerRegressionDeviceTest](TEST-RESULTS.md#lyricsplayerregressiondevicetest) | 截图断言不是视觉逐像素金标 |
| F066 | 连续滚动和手动浏览恢复 | 居中、离屏seek、手动暂停跟随、5秒恢复、回当前句 | 局部通过 | [LyricScrollTest](TEST-RESULTS.md#lyricscrolltest)、[LyricsMotionTest](TEST-RESULTS.md#lyricsmotiontest)、[LyricsScrollDeviceTest](TEST-RESULTS.md#lyricsscrolldevicetest)、[LyricsPreviewMotionDeviceTest](TEST-RESULTS.md#lyricspreviewmotiondevicetest) | 原生手势延迟受设备与负载影响 |
| F067 | 纯文本歌词与大字体 | 无时间轴不假同步，长行和双语仍可浏览 | 局部通过 | [LyricsPlayerRegressionTest](TEST-RESULTS.md#lyricsplayerregressiontest)、[LyricsScrollDeviceTest](TEST-RESULTS.md#lyricsscrolldevicetest) | 全部语言字体回退未穷举 |
| F068 | 原文/译文/双语模式 | 换模式保留时间轴与列表身份 | 局部通过 | [LyricsTranslationControllerTest](TEST-RESULTS.md#lyricstranslationcontrollertest)、[LyricsTranslationDeviceTest](TEST-RESULTS.md#lyricstranslationdevicetest) | 翻译语义质量未人工评审 |
| F069 | 目标语言翻译与缓存 | 每批50行、等长JSON、文本hash+语言复用 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest)、[LyricsTranslationControllerTest](TEST-RESULTS.md#lyricstranslationcontrollertest) | 真实付费模型未调用 |
| F070 | 翻译失败重试与迟到隔离 | 保留原词/已有译文；切歌/换语言取消旧结果 | 局部通过 | [LyricsTranslationControllerTest](TEST-RESULTS.md#lyricstranslationcontrollertest)、[LyricsTranslationDeviceTest](TEST-RESULTS.md#lyricstranslationdevicetest) | 使用假AI服务验证 |
| F071 | 显式AI生成歌词文本 | 不自动付费、不传音频、去掉模型伪时间戳 | 局部通过 | [DeepSeekClientTest](TEST-RESULTS.md#deepseekclienttest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 真实原曲歌词准确性未验收 |
| F072 | 音频识别同步歌词 | 16k单声道PCM、真实识别时间对齐、失败保留原词 | 局部通过 | [AudioLyricAlignmentTest](TEST-RESULTS.md#audiolyricalignmenttest)、[MusicFeaturesIntegrationTest](TEST-RESULTS.md#musicfeaturesintegrationtest) | 收费WSS服务未调用，使用假识别结果 |
| F073 | 语音时长预算与原文备份 | 预留整曲时长、失败保守计入、缓存与清理 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest)、[AudioLyricAlignmentTest](TEST-RESULTS.md#audiolyricalignmenttest) | 供应商账单未在线核对 |
| F074 | 单曲整体偏移 | 正负调整与±600000ms范围，独立持久化 | 局部通过 | [StorageAndAssistantExtraTest](TEST-RESULTS.md#storageandassistantextratest) | 无法校正不同剪辑版的渐进漂移 |
| F075 | 原文件歌词写回 | 界面应明确平台是否支持实际写标签 | 未执行专门验收 | 源码/系统入口 | 能力有限，未执行真实原文件写回 |
| F076 | Android系统悬浮歌词 | 权限引导、后台切句、锁定/拖动/字号 | 局部通过 | [FloatingLyricsTest](TEST-RESULTS.md#floatinglyricstest) | 系统悬浮权限/跨应用实操未授权测试包，未执行 |
| F077 | 桌面悬浮歌词窗口 | 主窗隐藏后更新，拖动/锁定/解锁/关闭/重开 | 局部通过 | [DesktopWindowLifecycleTest](TEST-RESULTS.md#desktopwindowlifecycletest) | 隔离X11通过；Wayland和Windows实窗未人工验收 |
## 元数据与封面

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F078 | 公共元数据补全 | 标题歌手严格匹配，只改缺失字段 | 局部通过 | [MetadataEnricherTest](TEST-RESULTS.md#metadataenrichertest) | 公开目录在线命中率未测 |
| F079 | 批量补全与逐曲保存 | 取消边界、单曲失败隔离、checkpoint与报告 | 局部通过 | [MetadataEnricherTest](TEST-RESULTS.md#metadataenrichertest) | 断电恢复未实际断电验证 |
| F080 | 明确允许AI兜底 | 公共来源未命中且选项允许才使用模型 | 局部通过 | [MetadataEnricherTest](TEST-RESULTS.md#metadataenrichertest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | AI输出事实准确性未实测 |
| F081 | 真实专辑封面匹配 | QQ/iTunes/酷狗目录校验、按专辑缓存 | 局部通过 | [AlbumCoverRepositoryTest](TEST-RESULTS.md#albumcoverrepositorytest) | 具体再版/合辑版本无法保证 |
| F082 | 自选封面与批量缺图匹配 | 保护手选图，不被后续自动任务覆盖 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 原生图片选择器未逐项点击 |
| F083 | 封面解码、回退和内存缓存 | 内嵌封面回退、并发合并、清缓存后旧结果拒绝 | 发现缺陷（隔离复现） | [ArtworkDecodeTest](TEST-RESULTS.md#artworkdecodetest)、[CatalogArtworkRematchTest](TEST-RESULTS.md#catalogartworkrematchtest)、[MusicFeaturesIntegrationTest](TEST-RESULTS.md#musicfeaturesintegrationtest) | 共享封面删除风险见[Q09](BUGS.md#q09) |
## 歌单包

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F084 | 单歌单和全库导出 | 音频/封面/元数据/关系往返，缺音频不导出为可播放 | 局部通过 | [PlainSyncPackageTest](TEST-RESULTS.md#plainsyncpackagetest)、[PlaylistArchiveFormatTest](TEST-RESULTS.md#playlistarchiveformattest) | 真实用户大库未打包 |
| F085 | 导出范围弹窗 | 默认全库、长歌单滚动、取消不导出、重开重置 | 局部通过 | [SyncPackageDialogTest](TEST-RESULTS.md#syncpackagedialogtest)、[SyncPackageDialogDeviceTest](TEST-RESULTS.md#syncpackagedialogdevicetest) | 真实系统保存选择器另验 |
| F086 | 导出命名与重名序号 | 歌单名/整个音乐库、非法字符清理、不覆盖已有导出包 | 局部通过 | [SyncPackageNamingTest](TEST-RESULTS.md#syncpackagenamingtest) | Windows路径实际保存未执行 |
| F087 | 处理中、完成位置和复制 | 任务跨页继续；结果显示实际位置与文件名 | 局部通过 | [SyncPackageFeedbackTest](TEST-RESULTS.md#syncpackagefeedbacktest)、[SyncPackageFeedbackDeviceTest](TEST-RESULTS.md#syncpackagefeedbackdevicetest) | 系统剪贴板交互不等于文件已写入 |
| F088 | 打开导出目录 | Android文档URI、新任务、无文件管理器回退 | 局部通过 | [AndroidExportLocationTest](TEST-RESULTS.md#androidexportlocationtest)、[SyncPackageFeedbackTest](TEST-RESULTS.md#syncpackagefeedbacktest) | 真实Downloads目录点击未在本轮手工验收 |
| F089 | 专用导入、普通导入和拖入包 | 单包/混合目录/空歌单/坏包隔离后刷新歌单 | 局部通过 | [DesktopSyncPackageImportTest](TEST-RESULTS.md#desktopsyncpackageimporttest)、[ApplicationUseCaseTest](TEST-RESULTS.md#applicationusecasetest) | Windows系统文件关联未验收 |
| F090 | 包格式安全校验 | SHA-256、白名单目录、路径逃逸、大小、旧加密包拒绝 | 发现缺陷（隔离复现） | [PlainSyncPackageTest](TEST-RESULTS.md#plainsyncpackagetest)、[PlaylistArchiveFormatTest](TEST-RESULTS.md#playlistarchiveformattest) | [Q01](BUGS.md#q01)提交时序、[Q08](BUGS.md#q08)引用数预算失败 |
| F091 | 导入失败回滚 | 歌单清单错误/后续文件失败时原音频应不变 | 发现缺陷（隔离复现） | 源码/系统入口 | [Q01](BUGS.md#q01)：隔离实测失败，现有实现无整包事务 |
## 歌源与下载

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F092 | 五个内置歌源和增量迁移 | 网易/酷我/酷狗/QQ/Archive，保留旧停用和移除状态 | 局部通过 | [BuiltinMusicSourcesTest](TEST-RESULTS.md#builtinmusicsourcestest)、[MusicSourceRegistryTest](TEST-RESULTS.md#musicsourceregistrytest) | 内置资源测试不代表公网每首可下载 |
| F093 | 恢复内置、启停与卸载 | 20源上限、卸载不删音频、主动恢复重新启用 | 局部通过 | [MusicSourceRegistryTest](TEST-RESULTS.md#musicsourceregistrytest)、[BuiltinMusicSourcesTest](TEST-RESULTS.md#builtinmusicsourcestest) | 真实第三方脚本兼容面有限 |
| F094 | 本地JS、URL和订阅安装 | 格式检查、安装时禁止联网、无效条目报错 | 局部通过 | [MusicSourceRegistryTest](TEST-RESULTS.md#musicsourceregistrytest) | 原生文件选择器未手工选择 |
| F095 | 脚本沙箱与执行预算 | 禁宿主反射/文件/API凭据，内存/栈/时限生效 | 局部通过 | [MusicSourceRegistryTest](TEST-RESULTS.md#musicsourceregistrytest) | 不是完整QuickJS安全审计 |
| F096 | 公网请求和重定向边界 | 拒绝内网/混合IP/用户信息，跨域不转发请求头 | 局部通过 | [MusicSourceRegistryTest](TEST-RESULTS.md#musicsourceregistrytest)、[PublicSourceDnsTest](TEST-RESULTS.md#publicsourcednstest) | 公网DNS重绑定长期对抗未执行 |
| F097 | VPN Fake-IP兼容 | 仅全198.18/15时固定HTTPS DNS回退，仍校验公网IP | 局部通过 | [PublicSourceDnsTest](TEST-RESULTS.md#publicsourcednstest) | 真实VPN网络切换未操作 |
| F098 | 统一在线搜索和分页条目ID | 并发3源交错、保留多次搜索ID、下载只用真实返回ID | 局部通过 | [MusicSourceRegistryTest](TEST-RESULTS.md#musicsourceregistrytest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 公开接口实时成功率未测 |
| F099 | 30秒试听与离页释放 | 64MiB限制，暂停主播放，离页清临时音频 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 两端实际试听听感与音频焦点未端到端验收 |
| F100 | 原版候选与试听片段过滤 | 过滤MV/现场/翻唱/异常时长；失败尝试下一源 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest)、[RecommendationPolicyTest](TEST-RESULTS.md#recommendationpolicytest) | 元数据规则不是录音真伪认证 |
| F101 | 来源下载与目标歌单入库 | 保留兼容编码、真实格式、登记来源与ownedAudio | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest)、[SourceDownloadTimeoutTest](TEST-RESULTS.md#sourcedownloadtimeouttest) | 真实公网下载未调用 |
| F102 | 下载取消、超时和失败汇总 | 区分响应超时/总时限、清临时文件、报告所有来源原因 | 局部通过 | [SourceDownloadTimeoutTest](TEST-RESULTS.md#sourcedownloadtimeouttest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 极慢/断续移动网络未实测 |
## B站

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F103 | 链接、BV、短链接和分P检查 | 解析视频/分P信息，拒绝不支持地址 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 真实B站metadata/账号环境未请求 |
| F104 | 导入前编辑信息和封面 | 编辑歌名歌手专辑/封面/歌单，MP3真实写标签 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest)、[MusicFeaturesIntegrationTest](TEST-RESULTS.md#musicfeaturesintegrationtest) | 真实账号下载未验通 |
| F105 | 音频/CDN回退与视频来源去重 | DASH/fallback、来源bvid:cid复用、原输入保留 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 未在线调用B站CDN |
| F106 | 扫码、网页登录和手动Cookie | 扫码轮询状态、Cookie合并、412真实错误 | 局部通过 | [BilibiliLoginServiceTest](TEST-RESULTS.md#bilibililoginservicetest) | 真实扫码/WebView登录未执行，不读取已有Cookie |
## AI助手

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F107 | 模型连接与独立视觉设置 | 千问/DeepSeek/豆包/自定义HTTPS、模型切换与预设 | 局部通过 | [MusicAiWireTest](TEST-RESULTS.md#musicaiwiretest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 真实供应商/Key未调用 |
| F108 | HTTPS协议、取消与用量 | tools/图片/响应解析、错误、超时、连接取消 | 局部通过 | [MusicAiWireTest](TEST-RESULTS.md#musicaiwiretest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 假传输；不证明供应商现网在线 |
| F109 | 多会话创建切换删除清空 | 最多30会话、当前会话16条、无跨会话上下文 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 界面全部会话管理按钮未逐一人工操作 |
| F110 | 真实曲库查询与歌单工具 | 按真实ID查询、分页、建立/重命名/删关系 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest)、[AssistantToolSessionTest](TEST-RESULTS.md#assistanttoolsessiontest) | 不把工具requested误写为操作完成 |
| F111 | 收藏和元数据工具 | 只对真实ID执行，输出不泄漏本地路径 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 不等于所有平台真实标签写回 |
| F112 | 音频删除授权 | 问法/解释/否定/只删歌单不能获得delete_tracks能力 | 发现缺陷（隔离复现） | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | [Q04](BUGS.md#q04)：三种非执行/否定语句仍放行 |
| F113 | 播放、暂停、seek和翻页动作 | 内存队列交前台确认，重启不重放 | 局部通过 | [StorageAndAssistantExtraTest](TEST-RESULTS.md#storageandassistantextratest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 前台缺音源恢复需端到端复核 |
| F114 | 本地帮助与教程工具 | 真实入口检索、不使用模型、提供可点教程 | 局部通过 | [StorageAndAssistantExtraTest](TEST-RESULTS.md#storageandassistantextratest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 教程锚点按设备结果判定 |
| F115 | 音乐链接附件与追加队列 | 只解析明确resonance链接，未知ID不猜配 | 局部通过 | [AssistantMusicLinksTest](TEST-RESULTS.md#assistantmusiclinkstest)、[UiVisualContractTest](TEST-RESULTS.md#uivisualcontracttest) | 外部URL不当作本地曲目 |
| F116 | 搜索和下载工具 | 保留多次搜索ID，未知ID失败，不伪称完成 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 真实歌曲版权/在线可用率不在夹具结果内 |
| F117 | 重复工具、上下文压缩和无进展暂停 | 相同参数复用；最近4组/有效JSON；连续6轮无进展停止 | 局部通过 | [AssistantToolSessionTest](TEST-RESULTS.md#assistanttoolsessiontest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 供应商实际成本账单未重建 |
| F118 | 长任务、取消、任务重试 | 持续有新成功动作不限总轮数；取消保留已入库 | 局部通过 | [AssistantToolSessionTest](TEST-RESULTS.md#assistanttoolsessiontest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 不自动重放聊天/删除/收费识别 |
| F119 | 月用量和操作分类统计 | 已知tokens/未返回用量失败分开，旧月度字段兼容 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 字符诊断不是token估算；无实际账单核对 |
## 截图识别

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F120 | 内置离线OCR模型 | 中英合成截图实际推理，无Key/不上传图片 | 局部通过 | [ScreenshotOcrTest](TEST-RESULTS.md#screenshotocrtest)、[LocalScreenshotOcrTest](TEST-RESULTS.md#localscreenshotocrtest) | 复杂旋转照片和任意排版未保证 |
| F121 | 长图分片和像素上限 | 宽1600/片高1800/重叠120，40片/30MB上限 | 局部通过 | [ArtworkDecodeTest](TEST-RESULTS.md#artworkdecodetest)、[LocalScreenshotOcrTest](TEST-RESULTS.md#localscreenshotocrtest)、[MusicFeaturesIntegrationTest](TEST-RESULTS.md#musicfeaturesintegrationtest) | 超过上限应提示分批 |
| F122 | 歌单布局解析 | 标题/歌手配对、数值曲名、排名/UI控件过滤、重叠去重 | 局部通过 | [ScreenshotOcrTest](TEST-RESULTS.md#screenshotocrtest) | 真实用户截图库准确率未测 |
| F123 | 截断名称证据核对 | 本地/公开目录/有证据B站标题；歧义不编造 | 局部通过 | [ScreenshotOcrTest](TEST-RESULTS.md#screenshotocrtest) | 真实公开目录在线命中未测 |
| F124 | 本地/云端后端与缓存隔离 | 后端/备注/图摘要不同不误用缓存，不偷偷云端兜底 | 局部通过 | [ScreenshotOcrTest](TEST-RESULTS.md#screenshotocrtest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 视觉API未真实调用 |
| F125 | 识别预览、勾选与编辑确认 | 取消不下载；编辑歌单和歌曲；草稿消费一次 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 进程终止仅保留OCR缓存，不保留草稿 |
| F126 | 确认后逐首导入与失败明细 | 先本地匹配，再来源下载，300首/20歌单边界 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 真实300首大任务未端到端执行 |
## 每日推荐与反馈

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F127 | 默认关闭、数量和间隔 | 1-168小时/1-30首配置往返、手动刷新 | 局部通过 | [RecommendationPolicyTest](TEST-RESULTS.md#recommendationpolicytest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 系统后台周期不是精确定时 |
| F128 | 歌源模式与AI模式 | 歌源不调用模型；AI模式使用显式连接 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest)、[RecommendationFeedbackPolicyTest](TEST-RESULTS.md#recommendationfeedbackpolicytest) | 未实测模型推荐质量 |
| F129 | 未听保护与到期状态 | 未听批次不自动换；手动可换；设置显示等待原因 | 局部通过 | [RecommendationPolicyTest](TEST-RESULTS.md#recommendationpolicytest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 通知与所有时区边界未穷举 |
| F130 | 失败冷却和旧配置升级 | 尝试前落盘，失败也等待间隔；手动立即重试 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 使用假时钟/假AI，不产生费用 |
| F131 | 不计费网络和后台调度 | 自动等待允许网络；手动刷新显式联网 | 局部通过 | [RecommendationPolicyTest](TEST-RESULTS.md#recommendationpolicytest) | 真实计费切换、WorkManager节电未执行 |
| F132 | 历史去重与拒绝过滤 | NFKC歌曲身份、7天冷却、365天历史 | 局部通过 | [RecommendationPolicyTest](TEST-RESULTS.md#recommendationpolicytest)、[RecommendationFeedbackPolicyTest](TEST-RESULTS.md#recommendationfeedbackpolicytest) | 不能识别所有相同/不同录音 |
| F133 | 推荐队列点播与缺音源恢复 | 只在本批有效音频切歌；恢复迟到不抢播 | 局部通过 | [RecommendationPlaybackTest](TEST-RESULTS.md#recommendationplaybacktest)、[PlaybackQueueResolutionTest](TEST-RESULTS.md#playbackqueueresolutiontest) | 真实公网恢复未测 |
| F134 | 新批提交、pending和中断恢复 | 至少一首成功才替换；全失败保留旧批；逐首pending | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 断电/硬盘故障未注入 |
| F135 | 临时旧曲清理保护 | 收藏/自建歌单/当前播放/共享路径不误删 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest)、[RecommendationPolicyTest](TEST-RESULTS.md#recommendationpolicytest) | 仅应用托管临时音频夹具 |
| F136 | 单首/多首/全批保存歌单 | 已有或新歌单，保留顺序和去重 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 旧批次边界见自动化清单 |
| F137 | 点赞/踩互斥及撤销 | 不等于收藏，踩不删文件，撤销解除旧拒绝 | 局部通过 | [RecommendationFeedbackPolicyTest](TEST-RESULTS.md#recommendationfeedbackpolicytest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 本地偏好，不是公共社交评论 |
| F138 | 评论编辑清空和历史反馈 | 500字上限、与投票独立、换批后保留 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest)、[RecommendationFeedbackPolicyTest](TEST-RESULTS.md#recommendationfeedbackpolicytest) | 任意自然语言语义不保证理解 |
| F139 | 不喜欢并删除与原因 | 显式删除音频/关系，原因进入后续推荐 | 发现缺陷（隔离复现） | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 底层删除失败语义另见[Q11](BUGS.md#q11) |
## 下载与空间

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F140 | 托管音频统计和精确重复 | 容量/保存状态/可选SHA，路径边界校验 | 局部通过 | [StorageAndAssistantExtraTest](TEST-RESULTS.md#storageandassistantextratest)、[MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 大库hash性能未压力测量 |
| F141 | 清理预览与保存保护 | 默认保护永久保存/收藏/歌单/正在播放/共享文件 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 外部用户歌曲不作为测试材料 |
| F142 | 显式释放已保存音频 | 删除托管文件，保留ID/收藏/歌单并置空来源 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 跨文件元数据失败非事务 |
| F143 | 按来源重新下载恢复 | 沿用旧ID/关系，更新歌单sourceUri | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest) | 原歌源不可用时可失败，公网未测 |
| F144 | 手动选择替代音频 | 取消不改数据；替换保留ID/收藏/关系 | 局部通过 | [ApplicationUseCaseTest](TEST-RESULTS.md#applicationusecasetest) | Android/桌面系统选择器本轮未点击 |
| F145 | 临时空间预算 | 64-8192MB配置，超限停止，不清已保存文件 | 局部通过 | [StorageAndAssistantExtraTest](TEST-RESULTS.md#storageandassistantextratest) | 下载后才检查预算；真实磁盘满未模拟 |
| F146 | 来源与封面溯源 | 区分目录图/歌源标注/视频图/自选与未知 | 局部通过 | [MusicAssistantTest](TEST-RESULTS.md#musicassistanttest)、[AlbumCoverRepositoryTest](TEST-RESULTS.md#albumcoverrepositorytest) | 来源证据不认证母带 |
## 智能管家与画像

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F147 | 曲库体检和垃圾标签清理 | 去前后缀/扩展名、拆歌手、规范未知专辑 | 局部通过 | [SmartLibraryAuditEngineTest](TEST-RESULTS.md#smartlibraryauditenginetest) | 默认建议仍需用户审阅 |
| F148 | 多版本去重与保留建议 | Studio/Live/Remix不得默认互删 | 发现缺陷（隔离复现） | [SmartLibraryAuditEngineTest](TEST-RESULTS.md#smartlibraryauditenginetest) | [Q05](BUGS.md#q05)：现场版仍被预选清理 |
| F149 | 智能管家批量清理接线 | 选多首应完整确认并报告逐首结果 | 源码确认缺口 | 源码/系统入口 | [Q06](BUGS.md#q06)：循环覆盖单曲确认，仅最后一首 |
| F150 | 六维品味雷达与性格评语 | 空库/关键词样本、歌手分布和比例边界 | 局部通过 | [TasteAnalyticsEngineTest](TEST-RESULTS.md#tasteanalyticsenginetest) | 基于元数据规则，不是音频分析结果 |
| F151 | 听歌时长胶囊和分享卡片 | 统计快照可用，海报内容/复制文案 | 局部通过 | [ListeningStatsRepositoryTest](TEST-RESULTS.md#listeningstatsrepositorytest) | 海报导出/系统分享未实际执行 |
| F152 | 本地音频特征提取 | 节拍/声学分析算法与缓存 | 算法通过；产品未接入 | [AudioFeatureAnalyzerTest](TEST-RESULTS.md#audiofeatureanalyzertest) | 算法单测通过；未接生产解码/搜索链，不能当可用功能 |
## 一起听

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F153 | 创建房间和邀请查看 | 明确创建，随机凭据、邀请过期/刷新 | 相关回归有失败 | [ListenTogetherTest](TEST-RESULTS.md#listentogethertest)、[TogetherSessionTest](TEST-RESULTS.md#togethersessiontest)、[TogetherUiDeviceTest](TEST-RESULTS.md#togetheruidevicetest) | 真实公网房间未创建 |
| F154 | 粘贴邀请和外部邀请确认 | 展示预览，确认前不联网，取消不加入 | 相关回归有失败 | [TogetherUiDeviceTest](TEST-RESULTS.md#togetheruidevicetest)、[TogetherConnectionBoundsTest](TEST-RESULTS.md#togetherconnectionboundstest) | 真实操作系统分享/深链未逐项实测 |
| F155 | 房主审批与拒绝成员 | 未批准不进入，拒绝/超时清理连接 | 局部通过 | [ListenTogetherTest](TEST-RESULTS.md#listentogethertest)、[TogetherCompanionTest](TEST-RESULTS.md#togethercompaniontest)、[TogetherCompanionUiDeviceTest](TEST-RESULTS.md#togethercompanionuidevicetest) | 跨公网并发加入未测 |
| F156 | TLS证书和加密音频传输 | 证书固定、摘要校验、临时音频边界 | 局部通过 | [ListenTogetherTest](TEST-RESULTS.md#listentogethertest)、[TogetherConnectionDeviceTest](TEST-RESULTS.md#togetherconnectiondevicetest) | 本地回环；不是互联网渗透测试 |
| F157 | 共享队列和点歌审批 | 显式队列、候选、批准/拒绝、预取取消 | 局部通过 | [TogetherSessionTest](TEST-RESULTS.md#togethersessiontest)、[TogetherSuggestionsTest](TEST-RESULTS.md#togethersuggestionstest)、[TogetherPrefetchCancellationTest](TEST-RESULTS.md#togetherprefetchcancellationtest) | 多人真实解码与听感同步未验证 |
| F158 | 播放/暂停/seek/切歌同步 | 角色门禁、远端状态、准备确认与驱动器 | 局部通过 | [TogetherPlaybackDriverTest](TEST-RESULTS.md#togetherplaybackdrivertest)、[TogetherSessionTest](TEST-RESULTS.md#togethersessiontest) | 未做Windows+手机实际双设备播放 |
| F159 | 同步歌词与AI候选 | 同步原歌词来源，候选不越过房主审批 | 局部通过 | [TogetherSuggestionsTest](TEST-RESULTS.md#togethersuggestionstest)、[ListenTogetherTest](TEST-RESULTS.md#listentogethertest) | 真实模型未调用 |
| F160 | 聊天投递、记录与清空 | 本机留存、消息边界、并发清理和重连语义 | 局部通过 | [TogetherCompanionTest](TEST-RESULTS.md#togethercompaniontest) | 跨设备离线长时记录未验证 |
| F161 | 常听伙伴、配对和撤销 | 配对/撤销/邀请与资料边界 | 局部通过 | [TogetherCompanionTest](TEST-RESULTS.md#togethercompaniontest)、[TogetherCompanionDeviceTest](TEST-RESULTS.md#togethercompaniondevicetest) | 进程停止时没有离线推送 |
| F162 | 有限断线恢复与邀请刷新 | 过期拒绝、按成员路由恢复、连接数量限制 | 局部通过 | [TogetherConnectionBoundsTest](TEST-RESULTS.md#togetherconnectionboundstest)、[TogetherSessionTest](TEST-RESULTS.md#togethersessiontest) | 真实网络抖动/蜂窝切换未执行 |
| F163 | 服务器路由与旧直连兼容 | 带服务器邀请只走服务器，不退回直连暴露设备地址 | 局部通过 | [TogetherServerRoutingTest](TEST-RESULTS.md#togetherserverroutingtest) | 真实公开服务与证书未探测 |
| F164 | WSS中继和寻址协议 | 回环WebSocket/TLS往返、签名发布/查询 | 局部通过 | [TogetherWebSocketTest](TEST-RESULTS.md#togetherwebsockettest)、[TogetherRendezvousTest](TEST-RESULTS.md#togetherrendezvoustest) | 生产地址验收跳过；回环不是跨网验证 |
| F165 | Android后台锁与通知/桌面托盘 | 后台连接存活，锁有界续期与通知权限 | 未执行专门验收 | 源码/系统入口 | 真实锁屏/省电/通知/两小时房间未执行 |
| F166 | 邀请复制与分享固定入口 | 滚动后仍能找到邀请和退出，队列首页限制 | 相关回归有失败 | [TogetherUiDeviceTest](TEST-RESULTS.md#togetheruidevicetest)、[TogetherCompanionUiDeviceTest](TEST-RESULTS.md#togethercompanionuidevicetest) | 系统分享目标未逐项点击 |
## 设置、天气与系统

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F167 | 深色/浅色/跟随系统主题 | 设置往返、弹窗可读、窗口背景一致 | 发现缺陷（隔离复现） | [WeatherPaletteTest](TEST-RESULTS.md#weatherpalettetest)、[UiVisualContractTest](TEST-RESULTS.md#uivisualcontracttest)、[LibraryDetailTransitionTest](TEST-RESULTS.md#librarydetailtransitiontest) | 坏主设置恢复失败见[Q03](BUGS.md#q03) |
| F168 | 天气配色、城市搜索和定位 | 天气码映射与缓存，定位失败可手选城市 | 局部通过 | [WeatherPaletteTest](TEST-RESULTS.md#weatherpalettetest) | 真实城市/定位权限/天气服务请求未执行 |
| F169 | 减少动态与系统动态偏好 | 键盘/减少动态即时或短淡变，不做封面飞行 | 局部通过 | [UiVisualContractTest](TEST-RESULTS.md#uivisualcontracttest)、[PlayerExpansionTest](TEST-RESULTS.md#playerexpansiontest) | 系统级动态设置切换未逐项实测 |
| F170 | 八组设置子页和帮助 | 打开/返回/滚动、保留实际配置回调 | 相关回归有失败 | [UiVisualContractTest](TEST-RESULTS.md#uivisualcontracttest)、[TutorialNavigationTest](TEST-RESULTS.md#tutorialnavigationtest) | 本轮实机结果按测试明细判断 |
| F171 | 桌面窗口、托盘与完全退出 | 隐藏主窗继续歌词，完全退出排空和释放 | 局部通过 | [DesktopWindowLifecycleTest](TEST-RESULTS.md#desktopwindowlifecycletest)、[ApplicationSessionTest](TEST-RESULTS.md#applicationsessiontest) | 桌面托盘真实菜单/Windows未操作 |
| F172 | 凭据加密和资料目录隔离 | Linux AES-GCM/AndroidKeyStore、无效数据恢复 | 局部通过 | [LinuxDesktopSupportTest](TEST-RESULTS.md#linuxdesktopsupporttest)、[MusicFeaturesIntegrationTest](TEST-RESULTS.md#musicfeaturesintegrationtest) | Windows DPAPI真实环境未执行；未读取用户Key |
| F173 | 偏好顺序写入和备份恢复 | 捕获操作值、submitLatest、取消不破坏worker | 发现缺陷（隔离复现） | [PersistenceQueueTest](TEST-RESULTS.md#persistencequeuetest)、[ApplicationUseCaseTest](TEST-RESULTS.md#applicationusecasetest)、[MusicJsonStoreTest](TEST-RESULTS.md#musicjsonstoretest) | 部分专用设置入口仍绕过恢复[Q03](BUGS.md#q03) |
| F174 | Android设置写盘失败反馈 | commit=false应向界面报告保存失败 | 源码确认缺口 | 源码/系统入口 | [Q10](BUGS.md#q10)：部分入口忽略返回值，仅源码确认 |
| F175 | 关于、版本与更新说明 | 版本来自version.properties，离线展示对应发布内容 | 局部通过 | [HorizontalPageSwipeUnitTest](TEST-RESULTS.md#horizontalpageswipeunittest) | 未检查GitHub发布链接在线状态 |
## 独立官网与服务

| 编号 | 功能 | 操作与期望 | 本次结论 | 测试证据 | 限制/发现 |
| --- | --- | --- | --- | --- | --- |
| F176 | 首页/下载/论坛/支持导航 | 公共页返回成功，无作者入口或过时内容泄漏 | 局部通过 | [本地website套件](FEATURE-TEST-REPORT.md) | Flask客户端测试；真实浏览器布局未验收 |
| F177 | 安装包直链与加速、Range和缓存 | 实际大小/hash、Range/ETag、无会话Cookie | 局部通过 | [本地website套件](FEATURE-TEST-REPORT.md) | 微型安装包夹具；CDN生产缓存未探测 |
| F178 | 作者上传版本与旧包清理 | 拒绝降版/格式错误，失败保留旧版，事务切换后清理 | 局部通过 | [本地website套件](FEATURE-TEST-REPORT.md) | 使用临时数据；未发布实际版本 |
| F179 | 匿名帖子、回复、昵称和截图 | 创建/查看、XSS转义、CSRF、上传边界 | 局部通过 | [本地website套件](FEATURE-TEST-REPORT.md) | 未向真实论坛发帖 |
| F180 | 话题申请、审批和排序删除 | 未审批不公开，只能删除空话题 | 局部通过 | [本地website套件](FEATURE-TEST-REPORT.md) | 后台真实账户未登录 |
| F181 | 作者登录、会话和看板 | 密码/会话过期、无默认密码、权限边界 | 局部通过 | [本地website套件](FEATURE-TEST-REPORT.md) | 作者私有地址/真实密码未读取 |
| F182 | 作者标记、置顶和精准删帖 | 作者回复优先、按ID/URL定位、关键词搜索 | 局部通过 | [本地website套件](FEATURE-TEST-REPORT.md) | 仅临时数据库 |
| F183 | 公告和版本介绍发布 | 校验版本后同事务更新，无效内容不覆盖 | 局部通过 | [本地website套件](FEATURE-TEST-REPORT.md) | 不真实群发邮件 |
| F184 | 订阅确认、推广选择和退订 | 推广默认关闭；确认链接/退订与CSRF | 局部通过 | [本地website套件](FEATURE-TEST-REPORT.md) | 模拟SMTP；真实邮件送达未验证 |
| F185 | 持久邮件队列、失败重试和收款码 | 队列重试、受众过滤、图片上传校验 | 局部通过 | [本地website套件](FEATURE-TEST-REPORT.md) | 未接真实订阅者/收款资料 |
| F186 | 连续导航的最后点击优先 | 先论坛后下载，迟到论坛不能覆盖下载 | 发现缺陷（隔离复现） | 源码/系统入口 | [Q12](BUGS.md#q12)：实际site.js函数隔离复现失败 |
| F187 | 响应式、减少动态和无缝切页视觉 | 320/390/768/1440宽度、返回与加载反馈 | 未执行专门验收 | 源码/系统入口 | 本轮未使用真实浏览器做视觉和可访问性验收 |
| F188 | 中继配对、容量和流量限制 | 双向字节、无效hello/等待超时清理、并发和日预算 | 局部通过 | [本地relay套件](FEATURE-TEST-REPORT.md) | 回环测试；不是公网压测 |
| F189 | 中继守护进程 | 启动/退出/失败恢复与资源清理 | 局部通过 | [本地relay套件](FEATURE-TEST-REPORT.md) | 未修改生产守护进程 |
| F190 | 签名寻址服务 | 签名/过期/速率/资料边界 | 局部通过 | [本地rendezvous套件](FEATURE-TEST-REPORT.md) | 可选旧架构；不是生产公网验收 |
| F191 | 部署性能工具 | 本地基准参数、统计与错误处理 | 局部通过 | [本地deployment套件](FEATURE-TEST-REPORT.md) | 未运行服务器迁移/Ubuntu部署/生产压测 |

## 明确不支持或已移除

- 播放器没有Web前端、账号系统或云端曲库；`website/`是独立网站。
- 设备/LAN/二维码歌单同步和酷狗歌单链接入口已移除；一起听邀请二维码与B站登录二维码仍存在，不能混为一项。
- Android不支持KGG v5转换；Windows需要对应酷狗密钥。KGMA仅实现的v3/slot1范围。
- 本地音频特征分析类存在，但未接生产自动提取和BPM搜索；歌词原文件写回能力有限。
- 背景推荐依赖进程/系统调度；彻底退出后不运行，没有进程外离线伙伴推送。

所有缺陷的前置条件、具体输出、优先级和修复验收条件见[问题复现报告](BUGS.md)。
