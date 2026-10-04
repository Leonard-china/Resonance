package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.model.Playlist
import com.resonance.player.model.Track

@Composable
internal fun PlaylistListTab(
    playlists: List<Playlist>,
    onOpenPlaylist: (Playlist) -> Unit,
    onCreatePlaylist: () -> Unit,
    contentPadding: PaddingValues,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(144.dp),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (playlists.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(vertical = 24.dp)) {
                Text("为喜欢的音乐建一个歌单", style = MaterialTheme.typography.titleMedium, color = ResonanceColors.TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text("新建歌单，或在工具中导入已有歌单。", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
            }
        }
        items(playlists, key = { it.id }) { playlist ->
            Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button) { onOpenPlaylist(playlist) }
                .semantics(mergeDescendants = true) { contentDescription = "打开歌单 ${playlist.name}，${playlist.tracks.size} 首歌曲" }) {
                AlbumArtwork(playlist.artworkSeed, Modifier.fillMaxWidth().aspectRatio(1f), 16.dp,
                    playlist.tracks.firstNotNullOfOrNull(Track::artworkPath))
                Text(playlist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    color = ResonanceColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp))
                Text("${playlist.tracks.size} 首歌曲", style = MaterialTheme.typography.bodySmall,
                    color = ResonanceColors.Muted, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
            }
        }
        item {
            Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onCreatePlaylist)) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))
                    .background(ResonanceColors.PrimarySoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = ResonanceColors.Primary, modifier = Modifier.size(36.dp))
                }
                Text("新建歌单", style = MaterialTheme.typography.titleMedium, color = ResonanceColors.Primary,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            }
        }
    }
}
