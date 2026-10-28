package com.resonance.player.ui
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.resonance.player.design.galleryPlinth
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.VideoLibrary

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import com.resonance.player.platform.loadSponsorQr
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.resonance.player.model.DeepSeekConfig
import com.resonance.player.model.DeepSeekTestResult
import com.resonance.player.model.TrackFilterOption
import com.resonance.player.model.TrackSortOption
import com.resonance.player.platform.ResonanceBackHandler
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.LocalReducedMotion
import com.resonance.player.design.LocalAppForeground
import com.resonance.player.design.LocalAppearance
import com.resonance.player.design.LocalGlassState
import com.resonance.player.design.resonanceSpring
import com.resonance.player.design.motionDuration
import dev.chrisbanes.haze.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import com.resonance.player.design.ResonanceShapes
import com.resonance.player.design.resonanceGlass
import com.resonance.player.design.resonancePressable
import com.resonance.player.ui.common.FluidAmbientCanvas
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.sin
import com.resonance.player.model.APP_VERSION
import com.resonance.player.model.APP_RELEASE_DATE
import com.resonance.player.model.APP_RELEASE_HIGHLIGHTS
import com.resonance.player.model.LibraryDestination
import com.resonance.player.model.LyricsUiState
import com.resonance.player.model.PlayerState
import com.resonance.player.model.Playlist
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.ThemeMode
import com.resonance.player.model.Track
import com.resonance.player.platform.decodeArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.resonance.player.tutorial.TutorialTarget

@Composable
internal fun ProfileScreen(
    compact: Boolean,
    onOpenImport: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val features = LocalMusicFeatures.current
    val pagePadding = if (compact) 16.dp else 32.dp
    Column(Modifier.widthIn(max = 880.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = pagePadding)) {
        Text(
            "工具",
            style = MaterialTheme.typography.headlineLarge,
            color = ResonanceColors.TextPrimary,
            modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),
        )
        SettingRow(
            icon = Icons.Default.LibraryMusic,
            title = "导入音乐",
            subtitle = if (features?.services?.supportsKgg == true) "扫描本机、转换 KGMA/KGG、B 站音频" else "扫描本机、转换 KGMA、B 站音频",
            onClick = onOpenImport,
        )
        SettingRow(
            icon = Icons.Outlined.Sync,
            title = "歌单导入与导出",
            subtitle = "完整保存音频、封面与歌单信息",
            onClick = onOpenSync,
        )
        SettingRow(Icons.Default.AutoAwesome, "音乐助手", "聊天管理、歌单截图和每日推荐", { features?.onNavigate?.invoke(LibraryDestination.Assistant) })
        SettingRow(Icons.Default.CleaningServices, "曲库智能管家", "智能洗歌、标签清洗与版本去重", { features?.onNavigate?.invoke(LibraryDestination.SmartManager) })
        SettingRow(Icons.Default.AutoAwesome, "音乐品味画像", "六维品味雷达图与听歌性格分析", { features?.onNavigate?.invoke(LibraryDestination.TasteRadar) })
        SettingRow(Icons.Default.LibraryMusic, "歌源", "搜索专辑单曲 · 导入 MusicFree 源", { features?.onNavigate?.invoke(LibraryDestination.Sources) })
        SettingRow(Icons.Default.FolderOpen, "下载与空间", "管理下载、临时空间和重复文件", { features?.onNavigate?.invoke(LibraryDestination.Storage) })
        SettingRow(
            icon = Icons.Default.Settings,
            title = "设置",
            subtitle = "外观、音乐库与播放偏好",
            onClick = onOpenSettings,
        )
        SettingRow(
            icon = Icons.Default.Info,
            title = "关于 Resonance",
            subtitle = "版本 v$APP_VERSION · 本地优先",
            onClick = onOpenAbout,
        )
    }
}

