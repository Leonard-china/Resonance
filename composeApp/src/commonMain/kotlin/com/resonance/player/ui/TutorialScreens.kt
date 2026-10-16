package com.resonance.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonance.player.design.LocalReducedMotion
import com.resonance.player.design.ResonanceColors
import com.resonance.player.model.LibraryDestination
import com.resonance.player.platform.ResonanceBackHandler
import com.resonance.player.tutorial.*
import kotlinx.coroutines.delay

/** Bounds are measured in window coordinates so insets/rails/player layers share one reference. */
fun Modifier.tutorialTarget(target: TutorialTarget): Modifier = composed {
    val tutorial = LocalTutorial.current
    if (tutorial?.step?.target != target) return@composed this
    val owner = remember { Any() }
    val requester = remember { BringIntoViewRequester() }
    var placed by remember { mutableStateOf<LayoutCoordinates?>(null) }
    DisposableEffect(tutorial, target) {
        onDispose { if (tutorial.anchors[target]?.owner === owner) tutorial.anchors.remove(target) }
    }
    LaunchedEffect(tutorial.locationRequest, target) {
        // Wait until the destination transition has attached the real control.
        delay(400)
        requester.bringIntoView()
        // graphicsLayer transitions do not necessarily trigger another layout callback.
        withFrameNanos { }
        placed?.takeIf { it.isAttached }?.let { tutorial.anchors[target] = TutorialAnchor(owner, it.boundsInWindow()) }
    }
    this.bringIntoViewRequester(requester).onGloballyPositioned { coordinates ->
        placed = coordinates
        tutorial.anchors[target] = TutorialAnchor(owner, coordinates.boundsInWindow())
    }.testTag("tutorial-target-${target.name}")
}

@Composable
internal fun TutorialDialogs(tutorial: TutorialController, ready: Boolean) {
    LaunchedEffect(ready, tutorial.loaded) { if (ready) tutorial.offerWelcome() }
    if (tutorial.showWelcome) AlertDialog(
        onDismissRequest = tutorial::dismissWelcome,
        icon = { Icon(Icons.Default.Headphones, null, tint = ResonanceColors.Primary) },
        title = { Text("欢迎使用 Resonance") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("选择适合你的方式，开始探索音乐。教程可离线阅读，随时保存在设置中。")
                Button(onClick = { tutorial.quick() }, modifier = Modifier.fillMaxWidth()) { Text("快速了解") }
                Text("几页图文认识主要功能，按自己的节奏浏览。", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = tutorial::directory, modifier = Modifier.fillMaxWidth()) { Text("跟着操作") }
                Text("在实际界面逐步学习，按章节保存进度。", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = tutorial::dismissWelcome) { Text("跳过，稍后再看") } },
    )
    TutorialCatalog.chapter(tutorial.finishedChapterId)?.let { chapter ->
        val progress = tutorial.progress.chapters[chapter.id] ?: ChapterProgress()
        AlertDialog(
            onDismissRequest = { tutorial.finishedChapterId = null },
            title = { Text(if (progress.skipped.isEmpty()) "这一章学完了" else "这一章已浏览完") },
            text = { Text("${chapter.title}：完成 ${progress.completed.size} 步，跳过 ${progress.skipped.size} 步。可以随时从教程目录重新练习。") },
            confirmButton = { TextButton(onClick = { tutorial.finishedChapterId = null }) { Text("返回教程目录") } },
        )
    }
}

