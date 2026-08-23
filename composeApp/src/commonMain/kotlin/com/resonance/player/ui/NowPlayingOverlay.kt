package com.resonance.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.resonanceGlass
import com.resonance.player.design.resonancePressable
import com.resonance.player.model.LyricLine
import com.resonance.player.model.Lyrics
import com.resonance.player.model.LyricsUiState
import com.resonance.player.model.PlayerState
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.Track
import com.resonance.player.model.durationTextToSeconds
import kotlin.math.cos
import kotlin.math.sin

private enum class PlayerPane(val label: String, val icon: ImageVector) {
    Cover("封面", Icons.Default.Album),
    Lyrics("歌词", Icons.Default.GraphicEq),
    Queue("队列", Icons.AutoMirrored.Filled.QueueMusic),
}

@Composable
internal fun NowPlayingOverlay(
    playerState: PlayerState,
    queue: List<Track>,
    lyricsState: LyricsUiState,
    onDismiss: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSeek: (Float) -> Unit,
    onTrackSelected: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onRefreshLyrics: () -> Unit,
) {
    val track = playerState.currentTrack ?: return
    var paneName by rememberSaveable { mutableStateOf(PlayerPane.Cover.name) }
    val pane = PlayerPane.entries.firstOrNull { it.name == paneName } ?: PlayerPane.Cover

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = ResonanceColors.Canvas) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 720.dp || maxWidth > maxHeight * 1.35f
                NowPlayingBackdrop(track.artworkSeed, playerState.isPlaying, Modifier.matchParentSize())
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = if (wide) 32.dp else 18.dp, vertical = 14.dp),
                ) {
                    NowPlayingTopBar(track, onDismiss, onToggleFavorite)
                    Spacer(Modifier.height(8.dp))
                    if (wide) {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(36.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ArtworkPane(track, playerState.isPlaying, Modifier.weight(0.95f).fillMaxHeight())
                            Column(Modifier.weight(1.05f).fillMaxHeight()) {
                                TrackIdentity(track)
                                Spacer(Modifier.height(14.dp))
                                PlayerPaneSelector(pane, onSelect = { paneName = it.name })
                                Spacer(Modifier.height(12.dp))
                                PaneContent(
                                    pane = pane,
                                    track = track,
                                    playerState = playerState,
                                    lyricsState = lyricsState,
                                    queue = queue,
                                    onSeek = onSeek,
                                    onTrackSelected = onTrackSelected,
                                    onRefreshLyrics = onRefreshLyrics,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.height(10.dp))
                                PlaybackProgress(track, playerState.progress, onSeek)
                                Spacer(Modifier.height(6.dp))
                                PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                            }
                        }
                    } else {
                        PlayerPaneSelector(pane, onSelect = { paneName = it.name })
                        Spacer(Modifier.height(10.dp))
                        PaneContent(
                            pane = pane,
                            track = track,
                            playerState = playerState,
                            lyricsState = lyricsState,
                            queue = queue,
                            onSeek = onSeek,
                            onTrackSelected = onTrackSelected,
                            onRefreshLyrics = onRefreshLyrics,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.height(10.dp))
                        TrackIdentity(track)
                        Spacer(Modifier.height(10.dp))
                        PlaybackProgress(track, playerState.progress, onSeek)
                        Spacer(Modifier.height(6.dp))
                        PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingBackdrop(seed: Int, playing: Boolean, modifier: Modifier = Modifier) {
    val energy by animateFloatAsState(
        targetValue = if (playing) 1f else 0.65f,
        animationSpec = tween(600),
        label = "playerBackdropEnergy",
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "auroraMotion")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart,
        ),
        label = "auroraAngle",
    )

    val phase = (((seed % 17) + 17) % 17) / 16f
    val rad = (angle * (kotlin.math.PI / 180.0)).toFloat()

    Canvas(
        modifier.background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF0F1524),
                    ResonanceColors.Canvas,
                    Color(0xFF04060A),
                ),
            ),
        ),
    ) {
        val radius = size.maxDimension * 0.65f
        val cosOffset = cos(rad) * (size.width * 0.08f)
        val sinOffset = sin(rad) * (size.height * 0.08f)

        val first = Offset(size.width * (0.22f + phase * 0.14f) + cosOffset, size.height * 0.15f + sinOffset)
        val second = Offset(size.width * (0.80f - phase * 0.12f) - cosOffset, size.height * 0.70f - sinOffset)
        val third = Offset(size.width * 0.50f, size.height * 0.45f)

        // Aurora Ambient Bleed Layer 1: Vibrant Coral
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ResonanceColors.Coral.copy(alpha = 0.18f * energy), Color.Transparent),
                center = first,
                radius = radius,
            ),
            center = first,
            radius = radius,
        )

        // Aurora Ambient Bleed Layer 2: Electric Violet
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ResonanceColors.Violet.copy(alpha = 0.16f * energy), Color.Transparent),
                center = second,
                radius = radius * 0.85f,
            ),
            center = second,
            radius = radius * 0.85f,
        )

        // Aurora Ambient Bleed Layer 3: Subtle Mint Center
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ResonanceColors.Mint.copy(alpha = 0.08f * energy), Color.Transparent),
                center = third,
                radius = radius * 0.7f,
            ),
            center = third,
            radius = radius * 0.7f,
        )
    }
}

