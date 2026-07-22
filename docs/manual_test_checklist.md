# Manual QA Checklist

- Generate the default Alex Zhao card.
- Scan the generated QR with iPhone Camera.
- Scan the generated QR with Samsung Camera.
- Scan the generated QR with WeChat.
- Confirm the contact save page shows name, company, title, mobile, email, address, and website.
- Confirm the US phone appears in the vCard as `+14015927928`.
- Generate preview PNG and confirm it contains only front and back card artwork.
- Generate print PDF and confirm it has four pages at 98 mm x 62 mm with 3 mm bleed.
- Generate the Editable SVG (ZIP), confirm it shares through WeChat/email, then unzip it, install the bundled Manrope and HarmonyOS Sans SC fonts, and open the `.svg` in Adobe Illustrator.
- Enter a non-US/CN number such as Brazil `+55 11 98765-4321`, and confirm it validates, generates the QR, and shows the `(BR)` suffix on the card.
- Confirm it is one file containing the front card above the back card, and that the logo sits in the top-left corner at the correct size.
- Confirm the employee text and QR are editable vectors.
- Move the `.svg` to another folder, reopen it, and confirm the logo and watermark still render (they are embedded, not linked).
- Open the generated preview PNG on at least two devices and confirm card text/layout is fixed.
- Confirm the print PDF specification page lists Manrope and HarmonyOS Sans SC font information.
- Print a physical sample at actual size and scan the QR.
- Import `samples/batch_cards.csv`, review validation results, and export the ZIP.
