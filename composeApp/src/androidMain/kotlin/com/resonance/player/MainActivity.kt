package com.resonance.player

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.resonance.player.platform.AndroidPlatformServices
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // ActivityResult launchers must be registered before this Activity reaches STARTED.
        // Creating the services lazily inside Compose can happen after RESUMED and crashes
        // on every Android version with a strict ActivityResultRegistry lifecycle check.
        val services = AndroidPlatformServices(this)
        intent?.dataString?.let(LanSyncLinkBus::offer)
        setContent {
            App(services)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let(LanSyncLinkBus::offer)
    }
}

object LanSyncLinkBus {
    private val channel = Channel<String>(Channel.UNLIMITED)
    val links = channel.receiveAsFlow()
    fun offer(link: String) { channel.trySend(link) }
}
