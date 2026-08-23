package com.resonance.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.resonanceGlass
import com.resonance.player.design.resonancePressable
import com.resonance.player.model.APP_VERSION
import com.resonance.player.model.LibraryDestination
import com.resonance.player.model.LyricsUiState
import com.resonance.player.model.PlayerState
import com.resonance.player.model.Playlist
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.ThemeMode
import com.resonance.player.model.Track

import com.resonance.player.platform.currentHourOfDay
import com.resonance.player.platform.decodeArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class WindowClass { Compact, Medium, Expanded }
private enum class LibraryCollection { Playlist, AllTracks, Favorites }

private data class DestinationItem(
    val destination: LibraryDestination,
    val label: String,
    val icon: ImageVector,
)

private val destinationItems = listOf(
    DestinationItem(LibraryDestination.Library, "音乐库", Icons.Default.Home),
    DestinationItem(LibraryDestination.Explore, "导入", Icons.Default.LibraryMusic),
    DestinationItem(LibraryDestination.Sync, "同步", Icons.Outlined.Sync),
    DestinationItem(LibraryDestination.Settings, "设置", Icons.Default.Settings),
)

private const val MotionQuick = 160
private const val MotionStandard = 280

private fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f,
    restingScale: Float = 1f,
): Modifier = this.resonancePressable(interactionSource, pressedScale, restingScale)


@Composable
private fun ResonanceBackdrop(
    seed: Int,
    energized: Boolean,
    modifier: Modifier = Modifier,
) {
    val isDark = ResonanceColors.isDark
    val coral = ResonanceColors.Coral
    val violet = ResonanceColors.Violet
    val violetSoft = ResonanceColors.VioletSoft
    val mint = ResonanceColors.Mint
    val azure = ResonanceColors.Azure
    val canvas = ResonanceColors.Canvas

    val energy by animateFloatAsState(
        targetValue = if (energized) 1f else 0.85f,
        animationSpec = tween(700),
        label = "backdropEnergy",
    )
    val transition = rememberInfiniteTransition(label = "backdropAuroraMotion")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = AnimationRepeatMode.Restart,
        ),
        label = "backdropAuroraAngle",
    )
    val rad = (angle * (kotlin.math.PI / 180.0)).toFloat()
    val cosOffset = kotlin.math.cos(rad)
    val sinOffset = kotlin.math.sin(rad)
    val phase = (((seed % 19) + 19) % 19) / 18f

    val bgGradient = if (isDark) {
        listOf(
            Color(0xFF090D16),
            canvas,
            Color(0xFF04060A),
        )
    } else {
        listOf(
            Color(0xFFF9FAFD),
            canvas,
            Color(0xFFE8EFF9),
        )
    }

    val coralAlpha = if (isDark) 0.26f * energy else 0.18f * energy
    val violetAlpha = if (isDark) 0.22f * energy else 0.16f * energy
    val mintAlpha = if (isDark) 0.14f * energy else 0.12f * energy
    val azureAlpha = if (isDark) 0.11f * energy else 0.10f * energy

    Canvas(
        modifier = modifier.background(
            Brush.verticalGradient(bgGradient),
        ),
    ) {
        val radius = size.maxDimension * 0.65f
        val coralCenter = Offset(
            size.width * (0.20f + phase * 0.12f) + cosOffset * (size.width * 0.08f),
            size.height * 0.10f + sinOffset * (size.height * 0.08f),
        )
        val violetCenter = Offset(
            size.width * (0.82f - phase * 0.10f) - cosOffset * (size.width * 0.08f),
            size.height * 0.45f - sinOffset * (size.height * 0.08f),
        )
        val mintCenter = Offset(
            size.width * 0.45f + sinOffset * (size.width * 0.06f),
            size.height * 0.88f - cosOffset * (size.height * 0.06f),
        )
        val azureCenter = Offset(
            size.width * 0.85f + cosOffset * (size.width * 0.05f),
            size.height * 0.90f + sinOffset * (size.height * 0.05f),
        )

        // Aurora Layer 1: Coral Vibrant Bloom
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    coral.copy(alpha = coralAlpha),
                    coral.copy(alpha = coralAlpha * 0.3f),
                    Color.Transparent,
                ),
                center = coralCenter,
                radius = radius,
            ),
            radius = radius,
            center = coralCenter,
        )
        // Aurora Layer 2: Violet Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    violet.copy(alpha = violetAlpha),
                    violetSoft.copy(alpha = violetAlpha * 0.3f),
                    Color.Transparent,
                ),
                center = violetCenter,
                radius = radius * 0.9f,
            ),
            radius = radius * 0.9f,
            center = violetCenter,
        )
        // Aurora Layer 3: Mint Electric Floor
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(mint.copy(alpha = mintAlpha), Color.Transparent),
                center = mintCenter,
                radius = size.minDimension * 0.55f,
            ),
            radius = size.minDimension * 0.55f,
            center = mintCenter,
        )
        // Aurora Layer 4: Azure Accent
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(azure.copy(alpha = azureAlpha), Color.Transparent),
                center = azureCenter,
                radius = size.minDimension * 0.42f,
            ),
            radius = size.minDimension * 0.42f,
            center = azureCenter,
        )
    }
}





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
    message: String?,
    operationInProgress: Boolean,
) {
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowClass = when {
            maxWidth < 720.dp -> WindowClass.Compact
            maxWidth < 1100.dp -> WindowClass.Medium
            else -> WindowClass.Expanded
        }
        val activeSeed = playerState.currentTrack?.artworkSeed
            ?: playlists.firstOrNull { it.id == selectedPlaylistId }?.artworkSeed
            ?: 0

        ResonanceBackdrop(
            seed = activeSeed,
            energized = playerState.isPlaying,
            modifier = Modifier.matchParentSize(),
        )

        when (windowClass) {
            WindowClass.Compact -> CompactShell(
                destination = destination,
                onDestinationChange = onDestinationChange,
                playlists = playlists,
                libraryTracks = libraryTracks,
                playerState = playerState,
                onTogglePlay = onTogglePlay,
                onPrevious = onPrevious,
                onNext = onNext,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onSeek = onSeek,
                onTrackSelected = onTrackSelected,
                selectedPlaylistId = selectedPlaylistId,
                userPlaylists = userPlaylists,
                onPlaylistSelected = onPlaylistSelected,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onRenamePlaylist = onRenamePlaylist,
                onDeletePlaylist = onDeletePlaylist,
                onDeleteLocalTrack = onDeleteLocalTrack,
                onToggleFavorite = onToggleFavorite,
                onOpenNowPlaying = { showNowPlaying = true },
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
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                message = message,
                operationInProgress = operationInProgress,
            )

            WindowClass.Medium -> WideShell(
                compactRail = true,
                destination = destination,
                onDestinationChange = onDestinationChange,
                playlists = playlists,
                libraryTracks = libraryTracks,
                playerState = playerState,
                onTogglePlay = onTogglePlay,
                onPrevious = onPrevious,
                onNext = onNext,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onSeek = onSeek,
                onTrackSelected = onTrackSelected,
                selectedPlaylistId = selectedPlaylistId,
                userPlaylists = userPlaylists,
                onPlaylistSelected = onPlaylistSelected,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onRenamePlaylist = onRenamePlaylist,
                onDeletePlaylist = onDeletePlaylist,
                onDeleteLocalTrack = onDeleteLocalTrack,
                onToggleFavorite = onToggleFavorite,
                onOpenNowPlaying = { showNowPlaying = true },
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
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                message = message,
                operationInProgress = operationInProgress,
            )

            WindowClass.Expanded -> WideShell(
                compactRail = false,
                destination = destination,
                onDestinationChange = onDestinationChange,
                playlists = playlists,
                libraryTracks = libraryTracks,
                playerState = playerState,
                onTogglePlay = onTogglePlay,
                onPrevious = onPrevious,
                onNext = onNext,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onSeek = onSeek,
                onTrackSelected = onTrackSelected,
                selectedPlaylistId = selectedPlaylistId,
                userPlaylists = userPlaylists,
                onPlaylistSelected = onPlaylistSelected,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onRenamePlaylist = onRenamePlaylist,
                onDeletePlaylist = onDeletePlaylist,
                onDeleteLocalTrack = onDeleteLocalTrack,
                onToggleFavorite = onToggleFavorite,
                onOpenNowPlaying = { showNowPlaying = true },
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
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                message = message,
                operationInProgress = operationInProgress,
            )
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
                onTrackSelected = onTrackSelected,
                onToggleFavorite = onToggleFavorite,
                onRefreshLyrics = onRefreshLyrics,
            )
        }
    }
}