@Composable
internal fun AboutScreen(compact: Boolean, onBack: () -> Unit) {
    var showSponsorDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    ToolPage("关于 Resonance", onBack, compact) {
        val appFeatures = listOf(
            "本地优先架构与数据自治" to "Resonance 没有中心化用户系统或服务端，不收集任何用户隐私数据。所有曲库元数据、播放统计、歌单索引、本地分析缓存与 AI 会话均持久化保存在本机，用户拥有对自己音频资产的完全控制权。",
            "全格式无损播放与自适应响度平衡" to "支持 FLAC、APE、WAV、MP3、AAC、OGG 等几乎所有主流音频格式。内置自主研发的无损响度分析算法（统一播放音量），自动平滑补偿不同歌曲忽大忽小的差异，在播放管线中动态计算增益，完全不改写源音频文件。",
            "加密格式（KGMA / KGG）无损提取转码" to "支持直接导入并离线转换 KGMA 与 KGG 专有加密音乐文件为通用高品质音频格式，还原纯净音频流，免除平台绑定限制。",
            "AI 智能助手与多会话隔离管理" to "深度整合千问（Qwen）、DeepSeek、豆包（Doubao）等主流大语言模型。支持多会话独立切换与隔离管理，会话内自动使用滑动窗口严控 Token 消耗；支持长截图识别解析并一键入库为歌单，提供曲库风格偏好分析与每日个性化推荐。",
            "阿里云百炼 Paraformer 音频对齐歌词" to "针对缺乏时间轴的纯文本歌词或本地曲目，可一键调用高精度语音大模型，根据人声演唱识别逐字逐句时间戳，生成精准双语/原文同步歌词并写入本地持久化缓存。",
            "系统级悬浮置顶桌面歌词" to "Windows 端支持置顶悬浮、透明背景与鼠标穿透；Android 端深度适配系统悬浮窗权限，切换状态即时反馈，在任意应用界面上轻松掌控实时律动。",
            "B 站高品质音频提取与智能标签" to "支持输入 B 站视频链接或 BV 号（支持多 P 分集选曲），直接提取高码率音频，智能匹配原视频高清封面并自动整理歌手与专辑信息。",
            "无损跨端数据归档与迁移 (.resonance)" to "具备防篡改校验机制的专属歌单归档方案。一键导出打包完整音频文件、内嵌及外挂歌词、高清专辑封面与歌单结构，轻松在 Android 与 Windows 之间无缝迁移整个曲库。",
            "流体微光设计与沉浸式灵动岛" to "基于曲目封面主色彩实时渲染 Fluid Ambient 动态流体微光背景；沉浸式全屏播放器与灵动岛交互无缝联动，触觉质感操作控件让听歌更加赏心悦目。",
        )

        val usageGuides = listOf(
            "如何快速开始" to "在「工具」->「导入音乐」中选择本地音乐文件夹或单曲即可建立曲库。导入时可自动扫描歌词与专辑封面；支持直接拖拽音频文件（Windows）或选择转换加密音频。",
            "如何使用 AI 音乐助手" to "在「设置」中填入支持的模型 API Key 即可激活。点击助手右上角「新对话」可开启全新独立话题，历史对话可在「历史」中随时切换或清理，完全不浪费额外 Token。",
            "桌面悬浮歌词使用提示" to "在播放界面或灵动岛展开后，点击顶部画中画图标即可一键开启桌面歌词。Android 设备初次开启需授予「悬浮窗」权限；Windows 端可通过右键进行锁定或穿透设置。",
            "数据备份与迁移技巧" to "建议定期使用「工具」->「歌单导入与导出」将重要歌单导出为 .resonance 归档文件。在新设备上直接导入该文件即可完整恢复所有歌曲和封面，无需重新扫描整理。",
        )

        SectionHeader("应用信息")
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Resonance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ResonanceColors.TextPrimary)
                Text("版本：v$APP_VERSION", style = MaterialTheme.typography.labelMedium, color = ResonanceColors.Primary)
                Text("平台：Android / Windows 原生跨平台 (Kotlin Multiplatform + Compose)", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                Text("架构模式：Local-First · Zero Cloud Dependency · High Fidelity", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
            }
        }

        SectionHeader("当前版本更新 · v$APP_VERSION")
        if (APP_RELEASE_DATE.isNotBlank()) Text("发布日期：" + APP_RELEASE_DATE,
            style = MaterialTheme.typography.labelMedium, color = ResonanceColors.Muted)
        if (APP_RELEASE_HIGHLIGHTS.isEmpty()) {
            Text("当前版本暂无内置更新说明。", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
        } else APP_RELEASE_HIGHLIGHTS.forEach { highlight ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•", color = ResonanceColors.Primary)
                Text(highlight, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.TextPrimary, modifier = Modifier.weight(1f))
            }
        }
        TextButton(onClick = { runCatching { uriHandler.openUri("https://github.com/Leonard-china/Resonance/releases/tag/v$APP_VERSION") } }) {
            Text("查看此版本完整发布说明")
        }

        SectionHeader("官方链接与支持")
        SettingRow(
            icon = Icons.Default.Language,
            title = "官方网站",
            subtitle = "https://leonard.asia · 访问官网、论坛社区与最新下载",
            onClick = { runCatching { uriHandler.openUri("https://leonard.asia") } },
        )
        SettingRow(
            icon = Icons.Default.Favorite,
            title = "为作者充电",
            subtitle = "微信支付 · 支付宝收款码支持",
            onClick = { showSponsorDialog = true },
        )
        SettingRow(
            icon = Icons.Default.FavoriteBorder,
            title = "爱发电地址",
            subtitle = "https://afdian.com/a/Leonucs · 通过爱发电平台赞助作者",
            onClick = { runCatching { uriHandler.openUri("https://afdian.com/a/Leonucs") } },
        )
        SettingRow(
            icon = Icons.Default.Code,
            title = "Github地址",
            subtitle = "https://github.com/Leonard-china/Resonance · 源码开源仓库与问题反馈",
            onClick = { runCatching { uriHandler.openUri("https://github.com/Leonard-china/Resonance") } },
        )

        SectionHeader("核心功能与特色")
        appFeatures.forEach { (title, desc) ->
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ResonanceColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Dim)
            }
        }

        SectionHeader("使用说明与技巧")
        usageGuides.forEach { (title, desc) ->
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Dim)
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showSponsorDialog) {
        SponsorDialog(onDismiss = { showSponsorDialog = false })
    }
}

