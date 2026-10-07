package com.resonance.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ripple
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import com.resonance.player.model.CalibrationStatus
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
    onTrackGainChange: (Float) -> Unit = {},
    onToggleAutoLoudness: () -> Unit = {},
    onStartCalibration: () -> Unit = {},
    onFinishCalibration: () -> Unit = {},
    onCancelCalibration: () -> Unit = {},
    onResetCalibration: () -> Unit = {},
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
    modifier: Modifier = Modifier,
) {
    val track = playerState.currentTrack ?: return
    var paneName by rememberSaveable { mutableStateOf(PlayerPane.Cover.name) }
    val pane = PlayerPane.entries.firstOrNull { it.name == paneName } ?: PlayerPane.Cover

    ResonanceBackHandler(enabled = true, onBack = onDismiss)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .shadow(
                elevation = 28.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                spotColor = Color.Black.copy(alpha = 0.50f),
            )
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent,
                    )
                ),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        color = ResonanceColors.Canvas,
    ) {
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
                                ArtworkPane(track, playerState.isPlaying, Modifier.weight(1f), onDismiss = onDismiss)
                            }
                            Column(Modifier.weight(1.05f).fillMaxHeight()
                                .then(if (short) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
                                TrackIdentity(track)
                                Spacer(Modifier.height(10.dp))
                                if (short) {
                                    PlaybackProgress(track, playerState.progress, onSeek)
                                    PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                                    VolumeAndSpeedRow(
                                        volume = playerState.volume,
                                        speed = playerState.playbackSpeed,
                                        onVolumeChange = onVolumeChange,
                                        onSpeedChange = onSpeedChange,
                                        trackGainDb = playerState.trackGainDb,
                                        onTrackGainChange = onTrackGainChange,
                                        autoLoudnessEnabled = playerState.autoLoudnessEnabled,
                                        onToggleAutoLoudness = onToggleAutoLoudness,
                                        calibrationStatus = playerState.calibrationStatus,
                                        isCurrentTrackCalibrated = playerState.isCurrentTrackCalibrated,
                                        onStartCalibration = onStartCalibration,
                                        onFinishCalibration = onFinishCalibration,
                                        onCancelCalibration = onCancelCalibration,
                                        onResetCalibration = onResetCalibration,
                                    )
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
                                    onDismiss = onDismiss,
                                    modifier = if (short) Modifier.height(shortPaneHeight) else Modifier.weight(1f),
                                )
                                if (!short) {
                                    Spacer(Modifier.height(8.dp))
                                    PlaybackProgress(track, playerState.progress, onSeek)
                                    PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                                    Spacer(Modifier.height(2.dp))
                                    VolumeAndSpeedRow(
                                        volume = playerState.volume,
                                        speed = playerState.playbackSpeed,
                                        onVolumeChange = onVolumeChange,
                                        onSpeedChange = onSpeedChange,
                                        trackGainDb = playerState.trackGainDb,
                                        onTrackGainChange = onTrackGainChange,
                                        autoLoudnessEnabled = playerState.autoLoudnessEnabled,
                                        onToggleAutoLoudness = onToggleAutoLoudness,
                                        calibrationStatus = playerState.calibrationStatus,
                                        isCurrentTrackCalibrated = playerState.isCurrentTrackCalibrated,
                                        onStartCalibration = onStartCalibration,
                                        onFinishCalibration = onFinishCalibration,
                                        onCancelCalibration = onCancelCalibration,
                                        onResetCalibration = onResetCalibration,
                                    )
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
                            onDismiss = onDismiss,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.height(8.dp))
                        TrackIdentity(track)
                        Spacer(Modifier.height(6.dp))
                        PlaybackProgress(track, playerState.progress, onSeek)
                        PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                        Spacer(Modifier.height(2.dp))
                        VolumeAndSpeedRow(
                            volume = playerState.volume,
                            speed = playerState.playbackSpeed,
                            onVolumeChange = onVolumeChange,
                            onSpeedChange = onSpeedChange,
                            trackGainDb = playerState.trackGainDb,
                            onTrackGainChange = onTrackGainChange,
                            autoLoudnessEnabled = playerState.autoLoudnessEnabled,
                            onToggleAutoLoudness = onToggleAutoLoudness,
                            calibrationStatus = playerState.calibrationStatus,
                            isCurrentTrackCalibrated = playerState.isCurrentTrackCalibrated,
                            onStartCalibration = onStartCalibration,
                            onFinishCalibration = onFinishCalibration,
                            onCancelCalibration = onCancelCalibration,
                            onResetCalibration = onResetCalibration,
                        )
                        Spacer(Modifier.height(4.dp))
                        PaneSwitcher(pane, onSelect = { paneName = it.name })
                        Spacer(Modifier.height(4.dp))
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
private fun TactileTopBarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ResonanceColors.TextPrimary,
    glowColor: Color? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.89f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "topBtnScale",
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .then(
                if (glowColor != null) {
                    Modifier.shadow(elevation = 6.dp, shape = CircleShape, spotColor = glowColor)
                } else {
                    Modifier.shadow(elevation = 3.dp, shape = CircleShape, spotColor = Color.Black.copy(alpha = 0.22f))
                }
            )
            .clip(CircleShape)
            .background(
                if (glowColor != null) {
                    glowColor.copy(alpha = 0.16f)
                } else {
                    Color.White.copy(alpha = 0.08f)
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    if (glowColor != null) {
                        listOf(glowColor.copy(alpha = 0.70f), glowColor.copy(alpha = 0.25f))
                    } else {
                        listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.06f))
                    }
                ),
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Color.White.copy(alpha = 0.2f), bounded = true),
                onClick = onClick,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                var dragDistY = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragDistY = 0f },
                    onDragEnd = {
                        if (dragDistY > 50f) onDismiss()
                        dragDistY = 0f
                    },
                    onDragCancel = { dragDistY = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        dragDistY += dragAmount
                        if (dragDistY > 100f) {
                            onDismiss()
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.40f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.60f), CircleShape)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TactileTopBarButton(
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "关闭正在播放",
                onClick = onDismiss,
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "正在播放",
                    style = MaterialTheme.typography.labelMedium,
                    color = ResonanceColors.Muted,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = track.album.ifBlank { "本地曲目" },
                    style = MaterialTheme.typography.bodySmall,
                    color = ResonanceColors.Dim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TactileTopBarButton(
                icon = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (track.isFavorite) "取消收藏" else "收藏",
                onClick = { onToggleFavorite(track) },
                tint = if (track.isFavorite) Color(0xFFFF4766) else ResonanceColors.Muted,
                glowColor = if (track.isFavorite) Color(0xFFFF4766).copy(alpha = 0.45f) else null,
            )
            Spacer(Modifier.width(8.dp))
            Box {
                TactileTopBarButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = "播放选项",
                    onClick = { menuExpanded = true },
                    tint = ResonanceColors.Muted,
                )
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (onOpenSleepTimer != null) DropdownMenuItem(
                        text = { Text("睡眠定时器") },
                        onClick = { menuExpanded = false; onOpenSleepTimer() },
                    )
                    if (onToggleFloatingLyrics != null) DropdownMenuItem(
                        text = { Text(if (floatingLyricsEnabled) "关闭桌面歌词" else "开启桌面歌词") },
                        onClick = { menuExpanded = false; onToggleFloatingLyrics() },
                    )
                    if (!track.sourceUri.isNullOrBlank() && onDeleteLocalTrack != null) DropdownMenuItem(
                        text = { Text("删除本地音频", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDeleteLocalTrack(track) },
                    )
                    if (onOpenSleepTimer == null && onToggleFloatingLyrics == null && onDeleteLocalTrack == null) {
                        DropdownMenuItem(text = { Text("本地音乐播放") }, onClick = { menuExpanded = false }, enabled = false)
                    }
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
    onDismiss: (() -> Unit)? = null,
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
                (slideInHorizontally(ResonanceMotionTokens.PageSlideSpec) { it * dir } +
                    fadeIn(ResonanceMotionTokens.PageFadeInSpec)) togetherWith
                (slideOutHorizontally(ResonanceMotionTokens.PageSlideSpec) { -it * dir } +
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
                        ArtworkPane(track, playerState.isPlaying, Modifier.weight(1f), onDismiss = onDismiss)
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
private fun ArtworkPane(
    track: Track,
    playing: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val scale by animateFloatAsState(
        targetValue = if (LocalReducedMotion.current || playing) 1f else 0.98f,
        animationSpec = resonanceSpring(),
        label = "heroArtworkScale",
    )

    BoxWithConstraints(
        modifier = modifier.then(
            if (onDismiss != null) {
                Modifier.pointerInput(Unit) {
                    var dragY = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dragY = 0f },
                        onDragEnd = {
                            if (dragY > 60f) onDismiss()
                            dragY = 0f
                        },
                        onDragCancel = { dragY = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            dragY += dragAmount
                            if (dragY > 120f) {
                                onDismiss()
                            }
                        }
                    )
                }
            } else Modifier
        ),
        contentAlignment = Alignment.Center,
    ) {
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
            AnimatedContent(
                targetState = track.id,
                transitionSpec = {
                    (fadeIn(tween(300)) + scaleIn(initialScale = 0.94f, animationSpec = tween(300)))
                        .togetherWith(fadeOut(tween(220)) + scaleOut(targetScale = 1.04f, animationSpec = tween(220)))
                },
                label = "heroArtworkTransition",
            ) { _ ->
                AlbumArtwork(track.artworkSeed, Modifier.fillMaxSize(), 24.dp, track.artworkPath)
            }
        }
    }
}

