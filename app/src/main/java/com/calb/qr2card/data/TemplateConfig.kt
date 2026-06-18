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
        val logo: BoxMm = BoxMm(7.0f, 6.0f, 26.0f, 8.5f),
        val companyTop: TextMm = TextMm(47.0f, 11.5f, 7.0f),
        val name: TextMm = TextMm(9.0f, 24.0f, 12.0f, 8.5f),
        val title: TextMm = TextMm(9.0f, 30.0f, 5.8f, 4.8f),
        val companyLine: TextMm = TextMm(9.0f, 36.0f, 5.8f),
        val infoLabels: TextMm = TextMm(47.0f, 31.0f, 5.4f),
        val infoValues: TextMm = TextMm(58.0f, 31.0f, 5.4f),
        val watermark: WatermarkMm = WatermarkMm(4.0f, 38.0f, 84.0f, 15.0f, 0.35f),
    )

    @Serializable
    data class BackConfig(
        val qr: SquareMm = SquareMm(32.0f, 11.5f, 28.0f),
        val caption: AlignedTextMm = AlignedTextMm(0.0f, 43.0f, 7.0f, "center"),
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
