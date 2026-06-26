import SwiftUI
import UIKit

struct ContentView: View {
    @State private var data = EmployeeCardData()
    @State private var validation = ValidationResult()
    @State private var statusMessage: String?
    @State private var previewURL: URL?
    @State private var printURL: URL?

    private let renderer = BusinessCardRenderer()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Preview")
                            .font(.headline)
                        BusinessCardArtworkPreview(data: data, front: true, renderer: renderer)
                        BusinessCardArtworkPreview(data: data, front: false, renderer: renderer)
                    }

                    formFields
                    actionButtons
                    validationPanel
                }
                .padding()
            }
            .navigationTitle("CALB Business Card")
        }
    }

    private var formFields: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Employee")
                .font(.headline)
            cardTextField("English Name", text: $data.englishName)
            HStack {
                cardTextField("First Name", text: $data.firstName)
                cardTextField("Last Name", text: $data.lastName)
            }
            cardTextField("Title", text: $data.title)
            cardTextField("Department", text: $data.companyLine)

            Text("Contact")
                .font(.headline)
                .padding(.top, 8)
            HStack {
                cardTextField("Country", text: $data.mobileCountryIso)
                    .frame(maxWidth: 120)
                cardTextField("Mobile", text: $data.mobileRawInput, keyboard: .phonePad)
            }
            HStack {
                cardTextField("Country 2", text: $data.mobile2CountryIso)
                    .frame(maxWidth: 120)
                cardTextField("Mobile 2 (optional)", text: $data.mobile2RawInput, keyboard: .phonePad)
            }
            cardTextField("Email", text: $data.email, keyboard: .emailAddress)
            cardTextField("Website", text: $data.website, keyboard: .URL)

            Text("Address")
                .font(.headline)
                .padding(.top, 8)
            cardTextField("Street", text: $data.street)
            HStack {
                cardTextField("City", text: $data.city)
                cardTextField("State", text: $data.state)
            }
            HStack {
                cardTextField("Postcode", text: $data.postcode)
                cardTextField("Country", text: $data.country)
            }
            cardTextField("Company Line", text: $data.note)
        }
    }

    private var actionButtons: some View {
        VStack(spacing: 10) {
            Button {
                _ = validateCurrentData()
            } label: {
                Label("Validate", systemImage: "checkmark.circle")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)

            Button {
                makePreview()
            } label: {
                Label("Export Preview PNG", systemImage: "photo")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)

            Button {
                makePrintPDF()
            } label: {
                Label("Export Print PDF", systemImage: "doc.richtext")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)

            if let previewURL {
                ShareLink(item: previewURL) {
                    Label("Share Preview PNG", systemImage: "square.and.arrow.up")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }

            if let printURL {
                ShareLink(item: printURL) {
                    Label("Share Print PDF", systemImage: "square.and.arrow.up")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
        }
    }

    private var validationPanel: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let statusMessage {
                Text(statusMessage)
                    .font(.subheadline)
                    .foregroundStyle(validation.isValid ? .green : .red)
            }

            if !validation.errors.isEmpty {
                Text("Errors")
                    .font(.headline)
                ForEach(validation.errors, id: \.self) { error in
                    Text(error)
                        .font(.subheadline)
                        .foregroundStyle(.red)
                }
            }

            if !validation.warnings.isEmpty {
                Text("Warnings")
                    .font(.headline)
                    .padding(.top, 4)
                ForEach(validation.warnings, id: \.self) { warning in
                    Text(warning)
                        .font(.subheadline)
                        .foregroundStyle(.orange)
                }
            }
        }
    }

    private func cardTextField(_ title: String, text: Binding<String>, keyboard: UIKeyboardType = .default) -> some View {
        TextField(title, text: text)
            .textFieldStyle(.roundedBorder)
            .textInputAutocapitalization(.never)
            .keyboardType(keyboard)
    }

    private func validateCurrentData() -> EmployeeCardData? {
        let outcome = ValidationService.validateAndNormalize(data)
        data = outcome.0
        validation = outcome.1
        previewURL = nil
        printURL = nil
        statusMessage = outcome.1.isValid ? "Validation passed." : "Validation failed."
        return outcome.1.isValid ? outcome.0 : nil
    }

    private func makePreview() {
        guard let normalized = validateCurrentData() else { return }
        do {
            let url = try renderer.makePreviewPNG(data: normalized)
            previewURL = url
            statusMessage = "Preview PNG exported: \(url.lastPathComponent)"
        } catch {
            statusMessage = "Preview PNG export failed: \(error.localizedDescription)"
        }
    }

    private func makePrintPDF() {
        guard let normalized = validateCurrentData() else { return }
        do {
            let url = try renderer.makePrintPDF(data: normalized)
            printURL = url
            statusMessage = "Print PDF exported: \(url.lastPathComponent)"
        } catch {
            statusMessage = "Print PDF export failed: \(error.localizedDescription)"
        }
    }
}

private struct BusinessCardArtworkPreview: View {
    let data: EmployeeCardData
    let front: Bool
    let renderer: BusinessCardRenderer

    var body: some View {
        let image = front ? renderer.makeFrontPreviewImage(data: data) : renderer.makeBackPreviewImage(data: data)
        Image(uiImage: image)
            .resizable()
            .scaledToFit()
            .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .stroke(Color.black.opacity(0.12), lineWidth: 1)
            )
            .accessibilityLabel(front ? "Front card preview" : "Back card preview")
    }
}
