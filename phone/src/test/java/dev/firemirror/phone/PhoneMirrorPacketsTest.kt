package dev.firemirror.phone

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class PhoneMirrorPacketsTest {
    @Test fun audioVideoAndResizePacketsHaveDistinctFraming() {
        val bytes = ByteArrayOutputStream()
        val output = DataOutputStream(bytes)
        PhoneMirrorPackets.audioFormat(output)
        PhoneMirrorPackets.audio(output, 321L, byteArrayOf(1, 2, 3, 4), 4)
        PhoneMirrorPackets.audioEnded(output)
        PhoneMirrorPackets.resize(output, 960, 540)
        PhoneMirrorPackets.video(output, 123L, byteArrayOf(5, 6))

        val input = DataInputStream(ByteArrayInputStream(bytes.toByteArray()))
        assertEquals(-1, input.readInt())
        assertEquals(48_000, input.readInt())
        assertEquals(2, input.readInt())
        assertEquals(-2, input.readInt())
        assertEquals(321L, input.readLong())
        assertEquals(4, input.readInt())
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), ByteArray(4).also { input.readFully(it) })
        assertEquals(-3, input.readInt())
        assertEquals(0, input.readInt())
        assertEquals(960, input.readInt())
        assertEquals(540, input.readInt())
        assertEquals(2, input.readInt())
        assertEquals(123L, input.readLong())
        assertArrayEquals(byteArrayOf(5, 6), ByteArray(2).also { input.readFully(it) })
        assertEquals(-1, input.read())
    }
}
