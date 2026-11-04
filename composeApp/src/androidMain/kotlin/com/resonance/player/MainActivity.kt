package com.resonance.player

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.resonance.player.platform.AndroidPlatformServices
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class MainActivity : ComponentActivity() {
    private lateinit var services: AndroidPlatformServices
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        // ActivityResult launchers must be registered before this Activity reaches STARTED.
        // Creating the services lazily inside Compose can happen after RESUMED and crashes
        // on every Android version with a strict ActivityResultRegistry lifecycle check.
        services = AndroidPlatformServices(this)
        offerInvitation(intent)
        setContent {
            App(services)
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        offerInvitation(intent)
    }
    private fun offerInvitation(incoming: Intent?) {
        if (incoming?.action != Intent.ACTION_VIEW) return
        val value = incoming.dataString ?: return
        if (value.length <= 4096 && incoming.data?.scheme == "resonance-listen") {
            services.offerTogetherInvitation(value)
        }
        // Never retain capability URLs or forward nested Intents/extras/URI permissions.
        intent = Intent(this, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
    }
}
