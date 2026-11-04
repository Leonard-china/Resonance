package com.resonance.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.model.Track

@Composable
internal fun AssistantMusicAttachment(tracks: List<Track>, onPlay: (Track) -> Unit, onAppend: (List<Track>) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = ResonanceColors.GlassLight,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            tracks.forEachIndexed { index, track ->
                Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "播放 ${track.title}") { onPlay(track) }
                    .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AlbumArtwork(track.artworkSeed, Modifier.size(42.dp), 10.dp, track.artworkPath, audioSource = track.sourceUri)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(track.artist, style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { onPlay(track) }) { Icon(Icons.Default.PlayArrow, "播放 ${track.title}") }
                }
                if (index != tracks.lastIndex) HorizontalDivider(color = ResonanceColors.Divider.copy(alpha = .35f))
            }
            TextButton(onClick = { onAppend(tracks) }, enabled = tracks.any { !it.sourceUri.isNullOrBlank() }) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp)); Text("加入播放队列")
            }
        }
    }
}
