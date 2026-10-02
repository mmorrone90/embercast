package dev.firemirror.phone

import java.io.ByteArrayOutputStream

/** Convert AVC length prefixed NAL units to the Annex B format used by the TV decoder. */
internal object AvcAnnexB {
    fun normalize(data: ByteArray): ByteArray? {
        if (data.size < 4) return null
        if (hasStartCode(data)) return data
        val output = ByteArrayOutputStream(data.size + 32)
        var offset = 0
        while (offset + 4 <= data.size) {
            val length = ((data[offset].toInt() and 255) shl 24) or
                ((data[offset + 1].toInt() and 255) shl 16) or
                ((data[offset + 2].toInt() and 255) shl 8) or
                (data[offset + 3].toInt() and 255)
            offset += 4
            if (length <= 0 || length > data.size - offset) return null
            output.write(byteArrayOf(0, 0, 0, 1))
            output.write(data, offset, length)
            offset += length
        }
        return if (offset == data.size) output.toByteArray() else null
    }

    private fun hasStartCode(data: ByteArray): Boolean =
        data[0] == 0.toByte() && data[1] == 0.toByte() &&
            (data[2] == 1.toByte() || (data[2] == 0.toByte() && data[3] == 1.toByte()))
}
