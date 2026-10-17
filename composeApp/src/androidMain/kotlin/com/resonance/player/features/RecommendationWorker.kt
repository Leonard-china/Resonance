package com.resonance.player.features

import android.content.Context
import androidx.work.*
import com.resonance.player.model.MusicFeatureSettings
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class RecommendationWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val runtime = AndroidMusicAccess.runtime(applicationContext)
        val config = runtime.settings()
        if (!config.recommendationsEnabled || !config.backgroundRecommendations) return Result.success()
        return try { setForeground(foregroundInfo()); runtime.recommend(); Result.success() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { if (runAttemptCount < 2) Result.retry() else Result.failure() }
    }
    private fun foregroundInfo(): ForegroundInfo {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channel = "resonance-recommendations"
        manager.createNotificationChannel(android.app.NotificationChannel(channel, "推荐音乐下载", android.app.NotificationManager.IMPORTANCE_LOW))
        val notification = android.app.Notification.Builder(applicationContext, channel)
            .setContentTitle("Resonance 每日推荐").setContentText("正在准备推荐音乐，可在音乐助手查看或取消")
            .setSmallIcon(android.R.drawable.ic_media_play).setOngoing(true).build()
        return if (android.os.Build.VERSION.SDK_INT >= 29) ForegroundInfo(2002, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else ForegroundInfo(2002, notification)
    }
    companion object {
        fun configure(context: Context, settings: MusicFeatureSettings) {
            val manager = WorkManager.getInstance(context)
            if (!settings.recommendationsEnabled || !settings.backgroundRecommendations) { manager.cancelUniqueWork("resonance-recommendations"); return }
            val request = PeriodicWorkRequestBuilder<RecommendationWorker>(settings.intervalHours.toLong(), TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(if (settings.recommendationsUnmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).setRequiresStorageNotLow(true).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES).build()
            manager.enqueueUniquePeriodicWork("resonance-recommendations", ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
