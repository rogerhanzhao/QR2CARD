package com.calb.qr2card.domain

import com.calb.qr2card.data.EmployeeCardData

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
)

data class ValidationOutcome(
    val data: EmployeeCardData,
    val result: ValidationResult,
)

class ValidationService(
    private val phoneNormalizer: PhoneNormalizer = PhoneNormalizer(),
    private val allowedEmailDomain: String = "calb-tech.com",
) {
    private val emailRegex = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)

    fun validateAndNormalize(data: EmployeeCardData): ValidationOutcome {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        fun requireField(label: String, value: String) {
            if (value.isBlank()) errors += "$label is required."
        }

        requireField("English name", data.englishName)
        requireField("First name", data.firstName)
        requireField("Last name", data.lastName)
        requireField("Title", data.title)
        requireField("Company line", data.companyLine)
        requireField("Email", data.email)
        requireField("Street", data.street)
        requireField("City", data.city)
        requireField("Postcode", data.postcode)
        requireField("Country", data.country)

        if (data.email.isNotBlank() && !emailRegex.matches(data.email)) {
            errors += "Email format is invalid."
        } else if (
            data.email.isNotBlank() &&
            !data.email.endsWith("@$allowedEmailDomain", ignoreCase = true)
        ) {
            warnings += "Email domain is not $allowedEmailDomain."
        }

        val phone = phoneNormalizer.normalize(data.mobileRawInput, data.mobileCountryIso)
        if (!phone.isValid) {
            errors += phone.error ?: "Mobile number is invalid."
        }

        // Second mobile is optional: only validate when a number is entered.
        val phone2 = if (data.mobile2RawInput.isNotBlank()) {
            phoneNormalizer.normalize(data.mobile2RawInput, data.mobile2CountryIso).also {
                if (!it.isValid) errors += it.error ?: "Second mobile number is invalid."
            }
        } else {
            null
        }

        if (data.englishName.length > 28) warnings += "Name may be too long for the template."
        if (data.title.length > 44) warnings += "Title may wrap or require smaller text."
        if (data.email.length > 36) warnings += "Email may require smaller text."
        if (data.street.length + data.city.length + data.state.length + data.postcode.length > 58) {
            warnings += "Address may exceed two lines."
        }

        var normalizedData = if (phone.isValid) {
            data.copy(
                mobileCountryIso = phone.regionIso.takeIf { it.isNotBlank() } ?: data.mobileCountryIso.uppercase(),
                mobileDisplay = phone.display,
                mobileE164 = phone.e164,
            )
        } else {
            data
        }
        normalizedData = when {
            phone2 == null -> normalizedData.copy(
                mobile2CountryIso = normalizedData.mobile2CountryIso.uppercase(),
                mobile2Display = "",
                mobile2E164 = "",
            )
            phone2.isValid -> normalizedData.copy(
                mobile2CountryIso = phone2.regionIso.takeIf { it.isNotBlank() } ?: normalizedData.mobile2CountryIso.uppercase(),
                mobile2Display = phone2.display,
                mobile2E164 = phone2.e164,
            )
            else -> normalizedData
        }

        return ValidationOutcome(
            data = normalizedData,
            result = ValidationResult(errors.isEmpty(), errors, warnings),
        )
    }
}
