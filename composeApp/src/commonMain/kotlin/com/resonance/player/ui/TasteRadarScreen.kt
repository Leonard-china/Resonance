package com.resonance.player.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.model.TasteAnalyticsEngine
import com.resonance.player.model.TasteDimensions
import com.resonance.player.model.TastePersona
import com.resonance.player.model.Track
import com.resonance.player.platform.ResonanceBackHandler
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TasteRadarScreen(
    tracks: List<Track>,
    onBack: () -> Unit,
    onOpenShareCard: (TastePersona) -> Unit,
    compact: Boolean = false,
) {
    ResonanceBackHandler(onBack = onBack)
    val persona = remember(tracks) { TasteAnalyticsEngine.analyze(tracks) }
    val scrollState = rememberScrollState()
    val pagePadding = if (compact) 16.dp else 32.dp

    Column(
        Modifier
            .fillMaxSize()
            .background(ResonanceColors.Canvas)
    ) {
        // Top Bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = pagePadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = ResonanceColors.TextPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "音乐品味画像",
                style = MaterialTheme.typography.titleLarge,
                color = ResonanceColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onOpenShareCard(persona) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ResonanceColors.Primary,
                    contentColor = ResonanceColors.Canvas,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("生成海报", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = pagePadding, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Persona Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ResonanceColors.Raised),
            ) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ResonanceColors.Primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ResonanceColors.Primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(persona.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ResonanceColors.TextPrimary)
                            Text(persona.subtitle, fontSize = 13.sp, color = ResonanceColors.Muted)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        persona.summary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = ResonanceColors.Muted,
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        InfoPill("分析歌曲", "${persona.totalTracksAnalyzed} 首")
                        InfoPill("主导氛围", persona.dominantVibe)
                        if (persona.topArtists.isNotEmpty()) {
                            InfoPill("偏好歌手", persona.topArtists.first())
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Radar Chart Section
            Text(
                "六维听觉品味雷达",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ResonanceColors.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(ResonanceColors.Raised, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                TasteRadarChart(
                    dimensions = persona.dimensions,
                    modifier = Modifier.size(260.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Dimension breakdown
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ResonanceColors.Raised),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("维度细则", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ResonanceColors.TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    DimensionRow("流行敏锐度", persona.dimensions.popVibrancy, "捕捉当代流行与高频律动的敏感程度")
                    DimensionRow("复古情怀", persona.dimensions.retroNostalgia, "对岁月原声、经典金曲与叙事沉淀的眷恋")
                    DimensionRow("小众探索度", persona.dimensions.indieExploration, "突破主流旋律、挖掘独立与纯音原声的意愿")
                    DimensionRow("情绪共鸣度", persona.dimensions.emotionalResonance, "细腻慢歌、深情文字与内心独白的共鸣强度")
                    DimensionRow("节奏活力感", persona.dimensions.rhythmicEnergy, "充满动感节拍、能量涌动与高速律动的偏好")
                    DimensionRow("流派多样性", persona.dimensions.genreDiversity, "横跨多种曲风与不同风格歌手的杂食广度")
                }
            }
        }
    }
}

@Composable
private fun InfoPill(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = ResonanceColors.Dim)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ResonanceColors.Primary)
    }
}

@Composable
private fun DimensionRow(label: String, score: Int, desc: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ResonanceColors.TextPrimary)
            Text("$score / 100", fontSize = 12.sp, color = ResonanceColors.Primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = ResonanceColors.Primary,
            trackColor = ResonanceColors.Divider,
        )
        Text(desc, fontSize = 11.sp, color = ResonanceColors.Dim, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun TasteRadarChart(
    dimensions: TasteDimensions,
    modifier: Modifier = Modifier,
    fillColor: Color = ResonanceColors.Primary.copy(alpha = 0.35f),
    lineColor: Color = ResonanceColors.Primary,
    gridColor: Color = ResonanceColors.Divider,
) {
    val labels = listOf("流行敏锐", "复古情怀", "小众探索", "情绪共鸣", "节奏活力", "流派多样")
    val a1 by animateFloatAsState(targetValue = dimensions.popVibrancy / 100f, animationSpec = tween(durationMillis = 800))
    val a2 by animateFloatAsState(targetValue = dimensions.retroNostalgia / 100f, animationSpec = tween(durationMillis = 800))
    val a3 by animateFloatAsState(targetValue = dimensions.indieExploration / 100f, animationSpec = tween(durationMillis = 800))
    val a4 by animateFloatAsState(targetValue = dimensions.emotionalResonance / 100f, animationSpec = tween(durationMillis = 800))
    val a5 by animateFloatAsState(targetValue = dimensions.rhythmicEnergy / 100f, animationSpec = tween(durationMillis = 800))
    val a6 by animateFloatAsState(targetValue = dimensions.genreDiversity / 100f, animationSpec = tween(durationMillis = 800))
    val animatedValues = listOf(a1, a2, a3, a4, a5, a6)
    val pointBgColor = ResonanceColors.Raised

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = (size.minDimension / 2f) * 0.72f
        val numSides = 6
        val angleStep = (2.0 * Math.PI / numSides).toFloat()
        val startAngle = (-Math.PI / 2.0).toFloat()

        // 1. Draw web grid rings (5 rings)
        for (step in 1..5) {
            val r = radius * (step / 5f)
            val ringPath = Path()
            for (i in 0 until numSides) {
                val angle = startAngle + i * angleStep
                val x = centerX + r * cos(angle)
                val y = centerY + r * sin(angle)
                if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
            }
            ringPath.close()
            drawPath(ringPath, gridColor, style = Stroke(width = 1.dp.toPx()))
        }

        // 2. Draw spokes
        for (i in 0 until numSides) {
            val angle = startAngle + i * angleStep
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            drawLine(gridColor, Offset(centerX, centerY), Offset(x, y), strokeWidth = 1.dp.toPx())
        }

        // 3. Draw user taste polygon
        val polyPath = Path()
        for (i in 0 until numSides) {
            val angle = startAngle + i * angleStep
            val r = radius * animatedValues[i]
            val x = centerX + r * cos(angle)
            val y = centerY + r * sin(angle)
            if (i == 0) polyPath.moveTo(x, y) else polyPath.lineTo(x, y)
        }
        polyPath.close()

        drawPath(polyPath, fillColor)
        drawPath(polyPath, lineColor, style = Stroke(width = 2.5.dp.toPx()))

        // 4. Draw points
        for (i in 0 until numSides) {
            val angle = startAngle + i * angleStep
            val r = radius * animatedValues[i]
            val x = centerX + r * cos(angle)
            val y = centerY + r * sin(angle)
            drawCircle(pointBgColor, radius = 5.dp.toPx(), center = Offset(x, y))
            drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(x, y))
        }
    }
}