@Composable
private fun TrackIdentity(track: Track) {
    AnimatedContent(
        targetState = track,
        transitionSpec = {
            (slideInHorizontally(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
            ) { width -> (width * 0.35f).toInt() } + fadeIn(tween(260)))
                .togetherWith(
                    slideOutHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                    ) { width -> -(width * 0.35f).toInt() } + fadeOut(tween(200))
                )
        },
        label = "trackIdentitySlide",
        modifier = Modifier.fillMaxWidth(),
    ) { currentTrack ->
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                currentTrack.title,
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
                    currentTrack.artist.ifBlank { "未知艺术家" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = ResonanceColors.Muted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (currentTrack.mimeType.contains("flac", ignoreCase = true)) "FLAC" else "320K",
                    style = MaterialTheme.typography.labelSmall,
                    color = ResonanceColors.Dim,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ResonanceColors.Soft)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ExquisitePlaybackScrubber(
    track: Track,
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val currentProgress = if (isDragging) dragFraction else progress.coerceIn(0f, 1f)

    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 5.dp else 3.5.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "scrubTrackH",
    )
    val knobSize by animateDpAsState(
        targetValue = if (isDragging) 14.dp else 8.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "scrubKnobSize",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.50f else 0f,
        animationSpec = tween(180),
        label = "scrubGlowAlpha",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(fraction)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            dragFraction = fraction
                            onSeek(fraction)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek(dragFraction)
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragFraction = fraction
                            onSeek(fraction)
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val activeWidth = maxWidth * currentProgress

            // 1. Inactive Track Rail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
            )

            // 2. Active Progress Track with luminous gradient
            Box(
                modifier = Modifier
                    .width(activeWidth)
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                ResonanceColors.Primary.copy(alpha = 0.85f),
                                ResonanceColors.PrimaryGlow,
                                ResonanceColors.PositiveGlow,
                            )
                        )
                    )
            )

            // 3. Exquisite Jewel Knob
            val knobOffset = (maxWidth - knobSize) * currentProgress
            Box(
                modifier = Modifier
                    .offset(x = knobOffset)
                    .size(knobSize),
                contentAlignment = Alignment.Center,
            ) {
                if (glowAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .size(knobSize + 8.dp)
                            .clip(CircleShape)
                            .background(ResonanceColors.Primary.copy(alpha = glowAlpha))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(knobSize)
                        .shadow(
                            elevation = if (isDragging) 6.dp else 2.5.dp,
                            shape = CircleShape,
                            spotColor = ResonanceColors.Primary.copy(alpha = 0.45f),
                        )
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(
                            width = if (isDragging) 2.dp else 1.5.dp,
                            color = ResonanceColors.Primary,
                            shape = CircleShape,
                        )
                )
            }
        }

        // Timestamps below
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPlaybackPosition(track.durationText, currentProgress),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = if (isDragging) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.5.sp,
                    letterSpacing = 0.3.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = if (isDragging) ResonanceColors.Primary else ResonanceColors.TextPrimary.copy(alpha = 0.72f),
            )
            Text(
                text = track.durationText,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.5.sp,
                    letterSpacing = 0.3.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = ResonanceColors.Dimmer,
            )
        }
    }
}

