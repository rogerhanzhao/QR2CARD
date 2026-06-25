import Foundation
import UIKit
import CoreImage
import CoreImage.CIFilterBuiltins

final class BusinessCardRenderer {
    private let template = CardTemplate()
    private let qrContext = CIContext()

    func makePreviewPNG(data: EmployeeCardData) throws -> URL {
        let scale: CGFloat = 300.0 / 72.0
        let cardSize = finishedCardSize
        let gapPt = CardConstants.mmToPt(4.0)
        let canvasSize = CGSize(width: cardSize.width, height: cardSize.height * 2 + gapPt)
        let format = UIGraphicsImageRendererFormat()
        format.scale = scale
        format.opaque = true

        let image = UIGraphicsImageRenderer(size: canvasSize, format: format).image { context in
            UIColor.white.setFill()
            context.cgContext.fill(CGRect(origin: .zero, size: canvasSize))
            renderFrontPage(context.cgContext, data: data, isPrintMode: false)
            context.cgContext.saveGState()
            context.cgContext.translateBy(x: 0, y: cardSize.height + gapPt)
            renderBackPage(context.cgContext, data: data, isPrintMode: false)
            context.cgContext.restoreGState()
        }

        let url = outputURL(name: "CALB_Business_Card_\(data.safeExportName)_Preview.png")
        guard let png = image.pngData() else {
            throw ExportError.imageEncodingFailed
        }
        try png.write(to: url, options: .atomic)
        return url
    }

    func makeFrontPreviewImage(data: EmployeeCardData) -> UIImage {
        makeSingleCardPreviewImage(data: data, front: true)
    }

    func makeBackPreviewImage(data: EmployeeCardData) -> UIImage {
        makeSingleCardPreviewImage(data: data, front: false)
    }

    func makePrintPDF(data: EmployeeCardData) throws -> URL {
        let bleed = template.page.bleedMm
        let pageSize = CGSize(
            width: CardConstants.mmToPt(template.page.finishedWidthMm + bleed * 2),
            height: CardConstants.mmToPt(template.page.finishedHeightMm + bleed * 2)
        )
        let url = outputURL(name: "CALB_Business_Card_\(data.safeExportName)_Print.pdf")
        let renderer = UIGraphicsPDFRenderer(bounds: CGRect(origin: .zero, size: pageSize))
        try renderer.writePDF(to: url) { context in
            context.beginPage()
            renderFrontPage(context.cgContext, data: data, isPrintMode: true)
            context.beginPage()
            renderBackPage(context.cgContext, data: data, isPrintMode: true)
            context.beginPage()
            renderPrintSpecPage(context.cgContext, isPrintMode: true)
            context.beginPage()
            renderColorSpecPage(context.cgContext, isPrintMode: true)
        }
        return url
    }

    private var finishedCardSize: CGSize {
        CGSize(
            width: CardConstants.mmToPt(template.page.finishedWidthMm),
            height: CardConstants.mmToPt(template.page.finishedHeightMm)
        )
    }

    private func makeSingleCardPreviewImage(data: EmployeeCardData, front: Bool) -> UIImage {
        let format = UIGraphicsImageRendererFormat()
        format.scale = UIScreen.main.scale
        format.opaque = true
        return UIGraphicsImageRenderer(size: finishedCardSize, format: format).image { rendererContext in
            if front {
                renderFrontPage(rendererContext.cgContext, data: data, isPrintMode: false)
            } else {
                renderBackPage(rendererContext.cgContext, data: data, isPrintMode: false)
            }
        }
    }

    private func pageSize(isPrintMode: Bool) -> CGSize {
        guard isPrintMode else { return finishedCardSize }
        let bleed = template.page.bleedMm
        return CGSize(
            width: CardConstants.mmToPt(template.page.finishedWidthMm + bleed * 2),
            height: CardConstants.mmToPt(template.page.finishedHeightMm + bleed * 2)
        )
    }