@Composable
internal fun TutorialScreen(compact: Boolean, onBack: () -> Unit) {
    val tutorial = LocalTutorial.current ?: return
    val reading = TutorialCatalog.chapter(tutorial.readingChapterId)
    ResonanceBackHandler(true) {
        when { tutorial.quickMode -> tutorial.directory(); reading != null -> tutorial.readingChapterId = null; else -> onBack() }
    }
    when {
        tutorial.quickMode -> QuickTutorial(tutorial, compact)
        reading != null -> ToolPage(reading.title, { tutorial.readingChapterId = null }, compact, backLabel = "返回教程目录") {
            Text(reading.description, color = ResonanceColors.Muted)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { tutorial.start(reading) }) { Text("跟着操作") }
            reading.steps.forEachIndexed { index, step ->
                SectionHeader("${index + 1}. ${step.title}")
                Text(step.body, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
            }
        }
        else -> ToolPage("使用教程", onBack, compact, backLabel = "返回设置") {
            Text("快速认识，也可以一步步学会。", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("学习进度保存在本机，退出后可回来继续。", color = ResonanceColors.Muted, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
            TutorialCard("快速了解", if (tutorial.progress.quickCompleted) "已看完 · 随时重新浏览" else "四页图文 · 从音乐导入到更多功能") {
                Button(onClick = { tutorial.quick(restart = tutorial.progress.quickCompleted) }) { Text(if (tutorial.progress.quickCompleted) "重新浏览" else "开始浏览") }
            }
            SectionHeader("跟着操作")
            TutorialCatalog.chapters.forEachIndexed { index, chapter ->
                val saved = tutorial.progress.chapters[chapter.id] ?: ChapterProgress()
                val ended = saved.nextStep >= chapter.steps.size
                val status = when {
                    ended && saved.skipped.isEmpty() -> "已完成"
                    ended -> "已浏览 · ${saved.skipped.size} 步跳过"
                    saved.nextStep > 0 -> "学到第 ${saved.nextStep + 1} 步"
                    else -> "${chapter.steps.size} 步"
                }
                TutorialCard("${index + 1}. ${chapter.title}", "${chapter.description} · $status") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { tutorial.readingChapterId = chapter.id }) { Text("图文说明") }
                        Button(onClick = { tutorial.start(chapter) }, modifier = Modifier.testTag("tutorial-start-${chapter.id}")) {
                            Text(if (ended) "重新练习" else if (saved.nextStep > 0) "继续学习" else "跟着操作")
                        }
                        if (saved.nextStep > 0 && !ended) TextButton(onClick = { tutorial.start(chapter, restart = true) }) { Text("从头练习") }
                    }
                }
            }
            SectionHeader("常见问题")
            listOf(
                "没有扫描到歌曲？" to "先确认音频权限或目录授权，Android 只能读取已授权范围。KGG 请在 Windows 使用匹配的本机酷狗密钥转换。",
                "为什么歌词没有滚动？" to "当前来源可能只有纯文本。可查找本地 LRC/KRC，或按歌词进阶章节主动生成同步歌词。",
                "登录或下载失败？" to "检查网络、登录凭据与任务错误。遇到 B 站官方验证需自行完成，教程不能绕过服务限制。",
                "AI 和语音如何计费？" to "使用自己的服务商账户。聊天按其计费规则，音频识别按时长；应用预算只作本地限制，不等同于实际账单。",
            ).forEach { (title, body) -> SectionHeader(title); Text(body, modifier = Modifier.padding(bottom = 10.dp)) }
        }
    }
}

@Composable
private fun TutorialCard(title: String, description: String, actions: @Composable () -> Unit) {
    Surface(color = ResonanceColors.Raised, shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ResonanceColors.Divider), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
            actions()
        }
    }
}

