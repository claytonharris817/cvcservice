package net.cityvending.servicereport

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.print.*
import android.provider.DocumentsContract
import android.view.View
import android.widget.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private val REQ_TREE = 1001
    private val prefs by lazy { getSharedPreferences("cvc_service_report", Context.MODE_PRIVATE) }

    private lateinit var serviceRequest: EditText
    private lateinit var serviceDate: EditText
    private lateinit var serviceTime: EditText
    private lateinit var technician: EditText
    private lateinit var cvcNumber: EditText
    private lateinit var followUp: CheckBox
    private lateinit var originalTicket: EditText
    private lateinit var location: EditText
    private lateinit var deviceType: Spinner
    private lateinit var complaints: MultiAutoCompleteTextView
    private lateinit var oldNayax: EditText
    private lateinit var newNayax: EditText
    private lateinit var repairs: EditText
    private lateinit var coolingUnit: Spinner
    private lateinit var workCompleted: Spinner
    private lateinit var driveStatus: TextView

    private val deviceTypes = arrayOf(
        "Select device type",
        "Snack Vending Machine",
        "Drink Vending Machine",
        "Combo Vending Machine",
        "Nayax / Card Reader",
        "Bill Acceptor",
        "Coin Mechanism",
        "Gaming Device",
        "Other"
    )

    private val complaintChoices = arrayOf(
        "FALSE VEND / NOT VENDING",
        "BREACH (jammed currency/product/debris)",
        "PRICING CONFLICT",
        "NAYAX/DEX connectivity",
        "HOT OR FROZEN PRODUCT",
        "REFUND DUE",
        "PAYMENT DEVICE ISSUE (coin, bill or CC)",
        "LOCKING MECHANISM",
        "LIGHT BULB",
        "NO POWER / ELECTRICAL",
        "UNKNOWN",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        setDefaultDateTime()
        updateDriveStatus()
    }

    private fun buildUi() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(24))
        }

        val header = TextView(this).apply {
            text = "CITY VENDING CO.\nSERVICE REPORT"
            textSize = 25f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(13, 71, 161))
            setPadding(0, 0, 0, dp(12))
        }
        outer.addView(header, matchWrap())

        serviceRequest = addField(outer, "SERVICE REQUEST #")
        serviceDate = addField(outer, "SERVICE DATE (MM/DD/YYYY) *")
        serviceTime = addField(outer, "SERVICE TIME *")
        technician = addField(outer, "TECHNICIAN NO. *")
        cvcNumber = addField(outer, "CVC # NUMBER *")

        followUp = CheckBox(this).apply {
            text = "Follow-up to a previous work order"
            textSize = 16f
            setPadding(0, dp(6), 0, dp(4))
        }
        outer.addView(followUp, matchWrap())

        originalTicket = addField(outer, "ORIGINAL TICKET #")
        location = addField(outer, "DEVICE / CUSTOMER LOCATION")

        addLabel(outer, "DEVICE TYPE *")
        deviceType = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, deviceTypes)
        }
        outer.addView(deviceType, matchWrap())

        addLabel(outer, "COMPLAINT(S)")
        complaints = MultiAutoCompleteTextView(this).apply {
            hint = "Choose/type complaints; separate with commas"
            setAdapter(ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, complaintChoices))
            setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
            minLines = 2
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        outer.addView(complaints, matchWrap())

        oldNayax = addField(outer, "OLD NAYAX SERIAL #")
        newNayax = addField(outer, "NEW NAYAX SERIAL #")

        addLabel(outer, "REPAIRS MADE")
        repairs = EditText(this).apply {
            minLines = 4
            gravity = Gravity.TOP
            hint = "Describe diagnosis, parts, adjustments, testing and conclusion"
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        outer.addView(repairs, matchWrap())

        addLabel(outer, "REPLACEMENT COOLING UNIT REQUIRED? *")
        coolingUnit = spinner(arrayOf("Select", "YES", "NO"))
        outer.addView(coolingUnit, matchWrap())

        addLabel(outer, "WORK COMPLETED?")
        workCompleted = spinner(arrayOf("Select", "YES", "NO - FOLLOW UP REQUIRED"))
        outer.addView(workCompleted, matchWrap())

        val divider = View(this).apply { setBackgroundColor(Color.LTGRAY) }
        outer.addView(divider, LinearLayout.LayoutParams(-1, dp(1)).apply {
            topMargin = dp(18); bottomMargin = dp(14)
        })

        driveStatus = TextView(this).apply {
            textSize = 14f
            setPadding(0, 0, 0, dp(8))
        }
        outer.addView(driveStatus, matchWrap())

        val chooseDrive = Button(this).apply {
            text = "CHOOSE GOOGLE DRIVE FOLDER"
            setOnClickListener { chooseDriveFolder() }
        }
        outer.addView(chooseDrive, matchWrap())

        val exportPrint = Button(this).apply {
            text = "SAVE PDF TO DRIVE + PRINT 4×6"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setOnClickListener { exportAndPrint() }
        }
        outer.addView(exportPrint, matchWrap())

        val pdfOnly = Button(this).apply {
            text = "SAVE PDF TO DRIVE ONLY"
            setOnClickListener { saveOnly() }
        }
        outer.addView(pdfOnly, matchWrap())

        val printOnly = Button(this).apply {
            text = "PRINT 4×6 ONLY"
            setOnClickListener { printOnly() }
        }
        outer.addView(printOnly, matchWrap())

        val clear = Button(this).apply {
            text = "CLEAR FORM"
            setOnClickListener { clearForm() }
        }
        outer.addView(clear, matchWrap())

        val note = TextView(this).apply {
            text = "PDF filing: selected Drive folder / YYYY-MM-DD / CVC-<number> / YYYY-MM-DD_CVC-<number>_ServiceReport.pdf\n\nPrinting uses Android's print service with a 4×6-inch page. Pair your Bluetooth printer and install/enable its Android print service if required."
            textSize = 13f
            setPadding(0, dp(12), 0, 0)
        }
        outer.addView(note, matchWrap())

        val scroll = ScrollView(this).apply { addView(outer) }
        setContentView(scroll)
    }

    private fun addLabel(parent: LinearLayout, text: String) {
        val label = TextView(this).apply {
            this.text = text
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(12), 0, dp(3))
        }
        parent.addView(label, matchWrap())
    }

    private fun addField(parent: LinearLayout, label: String): EditText {
        addLabel(parent, label)
        val e = EditText(this).apply {
            setPadding(dp(10), dp(9), dp(10), dp(9))
            singleLine = true
        }
        parent.addView(e, matchWrap())
        return e
    }

    private fun spinner(items: Array<String>): Spinner =
        Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items)
        }

    private fun setDefaultDateTime() {
        val now = Date()
        serviceDate.setText(SimpleDateFormat("MM/dd/yyyy", Locale.US).format(now))
        serviceTime.setText(SimpleDateFormat("hh:mm a", Locale.US).format(now))
    }

    private fun chooseDriveFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }
        startActivityForResult(intent, REQ_TREE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_TREE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            try {
                contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}
            prefs.edit().putString("drive_tree", uri.toString()).apply()
            updateDriveStatus()
            Toast.makeText(this, "Drive folder saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateDriveStatus() {
        val saved = prefs.getString("drive_tree", null)
        driveStatus.text = if (saved == null)
            "Google Drive folder: NOT SET"
        else
            "Google Drive folder: READY"
    }

    private fun validateRequired(): Boolean {
        if (serviceDate.text.isNullOrBlank() || serviceTime.text.isNullOrBlank() ||
            technician.text.isNullOrBlank() || cvcNumber.text.isNullOrBlank()) {
            AlertDialog.Builder(this)
                .setTitle("Missing information")
                .setMessage("Please enter service date/time, technician number, and CVC number.")
                .setPositiveButton("OK", null)
                .show()
            return false
        }
        return true
    }

    private fun exportAndPrint() {
        if (!validateRequired()) return
        val file = buildPdfToCache() ?: return
        val saved = savePdfToDrive(file)
        if (saved) {
            Toast.makeText(this, "PDF saved to Google Drive", Toast.LENGTH_LONG).show()
        }
        printPdf(file)
    }

    private fun saveOnly() {
        if (!validateRequired()) return
        val file = buildPdfToCache() ?: return
        if (savePdfToDrive(file)) {
            Toast.makeText(this, "PDF saved to Google Drive", Toast.LENGTH_LONG).show()
        }
    }

    private fun printOnly() {
        if (!validateRequired()) return
        val file = buildPdfToCache() ?: return
        printPdf(file)
    }

    private fun buildPdfToCache(): File? {
        val pdf = PdfDocument()
        // PDF point dimensions: 4in x 6in at 72 points/inch.
        val width = 288
        val height = 432
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = pdf.startPage(pageInfo)
        val c = page.canvas

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(13, 71, 161)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 7.2f
            typeface = Typeface.DEFAULT
        }
        val bold = Paint(small).apply { typeface = Typeface.DEFAULT_BOLD }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            strokeWidth = 0.7f
        }

        c.drawText("CITY VENDING CO. SERVICE REPORT", width / 2f, 18f, title)
        c.drawLine(10f, 24f, width - 10f, 24f, line)

        var y = 36f
        fun row(label: String, value: String, maxChars: Int = 45) {
            if (y > height - 18) return
            c.drawText(label, 10f, y, bold)
            val lx = 104f
            val clipped = value.replace("\n", " ").take(maxChars)
            c.drawText(clipped, lx, y, small)
            y += 12f
        }

        row("Service Request #:", serviceRequest.text.toString())
        row("Service Date / Time:", "${serviceDate.text}  ${serviceTime.text}")
        row("Technician No.:", technician.text.toString())
        row("CVC #:", cvcNumber.text.toString())
        row("Follow-up:", if (followUp.isChecked) "YES" else "NO")
        if (followUp.isChecked) row("Original Ticket #:", originalTicket.text.toString())
        row("Location:", location.text.toString(), 42)
        row("Device Type:", deviceType.selectedItem?.toString() ?: "")
        row("Old Nayax Serial #:", oldNayax.text.toString())
        row("New Nayax Serial #:", newNayax.text.toString())
        row("Cooling Unit:", coolingUnit.selectedItem?.toString() ?: "")
        row("Work Completed:", workCompleted.selectedItem?.toString() ?: "")

        fun wrappedBlock(label: String, text: String, maxLines: Int) {
            if (y > height - 24) return
            c.drawText(label, 10f, y, bold)
            y += 9f
            val lines = wrapText(text, 72).take(maxLines)
            for (s in lines) {
                c.drawText(s, 12f, y, small)
                y += 9f
            }
            y += 3f
        }

        wrappedBlock("Complaint(s):", complaints.text.toString(), 4)
        wrappedBlock("Repairs Made / Conclusion:", repairs.text.toString(), 8)

        val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 6.2f
            textAlign = Paint.Align.CENTER
        }
        c.drawText("City Vending Co. • CVC ${cvcNumber.text} • ${serviceDate.text}", width / 2f, height - 8f, footer)

        pdf.finishPage(page)

        val safeCvc = sanitize(cvcNumber.text.toString())
        val iso = isoDate()
        val file = File(cacheDir, "${iso}_CVC-${safeCvc}_ServiceReport.pdf")
        return try {
            FileOutputStream(file).use { pdf.writeTo(it) }
            pdf.close()
            file
        } catch (e: Exception) {
            pdf.close()
            Toast.makeText(this, "Could not create PDF: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun wrapText(text: String, maxChars: Int): List<String> {
        val words = text.replace("\n", " ").trim().split(Regex("\\s+"))
        if (words.isEmpty()) return listOf("")
        val out = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            if (current.length + word.length + 1 > maxChars) {
                out.add(current.toString())
                current = StringBuilder()
            }
            if (current.isNotEmpty()) current.append(" ")
            current.append(word)
        }
        if (current.isNotEmpty()) out.add(current.toString())
        return out
    }

    private fun savePdfToDrive(source: File): Boolean {
        val treeString = prefs.getString("drive_tree", null)
        if (treeString == null) {
            AlertDialog.Builder(this)
                .setTitle("Choose Google Drive folder")
                .setMessage("Tap CHOOSE GOOGLE DRIVE FOLDER first, then select or create a folder such as “City Vending Service Reports” in Google Drive.")
                .setPositiveButton("Choose folder") { _, _ -> chooseDriveFolder() }
                .setNegativeButton("Cancel", null)
                .show()
            return false
        }

        return try {
            val treeUri = Uri.parse(treeString)
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)

            val dateFolderName = isoDate()
            val cvcFolderName = "CVC-${sanitize(cvcNumber.text.toString())}"

            val dateFolder = findOrCreateDirectory(treeUri, rootDocUri, dateFolderName)
                ?: throw IOException("Could not create date folder")
            val cvcFolder = findOrCreateDirectory(treeUri, dateFolder, cvcFolderName)
                ?: throw IOException("Could not create CVC folder")

            val filename = "${dateFolderName}_${cvcFolderName}_ServiceReport.pdf"
            val existing = findChildByName(treeUri, cvcFolder, filename)
            if (existing != null) {
                try { DocumentsContract.deleteDocument(contentResolver, existing) } catch (_: Exception) {}
            }
            val dest = DocumentsContract.createDocument(
                contentResolver, cvcFolder, "application/pdf", filename
            ) ?: throw IOException("Could not create PDF in Drive")

            contentResolver.openOutputStream(dest, "w")!!.use { out ->
                FileInputStream(source).use { it.copyTo(out) }
            }
            true
        } catch (e: Exception) {
            Toast.makeText(this, "Drive save failed: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun findOrCreateDirectory(treeUri: Uri, parentUri: Uri, name: String): Uri? {
        val existing = findChildByName(treeUri, parentUri, name)
        if (existing != null) return existing
        return DocumentsContract.createDocument(
            contentResolver,
            parentUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            name
        )
    }

    private fun findChildByName(treeUri: Uri, parentUri: Uri, name: String): Uri? {
        val parentId = DocumentsContract.getDocumentId(parentUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == name) {
                    return DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(idIndex))
                }
            }
        }
        return null
    }

    private fun printPdf(file: File) {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val attrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize("CVC_4X6", "4 x 6", 4000, 6000))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()

        printManager.print(
            "CVC ${cvcNumber.text} Service Report",
            PdfFilePrintAdapter(file),
            attrs
        )
    }

    inner class PdfFilePrintAdapter(private val file: File) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: android.os.CancellationSignal,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal.isCanceled) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder(file.name)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: android.os.ParcelFileDescriptor,
            cancellationSignal: android.os.CancellationSignal,
            callback: WriteResultCallback
        ) {
            try {
                FileInputStream(file).use { input ->
                    FileOutputStream(destination.fileDescriptor).use { output ->
                        input.copyTo(output)
                    }
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }
    }

    private fun isoDate(): String {
        return try {
            val parsed = SimpleDateFormat("MM/dd/yyyy", Locale.US).parse(serviceDate.text.toString())
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsed ?: Date())
        } catch (_: Exception) {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
    }

    private fun sanitize(s: String): String =
        s.trim().replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "UNKNOWN" }

    private fun clearForm() {
        serviceRequest.text.clear()
        technician.text.clear()
        cvcNumber.text.clear()
        followUp.isChecked = false
        originalTicket.text.clear()
        location.text.clear()
        deviceType.setSelection(0)
        complaints.text.clear()
        oldNayax.text.clear()
        newNayax.text.clear()
        repairs.text.clear()
        coolingUnit.setSelection(0)
        workCompleted.setSelection(0)
        setDefaultDateTime()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}