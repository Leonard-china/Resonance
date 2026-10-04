package com.resonance.player.ui

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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

@Composable
internal fun ProfileScreen(
    compact: Boolean,
    onOpenImport: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenSettings: () -> Unit,
) {
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
            subtitle = "扫描本机、转换 KGMA/KGG、导入歌单",
            onClick = onOpenImport,
        )
        SettingRow(
            icon = Icons.Outlined.Sync,
            title = "设备同步",
            subtitle = "局域网扫码同步 / 同步包导入导出",
            onClick = onOpenSync,
        )
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
            onClick = onOpenSettings,
        )
    }
}

@Composable
internal fun ToolPage(
    title: String,
    onBack: () -> Unit,
    compact: Boolean,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val pagePadding = if (compact) 16.dp else 32.dp
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.widthIn(max = 880.dp).fillMaxWidth().padding(start = pagePadding - 12.dp, end = pagePadding, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccessibleIconButton("返回工具", onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = ResonanceColors.TextPrimary)
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = ResonanceColors.TextPrimary)
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
    onImportPlaylist: () -> Unit,
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
        SectionHeader("来源")
        SettingRow(
            icon = Icons.Default.Headphones,
            title = "扫描本机音乐",
            subtitle = "自动查找 MP3 / FLAC / M4A 等音频文件",
            onClick = onImport,
        )
        SettingRow(
            icon = Icons.Default.FolderOpen,
            title = "选择文件夹并转换 KGMA/KGG",
            subtitle = "递归转换为 320 kbps MP3，保留源文件",
            onClick = onConvert,
        )
        SettingRow(
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            title = "导入酷狗歌单链接",
            subtitle = "读取公开歌单目录并匹配本机已有歌曲",
            onClick = onImportPlaylist,
        )
    }
}

@Composable
internal fun SyncScreen(
    compact: Boolean,
    onBack: () -> Unit,
    onExportSync: () -> Unit,
    onImportSync: () -> Unit,
    onStartLanSync: () -> Unit,
    lanQrPath: String?,
    message: String?,
) {
    ToolPage(title = "设备同步", onBack = onBack, compact = compact) {
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
        SectionHeader("局域网")
        SettingRow(
            icon = Icons.Default.Devices,
            title = "生成配对二维码",
            subtitle = "10 分钟内有效，另一台设备扫码即可加密同步",
            onClick = onStartLanSync,
        )
        if (lanQrPath != null) {
            Spacer(Modifier.height(16.dp))
            AlbumArtwork(
                seed = 0,
                modifier = Modifier.size(220.dp).align(Alignment.CenterHorizontally),
                cornerRadius = 12.dp,
                artworkPath = lanQrPath,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "二维码已保存：$lanQrPath",
                style = MaterialTheme.typography.bodySmall,
                color = ResonanceColors.Dim,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        SectionHeader("同步包")
        SettingRow(
            icon = Icons.Default.FolderOpen,
            title = "导出同步包",
            subtitle = "打包 MP3、封面与元数据为 .resonance 文件",
            onClick = onExportSync,
        )
        SettingRow(
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            title = "导入同步包",
            subtitle = "校验完整性后合并到本机音乐库",
            onClick = onImportSync,
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
    var apiKey by remember(deepSeekConfig.apiKey) { mutableStateOf(deepSeekConfig.apiKey) }
    var baseUrl by remember(deepSeekConfig.baseUrl) { mutableStateOf(deepSeekConfig.baseUrl) }
    var model by remember(deepSeekConfig.model) { mutableStateOf(deepSeekConfig.model) }
    var showApiKey by remember { mutableStateOf(false) }
    var testInProgress by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<DeepSeekTestResult?>(null) }
    var lyricsFolder by remember(customLyricsFolder) { mutableStateOf(customLyricsFolder.orEmpty()) }
    var folderSavedMessage by remember { mutableStateOf<String?>(null) }

    ToolPage(title = "设置", onBack = onBack, compact = compact) {
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

        SectionHeader("DeepSeek AI 增强")
        Text(
            "当本地歌词和 LRCLIB 均无结果时，自动调用 DeepSeek AI 搜索或生成精准逐行时间轴歌词与专辑视觉。",
            style = MaterialTheme.typography.bodySmall,
            color = ResonanceColors.Dim,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = {
                apiKey = it.trim()
                onSaveDeepSeekConfig(DeepSeekConfig(apiKey = apiKey, baseUrl = baseUrl, model = model))
            },
            label = { Text("DeepSeek API Key") },
            placeholder = { Text("sk-...", color = ResonanceColors.Dim) },
            singleLine = true,
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showApiKey) "隐藏" else "显示",
                        tint = ResonanceColors.Dim,
                    )
                }
            },
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

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = model,
                onValueChange = {
                    model = it.trim()
                    onSaveDeepSeekConfig(DeepSeekConfig(apiKey = apiKey, baseUrl = baseUrl, model = model))
                },
                label = { Text("模型") },
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
            OutlinedTextField(
                value = baseUrl,
                onValueChange = {
                    baseUrl = it.trim()
                    onSaveDeepSeekConfig(DeepSeekConfig(apiKey = apiKey, baseUrl = baseUrl, model = model))
                },
                label = { Text("API 接口地址") },
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
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    testInProgress = true
                    testResult = null
                    coroutineScope.launch {
                        val config = DeepSeekConfig(apiKey = apiKey, baseUrl = baseUrl, model = model)
                        onSaveDeepSeekConfig(config)
                        testResult = onTestDeepSeek(config)
                        testInProgress = false
                    }
                },
                enabled = apiKey.isNotBlank() && !testInProgress,
                shape = RoundedCornerShape(8.dp),
            ) {
                if (testInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ResonanceColors.Primary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("正在测试…")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = ResonanceColors.Primary)
                    Spacer(Modifier.width(6.dp))
                    Text("测试 DeepSeek 连接", color = ResonanceColors.Primary)
                }
            }
        }

        testResult?.let { res ->
            Spacer(Modifier.height(6.dp))
            Surface(
                color = if (res.success) ResonanceColors.Soft else ResonanceColors.PrimarySoft,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    res.message,
                    color = if (res.success) ResonanceColors.TextPrimary else ResonanceColors.Primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

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
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
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
        style = MaterialTheme.typography.labelMedium,
        color = ResonanceColors.Dim,
        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
    )
}

@Composable
internal fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(clickableModifier)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = ResonanceColors.Muted, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = ResonanceColors.TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ResonanceColors.Dim,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onClick != null) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ResonanceColors.Dimmer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        HorizontalDivider(color = ResonanceColors.Divider, thickness = Dp.Hairline)
    }
}

