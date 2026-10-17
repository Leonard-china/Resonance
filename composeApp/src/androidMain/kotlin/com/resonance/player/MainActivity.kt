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
        // ActivityResult launchers must be registered before this Activity reaches STARTED.
        // Creating the services lazily inside Compose can happen after RESUMED and crashes
        // on every Android version with a strict ActivityResultRegistry lifecycle check.
        val services = AndroidPlatformServices(this)
        setContent {
            App(services)
        }
    }

}
