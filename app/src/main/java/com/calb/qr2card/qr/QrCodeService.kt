package com.calb.qr2card.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class QrCodeService {
    fun generateQrBitmap(
        value: String,
        sizePx: Int,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE,
        quietZoneModules: Int = 4,
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to quietZoneModules,
        )
        val matrix = MultiFormatWriter().encode(
            value,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            hints,
        )
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            for (x in 0 until sizePx) {
                pixels[y * sizePx + x] = if (matrix[x, y]) foregroundColor else backgroundColor
            }
        }
        return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }
}
