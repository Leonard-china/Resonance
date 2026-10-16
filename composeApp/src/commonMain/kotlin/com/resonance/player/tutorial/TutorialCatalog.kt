package com.resonance.player.tutorial

import com.resonance.player.model.LibraryDestination

/** Local learning preferences, deliberately separate from the music library and AI settings. */
data class TutorialProgress(
    val welcomeDismissed: Boolean = false,
    val quickPage: Int = 0,
    val quickCompleted: Boolean = false,
    val chapters: Map<String, ChapterProgress> = emptyMap(),
)

data class ChapterProgress(
    val nextStep: Int = 0,
    val completed: Set<String> = emptySet(),
    val skipped: Set<String> = emptySet(),
)

enum class TutorialTarget {
    LocalImport, TrackList, MiniPlayer, PlayerLyrics, PlayerQueue, PlayerFavorite,
    CreatePlaylist, TrackMenu, BiliLogin, VideoImport, AiConfig, AiTest,
    AssistantSend, Screenshot, RecommendationSettings, DailyCard, TranslationSettings,
    SpeechSettings, FloatingLyrics, ExportArchive, ImportArchive, Tasks,
}

enum class TutorialCompletion {
    MusicAvailable, TrackSelected, PlayerOpened, LyricsViewed, QueueViewed, TrackFavorite,
    PlaylistCreated, TrackAdded, CookieAvailable, VideoFormOpened, AiConfigured, ChatSent,
    RecommendationsEnabled, DailyOpened, FloatingEnabled,
}

data class TutorialStep(
    val id: String,
    val title: String,
    val body: String,
    val target: TutorialTarget,
    val destination: LibraryDestination,
    val completion: TutorialCompletion? = null,
    val needsTrack: Boolean = false,
    val libraryTab: String? = null,
)

data class TutorialChapter(val id: String, val title: String, val description: String, val steps: List<TutorialStep>)
data class QuickLesson(val title: String, val body: String, val points: List<String>, val illustration: List<String>)

object TutorialCatalog {
    val quick = listOf(
        QuickLesson("让音乐来到你的曲库", "从自己的音乐文件开始，也可以导入喜欢的 B 站视频音频。", listOf(
            "工具 → 导入音乐：扫描本机，或选择文件夹。",
            "KGMA 可以转换为 MP3，源文件会保留；KGG 转换仅 Windows 支持。",
            "B 站导入支持链接、BV 号和分 P，可修改歌名、歌手与专辑。",
        ), listOf("导入音乐", "扫描本机音乐", "选择文件夹并转换", "从 B 站视频导入音频")),
        QuickLesson("从第一首歌开始", "点选歌曲播放，点击底部播放器展开完整的听歌界面。", listOf(
            "在封面、歌词和队列之间切换，收藏喜欢的歌曲。",
            "用搜索、歌单、专辑和歌手浏览音乐。",
            "统一播放音量在本机分析响度，不改写原始音频。",
        ), listOf("正在播放", "歌曲 · 歌手", "播放 / 暂停", "封面     歌词     队列")),
        QuickLesson("让助手帮你整理", "用聊天管理歌单，也可以发送歌单截图，查找并导入音乐。", listOf(
            "设置 → 配置模型与 API Key：填写自己的连接并测试。",
            "文本管理需要支持工具调用；截图识别需要支持图片的模型。",
            "调用由所选服务商计费；本地听歌可以直接使用。",
        ), listOf("音乐助手", "新对话     历史", "帮我整理一份通勤歌单", "歌单截图     发送")),
        QuickLesson("按自己的节奏探索", "每日推荐、桌面歌词和歌单文件迁移，都可以以后慢慢体验。", listOf(
            "推荐歌曲收藏或加入自建歌单后，可避免下批自动清理。",
            "推荐页“不喜欢”会删除实际音频；普通删除歌曲也可能删除原文件。",
            "教程始终保留在设置中，可以查说明，也可以跟着操作。",
        ), listOf("使用教程", "快速了解", "跟着操作 · 按章学习", "随时退出，保存进度")),
    )

    private fun step(id: String, title: String, body: String, target: TutorialTarget, page: LibraryDestination,
                     completion: TutorialCompletion? = null, track: Boolean = false, tab: String? = null) =
        TutorialStep(id, title, body, target, page, completion, track, tab)