@Composable
private fun NowPlayingTopBar(track: Track, onDismiss: () -> Unit, onToggleFavorite: (Track) -> Unit) {
    val closeInteraction = remember { MutableInteractionSource() }
    val favInteraction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(44.dp).resonancePressable(closeInteraction),
            interactionSource = closeInteraction,
        ) {
            Icon(Icons.Default.Close, contentDescription = "关闭正在播放", tint = ResonanceColors.Ivory)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ResonanceColors.Coral),
                )
                Spacer(Modifier.width(6.dp))
                Text("正在播放", style = MaterialTheme.typography.labelLarge, color = ResonanceColors.CoralGlow)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                track.album.ifBlank { "本地曲目" },
                style = MaterialTheme.typography.bodyMedium,
                color = ResonanceColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = { onToggleFavorite(track) },
            modifier = Modifier.size(44.dp).resonancePressable(favInteraction),
            interactionSource = favInteraction,
        ) {
            Icon(
                if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (track.isFavorite) "取消收藏" else "收藏",
                tint = if (track.isFavorite) ResonanceColors.Coral else ResonanceColors.Muted,
            )
        }
    }
}

@Composable
private fun PlayerPaneSelector(selected: PlayerPane, onSelect: (PlayerPane) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .resonanceGlass(
                shape = RoundedCornerShape(18.dp),
                backgroundColor = ResonanceColors.Glass,
                shadowElevation = 4.dp,
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PlayerPane.entries.forEach { pane ->
            val active = pane == selected
            val interactionSource = remember { MutableInteractionSource() }
            val color by animateColorAsState(
                targetValue = if (active) ResonanceColors.CoralSoft else Color.Transparent,
                animationSpec = tween(180),
                label = "playerPaneBg",
            )
            val borderColor = if (active) ResonanceColors.Coral.copy(alpha = 0.5f) else Color.Transparent

            Surface(
                onClick = { onSelect(pane) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .resonancePressable(interactionSource, pressedScale = 0.95f),
                color = color,
                shape = RoundedCornerShape(14.dp),
                border = if (active) BorderStroke(1.dp, borderColor) else null,
                interactionSource = interactionSource,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        pane.icon,
                        contentDescription = null,
                        tint = if (active) ResonanceColors.CoralGlow else ResonanceColors.Muted,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        pane.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) ResonanceColors.Ivory else ResonanceColors.Muted,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaneContent(
    pane: PlayerPane,
    track: Track,
    playerState: PlayerState,
    lyricsState: LyricsUiState,
    queue: List<Track>,
    onSeek: (Float) -> Unit,
    onTrackSelected: (Track) -> Unit,
    onRefreshLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = pane,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = {
            (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 14 }) togetherWith
                (fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { -it / 14 })
        },
        label = "playerPaneContent",
    ) { active ->
        when (active) {
            PlayerPane.Cover -> ArtworkPane(track, playerState.isPlaying, Modifier.fillMaxSize())
            PlayerPane.Lyrics -> LyricsPane(track, playerState.progress, lyricsState, onSeek, onRefreshLyrics)
            PlayerPane.Queue -> QueuePane(queue, track.id, onTrackSelected)
        }
    }
}

@Composable
private fun ArtworkPane(track: Track, playing: Boolean, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = if (playing) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "heroArtworkScale",
    )

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val artworkSize = minOf(maxWidth - 20.dp, maxHeight - 20.dp, 360.dp).coerceAtLeast(140.dp)
        Box(
            modifier = Modifier
                .size(artworkSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    cameraDistance = 16f * density
                }
                .shadow(
                    elevation = 32.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = ResonanceColors.Coral.copy(alpha = 0.25f),
                    spotColor = ResonanceColors.Shadow,
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0x66FFFFFF),
                                ResonanceColors.DividerStrong.copy(alpha = 0.4f),
                            ),
                        ),
                    ),
                    RoundedCornerShape(28.dp),
                ),
        ) {
            AlbumArtwork(track.artworkSeed, Modifier.fillMaxSize(), 28.dp, track.artworkPath)
        }
    }
}

