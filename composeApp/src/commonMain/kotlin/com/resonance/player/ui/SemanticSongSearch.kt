package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.model.*

@Composable
fun SemanticSongSearch(
    query: String,
    libraryTracks: List<Track>,
    onTrackSelected: (Track) -> Unit,
    onDownloadOnline: ((SourceSong) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (query.isBlank()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ResonanceColors.Primary, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(12.dp))
                Text("输入一句话意图找歌", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = ResonanceColors.TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text("例如：“适合下雨天听的安静民谣”、“140 BPM 跑步燃歌”", fontSize = 13.sp, color = ResonanceColors.Dim)
            }
        }
        return
    }

    val intent = remember(query) { SemanticSearchIntentParser.parse(query) }
    val localMatches = remember(query, libraryTracks) {
        val list = mutableListOf<SemanticTrackMatch>()
        for (track in libraryTracks) {
            val text = (track.title + " " + track.artist + " " + track.album).lowercase()
            var score = 0f
            val reasons = mutableListOf<String>()

            for (kw in intent.keywords) {
                if (text.contains(kw.lowercase())) {
                    score += 40f
                    reasons.add("命中关键词“$kw”")
                }
            }
            for (genre in intent.genres) {
                if (text.contains(genre)) {
                    score += 30f
                    reasons.add("曲风契合：$genre")
                }
            }
            for (mood in intent.moods) {
                if (text.contains(mood)) {
                    score += 25f
                    reasons.add("情绪契合：$mood")
                }
            }
            for (scene in intent.scenes) {
                if (text.contains(scene)) {
                    score += 25f
                    reasons.add("场景契合：$scene")
                }
            }

            if (score > 0f) {
                list.add(
                    SemanticTrackMatch(
                        track = track,
                        sourceSong = null,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        reason = reasons.distinct().joinToString(" · "),
                        confidence = score,
                        isLocal = true,
                    )
                )
            }
        }
        list.sortedByDescending { it.confidence }
    }

    Column(modifier.fillMaxSize()) {
        // Intent Pills Bar
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("意图识别：", fontSize = 12.sp, color = ResonanceColors.Dim)
            for (scene in intent.scenes) {
                PillTag("场景: $scene")
            }
            for (mood in intent.moods) {
                PillTag("情绪: $mood")
            }
            for (genre in intent.genres) {
                PillTag("曲风: $genre")
            }
            if (intent.bpmRange != null) {
                PillTag("${intent.bpmRange.first}-${intent.bpmRange.last} BPM")
            }
            if (intent.scenes.isEmpty() && intent.moods.isEmpty() && intent.genres.isEmpty() && intent.bpmRange == null) {
                PillTag("模糊语义联想")
            }
        }

        if (localMatches.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("本地曲库未命中符合该意图的歌曲，可切换“在线”在歌源中查找", color = ResonanceColors.Muted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(localMatches, key = { it.track?.id ?: it.title }) { match ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { match.track?.let { onTrackSelected(it) } },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ResonanceColors.Raised),
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ResonanceColors.Primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "播放", tint = ResonanceColors.Primary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(match.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ResonanceColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(3.dp))
                                Text(match.artist + if (match.album.isNotBlank()) " · ${match.album}" else "", fontSize = 12.sp, color = ResonanceColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    match.reason,
                                    fontSize = 11.sp,
                                    color = ResonanceColors.Primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PillTag(text: String) {
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = ResonanceColors.Primary,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ResonanceColors.Primary.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}
