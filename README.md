# QR2CARD - CALB Business Card Generator

[![Android APK Build](https://github.com/rogerhanzhao/QR2CARD/actions/workflows/android-apk.yml/badge.svg)](https://github.com/rogerhanzhao/QR2CARD/actions/workflows/android-apk.yml)
[![iOS Build](https://github.com/rogerhanzhao/QR2CARD/actions/workflows/ios-build.yml/badge.svg?branch=codex/ios-version)](https://github.com/rogerhanzhao/QR2CARD/actions/workflows/ios-build.yml?query=branch%3Acodex%2Fios-version)

Native Android app for generating CALB-standard business cards locally on device. The app follows the PDF prompt in this folder and uses the supplied CALB card template as the visual reference.

The experimental iOS SwiftUI implementation lives on branch `codex/ios-version` under `ios/`. It is a separate native app, not a compile-only Android target. GitHub Actions builds an iOS Simulator artifact; installable iPhone builds require Apple signing through TestFlight, App Store, Ad Hoc, or Apple Business Manager. See `ios/README.md`.

## What it does

- Single-card form for employee information.
- US/CN/international phone normalization with E.164 vCard output.
- Static vCard 3.0 QR generation with CRLF line endings.
- Preview PNG containing only the front and back card artwork, with text and layout fixed as pixels.
- Four-page print PDF at 98 mm x 62 mm with 3 mm bleed, crop marks, and downloadable print-ready card pages.
- Print PDF pages 1-2 are the front/back card artwork. Pages 3-4 are Chinese print-detail notes for paper, dimensions, bleed, colors, QR, fonts, and delivery use.
- Editable SVG (ZIP): one Illustrator-compatible SVG holding the front and back card, with editable text, vector QR squares, and embedded logo/watermark artwork. Delivered inside a ZIP (with the required fonts) because apps such as WeChat block a raw `.svg` attachment.
- CSV batch import, validation report, and ZIP export for valid rows.
- Local-only processing. No backend, account, analytics, or personal-data upload.

## Build

Open the folder in Android Studio and sync Gradle. The project targets:

- Kotlin + Jetpack Compose
- Android Gradle Plugin 8.10.1
- Gradle 8.11.1 or compatible
- JDK 17
- Minimum SDK 26

CLI build, after installing JDK 17 and Android SDK:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

## Run

1. Open `D:\QR2CARD` in Android Studio.
2. Let Android Studio sync dependencies from Google Maven and Maven Central.
3. Run the `app` configuration on an emulator or device.

## Single Card Workflow

1. Tap `Create Single Card`.
2. Enter or edit employee data.
3. Tap `Validate`.
4. Review the front/back preview.
5. Use `Preview PNG - Share...`, `Print PDF - Share...`, or `Editable SVG (ZIP) - Share...` to open the Android share sheet and send the file through installed messaging, email, or social apps.
6. Use the corresponding `Save to...` action to choose a visible local/cloud destination with the Android system file picker.

## Batch CSV

Use `samples/batch_cards.csv` as the import format:

```csv
EnglishName,FirstName,LastName,Title,CompanyLine,Department,MobileCountry,MobileNumber,Email,Website,Street,City,State,Postcode,Country
```

The app validates each row. Valid rows can be exported into a ZIP containing preview PNG, print PDF, editable SVG, VCF, QR PNG, and `batch_validation_report.csv`.
`Department` is optional; the prior `Note` column is accepted for backward-compatible imports.

## Phone Normalization

Phone numbers are parsed with Google's libphonenumber. The card displays US numbers as `+1 (213) 589-7421` and China numbers as `+86 139 6725 8941`; vCard `TEL` values always use E.164, for example `+14015927928`.

## Assets

The app uses `app/src/main/res/drawable/calb_logo.png`, copied from the provided CALB logo image. Replace that file to update the logo. Keep the replacement high resolution and transparent if possible. The current project has no original CALB AI/SVG/EPS assets; therefore the Editable SVG embeds the approved PNG logo and watermark directly in the file, so it opens correctly wherever it is moved. Supply approved vector artwork to make those two brand elements editable too.

Bundled PDF/card fonts live in `app/src/main/assets/fonts/`:

- `Manrope-Regular.otf` and `Manrope-Bold.otf` for English card text.
- `HarmonyOS_Sans_SC_Regular.ttf` for Chinese print-detail notes.

## Template JSON

Template coordinates live in `app/src/main/assets/template_config.json`. Coordinates and sizes are in millimeters. Font sizes are PDF points. The app falls back to an internal default if the JSON cannot be parsed.

## Known Limitations

- The PDF renderer uses Android `PdfDocument`, so page dimensions are rounded to whole PDF points.
- PDF text uses bundled project font files instead of Android system sans-serif to keep output typography consistent across devices.
- Print PDF is a print-production file, not an Adobe Illustrator source file. Use the Editable SVG (ZIP) for Illustrator editing; unzip it and install the bundled Manrope and HarmonyOS Sans SC fonts first so the text renders as intended.
- The Editable SVG covers the card faces only. The Chinese print-detail notes stay on pages 3-4 of the Print PDF.
- Phone numbers are not restricted to one nationality: a `+`-prefixed number (e.g. Brazil `+55 …`) is accepted regardless of the country field, and the `(XX)` card suffix is taken from the number itself.
- Advanced template editing is file-based in V1 rather than a full visual editor.
