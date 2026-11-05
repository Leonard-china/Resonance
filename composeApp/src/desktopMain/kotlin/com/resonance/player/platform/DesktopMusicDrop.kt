@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.resonance.player.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceTheme
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File
import java.net.URI
import java.nio.file.Path

internal fun supportsMusicDrop(data: Transferable): Boolean = data.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
    data.transferDataFlavors.any { it.isMimeTypeEqual("text/uri-list") }

/** Local file-manager payloads only. Never fetch dropped URLs or decode paths by hand. */
internal fun droppedMusicPaths(data: Transferable): List<Path> = runCatching {
    if (data.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        (data.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
            .orEmpty().filterIsInstance<File>().take(10_000).map(File::toPath)
    } else {
        val flavor = data.transferDataFlavors.firstOrNull { it.isMimeTypeEqual("text/uri-list") }
            ?: return emptyList()
        flavor.getReaderForText(data).use { reader ->
            reader.readTextBounded(1_048_576).lineSequence().map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith('#') }.take(10_000).mapNotNull { raw ->
                    runCatching {
                        val uri = URI(raw)
                        if (!uri.scheme.equals("file", true) || !uri.authority.isNullOrEmpty()) null else Path.of(uri)
                    }.getOrNull()
                }.toList()
        }
    }
}.getOrDefault(emptyList()).distinct()

private fun java.io.Reader.readTextBounded(limit: Int): String {
    val result = StringBuilder()
    val buffer = CharArray(4096)
    while (true) {
        val count = read(buffer)
        if (count < 0) return result.toString()
        require(result.length + count <= limit) { "拖入文件列表过大" }
        result.append(buffer, 0, count)
    }
}

@Composable
internal fun DesktopMusicDropHost(services: DesktopPlatformServices, content: @Composable () -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(feedback) { if (feedback != null) { kotlinx.coroutines.delay(4_000); feedback = null } }
    val target = remember(services) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) { dragging = true }
            override fun onExited(event: DragAndDropEvent) { dragging = false }
            override fun onEnded(event: DragAndDropEvent) { dragging = false }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                dragging = false
                // Import never moves the source, even if the file manager proposes Move.
                val native = event.nativeEvent as? java.awt.dnd.DropTargetDropEvent
                if (native != null) {
                    if (native.sourceActions and java.awt.dnd.DnDConstants.ACTION_COPY == 0) {
                        feedback = "请以复制方式拖入音乐，源文件将保留"
                        return false
                    }
                    native.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY)
                }
                val paths = droppedMusicPaths(event.awtTransferable)
                val accepted = services.offerDroppedMusic(paths)
                feedback = when {
                    paths.isEmpty() -> "请拖入本机音乐、.resonance 歌单包或文件夹"
                    !accepted -> "正在导入音乐，请完成或取消后再拖入"
                    else -> null
                }
                return accepted
            }
        }
    }
    Box(Modifier.fillMaxSize().dragAndDropTarget({ supportsMusicDrop(it.awtTransferable) }, target)) {
        content()
        if (dragging) ResonanceTheme {
            Box(Modifier.matchParentSize().padding(16.dp)
                .background(Color(0xEEFFFFFF), RoundedCornerShape(24.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("松开以导入音乐", style = MaterialTheme.typography.headlineMedium)
                    Text("支持音频、.resonance 歌单包和文件夹，保留源文件", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        feedback?.let { message -> ResonanceTheme {
            Snackbar(Modifier.align(Alignment.BottomCenter).padding(24.dp)) { Text(message) }
        } }
    }
}
