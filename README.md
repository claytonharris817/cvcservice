# City Vending Service Report – Android

Native Android form modeled on the City Vending Co. Jotform Service Report.

## Included
- Android-friendly service report entry form
- Required Service Date/Time, Technician No. and CVC #
- Follow-up / original ticket fields
- Location, device type, complaint(s)
- Old/New Nayax serials
- Repairs made / conclusion
- Cooling-unit replacement and work-completed status
- Creates a **true 4 × 6 inch PDF**
- Saves to a selected Google Drive folder using Android's Storage Access Framework
- Files automatically into:
  - `YYYY-MM-DD/`
  - `CVC-<number>/`
  - `YYYY-MM-DD_CVC-<number>_ServiceReport.pdf`
- Opens Android Print using 4 × 6 paper, compatible with Bluetooth printers that expose an Android print service

## First run
1. Install/open the app.
2. Tap **CHOOSE GOOGLE DRIVE FOLDER**.
3. In Android's folder picker, select Google Drive.
4. Create/select a folder such as `City Vending Service Reports`.
5. Fill out a report.
6. Tap **SAVE PDF TO DRIVE + PRINT 4×6**.
7. Android will save the PDF to Drive and open the print dialog.
8. Select your paired Bluetooth printer / print service and confirm 4 × 6 paper.

## Building an APK
Open the folder in Android Studio (JDK 17). Allow Gradle sync, then:
`Build > Build APK(s)`

The APK will normally be created under:
`app/build/outputs/apk/debug/app-debug.apk`

## Bluetooth note
Android does not allow a normal app to silently print to every Bluetooth printer model. The included app uses Android's standard Print Framework. Your printer should be paired and its manufacturer's Android print service/plugin should be installed or enabled when required. This is the most broadly compatible way to preserve the 4 × 6 PDF page size.