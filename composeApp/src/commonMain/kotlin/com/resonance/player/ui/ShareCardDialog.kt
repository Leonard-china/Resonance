package com.resonance.player.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.resonance.player.design.ResonanceColors
import com.resonance.player.model.Playlist
import com.resonance.player.model.TastePersona
import com.resonance.player.model.Track

sealed class ShareCardContent {
    data class PersonaCard(val persona: TastePersona) : ShareCardContent()
    data class PlaylistCard(val playlist: Playlist) : ShareCardContent()
}

@Composable
fun ShareCardDialog(
    content: ShareCardContent,
    onDismiss: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ResonanceColors.Raised,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(16.dp),
        ) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Top close
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = ResonanceColors.Muted)
                    }
                }

                // Poster Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E2836),
                                    Color(0xFF0F1722),
                                    Color(0xFF0A0E17),
                                )
                            )
                        )
                        .border(1.dp, ResonanceColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    when (content) {
                        is ShareCardContent.PersonaCard -> PersonaCardView(content.persona)
                        is ShareCardContent.PlaylistCard -> PlaylistCardView(content.playlist)
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Actions
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ResonanceColors.TextPrimary),
                    ) {
                        Text("完成")
                    }

                    Button(
                        onClick = {
                            val text = when (content) {
                                is ShareCardContent.PersonaCard -> "【Resonance 听歌性格画像】\n称号：${content.persona.title} (${content.persona.subtitle})\n主导氛围：${content.persona.dominantVibe}\n评语：${content.persona.summary}"
                                is ShareCardContent.PlaylistCard -> "【Resonance 精选歌单】\n歌单名：${content.playlist.name}\n收录曲目数：${content.playlist.tracks.size} 首\n精选歌曲：${content.playlist.tracks.take(5).joinToString("、") { it.title }}"
                            }
                            clipboardManager.setText(AnnotatedString(text))
                            copied = true
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ResonanceColors.Primary,
                            contentColor = ResonanceColors.Canvas,
                        ),
                    ) {
                        Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (copied) "已复制文案" else "复制分享卡片")
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonaCardView(persona: TastePersona) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = ResonanceColors.Primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ResonanceColors.Primary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text("RESONANCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = ResonanceColors.Primary)
            }
            Text("TASTE PROFILE", fontSize = 10.sp, color = ResonanceColors.Dim, letterSpacing = 1.sp)
        }

        Spacer(Modifier.height(18.dp))

        Text(persona.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text(persona.subtitle, fontSize = 13.sp, color = ResonanceColors.Primary, fontWeight = FontWeight.Medium)

        Spacer(Modifier.height(14.dp))

        Text(
            persona.summary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = Color(0xFFCAD5E2),
        )

        Spacer(Modifier.height(16.dp))

        // Mini Radar Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center,
        ) {
            TasteRadarChart(
                dimensions = persona.dimensions,
                modifier = Modifier.size(150.dp),
                fillColor = ResonanceColors.Primary.copy(alpha = 0.4f),
                lineColor = ResonanceColors.Primary,
                gridColor = Color(0x334E6580),
            )
        }

        Spacer(Modifier.height(14.dp))

        HorizontalDivider(color = Color(0x22FFFFFF))
        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("主导氛围：${persona.dominantVibe}", fontSize = 11.sp, color = ResonanceColors.Dim)
            Text("共收录 ${persona.totalTracksAnalyzed} 轨声波", fontSize = 11.sp, color = ResonanceColors.Dim)
        }
    }
}

@Composable
private fun PlaylistCardView(playlist: Playlist) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = ResonanceColors.Primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = ResonanceColors.Primary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text("RESONANCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = ResonanceColors.Primary)
            }
            Text("PLAYLIST POSTER", fontSize = 10.sp, color = ResonanceColors.Dim, letterSpacing = 1.sp)
        }

        Spacer(Modifier.height(18.dp))

        Text(playlist.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        if (playlist.subtitle.isNotBlank()) {
            Text(playlist.subtitle, fontSize = 13.sp, color = ResonanceColors.Muted)
        }

        Spacer(Modifier.height(14.dp))

        // Track Highlights
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x22FFFFFF))
                .padding(12.dp)
        ) {
            Text("收录精选 (${playlist.tracks.size} 首)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ResonanceColors.Primary)
            Spacer(Modifier.height(8.dp))
            val displayTracks = playlist.tracks.take(5)
            for ((index, track) in displayTracks.withIndex()) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${index + 1}.",
                        fontSize = 12.sp,
                        color = ResonanceColors.Dim,
                        modifier = Modifier.width(22.dp)
                    )
                    Text(
                        track.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        track.artist,
                        fontSize = 11.sp,
                        color = ResonanceColors.Dim,
                        maxLines = 1,
                    )
                }
            }
            if (playlist.tracks.size > 5) {
                Text(
                    "… 及其他 ${playlist.tracks.size - 5} 首曲目",
                    fontSize = 10.sp,
                    color = ResonanceColors.Dim,
                    modifier = Modifier.padding(top = 4.dp, start = 22.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color(0x22FFFFFF))
        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("私享原生播放器 · 沉浸声音艺术", fontSize = 10.sp, color = ResonanceColors.Dim)
            Text("Resonance Player", fontSize = 10.sp, color = ResonanceColors.Dim)
        }
    }
}
