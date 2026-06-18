# QR2CARD - CALB Business Card Generator

[![Android APK Build](https://github.com/rogerhanzhao/QR2CARD/actions/workflows/android-apk.yml/badge.svg)](https://github.com/rogerhanzhao/QR2CARD/actions/workflows/android-apk.yml)

Native Android app for generating CALB-standard business cards locally on device. The app follows the PDF prompt in this folder and uses the supplied CALB card template as the visual reference.

## What it does

- Single-card form for employee information.
- US/CN/international phone normalization with E.164 vCard output.
- Static vCard 3.0 QR generation with CRLF line endings.
- Two-page preview PDF at 92 mm x 56 mm.
- Two-page print PDF at 98 mm x 62 mm with 3 mm bleed and crop marks.
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
5. Generate preview PDF or print-ready PDF.
6. Use `Share Last Export` to send the file through the Android share sheet.

## Batch CSV

Use `samples/batch_cards.csv` as the import format:

```csv
EnglishName,FirstName,LastName,Title,CompanyLine,MobileCountry,MobileNumber,Email,Website,Street,City,State,Postcode,Country,Note
```

The app validates each row. Valid rows can be exported into a ZIP containing preview PDF, print PDF, VCF, QR PNG, and `batch_validation_report.csv`.

## Phone Normalization

Phone numbers are parsed with Google's libphonenumber. Display values may contain spaces and a country label, for example `+1 401 592 7928 (US)`. vCard `TEL` values always use E.164, for example `+14015927928`.

## Assets

The app uses `app/src/main/res/drawable/calb_logo.png`, copied from the provided CALB logo image. Replace that file to update the logo. Keep the replacement high resolution and transparent if possible.

## Template JSON

Template coordinates live in `app/src/main/assets/template_config.json`. Coordinates and sizes are in millimeters. Font sizes are PDF points. The app falls back to an internal default if the JSON cannot be parsed.

## Known Limitations

- The PDF renderer uses Android `PdfDocument`, so page dimensions are rounded to whole PDF points.
- Font embedding is not implemented; Android system sans-serif is used consistently.
- Advanced template editing is file-based in V1 rather than a full visual editor.