    private func outputURL(name: String) -> URL {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("QR2CARD", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory.appendingPathComponent(name)
    }

    private func renderFrontPage(_ context: CGContext, data: EmployeeCardData, isPrintMode: Bool) {
        let offsetMm = isPrintMode ? template.page.bleedMm : 0
        drawPaperBackground(context, size: pageSize(isPrintMode: isPrintMode), paperEffect: !isPrintMode)
        if isPrintMode {
            drawCropMarks(context)
        }

        if let logo = UIImage(named: "calb_logo") {
            drawImageFit(logo, in: rect(template.logo, offsetMm: offsetMm))
        }
        if let watermark = UIImage(named: "calb_watermark_outline") {
            watermark.draw(in: rect(template.watermark, offsetMm: offsetMm))
        }

        let blue = UIColor(hex: template.deepBlue)
        let regular = brandFont(size: template.companyTop.fontSize)
        drawTrackedText(
            "CALB Group Co., Ltd.",
            x: x(template.companyTop.x, offsetMm),
            baseline: y(template.companyTop.y, offsetMm),
            font: regular,
            color: blue,
            maxWidth: CardConstants.mmToPt(42),
            tracking: CardConstants.mmToPt(0.14),
            minSize: 5.5
        )

        drawFittedText(
            data.englishName,
            x: x(template.name.x, offsetMm),
            baseline: y(template.name.y, offsetMm),
            desiredSize: template.name.fontSize,
            minSize: template.name.minFontSize,
            maxWidth: CardConstants.mmToPt(37),
            color: blue,
            fontFactory: boldFont
        )

        drawWrappedText(
            data.title,
            x: x(template.title.x, offsetMm),
            firstBaseline: y(template.title.y, offsetMm),
            font: brandFont(size: template.title.fontSize),
            color: blue,
            maxWidth: CardConstants.mmToPt(39),
            maxLines: 2,
            lineGap: template.title.fontSize * 1.25
        )

        drawText(
            data.companyLine,
            x: x(template.companyLine.x, offsetMm),
            baseline: y(template.companyLine.y, offsetMm),
            font: brandFont(size: template.companyLine.fontSize),
            color: blue
        )
        drawContactBlock(context, data: data, offsetMm: offsetMm)
    }

    private func renderBackPage(_ context: CGContext, data: EmployeeCardData, isPrintMode: Bool) {
        let offsetMm = isPrintMode ? template.page.bleedMm : 0
        drawPaperBackground(context, size: pageSize(isPrintMode: isPrintMode), paperEffect: !isPrintMode)
        if isPrintMode {
            drawCropMarks(context)
        }
        let qr = makeQRCode(VCardService.buildVCard(data))
        qr.draw(in: rect(template.qr, offsetMm: offsetMm))
        drawCenteredText(
            "Scan and Save Contact",
            centerX: CardConstants.mmToPt(offsetMm + template.page.finishedWidthMm / 2),
            baseline: y(template.caption.y, offsetMm),
            font: brandFont(size: template.caption.fontSize),
            color: UIColor(hex: template.black)
        )
    }

    private func renderPrintSpecPage(_ context: CGContext, isPrintMode: Bool) {
        let offsetMm = isPrintMode ? template.page.bleedMm : 0
        drawPaperBackground(context, size: pageSize(isPrintMode: isPrintMode), paperEffect: false)
        drawCropMarks(context)
        if let logo = UIImage(named: "calb_logo") {
            drawImageFit(logo, in: CGRect(x: x(7, offsetMm), y: y(6.5, offsetMm), width: CardConstants.mmToPt(22), height: CardConstants.mmToPt(6.8)))
        }
        let blue = UIColor(hex: template.deepBlue)
        let grey = UIColor(hex: template.wiseGrey)
        drawText("Print specifications", x: x(7, offsetMm), baseline: y(20, offsetMm), font: harmonyFont(size: 7), color: blue)
        drawLine(context, from: CGPoint(x: x(7, offsetMm), y: y(22.2, offsetMm)), to: CGPoint(x: x(85, offsetMm), y: y(22.2, offsetMm)), color: grey, width: 0.7)

        let lines = [
            "Page 1: front card artwork.",
            "Page 2: back card artwork with vCard QR.",
            "Finished size: 92.0 x 56.0 mm.",
            "Print size: 98.0 x 62.0 mm with 3.0 mm bleed.",
            "Paper: Ice White Pearl 300g.",
            "Fonts: Manrope Regular/Bold OTF; HarmonyOS Sans SC Regular TTF.",
            "Pages 3-4 are production notes and should not be trimmed as cards."
        ]
        drawInstructionLines(lines, x: x(7, offsetMm), firstBaseline: y(27, offsetMm), font: harmonyFont(size: 4.4), color: blue)
    }

    private func renderColorSpecPage(_ context: CGContext, isPrintMode: Bool) {
        let offsetMm = isPrintMode ? template.page.bleedMm : 0
        drawPaperBackground(context, size: pageSize(isPrintMode: isPrintMode), paperEffect: false)
        drawCropMarks(context)
        let blue = UIColor(hex: template.deepBlue)
        let grey = UIColor(hex: template.wiseGrey)
        drawText("Color and output", x: x(7, offsetMm), baseline: y(9.5, offsetMm), font: harmonyFont(size: 7), color: blue)
        drawColorBlock(context, label: "DEEP BLUE", chinese: "Deep Blue", details: ["PMS:7700C; CMYK:92/75/46/8", "RGB:35/73/107; HEX:#23496B"], yMm: 15, fill: blue, textColor: .white, offsetMm: offsetMm)
        drawColorBlock(context, label: "WISE GREY", chinese: "Wise Grey", details: ["PMS:642C; CMYK:23/11/5/0", "RGB:206/219/234; HEX:#CEDBEA"], yMm: 29.5, fill: grey, textColor: blue, offsetMm: offsetMm)
        drawInstructionLines(["QR: black output with quiet zone.", "Preview PNG is for review; Print PDF is for production."], x: x(7, offsetMm), firstBaseline: y(47, offsetMm), font: harmonyFont(size: 4.1), color: blue)
    }

    private func drawContactBlock(_ context: CGContext, data: EmployeeCardData, offsetMm: CGFloat) {
        var mobileLines = [data.mobileDisplay]
        if !data.mobile2Display.isEmpty {
            mobileLines.append(data.mobile2Display)
        }
        let labels = ["Mobile", "Mail", "Postcode", "Address"]
        let values = [mobileLines, [data.email], [data.postcode], data.cardAddressLines]
        let rowGap = CardConstants.mmToPt(3.75)
        var cursorY = y(template.infoLabels.y, offsetMm)
        let blue = UIColor(hex: template.deepBlue)
        for index in labels.indices {
            drawText(labels[index], x: x(template.infoLabels.x, offsetMm), baseline: cursorY, font: brandFont(size: template.infoLabels.fontSize), color: blue)
            for (lineIndex, value) in values[index].enumerated() {
                drawFittedText(
                    value,
                    x: x(template.infoValues.x, offsetMm),
                    baseline: cursorY + rowGap * CGFloat(lineIndex),
                    desiredSize: template.infoValues.fontSize,
                    minSize: 4.2,
                    maxWidth: CardConstants.mmToPt(31.2),
                    color: blue,
                    fontFactory: brandFont
                )
            }
            cursorY += rowGap * CGFloat(values[index].count)
        }
    }

    private func drawPaperBackground(_ context: CGContext, size: CGSize, paperEffect: Bool) {
        UIColor(hex: template.white).setFill()
        context.fill(CGRect(origin: .zero, size: size))
        guard paperEffect else { return }
        let paint = UIColor(hex: template.wiseGrey).withAlphaComponent(0.035)
        paint.setStroke()
        var row = 0
        var yy = CardConstants.mmToPt(2.2)
        while yy < size.height {
            var xx = CardConstants.mmToPt(row.isMultiple(of: 2) ? 1.4 : 4.8)
            while xx < size.width {
                context.stroke(CGRect(x: xx, y: yy, width: 0.45, height: 0.45))
                xx += CardConstants.mmToPt(8.6)
            }
            row += 1
            yy += CardConstants.mmToPt(5.8)
        }
    }

    private func drawCropMarks(_ context: CGContext) {
        let bleed = template.page.bleedMm
        let left = CardConstants.mmToPt(bleed)
        let top = CardConstants.mmToPt(bleed)
        let right = left + CardConstants.mmToPt(template.page.finishedWidthMm)
        let bottom = top + CardConstants.mmToPt(template.page.finishedHeightMm)
        let mark = CardConstants.mmToPt(3.5)
        let gap = CardConstants.mmToPt(0.7)
        UIColor.black.setStroke()
        context.setLineWidth(0.4)
        [
            (CGPoint(x: left - mark, y: top), CGPoint(x: left - gap, y: top)),
            (CGPoint(x: left, y: top - mark), CGPoint(x: left, y: top - gap)),
            (CGPoint(x: right + gap, y: top), CGPoint(x: right + mark, y: top)),
            (CGPoint(x: right, y: top - mark), CGPoint(x: right, y: top - gap)),
            (CGPoint(x: left - mark, y: bottom), CGPoint(x: left - gap, y: bottom)),
            (CGPoint(x: left, y: bottom + gap), CGPoint(x: left, y: bottom + mark)),
            (CGPoint(x: right + gap, y: bottom), CGPoint(x: right + mark, y: bottom)),
            (CGPoint(x: right, y: bottom + gap), CGPoint(x: right, y: bottom + mark))
        ].forEach { start, end in
            context.move(to: start)
            context.addLine(to: end)
            context.strokePath()
        }
    }

    private func drawColorBlock(_ context: CGContext, label: String, chinese: String, details: [String], yMm: CGFloat, fill: UIColor, textColor: UIColor, offsetMm: CGFloat) {
        let box = CGRect(x: x(7, offsetMm), y: y(yMm, offsetMm), width: CardConstants.mmToPt(23.5), height: CardConstants.mmToPt(10.5))
        fill.setFill()
        context.fill(box)
        let parts = label.split(separator: " ").map(String.init)
        drawText(parts[0], x: x(9, offsetMm), baseline: y(yMm + 3.8, offsetMm), font: boldFont(size: 4.5), color: textColor)
        if parts.count > 1 {
            drawText(parts.dropFirst().joined(separator: " "), x: x(9, offsetMm), baseline: y(yMm + 6.8, offsetMm), font: boldFont(size: 4.5), color: textColor)
        }
        drawText(chinese, x: x(9, offsetMm), baseline: y(yMm + 9.4, offsetMm), font: harmonyFont(size: 4), color: textColor)
        drawInstructionLines(details, x: x(34, offsetMm), firstBaseline: y(yMm + 4.1, offsetMm), font: harmonyFont(size: 4), color: UIColor(hex: template.deepBlue))
    }

    private func drawText(_ text: String, x: CGFloat, baseline: CGFloat, font: UIFont, color: UIColor) {
        let attributes: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: color]
        (text as NSString).draw(at: CGPoint(x: x, y: baseline - font.ascender), withAttributes: attributes)
    }

