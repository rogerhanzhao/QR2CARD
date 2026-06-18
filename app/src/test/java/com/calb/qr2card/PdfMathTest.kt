package com.calb.qr2card

import com.calb.qr2card.pdf.PdfMath
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfMathTest {
    @Test
    fun convertsMillimetersToPdfPoints() {
        assertEquals(72f, PdfMath.mmToPt(25.4f), 0.001f)
    }
}
