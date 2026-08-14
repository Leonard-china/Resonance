package com.resonance.player.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.resonance.player.model.LibraryDestination
import com.resonance.player.model.PlayerState
import com.resonance.player.model.Playlist
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.Track
import com.resonance.player.platform.decodeArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class WindowClass { Compact, Medium, Expanded }

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

@Composable
fun LibraryShell(
    destination: LibraryDestination,
    onDestinationChange: (LibraryDestination) -> Unit,
    playlists: List<Playlist>,
    playerState: PlayerState,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSeek: (Float) -> Unit,
    onTrackSelected: (Track) -> Unit,
    selectedPlaylistId: String,
    userPlaylists: List<Playlist>,
    onPlaylistSelected: (String) -> Unit,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
    onCreatePlaylist: () -> Unit,
    onImport: (Boolean) -> Unit,
    onImportPlaylist: () -> Unit,
    onExportSync: () -> Unit,
    onImportSync: () -> Unit,
    onStartLanSync: () -> Unit,
    lanQrPath: String?,
    onExitPreview: () -> Unit,
    previewMode: Boolean,
    message: String?,
    operationInProgress: Boolean,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ResonanceColors.Canvas)) {
        val windowClass = when {
            maxWidth < 720.dp -> WindowClass.Compact
            maxWidth < 1100.dp -> WindowClass.Medium
            else -> WindowClass.Expanded
        }

        when (windowClass) {
            WindowClass.Compact -> CompactShell(
                destination = destination,
                onDestinationChange = onDestinationChange,
                playlists = playlists,
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
                onCreatePlaylist = onCreatePlaylist,
                onImport = onImport,
                onImportPlaylist = onImportPlaylist,
                onExportSync = onExportSync,
                onImportSync = onImportSync,
                onStartLanSync = onStartLanSync,
                lanQrPath = lanQrPath,
                onExitPreview = onExitPreview,
                previewMode = previewMode,
                message = message,
                operationInProgress = operationInProgress,
            )

            WindowClass.Medium -> WideShell(
                compactRail = true,
                destination = destination,
                onDestinationChange = onDestinationChange,
                playlists = playlists,
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
                onCreatePlaylist = onCreatePlaylist,
                onImport = onImport,
                onImportPlaylist = onImportPlaylist,
                onExportSync = onExportSync,
                onImportSync = onImportSync,
                onStartLanSync = onStartLanSync,
                lanQrPath = lanQrPath,
                onExitPreview = onExitPreview,
                previewMode = previewMode,
                message = message,
                operationInProgress = operationInProgress,
            )

            WindowClass.Expanded -> WideShell(
                compactRail = false,
                destination = destination,
                onDestinationChange = onDestinationChange,
                playlists = playlists,
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
                onCreatePlaylist = onCreatePlaylist,
                onImport = onImport,
                onImportPlaylist = onImportPlaylist,
                onExportSync = onExportSync,
                onImportSync = onImportSync,
                onStartLanSync = onStartLanSync,
                lanQrPath = lanQrPath,
                onExitPreview = onExitPreview,
                previewMode = previewMode,
                message = message,
                operationInProgress = operationInProgress,
            )
        }
    }
}

