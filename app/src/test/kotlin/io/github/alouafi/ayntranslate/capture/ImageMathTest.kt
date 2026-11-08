package io.github.alouafi.ayntranslate.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageMathTest {

    @Test
    fun `large screens shrink to the upload limit`() {
        assertEquals(1600f / 1920f, uploadScale(1920, 1080), 1e-6f)
        assertEquals(1600f / 1920f, uploadScale(1080, 1920), 1e-6f)
    }

    @Test
    fun `small crops grow so the text stays legible`() {
        assertEquals(768f / 400f, uploadScale(400, 120), 1e-6f)
    }

    @Test
    fun `medium images keep their size`() {
        assertEquals(1f, uploadScale(1240, 1080))
        assertEquals(1f, uploadScale(768, 200))
        assertEquals(1f, uploadScale(1600, 900))
    }
}