@Composable
private fun PlaybackProgress(track: Track, progress: Float, onSeek: (Float) -> Unit) {
    ExquisitePlaybackScrubber(track, progress, onSeek)
}

@Composable
private fun TactilePlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "playPressScale",
    )

    Box(
        modifier = modifier
            .size(68.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                spotColor = ResonanceColors.Primary.copy(alpha = 0.50f),
                ambientColor = ResonanceColors.Shadow.copy(alpha = 0.35f),
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        ResonanceColors.PrimaryGlow,
                        ResonanceColors.Primary,
                        ResonanceColors.Positive,
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent,
                    )
                ),
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Color.White.copy(alpha = 0.3f), bounded = true),
                onClick = onClick,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.65f)
                .height(18.dp)
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.Transparent,
                        )
                    )
                )
        )
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "暂停" else "播放",
            tint = Color.White,
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
private fun TactileSkipButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.89f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "skipPressScale",
    )

    Box(
        modifier = modifier
            .size(52.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = 0.30f),
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f),
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.38f),
                        Color.White.copy(alpha = 0.08f),
                    )
                ),
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Color.White.copy(alpha = 0.2f), bounded = true),
                onClick = onClick,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isPressed) ResonanceColors.Primary else ResonanceColors.TextPrimary,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun TactileSecondaryButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "secondaryPressScale",
    )

    Box(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .then(
                if (isActive) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        spotColor = ResonanceColors.Primary.copy(alpha = 0.40f),
                    )
                } else Modifier
            )
            .clip(CircleShape)
            .background(
                if (isActive) {
                    Brush.linearGradient(
                        listOf(
                            ResonanceColors.PrimarySoft,
                            ResonanceColors.PrimarySoft.copy(alpha = 0.35f),
                        )
                    )
                } else {
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.07f),
                            Color.White.copy(alpha = 0.02f),
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = if (isActive) {
                    Brush.verticalGradient(
                        listOf(
                            ResonanceColors.Primary.copy(alpha = 0.70f),
                            ResonanceColors.Primary.copy(alpha = 0.30f),
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.05f),
                        )
                    )
                },
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Color.White.copy(alpha = 0.2f), bounded = true),
                onClick = onClick,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) ResonanceColors.Primary else ResonanceColors.Dim,
                modifier = Modifier.size(20.dp),
            )
            if (isActive) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .clip(CircleShape)
                        .background(ResonanceColors.Primary)
                )
            }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TactileSecondaryButton(
            icon = Icons.Default.Shuffle,
            contentDescription = if (playerState.shuffleEnabled) "关闭随机播放" else "开启随机播放",
            isActive = playerState.shuffleEnabled,
            onClick = onToggleShuffle,
        )
        TactileSkipButton(
            icon = Icons.Default.SkipPrevious,
            contentDescription = "上一首",
            onClick = onPrevious,
        )
        TactilePlayPauseButton(
            isPlaying = playerState.isPlaying,
            onClick = onTogglePlay,
        )
        TactileSkipButton(
            icon = Icons.Default.SkipNext,
            contentDescription = "下一首",
            onClick = onNext,
        )
        TactileSecondaryButton(
            icon = if (playerState.repeatMode == RepeatMode.One) Icons.Default.RepeatOne else Icons.Default.Repeat,
            contentDescription = when (playerState.repeatMode) {
                RepeatMode.Off -> "开启列表循环"
                RepeatMode.All -> "开启单曲循环"
                RepeatMode.One -> "关闭循环"
            },
            isActive = playerState.repeatMode != RepeatMode.Off,
            onClick = onCycleRepeat,
        )
    }
}

