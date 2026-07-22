package com.calb.qr2card.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class QrCodeService {
    fun generateQrBitmap(
        value: String,
        sizePx: Int,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE,
        quietZoneModules: Int = 4,
    ): Bitmap {
        val matrix = generateQrMatrix(value, sizePx, quietZoneModules)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            for (x in 0 until sizePx) {
                pixels[y * sizePx + x] = if (matrix[x, y]) foregroundColor else backgroundColor
            }
        }
        return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }

    /**
     * Returns the QR modules so an export format can preserve them as vector squares.
     * A caller may use a small requested size (for example 1) to obtain one matrix cell
     * per QR module rather than a bitmap-scaled matrix.
     */
    fun generateQrMatrix(
        value: String,
        sizePx: Int,
        quietZoneModules: Int = 4,
    ): BitMatrix {
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
        return matrix
    }
}
