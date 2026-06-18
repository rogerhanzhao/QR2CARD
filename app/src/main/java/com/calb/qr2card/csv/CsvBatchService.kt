package com.calb.qr2card.csv

import com.calb.qr2card.data.EmployeeCardData
import com.calb.qr2card.domain.ValidationResult
import com.calb.qr2card.domain.ValidationService

data class BatchValidationRow(
    val rowNumber: Int,
    val data: EmployeeCardData,
    val result: ValidationResult,
)

class CsvBatchService(
    private val validationService: ValidationService = ValidationService(),
) {
    fun validateCsv(text: String): List<BatchValidationRow> {
        val rows = parseCsv(text).filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) return emptyList()

        val header = rows.first().map { it.trim() }
        return rows.drop(1).mapIndexed { index, values ->
            val map = header.zip(values + List((header.size - values.size).coerceAtLeast(0)) { "" }).toMap()
            val outcome = validationService.validateAndNormalize(map.toEmployeeCardData())
            BatchValidationRow(index + 2, outcome.data, outcome.result)
        }
    }

    fun reportCsv(rows: List<BatchValidationRow>): String {
        val header = "Row,EnglishName,Valid,Errors,Warnings"
        val body = rows.joinToString("\r\n") { row ->
            listOf(
                row.rowNumber.toString(),
                row.data.englishName,
                row.result.isValid.toString(),
                row.result.errors.joinToString(" | "),
                row.result.warnings.joinToString(" | "),
            ).joinToString(",") { escapeCsv(it) }
        }
        return "$header\r\n$body\r\n"
    }

    fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        fun finishField() {
            row += field.toString()
            field.clear()
        }

        fun finishRow() {
            finishField()
            rows += row
            row = mutableListOf()
        }

        while (i < text.length) {
            val char = text[i]
            when {
                inQuotes && char == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    field.append('"')
                    i++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> finishField()
                (char == '\n' || char == '\r') && !inQuotes -> {
                    if (char == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    finishRow()
                }
                else -> field.append(char)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) finishRow()
        return rows
    }

    private fun Map<String, String>.toEmployeeCardData(): EmployeeCardData {
        fun value(name: String): String = this[name].orEmpty().trim()
        return EmployeeCardData(
            englishName = value("EnglishName"),
            firstName = value("FirstName"),
            lastName = value("LastName"),
            title = value("Title"),
            companyLine = value("CompanyLine").ifBlank { "CALB AMERICAS INC." },
            mobileCountryIso = value("MobileCountry").ifBlank { "US" },
            mobileRawInput = value("MobileNumber"),
            email = value("Email"),
            website = value("Website").ifBlank { "https://www.calb-tech.com" },
            street = value("Street"),
            city = value("City"),
            state = value("State"),
            postcode = value("Postcode"),
            country = value("Country"),
            note = value("Note"),
        )
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
