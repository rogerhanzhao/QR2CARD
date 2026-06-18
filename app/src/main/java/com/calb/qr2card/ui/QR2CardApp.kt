package com.calb.qr2card.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calb.qr2card.R
import com.calb.qr2card.csv.BatchExportService
import com.calb.qr2card.csv.BatchValidationRow
import com.calb.qr2card.csv.CsvBatchService
import com.calb.qr2card.data.EmployeeCardData
import com.calb.qr2card.data.defaultCompanyLines
import com.calb.qr2card.data.displayAddress
import com.calb.qr2card.domain.VCardService
import com.calb.qr2card.pdf.PdfRendererService
import com.calb.qr2card.qr.QrCodeService
import com.calb.qr2card.util.ShareUtils
import java.util.Locale

private val DeepBlue = Color(0xFF23496B)
private val WiseGrey = Color(0xFFCEDBEA)

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
                AppScreen.Home -> HomeScreen(viewModel::navigate)
                AppScreen.Single -> SingleCardScreen(
                    state = state,
                    viewModel = viewModel,
                    onGeneratePreview = {
                        viewModel.validateForExport()?.let { data ->
                            val file = pdfRendererService.generatePreviewPdf(context, data, state.templateConfig)
                            viewModel.setLastExport(file)
                        }
                    },
                    onGeneratePrint = {
                        viewModel.validateForExport()?.let { data ->
                            val file = pdfRendererService.generatePrintPdf(context, data, state.templateConfig)
                            viewModel.setLastExport(file)
                        }
                    },
                    onShare = {
                        state.lastExport?.let { file ->
                            val mime = if (file.extension.equals("zip", ignoreCase = true)) {
                                "application/zip"
                            } else {
                                "application/pdf"
                            }
                            ShareUtils.shareFile(context, file, mime)
                        } ?: viewModel.setStatus("Generate a file before sharing.")
                    },
                )
                AppScreen.Preview -> PreviewScreen(state.cardData)
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

@Composable
private fun HomeScreen(onNavigate: (AppScreen) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BusinessCardPreview(data = EmployeeCardData(), front = true)
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
    onGeneratePreview: () -> Unit,
    onGeneratePrint: () -> Unit,
    onShare: () -> Unit,
) {
    val data = state.cardData
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SimpleDropdown(
            label = "Company Line",
            selected = data.companyLine,
            options = defaultCompanyLines,
            onSelected = { selected -> viewModel.updateCardData { it.copy(companyLine = selected) } },
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
        Field("Note", data.note) { value -> viewModel.updateCardData { it.copy(note = value) } }

        ValidationPanel(state)

        Button(onClick = { viewModel.validate() }, modifier = Modifier.fillMaxWidth()) {
            Text("Validate")
        }
        OutlinedButton(onClick = { viewModel.navigate(AppScreen.Preview) }, modifier = Modifier.fillMaxWidth()) {
            Text("Preview Front / Back")
        }
        Button(onClick = onGeneratePreview, modifier = Modifier.fillMaxWidth()) {
            Text("Generate Preview PDF")
        }
        Button(onClick = onGeneratePrint, modifier = Modifier.fillMaxWidth()) {
            Text("Generate Print PDF")
        }
        OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
            Text("Share Last Export")
        }
        state.lastExport?.let { file ->
            Text("Last file: ${file.absolutePath}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PreviewScreen(data: EmployeeCardData) {
    var showVCard by remember { mutableStateOf(false) }
    val vCard = remember(data) { VCardService().buildVCard(data) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BusinessCardPreview(data = data, front = true)
        BusinessCardPreview(data = data, front = false)
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
        SimpleDropdown(
            label = "Default Company Line",
            selected = data.companyLine,
            options = defaultCompanyLines,
            onSelected = { selected -> viewModel.updateCardData { it.copy(companyLine = selected) } },
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
        Text("Preview PDF: 92 mm x 56 mm")
        Text("Print PDF: 98 mm x 62 mm with 3 mm bleed")
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
private fun BusinessCardPreview(data: EmployeeCardData, front: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(92f / 56f)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .border(1.dp, WiseGrey, RoundedCornerShape(4.dp))
            .padding(18.dp),
    ) {
        if (front) {
            Text(
                "CALB",
                modifier = Modifier.align(Alignment.BottomCenter),
                color = WiseGrey.copy(alpha = 0.55f),
                fontSize = 58.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
            Image(
                painter = painterResource(R.drawable.calb_logo),
                contentDescription = "CALB logo",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(120.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                "CALB Group Co., Ltd.",
                modifier = Modifier.align(Alignment.TopEnd),
                color = DeepBlue,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
            )
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(data.englishName, color = DeepBlue, fontSize = 21.sp, fontWeight = FontWeight.Medium)
                Text(data.title, color = DeepBlue, fontSize = 10.sp)
                Text(data.companyLine, color = DeepBlue, fontSize = 10.sp)
            }
            Column(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                PreviewInfoRow("Mobile", data.mobileDisplay)
                PreviewInfoRow("Mail", data.email)
                PreviewInfoRow("Postcode", data.postcode)
                PreviewInfoRow("Address", data.displayAddress())
            }
        } else {
            val qrImage = remember(data) {
                val vCard = VCardService().buildVCard(data)
                QrCodeService().generateQrBitmap(vCard, 512).asImageBitmap()
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    bitmap = qrImage,
                    contentDescription = "vCard QR",
                    modifier = Modifier.size(132.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("Scan and Save Contact", color = Color(0xFF222222), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun PreviewInfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = DeepBlue, fontSize = 9.sp, modifier = Modifier.width(44.dp))
        Text(value, color = DeepBlue, fontSize = 9.sp, modifier = Modifier.width(130.dp))
    }
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

@Composable
private fun SimpleDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected.ifBlank { "Select" }, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
