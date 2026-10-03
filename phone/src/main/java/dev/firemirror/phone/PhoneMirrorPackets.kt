package dev.firemirror.phone

import java.io.DataOutputStream

/** One lock keeps video, resize, and audio packets intact on the shared TCP stream. */
internal object PhoneMirrorPackets {
    const val MAGIC = 0x464D5032 // FMP2; the receiver still accepts the older video-only FMP1
    const val AUDIO_SAMPLE_RATE = 48_000
    const val AUDIO_CHANNELS = 2
    const val AUDIO_CHUNK_BYTES = 3_840 // 20 ms of stereo PCM16

    fun resize(output: DataOutputStream, width: Int, height: Int) = synchronized(output) {
        output.writeInt(0)
        output.writeInt(width)
        output.writeInt(height)
        output.flush()
    }

    fun video(output: DataOutputStream, timestampNs: Long, bytes: ByteArray) = synchronized(output) {
        output.writeInt(bytes.size)
        output.writeLong(timestampNs)
        output.write(bytes)
    }

    fun audioFormat(output: DataOutputStream) = synchronized(output) {
        output.writeInt(-1)
        output.writeInt(AUDIO_SAMPLE_RATE)
        output.writeInt(AUDIO_CHANNELS)
        output.flush()
    }

    fun audio(output: DataOutputStream, timestampNs: Long, bytes: ByteArray, length: Int) = synchronized(output) {
        require(length in 1..bytes.size)
        output.writeInt(-2)
        output.writeLong(timestampNs)
        output.writeInt(length)
        output.write(bytes, 0, length)
    }

    fun audioEnded(output: DataOutputStream) = synchronized(output) {
        output.writeInt(-3)
        output.flush()
    }
}
