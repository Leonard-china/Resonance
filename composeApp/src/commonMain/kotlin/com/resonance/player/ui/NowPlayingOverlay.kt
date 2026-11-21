package com.resonance.player.ui

import com.resonance.player.design.lyricFocus

import com.resonance.player.design.FrostedAlertDialog as AlertDialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ripple
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.resonance.player.design.ResonanceMotionTokens
import com.resonance.player.ui.common.SoftHeroPlayButton
import com.resonance.player.ui.common.SoftSkipButton
import com.resonance.player.ui.common.SoftUtilityButton
import com.resonance.player.ui.common.SoftRefinedPlaybackScrubber
import com.resonance.player.ui.common.SoftRefinedVolumeSlider
import com.resonance.player.ui.common.SoftNeumorphicTokens
import com.resonance.player.ui.common.softConvexDisc
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.delay
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
import androidx.compose.material.icons.filled.Subtitles
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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
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
import androidx.compose.foundation.border
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.FrostedBackdrop
import com.resonance.player.design.MusicStageBackdrop
import com.resonance.player.design.LocalAlbumAtmosphere
import com.resonance.player.design.rememberAlbumAtmosphere
import com.resonance.player.design.musicSleevePerspective
import com.resonance.player.design.musicDock
import com.resonance.player.design.GalleryArtworkMount
import com.resonance.player.design.GalleryTokens
import com.resonance.player.design.galleryPlinth
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
import com.resonance.player.tutorial.TutorialTarget
import com.resonance.player.tutorial.TutorialCompletion
import com.resonance.player.model.durationTextToSeconds
import com.resonance.player.platform.ResonanceBackHandler

private enum class PlayerPane(val label: String, val icon: ImageVector) {
    Cover("封面", Icons.Default.Album),
    Lyrics("歌词", Icons.Default.GraphicEq),
    Queue("队列", Icons.AutoMirrored.Filled.QueueMusic),
}

