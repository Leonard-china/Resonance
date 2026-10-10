package com.resonance.player.audio

import android.content.Context
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

class AndroidLoudnessAnalyzer(context: Context) {
    private val context = context.applicationContext
    private val preferences = this.context.getSharedPreferences("resonance_ui_state", Context.MODE_PRIVATE)
    fun cached(trackId: String): Float? {
        val key = "gain.auto.${LoudnessAnalysis.CACHE_VERSION}.$trackId"
        return if (preferences.contains(key)) preferences.getFloat(key, 0f).takeIf(Float::isFinite) else null
    }

    suspend fun analyze(trackId: String, source: String): Float = analysisMutex.withLock {
        cached(trackId)?.let { return@withLock it }
        val input = withContext(Dispatchers.IO) {
            if (source.startsWith("content://")) FFmpegKitConfig.getSafParameterForRead(context, Uri.parse(source))
            else if (source.startsWith("file:")) Uri.parse(source).path ?: source else source
        }
        val gain = withTimeout(180_000) {
            suspendCancellableCoroutine<Float?> { continuation ->
                val session = FFmpegKit.executeWithArgumentsAsync(LoudnessAnalysis.arguments(input)) { completed ->
                    if (continuation.isActive) {
                        val measured = if (ReturnCode.isSuccess(completed.returnCode))
                            LoudnessAnalysis.gainFromOutput(completed.getAllLogsAsString(5_000).orEmpty()) else null
                        continuation.resume(measured)
                    }
                }
                continuation.invokeOnCancellation { FFmpegKit.cancel(session.sessionId) }
            }
        } ?: return@withLock 0f
        withContext(Dispatchers.IO) {
            check(preferences.edit().putFloat("gain.auto.${LoudnessAnalysis.CACHE_VERSION}.$trackId", gain).commit())
        }
        gain
    }

    companion object { private val analysisMutex = Mutex() }
}
