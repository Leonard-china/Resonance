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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import com.resonance.player.design.MusicStageBackdrop
import com.resonance.player.design.musicSleevePerspective
import com.resonance.player.design.musicDock
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.GalleryArtworkMount
import com.resonance.player.design.GalleryTokens
import com.resonance.player.model.Playlist
import com.resonance.player.model.Track
import com.resonance.player.tutorial.TutorialTarget
import com.resonance.player.tutorial.TutorialCompletion

@Composable
internal fun PlaylistListTab(
    playlists: List<Playlist>,
    onOpenPlaylist: (Playlist) -> Unit,
    onCreatePlaylist: () -> Unit,
    contentPadding: PaddingValues,
    state: LazyGridState = rememberLazyGridState(),
) {
    val feature = LocalMusicFeatures.current
    val tutorial = LocalTutorial.current
    val enabled = feature?.settings?.recommendationsEnabled == true
    val daily = playlists.firstOrNull { it.id == com.resonance.player.model.DAILY_PLAYLIST_ID }
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Adaptive(152.dp),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "pinned-daily", contentType = "featured", span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp).musicDock()
                .tutorialTarget(TutorialTarget.DailyCard).clickable(role = Role.Button) {
                feature?.onNavigate(if (enabled) com.resonance.player.model.LibraryDestination.Recommendations else com.resonance.player.model.LibraryDestination.Settings)
                if (enabled) tutorial?.event(TutorialCompletion.DailyOpened)
            }.semantics(mergeDescendants = true) { contentDescription = if (enabled) "打开每日推荐" else "每日推荐未开启，打开推荐设置" }) {
                MusicStageBackdrop(daily?.artworkSeed ?: 7, false, Modifier.matchParentSize())
                Row(Modifier.fillMaxWidth().heightIn(min = 196.dp).padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("每日推荐", style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = ResonanceColors.TextPrimary)
                        Text(if (enabled) "${daily?.tracks?.size ?: 0} 首 · 按你的口味更新" else "开启推荐，发现下一首喜欢的歌",
                            style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
                        Text(if (enabled) "打开今日歌单  ›" else "设置每日推荐  ›",
                            style = MaterialTheme.typography.labelLarge, color = ResonanceColors.Primary)
                    }
                    if (LocalDensity.current.fontScale <= 1.2f) {
                        Box(Modifier.size(112.dp)) {
                            Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = 10f; translationX = 8.dp.toPx() }
                                .clip(RoundedCornerShape(10.dp)).background(ResonanceColors.Soft))
                            AlbumArtwork(daily?.artworkSeed ?: 7,
                                Modifier.fillMaxSize().musicSleevePerspective(), 10.dp,
                                daily?.tracks?.firstNotNullOfOrNull(Track::artworkPath))
                        }
                    }
                }
            }
        }
        if (playlists.isEmpty()) item(key = "empty", contentType = "empty", span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(vertical = 24.dp)) {
                Text("为喜欢的音乐建一个歌单", style = MaterialTheme.typography.titleMedium, color = ResonanceColors.TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text("新建歌单，或在工具中导入已有歌单。", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
            }
        }
        items(playlists.filterNot { it.id == com.resonance.player.model.DAILY_PLAYLIST_ID }, key = { it.id }, contentType = { "playlist" }) { playlist ->
            Column(Modifier.clickable(role = Role.Button) { onOpenPlaylist(playlist) }
                .semantics(mergeDescendants = true) { contentDescription = "打开歌单 ${playlist.name}，${playlist.tracks.size} 首歌曲" }) {
                GalleryArtworkMount(Modifier.fillMaxWidth().musicSleevePerspective()) {
                    AlbumArtwork(playlist.artworkSeed, Modifier.fillMaxSize(), GalleryTokens.ArtworkCorner,
                        playlist.tracks.firstNotNullOfOrNull(Track::artworkPath))
                }
                Text(playlist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    color = ResonanceColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp))
                Text("${playlist.tracks.size} 首歌曲", style = MaterialTheme.typography.bodySmall,
                    color = ResonanceColors.Muted, modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 8.dp))
            }
        }
        item(key = "create-playlist", contentType = "create") {
            Column(Modifier.clickable(role = Role.Button, onClick = onCreatePlaylist)) {
                GalleryArtworkMount(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxSize().background(ResonanceColors.PrimarySoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = ResonanceColors.Primary, modifier = Modifier.size(36.dp))
                    }
                }
                Text("新建歌单", style = MaterialTheme.typography.titleMedium, color = ResonanceColors.Primary,
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 4.dp))
            }
        }
    }
}