    private func drawCenteredText(_ text: String, centerX: CGFloat, baseline: CGFloat, font: UIFont, color: UIColor) {
        let width = measure(text, font: font)
        drawText(text, x: centerX - width / 2, baseline: baseline, font: font, color: color)
    }

    private func drawFittedText(_ text: String, x: CGFloat, baseline: CGFloat, desiredSize: CGFloat, minSize: CGFloat, maxWidth: CGFloat, color: UIColor, fontFactory: (CGFloat) -> UIFont) {
        var size = desiredSize
        var font = fontFactory(size)
        while measure(text, font: font) > maxWidth && size > minSize {
            size -= 0.2
            font = fontFactory(size)
        }
        drawText(text, x: x, baseline: baseline, font: font, color: color)
    }

    private func drawTrackedText(_ text: String, x: CGFloat, baseline: CGFloat, font: UIFont, color: UIColor, maxWidth: CGFloat, tracking: CGFloat, minSize: CGFloat) {
        var fontSize = font.pointSize
        var currentFont = font
        while measureTracked(text, font: currentFont, tracking: tracking) > maxWidth && fontSize > minSize {
            fontSize -= 0.2
            currentFont = brandFont(size: fontSize)
        }
        var cursor = x
        for character in text.map(String.init) {
            drawText(character, x: cursor, baseline: baseline, font: currentFont, color: color)
            cursor += measure(character, font: currentFont) + tracking
        }
    }

