package com.resonance.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonance.player.model.Playlist

/**
 * 免密码同步包对话框。
 * 导出：选择单个歌单或整个音乐库，打包 MP3、封面与元数据为普通 Zip。
 * 导入：直接选择 .resonance 同步包即可，无需口令。
 */
@Composable
fun SyncPackageDialog(
    exporting: Boolean,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exporting) "导出同步包" else "导入同步包") },
        text = {
            if (exporting) {
                Column {
                    Text(
                        "选择要打包的内容。MP3、封面与歌曲元数据会写入一个普通 Zip 包（.resonance），无需密码。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        PlaylistOption(
                            label = "整个音乐库",
                            detail = "全部歌单与本地曲目",
                            selected = selectedPlaylistId == null,
                            onClick = { selectedPlaylistId = null },
                        )
                        playlists.forEach { playlist ->
                            PlaylistOption(
                                label = playlist.name,
                                detail = playlist.subtitle,
                                selected = selectedPlaylistId == playlist.id,
                                onClick = { selectedPlaylistId = playlist.id },
                            )
                        }
                    }
                }
            } else {
                Text(
                    "选择一个 .resonance 同步包（普通 Zip，无需密码）。包内的歌曲、封面和歌单会合并到本机音乐库。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(if (exporting) selectedPlaylistId else null) }) {
                Text(if (exporting) "导出" else "选择同步包")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun PlaylistOption(
    label: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
