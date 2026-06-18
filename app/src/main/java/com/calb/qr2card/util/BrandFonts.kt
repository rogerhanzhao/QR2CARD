package com.calb.qr2card.util

import android.content.Context
import android.graphics.Typeface

object BrandFonts {
    const val MANROPE_REGULAR_ASSET = "fonts/Manrope-Regular.otf"
    const val MANROPE_BOLD_ASSET = "fonts/Manrope-Bold.otf"
    const val HARMONY_SC_REGULAR_ASSET = "fonts/HarmonyOS_Sans_SC_Regular.ttf"

    val PRINT_FONT_INFO_LINES = listOf(
        "字体：英文 Manrope Regular/Bold（OpenType/CFF .otf）。",
        "中文说明：HarmonyOS Sans SC Regular（TrueType .ttf）；PDF 输出使用打包字体，避免设备字体替换。",
    )

    private val cache = mutableMapOf<String, Typeface>()

    fun manropeRegular(context: Context): Typeface = load(context, MANROPE_REGULAR_ASSET)

    fun manropeBold(context: Context): Typeface = load(context, MANROPE_BOLD_ASSET)

    fun harmonyScRegular(context: Context): Typeface = load(context, HARMONY_SC_REGULAR_ASSET)

    private fun load(context: Context, assetPath: String): Typeface = synchronized(cache) {
        cache.getOrPut(assetPath) {
            Typeface.createFromAsset(context.applicationContext.assets, assetPath)
        }
    }
}
