package com.calb.qr2card.domain

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

data class PhoneNormalizationResult(
    val isValid: Boolean,
    val display: String = "",
    val e164: String = "",
    val error: String? = null,
)

class PhoneNormalizer(
    private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance(),
) {
    fun normalize(rawInput: String, countryIso: String): PhoneNormalizationResult {
        val region = countryIso.trim().uppercase(Locale.US)
        if (rawInput.isBlank()) {
            return PhoneNormalizationResult(false, error = "Mobile number is required.")
        }
        if (region.length != 2) {
            return PhoneNormalizationResult(false, error = "Mobile country must be a 2-letter ISO code.")
        }

        return try {
            val parsed = phoneUtil.parse(rawInput, region)
            val isValid = phoneUtil.isValidNumber(parsed)
            if (!isValid) {
                PhoneNormalizationResult(false, error = "Mobile number is invalid for $region.")
            } else {
                val display = phoneUtil
                    .format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                    .replace('\u00A0', ' ')
                    .replace('-', ' ')
                    .replace(Regex("\\s+"), " ")
                    .trim() + " ($region)"
                PhoneNormalizationResult(
                    isValid = true,
                    display = display,
                    e164 = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164),
                )
            }
        } catch (error: NumberParseException) {
            PhoneNormalizationResult(false, error = error.message ?: "Mobile number cannot be parsed.")
        }
    }
}
