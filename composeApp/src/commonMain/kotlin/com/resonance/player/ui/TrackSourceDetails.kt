package com.resonance.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resonance.player.model.MediaOrigin
import com.resonance.player.model.Track

@Composable
internal fun TrackSourceDetails(track: Track, origin: MediaOrigin?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(track.artist + " · " + track.album, style = MaterialTheme.typography.bodyMedium)
        Text("音频：" + (origin?.version ?: "版本未确认"))
        Text("来源：" + (origin?.source?.takeIf(String::isNotBlank) ?: "本地文件／旧导入记录"))
        Text("封面：" + (origin?.coverKind ?: if (track.artworkPath != null) "已有图片，来源未记录" else "本地内嵌封面或待匹配"))
        if (!origin?.coverSource.isNullOrBlank()) Text("封面目录：" + origin.coverSource)
        if (!origin?.albumId.isNullOrBlank()) Text("专辑标识：" + origin.albumId)
        val technical = listOfNotNull(
            origin?.format?.takeIf(String::isNotBlank)?.uppercase(),
            origin?.bitrate?.takeIf { it > 0 }?.let { (it / 1000).toString() + " kbps" },
            origin?.sampleRate?.takeIf { it > 0 }?.let { it.toString() + " Hz" },
            origin?.channels?.takeIf { it > 0 }?.let { it.toString() + " 声道" },
        )
        if (technical.isNotEmpty()) Text("实际文件：" + technical.joinToString(" · "))
        if (origin != null) Text(if (origin.preserved) "保存原音轨；编码参数不能证明母带质量。" else "经过格式转换／旧记录未标明；不根据输出码率推定原音质。", style = MaterialTheme.typography.bodySmall)
        if (!origin?.evidence.isNullOrBlank()) Text(origin.evidence, style = MaterialTheme.typography.bodySmall)
        if (track.sourceUri.isNullOrBlank()) Text("本地未下载，可到“下载与空间”重新获取。", color = MaterialTheme.colorScheme.primary)
    }
}
