package com.calb.qr2card.data

data class AddressPreset(
    val name: String,
    val street: String,
    val city: String,
    val state: String,
    val postcode: String,
    val country: String,
    val displayCountryShort: String,
)

data class EmployeeCardData(
    val englishName: String = "Alex Zhao",
    val firstName: String = "Alex",
    val lastName: String = "Zhao",
    val title: String = "Director of Pre-sale & Solution",
    val companyLine: String = "CALB Americas Inc",
    val mobileCountryIso: String = "US",
    val mobileRawInput: String = "4015927928",
    val mobileDisplay: String = "+14015927928 (US)",
    val mobileE164: String = "+14015927928",
    val email: String = "alex.zhao@calb-tech.com",
    val website: String = "https://www.calb-tech.com",
    val street: String = "839 FM 1489 Rd",
    val city: String = "Brookshire",
    val state: String = "TX",
    val postcode: String = "77423",
    val country: String = "United States",
    val note: String = "CALB Group Co. Ltd.",
)

val defaultCompanyLines = listOf(
    "CALB Group Co., Ltd.",
    "CALB Americas Inc",
    "CALB AMERICAS INC.",
)

val defaultAddressPresets = listOf(
    AddressPreset(
        name = "Brookshire Office",
        street = "839 FM 1489 Rd",
        city = "Brookshire",
        state = "TX",
        postcode = "77423",
        country = "United States",
        displayCountryShort = "US",
    ),
)

fun EmployeeCardData.displayAddress(): String {
    val statePostcode = listOf(state, postcode)
        .filter { it.isNotBlank() }
        .joinToString(" ")
    val locality = listOf(city, statePostcode)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    return listOf(street, locality, countryShort())
        .filter { it.isNotBlank() }
        .joinToString(", ")
}

fun EmployeeCardData.displayCardAddressLines(): List<String> {
    val line1 = street.trim()
        .takeIf { it.isNotBlank() }
        ?.let { if (it.endsWith(",")) it else "$it," }
        .orEmpty()
    val locality = listOf(city, state)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    val line2 = listOf(locality, "${postcode.trim()},${countryShort()}")
        .filter { it.isNotBlank() }
        .joinToString(" ")
    return listOf(line1, line2).filter { it.isNotBlank() }
}

fun EmployeeCardData.countryShort(): String = when (country.trim().lowercase()) {
    "united states", "usa", "us" -> "US"
    "china", "cn", "prc" -> "CN"
    else -> country
}

fun EmployeeCardData.exportSafeName(): String =
    englishName.ifBlank { listOf(firstName, lastName).joinToString(" ").trim() }
        .ifBlank { "Business_Card" }
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_')
        .ifBlank { "Business_Card" }
