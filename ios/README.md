# QR2CARD iOS

This branch contains an iOS SwiftUI implementation of the CALB business card generator. It is separate from the Android app because the current Android app is native Kotlin/Compose, so iOS is not a compile-only target.

## Scope

- Native SwiftUI form for one business card.
- Uses the same CALB logo, watermark, card dimensions, template coordinates, colors, and bundled fonts as the Android version.
- Generates a preview PNG containing only the front and back card artwork.
- Generates a four-page print PDF: front, back, print notes, and color/output notes.
- Print PDF notes include font file information:
  - `Manrope-Regular.otf` and `Manrope-Bold.otf` for English card text.
  - `HarmonyOS_Sans_SC_Regular.ttf` for Chinese notes.

## GitHub Build

The workflow `.github/workflows/ios-build.yml` runs on GitHub macOS runners:

1. Installs XcodeGen.
2. Generates `QR2CARDiOS.xcodeproj` from `ios/project.yml`.
3. Builds a Release iOS Simulator app with code signing disabled.
4. Uploads `QR2CARD-iOS-simulator-app` as a workflow artifact.

The uploaded simulator app is useful for CI validation and simulator testing. It cannot be installed on a normal iPhone.

## Local Build On macOS

```bash
cd ios
brew install xcodegen
xcodegen generate --spec project.yml
xcodebuild \
  -project QR2CARDiOS.xcodeproj \
  -scheme QR2CARDiOS \
  -configuration Release \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

## How iOS Users Can Install

Ordinary iOS users can only install apps that are signed with an Apple Developer account. GitHub cannot produce a user-installable iPhone build unless the repository is configured with Apple signing credentials.

Recommended paths:

1. TestFlight for internal and external testers.
   - Enroll in the Apple Developer Program.
   - Create an App ID for `com.calb.qr2card.ios`.
   - Create signing certificates and provisioning profiles.
   - Archive the app in Xcode or in GitHub Actions with signing secrets.
   - Upload the archive to App Store Connect.
   - Add testers in TestFlight and send invitations.

2. App Store for public distribution.
   - Complete the TestFlight/App Store Connect setup.
   - Add app privacy, screenshots, support URL, and review metadata.
   - Submit the app for Apple review.

3. Ad Hoc for a small known device list.
   - Register each iPhone UDID in the Apple Developer portal.
   - Build a signed IPA with an Ad Hoc provisioning profile.
   - Distribute through Apple Configurator, MDM, or a compliant internal channel.

4. Apple Business Manager / Custom App for business-only distribution.
   - Publish as a Custom App in App Store Connect.
   - Assign it to the target organization in Apple Business Manager.

Enterprise distribution is only available to organizations approved for the Apple Developer Enterprise Program and should be used only for internal employees.

## Signed IPA In GitHub Actions

To add a signed IPA workflow later, configure repository secrets for:

- App Store Connect API key.
- Apple Team ID.
- Distribution certificate, password, and provisioning profile.
- Bundle ID `com.calb.qr2card.ios`.

Then add an archive/export workflow that runs on `macos-latest` and uploads either to TestFlight or as a signed internal artifact.
