package io.github.alouafi.aynrikai.dictionary

import org.junit.Assert.assertEquals
import org.junit.Test

class LinesTest {

    private val lines = Lines("# header\nあ\t0\n\nかな\t1,2\n漢字\t3\n".encodeToByteArray())

    @Test
    fun `skips comments and empty lines`() {
        assertEquals(3, lines.size)
        assertEquals(listOf("あ\t0", "かな\t1,2", "漢字\t3"), (0 until lines.size).map { lines[it] })
    }

    @Test
    fun `finds lines by their first field`() {
        assertEquals(0, lines.find("あ"))
        assertEquals(1, lines.find("かな"))
        assertEquals(2, lines.find("漢字"))
        assertEquals(-1, lines.find("か"))
        assertEquals(-1, lines.find("かなかな"))
        assertEquals(-1, lines.find(""))
    }
}