private val LocalLyricStage = androidx.compose.runtime.staticCompositionLocalOf { false }

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
    onAudioEffectPresetChange: (com.resonance.player.model.AudioEffectPreset) -> Unit = {},
    onPlayNext: ((Track) -> Unit)? = null,
    onRemoveFromQueue: ((Track) -> Unit)? = null,
    onClearQueue: (() -> Unit)? = null,
    onMoveInQueueOrder: ((Int, Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val track = playerState.currentTrack ?: return
    val atmosphere = rememberAlbumAtmosphere(track.artworkSeed, track.artworkPath, track.sourceUri)
    val glassState = rememberHazeState()
    var paneName by rememberSaveable { mutableStateOf<String?>(null) }
    var playbackSettings by remember { mutableStateOf(false) }
    var audioEffects by remember { mutableStateOf(false) }
    val pane = PlayerPane.entries.firstOrNull { it.name == paneName } ?: PlayerPane.Cover
    val tutorial = LocalTutorial.current


    ResonanceBackHandler(enabled = true, onBack = onDismiss)
    if (audioEffects) AudioEffectDialog(currentPreset = playerState.audioEffectPreset,
        onPresetSelect = onAudioEffectPresetChange, onDismiss = { audioEffects = false })
    if (playbackSettings) AlertDialog(onDismissRequest = { playbackSettings = false }, title = { Text("播放设置") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            VolumeAndSpeedRow(playerState.volume, playerState.playbackSpeed, onVolumeChange, onSpeedChange,
                playerState.trackGainDb, onTrackGainChange, playerState.autoLoudnessEnabled, onToggleAutoLoudness,
                playerState.calibrationStatus, playerState.isCurrentTrackCalibrated,
                onStartCalibration, onFinishCalibration, onCancelCalibration, onResetCalibration,
                audioEffectPreset = playerState.audioEffectPreset,
                onAudioEffectPresetChange = onAudioEffectPresetChange,
                compact = true)
        } }, confirmButton = { TextButton(onClick = { playbackSettings = false }) { Text("完成") } })

    CompositionLocalProvider(
        com.resonance.player.design.LocalResonanceColors provides com.resonance.player.design.ImmersivePlayerColors,
        LocalLyricStage provides true,
    ) {
    MaterialTheme(colorScheme = com.resonance.player.design.ImmersivePlayerColorScheme) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                spotColor = Color.Black.copy(alpha = 0.45f),
            )
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        color = ResonanceColors.Canvas,
        contentColor = ResonanceColors.TextPrimary,
    ) {
        CompositionLocalProvider(LocalGlassState provides glassState, LocalAlbumAtmosphere provides atmosphere) {
            BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                val wide = maxWidth >= 720.dp || maxWidth > maxHeight * 1.35f
                val effectivePane = if (wide && paneName == null) PlayerPane.Lyrics else pane
                LaunchedEffect(effectivePane, tutorial?.step?.id) {
                    if (effectivePane == PlayerPane.Lyrics) tutorial?.event(TutorialCompletion.LyricsViewed)
                    if (effectivePane == PlayerPane.Queue) tutorial?.event(TutorialCompletion.QueueViewed)
                }
                val short = maxHeight < 560.dp || (LocalDensity.current.fontScale > 1.2f && maxHeight < 840.dp)
                FrostedBackdrop(
                    track = track,
                    modifier = Modifier.matchParentSize().hazeSource(glassState),
                    immersive = true,
                    isPlaying = playerState.isPlaying && ambientMotionEnabled,
                )
                val statusText = if (playerState.isBuffering) "正在缓冲…" else if (playerState.isPreparing) "正在准备…" else null
                Column(
                    modifier = Modifier.align(Alignment.Center).widthIn(max = 1280.dp).heightIn(max = 900.dp)
                        .fillMaxSize().padding(horizontal = if (wide) 36.dp else 20.dp, vertical = 10.dp),
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
                        onOpenPlaybackSettings = { playbackSettings = true },
                        onAudioEffectPresetChange = onAudioEffectPresetChange,
                    )
                    if (wide) {
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(52.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(.45f).fillMaxHeight().padding(bottom = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                ArtworkPane(track, playerState.isPlaying, Modifier.weight(1f).fillMaxWidth())
                                Spacer(Modifier.height(if (short) 6.dp else 18.dp))
                                TrackIdentity(track, compact = short, statusText = statusText)
                                Spacer(Modifier.height(if (short) 2.dp else 6.dp))
                                PlaybackProgress(track, playerState.progress, onSeek, playerState.isPlaying)
                                PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                                PlayerVolume(playerState.volume, onVolumeChange)
                                if (!short) PlayerQuickActions(
                                    playerState = playerState,
                                    onSettings = { playbackSettings = true },
                                    onEffects = { audioEffects = true },
                                    onSleep = onOpenSleepTimer,
                                )
                            }
                            Column(Modifier.weight(.55f).fillMaxHeight()) {
                                CompositionLocalProvider(LocalLyricStage provides true) {
                                    PaneContent(effectivePane, { paneName = it.name }, track, playerState, lyricsState, queue,
                                        onSeek, onTrackSelected, onRefreshLyrics, onRequestAiLyrics,
                                        onOpenFullLyrics = { paneName = PlayerPane.Lyrics.name },
                                        onAdjustOffset = onAdjustLyricsOffset, onEmbedLyrics = onEmbedLyrics,
                                        showArtwork = false, modifier = Modifier.weight(1f),
                                        onPlayNext = onPlayNext, onRemoveFromQueue = onRemoveFromQueue,
                                        onClearQueue = onClearQueue, onMoveInQueueOrder = onMoveInQueueOrder)
                                }
                                Spacer(Modifier.height(6.dp))
                                PaneSwitcher(effectivePane, onSelect = { paneName = it.name })
                            }
                        }
                    } else {
                        PaneContent(
                            effectivePane, { paneName = it.name }, track, playerState, lyricsState, queue,
                            onSeek, onTrackSelected, onRefreshLyrics, onRequestAiLyrics,
                            onOpenFullLyrics = { paneName = PlayerPane.Lyrics.name },
                            onAdjustOffset = onAdjustLyricsOffset, onEmbedLyrics = onEmbedLyrics,
                            modifier = Modifier.weight(1f),
                            onPlayNext = onPlayNext, onRemoveFromQueue = onRemoveFromQueue,
                            onClearQueue = onClearQueue, onMoveInQueueOrder = onMoveInQueueOrder,
                        )
                        Spacer(Modifier.height(if (short) 4.dp else 12.dp))
                        TrackIdentity(track, compact = short, singleLine = short, statusText = statusText)
                        Spacer(Modifier.height(if (short) 2.dp else 4.dp))
                        PlaybackProgress(track, playerState.progress, onSeek, playerState.isPlaying)
                        PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                        if (!short) {
                            PlayerVolume(playerState.volume, onVolumeChange)
                            PlayerQuickActions(
                                playerState = playerState,
                                onSettings = { playbackSettings = true },
                                onEffects = { audioEffects = true },
                                onSleep = onOpenSleepTimer,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        PaneSwitcher(effectivePane, onSelect = { paneName = it.name })
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}
}
}

/** Native stage lighting; the atmosphere is decorative, never presented as an audio spectrum. */
@Composable
private fun NowPlayingBackdrop(seed: Int, isPlaying: Boolean, modifier: Modifier = Modifier) {
    MusicStageBackdrop(seed, isPlaying, modifier.fillMaxSize(), LocalAlbumAtmosphere.current)
}

@Composable
private fun TactileTopBarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    glowColor: Color? = null,
    popOnActivate: Boolean = false,
) {
    val active = glowColor != null
    val interaction = remember { MutableInteractionSource() }
    val activeColor = tint ?: Color.White
    val reduced = LocalReducedMotion.current
    val popScale = remember { Animatable(1f) }
    var wasActive by remember { mutableStateOf(active) }
    LaunchedEffect(active, popOnActivate, reduced) {
        if (popOnActivate && active && !wasActive && !reduced) {
            popScale.snapTo(1f)
            popScale.animateTo(1.30f, spring(dampingRatio = 0.42f, stiffness = 520f))
            popScale.animateTo(1f, spring(dampingRatio = 0.56f, stiffness = 360f))
        }
        wasActive = active
    }
    val fill by animateColorAsState(
        targetValue = if (active) activeColor.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.09f),
        animationSpec = tween(motionDuration(ResonanceMotionTokens.StateDuration)),
        label = "topBarBtnFill",
    )
    val rim by animateColorAsState(
        targetValue = if (active) activeColor.copy(alpha = 0.36f) else Color.White.copy(alpha = 0.13f),
        animationSpec = tween(motionDuration(ResonanceMotionTokens.StateDuration)),
        label = "topBarBtnRim",
    )
    Box(
        modifier = modifier
            .size(48.dp)
            .semantics {
                this.contentDescription = contentDescription
                if (active) stateDescription = "已开启"
            }
            .clickable(interactionSource = interaction, indication = ripple(bounded = false, radius = 24.dp), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .resonancePressable(interaction, pressedScale = 0.90f)
                .clip(CircleShape)
                .background(fill)
                .border(1.dp, rim, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                null,
                Modifier.size(20.dp).graphicsLayer {
                    scaleX = popScale.value
                    scaleY = popScale.value
                },
                tint = tint ?: Color.White.copy(alpha = 0.90f),
            )
        }
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
    onOpenPlaybackSettings: () -> Unit = {},
    onAudioEffectPresetChange: (com.resonance.player.model.AudioEffectPreset) -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showTrackInfo by remember(track.id) { mutableStateOf(false) }
    var showEffectDialogInMenu by remember { mutableStateOf(false) }
    if (showEffectDialogInMenu) {
        AudioEffectDialog(
            currentPreset = playerState.audioEffectPreset,
            onPresetSelect = onAudioEffectPresetChange,
            onDismiss = { showEffectDialogInMenu = false }
        )
    }
    if (showTrackInfo) {
        val feature = LocalMusicFeatures.current
        val state = feature?.assistant?.state?.collectAsState()?.value
        val stats = rememberListeningStats(feature?.services).stats.trackStats[track.id]
        AlertDialog(onDismissRequest = { showTrackInfo = false }, title = { Text("歌曲与来源") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { TrackSourceDetails(track, state?.origins?.get(track.id), stats, playerState.audioEffectPreset) } },
            confirmButton = { TextButton(onClick = { showTrackInfo = false }) { Text("关闭") } })
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("player-drag-handle")
            .then(playerDismissGestureModifier())
            .drawBehind {
                drawRoundRect(Color.White.copy(alpha = .28f),
                    androidx.compose.ui.geometry.Offset(size.width / 2f - 18.dp.toPx(), 0f),
                    androidx.compose.ui.geometry.Size(36.dp.toPx(), 3.dp.toPx()),
                    androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx()))
            }
    ) {
        val rightActions: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TactileTopBarButton(
                    icon = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (track.isFavorite) "取消收藏" else "收藏",
                    onClick = { onToggleFavorite(track) },
                    modifier = Modifier.tutorialTarget(TutorialTarget.PlayerFavorite),
                    tint = if (track.isFavorite) SoftNeumorphicTokens.ActiveHeart else Color.White.copy(alpha = 0.86f),
                    glowColor = if (track.isFavorite) SoftNeumorphicTokens.ActiveHeart.copy(alpha = 0.45f) else null,
                    popOnActivate = true,
                )
                if (onToggleFloatingLyrics != null) {
                    TactileTopBarButton(
                        icon = Icons.Default.PictureInPicture,
                        contentDescription = if (floatingLyricsEnabled) "关闭桌面歌词" else "开启桌面歌词",
                        onClick = onToggleFloatingLyrics,
                        tint = if (floatingLyricsEnabled) Color.White else Color.White.copy(alpha = 0.80f),
                        glowColor = if (floatingLyricsEnabled) Color.White.copy(alpha = 0.35f) else null,
                    )
                }
                Box {
                    TactileTopBarButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "播放选项",
                        onClick = { menuExpanded = true },
                        tint = Color.White.copy(alpha = 0.86f),
                    )
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (!track.id.startsWith("together:")) DropdownMenuItem(text = { Text("歌曲与来源") }, onClick = { menuExpanded = false; showTrackInfo = true })
                        DropdownMenuItem(text = { Text("音效调校 · ${playerState.audioEffectPreset.displayName}") }, onClick = { menuExpanded = false; showEffectDialogInMenu = true })
                        DropdownMenuItem(text = { Text("播放设置 · 音量与速度") }, onClick = { menuExpanded = false; onOpenPlaybackSettings() })
                        LocalMusicFeatures.current?.let { feature ->
                            DropdownMenuItem(text = { Text("与朋友一起听") }, onClick = {
                                menuExpanded = false; onDismiss(); feature.onNavigate(com.resonance.player.model.LibraryDestination.Together)
                            })
                            val vocalEnabled by feature.services.isVocalSuppressionEnabled.collectAsState()
                            DropdownMenuItem(text = { Text(if (vocalEnabled) "伴奏模式 · 已开启" else "伴奏模式 (人声消除)") }, onClick = { menuExpanded = false; feature.services.setVocalSuppression(!vocalEnabled) })
                            if (!track.id.startsWith("together:")) {
                                DropdownMenuItem(text = { Text("匹配专辑封面") }, onClick = { menuExpanded = false; feature.launch { feature.assistant?.matchCover(track); feature.onReload() } })
                                DropdownMenuItem(text = { Text("自定义封面") }, onClick = { menuExpanded = false; feature.launch { feature.services.chooseCoverImage()?.let { feature.assistant?.setCover(track, it); feature.onReload() } } })
                                DropdownMenuItem(text = { Text("生成同步歌词") }, onClick = { menuExpanded = false; feature.showGenerateLyrics = true })
                            }
                        }
                        if (onOpenSleepTimer != null) DropdownMenuItem(
                            text = { Text(when {
                                playerState.sleepTimerOption == com.resonance.player.model.SleepTimerOption.EndOfTrack -> "睡眠定时 · 播完本首停止"
                                playerState.sleepTimerRemainingSeconds != null -> "睡眠定时 · 剩余 ${playerState.sleepTimerRemainingSeconds / 60}:${(playerState.sleepTimerRemainingSeconds % 60).toString().padStart(2, '0')}"
                                else -> "睡眠定时器"
                            }) },
                            onClick = { menuExpanded = false; onOpenSleepTimer() },
                        )
                        if (onToggleFloatingLyrics != null) DropdownMenuItem(
                            text = { Text(if (floatingLyricsEnabled) "关闭桌面歌词" else "开启桌面歌词") },
                            onClick = { menuExpanded = false; onToggleFloatingLyrics() },
                        )
                        if (!track.id.startsWith("together:") && !track.sourceUri.isNullOrBlank() && onDeleteLocalTrack != null) DropdownMenuItem(
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
        val titleBlock: @Composable (Modifier) -> Unit = { titleModifier ->
            Column(
                modifier = titleModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "正在播放",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = Color.White.copy(alpha = 0.56f),
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = track.album.ifBlank { "本地曲目" },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.86f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            val symmetricInset = ((if (onToggleFloatingLyrics != null) 3 else 2) * 48).dp + 4.dp
            if (maxWidth - symmetricInset * 2 >= 116.dp) {
                TactileTopBarButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    contentDescription = "关闭正在播放",
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                titleBlock(Modifier.align(Alignment.Center).padding(horizontal = symmetricInset))
                Box(Modifier.align(Alignment.CenterEnd)) { rightActions() }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TactileTopBarButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        contentDescription = "关闭正在播放",
                        onClick = onDismiss,
                    )
                    titleBlock(Modifier.weight(1f).padding(horizontal = 8.dp))
                    rightActions()
                }
            }
        }
    }
}

