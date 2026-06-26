package com.calb.qr2card.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calb.qr2card.R
import com.calb.qr2card.csv.BatchExportService
import com.calb.qr2card.csv.BatchValidationRow
import com.calb.qr2card.csv.CsvBatchService
import com.calb.qr2card.data.EmployeeCardData
import com.calb.qr2card.data.TemplateConfig
import com.calb.qr2card.data.displayCardAddressLines
import com.calb.qr2card.data.defaultCompanyLines
import com.calb.qr2card.domain.VCardService
import com.calb.qr2card.pdf.PdfRendererService
import com.calb.qr2card.qr.QrCodeService
import com.calb.qr2card.util.BrandFonts
import com.calb.qr2card.util.decodeSampledBitmapResource
import com.calb.qr2card.util.ShareUtils
import java.util.Locale

private val DeepBlue = Color(0xFF23496B)
private val WiseGrey = Color(0xFFCEDBEA)
private const val CardWidthMm = 92f
private const val CardHeightMm = 56f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QR2CardApp(viewModel: CardViewModel) {
    val state = viewModel.uiState
    val context = LocalContext.current
    val pdfRendererService = remember { PdfRendererService() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle(state.screen)) },
                navigationIcon = {
                    if (state.screen != AppScreen.Home) {
                        TextButton(onClick = { viewModel.navigate(AppScreen.Home) }) {
                            Text("Back")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            state.statusMessage?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WiseGrey.copy(alpha = 0.45f))
                        .padding(12.dp),
                    color = DeepBlue,
                )
            }

            when (state.screen) {
                AppScreen.Home -> HomeScreen(viewModel::navigate, state.templateConfig)
                AppScreen.Single -> SingleCardScreen(
                    state = state,
                    viewModel = viewModel,
                    onGeneratePreview = {
                        val data = viewModel.validateForExport()
                        if (data != null) {
                            val file = pdfRendererService.generatePreviewImage(context, data, state.templateConfig)
                            viewModel.setLastExport(file)
                            file
                        } else {
                            null
                        }
                    },
                    onGeneratePrint = {
                        val data = viewModel.validateForExport()
                        if (data != null) {
                            val file = pdfRendererService.generatePrintPdf(context, data, state.templateConfig)
                            viewModel.setLastExport(file)
                            file
                        } else {
                            null
                        }
                    },
                    onShare = {
                        state.lastExport?.let { file ->
                            ShareUtils.shareFile(context, file, exportMimeType(file))
                        } ?: viewModel.setStatus("Generate a file before sharing.")
                    },
                )
                AppScreen.Preview -> PreviewScreen(state.cardData, state.templateConfig)
                AppScreen.Batch -> BatchScreen(state, viewModel)
                AppScreen.Settings -> SettingsScreen(state, viewModel)
                AppScreen.About -> AboutScreen()
            }
        }
    }
}

private fun screenTitle(screen: AppScreen): String = when (screen) {
    AppScreen.Home -> "CALB Business Card Generator"
    AppScreen.Single -> "Create Single Card"
    AppScreen.Preview -> "Preview"
    AppScreen.Batch -> "Batch Import"
    AppScreen.Settings -> "Settings"
    AppScreen.About -> "About"
}

private fun exportMimeType(file: java.io.File): String = when (file.extension.lowercase(Locale.US)) {
    "png" -> "image/png"
    "pdf" -> "application/pdf"
    "zip" -> "application/zip"
    else -> "application/octet-stream"
}

@Composable
private fun HomeScreen(onNavigate: (AppScreen) -> Unit, config: TemplateConfig) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BusinessCardPreview(data = EmployeeCardData(), front = true, config = config)
        Button(onClick = { onNavigate(AppScreen.Single) }, modifier = Modifier.fillMaxWidth()) {
            Text("Create Single Card")
        }
        OutlinedButton(onClick = { onNavigate(AppScreen.Batch) }, modifier = Modifier.fillMaxWidth()) {
            Text("Batch Import")
        }
        OutlinedButton(onClick = { onNavigate(AppScreen.Settings) }, modifier = Modifier.fillMaxWidth()) {
            Text("Settings")
        }
        OutlinedButton(onClick = { onNavigate(AppScreen.About) }, modifier = Modifier.fillMaxWidth()) {
            Text("About")
        }
    }
}