@Composable
private fun ExquisiteVolumeSlider(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragVolume by remember { mutableFloatStateOf(volume) }
    val displayedVolume = if (isDragging) dragVolume else volume.coerceIn(0f, 1f)

    val trackHeight by animateDpAsState(if (isDragging) 4.5.dp else 3.dp, label = "volTrackH")
    val knobSize by animateDpAsState(if (isDragging) 12.dp else 7.dp, label = "volKnobSize")

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onVolumeChange(fraction)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        dragVolume = fraction
                        onVolumeChange(fraction)
                    },
                    onDragEnd = {
                        isDragging = false
                        onVolumeChange(dragVolume)
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragVolume = fraction
                        onVolumeChange(fraction)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val filledWidth = maxWidth * displayedVolume

        // Track background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        )

        // Active Track
        Box(
            modifier = Modifier
                .width(filledWidth)
                .height(trackHeight)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            ResonanceColors.TextPrimary.copy(alpha = 0.85f),
                            ResonanceColors.TextPrimary,
                        )
                    )
                )
        )

        // Knob
        val knobOffset = (maxWidth - knobSize) * displayedVolume
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(knobSize)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, ResonanceColors.CanvasElevated, CircleShape)
        )
    }
}

@Composable
private fun VolumeAndSpeedRow(
    volume: Float,
    speed: Float,
    onVolumeChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    trackGainDb: Float = 0f,
    onTrackGainChange: (Float) -> Unit = {},
    autoLoudnessEnabled: Boolean = true,
    onToggleAutoLoudness: () -> Unit = {},
    calibrationStatus: CalibrationStatus = CalibrationStatus(),
    isCurrentTrackCalibrated: Boolean = false,
    onStartCalibration: () -> Unit = {},
    onFinishCalibration: () -> Unit = {},
    onCancelCalibration: () -> Unit = {},
    onResetCalibration: () -> Unit = {},
) {
    var showSpeedMenu by remember { mutableStateOf(false) }
    var previousVolume by remember { mutableStateOf(1f) }
    var showCalibration by remember { mutableStateOf(false) }

    val isCalibrationActive = calibrationStatus.isActive
    val isPanelVisible = showCalibration || isCalibrationActive

    fun formatGain(db: Float): String {
        val rounded = kotlin.math.round(db * 10) / 10f
        return if (rounded > 0f) "+$rounded dB" else "$rounded dB"
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TactileTopBarButton(
                icon = when {
                    volume <= 0.01f -> Icons.AutoMirrored.Filled.VolumeOff
                    volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                    else -> Icons.AutoMirrored.Filled.VolumeUp
                },
                contentDescription = "音量调节与静音",
                onClick = {
                    if (volume > 0.01f) {
                        previousVolume = volume
                        onVolumeChange(0f)
                    } else {
                        onVolumeChange(if (previousVolume > 0.05f) previousVolume else 1f)
                    }
                },
                tint = if (volume <= 0.01f) ResonanceColors.Dim else ResonanceColors.TextPrimary,
                modifier = Modifier.size(34.dp),
            )

            Spacer(Modifier.width(8.dp))

            ExquisiteVolumeSlider(
                volume = volume,
                onVolumeChange = onVolumeChange,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = "${(volume.coerceIn(0f, 1f) * 100).toInt()}%",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = ResonanceColors.Dim,
                modifier = Modifier.width(34.dp),
                textAlign = TextAlign.End,
            )

            Spacer(Modifier.width(8.dp))

            Box {
                val speedInteraction = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(30.dp)
                        .clip(CircleShape)
                        .background(if (speed != 1.0f) ResonanceColors.PrimarySoft else Color.White.copy(alpha = 0.07f))
                        .border(
                            1.dp,
                            if (speed != 1.0f) ResonanceColors.Primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f),
                            CircleShape
                        )
                        .resonancePressable(speedInteraction, pressedScale = 0.92f)
                        .clickable(interactionSource = speedInteraction, indication = null) { showSpeedMenu = true }
                        .padding(horizontal = 9.dp),
                ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (speed != 1.0f) ResonanceColors.Primary else ResonanceColors.Dim,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${if (speed == 1.0f) "1.0" else speed}x",
                        style = MaterialTheme.typography.labelSmall,
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

            Spacer(Modifier.width(6.dp))

            TactileTopBarButton(
                icon = Icons.Default.Tune,
                contentDescription = "响度增益与专属校准模式",
                onClick = { showCalibration = !showCalibration },
                tint = if (isPanelVisible || isCurrentTrackCalibrated || trackGainDb != 0f) ResonanceColors.Primary else ResonanceColors.Dim,
                glowColor = if (isPanelVisible || isCurrentTrackCalibrated || trackGainDb != 0f) ResonanceColors.Primary.copy(alpha = 0.45f) else null,
                modifier = Modifier.size(34.dp),
            )
        }

        AnimatedVisibility(
            visible = isPanelVisible,
            enter = fadeIn(tween(220)) + expandVertically(),
            exit = fadeOut(tween(180)) + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp, start = 4.dp, end = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ResonanceColors.PrimarySoft.copy(alpha = 0.35f))
                    .border(1.dp, ResonanceColors.GlassBorderGlow.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = ResonanceColors.Primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "音频前级响度均衡 & 专属校准",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ResonanceColors.TextPrimary,
                        )
                    }
                    Text(
                        text = when {
                            isCalibrationActive -> "🔴 校准中"
                            isCurrentTrackCalibrated -> "🎯 已专属记忆 (${formatGain(trackGainDb)})"
                            autoLoudnessEnabled && trackGainDb != 0f -> "⚡ 自动均衡 (${formatGain(trackGainDb)})"
                            trackGainDb != 0f -> formatGain(trackGainDb)
                            else -> "0.0 dB (标准)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCalibrationActive || isCurrentTrackCalibrated || trackGainDb != 0f) ResonanceColors.Primary else ResonanceColors.Dim,
                    )
                }

                Spacer(Modifier.height(6.dp))

                if (isCalibrationActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ResonanceColors.Primary.copy(alpha = 0.15f))
                            .border(1.dp, ResonanceColors.Primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            "🔴 正在进行物理音量校准模式",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ResonanceColors.Primary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "请直接使用手机侧边的【物理音量按键】调节到当前曲目最舒适的音量。",
                            style = MaterialTheme.typography.bodySmall,
                            color = ResonanceColors.TextPrimary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "基准音量: ${calibrationStatus.baseVolume} → 当前: ${calibrationStatus.currentVolume} / ${calibrationStatus.maxVolume}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ResonanceColors.Dim
                            )
                            Text(
                                "相对增益: ${formatGain(calibrationStatus.deltaDb)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ResonanceColors.Primary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onCancelCalibration) {
                                Text("取消", color = ResonanceColors.Dim)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = onFinishCalibration,
                                colors = ButtonDefaults.buttonColors(containerColor = ResonanceColors.Primary)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("保存并还原手机音量", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    if (isCurrentTrackCalibrated) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ResonanceColors.Soft)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "🎯 本曲已记忆专属振幅 (${formatGain(trackGainDb)})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ResonanceColors.Primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "播放时已自动应用专属振幅，不影响手机系统音量",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ResonanceColors.Dim
                                )
                            }
                            Row {
                                TextButton(onClick = onStartCalibration) {
                                    Text("重新校准", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Primary)
                                }
                                TextButton(onClick = onResetCalibration) {
                                    Text("清除", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Dim)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (autoLoudnessEnabled) ResonanceColors.Primary else ResonanceColors.Dim,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "全自动动态响度均衡",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ResonanceColors.TextPrimary
                                    )
                                }
                                Text(
                                    if (autoLoudnessEnabled) "EBU R128 标准，平拉钢琴/弱录音与流行乐" else "已关闭自动均衡，使用标准默认输出",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ResonanceColors.Dim
                                )
                            }
                            SuggestionChip(
                                onClick = onToggleAutoLoudness,
                                label = { Text(if (autoLoudnessEnabled) "已开启" else "已关闭", style = MaterialTheme.typography.labelSmall) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (autoLoudnessEnabled) ResonanceColors.Primary.copy(alpha = 0.2f) else ResonanceColors.Soft,
                                    labelColor = if (autoLoudnessEnabled) ResonanceColors.Primary else ResonanceColors.Dim
                                ),
                                border = if (autoLoudnessEnabled) BorderStroke(1.dp, ResonanceColors.Primary) else null,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onStartCalibration,
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            border = BorderStroke(1.dp, ResonanceColors.GlassBorderGlow)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = ResonanceColors.Primary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("开启物理音量校准模式 (按手机侧键记忆)", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.TextPrimary)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("-12dB", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Dim)
                    Slider(
                        value = trackGainDb.coerceIn(-12f, 12f),
                        onValueChange = onTrackGainChange,
                        valueRange = -12f..12f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = ResonanceColors.Primary,
                            activeTrackColor = ResonanceColors.Primary,
                            inactiveTrackColor = ResonanceColors.DividerStrong,
                        ),
                    )
                    Text("+12dB", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Dim)
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(
                        "标准 (0dB)" to 0f,
                        "钢琴/弱音 (+6dB)" to 6f,
                        "强劲 (+10dB)" to 10f,
                        "降躁 (-3dB)" to -3f,
                    ).forEach { (label, presetGain) ->
                        val isSelected = kotlin.math.abs(trackGainDb - presetGain) < 0.1f
                        SuggestionChip(
                            onClick = { onTrackGainChange(presetGain) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) ResonanceColors.Primary.copy(alpha = 0.2f) else ResonanceColors.Soft,
                                labelColor = if (isSelected) ResonanceColors.Primary else ResonanceColors.TextPrimary,
                            ),
                            border = if (isSelected) BorderStroke(1.dp, ResonanceColors.Primary) else null,
                            modifier = Modifier.height(28.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = ResonanceColors.Positive,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "软限幅防削波保护已激活 · Hi-Fi 无损保真无杂音",
                        style = MaterialTheme.typography.labelSmall,
                        color = ResonanceColors.Positive,
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
                if (reducedMotion) {
                    listState.scrollToItem(activeIndex)
                } else {
                    val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == activeIndex }
                    if (itemInfo != null) {
                        val viewportHeight = listState.layoutInfo.viewportSize.height
                        val targetOffset = (viewportHeight - itemInfo.size) / 2
                        val diff = itemInfo.offset - targetOffset
                        listState.animateScrollBy(
                            diff.toFloat(),
                            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                        )
                    } else {
                        listState.animateScrollToItem(activeIndex)
                    }
                }
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
        targetValue = if (active) ResonanceColors.Primary else ResonanceColors.Dim.copy(alpha = 0.55f),
        animationSpec = tween(motionDuration(300)),
        label = "lyricColor",
    )
    val activeScale by animateFloatAsState(
        targetValue = if (active) 1.05f else 0.98f,
        animationSpec = resonanceSpring(),
        label = "lyricScale",
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
            .heightIn(min = 44.dp)
            .graphicsLayer {
                scaleX = activeScale
                scaleY = activeScale
            }
            .then(interaction)
            .padding(horizontal = 14.dp, vertical = 8.dp),
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
        val queueListState = rememberLazyListState()
        LaunchedEffect(currentTrackId) {
            val idx = queue.indexOfFirst { it.id == currentTrackId }
            if (idx >= 0) {
                val isVisible = queueListState.layoutInfo.visibleItemsInfo.any { it.index == idx }
                if (!isVisible) {
                    queueListState.animateScrollToItem(idx)
                }
            }
        }
        LazyColumn(
            state = queueListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            itemsIndexed(queue, key = { index, item -> "queue-${item.id}-$index" }) { _, item ->
                val current = item.id == currentTrackId
                val interactionSource = remember { MutableInteractionSource() }
                val itemBg by animateColorAsState(
                    targetValue = if (current) ResonanceColors.PrimarySoft else Color.Transparent,
                    animationSpec = tween(280),
                    label = "queueItemBg",
                )
                val itemBorder by animateColorAsState(
                    targetValue = if (current) ResonanceColors.GlassBorderGlow.copy(alpha = 0.45f) else Color.Transparent,
                    animationSpec = tween(280),
                    label = "queueItemBorder",
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(itemBg)
                        .border(1.dp, itemBorder, RoundedCornerShape(12.dp))
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