/** Apple-inspired frosted capsule switcher with spring-gliding lens and 48dp touch targets. */
@Composable
private fun PaneSwitcher(selected: PlayerPane, onSelect: (PlayerPane) -> Unit) {
    val reduced = LocalReducedMotion.current
    val tabWidth = 58.dp
    val tabGap = 4.dp
    val indicatorOffset by animateDpAsState(
        targetValue = (tabWidth + tabGap) * selected.ordinal,
        animationSpec = if (reduced) tween(0) else spring(dampingRatio = 0.78f, stiffness = 400f),
        label = "paneSwitcherLens",
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .clip(ResonanceShapes.Capsule)
                .background(Color.White.copy(alpha = 0.09f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), ResonanceShapes.Capsule)
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = indicatorOffset + 2.dp)
                    .size(width = 54.dp, height = 38.dp)
                    .clip(ResonanceShapes.Capsule)
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(0.75.dp, Color.White.copy(alpha = 0.20f), ResonanceShapes.Capsule)
            )
            Row(
                Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(tabGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerPane.entries.forEach { entry ->
                    val isSelected = entry == selected
                    val icon = when (entry) {
                        PlayerPane.Cover -> Icons.Default.Album
                        PlayerPane.Lyrics -> Icons.Default.Subtitles
                        PlayerPane.Queue -> Icons.AutoMirrored.Filled.QueueMusic
                    }
                    val target = when (entry) {
                        PlayerPane.Lyrics -> Modifier.tutorialTarget(TutorialTarget.PlayerLyrics)
                        PlayerPane.Queue -> Modifier.tutorialTarget(TutorialTarget.PlayerQueue)
                        else -> Modifier
                    }
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected && !reduced) 1.07f else 1f,
                        animationSpec = if (reduced) tween(0) else ResonanceMotionTokens.AppleFluidSpring,
                        label = "paneIconScale",
                    )
                    Box(
                        Modifier
                            .size(width = tabWidth, height = 48.dp)
                            .then(target)
                            .selectable(isSelected, role = Role.Tab, onClick = { onSelect(entry) })
                            .semantics { contentDescription = entry.label },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            icon,
                            null,
                            Modifier.size(21.dp).graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                            tint = Color.White.copy(alpha = if (isSelected) 0.98f else 0.56f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaneContent(
    pane: PlayerPane,
    onPaneChange: (PlayerPane) -> Unit,
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
    onPlayNext: ((Track) -> Unit)? = null,
    onRemoveFromQueue: ((Track) -> Unit)? = null,
    onClearQueue: (() -> Unit)? = null,
    onMoveInQueueOrder: ((Int, Int) -> Unit)? = null,
) {
    val reducedMotion = LocalReducedMotion.current
    val keyboardNavigation = LocalInputModeManager.current.inputMode == InputMode.Keyboard

    ContinuousPageTransition(
        target = pane,
        modifier = modifier.fillMaxWidth().testTag("player-page-swipe").horizontalPageSwipe(
            index = pane.ordinal, count = PlayerPane.entries.size,
            enabled = LocalTutorial.current?.step == null,
            onSelect = { onPaneChange(PlayerPane.entries[it]) },
        ),
        direction = { from, to -> if (to.ordinal >= from.ordinal) 1 else -1 },
    ) { active ->
        when (active) {
            PlayerPane.Cover -> BoxWithConstraints(Modifier.fillMaxSize()) {
                // Allocate the preview from the space left after a useful cover, not the reverse.
                val previewHeight = (maxHeight - 220.dp).coerceAtLeast(50.dp * LocalDensity.current.fontScale)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (showArtwork) Arrangement.SpaceBetween else Arrangement.Center,
                ) {
                    if (showArtwork) {
                        ArtworkPane(track, playerState.isPlaying, Modifier.weight(1f))
                        Spacer(Modifier.height(6.dp))
                    }
                    HeroSyncedLyricsPreview(
                        track = track,
                        progress = playerState.progress,
                        nativePositionMs = playerState.positionMs,
                        lyricsState = lyricsState,
                        onOpenFullLyrics = onOpenFullLyrics,
                        coverPreview = showArtwork,
                        maximumPreviewHeight = if (showArtwork) previewHeight else null,
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
            PlayerPane.Queue -> QueuePane(
                queue = queue,
                currentTrackId = track.id,
                onTrackSelected = onTrackSelected,
                onPlayNext = onPlayNext,
                onRemoveFromQueue = onRemoveFromQueue,
                onClearQueue = onClearQueue,
                onMoveInQueueOrder = onMoveInQueueOrder,
            )
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
    coverPreview: Boolean = false,
    maximumPreviewHeight: androidx.compose.ui.unit.Dp? = null,
) {
    val stage = LocalLyricStage.current && !coverPreview
    val compactPreview = !stage && LocalDensity.current.fontScale > 1.2f
    val durationMs = durationTextToSeconds(track.durationText).coerceAtLeast(1) * 1_000L
    val positionMs = (nativePositionMs ?: (durationMs * progress.coerceIn(0f, 1f)).toLong())
    val lyricsInteraction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(interactionSource = lyricsInteraction, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onOpenFullLyrics)
            .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
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

                    SyncedLyricsPreview(
                        lyrics = lyrics,
                        activeIndex = activeIndex,
                        display = LocalMusicFeatures.current?.settings?.lyricDisplay
                            ?: com.resonance.player.model.LyricDisplay.Bilingual,
                        stage = stage,
                        compact = compactPreview,
                        maximumHeight = maximumPreviewHeight,
                    )
                } else {
                    val display = LocalMusicFeatures.current?.settings?.lyricDisplay ?: com.resonance.player.model.LyricDisplay.Bilingual
                    val firstLineText = lyrics.lines.firstOrNull()?.let { line ->
                        if (display == com.resonance.player.model.LyricDisplay.Translation) line.translation?.takeIf(String::isNotBlank) ?: line.text
                        else line.text
                    }
                    val notice = firstLineText ?: "纯文本歌词 · 点击浏览或生成同步歌词"
                    Text(
                        notice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ResonanceColors.TextPrimary,
                        maxLines = 2,
                        softWrap = true,
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
                    Text("暂无歌词 · 点击查找", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Dim)
                }
            }
            LyricsUiState.Idle -> {
                if (stage) {
                    Text(track.title, style = MaterialTheme.typography.headlineLarge,
                        color = ResonanceColors.TextPrimary, textAlign = TextAlign.Center,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(16.dp))
                }
                Text("点击查看歌词", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Dim)
            }
        }
    }
}

@Composable
private fun ArtworkPane(
    track: Track,
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val atmosphere = LocalAlbumAtmosphere.current
    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying || reducedMotion) 1f else 0.92f,
        animationSpec = if (reducedMotion) tween(0) else spring(dampingRatio = 0.70f, stiffness = 260f),
        label = "coverBreathScale",
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (isPlaying && !reducedMotion) 0.34f else 0.14f,
        animationSpec = if (reducedMotion) tween(0) else spring(dampingRatio = 0.85f, stiffness = 220f),
        label = "coverHaloAlpha",
    )

    BoxWithConstraints(
        modifier = modifier.then(playerDismissGestureModifier()),
        contentAlignment = Alignment.Center,
    ) {
        val artworkCorner = 18.dp
        val shape = RoundedCornerShape(artworkCorner)
        val artworkSize = minOf(maxWidth, maxHeight - 8.dp, 460.dp).coerceAtLeast(0.dp)
        Box(
            modifier = Modifier
                .size(artworkSize)
                .drawBehind {
                    val radius = size.maxDimension * 0.68f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                atmosphere.glow.copy(alpha = haloAlpha),
                                atmosphere.edge.copy(alpha = haloAlpha * 0.42f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.54f),
                            radius = radius,
                        ),
                        radius = radius,
                        center = Offset(size.width * 0.5f, size.height * 0.54f),
                    )
                }
                .testTag("player-artwork")
                .playerExpansionArtwork()
                .graphicsLayer {
                    scaleX = coverScale
                    scaleY = coverScale
                }
                .shadow(
                    elevation = if (isPlaying) 24.dp else 11.dp,
                    shape = shape,
                    spotColor = Color.Black.copy(alpha = 0.56f),
                    ambientColor = Color.Black.copy(alpha = 0.32f),
                )
                .clip(shape)
                .background(ResonanceColors.Soft),
        ) {
            AnimatedContent(
                targetState = track,
                contentKey = { it.id },
                transitionSpec = {
                    (if (reducedMotion) fadeIn(tween(90)) togetherWith fadeOut(tween(90))
                    else (fadeIn(tween(240)) + scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = 0.80f, stiffness = 320f)))
                        .togetherWith(fadeOut(tween(160)) + scaleOut(targetScale = 1.03f, animationSpec = tween(160)))) using null
                },
                label = "heroArtworkTransition",
            ) { artworkTrack ->
                AlbumArtwork(artworkTrack.artworkSeed, Modifier.fillMaxSize(), artworkCorner, artworkTrack.artworkPath, artworkTrack.sourceUri)
            }
            // Delicate crystal specular rim along the top edge
            Box(
                Modifier
                    .matchParentSize()
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.05f))
                        ),
                        shape = shape,
                    )
            )
        }
    }
}