@Composable
private fun CompactShell(
    destination: LibraryDestination,
    onDestinationChange: (LibraryDestination) -> Unit,
    playlists: List<Playlist>,
    libraryTracks: List<Track>,
    playerState: PlayerState,
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
    onOpenNowPlaying: () -> Unit,
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
    message: String?,
    operationInProgress: Boolean,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            DestinationContent(
                destination = destination,
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
                compact = true,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                message = message,
                operationInProgress = operationInProgress,
            )
        }
        if (playerState.currentTrack != null) {
            MiniPlayer(
                playerState,
                onTogglePlay,
                onPrevious,
                onNext,
                onToggleShuffle,
                onCycleRepeat,
                onSeek,
                onOpenNowPlaying,
                compact = true,
                horizontalPadding = 8.dp,
            )
        }
        NavigationBar(
            containerColor = ResonanceColors.Glass,
            tonalElevation = 0.dp,
            modifier = Modifier
                .heightIn(min = 74.dp)
                .resonanceGlass(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                    backgroundColor = ResonanceColors.Glass,
                    shadowElevation = 10.dp,
                ),
        ) {
            destinationItems.forEach { item ->
                val selected = destination == item.destination
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.15f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "bottomNavIcon",
                )
                NavigationBarItem(
                    selected = selected,
                    onClick = { onDestinationChange(item.destination) },
                    icon = {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (selected) ResonanceColors.CoralGlow else ResonanceColors.Dim,
                            modifier = Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                        )
                    },
                    label = {
                        Text(
                            item.label,
                            color = if (selected) ResonanceColors.Ivory else ResonanceColors.Dim,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun WideShell(
    compactRail: Boolean,
    destination: LibraryDestination,
    onDestinationChange: (LibraryDestination) -> Unit,
    playlists: List<Playlist>,
    libraryTracks: List<Track>,
    playerState: PlayerState,
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
    onOpenNowPlaying: () -> Unit,
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
    message: String?,
    operationInProgress: Boolean,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        if (compactRail) {
            NavigationRail(
                containerColor = ResonanceColors.Glass,
                header = {
                    BrandMark(modifier = Modifier.padding(vertical = 20.dp))
                },
            ) {
                destinationItems.forEach { item ->
                    val selected = destination == item.destination
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.12f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "railIcon",
                    )
                    NavigationRailItem(
                        selected = selected,
                        onClick = { onDestinationChange(item.destination) },
                        icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }) },
                        label = { Text(item.label) },
                    )
                }
            }
        } else {
            DesktopSidebar(
                destination = destination,
                onDestinationChange = onDestinationChange,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
            )
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Box(modifier = Modifier.weight(1f)) {
                DestinationContent(
                    destination = destination,
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
                    compact = compactRail,
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    message = message,
                    operationInProgress = operationInProgress,
                )
            }
            if (playerState.currentTrack != null) {
                MiniPlayer(
                    playerState,
                    onTogglePlay,
                    onPrevious,
                    onNext,
                    onToggleShuffle,
                    onCycleRepeat,
                    onSeek,
                    onOpenNowPlaying,
                    compact = false,
                    horizontalPadding = 16.dp,
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
    val sidebarBg = if (isDark) {
        listOf(
            Color(0xFF131826),
            ResonanceColors.Raised,
            Color(0xFF090D14),
        )
    } else {
        listOf(
            Color(0xFFFFFFFF),
            ResonanceColors.Canvas,
            Color(0xFFF0F4FA),
        )
    }

    Column(
        modifier = Modifier
            .width(236.dp)
            .fillMaxHeight()
            .background(Brush.verticalGradient(sidebarBg))
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(ResonanceColors.GlassBorder, ResonanceColors.DividerStrong.copy(alpha = 0.35f)),
                    ),
                ),
            )
            .padding(horizontal = 18.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("RESONANCE", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text("LOCAL MUSIC", style = MaterialTheme.typography.labelMedium, color = ResonanceColors.Mint)
            }
            val nextMode = when (themeMode) {
                ThemeMode.Dark -> ThemeMode.Light
                ThemeMode.Light -> ThemeMode.Dark
                ThemeMode.System -> if (isDark) ThemeMode.Light else ThemeMode.Dark
            }
            IconButton(
                onClick = { onThemeModeChange(nextMode) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "切换主题模式",
                    tint = ResonanceColors.CoralGlow,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(34.dp))
        destinationItems.forEach { item ->
            SidebarDestination(
                item = item,
                selected = destination == item.destination,
                onClick = { onDestinationChange(item.destination) },
            )
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .resonanceGlass(
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = ResonanceColors.GlassUltra,
                    shadowElevation = 4.dp,
                ),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(ResonanceColors.Mint))
                    Spacer(Modifier.width(8.dp))
                    Text("本地优先", style = MaterialTheme.typography.labelLarge, color = ResonanceColors.Ivory)
                }
                Spacer(Modifier.height(6.dp))
                Text("音乐只在你的设备之间安全流动", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
            }
        }
    }
}




@Composable
private fun SidebarDestination(item: DestinationItem, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        if (selected) ResonanceColors.CoralSoft.copy(alpha = 0.95f) else Color.Transparent,
        animationSpec = tween(MotionQuick),
        label = "sidebarBackground",
    )
    val foreground by animateColorAsState(
        if (selected) ResonanceColors.CoralGlow else ResonanceColors.Muted,
        animationSpec = tween(MotionQuick),
        label = "sidebarForeground",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource, pressedScale = 0.975f)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = if (selected) ResonanceColors.Coral.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (selected) {
            Box(
                Modifier
                    .width(3.5.dp)
                    .height(18.dp)
                    .clip(CircleShape)
                    .background(ResonanceColors.Coral),
            )
            Spacer(Modifier.width(9.dp))
        }
        Icon(item.icon, contentDescription = null, tint = foreground, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) ResonanceColors.Ivory else foreground,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}


