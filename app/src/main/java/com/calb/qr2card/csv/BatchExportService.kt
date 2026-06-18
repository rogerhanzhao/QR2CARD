package com.calb.qr2card.csv

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.calb.qr2card.data.TemplateConfig
import com.calb.qr2card.data.exportSafeName
import com.calb.qr2card.domain.VCardService
import com.calb.qr2card.pdf.PdfRendererService
import com.calb.qr2card.qr.QrCodeService
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BatchExportService(
    private val pdfRendererService: PdfRendererService = PdfRendererService(),
    private val qrCodeService: QrCodeService = QrCodeService(),
    private val vCardService: VCardService = VCardService(),
    private val csvBatchService: CsvBatchService = CsvBatchService(),
) {
    fun exportValidRows(
        context: Context,
        rows: List<BatchValidationRow>,
        config: TemplateConfig,
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val exportRoot = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            "batch_exports/$timestamp",
        )
        exportRoot.mkdirs()
        val zipFile = File(exportRoot.parentFile, "CALB_Business_Cards_$timestamp.zip")
        val validRows = rows.filter { it.result.isValid }

        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            zip.addText("CALB_Business_Cards/batch_validation_report.csv", csvBatchService.reportCsv(rows))

            validRows.forEach { row ->
                val safeName = row.data.exportSafeName()
                val folder = "CALB_Business_Cards/$safeName/"
                val personDir = File(exportRoot, safeName).apply { mkdirs() }

                val preview = pdfRendererService.generatePreviewPdf(context, row.data, config, personDir)
                val print = pdfRendererService.generatePrintPdf(context, row.data, config, personDir)
                zip.addFile(folder + preview.name, preview)
                zip.addFile(folder + print.name, print)

                val vCard = vCardService.buildVCard(row.data)
                zip.addText("$folder$safeName.vcf", vCard)

                val qrFile = File(personDir, "${safeName}_QR.png")
                FileOutputStream(qrFile).use { output ->
                    qrCodeService.generateQrBitmap(vCard, 1024)
                        .compress(Bitmap.CompressFormat.PNG, 100, output)
                }
                zip.addFile(folder + qrFile.name, qrFile)
            }
        }

        return zipFile
    }

    private fun ZipOutputStream.addText(path: String, text: String) {
        putNextEntry(ZipEntry(path))
        write(text.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun ZipOutputStream.addFile(path: String, file: File) {
        putNextEntry(ZipEntry(path))
        FileInputStream(file).use { input -> input.copyTo(this) }
        closeEntry()
    }
}
