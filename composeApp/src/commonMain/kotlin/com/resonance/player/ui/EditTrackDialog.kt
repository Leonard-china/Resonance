package com.resonance.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.resonance.player.design.ResonanceColors
import com.resonance.player.model.Track

@Composable
fun EditTrackDialog(
    track: Track,
    hasLyrics: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String, newArtist: String, newAlbum: String, embedLyrics: Boolean) -> Unit,
) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    var album by remember { mutableStateOf(track.album) }
    var embedLyrics by remember { mutableStateOf(hasLyrics) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = ResonanceColors.Surface,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = ResonanceColors.Coral,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "编辑歌曲信息",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ResonanceColors.Ivory,
                    )
                }

                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("歌曲标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ResonanceColors.Coral,
                        unfocusedBorderColor = ResonanceColors.Divider,
                        focusedLabelColor = ResonanceColors.Coral,
                        unfocusedLabelColor = ResonanceColors.Muted,
                        focusedTextColor = ResonanceColors.Ivory,
                        unfocusedTextColor = ResonanceColors.Ivory,
                    ),
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("歌手 / 艺术家") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ResonanceColors.Coral,
                        unfocusedBorderColor = ResonanceColors.Divider,
                        focusedLabelColor = ResonanceColors.Coral,
                        unfocusedLabelColor = ResonanceColors.Muted,
                        focusedTextColor = ResonanceColors.Ivory,
                        unfocusedTextColor = ResonanceColors.Ivory,
                    ),
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("专辑名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ResonanceColors.Coral,
                        unfocusedBorderColor = ResonanceColors.Divider,
                        focusedLabelColor = ResonanceColors.Coral,
                        unfocusedLabelColor = ResonanceColors.Muted,
                        focusedTextColor = ResonanceColors.Ivory,
                        unfocusedTextColor = ResonanceColors.Ivory,
                    ),
                )

                if (hasLyrics) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = embedLyrics,
                            onCheckedChange = { embedLyrics = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ResonanceColors.Coral,
                                uncheckedColor = ResonanceColors.Dim,
                            ),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "同时将当前歌词写入音频文件 ID3 标签",
                            style = MaterialTheme.typography.bodySmall,
                            color = ResonanceColors.Ivory,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = ResonanceColors.Muted)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(title, artist, album, embedLyrics)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ResonanceColors.Coral,
                            contentColor = androidx.compose.ui.graphics.Color.White,
                        ),
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}