@Composable
private fun DestinationContent(
    destination: LibraryDestination,
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
    message: String?,
    operationInProgress: Boolean,
) {
    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            val movingForward = targetState.ordinal >= initialState.ordinal
            val enter = fadeIn(tween(MotionStandard)) + slideInHorizontally(
                animationSpec = tween(MotionStandard),
                initialOffsetX = { width -> if (movingForward) width / 12 else -width / 12 },
            )
            val exit = fadeOut(tween(MotionQuick)) + slideOutHorizontally(
                animationSpec = tween(MotionQuick),
                targetOffsetX = { width -> if (movingForward) -width / 18 else width / 18 },
            )
            enter togetherWith exit using SizeTransform(clip = false)
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
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            message = message,
            operationInProgress = operationInProgress,
        )
        LibraryDestination.Explore -> ImportScreen(
            compact = compact,
            onImport = { onImport(false) },
            onConvert = { onImport(true) },
            onImportPlaylist = onImportPlaylist,
            message = message,
            operationInProgress = operationInProgress,
        )
        LibraryDestination.Sync -> SyncScreen(
            compact = compact,
            onExportSync = onExportSync,
            onImportSync = onImportSync,
            onStartLanSync = onStartLanSync,
            lanQrPath = lanQrPath,
            message = message,
        )
        LibraryDestination.Settings -> SettingsScreen(
            compact = compact,
            libraryLocation = libraryLocation,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
        )
        }
    }
}

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
    themeMode: ThemeMode = ThemeMode.Dark,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    message: String?,
    operationInProgress: Boolean,
) {
    if (playlists.isEmpty() && libraryTracks.isEmpty()) {
        if (operationInProgress) LibraryLoading() else EmptyLibrary(onCreatePlaylist, { onImport(false) }, compact, message)
        return
    }

    val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId } ?: playlists.firstOrNull()
    var collection by remember { mutableStateOf(if (selectedPlaylist == null) LibraryCollection.AllTracks else LibraryCollection.Playlist) }
    val activeCollection = if (collection == LibraryCollection.Playlist && selectedPlaylist == null) {
        LibraryCollection.AllTracks
    } else {
        collection
    }
    var showRenameDialog by remember(selectedPlaylist?.id) { mutableStateOf(false) }
    var showDeleteDialog by remember(selectedPlaylist?.id) { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val trimmedQuery = searchQuery.trim()
    val allKnownTracks = (libraryTracks + playlists.flatMap(Playlist::tracks)).distinctBy(Track::id)
    val searchResults = if (trimmedQuery.isEmpty()) {
        emptyList()
    } else {
        allKnownTracks
            .filter { track ->
                track.title.contains(trimmedQuery, ignoreCase = true) ||
                    track.artist.contains(trimmedQuery, ignoreCase = true) ||
                    track.album.contains(trimmedQuery, ignoreCase = true)
            }
    }
    val collectionTracks = when (activeCollection) {
        LibraryCollection.Playlist -> selectedPlaylist?.tracks.orEmpty()
        LibraryCollection.AllTracks -> libraryTracks
        LibraryCollection.Favorites -> allKnownTracks.filter(Track::isFavorite)
    }
    val collectionTitle = when (activeCollection) {
        LibraryCollection.Playlist -> selectedPlaylist?.name.orEmpty()
        LibraryCollection.AllTracks -> "全部歌曲"
        LibraryCollection.Favorites -> "我的收藏"
    }
    val collectionSubtitle = when (activeCollection) {
        LibraryCollection.Playlist -> selectedPlaylist?.subtitle.orEmpty()
        LibraryCollection.AllTracks -> "${collectionTracks.size} 首 · 本机可播放"
        LibraryCollection.Favorites -> "${collectionTracks.size} 首 · 随时回来听"
    }
    val collectionSeed = when (activeCollection) {
        LibraryCollection.Playlist -> selectedPlaylist?.artworkSeed ?: 0
        LibraryCollection.AllTracks -> libraryTracks.firstOrNull()?.artworkSeed ?: 4
        LibraryCollection.Favorites -> collectionTracks.firstOrNull()?.artworkSeed ?: 9
    }
    val pagePadding = if (compact) 20.dp else 36.dp
    LazyColumn(
        contentPadding = PaddingValues(start = pagePadding, end = pagePadding, top = 24.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            LibraryHeader(
                compact = compact,
                previewMode = previewMode,
                onExitPreview = onExitPreview,
                onCreatePlaylist = onCreatePlaylist,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
            )
            Spacer(Modifier.height(if (compact) 16.dp else 20.dp))
            LibrarySearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
            Spacer(Modifier.height(if (compact) 20.dp else 24.dp))
        }

        if (message != null) {
            item {
                Surface(
                    color = ResonanceColors.CoralSoft.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ResonanceColors.Coral.copy(alpha = 0.24f)),
                    shadowElevation = 2.dp,
                ) {
                    Text(message, color = ResonanceColors.Coral, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                }
                Spacer(Modifier.height(18.dp))
            }
        }
        if (trimmedQuery.isEmpty()) {
            item {
                Text("快速访问", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(14.dp))
                LibraryQuickAccess(
                    allTrackCount = libraryTracks.size,
                    favoriteCount = allKnownTracks.count(Track::isFavorite),
                    selected = activeCollection,
                    onSelect = { collection = it },
                )
                Spacer(Modifier.height(if (compact) 26.dp else 32.dp))
            }
            if (playlists.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("你的歌单", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            color = ResonanceColors.Soft,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, ResonanceColors.Divider),
                        ) {
                            Text(
                                playlists.size.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = ResonanceColors.Muted,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    PlaylistStrip(
                        playlists = playlists,
                        compact = compact,
                        selectedPlaylistId = if (activeCollection == LibraryCollection.Playlist) selectedPlaylist?.id else null,
                        onPlaylistSelected = { playlistId ->
                            collection = LibraryCollection.Playlist
                            onPlaylistSelected(playlistId)
                        },
                    )
                    Spacer(Modifier.height(if (compact) 30.dp else 38.dp))
                }
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ResonanceColors.Glass,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, ResonanceColors.Divider.copy(alpha = 0.9f)),
                    shadowElevation = 6.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlbumArtwork(
                            seed = collectionSeed,
                            modifier = Modifier.size(if (compact) 58.dp else 68.dp).shadow(8.dp, RoundedCornerShape(15.dp)),
                            cornerRadius = 15.dp,
                            artworkPath = collectionTracks.firstNotNullOfOrNull(Track::artworkPath),
                        )
                        Spacer(Modifier.width(15.dp))
                        Column(Modifier.weight(1f)) {
                            Text(collectionTitle, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Text(collectionSubtitle, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
                        }
                        if (!previewMode && activeCollection == LibraryCollection.Playlist && selectedPlaylist != null) {
                            IconButton(onClick = { showRenameDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "重命名歌单", tint = ResonanceColors.Muted)
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除歌单", tint = ResonanceColors.Muted)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            if (collectionTracks.isEmpty()) {
                item {
                    EmptyPlaylistNotice(
                        canAddMusic = activeCollection == LibraryCollection.Playlist &&
                            userPlaylists.any { it.id != selectedPlaylist?.id && it.tracks.isNotEmpty() },
                        onImport = { onImport(false) },
                    )
                }
            }
            itemsIndexed(collectionTracks, key = { index, track -> "${activeCollection.name}-${track.id}-$index" }) { _, track ->
                TrackRow(
                    track = track,
                    selected = playerState.currentTrack?.id == track.id,
                    isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                    onClick = { onTrackSelected(track) },
                    compact = compact,
                    userPlaylists = userPlaylists,
                    onPlaylistMembershipChange = onPlaylistMembershipChange,
                    onDeleteLocalTrack = onDeleteLocalTrack,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.animateItem(),
                )
            }
        } else {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("搜索结果", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        color = ResonanceColors.Soft,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, ResonanceColors.Divider),
                    ) {
                        Text(
                            searchResults.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = ResonanceColors.Muted,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            if (searchResults.isEmpty()) {
                item {
                    Surface(
                        color = ResonanceColors.Soft,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(22.dp)) {
                            Text("没有找到「$trimmedQuery」", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "换个关键词试试，支持歌名、歌手和专辑的模糊匹配。",
                                color = ResonanceColors.Muted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            itemsIndexed(searchResults, key = { index, track -> "search-${track.id}-$index" }) { _, track ->
                TrackRow(
                    track = track,
                    selected = playerState.currentTrack?.id == track.id,
                    isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                    onClick = { onTrackSelected(track) },
                    compact = compact,
                    userPlaylists = userPlaylists,
                    onPlaylistMembershipChange = onPlaylistMembershipChange,
                    onDeleteLocalTrack = onDeleteLocalTrack,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    if (showRenameDialog && selectedPlaylist != null) {
        RenamePlaylistDialog(
            currentName = selectedPlaylist.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { name ->
                onRenamePlaylist(selectedPlaylist.id, name)
                showRenameDialog = false
            },
        )
    }
    if (showDeleteDialog && selectedPlaylist != null) {
        DeletePlaylistDialog(
            playlistName = selectedPlaylist.name,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                onDeletePlaylist(selectedPlaylist.id)
                showDeleteDialog = false
            },
        )
    }
}

@Composable
private fun LibraryLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            color = ResonanceColors.Glass,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, ResonanceColors.Divider),
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = ResonanceColors.Coral, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(16.dp))
                Text("正在整理音乐库…", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("读取曲目、封面与歌单", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    compact: Boolean,
    previewMode: Boolean,
    onExitPreview: () -> Unit,
    onCreatePlaylist: () -> Unit,
    themeMode: ThemeMode = ThemeMode.Dark,
    onThemeModeChange: (ThemeMode) -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val lowProfile = compact && maxWidth > 500.dp
        val shape = RoundedCornerShape(if (compact) 24.dp else 30.dp)
        val addInteraction = remember { MutableInteractionSource() }
        val greeting = when (currentHourOfDay()) {
            in 5..10 -> "早上好"
            in 11..13 -> "中午好"
            in 14..18 -> "下午好"
            else -> "晚上好"
        }
        val isDark = ResonanceColors.isDark
        val coral = ResonanceColors.Coral
        val violet = ResonanceColors.Violet
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .resonanceGlass(
                    shape = shape,
                    backgroundColor = ResonanceColors.Glass,
                    borderColors = listOf(ResonanceColors.GlassBorderGlow, ResonanceColors.GlassBorder),
                    shadowElevation = 14.dp,
                ),
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(coral.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.10f),
                        radius = size.maxDimension * 0.65f,
                    ),
                    radius = size.maxDimension * 0.65f,
                    center = Offset(size.width * 0.88f, size.height * 0.10f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(violet.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.10f, size.height * 0.95f),
                        radius = size.minDimension * 0.55f,
                    ),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.10f, size.height * 0.95f),
                )
            }

            Row(
                modifier = Modifier.padding(horizontal = if (compact) 20.dp else 28.dp, vertical = if (compact) 20.dp else 26.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ResonanceColors.CoralSoft)
                                .border(1.dp, ResonanceColors.Coral.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "✦ RESONANCE 0.2.0 · HIFI",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 0.8.sp),
                                color = ResonanceColors.CoralGlow,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("· $greeting", style = MaterialTheme.typography.labelMedium, color = ResonanceColors.Muted)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (lowProfile) "你的音乐，留在身边。" else "你的音乐，\n留在身边。",
                        style = when {
                            lowProfile -> MaterialTheme.typography.headlineLarge
                            compact -> MaterialTheme.typography.displaySmall
                            else -> MaterialTheme.typography.displayLarge
                        },
                        color = ResonanceColors.Ivory,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                    if (!compact) {
                        Spacer(Modifier.height(10.dp))
                        Text("本地优先 · 无损音质 · 跨设备自由同步", style = MaterialTheme.typography.bodyLarge, color = ResonanceColors.Muted)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val nextMode = when (themeMode) {
                            ThemeMode.Dark -> ThemeMode.Light
                            ThemeMode.Light -> ThemeMode.Dark
                            ThemeMode.System -> if (isDark) ThemeMode.Light else ThemeMode.Dark
                        }
                        IconButton(
                            onClick = { onThemeModeChange(nextMode) },
                            modifier = Modifier.size(46.dp),
                        ) {
                            Icon(
                                if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "切换明暗主题",
                                tint = ResonanceColors.CoralGlow,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        FilledIconButton(
                            onClick = onCreatePlaylist,
                            interactionSource = addInteraction,
                            modifier = Modifier
                                .size(54.dp)
                                .pressScale(addInteraction, pressedScale = 0.88f)
                                .shadow(12.dp, CircleShape, ambientColor = ResonanceColors.Coral.copy(alpha = 0.5f), spotColor = ResonanceColors.CoralGlow),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = ResonanceColors.Coral,
                                contentColor = Color(0xFF2A0B07),
                            ),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "新建歌单", modifier = Modifier.size(26.dp))
                        }
                    }
                    if (previewMode) {
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = onExitPreview) { Text("清空预览", color = ResonanceColors.Muted) }
                    }
                }
            }
        }
    }
}


@Composable
private fun LibraryQuickAccess(
    allTrackCount: Int,
    favoriteCount: Int,
    selected: LibraryCollection,
    onSelect: (LibraryCollection) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            QuickAccessCard(
                title = "全部歌曲",
                subtitle = "$allTrackCount 首本地音乐",
                icon = Icons.Default.Headphones,
                selected = selected == LibraryCollection.AllTracks,
                isMint = true,
                onClick = { onSelect(LibraryCollection.AllTracks) },
            )
        }
        item {
            QuickAccessCard(
                title = "我的收藏",
                subtitle = "$favoriteCount 首喜欢的歌",
                icon = Icons.Default.Favorite,
                selected = selected == LibraryCollection.Favorites,
                isMint = false,
                onClick = { onSelect(LibraryCollection.Favorites) },
            )
        }
    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    isMint: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val accentColor = if (isMint) ResonanceColors.Mint else ResonanceColors.Coral
    val accentSoft = if (isMint) ResonanceColors.MintSoft else ResonanceColors.CoralSoft
    val accentGlow = if (isMint) ResonanceColors.MintGlow else ResonanceColors.CoralGlow

    val container by animateColorAsState(
        targetValue = if (selected) accentSoft.copy(alpha = 0.95f) else ResonanceColors.Glass,
        animationSpec = tween(MotionQuick),
        label = "quickAccessContainer",
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) 10.dp else 3.dp,
        animationSpec = tween(MotionQuick),
        label = "quickAccessElevation",
    )

    Box(
        modifier = Modifier
            .widthIn(min = 168.dp)
            .heightIn(min = 84.dp)
            .pressScale(interactionSource, pressedScale = 0.97f)
            .resonanceGlass(
                shape = RoundedCornerShape(20.dp),
                backgroundColor = container,
                borderColors = if (selected) listOf(accentColor.copy(alpha = 0.7f), accentColor.copy(alpha = 0.25f)) else listOf(ResonanceColors.GlassBorder, ResonanceColors.GlassBorderSubtle),
                shadowElevation = elevation,
                shadowColor = if (selected) accentColor.copy(alpha = 0.35f) else ResonanceColors.Shadow,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = androidx.compose.ui.semantics.Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentSoft)
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accentGlow, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = ResonanceColors.Ivory, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = if (selected) accentGlow else ResonanceColors.Muted)
            }
        }
    }
}