@Composable
internal fun SponsorDialog(onDismiss: () -> Unit) {
    var isWeChat by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val qrImage = remember(isWeChat) { loadSponsorQr(isWeChat) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = ResonanceColors.Primary)
                Spacer(Modifier.width(8.dp))
                Text("为作者充电", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "感谢你对 Resonance 独立开发者的支持与喜爱！你的鼓励是软件保持纯粹、本地优先与持续演进的最大动力。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ResonanceColors.Dim,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    val aliBg = if (!isWeChat) ResonanceColors.PrimarySoft else Color.Transparent
                    val wxBg = if (isWeChat) ResonanceColors.PrimarySoft else Color.Transparent
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(aliBg)
                            .clickable { isWeChat = false }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "支付宝",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (!isWeChat) ResonanceColors.Primary else ResonanceColors.Muted,
                            fontWeight = if (!isWeChat) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(wxBg)
                            .clickable { isWeChat = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "微信支付",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isWeChat) ResonanceColors.Primary else ResonanceColors.Muted,
                            fontWeight = if (isWeChat) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .width(230.dp)
                        .height(310.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (qrImage != null) {
                        Image(
                            bitmap = qrImage,
                            contentDescription = if (isWeChat) "微信收款码" else "支付宝收款码",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("收款码加载中...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (!isWeChat) {
                    FilledTonalButton(
                        onClick = { runCatching { uriHandler.openUri("https://qr.alipay.com/fkx196124amke88z1kqoe97") } },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    ) {
                        Text("直接拉起支付宝扫码支付")
                    }
                }

                OutlinedButton(
                    onClick = { runCatching { uriHandler.openUri("https://afdian.com/a/Leonucs") } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    Text("前往爱发电主页赞助 (afdian.com)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
internal fun ToolPage(
    title: String,
    onBack: () -> Unit,
    compact: Boolean,
    backLabel: String = "返回工具",
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val pagePadding = if (compact) 16.dp else 32.dp
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.widthIn(max = 880.dp).fillMaxWidth().padding(start = pagePadding - 12.dp, end = pagePadding, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccessibleIconButton(backLabel, onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = ResonanceColors.TextPrimary)
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = ResonanceColors.TextPrimary, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
        }
        HorizontalDivider(color = ResonanceColors.Divider, thickness = Dp.Hairline)
        Column(
            modifier = Modifier
                .widthIn(max = 880.dp)
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = pagePadding, vertical = 12.dp),
        ) {
            content()
        }
    }
}

@Composable
internal fun ImportScreen(
    compact: Boolean,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onConvert: () -> Unit,
    message: String?,
    operationInProgress: Boolean,
) {
    ToolPage(title = "导入音乐", onBack = onBack, compact = compact) {
        if (operationInProgress) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = ResonanceColors.Primary,
                trackColor = ResonanceColors.Soft,
            )
            Spacer(Modifier.height(12.dp))
        }
        if (message != null) {
            Surface(color = ResonanceColors.PrimarySoft, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    message,
                    color = ResonanceColors.Primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        LocalMusicFeatures.current?.let { features ->
            val progress by features.services.importProgress.collectAsState()
            progress?.let { MusicTaskProgress(it, features.services::cancelImport) }
        }
        SectionHeader("来源")
        SettingRow(
            icon = Icons.Default.Headphones,
            title = "扫描本机音乐",
            subtitle = "自动查找 MP3 / FLAC / M4A 等音频文件",
            onClick = onImport,
            modifier = Modifier.tutorialTarget(TutorialTarget.LocalImport),
        )
        SettingRow(
            icon = Icons.Default.FolderOpen,
            title = if (LocalMusicFeatures.current?.services?.supportsKgg == true) "选择文件夹并转换 KGMA/KGG" else "选择文件夹并转换 KGMA",
            subtitle = "递归转换为 320 kbps MP3，保留源文件",
            onClick = onConvert,
        )

        val features = LocalMusicFeatures.current
        val tutorial = LocalTutorial.current
        SettingRow(Icons.Default.VideoLibrary, "从 B 站视频导入音频", "自定义歌名、歌手、专辑和封面", {
            features?.showVideoImport = true
            tutorial?.event(com.resonance.player.tutorial.TutorialCompletion.VideoFormOpened)
        }, Modifier.tutorialTarget(TutorialTarget.VideoImport))

    }
}

@Composable
internal fun SyncScreen(
    compact: Boolean,
    onBack: () -> Unit,
    onExportSync: () -> Unit,
    onImportSync: () -> Unit,
    message: String?,
) {
    ToolPage(title = "歌单导入与导出", onBack = onBack, compact = compact) {
        if (message != null) {
            Surface(color = ResonanceColors.PrimarySoft, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    message,
                    color = ResonanceColors.Primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        SectionHeader("歌单文件")
        SettingRow(
            icon = Icons.Default.FolderOpen,
            title = "导出歌单",
            subtitle = "将歌曲音频、封面和歌单打包为 .resonance",
            onClick = onExportSync,
            modifier = Modifier.tutorialTarget(TutorialTarget.ExportArchive),
        )
        SettingRow(
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            title = "导入歌单",
            subtitle = "校验完整性后合并到本机音乐库",
            onClick = onImportSync,
            modifier = Modifier.tutorialTarget(TutorialTarget.ImportArchive),
        )
    }
}

@Composable
internal fun SettingsScreen(
    compact: Boolean,
    onBack: () -> Unit,
    libraryLocation: String,
    onOpenImport: () -> Unit,
    themeMode: ThemeMode = ThemeMode.Dark,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    deepSeekConfig: DeepSeekConfig = DeepSeekConfig(),
    onSaveDeepSeekConfig: (DeepSeekConfig) -> Unit = {},
    onTestDeepSeek: suspend (DeepSeekConfig) -> DeepSeekTestResult = { DeepSeekTestResult(false, "") },
    customLyricsFolder: String? = null,
    onSaveCustomLyricsFolder: (String?) -> Unit = {},
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val appearance = LocalAppearance.current
    var lyricsFolder by remember(customLyricsFolder) { mutableStateOf(customLyricsFolder.orEmpty()) }
    var folderSavedMessage by remember { mutableStateOf<String?>(null) }

    ToolPage(title = "设置", onBack = onBack, compact = compact) {
        val tutorial = LocalTutorial.current
        SettingRow(Icons.Default.Info, "使用教程", "快速了解 · 跟着操作 · 随时继续学习", { tutorial?.directory() })
        SectionHeader("外观")
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeOption("深色", Icons.Default.DarkMode, themeMode == ThemeMode.Dark, { onThemeModeChange(ThemeMode.Dark) }, Modifier.weight(1f))
            ThemeOption("浅色", Icons.Default.LightMode, themeMode == ThemeMode.Light, { onThemeModeChange(ThemeMode.Light) }, Modifier.weight(1f))
            ThemeOption("跟随系统", Icons.Default.BrightnessAuto, themeMode == ThemeMode.System, { onThemeModeChange(ThemeMode.System) }, Modifier.weight(1f))
        }

        AppearanceSettings()

        MusicFeatureSettingsSection()

        SectionHeader("本地歌词与酷狗下载目录")
        Text(
            "自动查找音频旁的 KRC / LRC 歌词，也可选择自己的歌词目录。Android 授权目录会建立本地歌词索引。",
            style = MaterialTheme.typography.bodySmall,
            color = ResonanceColors.Dim,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        OutlinedTextField(
            value = lyricsFolder,
            onValueChange = { lyricsFolder = it },
            label = { Text("本地歌词目录") },
            placeholder = { Text("未设置，使用自动查找", color = ResonanceColors.Dim) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ResonanceColors.Primary,
                unfocusedBorderColor = ResonanceColors.Divider,
                focusedContainerColor = ResonanceColors.Canvas,
                unfocusedContainerColor = ResonanceColors.Canvas,
                cursorColor = ResonanceColors.Primary,
            ),
        )

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.material3.Button(
                onClick = {
                    onSaveCustomLyricsFolder(lyricsFolder.trim().takeIf(String::isNotBlank))
                    folderSavedMessage = "歌词目录已保存"
                },
                shape = RoundedCornerShape(8.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = ResonanceColors.Primary,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
            ) {
                Text("保存目录")
            }
            androidx.compose.material3.TextButton(
                onClick = {
                    lyricsFolder = ""
                    onSaveCustomLyricsFolder(null)
                    folderSavedMessage = "已恢复自动查找歌词"
                },
            ) {
                Text("恢复自动查找", color = ResonanceColors.Muted)
            }
        }

        TextButton(onClick = {
            coroutineScope.launch {
                try {
                    appearance.chooseLyricsFolder()?.let { folder ->
                        lyricsFolder = folder
                        onSaveCustomLyricsFolder(folder)
                        folderSavedMessage = "歌词目录已保存"
                    }
                } catch (_: Exception) { folderSavedMessage = "无法选择歌词目录" }
            }
        }) { Text("选择歌词目录") }

        folderSavedMessage?.let { msg ->
            Spacer(Modifier.height(4.dp))
            Text(msg, style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Primary)
        }

        SectionHeader("音乐库")
        SettingRow(Icons.Default.LibraryMusic, "扫描音乐", "查找并导入本机音频文件", onClick = onOpenImport)
        SettingRow(Icons.Default.FolderOpen, "音乐库位置", libraryLocation, onClick = null)

        SectionHeader("播放与解码")
        SettingRow(Icons.Default.GraphicEq, "转换质量", "默认 MP3 320 kbps · 原 MP3 不重新编码", onClick = null)
        SettingRow(Icons.Default.MusicNote, "KRC / LRC 解密", "支持酷狗原生 KRC 解密、LRCLIB 自动匹配及本地多级模糊索引", onClick = null)
        SettingRow(Icons.Default.Shuffle, "播放与队列", "记住随机 / 循环模式和上次选中的歌单", onClick = null)

        SectionHeader("关于")
        SettingRow(Icons.Default.Info, "Resonance", "版本 v$APP_VERSION · 本地优先的跨平台音乐播放器", onClick = null)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun ThemeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        if (selected) ResonanceColors.PrimarySoft else ResonanceColors.Raised,
        animationSpec = tween(motionDuration(160)),
        label = "themeOptionBg",
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .selectable(selected, onClick = onClick, role = Role.RadioButton)
            .border(1.dp, if (selected) ResonanceColors.Primary else Color.Transparent, RoundedCornerShape(10.dp))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) ResonanceColors.Primary else ResonanceColors.Dim,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) ResonanceColors.Primary else ResonanceColors.Muted,
        )
    }
}

@Composable
internal fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = ResonanceColors.Muted,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
internal fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val background by animateColorAsState(if (focused || hovered) ResonanceColors.Soft else Color.Transparent,
        tween(motionDuration(160)), label = "settingRowBackground")
    Column(modifier) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(background)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = ripple(),
                role = Role.Button, onClick = onClick) else Modifier)
            .border(1.5.dp, if (focused) ResonanceColors.Primary else Color.Transparent, RoundedCornerShape(14.dp))
            .heightIn(min = 72.dp).padding(horizontal = 10.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).galleryPlinth(corner = 4.dp, depth = 3.dp),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = ResonanceColors.Primary, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = ResonanceColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
            }
            if (onClick != null) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = ResonanceColors.Dimmer, modifier = Modifier.size(20.dp))
            }
        }
        HorizontalDivider(Modifier.padding(start = 64.dp), color = ResonanceColors.Divider, thickness = Dp.Hairline)
    }
}
