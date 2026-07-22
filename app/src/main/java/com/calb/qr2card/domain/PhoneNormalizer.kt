package com.calb.qr2card.domain

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

data class PhoneNormalizationResult(
    val isValid: Boolean,
    val display: String = "",
    val e164: String = "",
    /** ISO country resolved from the number itself (e.g. "BR"), used for the (XX) card suffix. */
    val regionIso: String = "",
    val error: String? = null,
)

class PhoneNormalizer(
    private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance(),
) {
    /**
     * Normalizes a mobile number without tying it to one nationality.
     *
     * A leading "+" carries its own country code, so any country's number (Brazil, etc.)
     * parses regardless of [countryIso]; without "+", [countryIso] supplies the country.
     * Acceptance uses `isPossibleNumber` rather than the strict `isValidNumber`, and even an
     * unrecognised-but-non-blank number is kept as-is, so a valid contact is never blocked
     * from export just because it does not match one country's rules.
     */
    fun normalize(rawInput: String, countryIso: String): PhoneNormalizationResult {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return PhoneNormalizationResult(false, error = "Mobile number is required.")
        }
        val hintRegion = countryIso.trim().uppercase(Locale.US).takeIf { it.length == 2 }

        return try {
            val parsed = phoneUtil.parse(trimmed, hintRegion)
            val resolvedRegion = phoneUtil.getRegionCodeForNumber(parsed)
                ?.takeIf { it.isNotBlank() && it != "ZZ" }
                ?: hintRegion.orEmpty()
            if (phoneUtil.isPossibleNumber(parsed)) {
                val display = if (resolvedRegion == "US") {
                    "+${parsed.countryCode} ${phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)}"
                } else {
                    phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                }
                PhoneNormalizationResult(
                    isValid = true,
                    display = display,
                    e164 = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164),
                    regionIso = resolvedRegion,
                )
            } else {
                // Parsed but implausible length: keep the typed value rather than block export.
                PhoneNormalizationResult(true, display = trimmed, e164 = trimmed, regionIso = resolvedRegion)
            }
        } catch (error: NumberParseException) {
            // Unparseable but present: accept as typed so the card and QR still generate.
            PhoneNormalizationResult(true, display = trimmed, e164 = trimmed, regionIso = hintRegion.orEmpty())
        }
    }
}