@Composable
private fun EmptyLibrary(onCreatePlaylist: () -> Unit, onImport: () -> Unit, compact: Boolean, message: String?) {
    if (compact) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            if (message != null) {
                EmptyLibraryMessage(message)
                Spacer(Modifier.height(18.dp))
            }
            EmptyLibraryArtwork(size = 164.dp)
            Spacer(Modifier.height(24.dp))
            Text("从第一首歌开始", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(10.dp))
            Text(
                "扫描本机音乐，或新建一个歌单。原文件不会被覆盖，转换后的 MP3 将整齐收进你的音乐库。",
                style = MaterialTheme.typography.bodyLarge,
                color = ResonanceColors.Muted,
            )
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryAction("扫描或导入音乐", Icons.Default.FolderOpen, onImport, Modifier.fillMaxWidth())
                SecondaryAction("新建空歌单", Icons.Default.Add, onCreatePlaylist, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "LOCAL · PRIVATE · YOURS",
                style = MaterialTheme.typography.labelMedium,
                color = ResonanceColors.Dim,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        return
    }
    Box(modifier = Modifier.fillMaxSize().padding(if (compact) 24.dp else 48.dp)) {
        Column(
            modifier = Modifier.align(Alignment.Center).widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (message != null) {
                EmptyLibraryMessage(message)
                Spacer(Modifier.height(20.dp))
            }
            EmptyLibraryArtwork(size = 232.dp)
            Spacer(Modifier.height(30.dp))
            Text(
                "从第一首歌开始",
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "扫描本机音乐，或新建一个歌单。原文件不会被覆盖，转换后的 MP3 将整齐收进你的音乐库。",
                style = MaterialTheme.typography.bodyLarge,
                color = ResonanceColors.Muted,
                modifier = Modifier.widthIn(max = 500.dp),
            )
            Spacer(Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryAction("扫描或导入音乐", Icons.Default.FolderOpen, onImport)
                SecondaryAction("新建空歌单", Icons.Default.Add, onCreatePlaylist)
            }
        }
        Text(
            "LOCAL · PRIVATE · YOURS",
            style = MaterialTheme.typography.labelMedium,
            color = ResonanceColors.Dim,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun EmptyLibraryMessage(message: String) {
    Surface(
        color = ResonanceColors.CoralSoft,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ResonanceColors.Coral.copy(alpha = 0.35f)),
    ) {
        Text(
            message,
            color = ResonanceColors.CoralGlow,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
        )
    }
}

@Composable
private fun PrimaryAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
        modifier = modifier
            .heightIn(min = 52.dp)
            .pressScale(interactionSource)
            .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = ResonanceColors.Shadow, spotColor = ResonanceColors.Shadow),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(9.dp))
        Text(label)
    }
}

