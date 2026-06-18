package com.calb.qr2card.pdf

object PdfMath {
    const val POINTS_PER_MM: Float = 72f / 25.4f

    fun mmToPt(mm: Float): Float = mm * POINTS_PER_MM
}