private fun trackFormatBadge(track: Track): String? {
    val mime = track.mimeType.lowercase()
    val uri = track.sourceUri?.lowercase().orEmpty()
    return when {
        "flac" in mime || uri.endsWith(".flac") -> "FLAC"
        "wav" in mime || uri.endsWith(".wav") -> "WAV"
        "alac" in mime || uri.endsWith(".alac") -> "ALAC"
        "ape" in mime || uri.endsWith(".ape") -> "APE"
        "ogg" in mime || "opus" in mime || uri.endsWith(".ogg") || uri.endsWith(".opus") -> "OGG"
        "aac" in mime || "mp4" in mime || uri.endsWith(".m4a") || uri.endsWith(".aac") -> "AAC"
        !track.sourceUri.isNullOrBlank() && ("mpeg" in mime || uri.endsWith(".mp3")) -> "MP3"
        else -> null
    }
}

@Composable
private fun TrackIdentity(track: Track, compact: Boolean = false, singleLine: Boolean = false, statusText: String? = null) {
    val badge = remember(track.mimeType, track.sourceUri) { trackFormatBadge(track) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalAlignment = Alignment.Start) {
        Text(
            track.title,
            style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Start,
            maxLines = if (singleLine) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            color = ResonanceColors.TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                statusText ?: track.artist.ifBlank { "未知艺术家" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (statusText != null) ResonanceColors.TextPrimary else ResonanceColors.Muted,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (statusText == null && badge != null && !compact) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFeatureSettings = "tnum",
                        letterSpacing = 0.6.sp,
                    ),
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(0.75.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 1.5.dp),
                )
            }
        }
    }
}

