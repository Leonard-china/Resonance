package com.resonance.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.resonance.player.design.ResonanceColors
import com.resonance.player.model.LyricLine
import com.resonance.player.model.Lyrics
import com.resonance.player.model.LyricsUiState
import com.resonance.player.model.PlayerState
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.Track
import com.resonance.player.model.durationTextToSeconds

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
    onTrackSelected: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onRefreshLyrics: () -> Unit,
) {
    val track = playerState.currentTrack ?: return
    var paneName by rememberSaveable { mutableStateOf(PlayerPane.Cover.name) }
    val pane = PlayerPane.entries.firstOrNull { it.name == paneName } ?: PlayerPane.Cover

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = ResonanceColors.Canvas) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 720.dp || maxWidth > maxHeight * 1.35f
                NowPlayingBackdrop(track.artworkSeed, playerState.isPlaying, Modifier.matchParentSize())
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = if (wide) 28.dp else 16.dp, vertical = 12.dp),
                ) {
                    NowPlayingTopBar(track, onDismiss, onToggleFavorite)
                    Spacer(Modifier.height(8.dp))
                    if (wide) {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ArtworkPane(track, playerState.isPlaying, Modifier.weight(0.9f).fillMaxHeight())
                            Column(Modifier.weight(1.1f).fillMaxHeight()) {
                                TrackIdentity(track)
                                Spacer(Modifier.height(14.dp))
                                PlayerPaneSelector(pane, onSelect = { paneName = it.name })
                                Spacer(Modifier.height(10.dp))
                                PaneContent(
                                    pane = pane,
                                    track = track,
                                    playerState = playerState,
                                    lyricsState = lyricsState,
                                    queue = queue,
                                    onSeek = onSeek,
                                    onTrackSelected = onTrackSelected,
                                    onRefreshLyrics = onRefreshLyrics,
                                    modifier = Modifier.weight(1f),
                                )
                                PlaybackProgress(track, playerState.progress, onSeek)
                                PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                            }
                        }
                    } else {
                        PlayerPaneSelector(pane, onSelect = { paneName = it.name })
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
                            modifier = Modifier.weight(1f),
                        )
                        TrackIdentity(track)
                        Spacer(Modifier.height(8.dp))
                        PlaybackProgress(track, playerState.progress, onSeek)
                        PlaybackControls(playerState, onTogglePlay, onPrevious, onNext, onToggleShuffle, onCycleRepeat)
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingBackdrop(seed: Int, playing: Boolean, modifier: Modifier = Modifier) {
    val energy by animateFloatAsState(
        targetValue = if (playing) 1f else 0.64f,
        animationSpec = tween(420),
        label = "playerBackdropEnergy",
    )
    val phase = (((seed % 17) + 17) % 17) / 16f
    Canvas(modifier.background(Brush.verticalGradient(listOf(Color(0xFF111522), ResonanceColors.Canvas, Color(0xFF05070B))))) {
        val radius = size.maxDimension * 0.62f
        val first = Offset(size.width * (0.16f + phase * 0.16f), size.height * 0.13f)
        val second = Offset(size.width * (0.85f - phase * 0.10f), size.height * 0.74f)
        drawCircle(
            brush = Brush.radialGradient(listOf(ResonanceColors.Coral.copy(alpha = 0.13f * energy), Color.Transparent), first, radius),
            center = first,
            radius = radius,
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(ResonanceColors.Violet.copy(alpha = 0.12f * energy), Color.Transparent), second, radius * 0.8f),
            center = second,
            radius = radius * 0.8f,
        )
    }
}

@Composable
private fun NowPlayingTopBar(track: Track, onDismiss: () -> Unit, onToggleFavorite: (Track) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Close, contentDescription = "关闭正在播放")
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("正在播放", style = MaterialTheme.typography.labelLarge, color = ResonanceColors.CoralGlow)
            Text(track.album, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = { onToggleFavorite(track) }, modifier = Modifier.size(48.dp)) {
            Icon(
                if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (track.isFavorite) "取消收藏" else "收藏",
                tint = if (track.isFavorite) ResonanceColors.Coral else ResonanceColors.Muted,
            )
        }
    }
}

