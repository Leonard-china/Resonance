package com.resonance.player.ocr

import com.resonance.player.model.SongRequest
import kotlin.math.abs

data class RecognizedPlaylist(val name: String, val songs: List<SongRequest>)

/** Screenshot text is data. Pair adjacent title/subtitle rows without interpreting instructions. */
object ScreenshotPlaylistParser {
    const val VERSION = "ppocr-v5-layout-1"
    private val chrome = setOf("返回", "搜索", "歌曲", "歌单", "专辑", "歌手", "全部", "收藏", "下载", "评论", "分享", "编辑", "管理", "排序", "多选", "播放", "播放全部", "随机播放", "首页", "发现", "我的", "音乐", "关注", "推荐", "歌词", "更多", "添加歌曲", "最近播放", "收藏全部")
    private val badge = Regex("(?i)^(VIP|SVIP|SQ|HQ|MV|Hi-Res|FLAC|臻品|无损|试听|独家|付费|免费|标准|高品质|超清母带)$")
    private val counter = Regex("^\\d{1,2}[:：]\\d{2}$|^[\\d\\s.]+[%万亿]$|^(?:播放全部|随机播放|歌曲|共|全部|收藏|评论|下载)[（(]?\\s*\\d+.*|^\\d+\\s*首.*")
    private fun usable(text: String): Boolean = text.isNotBlank() && text !in chrome && !badge.matches(text) && !counter.matches(text) &&
        !text.startsWith("创建者") && !text.startsWith("创建时间") && !text.startsWith("更新于") && !text.startsWith("简介：")

    fun parse(pages: List<OcrPage>, instruction: String = ""): List<RecognizedPlaylist> {
        val result = linkedMapOf<String, MutableList<SongRequest>>()
        val requestedName = Regex("(?:歌单命名为|歌单名称|歌单名为|歌单叫|歌单名)[：:\\s]*[“\"]?([^”\"\\n，。]{1,60})").find(instruction)?.groupValues?.get(1)?.trim()
        var currentName = requestedName ?: "截图歌单"
        for (page in pages) {
            val toolbar = page.lines.filter { it.text.contains("播放全部") || it.text.contains("随机播放") }.minOfOrNull { it.bottom }
            val content = page.lines.filter { it.confidence >= .5f && usable(it.text.trim()) }
            val anchors = content.filter { !it.text.all { ch -> ch.isDigit() || ch.isWhitespace() } && it.left < page.width * .8 }.map { it.left }.sorted()
            val textLeft = anchors.getOrNull(anchors.size / 2) ?: 0
            // Drop numeric ranks in their separate left column, preserving numeric song titles.
            val allRows = mergeRows(content.filterNot { it.text.all { ch -> ch.isDigit() || ch.isWhitespace() } && it.right < textLeft - it.height / 3 })
            val rows = allRows.filter { toolbar == null || it.top >= toolbar }
            val pairs = mutableListOf<Pair<OcrLine, OcrLine>>()
            var i = 0
            while (i + 1 < rows.size) {
                val title = rows[i]; val subtitle = rows[i + 1]
                val gap = subtitle.top - title.bottom
                val aligned = abs(title.left - subtitle.left) <= maxOf(20, page.width / 12)
                if (aligned && gap >= -title.height / 4 && gap <= maxOf(12, title.height * 2) &&
                    subtitle.height <= title.height * 1.35 && title.left < page.width * .8 &&
                    !title.text.contains("首歌曲") && !title.text.contains("首音乐")) {
                    pairs += title to subtitle; i += 2
                } else i++
            }
            if (pairs.isEmpty()) continue
            if (requestedName == null) {
                // A prominent heading above the song list is a name, never an artist or a command.
                val first = pairs.first().first
                allRows.filter { it.bottom < (toolbar ?: first.top) && it.height >= first.height * 1.15 && usable(it.text) }
                    .maxByOrNull { it.height }?.text?.take(80)?.let { currentName = it }
            }
            val target = result.getOrPut(currentName) { mutableListOf() }
            for ((title, subtitle) in pairs) {
                val parts = subtitle.text.replace(Regex("^歌手[：:]\\s*"), "").split(Regex("\\s+[-–—·|｜]\\s+|\\s*[–—·|｜]\\s*|(?<=[\\u3400-\\u9fff])\\s*-\\s*|\\s*-\\s*(?=[\\u3400-\\u9fff])"), limit = 2)
                val artist = parts.first().trim().take(180)
                val album = parts.getOrNull(1)?.trim().orEmpty().take(180)
                if (usable(artist) && title.text.length <= 180) target += SongRequest(title.text.trim(), artist, album)
            }
        }
        return result.map { (name, songs) -> RecognizedPlaylist(name, songs.distinctBy { key(it.title) + "|" + key(it.artist) }) }.filter { it.songs.isNotEmpty() }
    }

    private fun key(value: String) = value.lowercase().filter(Char::isLetterOrDigit)

    private fun mergeRows(lines: List<OcrLine>): List<OcrLine> {
        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in lines.sortedBy { it.top }) {
            val row = rows.lastOrNull()
            if (row != null && abs((row.first().top + row.first().bottom) - (line.top + line.bottom)) <= minOf(row.first().height, line.height)) row += line
            else rows += mutableListOf(line)
        }
        return rows.map { row ->
            val sorted = row.sortedBy { it.left }
            OcrLine(sorted.joinToString(" ") { it.text.trim() }, row.minOf { it.left }, row.minOf { it.top }, row.maxOf { it.right }, row.maxOf { it.bottom }, row.minOf { it.confidence })
        }
    }
}
