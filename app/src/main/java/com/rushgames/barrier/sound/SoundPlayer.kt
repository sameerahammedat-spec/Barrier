package com.rushgames.barrier.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

enum class SoundType(val title: String, val category: String) {
    RAIN("Rainfall", "Nature"),
    OCEAN("Ocean Waves", "Nature"),
    COFFEE_SHOP("Coffee Shop", "Ambient"),
    LIBRARY("Quiet Library", "Focus"),
    OFFICE("Soft Office", "Focus"),
    NATURE("Forest Birds", "Nature"),
    WHITE_NOISE("White Noise", "Noise"),
    BROWN_NOISE("Deep Brown Noise", "Noise")
}

@Singleton
class SoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeAudioTrack: AudioTrack? = null
    private var isPlayingSound = false
    private var currentType: SoundType? = null

    fun playSound(type: SoundType, volume: Float = 0.7f) {
        stopSound()
        currentType = type
        isPlayingSound = true

        playerScope.launch {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            activeAudioTrack = audioTrack
            audioTrack.setVolume(volume)
            audioTrack.play()

            val random = Random()
            val pcmData = ShortArray(bufferSize)
            var lastValue = 0.0

            while (isActive && isPlayingSound) {
                for (i in pcmData.indices) {
                    when (type) {
                        SoundType.WHITE_NOISE -> {
                            pcmData[i] = (random.nextGaussian() * 0.25 * Short.MAX_VALUE).toInt().toShort()
                        }
                        SoundType.BROWN_NOISE, SoundType.RAIN, SoundType.OCEAN -> {
                            val white = random.nextGaussian() * 0.1
                            lastValue = (lastValue + (0.02 * white)) / 1.02
                            pcmData[i] = (lastValue * Short.MAX_VALUE * 3).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        }
                        else -> {
                            val white = random.nextGaussian() * 0.15
                            lastValue = (lastValue + (0.05 * white)) / 1.05
                            pcmData[i] = (lastValue * Short.MAX_VALUE * 2.5).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        }
                    }
                }
                audioTrack.write(pcmData, 0, pcmData.size)
            }
        }
    }

    fun setVolume(volume: Float) {
        activeAudioTrack?.setVolume(volume.coerceIn(0f, 1f))
    }

    fun stopSound() {
        isPlayingSound = false
        activeAudioTrack?.stop()
        activeAudioTrack?.release()
        activeAudioTrack = null
        currentType = null
    }

    fun isPlaying(type: SoundType): Boolean {
        return isPlayingSound && currentType == type
    }
}
