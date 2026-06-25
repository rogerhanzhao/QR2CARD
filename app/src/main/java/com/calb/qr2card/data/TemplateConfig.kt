package com.calb.qr2card.data

import kotlinx.serialization.Serializable

@Serializable
data class TemplateConfig(
    val page: PageConfig = PageConfig(),
    val colors: ColorConfig = ColorConfig(),
    val front: FrontConfig = FrontConfig(),
    val back: BackConfig = BackConfig(),
) {
    @Serializable
    data class PageConfig(
        val finishedWidthMm: Float = 92.0f,
        val finishedHeightMm: Float = 56.0f,
        val bleedMm: Float = 3.0f,
    )

    @Serializable
    data class ColorConfig(
        val deepBlue: String = "#23496b",
        val wiseGrey: String = "#cedbea",
        val black: String = "#1e1e1e",
        val white: String = "#ffffff",
    )

    @Serializable
    data class FrontConfig(
        val logo: BoxMm = BoxMm(8.3f, 7.35f, 22.7f, 7.1f),
        val companyTop: TextMm = TextMm(46.8f, 11.5f, 9.2f),
        val name: TextMm = TextMm(9.0f, 24.2f, 12.6f, 8.5f),
        val title: TextMm = TextMm(9.0f, 29.95f, 6.3f, 4.8f),
        val companyLine: TextMm = TextMm(9.0f, 33.7f, 6.3f),
        val infoLabels: TextMm = TextMm(46.9f, 29.95f, 6.1f),
        val infoValues: TextMm = TextMm(57.4f, 29.95f, 6.1f),
        val watermark: WatermarkMm = WatermarkMm(-3.1f, -0.6f, 98.0f, 59.6f, 1.0f),
    )

    @Serializable
    data class BackConfig(
        val qr: SquareMm = SquareMm(31.0f, 10.1f, 28.0f),
        val caption: AlignedTextMm = AlignedTextMm(0.0f, 40.8f, 9.0f, "center"),
    )

    @Serializable
    data class BoxMm(
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
    )

    @Serializable
    data class SquareMm(
        val x: Float,
        val y: Float,
        val size: Float,
    )

    @Serializable
    data class TextMm(
        val x: Float,
        val y: Float,
        val fontSize: Float,
        val minFontSize: Float = fontSize,
    )

    @Serializable
    data class AlignedTextMm(
        val x: Float,
        val y: Float,
        val fontSize: Float,
        val align: String,
    )

    @Serializable
    data class WatermarkMm(
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
        val opacity: Float,
    )
}