    private func drawWrappedText(_ text: String, x: CGFloat, firstBaseline: CGFloat, font: UIFont, color: UIColor, maxWidth: CGFloat, maxLines: Int, lineGap: CGFloat) {
        let lines = wrap(text, font: font, maxWidth: maxWidth, maxLines: maxLines)
        for (index, line) in lines.enumerated() {
            drawText(line, x: x, baseline: firstBaseline + CGFloat(index) * lineGap, font: font, color: color)
        }
    }

    private func drawInstructionLines(_ lines: [String], x: CGFloat, firstBaseline: CGFloat, font: UIFont, color: UIColor) {
        var baseline = firstBaseline
        for line in lines {
            for wrapped in wrap(line, font: font, maxWidth: CardConstants.mmToPt(78), maxLines: 2) {
                drawText(wrapped, x: x, baseline: baseline, font: font, color: color)
                baseline += CardConstants.mmToPt(4.0)
            }
        }
    }

    private func drawImageFit(_ image: UIImage, in target: CGRect) {
        let sourceRatio = image.size.width / image.size.height
        let targetRatio = target.width / target.height
        var rect = target
        if sourceRatio > targetRatio {
            let fittedHeight = target.width / sourceRatio
            rect.origin.y += (target.height - fittedHeight) / 2
            rect.size.height = fittedHeight
        } else {
            let fittedWidth = target.height * sourceRatio
            rect.origin.x += (target.width - fittedWidth) / 2
            rect.size.width = fittedWidth
        }
        image.draw(in: rect)
    }

