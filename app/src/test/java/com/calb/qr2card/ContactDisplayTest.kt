package com.calb.qr2card

import com.calb.qr2card.data.EmployeeCardData
import com.calb.qr2card.data.displayContactRows
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactDisplayTest {
    @Test
    fun groupsBothNumbersUnderSingleMobileLabelWithCountrySuffix() {
        val rows = EmployeeCardData(
            mobileCountryIso = "US",
            mobileDisplay = "+1 (213) 589-7421",
            mobile2CountryIso = "CN",
            mobile2Display = "+86 139 6725 8941",
        ).displayContactRows()

        assertEquals("Mobile", rows[0].label)
        assertEquals(
            listOf("+1 (213) 589-7421 (US)", "+86 139 6725 8941 (CN)"),
            rows[0].values,
        )
        assertEquals("Mail", rows[1].label)
        assertEquals("Address", rows[2].label)
        assertEquals(listOf("839 FM 1489 Rd,", "Brookshire, TX 77423, US"), rows[2].values)
    }

    @Test
    fun omitsSecondNumberWhenBlank() {
        val rows = EmployeeCardData(
            mobileCountryIso = "US",
            mobileDisplay = "+1 (213) 589-7421",
            mobile2Display = "",
        ).displayContactRows()

        assertEquals("Mobile", rows[0].label)
        assertEquals(listOf("+1 (213) 589-7421 (US)"), rows[0].values)
    }
}
