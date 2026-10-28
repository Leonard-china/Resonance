package com.resonance.player.audio

import com.resonance.player.model.AudioEffectPreset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.tanh

/**
 * 跨平台 16-bit PCM 音频 DSP 处理器：
 * - 192ms 平滑增益斜坡（Gain Ramp）
 * - 软峰值饱和防爆音（Soft Peak Knee Limiter）
 * - 音效预设滤镜（原声、人声凸显、低音增强、流行、摇滚、3D环绕、K歌伴奏中置人声消除）
 */
class PcmGainProcessor(
    initialGain: Double = 1.0,
    private val sampleRate: Int = 48_000,
    private val channels: Int = 2,
    initialVocalSuppression: Double = 0.0,
    initialPreset: AudioEffectPreset = AudioEffectPreset.Off,
) {
    private var currentGain = initialGain
    private var previousTarget = initialGain
    private var step = 0.0
    private var remaining = 0
    private var currentVocalSuppression = initialVocalSuppression.coerceIn(0.0, 1.0)
    private var currentPreset = initialPreset

    // 1-pole 低通滤波器状态（伴奏消除保留 <160Hz 低音，以及低音增强使用）
    private var lowPassState = 0.0
    private val lowPassAlpha = (2.0 * PI * 160.0 / sampleRate).coerceIn(0.001, 0.2)

    // 低音增强低通滤波器（L/R）
    private var bassStateL = 0.0
    private var bassStateR = 0.0
    private val bassAlpha = (2.0 * PI * 180.0 / sampleRate).coerceIn(0.001, 0.2)

    // 人声中频带通滤波器（L/R: low mid ~600Hz, high mid ~3200Hz）
    private var midLowStateL = 0.0
    private var midLowStateR = 0.0
    private val midLowAlpha = (2.0 * PI * 600.0 / sampleRate).coerceIn(0.001, 0.2)
    private var midHighStateL = 0.0
    private var midHighStateR = 0.0
    private val midHighAlpha = (2.0 * PI * 3200.0 / sampleRate).coerceIn(0.001, 0.5)

    // 高频高通滤波器状态（L/R: ~4500Hz）
    private var trebleLowStateL = 0.0
    private var trebleLowStateR = 0.0
    private val trebleLowAlpha = (2.0 * PI * 4500.0 / sampleRate).coerceIn(0.001, 0.6)

    // 环绕声微延迟缓冲区 (~12ms delay for Side channel stereo widening)
    private val surroundDelaySize = (sampleRate * 0.012).toInt().coerceAtLeast(1)
    private val surroundDelayBuffer = DoubleArray(surroundDelaySize)
    private var surroundDelayIndex = 0

    fun setPreset(preset: AudioEffectPreset) {
        currentPreset = preset
        if (preset == AudioEffectPreset.Karaoke) {
            currentVocalSuppression = 1.0
        } else if (currentVocalSuppression >= 1.0) {
            currentVocalSuppression = 0.0
        }
    }

    fun process(
        bytes: ByteArray,
        count: Int,
        targetGain: Double,
        vocalSuppression: Double = currentVocalSuppression,
        preset: AudioEffectPreset = currentPreset,
    ) {
        val target = targetGain.takeIf(Double::isFinite)?.coerceIn(0.0, 8.0) ?: 0.0
        val targetVocal = if (preset == AudioEffectPreset.Karaoke) 1.0 else vocalSuppression.coerceIn(0.0, 1.0)
        currentVocalSuppression = targetVocal
        currentPreset = preset

        if (target != previousTarget) {
            remaining = (sampleRate * 0.192).toInt().coerceAtLeast(1)
            step = (target - currentGain) / remaining
            previousTarget = target
        }
        val frames = count / (channels * 2)
        val isStereo = channels == 2

        for (frame in 0 until frames) {
            if (remaining > 0) {
                currentGain += step
                if (--remaining == 0) currentGain = previousTarget
            }

            if (isStereo) {
                val offsetL = frame * 4
                val offsetR = offsetL + 2
                val sampleL = ((bytes[offsetL].toInt() and 0xff) or (bytes[offsetL + 1].toInt() shl 8)).toShort().toInt()
                val sampleR = ((bytes[offsetR].toInt() and 0xff) or (bytes[offsetR + 1].toInt() shl 8)).toShort().toInt()

                var normL = sampleL / 32768.0
                var normR = sampleR / 32768.0

                // 1. K歌伴奏人声消除
                if (currentVocalSuppression > 0.0) {
                    val mid = (normL + normR) * 0.5
                    lowPassState += lowPassAlpha * (mid - lowPassState)
                    val midVocal = mid - lowPassState
                    normL -= currentVocalSuppression * midVocal
                    normR -= currentVocalSuppression * midVocal
                }

                // 2. 音效预设滤波调校
                when (currentPreset) {
                    AudioEffectPreset.BassBoost -> {
                        bassStateL += bassAlpha * (normL - bassStateL)
                        bassStateR += bassAlpha * (normR - bassStateR)
                        normL += 0.70 * bassStateL
                        normR += 0.70 * bassStateR
                    }
                    AudioEffectPreset.Vocal -> {
                        midLowStateL += midLowAlpha * (normL - midLowStateL)
                        midLowStateR += midLowAlpha * (normR - midLowStateR)
                        midHighStateL += midHighAlpha * (normL - midHighStateL)
                        midHighStateR += midHighAlpha * (normR - midHighStateR)
                        val vocalL = midHighStateL - midLowStateL
                        val vocalR = midHighStateR - midLowStateR
                        normL += 0.65 * vocalL
                        normR += 0.65 * vocalR
                    }
                    AudioEffectPreset.Pop -> {
                        bassStateL += bassAlpha * (normL - bassStateL)
                        bassStateR += bassAlpha * (normR - bassStateR)
                        midLowStateL += midLowAlpha * (normL - midLowStateL)
                        midLowStateR += midLowAlpha * (normR - midLowStateR)
                        midHighStateL += midHighAlpha * (normL - midHighStateL)
                        midHighStateR += midHighAlpha * (normR - midHighStateR)
                        normL += 0.35 * bassStateL + 0.30 * (midHighStateL - midLowStateL)
                        normR += 0.35 * bassStateR + 0.30 * (midHighStateR - midLowStateR)
                    }
                    AudioEffectPreset.Rock -> {
                        bassStateL += bassAlpha * (normL - bassStateL)
                        bassStateR += bassAlpha * (normR - bassStateR)
                        trebleLowStateL += trebleLowAlpha * (normL - trebleLowStateL)
                        trebleLowStateR += trebleLowAlpha * (normR - trebleLowStateR)
                        val trebleL = normL - trebleLowStateL
                        val trebleR = normR - trebleLowStateR
                        normL += 0.55 * bassStateL + 0.45 * trebleL
                        normR += 0.55 * bassStateR + 0.45 * trebleR
                    }
                    AudioEffectPreset.Surround -> {
                        val mid = (normL + normR) * 0.5
                        val side = (normL - normR) * 0.5
                        val delayedSide = surroundDelayBuffer[surroundDelayIndex]
                        surroundDelayBuffer[surroundDelayIndex] = side
                        surroundDelayIndex = (surroundDelayIndex + 1) % surroundDelaySize
                        val widenedSide = side * 1.35 + delayedSide * 0.20
                        normL = mid + widenedSide
                        normR = mid - widenedSide
                    }
                    AudioEffectPreset.Off, AudioEffectPreset.Karaoke -> {
                        // 无需额外滤波
                    }
                }

                applySample(bytes, offsetL, normL * currentGain)
                applySample(bytes, offsetR, normR * currentGain)
            } else {
                for (channel in 0 until channels) {
                    val offset = (frame * channels + channel) * 2
                    val sample = ((bytes[offset].toInt() and 0xff) or (bytes[offset + 1].toInt() shl 8)).toShort().toInt()
                    applySample(bytes, offset, (sample / 32768.0) * currentGain)
                }
            }
        }
    }

    private fun applySample(bytes: ByteArray, offset: Int, amplified: Double) {
        val magnitude = abs(amplified)
        val limited = if (magnitude <= 0.98) amplified else
            (0.98 + 0.02 * tanh((magnitude - 0.98) / 0.02)) * if (amplified < 0) -1 else 1
        val value = (limited * 32768).toInt().coerceIn(-32768, 32767)
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value shr 8).toByte()
    }
}
