package com.calb.qr2card

import com.calb.qr2card.data.EmployeeCardData
import com.calb.qr2card.domain.VCardService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VCardServiceTest {
    private val service = VCardService()

    @Test
    fun buildsVCardWithCrLfAndExpectedPhone() {
        val vCard = service.buildVCard(EmployeeCardData())

        assertTrue(vCard.contains("\r\nVERSION:3.0\r\n"))
        assertTrue(vCard.contains("TEL;TYPE=CELL,WORK,VOICE:+14015927928\r\n"))
        assertTrue(vCard.endsWith("END:VCARD\r\n"))
    }

    @Test
    fun escapesVCardReservedCharacters() {
        val vCard = service.buildVCard(
            EmployeeCardData(
                englishName = "Alex; Zhao",
                firstName = "Alex",
                lastName = "Zhao",
                title = "Director, Pre-sale",
                companyLine = "CALB Americas Inc",
                department = "Sales; Marketing",
            ),
        )

        assertTrue(vCard.contains("FN:Alex\\; Zhao"))
        assertTrue(vCard.contains("TITLE:Director\\, Pre-sale"))
        assertTrue(vCard.contains("ORG:CALB Americas Inc;Sales\\; Marketing"))
        assertFalse(vCard.contains("NOTE:"))
    }
}
