package com.resonance.player.together

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.resonance.player.model.PlayerState
import com.resonance.player.model.Track
import com.resonance.player.playback.TogetherPlaybackDriver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.nio.file.Files
import java.nio.file.Paths

/** Application-owned room, bound to the existing MediaSessionService's player rather than an Activity. */
class AndroidTogetherRuntime private constructor(context: Context) {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    private var scope: CoroutineScope? = null
    private var driver: TogetherPlaybackDriver? = null
    private var hostDriver: com.resonance.player.playback.TogetherHostPlaybackDriver? = null
    var localPlaybackSnapshot: Any? = null
    val service = JvmListenTogetherService(app.cacheDir.toPath().resolve("together-audio"), { track ->
        val source = requireNotNull(track.sourceUri)
        when {
            source.startsWith("content://") -> app.contentResolver.openInputStream(android.net.Uri.parse(source)) ?: error("无法读取音频")
            source.startsWith("file:") -> Files.newInputStream(Paths.get(java.net.URI(source)))
            !source.contains("://") -> Files.newInputStream(Paths.get(source))
            else -> error("一起听仅共享本机可读取的音频")
        }
    }, onGuestStopped = { main.post { player?.pause() } }, prefetchNetworkAllowed = {
        val manager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val network = manager?.activeNetwork
        val caps = network?.let { manager.getNetworkCapabilities(it) }
        caps != null &&
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
            !manager.isActiveNetworkMetered
    }, companionStore = TogetherCompanionStore(app.filesDir.toPath().resolve("together-companions"),
        com.resonance.player.features.AndroidCredentialVault::encrypt,
        com.resonance.player.features.AndroidCredentialVault::decrypt, com.resonance.player.platform.TogetherDefaults.RelayUrl)).apply { trackPlayerReadiness(true) }
    private val messageNotifications = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val powerManager = app.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
    private val wifiManager = app.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null

    init {
        messageNotifications.launch(start = CoroutineStart.UNDISPATCHED) {
            service.chatMessageEvents.collect { showChatNotification() }
        }
        messageNotifications.launch(start = CoroutineStart.UNDISPATCHED) {
            var renewal: Job? = null
            service.state.collect { state ->
                updatePowerLocks(state.status)
                if (needsPowerLocks(state.status)) {
                    if (renewal?.isActive != true) renewal = launch {
                        while (isActive) {
                            delay(30_000)
                            updatePowerLocks(service.state.value.status)
                        }
                    }
                } else {
                    renewal?.cancel()
                    renewal = null
                }
            }
        }
    }

    private fun needsPowerLocks(status: com.resonance.player.platform.TogetherStatus) = status in setOf(
            com.resonance.player.platform.TogetherStatus.Preparing,
            com.resonance.player.platform.TogetherStatus.Receiving,
            com.resonance.player.platform.TogetherStatus.Connected,
            com.resonance.player.platform.TogetherStatus.Reconnecting,
        ) || (status == com.resonance.player.platform.TogetherStatus.AwaitingApproval && service.state.value.members.size > 1)

