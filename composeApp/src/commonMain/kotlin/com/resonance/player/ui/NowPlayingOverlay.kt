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
import com.resonance.player.ui.common.SoftHeroPlayButton
import com.resonance.player.ui.common.SoftSkipButton
import com.resonance.player.ui.common.SoftUtilityButton
import com.resonance.player.ui.common.SoftRefinedPlaybackScrubber
import com.resonance.player.ui.common.SoftRefinedVolumeSlider
import com.resonance.player.ui.common.SoftNeumorphicTokens
import com.resonance.player.ui.common.softConvexDisc
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
import androidx.compose.material.icons.filled.PictureInPicture
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
import com.resonance.player.tutorial.TutorialTarget
import com.resonance.player.tutorial.TutorialCompletion
import com.resonance.player.model.durationTextToSeconds
import com.resonance.player.platform.ResonanceBackHandler

private enum class PlayerPane(val label: String, val icon: ImageVector) {
    Cover("封面", Icons.Default.Album),
    Lyrics("歌词", Icons.Default.GraphicEq),
    Queue("队列", Icons.AutoMirrored.Filled.QueueMusic),
}

@Composable
internal fun NowPlayingOverlay(
    ambientMotionEnabled: Boolean = true,
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
    val tutorial = LocalTutorial.current
    LaunchedEffect(pane, tutorial?.step?.id) {
        if (pane == PlayerPane.Lyrics) tutorial?.event(TutorialCompletion.LyricsViewed)
        if (pane == PlayerPane.Queue) tutorial?.event(TutorialCompletion.QueueViewed)
    }

    ResonanceBackHandler(enabled = true, onBack = onDismiss)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                spotColor = Color.Black.copy(alpha = 0.40f),
            )
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
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
                NowPlayingBackdrop(track.artworkSeed, playerState.isPlaying && ambientMotionEnabled, Modifier.matchParentSize().hazeSource(glassState))
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
    tint: Color? = null,
    glowColor: Color? = null,
) {
    SoftUtilityButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        size = 40.dp,
        isActive = glowColor != null,
        activeColor = tint ?: SoftNeumorphicTokens.ActiveEmerald,
        activeGlowColor = glowColor ?: SoftNeumorphicTokens.ActiveEmeraldGlow,
        customTint = tint,
    )
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
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
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
                modifier = Modifier.tutorialTarget(TutorialTarget.PlayerFavorite),
                tint = if (track.isFavorite) SoftNeumorphicTokens.ActiveHeart else null,
                glowColor = if (track.isFavorite) SoftNeumorphicTokens.ActiveHeart.copy(alpha = 0.45f) else null,
            )
            if (onToggleFloatingLyrics != null) {
                Spacer(Modifier.width(8.dp))
                TactileTopBarButton(
                    icon = Icons.Default.PictureInPicture,
                    contentDescription = if (floatingLyricsEnabled) "关闭桌面歌词" else "开启桌面歌词",
                    onClick = onToggleFloatingLyrics,
                    tint = if (floatingLyricsEnabled) ResonanceColors.Primary else ResonanceColors.Muted,
                    glowColor = if (floatingLyricsEnabled) ResonanceColors.Primary.copy(alpha = 0.45f) else null,
                )
            }
            Spacer(Modifier.width(8.dp))
            Box {
                TactileTopBarButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = "播放选项",
                    onClick = { menuExpanded = true },
                    tint = ResonanceColors.Muted,
                )
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    LocalMusicFeatures.current?.let { feature ->
                        DropdownMenuItem(text = { Text("匹配专辑封面") }, onClick = { menuExpanded = false; feature.launch { feature.assistant?.matchCover(track); feature.onReload() } })
                        DropdownMenuItem(text = { Text("自定义封面") }, onClick = { menuExpanded = false; feature.launch { feature.services.chooseCoverImage()?.let { feature.assistant?.setCover(track, it); feature.onReload() } } })
                        DropdownMenuItem(text = { Text("生成同步歌词") }, onClick = { menuExpanded = false; feature.showGenerateLyrics = true })
                    }
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
                        .then(when (pane) {
                            PlayerPane.Lyrics -> Modifier.tutorialTarget(TutorialTarget.PlayerLyrics)
                            PlayerPane.Queue -> Modifier.tutorialTarget(TutorialTarget.PlayerQueue)
                            else -> Modifier
                        })
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
                        nativePositionMs = playerState.positionMs,
                        lyricsState = lyricsState,
                        onOpenFullLyrics = onOpenFullLyrics,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    )
                }
            }
            PlayerPane.Lyrics -> LyricsPane(
                track = track,
                progress = playerState.progress,
                nativePositionMs = playerState.positionMs,
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
    nativePositionMs: Long? = null,
    lyricsState: LyricsUiState,
    onOpenFullLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lyricMotion = resonanceSpring<androidx.compose.ui.unit.IntOffset>()
    val reducedMotion = LocalReducedMotion.current
    val durationMs = durationTextToSeconds(track.durationText).coerceAtLeast(1) * 1_000L
    val positionMs = (nativePositionMs ?: (durationMs * progress.coerceIn(0f, 1f)).toLong())
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
                    val firstLine = "纯文本歌词 · 点击浏览或生成同步歌词"
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
private fun PlaybackProgress(track: Track, progress: Float, onSeek: (Float) -> Unit) {
    SoftRefinedPlaybackScrubber(
        progress = progress,
        durationText = track.durationText,
        onSeek = onSeek,
        formatPosition = ::formatPlaybackPosition,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun TactilePlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftHeroPlayButton(
        isPlaying = isPlaying,
        onClick = onClick,
        modifier = modifier,
        size = 70.dp,
    )
}

@Composable
private fun TactileSkipButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftSkipButton(
        isNext = icon == Icons.Default.SkipNext,
        onClick = onClick,
        modifier = modifier,
        size = 54.dp,
        contentDescription = contentDescription,
    )
}

@Composable
private fun TactileSecondaryButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftUtilityButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        size = 44.dp,
        isActive = isActive,
    )
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
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
                modifier = Modifier.size(36.dp),
            )

            Spacer(Modifier.width(8.dp))

            SoftRefinedVolumeSlider(
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
                val speedPressed by speedInteraction.collectIsPressedAsState()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(30.dp)
                        .graphicsLayer {
                            val s = if (speedPressed) 0.92f else 1f
                            scaleX = s
                            scaleY = s
                        }
                        .softConvexDisc(
                            isPressed = speedPressed,
                            elevationResting = if (speed != 1.0f) 5.dp else 3.dp,
                            elevationPressed = 1.dp,
                            glowColor = if (speed != 1.0f) SoftNeumorphicTokens.ActiveEmeraldGlow else null,
                        )
                        .clickable(interactionSource = speedInteraction, indication = null) { showSpeedMenu = true }
                        .padding(horizontal = 9.dp),
                ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (speed != 1.0f) SoftNeumorphicTokens.ActiveEmerald else (if (ResonanceColors.isDark) Color(0xFFA5BDB6) else SoftNeumorphicTokens.IconPetroleumTeal.copy(alpha = 0.76f)),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${if (speed == 1.0f) "1.0" else speed}x",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (speed != 1.0f) SoftNeumorphicTokens.ActiveEmerald else (if (ResonanceColors.isDark) Color(0xFFDCEDE7) else SoftNeumorphicTokens.IconPetroleumTeal),
                    )
                }
                DropdownMenu(
                    expanded = showSpeedMenu,
                    onDismissRequest = { showSpeedMenu = false },
                ) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { s ->
                        DropdownMenuItem(
                            text = { Text("${s}x 倍速", color = if (s == speed) SoftNeumorphicTokens.ActiveEmerald else ResonanceColors.TextPrimary) },
                            onClick = {
                                onSpeedChange(s)
                                showSpeedMenu = false
                            },
                            trailingIcon = if (s == speed) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = SoftNeumorphicTokens.ActiveEmerald, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }
            }

            Spacer(Modifier.width(6.dp))

            androidx.compose.material3.FilterChip(
                selected = autoLoudnessEnabled,
                onClick = onToggleAutoLoudness,
                label = { Text("音量平衡") },
            )
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
    nativePositionMs: Long? = null,
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
            is LyricsUiState.Unavailable -> Column(Modifier.fillMaxSize()) { TranslationControls(); Box(Modifier.weight(1f)) { LyricsUnavailable(state.message, onRefresh, onRequestAi) } }
            is LyricsUiState.Ready -> LyricsContent(
                track = track,
                progress = progress,
                nativePositionMs = nativePositionMs,
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
                Text("AI 查找歌词", color = ResonanceColors.Primary)
            }
        }
    }
}

@Composable
private fun LyricsContent(
    track: Track,
    progress: Float,
    nativePositionMs: Long? = null,
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
    val positionMs = (nativePositionMs ?: (durationMs * progress.coerceIn(0f, 1f)).toLong()) + lyrics.offsetMs
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
        TranslationControls()
        Text(if (lyrics.synchronized) "同步歌词 · ${lyrics.source}" else "纯文本歌词 · 可手动滚动，或生成同步歌词", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted, modifier = Modifier.padding(horizontal = 8.dp))
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
                        onClick = line.timestampMs?.takeIf { lyrics.synchronized }?.let { timestamp ->
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
        val display = LocalMusicFeatures.current?.settings?.lyricDisplay ?: com.resonance.player.model.LyricDisplay.Bilingual
        Column {
        Text(
            if (display == com.resonance.player.model.LyricDisplay.Translation) line.translation ?: line.text else line.text,
            style = if (active) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = color,
        )
        if (display == com.resonance.player.model.LyricDisplay.Bilingual && !line.translation.isNullOrBlank()) {
            Text(line.translation, style = MaterialTheme.typography.bodyMedium, color = color.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp))
        }
        }
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