@Composable
private fun TrackIdentity(track: Track) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            track.title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = ResonanceColors.Ivory,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                track.artist.ifBlank { "未知歌手" },
                style = MaterialTheme.typography.bodyLarge,
                color = ResonanceColors.Muted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            // Audio specs quality badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ResonanceColors.SurfaceSubtle)
                    .border(1.dp, ResonanceColors.GlassBorderSubtle, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = if (track.mimeType.contains("flac", ignoreCase = true)) "FLAC LOSSLESS" else "320K MP3",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, letterSpacing = 0.5.sp),
                    color = ResonanceColors.Mint,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PlaybackProgress(track: Track, progress: Float, onSeek: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth().height(32.dp),
            colors = SliderDefaults.colors(
                thumbColor = ResonanceColors.CoralGlow,
                activeTrackColor = ResonanceColors.Coral,
                inactiveTrackColor = ResonanceColors.DividerStrong.copy(alpha = 0.8f),
            ),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                formatPlaybackPosition(track.durationText, progress),
                style = MaterialTheme.typography.labelMedium,
                color = ResonanceColors.Muted,
            )
            Text(
                track.durationText,
                style = MaterialTheme.typography.labelMedium,
                color = ResonanceColors.Dim,
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    playerState: PlayerState,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    val playInteraction = remember { MutableInteractionSource() }
    val prevInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val shuffleInteraction = remember { MutableInteractionSource() }
    val repeatInteraction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onToggleShuffle,
            modifier = Modifier.size(48.dp).resonancePressable(shuffleInteraction),
            interactionSource = shuffleInteraction,
        ) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = if (playerState.shuffleEnabled) "关闭随机播放" else "开启随机播放",
                tint = if (playerState.shuffleEnabled) ResonanceColors.Coral else ResonanceColors.Dim,
            )
        }
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(52.dp).resonancePressable(prevInteraction),
            interactionSource = prevInteraction,
        ) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "上一首",
                tint = ResonanceColors.Ivory,
                modifier = Modifier.size(32.dp),
            )
        }
        FilledIconButton(
            onClick = onTogglePlay,
            modifier = Modifier
                .size(66.dp)
                .resonancePressable(playInteraction, pressedScale = 0.92f)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = ResonanceColors.Coral.copy(alpha = 0.5f),
                    spotColor = ResonanceColors.CoralGlow,
                ),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = ResonanceColors.Coral,
                contentColor = Color(0xFF2A0B07),
            ),
            interactionSource = playInteraction,
        ) {
            Icon(
                if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playerState.isPlaying) "暂停" else "播放",
                modifier = Modifier.size(36.dp),
            )
        }
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(52.dp).resonancePressable(nextInteraction),
            interactionSource = nextInteraction,
        ) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "下一首",
                tint = ResonanceColors.Ivory,
                modifier = Modifier.size(32.dp),
            )
        }
        IconButton(
            onClick = onCycleRepeat,
            modifier = Modifier.size(48.dp).resonancePressable(repeatInteraction),
            interactionSource = repeatInteraction,
        ) {
            Icon(
                if (playerState.repeatMode == RepeatMode.One) Icons.Default.RepeatOne else Icons.Default.Repeat,
                contentDescription = when (playerState.repeatMode) {
                    RepeatMode.Off -> "开启列表循环"
                    RepeatMode.All -> "开启单曲循环"
                    RepeatMode.One -> "关闭循环"
                },
                tint = if (playerState.repeatMode == RepeatMode.Off) ResonanceColors.Dim else ResonanceColors.Coral,
            )
        }
    }
}

@Composable
private fun LyricsPane(
    track: Track,
    progress: Float,
    state: LyricsUiState,
    onSeek: (Float) -> Unit,
    onRefresh: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .resonanceGlass(
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = ResonanceColors.GlassLight,
                    shadowElevation = 8.dp,
                ),
        ) {
            when (state) {
                LyricsUiState.Idle, LyricsUiState.Loading -> LyricsLoading()
                is LyricsUiState.Unavailable -> LyricsUnavailable(state.message, onRefresh)
                is LyricsUiState.Ready -> LyricsContent(track, progress, state.lyrics, onSeek)
            }
        }
    }
}

@Composable
private fun LyricsLoading() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = ResonanceColors.Coral, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(18.dp))
        Text("正在自动匹配云端与本地歌词…", style = MaterialTheme.typography.titleMedium, color = ResonanceColors.Ivory)
        Spacer(Modifier.height(6.dp))
        Text("LRCLIB 实时检索 · 纯净播放体验", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
    }
}

