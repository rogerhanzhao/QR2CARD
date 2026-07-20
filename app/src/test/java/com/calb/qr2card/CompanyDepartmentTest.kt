package com.calb.qr2card

import com.calb.qr2card.data.EmployeeCardData
import com.calb.qr2card.domain.VCardService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanyDepartmentTest {
    @Test
    fun keepsTheOriginalCompanyLineAsTheDefault() {
        assertEquals("CALB Group Co., Ltd.", EmployeeCardData().companyLine)
    }

    @Test
    fun allowsABlankDepartmentWithoutChangingTheCompany() {
        val vCard = VCardService().buildVCard(
            EmployeeCardData(companyLine = "CALB Americas Inc", department = ""),
        )

        assertTrue(vCard.contains("ORG:CALB Americas Inc\r\n"))
        assertFalse(vCard.contains("ORG:CALB Americas Inc;"))
    }
}
