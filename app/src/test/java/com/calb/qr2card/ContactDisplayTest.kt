package com.calb.qr2card

import com.calb.qr2card.data.EmployeeCardData
import com.calb.qr2card.data.displayContactRows
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactDisplayTest {
    @Test
    fun usesUsAndCnTelephoneLabelsOnSeparateRows() {
        val rows = EmployeeCardData(
            mobileCountryIso = "US",
            mobileDisplay = "+1 (213) 589-7421",
            mobile2CountryIso = "CN",
            mobile2Display = "+86 139 6725 8941",
        ).displayContactRows()

        assertEquals("US Tel:", rows[0].label)
        assertEquals(listOf("+1 (213) 589-7421"), rows[0].values)
        assertEquals("CN Tel:", rows[1].label)
        assertEquals(listOf("+86 139 6725 8941"), rows[1].values)
        assertEquals("Mail", rows[2].label)
        assertEquals("Address", rows[3].label)
    }
}
