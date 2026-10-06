package com.resonance.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.resonance.player.design.ResonanceMotionTokens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.border
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.LocalReducedMotion
import com.resonance.player.design.LocalAppForeground
import com.resonance.player.design.LocalAppearance
import com.resonance.player.design.LocalGlassState
import com.resonance.player.design.resonanceSpring
import com.resonance.player.design.motionDuration
import dev.chrisbanes.haze.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import com.resonance.player.design.ResonanceShapes
import com.resonance.player.design.resonanceGlass
import com.resonance.player.design.resonancePressable
import com.resonance.player.ui.common.FluidAmbientCanvas
import com.resonance.player.model.LyricLine
import com.resonance.player.model.Lyrics
import com.resonance.player.model.LyricsUiState
import com.resonance.player.model.PlayerState
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.Track
import com.resonance.player.model.durationTextToSeconds
import com.resonance.player.platform.ResonanceBackHandler
import com.resonance.player.platform.ResonanceDialogSystemBars

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
    onVolumeChange: (Float) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onOpenSleepTimer: (() -> Unit)? = null,
    onAdjustLyricsOffset: ((Long) -> Unit)? = null,
    onEmbedLyrics: (() -> Unit)? = null,
    onToggleFloatingLyrics: (() -> Unit)? = null,
    floatingLyricsEnabled: Boolean = false,
    onTrackSelected: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onRefreshLyrics: () -> Unit,
    onRequestAiLyrics: () -> Unit = onRefreshLyrics,
    onDeleteLocalTrack: ((Track) -> Unit)? = null,
) {
    val track = playerState.currentTrack ?: return
    var paneName by rememberSaveable { mutableStateOf(PlayerPane.Cover.name) }
    val pane = PlayerPane.entries.firstOrNull { it.name == paneName } ?: PlayerPane.Cover

    ResonanceBackHandler(enabled = true, onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ResonanceDialogSystemBars(ResonanceColors.isDark)
        Surface(modifier = Modifier.fillMaxSize(), color = ResonanceColors.Canvas) {
            val glassState = rememberHazeState()
            CompositionLocalProvider(LocalGlassState provides glassState) {
            BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                val wide = maxWidth >= 720.dp || maxWidth > maxHeight * 1.35f
                val short = maxHeight < 560.dp
                val shortPaneHeight = (maxHeight - 290.dp).coerceIn(120.dp, 240.dp)
                NowPlayingBackdrop(track.artworkSeed, playerState.isPlaying, Modifier.matchParentSize().hazeSource(glassState))
                Column(
                    modifier = Modifier.align(Alignment.Center).widthIn(max = 1280.dp).heightIn(max = 900.dp)
                        .fillMaxSize().padding(horizontal = if (wide) 32.dp else 20.dp, vertical = 10.dp),
                ) {
                    NowPlayingTopBar(
                        track = track,
                        playerState = playerState,
                        onDismiss = onDismiss,
                        onToggleFavorite = onToggleFavorite,
                        onDeleteLocalTrack = onDeleteLocalTrack,
                        onOpenSleepTimer = onOpenSleepTimer,
                        onToggleFloatingLyrics = onToggleFloatingLyrics,
                        floatingLyricsEnabled = floatingLyricsEnabled,
                    )
                    if (wide) {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(0.95f).fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                ArtworkPane(track, playerState.isPlaying, Modifier.weight(1f))
                            }
                            Column(Modifier.weight(1.05f).fillMaxHeight()
                                .then(if (short) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
                                TrackIdentity(track)
                                Spacer(Modifier.height(10.dp))
                                if (short) {
                                    PlaybackProgress(track, playerState.progress, onSeek)
                                    PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                                    VolumeAndSpeedRow(playerState.volume, playerState.playbackSpeed, onVolumeChange, onSpeedChange)
                                    Spacer(Modifier.height(8.dp))
                                }
                                PaneSwitcher(pane, onSelect = { paneName = it.name })
                                Spacer(Modifier.height(8.dp))
                                PaneContent(
                                    pane = pane,
                                    track = track,
                                    playerState = playerState,
                                    lyricsState = lyricsState,
                                    queue = queue,
                                    onSeek = onSeek,
                                    onTrackSelected = onTrackSelected,
                                    onRefreshLyrics = onRefreshLyrics,
                                    onRequestAiLyrics = onRequestAiLyrics,
                                    onOpenFullLyrics = { paneName = PlayerPane.Lyrics.name },
                                    onAdjustOffset = onAdjustLyricsOffset,
                                    onEmbedLyrics = onEmbedLyrics,
                                    showArtwork = false,
                                    modifier = if (short) Modifier.height(shortPaneHeight) else Modifier.weight(1f),
                                )
                                if (!short) {
                                    Spacer(Modifier.height(8.dp))
                                    PlaybackProgress(track, playerState.progress, onSeek)
                                    PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                                    Spacer(Modifier.height(2.dp))
                                    VolumeAndSpeedRow(playerState.volume, playerState.playbackSpeed, onVolumeChange, onSpeedChange)
                                }
                            }
                        }
                    } else {
                        PaneContent(
                            pane = pane,
                            track = track,
                            playerState = playerState,
                            lyricsState = lyricsState,
                            queue = queue,
                            onSeek = onSeek,
                            onTrackSelected = onTrackSelected,
                            onRefreshLyrics = onRefreshLyrics,
                            onRequestAiLyrics = onRequestAiLyrics,
                            onOpenFullLyrics = { paneName = PlayerPane.Lyrics.name },
                            onAdjustOffset = onAdjustLyricsOffset,
                            onEmbedLyrics = onEmbedLyrics,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.height(8.dp))
                        TrackIdentity(track)
                        Spacer(Modifier.height(6.dp))
                        PlaybackProgress(track, playerState.progress, onSeek)
                        PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                        Spacer(Modifier.height(2.dp))
                        VolumeAndSpeedRow(playerState.volume, playerState.playbackSpeed, onVolumeChange, onSpeedChange)
                        Spacer(Modifier.height(4.dp))
                        PaneSwitcher(pane, onSelect = { paneName = it.name })
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
}

/** 封面种子色衍生的流动极光光晕背景（Now Playing 沉浸专属）。 */
@Composable
private fun NowPlayingBackdrop(seed: Int, isPlaying: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(ResonanceColors.Canvas)) {
        FluidAmbientCanvas(
            seed = seed,
            isPlaying = isPlaying,
            intensity = 1.35f,
        )
    }
}

@Composable
private fun NowPlayingTopBar(
    track: Track,
    playerState: PlayerState,
    onDismiss: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onDeleteLocalTrack: ((Track) -> Unit)? = null,
    onOpenSleepTimer: (() -> Unit)? = null,
    onToggleFloatingLyrics: (() -> Unit)? = null,
    floatingLyricsEnabled: Boolean = false,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        AccessibleIconButton("关闭正在播放", onClick = onDismiss) {
            Icon(Icons.Default.KeyboardArrowDown, "关闭正在播放", tint = ResonanceColors.TextPrimary)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("正在播放", style = MaterialTheme.typography.labelMedium, color = ResonanceColors.Muted)
            Text(track.album.ifBlank { "本地曲目" }, style = MaterialTheme.typography.bodySmall,
                color = ResonanceColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AccessibleIconButton(if (track.isFavorite) "取消收藏" else "收藏", onClick = { onToggleFavorite(track) }) {
            Icon(if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                if (track.isFavorite) "取消收藏" else "收藏", tint = if (track.isFavorite) ResonanceColors.Primary else ResonanceColors.Muted)
        }
        Box {
            AccessibleIconButton("播放选项", onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "播放选项", tint = ResonanceColors.Muted) }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (onOpenSleepTimer != null) DropdownMenuItem(text = { Text("睡眠定时器") },
                    onClick = { menuExpanded = false; onOpenSleepTimer() })
                if (onToggleFloatingLyrics != null) DropdownMenuItem(
                    text = { Text(if (floatingLyricsEnabled) "关闭桌面歌词" else "开启桌面歌词") },
                    onClick = { menuExpanded = false; onToggleFloatingLyrics() })
                if (!track.sourceUri.isNullOrBlank() && onDeleteLocalTrack != null) DropdownMenuItem(
                    text = { Text("删除本地音频", color = MaterialTheme.colorScheme.error) },
                    onClick = { menuExpanded = false; onDeleteLocalTrack(track) })
                if (onOpenSleepTimer == null && onToggleFloatingLyrics == null && onDeleteLocalTrack == null) {
                    DropdownMenuItem(text = { Text("本地音乐播放") }, onClick = { menuExpanded = false }, enabled = false)
                }
            }
        }
    }
}

/** 底部磨砂浮动胶囊页签切换（封面 / 歌词 / 队列）。 */
@Composable
private fun PaneSwitcher(selected: PlayerPane, onSelect: (PlayerPane) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .resonanceGlass(
                shape = ResonanceShapes.Capsule,
                borderColors = listOf(ResonanceColors.GlassBorder, ResonanceColors.GlassBorderSubtle),
                shadowElevation = 8.dp,
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            PlayerPane.entries.forEach { pane ->
                val active = pane == selected
                val interaction = remember { MutableInteractionSource() }
                val scale by animateFloatAsState(
                    targetValue = if (active) 1.0f else 0.96f,
                    animationSpec = resonanceSpring(),
                    label = "paneTabScale",
                )
                Row(
                    modifier = Modifier
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .resonancePressable(interaction, pressedScale = 0.94f)
                        .clip(ResonanceShapes.Capsule)
                        .then(
                            if (active) {
                                Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                ResonanceColors.PrimarySoft,
                                                ResonanceColors.PrimarySoft.copy(alpha = 0.25f),
                                            )
                                        )
                                    )
                                    .border(1.dp, ResonanceColors.GlassBorderGlow.copy(alpha = 0.5f), ResonanceShapes.Capsule)
                            } else Modifier
                        )
                        .clickable(interactionSource = interaction, indication = null, role = Role.Tab) { onSelect(pane) }
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics { this.selected = active }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        pane.icon,
                        contentDescription = null,
                        tint = if (active) ResonanceColors.Primary else ResonanceColors.Dim,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        pane.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) ResonanceColors.Primary else ResonanceColors.Dim,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
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
    onRequestAiLyrics: () -> Unit,
    onOpenFullLyrics: () -> Unit,
    onAdjustOffset: ((Long) -> Unit)? = null,
    onEmbedLyrics: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
) {
    val reducedMotion = LocalReducedMotion.current

    AnimatedContent(
        targetState = pane,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = {
            if (reducedMotion) {
                fadeIn(ResonanceMotionTokens.PageFadeInSpec) togetherWith fadeOut(ResonanceMotionTokens.PageFadeOutSpec)
            } else {
                val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                (slideInHorizontally(ResonanceMotionTokens.PageSlideSpring) { (it * 0.3f * dir).toInt() } +
                    fadeIn(ResonanceMotionTokens.PageFadeInSpec)) togetherWith
                (slideOutHorizontally(ResonanceMotionTokens.PageSlideSpring) { (-it * 0.3f * dir).toInt() } +
                    fadeOut(ResonanceMotionTokens.PageFadeOutSpec))
            }
        },
        label = "playerPaneContent",
    ) { active ->
        when (active) {
            PlayerPane.Cover -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (showArtwork) Arrangement.SpaceBetween else Arrangement.Center,
                ) {
                    if (showArtwork) {
                        ArtworkPane(track, playerState.isPlaying, Modifier.weight(1f))
                        Spacer(Modifier.height(8.dp))
                    }
                    HeroSyncedLyricsPreview(
                        track = track,
                        progress = playerState.progress,
                        lyricsState = lyricsState,
                        onOpenFullLyrics = onOpenFullLyrics,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    )
                }
            }
            PlayerPane.Lyrics -> LyricsPane(
                track = track,
                progress = playerState.progress,
                state = lyricsState,
                onSeek = onSeek,
                onRefresh = onRefreshLyrics,
                onRequestAi = onRequestAiLyrics,
                onAdjustOffset = onAdjustOffset,
                onEmbedLyrics = onEmbedLyrics,
            )
            PlayerPane.Queue -> QueuePane(queue, track.id, onTrackSelected)
        }
    }
}

