package com.calb.qr2card.util

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory

fun decodeSampledBitmapResource(
    resources: Resources,
    resId: Int,
    maxWidthPx: Int,
    maxHeightPx: Int,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        inScaled = false
    }
    BitmapFactory.decodeResource(resources, resId, bounds)

    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inScaled = false
        inSampleSize = calculateInSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            maxWidthPx = maxWidthPx,
            maxHeightPx = maxHeightPx,
        )
    }
    return BitmapFactory.decodeResource(resources, resId, options)
}

private fun calculateInSampleSize(
    width: Int,
    height: Int,
    maxWidthPx: Int,
    maxHeightPx: Int,
): Int {
    if (width <= 0 || height <= 0 || maxWidthPx <= 0 || maxHeightPx <= 0) return 1

    var sampleSize = 1
    while (
        width / (sampleSize * 2) >= maxWidthPx &&
        height / (sampleSize * 2) >= maxHeightPx
    ) {
        sampleSize *= 2
    }
    return sampleSize
}
