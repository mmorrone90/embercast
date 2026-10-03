package dev.firemirror.phone

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.sin

/** A short media tone that can be captured by the active screen sharing session. */
internal object TestSound {
    fun play() {
        Thread({
            try {
                val sampleRate = 48_000
                val format = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
                val minimum = AudioTrack.getMinBufferSize(
                    sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
                )
                require(minimum > 0)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(maxOf(minimum, 3_840 * 2))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                try {
                    track.play()
                    val chunk = ByteArray(3_840)
                    repeat(50) { block ->
                        for (sample in 0 until 960) {
                            val fade = minOf(block * 960 + sample, 48_000 - (block * 960 + sample)) / 2_400.0
                            val amplitude = minOf(1.0, fade.coerceAtLeast(0.0))
                            val value = (5_000 * amplitude * sin(2 * Math.PI * 440 * (block * 960 + sample) / sampleRate)).toInt()
                            val offset = sample * 4
                            chunk[offset] = value.toByte()
                            chunk[offset + 1] = (value shr 8).toByte()
                            chunk[offset + 2] = value.toByte()
                            chunk[offset + 3] = (value shr 8).toByte()
                        }
                        track.write(chunk, 0, chunk.size, AudioTrack.WRITE_BLOCKING)
                    }
                    track.stop()
                } finally {
                    track.release()
                }
            } catch (e: Exception) {
                Log.w("FireMirrorTestSound", "Could not play test sound", e)
            }
        }, "FireMirrorTestSound").start()
    }
}