@Composable
private fun HeroSyncedLyricsPreview(
    track: Track,
    progress: Float,
    lyricsState: LyricsUiState,
    onOpenFullLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lyricMotion = resonanceSpring<androidx.compose.ui.unit.IntOffset>()
    val reducedMotion = LocalReducedMotion.current
    val durationMs = durationTextToSeconds(track.durationText).coerceAtLeast(1) * 1_000L
    val positionMs = (durationMs * progress.coerceIn(0f, 1f)).toLong()
    val lyricsInteraction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .resonancePressable(lyricsInteraction, pressedScale = 0.98f)
            .resonanceGlass(
                shape = RoundedCornerShape(18.dp),
                borderColors = listOf(ResonanceColors.GlassBorder, ResonanceColors.GlassBorderSubtle),
                shadowElevation = 8.dp,
            )
            .clickable(interactionSource = lyricsInteraction, indication = null, role = Role.Button, onClick = onOpenFullLyrics)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (lyricsState) {
            is LyricsUiState.Ready -> {
                val lyrics = lyricsState.lyrics
                if (lyrics.instrumental) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = ResonanceColors.Dim, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("纯音乐 · 请享受旋律", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
                    }
                } else if (lyrics.synchronized && lyrics.lines.isNotEmpty()) {
                    val positionWithOffset = positionMs + lyrics.offsetMs
                    val activeIndex = lyrics.lines.indexOfLast { (it.timestampMs ?: Long.MAX_VALUE) <= positionWithOffset }

                    AnimatedContent(
                        targetState = activeIndex,
                        transitionSpec = {
                            if (reducedMotion) {
                                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                            } else if (targetState > initialState) {
                                (slideInVertically(
                                    animationSpec = lyricMotion
                                ) { height -> (height * 0.45f).toInt() } + fadeIn(tween(240)))
                                    .togetherWith(
                                        slideOutVertically(
                                            animationSpec = lyricMotion
                                        ) { height -> -(height * 0.45f).toInt() } + fadeOut(tween(180))
                                    )
                            } else {
                                (slideInVertically(
                                    animationSpec = lyricMotion
                                ) { height -> -(height * 0.45f).toInt() } + fadeIn(tween(240)))
                                    .togetherWith(
                                        slideOutVertically(
                                            animationSpec = lyricMotion
                                        ) { height -> (height * 0.45f).toInt() } + fadeOut(tween(180))
                                    )
                            }
                        },
                        label = "heroSyncedLyricsScroll",
                        modifier = Modifier.fillMaxWidth().clipToBounds(),
                    ) { targetIdx ->
                        val prevLine = if (targetIdx > 0) lyrics.lines.getOrNull(targetIdx - 1)?.text else null
                        val currentLine = if (targetIdx >= 0) lyrics.lines.getOrNull(targetIdx)?.text else lyrics.lines.firstOrNull()?.text
                        val nextLine = if (targetIdx >= 0) lyrics.lines.getOrNull(targetIdx + 1)?.text else lyrics.lines.getOrNull(1)?.text

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (prevLine != null) {
                                Text(
                                    prevLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ResonanceColors.Dim,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(1.dp))
                            }
                            Text(
                                currentLine ?: "…",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ResonanceColors.Primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                            if (nextLine != null) {
                                Spacer(Modifier.height(1.dp))
                                Text(
                                    nextLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ResonanceColors.Dim,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else {
                    val firstLine = lyrics.lines.firstOrNull()?.text ?: "…"
                    Text(
                        firstLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ResonanceColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            is LyricsUiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = ResonanceColors.Primary, modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("正在匹配歌词…", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Dim)
                }
            }
            is LyricsUiState.Unavailable -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = ResonanceColors.Dim, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("暂无歌词 · 点击查看或使用 AI 检索", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Dim)
                }
            }
            LyricsUiState.Idle -> {
                Text("点击查看歌词", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Dim)
            }
        }
    }
}

@Composable
private fun ArtworkPane(track: Track, playing: Boolean, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = if (LocalReducedMotion.current || playing) 1f else 0.98f,
        animationSpec = resonanceSpring(),
        label = "heroArtworkScale",
    )

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val artworkSize = minOf(maxWidth - 24.dp, maxHeight - 16.dp, 360.dp).coerceAtLeast(0.dp)
        Box(
            modifier = Modifier
                .size(artworkSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = 10.dp,
                    shape = ResonanceShapes.ArtworkLarge,
                    ambientColor = ResonanceColors.Shadow.copy(alpha = 0.35f),
                    spotColor = ResonanceColors.Shadow.copy(alpha = 0.10f),
                ),
        ) {
            AlbumArtwork(track.artworkSeed, Modifier.fillMaxSize(), 24.dp, track.artworkPath)
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
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = ResonanceColors.TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                track.artist.ifBlank { "未知艺术家" },
                style = MaterialTheme.typography.bodyMedium,
                color = ResonanceColors.Muted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (track.mimeType.contains("flac", ignoreCase = true)) "FLAC" else "320K",
                style = MaterialTheme.typography.labelSmall,
                color = ResonanceColors.Dim,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(ResonanceColors.Soft)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun PlaybackProgress(track: Track, progress: Float, onSeek: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = "播放进度" },
            colors = SliderDefaults.colors(
                thumbColor = ResonanceColors.Primary,
                activeTrackColor = ResonanceColors.Primary,
                inactiveTrackColor = ResonanceColors.DividerStrong,
            ),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp),
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
    val isPlaying = playerState.isPlaying
    val playPopScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = resonanceSpring(),
        label = "playPopScale",
    )

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleShuffle, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = if (playerState.shuffleEnabled) "关闭随机播放" else "开启随机播放",
                tint = if (playerState.shuffleEnabled) ResonanceColors.Primary else ResonanceColors.Dim,
                modifier = Modifier.size(21.dp),
            )
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(52.dp)) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "上一首",
                tint = ResonanceColors.TextPrimary,
                modifier = Modifier.size(32.dp),
            )
        }
        FilledIconButton(
            onClick = onTogglePlay,
            modifier = Modifier
                .size(66.dp)
                .graphicsLayer {
                    scaleX = playPopScale
                    scaleY = playPopScale
                }
                .resonancePressable(playInteraction, pressedScale = 0.88f)
                .shadow(
                    elevation = 5.dp,
                    shape = CircleShape,
                    spotColor = ResonanceColors.Shadow.copy(alpha = 0.12f),
                ),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = ResonanceColors.Primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            interactionSource = playInteraction,
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                modifier = Modifier.size(34.dp),
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "下一首",
                tint = ResonanceColors.TextPrimary,
                modifier = Modifier.size(32.dp),
            )
        }
        IconButton(onClick = onCycleRepeat, modifier = Modifier.size(48.dp)) {
            Icon(
                if (playerState.repeatMode == RepeatMode.One) Icons.Default.RepeatOne else Icons.Default.Repeat,
                contentDescription = when (playerState.repeatMode) {
                    RepeatMode.Off -> "开启列表循环"
                    RepeatMode.All -> "开启单曲循环"
                    RepeatMode.One -> "关闭循环"
                },
                tint = if (playerState.repeatMode == RepeatMode.Off) ResonanceColors.Dim else ResonanceColors.Primary,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun VolumeAndSpeedRow(
    volume: Float,
    speed: Float,
    onVolumeChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
) {
    var showSpeedMenu by remember { mutableStateOf(false) }
    var previousVolume by remember { mutableStateOf(1f) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                if (volume > 0.01f) {
                    previousVolume = volume
                    onVolumeChange(0f)
                } else {
                    onVolumeChange(if (previousVolume > 0.05f) previousVolume else 1f)
                }
            },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                when {
                    volume <= 0.01f -> Icons.AutoMirrored.Filled.VolumeOff
                    volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                    else -> Icons.AutoMirrored.Filled.VolumeUp
                },
                contentDescription = "音量调节与静音",
                tint = if (volume <= 0.01f) ResonanceColors.Dim else ResonanceColors.TextPrimary,
                modifier = Modifier.size(18.dp),
            )
        }

        Slider(
            value = volume.coerceIn(0f, 1f),
            onValueChange = onVolumeChange,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { contentDescription = "音量" },
            colors = SliderDefaults.colors(
                thumbColor = ResonanceColors.TextPrimary,
                activeTrackColor = ResonanceColors.TextPrimary,
                inactiveTrackColor = ResonanceColors.DividerStrong,
            ),
        )

        Spacer(Modifier.width(10.dp))

        Box {
            TextButton(
                onClick = { showSpeedMenu = true },
                modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = if (speed != 1.0f) ResonanceColors.Primary else ResonanceColors.Dim, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${if (speed == 1.0f) "1.0" else speed}x",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (speed != 1.0f) ResonanceColors.Primary else ResonanceColors.TextPrimary,
                )
            }
            DropdownMenu(
                expanded = showSpeedMenu,
                onDismissRequest = { showSpeedMenu = false },
            ) {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { s ->
                    DropdownMenuItem(
                        text = { Text("${s}x 倍速", color = if (s == speed) ResonanceColors.Primary else ResonanceColors.TextPrimary) },
                        onClick = {
                            onSpeedChange(s)
                            showSpeedMenu = false
                        },
                        trailingIcon = if (s == speed) {
                            { Icon(Icons.Default.Check, contentDescription = null, tint = ResonanceColors.Primary, modifier = Modifier.size(16.dp)) }
                        } else null,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 歌词（磨砂玻璃卡片悬浮容器）
// ---------------------------------------------------------------------------

@Composable
private fun LyricsPane(
    track: Track,
    progress: Float,
    state: LyricsUiState,
    onSeek: (Float) -> Unit,
    onRefresh: () -> Unit,
    onRequestAi: () -> Unit,
    onAdjustOffset: ((Long) -> Unit)? = null,
    onEmbedLyrics: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .resonanceGlass(
                shape = RoundedCornerShape(22.dp),
                borderColors = listOf(ResonanceColors.GlassBorder, ResonanceColors.GlassBorderSubtle),
                shadowElevation = 8.dp,
            )
            .padding(8.dp)
    ) {
        when (state) {
            LyricsUiState.Idle, LyricsUiState.Loading -> LyricsLoading()
            is LyricsUiState.Unavailable -> LyricsUnavailable(state.message, onRefresh, onRequestAi)
            is LyricsUiState.Ready -> LyricsContent(
                track = track,
                progress = progress,
                lyrics = state.lyrics,
                onSeek = onSeek,
                onAdjustOffset = onAdjustOffset,
                onEmbedLyrics = onEmbedLyrics,
            )
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
        CircularProgressIndicator(color = ResonanceColors.Primary, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(14.dp))
        Text("正在匹配歌词…", style = MaterialTheme.typography.titleSmall, color = ResonanceColors.Muted)
    }
}

@Composable
private fun LyricsUnavailable(message: String, onRefresh: () -> Unit, onRequestAi: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = ResonanceColors.Dimmer, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, color = ResonanceColors.Muted)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = ResonanceColors.Primary)
                Spacer(Modifier.width(6.dp))
                Text("重试检索", color = ResonanceColors.Primary)
            }
            OutlinedButton(onClick = onRequestAi) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = ResonanceColors.Primary)
                Spacer(Modifier.width(6.dp))
                Text("DeepSeek AI 检索", color = ResonanceColors.Primary)
            }
        }
    }
}

