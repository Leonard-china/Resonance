package com.resonance.player.data

import com.resonance.player.model.Playlist
import com.resonance.player.model.Track

object PreviewLibrary {
    val tracks = listOf(
        Track("track-1", "The Sweet Escape", "Gwen Stefani, Akon", "The Sweet Escape", "4:06", 7, true),
        Track("track-2", "情歌", "梁静茹", "静茹＆情歌", "4:20", 2),
        Track("track-3", "给我一首歌的时间", "周杰伦", "魔杰座", "4:13", 5, true),
        Track("track-4", "Sweet Child O' Mine", "Guns N' Roses", "Appetite for Destruction", "5:56", 9),
        Track("track-5", "会呼吸的痛", "梁静茹", "崇拜", "4:32", 3),
        Track("track-6", "爱情转移", "陈奕迅", "认了吧", "4:19", 8),
    )

    val playlists = listOf(
        Playlist("playlist-1", "夜行收藏", "6 首 · 25 分钟", tracks, 11),
        Playlist("playlist-2", "中文旧唱片", "4 首 · 最近更新", tracks.drop(1).take(4), 4),
        Playlist("playlist-3", "公路与晚风", "3 首 · 本地歌单", tracks.takeLast(3), 14),
    )
}
