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
import androidx.compose.foundation.gestures.scrollBy
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
import com.resonance.player.design.ResonanceMotionTokens
import com.resonance.player.ui.common.SoftHeroPlayButton
import com.resonance.player.ui.common.SoftSkipButton
import com.resonance.player.ui.common.SoftUtilityButton
import com.resonance.player.ui.common.SoftRefinedPlaybackScrubber
import com.resonance.player.ui.common.SoftRefinedVolumeSlider
import com.resonance.player.ui.common.SoftNeumorphicTokens
import com.resonance.player.ui.common.softConvexDisc
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
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
    modifier: Modifier = Modifier,
) {
    val track = playerState.currentTrack ?: return
    val atmosphere = rememberAlbumAtmosphere(track.artworkSeed, track.artworkPath, track.sourceUri)
    var paneName by rememberSaveable { mutableStateOf(PlayerPane.Cover.name) }
    var playbackSettings by remember { mutableStateOf(false) }
    val pane = PlayerPane.entries.firstOrNull { it.name == paneName } ?: PlayerPane.Cover
    val tutorial = LocalTutorial.current
    LaunchedEffect(pane, tutorial?.step?.id) {
        if (pane == PlayerPane.Lyrics) tutorial?.event(TutorialCompletion.LyricsViewed)
        if (pane == PlayerPane.Queue) tutorial?.event(TutorialCompletion.QueueViewed)
    }

    ResonanceBackHandler(enabled = true, onBack = onDismiss)
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
        CompositionLocalProvider(LocalGlassState provides null, LocalAlbumAtmosphere provides atmosphere) {
            BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                val wide = maxWidth >= 720.dp || maxWidth > maxHeight * 1.35f
                val short = maxHeight < 560.dp || (LocalDensity.current.fontScale > 1.2f && maxHeight < 840.dp)
                NowPlayingBackdrop(track.artworkSeed, playerState.isPlaying && ambientMotionEnabled, Modifier.matchParentSize())
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
                    if (wide && !short) {
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(40.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(.43f).fillMaxHeight().padding(vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                ArtworkPane(track, Modifier.weight(1f), onDismiss)
                                Spacer(Modifier.height(20.dp))
                                TrackIdentity(track)
                                Spacer(Modifier.height(12.dp))
                            }
                            Column(Modifier.weight(.57f).fillMaxHeight().padding(vertical = 16.dp)) {
                                PaneSwitcher(pane, onSelect = { paneName = it.name })
                                CompositionLocalProvider(LocalLyricStage provides true) {
                                    PaneContent(pane, { paneName = it.name }, track, playerState, lyricsState, queue,
                                        onSeek, onTrackSelected, onRefreshLyrics, onRequestAiLyrics,
                                        onOpenFullLyrics = { paneName = PlayerPane.Lyrics.name },
                                        onAdjustOffset = onAdjustLyricsOffset, onEmbedLyrics = onEmbedLyrics,
                                        onDismiss = onDismiss, showArtwork = false, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        Column(Modifier.align(Alignment.CenterHorizontally).widthIn(max = 820.dp).fillMaxWidth()
                            .musicDock().padding(horizontal = 28.dp, vertical = 8.dp)) {
                            PlaybackProgress(track, playerState.progress, onSeek, playerState.isPlaying)
                            PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                        }
                        Spacer(Modifier.height(12.dp))
                    } else if (wide) {
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).fillMaxHeight()) {
                                PaneContent(pane, { paneName = it.name }, track, playerState, lyricsState, queue,
                                    onSeek, onTrackSelected, onRefreshLyrics, onRequestAiLyrics,
                                    onOpenFullLyrics = { paneName = PlayerPane.Lyrics.name },
                                    onAdjustOffset = onAdjustLyricsOffset, onEmbedLyrics = onEmbedLyrics,
                                    onDismiss = onDismiss, modifier = Modifier.weight(1f))
                                TrackIdentity(track, compact = true, singleLine = true)
                            }
                            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                                PaneSwitcher(pane, onSelect = { paneName = it.name })
                                PlaybackProgress(track, playerState.progress, onSeek, playerState.isPlaying)
                                PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                            }
                        }
                    } else {
                        PaneContent(
                            pane = pane,
                            onPaneChange = { paneName = it.name },
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
                        TrackIdentity(track, compact = short || pane != PlayerPane.Cover)
                        if (pane == PlayerPane.Cover) {
                            HeroSyncedLyricsPreview(track, playerState.progress, playerState.positionMs, lyricsState,
                                onOpenFullLyrics = { paneName = PlayerPane.Lyrics.name }, modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(Modifier.height(6.dp))
                        PlaybackProgress(track, playerState.progress, onSeek, playerState.isPlaying)
                        PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                        Spacer(Modifier.height(2.dp))
                        Spacer(Modifier.height(4.dp))
                        PaneSwitcher(pane, onSelect = { paneName = it.name })
                        Spacer(Modifier.height(4.dp))
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
        val stats = remember(track.id) { feature?.services?.getTrackListeningStats(track.id) }
        AlertDialog(onDismissRequest = { showTrackInfo = false }, title = { Text("歌曲与来源") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { TrackSourceDetails(track, state?.origins?.get(track.id), stats, playerState.audioEffectPreset) } },
            confirmButton = { TextButton(onClick = { showTrackInfo = false }) { Text("关闭") } })
    }
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
                    .background(ResonanceColors.DividerStrong)
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
}

/** One quiet segmented dock; tabs keep their native selection and keyboard semantics. */
@Composable
private fun PaneSwitcher(selected: PlayerPane, onSelect: (PlayerPane) -> Unit) {
    val position = animateFloatAsState(selected.ordinal.toFloat(),
        if (LocalReducedMotion.current || LocalInputModeManager.current.inputMode == InputMode.Keyboard) tween(0)
        else ResonanceMotionTokens.SelectionBounceSpec, label = "playerPaneIndicator")
    val face = ResonanceColors.Raised
    Row(Modifier.fillMaxWidth().selectableGroup().clip(RoundedCornerShape(20.dp))
        .background(ResonanceColors.TextPrimary.copy(alpha = .045f)).drawWithCache {
            val inset = 4.dp.toPx()
            val width = ((size.width - inset * 2) / PlayerPane.entries.size).coerceAtLeast(0f)
            onDrawBehind {
                drawRoundRect(face, topLeft = androidx.compose.ui.geometry.Offset(inset + width * position.value, inset),
                    size = androidx.compose.ui.geometry.Size(width, (size.height - inset * 2).coerceAtLeast(0f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()))
            }
        }.padding(4.dp)) {
        PlayerPane.entries.forEach { pane ->
            ResonanceSelectionTab(pane.label, pane == selected, { onSelect(pane) },
                icon = pane.icon, contained = true,
                modifier = Modifier.weight(1f).then(when (pane) {
                    PlayerPane.Lyrics -> Modifier.tutorialTarget(TutorialTarget.PlayerLyrics)
                    PlayerPane.Queue -> Modifier.tutorialTarget(TutorialTarget.PlayerQueue)
                    else -> Modifier
                }))
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
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
) {
    val reducedMotion = LocalReducedMotion.current
    val keyboardNavigation = LocalInputModeManager.current.inputMode == InputMode.Keyboard

    AnimatedContent(
        targetState = pane,
        modifier = modifier.fillMaxWidth().testTag("player-page-swipe").horizontalPageSwipe(
            index = pane.ordinal, count = PlayerPane.entries.size,
            enabled = LocalTutorial.current?.step == null,
            onSelect = { onPaneChange(PlayerPane.entries[it]) },
        ),
        transitionSpec = {
            if (keyboardNavigation) {
                androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
            } else if (reducedMotion) {
                fadeIn(tween(90)) togetherWith fadeOut(tween(90))
            } else {
                val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                (slideInHorizontally(ResonanceMotionTokens.PageSlideSpec) { it * dir } +
                    fadeIn(ResonanceMotionTokens.PageFadeInSpec)) togetherWith
                (slideOutHorizontally(ResonanceMotionTokens.PageSlideSpec) { -it * dir } +
                    fadeOut(ResonanceMotionTokens.PageFadeOutSpec))
            } using null
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
                        ArtworkPane(track, Modifier.weight(1f), onDismiss = onDismiss)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (!showArtwork) HeroSyncedLyricsPreview(
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
    val stage = LocalLyricStage.current
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
                        val display = LocalMusicFeatures.current?.settings?.lyricDisplay ?: com.resonance.player.model.LyricDisplay.Bilingual
                        val prevLine = if (targetIdx > 0) lyrics.lines.getOrNull(targetIdx - 1) else null
                        val currentLine = if (targetIdx >= 0) lyrics.lines.getOrNull(targetIdx) else null
                        val nextLine = if (targetIdx >= 0) lyrics.lines.getOrNull(targetIdx + 1) else lyrics.lines.firstOrNull()

                        fun linePrimaryText(line: LyricLine?): String? = when {
                            line == null -> null
                            display == com.resonance.player.model.LyricDisplay.Translation -> line.translation?.takeIf(String::isNotBlank) ?: line.text
                            else -> line.text
                        }

                        fun lineTranslationText(line: LyricLine?): String? = when {
                            line == null -> null
                            display == com.resonance.player.model.LyricDisplay.Bilingual && !line.translation.isNullOrBlank() -> line.translation
                            else -> null
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val prevText = linePrimaryText(prevLine)
                            if (prevText != null && !compactPreview) {
                                Text(
                                    prevText,
                                    style = if (stage) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodySmall,
                                    color = ResonanceColors.Dimmer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(if (stage) 24.dp else 6.dp))
                            }
                            val currentPrimary = if (targetIdx >= 0) (linePrimaryText(currentLine) ?: "…") else "前奏 · 等待人声"
                            Text(
                                currentPrimary,
                                style = if (stage) MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp, lineHeight = 48.sp)
                                    else if (compactPreview) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 30.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = ResonanceColors.TextPrimary,
                                maxLines = if (stage) 4 else 2,
                                softWrap = true,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                            val currentTrans = lineTranslationText(currentLine)
                            if (currentTrans != null) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    currentTrans,
                                    style = if (stage) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
                                    color = ResonanceColors.Muted,
                                    maxLines = 2,
                                    softWrap = true,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            val nextText = linePrimaryText(nextLine)
                            if (nextText != null && !compactPreview) {
                                Spacer(Modifier.height(if (stage) 24.dp else 6.dp))
                                Text(
                                    nextText,
                                    style = if (stage) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodySmall,
                                    color = ResonanceColors.Dim,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
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
                    Text("暂无歌词 · 点击查看或使用 AI 检索", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Dim)
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
    onDismiss: (() -> Unit)? = null,
) {
    val reducedMotion = LocalReducedMotion.current

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
        val artworkCorner = 10.dp
        val artworkSize = minOf(maxWidth - 48.dp, maxHeight - 48.dp, 352.dp).coerceAtLeast(0.dp)
        Box(
            modifier = Modifier
                .size(artworkSize)
                .testTag("player-artwork")
                .musicSleevePerspective()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(artworkCorner),
                    spotColor = if (ResonanceColors.isDark) Color.Black.copy(alpha = 0.6f) else Color(0xFF29342F).copy(alpha = 0.18f),
                    ambientColor = if (ResonanceColors.isDark) Color.Black.copy(alpha = 0.35f) else Color(0xFF29342F).copy(alpha = 0.08f),
                )
                .clip(RoundedCornerShape(artworkCorner))
                .border(1.dp, ResonanceColors.Divider.copy(alpha = if (ResonanceColors.isDark) 0.35f else 0.5f), RoundedCornerShape(artworkCorner))
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
private fun TrackIdentity(track: Track, compact: Boolean = false, singleLine: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(track.title, style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center, maxLines = if (singleLine) 1 else 2, overflow = TextOverflow.Ellipsis,
            color = ResonanceColors.TextPrimary, fontWeight = FontWeight.Bold)
        Text(track.artist.ifBlank { "未知艺术家" }, style = MaterialTheme.typography.bodyMedium,
            color = ResonanceColors.Muted, textAlign = TextAlign.Center, maxLines = if (singleLine) 1 else 2,
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
            .clip(RoundedCornerShape(8.dp))
            .background(ResonanceColors.CanvasElevated)
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

    val density = LocalDensity.current
    val approxItemHeight = with(density) { 52.dp.roundToPx() }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            val mark = lastUserDragMark
            val allowAutoScroll = !isDragged && (mark == null || mark.elapsedNow().inWholeMilliseconds >= 3000L)
            if (allowAutoScroll) {
                val viewportHeight = listState.layoutInfo.viewportSize.height
                val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == activeIndex }
                if (itemInfo != null) {
                    val targetOffset = (viewportHeight - itemInfo.size) / 2
                    val diff = itemInfo.offset - targetOffset
                    if (reducedMotion) {
                        listState.scrollBy(diff.toFloat())
                    } else {
                        listState.animateScrollBy(
                            diff.toFloat(),
                            animationSpec = tween(durationMillis = 280, easing = ResonanceMotionTokens.ExitEasing)
                        )
                    }
                } else {
                    listState.scrollToItem(activeIndex)
                    val itemNow = listState.layoutInfo.visibleItemsInfo.find { it.index == activeIndex }
                    val currentOffset = itemNow?.offset ?: 0
                    val currentSize = itemNow?.size ?: approxItemHeight
                    val realTarget = (viewportHeight - currentSize) / 2
                    val diff = currentOffset - realTarget
                    if (diff != 0) {
                        listState.scrollBy(diff.toFloat())
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TranslationControls()
        Text(if (lyrics.synchronized) "同步歌词 · ${lyrics.source}" else "纯文本歌词 · 可手动滚动，或生成同步歌词", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted, modifier = Modifier.padding(horizontal = 8.dp))
        if (lyrics.synchronized && onAdjustOffset != null) {
            var calibrating by remember(track.id) { mutableStateOf(false) }
            TextButton(onClick = { calibrating = !calibrating }) {
                Text(if (calibrating) "收起校准" else "时间不准？校准时间轴")
            }
            if (calibrating) {
                Text("整体提前／落后可校准；越唱偏差越大时请更换歌词或音源版本。",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { onAdjustOffset(-500L) }) { Text("歌词提前了") }
                    TextButton(onClick = { onAdjustOffset(-lyrics.offsetMs) }, enabled = lyrics.offsetMs != 0L) {
                        Text(if (lyrics.offsetMs == 0L) "正常" else "%+.1fs · 重置".format(lyrics.offsetMs / 1000.0))
                    }
                    TextButton(onClick = { onAdjustOffset(500L) }) { Text("歌词落后了") }
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
    val stage = LocalLyricStage.current
    val interactionSource = remember { MutableInteractionSource() }
    val color by animateColorAsState(
        targetValue = if (active) ResonanceColors.TextPrimary else ResonanceColors.Muted,
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
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(interaction)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val display = LocalMusicFeatures.current?.settings?.lyricDisplay ?: com.resonance.player.model.LyricDisplay.Bilingual
        Column {
        Text(
            if (display == com.resonance.player.model.LyricDisplay.Translation) line.translation ?: line.text else line.text,
            style = if (stage) MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 38.sp)
                else if (active) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = color,
        )
        if (display == com.resonance.player.model.LyricDisplay.Bilingual && !line.translation.isNullOrBlank()) {
            Text(line.translation, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp))
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
            .clip(RoundedCornerShape(8.dp))
            .background(ResonanceColors.CanvasElevated)
            .padding(8.dp)
    ) {
        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("播放队列为空", color = ResonanceColors.Dim, style = MaterialTheme.typography.bodyMedium)
            }
            return@Box
        }
        val reducedMotion = LocalReducedMotion.current
        val queueListState = rememberLazyListState()
        LaunchedEffect(currentTrackId) {
            val idx = queue.indexOfFirst { it.id == currentTrackId }
            if (idx >= 0) {
                val isVisible = queueListState.layoutInfo.visibleItemsInfo.any { it.index == idx }
                if (!isVisible) {
                    if (reducedMotion) queueListState.scrollToItem(idx) else queueListState.animateScrollToItem(idx)
                }
            }
        }
        LazyColumn(
            state = queueListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            itemsIndexed(queue, key = { index, item -> "queue-${item.id}-$index" }, contentType = { _, _ -> "queue-track" }) { _, item ->
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
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArtwork(item.artworkSeed, Modifier.size(42.dp), 8.dp, item.artworkPath, item.sourceUri)
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
