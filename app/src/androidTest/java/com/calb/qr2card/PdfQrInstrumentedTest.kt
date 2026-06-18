package com.calb.qr2card

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.calb.qr2card.data.EmployeeCardData
import com.calb.qr2card.data.TemplateConfig
import com.calb.qr2card.domain.VCardService
import com.calb.qr2card.pdf.PdfRendererService
import com.calb.qr2card.qr.QrCodeService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfQrInstrumentedTest {
    @Test
    fun generatesQrBitmap() {
        val vCard = VCardService().buildVCard(EmployeeCardData())
        val bitmap = QrCodeService().generateQrBitmap(vCard, 256)

        assertEquals(256, bitmap.width)
        assertEquals(256, bitmap.height)
    }

    @Test
    fun createsPreviewPdfFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = PdfRendererService().generatePreviewPdf(
            context = context,
            data = EmployeeCardData(),
            config = TemplateConfig(),
        )

        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }
}
