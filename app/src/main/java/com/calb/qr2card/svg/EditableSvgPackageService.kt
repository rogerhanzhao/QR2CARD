package com.calb.qr2card.svg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Environment
import android.util.Base64
import com.calb.qr2card.R
import com.calb.qr2card.data.EmployeeCardData
import com.calb.qr2card.data.TemplateConfig
import com.calb.qr2card.data.displayContactRows
import com.calb.qr2card.data.exportSafeName
import com.calb.qr2card.domain.VCardService
import com.calb.qr2card.pdf.PdfMath
import com.calb.qr2card.qr.QrCodeService
import com.calb.qr2card.util.BrandFonts
import com.calb.qr2card.util.decodeSampledBitmapResource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val SVG_MANROPE_FAMILY = "Manrope"
/** Gap between the front and back card artboards, matching the preview PNG. */
private const val CARD_GAP_MM = 4f

/**
 * Creates an Illustrator-compatible SVG of the business card for design amendments.
 *
 * Both faces live in one file, front above back, so the designer opens a single document.
 * Card text stays live SVG text and the QR code consists of vector square paths. The
 * supplied CALB logo and watermark are currently PNG-only project assets, so they are
 * embedded as data URIs to preserve their approved appearance and keep the file
 * self-contained. Their original AI/SVG/EPS artwork can replace those image elements
 * later without changing card coordinates.
 */
