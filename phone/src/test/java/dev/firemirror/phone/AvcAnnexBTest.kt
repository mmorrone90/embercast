package dev.firemirror.phone

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvcAnnexBTest {
    @Test fun convertsMultipleLengthPrefixedNalUnits() {
        val avcc = byteArrayOf(0, 0, 0, 2, 0x67, 0x11, 0, 0, 0, 2, 0x65, 0x22)
        assertArrayEquals(
            byteArrayOf(0, 0, 0, 1, 0x67, 0x11, 0, 0, 0, 1, 0x65, 0x22),
            AvcAnnexB.normalize(avcc)
        )
    }

    @Test fun rejectsIncompleteNalLength() {
        assertNull(AvcAnnexB.normalize(byteArrayOf(0, 0, 0, 8, 0x65)))
    }
}
