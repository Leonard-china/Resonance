package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonance.player.design.GalleryArtworkMount
import com.resonance.player.design.GalleryTokens
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.ResonanceShapes
import com.resonance.player.design.atelierCard
import com.resonance.player.design.musicSleevePerspective
import com.resonance.player.design.resonancePressable
import com.resonance.player.model.Playlist
import com.resonance.player.model.Track
import com.resonance.player.tutorial.TutorialCompletion
import com.resonance.player.tutorial.TutorialTarget

@Composable
internal fun PlaylistListTab(
    playlists: List<Playlist>,
    onOpenPlaylist: (Playlist) -> Unit,
    onCreatePlaylist: () -> Unit,
    favoriteCount: Int = 0,
    onOpenFavorites: () -> Unit = {},
    contentPadding: PaddingValues,
    state: LazyGridState = rememberLazyGridState(),
) {
    val feature = LocalMusicFeatures.current
    val tutorial = LocalTutorial.current
    val enabled = feature?.settings?.recommendationsEnabled == true
    val showDaily = enabled || tutorial?.step != null
    val daily = playlists.firstOrNull { it.id == com.resonance.player.model.DAILY_PLAYLIST_ID }
    val userLists = playlists.filterNot { it.id == com.resonance.player.model.DAILY_PLAYLIST_ID }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        val columns = if (compact) 2 else if (maxWidth < 900.dp) 3 else 4
        LazyVerticalGrid(
            state = state,
            columns = GridCells.Fixed(columns),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "pinned-quick-hub", contentType = "featured", span = { GridItemSpan(maxLineSpan) }) {
                val favWash = Brush.linearGradient(
                    listOf(
                        ResonanceColors.PrimarySoft.copy(alpha = if (ResonanceColors.isDark) 0.28f else 0.16f),
                        Color.Transparent,
                    )
                )
                val dailyTint = ResonanceColors.Info
                val dailyWash = Brush.linearGradient(
                    listOf(
                        dailyTint.copy(alpha = if (ResonanceColors.isDark) 0.22f else 0.11f),
                        Color.Transparent,
                    )
                )
                if (showDaily) {
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
                    ) {
                        val favInteraction = remember { MutableInteractionSource() }
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .resonancePressable(favInteraction, pressedScale = 0.97f)
                                .atelierCard(shape = ResonanceShapes.Card)
                                .background(favWash)
                                .clickable(
                                    interactionSource = favInteraction,
                                    indication = ripple(),
                                    role = Role.Button,
                                    onClick = onOpenFavorites,
                                )
                                .heightIn(min = 104.dp)
                                .padding(horizontal = 15.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Box(
                                    Modifier
                                        .size(38.dp)
                                        .clip(ResonanceShapes.Button)
                                        .background(ResonanceColors.PrimarySoft),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Favorite, null, Modifier.size(19.dp), tint = ResonanceColors.Primary)
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp), tint = ResonanceColors.Dim)
                            }
                            Spacer(Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "我喜欢的音乐",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ResonanceColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "$favoriteCount 首",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    color = ResonanceColors.Muted,
                                )
                            }
                        }

                        val dailyInteraction = remember { MutableInteractionSource() }
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .resonancePressable(dailyInteraction, pressedScale = 0.97f)
                                .atelierCard(shape = ResonanceShapes.Card)
                                .background(dailyWash)
                                .tutorialTarget(TutorialTarget.DailyCard)
                                .clickable(
                                    interactionSource = dailyInteraction,
                                    indication = ripple(),
                                    role = Role.Button,
                                ) {
                                    feature?.onNavigate(
                                        if (enabled) com.resonance.player.model.LibraryDestination.Recommendations
                                        else com.resonance.player.model.LibraryDestination.Settings
                                    )
                                    if (enabled) tutorial?.event(TutorialCompletion.DailyOpened)
                                }
                                .semantics(mergeDescendants = true) {
                                    contentDescription = if (enabled) "打开每日推荐" else "每日推荐未开启，打开推荐设置"
                                }
                                .heightIn(min = 104.dp)
                                .padding(horizontal = 15.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                if (daily != null && daily.tracks.isNotEmpty()) {
                                    AlbumArtwork(
                                        daily.artworkSeed,
                                        Modifier.size(38.dp),
                                        10.dp,
                                        daily.tracks.firstNotNullOfOrNull(Track::artworkPath),
                                    )
                                } else {
                                    Box(
                                        Modifier
                                            .size(38.dp)
                                            .clip(ResonanceShapes.Button)
                                            .background(dailyTint.copy(alpha = if (ResonanceColors.isDark) 0.22f else 0.12f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(19.dp), tint = dailyTint)
                                    }
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp), tint = ResonanceColors.Dim)
                            }
                            Spacer(Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "每日推荐",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ResonanceColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (enabled) "${daily?.tracks?.size ?: 0} 首 · 口味精选" else "去开启每日推荐",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    color = ResonanceColors.Muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                } else {
                    val favInteraction = remember { MutableInteractionSource() }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .resonancePressable(favInteraction, pressedScale = 0.98f)
                            .atelierCard(shape = ResonanceShapes.Card)
                            .background(favWash)
                            .clickable(
                                interactionSource = favInteraction,
                                indication = ripple(),
                                role = Role.Button,
                                onClick = onOpenFavorites,
                            )
                            .heightIn(min = 76.dp)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(ResonanceShapes.Button)
                                .background(ResonanceColors.PrimarySoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Favorite, null, Modifier.size(22.dp), tint = ResonanceColors.Primary)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "我喜欢的音乐",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ResonanceColors.TextPrimary,
                            )
                            Text(
                                "$favoriteCount 首",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = ResonanceColors.Muted,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = ResonanceColors.Dim)
                    }
                }
            }
            item(key = "playlist-heading", contentType = "heading", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "我的歌单",
                        style = MaterialTheme.typography.titleLarge,
                        color = ResonanceColors.TextPrimary,
                    )
                    if (userLists.isNotEmpty()) {
                        Text(
                            "${userLists.size}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = ResonanceColors.Primary,
                            modifier = Modifier
                                .clip(ResonanceShapes.Capsule)
                                .background(ResonanceColors.PrimarySoft)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    AccessibleIconButton(
                        "新建歌单",
                        onClick = onCreatePlaylist,
                        modifier = Modifier.clip(CircleShape).background(ResonanceColors.Soft),
                    ) {
                        Icon(Icons.Default.Add, null, tint = ResonanceColors.TextPrimary)
                    }
                }
            }
            if (userLists.isEmpty()) item(key = "empty", contentType = "empty", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(vertical = 24.dp)) {
                    Text("为喜欢的音乐建一个歌单", style = MaterialTheme.typography.titleMedium, color = ResonanceColors.TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text("新建歌单，或在工具中导入已有歌单。", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
                }
            }
            items(userLists, key = { it.id }, contentType = { "playlist" }) { playlist ->
                val cardInteraction = remember { MutableInteractionSource() }
                Column(
                    Modifier
                        .resonancePressable(cardInteraction, pressedScale = 0.965f)
                        .clip(ResonanceShapes.Card)
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = ripple(),
                            role = Role.Button,
                        ) { onOpenPlaylist(playlist) }
                        .semantics(mergeDescendants = true) {
                            contentDescription = "打开歌单 ${playlist.name}，${playlist.tracks.size} 首歌曲"
                        },
                ) {
                    GalleryArtworkMount(Modifier.fillMaxWidth().musicSleevePerspective()) {
                        PlaylistMosaicArtwork(
                            tracks = playlist.tracks,
                            fallbackSeed = playlist.artworkSeed,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = GalleryTokens.ArtworkCorner,
                        )
                    }
                    Text(
                        playlist.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ResonanceColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp),
                    )
                    Text(
                        "${playlist.tracks.size} 首歌曲",
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = ResonanceColors.Muted,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 4.dp),
                    )
                }
            }
        }
    }
}