    val chapters = listOf(
        TutorialChapter("first-song", "第一次听歌", "导入、播放、歌词与队列", listOf(
            step("import", "添加第一首音乐", "点击高亮的“扫描本机音乐”，按系统提示选择或授权音频。也可选择文件夹转换 KGMA（Windows 另支持 KGG）。导入完成后继续；已有歌曲会自动跳过准备。取消或失败时可重试，也可跳过本步。", TutorialTarget.LocalImport, LibraryDestination.Import, TutorialCompletion.MusicAvailable),
            step("play", "点选一首歌曲", "点击歌曲开始播放。找不到歌曲时，先到工具中导入音乐；教程不会自动播放或下载歌曲。", TutorialTarget.TrackList, LibraryDestination.Library, TutorialCompletion.TrackSelected, tab = "Tracks"),
            step("expand", "展开播放器", "点击底部显示歌名的区域，打开完整播放器。播放按钮仍可用于暂停和继续。", TutorialTarget.MiniPlayer, LibraryDestination.Library, TutorialCompletion.PlayerOpened, true),
            step("lyrics", "查看歌词", "点击“歌词”。同步歌词跟随播放位置；纯文本歌词可手动浏览。暂时没有歌词也可以继续学习。", TutorialTarget.PlayerLyrics, LibraryDestination.Playing, TutorialCompletion.LyricsViewed, true),
            step("queue", "看看接下来播放什么", "点击“队列”，查看当前播放列表。完成后可继续听歌，或退出教学。", TutorialTarget.PlayerQueue, LibraryDestination.Playing, TutorialCompletion.QueueViewed, true),
        )),
        TutorialChapter("organize", "整理音乐", "收藏、自建歌单与歌曲关系", listOf(
            step("favorite", "收藏喜欢的歌曲", "点击心形图标收藏当前歌曲。已收藏时会自动继续；无需为了练习取消收藏。没有当前歌曲时，可先返回曲库选择一首。", TutorialTarget.PlayerFavorite, LibraryDestination.Playing, TutorialCompletion.TrackFavorite, true),
            step("create", "建立自己的歌单", "点击右上角“新建歌单”，输入名称并确认。已有自建歌单时可直接继续。删除歌单只移除歌单关系，保留音频。", TutorialTarget.CreatePlaylist, LibraryDestination.Library, TutorialCompletion.PlaylistCreated, tab = "Playlists"),
            step("membership", "把歌曲加入歌单", "打开当前歌曲右侧的更多菜单，选择“加入”目标歌单。歌曲可属于多个歌单；移出歌单不会删除音频。", TutorialTarget.TrackMenu, LibraryDestination.Library, TutorialCompletion.TrackAdded, true, "Tracks"),
            step("delete-note", "分清移出和删除", "“从歌单移除”只改变关系；“删除本地歌曲”会尝试删除实际音频，包括应用外导入的文件。本步只阅读说明，无需执行删除。", TutorialTarget.TrackMenu, LibraryDestination.Library, tab = "Tracks"),
        )),
        TutorialChapter("bilibili", "B 站音频导入", "登录、视频信息与下载任务", listOf(
            step("login", "登录自己的 B 站账号", "点击“扫码登录”，用 B 站 App 扫码并确认。Android 也支持网页快捷登录。凭据保存在本机；可跳过登录，遇到官方验证或限制时再配置。", TutorialTarget.BiliLogin, LibraryDestination.Settings, TutorialCompletion.CookieAvailable),
            step("form", "打开视频导入", "点击“从 B 站视频导入音频”，填写链接或 BV 号，读取信息后核对分 P、歌名、歌手、专辑、封面和目标歌单。只有确认下载才会导入音频，可随时取消。", TutorialTarget.VideoImport, LibraryDestination.Import, TutorialCompletion.VideoFormOpened),
            step("tasks", "查看处理进度", "确认下载后，可在任务页查看下载、转码、封面和歌词等步骤。失败时按具体步骤重试，封面失败无需重新下载音频。没有发起下载也可读完本步。", TutorialTarget.Tasks, LibraryDestination.Library),
        )),
        TutorialChapter("assistant", "音乐助手", "连接模型、聊天和歌单截图", listOf(
            step("configure", "配置自己的模型", "模型设置已展开。选择服务商，填写下方的 HTTPS 地址、模型名称与 API Key，保存并选择“用于音乐管理”。文本模型需支持工具调用。已有可用配置时自动继续；本地播放无需配置 AI。", TutorialTarget.AiConfig, LibraryDestination.Settings, TutorialCompletion.AiConfigured),
            step("test", "测试连接", "“测试连接”会向服务商发送请求，可能产生用量。可主动测试并查看结果，也可点“已了解”继续。不要把 API Key 发到聊天或公开截图中。", TutorialTarget.AiTest, LibraryDestination.Settings),
            step("chat", "发起一次对话", "输入自己的需求再点击“发送”，例如“列出我的歌单”。请求会发送相关曲库摘要和当前会话上下文，由所选服务商计费。教程不会替你发送；不想调用时可跳过。", TutorialTarget.AssistantSend, LibraryDestination.Assistant, TutorialCompletion.ChatSent),
            step("screenshot", "从歌单截图开始", "“歌单截图”可选择长截图，图片会发给配置的视觉模型识别。发送后会查找、下载并整理歌曲，请核对自己的需求。历史会话独立；切换或新建会话本身不消耗 Token。", TutorialTarget.Screenshot, LibraryDestination.Assistant),
        )),
        TutorialChapter("recommendations", "每日推荐", "按需开启，了解保留与清理", listOf(
            step("retention", "先了解歌曲的保留规则", "推荐会自动下载歌曲。下一批成功后，未收藏且不在自建歌单的旧推荐会被物理删除；当前播放曲目延后清理。收藏或加入自建歌单可保留，取消保护后仍可能被清理。", TutorialTarget.RecommendationSettings, LibraryDestination.Settings),
            step("enable", "按需要开启推荐", "开启“自动推荐并下载”，设置数量和间隔。后台运行可选；Android 由系统调度，Windows 需保持托盘运行。生成会调用配置的模型并联网下载；不需要时可跳过。", TutorialTarget.RecommendationSettings, LibraryDestination.Settings, TutorialCompletion.RecommendationsEnabled),
            step("open", "找到每日推荐", "歌单页第一张卡片始终是每日推荐。已开启时点击进入推荐页面；未开启时会前往设置。", TutorialTarget.DailyCard, LibraryDestination.Library, TutorialCompletion.DailyOpened, tab = "Playlists"),
            step("dislike", "谨慎使用“不喜欢”", "推荐页的“不喜欢”会立即取消收藏、移出所有歌单并删除实际音频，即使此前保存过。可填写原因影响后续推荐。本步仅讲解，不要求尝试删除。", TutorialTarget.DailyCard, LibraryDestination.Library, tab = "Playlists"),
        )),
        TutorialChapter("lyrics", "歌词进阶", "翻译、音频同步与桌面歌词", listOf(
            step("display", "同步歌词与纯文本", "在完整播放器打开歌词。同步歌词随进度滚动；纯文本可手动阅读。自动匹配和本地 LRC/KRC 优先，找到文本并不意味着已有同步时间轴。", TutorialTarget.PlayerLyrics, LibraryDestination.Playing, TutorialCompletion.LyricsViewed, true),
            step("translation", "设置翻译语言", "在这里设置目标语言与原文／译文／双语显示，再到歌词页主动翻译。翻译会发送歌词文本并使用模型额度；本机缓存可复用，失败保留原文。", TutorialTarget.TranslationSettings, LibraryDestination.Settings),
            step("speech", "为音频生成同步歌词", "没有同步轴时，可在播放器菜单主动生成。应用会上传准备后的整首音频，由语音服务按时长计费，与聊天 Token 分开；失败或取消也可能计费。结果在本机缓存。教程只解释入口，不会发起识别。", TutorialTarget.SpeechSettings, LibraryDestination.Settings),
            step("floating", "开启桌面歌词", "开启“桌面悬浮歌词”。Android 首次需要按提示授予悬浮窗权限；Windows 直接显示。歌词可调整字号、拖动和锁定，锁定后可回设置解锁。若暂时不需要，可跳过。", TutorialTarget.FloatingLyrics, LibraryDestination.Settings, TutorialCompletion.FloatingEnabled),
        )),
        TutorialChapter("archive", "歌单迁移与排错", "导出、导入与失败重试", listOf(
            step("export", "导出歌单文件", "点击“导出歌单”，选择歌单或整个曲库。生成的 .resonance 文件含可读取的音频、封面和歌单关系，是明文 ZIP，请按音乐文件保管。不会包含 API Key 和 Cookie。本步可只了解入口。", TutorialTarget.ExportArchive, LibraryDestination.PlaylistArchive),
            step("import", "在另一台设备导入", "把 .resonance 文件传到另一台设备，选择“导入歌单”。校验通过后合并到本机。重复导入可能替换受控目录中的同名目标，无需为了练习重复导入。", TutorialTarget.ImportArchive, LibraryDestination.PlaylistArchive),
            step("retry", "从失败步骤继续", "任务页保留最近的处理步骤。网络失败时查看具体错误再重试；已入库音频会保留，应用中断后可手动继续。B 站官方验证、失效目录授权和模型连接问题需先处理原因。", TutorialTarget.Tasks, LibraryDestination.Library),
        )),
    )

    fun chapter(id: String?) = chapters.firstOrNull { it.id == id }
}

fun TutorialProgress.record(chapter: TutorialChapter, index: Int, skipped: Boolean): TutorialProgress {
    val step = chapter.steps.getOrNull(index) ?: return this
    val previous = chapters[chapter.id] ?: ChapterProgress()
    val next = previous.copy(nextStep = index + 1,
        completed = if (skipped) previous.completed - step.id else previous.completed + step.id,
        skipped = if (skipped) previous.skipped + step.id else previous.skipped - step.id)
    return copy(chapters = chapters + (chapter.id to next))
}
