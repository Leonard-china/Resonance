package com.resonance.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.resonance.player.platform.AndroidPlatformServices

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
        setContent {
            App(services)
        }
    }
}