    private fun updatePowerLocks(status: com.resonance.player.platform.TogetherStatus) {
        val active = needsPowerLocks(status)
        synchronized(this) {
            if (active) {
                if (wakeLock == null) {
                    wakeLock = runCatching {
                        powerManager?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "resonance:together_keepalive")?.apply { setReferenceCounted(false) }
                    }.getOrNull()
                }
                if (wakeLock?.isHeld == false) runCatching { wakeLock?.acquire(90_000L) }
                val connectivity = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                val activeNetwork = connectivity?.activeNetwork
                val onWifi = activeNetwork?.let { connectivity?.getNetworkCapabilities(it) }
                    ?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
                if (onWifi && wifiLock == null) {
                    val mode = if (Build.VERSION.SDK_INT >= 29) {
                        android.net.wifi.WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                    } else {
                        android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF
                    }
                    wifiLock = runCatching { wifiManager?.createWifiLock(mode, "resonance:together_wifi")?.apply { setReferenceCounted(false) } }.getOrNull()
                }
                if (onWifi && wifiLock?.isHeld == false) runCatching { wifiLock?.acquire() }
                if (!onWifi) {
                    runCatching { wifiLock?.takeIf { it.isHeld }?.release() }
                    wifiLock = null
                }
            } else {
                runCatching { wakeLock?.release() }
                wakeLock = null
                runCatching { wifiLock?.release() }
                wifiLock = null
            }
        }
    }

    private fun showChatNotification() {
        val manager = NotificationManagerCompat.from(app)
        if (!manager.areNotificationsEnabled() || (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(app, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)) return
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("together-messages", "一起听消息", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "朋友发来一起听消息时提醒，不显示消息正文"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }
            app.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val launch = Intent(app, com.resonance.player.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val open = PendingIntent.getActivity(app, 40572, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notice = NotificationCompat.Builder(app, "together-messages")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Resonance")
            .setContentText("朋友发来一条一起听消息")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try { manager.notify(40572, notice) } catch (_: SecurityException) { /* Permission can change concurrently. */ }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun bind(native: ExoPlayer) {
        if (player === native) return
        driver?.close(); hostDriver?.close(); scope?.cancel()
        player = native
        val owner = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = owner
        hostDriver = com.resonance.player.playback.TogetherHostPlaybackDriver(service, owner, current = ::snapshot,
            replaceQueue = { tracks, target -> reconcileHostQueue(native, tracks, target) },
            select = { track, tracks, autoPlay ->
                reconcileHostQueue(native, tracks, track.id)
                val index = (0 until native.mediaItemCount).firstOrNull { native.getMediaItemAt(it).mediaId == track.id }
                if (index != null) { native.seekTo(index, 0L); native.prepare(); native.playWhenReady = autoPlay }
            }, playing = { native.playWhenReady = it }, seek = { fraction ->
                native.seekTo(((native.duration.takeIf { it > 0 } ?: 0) * fraction).toLong())
            }, onError = service::reportHostPlaybackFailure)
        driver = TogetherPlaybackDriver(service, owner, current = ::snapshot,
            prepare = { frame ->
                val track = requireNotNull(frame.track)
                val item = MediaItem.Builder().setMediaId(track.id).setUri(requireNotNull(track.sourceUri))
                    .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist).setAlbumTitle(track.album).build()).build()
                native.setMediaItems(listOf(item), 0, (frame.progress * frame.durationMs).toLong())
                native.repeatMode = Player.REPEAT_MODE_OFF
                native.shuffleModeEnabled = false
                native.setPlaybackSpeed(frame.speed)
                native.playWhenReady = false
                native.prepare()
            }, smoothSpeedChanges = true, seek = { fraction ->
                val duration = native.duration.takeIf { it > 0 } ?: 0
                native.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
            }, playing = { native.playWhenReady = it }, speed = { native.setPlaybackSpeed(it) }, ready = { id ->
                withTimeoutOrNull(15_000) {
                    while (native.currentMediaItem?.mediaId != id || native.playbackState != Player.STATE_READY) delay(25)
                    true
                } ?: false
            })
        owner.launch {
            while (isActive) {
                val room = service.state.value
                if (room.role == com.resonance.player.platform.TogetherRole.Host) service.publishNativePlayback(snapshot(), room.roomSessionId)
                delay(if (service.state.value.role == com.resonance.player.platform.TogetherRole.Host) 100 else 500)
            }
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun reconcileHostQueue(native: ExoPlayer, tracks: List<Track>, target: String?) {
        val playWhenReady = native.playWhenReady
        val previousId = native.currentMediaItem?.mediaId
        if (tracks.isEmpty()) { native.pause(); native.clearMediaItems(); return }
        for ((index, track) in tracks.withIndex()) {
            if (index < native.mediaItemCount && native.getMediaItemAt(index).mediaId == track.id) continue
            val existing = (index until native.mediaItemCount).firstOrNull { native.getMediaItemAt(it).mediaId == track.id }
            if (existing != null) native.moveMediaItem(existing, index) else native.addMediaItem(index,
                MediaItem.Builder().setMediaId(track.id).setUri(requireNotNull(track.sourceUri))
                    .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist).setAlbumTitle(track.album).build()).build())
        }
        if (native.mediaItemCount > tracks.size) native.removeMediaItems(tracks.size, native.mediaItemCount)
        val targetIndex = tracks.indexOfFirst { it.id == target }.coerceAtLeast(0)
        if (tracks.none { it.id == previousId }) native.seekTo(targetIndex, 0L)
        if (native.playbackState == Player.STATE_IDLE) native.prepare()
        native.playWhenReady = playWhenReady
    }

    private fun snapshot(): PlayerState {
        val native = player ?: return PlayerState()
        val item = native.currentMediaItem ?: return PlayerState()
        val duration = native.duration.takeIf { it > 0 } ?: 1
        val metadata = item.mediaMetadata
        val track = service.nativeTrack(item.mediaId) ?: Track(item.mediaId, metadata.title?.toString().orEmpty(),
            metadata.artist?.toString().orEmpty(), metadata.albumTitle?.toString().orEmpty(),
            "%d:%02d".format(duration / 60_000, duration / 1_000 % 60), 0, sourceUri = item.localConfiguration?.uri?.toString())
        return PlayerState(currentTrack = track, isPlaying = native.isPlaying, positionMs = native.currentPosition,
            progress = (native.currentPosition.toFloat() / duration).coerceIn(0f, 1f), playbackSpeed = native.playbackParameters.speed,
            shuffleEnabled = native.shuffleModeEnabled, repeatMode = when (native.repeatMode) {
                Player.REPEAT_MODE_ONE -> com.resonance.player.model.RepeatMode.One
                Player.REPEAT_MODE_ALL -> com.resonance.player.model.RepeatMode.All
                else -> com.resonance.player.model.RepeatMode.Off
            })
    }

    fun unbind(native: Player?) {
        if (player !== native) return
        service.leave()
        driver?.close(); driver = null; hostDriver?.close(); hostDriver = null; scope?.cancel(); scope = null; player = null
    }

    companion object {
        @Volatile private var instance: AndroidTogetherRuntime? = null
        fun get(context: Context): AndroidTogetherRuntime = instance ?: synchronized(this) {
            instance ?: AndroidTogetherRuntime(context).also { instance = it }
        }
    }
}
