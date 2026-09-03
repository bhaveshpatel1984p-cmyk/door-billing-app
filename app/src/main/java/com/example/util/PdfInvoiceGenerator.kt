package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.db.BillWithItems
import com.example.data.db.CompanyProfileEntity
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfInvoiceGenerator {

    /**
     * Generates a crisp, standard A4 PDF document for the invoice and saves it in app cache.
     * Returns the generated PDF File.
     */
    fun generateInvoicePdf(
        context: Context,
        billWithItems: BillWithItems,
        company: CompanyProfileEntity
    ): File {
        val invoiceDir = File(context.cacheDir, "invoices")
        if (!invoiceDir.exists()) {
            invoiceDir.mkdirs()
        }

        val safeInvoiceNo = billWithItems.bill.invoiceNo.replace("/", "_").replace("\\", "_").replace(" ", "_")
        val pdfFile = File(invoiceDir, "Invoice_$safeInvoiceNo.pdf")

        val document = PdfDocument()
        val pageWidth = 595 // Standard A4 width in points
        val pageHeight = 842 // Standard A4 height in points
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        drawInvoice(canvas, pageWidth.toFloat(), pageHeight.toFloat(), billWithItems, company, context)

        document.finishPage(page)

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    private fun drawInvoice(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        billWithItems: BillWithItems,
        company: CompanyProfileEntity,
        context: Context
    ) {
        val bill = billWithItems.bill
        val items = billWithItems.items
        val margin = 20f
        val contentWidth = pageWidth - (margin * 2)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#0284C7")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }

        val fillPaint = Paint().apply {
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#0F172A")
        }

        // Draw Outer Border
        canvas.drawRect(margin, margin, pageWidth - margin, pageHeight - margin, borderPaint)

        // 1. Top Title Bar
        fillPaint.color = Color.parseColor("#F0F9FF")
        canvas.drawRect(margin, margin, pageWidth - margin, margin + 26f, fillPaint)
        canvas.drawLine(margin, margin + 26f, pageWidth - margin, margin + 26f, borderPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 11f
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText("TAX INVOICE / BILL OF SUPPLY", margin + 10f, margin + 17f, textPaint)

        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 8.5f
        textPaint.color = Color.parseColor("#475569")
        val subTitle = "(Door Manufacturing & Billing)"
        canvas.drawText(subTitle, pageWidth - margin - textPaint.measureText(subTitle) - 10f, margin + 17f, textPaint)

        // 2. Company Header & Invoice Meta
        var currentY = margin + 35f

        // Try decoding company logo if available
        var logoBitmap: Bitmap? = null
        try {
            if (!company.logoUri.isNullOrBlank()) {
                val logoFile = File(company.logoUri)
                if (logoFile.exists()) {
                    logoBitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
                }
            }
            if (logoBitmap == null) {
                val defaultLogoFile = File(context.filesDir, "company_logo.png")
                if (defaultLogoFile.exists()) {
                    logoBitmap = BitmapFactory.decodeFile(defaultLogoFile.absolutePath)
                }
            }
            if (logoBitmap == null) {
                logoBitmap = BitmapFactory.decodeResource(context.resources, com.example.R.drawable.img_nirmal_door_logo)
            }
        } catch (e: Exception) {
            logoBitmap = null
        }

        var textStartX = margin + 12f
        if (logoBitmap != null) {
            val logoSize = 48f
            val destRect = RectF(textStartX, currentY, textStartX + logoSize, currentY + logoSize)
            canvas.drawBitmap(logoBitmap, null, destRect, null)
            textStartX += logoSize + 10f
        }

        // Business Name
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 15f
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText(company.businessName.uppercase(Locale.getDefault()), textStartX, currentY + 12f, textPaint)

        // Address & Contacts
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 8f
        textPaint.color = Color.parseColor("#334155")
        canvas.drawText(company.address, textStartX, currentY + 23f, textPaint)

        val taxInfo = "GSTIN: ${company.gstNo} | PAN: ${company.pan}"
        canvas.drawText(taxInfo, textStartX, currentY + 33f, textPaint)

        // Email & Mobile (Requirement 3: email printed clearly)
        val contactInfo = buildString {
            append("Mobile: ${company.mobile}")
            if (company.email.isNotBlank()) {
                append(" | Email: ${company.email}")
            }
            append(" | State: ${company.state} (${company.stateCode})")
        }
        canvas.drawText(contactInfo, textStartX, currentY + 43f, textPaint)

        // Invoice Meta on Right Side
        val metaStartX = pageWidth - margin - 150f
        fillPaint.color = Color.parseColor("#F8FAFC")
        canvas.drawRect(metaStartX, margin + 26f, pageWidth - margin, margin + 85f, fillPaint)
        canvas.drawLine(metaStartX, margin + 26f, metaStartX, margin + 85f, linePaint)

        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.DEFAULT
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("Invoice No:", metaStartX + 8f, margin + 42f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#0284C7")
        canvas.drawText(bill.invoiceNo, metaStartX + 60f, margin + 42f, textPaint)

        textPaint.typeface = Typeface.DEFAULT
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("Date:", metaStartX + 8f, margin + 56f, textPaint)
        textPaint.color = Color.parseColor("#0F172A")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(DimensionCalculator.formatDate(bill.dateMillis), metaStartX + 60f, margin + 56f, textPaint)

        textPaint.typeface = Typeface.DEFAULT
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("Unit:", metaStartX + 8f, margin + 70f, textPaint)
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText("${bill.dimensionUnit} (Door)", metaStartX + 60f, margin + 70f, textPaint)

        currentY = margin + 85f
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, borderPaint)

        // 3. Customer Box & Summary
        fillPaint.color = Color.parseColor("#FFFFFF")
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 54f, fillPaint)

        val custMidX = margin + (contentWidth * 0.62f)
        canvas.drawLine(custMidX, currentY, custMidX, currentY + 54f, linePaint)

        // Left: Billed To
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText("BILLED TO (CUSTOMER):", margin + 8f, currentY + 12f, textPaint)

        textPaint.textSize = 10.5f
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText(bill.customerName, margin + 8f, currentY + 24f, textPaint)

        textPaint.textSize = 8f
        textPaint.typeface = Typeface.DEFAULT
        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("Address: ${bill.customerAddress.ifBlank { "N/A" }}", margin + 8f, currentY + 35f, textPaint)
        val custContact = "Mobile: ${bill.customerMobile.ifBlank { "N/A" }} | GSTIN: ${bill.customerGstNo.ifBlank { "Unregistered" }}"
        canvas.drawText(custContact, margin + 8f, currentY + 46f, textPaint)

        // Right: Delivery & Totals Summary
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText("SUMMARY:", custMidX + 8f, currentY + 12f, textPaint)

        val totalQty = items.sumOf { it.qty }
        val totalSqFt = items.sumOf { it.sqFt }

        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.DEFAULT
        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("Total Doors: $totalQty Pcs", custMidX + 8f, currentY + 25f, textPaint)
        canvas.drawText("Total Area: ${String.format(Locale.US, "%.2f", totalSqFt)} Sq.Ft", custMidX + 8f, currentY + 37f, textPaint)

        val payStatus = if (bill.paidAmount >= bill.grandTotal) "PAID" else if (bill.paidAmount > 0) "PARTIAL" else "UNPAID"
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = if (payStatus == "PAID") Color.parseColor("#16A34A") else Color.parseColor("#D97706")
        canvas.drawText("Status: $payStatus", custMidX + 8f, currentY + 49f, textPaint)

        currentY += 54f
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, borderPaint)

        // 4. Items Table
        val colSl = margin
        val colParticular = margin + 24f
        val colHsn = margin + 175f
        val colHigh = margin + 225f
        val colWidth = margin + 270f
        val colQty = margin + 315f
        val colSqFt = margin + 355f
        val colRate = margin + 410f
        val colAmount = margin + 475f
        val colEnd = pageWidth - margin

        val headerHeight = 20f
        fillPaint.color = Color.parseColor("#0284C7")
        canvas.drawRect(margin, currentY, colEnd, currentY + headerHeight, fillPaint)

        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.WHITE

        canvas.drawText("Sl", colSl + 6f, currentY + 13f, textPaint)
        canvas.drawText("Particular (Door Type)", colParticular + 4f, currentY + 13f, textPaint)
        canvas.drawText("HSN", colHsn + 4f, currentY + 13f, textPaint)
        val unitShort = bill.dimensionUnit.take(2)
        canvas.drawText("H($unitShort)", colHigh + 4f, currentY + 13f, textPaint)
        canvas.drawText("W($unitShort)", colWidth + 4f, currentY + 13f, textPaint)
        canvas.drawText("Qty", colQty + 4f, currentY + 13f, textPaint)
        canvas.drawText("Sq.Ft", colSqFt + 4f, currentY + 13f, textPaint)
        canvas.drawText("Rate", colRate + 4f, currentY + 13f, textPaint)
        canvas.drawText("Amount", colAmount + 4f, currentY + 13f, textPaint)

        currentY += headerHeight

        // Item Rows
        val rowHeight = 18f
        items.forEachIndexed { index, item ->
            if (index % 2 == 1) {
                fillPaint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(margin, currentY, colEnd, currentY + rowHeight, fillPaint)
            }

            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 8f
            textPaint.color = Color.parseColor("#0F172A")

            canvas.drawText("${item.slNo}", colSl + 6f, currentY + 12f, textPaint)

            val partText = if (item.particular.length > 25) item.particular.take(23) + ".." else item.particular
            canvas.drawText(partText, colParticular + 4f, currentY + 12f, textPaint)

            canvas.drawText(item.hsnSac, colHsn + 4f, currentY + 12f, textPaint)
            canvas.drawText(DimensionCalculator.formatDimension(item.height), colHigh + 4f, currentY + 12f, textPaint)
            canvas.drawText(DimensionCalculator.formatDimension(item.width), colWidth + 4f, currentY + 12f, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${item.qty}", colQty + 8f, currentY + 12f, textPaint)

            canvas.drawText(String.format(Locale.US, "%.2f", item.sqFt), colSqFt + 4f, currentY + 12f, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.rate), colRate + 4f, currentY + 12f, textPaint)
            canvas.drawText("₹${String.format(Locale.US, "%.2f", item.amount)}", colAmount + 4f, currentY + 12f, textPaint)

            canvas.drawLine(margin, currentY + rowHeight, colEnd, currentY + rowHeight, linePaint)
            currentY += rowHeight
        }

        // Draw vertical column separators for items
        val tableBottomY = currentY
        val tableTopY = currentY - (items.size * rowHeight) - headerHeight
        canvas.drawLine(colParticular, tableTopY, colParticular, tableBottomY, linePaint)
        canvas.drawLine(colHsn, tableTopY, colHsn, tableBottomY, linePaint)
        canvas.drawLine(colHigh, tableTopY, colHigh, tableBottomY, linePaint)
        canvas.drawLine(colWidth, tableTopY, colWidth, tableBottomY, linePaint)
        canvas.drawLine(colQty, tableTopY, colQty, tableBottomY, linePaint)
        canvas.drawLine(colSqFt, tableTopY, colSqFt, tableBottomY, linePaint)
        canvas.drawLine(colRate, tableTopY, colRate, tableBottomY, linePaint)
        canvas.drawLine(colAmount, tableTopY, colAmount, tableBottomY, linePaint)

        // Sub Total Row
        fillPaint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(margin, currentY, colEnd, currentY + 20f, fillPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 8.5f
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText("Sub Total (${totalQty} Qty | ${String.format(Locale.US, "%.2f", totalSqFt)} Sq.Ft):", margin + 12f, currentY + 14f, textPaint)

        val subTotalStr = "₹${String.format(Locale.US, "%.2f", bill.subTotal)}"
        canvas.drawText(subTotalStr, colAmount + 4f, currentY + 14f, textPaint)

        currentY += 20f
        canvas.drawLine(margin, currentY, colEnd, currentY, linePaint)

        // GST Breakdown (if applicable)
        if (bill.isGstIncluded && bill.taxRate > 0) {
            val halfRate = bill.taxRate / 2.0
            val cgstStr = "₹${String.format(Locale.US, "%.2f", bill.cgstAmount)}"
            val sgstStr = "₹${String.format(Locale.US, "%.2f", bill.sgstAmount)}"

            canvas.drawText("CGST ($halfRate%):", colRate - 65f, currentY + 13f, textPaint)
            canvas.drawText(cgstStr, colAmount + 4f, currentY + 13f, textPaint)
            currentY += 16f
            canvas.drawLine(margin, currentY, colEnd, currentY, linePaint)

            canvas.drawText("SGST ($halfRate%):", colRate - 65f, currentY + 13f, textPaint)
            canvas.drawText(sgstStr, colAmount + 4f, currentY + 13f, textPaint)
            currentY += 16f
            canvas.drawLine(margin, currentY, colEnd, currentY, linePaint)
        }

        // Discount (if applicable)
        if (bill.discountAmount > 0) {
            textPaint.color = Color.parseColor("#DC2626")
            canvas.drawText("Discount:", colRate - 65f, currentY + 13f, textPaint)
            canvas.drawText("- ₹${String.format(Locale.US, "%.2f", bill.discountAmount)}", colAmount + 4f, currentY + 13f, textPaint)
            currentY += 16f
            canvas.drawLine(margin, currentY, colEnd, currentY, linePaint)
        }

        // GRAND TOTAL Highlight Row
        fillPaint.color = Color.parseColor("#0284C7")
        canvas.drawRect(margin, currentY, colEnd, currentY + 24f, fillPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 10f
        textPaint.color = Color.WHITE
        canvas.drawText("GRAND TOTAL:", margin + 12f, currentY + 16f, textPaint)

        val grandTotalStr = "₹${String.format(Locale.US, "%.2f", bill.grandTotal)}"
        canvas.drawText(grandTotalStr, colAmount + 4f, currentY + 16f, textPaint)

        currentY += 24f
        canvas.drawLine(margin, currentY, colEnd, currentY, borderPaint)

        // Amount in words
        fillPaint.color = Color.parseColor("#F8FAFC")
        canvas.drawRect(margin, currentY, colEnd, currentY + 18f, fillPaint)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 8f
        textPaint.color = Color.parseColor("#334155")
        val inWords = "Amount in Words: " + DimensionCalculator.convertToIndianCurrencyWords(bill.grandTotal)
        canvas.drawText(inWords, margin + 8f, currentY + 12f, textPaint)

        currentY += 18f
        canvas.drawLine(margin, currentY, colEnd, currentY, linePaint)

        // Bank Details & Declaration
        val termsHeight = 70f
        val bankWidth = contentWidth * 0.58f

        // Bank Details Box
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 8f
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText("BANK ACCOUNT DETAILS:", margin + 8f, currentY + 13f, textPaint)

        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#1E293B")
        canvas.drawText("Bank: ${company.bankName} | A/C: ${company.accountNo}", margin + 8f, currentY + 25f, textPaint)
        canvas.drawText("IFSC Code: ${company.ifscCode}", margin + 8f, currentY + 36f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText("DECLARATION:", margin + 8f, currentY + 48f, textPaint)

        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 6.5f
        textPaint.color = Color.parseColor("#64748B")
        val declShort = if (company.declaration.length > 130) company.declaration.take(125) + "..." else company.declaration
        canvas.drawText(declShort, margin + 8f, currentY + 58f, textPaint)

        // Signature Box
        canvas.drawLine(margin + bankWidth, currentY, margin + bankWidth, currentY + termsHeight, linePaint)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#475569")
        val forText = "For ${company.businessName}"
        canvas.drawText(forText, margin + bankWidth + 12f, currentY + 18f, textPaint)

        canvas.drawLine(margin + bankWidth + 12f, currentY + 52f, colEnd - 12f, currentY + 52f, linePaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText("AUTHORIZED SIGNATORY", margin + bankWidth + 16f, currentY + 62f, textPaint)

        currentY += termsHeight
        canvas.drawLine(margin, currentY, colEnd, currentY, borderPaint)

        // 5. REQUIREMENT 4: JURISDICTION AT THE BOTTOM END
        val jurisdictionBannerHeight = 22f
        fillPaint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(margin, pageHeight - margin - jurisdictionBannerHeight, colEnd, pageHeight - margin, fillPaint)
        canvas.drawLine(margin, pageHeight - margin - jurisdictionBannerHeight, colEnd, pageHeight - margin - jurisdictionBannerHeight, linePaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 8.5f
        textPaint.color = Color.parseColor("#1E293B")
        val jurisdictionText = "SUBJECT TO ${company.jurisdiction.ifBlank { "LOCAL" }.uppercase(Locale.getDefault())} JURISDICTION"
        val jWidth = textPaint.measureText(jurisdictionText)
        canvas.drawText(jurisdictionText, (pageWidth - jWidth) / 2f, pageHeight - margin - 8f, textPaint)
    }
}
