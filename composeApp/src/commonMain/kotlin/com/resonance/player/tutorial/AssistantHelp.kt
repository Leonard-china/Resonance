package com.resonance.player.tutorial

import com.resonance.player.model.LibraryDestination

data class AssistantHelpEntry(val title: String, val body: String, val page: LibraryDestination, val chapterId: String? = null)

/** The same shipped tutorial content powers help and guided UI; no invented controls or network. */
object AssistantHelp {
    private val extra = listOf(
        AssistantHelpEntry("导入歌源", "工具 → 歌源，可选择 MusicFree .js 文件或填写 HTTPS 地址。导入后可搜索、核对并下载；插件能力和依赖不兼容会显示实际错误。", LibraryDestination.Sources),
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
        val terms = listOf("歌源", "空间", "下载", "推荐", "歌单", "歌词", "封面", "版本", "收藏", "导入", "教程", "悬浮", "翻译", "音量")
            .filter(query::contains)
        return entries.map { entry -> entry to terms.count { (entry.title + entry.body).contains(it) } }
            .filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }.take(3)
    }
    fun isHelpQuestion(query: String): Boolean = listOf("怎么", "如何", "不会", "在哪", "哪里", "教我", "教程", "使用说明", "帮助").any(query::contains)
}
