package com.example.engine.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.sin

object SpaceAudio {
    private var isPlaying = false
    private var audioTrack: AudioTrack? = null

    fun playSpaceDrone() {
        if (isPlaying) return
        isPlaying = true

        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )

        audioTrack?.play()

        Thread {
            val buffer = ShortArray(bufferSize)
            var phase1 = 0.0
            var phase2 = 0.0
            var phase3 = 0.0
            val phaseIncrement1 = 2.0 * Math.PI * 55.0 / sampleRate // A1 note
            val phaseIncrement2 = 2.0 * Math.PI * 55.5 / sampleRate // Beating frequency
            val phaseIncrement3 = 2.0 * Math.PI * 82.41 / sampleRate // E2 note

            while (isPlaying) {
                for (i in buffer.indices) {
                    val sample1 = sin(phase1)
                    val sample2 = sin(phase2)
                    val sample3 = sin(phase3)
                    
                    // Mix the sines and add a bit of noise
                    val noise = (Math.random() * 2.0 - 1.0) * 0.05
                    val mixed = (sample1 * 0.4 + sample2 * 0.4 + sample3 * 0.2 + noise) * 0.5
                    
                    // Modulate envelope slowly for a breathing effect
                    val envelope = (sin(phase1 * 0.01) * 0.5 + 0.5) * 0.5 + 0.5

                    // Soft volume and clip prevention
                    val finalSample = (mixed * envelope * 16000.0).toInt().toShort()

                    buffer[i] = finalSample

                    phase1 += phaseIncrement1
                    phase2 += phaseIncrement2
                    phase3 += phaseIncrement3
                    
                    if (phase1 > 2.0 * Math.PI) phase1 -= 2.0 * Math.PI
                    if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI
                    if (phase3 > 2.0 * Math.PI) phase3 -= 2.0 * Math.PI
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }.start()
    }

    fun stop() {
        isPlaying = false
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