@Composable
private fun LyricsContent(
    track: Track,
    progress: Float,
    lyrics: Lyrics,
    onSeek: (Float) -> Unit,
    onAdjustOffset: ((Long) -> Unit)? = null,
    onEmbedLyrics: (() -> Unit)? = null,
) {
    val reducedMotion = LocalReducedMotion.current

    if (lyrics.instrumental) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = ResonanceColors.Dim, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(14.dp))
            Text("纯音乐 · 请享受旋律", style = MaterialTheme.typography.titleMedium, color = ResonanceColors.TextPrimary)
        }
        return
    }

    val durationMs = durationTextToSeconds(track.durationText).coerceAtLeast(1) * 1_000L
    val positionMs = (durationMs * progress.coerceIn(0f, 1f)).toLong() + lyrics.offsetMs
    val activeIndex = if (lyrics.synchronized) {
        lyrics.lines.indexOfLast { (it.timestampMs ?: Long.MAX_VALUE) <= positionMs }
    } else {
        -1
    }
    val listState = rememberLazyListState()
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var lastUserDragMark by remember { mutableStateOf<TimeMark?>(null) }

    LaunchedEffect(isDragged) {
        if (isDragged) {
            lastUserDragMark = TimeSource.Monotonic.markNow()
        }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            val mark = lastUserDragMark
            val allowAutoScroll = !isDragged && (mark == null || mark.elapsedNow().inWholeMilliseconds >= 3000L)
            if (allowAutoScroll) {
                if (reducedMotion) listState.scrollToItem(activeIndex) else listState.animateScrollToItem(activeIndex)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (lyrics.synchronized && onAdjustOffset != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("时间轴", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Dim)
                    TextButton(onClick = { onAdjustOffset(-500L) }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                        Text("-0.5s", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Primary)
                    }
                    TextButton(
                        onClick = { onAdjustOffset(-lyrics.offsetMs) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        enabled = lyrics.offsetMs != 0L,
                    ) {
                        val offsetSec = lyrics.offsetMs / 1000.0
                        Text(
                            if (lyrics.offsetMs == 0L) "正常" else "%+.1fs".format(offsetSec),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (lyrics.offsetMs == 0L) ResonanceColors.Dim else ResonanceColors.TextPrimary,
                        )
                    }
                    TextButton(onClick = { onAdjustOffset(500L) }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                        Text("+0.5s", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Primary)
                    }
                }

                if (onEmbedLyrics != null && !track.sourceUri.isNullOrBlank()) {
                    TextButton(
                        onClick = onEmbedLyrics,
                        modifier = Modifier.heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    ) {
                        Text("嵌入到音频文件", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted)
                    }
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val centerPadding = (maxHeight / 2 - 28.dp).coerceAtLeast(20.dp)

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = centerPadding, bottom = centerPadding, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(lyrics.lines, key = { index, line -> "${line.timestampMs}-$index" }) { index, line ->
                    val isActive = index == activeIndex
                    LyricRow(
                        line = line,
                        active = isActive,
                        onClick = line.timestampMs?.let { timestamp ->
                            {
                                lastUserDragMark = null
                                onSeek((timestamp - lyrics.offsetMs).coerceAtLeast(0L).toFloat() / durationMs.toFloat())
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricRow(line: LyricLine, active: Boolean, onClick: (() -> Unit)?) {
    val interactionSource = remember { MutableInteractionSource() }
    val color by animateColorAsState(
        targetValue = if (active) ResonanceColors.TextPrimary else ResonanceColors.Dim,
        animationSpec = tween(motionDuration(180)),
        label = "lyricColor",
    )
    val activeScale by animateFloatAsState(
        targetValue = if (active) 1.04f else 0.98f,
        animationSpec = resonanceSpring(),
        label = "lyricScale",
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (active) ResonanceColors.PrimarySoft else Color.Transparent,
        animationSpec = tween(motionDuration(180)),
        label = "lyricBgColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (active) ResonanceColors.GlassBorderGlow.copy(alpha = 0.45f) else Color.Transparent,
        animationSpec = tween(motionDuration(180)),
        label = "lyricBorderColor",
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
            .heightIn(min = 48.dp)
            .graphicsLayer {
                scaleX = activeScale
                scaleY = activeScale
            }
            .clip(RoundedCornerShape(12.dp))
            .then(interaction)
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            line.text,
            style = if (active) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = color,
        )
    }
}

// ---------------------------------------------------------------------------
// 播放队列（磨砂玻璃卡片容器）
// ---------------------------------------------------------------------------

@Composable
private fun QueuePane(queue: List<Track>, currentTrackId: String, onTrackSelected: (Track) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .resonanceGlass(
                shape = RoundedCornerShape(22.dp),
                borderColors = listOf(ResonanceColors.GlassBorder, ResonanceColors.GlassBorderSubtle),
                shadowElevation = 8.dp,
            )
            .padding(8.dp)
    ) {
        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("播放队列为空", color = ResonanceColors.Dim, style = MaterialTheme.typography.bodyMedium)
            }
            return@Box
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            itemsIndexed(queue, key = { index, item -> "queue-${item.id}-$index" }) { _, item ->
                val current = item.id == currentTrackId
                val interactionSource = remember { MutableInteractionSource() }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (current) {
                                Modifier
                                    .background(ResonanceColors.PrimarySoft)
                                    .border(1.dp, ResonanceColors.GlassBorderGlow.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            } else Modifier
                        )
                        .resonancePressable(interactionSource, pressedScale = 0.98f)
                        .clickable(interactionSource = interactionSource, indication = null) { onTrackSelected(item) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArtwork(item.artworkSeed, Modifier.size(42.dp), 8.dp, item.artworkPath)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (current) ResonanceColors.Primary else ResonanceColors.TextPrimary,
                            fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            item.artist.ifBlank { "未知艺术家" },
                            style = MaterialTheme.typography.bodySmall,
                            color = ResonanceColors.Dim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (current) {
                        NowPlayingIndicator(isPlaying = true)
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}
