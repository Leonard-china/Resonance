package com.resonance.player

import android.graphics.*
import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import com.resonance.player.ui.NowPlayingOverlay
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.sin

/** Opt-in camera rig for the production Compose UI. No user library, credentials, or paid services. */
class PromoCaptureDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()
    private val args = InstrumentationRegistry.getArguments()
    private val marks = StringBuilder()
    private val start = SystemClock.elapsedRealtime()

    private fun mark(label: String) {
        marks.append("$label,${SystemClock.elapsedRealtime() - start}\n")
    }

    private fun hold(seconds: Double) {
        val until = SystemClock.elapsedRealtime() + (seconds * 1_000).toLong()
        while (SystemClock.elapsedRealtime() < until) {
            ui.mainClock.advanceTimeBy(32)
            Thread.sleep(24)
        }
    }

    /** Original procedural sleeve artwork; no downloaded album art or private audio. */
    private fun sleeve(index: Int): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "promo-sleeve-$index.png")
        val bitmap = Bitmap.createBitmap(960, 960, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val palettes = listOf(intArrayOf(0xff1b514e.toInt(), 0xff081c25.toInt(), 0xffa9d9c4.toInt()),
            intArrayOf(0xffad774c.toInt(), 0xff312e3b.toInt(), 0xfff0cda2.toInt()),
            intArrayOf(0xff545f91.toInt(), 0xff1e253e.toInt(), 0xffd1d6ee.toInt()))
        val colors = palettes[index % palettes.size]
        p.shader = LinearGradient(0f, 0f, 960f, 960f, colors[0], colors[1], Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, 960f, 960f, p)
        p.shader = RadialGradient(660f, 280f, 380f, intArrayOf(colors[2], colors[2] and 0xffffff), null, Shader.TileMode.CLAMP)
        c.drawCircle(660f, 280f, 380f, p)
        p.shader = null
        p.color = colors[2]; p.alpha = 200
        c.drawCircle(650f, 265f, 110f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.4f
        repeat(72) { row ->
            p.color = colors[2]; p.alpha = 40 + row / 2
            val path = Path()
            for (x in -20..980 step 8) {
                val y = 385 + row * 9 + sin(x / 165.0 + row * .065 + index) * (34 + row * .4)
                if (x == -20) path.moveTo(x.toFloat(), y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat())
            }
            c.drawPath(path, p)
        }
        p.style = Paint.Style.FILL; p.alpha = 230; p.color = Color.WHITE
        p.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); p.textSize = 22f
        c.drawText("R E S O N A N C E   S E S S I O N S", 64f, 94f, p)
        p.textSize = 52f
        c.drawText(listOf("AFTER THE TIDE", "SLOW MORNING", "IN THE QUIET")[index % 3], 64f, 841f, p)
        p.textSize = 18f; p.alpha = 150
        c.drawText("O R I G I N A L   S O U N D S C A P E S     /     0${index + 1}", 67f, 890f, p)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file.absolutePath
    }

    @Test fun recordNativePlayerStory() {
        assumeTrue("Pass -e capturePromo true to record", args.getString("capturePromo") == "true")
        marks.append("epoch,$start\n")
        ui.mainClock.autoAdvance = false
        val dark = args.getString("promoTheme") == "dark"
        val tracks = listOf("此刻，听见共鸣", "晚风与海", "沿着光，慢慢走", "城市入夜以后", "把心事交给旋律").mapIndexed { i, title ->
            Track("promo-$i", title, "Resonance Sessions", "听见此刻", "2:40", i + 3, artworkPath = sleeve(i % 3))
        }
        val text = listOf(
            "把忙碌留在窗外" to "Leave the rush outside",
            "让晚风轻轻经过" to "Let the evening breeze pass by",
            "一段旋律，一次呼吸" to "A melody, a breath",
            "把此刻交给音乐" to "Give this moment to music",
            "让心事慢慢找到回响" to "Let your thoughts find their echo",
            "在每一句里，遇见自己" to "Find yourself between the lines",
            "走得再远，也听得见" to "Wherever you go, the music follows",
            "那些温柔，始终在这里" to "The quiet warmth is always here",
            "不必赶路，慢慢听" to "There is no hurry. Just listen",
            "喜欢的声音，值得停留" to "Stay with the sounds you love",
            "让旋律照亮平凡的一天" to "Let a melody light up an ordinary day",
            "此刻，听见共鸣" to "Right here. In resonance",
        )
        ui.setContent {
            var queue by remember { mutableStateOf(tracks) }
            var player by remember { mutableStateOf(PlayerState(currentTrack = tracks.first(), positionMs = 8_000, progress = .05f)) }
            LaunchedEffect(player.isPlaying) {
                while (player.isPlaying) {
                    kotlinx.coroutines.delay(80)
                    val position = (player.positionMs ?: 0) + 80
                    player = player.copy(positionMs = position, progress = position / 160_000f)
                }
            }
            val lyrics = LyricsUiState.Ready(Lyrics(player.currentTrack!!.id,
                text.mapIndexed { i, (original, translation) -> LyricLine(i * 3_600L, original, translation) }, true, false, "Resonance Sessions"))
            ResonanceTheme(if (dark) ThemeMode.Dark else ThemeMode.Light, reducedMotion = false) {
                NowPlayingOverlay(playerState = player, queue = queue, lyricsState = lyrics,
                    onDismiss = {}, onTogglePlay = { player = player.copy(isPlaying = !player.isPlaying) },
                    onPrevious = { player = player.copy(currentTrack = tracks.first(), positionMs = 8_000) },
                    onNext = { player = player.copy(currentTrack = tracks[1], positionMs = 8_000) },
                    onToggleShuffle = { player = player.copy(shuffleEnabled = !player.shuffleEnabled) },
                    onCycleRepeat = { player = player.copy(repeatMode = RepeatMode.All) },
                    onSeek = { player = player.copy(progress = it, positionMs = (it * 160_000).toLong()) },
                    onTrackSelected = { player = player.copy(currentTrack = it, positionMs = 8_000) },
                    onToggleFavorite = { selected -> player = player.copy(currentTrack = selected.copy(isFavorite = !selected.isFavorite)) },
                    onRefreshLyrics = {}, onAdjustLyricsOffset = {},
                    onPlayNext = { selected -> queue = listOf(queue.first(), selected) + queue.drop(1).filter { it.id != selected.id } },
                    onRemoveFromQueue = { selected -> queue = queue.filterNot { it.id == selected.id } },
                    onClearQueue = { queue = listOf(player.currentTrack!!) },
                    onMoveInQueueOrder = { from, to -> queue = queue.toMutableList().apply { add(to, removeAt(from)) } },
                )
            }
        }
        hold(1.0)
        mark("ready")
        hold(3.0)
        ui.onNodeWithContentDescription("播放").performClick()
        mark("cover")
        hold(9.0)
        ui.onNodeWithText("歌词", useUnmergedTree = true).performClick()
        mark("lyrics")
        hold(10.0)
        ui.onNodeWithTag("player-lyrics-list", useUnmergedTree = true).performTouchInput {
            swipe(Offset(width / 2f, height * .4f), Offset(width / 2f, height * .6f), durationMillis = 650)
        }
        mark("browse")
        hold(2.0)
        ui.onNodeWithTag("lyric-return-current", useUnmergedTree = true).performClick()
        mark("return")
        hold(4.0)
        ui.onNodeWithText("队列", useUnmergedTree = true).performClick()
        mark("queue")
        hold(3.0)
        ui.onAllNodesWithContentDescription("置顶为下一首").onLast().performClick()
        mark("reorder")
        hold(3.0)
        ui.onNodeWithText("封面", useUnmergedTree = true).performClick()
        hold(.6)
        ui.onNodeWithContentDescription("下一首").performClick()
        mark("next")
        hold(8.0)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "promo-${if (dark) "dark" else "light"}-marks.csv").writeText(marks.toString())
    }
}