class EditableSvgPackageService(
    private val qrCodeService: QrCodeService = QrCodeService(),
    private val vCardService: VCardService = VCardService(),
) {
    /**
     * Writes the single self-contained editable SVG (both card faces, embedded artwork) and
     * returns the .svg file. Used by batch export, where each card is a loose file already
     * inside the batch ZIP.
     */
    fun generateEditableSvg(
        context: Context,
        data: EmployeeCardData,
        config: TemplateConfig,
        outputDir: File? = null,
    ): File {
        val dir = exportDir(context, outputDir)
        val safeName = data.exportSafeName()
        val file = File(dir, "CALB_Business_Card_${safeName}_Editable.svg")
        file.writeText(buildCardSvg(context, data, config), Charsets.UTF_8)
        return file
    }

    /**
     * Zips the editable SVG together with the fonts a designer must install. Apps such as
     * WeChat block a raw .svg attachment for "security" reasons, so the single-card share/save
     * flow hands off this ZIP instead; the SVG inside is still one self-contained file.
     */
    fun generateEditablePackage(
        context: Context,
        data: EmployeeCardData,
        config: TemplateConfig,
        outputDir: File? = null,
    ): File {
        val dir = exportDir(context, outputDir)
        val safeName = data.exportSafeName()
        val svg = buildCardSvg(context, data, config)
        val file = File(dir, "CALB_Business_Card_${safeName}_Editable.zip")
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            zip.writeText("${safeName}_Editable.svg", svg)
            zip.writeAsset("fonts/Manrope-Regular.otf", context, BrandFonts.MANROPE_REGULAR_ASSET)
            zip.writeAsset("fonts/Manrope-Bold.otf", context, BrandFonts.MANROPE_BOLD_ASSET)
            zip.writeAsset("fonts/HarmonyOS_Sans_SC_Regular.ttf", context, BrandFonts.HARMONY_SC_REGULAR_ASSET)
            zip.writeText("README.txt", packageReadme(safeName))
        }
        return file
    }

    private fun exportDir(context: Context, outputDir: File?): File {
        val dir = outputDir ?: File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            "exports",
        )
        dir.mkdirs()
        return dir
    }

    // Embed the artwork rather than linking it. A relative "assets/..." link breaks as soon
    // as the file is moved, and Illustrator then draws a missing-link placeholder box where
    // the logo should be. Data URIs keep the SVG self-contained.
    private fun buildCardSvg(context: Context, data: EmployeeCardData, config: TemplateConfig): String {
        val assets = SvgAssets(
            logoHref = pngDataUri(context, R.drawable.calb_logo, 1200, 400),
            watermarkHref = pngDataUri(context, R.drawable.calb_watermark_outline, 1800, 1100),
        )
        return cardDocumentSvg(context, data, config, assets)
    }

    private fun renderFrontSvg(
        context: Context,
        data: EmployeeCardData,
        config: TemplateConfig,
        assets: SvgAssets,
    ): String {
        val bleed = config.page.bleedMm
        val front = config.front
        val regularTypeface = BrandFonts.manropeRegular(context)
        val boldTypeface = BrandFonts.manropeBold(context)
        val companySize = fittedTextSizePt(
            text = data.companyLine,
            typeface = regularTypeface,
            desiredSizePt = front.companyTop.fontSize,
            minSizePt = 5.5f,
            maxWidthMm = 42f,
            trackingMm = 0.14f,
        )
        val nameSize = fittedTextSizePt(
            text = data.englishName,
            typeface = boldTypeface,
            desiredSizePt = data.nameFontSizePt,
            minSizePt = front.name.minFontSize,
            maxWidthMm = 37f,
        )
        val titleLines = wrapText(
            text = data.title,
            typeface = regularTypeface,
            fontSizePt = front.title.fontSize,
            maxWidthMm = 39f,
            maxLines = 2,
        )
        val departmentSize = fittedTextSizePt(
            text = data.department,
            typeface = regularTypeface,
            desiredSizePt = front.companyLine.fontSize,
            minSizePt = 4.8f,
            maxWidthMm = 36f,
        )
        val contactRows = buildString {
            var cursorY = front.infoLabels.y
            data.displayContactRows().forEach { row ->
                append(svgText(
                    id = "label-${row.label.lowercase(Locale.US)}",
                    text = row.label,
                    xMm = front.infoLabels.x + bleed,
                    yMm = cursorY + bleed,
                    fontFamily = SVG_MANROPE_FAMILY,
                    fontSizePt = front.infoLabels.fontSize,
                    fill = config.colors.deepBlue,
                ))
                row.values.forEachIndexed { lineIndex, value ->
                    val valueSize = fittedTextSizePt(
                        text = value,
                        typeface = regularTypeface,
                        desiredSizePt = front.infoValues.fontSize,
                        minSizePt = 4.2f,
                        maxWidthMm = 31.2f,
                    )
                    append(svgText(
                        id = "value-${row.label.lowercase(Locale.US)}-$lineIndex",
                        text = value,
                        xMm = front.infoValues.x + bleed,
                        yMm = cursorY + bleed + lineIndex * 3.75f,
                        fontFamily = SVG_MANROPE_FAMILY,
                        fontSizePt = valueSize,
                        fill = config.colors.deepBlue,
                    ))
                }
                cursorY += 3.75f * row.values.size
            }
        }

        val content = buildString {
            append("<image id=\"watermark-raster\" x=\"")
            append(svgNumber(front.watermark.x + bleed))
            append("\" y=\"")
            append(svgNumber(front.watermark.y + bleed))
            append("\" width=\"")
            append(svgNumber(front.watermark.w))
            append("\" height=\"")
            append(svgNumber(front.watermark.h))
            append("\" preserveAspectRatio=\"none\" xlink:href=\"")
            append(assets.watermarkHref)
            append("\"/>\n")
            append("<image id=\"logo-raster\" x=\"")
            append(svgNumber(front.logo.x + bleed))
            append("\" y=\"")
            append(svgNumber(front.logo.y + bleed))
            append("\" width=\"")
            append(svgNumber(front.logo.w))
            append("\" height=\"")
            append(svgNumber(front.logo.h))
            append("\" preserveAspectRatio=\"xMidYMid meet\" xlink:href=\"")
            append(assets.logoHref)
            append("\"/>\n")
            append(svgText(
                id = "company-line",
                text = data.companyLine,
                xMm = front.companyTop.x + bleed,
                yMm = front.companyTop.y + bleed,
                fontFamily = SVG_MANROPE_FAMILY,
                fontSizePt = companySize,
                fill = config.colors.deepBlue,
                letterSpacingMm = 0.14f,
            ))
            append(svgText(
                id = "name",
                text = data.englishName,
                xMm = front.name.x + bleed,
                yMm = front.name.y + bleed,
                fontFamily = SVG_MANROPE_FAMILY,
                fontSizePt = nameSize,
                fontWeight = 700,
                fill = config.colors.deepBlue,
            ))
            titleLines.forEachIndexed { index, line ->
                append(svgText(
                    id = "title-$index",
                    text = line,
                    xMm = front.title.x + bleed,
                    yMm = front.title.y + bleed + index * ptToMm(front.title.fontSize * 1.25f),
                    fontFamily = SVG_MANROPE_FAMILY,
                    fontSizePt = front.title.fontSize,
                    fill = config.colors.deepBlue,
                ))
            }
            if (data.department.isNotBlank()) {
                append(svgText(
                    id = "department",
                    text = data.department,
                    xMm = front.companyLine.x + bleed,
                    yMm = front.companyLine.y + bleed,
                    fontFamily = SVG_MANROPE_FAMILY,
                    fontSizePt = departmentSize,
                    fill = config.colors.deepBlue,
                ))
            }
            append(contactRows)
        }
        return content
    }

    private fun renderBackSvg(data: EmployeeCardData, config: TemplateConfig): String {
        val bleed = config.page.bleedMm
        val matrix = qrCodeService.generateQrMatrix(
            value = vCardService.buildVCard(data),
            sizePx = 1,
        )
        val qr = config.back.qr
        val matrixScale = qr.size / matrix.width.toFloat()
        val content = buildString {
            append("<g id=\"qr-code-vector\" transform=\"translate(")
            append(svgNumber(qr.x + bleed))
            append(' ')
            append(svgNumber(qr.y + bleed))
            append(") scale(")
            append(svgNumber(matrixScale))
            append(")\">\n<path d=\"")
            append(qrPathData(matrix))
            append("\" fill=\"")
            append(config.colors.black)
            append("\"/>\n</g>\n")
            append(svgText(
                id = "qr-caption",
                text = "Scan and Save Contact",
                xMm = bleed + config.page.finishedWidthMm / 2f,
                yMm = bleed + config.back.caption.y,
                fontFamily = SVG_MANROPE_FAMILY,
                fontSizePt = config.back.caption.fontSize,
                fill = config.colors.black,
                textAnchor = "middle",
            ))
        }
        return content
    }

    /**
     * One self-contained SVG holding both card faces, front above back, separated by
     * [CARD_GAP_MM]. The print guide pages are deliberately not included here: they are
     * production notes and ship as pages 3-4 of the Print PDF instead.
     */
    private fun cardDocumentSvg(
        context: Context,
        data: EmployeeCardData,
        config: TemplateConfig,
        assets: SvgAssets,
    ): String {
        val cardWidth = config.page.finishedWidthMm + config.page.bleedMm * 2f
        val cardHeight = config.page.finishedHeightMm + config.page.bleedMm * 2f
        val backOffset = cardHeight + CARD_GAP_MM
        val docHeight = cardHeight * 2f + CARD_GAP_MM
        return buildString {
            // The XML declaration must be the first bytes of the file. Some strict SVG
            // consumers, including Adobe Illustrator, reject whitespace before it.
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\"")
            append(svgNumber(cardWidth))
            append("mm\" height=\"")
            append(svgNumber(docHeight))
            append("mm\" viewBox=\"0 0 ")
            append(svgNumber(cardWidth))
            append(' ')
            append(svgNumber(docHeight))
            append("\">\n")
            append("  <title>CALB business card - editable SVG</title>\n")
            append("  <desc>Editable CALB artwork. Text and QR are vector objects. Raster asset elements are labelled by id.</desc>\n")
            append(cardGroupSvg(
                id = "card-front",
                config = config,
                offsetYMm = 0f,
                content = renderFrontSvg(context, data, config, assets),
            ))
            append(cardGroupSvg(
                id = "card-back",
                config = config,
                offsetYMm = backOffset,
                content = renderBackSvg(data, config),
            ))
            append("</svg>")
        }
    }

    private fun cardGroupSvg(
        id: String,
        config: TemplateConfig,
        offsetYMm: Float,
        content: String,
    ): String {
        val cardWidth = config.page.finishedWidthMm + config.page.bleedMm * 2f
        val cardHeight = config.page.finishedHeightMm + config.page.bleedMm * 2f
        return buildString {
            append("  <g id=\"")
            append(id)
            append("\" transform=\"translate(0 ")
            append(svgNumber(offsetYMm))
            append(")\">\n")
            append("    <rect id=\"paper-")
            append(id)
            append("\" x=\"0\" y=\"0\" width=\"")
            append(svgNumber(cardWidth))
            append("\" height=\"")
            append(svgNumber(cardHeight))
            append("\" fill=\"")
            append(config.colors.white)
            append("\"/>\n")
            append(cropMarksSvg(config, id).prependIndent("    "))
            append('\n')
            append(content.trimEnd().prependIndent("    "))
            append("\n  </g>\n")
        }
    }

    private fun cropMarksSvg(config: TemplateConfig, cardId: String): String {
        val bleed = config.page.bleedMm
        val left = bleed
        val top = bleed
        val right = left + config.page.finishedWidthMm
        val bottom = top + config.page.finishedHeightMm
        val mark = 3.5f
        val gap = 0.7f
        return """
            <g id="crop-marks-$cardId" fill="none" stroke="#000000" stroke-width="0.14">
              <path d="M${svgNumber(left - mark)} ${svgNumber(top)}H${svgNumber(left - gap)} M${svgNumber(left)} ${svgNumber(top - mark)}V${svgNumber(top - gap)} M${svgNumber(right + gap)} ${svgNumber(top)}H${svgNumber(right + mark)} M${svgNumber(right)} ${svgNumber(top - mark)}V${svgNumber(top - gap)}"/>
              <path d="M${svgNumber(left - mark)} ${svgNumber(bottom)}H${svgNumber(left - gap)} M${svgNumber(left)} ${svgNumber(bottom + gap)}V${svgNumber(bottom + mark)} M${svgNumber(right + gap)} ${svgNumber(bottom)}H${svgNumber(right + mark)} M${svgNumber(right)} ${svgNumber(bottom + gap)}V${svgNumber(bottom + mark)}"/>
            </g>
        """.trimIndent()
    }

    private fun svgText(
        id: String,
        text: String,
        xMm: Float,
        yMm: Float,
        fontFamily: String,
        fontSizePt: Float,
        fill: String,
        fontWeight: Int? = null,
        letterSpacingMm: Float? = null,
        textAnchor: String? = null,
    ): String = buildString {
        append("<text id=\"")
        append(escapeSvgText(id))
        append("\" x=\"")
        append(svgNumber(xMm))
        append("\" y=\"")
        append(svgNumber(yMm))
        append("\" font-family=\"")
        append(fontFamily)
        append("\" font-size=\"")
        append(svgNumber(ptToMm(fontSizePt)))
        append("\" fill=\"")
        append(fill)
        append("\"")
        fontWeight?.let { append(" font-weight=\"$it\"") }
        letterSpacingMm?.let { append(" letter-spacing=\"${svgNumber(it)}\"") }
        textAnchor?.let { append(" text-anchor=\"$it\"") }
        append(">")
        append(escapeSvgText(text))
        append("</text>\n")
    }

    private fun fittedTextSizePt(
        text: String,
        typeface: android.graphics.Typeface,
        desiredSizePt: Float,
        minSizePt: Float,
        maxWidthMm: Float,
        trackingMm: Float = 0f,
    ): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            this.typeface = typeface
            textSize = desiredSizePt
            letterSpacing = PdfMath.mmToPt(trackingMm) / textSize
        }
        val maxWidthPt = PdfMath.mmToPt(maxWidthMm)
        while (paint.measureText(text) > maxWidthPt && paint.textSize > minSizePt) {
            paint.textSize -= 0.2f
            paint.letterSpacing = PdfMath.mmToPt(trackingMm) / paint.textSize
        }
        return paint.textSize
    }

    private fun wrapText(
        text: String,
        typeface: android.graphics.Typeface?,
        fontSizePt: Float,
        maxWidthMm: Float,
        maxLines: Int,
    ): List<String> {
        if (text.isBlank()) return emptyList()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            typeface?.let { this.typeface = it }
            textSize = fontSizePt
        }
        val maxWidthPt = PdfMath.mmToPt(maxWidthMm)
        val tokens = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val lines = mutableListOf<String>()
        var current = ""
        tokens.forEach { token ->
            val candidate = if (current.isBlank()) token else "$current $token"
            if (paint.measureText(candidate) <= maxWidthPt || current.isBlank()) {
                current = candidate
            } else {
                lines += current
                current = token
            }
        }
        if (current.isNotBlank()) lines += current

        if (lines.size <= maxLines) return lines
        val clipped = lines.take(maxLines).toMutableList()
        var last = clipped.last()
        while (paint.measureText("$last...") > maxWidthPt && last.length > 3) {
            last = last.dropLast(1)
        }
        clipped[clipped.lastIndex] = "$last..."
        return clipped
    }

    /**
     * Encodes a drawable as a self-contained PNG `data:` URI for an SVG `xlink:href`.
     *
     * Decodes and re-compresses via the same [decodeSampledBitmapResource] path the working
     * PDF/preview export uses, rather than `openRawResource`: some devices store crunched or
     * indexed PNGs that `openRawResource` cannot hand back as usable bytes, which would make
     * the whole SVG export throw and silently produce nothing.
     */
    private fun pngDataUri(context: Context, resourceId: Int, maxWidthPx: Int, maxHeightPx: Int): String {
        val bitmap = decodeSampledBitmapResource(context.resources, resourceId, maxWidthPx, maxHeightPx)
            ?: return ""
        val bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
        bitmap.recycle()
        return "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun ZipOutputStream.writeText(path: String, text: String) {
        putNextEntry(ZipEntry(path))
        write(text.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun ZipOutputStream.writeAsset(path: String, context: Context, assetPath: String) {
        putNextEntry(ZipEntry(path))
        context.assets.open(assetPath).use { input -> input.copyTo(this) }
        closeEntry()
    }

    private fun packageReadme(safeName: String): String = """
        CALB editable business card

        ${safeName}_Editable.svg
          One self-contained file: front card on top, back card below. Text and the QR
          code are editable vector objects; the CALB logo and watermark are embedded, so
          the file opens correctly on its own in Adobe Illustrator.

        fonts/
          Install Manrope Regular/Bold and HarmonyOS Sans SC before editing so the text
          keeps its intended appearance.

        This ZIP is only a transport wrapper (some apps block a raw .svg attachment). Unzip
        it and open the .svg. The Print PDF remains the print-production file.
    """.trimIndent() + "\n"

    private data class SvgAssets(
        val logoHref: String,
        val watermarkHref: String,
    )
}

internal fun escapeSvgText(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")

internal fun svgNumber(value: Float): String = String.format(Locale.US, "%.3f", value)
    .trimEnd('0')
    .trimEnd('.')
    .ifBlank { "0" }

internal fun ptToMm(points: Float): Float = points / PdfMath.POINTS_PER_MM

internal fun qrPathData(matrix: com.google.zxing.common.BitMatrix): String = buildString {
    for (y in 0 until matrix.height) {
        for (x in 0 until matrix.width) {
            if (matrix[x, y]) append("M${x} ${y}h1v1h-1z")
        }
    }
}
