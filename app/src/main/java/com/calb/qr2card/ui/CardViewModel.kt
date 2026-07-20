package com.calb.qr2card.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.calb.qr2card.csv.BatchValidationRow
import com.calb.qr2card.data.EmployeeCardData
import com.calb.qr2card.data.TemplateConfig
import com.calb.qr2card.data.TemplateRepository
import com.calb.qr2card.data.defaultAddressPresets
import com.calb.qr2card.domain.PhoneNormalizer
import com.calb.qr2card.domain.ValidationResult
import com.calb.qr2card.domain.ValidationService
import java.io.File

enum class AppScreen {
    Home,
    Single,
    Preview,
    Batch,
    Settings,
    About,
}

data class CardUiState(
    val screen: AppScreen = AppScreen.Home,
    val cardData: EmployeeCardData = EmployeeCardData(),
    val templateConfig: TemplateConfig = TemplateConfig(),
    val validationResult: ValidationResult? = null,
    val batchRows: List<BatchValidationRow> = emptyList(),
    val lastExport: File? = null,
    val statusMessage: String? = null,
)

class CardViewModel(
    private val validationService: ValidationService = ValidationService(),
    private val templateRepository: TemplateRepository = TemplateRepository(),
    private val phoneNormalizer: PhoneNormalizer = PhoneNormalizer(),
) : ViewModel() {
    var uiState by mutableStateOf(CardUiState())
        private set

    fun loadTemplate(context: Context) {
        uiState = uiState.copy(templateConfig = templateRepository.load(context))
    }

    fun navigate(screen: AppScreen) {
        uiState = uiState.copy(screen = screen, statusMessage = null)
    }

    fun updateCardData(update: (EmployeeCardData) -> EmployeeCardData) {
        val updatedData = update(uiState.cardData)
        uiState = uiState.copy(
            cardData = updatePhoneDisplays(updatedData),
            validationResult = null,
            statusMessage = null,
        )
    }

    fun applyBrookshirePreset() {
        val preset = defaultAddressPresets.first()
        updateCardData {
            it.copy(
                street = preset.street,
                city = preset.city,
                state = preset.state,
                postcode = preset.postcode,
                country = preset.country,
            )
        }
    }

    fun validate(): Boolean {
        val outcome = validationService.validateAndNormalize(uiState.cardData)
        uiState = uiState.copy(
            cardData = outcome.data,
            validationResult = outcome.result,
            statusMessage = if (outcome.result.isValid) "Validation passed." else "Fix validation errors before export.",
        )
        return outcome.result.isValid
    }

    fun validateForExport(): EmployeeCardData? {
        return if (validate()) uiState.cardData else null
    }

    fun setBatchRows(rows: List<BatchValidationRow>) {
        val validCount = rows.count { it.result.isValid }
        uiState = uiState.copy(
            batchRows = rows,
            statusMessage = "CSV loaded: $validCount/${rows.size} valid rows.",
        )
    }

    fun setLastExport(file: File) {
        uiState = uiState.copy(
            lastExport = file,
            statusMessage = "Exported: ${file.name}",
        )
    }

    fun setStatus(message: String) {
        uiState = uiState.copy(statusMessage = message)
    }

    fun updateQrSize(sizeMm: Float) {
        val old = uiState.templateConfig
        uiState = uiState.copy(
            templateConfig = old.copy(
                back = old.back.copy(qr = old.back.qr.copy(size = sizeMm)),
            ),
        )
    }

    private fun updatePhoneDisplays(data: EmployeeCardData): EmployeeCardData {
        val primary = phoneNormalizer.normalize(data.mobileRawInput, data.mobileCountryIso)
        val secondary = if (data.mobile2RawInput.isBlank()) null else {
            phoneNormalizer.normalize(data.mobile2RawInput, data.mobile2CountryIso)
        }
        return data.copy(
            mobileDisplay = primary.display.takeIf { primary.isValid }.orEmpty(),
            mobileE164 = primary.e164.takeIf { primary.isValid }.orEmpty(),
            mobile2Display = secondary?.takeIf { it.isValid }?.display.orEmpty(),
            mobile2E164 = secondary?.takeIf { it.isValid }?.e164.orEmpty(),
        )
    }
}