@Composable
private fun PlayerPaneSelector(selected: PlayerPane, onSelect: (PlayerPane) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ResonanceColors.Glass).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PlayerPane.entries.forEach { pane ->
            val active = pane == selected
            val color by animateColorAsState(
                targetValue = if (active) ResonanceColors.CoralSoft else Color.Transparent,
                animationSpec = tween(180),
                label = "playerPane",
            )
            Surface(
                onClick = { onSelect(pane) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                color = color,
                shape = RoundedCornerShape(14.dp),
                border = if (active) BorderStroke(1.dp, ResonanceColors.Coral.copy(alpha = 0.42f)) else null,
            ) {
                Row(
                    // Avoid fillMaxSize here: inside the portrait player's Column it
                    // greedily consumed all available height and pushed the artwork,
                    // metadata and controls off-screen.
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(pane.icon, contentDescription = null, tint = if (active) ResonanceColors.CoralGlow else ResonanceColors.Muted, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(pane.label, style = MaterialTheme.typography.labelLarge, color = if (active) ResonanceColors.Ivory else ResonanceColors.Muted)
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
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = pane,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = {
            (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 }) togetherWith
                (fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it / 12 })
        },
        label = "playerPaneContent",
    ) { active ->
        when (active) {
            PlayerPane.Cover -> ArtworkPane(track, playerState.isPlaying, Modifier.fillMaxSize())
            PlayerPane.Lyrics -> LyricsPane(track, playerState.progress, lyricsState, onSeek, onRefreshLyrics)
            PlayerPane.Queue -> QueuePane(queue, track.id, onTrackSelected)
        }
    }
}

@Composable
private fun ArtworkPane(track: Track, playing: Boolean, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = if (playing) 1f else 0.965f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "heroArtwork",
    )
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val artworkSize = minOf(maxWidth - 24.dp, maxHeight - 24.dp, 380.dp).coerceAtLeast(128.dp)
        Box(
            modifier = Modifier
                .size(artworkSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationX = if (playing) 0f else 1.5f
                    rotationY = if (playing) 0f else -1.5f
                    cameraDistance = 18f * density
                }
                .shadow(28.dp, RoundedCornerShape(30.dp), ambientColor = ResonanceColors.Coral.copy(alpha = 0.16f), spotColor = ResonanceColors.Shadow)
                .border(1.dp, ResonanceColors.DividerStrong.copy(alpha = 0.78f), RoundedCornerShape(30.dp)),
        ) {
            AlbumArtwork(track.artworkSeed, Modifier.fillMaxSize(), 30.dp, track.artworkPath)
        }
    }
}

@Composable
private fun TrackIdentity(track: Track) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(track.title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        Text(track.artist, style = MaterialTheme.typography.bodyLarge, color = ResonanceColors.Muted, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlaybackProgress(track: Track, progress: Float, onSeek: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth().height(34.dp),
            colors = SliderDefaults.colors(
                thumbColor = ResonanceColors.Coral,
                activeTrackColor = ResonanceColors.Coral,
                inactiveTrackColor = ResonanceColors.DividerStrong,
            ),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatPlaybackPosition(track.durationText, progress), style = MaterialTheme.typography.labelMedium, color = ResonanceColors.Muted)
            Text(track.durationText, style = MaterialTheme.typography.labelMedium, color = ResonanceColors.Muted)
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
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleShuffle, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = if (playerState.shuffleEnabled) "关闭随机播放" else "开启随机播放",
                tint = if (playerState.shuffleEnabled) ResonanceColors.Coral else ResonanceColors.Muted,
            )
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "上一首", modifier = Modifier.size(30.dp))
        }
        FilledIconButton(
            onClick = onTogglePlay,
            modifier = Modifier.size(64.dp).shadow(12.dp, CircleShape, spotColor = ResonanceColors.Coral.copy(alpha = 0.26f)),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = ResonanceColors.Coral, contentColor = Color(0xFF2A0B07)),
        ) {
            Icon(
                if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playerState.isPlaying) "暂停" else "播放",
                modifier = Modifier.size(34.dp),
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Default.SkipNext, contentDescription = "下一首", modifier = Modifier.size(30.dp))
        }
        IconButton(onClick = onCycleRepeat, modifier = Modifier.size(48.dp)) {
            Icon(
                if (playerState.repeatMode == RepeatMode.One) Icons.Default.RepeatOne else Icons.Default.Repeat,
                contentDescription = when (playerState.repeatMode) {
                    RepeatMode.Off -> "开启列表循环"
                    RepeatMode.All -> "开启单曲循环"
                    RepeatMode.One -> "关闭循环"
                },
                tint = if (playerState.repeatMode == RepeatMode.Off) ResonanceColors.Muted else ResonanceColors.Coral,
            )
        }
    }
}

