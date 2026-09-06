package com.resonance.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberWindowState
import com.resonance.player.design.ResonanceColors
import com.resonance.player.model.LyricsUiState
import com.resonance.player.model.PlayerState
import com.resonance.player.model.durationTextToSeconds

@Composable
fun DesktopFloatingLyricsWindow(
    playerState: PlayerState,
    lyricsState: LyricsUiState,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val windowState = rememberWindowState(size = DpSize(720.dp, 130.dp))
    var isLocked by remember { mutableStateOf(false) }

    Window(
        onCloseRequest = onClose,
        title = "Resonance 桌面歌词",
        state = windowState,
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = true,
    ) {
        FloatingLyricsView(
            windowScope = this,
            playerState = playerState,
            lyricsState = lyricsState,
            isLocked = isLocked,
            onToggleLock = { isLocked = !isLocked },
            onClose = onClose,
            onTogglePlay = onTogglePlay,
            onPrevious = onPrevious,
            onNext = onNext,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FloatingLyricsView(
    windowScope: WindowScope,
    playerState: PlayerState,
    lyricsState: LyricsUiState,
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    var isHovered by remember { mutableStateOf(false) }
    val track = playerState.currentTrack

    // 计算当前歌词行与下一行
    var currentLine = if (track != null) "《${track.title}》- ${track.artist}" else "Resonance 音乐播放器"
    var nextLine = ""

    if (track != null && lyricsState is LyricsUiState.Ready) {
        val lyrics = lyricsState.lyrics
        if (lyrics.instrumental) {
            currentLine = "纯音乐 · 请享受旋律"
            nextLine = track.title
        } else if (lyrics.synchronized && lyrics.lines.isNotEmpty()) {
            val durationMs = durationTextToSeconds(track.durationText).coerceAtLeast(1) * 1_000L
            val positionMs = (durationMs * playerState.progress.coerceIn(0f, 1f)).toLong() + lyrics.offsetMs
            val activeIndex = lyrics.lines.indexOfLast { (it.timestampMs ?: Long.MAX_VALUE) <= positionMs }
            if (activeIndex >= 0) {
                currentLine = lyrics.lines.getOrNull(activeIndex)?.text.orEmpty().ifBlank { "…" }
                nextLine = lyrics.lines.getOrNull(activeIndex + 1)?.text.orEmpty()
            } else {
                currentLine = lyrics.lines.firstOrNull()?.text.orEmpty()
                nextLine = lyrics.lines.getOrNull(1)?.text.orEmpty()
            }
        } else {
            currentLine = lyrics.lines.firstOrNull()?.text ?: track.title
            nextLine = lyrics.lines.getOrNull(1)?.text ?: track.artist
        }
    }

    Surface(
        color = if (isHovered || isLocked) Color(0xDE16181E) else Color(0x99101217),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                        }
                    }
                }
            },
    ) {
        val content = @Composable {
            Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                // 歌词内容显示
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = currentLine,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                        fontWeight = FontWeight.Bold,
                        color = ResonanceColors.Coral,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (nextLine.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = nextLine,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = ResonanceColors.Dim,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // 悬浮悬停时出现的快捷控制栏
                AnimatedVisibility(
                    visible = isHovered,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x88000000))
                            .padding(2.dp),
                    ) {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "上一首", tint = ResonanceColors.Ivory, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onTogglePlay, modifier = Modifier.size(26.dp)) {
                            Icon(
                                if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "暂停" else "播放",
                                tint = ResonanceColors.Coral,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(onClick = onNext, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.SkipNext, contentDescription = "下一首", tint = ResonanceColors.Ivory, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onToggleLock, modifier = Modifier.size(26.dp)) {
                            Icon(
                                if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (isLocked) "已锁定" else "锁定位置",
                                tint = if (isLocked) ResonanceColors.Coral else ResonanceColors.Dim,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        IconButton(onClick = onClose, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = ResonanceColors.Dim, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        if (!isLocked) {
            windowScope.WindowDraggableArea {
                content()
            }
        } else {
            content()
        }
    }
}