@Composable
private fun PlaybackProgress(track: Track, progress: Float, onSeek: (Float) -> Unit, isPlaying: Boolean) {
    SoftRefinedPlaybackScrubber(
        progress = progress,
        durationText = track.durationText,
        onSeek = onSeek,
        formatPosition = ::formatPlaybackPosition,
        modifier = Modifier.padding(horizontal = 2.dp),
        isPlaying = isPlaying,
        flat = true,
    )
}

@Composable
private fun TactilePlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val reduced = LocalReducedMotion.current
    val desc = if (isLoading) "加载中" else if (isPlaying) "暂停" else "播放"
    Box(
        modifier = modifier
            .size(64.dp)
            .semantics { contentDescription = desc }
            .clickable(interactionSource = interaction, indication = ripple(bounded = false, radius = 34.dp), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(60.dp)
                .resonancePressable(interaction, pressedScale = 0.88f)
                .shadow(10.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.42f), ambientColor = Color.Black.copy(alpha = 0.24f))
                .clip(CircleShape)
                .background(Color(0xFFF5FAFA)),
            contentAlignment = Alignment.Center,
        ) {
            val ink = Color(0xFF0D171B)
            if (isLoading) {
                CircularProgressIndicator(
                    Modifier.size(26.dp).semantics { contentDescription = "加载中" },
                    color = ink,
                    strokeWidth = 2.5.dp,
                )
            } else {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        if (reduced) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        else (scaleIn(initialScale = 0.70f, animationSpec = spring(dampingRatio = 0.60f, stiffness = 480f)) +
                            fadeIn(tween(120))) togetherWith
                            (scaleOut(targetScale = 0.70f, animationSpec = tween(100)) + fadeOut(tween(90)))
                    },
                    label = "stagePlayPauseMorph",
                ) { playing ->
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        Modifier.size(32.dp).offset(x = if (playing) 0.dp else 1.5.dp),
                        tint = ink,
                    )
                }
            }
        }
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
    Box(
        modifier = modifier
            .size(54.dp)
            .semantics { this.contentDescription = contentDescription }
            .clickable(interactionSource = interaction, indication = ripple(bounded = false, radius = 27.dp), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            null,
            Modifier.size(32.dp).resonancePressable(interaction, pressedScale = 0.90f),
            tint = Color.White.copy(alpha = 0.94f),
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
    val fill by animateColorAsState(
        if (isActive) Color.White.copy(alpha = 0.18f) else Color.Transparent,
        tween(motionDuration(ResonanceMotionTokens.StateDuration)),
        label = "secondaryBtnFill",
    )
    Box(
        modifier = modifier
            .size(48.dp)
            .semantics {
                this.contentDescription = contentDescription
                if (isActive) stateDescription = "已开启"
            }
            .clickable(interactionSource = interaction, indication = ripple(bounded = false, radius = 24.dp), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .size(40.dp)
                .resonancePressable(interaction, pressedScale = 0.92f)
                .clip(RoundedCornerShape(12.dp))
                .background(fill),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, Modifier.size(21.dp), tint = Color.White.copy(alpha = if (isActive) 1f else 0.58f))
            if (isActive) {
                Spacer(Modifier.height(2.dp))
                Box(Modifier.size(3.5.dp).clip(CircleShape).background(Color.White))
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
            .padding(top = 6.dp, bottom = 8.dp)
            .padding(horizontal = 4.dp),
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
            isLoading = playerState.isLoading,
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PlayerVolume(volume: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 42.dp).padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.VolumeDown, null, Modifier.size(17.dp), tint = Color.White.copy(alpha = .58f))
        Slider(volume, onChange, Modifier.weight(1f).padding(horizontal = 10.dp).semantics { contentDescription = "播放音量" },
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White.copy(alpha = .85f), inactiveTrackColor = Color.White.copy(alpha = .20f)),
            thumb = { }, track = { state -> SliderDefaults.Track(state, Modifier.height(4.dp), thumbTrackGapSize = 0.dp, drawStopIndicator = null, colors = SliderDefaults.colors(activeTrackColor = Color.White.copy(alpha = .85f), inactiveTrackColor = Color.White.copy(alpha = .20f))) })
        Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(18.dp), tint = Color.White.copy(alpha = .58f))
    }
}