@Composable
private fun SecondaryAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ResonanceColors.Soft, contentColor = ResonanceColors.Ivory),
        border = BorderStroke(1.dp, ResonanceColors.Divider),
        modifier = modifier.heightIn(min = 52.dp).pressScale(interactionSource),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(9.dp))
        Text(label)
    }
}

@Composable
private fun PlaylistStrip(
    playlists: List<Playlist>,
    compact: Boolean,
    selectedPlaylistId: String?,
    onPlaylistSelected: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(playlists, key = { it.id }) { playlist ->
            val selected = playlist.id == selectedPlaylistId
            val interactionSource = remember { MutableInteractionSource() }
            val borderColor by animateColorAsState(
                targetValue = if (selected) ResonanceColors.Coral.copy(alpha = 0.7f) else ResonanceColors.GlassBorderSubtle,
                animationSpec = tween(MotionStandard),
                label = "playlistBorder",
            )
            val titleColor by animateColorAsState(
                targetValue = if (selected) ResonanceColors.CoralGlow else ResonanceColors.Ivory,
                animationSpec = tween(MotionStandard),
                label = "playlistTitle",
            )
            val elevation by animateDpAsState(
                targetValue = if (selected) 12.dp else 4.dp,
                animationSpec = tween(MotionStandard),
                label = "playlistElevation",
            )
            Column(
                modifier = Modifier
                    .width(if (compact) 160.dp else 192.dp)
                    .pressScale(
                        interactionSource = interactionSource,
                        pressedScale = 0.955f,
                        restingScale = if (selected) 1.02f else 1f,
                    )
                    .shadow(
                        elevation = elevation,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = if (selected) ResonanceColors.Coral.copy(alpha = 0.35f) else ResonanceColors.Shadow,
                        spotColor = if (selected) ResonanceColors.CoralGlow else ResonanceColors.Shadow,
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            if (selected) {
                                listOf(Color(0xE62A1D2A), Color(0xCC161422))
                            } else {
                                listOf(Color(0xD9182030), Color(0xB3101624))
                            },
                        ),
                    )
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(24.dp),
                    )
                    .semantics { this.selected = selected }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Tab,
                    ) { onPlaylistSelected(playlist.id) }
                    .padding(10.dp),
            ) {
                Box {
                    AlbumArtwork(
                        seed = playlist.artworkSeed,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        cornerRadius = 18.dp,
                        artworkPath = playlist.tracks.firstNotNullOfOrNull(Track::artworkPath),
                    )
                    if (selected) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                            color = ResonanceColors.CoralSoft,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, ResonanceColors.Coral.copy(alpha = 0.4f)),
                        ) {
                            Text(
                                "当前歌单",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                color = ResonanceColors.CoralGlow,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    playlist.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ResonanceColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun LibrarySearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("搜索歌名、歌手或专辑", color = ResonanceColors.Dim) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ResonanceColors.CoralGlow) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "清空搜索", tint = ResonanceColors.Muted)
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ResonanceColors.Coral.copy(alpha = 0.65f),
            unfocusedBorderColor = ResonanceColors.GlassBorderSubtle,
            focusedContainerColor = ResonanceColors.Glass,
            unfocusedContainerColor = ResonanceColors.Glass,
            cursorColor = ResonanceColors.Coral,
        ),
    )
}

