package com.resonance.player.tutorial

import com.resonance.player.model.LibraryDestination

data class AssistantHelpEntry(val title: String, val body: String, val page: LibraryDestination, val chapterId: String? = null)

/** The same shipped tutorial content powers help and guided UI; no invented controls or network. */
object AssistantHelp {
    private val extra = listOf(
        AssistantHelpEntry("导入歌源", "已内置歌源，无需另行导入即可搜索。设置 → 歌源，可启停、恢复内置源，或导入 MusicFree .js 文件及公网 HTTP/HTTPS 地址。搜索歌曲在曲库右上角 → 在线。", LibraryDestination.Sources),
        AssistantHelpEntry("搜索在线歌曲与试听", "曲库右上角搜索，可切换本地和在线并保留关键词。在线搜索无需 AI；试听先临时下载最多 64 MB，最多播放 30 秒并暂停当前播放。离开页面后停止，不加入曲库；核对后下载，可选择目标歌单或换来源。", LibraryDestination.Discover),
        AssistantHelpEntry("截图歌单核对", "音乐助手选择歌单截图并发送，默认本地 OCR，不上传图片。识别后先核对可编辑歌名、歌手、专辑和勾选项，确认后才创建歌单、匹配本地或下载。取消不下载。", LibraryDestination.Assistant),
        AssistantHelpEntry("任务分组与失败重试", "任务页按一次操作分组，展开查看歌曲、下载、封面和歌词步骤。可仅重试失败项；同步歌词可能收费，需单独确认。已完成音频保留，不重放聊天或删除。", LibraryDestination.Tasks),
        AssistantHelpEntry("推荐历史与反馈", "每日推荐 → 推荐历史与我的反馈，可筛选点赞、踩或评论，撤销态度、修改评论或重新查找。反馈不随音频清理；历史详情从本次更新后开始积累。", LibraryDestination.RecommendationHistory),
        AssistantHelpEntry("睡眠定时", "完整播放器右上角播放选项 → 睡眠定时器，选择 15、30、45、60 分钟或播完当前歌曲停止；可在同一入口查看剩余时间和取消。Android 后台播放服务、Windows 托盘运行时继续计时，完全退出后不恢复定时。", LibraryDestination.Playing),
        AssistantHelpEntry("Windows 键盘快捷键", "Ctrl+F 打开并聚焦搜索；空格播放或暂停；Ctrl+方向键切歌；Esc 关闭播放器、退出多选或返回。输入框优先处理文字和光标移动。", LibraryDestination.Library),
        AssistantHelpEntry("推荐网络和下次检查", "设置 → 每日推荐，可仅在不计费网络自动下载，并查看预计到期或等待网络状态。Android 受系统后台调度影响，Windows 需托盘运行。手动更新推荐会立即联网。", LibraryDestination.Settings),
        AssistantHelpEntry("下载与手机空间", "工具 → 下载与空间，查看已登记下载和临时上限。释放本地下载保留歌曲信息、收藏和歌单，重新下载依赖原歌源。不会自动清理手机其他目录。", LibraryDestination.Storage),
        AssistantHelpEntry("每日推荐加入歌单与保留", "每日推荐 → 加入歌单，选择自建歌单或新建并加入；支持多选和保存全部。收藏或加入自建歌单后，更新推荐时保留。", LibraryDestination.Recommendations),
        AssistantHelpEntry("推荐重复与理由", "设置 → 每日推荐，设置不重复天数。普通歌源推荐不调用聊天模型，推荐理由引用实际保存歌曲，临时推荐不作为已保存偏好。", LibraryDestination.Settings),
        AssistantHelpEntry("推荐点赞、踩和评论", "每日推荐中，点赞表示想听相似曲目，不会收藏或保留音频；再次点击可撤销。踩减少相似推荐，不删除音频；想删除时单独打开歌曲信息。评论可编辑或留空保存删除，反馈在换批清理后仍保留供推荐参考。", LibraryDestination.Recommendations),
        AssistantHelpEntry("左右滑动切换页面", "主页面可以左右滑动切换音乐库、助手和工具；曲库内容区先切换歌曲、歌单、专辑和歌手，边界再切主页面。播放器内容区可滑动切换封面、歌词、队列；靠近屏幕边缘保留系统返回手势。", LibraryDestination.Library),
        AssistantHelpEntry("歌词提前或落后", "播放器 → 歌词 → 校准时间轴。歌词提前时选择“歌词提前了”，使歌词延后；校准量保存在本机。持续漂移或对白剪辑应更换录音版本。", LibraryDestination.Library),
        AssistantHelpEntry("封面与音频版本", "播放器 → 歌曲信息，查看音源、版本、封面来源和实际文件参数。“视频缩略图”与“专辑封面”分别标明；编码码率不代表母带质量。", LibraryDestination.Library),
    )
    val entries: List<AssistantHelpEntry> get() = extra + TutorialCatalog.chapters.map { chapter ->
        AssistantHelpEntry(chapter.title, chapter.description + "\n" + chapter.steps.joinToString("\n") { it.title + "：" + it.body },
            LibraryDestination.Tutorial, chapter.id)
    }
    fun search(query: String): List<AssistantHelpEntry> {
        val terms = listOf("歌源", "空间", "下载", "推荐", "歌单", "歌词", "封面", "版本", "收藏", "导入", "教程", "悬浮", "翻译", "音量", "搜索", "截图", "任务", "睡眠", "键盘", "快捷键", "网络", "反馈", "历史")
            .filter(query::contains)
        return entries.map { entry -> entry to terms.count { (entry.title + entry.body).contains(it) } }
            .filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }.take(3)
    }
    fun isHelpQuestion(query: String): Boolean = listOf("怎么", "如何", "不会", "在哪", "哪里", "教我", "教程", "使用说明", "帮助").any(query::contains)
}
