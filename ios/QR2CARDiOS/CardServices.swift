import Foundation

enum ValidationService {
    static func validateAndNormalize(_ input: EmployeeCardData) -> (EmployeeCardData, ValidationResult) {
        var data = input
        var errors: [String] = []
        var warnings: [String] = []

        func require(_ label: String, _ value: String) {
            if value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                errors.append("\(label) is required.")
            }
        }

        require("English name", data.englishName)
        require("First name", data.firstName)
        require("Last name", data.lastName)
        require("Title", data.title)
        require("Company line", data.companyLine)
        require("Email", data.email)
        require("Street", data.street)
        require("City", data.city)
        require("Postcode", data.postcode)
        require("Country", data.country)

        if !data.email.isEmpty && !isValidEmail(data.email) {
            errors.append("Email format is invalid.")
        } else if !data.email.isEmpty && !data.email.lowercased().hasSuffix("@calb-tech.com") {
            warnings.append("Email domain is not calb-tech.com.")
        }

        let phone = normalizePhone(data.mobileRawInput, region: data.mobileCountryIso)
        if phone.isValid {
            data.mobileCountryIso = data.mobileCountryIso.uppercased()
            data.mobileDisplay = "\(phone.e164) (\(data.mobileCountryIso))"
            data.mobileE164 = phone.e164
        } else {
            errors.append(phone.error)
        }

        // Second mobile is optional: only validate when a number is entered.
        data.mobile2CountryIso = data.mobile2CountryIso.uppercased()
        if data.mobile2RawInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            data.mobile2Display = ""
            data.mobile2E164 = ""
        } else {
            let phone2 = normalizePhone(data.mobile2RawInput, region: data.mobile2CountryIso)
            if phone2.isValid {
                data.mobile2Display = "\(phone2.e164) (\(data.mobile2CountryIso))"
                data.mobile2E164 = phone2.e164
            } else {
                errors.append("Second mobile number is invalid for \(data.mobile2CountryIso).")
            }
        }

        if data.englishName.count > 28 {
            warnings.append("Name may be too long for the template.")
        }
        if data.title.count > 44 {
            warnings.append("Title may wrap or require smaller text.")
        }
        if data.email.count > 36 {
            warnings.append("Email may require smaller text.")
        }

        return (data, ValidationResult(isValid: errors.isEmpty, errors: errors, warnings: warnings))
    }

    private static func isValidEmail(_ value: String) -> Bool {
        let pattern = #"^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$"#
        return value.range(of: pattern, options: [.regularExpression, .caseInsensitive]) != nil
    }

    private static func normalizePhone(_ raw: String, region: String) -> (isValid: Bool, e164: String, error: String) {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        let digits = trimmed.filter(\.isNumber)
        let normalizedRegion = region.uppercased()

        if trimmed.hasPrefix("+"), !digits.isEmpty {
            return (true, "+\(digits)", "")
        }
        if normalizedRegion == "US", digits.count == 10 {
            return (true, "+1\(digits)", "")
        }
        if normalizedRegion == "US", digits.count == 11, digits.hasPrefix("1") {
            return (true, "+\(digits)", "")
        }
        if normalizedRegion == "CN", digits.count == 11 {
            return (true, "+86\(digits)", "")
        }
        return (false, "", "Mobile number is invalid for \(normalizedRegion).")
    }
}

enum VCardService {
    static func buildVCard(_ data: EmployeeCardData) -> String {
        let address = [data.street, data.city, data.state, data.postcode, data.country]
            .map(escape)
            .joined(separator: ";")
        let secondMobile = data.mobile2E164.isEmpty ? data.mobile2RawInput : data.mobile2E164
        let lines: [String?] = [
            "BEGIN:VCARD",
            "VERSION:3.0",
            "N:\(escape(data.lastName));\(escape(data.firstName));;;",
            "FN:\(escape(data.englishName))",
            "ORG:\(escape(data.companyLine))",
            "TITLE:\(escape(data.title))",
            "TEL;TYPE=CELL:\(data.mobileE164)",
            secondMobile.isEmpty ? nil : "TEL;TYPE=CELL:\(secondMobile)",
            "EMAIL:\(escape(data.email))",
            "URL:\(escape(data.website))",
            "ADR;TYPE=WORK:;;\(address)",
            "NOTE:\(escape(data.note))",
            "END:VCARD"
        ]
        return lines.compactMap { $0 }.joined(separator: "\r\n")
    }

    private static func escape(_ value: String) -> String {
        value
            .replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: ";", with: "\\;")
            .replacingOccurrences(of: ",", with: "\\,")
            .replacingOccurrences(of: "\n", with: "\\n")
            .replacingOccurrences(of: "\r", with: "")
    }
}
