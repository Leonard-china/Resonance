package com.resonance.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.resonance.player.design.ResonanceMotionTokens
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.resonance.player.model.DeepSeekConfig
import com.resonance.player.model.DeepSeekTestResult
import com.resonance.player.model.TrackFilterOption
import com.resonance.player.model.TrackSortOption
import com.resonance.player.platform.ResonanceBackHandler
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.LocalReducedMotion
import com.resonance.player.design.LocalAppForeground
import com.resonance.player.design.LocalAppearance
import com.resonance.player.design.LocalGlassState
import com.resonance.player.design.resonanceSpring
import com.resonance.player.design.motionDuration
import dev.chrisbanes.haze.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import com.resonance.player.design.ResonanceShapes
import com.resonance.player.design.resonanceGlass
import com.resonance.player.design.resonancePressable
import com.resonance.player.ui.common.FluidAmbientCanvas
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.sin
import com.resonance.player.model.APP_VERSION
import com.resonance.player.model.LibraryDestination
import com.resonance.player.model.LyricsUiState
import com.resonance.player.model.PlayerState
import com.resonance.player.model.Playlist
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.ThemeMode
import com.resonance.player.model.Track
import com.resonance.player.platform.decodeArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class WindowClass { Compact, Medium, Expanded }
private enum class LibraryTab(val label: String) { Tracks("歌曲"), Playlists("歌单"), Albums("专辑"), Artists("歌手") }

private sealed interface LibraryDetail {
    data object Favorites : LibraryDetail
    data class PlaylistDetail(val playlistId: String) : LibraryDetail
    data class AlbumDetail(val album: String) : LibraryDetail
    data class ArtistDetail(val artist: String) : LibraryDetail
}

private data class DestinationItem(
    val destination: LibraryDestination,
    val label: String,
    val icon: ImageVector,
)

private val destinationItems = listOf(
    DestinationItem(LibraryDestination.Library, "音乐库", Icons.Default.Home),
    DestinationItem(LibraryDestination.Discover, "搜索", Icons.Default.Search),
    DestinationItem(LibraryDestination.Profile, "工具", Icons.Default.Settings),
)

private const val MotionQuick = 160
private const val MotionStandard = 280

@Composable
fun LibraryShell(
    destination: LibraryDestination,
    onDestinationChange: (LibraryDestination) -> Unit,
    playlists: List<Playlist>,
    libraryTracks: List<Track>,
    playerState: PlayerState,
    playbackQueue: List<Track>,
    lyricsState: LyricsUiState,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSeek: (Float) -> Unit,
    onTrackSelected: (Track, List<Track>?) -> Unit = { _, _ -> },
    onToggleShufflePlay: ((List<Track>) -> Unit)? = null,
    selectedPlaylistId: String?,
    userPlaylists: List<Playlist>,
    onPlaylistSelected: (String) -> Unit,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onRefreshLyrics: () -> Unit,
    onRequestAiLyrics: () -> Unit = onRefreshLyrics,
    libraryLocation: String,
    onCreatePlaylist: () -> Unit,
    onImport: (Boolean) -> Unit,
    onImportPlaylist: () -> Unit,
    onExportSync: () -> Unit,
    onImportSync: () -> Unit,
    onStartLanSync: () -> Unit,
    lanQrPath: String?,
    onExitPreview: () -> Unit,
    previewMode: Boolean,
    themeMode: ThemeMode = ThemeMode.Dark,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    deepSeekConfig: DeepSeekConfig = DeepSeekConfig(),
    onSaveDeepSeekConfig: (DeepSeekConfig) -> Unit = {},
    onTestDeepSeek: suspend (DeepSeekConfig) -> DeepSeekTestResult = { DeepSeekTestResult(false, "") },
    customLyricsFolder: String? = null,
    onSaveCustomLyricsFolder: (String?) -> Unit = {},
    onOpenBatchEnrich: (List<Track>) -> Unit = {},
    onBatchDelete: (List<Track>) -> Unit = {},
    onBatchAddToPlaylist: (List<Track>, String) -> Unit = { _, _ -> },
    onVolumeChange: (Float) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onAdjustLyricsOffset: (Long) -> Unit = {},
    onEmbedLyrics: (Track) -> Unit = {},
    onToggleFloatingLyrics: (() -> Unit)? = null,
    floatingLyricsEnabled: Boolean = false,
    onEditTrack: (track: Track, newTitle: String, newArtist: String, newAlbum: String, embedLyrics: Boolean) -> Unit = { _, _, _, _, _ -> },
    message: String?,
    operationInProgress: Boolean,
) {
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var trackPendingEdit by remember { mutableStateOf<Track?>(null) }

    val handleDestination: (LibraryDestination) -> Unit = { target ->
        if (target == LibraryDestination.Playing && playerState.currentTrack != null) {
            showNowPlaying = true
        } else {
            onDestinationChange(target)
        }
    }

    if (destination in listOf(LibraryDestination.Import, LibraryDestination.Sync, LibraryDestination.Settings)) {
        ResonanceBackHandler(enabled = true) { onDestinationChange(LibraryDestination.Profile) }
    } else if (destination != LibraryDestination.Library) {
        ResonanceBackHandler(enabled = true) { onDestinationChange(LibraryDestination.Library) }
    }

    val glassState = rememberHazeState()
    CompositionLocalProvider(LocalGlassState provides glassState) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ResonanceColors.Canvas)) {
        FluidAmbientCanvas(
            modifier = Modifier.matchParentSize().hazeSource(glassState),
            seed = playerState.currentTrack?.artworkSeed ?: 0,
            isPlaying = playerState.isPlaying,
            intensity = 0.85f,
        )

        val windowClass = when {
            maxWidth < 720.dp -> WindowClass.Compact
            maxWidth < 1100.dp -> WindowClass.Medium
            else -> WindowClass.Expanded
        }

        val sharedContent: @Composable (Boolean) -> Unit = { compact ->
            DestinationContent(
                destination = destination,
                onDestinationChange = handleDestination,
                playlists = playlists,
                libraryTracks = libraryTracks,
                playerState = playerState,
                onTrackSelected = onTrackSelected,
                onToggleShuffle = onToggleShuffle,
                onToggleShufflePlay = onToggleShufflePlay,
                selectedPlaylistId = selectedPlaylistId,
                userPlaylists = userPlaylists,
                onPlaylistSelected = onPlaylistSelected,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onRenamePlaylist = onRenamePlaylist,
                onDeletePlaylist = onDeletePlaylist,
                onDeleteLocalTrack = onDeleteLocalTrack,
                onToggleFavorite = onToggleFavorite,
                libraryLocation = libraryLocation,
                onCreatePlaylist = onCreatePlaylist,
                onImport = onImport,
                onImportPlaylist = onImportPlaylist,
                onExportSync = onExportSync,
                onImportSync = onImportSync,
                onStartLanSync = onStartLanSync,
                lanQrPath = lanQrPath,
                onExitPreview = onExitPreview,
                previewMode = previewMode,
                compact = compact,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                deepSeekConfig = deepSeekConfig,
                onSaveDeepSeekConfig = onSaveDeepSeekConfig,
                onTestDeepSeek = onTestDeepSeek,
                customLyricsFolder = customLyricsFolder,
                onSaveCustomLyricsFolder = onSaveCustomLyricsFolder,
                onOpenBatchEnrich = onOpenBatchEnrich,
                onBatchDelete = onBatchDelete,
                onBatchAddToPlaylist = onBatchAddToPlaylist,
                message = message,
                operationInProgress = operationInProgress,
            )
        }

        when (windowClass) {
            WindowClass.Compact -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { sharedContent(true) }
                if (playerState.currentTrack != null) {
                    MiniPlayer(
                        playerState = playerState,
                        onTogglePlay = onTogglePlay,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onOpenNowPlaying = { showNowPlaying = true },
                        compact = true,
                    )
                }
                BottomNavigationBar(destination, handleDestination)
            }
            WindowClass.Medium -> Row(Modifier.fillMaxSize()) {
                SideNavigationRail(destination, handleDestination)
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Box(Modifier.weight(1f)) { sharedContent(false) }
                    if (playerState.currentTrack != null) {
                        MiniPlayer(
                            playerState = playerState,
                            onTogglePlay = onTogglePlay,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onOpenNowPlaying = { showNowPlaying = true },
                            compact = false,
                        )
                    }
                }
            }
            WindowClass.Expanded -> Row(Modifier.fillMaxSize()) {
                DesktopSidebar(
                    destination = destination,
                    onDestinationChange = handleDestination,
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                )
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Box(Modifier.weight(1f)) { sharedContent(false) }
                    if (playerState.currentTrack != null) {
                        MiniPlayer(
                            playerState = playerState,
                            onTogglePlay = onTogglePlay,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onOpenNowPlaying = { showNowPlaying = true },
                            compact = false,
                        )
                    }
                }
            }
        }

        var activeOverlayTrack by remember { mutableStateOf(playerState.currentTrack) }
        LaunchedEffect(playerState.currentTrack) {
            if (playerState.currentTrack != null) {
                activeOverlayTrack = playerState.currentTrack
            }
        }
        val currentTrackForOverlay = playerState.currentTrack ?: activeOverlayTrack
        val isNowPlayingVisible = showNowPlaying && currentTrackForOverlay != null
        val reducedMotion = LocalReducedMotion.current

        AnimatedVisibility(
            visible = isNowPlayingVisible,
            enter = if (reducedMotion) {
                fadeIn(ResonanceMotionTokens.PageFadeInSpec)
            } else {
                slideInVertically(
                    animationSpec = ResonanceMotionTokens.DetailSlideSpec,
                    initialOffsetY = { fullHeight -> fullHeight },
                ) + fadeIn(ResonanceMotionTokens.PageFadeInSpec)
            },
            exit = if (reducedMotion) {
                fadeOut(ResonanceMotionTokens.PageFadeOutSpec)
            } else {
                slideOutVertically(
                    animationSpec = ResonanceMotionTokens.DetailSlideSpec,
                    targetOffsetY = { fullHeight -> fullHeight },
                ) + fadeOut(ResonanceMotionTokens.PageFadeOutSpec)
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            if (currentTrackForOverlay != null) {
                NowPlayingOverlay(
                    playerState = if (playerState.currentTrack != null) playerState else playerState.copy(currentTrack = currentTrackForOverlay),
                    queue = playbackQueue,
                    lyricsState = lyricsState,
                    onDismiss = { showNowPlaying = false },
                    onTogglePlay = onTogglePlay,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    onSeek = onSeek,
                    onVolumeChange = onVolumeChange,
                    onSpeedChange = onSpeedChange,
                    onOpenSleepTimer = onOpenSleepTimer,
                    onAdjustLyricsOffset = onAdjustLyricsOffset,
                    onEmbedLyrics = { onEmbedLyrics(playerState.currentTrack ?: currentTrackForOverlay) },
                    onToggleFloatingLyrics = onToggleFloatingLyrics,
                    floatingLyricsEnabled = floatingLyricsEnabled,
                    onTrackSelected = { track -> onTrackSelected(track, playbackQueue) },
                    onToggleFavorite = onToggleFavorite,
                    onRefreshLyrics = onRefreshLyrics,
                    onRequestAiLyrics = onRequestAiLyrics,
                    onDeleteLocalTrack = { track ->
                        showNowPlaying = false
                        onDeleteLocalTrack(track)
                    },
                )
            }
        }

        trackPendingEdit?.let { track ->
            EditTrackDialog(
                track = track,
                hasLyrics = lyricsState is LyricsUiState.Ready,
                onDismiss = { trackPendingEdit = null },
                onConfirm = { newTitle, newArtist, newAlbum, embedLyrics ->
                    onEditTrack(track, newTitle, newArtist, newAlbum, embedLyrics)
                    trackPendingEdit = null
                },
            )
        }
    }
}
}

