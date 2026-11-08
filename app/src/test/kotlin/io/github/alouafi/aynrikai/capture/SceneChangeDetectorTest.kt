package io.github.alouafi.aynrikai.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneChangeDetectorTest {

    private val cells = FrameSignature.WIDTH * FrameSignature.HEIGHT

    /** A gray frame where the first [changedCells] cells are white, like text appearing in a box. */
    private fun frame(changedCells: Int = 0, gray: Int = 0x40): FrameSignature {
        val background = 0xFF000000.toInt() or (gray shl 16) or (gray shl 8) or gray
        return FrameSignature.fromArgb(IntArray(cells) { if (it < changedCells) 0xFFFFFFFF.toInt() else background })
    }

    @Test
    fun `difference is the share of changed cells`() {
        assertEquals(0f, frame().differenceFrom(frame()))
        assertEquals(0.5f, frame(changedCells = cells / 2).differenceFrom(frame()))
        assertEquals(1f, frame(changedCells = cells).differenceFrom(frame()))
    }

    @Test
    fun `small brightness noise does not count`() {
        assertEquals(0f, frame(gray = 0x45).differenceFrom(frame(gray = 0x40)))
        assertEquals(1f, frame(gray = 0x60).differenceFrom(frame(gray = 0x40)))
    }

    @Test
    fun `signatures of different sizes are fully different`() {
        assertEquals(1f, FrameSignature(IntArray(4)).differenceFrom(frame()))
    }

    @Test
    fun `reads a new scene once it has settled`() {
        val detector = SceneChangeDetector()

        assertFalse("the first frame has nothing to settle against", detector.shouldAnalyze(frame()))
        assertTrue(detector.shouldAnalyze(frame()))
        assertFalse("already read", detector.shouldAnalyze(frame()))
    }

    @Test
    fun `waits for typing text to finish`() {
        val detector = SceneChangeDetector()
        detector.shouldAnalyze(frame())
        detector.shouldAnalyze(frame())

        assertFalse(detector.shouldAnalyze(frame(changedCells = 100)))
        assertFalse(detector.shouldAnalyze(frame(changedCells = 200)))
        assertFalse(detector.shouldAnalyze(frame(changedCells = 300)))
        assertTrue(detector.shouldAnalyze(frame(changedCells = 300)))
    }

    @Test
    fun `ignores a blinking cursor`() {
        val detector = SceneChangeDetector()
        detector.shouldAnalyze(frame(changedCells = 300))
        detector.shouldAnalyze(frame(changedCells = 300))

        assertFalse(detector.shouldAnalyze(frame(changedCells = 304)))
        assertFalse(detector.shouldAnalyze(frame(changedCells = 300)))
    }
}
