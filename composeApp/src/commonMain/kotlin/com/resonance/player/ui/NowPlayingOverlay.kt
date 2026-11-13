package com.resonance.player.ui

import com.resonance.player.design.lyricFocus

import com.resonance.player.design.FrostedAlertDialog as AlertDialog

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
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

    CompositionLocalProvider(com.resonance.player.design.LocalResonanceColors provides com.resonance.player.design.ImmersivePlayerColors,
        LocalLyricStage provides true) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(
        primary = Color.White, onPrimary = Color(0xFF1C1C1E), secondary = Color.White,
        primaryContainer = Color.White.copy(alpha = .15f), onPrimaryContainer = Color.White,
        surface = Color(0xFF262126), onSurface = Color.White, onSurfaceVariant = Color(0xFFE4DDE0),
        background = Color(0xFF262126), onBackground = Color.White,
        surfaceContainerLowest = Color(0xFF0B0B0C), surfaceContainerLow = Color(0xFF1C1C1E),
        surfaceContainer = Color(0xFF2C2C2E), surfaceContainerHigh = Color(0xFF3A3A3C),
        surfaceContainerHighest = Color(0xFF444448),
    )) {
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
        contentColor = Color.White,
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
                FrostedBackdrop(track, Modifier.matchParentSize().hazeSource(glassState), immersive = true)
                val statusText = if (playerState.isBuffering) "正在缓冲…" else if (playerState.isPreparing) "正在准备…" else null
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
                        onOpenPlaybackSettings = { playbackSettings = true },
                        onAudioEffectPresetChange = onAudioEffectPresetChange,
                    )
                    if (wide) {
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(56.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(.46f).fillMaxHeight().padding(bottom = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                ArtworkPane(track, Modifier.weight(1f).fillMaxWidth())
                                Spacer(Modifier.height(if (short) 4.dp else 20.dp))
                                TrackIdentity(track, compact = short, statusText = statusText)
                                PlaybackProgress(track, playerState.progress, onSeek, playerState.isPlaying)
                                PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                                PlayerVolume(playerState.volume, onVolumeChange)
                                if (!short) PlayerQuickActions({ playbackSettings = true }, { audioEffects = true }, onOpenSleepTimer)
                            }
                            Column(Modifier.weight(.54f).fillMaxHeight()) {
                                CompositionLocalProvider(LocalLyricStage provides true) {
                                    PaneContent(effectivePane, { paneName = it.name }, track, playerState, lyricsState, queue,
                                        onSeek, onTrackSelected, onRefreshLyrics, onRequestAiLyrics,
                                        onOpenFullLyrics = { paneName = PlayerPane.Lyrics.name },
                                        onAdjustOffset = onAdjustLyricsOffset, onEmbedLyrics = onEmbedLyrics,
                                        showArtwork = false, modifier = Modifier.weight(1f),
                                        onPlayNext = onPlayNext, onRemoveFromQueue = onRemoveFromQueue,
                                        onClearQueue = onClearQueue, onMoveInQueueOrder = onMoveInQueueOrder)
                                }
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
                        Spacer(Modifier.height(if (short) 4.dp else 20.dp))
                        TrackIdentity(track, compact = short, singleLine = short, statusText = statusText)
                        PlaybackProgress(track, playerState.progress, onSeek, playerState.isPlaying)
                        PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                        if (!short) {
                            PlayerVolume(playerState.volume, onVolumeChange)
                            PlayerQuickActions({ playbackSettings = true }, { audioEffects = true }, onOpenSleepTimer)
                        }
                        PaneSwitcher(effectivePane, onSelect = { paneName = it.name })
                        Spacer(Modifier.height(4.dp))
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
) {
    SoftUtilityButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        size = 48.dp,
        isActive = glowColor != null,
        activeColor = tint ?: ResonanceColors.Primary,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp),
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
                    if (!track.id.startsWith("together:")) DropdownMenuItem(text = { Text("歌曲与来源") }, onClick = { menuExpanded = false; showTrackInfo = true })
                    DropdownMenuItem(text = { Text("音效调校 · ${playerState.audioEffectPreset.displayName}") }, onClick = { menuExpanded = false; showEffectDialogInMenu = true })
                    DropdownMenuItem(text = { Text("播放设置 · 音量与速度") }, onClick = { menuExpanded = false; onOpenPlaybackSettings() })
                    LocalMusicFeatures.current?.let { feature ->
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
}

/** One quiet segmented dock; tabs keep their native selection and keyboard semantics. */
@Composable
private fun PaneSwitcher(selected: PlayerPane, onSelect: (PlayerPane) -> Unit) {
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceEvenly) {
        PlayerPane.entries.forEach { entry ->
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
            Box(Modifier.size(48.dp).then(target).clip(RoundedCornerShape(12.dp))
                .background(if (entry == selected) Color.White.copy(alpha = .18f) else Color.Transparent)
                .selectable(entry == selected, role = Role.Tab, onClick = { onSelect(entry) })
                .semantics { contentDescription = entry.label }, contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(24.dp), tint = Color.White.copy(alpha = if (entry == selected) 1f else .65f))
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
                // Keep room for a wrapped lyric and translation before measuring the artwork.
                val minimumPreviewHeight = 112.dp * LocalDensity.current.fontScale
                val previewHeight = (maxHeight - 220.dp).coerceAtLeast(minimumPreviewHeight)
                    .coerceAtMost((maxHeight - 48.dp).coerceAtLeast(0.dp))
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (showArtwork) Arrangement.SpaceBetween else Arrangement.Center,
                ) {
                    if (showArtwork) {
                        ArtworkPane(track, Modifier.weight(1f))
                        Spacer(Modifier.height(8.dp))
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
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current

    BoxWithConstraints(
        modifier = modifier.then(playerDismissGestureModifier()),
        contentAlignment = Alignment.Center,
    ) {
        val artworkCorner = 14.dp
        val artworkSize = minOf(maxWidth, maxHeight - 12.dp, 520.dp).coerceAtLeast(0.dp)
        Box(
            modifier = Modifier
                .size(artworkSize)
                .testTag("player-artwork")
                .playerExpansionArtwork()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(artworkCorner),
                    spotColor = if (ResonanceColors.isDark) Color.Black.copy(alpha = 0.6f) else Color(0xFF29342F).copy(alpha = 0.18f),
                    ambientColor = if (ResonanceColors.isDark) Color.Black.copy(alpha = 0.35f) else Color(0xFF29342F).copy(alpha = 0.08f),
                )
                .clip(RoundedCornerShape(artworkCorner))

                .background(ResonanceColors.Soft),
        ) {
            AnimatedContent(
                targetState = track,
                contentKey = { it.id },
                transitionSpec = {
                    (if (reducedMotion) fadeIn(tween(90)) togetherWith fadeOut(tween(90))
                    else (fadeIn(tween(220)) + slideInVertically(tween(220, easing = ResonanceMotionTokens.ExitEasing)) { it / 40 })
                        .togetherWith(fadeOut(tween(160)))) using null
                },
                label = "heroArtworkTransition",
            ) { artworkTrack ->
                AlbumArtwork(artworkTrack.artworkSeed, Modifier.fillMaxSize(), artworkCorner, artworkTrack.artworkPath, artworkTrack.sourceUri)
            }
        }
    }
}

@Composable
private fun TrackIdentity(track: Track, compact: Boolean = false, singleLine: Boolean = false, statusText: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalAlignment = Alignment.Start) {
        Text(track.title, style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Start, maxLines = if (singleLine) 1 else 2, overflow = TextOverflow.Ellipsis,
            color = ResonanceColors.TextPrimary, fontWeight = FontWeight.Bold)
        Text(statusText ?: track.artist.ifBlank { "未知艺术家" }, style = MaterialTheme.typography.bodyMedium,
            color = if (statusText != null) ResonanceColors.Primary else ResonanceColors.Muted, textAlign = TextAlign.Start, maxLines = if (singleLine) 1 else 2,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlaybackProgress(track: Track, progress: Float, onSeek: (Float) -> Unit, isPlaying: Boolean) {
    SoftRefinedPlaybackScrubber(
        progress = progress,
        durationText = track.durationText,
        onSeek = onSeek,
        formatPosition = ::formatPlaybackPosition,
        modifier = Modifier.padding(horizontal = 4.dp),
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
    IconButton(onClick = onClick, modifier = modifier.size(64.dp)) {
        if (isLoading) CircularProgressIndicator(Modifier.size(32.dp).semantics { contentDescription = "加载中" }, color = Color.White, strokeWidth = 2.dp)
        else Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            if (isPlaying) "暂停" else "播放", Modifier.size(48.dp), tint = Color.White)
    }
}

@Composable
private fun TactileSkipButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier.size(56.dp)) {
        Icon(if (icon == Icons.Default.SkipNext) Icons.Default.FastForward else Icons.Default.FastRewind,
            contentDescription, Modifier.size(36.dp), tint = Color.White)
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
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)
        .background(if (isActive) Color.White.copy(alpha = .18f) else Color.Transparent, RoundedCornerShape(10.dp))) {
        Icon(icon, contentDescription, Modifier.size(23.dp), tint = Color.White)
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
            .padding(top = 8.dp, bottom = 12.dp)
            .padding(horizontal = 6.dp),
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
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.VolumeDown, null, Modifier.size(18.dp), tint = Color.White.copy(alpha = .7f))
        Slider(volume, onChange, Modifier.weight(1f).padding(horizontal = 12.dp).semantics { contentDescription = "播放音量" },
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White.copy(alpha = .8f), inactiveTrackColor = Color.White.copy(alpha = .22f)),
            thumb = { }, track = { state -> SliderDefaults.Track(state, Modifier.height(4.dp), thumbTrackGapSize = 0.dp, drawStopIndicator = null, colors = SliderDefaults.colors(activeTrackColor = Color.White.copy(alpha = .8f), inactiveTrackColor = Color.White.copy(alpha = .22f))) })
        Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(20.dp), tint = Color.White.copy(alpha = .7f))
    }
}

