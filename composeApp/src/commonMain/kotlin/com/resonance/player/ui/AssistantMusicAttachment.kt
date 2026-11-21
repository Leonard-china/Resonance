package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.ResonanceShapes
import com.resonance.player.design.atelierCard
import com.resonance.player.model.Track

@Composable
internal fun AssistantMusicAttachment(tracks: List<Track>, onPlay: (Track) -> Unit, onAppend: (List<Track>) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 10.dp).atelierCard(shape = ResonanceShapes.Card)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            tracks.forEachIndexed { index, track ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClickLabel = "播放 ${track.title}") { onPlay(track) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArtwork(track.artworkSeed, Modifier.size(44.dp), 10.dp, track.artworkPath, audioSource = track.sourceUri)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = ResonanceColors.TextPrimary,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Text(
                            track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = ResonanceColors.Muted,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { onPlay(track) }) {
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(ResonanceColors.PrimarySoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.PlayArrow, "播放 ${track.title}", Modifier.size(18.dp), tint = ResonanceColors.Primary)
                        }
                    }
                }
                if (index != tracks.lastIndex) HorizontalDivider(color = ResonanceColors.Divider, thickness = Dp.Hairline)
            }
            HorizontalDivider(color = ResonanceColors.Divider, thickness = Dp.Hairline)
            TextButton(onClick = { onAppend(tracks) }, enabled = tracks.any { !it.sourceUri.isNullOrBlank() }) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("加入播放队列")
            }
        }
    }
}
