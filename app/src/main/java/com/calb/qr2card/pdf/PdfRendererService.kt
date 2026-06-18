package com.calb.qr2card.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.calb.qr2card.data.displayAddress
import com.calb.qr2card.data.exportSafeName
import com.calb.qr2card.domain.VCardService
import com.calb.qr2card.qr.QrCodeService
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class PdfRendererService(
    private val qrCodeService: QrCodeService = QrCodeService(),
    private val vCardService: VCardService = VCardService(),
) {
    fun generatePreviewPdf(
        context: Context,
        data: EmployeeCardData,
        config: TemplateConfig,
        outputDir: File? = null,
    ): File = generatePdf(
        context = context,
        data = data,
        config = config,
        isPrintMode = false,
        outputDir = outputDir,
        fileName = "CALB_Business_Card_${data.exportSafeName()}_Preview.pdf",
    )

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
        renderBackPage(backPage.canvas, data, config, isPrintMode)
        document.finishPage(backPage)

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

    fun renderFrontPage(
        context: Context,
        canvas: Canvas,
        data: EmployeeCardData,
        config: TemplateConfig,
        isPrintMode: Boolean,
    ) {
        val trimOffsetMm = if (isPrintMode) config.page.bleedMm else 0f
        val deepBlue = Color.parseColor(config.colors.deepBlue)
        val wiseGrey = withAlpha(Color.parseColor(config.colors.wiseGrey), config.front.watermark.opacity)
        canvas.drawColor(Color.parseColor(config.colors.white))

        if (isPrintMode) drawCropMarks(canvas, config)

        BitmapFactory.decodeResource(context.resources, R.drawable.calb_logo)?.let { logo ->
            drawBitmapFit(canvas, logo, rect(config.front.logo, trimOffsetMm))
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = deepBlue
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val boldPaint = Paint(textPaint).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        drawFittedText(
            canvas = canvas,
            paint = textPaint,
            text = "CALB Group Co., Ltd.",
            x = x(config.front.companyTop.x, trimOffsetMm),
            y = y(config.front.companyTop.y, trimOffsetMm),
            desiredSize = config.front.companyTop.fontSize,
            minSize = 5.5f,
            maxWidth = PdfMath.mmToPt(39f),
        )

        drawFittedText(
            canvas = canvas,
            paint = boldPaint,
            text = data.englishName,
            x = x(config.front.name.x, trimOffsetMm),
            y = y(config.front.name.y, trimOffsetMm),
            desiredSize = config.front.name.fontSize,
            minSize = config.front.name.minFontSize,
            maxWidth = PdfMath.mmToPt(35f),
        )

        drawWrappedText(
            canvas = canvas,
            paint = textPaint,
            text = data.title,
            x = x(config.front.title.x, trimOffsetMm),
            y = y(config.front.title.y, trimOffsetMm),
            fontSize = config.front.title.fontSize,
            maxWidth = PdfMath.mmToPt(37f),
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
        drawWatermark(canvas, config, trimOffsetMm, wiseGrey)
    }

    fun renderBackPage(
        canvas: Canvas,
        data: EmployeeCardData,
        config: TemplateConfig,
        isPrintMode: Boolean,
    ) {
        val trimOffsetMm = if (isPrintMode) config.page.bleedMm else 0f
        canvas.drawColor(Color.WHITE)
        if (isPrintMode) drawCropMarks(canvas, config)

        val vCard = vCardService.buildVCard(data)
        val qrBitmap = qrCodeService.generateQrBitmap(vCard, 1024)
        canvas.drawBitmap(qrBitmap, null, rect(config.back.qr, trimOffsetMm), null)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(config.colors.black)
            textSize = config.back.caption.fontSize
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Scan and Save Contact",
            PdfMath.mmToPt(trimOffsetMm + config.page.finishedWidthMm / 2f),
            y(config.back.caption.y, trimOffsetMm),
            paint,
        )
    }

    private fun drawContactBlock(
        canvas: Canvas,
        data: EmployeeCardData,
        config: TemplateConfig,
        trimOffsetMm: Float,
        basePaint: Paint,
    ) {
        val labels = listOf("Mobile", "Mail", "Postcode", "Address")
        val addressLines = wrapForPaint(
            paint = Paint(basePaint).apply { textSize = config.front.infoValues.fontSize },
            text = data.displayAddress(),
            maxWidth = PdfMath.mmToPt(28f),
            maxLines = 2,
        )
        val values = listOf(
            listOf(data.mobileDisplay),
            listOf(data.email),
            listOf(data.postcode),
            addressLines,
        )
        val labelPaint = Paint(basePaint).apply { textSize = config.front.infoLabels.fontSize }
        val valuePaint = Paint(basePaint).apply { textSize = config.front.infoValues.fontSize }
        val rowGap = PdfMath.mmToPt(4.2f)
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
                    maxWidth = PdfMath.mmToPt(29f),
                )
            }
            cursorY += if (index == labels.lastIndex) rowGap * values[index].size else rowGap
        }
    }

    private fun drawWatermark(
        canvas: Canvas,
        config: TemplateConfig,
        trimOffsetMm: Float,
        color: Int,
    ) {
        val box = config.front.watermark
        val left = x(box.x, trimOffsetMm)
        val top = y(box.y, trimOffsetMm)
        val width = PdfMath.mmToPt(box.w)
        val height = PdfMath.mmToPt(box.h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = PdfMath.mmToPt(0.28f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        paint.textSize = height * 1.12f
        while (paint.measureText("CALB") > width && paint.textSize > 10f) {
            paint.textSize -= 1f
        }
        val fm = paint.fontMetrics
        val baseline = top + (height - fm.ascent - fm.descent) / 2f
        canvas.drawText("CALB", left, baseline, paint)
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

    private fun x(mm: Float, trimOffsetMm: Float): Float = PdfMath.mmToPt(mm + trimOffsetMm)

    private fun y(mm: Float, trimOffsetMm: Float): Float = PdfMath.mmToPt(mm + trimOffsetMm)

    private fun withAlpha(color: Int, opacity: Float): Int {
        val alpha = (opacity.coerceIn(0f, 1f) * 255).roundToInt()
        return (color and 0x00ffffff) or (alpha shl 24)
    }
}
