package com.calb.qr2card

import com.calb.qr2card.csv.CsvBatchService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvBatchServiceTest {
    private val service = CsvBatchService()

    @Test
    fun validatesSampleCsv() {
        val csv = """
            EnglishName,FirstName,LastName,Title,CompanyLine,Department,MobileCountry,MobileNumber,Email,Website,Street,City,State,Postcode,Country
            Alex Zhao,Alex,Zhao,Director,CALB AMERICAS INC.,Sales,US,4015927928,alex.zhao@calb-tech.com,https://www.calb-tech.com,839 FM 1489 Rd,Brookshire,TX,77423,United States
        """.trimIndent()

        val rows = service.validateCsv(csv)

        assertEquals(1, rows.size)
        assertTrue(rows.first().result.isValid)
        assertEquals("+14015927928", rows.first().data.mobileE164)
        assertEquals("Sales", rows.first().data.department)
    }

    @Test
    fun marksInvalidRows() {
        val csv = """
            EnglishName,FirstName,LastName,Title,CompanyLine,Department,MobileCountry,MobileNumber,Email,Website,Street,City,State,Postcode,Country
            Bad Row,Bad,Row,Director,CALB AMERICAS INC.,,US,123,bad-email,https://www.calb-tech.com,839 FM 1489 Rd,Brookshire,TX,77423,United States
        """.trimIndent()

        val rows = service.validateCsv(csv)

        assertEquals(1, rows.size)
        assertFalse(rows.first().result.isValid)
    }

    @Test
    fun acceptsLegacyNoteColumnAsDepartment() {
        val csv = """
            EnglishName,FirstName,LastName,Title,CompanyLine,MobileCountry,MobileNumber,Email,Website,Street,City,State,Postcode,Country,Note
            Alex Zhao,Alex,Zhao,Director,CALB AMERICAS INC.,US,4015927928,alex.zhao@calb-tech.com,https://www.calb-tech.com,839 FM 1489 Rd,Brookshire,TX,77423,United States,Sales
        """.trimIndent()

        val rows = service.validateCsv(csv)

        assertEquals("Sales", rows.first().data.department)
    }
}
