package com.calb.qr2card.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.calb.qr2card.R
import com.calb.qr2card.data.EmployeeCardData
import com.calb.qr2card.data.TemplateConfig
import com.calb.qr2card.data.displayCardAddressLines
import com.calb.qr2card.data.exportSafeName
import com.calb.qr2card.domain.VCardService
import com.calb.qr2card.qr.QrCodeService
import com.calb.qr2card.util.BrandFonts
import com.calb.qr2card.util.decodeSampledBitmapResource
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

private const val PREVIEW_IMAGE_DPI = 300f
private const val PREVIEW_IMAGE_GAP_MM = 4f

class PdfRendererService(
    private val qrCodeService: QrCodeService = QrCodeService(),
    private val vCardService: VCardService = VCardService(),
) {
    fun generatePreviewImage(
        context: Context,
        data: EmployeeCardData,
        config: TemplateConfig,
        outputDir: File? = null,
    ): File {
        val scale = PREVIEW_IMAGE_DPI / 72f
        val cardWidthPt = PdfMath.mmToPt(config.page.finishedWidthMm)
        val cardHeightPt = PdfMath.mmToPt(config.page.finishedHeightMm)
        val cardWidthPx = (cardWidthPt * scale).roundToInt()
        val cardHeightPx = (cardHeightPt * scale).roundToInt()
        val gapPx = (PdfMath.mmToPt(PREVIEW_IMAGE_GAP_MM) * scale).roundToInt()

        val frontBitmap = renderCardBitmap(cardWidthPx, cardHeightPx, scale) { canvas ->
            renderFrontPage(context, canvas, data, config, isPrintMode = false)
        }
        val backBitmap = renderCardBitmap(cardWidthPx, cardHeightPx, scale) { canvas ->
            renderBackPage(context, canvas, data, config, isPrintMode = false)
        }
        val previewBitmap = Bitmap.createBitmap(
            cardWidthPx,
            cardHeightPx * 2 + gapPx,
            Bitmap.Config.ARGB_8888,
        )
        Canvas(previewBitmap).apply {
            drawColor(Color.WHITE)
            drawBitmap(frontBitmap, 0f, 0f, null)
            drawBitmap(backBitmap, 0f, (cardHeightPx + gapPx).toFloat(), null)
        }

        val dir = outputDir ?: File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            "exports",
        )
        dir.mkdirs()
        val file = File(dir, "CALB_Business_Card_${data.exportSafeName()}_Preview.png")
        FileOutputStream(file).use { output ->
            previewBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        frontBitmap.recycle()
        backBitmap.recycle()
        previewBitmap.recycle()
        return file
    }

    fun generatePrintPdf(
        context: Context,
        data: EmployeeCardData,
        config: TemplateConfig,
        outputDir: File? = null,
    ): File = generatePdf(
        context = context,
        data = data,
        config = config,
        isPrintMode = true,
        outputDir = outputDir,
        fileName = "CALB_Business_Card_${data.exportSafeName()}_Print.pdf",
    )

    private fun generatePdf(
        context: Context,
        data: EmployeeCardData,
        config: TemplateConfig,
        isPrintMode: Boolean,
        outputDir: File?,
        fileName: String,
    ): File {
        val bleed = if (isPrintMode) config.page.bleedMm else 0f
        val pageWidth = PdfMath.mmToPt(config.page.finishedWidthMm + bleed * 2).roundToInt()
        val pageHeight = PdfMath.mmToPt(config.page.finishedHeightMm + bleed * 2).roundToInt()
        val document = PdfDocument()

        val frontPage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
        renderFrontPage(context, frontPage.canvas, data, config, isPrintMode)
        document.finishPage(frontPage)

        val backPage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create())
        renderBackPage(context, backPage.canvas, data, config, isPrintMode)
        document.finishPage(backPage)

        val specPage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3).create())
        renderPrintSpecPage(context, specPage.canvas, config, isPrintMode)
        document.finishPage(specPage)

        val colorPage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 4).create())
        renderColorSpecPage(context, colorPage.canvas, config, isPrintMode)
        document.finishPage(colorPage)

        val dir = outputDir ?: File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            "exports",
        )
        dir.mkdirs()
        val file = File(dir, fileName)
        FileOutputStream(file).use { output -> document.writeTo(output) }
        document.close()
        return file
    }

    private fun renderCardBitmap(
        widthPx: Int,
        heightPx: Int,
        scale: Float,
        render: (Canvas) -> Unit,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            scale(scale, scale)
            render(this)
        }
        return bitmap
    }

    fun renderFrontPage(
        context: Context,
        canvas: Canvas,
        data: EmployeeCardData,
        config: TemplateConfig,
        isPrintMode: Boolean,
    ) {
        val trimOffsetMm = if (isPrintMode) config.page.bleedMm else 0f
        val deepBlue = Color.parseColor(config.colors.deepBlue)
        val regularTypeface = BrandFonts.manropeRegular(context)
        val boldTypeface = BrandFonts.manropeBold(context)
        drawPaperBackground(canvas, config, paperEffect = !isPrintMode)

        if (isPrintMode) drawCropMarks(canvas, config)

        decodeSampledBitmapResource(context.resources, R.drawable.calb_logo, 1200, 400)?.let { logo ->
            drawBitmapFit(canvas, logo, rect(config.front.logo, trimOffsetMm))
        }

        decodeSampledBitmapResource(context.resources, R.drawable.calb_watermark_outline, 1800, 1100)?.let { watermark ->
            drawBitmapStretch(canvas, watermark, rect(config.front.watermark, trimOffsetMm))
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = deepBlue
            typeface = regularTypeface
        }
        drawTrackedFittedText(
            canvas = canvas,
            paint = textPaint,
            text = "CALB Group Co., Ltd.",
            x = x(config.front.companyTop.x, trimOffsetMm),
            y = y(config.front.companyTop.y, trimOffsetMm),
            desiredSize = config.front.companyTop.fontSize,
            minSize = 5.5f,
            maxWidth = PdfMath.mmToPt(42f),
            tracking = PdfMath.mmToPt(0.14f),
        )
        textPaint.typeface = regularTypeface

        drawFittedText(
            canvas = canvas,
            paint = textPaint.apply { typeface = boldTypeface },
            text = data.englishName,
            x = x(config.front.name.x, trimOffsetMm),
            y = y(config.front.name.y, trimOffsetMm),
            desiredSize = config.front.name.fontSize,
            minSize = config.front.name.minFontSize,
            maxWidth = PdfMath.mmToPt(37f),
        )
        textPaint.typeface = regularTypeface

        drawWrappedText(
            canvas = canvas,
            paint = textPaint,
            text = data.title,
            x = x(config.front.title.x, trimOffsetMm),
            y = y(config.front.title.y, trimOffsetMm),
            fontSize = config.front.title.fontSize,
            maxWidth = PdfMath.mmToPt(39f),
            maxLines = 2,
        )

        textPaint.textSize = config.front.companyLine.fontSize
        canvas.drawText(
            data.companyLine,
            x(config.front.companyLine.x, trimOffsetMm),
            y(config.front.companyLine.y, trimOffsetMm),
            textPaint,
        )

        drawContactBlock(canvas, data, config, trimOffsetMm, textPaint)
    }

    fun renderBackPage(
        context: Context,
        canvas: Canvas,
        data: EmployeeCardData,
        config: TemplateConfig,
        isPrintMode: Boolean,
    ) {
        val trimOffsetMm = if (isPrintMode) config.page.bleedMm else 0f
        drawPaperBackground(canvas, config, paperEffect = !isPrintMode)
        if (isPrintMode) drawCropMarks(canvas, config)

        val vCard = vCardService.buildVCard(data)
        val qrBitmap = qrCodeService.generateQrBitmap(vCard, 1024, backgroundColor = Color.TRANSPARENT)
        canvas.drawBitmap(
            qrBitmap,
            null,
            rect(config.back.qr, trimOffsetMm),
            Paint(Paint.FILTER_BITMAP_FLAG).apply {
                isAntiAlias = false
                isFilterBitmap = false
            },
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(config.colors.black)
            textSize = config.back.caption.fontSize
            typeface = BrandFonts.manropeRegular(context)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Scan and Save Contact",
            PdfMath.mmToPt(trimOffsetMm + config.page.finishedWidthMm / 2f),
            y(config.back.caption.y, trimOffsetMm),
            paint,
        )
    }

    private fun renderPrintSpecPage(
        context: Context,
        canvas: Canvas,
        config: TemplateConfig,
        isPrintMode: Boolean,
    ) {
        val trimOffsetMm = if (isPrintMode) config.page.bleedMm else 0f
        val deepBlue = Color.parseColor(config.colors.deepBlue)
        val wiseGrey = Color.parseColor(config.colors.wiseGrey)
        val chineseTypeface = BrandFonts.harmonyScRegular(context)
        drawPaperBackground(canvas, config, paperEffect = false)
        if (isPrintMode) drawCropMarks(canvas, config)

        decodeSampledBitmapResource(context.resources, R.drawable.calb_logo, 1200, 400)?.let { logo ->
            drawBitmapFit(canvas, logo, RectF(
                x(7.0f, trimOffsetMm),
                y(6.5f, trimOffsetMm),
                x(29.0f, trimOffsetMm),
                y(13.3f, trimOffsetMm),
            ))
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = deepBlue
            typeface = chineseTypeface
            textSize = 7.0f
        }
        canvas.drawText("印刷规格说明", x(7.0f, trimOffsetMm), y(20.0f, trimOffsetMm), titlePaint)

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = deepBlue
            typeface = chineseTypeface
            textSize = 4.4f
        }
        val lines = listOf(
            "第1页：名片正面，白底冰白珠光纸效果。",
            "第2页：名片背面，白底 vCard 二维码。",
            "成品尺寸：92.0 × 56.0 mm。",
            "打印版：四周 3.0 mm 出血，页面 98.0 × 62.0 mm。",
            "纸张：冰白珠光 300g。",
        ) + BrandFonts.PRINT_FONT_INFO_LINES + listOf(
            "第3-4页为印刷说明，不参与名片裁切。",
        )
        drawInstructionLines(
            canvas = canvas,
            paint = bodyPaint,
            lines = lines,
            x = x(7.0f, trimOffsetMm),
            firstBaseline = y(27.0f, trimOffsetMm),
            lineGap = PdfMath.mmToPt(4.0f),
            maxWidth = PdfMath.mmToPt(78.0f),
        )

        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = wiseGrey
            strokeWidth = 0.7f
        }
        canvas.drawLine(x(7.0f, trimOffsetMm), y(22.2f, trimOffsetMm), x(85.0f, trimOffsetMm), y(22.2f, trimOffsetMm), rulePaint)
    }

    private fun renderColorSpecPage(
        context: Context,
        canvas: Canvas,
        config: TemplateConfig,
        isPrintMode: Boolean,
    ) {
        val trimOffsetMm = if (isPrintMode) config.page.bleedMm else 0f
        val deepBlue = Color.parseColor(config.colors.deepBlue)
        val wiseGrey = Color.parseColor(config.colors.wiseGrey)
        val boldTypeface = BrandFonts.manropeBold(context)
        val chineseTypeface = BrandFonts.harmonyScRegular(context)
        drawPaperBackground(canvas, config, paperEffect = false)
        if (isPrintMode) drawCropMarks(canvas, config)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = deepBlue
            typeface = chineseTypeface
            textSize = 7.0f
        }
        canvas.drawText("颜色与输出要求", x(7.0f, trimOffsetMm), y(9.5f, trimOffsetMm), titlePaint)

        drawColorSpecBlock(
            canvas = canvas,
            trimOffsetMm = trimOffsetMm,
            yMm = 15.0f,
            fillColor = deepBlue,
            labelColor = Color.WHITE,
            label = "DEEP BLUE",
            chinese = "深邃蓝",
            details = listOf("PMS:7700C；CMYK:92/75/46/8", "RGB:35/73/107；HEX:#23496B"),
            config = config,
            labelTypeface = boldTypeface,
            chineseTypeface = chineseTypeface,
            detailTypeface = chineseTypeface,
        )
        drawColorSpecBlock(
            canvas = canvas,
            trimOffsetMm = trimOffsetMm,
            yMm = 29.5f,
            fillColor = wiseGrey,
            labelColor = deepBlue,
            label = "WISE GREY",
            chinese = "智慧灰",
            details = listOf("PMS:642C；CMYK:23/11/5/0", "RGB:206/219/234；HEX:#CEDBEA"),
            config = config,
            labelTypeface = boldTypeface,
            chineseTypeface = chineseTypeface,
            detailTypeface = chineseTypeface,
        )

        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = deepBlue
            typeface = chineseTypeface
            textSize = 4.1f
        }
        drawInstructionLines(
            canvas = canvas,
            paint = notePaint,
            lines = listOf(
                "QR：黑色输出，保留静区，交付前需实机扫码。",
                "Preview PNG 用于校对；Print PDF 为可下载印刷版。",
            ),
            x = x(7.0f, trimOffsetMm),
            firstBaseline = y(47.0f, trimOffsetMm),
            lineGap = PdfMath.mmToPt(4.0f),
            maxWidth = PdfMath.mmToPt(78.0f),
        )
    }

    private fun drawPaperBackground(
        canvas: Canvas,
        config: TemplateConfig,
        paperEffect: Boolean,
    ) {
        canvas.drawColor(Color.parseColor(config.colors.white))
        if (!paperEffect) return

        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        val speckPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(config.colors.wiseGrey)
            alpha = 6
            strokeWidth = 0.45f
        }
        var row = 0
        var y = PdfMath.mmToPt(2.2f)
        while (y < height) {
            var px = PdfMath.mmToPt(if (row % 2 == 0) 1.4f else 4.8f)
            while (px < width) {
                canvas.drawPoint(px, y, speckPaint)
                px += PdfMath.mmToPt(8.6f)
            }
            row += 1
            y += PdfMath.mmToPt(5.8f)
        }
    }

    private fun drawColorSpecBlock(
        canvas: Canvas,
        trimOffsetMm: Float,
        yMm: Float,
        fillColor: Int,
        labelColor: Int,
        label: String,
        chinese: String,
        details: List<String>,
        config: TemplateConfig,
        labelTypeface: Typeface,
        chineseTypeface: Typeface,
        detailTypeface: Typeface,
    ) {
        val box = RectF(
            x(7.0f, trimOffsetMm),
            y(yMm, trimOffsetMm),
            x(30.5f, trimOffsetMm),
            y(yMm + 10.5f, trimOffsetMm),
        )
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
        canvas.drawRect(box, fillPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            typeface = labelTypeface
            textSize = 4.5f
        }
        val labelParts = label.split(" ")
        canvas.drawText(labelParts.first(), x(9.0f, trimOffsetMm), y(yMm + 3.8f, trimOffsetMm), labelPaint)
        if (labelParts.size > 1) {
            canvas.drawText(labelParts.drop(1).joinToString(" "), x(9.0f, trimOffsetMm), y(yMm + 6.8f, trimOffsetMm), labelPaint)
        }
        labelPaint.textSize = 4.0f
        labelPaint.typeface = chineseTypeface
        canvas.drawText(chinese, x(9.0f, trimOffsetMm), y(yMm + 9.4f, trimOffsetMm), labelPaint)

        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(config.colors.deepBlue)
            typeface = detailTypeface
            textSize = 4.0f
        }
        drawInstructionLines(
            canvas = canvas,
            paint = detailPaint,
            lines = details,
            x = x(34.0f, trimOffsetMm),
            firstBaseline = y(yMm + 4.1f, trimOffsetMm),
            lineGap = PdfMath.mmToPt(4.1f),
            maxWidth = PdfMath.mmToPt(51.0f),
        )
    }

    private fun drawInstructionLines(
        canvas: Canvas,
        paint: Paint,
        lines: List<String>,
        x: Float,
        firstBaseline: Float,
        lineGap: Float,
        maxWidth: Float,
    ) {
        var baseline = firstBaseline
        lines.forEach { line ->
            val wrapped = wrapForPaint(paint, line, maxWidth, maxLines = 2)
            wrapped.forEach { text ->
                canvas.drawText(text, x, baseline, paint)
                baseline += lineGap
            }
        }
    }

    private fun drawContactBlock(
        canvas: Canvas,
        data: EmployeeCardData,
        config: TemplateConfig,
        trimOffsetMm: Float,
        basePaint: Paint,
    ) {
        val labels = listOf("Mobile", "Mail", "Postcode", "Address")
        val values = listOf(
            listOf(data.mobileDisplay),
            listOf(data.email),
            listOf(data.postcode),
            data.displayCardAddressLines(),
        )
        val labelPaint = Paint(basePaint).apply { textSize = config.front.infoLabels.fontSize }
        val valuePaint = Paint(basePaint).apply { textSize = config.front.infoValues.fontSize }
        val rowGap = PdfMath.mmToPt(3.75f)
        var cursorY = y(config.front.infoLabels.y, trimOffsetMm)

        labels.forEachIndexed { index, label ->
            canvas.drawText(label, x(config.front.infoLabels.x, trimOffsetMm), cursorY, labelPaint)
            values[index].forEachIndexed { lineIndex, value ->
                drawFittedText(
                    canvas = canvas,
                    paint = valuePaint,
                    text = value,
                    x = x(config.front.infoValues.x, trimOffsetMm),
                    y = cursorY + rowGap * lineIndex,
                    desiredSize = config.front.infoValues.fontSize,
                    minSize = 4.2f,
                    maxWidth = PdfMath.mmToPt(31.2f),
                )
            }
            cursorY += if (index == labels.lastIndex) rowGap * values[index].size else rowGap
        }
    }

    private fun drawCropMarks(canvas: Canvas, config: TemplateConfig) {
        val bleed = config.page.bleedMm
        val left = PdfMath.mmToPt(bleed)
        val top = PdfMath.mmToPt(bleed)
        val right = left + PdfMath.mmToPt(config.page.finishedWidthMm)
        val bottom = top + PdfMath.mmToPt(config.page.finishedHeightMm)
        val mark = PdfMath.mmToPt(3.5f)
        val gap = PdfMath.mmToPt(0.7f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = 0.4f
        }

        canvas.drawLine(left - mark, top, left - gap, top, paint)
        canvas.drawLine(left, top - mark, left, top - gap, paint)
        canvas.drawLine(right + gap, top, right + mark, top, paint)
        canvas.drawLine(right, top - mark, right, top - gap, paint)
        canvas.drawLine(left - mark, bottom, left - gap, bottom, paint)
        canvas.drawLine(left, bottom + gap, left, bottom + mark, paint)
        canvas.drawLine(right + gap, bottom, right + mark, bottom, paint)
        canvas.drawLine(right, bottom + gap, right, bottom + mark, paint)
    }

    private fun drawBitmapFit(canvas: Canvas, bitmap: Bitmap, target: RectF) {
        val sourceRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val targetRatio = target.width() / target.height()
        val fitted = RectF(target)
        if (sourceRatio > targetRatio) {
            val fittedHeight = target.width() / sourceRatio
            fitted.top = target.top + (target.height() - fittedHeight) / 2f
            fitted.bottom = fitted.top + fittedHeight
        } else {
            val fittedWidth = target.height() * sourceRatio
            fitted.left = target.left + (target.width() - fittedWidth) / 2f
            fitted.right = fitted.left + fittedWidth
        }
        canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), fitted, null)
    }

    private fun drawBitmapStretch(canvas: Canvas, bitmap: Bitmap, target: RectF) {
        canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), target, null)
    }

    private fun drawFittedText(
        canvas: Canvas,
        paint: Paint,
        text: String,
        x: Float,
        y: Float,
        desiredSize: Float,
        minSize: Float,
        maxWidth: Float,
    ) {
        paint.textSize = desiredSize
        while (paint.measureText(text) > maxWidth && paint.textSize > minSize) {
            paint.textSize -= 0.2f
        }
        canvas.drawText(text, x, y, paint)
    }

    private fun drawTrackedFittedText(
        canvas: Canvas,
        paint: Paint,
        text: String,
        x: Float,
        y: Float,
        desiredSize: Float,
        minSize: Float,
        maxWidth: Float,
        tracking: Float,
    ) {
        paint.textSize = desiredSize
        while (measureTrackedText(paint, text, tracking) > maxWidth && paint.textSize > minSize) {
            paint.textSize -= 0.2f
        }
        drawTrackedText(canvas, paint, text, x, y, tracking)
    }

    private fun drawTrackedText(
        canvas: Canvas,
        paint: Paint,
        text: String,
        x: Float,
        y: Float,
        tracking: Float,
    ) {
        var cursor = x
        text.forEach { char ->
            val value = char.toString()
            canvas.drawText(value, cursor, y, paint)
            cursor += paint.measureText(value) + tracking
        }
    }

    private fun measureTrackedText(paint: Paint, text: String, tracking: Float): Float {
        if (text.isEmpty()) return 0f
        var width = 0f
        text.forEach { char -> width += paint.measureText(char.toString()) }
        return width + tracking * (text.length - 1)
    }

    private fun drawWrappedText(
        canvas: Canvas,
        paint: Paint,
        text: String,
        x: Float,
        y: Float,
        fontSize: Float,
        maxWidth: Float,
        maxLines: Int,
    ) {
        paint.textSize = fontSize
        val lines = wrapForPaint(paint, text, maxWidth, maxLines)
        val lineHeight = fontSize * 1.25f
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, x, y + lineHeight * index, paint)
        }
    }

    private fun wrapForPaint(
        paint: Paint,
        text: String,
        maxWidth: Float,
        maxLines: Int,
    ): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isBlank()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth || current.isBlank()) {
                current = candidate
            } else {
                lines += current
                current = word
            }
        }
        if (current.isNotBlank()) lines += current
        if (lines.size <= maxLines) return lines
        val clipped = lines.take(maxLines).toMutableList()
        val last = clipped.last()
        var ellipsized = "$last..."
        while (paint.measureText(ellipsized) > maxWidth && ellipsized.length > 4) {
            ellipsized = ellipsized.dropLast(4) + "..."
        }
        clipped[clipped.lastIndex] = ellipsized
        return clipped
    }

    private fun rect(box: TemplateConfig.BoxMm, trimOffsetMm: Float): RectF = RectF(
        x(box.x, trimOffsetMm),
        y(box.y, trimOffsetMm),
        x(box.x + box.w, trimOffsetMm),
        y(box.y + box.h, trimOffsetMm),
    )

    private fun rect(box: TemplateConfig.SquareMm, trimOffsetMm: Float): RectF = RectF(
        x(box.x, trimOffsetMm),
        y(box.y, trimOffsetMm),
        x(box.x + box.size, trimOffsetMm),
        y(box.y + box.size, trimOffsetMm),
    )

    private fun rect(box: TemplateConfig.WatermarkMm, trimOffsetMm: Float): RectF = RectF(
        x(box.x, trimOffsetMm),
        y(box.y, trimOffsetMm),
        x(box.x + box.w, trimOffsetMm),
        y(box.y + box.h, trimOffsetMm),
    )

    private fun x(mm: Float, trimOffsetMm: Float): Float = PdfMath.mmToPt(mm + trimOffsetMm)

    private fun y(mm: Float, trimOffsetMm: Float): Float = PdfMath.mmToPt(mm + trimOffsetMm)

}