@Composable
private fun SingleCardScreen(
    state: CardUiState,
    viewModel: CardViewModel,
    onGeneratePreview: () -> java.io.File?,
    onGeneratePrint: () -> java.io.File?,
    onShare: () -> Unit,
) {
    val data = state.cardData
    val context = LocalContext.current
    var pendingSaveFile by remember { mutableStateOf<java.io.File?>(null) }

    fun finishSave(uri: android.net.Uri?) {
        val file = pendingSaveFile
        pendingSaveFile = null
        if (uri == null) {
            viewModel.setStatus("Save cancelled.")
            return
        }
        if (file == null) {
            viewModel.setStatus("No generated file is available to save.")
            return
        }
        runCatching {
            ShareUtils.saveFileToUri(context, file, uri)
        }.onSuccess {
            viewModel.setStatus("Saved to selected location: ${file.name}")
        }.onFailure { error ->
            viewModel.setStatus("Save failed: ${error.message}")
        }
    }
    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> finishSave(uri) }
    val savePngLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png"),
    ) { uri -> finishSave(uri) }

    fun shareExport(file: java.io.File?) {
        file?.let { ShareUtils.shareFile(context, it, exportMimeType(it)) }
    }

    fun saveExport(file: java.io.File?) {
        file?.let {
            pendingSaveFile = it
            if (it.extension.equals("png", ignoreCase = true)) {
                savePngLauncher.launch(it.name)
            } else {
                savePdfLauncher.launch(it.name)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EditableDropdown(
            label = "Department",
            value = data.companyLine,
            options = defaultCompanyLines,
            onValueChange = { value -> viewModel.updateCardData { it.copy(companyLine = value) } },
        )
        Field("English Name", data.englishName) { value -> viewModel.updateCardData { it.copy(englishName = value) } }
        Field("First Name", data.firstName) { value -> viewModel.updateCardData { it.copy(firstName = value) } }
        Field("Last Name", data.lastName) { value -> viewModel.updateCardData { it.copy(lastName = value) } }
        Field("Title", data.title) { value -> viewModel.updateCardData { it.copy(title = value) } }
        Field("Mobile Country", data.mobileCountryIso) { value -> viewModel.updateCardData { it.copy(mobileCountryIso = value.uppercase()) } }
        Field(
            label = "Mobile Number",
            value = data.mobileRawInput,
            keyboardType = KeyboardType.Phone,
            onValueChange = { value -> viewModel.updateCardData { it.copy(mobileRawInput = value) } },
        )
        Field("Mobile Country 2 (optional)", data.mobile2CountryIso) { value -> viewModel.updateCardData { it.copy(mobile2CountryIso = value.uppercase()) } }
        Field(
            label = "Mobile Number 2 (optional, China)",
            value = data.mobile2RawInput,
            keyboardType = KeyboardType.Phone,
            onValueChange = { value -> viewModel.updateCardData { it.copy(mobile2RawInput = value) } },
        )
        Field(
            label = "Email",
            value = data.email,
            keyboardType = KeyboardType.Email,
            onValueChange = { value -> viewModel.updateCardData { it.copy(email = value) } },
        )
        Field("Website", data.website) { value -> viewModel.updateCardData { it.copy(website = value) } }
        OutlinedButton(onClick = viewModel::applyBrookshirePreset, modifier = Modifier.fillMaxWidth()) {
            Text("Use Brookshire Office Address")
        }
        Field("Street", data.street) { value -> viewModel.updateCardData { it.copy(street = value) } }
        Field("City", data.city) { value -> viewModel.updateCardData { it.copy(city = value) } }
        Field("State/Province", data.state) { value -> viewModel.updateCardData { it.copy(state = value) } }
        Field("Postcode", data.postcode) { value -> viewModel.updateCardData { it.copy(postcode = value) } }
        Field("Country", data.country) { value -> viewModel.updateCardData { it.copy(country = value) } }
        Field("Company Line", data.note) { value -> viewModel.updateCardData { it.copy(note = value) } }

        ValidationPanel(state)

        Button(onClick = { viewModel.validate() }, modifier = Modifier.fillMaxWidth()) {
            Text("Validate")
        }
        OutlinedButton(onClick = { viewModel.navigate(AppScreen.Preview) }, modifier = Modifier.fillMaxWidth()) {
            Text("Preview Front / Back")
        }
        Button(onClick = { shareExport(onGeneratePreview()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Preview PNG - Share...")
        }
        OutlinedButton(onClick = { saveExport(onGeneratePreview()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Preview PNG - Save to...")
        }
        Button(onClick = { shareExport(onGeneratePrint()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Print PDF - Share...")
        }
        OutlinedButton(onClick = { saveExport(onGeneratePrint()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Print PDF - Save to...")
        }
        state.lastExport?.let { file ->
            OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                Text("Share Last Export Again")
            }
            Text("Last generated: ${file.name}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PreviewScreen(data: EmployeeCardData, config: TemplateConfig) {
    var showVCard by remember { mutableStateOf(false) }
    val vCard = remember(data) { VCardService().buildVCard(data) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BusinessCardPreview(data = data, front = true, config = config)
        BusinessCardPreview(data = data, front = false, config = config)
        OutlinedButton(onClick = { showVCard = !showVCard }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showVCard) "Hide vCard Text" else "Show vCard Text")
        }
        if (showVCard) {
            Text(
                text = vCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(1.dp, WiseGrey)
                    .padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BatchScreen(state: CardUiState, viewModel: CardViewModel) {
    val context = LocalContext.current
    val csvBatchService = remember { CsvBatchService() }
    val batchExportService = remember { BatchExportService() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8).use { reader ->
                csvBatchService.validateCsv(reader?.readText().orEmpty())
            }
        }.onSuccess(viewModel::setBatchRows)
            .onFailure { viewModel.setStatus("CSV import failed: ${it.message}") }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = { launcher.launch("text/*") }, modifier = Modifier.fillMaxWidth()) {
            Text("Import CSV")
        }
        Button(
            onClick = {
                val file = batchExportService.exportValidRows(context, state.batchRows, state.templateConfig)
                viewModel.setLastExport(file)
                ShareUtils.shareFile(context, file, "application/zip")
            },
            enabled = state.batchRows.any { it.result.isValid },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Export Valid Rows ZIP")
        }

        BatchSummary(rows = state.batchRows)
    }
}

@Composable
private fun BatchSummary(rows: List<BatchValidationRow>) {
    if (rows.isEmpty()) {
        Text("No CSV imported yet.")
        return
    }

    Text(
        text = "Valid rows: ${rows.count { it.result.isValid }} / ${rows.size}",
        fontWeight = FontWeight.Bold,
    )
    rows.take(30).forEach { row ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WiseGrey, RoundedCornerShape(6.dp))
                .padding(10.dp),
        ) {
            Text("Row ${row.rowNumber}: ${row.data.englishName}", fontWeight = FontWeight.Bold)
            Text(if (row.result.isValid) "Valid" else "Invalid")
            if (row.result.errors.isNotEmpty()) Text("Errors: ${row.result.errors.joinToString("; ")}")
            if (row.result.warnings.isNotEmpty()) Text("Warnings: ${row.result.warnings.joinToString("; ")}")
        }
    }
}

@Composable
private fun SettingsScreen(state: CardUiState, viewModel: CardViewModel) {
    val data = state.cardData
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Default Company Information", fontWeight = FontWeight.Bold)
        EditableDropdown(
            label = "Default Company Line",
            value = data.note,
            options = defaultCompanyLines,
            onValueChange = { value -> viewModel.updateCardData { it.copy(note = value) } },
        )
        Field("Default Website", data.website) { value -> viewModel.updateCardData { it.copy(website = value) } }
        HorizontalDivider()
        Text("Address Presets", fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = viewModel::applyBrookshirePreset, modifier = Modifier.fillMaxWidth()) {
            Text("Apply Brookshire Office")
        }
        HorizontalDivider()
        Text("QR Options", fontWeight = FontWeight.Bold)
        Text("QR size: ${String.format(Locale.US, "%.1f", state.templateConfig.back.qr.size)} mm")
        Slider(
            value = state.templateConfig.back.qr.size,
            onValueChange = viewModel::updateQrSize,
            valueRange = 26f..30f,
        )
        Text("QR color: black. Deep Blue can be added later, but black is safer for print scanning.")
        HorizontalDivider()
        Text("Export Options", fontWeight = FontWeight.Bold)
        Text("Preview PNG: one image containing front and back card artwork")
        Text("Print PDF: 4 pages, 98 mm x 62 mm with 3 mm bleed and crop marks")
        Text("Pages 3-4 contain Chinese print-detail notes.")
        Text("Template version: V1 JSON")
    }
}

@Composable
private fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("CALB Business Card Generator", style = MaterialTheme.typography.titleLarge)
        Text("Version 0.1.0")
        Text("Internal-use business card generation tool.")
        Text("All business card data is processed locally on this device. No personal information is uploaded.")
    }
}

@Composable
private fun BusinessCardPreview(data: EmployeeCardData, front: Boolean, config: TemplateConfig) {
    val context = LocalContext.current
    val logoBitmap = remember {
        decodeSampledBitmapResource(context.resources, R.drawable.calb_logo, 1200, 400)
    }
    val watermarkBitmap = remember {
        decodeSampledBitmapResource(context.resources, R.drawable.calb_watermark_outline, 1800, 1100)
    }
    val qrBitmap = remember(data) {
        val vCard = VCardService().buildVCard(data)
        QrCodeService().generateQrBitmap(vCard, 1024, backgroundColor = AndroidColor.TRANSPARENT)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(CardWidthMm / CardHeightMm)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .border(1.dp, WiseGrey, RoundedCornerShape(4.dp)),
    ) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            if (front) {
                drawBusinessCardFront(
                    nativeCanvas,
                    context,
                    size.width,
                    size.height,
                    data,
                    config,
                    logoBitmap,
                    watermarkBitmap,
                )
            } else {
                drawBusinessCardBack(nativeCanvas, context, size.width, size.height, data, config, qrBitmap)
            }
        }
    }
}

private fun drawBusinessCardFront(
    canvas: android.graphics.Canvas,
    context: Context,
    widthPx: Float,
    heightPx: Float,
    data: EmployeeCardData,
    config: TemplateConfig,
    logoBitmap: Bitmap?,
    watermarkBitmap: Bitmap?,
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val blue = AndroidColor.rgb(35, 73, 107)
    val regularTypeface = BrandFonts.manropeRegular(context)
    val boldTypeface = BrandFonts.manropeBold(context)
    fun x(mm: Float) = widthPx * mm / CardWidthMm
    fun y(mm: Float) = heightPx * mm / CardHeightMm
    fun pt(value: Float) = value * heightPx / (CardHeightMm * 72f / 25.4f)

    drawPreviewPaperBackground(canvas, widthPx, heightPx)

    logoBitmap?.let { logo ->
        val box = config.front.logo
        drawBitmapFit(
            canvas = canvas,
            bitmap = logo,
            target = RectF(x(box.x), y(box.y), x(box.x + box.w), y(box.y + box.h)),
        )
    }

    watermarkBitmap?.let { watermark ->
        val box = config.front.watermark
        drawBitmapStretch(
            canvas = canvas,
            bitmap = watermark,
            target = RectF(x(box.x), y(box.y), x(box.x + box.w), y(box.y + box.h)),
        )
    }

    paint.color = blue
    paint.style = Paint.Style.FILL
    paint.strokeWidth = 0f
    paint.typeface = regularTypeface
    paint.textAlign = Paint.Align.LEFT

    val front = config.front
    drawTrackedCanvasText(
        canvas,
        paint,
        "CALB Group Co., Ltd.",
        x(front.companyTop.x),
        y(front.companyTop.y),
        pt(front.companyTop.fontSize),
        x(42f),
        x(0.14f),
        typeface = regularTypeface,
    )
    drawFittedCanvasText(
        canvas = canvas,
        paint = paint,
        text = data.englishName,
        x = x(front.name.x),
        baseline = y(front.name.y),
        fontPx = pt(front.name.fontSize),
        maxWidthPx = x(34f),
        typeface = boldTypeface,
        minFontPx = pt(front.name.minFontSize),
    )
    drawWrappedCanvasText(
        canvas,
        paint,
        data.title,
        x(front.title.x),
        y(front.title.y),
        pt(front.title.fontSize),
        x(38f),
        2,
        y(3.1f),
        typeface = regularTypeface,
    )
    drawFittedCanvasText(
        canvas,
        paint,
        data.companyLine,
        x(front.companyLine.x),
        y(front.companyLine.y),
        pt(front.companyLine.fontSize),
        x(36f),
        typeface = regularTypeface,
    )

    val mobileLines = buildList {
        add(data.mobileDisplay)
        if (data.mobile2Display.isNotBlank()) add(data.mobile2Display)
    }
    val labels = listOf("Mobile", "Mail", "Postcode", "Address")
    val values = listOf(
        mobileLines,
        listOf(data.email),
        listOf(data.postcode),
        data.displayCardAddressLines(),
    )
    val rowGap = 3.75f
    var cursorYmm = front.infoLabels.y
    labels.forEachIndexed { index, label ->
        val baseline = y(cursorYmm)
        drawFittedCanvasText(
            canvas,
            paint,
            label,
            x(front.infoLabels.x),
            baseline,
            pt(front.infoLabels.fontSize),
            x(10.2f),
            typeface = regularTypeface,
        )
        values[index].forEachIndexed { lineIndex, value ->
            drawFittedCanvasText(
                canvas,
                paint,
                value,
                x(front.infoValues.x),
                baseline + y(rowGap * lineIndex),
                pt(front.infoValues.fontSize),
                x(31.2f),
                minFontPx = pt(4.4f),
                typeface = regularTypeface,
            )
        }
        cursorYmm += rowGap * values[index].size
    }
}

private fun drawBusinessCardBack(
    canvas: android.graphics.Canvas,
    context: Context,
    widthPx: Float,
    heightPx: Float,
    data: EmployeeCardData,
    config: TemplateConfig,
    qrBitmap: Bitmap,
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val regularTypeface = BrandFonts.manropeRegular(context)
    fun x(mm: Float) = widthPx * mm / CardWidthMm
    fun y(mm: Float) = heightPx * mm / CardHeightMm
    fun pt(value: Float) = value * heightPx / (CardHeightMm * 72f / 25.4f)

    drawPreviewPaperBackground(canvas, widthPx, heightPx)
    drawBitmapFit(
        canvas = canvas,
        bitmap = qrBitmap,
        target = RectF(
            x(config.back.qr.x),
            y(config.back.qr.y),
            x(config.back.qr.x + config.back.qr.size),
            y(config.back.qr.y + config.back.qr.size),
        ),
    )
    paint.color = AndroidColor.rgb(34, 34, 34)
    paint.style = Paint.Style.FILL
    paint.textAlign = Paint.Align.CENTER
    paint.typeface = regularTypeface
    paint.textSize = pt(config.back.caption.fontSize)
    canvas.drawText("Scan and Save Contact", widthPx / 2f, y(config.back.caption.y), paint)
}

private fun drawBitmapFit(
    canvas: android.graphics.Canvas,
    bitmap: Bitmap,
    target: RectF,
) {
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

private fun drawBitmapStretch(
    canvas: android.graphics.Canvas,
    bitmap: Bitmap,
    target: RectF,
) {
    canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), target, null)
}

private fun drawPreviewPaperBackground(
    canvas: android.graphics.Canvas,
    widthPx: Float,
    heightPx: Float,
) {
    canvas.drawColor(AndroidColor.WHITE)
    val speckPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(206, 219, 234)
        alpha = 6
        strokeWidth = 0.45f
    }
    var row = 0
    var y = heightPx * 2.2f / CardHeightMm
    while (y < heightPx) {
        var px = widthPx * (if (row % 2 == 0) 1.4f else 4.8f) / CardWidthMm
        while (px < widthPx) {
            canvas.drawPoint(px, y, speckPaint)
            px += widthPx * 8.6f / CardWidthMm
        }
        row += 1
        y += heightPx * 5.8f / CardHeightMm
    }
}

private fun drawFittedCanvasText(
    canvas: android.graphics.Canvas,
    paint: Paint,
    text: String,
    x: Float,
    baseline: Float,
    fontPx: Float,
    maxWidthPx: Float,
    minFontPx: Float = fontPx * 0.78f,
    typeface: Typeface = Typeface.DEFAULT,
) {
    paint.style = Paint.Style.FILL
    paint.strokeWidth = 0f
    paint.textAlign = Paint.Align.LEFT
    paint.typeface = typeface
    paint.textSize = fontPx
    while (paint.measureText(text) > maxWidthPx && paint.textSize > minFontPx) {
        paint.textSize -= 0.5f
    }
    canvas.drawText(text, x, baseline, paint)
}

private fun drawTrackedCanvasText(
    canvas: android.graphics.Canvas,
    paint: Paint,
    text: String,
    x: Float,
    baseline: Float,
    fontPx: Float,
    maxWidthPx: Float,
    trackingPx: Float,
    minFontPx: Float = fontPx * 0.78f,
    typeface: Typeface = Typeface.DEFAULT,
) {
    paint.style = Paint.Style.FILL
    paint.strokeWidth = 0f
    paint.textAlign = Paint.Align.LEFT
    paint.typeface = typeface
    paint.textSize = fontPx
    while (measureTrackedCanvasText(paint, text, trackingPx) > maxWidthPx && paint.textSize > minFontPx) {
        paint.textSize -= 0.5f
    }
    var cursor = x
    text.forEach { char ->
        val value = char.toString()
        canvas.drawText(value, cursor, baseline, paint)
        cursor += paint.measureText(value) + trackingPx
    }
}

private fun measureTrackedCanvasText(paint: Paint, text: String, trackingPx: Float): Float {
    if (text.isEmpty()) return 0f
    var width = 0f
    text.forEach { char -> width += paint.measureText(char.toString()) }
    return width + trackingPx * (text.length - 1)
}

private fun drawWrappedCanvasText(
    canvas: android.graphics.Canvas,
    paint: Paint,
    text: String,
    x: Float,
    firstBaseline: Float,
    fontPx: Float,
    maxWidthPx: Float,
    maxLines: Int,
    lineGapPx: Float,
    typeface: Typeface = Typeface.DEFAULT,
) {
    paint.style = Paint.Style.FILL
    paint.strokeWidth = 0f
    paint.textAlign = Paint.Align.LEFT
    paint.typeface = typeface
    paint.textSize = fontPx
    val lines = wrapCanvasText(paint, text, maxWidthPx, maxLines)
    lines.forEachIndexed { index, line ->
        canvas.drawText(line, x, firstBaseline + index * lineGapPx, paint)
    }
}

private fun wrapCanvasText(
    paint: Paint,
    text: String,
    maxWidthPx: Float,
    maxLines: Int,
): List<String> {
    val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
    val lines = mutableListOf<String>()
    var current = ""
    words.forEach { word ->
        val candidate = if (current.isBlank()) word else "$current $word"
        if (paint.measureText(candidate) <= maxWidthPx || current.isBlank()) {
            current = candidate
        } else {
            lines += current
            current = word
        }
    }
    if (current.isNotBlank()) lines += current
    if (lines.size <= maxLines) return lines
    val clipped = lines.take(maxLines).toMutableList()
    var last = clipped.last()
    while (paint.measureText("$last...") > maxWidthPx && last.length > 3) {
        last = last.dropLast(1)
    }
    clipped[clipped.lastIndex] = "$last..."
    return clipped
}

@Composable
private fun ValidationPanel(state: CardUiState) {
    val result = state.validationResult ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (result.isValid) WiseGrey else Color(0xFFB00020), RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(if (result.isValid) "Validation passed" else "Validation failed", fontWeight = FontWeight.Bold)
        result.errors.forEach { Text("Error: $it", color = Color(0xFFB00020)) }
        result.warnings.forEach { Text("Warning: $it", color = DeepBlue) }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = label != "Note",
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Editable dropdown: the user can type a custom value freely, while still being
 * able to pick from [options]. Suggestions are filtered by what has been typed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableDropdown(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = remember(value, options) {
        if (value.isBlank()) options
        else options.filter { it.contains(value, ignoreCase = true) && it != value }
    }
    ExposedDropdownMenuBox(
        expanded = expanded && suggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && suggestions.isNotEmpty())
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
        )
        ExposedDropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            suggestions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