private fun tabDestinationOf(destination: LibraryDestination): LibraryDestination = when (destination) {
    LibraryDestination.Import, LibraryDestination.Sync, LibraryDestination.Settings -> LibraryDestination.Profile
    LibraryDestination.Playing -> LibraryDestination.Library
    else -> destination
}

private val profileSubPages = setOf(
    LibraryDestination.Import,
    LibraryDestination.Sync,
    LibraryDestination.Settings,
)

private fun mainTabIndexOf(destination: LibraryDestination): Int = when (tabDestinationOf(destination)) {
    LibraryDestination.Library -> 0
    LibraryDestination.Discover -> 1
    LibraryDestination.Profile -> 2
    else -> 0
}

private fun subPageIndexOf(destination: LibraryDestination): Int = when (destination) {
    LibraryDestination.Import -> 0
    LibraryDestination.Sync -> 1
    LibraryDestination.Settings -> 2
    else -> 0
}

@Composable
private fun BottomNavigationBar(
    destination: LibraryDestination,
    onDestinationChange: (LibraryDestination) -> Unit,
) {
    val activeTab = tabDestinationOf(destination)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .resonanceGlass(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                borderColors = listOf(ResonanceColors.GlassBorder, ResonanceColors.GlassBorderSubtle),
                shadowElevation = 5.dp,
            ),
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
        ) {
            destinationItems.forEach { item ->
                val selected = activeTab == item.destination
                val iconScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = resonanceSpring(),
                    label = "navIconScale",
                )
                NavigationBarItem(
                    selected = selected,
                    onClick = { onDestinationChange(item.destination) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ResonanceColors.Primary,
                        selectedTextColor = ResonanceColors.Primary,
                        unselectedIconColor = ResonanceColors.Dim,
                        unselectedTextColor = ResonanceColors.Dim,
                        indicatorColor = Color.Transparent,
                    ),
                    icon = {
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                                .then(
                                    if (selected) {
                                        Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ResonanceColors.PrimarySoft)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    } else Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(23.dp),
                            )
                        }
                    },
                    label = {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SideNavigationRail(
    destination: LibraryDestination,
    onDestinationChange: (LibraryDestination) -> Unit,
) {
    val activeTab = tabDestinationOf(destination)
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .resonanceGlass(
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                shadowElevation = 5.dp,
            ),
    ) {
        NavigationRail(
            containerColor = Color.Transparent,
            header = { BrandMark(modifier = Modifier.padding(vertical = 16.dp)) },
        ) {
            destinationItems.forEach { item ->
                val selected = activeTab == item.destination
                val scale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = resonanceSpring(),
                    label = "railIconScale",
                )
                NavigationRailItem(
                    selected = selected,
                    onClick = { onDestinationChange(item.destination) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = ResonanceColors.Primary,
                        selectedTextColor = ResonanceColors.Primary,
                        unselectedIconColor = ResonanceColors.Dim,
                        unselectedTextColor = ResonanceColors.Dim,
                        indicatorColor = Color.Transparent,
                    ),
                    icon = {
                        Box(
                            modifier = Modifier
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .then(
                                    if (selected) {
                                        Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ResonanceColors.PrimarySoft)
                                            .padding(8.dp)
                                    } else Modifier.padding(8.dp)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(23.dp))
                        }
                    },
                    label = {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun DesktopSidebar(
    destination: LibraryDestination,
    onDestinationChange: (LibraryDestination) -> Unit,
    themeMode: ThemeMode = ThemeMode.Dark,
    onThemeModeChange: (ThemeMode) -> Unit = {},
) {
    val isDark = ResonanceColors.isDark
    val activeTab = tabDestinationOf(destination)
    Column(
        modifier = Modifier
            .width(236.dp)
            .fillMaxHeight()
            .resonanceGlass(
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                borderColors = listOf(ResonanceColors.GlassBorder, ResonanceColors.GlassBorderSubtle),
                shadowElevation = 5.dp,
            )
            .padding(horizontal = 14.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark()
            Spacer(Modifier.width(10.dp))
            Text("Resonance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            val nextMode = when (themeMode) {
                ThemeMode.Dark -> ThemeMode.Light
                ThemeMode.Light -> ThemeMode.Dark
                ThemeMode.System -> if (isDark) ThemeMode.Light else ThemeMode.Dark
            }
            AccessibleIconButton("切换主题模式", onClick = { onThemeModeChange(nextMode) }) {
                Icon(
                    if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "切换主题模式",
                    tint = ResonanceColors.Muted,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        destinationItems.forEach { item ->
            SidebarDestination(
                item = item,
                selected = activeTab == item.destination,
                onClick = { onDestinationChange(item.destination) },
            )
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(
            "v$APP_VERSION · 本地音乐库",
            style = MaterialTheme.typography.labelMedium,
            color = ResonanceColors.Dimmer,
            modifier = Modifier.padding(start = 10.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun SidebarDestination(item: DestinationItem, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = resonanceSpring(),
        label = "sidebarScale",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .resonancePressable(interactionSource, pressedScale = 0.96f)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) {
                    Modifier
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(
                                    ResonanceColors.PrimarySoft,
                                    ResonanceColors.PrimarySoft.copy(alpha = 0.15f),
                                )
                            )
                        )
                        .border(1.dp, ResonanceColors.GlassBorderGlow.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                } else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Icon(
            item.icon,
            contentDescription = null,
            tint = if (selected) ResonanceColors.Primary else ResonanceColors.Dim,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) ResonanceColors.Primary else ResonanceColors.Muted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun DestinationContent(
    destination: LibraryDestination,
    onDestinationChange: (LibraryDestination) -> Unit,
    playlists: List<Playlist>,
    libraryTracks: List<Track>,
    playerState: PlayerState,
    onTrackSelected: (Track, List<Track>?) -> Unit,
    onToggleShuffle: () -> Unit = {},
    onToggleShufflePlay: ((List<Track>) -> Unit)? = null,
    selectedPlaylistId: String?,
    userPlaylists: List<Playlist>,
    onPlaylistSelected: (String) -> Unit,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    libraryLocation: String,
    onCreatePlaylist: () -> Unit,
    onImport: (Boolean) -> Unit,
    onImportPlaylist: () -> Unit,
    onExportSync: () -> Unit,
    onImportSync: () -> Unit,
    onStartLanSync: () -> Unit,
    lanQrPath: String?,
    onExitPreview: () -> Unit,
    previewMode: Boolean,
    compact: Boolean,
    themeMode: ThemeMode = ThemeMode.Dark,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    deepSeekConfig: DeepSeekConfig = DeepSeekConfig(),
    onSaveDeepSeekConfig: (DeepSeekConfig) -> Unit = {},
    onTestDeepSeek: suspend (DeepSeekConfig) -> DeepSeekTestResult = { DeepSeekTestResult(false, "") },
    customLyricsFolder: String? = null,
    onSaveCustomLyricsFolder: (String?) -> Unit = {},
    onOpenBatchEnrich: (List<Track>) -> Unit = {},
    onBatchDelete: (List<Track>) -> Unit = {},
    onBatchAddToPlaylist: (List<Track>, String) -> Unit = { _, _ -> },
    message: String?,
    operationInProgress: Boolean,
) {
    val reducedMotion = LocalReducedMotion.current
    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            if (reducedMotion) {
                (fadeIn(ResonanceMotionTokens.PageFadeInSpec)) togetherWith
                    (fadeOut(ResonanceMotionTokens.PageFadeOutSpec)) using SizeTransform(clip = false)
            } else if (initialState == LibraryDestination.Profile && targetState in profileSubPages) {
                // 进入工具二级页面：从右侧满屏推入，主页面向左视差退场
                (slideInHorizontally(ResonanceMotionTokens.DetailSlideSpec) { it } +
                    fadeIn(ResonanceMotionTokens.PageFadeInSpec)) togetherWith
                (slideOutHorizontally(ResonanceMotionTokens.DetailSlideSpec) { -it / 3 } +
                    fadeOut(ResonanceMotionTokens.PageFadeOutSpec)) using SizeTransform(clip = false)
            } else if (initialState in profileSubPages && targetState == LibraryDestination.Profile) {
                // 返回工具主页面：主页面从左侧视差归位，二级页面向右满屏滑出
                (slideInHorizontally(ResonanceMotionTokens.DetailSlideSpec) { -it / 3 } +
                    fadeIn(ResonanceMotionTokens.PageFadeInSpec)) togetherWith
                (slideOutHorizontally(ResonanceMotionTokens.DetailSlideSpec) { it } +
                    fadeOut(ResonanceMotionTokens.PageFadeOutSpec)) using SizeTransform(clip = false)
            } else if (initialState in profileSubPages && targetState in profileSubPages) {
                // 工具二级页面之间切换：满屏丝滑平移
                val dir = if (subPageIndexOf(targetState) >= subPageIndexOf(initialState)) 1 else -1
                (slideInHorizontally(ResonanceMotionTokens.PageSlideSpec) { it * dir } +
                    fadeIn(ResonanceMotionTokens.PageFadeInSpec)) togetherWith
                (slideOutHorizontally(ResonanceMotionTokens.PageSlideSpec) { -it * dir } +
                    fadeOut(ResonanceMotionTokens.PageFadeOutSpec)) using SizeTransform(clip = false)
            } else {
                // 同级主导航切换（音乐库 ⟷ 搜索 ⟷ 工具）：满屏丝滑平移
                val fromTab = mainTabIndexOf(initialState)
                val toTab = mainTabIndexOf(targetState)
                val dir = if (toTab >= fromTab) 1 else -1
                (slideInHorizontally(ResonanceMotionTokens.PageSlideSpec) { it * dir } +
                    fadeIn(ResonanceMotionTokens.PageFadeInSpec)) togetherWith
                (slideOutHorizontally(ResonanceMotionTokens.PageSlideSpec) { -it * dir } +
                    fadeOut(ResonanceMotionTokens.PageFadeOutSpec)) using SizeTransform(clip = false)
            }
        },
        label = "destinationContent",
    ) { activeDestination ->
        when (activeDestination) {
            LibraryDestination.Library -> LibraryScreen(
                playlists = playlists,
                libraryTracks = libraryTracks,
                playerState = playerState,
                onTrackSelected = onTrackSelected,
                onToggleShuffle = onToggleShuffle,
                onToggleShufflePlay = onToggleShufflePlay,
                selectedPlaylistId = selectedPlaylistId,
                userPlaylists = userPlaylists,
                onPlaylistSelected = onPlaylistSelected,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onRenamePlaylist = onRenamePlaylist,
                onDeletePlaylist = onDeletePlaylist,
                onDeleteLocalTrack = onDeleteLocalTrack,
                onToggleFavorite = onToggleFavorite,
                onCreatePlaylist = onCreatePlaylist,
                onImport = onImport,
                onOpenSearch = { onDestinationChange(LibraryDestination.Discover) },
                onExitPreview = onExitPreview,
                previewMode = previewMode,
                compact = compact,
                onOpenBatchEnrich = onOpenBatchEnrich,
                onBatchDelete = onBatchDelete,
                onBatchAddToPlaylist = onBatchAddToPlaylist,
                message = message,
                operationInProgress = operationInProgress,
            )
            LibraryDestination.Discover -> DiscoverScreen(
                libraryTracks = libraryTracks,
                playlists = playlists,
                playerState = playerState,
                onTrackSelected = onTrackSelected,
                userPlaylists = userPlaylists,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onDeleteLocalTrack = onDeleteLocalTrack,
                onToggleFavorite = onToggleFavorite,
                compact = compact,
                onOpenBatchEnrich = onOpenBatchEnrich,
                onBatchDelete = onBatchDelete,
                onBatchAddToPlaylist = onBatchAddToPlaylist,
                onCreatePlaylist = onCreatePlaylist,
            )
            LibraryDestination.Playing -> PlayingPlaceholderScreen(
                onOpenLibrary = { onDestinationChange(LibraryDestination.Library) },
            )
            LibraryDestination.Profile -> ProfileScreen(
                compact = compact,
                onOpenImport = { onDestinationChange(LibraryDestination.Import) },
                onOpenSync = { onDestinationChange(LibraryDestination.Sync) },
                onOpenSettings = { onDestinationChange(LibraryDestination.Settings) },
            )
            LibraryDestination.Import -> ImportScreen(
                compact = compact,
                onBack = { onDestinationChange(LibraryDestination.Profile) },
                onImport = { onImport(false) },
                onConvert = { onImport(true) },
                onImportPlaylist = onImportPlaylist,
                message = message,
                operationInProgress = operationInProgress,
            )
            LibraryDestination.Sync -> SyncScreen(
                compact = compact,
                onBack = { onDestinationChange(LibraryDestination.Profile) },
                onExportSync = onExportSync,
                onImportSync = onImportSync,
                onStartLanSync = onStartLanSync,
                lanQrPath = lanQrPath,
                message = message,
            )
            LibraryDestination.Settings -> SettingsScreen(
                compact = compact,
                onBack = { onDestinationChange(LibraryDestination.Profile) },
                libraryLocation = libraryLocation,
                onOpenImport = { onDestinationChange(LibraryDestination.Import) },
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                deepSeekConfig = deepSeekConfig,
                onSaveDeepSeekConfig = onSaveDeepSeekConfig,
                onTestDeepSeek = onTestDeepSeek,
                customLyricsFolder = customLyricsFolder,
                onSaveCustomLyricsFolder = onSaveCustomLyricsFolder,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 批量操作栏（多选模式）
// ---------------------------------------------------------------------------

@Composable
private fun MultiSelectToolbar(
    selectedCount: Int,
    currentViewTracks: List<Track>,
    selectedTrackIds: Set<String>,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onSelectMissingOnly: () -> Unit,
    onEnrichSelected: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDeleteSelected: () -> Unit,
    onExitMultiSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val missingInView = remember(currentViewTracks) { currentViewTracks.count { it.isMissingAnyMetadata } }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .resonanceGlass(shape = RoundedCornerShape(18.dp), shadowElevation = 10.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "已选 $selectedCount / ${currentViewTracks.size} 首",
                    style = MaterialTheme.typography.titleSmall,
                    color = ResonanceColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                androidx.compose.foundation.layout.FlowRow(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onSelectAll, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("全选", color = ResonanceColors.Primary, style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = onDeselectAll, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("清空", color = ResonanceColors.Muted, style = MaterialTheme.typography.labelMedium)
                    }
                    if (missingInView > 0) {
                        TextButton(onClick = onSelectMissingOnly, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("仅选缺失 ($missingInView)", color = ResonanceColors.Primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    IconButton(onClick = onExitMultiSelect, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "退出多选", tint = ResonanceColors.Muted, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onEnrichSelected,
                    enabled = selectedCount > 0,
                    shape = ResonanceShapes.Button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResonanceColors.Primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = ResonanceColors.Soft,
                        disabledContentColor = ResonanceColors.Dim,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.3f),
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("AI 智能补全", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = onAddToPlaylist,
                    enabled = selectedCount > 0,
                    shape = ResonanceShapes.Button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResonanceColors.Soft,
                        contentColor = ResonanceColors.TextPrimary,
                        disabledContainerColor = ResonanceColors.Soft,
                        disabledContentColor = ResonanceColors.Dim,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("加入歌单", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = onDeleteSelected,
                    enabled = selectedCount > 0,
                    shape = ResonanceShapes.Button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResonanceColors.PrimarySoft,
                        contentColor = ResonanceColors.Primary,
                        disabledContainerColor = ResonanceColors.Soft,
                        disabledContentColor = ResonanceColors.Dim,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(0.9f),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("删除", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 音乐库（首页）
// ---------------------------------------------------------------------------

@Composable
private fun LibraryScreen(
    playlists: List<Playlist>,
    libraryTracks: List<Track>,
    playerState: PlayerState,
    onTrackSelected: (Track, List<Track>?) -> Unit,
    onToggleShuffle: () -> Unit = {},
    onToggleShufflePlay: ((List<Track>) -> Unit)? = null,
    selectedPlaylistId: String?,
    userPlaylists: List<Playlist>,
    onPlaylistSelected: (String) -> Unit,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onCreatePlaylist: () -> Unit,
    onImport: (Boolean) -> Unit,
    onOpenSearch: () -> Unit,
    onExitPreview: () -> Unit,
    previewMode: Boolean,
    compact: Boolean,
    onOpenBatchEnrich: (List<Track>) -> Unit = {},
    onBatchDelete: (List<Track>) -> Unit = {},
    onBatchAddToPlaylist: (List<Track>, String) -> Unit = { _, _ -> },
    message: String?,
    operationInProgress: Boolean,
) {
    if (playlists.isEmpty() && libraryTracks.isEmpty()) {
        if (operationInProgress) LibraryLoading() else EmptyLibrary(onCreatePlaylist, { onImport(false) }, message)
        return
    }

    var tab by rememberSaveable { mutableStateOf(LibraryTab.Playlists.name) }
    val activeTab = LibraryTab.entries.firstOrNull { it.name == tab } ?: LibraryTab.Playlists
    var detail by remember { mutableStateOf<LibraryDetail?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var isMultiSelectMode by rememberSaveable { mutableStateOf(false) }
    var selectedTrackIds by remember { mutableStateOf(setOf<String>()) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showBatchAddToPlaylistDialog by remember { mutableStateOf(false) }

    val allKnownTracks = (libraryTracks + playlists.flatMap(Playlist::tracks)).distinctBy(Track::id)
    val favoriteTracks = allKnownTracks.filter(Track::isFavorite)
    val allLibraryMissingTracks = remember(allKnownTracks) { allKnownTracks.filter { it.isMissingAnyMetadata } }
    val pagePadding = if (compact) 16.dp else 32.dp

    val detailPlaylist = (detail as? LibraryDetail.PlaylistDetail)?.let { d ->
        playlists.firstOrNull { it.id == d.playlistId }
    }

    val currentViewTracks = remember(tab, activeTab, detail, allKnownTracks, libraryTracks, favoriteTracks, playlists) {
        if (detail != null) {
            when (val d = detail) {
                LibraryDetail.Favorites -> favoriteTracks
                is LibraryDetail.PlaylistDetail -> playlists.firstOrNull { it.id == d.playlistId }?.tracks.orEmpty()
                is LibraryDetail.AlbumDetail -> allKnownTracks.filter { it.album.ifBlank { "未知专辑" } == d.album }
                is LibraryDetail.ArtistDetail -> allKnownTracks.filter { it.artist.ifBlank { "未知艺术家" } == d.artist }
                null -> emptyList()
            }
        } else {
            when (activeTab) {
                LibraryTab.Tracks -> libraryTracks
                LibraryTab.Albums -> allKnownTracks
                LibraryTab.Artists -> allKnownTracks
                LibraryTab.Playlists -> emptyList()
            }
        }
    }

    val selectedTracks = remember(selectedTrackIds, allKnownTracks) {
        allKnownTracks.filter { it.id in selectedTrackIds }
    }

    fun toggleTrackSelect(trackId: String) {
        selectedTrackIds = if (trackId in selectedTrackIds) {
            selectedTrackIds - trackId
        } else {
            selectedTrackIds + trackId
        }
    }

    fun selectAllInView() {
        selectedTrackIds = selectedTrackIds + currentViewTracks.map(Track::id)
    }

    fun deselectAll() {
        selectedTrackIds = emptySet()
    }

    fun selectMissingInView() {
        selectedTrackIds = currentViewTracks.filter { it.isMissingAnyMetadata }.map(Track::id).toSet()
    }

    if (isMultiSelectMode) {
        ResonanceBackHandler(enabled = true) {
            isMultiSelectMode = false
            selectedTrackIds = emptySet()
        }
    } else if (detail != null) {
        ResonanceBackHandler(enabled = true) {
            detail = null
        }
    }

    Column(Modifier.fillMaxSize()) {
        // 顶部栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = pagePadding, end = pagePadding - 8.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (isMultiSelectMode) "批量选择" else if (activeTab == LibraryTab.Playlists && detail == null) "你的歌单" else "音乐库",
                style = MaterialTheme.typography.headlineLarge,
                color = ResonanceColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (previewMode) {
                TextButton(onClick = onExitPreview) { Text("清空预览", color = ResonanceColors.Muted) }
            }
            if (!isMultiSelectMode) {
                IconButton(onClick = {
                    isMultiSelectMode = true
                    selectedTrackIds = emptySet()
                }) {
                    Icon(Icons.Default.Checklist, contentDescription = "多选", tint = ResonanceColors.Muted)
                }
                IconButton(onClick = {
                    onOpenSearch()
                }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "搜索本地音乐",
                        tint = ResonanceColors.Muted,
                    )
                }
                AccessibleIconButton("新建歌单", onClick = onCreatePlaylist) {
                    Icon(Icons.Default.Add, contentDescription = "新建歌单", tint = ResonanceColors.Muted)
                }
            } else {
                TextButton(onClick = {
                    isMultiSelectMode = false
                    selectedTrackIds = emptySet()
                }) {
                    Text("退出多选", color = ResonanceColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (isMultiSelectMode) {
            MultiSelectToolbar(
                selectedCount = selectedTrackIds.size,
                currentViewTracks = currentViewTracks,
                selectedTrackIds = selectedTrackIds,
                onSelectAll = { selectAllInView() },
                onDeselectAll = { deselectAll() },
                onSelectMissingOnly = { selectMissingInView() },
                onEnrichSelected = {
                    if (selectedTracks.isNotEmpty()) {
                        onOpenBatchEnrich(selectedTracks)
                    }
                },
                onAddToPlaylist = {
                    if (selectedTracks.isNotEmpty()) {
                        showBatchAddToPlaylistDialog = true
                    }
                },
                onDeleteSelected = {
                    if (selectedTracks.isNotEmpty()) {
                        showBatchDeleteDialog = true
                    }
                },
                onExitMultiSelect = {
                    isMultiSelectMode = false
                    selectedTrackIds = emptySet()
                },
                modifier = Modifier.padding(horizontal = pagePadding),
            )
        }

        if (message != null) {
            Surface(
                color = ResonanceColors.PrimarySoft,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = pagePadding, vertical = 6.dp),
            ) {
                Text(
                    message,
                    color = ResonanceColors.Primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }
        }

        val reducedMotion = LocalReducedMotion.current
        AnimatedContent(
            targetState = detail,
            modifier = Modifier.fillMaxWidth().weight(1f),
            transitionSpec = {
                if (reducedMotion) {
                    (fadeIn(ResonanceMotionTokens.PageFadeInSpec)) togetherWith
                        (fadeOut(ResonanceMotionTokens.PageFadeOutSpec)) using SizeTransform(clip = false)
                } else if (targetState != null && initialState == null) {
                    // 进入详情（歌单 / 专辑 / 艺术家 / 收藏）：从右侧满屏推入，列表视差滑出
                    (slideInHorizontally(ResonanceMotionTokens.DetailSlideSpec) { it } +
                        fadeIn(ResonanceMotionTokens.PageFadeInSpec)) togetherWith
                    (slideOutHorizontally(ResonanceMotionTokens.DetailSlideSpec) { -it / 3 } +
                        fadeOut(ResonanceMotionTokens.PageFadeOutSpec)) using SizeTransform(clip = false)
                } else if (targetState == null && initialState != null) {
                    // 从详情返回列表：列表视差滑入，详情向右滑出
                    (slideInHorizontally(ResonanceMotionTokens.DetailSlideSpec) { -it / 3 } +
                        fadeIn(ResonanceMotionTokens.PageFadeInSpec)) togetherWith
                    (slideOutHorizontally(ResonanceMotionTokens.DetailSlideSpec) { it } +
                        fadeOut(ResonanceMotionTokens.PageFadeOutSpec)) using SizeTransform(clip = false)
                } else {
                    // 详情之间相互切换：满屏丝滑平移
                    (slideInHorizontally(ResonanceMotionTokens.PageSlideSpec) { it } +
                        fadeIn(ResonanceMotionTokens.PageFadeInSpec)) togetherWith
                    (slideOutHorizontally(ResonanceMotionTokens.PageSlideSpec) { -it } +
                        fadeOut(ResonanceMotionTokens.PageFadeOutSpec)) using SizeTransform(clip = false)
                }
            },
            label = "libraryDetailTransition",
        ) { activeDetail ->
            if (activeDetail != null) {
                // 详情视图（歌单 / 专辑 / 艺术家 / 收藏）
                val currentDetailPlaylist = (activeDetail as? LibraryDetail.PlaylistDetail)?.let { d ->
                    playlists.firstOrNull { it.id == d.playlistId }
                }
                LibraryDetailView(
                    detail = activeDetail,
                    playlists = playlists,
                    libraryTracks = libraryTracks,
                    favoriteTracks = favoriteTracks,
                    playerState = playerState,
                    onBack = { detail = null },
                    onTrackSelected = onTrackSelected,
                    userPlaylists = userPlaylists,
                    onPlaylistMembershipChange = onPlaylistMembershipChange,
                    onDeleteLocalTrack = onDeleteLocalTrack,
                    onToggleFavorite = onToggleFavorite,
                    onToggleShufflePlay = onToggleShufflePlay ?: { tracks ->
                        val playable = tracks.filter { it.sourceUri != null }
                        val pick = playable.takeIf { it.isNotEmpty() }?.random()
                        if (pick != null) {
                            if (!playerState.shuffleEnabled) {
                                onToggleShuffle()
                            }
                            onTrackSelected(pick, playable)
                        }
                    },
                    onRenamePlaylist = if (currentDetailPlaylist != null && !previewMode) { { showRenameDialog = true } } else null,
                    onDeletePlaylist = if (currentDetailPlaylist != null && !previewMode) { { showDeleteDialog = true } } else null,
                    isMultiSelectMode = isMultiSelectMode,
                    selectedTrackIds = selectedTrackIds,
                    onToggleTrackSelect = ::toggleTrackSelect,
                    pagePadding = pagePadding,
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    LibraryTabRow(
                        activeTab = activeTab,
                        onSelect = { tab = it.name },
                        modifier = Modifier.padding(horizontal = pagePadding),
                    )
                    if (activeTab == LibraryTab.Tracks && allLibraryMissingTracks.isNotEmpty()) {
                        TextButton(onClick = { onOpenBatchEnrich(allLibraryMissingTracks) }, modifier = Modifier.padding(horizontal = pagePadding)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("补全缺失信息 · ${allLibraryMissingTracks.size} 首")
                        }
                    }
                    AnimatedContent(
                        targetState = activeTab,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        transitionSpec = {
                            if (reducedMotion) {
                                (fadeIn(ResonanceMotionTokens.PageFadeInSpec)) togetherWith
                                    (fadeOut(ResonanceMotionTokens.PageFadeOutSpec)) using SizeTransform(clip = false)
                            } else {
                                val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                                (slideInHorizontally(ResonanceMotionTokens.PageSlideSpec) { it * dir } +
                                    fadeIn(ResonanceMotionTokens.PageFadeInSpec)) togetherWith
                                (slideOutHorizontally(ResonanceMotionTokens.PageSlideSpec) { -it * dir } +
                                    fadeOut(ResonanceMotionTokens.PageFadeOutSpec)) using SizeTransform(clip = false)
                            }
                        },
                        label = "libraryTabTransition",
                    ) { currentTab ->
                        when (currentTab) {
                            LibraryTab.Tracks -> TrackListTab(
                                tracks = libraryTracks,
                                favoriteCount = favoriteTracks.size,
                                playerState = playerState,
                                onTrackSelected = onTrackSelected,
                                onOpenFavorites = { detail = LibraryDetail.Favorites },
                                userPlaylists = userPlaylists,
                                onPlaylistMembershipChange = onPlaylistMembershipChange,
                                onDeleteLocalTrack = onDeleteLocalTrack,
                                onToggleFavorite = onToggleFavorite,
                                isMultiSelectMode = isMultiSelectMode,
                                selectedTrackIds = selectedTrackIds,
                                onToggleTrackSelect = ::toggleTrackSelect,
                                contentPadding = PaddingValues(start = pagePadding, end = pagePadding, bottom = 24.dp),
                            )
                            LibraryTab.Playlists -> PlaylistListTab(
                                playlists = playlists,
                                onOpenPlaylist = { playlist ->
                                    onPlaylistSelected(playlist.id)
                                    detail = LibraryDetail.PlaylistDetail(playlist.id)
                                },
                                onCreatePlaylist = onCreatePlaylist,
                                contentPadding = PaddingValues(start = pagePadding, end = pagePadding, bottom = 24.dp),
                            )
                            LibraryTab.Albums -> AlbumListTab(
                                tracks = allKnownTracks,
                                onOpenAlbum = { detail = LibraryDetail.AlbumDetail(it) },
                                contentPadding = PaddingValues(start = pagePadding, end = pagePadding, bottom = 24.dp),
                            )
                            LibraryTab.Artists -> ArtistListTab(
                                tracks = allKnownTracks,
                                onOpenArtist = { detail = LibraryDetail.ArtistDetail(it) },
                                contentPadding = PaddingValues(start = pagePadding, end = pagePadding, bottom = 24.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog && detailPlaylist != null) {
        RenamePlaylistDialog(
            currentName = detailPlaylist.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { name ->
                onRenamePlaylist(detailPlaylist.id, name)
                showRenameDialog = false
            },
        )
    }
    if (showDeleteDialog && detailPlaylist != null) {
        DeletePlaylistDialog(
            playlistName = detailPlaylist.name,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                onDeletePlaylist(detailPlaylist.id)
                showDeleteDialog = false
                detail = null
            },
        )
    }
    if (showBatchDeleteDialog && selectedTracks.isNotEmpty()) {
        BatchDeleteTracksDialog(
            count = selectedTracks.size,
            onDismiss = { showBatchDeleteDialog = false },
            onConfirm = {
                val toDelete = selectedTracks
                showBatchDeleteDialog = false
                isMultiSelectMode = false
                selectedTrackIds = emptySet()
                onBatchDelete(toDelete)
            },
        )
    }
    if (showBatchAddToPlaylistDialog && selectedTracks.isNotEmpty()) {
        BatchAddToPlaylistDialog(
            playlists = userPlaylists,
            trackCount = selectedTracks.size,
            onDismiss = { showBatchAddToPlaylistDialog = false },
            onSelectPlaylist = { playlistId ->
                val toAdd = selectedTracks
                showBatchAddToPlaylistDialog = false
                isMultiSelectMode = false
                selectedTrackIds = emptySet()
                onBatchAddToPlaylist(toAdd, playlistId)
            },
            onCreateNewPlaylist = {
                showBatchAddToPlaylistDialog = false
                onCreatePlaylist()
            },
        )
    }
}

@Composable
private fun LibraryTabRow(
    activeTab: LibraryTab,
    onSelect: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibraryTab.entries.forEach { tab ->
            val selected = tab == activeTab
            val interaction = remember { MutableInteractionSource() }
            val tabScale by animateFloatAsState(
                targetValue = if (selected) 1.0f else 0.97f,
                animationSpec = resonanceSpring(),
                label = "tabScale",
            )
            Box(
                modifier = Modifier
                    .graphicsLayer { scaleX = tabScale; scaleY = tabScale }
                    .resonancePressable(interaction, pressedScale = 0.94f)
                    .clip(ResonanceShapes.Capsule)
                    .then(
                        if (selected) {
                            Modifier
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(
                                            ResonanceColors.PrimarySoft,
                                            ResonanceColors.PrimarySoft.copy(alpha = 0.20f),
                                        )
                                    )
                                )
                                .border(1.dp, ResonanceColors.GlassBorderGlow.copy(alpha = 0.45f), ResonanceShapes.Capsule)
                        } else {
                            Modifier
                                .background(ResonanceColors.Soft.copy(alpha = 0.35f))
                                .border(1.dp, ResonanceColors.GlassBorderSubtle, ResonanceShapes.Capsule)
                        }
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Tab,
                    ) { onSelect(tab) }
                    .heightIn(min = 48.dp)
                    .semantics { this.selected = selected }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) ResonanceColors.Primary else ResonanceColors.Dim,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun TrackListTab(
    tracks: List<Track>,
    favoriteCount: Int,
    playerState: PlayerState,
    onTrackSelected: (Track, List<Track>?) -> Unit,
    onOpenFavorites: () -> Unit,
    userPlaylists: List<Playlist>,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    isMultiSelectMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleTrackSelect: (String) -> Unit = {},
    contentPadding: PaddingValues,
) {
    val trackListState = rememberLazyListState()
    val currentTrackId = playerState.currentTrack?.id
    LaunchedEffect(currentTrackId) {
        if (currentTrackId != null) {
            val idx = tracks.indexOfFirst { it.id == currentTrackId }
            if (idx >= 0) {
                val targetIndex = idx + 1
                val isVisible = trackListState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
                if (!isVisible) {
                    trackListState.animateScrollToItem(targetIndex)
                }
            }
        }
    }
    LazyColumn(state = trackListState, contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenFavorites)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(ResonanceShapes.ArtworkSmall)
                        .background(ResonanceColors.PrimarySoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = ResonanceColors.Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("我的收藏", style = MaterialTheme.typography.titleSmall, color = ResonanceColors.TextPrimary)
                    Text("$favoriteCount 首", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Dim)
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ResonanceColors.Dimmer,
                    modifier = Modifier.size(20.dp),
                )
            }
            HorizontalDivider(color = ResonanceColors.Divider, thickness = Dp.Hairline)
        }
        itemsIndexed(tracks, key = { index, track -> "tracks-${track.id}-$index" }) { _, track ->
            TrackRow(
                track = track,
                selected = playerState.currentTrack?.id == track.id,
                isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                onClick = { onTrackSelected(track, tracks) },
                userPlaylists = userPlaylists,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onDeleteLocalTrack = onDeleteLocalTrack,
                onToggleFavorite = onToggleFavorite,
                isMultiSelectMode = isMultiSelectMode,
                isMultiSelected = selectedTrackIds.contains(track.id),
                onToggleMultiSelect = { onToggleTrackSelect(track.id) },
                modifier = if (LocalReducedMotion.current) Modifier else Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun AlbumListTab(
    tracks: List<Track>,
    onOpenAlbum: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val albums = tracks
        .groupBy { it.album.ifBlank { "未知专辑" } }
        .toList()
        .sortedBy { (name, _) -> name }
    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(albums, key = { index, album -> "album-${album.first}-$index" }) { _, (album, albumTracks) ->
            val first = albumTracks.first()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAlbum(album) }
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArtwork(
                    seed = first.artworkSeed,
                    modifier = Modifier.size(48.dp),
                    cornerRadius = 8.dp,
                    artworkPath = first.artworkPath,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        album,
                        style = MaterialTheme.typography.titleSmall,
                        color = ResonanceColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${first.artist.ifBlank { "未知艺术家" }} · ${albumTracks.size} 首",
                        style = MaterialTheme.typography.bodySmall,
                        color = ResonanceColors.Dim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ResonanceColors.Dimmer,
                    modifier = Modifier.size(20.dp),
                )
            }
            HorizontalDivider(color = ResonanceColors.Divider, thickness = Dp.Hairline, modifier = Modifier.padding(start = 60.dp))
        }
    }
}

@Composable
private fun ArtistListTab(
    tracks: List<Track>,
    onOpenArtist: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val artists = tracks
        .groupBy { it.artist.ifBlank { "未知艺术家" } }
        .toList()
        .sortedBy { (name, _) -> name }
    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(artists, key = { index, artist -> "artist-${artist.first}-$index" }) { _, (artist, artistTracks) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenArtist(artist) }
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ResonanceColors.Soft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = ResonanceColors.Dim, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        artist,
                        style = MaterialTheme.typography.titleSmall,
                        color = ResonanceColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${artistTracks.size} 首",
                        style = MaterialTheme.typography.bodySmall,
                        color = ResonanceColors.Dim,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ResonanceColors.Dimmer,
                    modifier = Modifier.size(20.dp),
                )
            }
            HorizontalDivider(color = ResonanceColors.Divider, thickness = Dp.Hairline, modifier = Modifier.padding(start = 60.dp))
        }
    }
}

@Composable
private fun LibraryDetailView(
    detail: LibraryDetail,
    playlists: List<Playlist>,
    libraryTracks: List<Track>,
    favoriteTracks: List<Track>,
    playerState: PlayerState,
    onBack: () -> Unit,
    onTrackSelected: (Track, List<Track>?) -> Unit,
    userPlaylists: List<Playlist>,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onToggleShufflePlay: (List<Track>) -> Unit,
    onRenamePlaylist: (() -> Unit)?,
    onDeletePlaylist: (() -> Unit)?,
    isMultiSelectMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleTrackSelect: (String) -> Unit = {},
    pagePadding: Dp,
) {
    val title: String
    val subtitle: String
    val tracks: List<Track>
    val artworkSeed: Int
    val artworkPath: String?
    when (detail) {
        LibraryDetail.Favorites -> {
            title = "我的收藏"
            subtitle = "${favoriteTracks.size} 首歌曲"
            tracks = favoriteTracks
            artworkSeed = favoriteTracks.firstOrNull()?.artworkSeed ?: 9
            artworkPath = favoriteTracks.firstNotNullOfOrNull(Track::artworkPath)
        }
        is LibraryDetail.PlaylistDetail -> {
            val playlist = playlists.firstOrNull { it.id == detail.playlistId }
            title = playlist?.name.orEmpty()
            subtitle = playlist?.subtitle ?: "0 首歌曲"
            tracks = playlist?.tracks.orEmpty()
            artworkSeed = playlist?.artworkSeed ?: 0
            artworkPath = tracks.firstNotNullOfOrNull(Track::artworkPath)
        }
        is LibraryDetail.AlbumDetail -> {
            val albumTracks = (libraryTracks + playlists.flatMap(Playlist::tracks))
                .distinctBy(Track::id)
                .filter { it.album.ifBlank { "未知专辑" } == detail.album }
            title = detail.album
            subtitle = "${albumTracks.firstOrNull()?.artist?.ifBlank { "未知艺术家" }.orEmpty()} · ${albumTracks.size} 首歌曲"
            tracks = albumTracks
            artworkSeed = albumTracks.firstOrNull()?.artworkSeed ?: 0
            artworkPath = albumTracks.firstNotNullOfOrNull(Track::artworkPath)
        }
        is LibraryDetail.ArtistDetail -> {
            val artistTracks = (libraryTracks + playlists.flatMap(Playlist::tracks))
                .distinctBy(Track::id)
                .filter { it.artist.ifBlank { "未知艺术家" } == detail.artist }
            title = detail.artist
            subtitle = "${artistTracks.size} 首歌曲"
            tracks = artistTracks
            artworkSeed = artistTracks.firstOrNull()?.artworkSeed ?: 0
            artworkPath = artistTracks.firstNotNullOfOrNull(Track::artworkPath)
        }
    }
    val playable = tracks.filter { it.sourceUri != null }
    val playlistListState = rememberLazyListState()
    val currentTrackId = playerState.currentTrack?.id
    LaunchedEffect(currentTrackId) {
        if (currentTrackId != null) {
            val idx = tracks.indexOfFirst { it.id == currentTrackId }
            if (idx >= 0) {
                val targetIndex = idx + 2
                val isVisible = playlistListState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
                if (!isVisible) {
                    playlistListState.animateScrollToItem(targetIndex)
                }
            }
        }
    }

    LazyColumn(
        state = playlistListState,
        contentPadding = PaddingValues(start = pagePadding, end = pagePadding, bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = ResonanceColors.TextPrimary)
                }
                Spacer(Modifier.weight(1f))
                if (onRenamePlaylist != null) {
                    TextButton(onClick = onRenamePlaylist) { Text("重命名", color = ResonanceColors.Muted) }
                }
                if (onDeletePlaylist != null) {
                    TextButton(onClick = onDeletePlaylist) { Text("删除歌单", color = ResonanceColors.Primary) }
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AlbumArtwork(
                    seed = artworkSeed,
                    modifier = Modifier.size(168.dp),
                    cornerRadius = 14.dp,
                    artworkPath = artworkPath,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = ResonanceColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Dim)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { playable.firstOrNull()?.let { onTrackSelected(it, playable) } },
                        enabled = playable.isNotEmpty(),
                        shape = ResonanceShapes.Button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ResonanceColors.Primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("播放全部", style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = { onToggleShufflePlay(playable) },
                        enabled = playable.isNotEmpty(),
                        shape = ResonanceShapes.Button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ResonanceColors.Soft,
                            contentColor = ResonanceColors.TextPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("随机播放", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = ResonanceColors.Divider, thickness = Dp.Hairline)
        }
        if (tracks.isEmpty()) {
            item {
                Text(
                    "这里还没有歌曲",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ResonanceColors.Dim,
                    modifier = Modifier.padding(vertical = 28.dp),
                )
            }
        }
        itemsIndexed(tracks, key = { index, track -> "detail-${track.id}-$index" }) { index, track ->
            TrackRow(
                track = track,
                selected = playerState.currentTrack?.id == track.id,
                isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                onClick = { onTrackSelected(track, playable) },
                userPlaylists = userPlaylists,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onDeleteLocalTrack = onDeleteLocalTrack,
                onToggleFavorite = onToggleFavorite,
                index = index + 1,
                isMultiSelectMode = isMultiSelectMode,
                isMultiSelected = selectedTrackIds.contains(track.id),
                onToggleMultiSelect = { onToggleTrackSelect(track.id) },
                modifier = if (LocalReducedMotion.current) Modifier else Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun SearchResultList(
    query: String,
    tracks: List<Track>,
    playerState: PlayerState,
    onTrackSelected: (Track, List<Track>?) -> Unit,
    userPlaylists: List<Playlist>,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    isMultiSelectMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleTrackSelect: (String) -> Unit = {},
    contentPadding: PaddingValues,
) {
    val trimmed = query.trim()
    val results = localSearchResults(query, tracks)
    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        if (trimmed.isNotEmpty() && results.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 36.dp)) {
                    Text("没有找到「$trimmed」", style = MaterialTheme.typography.titleMedium, color = ResonanceColors.TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "换个关键词试试，支持歌名、歌手、专辑及拼音首字母缩写。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ResonanceColors.Dim,
                    )
                }
            }
        }
        itemsIndexed(results, key = { index, track -> "search-${track.id}-$index" }) { _, track ->
            TrackRow(
                track = track,
                selected = playerState.currentTrack?.id == track.id,
                isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                onClick = { onTrackSelected(track, results) },
                userPlaylists = userPlaylists,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onDeleteLocalTrack = onDeleteLocalTrack,
                onToggleFavorite = onToggleFavorite,
                isMultiSelectMode = isMultiSelectMode,
                isMultiSelected = selectedTrackIds.contains(track.id),
                onToggleMultiSelect = { onToggleTrackSelect(track.id) },
                modifier = if (LocalReducedMotion.current) Modifier else Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun LibraryLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ResonanceColors.Primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(14.dp))
            Text("正在整理音乐库…", style = MaterialTheme.typography.titleSmall, color = ResonanceColors.Muted)
        }
    }
}

@Composable
private fun EmptyLibrary(onCreatePlaylist: () -> Unit, onImport: () -> Unit, message: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (message != null) {
            Surface(color = ResonanceColors.PrimarySoft, shape = RoundedCornerShape(10.dp)) {
                Text(
                    message,
                    color = ResonanceColors.Primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ResonanceColors.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = ResonanceColors.Primary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("从第一首歌开始", style = MaterialTheme.typography.headlineMedium, color = ResonanceColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "扫描本机音乐，或新建一个歌单。原文件不会被覆盖，转换后的 MP3 将收进你的音乐库。",
            style = MaterialTheme.typography.bodyMedium,
            color = ResonanceColors.Muted,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onImport,
            shape = ResonanceShapes.Button,
            colors = ButtonDefaults.buttonColors(containerColor = ResonanceColors.Primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("扫描或导入音乐", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onCreatePlaylist,
            shape = ResonanceShapes.Button,
            colors = ButtonDefaults.buttonColors(containerColor = ResonanceColors.Soft, contentColor = ResonanceColors.TextPrimary),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("新建空歌单", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ---------------------------------------------------------------------------
// 发现（全局搜索）
// ---------------------------------------------------------------------------

@Composable
private fun DiscoverScreen(
    libraryTracks: List<Track>,
    playlists: List<Playlist>,
    playerState: PlayerState,
    onTrackSelected: (Track, List<Track>?) -> Unit,
    userPlaylists: List<Playlist>,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    compact: Boolean,
    onOpenBatchEnrich: (List<Track>) -> Unit,
    onBatchDelete: (List<Track>) -> Unit,
    onBatchAddToPlaylist: (List<Track>, String) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var showDelete by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    val pagePadding = if (compact) 16.dp else 32.dp
    val allKnownTracks = (libraryTracks + playlists.flatMap(Playlist::tracks)).distinctBy(Track::id)

    val results = localSearchResults(query, allKnownTracks)
    val selectedTracks = allKnownTracks.filter { it.id in selectedIds }
    fun toggle(id: String) { selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id }
    ResonanceBackHandler(enabled = selecting) { selecting = false; selectedIds = emptySet() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(end = pagePadding), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "搜索",
            style = MaterialTheme.typography.headlineLarge,
            color = ResonanceColors.TextPrimary,
            modifier = Modifier.weight(1f).padding(start = pagePadding, top = 14.dp, bottom = 10.dp),
        )
        IconButton(onClick = { selecting = !selecting; selectedIds = emptySet() }) {
            Icon(if (selecting) Icons.Default.Close else Icons.Default.Checklist,
                contentDescription = if (selecting) "退出搜索多选" else "多选搜索结果", tint = ResonanceColors.Muted)
        }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = pagePadding),
            placeholder = { Text("搜索歌名、歌手或专辑", color = ResonanceColors.Dim) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ResonanceColors.Dim) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "清空", tint = ResonanceColors.Muted)
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ResonanceColors.Primary,
                unfocusedBorderColor = ResonanceColors.Divider,
                focusedContainerColor = ResonanceColors.Raised,
                unfocusedContainerColor = ResonanceColors.Raised,
                cursorColor = ResonanceColors.Primary,
            ),
        )
        if (selecting) MultiSelectToolbar(
            selectedCount = selectedTracks.size, currentViewTracks = results, selectedTrackIds = selectedIds,
            onSelectAll = { selectedIds = selectedIds + results.map(Track::id) },
            onDeselectAll = { selectedIds = emptySet() },
            onSelectMissingOnly = { selectedIds = results.filter { it.isMissingAnyMetadata }.map(Track::id).toSet() },
            onEnrichSelected = { onOpenBatchEnrich(selectedTracks) },
            onAddToPlaylist = { showAdd = true }, onDeleteSelected = { showDelete = true },
            onExitMultiSelect = { selecting = false; selectedIds = emptySet() },
            modifier = Modifier.padding(horizontal = pagePadding),
        )
        Spacer(Modifier.height(6.dp))
        if (query.trim().isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = ResonanceColors.Dimmer, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("搜索本地音乐", style = MaterialTheme.typography.titleSmall, color = ResonanceColors.Muted)
                    Text("按歌名、歌手或专辑查找", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                }
            }
        } else {
            SearchResultList(
                query = query,
                tracks = allKnownTracks,
                playerState = playerState,
                onTrackSelected = onTrackSelected,
                userPlaylists = userPlaylists,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onDeleteLocalTrack = onDeleteLocalTrack,
                onToggleFavorite = onToggleFavorite,
                isMultiSelectMode = selecting,
                selectedTrackIds = selectedIds,
                onToggleTrackSelect = ::toggle,
                contentPadding = PaddingValues(horizontal = pagePadding, vertical = 10.dp),
            )
        }
    }
    if (showDelete && selectedTracks.isNotEmpty()) BatchDeleteTracksDialog(selectedTracks.size,
        onDismiss = { showDelete = false }, onConfirm = {
            onBatchDelete(selectedTracks); showDelete = false; selecting = false; selectedIds = emptySet()
        })
    if (showAdd && selectedTracks.isNotEmpty()) BatchAddToPlaylistDialog(userPlaylists, selectedTracks.size,
        onDismiss = { showAdd = false }, onSelectPlaylist = { id ->
            onBatchAddToPlaylist(selectedTracks, id); showAdd = false; selecting = false; selectedIds = emptySet()
        }, onCreateNewPlaylist = { showAdd = false; onCreatePlaylist() })

}

@Composable
private fun PlayingPlaceholderScreen(onOpenLibrary: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = ResonanceColors.Dimmer, modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(14.dp))
        Text("还没有在播放的音乐", style = MaterialTheme.typography.titleMedium, color = ResonanceColors.TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("去音乐库挑一首开始播放", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Dim)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onOpenLibrary,
            shape = ResonanceShapes.Button,
            colors = ButtonDefaults.buttonColors(containerColor = ResonanceColors.Primary, contentColor = MaterialTheme.colorScheme.onPrimary),
        ) {
            Text("打开音乐库", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ---------------------------------------------------------------------------
// 我的 / 工具页（导入、同步、设置）
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// 歌曲列表项（核心组件，扁平化）
// ---------------------------------------------------------------------------

@Composable
private fun TrackRow(
    track: Track,
    selected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    userPlaylists: List<Playlist>,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    modifier: Modifier = Modifier,
    index: Int? = null,
    isMultiSelectMode: Boolean = false,
    isMultiSelected: Boolean = false,
    onToggleMultiSelect: () -> Unit = {},
    onEditTrack: ((Track) -> Unit)? = null,
) {
    val available = track.sourceUri != null
    var menuExpanded by remember(track.id) { mutableStateOf(false) }
    val dividerIndent = if (isMultiSelectMode) 44.dp else if (index != null) 34.dp else 58.dp
    val rowInteraction = remember { MutableInteractionSource() }
    val rowHighlight = when {
        isMultiSelected -> ResonanceColors.PrimarySoft.copy(alpha = 0.25f)
        selected -> ResonanceColors.PrimarySoft.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val animatedBg by animateColorAsState(
        targetValue = rowHighlight,
        animationSpec = tween(motionDuration(280)),
        label = "trackRowBg",
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (selected) ResonanceColors.GlassBorderGlow.copy(alpha = 0.35f) else Color.Transparent,
        animationSpec = tween(motionDuration(280)),
        label = "trackRowBorder",
    )
    val animatedTitleColor by animateColorAsState(
        targetValue = if (isMultiSelected || selected) ResonanceColors.Primary else ResonanceColors.TextPrimary,
        animationSpec = tween(motionDuration(260)),
        label = "trackRowTitleColor",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .resonancePressable(rowInteraction, pressedScale = 0.98f)
                .clip(RoundedCornerShape(12.dp))
                .background(animatedBg)
                .border(1.dp, animatedBorder, RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = rowInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = {
                        if (isMultiSelectMode) {
                            onToggleMultiSelect()
                        } else {
                            onClick()
                        }
                    },
                )
                .heightIn(min = 72.dp)
                .semantics { this.selected = selected; stateDescription = if (selected) "当前曲目" else if (!available) "音频待匹配" else "可播放" }
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = selected && !isMultiSelectMode,
                enter = slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeIn(tween(220)),
                exit = slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeOut(tween(180)),
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .width(3.5.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ResonanceColors.Primary)
                )
            }
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isMultiSelected,
                    onCheckedChange = { onToggleMultiSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ResonanceColors.Primary,
                        uncheckedColor = ResonanceColors.Dim,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            if (index != null && !isMultiSelectMode) {
                Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "%02d".format(index),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) ResonanceColors.Primary else ResonanceColors.Dim,
                    )
                }
            } else {
                AlbumArtwork(
                    track.artworkSeed,
                    Modifier.size(46.dp),
                    8.dp,
                    track.artworkPath,
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(
                        visible = selected && !isMultiSelectMode,
                        enter = fadeIn(tween(220)) + expandHorizontally(),
                        exit = fadeOut(tween(180)) + shrinkHorizontally(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NowPlayingIndicator(isPlaying = isPlaying)
                            Spacer(Modifier.width(7.dp))
                        }
                    }
                    Text(
                        track.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = animatedTitleColor,
                        fontWeight = if (isMultiSelected || selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!available) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "待匹配",
                            style = MaterialTheme.typography.labelSmall,
                            color = ResonanceColors.Dim,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ResonanceColors.Soft)
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    track.artist.ifBlank { "未知艺术家" },
                    style = MaterialTheme.typography.bodySmall,
                    color = ResonanceColors.Dim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                track.durationText,
                style = MaterialTheme.typography.bodySmall,
                color = ResonanceColors.Dim,
            )
            if (!isMultiSelectMode) {
                Box {
                    AccessibleIconButton("更多操作", onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多操作",
                            tint = ResonanceColors.Dim,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (track.isFavorite) "取消收藏" else "收藏") },
                            leadingIcon = {
                                Icon(
                                    if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (track.isFavorite) ResonanceColors.Primary else ResonanceColors.Muted,
                                )
                            },
                            onClick = {
                                onToggleFavorite(track)
                                menuExpanded = false
                            },
                        )
                        if (onEditTrack != null) {
                            DropdownMenuItem(
                                text = { Text("编辑歌曲信息") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = ResonanceColors.TextPrimary,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEditTrack(track)
                                },
                            )
                        }
                        if (userPlaylists.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("先新建一个歌单") },
                                onClick = { menuExpanded = false },
                                enabled = false,
                            )
                        } else {
                            userPlaylists.forEach { playlist ->
                                val included = playlist.tracks.any { it.id == track.id }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (included) "从「${playlist.name}」移出" else "加入「${playlist.name}」",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    leadingIcon = if (included) {
                                        { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ResonanceColors.Primary) }
                                    } else null,
                                    onClick = {
                                        onPlaylistMembershipChange(track, playlist.id, !included)
                                        menuExpanded = false
                                    },
                                )
                            }
                        }
                        if (available && !track.sourceUri.isNullOrBlank()) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("删除本地音频文件", color = ResonanceColors.Primary) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ResonanceColors.Primary) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteLocalTrack(track)
                                },
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(
            color = ResonanceColors.Divider,
            thickness = Dp.Hairline,
            modifier = Modifier.padding(start = dividerIndent),
        )
    }
}

@Composable
internal fun NowPlayingIndicator(isPlaying: Boolean) {
    if (!isPlaying || LocalReducedMotion.current || !LocalAppForeground.current) {
        Icon(
            Icons.Default.GraphicEq,
            contentDescription = "当前曲目",
            tint = ResonanceColors.Primary,
            modifier = Modifier.size(16.dp),
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "fluidPlayingBars")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = AnimationRepeatMode.Restart,
        ),
        label = "fluidWavePhase",
    )

    Row(
        modifier = Modifier.width(18.dp).height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(0.0f, 1.2f, 2.4f, 3.6f).forEach { offset ->
            val rad = (phase * kotlin.math.PI / 180.0 + offset).toFloat()
            val heightFraction = ((sin(rad) + 1f) * 0.38f + 0.24f).coerceIn(0.2f, 1.0f)
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightFraction)
                    .clip(ResonanceShapes.Capsule)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(ResonanceColors.PrimaryGlow, ResonanceColors.Primary)
                        )
                    ),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Mini Player（水润悬浮磨砂胶囊）
// ---------------------------------------------------------------------------

@Composable
private fun MiniPlayer(
    playerState: PlayerState,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    compact: Boolean,
) {
    val track = playerState.currentTrack ?: return
    val isPlaying = playerState.isPlaying
    val playInteraction = remember { MutableInteractionSource() }

    // 播放按钮弹性水滴爆裂动效
    val playButtonScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = resonanceSpring(),
        label = "miniPlayerPlayScale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (compact) 12.dp else 20.dp,
                end = if (compact) 12.dp else 20.dp,
                top = 2.dp,
                bottom = if (compact) 8.dp else 12.dp,
            )
            .resonanceGlass(
                shape = RoundedCornerShape(22.dp),
                borderColors = listOf(ResonanceColors.GlassBorder, ResonanceColors.GlassBorderSubtle),
                shadowElevation = 5.dp,
                shadowColor = ResonanceColors.Shadow.copy(alpha = if (ResonanceColors.isDark) 0.45f else 0.12f),
            ),
    ) {
        Column(Modifier.fillMaxWidth()) {
            // 顶部流体渐变发光进度条（附带水珠流光）
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(ResonanceColors.Divider.copy(alpha = 0.25f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(playerState.progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(
                                    ResonanceColors.Primary,
                                    ResonanceColors.PrimaryGlow,
                                    ResonanceColors.PositiveGlow,
                                )
                            )
                        ),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .clickable(role = Role.Button, onClick = onOpenNowPlaying)
                    .semantics { contentDescription = "打开正在播放详情与歌词" }
                    .padding(start = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedContent(
                    targetState = track,
                    transitionSpec = {
                        (slideInHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
                        ) { width -> (width * 0.4f).toInt() } + fadeIn(tween(260)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                                ) { width -> -(width * 0.4f).toInt() } + fadeOut(tween(200))
                            )
                    },
                    label = "miniPlayerTrackSlide",
                    modifier = Modifier.weight(1f),
                ) { currentTrack ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(10.dp),
                                spotColor = ResonanceColors.Primary.copy(alpha = 0.35f),
                            ),
                        ) {
                            AlbumArtwork(
                                currentTrack.artworkSeed,
                                Modifier.size(42.dp),
                                10.dp,
                                currentTrack.artworkPath,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isPlaying) {
                                    NowPlayingIndicator(isPlaying = true)
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    currentTrack.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = ResonanceColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(Modifier.height(1.dp))
                            Text(
                                currentTrack.artist.ifBlank { "未知艺术家" },
                                style = MaterialTheme.typography.bodySmall,
                                color = ResonanceColors.Dim,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (!compact) {
                    IconButton(onClick = onPrevious) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "上一首", tint = ResonanceColors.Muted)
                    }
                }

                // 水灵灵的弹性大播放键
                FilledIconButton(
                    onClick = onTogglePlay,
                    interactionSource = playInteraction,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            scaleX = playButtonScale
                            scaleY = playButtonScale
                        }
                        .resonancePressable(playInteraction, pressedScale = 0.88f)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            spotColor = ResonanceColors.Primary.copy(alpha = 0.5f),
                        ),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ResonanceColors.Primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(22.dp),
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "下一首", tint = ResonanceColors.Muted)
                }
            }
        }
    }
}

internal fun formatPlaybackPosition(durationText: String, progress: Float): String {
    val total = com.resonance.player.model.durationTextToSeconds(durationText)
    if (total <= 0) return "0:00"
    val elapsed = (total * progress.coerceIn(0f, 1f)).toInt()
    return "%d:%02d".format(elapsed / 60, elapsed % 60)
}

// ---------------------------------------------------------------------------
// 通用组件
// ---------------------------------------------------------------------------

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ResonanceColors.Primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(19.dp))
    }
}

@Composable
fun AlbumArtwork(seed: Int, modifier: Modifier = Modifier, cornerRadius: Dp = 12.dp, artworkPath: String? = null) {
    val decoded by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, artworkPath) {
        value = artworkPath?.let { path -> withContext(Dispatchers.Default) { decodeArtwork(path) } }
    }
    val artwork = decoded
    if (artwork != null) {
        Image(
            bitmap = artwork,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
        )
        return
    }
    val palettes = listOf(
        listOf(Color(0xFFA3C6BA), Color(0xFF5D887B), Color(0xFF25463E)),
        listOf(Color(0xFF5FD19B), Color(0xFF1C4D4A), Color(0xFF101821)),
        listOf(Color(0xFFAAC3E7), Color(0xFF5F83B4), Color(0xFF293E5B)),
        listOf(Color(0xFFBDC9D3), Color(0xFF718997), Color(0xFF293D47)),
    )
    val palette = palettes[((seed % palettes.size) + palettes.size) % palettes.size]
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(palette)),
    ) {
        val unit = size.minDimension
        rotate(seed * 7f) {
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = unit * 0.34f,
                center = Offset(size.width * 0.78f, size.height * 0.28f),
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.16f),
                radius = unit * 0.44f,
                center = Offset(size.width * 0.18f, size.height * 0.86f),
            )
            val path = Path().apply {
                moveTo(size.width * 0.08f, size.height * 0.58f)
                lineTo(size.width * 0.88f, size.height * 0.20f)
                lineTo(size.width * 0.74f, size.height * 0.88f)
                close()
            }
            drawPath(path, color = Color.White.copy(alpha = 0.07f))
        }
    }
}

private fun localSearchResults(query: String, tracks: List<Track>): List<Track> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()
    return tracks.filter { track ->
        com.resonance.player.util.PinyinUtils.matches(track.title, trimmed) ||
            com.resonance.player.util.PinyinUtils.matches(track.artist, trimmed) ||
            com.resonance.player.util.PinyinUtils.matches(track.album, trimmed)
    }
}
