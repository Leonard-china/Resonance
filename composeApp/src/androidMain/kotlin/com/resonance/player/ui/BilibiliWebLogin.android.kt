package com.resonance.player.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun BilibiliWebLoginDialog(
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var isCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val cookieManager = CookieManager.getInstance()
        while (isActive && !isCompleted) {
            delay(1000)
            val cookies = cookieManager.getCookie("https://bilibili.com") ?: ""
            if (cookies.contains("SESSDATA=") && (cookies.contains("bili_jct=") || cookies.contains("DedeUserID="))) {
                isCompleted = true
                onSuccess(cookies)
                break
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "哔哩哔哩网页登录",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "登录成功后将自动提取 Cookie 并保存",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (!isCompleted) {
                                        val cookies = cookieManager.getCookie("https://bilibili.com") ?: ""
                                        if (cookies.contains("SESSDATA=") && (cookies.contains("bili_jct=") || cookies.contains("DedeUserID="))) {
                                            isCompleted = true
                                            onSuccess(cookies)
                                        }
                                    }
                                }
                            }
                            loadUrl("https://passport.bilibili.com/h5-app/passport/login")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
