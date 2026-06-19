import Foundation
import CoreGraphics

struct EmployeeCardData {
    var englishName = "Alex Zhao"
    var firstName = "Alex"
    var lastName = "Zhao"
    var title = "Director of Pre-sale & Solution"
    var companyLine = "CALB Americas Inc"
    var mobileCountryIso = "US"
    var mobileRawInput = "4015927928"
    var mobileDisplay = "+14015927928 (US)"
    var mobileE164 = "+14015927928"
    var email = "alex.zhao@calb-tech.com"
    var website = "https://www.calb-tech.com"
    var street = "839 FM 1489 Rd"
    var city = "Brookshire"
    var state = "TX"
    var postcode = "77423"
    var country = "United States"
    var note = "CALB Group Co. Ltd."
}

struct ValidationResult {
    var isValid: Bool = true
    var errors: [String] = []
    var warnings: [String] = []
}

struct CardPageConfig {
    let finishedWidthMm: CGFloat = 92.0
    let finishedHeightMm: CGFloat = 56.0
    let bleedMm: CGFloat = 3.0
}

struct CardTextBox {
    let x: CGFloat
    let y: CGFloat
    let fontSize: CGFloat
    let minFontSize: CGFloat

    init(_ x: CGFloat, _ y: CGFloat, _ fontSize: CGFloat, _ minFontSize: CGFloat? = nil) {
        self.x = x
        self.y = y
        self.fontSize = fontSize
        self.minFontSize = minFontSize ?? fontSize
    }
}

struct CardBox {
    let x: CGFloat
    let y: CGFloat
    let w: CGFloat
    let h: CGFloat
}

struct CardSquare {
    let x: CGFloat
    let y: CGFloat
    let size: CGFloat
}

struct CardTemplate {
    let page = CardPageConfig()
    let deepBlue = "#23496b"
    let wiseGrey = "#cedbea"
    let black = "#1e1e1e"
    let white = "#ffffff"

    let logo = CardBox(x: 8.3, y: 7.35, w: 22.7, h: 7.1)
    let companyTop = CardTextBox(46.8, 11.5, 9.2)
    let name = CardTextBox(9.0, 24.2, 12.6, 8.5)
    let title = CardTextBox(9.0, 27.7, 6.3, 4.8)
    let companyLine = CardTextBox(9.0, 31.5, 6.3)
    let infoLabels = CardTextBox(46.9, 29.95, 6.1)
    let infoValues = CardTextBox(57.4, 29.95, 6.1)
    let watermark = CardBox(x: 0.0, y: 0.0, w: 92.0, h: 56.0)
    let qr = CardSquare(x: 31.0, y: 10.1, size: 28.0)
    let caption = CardTextBox(0.0, 40.8, 9.0)
}

extension EmployeeCardData {
    var safeExportName: String {
        let raw = englishName.isEmpty ? [firstName, lastName].joined(separator: " ") : englishName
        let cleaned = raw
            .replacingOccurrences(of: "[^A-Za-z0-9._-]+", with: "_", options: .regularExpression)
            .trimmingCharacters(in: CharacterSet(charactersIn: "_"))
        return cleaned.isEmpty ? "Business_Card" : cleaned
    }

    var shortCountry: String {
        switch country.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "united states", "usa", "us":
            return "US"
        case "china", "cn", "prc":
            return "CN"
        default:
            return country
        }
    }

    var cardAddressLines: [String] {
        let line1 = street.trimmingCharacters(in: .whitespacesAndNewlines)
        let formattedLine1 = line1.isEmpty || line1.hasSuffix(",") ? line1 : "\(line1),"
        let locality = [city, state].filter { !$0.isEmpty }.joined(separator: ", ")
        let line2 = [locality, "\(postcode.trimmingCharacters(in: .whitespacesAndNewlines)),\(shortCountry)"]
            .filter { !$0.isEmpty }
            .joined(separator: " ")
        return [formattedLine1, line2].filter { !$0.isEmpty }
    }
}

enum CardConstants {
    static let companyLines = [
        "CALB Group Co., Ltd.",
        "CALB Americas Inc",
        "CALB AMERICAS INC."
    ]

    static func mmToPt(_ mm: CGFloat) -> CGFloat {
        mm * 72.0 / 25.4
    }
}
