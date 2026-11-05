package com.resonance.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.resonance.player.application.AudioRelinkSession
import com.resonance.player.model.*

@Composable
internal fun AudioRelinkEntry(tracks: List<Track>) {
    val feature = LocalMusicFeatures.current ?: return
    var scanning by remember { mutableStateOf(false) }
    var matches by remember { mutableStateOf<List<AudioRelinkMatch>?>(null) }
    var missingCount by remember { mutableStateOf(0) }
    TextButton(enabled = !scanning, onClick = { scanning = true; feature.launch {
        try {
            val session = AudioRelinkSession(feature.services)
            val missing = session.missing(tracks); missingCount = missing.size
            if (missing.isEmpty()) { feature.message = "当前曲目均可读取"; return@launch }
            val candidates = feature.services.chooseRelinkDirectory() ?: return@launch
            matches = matchAudioRelinks(missing, candidates)
        } finally { scanning = false }
    } }) { Text(if (scanning) "正在查找失效音频…" else "批量修复失效音频路径") }
    if (feature.message.isNotBlank()) Text(feature.message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
    matches?.let { plan ->
        var selected by remember(plan) { mutableStateOf(plan.map { it.track.id }.toSet()) }
        var applying by remember { mutableStateOf(false) }
        Dialog(onDismissRequest = { if (!applying) matches = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxWidth(.94f).widthIn(max = 680.dp).fillMaxHeight(.85f), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp)) {
                    Text("核对新的音频位置", style = MaterialTheme.typography.titleLarge)
                    Text("失效 $missingCount 首，唯一文件名匹配 ${plan.size} 首；重名或无法确认的歌曲保留原记录。只更改来源，不移动或删除文件。")
                    LazyColumn(Modifier.weight(1f)) {
                        items(plan, key = { it.track.id }, contentType = { "relink" }) { match ->
                            Row {
                                Checkbox(match.track.id in selected, enabled = !applying, onCheckedChange = { checked -> selected = if (checked) selected + match.track.id else selected - match.track.id })
                                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                                    Text("${match.track.title} · ${match.track.artist}")
                                    Text(match.candidate.relativePath, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(enabled = !applying, onClick = { matches = null }) { Text("取消") }
                        Button(enabled = !applying && selected.isNotEmpty(), onClick = { applying = true; feature.launch {
                            try {
                                val result = AudioRelinkSession(feature.services).apply(plan.filter { it.track.id in selected })
                                feature.onReload(); feature.message = "已修复 ${result.first} 首" + if (result.second.isEmpty()) "" else "；未完成：" + result.second.take(5).joinToString("；")
                                matches = null
                            } finally { applying = false }
                        } }) { Text(if (applying) "正在保存…" else "确认修复 ${selected.size} 首") }
                    }
                }
            }
        }
    }
}