@Composable
private fun CompactShell(
    destination: LibraryDestination,
    onDestinationChange: (LibraryDestination) -> Unit,
    playlists: List<Playlist>,
    playerState: PlayerState,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSeek: (Float) -> Unit,
    onTrackSelected: (Track) -> Unit,
    selectedPlaylistId: String,
    userPlaylists: List<Playlist>,
    onPlaylistSelected: (String) -> Unit,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
    onCreatePlaylist: () -> Unit,
    onImport: (Boolean) -> Unit,
    onImportPlaylist: () -> Unit,
    onExportSync: () -> Unit,
    onImportSync: () -> Unit,
    onStartLanSync: () -> Unit,
    lanQrPath: String?,
    onExitPreview: () -> Unit,
    previewMode: Boolean,
    message: String?,
    operationInProgress: Boolean,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            DestinationContent(
                destination = destination,
                playlists = playlists,
                playerState = playerState,
                onTrackSelected = onTrackSelected,
                selectedPlaylistId = selectedPlaylistId,
                userPlaylists = userPlaylists,
                onPlaylistSelected = onPlaylistSelected,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onRenamePlaylist = onRenamePlaylist,
                onDeletePlaylist = onDeletePlaylist,
                onDeleteLocalTrack = onDeleteLocalTrack,
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
                message = message,
                operationInProgress = operationInProgress,
            )
        }
        if (playerState.currentTrack != null) {
            MiniPlayer(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat, onSeek, horizontalPadding = 8.dp)
        }
        NavigationBar(
            containerColor = ResonanceColors.Raised,
            tonalElevation = 0.dp,
            modifier = Modifier.height(72.dp),
        ) {
            destinationItems.forEach { item ->
                NavigationBarItem(
                    selected = destination == item.destination,
                    onClick = { onDestinationChange(item.destination) },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
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
    playerState: PlayerState,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSeek: (Float) -> Unit,
    onTrackSelected: (Track) -> Unit,
    selectedPlaylistId: String,
    userPlaylists: List<Playlist>,
    onPlaylistSelected: (String) -> Unit,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
    onCreatePlaylist: () -> Unit,
    onImport: (Boolean) -> Unit,
    onImportPlaylist: () -> Unit,
    onExportSync: () -> Unit,
    onImportSync: () -> Unit,
    onStartLanSync: () -> Unit,
    lanQrPath: String?,
    onExitPreview: () -> Unit,
    previewMode: Boolean,
    message: String?,
    operationInProgress: Boolean,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        if (compactRail) {
            NavigationRail(
                containerColor = ResonanceColors.Raised,
                header = {
                    BrandMark(modifier = Modifier.padding(vertical = 20.dp))
                },
            ) {
                destinationItems.forEach { item ->
                    NavigationRailItem(
                        selected = destination == item.destination,
                        onClick = { onDestinationChange(item.destination) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        } else {
            DesktopSidebar(
                destination = destination,
                onDestinationChange = onDestinationChange,
            )
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Box(modifier = Modifier.weight(1f)) {
                DestinationContent(
                    destination = destination,
                    playlists = playlists,
                    playerState = playerState,
                    onTrackSelected = onTrackSelected,
                    selectedPlaylistId = selectedPlaylistId,
                    userPlaylists = userPlaylists,
                    onPlaylistSelected = onPlaylistSelected,
                    onPlaylistMembershipChange = onPlaylistMembershipChange,
                    onRenamePlaylist = onRenamePlaylist,
                    onDeletePlaylist = onDeletePlaylist,
                    onDeleteLocalTrack = onDeleteLocalTrack,
                    onCreatePlaylist = onCreatePlaylist,
                    onImport = onImport,
                    onImportPlaylist = onImportPlaylist,
                    onExportSync = onExportSync,
                    onImportSync = onImportSync,
                    onStartLanSync = onStartLanSync,
                    lanQrPath = lanQrPath,
                    onExitPreview = onExitPreview,
                    previewMode = previewMode,
                    compact = false,
                    message = message,
                    operationInProgress = operationInProgress,
                )
            }
            if (playerState.currentTrack != null) {
                MiniPlayer(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat, onSeek, horizontalPadding = 16.dp)
            }
        }
    }
}

@Composable
private fun DesktopSidebar(
    destination: LibraryDestination,
    onDestinationChange: (LibraryDestination) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(232.dp)
            .fillMaxHeight()
            .background(ResonanceColors.Raised)
            .padding(horizontal = 18.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark()
            Spacer(Modifier.width(12.dp))
            Column {
                Text("RESONANCE", style = MaterialTheme.typography.titleMedium)
                Text("LOCAL MUSIC", style = MaterialTheme.typography.labelMedium, color = ResonanceColors.Dim)
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
        Surface(
            color = ResonanceColors.Soft,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(ResonanceColors.Mint))
                    Spacer(Modifier.width(8.dp))
                    Text("本地模式", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(6.dp))
                Text("音乐只在你的设备之间流动", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
            }
        }
    }
}

@Composable
private fun SidebarDestination(item: DestinationItem, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        if (selected) ResonanceColors.CoralSoft else Color.Transparent,
        animationSpec = tween(180),
        label = "sidebarBackground",
    )
    val foreground by animateColorAsState(
        if (selected) ResonanceColors.Coral else ResonanceColors.Muted,
        animationSpec = tween(180),
        label = "sidebarForeground",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(item.icon, contentDescription = null, tint = foreground, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(12.dp))
        Text(item.label, style = MaterialTheme.typography.labelLarge, color = foreground)
    }
}

@Composable
private fun DestinationContent(
    destination: LibraryDestination,
    playlists: List<Playlist>,
    playerState: PlayerState,
    onTrackSelected: (Track) -> Unit,
    selectedPlaylistId: String,
    userPlaylists: List<Playlist>,
    onPlaylistSelected: (String) -> Unit,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
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
    message: String?,
    operationInProgress: Boolean,
) {
    when (destination) {
        LibraryDestination.Library -> LibraryScreen(
            playlists = playlists,
            playerState = playerState,
            onTrackSelected = onTrackSelected,
            selectedPlaylistId = selectedPlaylistId,
            userPlaylists = userPlaylists,
            onPlaylistSelected = onPlaylistSelected,
            onPlaylistMembershipChange = onPlaylistMembershipChange,
            onRenamePlaylist = onRenamePlaylist,
            onDeletePlaylist = onDeletePlaylist,
            onDeleteLocalTrack = onDeleteLocalTrack,
            onCreatePlaylist = onCreatePlaylist,
            onImport = onImport,
            onExitPreview = onExitPreview,
            previewMode = previewMode,
            compact = compact,
            message = message,
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
        LibraryDestination.Settings -> SettingsScreen(compact = compact)
    }
}

@Composable
private fun LibraryScreen(
    playlists: List<Playlist>,
    playerState: PlayerState,
    onTrackSelected: (Track) -> Unit,
    selectedPlaylistId: String,
    userPlaylists: List<Playlist>,
    onPlaylistSelected: (String) -> Unit,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
    onCreatePlaylist: () -> Unit,
    onImport: (Boolean) -> Unit,
    onExitPreview: () -> Unit,
    previewMode: Boolean,
    compact: Boolean,
    message: String?,
) {
    if (playlists.isEmpty()) {
        EmptyLibrary(onCreatePlaylist, { onImport(false) }, compact)
        return
    }

    val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId } ?: playlists.first()
    var showRenameDialog by remember(selectedPlaylist.id) { mutableStateOf(false) }
    var showDeleteDialog by remember(selectedPlaylist.id) { mutableStateOf(false) }
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
            )
            Spacer(Modifier.height(if (compact) 24.dp else 32.dp))
        }
        if (message != null) {
            item {
                Surface(color = ResonanceColors.CoralSoft, shape = RoundedCornerShape(12.dp)) {
                    Text(message, color = ResonanceColors.Coral, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                }
                Spacer(Modifier.height(18.dp))
            }
        }
        item {
            Text("你的歌单", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(14.dp))
            PlaylistStrip(
                playlists = playlists,
                compact = compact,
                selectedPlaylistId = selectedPlaylist.id,
                onPlaylistSelected = onPlaylistSelected,
            )
            Spacer(Modifier.height(if (compact) 30.dp else 38.dp))
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(selectedPlaylist.name, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        if (selectedPlaylist.id == "local-library") "全部已导入音乐" else selectedPlaylist.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ResonanceColors.Muted,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (selectedPlaylist.id != "local-library" && !previewMode) {
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "重命名歌单", tint = ResonanceColors.Muted)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除歌单", tint = ResonanceColors.Muted)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (selectedPlaylist.tracks.isEmpty()) {
            item {
                EmptyPlaylistNotice(
                    canAddMusic = userPlaylists.isNotEmpty(),
                    onImport = { onImport(false) },
                )
            }
        }
        itemsIndexed(selectedPlaylist.tracks, key = { index, track -> "${track.id}-$index" }) { _, track ->
            TrackRow(
                track = track,
                selected = playerState.currentTrack?.id == track.id,
                onClick = { onTrackSelected(track) },
                compact = compact,
                userPlaylists = userPlaylists,
                onPlaylistMembershipChange = onPlaylistMembershipChange,
                onDeleteLocalTrack = onDeleteLocalTrack,
            )
        }
    }

    if (showRenameDialog) {
        RenamePlaylistDialog(
            currentName = selectedPlaylist.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { name ->
                onRenamePlaylist(selectedPlaylist.id, name)
                showRenameDialog = false
            },
        )
    }
    if (showDeleteDialog) {
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
private fun LibraryHeader(
    compact: Boolean,
    previewMode: Boolean,
    onExitPreview: () -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val lowProfile = compact && maxWidth > 500.dp
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("晚上好", style = MaterialTheme.typography.labelLarge, color = ResonanceColors.Coral)
                Spacer(Modifier.height(5.dp))
                Text(
                    if (lowProfile) "你的音乐，留在身边。" else "你的音乐，\n留在身边。",
                    style = when {
                        lowProfile -> MaterialTheme.typography.headlineLarge
                        compact -> MaterialTheme.typography.displaySmall
                        else -> MaterialTheme.typography.displayLarge
                    },
                )
                if (!compact) {
                    Spacer(Modifier.height(10.dp))
                    Text("离线收藏、播放与设备同步，都由你掌控。", style = MaterialTheme.typography.bodyLarge, color = ResonanceColors.Muted)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                FilledIconButton(
                    onClick = onCreatePlaylist,
                    modifier = Modifier.size(50.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = ResonanceColors.Coral),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新建歌单")
                }
                if (previewMode) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onExitPreview) { Text("清空预览") }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(onCreatePlaylist: () -> Unit, onImport: () -> Unit, compact: Boolean) {
    if (compact) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.Start,
        ) {
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
private fun PrimaryAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(9.dp))
        Text(label)
    }
}

@Composable
private fun SecondaryAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ResonanceColors.Soft, contentColor = ResonanceColors.Ivory),
        modifier = modifier.heightIn(min = 52.dp),
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
    selectedPlaylistId: String,
    onPlaylistSelected: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(playlists, key = { it.id }) { playlist ->
            val selected = playlist.id == selectedPlaylistId
            Column(
                modifier = Modifier
                    .width(if (compact) 148.dp else 176.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        width = if (selected) 2.dp else 0.dp,
                        color = if (selected) ResonanceColors.Coral else Color.Transparent,
                        shape = RoundedCornerShape(18.dp),
                    )
                    .clickable { onPlaylistSelected(playlist.id) }
                    .padding(8.dp),
            ) {
                AlbumArtwork(
                    seed = playlist.artworkSeed,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    cornerRadius = 18.dp,
                    artworkPath = playlist.tracks.firstNotNullOfOrNull(Track::artworkPath),
                )
                Spacer(Modifier.height(11.dp))
                Text(
                    playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) ResonanceColors.Coral else ResonanceColors.Ivory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(playlist.subtitle, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    selected: Boolean,
    onClick: () -> Unit,
    compact: Boolean,
    userPlaylists: List<Playlist>,
    onPlaylistMembershipChange: (Track, String, Boolean) -> Unit,
    onDeleteLocalTrack: (Track) -> Unit,
) {
    val available = track.sourceUri != null
    var menuExpanded by remember(track.id) { mutableStateOf(false) }
    val background by animateColorAsState(
        if (selected) ResonanceColors.CoralSoft else Color.Transparent,
        animationSpec = tween(160),
        label = "trackBackground",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArtwork(track.artworkSeed, Modifier.size(if (compact) 52.dp else 58.dp), 12.dp, track.artworkPath)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selected) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = ResonanceColors.Coral, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) ResonanceColors.Coral else ResonanceColors.Ivory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!available) {
                    Spacer(Modifier.width(8.dp))
                    Surface(color = ResonanceColors.Soft, shape = RoundedCornerShape(6.dp)) {
                        Text("待匹配", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
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
        if (track.isFavorite) {
            Icon(Icons.Default.Favorite, contentDescription = "已收藏", tint = ResonanceColors.Coral, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
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
                if (canAddMusic) "在“本地音乐”里打开歌曲右侧菜单，即可加入这里。" else "先导入本机音乐，再把喜欢的歌曲加入歌单。",
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
    horizontalPadding: Dp,
) {
    val track = playerState.currentTrack ?: return
    Column(
        modifier = Modifier
            .padding(horizontal = horizontalPadding, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ResonanceColors.Soft),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtwork(track.artworkSeed, Modifier.size(50.dp), 12.dp, track.artworkPath)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onToggleShuffle, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = if (playerState.shuffleEnabled) "关闭随机播放" else "开启随机播放",
                    tint = if (playerState.shuffleEnabled) ResonanceColors.Coral else ResonanceColors.Muted,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "上一首")
            }
            FilledIconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(46.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = ResonanceColors.Coral),
            ) {
                Icon(if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (playerState.isPlaying) "暂停" else "播放")
            }
            IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "下一首")
            }
            IconButton(onClick = onCycleRepeat, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (playerState.repeatMode == RepeatMode.One) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = when (playerState.repeatMode) {
                        RepeatMode.Off -> "开启列表循环"
                        RepeatMode.All -> "开启单曲循环"
                        RepeatMode.One -> "关闭循环"
                    },
                    tint = if (playerState.repeatMode == RepeatMode.Off) ResonanceColors.Muted else ResonanceColors.Coral,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Slider(
            value = playerState.progress.coerceIn(0f, 1f),
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth().height(16.dp),
            colors = SliderDefaults.colors(
                thumbColor = ResonanceColors.Coral,
                activeTrackColor = ResonanceColors.Coral,
                inactiveTrackColor = ResonanceColors.Divider,
            ),
        )
    }
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
                    Triple(Icons.Default.FolderOpen, "选择文件夹并转换 KGMA", "递归转为 320 kbps MP3；获得写权限后删除源文件"),
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
                    Triple(Icons.Default.Devices, "导出加密同步包", "包含歌单、封面和可用 MP3，可通过 U 盘或局域网传送"),
                    Triple(Icons.Default.FolderOpen, "导入加密同步包", "验证口令、完整性和曲目哈希后再合并到本机"),
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
private fun SettingsScreen(compact: Boolean) {
    FeaturePage(
        eyebrow = "设置",
        title = "音乐库偏好",
        body = "控制导入质量、文件位置、播放行为和设备同步。",
        compact = compact,
    ) {
        ActionList(
            listOf(
                Triple(Icons.Default.GraphicEq, "转换质量", "默认 MP3 320 kbps · 原 MP3 不重新编码"),
                Triple(Icons.Default.FolderOpen, "音乐库位置", "使用系统 Music/Resonance 目录"),
                Triple(Icons.Default.Shuffle, "播放与队列", "记住播放模式和上次位置"),
            ),
            onClick = {},
        )
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
    LazyColumn(
        contentPadding = PaddingValues(if (compact) 24.dp else 48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(eyebrow, style = MaterialTheme.typography.labelLarge, color = ResonanceColors.Coral)
            Spacer(Modifier.height(8.dp))
            Text(title, style = if (compact) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(12.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge, color = ResonanceColors.Muted, modifier = Modifier.widthIn(max = 620.dp))
        }
        item { Column(Modifier.widthIn(max = 760.dp)) { content() } }
    }
}

@Composable
private fun ActionList(actions: List<Triple<ImageVector, String, String>>, onClick: (Int) -> Unit) {
    Column {
        actions.forEachIndexed { index, action ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onClick(index) }
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(ResonanceColors.Soft), contentAlignment = Alignment.Center) {
                    Icon(action.first, contentDescription = null, tint = ResonanceColors.Coral)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(action.second, style = MaterialTheme.typography.titleMedium)
                    Text(action.third, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ResonanceColors.Dim)
            }
            if (index < actions.lastIndex) HorizontalDivider(color = ResonanceColors.Divider, modifier = Modifier.padding(start = 72.dp))
        }
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(ResonanceColors.Coral),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF2B0C08), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun EmptyLibraryArtwork(size: Dp) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(listOf(ResonanceColors.Coral.copy(alpha = 0.26f), Color.Transparent)),
                radius = this.size.minDimension * 0.52f,
            )
            drawCircle(color = Color(0xFF090C10), radius = this.size.minDimension * 0.37f)
            drawCircle(color = ResonanceColors.Soft, radius = this.size.minDimension * 0.17f)
            drawCircle(color = ResonanceColors.Coral, radius = this.size.minDimension * 0.055f)
            repeat(5) { index ->
                drawCircle(
                    color = ResonanceColors.Divider.copy(alpha = 0.55f),
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
    val palette = palettes[kotlin.math.abs(seed) % palettes.size]
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
