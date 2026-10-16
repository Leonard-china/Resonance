package com.resonance.player.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resonance.player.model.BiliQrPollStatus
import com.resonance.player.model.BiliQrSession
import com.resonance.player.platform.PlatformServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun BilibiliQrLoginDialog(
    services: PlatformServices?,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit,
) {
    var session by remember { mutableStateOf<BiliQrSession?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("正在获取二维码...") }
    var isExpired by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        statusMessage = "正在获取二维码..."
        isExpired = false
        val newSession = services?.musicAssistant?.createBilibiliQrSession()
        session = newSession
        isLoading = false
        if (newSession == null) {
            statusMessage = "获取二维码失败，请检查网络后重试"
        } else {
            statusMessage = "请使用哔哩哔哩手机客户端扫码"
        }
    }

    LaunchedEffect(session?.qrcodeKey) {
        val currentKey = session?.qrcodeKey ?: return@LaunchedEffect
        while (isActive && !isExpired) {
            delay(2000)
            val result = services?.musicAssistant?.pollBilibiliQrStatus(currentKey) ?: break
            when (result.status) {
                BiliQrPollStatus.WAITING -> {
                    statusMessage = result.message.ifBlank { "请使用哔哩哔哩手机客户端扫码" }
                }
                BiliQrPollStatus.SCANNED -> {
                    statusMessage = result.message.ifBlank { "已扫码，请在手机端确认登录" }
                }
                BiliQrPollStatus.SUCCESS -> {
                    statusMessage = "登录成功！正在保存..."
                    val cookie = result.cookie.orEmpty()
                    if (cookie.isNotBlank()) {
                        onSuccess(cookie)
                    }
                    break
                }
                BiliQrPollStatus.EXPIRED -> {
                    isExpired = true
                    statusMessage = result.message.ifBlank { "二维码已失效，请刷新" }
                    break
                }
                BiliQrPollStatus.ERROR -> {
                    statusMessage = result.message
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("哔哩哔哩扫码登录") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(210.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else if (session != null) {
                        val currentSession = session!!
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val matrixSize = currentSession.matrixSize
                                if (matrixSize > 0) {
                                    val cellSize = size.width / matrixSize
                                    for (y in 0 until matrixSize) {
                                        for (x in 0 until matrixSize) {
                                            if (currentSession.bits[y * matrixSize + x]) {
                                                drawRect(
                                                    color = Color.Black,
                                                    topLeft = Offset(x * cellSize, y * cellSize),
                                                    size = Size(cellSize, cellSize)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (isExpired) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White.copy(alpha = 0.88f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FilledTonalButton(onClick = { refreshTrigger++ }) {
                                        Text("刷新二维码")
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            if (isExpired || session == null) {
                TextButton(onClick = { refreshTrigger++ }) {
                    Text("刷新")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
expect fun BilibiliWebLoginDialog(
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
)
