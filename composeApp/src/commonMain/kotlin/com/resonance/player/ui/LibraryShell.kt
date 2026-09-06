package com.resonance.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.unit.sp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.ResonanceShapes
import com.resonance.player.design.resonancePressable
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
private enum class LibraryTab(val label: String) { Tracks("歌曲"), Playlists("歌单"), Albums("专辑"), Artists("艺术家") }

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
    DestinationItem(LibraryDestination.Discover, "发现", Icons.Default.Search),
    DestinationItem(LibraryDestination.Playing, "播放", Icons.Default.PlayCircle),
    DestinationItem(LibraryDestination.Profile, "我的", Icons.Default.Person),
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
    onTrackSelected: (Track) -> Unit,
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ResonanceColors.Canvas)) {
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
                    Box(Modifier.weight(1f)) { sharedContent(true) }
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

        if (showNowPlaying && playerState.currentTrack != null) {
            NowPlayingOverlay(
                playerState = playerState,
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
                onEmbedLyrics = { onEmbedLyrics(playerState.currentTrack) },
                onToggleFloatingLyrics = onToggleFloatingLyrics,
                floatingLyricsEnabled = floatingLyricsEnabled,
                onTrackSelected = onTrackSelected,
                onToggleFavorite = onToggleFavorite,
                onRefreshLyrics = onRefreshLyrics,
                onRequestAiLyrics = onRequestAiLyrics,
                onDeleteLocalTrack = { track ->
                    showNowPlaying = false
                    onDeleteLocalTrack(track)
                },
            )
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

private fun tabDestinationOf(destination: LibraryDestination): LibraryDestination = when (destination) {
    LibraryDestination.Import, LibraryDestination.Sync, LibraryDestination.Settings -> LibraryDestination.Profile
    else -> destination
}

@Composable
private fun BottomNavigationBar(
    destination: LibraryDestination,
    onDestinationChange: (LibraryDestination) -> Unit,
) {
    val activeTab = tabDestinationOf(destination)
    Column(Modifier.fillMaxWidth().background(ResonanceColors.CanvasElevated)) {
        HorizontalDivider(color = ResonanceColors.Divider, thickness = Dp.Hairline)
        NavigationBar(
            containerColor = ResonanceColors.CanvasElevated,
            tonalElevation = 0.dp,
        ) {
            destinationItems.forEach { item ->
                val selected = activeTab == item.destination
                NavigationBarItem(
                    selected = selected,
                    onClick = { onDestinationChange(item.destination) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ResonanceColors.Coral,
                        selectedTextColor = ResonanceColors.Coral,
                        unselectedIconColor = ResonanceColors.Dim,
                        unselectedTextColor = ResonanceColors.Dim,
                        indicatorColor = Color.Transparent,
                    ),
                    icon = {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(23.dp),
                        )
                    },
                    label = {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
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
    NavigationRail(
        containerColor = ResonanceColors.CanvasElevated,
        header = { BrandMark(modifier = Modifier.padding(vertical = 16.dp)) },
    ) {
        destinationItems.forEach { item ->
            val selected = activeTab == item.destination
            NavigationRailItem(
                selected = selected,
                onClick = { onDestinationChange(item.destination) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = ResonanceColors.Coral,
                    selectedTextColor = ResonanceColors.Coral,
                    unselectedIconColor = ResonanceColors.Dim,
                    unselectedTextColor = ResonanceColors.Dim,
                    indicatorColor = Color.Transparent,
                ),
                icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(23.dp)) },
                label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
            )
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
            .width(232.dp)
            .fillMaxHeight()
            .background(ResonanceColors.CanvasElevated)
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
            IconButton(onClick = { onThemeModeChange(nextMode) }, modifier = Modifier.size(36.dp)) {
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
            Spacer(Modifier.height(2.dp))
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
    val background by animateColorAsState(
        if (selected) ResonanceColors.CoralSoft else Color.Transparent,
        animationSpec = tween(MotionQuick),
        label = "sidebarBackground",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .resonancePressable(interactionSource, pressedScale = 0.98f)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Icon(
            item.icon,
            contentDescription = null,
            tint = if (selected) ResonanceColors.Coral else ResonanceColors.Dim,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) ResonanceColors.Coral else ResonanceColors.Muted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
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
    onTrackSelected: (Track) -> Unit,
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
    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            (fadeIn(tween(MotionStandard))) togetherWith (fadeOut(tween(MotionQuick))) using
                SizeTransform(clip = false)
        },
        label = "destinationContent",
    ) { activeDestination ->
        when (activeDestination) {
            LibraryDestination.Library -> LibraryScreen(
                playlists = playlists,
                libraryTracks = libraryTracks,
                playerState = playerState,
                onTrackSelected = onTrackSelected,
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
    Surface(
        color = ResonanceColors.Raised,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "已选 $selectedCount / ${currentViewTracks.size} 首",
                    style = MaterialTheme.typography.titleSmall,
                    color = ResonanceColors.Ivory,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onSelectAll, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("全选", color = ResonanceColors.Coral, style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = onDeselectAll, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("清空", color = ResonanceColors.Muted, style = MaterialTheme.typography.labelMedium)
                    }
                    if (missingInView > 0) {
                        TextButton(onClick = onSelectMissingOnly, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("仅选缺失 ($missingInView)", color = ResonanceColors.Coral, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    IconButton(onClick = onExitMultiSelect, modifier = Modifier.size(28.dp)) {
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
                        containerColor = ResonanceColors.Coral,
                        contentColor = Color.White,
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
                        contentColor = ResonanceColors.Ivory,
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
                        containerColor = ResonanceColors.CoralSoft,
                        contentColor = ResonanceColors.Coral,
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
    onTrackSelected: (Track) -> Unit,
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

    var tab by rememberSaveable { mutableStateOf(LibraryTab.Tracks.name) }
    val activeTab = LibraryTab.entries.firstOrNull { it.name == tab } ?: LibraryTab.Tracks
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
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

    val currentViewTracks = remember(tab, activeTab, detail, searchActive, searchQuery, allKnownTracks, libraryTracks, favoriteTracks, playlists) {
        if (searchActive) {
            val q = searchQuery.trim()
            if (q.isEmpty()) emptyList()
            else allKnownTracks.filter { it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) || it.album.contains(q, ignoreCase = true) }
        } else if (detail != null) {
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
    } else if (searchActive) {
        ResonanceBackHandler(enabled = true) {
            searchQuery = ""
            searchActive = false
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
                if (isMultiSelectMode) "批量选择" else if (searchActive) "搜索" else "音乐库",
                style = MaterialTheme.typography.headlineLarge,
                color = ResonanceColors.Ivory,
                modifier = Modifier.weight(1f),
            )
            if (previewMode) {
                TextButton(onClick = onExitPreview) { Text("清空预览", color = ResonanceColors.Muted) }
            }
            if (!isMultiSelectMode) {
                if (allLibraryMissingTracks.isNotEmpty() && !searchActive && detail == null) {
                    Surface(
                        color = ResonanceColors.CoralSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable { onOpenBatchEnrich(allLibraryMissingTracks) }
                            .padding(end = 6.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ResonanceColors.Coral, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "一键补全缺失 (${allLibraryMissingTracks.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = ResonanceColors.Coral,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                IconButton(onClick = {
                    isMultiSelectMode = true
                    selectedTrackIds = emptySet()
                }) {
                    Icon(Icons.Default.Checklist, contentDescription = "多选", tint = ResonanceColors.Muted)
                }
                IconButton(onClick = {
                    if (searchActive) searchQuery = ""
                    searchActive = !searchActive
                }) {
                    Icon(
                        if (searchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (searchActive) "关闭搜索" else "搜索",
                        tint = ResonanceColors.Muted,
                    )
                }
                if (!searchActive) {
                    IconButton(onClick = onCreatePlaylist) {
                        Icon(Icons.Default.Add, contentDescription = "新建歌单", tint = ResonanceColors.Muted)
                    }
                }
            } else {
                TextButton(onClick = {
                    isMultiSelectMode = false
                    selectedTrackIds = emptySet()
                }) {
                    Text("退出多选", color = ResonanceColors.Coral, fontWeight = FontWeight.SemiBold)
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
                color = ResonanceColors.CoralSoft,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = pagePadding, vertical = 6.dp),
            ) {
                Text(
                    message,
                    color = ResonanceColors.Coral,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }
        }

        if (searchActive) {
            // 搜索模式
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = pagePadding, vertical = 8.dp),
                placeholder = { Text("搜索歌名、歌手或专辑", color = ResonanceColors.Dim) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ResonanceColors.Dim) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "清空", tint = ResonanceColors.Muted)
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ResonanceColors.Coral,
                    unfocusedBorderColor = ResonanceColors.Divider,
                    focusedContainerColor = ResonanceColors.Raised,
                    unfocusedContainerColor = ResonanceColors.Raised,
                    cursorColor = ResonanceColors.Coral,
                ),
            )
            SearchResultList(
                query = searchQuery,
                tracks = allKnownTracks,
                playerState = playerState,
                onTrackSelected = onTrackSelected,
                userPlaylists = userPlaylists,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onDeleteLocalTrack = onDeleteLocalTrack,
                onToggleFavorite = onToggleFavorite,
                isMultiSelectMode = isMultiSelectMode,
                selectedTrackIds = selectedTrackIds,
                onToggleTrackSelect = ::toggleTrackSelect,
                contentPadding = PaddingValues(start = pagePadding, end = pagePadding, bottom = 24.dp),
            )
            return@Column
        }

        val currentDetail = detail
        if (currentDetail != null) {
            // 详情视图（歌单 / 专辑 / 艺术家 / 收藏）
            LibraryDetailView(
                detail = currentDetail,
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
                onToggleShufflePlay = { tracks ->
                    tracks.filter { it.sourceUri != null }.takeIf { it.isNotEmpty() }?.random()?.let(onTrackSelected)
                },
                onRenamePlaylist = if (detailPlaylist != null && !previewMode) { { showRenameDialog = true } } else null,
                onDeletePlaylist = if (detailPlaylist != null && !previewMode) { { showDeleteDialog = true } } else null,
                isMultiSelectMode = isMultiSelectMode,
                selectedTrackIds = selectedTrackIds,
                onToggleTrackSelect = ::toggleTrackSelect,
                pagePadding = pagePadding,
            )
        } else {
            // 标签页
            LibraryTabRow(
                activeTab = activeTab,
                onSelect = { tab = it.name },
                modifier = Modifier.padding(horizontal = pagePadding),
            )
            when (activeTab) {
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
    Row(modifier = modifier.fillMaxWidth()) {
        LibraryTab.entries.forEach { tab ->
            val selected = tab == activeTab
            Column(
                modifier = Modifier
                    .padding(end = 22.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                    ) { onSelect(tab) }
                    .padding(vertical = 10.dp),
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) ResonanceColors.Ivory else ResonanceColors.Dim,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .width(18.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(if (selected) ResonanceColors.Coral else Color.Transparent),
                )
            }
        }
    }
    HorizontalDivider(color = ResonanceColors.Divider, thickness = Dp.Hairline)
}

@Composable
private fun TrackListTab(
    tracks: List<Track>,
    favoriteCount: Int,
    playerState: PlayerState,
    onTrackSelected: (Track) -> Unit,
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
    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
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
                        .background(ResonanceColors.CoralSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = ResonanceColors.Coral, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("我的收藏", style = MaterialTheme.typography.titleSmall, color = ResonanceColors.Ivory)
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
                onClick = { onTrackSelected(track) },
                userPlaylists = userPlaylists,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onDeleteLocalTrack = onDeleteLocalTrack,
                onToggleFavorite = onToggleFavorite,
                isMultiSelectMode = isMultiSelectMode,
                isMultiSelected = selectedTrackIds.contains(track.id),
                onToggleMultiSelect = { onToggleTrackSelect(track.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun PlaylistListTab(
    playlists: List<Playlist>,
    onOpenPlaylist: (Playlist) -> Unit,
    onCreatePlaylist: () -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(playlists, key = { _, playlist -> playlist.id }) { _, playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPlaylist(playlist) }
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArtwork(
                    seed = playlist.artworkSeed,
                    modifier = Modifier.size(48.dp),
                    cornerRadius = 8.dp,
                    artworkPath = playlist.tracks.firstNotNullOfOrNull(Track::artworkPath),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        playlist.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = ResonanceColors.Ivory,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        playlist.subtitle,
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
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreatePlaylist)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(ResonanceShapes.ArtworkSmall)
                        .background(ResonanceColors.Soft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = ResonanceColors.Muted, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("新建歌单", style = MaterialTheme.typography.titleSmall, color = ResonanceColors.Muted)
            }
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
                        color = ResonanceColors.Ivory,
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
                        color = ResonanceColors.Ivory,
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
    onTrackSelected: (Track) -> Unit,
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

    LazyColumn(
        contentPadding = PaddingValues(start = pagePadding, end = pagePadding, bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = ResonanceColors.Ivory)
                }
                Spacer(Modifier.weight(1f))
                if (onRenamePlaylist != null) {
                    TextButton(onClick = onRenamePlaylist) { Text("重命名", color = ResonanceColors.Muted) }
                }
                if (onDeletePlaylist != null) {
                    TextButton(onClick = onDeletePlaylist) { Text("删除歌单", color = ResonanceColors.Coral) }
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
                    color = ResonanceColors.Ivory,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Dim)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { playable.firstOrNull()?.let(onTrackSelected) },
                        enabled = playable.isNotEmpty(),
                        shape = ResonanceShapes.Button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ResonanceColors.Coral,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("播放全部", style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = { onToggleShufflePlay(tracks) },
                        enabled = playable.isNotEmpty(),
                        shape = ResonanceShapes.Button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ResonanceColors.Soft,
                            contentColor = ResonanceColors.Ivory,
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
                onClick = { onTrackSelected(track) },
                userPlaylists = userPlaylists,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onDeleteLocalTrack = onDeleteLocalTrack,
                onToggleFavorite = onToggleFavorite,
                index = index + 1,
                isMultiSelectMode = isMultiSelectMode,
                isMultiSelected = selectedTrackIds.contains(track.id),
                onToggleMultiSelect = { onToggleTrackSelect(track.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun SearchResultList(
    query: String,
    tracks: List<Track>,
    playerState: PlayerState,
    onTrackSelected: (Track) -> Unit,
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
    val results = if (trimmed.isEmpty()) {
        emptyList()
    } else {
        tracks.filter { track ->
            com.resonance.player.util.PinyinUtils.matches(track.title, trimmed) ||
                com.resonance.player.util.PinyinUtils.matches(track.artist, trimmed) ||
                com.resonance.player.util.PinyinUtils.matches(track.album, trimmed)
        }
    }
    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        if (trimmed.isNotEmpty() && results.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 36.dp)) {
                    Text("没有找到「$trimmed」", style = MaterialTheme.typography.titleMedium, color = ResonanceColors.Ivory)
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
                onClick = { onTrackSelected(track) },
                userPlaylists = userPlaylists,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onDeleteLocalTrack = onDeleteLocalTrack,
                onToggleFavorite = onToggleFavorite,
                isMultiSelectMode = isMultiSelectMode,
                isMultiSelected = selectedTrackIds.contains(track.id),
                onToggleMultiSelect = { onToggleTrackSelect(track.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun LibraryLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ResonanceColors.Coral, modifier = Modifier.size(32.dp))
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
            Surface(color = ResonanceColors.CoralSoft, shape = RoundedCornerShape(10.dp)) {
                Text(
                    message,
                    color = ResonanceColors.Coral,
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
                .background(ResonanceColors.CoralSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = ResonanceColors.Coral, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("从第一首歌开始", style = MaterialTheme.typography.headlineMedium, color = ResonanceColors.Ivory)
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
            colors = ButtonDefaults.buttonColors(containerColor = ResonanceColors.Coral, contentColor = Color.White),
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
            colors = ButtonDefaults.buttonColors(containerColor = ResonanceColors.Soft, contentColor = ResonanceColors.Ivory),
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
    onTrackSelected: (Track) -> Unit,
    userPlaylists: List<Playlist>,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    compact: Boolean,
) {
    var query by remember { mutableStateOf("") }
    val pagePadding = if (compact) 16.dp else 32.dp
    val allKnownTracks = (libraryTracks + playlists.flatMap(Playlist::tracks)).distinctBy(Track::id)

    Column(Modifier.fillMaxSize()) {
        Text(
            "发现",
            style = MaterialTheme.typography.headlineLarge,
            color = ResonanceColors.Ivory,
            modifier = Modifier.padding(start = pagePadding, end = pagePadding, top = 14.dp, bottom = 10.dp),
        )
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
                focusedBorderColor = ResonanceColors.Coral,
                unfocusedBorderColor = ResonanceColors.Divider,
                focusedContainerColor = ResonanceColors.Raised,
                unfocusedContainerColor = ResonanceColors.Raised,
                cursorColor = ResonanceColors.Coral,
            ),
        )
        Spacer(Modifier.height(6.dp))
        if (query.trim().isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = ResonanceColors.Dimmer, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("搜索你的音乐库", style = MaterialTheme.typography.titleSmall, color = ResonanceColors.Dim)
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
                contentPadding = PaddingValues(horizontal = pagePadding, vertical = 10.dp),
            )
        }
    }
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
        Text("还没有在播放的音乐", style = MaterialTheme.typography.titleMedium, color = ResonanceColors.Ivory)
        Spacer(Modifier.height(6.dp))
        Text("去音乐库挑一首开始播放", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Dim)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onOpenLibrary,
            shape = ResonanceShapes.Button,
            colors = ButtonDefaults.buttonColors(containerColor = ResonanceColors.Coral, contentColor = Color.White),
        ) {
            Text("打开音乐库", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ---------------------------------------------------------------------------
// 我的 / 工具页（导入、同步、设置）
// ---------------------------------------------------------------------------

@Composable
private fun ProfileScreen(
    compact: Boolean,
    onOpenImport: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val pagePadding = if (compact) 16.dp else 32.dp
    Column(Modifier.fillMaxSize().padding(horizontal = pagePadding)) {
        Text(
            "我的",
            style = MaterialTheme.typography.headlineLarge,
            color = ResonanceColors.Ivory,
            modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),
        )
        SettingRow(
            icon = Icons.Default.LibraryMusic,
            title = "导入音乐",
            subtitle = "扫描本机、转换 KGMA/KGG、导入歌单",
            onClick = onOpenImport,
        )
        SettingRow(
            icon = Icons.Outlined.Sync,
            title = "设备同步",
            subtitle = "局域网扫码同步 / 同步包导入导出",
            onClick = onOpenSync,
        )
        SettingRow(
            icon = Icons.Default.Settings,
            title = "设置",
            subtitle = "外观、音乐库与播放偏好",
            onClick = onOpenSettings,
        )
        SettingRow(
            icon = Icons.Default.Info,
            title = "关于 Resonance",
            subtitle = "版本 v$APP_VERSION · 本地优先",
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun ToolPage(
    title: String,
    onBack: () -> Unit,
    compact: Boolean,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val pagePadding = if (compact) 16.dp else 32.dp
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = pagePadding - 12.dp, end = pagePadding, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = ResonanceColors.Ivory)
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = ResonanceColors.Ivory)
        }
        HorizontalDivider(color = ResonanceColors.Divider, thickness = Dp.Hairline)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = pagePadding, vertical = 12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun ImportScreen(
    compact: Boolean,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onConvert: () -> Unit,
    onImportPlaylist: () -> Unit,
    message: String?,
    operationInProgress: Boolean,
) {
    ToolPage(title = "导入音乐", onBack = onBack, compact = compact) {
        if (operationInProgress) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = ResonanceColors.Coral,
                trackColor = ResonanceColors.Soft,
            )
            Spacer(Modifier.height(12.dp))
        }
        if (message != null) {
            Surface(color = ResonanceColors.CoralSoft, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    message,
                    color = ResonanceColors.Coral,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        SectionHeader("来源")
        SettingRow(
            icon = Icons.Default.Headphones,
            title = "扫描本机音乐",
            subtitle = "自动查找 MP3 / FLAC / M4A 等音频文件",
            onClick = onImport,
        )
        SettingRow(
            icon = Icons.Default.FolderOpen,
            title = "选择文件夹并转换 KGMA/KGG",
            subtitle = "递归转换为 320 kbps MP3，保留源文件",
            onClick = onConvert,
        )
        SettingRow(
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            title = "导入酷狗歌单链接",
            subtitle = "读取公开歌单目录并匹配本机已有歌曲",
            onClick = onImportPlaylist,
        )
    }
}

@Composable
private fun SyncScreen(
    compact: Boolean,
    onBack: () -> Unit,
    onExportSync: () -> Unit,
    onImportSync: () -> Unit,
    onStartLanSync: () -> Unit,
    lanQrPath: String?,
    message: String?,
) {
    ToolPage(title = "设备同步", onBack = onBack, compact = compact) {
        if (message != null) {
            Surface(color = ResonanceColors.CoralSoft, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    message,
                    color = ResonanceColors.Coral,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        SectionHeader("局域网")
        SettingRow(
            icon = Icons.Default.Devices,
            title = "生成配对二维码",
            subtitle = "10 分钟内有效，另一台设备扫码即可加密同步",
            onClick = onStartLanSync,
        )
        if (lanQrPath != null) {
            Spacer(Modifier.height(16.dp))
            AlbumArtwork(
                seed = 0,
                modifier = Modifier.size(220.dp).align(Alignment.CenterHorizontally),
                cornerRadius = 12.dp,
                artworkPath = lanQrPath,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "二维码已保存：$lanQrPath",
                style = MaterialTheme.typography.bodySmall,
                color = ResonanceColors.Dim,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        SectionHeader("同步包")
        SettingRow(
            icon = Icons.Default.FolderOpen,
            title = "导出同步包",
            subtitle = "打包 MP3、封面与元数据为 .resonance 文件",
            onClick = onExportSync,
        )
        SettingRow(
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            title = "导入同步包",
            subtitle = "校验完整性后合并到本机音乐库",
            onClick = onImportSync,
        )
    }
}

@Composable
private fun SettingsScreen(
    compact: Boolean,
    onBack: () -> Unit,
    libraryLocation: String,
    onOpenImport: () -> Unit,
    themeMode: ThemeMode = ThemeMode.Dark,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    deepSeekConfig: DeepSeekConfig = DeepSeekConfig(),
    onSaveDeepSeekConfig: (DeepSeekConfig) -> Unit = {},
    onTestDeepSeek: suspend (DeepSeekConfig) -> DeepSeekTestResult = { DeepSeekTestResult(false, "") },
    customLyricsFolder: String? = null,
    onSaveCustomLyricsFolder: (String?) -> Unit = {},
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var apiKey by remember(deepSeekConfig.apiKey) { mutableStateOf(deepSeekConfig.apiKey) }
    var baseUrl by remember(deepSeekConfig.baseUrl) { mutableStateOf(deepSeekConfig.baseUrl) }
    var model by remember(deepSeekConfig.model) { mutableStateOf(deepSeekConfig.model) }
    var showApiKey by remember { mutableStateOf(false) }
    var testInProgress by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<DeepSeekTestResult?>(null) }
    var lyricsFolder by remember(customLyricsFolder) { mutableStateOf(customLyricsFolder ?: "D:\\Music\\Kugou\\Leonard\\Lyrics") }
    var folderSavedMessage by remember { mutableStateOf<String?>(null) }

    ToolPage(title = "设置", onBack = onBack, compact = compact) {
        SectionHeader("外观")
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeOption("深色", Icons.Default.DarkMode, themeMode == ThemeMode.Dark, { onThemeModeChange(ThemeMode.Dark) }, Modifier.weight(1f))
            ThemeOption("浅色", Icons.Default.LightMode, themeMode == ThemeMode.Light, { onThemeModeChange(ThemeMode.Light) }, Modifier.weight(1f))
            ThemeOption("跟随系统", Icons.Default.BrightnessAuto, themeMode == ThemeMode.System, { onThemeModeChange(ThemeMode.System) }, Modifier.weight(1f))
        }

        SectionHeader("DeepSeek AI 增强")
        Text(
            "当本地歌词和 LRCLIB 均无结果时，自动调用 DeepSeek AI 搜索或生成精准逐行时间轴歌词与专辑视觉。",
            style = MaterialTheme.typography.bodySmall,
            color = ResonanceColors.Dim,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = {
                apiKey = it.trim()
                onSaveDeepSeekConfig(DeepSeekConfig(apiKey = apiKey, baseUrl = baseUrl, model = model))
            },
            label = { Text("DeepSeek API Key") },
            placeholder = { Text("sk-...", color = ResonanceColors.Dim) },
            singleLine = true,
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showApiKey) "隐藏" else "显示",
                        tint = ResonanceColors.Dim,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ResonanceColors.Coral,
                unfocusedBorderColor = ResonanceColors.Divider,
                focusedContainerColor = ResonanceColors.Canvas,
                unfocusedContainerColor = ResonanceColors.Canvas,
                cursorColor = ResonanceColors.Coral,
            ),
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = model,
                onValueChange = {
                    model = it.trim()
                    onSaveDeepSeekConfig(DeepSeekConfig(apiKey = apiKey, baseUrl = baseUrl, model = model))
                },
                label = { Text("模型") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ResonanceColors.Coral,
                    unfocusedBorderColor = ResonanceColors.Divider,
                    focusedContainerColor = ResonanceColors.Canvas,
                    unfocusedContainerColor = ResonanceColors.Canvas,
                    cursorColor = ResonanceColors.Coral,
                ),
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = {
                    baseUrl = it.trim()
                    onSaveDeepSeekConfig(DeepSeekConfig(apiKey = apiKey, baseUrl = baseUrl, model = model))
                },
                label = { Text("API 接口地址") },
                singleLine = true,
                modifier = Modifier.weight(1.3f),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ResonanceColors.Coral,
                    unfocusedBorderColor = ResonanceColors.Divider,
                    focusedContainerColor = ResonanceColors.Canvas,
                    unfocusedContainerColor = ResonanceColors.Canvas,
                    cursorColor = ResonanceColors.Coral,
                ),
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    testInProgress = true
                    testResult = null
                    coroutineScope.launch {
                        val config = DeepSeekConfig(apiKey = apiKey, baseUrl = baseUrl, model = model)
                        onSaveDeepSeekConfig(config)
                        testResult = onTestDeepSeek(config)
                        testInProgress = false
                    }
                },
                enabled = apiKey.isNotBlank() && !testInProgress,
                shape = RoundedCornerShape(8.dp),
            ) {
                if (testInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ResonanceColors.Coral, strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("正在测试…")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = ResonanceColors.Coral)
                    Spacer(Modifier.width(6.dp))
                    Text("测试 DeepSeek 连接", color = ResonanceColors.Coral)
                }
            }
        }

        testResult?.let { res ->
            Spacer(Modifier.height(6.dp))
            Surface(
                color = if (res.success) ResonanceColors.Soft else ResonanceColors.CoralSoft,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    res.message,
                    color = if (res.success) ResonanceColors.Ivory else ResonanceColors.Coral,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        SectionHeader("本地歌词与酷狗下载目录")
        Text(
            "桌面端自动识别同一目录下的 .krc 与 .lrc 歌词，以及酷狗默认下载歌词文件夹。",
            style = MaterialTheme.typography.bodySmall,
            color = ResonanceColors.Dim,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        OutlinedTextField(
            value = lyricsFolder,
            onValueChange = { lyricsFolder = it },
            label = { Text("本地歌词文件夹绝对路径") },
            placeholder = { Text("例如：D:\\Music\\Kugou\\Leonard\\Lyrics", color = ResonanceColors.Dim) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ResonanceColors.Coral,
                unfocusedBorderColor = ResonanceColors.Divider,
                focusedContainerColor = ResonanceColors.Canvas,
                unfocusedContainerColor = ResonanceColors.Canvas,
                cursorColor = ResonanceColors.Coral,
            ),
        )

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.material3.Button(
                onClick = {
                    onSaveCustomLyricsFolder(lyricsFolder.trim().takeIf(String::isNotBlank))
                    folderSavedMessage = "歌词目录已保存"
                },
                shape = RoundedCornerShape(8.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = ResonanceColors.Coral,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
            ) {
                Text("保存目录")
            }
            androidx.compose.material3.TextButton(
                onClick = {
                    lyricsFolder = "D:\\Music\\Kugou\\Leonard\\Lyrics"
                    onSaveCustomLyricsFolder(lyricsFolder)
                    folderSavedMessage = "已重置为默认酷狗歌词目录"
                },
            ) {
                Text("恢复默认路径", color = ResonanceColors.Muted)
            }
        }

        folderSavedMessage?.let { msg ->
            Spacer(Modifier.height(4.dp))
            Text(msg, style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Coral)
        }

        SectionHeader("音乐库")
        SettingRow(Icons.Default.LibraryMusic, "扫描音乐", "查找并导入本机音频文件", onClick = onOpenImport)
        SettingRow(Icons.Default.FolderOpen, "音乐库位置", libraryLocation, onClick = null)

        SectionHeader("播放与解码")
        SettingRow(Icons.Default.GraphicEq, "转换质量", "默认 MP3 320 kbps · 原 MP3 不重新编码", onClick = null)
        SettingRow(Icons.Default.MusicNote, "KRC / LRC 解密", "支持酷狗原生 KRC 解密、LRCLIB 自动匹配及本地多级模糊索引", onClick = null)
        SettingRow(Icons.Default.Shuffle, "播放与队列", "记住随机 / 循环模式和上次选中的歌单", onClick = null)

        SectionHeader("关于")
        SettingRow(Icons.Default.Info, "Resonance", "版本 v$APP_VERSION · 本地优先的跨平台音乐播放器", onClick = null)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ThemeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        if (selected) ResonanceColors.CoralSoft else ResonanceColors.Raised,
        animationSpec = tween(MotionQuick),
        label = "themeOptionBg",
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) ResonanceColors.Coral else ResonanceColors.Dim,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) ResonanceColors.Coral else ResonanceColors.Muted,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = ResonanceColors.Dim,
        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(clickableModifier)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = ResonanceColors.Muted, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = ResonanceColors.Ivory)
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ResonanceColors.Dim,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onClick != null) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ResonanceColors.Dimmer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        HorizontalDivider(color = ResonanceColors.Divider, thickness = Dp.Hairline)
    }
}

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

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClick = {
                        if (isMultiSelectMode) {
                            onToggleMultiSelect()
                        } else {
                            onClick()
                        }
                    },
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isMultiSelected,
                    onCheckedChange = { onToggleMultiSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ResonanceColors.Coral,
                        uncheckedColor = ResonanceColors.Dim,
                        checkmarkColor = Color.White,
                    ),
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            if (index != null && !isMultiSelectMode) {
                Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "%02d".format(index),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) ResonanceColors.Coral else ResonanceColors.Dim,
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
                    if (selected && !isMultiSelectMode) {
                        NowPlayingIndicator(isPlaying = isPlaying)
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(
                        track.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isMultiSelected || selected) ResonanceColors.Coral else ResonanceColors.Ivory,
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
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(38.dp)) {
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
                                    tint = if (track.isFavorite) ResonanceColors.Coral else ResonanceColors.Muted,
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
                                        tint = ResonanceColors.Ivory,
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
                                        { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ResonanceColors.Coral) }
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
                                text = { Text("删除本地音频文件", color = ResonanceColors.Coral) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ResonanceColors.Coral) },
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
private fun NowPlayingIndicator(isPlaying: Boolean) {
    if (!isPlaying) {
        Icon(
            Icons.Default.GraphicEq,
            contentDescription = "当前曲目",
            tint = ResonanceColors.Coral,
            modifier = Modifier.size(15.dp),
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "playingBars")
    val first by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(460), AnimationRepeatMode.Reverse),
        label = "playingBarOne",
    )
    val second by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(tween(620), AnimationRepeatMode.Reverse),
        label = "playingBarTwo",
    )
    val third by transition.animateFloat(
        initialValue = 0.48f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(540), AnimationRepeatMode.Reverse),
        label = "playingBarThree",
    )
    Row(
        modifier = Modifier.width(15.dp).height(15.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(first, second, third).forEach { heightFraction ->
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightFraction)
                    .clip(CircleShape)
                    .background(ResonanceColors.Coral),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Mini Player（扁平窄条）
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
    Column(Modifier.fillMaxWidth().background(ResonanceColors.CanvasElevated)) {
        LinearProgressIndicator(
            progress = { playerState.progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = ResonanceColors.Coral,
            trackColor = Color.Transparent,
            drawStopIndicator = {},
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clickable(role = Role.Button, onClick = onOpenNowPlaying)
                .semantics { contentDescription = "打开正在播放详情与歌词" }
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtwork(
                track.artworkSeed,
                Modifier.size(40.dp),
                8.dp,
                track.artworkPath,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = ResonanceColors.Ivory,
                )
                Text(
                    track.artist.ifBlank { "未知艺术家" },
                    style = MaterialTheme.typography.bodySmall,
                    color = ResonanceColors.Dim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!compact) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "上一首", tint = ResonanceColors.Muted)
                }
            }
            FilledIconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(38.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ResonanceColors.Coral,
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "下一首", tint = ResonanceColors.Muted)
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
            .background(ResonanceColors.Coral),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
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
        listOf(Color(0xFFE96B55), Color(0xFF6D2438), Color(0xFF151B23)),
        listOf(Color(0xFF5FD19B), Color(0xFF1C4D4A), Color(0xFF101821)),
        listOf(Color(0xFFF2BD5B), Color(0xFF81412D), Color(0xFF15131D)),
        listOf(Color(0xFF8EA7FF), Color(0xFF3D356B), Color(0xFF151824)),
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
