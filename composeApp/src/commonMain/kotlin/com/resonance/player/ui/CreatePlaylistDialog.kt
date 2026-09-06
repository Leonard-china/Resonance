package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.ResonanceShapes
import com.resonance.player.design.resonancePressable

@Composable
fun CreatePlaylistDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val createInteraction = remember { MutableInteractionSource() }
    com.resonance.player.platform.ResonanceBackHandler(enabled = true, onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            shape = ResonanceShapes.Panel,
            color = ResonanceColors.Raised,
            tonalElevation = 0.dp,
        ) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("新建歌单", style = MaterialTheme.typography.titleLarge, color = ResonanceColors.Ivory)
                        Spacer(Modifier.height(2.dp))
                        Text("创建后可向歌单添加歌曲", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Dim)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = ResonanceColors.Muted)
                    }
                }
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("歌单名称") },
                    placeholder = { Text("例如：夜行收藏", color = ResonanceColors.Dim) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ResonanceColors.Coral,
                        unfocusedBorderColor = ResonanceColors.Divider,
                        focusedContainerColor = ResonanceColors.Canvas,
                        unfocusedContainerColor = ResonanceColors.Canvas,
                        cursorColor = ResonanceColors.Coral,
                    ),
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = ResonanceColors.Muted)
                    }
                    Button(
                        onClick = { onConfirm(name.trim()) },
                        enabled = name.isNotBlank(),
                        shape = ResonanceShapes.Button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ResonanceColors.Coral,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier.resonancePressable(createInteraction),
                        interactionSource = createInteraction,
                    ) {
                        Text("创建歌单")
                    }
                }
            }
        }
    }
}

