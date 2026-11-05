package com.resonance.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import com.resonance.player.model.isAssistantWebLink
import com.resonance.player.model.MusicTaskEntry
import com.resonance.player.model.MusicTaskOutcome

/** Native, selectable Markdown from the upstream Compose renderer; images stay disabled. */
@Composable
internal fun AssistantReply(content: String, modifier: Modifier = Modifier) {
    val target = LocalUriHandler.current
    val links = remember(target) { object : UriHandler {
        override fun openUri(uri: String) { if (isAssistantWebLink(uri)) target.openUri(uri) }
    } }
    val markdown = rememberMarkdownState(content, retainState = true)
    CompositionLocalProvider(LocalUriHandler provides links) {
        SelectionContainer(modifier) {
            Markdown(markdown, modifier = Modifier.fillMaxWidth(),
                typography = markdownTypography(h1 = MaterialTheme.typography.titleLarge,
                    h2 = MaterialTheme.typography.titleMedium, h3 = MaterialTheme.typography.titleMedium),
                loading = { Text(content, it) }, error = { Text(content, it) })
        }
    }
}

@Composable
internal fun AssistantResearchSteps(steps: List<MusicTaskEntry>, running: Boolean) {
    var expanded by remember(steps.firstOrNull()?.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (running) "正在检索 · ${steps.size} 项步骤" else if (expanded) "收起检索过程" else "查看检索过程 · ${steps.size} 项步骤")
        }
        if (running || expanded) steps.takeLast(6).forEach { step ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val icon = when (step.outcome) {
                    MusicTaskOutcome.Completed -> Icons.Default.Check
                    MusicTaskOutcome.Running -> Icons.Default.MoreHoriz
                    else -> Icons.Default.Close
                }
                val status = when (step.outcome) {
                    MusicTaskOutcome.Completed -> "已完成"
                    MusicTaskOutcome.Running -> "进行中"
                    MusicTaskOutcome.Cancelled -> "已取消"
                    else -> "未完成"
                }
                Icon(icon, status, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f)) {
                    Text(step.title, style = MaterialTheme.typography.labelMedium)
                    if (step.progress.fileName.isNotBlank()) Text(step.progress.fileName,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (step.outcome == MusicTaskOutcome.Failed) Text(step.message,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
