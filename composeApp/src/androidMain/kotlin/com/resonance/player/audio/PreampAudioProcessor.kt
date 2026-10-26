package com.resonance.player.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PreampAudioProcessor : BaseAudioProcessor() {
    @Volatile private var targetGain = 1.0
    @Volatile private var targetVocalSuppression = 0.0
    private var processor = PcmGainProcessor()
    private var scratch = ByteArray(0)
    fun setGainDb(db: Float) { targetGain = Math.pow(10.0, db.toDouble().coerceIn(-60.0, 18.0) / 20.0) }
    fun setVocalSuppression(enabled: Boolean) { targetVocalSuppression = if (enabled) 1.0 else 0.0 }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT)
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        return inputAudioFormat
    }
    override fun onFlush() {
        processor = PcmGainProcessor(targetGain, inputAudioFormat.sampleRate, inputAudioFormat.channelCount, targetVocalSuppression)
    }
    override fun queueInput(inputBuffer: ByteBuffer) {
        val count = inputBuffer.remaining()
        if (scratch.size < count) scratch = ByteArray(count)
        inputBuffer.get(scratch, 0, count)
        processor.process(scratch, count, targetGain, targetVocalSuppression)
        replaceOutputBuffer(count).put(scratch, 0, count).flip()
    }
}
