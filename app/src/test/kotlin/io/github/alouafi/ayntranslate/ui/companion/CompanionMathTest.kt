package io.github.alouafi.ayntranslate.ui.companion

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class CompanionMathTest {

    @Test
    fun `wide frames fit the width and center vertically`() {
        assertEquals(Rect(0f, 75f, 800f, 525f), fitInside(Size(1920f, 1080f), Size(800f, 600f)))
    }

    @Test
    fun `tall frames fit the height and center horizontally`() {
        assertEquals(Rect(150f, 0f, 450f, 400f), fitInside(Size(300f, 400f), Size(600f, 400f)))
    }

    @Test
    fun `empty frames take no space`() {
        assertEquals(Rect.Zero, fitInside(Size(0f, 0f), Size(600f, 400f)))
    }
}