@Composable
private fun LyricsPane(
    track: Track,
    progress: Float,
    state: LyricsUiState,
    onSeek: (Float) -> Unit,
    onRefresh: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ResonanceColors.Glass,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ResonanceColors.Divider.copy(alpha = 0.84f)),
    ) {
        when (state) {
            LyricsUiState.Idle, LyricsUiState.Loading -> LyricsLoading()
            is LyricsUiState.Unavailable -> LyricsUnavailable(state.message, onRefresh)
            is LyricsUiState.Ready -> LyricsContent(track, progress, state.lyrics, onSeek)
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
        CircularProgressIndicator(color = ResonanceColors.Coral, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(16.dp))
        Text("正在自动匹配歌词…", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(5.dp))
        Text("不需要粘贴文本或提供 API Key", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
    }
}

@Composable
private fun LyricsUnavailable(message: String, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = ResonanceColors.Dim, modifier = Modifier.size(46.dp))
        Spacer(Modifier.height(14.dp))
        Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRefresh, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("重新匹配")
        }
    }
}

@Composable
private fun LyricsContent(track: Track, progress: Float, lyrics: Lyrics, onSeek: (Float) -> Unit) {
    if (lyrics.instrumental) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = ResonanceColors.CoralGlow, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(14.dp))
            Text("纯音乐，请享受旋律", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("歌词来源：${lyrics.source}", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
        }
        return
    }

    val durationMs = durationTextToSeconds(track.durationText).coerceAtLeast(1) * 1_000L
    val positionMs = (durationMs * progress.coerceIn(0f, 1f)).toLong()
    val activeIndex = if (lyrics.synchronized) {
        lyrics.lines.indexOfLast { (it.timestampMs ?: Long.MAX_VALUE) <= positionMs }
    } else {
        -1
    }
    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Text(
                if (lyrics.synchronized) "逐行歌词 · 点按可跳转" else "歌词",
                style = MaterialTheme.typography.labelLarge,
                color = ResonanceColors.CoralGlow,
            )
            Text(
                "${lyrics.source} 自动匹配${if (lyrics.fromCache) " · 已缓存" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = ResonanceColors.Dim,
            )
            Spacer(Modifier.height(18.dp))
        }
        itemsIndexed(lyrics.lines, key = { index, line -> "${line.timestampMs}-$index" }) { index, line ->
            LyricRow(
                line = line,
                active = index == activeIndex,
                onClick = line.timestampMs?.let { timestamp -> { onSeek(timestamp.toFloat() / durationMs.toFloat()) } },
            )
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun LyricRow(line: LyricLine, active: Boolean, onClick: (() -> Unit)?) {
    val color by animateColorAsState(
        targetValue = if (active) ResonanceColors.Ivory else ResonanceColors.Muted,
        animationSpec = tween(180),
        label = "lyricColor",
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1f else 0.96f,
        animationSpec = tween(180),
        label = "lyricScale",
    )
    val interaction = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(interaction)
            .background(if (active) ResonanceColors.CoralSoft.copy(alpha = 0.7f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            line.text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = color,
        )
    }
}

@Composable
private fun QueuePane(queue: List<Track>, currentTrackId: String, onTrackSelected: (Track) -> Unit) {
    if (queue.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("播放队列为空", color = ResonanceColors.Muted)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        item {
            Text("接下来播放 · ${queue.size} 首", style = MaterialTheme.typography.labelLarge, color = ResonanceColors.CoralGlow, modifier = Modifier.padding(10.dp))
        }
        itemsIndexed(queue, key = { index, item -> "queue-${item.id}-$index" }) { _, item ->
            val current = item.id == currentTrackId
            Surface(
                onClick = { onTrackSelected(item) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                color = if (current) ResonanceColors.CoralSoft else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    AlbumArtwork(item.artworkSeed, Modifier.size(46.dp), 12.dp, item.artworkPath)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.artist, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (current) Icon(Icons.Default.GraphicEq, contentDescription = "当前播放", tint = ResonanceColors.Coral)
                }
            }
        }
    }
}