@Composable
private fun TrackRow(
    track: Track,
    selected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    compact: Boolean,
    userPlaylists: List<Playlist>,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    val available = track.sourceUri != null
    var menuExpanded by remember(track.id) { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFlac = track.mimeType == "audio/flac" || track.title.endsWith(".flac", ignoreCase = true)

    val background by animateColorAsState(
        if (selected) ResonanceColors.CoralSoft.copy(alpha = 0.92f) else ResonanceColors.GlassLight.copy(alpha = 0.20f),
        animationSpec = tween(MotionQuick),
        label = "trackBackground",
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) 6.dp else 0.dp,
        animationSpec = tween(MotionQuick),
        label = "trackElevation",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp)
            .pressScale(interactionSource, pressedScale = 0.988f)
            .shadow(elevation, RoundedCornerShape(16.dp), ambientColor = if (selected) ResonanceColors.Coral.copy(alpha = 0.3f) else ResonanceColors.Shadow, spotColor = ResonanceColors.Shadow)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    if (selected) listOf(ResonanceColors.Coral.copy(alpha = 0.6f), ResonanceColors.Coral.copy(alpha = 0.2f))
                    else listOf(ResonanceColors.GlassBorderSubtle, Color.Transparent),
                ),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArtwork(
            track.artworkSeed,
            Modifier
                .size(if (compact) 52.dp else 56.dp)
                .shadow(6.dp, RoundedCornerShape(13.dp)),
            13.dp,
            track.artworkPath,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selected) {
                    NowPlayingIndicator(isPlaying = isPlaying)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) ResonanceColors.CoralGlow else ResonanceColors.Ivory,
                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                if (!available) {
                    Surface(color = ResonanceColors.Soft, shape = RoundedCornerShape(6.dp)) {
                        Text("待匹配", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                } else {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isFlac) ResonanceColors.VioletSoft else ResonanceColors.SurfaceSubtle)
                            .border(0.5.dp, if (isFlac) ResonanceColors.Violet.copy(alpha = 0.4f) else ResonanceColors.GlassBorderSubtle, RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            if (isFlac) "FLAC" else "320K",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                            color = if (isFlac) ResonanceColors.VioletGlow else ResonanceColors.Mint,
                        )
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                if (compact) track.artist else "${track.artist}  ·  ${track.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = ResonanceColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = { onToggleFavorite(track) }, modifier = Modifier.size(46.dp)) {
            Icon(
                if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (track.isFavorite) "取消收藏" else "收藏",
                tint = if (track.isFavorite) ResonanceColors.Coral else ResonanceColors.Dim,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(track.durationText, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Dim)
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "歌单选项", tint = ResonanceColors.Muted)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
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
                if (available && track.mimeType == "audio/mpeg") {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("删除本地 MP3", color = ResonanceColors.Coral) },
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


@Composable
private fun NowPlayingIndicator(isPlaying: Boolean) {
    if (!isPlaying) {
        Icon(
            Icons.Default.GraphicEq,
            contentDescription = "当前曲目",
            tint = ResonanceColors.Coral,
            modifier = Modifier.size(16.dp),
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
        modifier = Modifier.width(16.dp).height(16.dp),
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

@Composable
private fun EmptyPlaylistNotice(canAddMusic: Boolean, onImport: () -> Unit) {
    Surface(
        color = ResonanceColors.Soft,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(22.dp)) {
            Text("这个歌单还没有歌曲", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                if (canAddMusic) "打开其他歌单的歌曲菜单，即可加入这里。" else "导入公开歌单并匹配本机音乐，或先扫描本机曲库。",
                color = ResonanceColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onImport) { Text("导入音乐") }
        }
    }
}

@Composable
private fun MiniPlayer(
    playerState: PlayerState,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSeek: (Float) -> Unit,
    onOpenNowPlaying: () -> Unit,
    compact: Boolean,
    horizontalPadding: Dp,
) {
    val track = playerState.currentTrack ?: return
    val shuffleInteraction = remember { MutableInteractionSource() }
    val previousInteraction = remember { MutableInteractionSource() }
    val playInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val repeatInteraction = remember { MutableInteractionSource() }
    val artworkScale by animateFloatAsState(
        targetValue = if (playerState.isPlaying) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "miniArtworkScale",
    )
    Column(
        modifier = Modifier
            .padding(horizontal = horizontalPadding, vertical = 8.dp)
            .fillMaxWidth()
            .resonanceGlass(
                shape = RoundedCornerShape(22.dp),
                backgroundColor = ResonanceColors.Glass,
                borderColors = listOf(ResonanceColors.Coral.copy(alpha = 0.35f), ResonanceColors.GlassBorderSubtle),
                shadowElevation = 12.dp,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClick = onOpenNowPlaying)
                    .semantics { contentDescription = "打开正在播放详情与歌词" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArtwork(
                    track.artworkSeed,
                    Modifier
                        .size(52.dp)
                        .graphicsLayer { scaleX = artworkScale; scaleY = artworkScale }
                        .shadow(10.dp, RoundedCornerShape(14.dp), ambientColor = ResonanceColors.Coral.copy(alpha = 0.3f), spotColor = ResonanceColors.Shadow),
                    14.dp,
                    track.artworkPath,
                )
                Spacer(Modifier.width(13.dp))
                AnimatedContent(
                    targetState = track,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        (fadeIn(tween(MotionStandard)) + slideInVertically(tween(MotionStandard)) { it / 3 }) togetherWith
                            (fadeOut(tween(MotionQuick)) + slideOutVertically(tween(MotionQuick)) { -it / 3 })
                    },
                    contentKey = Track::id,
                    label = "miniTrack",
                ) { activeTrack ->
                    Column {
                        Text(
                            activeTrack.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = ResonanceColors.Ivory,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            activeTrack.artist.ifBlank { "未知歌手" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = ResonanceColors.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (!compact) {
                IconButton(
                    onClick = onToggleShuffle,
                    interactionSource = shuffleInteraction,
                    modifier = Modifier.size(46.dp).pressScale(shuffleInteraction, pressedScale = 0.85f),
                ) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = if (playerState.shuffleEnabled) "关闭随机播放" else "开启随机播放",
                        tint = if (playerState.shuffleEnabled) ResonanceColors.Coral else ResonanceColors.Dim,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = onPrevious,
                    interactionSource = previousInteraction,
                    modifier = Modifier.size(46.dp).pressScale(previousInteraction, pressedScale = 0.85f),
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "上一首", tint = ResonanceColors.Ivory)
                }
            }
            FilledIconButton(
                onClick = onTogglePlay,
                interactionSource = playInteraction,
                modifier = Modifier
                    .size(50.dp)
                    .pressScale(playInteraction, pressedScale = 0.88f)
                    .shadow(10.dp, CircleShape, ambientColor = ResonanceColors.Coral.copy(alpha = 0.45f), spotColor = ResonanceColors.CoralGlow),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ResonanceColors.Coral,
                    contentColor = Color(0xFF2A0B07),
                ),
            ) {
                AnimatedContent(
                    targetState = playerState.isPlaying,
                    transitionSpec = { scaleIn(tween(MotionQuick)) togetherWith scaleOut(tween(MotionQuick)) },
                    label = "playPause",
                ) { playing ->
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "暂停" else "播放",
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            IconButton(
                onClick = onNext,
                interactionSource = nextInteraction,
                modifier = Modifier.size(46.dp).pressScale(nextInteraction, pressedScale = 0.85f),
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "下一首", tint = ResonanceColors.Ivory)
            }
            if (!compact) {
                IconButton(
                    onClick = onCycleRepeat,
                    interactionSource = repeatInteraction,
                    modifier = Modifier.size(46.dp).pressScale(repeatInteraction, pressedScale = 0.85f),
                ) {
                    Icon(
                        if (playerState.repeatMode == RepeatMode.One) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = when (playerState.repeatMode) {
                            RepeatMode.Off -> "开启列表循环"
                            RepeatMode.All -> "开启单曲循环"
                            RepeatMode.One -> "关闭循环"
                        },
                        tint = if (playerState.repeatMode == RepeatMode.Off) ResonanceColors.Dim else ResonanceColors.Coral,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Slider(
            value = playerState.progress.coerceIn(0f, 1f),
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth().height(16.dp).padding(horizontal = 6.dp),
            colors = SliderDefaults.colors(
                thumbColor = ResonanceColors.CoralGlow,
                activeTrackColor = ResonanceColors.Coral,
                inactiveTrackColor = ResonanceColors.DividerStrong.copy(alpha = 0.7f),
            ),
        )
        if (!compact) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    formatPlaybackPosition(track.durationText, playerState.progress),
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
}

internal fun formatPlaybackPosition(durationText: String, progress: Float): String {
    val total = com.resonance.player.model.durationTextToSeconds(durationText)
    if (total <= 0) return "0:00"
    val elapsed = (total * progress.coerceIn(0f, 1f)).toInt()
    return "%d:%02d".format(elapsed / 60, elapsed % 60)
}

@Composable
private fun ImportScreen(
    compact: Boolean,
    onImport: () -> Unit,
    onConvert: () -> Unit,
    onImportPlaylist: () -> Unit,
    message: String?,
    operationInProgress: Boolean,
) {
    FeaturePage(
        eyebrow = "导入音乐",
        title = "把散落的音乐\n带回一个地方",
        body = "扫描系统音乐、选择 KGMA 所在文件夹，或导入酷狗公开歌单目录。",
        compact = compact,
    ) {
        if (operationInProgress) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (message != null) {
                Surface(
                    color = ResonanceColors.CoralSoft,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        message,
                        color = ResonanceColors.Coral,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
            ActionList(
                actions = listOf(
                    Triple(Icons.Default.Headphones, "扫描本机音乐", "查找 MP3、FLAC、M4A 等系统可见音频"),
                    Triple(Icons.Default.FolderOpen, "选择文件夹并转换 KGMA", "递归转为 320 kbps MP3；保留源文件"),
                    Triple(Icons.AutoMirrored.Filled.QueueMusic, "导入酷狗歌单链接", "读取公开歌单目录并匹配本机已有歌曲"),
                ),
                onClick = { index ->
                    when (index) {
                        0 -> onImport()
                        1 -> onConvert()
                        else -> onImportPlaylist()
                    }
                },
            )
        }
    }
}

@Composable
private fun SyncScreen(
    compact: Boolean,
    onExportSync: () -> Unit,
    onImportSync: () -> Unit,
    onStartLanSync: () -> Unit,
    lanQrPath: String?,
    message: String?,
) {
    FeaturePage(
        eyebrow = "设备同步",
        title = "不经过云端，\n只在设备之间",
        body = "同一局域网内加密同步歌单、封面和缺失的 MP3。第一次连接使用二维码配对。",
        compact = compact,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (message != null) {
                Surface(color = ResonanceColors.CoralSoft, shape = RoundedCornerShape(12.dp)) {
                    Text(message, color = ResonanceColors.Coral, modifier = Modifier.padding(14.dp))
                }
            }
            ActionList(
                actions = listOf(
                    Triple(Icons.Default.Devices, "开启局域网扫码同步", "Windows 生成 10 分钟一次性二维码，手机系统相机扫码即传"),
                    Triple(Icons.Default.Devices, "导出同步包", "选择歌单或整个音乐库，打包 MP3、封面与元数据（普通 Zip，无需密码）"),
                    Triple(Icons.Default.FolderOpen, "导入同步包", "选择 .resonance 同步包，校验完整性后合并到本机音乐库"),
                ),
                onClick = { index ->
                    when (index) {
                        0 -> onStartLanSync()
                        1 -> onExportSync()
                        else -> onImportSync()
                    }
                },
            )
            if (lanQrPath != null) {
                AlbumArtwork(
                    seed = 0,
                    modifier = Modifier.size(if (compact) 220.dp else 260.dp),
                    cornerRadius = 16.dp,
                    artworkPath = lanQrPath,
                )
                Text("二维码已保存：$lanQrPath", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    compact: Boolean,
    libraryLocation: String,
    themeMode: ThemeMode = ThemeMode.Dark,
    onThemeModeChange: (ThemeMode) -> Unit = {},
) {
    FeaturePage(
        eyebrow = "设置",
        title = "音乐库偏好",
        body = "查看导入质量、外观主题、文件位置与应用信息。",
        compact = compact,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemeModeSelector(currentMode = themeMode, onModeSelect = onThemeModeChange)
            SettingsInfoRow(Icons.Default.GraphicEq, "转换质量", "默认 MP3 320 kbps · 原 MP3 不重新编码")
            SettingsInfoRow(Icons.Default.MusicNote, "自动歌词", "LRCLIB 自动匹配逐行歌词 · 结果仅缓存在本机")
            SettingsInfoRow(Icons.Default.FolderOpen, "音乐库位置", libraryLocation)
            SettingsInfoRow(Icons.Default.Shuffle, "播放与队列", "记住随机 / 循环模式和上次选中的歌单")
            Spacer(Modifier.height(8.dp))
            Surface(
                color = ResonanceColors.Soft,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ResonanceColors.Divider.copy(alpha = 0.8f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("关于 Resonance", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("版本 v$APP_VERSION", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "本地优先的跨平台音乐播放器。音乐、歌单与封面只保存在你的设备上。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ResonanceColors.Muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    currentMode: ThemeMode,
    onModeSelect: (ThemeMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .resonanceGlass(
                shape = RoundedCornerShape(20.dp),
                backgroundColor = ResonanceColors.Glass,
                borderColors = listOf(ResonanceColors.GlassBorder, ResonanceColors.GlassBorderSubtle),
                shadowElevation = 6.dp,
            )
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ResonanceColors.CoralSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = ResonanceColors.CoralGlow, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("界面主题风格", style = MaterialTheme.typography.titleMedium, color = ResonanceColors.Ivory, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text("即时切换深色极光或纯净浅色磨砂质感", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ThemeOptionButton(
                label = "深色模式",
                icon = Icons.Default.DarkMode,
                selected = currentMode == ThemeMode.Dark,
                onClick = { onModeSelect(ThemeMode.Dark) },
                modifier = Modifier.weight(1f),
            )
            ThemeOptionButton(
                label = "浅色模式",
                icon = Icons.Default.LightMode,
                selected = currentMode == ThemeMode.Light,
                onClick = { onModeSelect(ThemeMode.Light) },
                modifier = Modifier.weight(1f),
            )
            ThemeOptionButton(
                label = "跟随系统",
                icon = Icons.Default.BrightnessAuto,
                selected = currentMode == ThemeMode.System,
                onClick = { onModeSelect(ThemeMode.System) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThemeOptionButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor by animateColorAsState(
        targetValue = if (selected) ResonanceColors.Coral.copy(alpha = 0.7f) else ResonanceColors.GlassBorderSubtle,
        animationSpec = tween(150),
        label = "themeBorder",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) ResonanceColors.CoralSoft else ResonanceColors.Raised,
        animationSpec = tween(150),
        label = "themeContainer",
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.96f)
            .shadow(if (selected) 6.dp else 1.dp, RoundedCornerShape(14.dp), ambientColor = if (selected) ResonanceColors.Coral else ResonanceColors.Shadow, spotColor = ResonanceColors.Shadow),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) ResonanceColors.CoralGlow else ResonanceColors.Dim,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                color = if (selected) ResonanceColors.CoralGlow else ResonanceColors.Muted,
            )
        }
    }
}


@Composable
private fun SettingsInfoRow(icon: ImageVector, title: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .resonanceGlass(
                shape = RoundedCornerShape(18.dp),
                backgroundColor = ResonanceColors.Glass,
                borderColors = listOf(ResonanceColors.GlassBorderSubtle, Color.Transparent),
                shadowElevation = 4.dp,
            )
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ResonanceColors.Soft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = ResonanceColors.CoralGlow, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = ResonanceColors.Ivory, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
        }
    }
}

@Composable
private fun FeaturePage(
    eyebrow: String,
    title: String,
    body: String,
    compact: Boolean,
    content: @Composable () -> Unit,
) {
    val violet = ResonanceColors.Violet
    val coral = ResonanceColors.Coral
    LazyColumn(
        contentPadding = PaddingValues(if (compact) 24.dp else 48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            val shape = RoundedCornerShape(if (compact) 24.dp else 30.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
                    .resonanceGlass(
                        shape = shape,
                        backgroundColor = ResonanceColors.Glass,
                        borderColors = listOf(ResonanceColors.VioletGlow.copy(alpha = 0.5f), ResonanceColors.GlassBorder),
                        shadowElevation = 14.dp,
                    ),
            ) {
                Canvas(Modifier.matchParentSize()) {
                    val center = Offset(size.width * 0.90f, size.height * 0.15f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(violet.copy(alpha = 0.22f), Color.Transparent),
                            center = center,
                            radius = size.maxDimension * 0.65f,
                        ),
                        radius = size.maxDimension * 0.65f,
                        center = center,
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(coral.copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(size.width * 0.12f, size.height * 0.90f),
                            radius = size.minDimension * 0.55f,
                        ),
                        radius = size.minDimension * 0.55f,
                        center = Offset(size.width * 0.12f, size.height * 0.90f),
                    )
                }
                Column(Modifier.padding(if (compact) 22.dp else 32.dp)) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ResonanceColors.VioletSoft)
                            .border(1.dp, ResonanceColors.Violet.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        Text(
                            eyebrow.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 0.8.sp),
                            color = ResonanceColors.VioletGlow,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        title,
                        style = if (compact) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayLarge,
                        color = ResonanceColors.Ivory,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(body, style = MaterialTheme.typography.bodyLarge, color = ResonanceColors.Muted, modifier = Modifier.widthIn(max = 620.dp))
                }
            }
        }
        item { Column(Modifier.widthIn(max = 760.dp)) { content() } }
    }
}

@Composable
private fun ActionList(actions: List<Triple<ImageVector, String, String>>, onClick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        actions.forEachIndexed { index, action ->
            val interactionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(interactionSource, pressedScale = 0.985f)
                    .resonanceGlass(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = ResonanceColors.Glass,
                        borderColors = listOf(ResonanceColors.GlassBorder, ResonanceColors.GlassBorderSubtle),
                        shadowElevation = 6.dp,
                    )
                    .clickable(interactionSource = interactionSource, indication = null) { onClick(index) }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(50.dp)
                        .shadow(6.dp, RoundedCornerShape(15.dp), ambientColor = ResonanceColors.Coral.copy(alpha = 0.3f), spotColor = ResonanceColors.Shadow)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Brush.linearGradient(listOf(ResonanceColors.CoralSoft, Color(0xFF261E38))))
                        .border(1.dp, ResonanceColors.Coral.copy(alpha = 0.3f), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(action.first, contentDescription = null, tint = ResonanceColors.CoralGlow, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(action.second, style = MaterialTheme.typography.titleMedium, color = ResonanceColors.Ivory, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text(action.third, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ResonanceColors.CoralGlow, modifier = Modifier.size(20.dp))
            }
        }
    }
}


@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .shadow(5.dp, RoundedCornerShape(13.dp), ambientColor = ResonanceColors.Shadow, spotColor = ResonanceColors.Shadow)
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.linearGradient(listOf(ResonanceColors.CoralGlow, ResonanceColors.Coral))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF2B0C08), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun EmptyLibraryArtwork(size: Dp) {
    val coral = ResonanceColors.Coral
    val soft = ResonanceColors.Soft
    val divider = ResonanceColors.Divider
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(listOf(coral.copy(alpha = 0.26f), Color.Transparent)),
                radius = this.size.minDimension * 0.52f,
            )
            drawCircle(color = Color(0xFF090C10), radius = this.size.minDimension * 0.37f)
            drawCircle(color = soft, radius = this.size.minDimension * 0.17f)
            drawCircle(color = coral, radius = this.size.minDimension * 0.055f)
            repeat(5) { index ->
                drawCircle(
                    color = divider.copy(alpha = 0.55f),
                    radius = this.size.minDimension * (0.21f + index * 0.026f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(size * 0.28f)
                .clip(CircleShape)
                .background(ResonanceColors.Coral),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF2B0C08), modifier = Modifier.size(size * 0.14f))
        }
    }
}

@Composable
fun AlbumArtwork(seed: Int, modifier: Modifier = Modifier, cornerRadius: Dp = 16.dp, artworkPath: String? = null) {
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