    private func drawLine(_ context: CGContext, from: CGPoint, to: CGPoint, color: UIColor, width: CGFloat) {
        color.setStroke()
        context.setLineWidth(width)
        context.move(to: from)
        context.addLine(to: to)
        context.strokePath()
    }

    private func makeQRCode(_ text: String) -> UIImage {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(text.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage else { return UIImage() }
        let transformed = output.transformed(by: CGAffineTransform(scaleX: 16, y: 16))
        guard let cgImage = qrContext.createCGImage(transformed, from: transformed.extent) else { return UIImage() }
        return UIImage(cgImage: cgImage)
    }

    private func wrap(_ text: String, font: UIFont, maxWidth: CGFloat, maxLines: Int) -> [String] {
        let words = text.split(whereSeparator: \.isWhitespace).map(String.init)
        var lines: [String] = []
        var current = ""
        for word in words {
            let candidate = current.isEmpty ? word : "\(current) \(word)"
            if measure(candidate, font: font) <= maxWidth || current.isEmpty {
                current = candidate
            } else {
                lines.append(current)
                current = word
            }
        }
        if !current.isEmpty { lines.append(current) }
        guard lines.count > maxLines else { return lines }
        var clipped = Array(lines.prefix(maxLines))
        var last = clipped[maxLines - 1]
        while measure("\(last)...", font: font) > maxWidth && last.count > 3 {
            last.removeLast()
        }
        clipped[maxLines - 1] = "\(last)..."
        return clipped
    }

    private func measure(_ text: String, font: UIFont) -> CGFloat {
        (text as NSString).size(withAttributes: [.font: font]).width
    }

    private func measureTracked(_ text: String, font: UIFont, tracking: CGFloat) -> CGFloat {
        let width = text.map(String.init).reduce(CGFloat.zero) { $0 + measure($1, font: font) }
        return width + tracking * CGFloat(max(text.count - 1, 0))
    }

    private func brandFont(size: CGFloat) -> UIFont {
        UIFont(name: "Manrope-Regular", size: size) ?? .systemFont(ofSize: size, weight: .regular)
    }

    private func boldFont(size: CGFloat) -> UIFont {
        UIFont(name: "Manrope-Bold", size: size) ?? .systemFont(ofSize: size, weight: .bold)
    }

    private func harmonyFont(size: CGFloat) -> UIFont {
        UIFont(name: "HarmonyOS_Sans_SC_Regular", size: size)
            ?? UIFont(name: "HarmonyOS_Sans_SC", size: size)
            ?? UIFont(name: "HarmonyOSSansSC-Regular", size: size)
            ?? UIFont(name: "HarmonyOS Sans SC", size: size)
            ?? .systemFont(ofSize: size, weight: .regular)
    }

    private func rect(_ box: CardBox, offsetMm: CGFloat) -> CGRect {
        CGRect(x: x(box.x, offsetMm), y: y(box.y, offsetMm), width: CardConstants.mmToPt(box.w), height: CardConstants.mmToPt(box.h))
    }

    private func rect(_ square: CardSquare, offsetMm: CGFloat) -> CGRect {
        CGRect(x: x(square.x, offsetMm), y: y(square.y, offsetMm), width: CardConstants.mmToPt(square.size), height: CardConstants.mmToPt(square.size))
    }

    private func x(_ mm: CGFloat, _ offsetMm: CGFloat) -> CGFloat {
        CardConstants.mmToPt(mm + offsetMm)
    }

    private func y(_ mm: CGFloat, _ offsetMm: CGFloat) -> CGFloat {
        CardConstants.mmToPt(mm + offsetMm)
    }

    enum ExportError: Error {
        case imageEncodingFailed
    }
}

private extension UIColor {
    convenience init(hex: String) {
        let value = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        let scanner = Scanner(string: value)
        var rgb: UInt64 = 0
        scanner.scanHexInt64(&rgb)
        self.init(
            red: CGFloat((rgb >> 16) & 0xff) / 255.0,
            green: CGFloat((rgb >> 8) & 0xff) / 255.0,
            blue: CGFloat(rgb & 0xff) / 255.0,
            alpha: 1.0
        )
    }
}
