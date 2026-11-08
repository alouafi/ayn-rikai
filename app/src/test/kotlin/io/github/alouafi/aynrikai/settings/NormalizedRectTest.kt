package io.github.alouafi.aynrikai.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalizedRectTest {

    @Test
    fun `spanning orders and clamps the corners`() {
        assertEquals(NormalizedRect(0.2f, 0f, 1f, 0.5f), NormalizedRect.spanning(1.4f, 0.5f, 0.2f, -0.3f))
    }

    @Test
    fun `tiny selections are not usable`() {
        assertTrue(NormalizedRect(0.1f, 0.6f, 0.9f, 0.95f).isUsable)
        assertFalse(NormalizedRect(0.1f, 0.6f, 0.11f, 0.95f).isUsable)
    }

    @Test
    fun `encodes and decodes`() {
        val rect = NormalizedRect(0.05f, 0.7f, 0.95f, 0.98f)

        assertEquals(rect, NormalizedRect.decode(rect.encode()))
        assertNull(NormalizedRect.decode("0.1,0.2"))
        assertNull(NormalizedRect.decode("a,b,c,d"))
        assertNull(NormalizedRect.decode("0.5,0.5,0.51,0.51"))
    }

    @Test
    fun `converts to pixels`() {
        assertEquals(PixelRect(96, 756, 1728, 302), NormalizedRect(0.05f, 0.7f, 0.95f, 0.98f).toPixels(1920, 1080))
    }

    @Test
    fun `pixel rectangles are never empty`() {
        assertEquals(PixelRect(99, 9, 1, 1), NormalizedRect(1f, 1f, 1f, 1f).toPixels(100, 10))
    }
}