@Composable
private fun PlayerQuickActions(onSettings: () -> Unit, onEffects: () -> Unit, onSleep: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onSettings) { Text("音量", color = Color.White.copy(alpha = .8f)) }
        TextButton(onEffects) { Text("音效", color = Color.White.copy(alpha = .8f)) }
        if (onSleep != null) TextButton(onSleep) { Text("定时", color = Color.White.copy(alpha = .8f)) }
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
    val timeline = remember(lyrics.lines) { com.resonance.player.model.LyricTimeline(lyrics.lines) }
    val activeIndex = if (lyrics.synchronized) {
        timeline.activeIndex(positionMs)
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
    var toolsExpanded by remember(track.id) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (lyrics.synchronized) "同步歌词 · ${lyrics.source}" else "纯文本歌词 · 可手动滚动",
                style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            TextButton(onClick = { toolsExpanded = !toolsExpanded }, modifier = Modifier.testTag("lyric-tools")) {
                Text(if (toolsExpanded) "收起工具" else "歌词工具")
            }
        }
        if (toolsExpanded) LyricsToolsDialog(lyrics, activeIndex, onAdjustOffset) { toolsExpanded = false }
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            val centerPadding = if (lyrics.synchronized) maxHeight / 2 else 8.dp
            val short = maxHeight < 240.dp
            val compact = maxWidth < 480.dp || maxHeight < 300.dp
            val typography = MaterialTheme.typography
            val lyricStyle = when {
                !stage -> typography.titleLarge
                short -> typography.titleLarge.copy(fontSize = 18.sp, lineHeight = 22.sp)
                compact -> typography.headlineSmall.copy(fontSize = 22.sp, lineHeight = 28.sp)
                else -> typography.headlineSmall.copy(fontSize = 34.sp, lineHeight = 44.sp)
            }
            val translationStyle = when {
                !stage -> typography.bodySmall
                short -> typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp)
                compact -> typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp)
                else -> typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("player-lyrics-list").nestedScroll(browsingConnection),
                contentPadding = PaddingValues(top = centerPadding, bottom = centerPadding, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(lyrics.lines, key = { index, line -> "${line.timestampMs}-$index" }, contentType = { _, _ -> "lyric" }) { index, line ->
                    val isActive = index == activeIndex
                    LyricRow(
                        line = line,
                        active = isActive,
                        synchronized = lyrics.synchronized,
                        lyricStyle = lyricStyle,
                        translationStyle = translationStyle,
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
                        .padding(horizontal = 14.dp, vertical = 7.dp),
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

@Composable
private fun LyricRow(
    line: LyricLine,
    active: Boolean,
    synchronized: Boolean,
    lyricStyle: TextStyle,
    translationStyle: TextStyle,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    contextDistance: Int = 0,
) {
    val stage = LocalLyricStage.current
    val interactionSource = remember { MutableInteractionSource() }
    val color by animateColorAsState(
        targetValue = if (stage) Color.White.copy(alpha = if (active || !synchronized) 1f else .45f) else if (active) ResonanceColors.Notice else ResonanceColors.Muted,
        animationSpec = tween(motionDuration(160)),
        label = "lyricColor",
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
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val display = LocalMusicFeatures.current?.settings?.lyricDisplay ?: com.resonance.player.model.LyricDisplay.Bilingual
        Column(Modifier.blur(if (stage && !LocalReducedMotion.current) minOf(contextDistance * 1.5f, 4f).dp else 0.dp)) {
        Text(
            if (display == com.resonance.player.model.LyricDisplay.Translation) line.translation ?: line.text else line.text,
            style = lyricStyle,
            fontWeight = if (stage) FontWeight.Bold else FontWeight.Medium,
            color = color,
        )
        if (display == com.resonance.player.model.LyricDisplay.Bilingual && !line.translation.isNullOrBlank()) {
            Text(
                line.translation,
                style = translationStyle,
                color = if (active) color else color.copy(alpha = color.alpha * 0.85f),
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

/** Separate native window: a moving lyric can never draw over or consume a tools gesture. */
@Composable
internal fun LyricsToolsDialog(lyrics: Lyrics, activeIndex: Int, onAdjustOffset: ((Long) -> Unit)?, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).widthIn(max = 560.dp).fillMaxHeight(.85f),
            shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("歌词工具", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClose) { Text("关闭") }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).testTag("lyrics-tools-scroll"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    lyrics.lines.getOrNull(activeIndex)?.let { Text(it.text, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                    TranslationControls()
                    if (lyrics.synchronized && onAdjustOffset != null) {
                        Text("时间不准？校准时间轴", style = MaterialTheme.typography.titleMedium)
                        Text("整体提前／落后可校准；越唱偏差越大时请更换歌词或音源版本。", style = MaterialTheme.typography.bodySmall)
                        Text("当前偏移：%+.1f 秒".format(lyrics.offsetMs / 1000.0))
                        TextButton(onClick = { onAdjustOffset(-500L) }, modifier = Modifier.fillMaxWidth()) { Text("歌词提前了 · 延后 0.5 秒") }
                        TextButton(onClick = { onAdjustOffset(500L) }, modifier = Modifier.fillMaxWidth()) { Text("歌词落后了 · 提前 0.5 秒") }
                        TextButton(onClick = { onAdjustOffset(-lyrics.offsetMs) }, enabled = lyrics.offsetMs != 0L,
                            modifier = Modifier.fillMaxWidth()) { Text("重置时间偏移") }
                    }
                }
            }
        }
    }
}
