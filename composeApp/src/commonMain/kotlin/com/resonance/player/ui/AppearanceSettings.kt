package com.resonance.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.resonance.player.design.*
import com.resonance.player.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun AppearanceSettings() {
    val appearance = LocalAppearance.current
    val preferences = appearance.preferences
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var cities by remember { mutableStateOf(emptyList<WeatherLocation>()) }
    var cityMessage by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }

    Text("配色", style = MaterialTheme.typography.titleSmall, color = ResonanceColors.TextPrimary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AccentMode.entries.forEach { mode ->
            FilterChip(selected = preferences.accentMode == mode,
                onClick = { appearance.onPreferencesChange(preferences.copy(accentMode = mode)) },
                label = { Text(mode.label) },
                leadingIcon = if (preferences.accentMode == mode) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                modifier = Modifier.heightIn(min = 48.dp))
        }
    }
    if (preferences.accentMode == AccentMode.Weather) {
        Text("晴天浅灰绿，雨雪冰蓝；阴天和获取失败时保持当前配色。",
            style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
        Text("使用设备大致位置获取 Open-Meteo 当地天气。仅在前台更新；定位不可用时使用你选择的城市。",
            style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted,
            modifier = Modifier.padding(top = 6.dp))
        appearance.snapshot?.let { weather ->
            Text("${weather.locationLabel} · ${weatherDescription(weather.code)}",
                style = MaterialTheme.typography.titleSmall, color = ResonanceColors.Primary,
                modifier = Modifier.padding(top = 12.dp))
            Text("上次更新：${formatWeatherUpdate(weather.updatedAtMillis)}",
                style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
        }
        if (appearance.status.isNotBlank()) Text(appearance.status,
            style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted,
            modifier = Modifier.padding(top = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { appearance.onRefresh(true) }, enabled = !appearance.updating) {
                Text(if (appearance.updating) "正在更新…" else "定位并更新")
            }
            TextButton(onClick = { appearance.onRefresh(false) }, enabled = !appearance.updating) { Text("刷新天气") }
        }
        Text("兜底城市：${preferences.fallbackCity?.label ?: "尚未设置"}",
            style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.TextPrimary,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        OutlinedTextField(value = query, onValueChange = { query = it; cities = emptyList(); cityMessage = null },
            enabled = !searching,
            label = { Text("搜索城市") }, placeholder = { Text("输入中文或英文城市名") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = {
            searching = true
            cityMessage = null
            scope.launch {
                try {
                    cities = appearance.searchCities(query)
                    cityMessage = if (cities.isEmpty()) "没有找到城市，试试英文名称" else "请选择具体城市"
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    cityMessage = "城市搜索失败，请检查网络后重试"
                } finally { searching = false }
            }
        }, enabled = query.trim().length >= 2 && !searching) { Text(if (searching) "正在搜索…" else "搜索城市") }
        cityMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted) }
        cities.forEach { city ->
            Text(city.label, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.TextPrimary,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                    appearance.onPreferencesChange(preferences.copy(fallbackCity = city))
                    cities = emptyList()
                    cityMessage = "兜底城市已更新"
                    appearance.onRefresh(false)
                }.padding(12.dp))
        }
        if (preferences.fallbackCity != null) TextButton(onClick = {
            appearance.onPreferencesChange(preferences.copy(fallbackCity = null))
        }) { Text("清除兜底城市") }
    }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("减少动态效果", color = ResonanceColors.TextPrimary, fontWeight = FontWeight.Medium)
            Text("关闭环境漂移与弹性缩放，保留必要状态反馈", style = MaterialTheme.typography.bodySmall,
                color = ResonanceColors.Muted)
        }
        Switch(checked = preferences.reduceMotion,
            onCheckedChange = { appearance.onPreferencesChange(preferences.copy(reduceMotion = it)) },
            modifier = Modifier.semantics { contentDescription = "减少动态效果" })
    }
}

internal expect fun formatWeatherUpdate(timestamp: Long): String
