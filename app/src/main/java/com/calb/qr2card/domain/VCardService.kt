package com.calb.qr2card.domain

import com.calb.qr2card.data.EmployeeCardData

class VCardService {
    fun buildVCard(data: EmployeeCardData): String {
        val phone = data.mobileE164.ifBlank { data.mobileRawInput }
        val phone2 = data.mobile2E164.ifBlank { data.mobile2RawInput }
        val lines = listOfNotNull(
            "BEGIN:VCARD",
            "VERSION:3.0",
            "N:${escape(data.lastName)};${escape(data.firstName)};;;",
            "FN:${escape(data.englishName)}",
            "ORG:${escape(data.companyLine)}",
            "TITLE:${escape(data.title)}",
            "TEL;TYPE=CELL,WORK,VOICE:$phone",
            if (phone2.isNotBlank()) "TEL;TYPE=CELL,WORK,VOICE:$phone2" else null,
            "EMAIL;TYPE=INTERNET,WORK:${escape(data.email)}",
            "ADR;TYPE=WORK:;;${escape(data.street)};${escape(data.city)};${escape(data.state)};${escape(data.postcode)};${escape(data.country)}",
            "URL;TYPE=WORK:${escape(data.website)}",
            "NOTE:${escape(data.note)}",
            "END:VCARD",
        )
        return lines.joinToString(separator = "\r\n", postfix = "\r\n")
    }

    fun escape(value: String?): String {
        return value.orEmpty()
            .replace("\\", "\\\\")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\n", "\\n")
            .replace(";", "\\;")
            .replace(",", "\\,")
    }
}