@Composable
private fun LyricsUnavailable(message: String, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = ResonanceColors.Dim, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(14.dp))
        Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = ResonanceColors.Ivory)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.buttonColors(containerColor = ResonanceColors.CoralSoft, contentColor = ResonanceColors.CoralGlow),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ResonanceColors.Coral.copy(alpha = 0.4f)),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("重新匹配歌词")
        }
    }
}

@Composable
private fun LyricsContent(track: Track, progress: Float, lyrics: Lyrics, onSeek: (Float) -> Unit) {
    if (lyrics.instrumental) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = ResonanceColors.Mint, modifier = Modifier.size(54.dp))
            Spacer(Modifier.height(16.dp))
            Text("纯音乐 · 请享受旋律", style = MaterialTheme.typography.headlineMedium, color = ResonanceColors.Ivory)
            Spacer(Modifier.height(8.dp))
            Text("歌词来源：${lyrics.source}", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
        }
        return
    }

    val durationMs = durationTextToSeconds(track.durationText).coerceAtLeast(1) * 1_000L
    val positionMs = (durationMs * progress.coerceIn(0f, 1f)).toLong()
    val activeIndex = if (lyrics.synchronized) {
        lyrics.lines.indexOfLast { (it.timestampMs ?: Long.MAX_VALUE) <= positionMs }
    } else {
        -1
    }
    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(7.dp).clip(CircleShape).background(ResonanceColors.Mint),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (lyrics.synchronized) "逐行歌词 · 点按任意行跳转" else "歌词",
                    style = MaterialTheme.typography.labelLarge,
                    color = ResonanceColors.Mint,
                )
            }
            Text(
                "${lyrics.source} 匹配${if (lyrics.fromCache) " · 已本地缓存" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = ResonanceColors.Dim,
            )
            Spacer(Modifier.height(16.dp))
        }
        itemsIndexed(lyrics.lines, key = { index, line -> "${line.timestampMs}-$index" }) { index, line ->
            LyricRow(
                line = line,
                active = index == activeIndex,
                onClick = line.timestampMs?.let { timestamp -> { onSeek(timestamp.toFloat() / durationMs.toFloat()) } },
            )
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun LyricRow(line: LyricLine, active: Boolean, onClick: (() -> Unit)?) {
    val interactionSource = remember { MutableInteractionSource() }
    val color by animateColorAsState(
        targetValue = if (active) ResonanceColors.Ivory else ResonanceColors.Dim,
        animationSpec = tween(200),
        label = "lyricColor",
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1.03f else 0.97f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "lyricScale",
    )
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (active) 0.85f else 0f,
        animationSpec = tween(200),
        label = "lyricBgAlpha",
    )

    val interaction = if (onClick != null) {
        Modifier.clickable(
            role = Role.Button,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(interaction)
            .background(
                if (backgroundAlpha > 0f) {
                    ResonanceColors.CoralSoft.copy(alpha = backgroundAlpha)
                } else Color.Transparent,
            )
            .border(
                if (active) BorderStroke(1.dp, ResonanceColors.Coral.copy(alpha = 0.35f)) else BorderStroke(0.dp, Color.Transparent),
                RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (active) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(ResonanceColors.CoralGlow),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            line.text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = color,
            lineHeight = 28.sp,
        )
    }
}

@Composable
private fun QueuePane(queue: List<Track>, currentTrackId: String, onTrackSelected: (Track) -> Unit) {
    if (queue.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("播放队列为空", color = ResonanceColors.Muted)
        }
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .resonanceGlass(
                shape = RoundedCornerShape(24.dp),
                backgroundColor = ResonanceColors.GlassLight,
                shadowElevation = 8.dp,
            ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ResonanceColors.Coral))
                Spacer(Modifier.width(8.dp))
                Text("播放队列 · 共 ${queue.size} 首", style = MaterialTheme.typography.labelLarge, color = ResonanceColors.CoralGlow)
            }
        }
        itemsIndexed(queue, key = { index, item -> "queue-${item.id}-$index" }) { _, item ->
            val current = item.id == currentTrackId
            val interactionSource = remember { MutableInteractionSource() }

            Surface(
                onClick = { onTrackSelected(item) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 62.dp)
                    .resonancePressable(interactionSource),
                color = if (current) ResonanceColors.CoralSoft else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
                border = if (current) BorderStroke(1.dp, ResonanceColors.Coral.copy(alpha = 0.45f)) else null,
                interactionSource = interactionSource,
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AlbumArtwork(item.artworkSeed, Modifier.size(46.dp), 12.dp, item.artworkPath)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (current) ResonanceColors.Ivory else ResonanceColors.Ivory.copy(alpha = 0.85f),
                            fontWeight = if (current) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            item.artist.ifBlank { "未知歌手" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = ResonanceColors.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (current) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = "当前播放",
                            tint = ResonanceColors.CoralGlow,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