@Composable
private fun QuickTutorial(tutorial: TutorialController, compact: Boolean) {
    val page = tutorial.progress.quickPage
    val reduced = LocalReducedMotion.current
    ToolPage("快速了解", tutorial::directory, compact, backLabel = "返回教程目录") {
        Text("${page + 1} / ${TutorialCatalog.quick.size}", color = ResonanceColors.Primary, style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(progress = { (page + 1f) / TutorialCatalog.quick.size }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp))
        AnimatedContent(page, transitionSpec = { fadeIn(tween(if (reduced) 0 else 160)) togetherWith fadeOut(tween(if (reduced) 0 else 100)) }, label = "quickTutorial") { index ->
            val lesson = TutorialCatalog.quick[index]
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TutorialIllustration(index, lesson)
                Text(lesson.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(lesson.body, style = MaterialTheme.typography.bodyLarge)
                lesson.points.forEach { point ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp), tint = ResonanceColors.Primary)
                        Text(point, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (page > 0) OutlinedButton(onClick = { tutorial.quickPage(page - 1) }) { Text("上一步") }
            if (page < TutorialCatalog.quick.lastIndex) Button(onClick = { tutorial.quickPage(page + 1) }) { Text("下一步") }
            else {
                Button(onClick = tutorial::finishQuick) { Text("开始使用") }
                OutlinedButton(onClick = { tutorial.finishQuick(); tutorial.directory() }) { Text("继续操作教学") }
                TextButton(onClick = { tutorial.finishQuick(); tutorial.onNavigate(LibraryDestination.Import) }) { Text("导入音乐") }
            }
            TextButton(onClick = { tutorial.dismissWelcome(); tutorial.onNavigate(LibraryDestination.Library) }) { Text("跳过教程") }
        }
        Text("随时在「设置 → 使用教程」中继续。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted, modifier = Modifier.padding(top = 12.dp))
    }
}

/** Code-native illustration follows theme/font size without shipping screenshots or user data. */
@Composable
private fun TutorialIllustration(page: Int, lesson: QuickLesson) {
    val icon = when (page) { 0 -> Icons.Default.LibraryMusic; 1 -> Icons.Default.Headphones; 2 -> Icons.Default.AutoAwesome; else -> Icons.AutoMirrored.Filled.QueueMusic }
    Surface(color = ResonanceColors.PrimarySoft, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, Modifier.size(30.dp), tint = ResonanceColors.Primary)
                Text(lesson.illustration.first(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("界面示意", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted)
            }
            lesson.illustration.drop(1).forEach { line ->
                Surface(color = ResonanceColors.Canvas.copy(alpha = 0.8f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(line, Modifier.padding(8.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Passive scrim: actual controls stay clickable, and the card avoids the highlighted target. */
@Composable
internal fun TutorialCoach(tutorial: TutorialController) {
    val step = tutorial.step ?: return
    val chapter = tutorial.chapter ?: return
    var origin by remember { mutableStateOf(Offset.Zero) }
    var collapsed by remember(step.id, chapter.id) { mutableStateOf(false) }
    val density = LocalDensity.current
    val highlight = tutorial.anchors[step.target]?.bounds?.translate(-origin)
    val missingTrack = step.needsTrack && !tutorial.hasTrack
    val color = ResonanceColors.Primary
    ResonanceBackHandler(true, tutorial::pause)
    BoxWithConstraints(Modifier.fillMaxSize().imePadding().onGloballyPositioned { origin = it.positionInWindow() }.testTag("tutorial-coach")) {
        val width = with(density) { maxWidth.toPx() }
        val height = with(density) { maxHeight.toPx() }
        val visible = highlight?.intersect(Rect(0f, 0f, width, height))?.takeIf { it.width > 1f && it.height > 1f }
        Canvas(Modifier.matchParentSize()) {
            val mask = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                visible?.let { addRoundRect(RoundRect(it.inflate(4.dp.toPx()), 12.dp.toPx(), 12.dp.toPx())) }
            }
            drawPath(mask, Color.Black.copy(alpha = 0.38f))
            visible?.let { drawRoundRect(color, it.topLeft - Offset(4.dp.toPx(), 4.dp.toPx()),
                androidx.compose.ui.geometry.Size(it.width + 8.dp.toPx(), it.height + 8.dp.toPx()),
                androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()), style = Stroke(2.dp.toPx())) }
        }
        val top = visible != null && visible.center.y > height / 2f
        Surface(
            color = ResonanceColors.Canvas,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
            shadowElevation = 12.dp,
            modifier = Modifier.align(if (top) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(12.dp).widthIn(max = 480.dp).fillMaxWidth().heightIn(max = maxHeight * if (collapsed) 0.27f else 0.46f)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${chapter.title} · ${tutorial.stepIndex + 1}/${chapter.steps.size}", style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.weight(1f))
                    IconButton(onClick = { collapsed = !collapsed }) {
                        Icon(if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess, if (collapsed) "展开说明" else "收起说明")
                    }
                    IconButton(onClick = tutorial::pause) { Icon(Icons.Default.Close, "暂停教学") }
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (!collapsed) {
                    Text(if (missingTrack) "先选择一首歌曲再继续这一操作。还没有音乐时可跳过本步，稍后在教程中重新练习。" else step.body,
                        style = MaterialTheme.typography.bodyMedium)
                    if (visible == null && !missingTrack) Text("可以滚动查找目标，或点击“定位本步”。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (step.completion == null) Button(onClick = { tutorial.advance() }) { Text("已了解，下一步") }
                    else if (missingTrack) Button(onClick = { tutorial.onNavigate(LibraryDestination.Library) }) { Text("选择歌曲") }
                    TextButton(onClick = { tutorial.advance(skipped = true) }) { Text("跳过本步") }
                    if (!collapsed) TextButton(onClick = tutorial::locate) { Text("定位本步") }
                }
                }
            }
        }
    }
}