@Composable
private fun PlayerQuickPill(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    semanticLabel: String = label,
) {
    val interaction = remember { MutableInteractionSource() }
    val fill by animateColorAsState(
        if (active) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.09f),
        tween(motionDuration(ResonanceMotionTokens.StateDuration)),
        label = "quickPillFill",
    )
    val rim by animateColorAsState(
        if (active) Color.White.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.13f),
        tween(motionDuration(ResonanceMotionTokens.StateDuration)),
        label = "quickPillRim",
    )
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) { contentDescription = semanticLabel }
            .clickable(interactionSource = interaction, indication = ripple(bounded = false, radius = 28.dp), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .resonancePressable(interaction, pressedScale = 0.94f)
                .clip(ResonanceShapes.Capsule)
                .background(fill)
                .border(1.dp, rim, ResonanceShapes.Capsule)
                .padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(icon, null, Modifier.size(15.dp), tint = Color.White.copy(alpha = if (active) 0.98f else 0.78f))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = if (active) 0.98f else 0.84f),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun PlayerQuickActions(
    playerState: PlayerState,
    onSettings: () -> Unit,
    onEffects: () -> Unit,
    onSleep: (() -> Unit)?,
) {
    val speedActive = kotlin.math.abs(playerState.playbackSpeed - 1f) > 0.01f
    val effectActive = playerState.audioEffectPreset != com.resonance.player.model.AudioEffectPreset.Off
    val sleepActive = playerState.sleepTimerOption != com.resonance.player.model.SleepTimerOption.Off
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerQuickPill(
            icon = Icons.Default.Tune,
            label = if (speedActive) "音量 · ${playerState.playbackSpeed}x" else "音量",
            semanticLabel = "音量",
            active = speedActive,
            onClick = onSettings,
        )
        PlayerQuickPill(
            icon = Icons.Default.GraphicEq,
            label = if (effectActive) playerState.audioEffectPreset.displayName else "音效",
            semanticLabel = "音效",
            active = effectActive,
            onClick = onEffects,
        )
        if (onSleep != null) {
            PlayerQuickPill(
                icon = Icons.Default.Timer,
                label = "定时",
                semanticLabel = "定时",
                active = sleepActive,
                onClick = onSleep,
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
    audioEffectPreset: com.resonance.player.model.AudioEffectPreset = com.resonance.player.model.AudioEffectPreset.Off,
    onAudioEffectPresetChange: (com.resonance.player.model.AudioEffectPreset) -> Unit = {},
    compact: Boolean = false,
) {
    var showSpeedMenu by remember { mutableStateOf(false) }
    var previousVolume by remember { mutableFloatStateOf(1f) }
    var showEffectDialog by remember { mutableStateOf(false) }
    if (showEffectDialog) {
        AudioEffectDialog(
            currentPreset = audioEffectPreset,
            onPresetSelect = onAudioEffectPresetChange,
            onDismiss = { showEffectDialog = false }
        )
    }
    val split = compact || LocalDensity.current.fontScale > 1.2f
    val volumeControls: @Composable (Modifier) -> Unit = { modifier ->
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            TactileTopBarButton(
                icon = when {
                    volume <= 0.01f -> Icons.AutoMirrored.Filled.VolumeOff
                    volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                    else -> Icons.AutoMirrored.Filled.VolumeUp
                }, contentDescription = "音量调节与静音",
                onClick = {
                    if (volume > 0.01f) { previousVolume = volume; onVolumeChange(0f) }
                    else onVolumeChange(if (previousVolume > 0.05f) previousVolume else 1f)
                })
            Spacer(Modifier.width(8.dp))
            SoftRefinedVolumeSlider(volume, onVolumeChange, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text("${(volume.coerceIn(0f, 1f) * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = ResonanceColors.Muted, maxLines = 1, softWrap = false,
                modifier = Modifier.widthIn(min = 40.dp), textAlign = TextAlign.End)
        }
    }
    val options: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                TextButton(onClick = { showSpeedMenu = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Default.Speed, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${speed}x", maxLines = 1, softWrap = false)
                }
                DropdownMenu(showSpeedMenu, { showSpeedMenu = false }) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { choice ->
                        DropdownMenuItem(text = { Text("${choice}x 倍速") }, onClick = {
                            onSpeedChange(choice); showSpeedMenu = false
                        }, trailingIcon = if (choice == speed) {
                            { Icon(Icons.Default.Check, null, tint = ResonanceColors.Primary, modifier = Modifier.size(16.dp)) }
                        } else null)
                    }
                }
            }
            androidx.compose.material3.FilterChip(
                selected = audioEffectPreset != com.resonance.player.model.AudioEffectPreset.Off,
                onClick = { showEffectDialog = true },
                label = { Text(if (audioEffectPreset == com.resonance.player.model.AudioEffectPreset.Off) "音效" else audioEffectPreset.displayName, maxLines = 1, softWrap = false) },
                modifier = Modifier.heightIn(min = 48.dp)
            )
            androidx.compose.material3.FilterChip(selected = autoLoudnessEnabled, onClick = onToggleAutoLoudness,
                label = { Text("音量平衡", maxLines = 1, softWrap = false) }, modifier = Modifier.heightIn(min = 48.dp))
        }
    }
    if (split) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ResonanceColors.CanvasElevated.copy(alpha = 0.6f)).border(1.dp, ResonanceColors.Divider.copy(alpha = 0.5f), RoundedCornerShape(14.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        volumeControls(Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { options() }
    } else Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ResonanceColors.CanvasElevated.copy(alpha = 0.6f)).border(1.dp, ResonanceColors.Divider.copy(alpha = 0.5f), RoundedCornerShape(14.dp)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        volumeControls(Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        options()
    }
}

// ---------------------------------------------------------------------------
// 歌词（画廊阅读表面）
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
            .padding(8.dp)
    ) {
        when (state) {
            LyricsUiState.Idle, LyricsUiState.Loading -> LyricsStatusContent("正在查找歌词…", true, onRefresh, onRequestAi)
            is LyricsUiState.Unavailable -> LyricsStatusContent(state.message, false, onRefresh, onRequestAi)
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
private fun LyricsStatusContent(message: String, loading: Boolean, onRefresh: () -> Unit, onRequestAi: () -> Unit) {
    val feature = LocalMusicFeatures.current
    val generating = feature?.generatingTextLyrics == true || feature?.generatingLyrics == true
    var toolsExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("lyrics-status"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, if (toolsExpanded) Alignment.Top else Alignment.CenterVertically),
    ) {
        if (!toolsExpanded) {
            if (loading || generating) CircularProgressIndicator(Modifier.size(28.dp), color = ResonanceColors.Primary)
            else Icon(Icons.Default.GraphicEq, null, tint = ResonanceColors.Dimmer, modifier = Modifier.size(24.dp))
            Text(when {
                feature?.generatingTextLyrics == true -> "正在请求 AI 歌词文本…"
                feature?.generatingLyrics == true -> "正在识别人声与时间轴…"
                else -> message
            }, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = ResonanceColors.Muted)
            if (!loading && !generating) {
                androidx.compose.material3.FilledTonalButton(onClick = onRefresh) { Text("查找歌词") }
            }
            if (!feature?.textLyricsError.isNullOrBlank()) {
                Text(feature!!.textLyricsError, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("lyrics-text-error"))
            }
        }
        TextButton(onClick = { toolsExpanded = !toolsExpanded }, modifier = Modifier.testTag("lyric-tools")) {
            Text(if (toolsExpanded) "收起工具" else "歌词工具")
        }
        if (toolsExpanded) {
            if (feature != null) TranslationControls()
            else TextButton(onClick = onRequestAi) { Text("AI 生成歌词文本") }
        }
    }
}

@Composable
internal fun LyricsContent(
    track: Track,
    progress: Float,
    nativePositionMs: Long? = null,
    lyrics: Lyrics,
    onSeek: (Float) -> Unit,
    onAdjustOffset: ((Long) -> Unit)? = null,
    onEmbedLyrics: (() -> Unit)? = null,
) {
    val reducedMotion = LocalReducedMotion.current
    val stage = LocalLyricStage.current

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
    val listState = rememberLyricListState(track.id, lyrics.lines, activeIndex)
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var browsing by remember(listState) { mutableStateOf(false) }
    var userInputRevision by remember(listState) { mutableStateOf(0) }
    var followRequest by remember(listState) { mutableStateOf(0) }
    val browsingConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    browsing = true
                    userInputRevision++
                }
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(isDragged) {
        if (isDragged) browsing = true
    }
    // Time starts after the finger and fling stop. A paused song can still resume following.
    LaunchedEffect(browsing, isDragged, listState.isScrollInProgress, userInputRevision) {
        if (browsing && !isDragged && !listState.isScrollInProgress) {
            delay(5_000)
            browsing = false
        }
    }
    val display = LocalMusicFeatures.current?.settings?.lyricDisplay
        ?: com.resonance.player.model.LyricDisplay.Bilingual
    FollowLyric(
        listState, activeIndex.coerceAtLeast(0),
        enabled = lyrics.synchronized && !browsing && !isDragged,
        reducedMotion = reducedMotion,
        layoutKey = listOf(display, LocalDensity.current.fontScale, lyrics.lines),
        request = followRequest,
    )
    val canAdjust = lyrics.synchronized && onAdjustOffset != null
    var panel by remember(track.id, canAdjust) { mutableStateOf(LyricPanel.Reading) }
    val closePanel = { panel = LyricPanel.Reading }
    val startTiming = {
        browsing = false
        followRequest++
        panel = LyricPanel.Timing
    }

    if (panel == LyricPanel.Tools) {
        AlertDialog(
            onDismissRequest = closePanel,
            title = { Text("歌词工具") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (canAdjust) {
                        TextButton(onClick = startTiming) { Text("校准时间轴") }
                    }
                    TranslationControls(onOpenAnotherScreen = closePanel)
                }
            },
            confirmButton = { TextButton(onClick = closePanel) { Text("关闭") } },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (panel == LyricPanel.Timing && onAdjustOffset != null) {
            ResonanceBackHandler(onBack = closePanel)
            LyricTimingBar(lyrics.offsetMs, onAdjustOffset, onDone = closePanel)
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (lyrics.synchronized) "同步歌词 · ${lyrics.source}" else "纯文本歌词 · 可手动滚动",
                    style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                if (canAdjust) {
                    TextButton(onClick = startTiming) { Text("校准") }
                }
                OutlinedButton(
                    onClick = { panel = LyricPanel.Tools }, modifier = Modifier.testTag("lyric-tools"),
                    border = BorderStroke(1.dp, ResonanceColors.Divider),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Text("歌词工具", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            val centerPadding = if (lyrics.synchronized) maxHeight / 2 else 8.dp
            val edgeFade = Brush.verticalGradient(
                0.0f to Color.Transparent,
                0.10f to Color.Black,
                0.90f to Color.Black,
                1.0f to Color.Transparent,
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        if (stage && lyrics.synchronized) {
                            drawRect(brush = edgeFade, blendMode = BlendMode.DstIn)
                        }
                    }
                    .testTag("player-lyrics-list")
                    .nestedScroll(browsingConnection),
                contentPadding = PaddingValues(top = centerPadding, bottom = centerPadding, start = 4.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(lyrics.lines, key = { index, line -> "${line.timestampMs}-$index" }) { index, line ->
                    val isActive = index == activeIndex
                    LyricRow(
                        line = line,
                        active = isActive,
                        synchronized = lyrics.synchronized,
                        modifier = Modifier.testTag("lyric-line-$index").semantics { selected = isActive },
                        contextDistance = if (stage && lyrics.synchronized && !browsing && activeIndex >= 0) kotlin.math.abs(index - activeIndex) else 0,
                        onClick = line.timestampMs?.takeIf { lyrics.synchronized }?.let { timestamp ->
                            {
                                browsing = false
                                followRequest++
                                onSeek(((timestamp - lyrics.offsetMs).coerceAtLeast(0L).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f))
                            }
                        },
                    )
                }
            }

            if (browsing && lyrics.synchronized && activeIndex >= 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .clip(CircleShape)
                        .background(ResonanceColors.Primary)
                        .testTag("lyric-return-current")
                        .clickable(role = Role.Button) { browsing = false; followRequest++ }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "回到当前播放",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

private enum class LyricPanel { Reading, Tools, Timing }

@Composable
private fun LyricTimingBar(offsetMs: Long, onAdjust: (Long) -> Unit, onDone: () -> Unit) {
    val offset = if (offsetMs == 0L) "0.0 秒" else "%+.1f 秒".format(offsetMs / 1000.0)
    Row(
        Modifier.fillMaxWidth().background(ResonanceColors.Glass, ResonanceShapes.Capsule)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LyricTimingStep("延后", Modifier.weight(1f)) { onAdjust(-500L) }
        TextButton(
            onClick = { onAdjust(-offsetMs) }, enabled = offsetMs != 0L,
            modifier = Modifier.weight(1.3f),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(offset, style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    color = ResonanceColors.TextPrimary)
                Text("重置", style = MaterialTheme.typography.labelSmall)
            }
        }
        LyricTimingStep("提前", Modifier.weight(1f)) { onAdjust(500L) }
        IconButton(onClick = onDone) { Icon(Icons.Default.Check, contentDescription = "完成校准") }
    }
}

@Composable
private fun LyricTimingStep(label: String, modifier: Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier,
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text("0.5 秒", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LyricRow(line: LyricLine, active: Boolean, synchronized: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier, contextDistance: Int = 0) {
    val stage = LocalLyricStage.current
    val reduced = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val color by animateColorAsState(
        targetValue = if (stage) Color(0xFFF5FAFA).copy(alpha = if (active || !synchronized) 1f else 0.40f) else if (active) ResonanceColors.Notice else ResonanceColors.Muted,
        animationSpec = if (reduced) tween(0) else spring(dampingRatio = 0.88f, stiffness = 340f),
        label = "lyricColor",
    )
    // Apple Music GPU-only optical emphasis: scales cleanly on the draw layer with zero layout height reflow.
    val focusScale by animateFloatAsState(
        targetValue = if (!stage || !synchronized || reduced) 1f else if (active) 1.03f else 0.96f,
        animationSpec = if (reduced) tween(0) else ResonanceMotionTokens.AppleFluidSpring,
        label = "lyricFocusScale",
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
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(interaction)
            .lyricFocus(active)
            .padding(start = 16.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val display = LocalMusicFeatures.current?.settings?.lyricDisplay ?: com.resonance.player.model.LyricDisplay.Bilingual
        Column(
            Modifier
                .graphicsLayer {
                    scaleX = focusScale
                    scaleY = focusScale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .blur(if (stage && !reduced) minOf(contextDistance * 1.15f, 3.0f).dp else 0.dp)
        ) {
            Text(
                if (display == com.resonance.player.model.LyricDisplay.Translation) line.translation ?: line.text else line.text,
                style = if (stage) MaterialTheme.typography.headlineSmall.copy(fontSize = 27.sp, lineHeight = 37.sp, letterSpacing = (-0.3).sp)
                    else MaterialTheme.typography.titleLarge,
                fontWeight = if (stage) FontWeight.Bold else FontWeight.Medium,
                color = color,
            )
            if (display == com.resonance.player.model.LyricDisplay.Bilingual && !line.translation.isNullOrBlank()) {
                Text(
                    line.translation,
                    style = if (stage) MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp, lineHeight = 23.sp) else MaterialTheme.typography.bodySmall,
                    color = if (active) color.copy(alpha = if (stage) 0.84f else 1f) else color.copy(alpha = color.alpha * 0.80f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 播放队列（磨砂玻璃卡片容器）
// ---------------------------------------------------------------------------

@Composable
private fun QueuePane(
    queue: List<Track>,
    currentTrackId: String,
    onTrackSelected: (Track) -> Unit,
    onPlayNext: ((Track) -> Unit)? = null,
    onRemoveFromQueue: ((Track) -> Unit)? = null,
    onClearQueue: (() -> Unit)? = null,
    onMoveInQueueOrder: ((Int, Int) -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = ResonanceColors.Dimmer,
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("播放队列为空", color = ResonanceColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("在曲库中选择歌曲或点击「下一首播放」即可加入队列", color = ResonanceColors.Dim, style = MaterialTheme.typography.bodySmall)
                }
            }
            return@Box
        }
        val reducedMotion = LocalReducedMotion.current
        val queueListState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val currentIndex = remember(queue, currentTrackId) {
            queue.indexOfFirst { it.id == currentTrackId }
        }

        LaunchedEffect(currentTrackId) {
            if (currentIndex >= 0) {
                val isVisible = queueListState.layoutInfo.visibleItemsInfo.any { it.index == currentIndex }
                if (!isVisible) {
                    if (reducedMotion) queueListState.scrollToItem(currentIndex) else queueListState.animateScrollToItem(currentIndex)
                }
            }
        }

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "播放队列 · ${queue.size} 首歌曲",
                    style = MaterialTheme.typography.titleSmall,
                    color = ResonanceColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                if (currentIndex >= 0) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (reducedMotion) queueListState.scrollToItem(currentIndex)
                                else queueListState.animateScrollToItem(currentIndex)
                            }
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = "定位当前歌曲",
                            tint = ResonanceColors.Primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                if (queue.size > 1 && onClearQueue != null) {
                    IconButton(
                        onClick = onClearQueue,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "清空其他歌曲",
                            tint = ResonanceColors.Dim,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            HorizontalDivider(color = ResonanceColors.Divider, thickness = Dp.Hairline)
            Spacer(Modifier.height(4.dp))

            LazyColumn(
                state = queueListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                itemsIndexed(queue, key = { index, item -> "queue-${item.id}-$index" }, contentType = { _, _ -> "queue-track" }) { index, item ->
                    val current = item.id == currentTrackId
                    val interactionSource = remember { MutableInteractionSource() }
                    val itemBg by animateColorAsState(
                        targetValue = if (current) ResonanceColors.PrimarySoft else Color.Transparent,
                        animationSpec = tween(motionDuration(160)),
                        label = "queueItemBg",
                    )
                    val itemBorder by animateColorAsState(
                        targetValue = if (current) ResonanceColors.GlassBorderGlow.copy(alpha = 0.45f) else Color.Transparent,
                        animationSpec = tween(motionDuration(160)),
                        label = "queueItemBorder",
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(itemBg)
                            .border(1.dp, itemBorder, RoundedCornerShape(12.dp))
                            .clickable(interactionSource = interactionSource, indication = null) { onTrackSelected(item) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlbumArtwork(item.artworkSeed, Modifier.size(40.dp), 8.dp, item.artworkPath, item.sourceUri)
                        Spacer(Modifier.width(10.dp))
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
                            Spacer(Modifier.width(6.dp))
                        } else {
                            if (onPlayNext != null) {
                                IconButton(
                                    onClick = { onPlayNext(item) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = "置顶为下一首",
                                        tint = ResonanceColors.Dim,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            if (onRemoveFromQueue != null) {
                                IconButton(
                                    onClick = { onRemoveFromQueue(item) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "从队列移除",
                                        tint = ResonanceColors.Dimmer,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                        if (onMoveInQueueOrder != null && queue.size > 1) {
                            Column {
                                if (index > 0) {
                                    IconButton(
                                        onClick = { onMoveInQueueOrder(index, index - 1) },
                                        modifier = Modifier.size(18.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "上移",
                                            tint = ResonanceColors.Dimmer,
                                            modifier = Modifier.size(14.dp).graphicsLayer { rotationZ = 180f },
                                        )
                                    }
                                }
                                if (index < queue.lastIndex) {
                                    IconButton(
                                        onClick = { onMoveInQueueOrder(index, index + 1) },
                                        modifier = Modifier.size(18.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "下移",
                                            tint = ResonanceColors.Dimmer,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}
