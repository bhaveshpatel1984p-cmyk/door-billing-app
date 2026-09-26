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
import com.example.data.db.CustomerEntity
import com.example.data.db.LedgerEntry
import com.example.data.db.PaymentEntity
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
        val titleBarHeight = 22f
        fillPaint.color = Color.parseColor("#DCEEF8")
        canvas.drawRect(margin, margin, pageWidth - margin, margin + titleBarHeight, fillPaint)
        canvas.drawLine(margin, margin + titleBarHeight, pageWidth - margin, margin + titleBarHeight, borderPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 10.5f
        textPaint.color = Color.parseColor("#0369A1")
        val titleText = if (bill.isQuotation) "ESTIMATE / QUOTATION" else "TAX INVOICE / BILL OF SUPPLY"
        canvas.drawText(titleText, margin + 10f, margin + 15f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        textPaint.textSize = 8f
        textPaint.color = Color.parseColor("#475569")
        val subTitle = "(Door Manufacturing & Joinery Billing)"
        val subTitleWidth = textPaint.measureText(subTitle)
        canvas.drawText(subTitle, pageWidth - margin - subTitleWidth - 10f, margin + 15f, textPaint)

        // 2. Company Header & Invoice Meta Row
        var currentY = margin + titleBarHeight
        val colSplitX = margin + (contentWidth * 0.62f)
        val row1Height = 84f

        // Draw backgrounds for Row 1
        fillPaint.color = Color.parseColor("#EEF5F9")
        canvas.drawRect(margin, currentY, colSplitX, currentY + row1Height, fillPaint)
        fillPaint.color = Color.parseColor("#D3E3ED")
        canvas.drawRect(colSplitX, currentY, pageWidth - margin, currentY + row1Height, fillPaint)

        // Vertical divider between company and invoice details
        canvas.drawLine(colSplitX, currentY, colSplitX, currentY + row1Height, borderPaint)

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

        var textStartX = margin + 10f
        if (logoBitmap != null) {
            val logoSize = 48f
            val destRect = RectF(textStartX, currentY + 10f, textStartX + logoSize, currentY + 10f + logoSize)
            canvas.drawBitmap(logoBitmap, null, destRect, null)
            textStartX += logoSize + 8f
        }

        // Business Name
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 13f
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText(company.businessName.uppercase(Locale.getDefault()), textStartX, currentY + 14f, textPaint)

        // Address & Contacts (Reference format: Name -> Address -> GSTIN/UIN -> Contact -> E-Mail -> State Name)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#334155")
        var leftY = currentY + 24f
        canvas.drawText(company.address, textStartX, leftY, textPaint)
        if (company.addressLine2.isNotBlank()) {
            leftY += 9f
            canvas.drawText(company.addressLine2, textStartX, leftY, textPaint)
        }

        leftY += 9.5f
        val taxInfo = "GSTIN/UIN: ${company.gstNo}${if (company.pan.isNotBlank()) " | PAN: ${company.pan}" else ""}"
        canvas.drawText(taxInfo, textStartX, leftY, textPaint)

        leftY += 9.5f
        canvas.drawText("Contact : ${company.displayMobile}", textStartX, leftY, textPaint)

        leftY += 9.5f
        canvas.drawText("E-Mail : ${company.email.ifBlank { "N/A" }}", textStartX, leftY, textPaint)

        leftY += 9.5f
        canvas.drawText("State Name : ${company.state}${if (company.stateCode.isNotBlank()) ", Code : ${company.stateCode}" else ""}", textStartX, leftY, textPaint)

        // Invoice Meta on Right Side (colSplitX to end)
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#334155")
        canvas.drawText(if (bill.isQuotation) "Quote No:" else "Invoice No:", colSplitX + 8f, currentY + 16f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#0284C7")
        textPaint.textSize = 9.5f
        canvas.drawText(bill.invoiceNo, colSplitX + 58f, currentY + 16f, textPaint)

        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("Date:", colSplitX + 8f, currentY + 32f, textPaint)
        textPaint.color = Color.parseColor("#0F172A")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(DimensionCalculator.formatDate(bill.dateMillis), colSplitX + 58f, currentY + 32f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("Unit:", colSplitX + 8f, currentY + 48f, textPaint)
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText("${bill.dimensionUnit} Dimension", colSplitX + 58f, currentY + 48f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("Place of Supply:", colSplitX + 8f, currentY + 64f, textPaint)
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText(company.state, colSplitX + 75f, currentY + 64f, textPaint)

        currentY += row1Height
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, borderPaint)

        // 3. Customer Box & Summary Row
        val row2Height = 58f
        fillPaint.color = Color.parseColor("#EEF5F9")
        canvas.drawRect(margin, currentY, colSplitX, currentY + row2Height, fillPaint)
        fillPaint.color = Color.parseColor("#D3E3ED")
        canvas.drawRect(colSplitX, currentY, pageWidth - margin, currentY + row2Height, fillPaint)

        // Vertical divider between customer and summary
        canvas.drawLine(colSplitX, currentY, colSplitX, currentY + row2Height, borderPaint)

        // Left: Billed To
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText("BILLED TO (CUSTOMER DETAILS):", margin + 8f, currentY + 11f, textPaint)
        canvas.drawLine(margin + 8f, currentY + 14f, colSplitX - 8f, currentY + 14f, linePaint)

        val rawCustPdf = bill.customerName.trim()
        val parenMatchPdf = Regex("^(.*?)\\s*\\((.*?)\\)$").find(rawCustPdf)
        val (firmPdf, contactPdf) = when {
            parenMatchPdf != null -> Pair(parenMatchPdf.groupValues[1].trim(), parenMatchPdf.groupValues[2].trim())
            rawCustPdf.contains("\n") -> {
                val parts = rawCustPdf.split("\n", limit = 2)
                Pair(parts[0].trim(), parts[1].trim())
            }
            else -> Pair(rawCustPdf, null)
        }

        if (!contactPdf.isNullOrBlank()) {
            textPaint.textSize = 9.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.parseColor("#0F172A")
            canvas.drawText(firmPdf, margin + 8f, currentY + 23f, textPaint)

            textPaint.textSize = 8f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.parseColor("#334155")
            canvas.drawText("Customer: $contactPdf", margin + 8f, currentY + 32f, textPaint)

            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.DEFAULT
            textPaint.color = Color.parseColor("#334155")
            canvas.drawText("Address: ${bill.customerAddress.ifBlank { "N/A" }}", margin + 8f, currentY + 41f, textPaint)
            val custContact = "Mobile: ${bill.customerMobile.ifBlank { "N/A" }} | GSTIN: ${bill.customerGstNo.ifBlank { "Unregistered" }}"
            canvas.drawText(custContact, margin + 8f, currentY + 50f, textPaint)
        } else {
            textPaint.textSize = 10f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.parseColor("#0F172A")
            canvas.drawText(firmPdf, margin + 8f, currentY + 24f, textPaint)

            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.DEFAULT
            textPaint.color = Color.parseColor("#334155")
            canvas.drawText("Address: ${bill.customerAddress.ifBlank { "N/A" }}", margin + 8f, currentY + 34f, textPaint)
            canvas.drawText("Mobile: ${bill.customerMobile.ifBlank { "N/A" }}", margin + 8f, currentY + 43f, textPaint)
            canvas.drawText("GSTIN: ${bill.customerGstNo.ifBlank { "Unregistered" }}", margin + 8f, currentY + 51f, textPaint)
        }

        // Right: Delivery & Totals Summary
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText("PAYMENT & DELIVERY SUMMARY:", colSplitX + 8f, currentY + 11f, textPaint)
        canvas.drawLine(colSplitX + 8f, currentY + 14f, pageWidth - margin - 8f, currentY + 14f, linePaint)

        val totalQty = items.sumOf { it.qty }
        val totalSqFt = items.sumOf { it.sqFt }

        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("Total Quantity:", colSplitX + 8f, currentY + 25f, textPaint)
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText("$totalQty Doors", colSplitX + 70f, currentY + 25f, textPaint)

        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("Total Area:", colSplitX + 8f, currentY + 37f, textPaint)
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText("${String.format(Locale.US, "%.2f", totalSqFt)} Sq.Ft", colSplitX + 70f, currentY + 37f, textPaint)

        val totalWithOldBalance = bill.grandTotal + bill.previousBalance
        val netPayableAmount = if (bill.netPayable > 0.0) bill.netPayable else totalWithOldBalance
        val finalRemainingDue = if (bill.paidAmount > 0.0) Math.max(0.0, netPayableAmount - bill.paidAmount) else netPayableAmount
        val payStatus = if (bill.paidAmount >= netPayableAmount) "PAID" else if (bill.paidAmount > 0) "PARTIAL" else "UNPAID"
        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("Payment Status:", colSplitX + 8f, currentY + 49f, textPaint)
        textPaint.color = if (payStatus == "PAID") Color.parseColor("#16A34A") else Color.parseColor("#D97706")
        canvas.drawText(payStatus, colSplitX + 70f, currentY + 49f, textPaint)

        currentY += row2Height
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
        val tableHeaderTopY = currentY
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
        items.forEachIndexed { index, item ->
            val partLines = item.particular.split("\n")
            val hasSecondLine = partLines.size > 1 && partLines[1].isNotBlank()
            val rowHeight = if (hasSecondLine) 24f else 18f

            if (index % 2 == 1) {
                fillPaint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(margin, currentY, colEnd, currentY + rowHeight, fillPaint)
            }

            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 8f
            textPaint.color = Color.parseColor("#0F172A")

            val baseTextY = if (hasSecondLine) currentY + 11f else currentY + 12f
            canvas.drawText("${item.slNo}", colSl + 6f, baseTextY, textPaint)

            if (hasSecondLine) {
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val line1 = if (partLines[0].length > 25) partLines[0].take(23) + ".." else partLines[0].trim()
                canvas.drawText(line1, colParticular + 4f, currentY + 10f, textPaint)

                textPaint.typeface = Typeface.DEFAULT
                textPaint.textSize = 7.5f
                textPaint.color = Color.parseColor("#475569")
                val line2 = if (partLines[1].length > 26) partLines[1].take(24) + ".." else partLines[1].trim()
                canvas.drawText(line2, colParticular + 4f, currentY + 20f, textPaint)

                textPaint.textSize = 8f
                textPaint.color = Color.parseColor("#0F172A")
            } else {
                val partText = if (item.particular.length > 25) item.particular.take(23) + ".." else item.particular
                canvas.drawText(partText, colParticular + 4f, baseTextY, textPaint)
            }

            canvas.drawText(item.hsnSac, colHsn + 4f, baseTextY, textPaint)
            canvas.drawText(DimensionCalculator.formatDimension(item.height), colHigh + 4f, baseTextY, textPaint)
            canvas.drawText(DimensionCalculator.formatDimension(item.width), colWidth + 4f, baseTextY, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${item.qty}", colQty + 8f, baseTextY, textPaint)

            canvas.drawText(String.format(Locale.US, "%.2f", item.sqFt), colSqFt + 4f, baseTextY, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.rate), colRate + 4f, baseTextY, textPaint)
            canvas.drawText("₹${String.format(Locale.US, "%.2f", item.amount)}", colAmount + 4f, baseTextY, textPaint)

            canvas.drawLine(margin, currentY + rowHeight, colEnd, currentY + rowHeight, linePaint)
            currentY += rowHeight
        }

        // Draw vertical column separators for items
        val tableBottomY = currentY
        val tableTopY = tableHeaderTopY
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

        // Other Charges / Cutting Charges (if applicable)
        if (bill.otherCharges > 0) {
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.parseColor("#0369A1")
            val chargeName = bill.otherChargesDescription.ifBlank { "Cutting Charges" }
            val chargeLabel = if (chargeName.length > 20) chargeName.take(18) + "..:" else "$chargeName:"
            canvas.drawText(chargeLabel, colRate - 65f, currentY + 13f, textPaint)
            canvas.drawText("+ ₹${String.format(Locale.US, "%.2f", bill.otherCharges)}", colAmount + 4f, currentY + 13f, textPaint)
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

        // Previous Balance / Due Balance (if applicable)
        if (bill.previousBalance > 0) {
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 8.5f
            textPaint.color = Color.parseColor("#C2410C")
            canvas.drawText("(+) Previous Balance / Due Balance:", margin + 12f, currentY + 13f, textPaint)
            canvas.drawText("+ ₹${String.format(Locale.US, "%.2f", bill.previousBalance)}", colAmount + 4f, currentY + 13f, textPaint)
            currentY += 16f
            canvas.drawLine(margin, currentY, colEnd, currentY, linePaint)
        }

        if (bill.previousBalance > 0 && bill.paidAmount <= 0.0) {
            // Bill Total Row
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 8.5f
            textPaint.color = Color.parseColor("#0F172A")
            canvas.drawText("Current Bill Total:", colRate - 65f, currentY + 13f, textPaint)
            canvas.drawText("₹${String.format(Locale.US, "%.2f", bill.grandTotal)}", colAmount + 4f, currentY + 13f, textPaint)
            currentY += 16f
            canvas.drawLine(margin, currentY, colEnd, currentY, linePaint)

            // TOTAL DUE Highlight Row
            fillPaint.color = Color.parseColor("#0284C7")
            canvas.drawRect(margin, currentY, colEnd, currentY + 24f, fillPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 10f
            textPaint.color = Color.WHITE
            canvas.drawText("TOTAL DUE:", margin + 12f, currentY + 16f, textPaint)

            val netPayableStr = "₹${String.format(Locale.US, "%.2f", netPayableAmount)}"
            canvas.drawText(netPayableStr, colAmount + 4f, currentY + 16f, textPaint)
        } else if (bill.paidAmount > 0.0) {
            // Bill Total Row
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 8.5f
            textPaint.color = Color.parseColor("#0F172A")
            canvas.drawText("Current Bill Total:", colRate - 65f, currentY + 13f, textPaint)
            canvas.drawText("₹${String.format(Locale.US, "%.2f", bill.grandTotal)}", colAmount + 4f, currentY + 13f, textPaint)
            currentY += 16f
            canvas.drawLine(margin, currentY, colEnd, currentY, linePaint)

            if (bill.previousBalance > 0) {
                textPaint.color = Color.parseColor("#0F172A")
                canvas.drawText("Total Due Amount:", colRate - 65f, currentY + 13f, textPaint)
                canvas.drawText("₹${String.format(Locale.US, "%.2f", netPayableAmount)}", colAmount + 4f, currentY + 13f, textPaint)
                currentY += 16f
                canvas.drawLine(margin, currentY, colEnd, currentY, linePaint)
            }

            // Paid Row
            textPaint.color = Color.parseColor("#16A34A")
            canvas.drawText("(-) Paid / Received Amount:", margin + 12f, currentY + 13f, textPaint)
            canvas.drawText("- ₹${String.format(Locale.US, "%.2f", bill.paidAmount)}", colAmount + 4f, currentY + 13f, textPaint)
            currentY += 16f
            canvas.drawLine(margin, currentY, colEnd, currentY, linePaint)

            // Remaining Due Highlight Row
            fillPaint.color = Color.parseColor("#0284C7")
            canvas.drawRect(margin, currentY, colEnd, currentY + 24f, fillPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 10f
            textPaint.color = Color.WHITE
            canvas.drawText("REMAINING DUE BALANCE:", margin + 12f, currentY + 16f, textPaint)

            val netPayableStr = "₹${String.format(Locale.US, "%.2f", finalRemainingDue)}"
            canvas.drawText(netPayableStr, colAmount + 4f, currentY + 16f, textPaint)
        } else {
            // GRAND TOTAL Highlight Row
            fillPaint.color = Color.parseColor("#0284C7")
            canvas.drawRect(margin, currentY, colEnd, currentY + 24f, fillPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 10f
            textPaint.color = Color.WHITE
            canvas.drawText("GRAND TOTAL:", margin + 12f, currentY + 16f, textPaint)

            val grandTotalStr = "₹${String.format(Locale.US, "%.2f", bill.grandTotal)}"
            canvas.drawText(grandTotalStr, colAmount + 4f, currentY + 16f, textPaint)
        }

        currentY += 24f
        canvas.drawLine(margin, currentY, colEnd, currentY, borderPaint)

        // Amount in words
        fillPaint.color = Color.parseColor("#F8FAFC")
        canvas.drawRect(margin, currentY, colEnd, currentY + 18f, fillPaint)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 8f
        textPaint.color = Color.parseColor("#334155")
        val effectivePayable = if (bill.paidAmount > 0.0 && finalRemainingDue > 0.0) finalRemainingDue else if (bill.previousBalance > 0) netPayableAmount else bill.grandTotal
        val inWords = "Amount in Words: " + DimensionCalculator.convertToIndianCurrencyWords(effectivePayable)
        canvas.drawText(inWords, margin + 8f, currentY + 12f, textPaint)

        currentY += 18f
        canvas.drawLine(margin, currentY, colEnd, currentY, linePaint)

        // Bank Details, QR Code & Declaration
        val qrBitmap = QrCodeHelper.getPaymentQrBitmap(company, effectivePayable, size = 180)
        val hasQr = qrBitmap != null
        val termsHeight = 92f
        val bankWidth = if (hasQr) contentWidth * 0.48f else contentWidth * 0.60f
        val qrWidth = if (hasQr) contentWidth * 0.20f else 0f

        // Bank Details Box (Left)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText("BANK ACCOUNT DETAILS:", margin + 8f, currentY + 11f, textPaint)

        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7f
        textPaint.color = Color.parseColor("#1E293B")
        canvas.drawText("Bank: ${company.bankName}", margin + 8f, currentY + 20f, textPaint)
        canvas.drawText("A/C: ${company.accountNo} | IFSC: ${company.ifscCode}", margin + 8f, currentY + 29f, textPaint)
        var declTopY = currentY + 36f
        val effectiveUpi = QrCodeHelper.resolveEffectiveUpiId(company)
        if (effectiveUpi.isNotBlank()) {
            canvas.drawText("UPI: $effectiveUpi", margin + 8f, currentY + 38f, textPaint)
            declTopY = currentY + 46f
        }

        // Divider before Declaration
        canvas.drawLine(margin + 8f, declTopY - 2f, margin + bankWidth - 8f, declTopY - 2f, linePaint)

        // Reference format: Underlined "Declaration"
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#0F172A")
        val declHeader = "Declaration"
        canvas.drawText(declHeader, margin + 8f, declTopY + 7f, textPaint)
        val declHeaderWidth = textPaint.measureText(declHeader)
        canvas.drawLine(margin + 8f, declTopY + 8.5f, margin + 8f + declHeaderWidth, declTopY + 8.5f, textPaint)

        // Draw FULL Declaration text across multiple lines without truncation
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 6.6f
        textPaint.color = Color.parseColor("#1E293B")
        val declMaxWidth = bankWidth - 16f
        var lineY = declTopY + 16.5f
        val words = company.declaration.split(" ")
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (textPaint.measureText(testLine) <= declMaxWidth) {
                currentLine.append(if (currentLine.isEmpty()) "" else " ").append(word)
            } else {
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine.toString(), margin + 8f, lineY, textPaint)
                    lineY += 8.2f
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine.toString(), margin + 8f, lineY, textPaint)
        }

        // QR Code Box (Center) if available
        if (hasQr && qrBitmap != null) {
            val qrBoxX = margin + bankWidth
            canvas.drawLine(qrBoxX, currentY, qrBoxX, currentY + termsHeight, linePaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 6f
            textPaint.color = Color.parseColor("#0369A1")
            val qrTitle = "SCAN TO PAY UPI"
            val qrTitleW = textPaint.measureText(qrTitle)
            canvas.drawText(qrTitle, qrBoxX + (qrWidth - qrTitleW) / 2f, currentY + 11f, textPaint)

            // Draw QR code image
            val qrSize = 52f
            val qrLeft = qrBoxX + (qrWidth - qrSize) / 2f
            val qrTop = currentY + 16f
            val destRect = RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)
            canvas.drawBitmap(qrBitmap, null, destRect, null)

            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 5.5f
            textPaint.color = Color.parseColor("#64748B")
            val qrFooter = "PhonePe • GPay • Paytm"
            val qrFooterW = textPaint.measureText(qrFooter)
            canvas.drawText(qrFooter, qrBoxX + (qrWidth - qrFooterW) / 2f, currentY + termsHeight - 4f, textPaint)
        }

        // Signature Box (Right)
        val signBoxX = margin + bankWidth + qrWidth
        val signBoxWidth = colEnd - signBoxX
        canvas.drawLine(signBoxX, currentY, signBoxX, currentY + termsHeight, linePaint)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#475569")
        val forText = "For ${company.businessName}"
        canvas.drawText(forText, signBoxX + 12f, currentY + 18f, textPaint)

        val authSignY = currentY + termsHeight - 12f
        canvas.drawLine(signBoxX + 12f, authSignY - 10f, colEnd - 12f, authSignY - 10f, linePaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#0F172A")
        val authText = "AUTHORIZED SIGNATORY"
        val authW = textPaint.measureText(authText)
        canvas.drawText(authText, signBoxX + (signBoxWidth - authW) / 2f, authSignY, textPaint)

        currentY += termsHeight
        canvas.drawLine(margin, currentY, colEnd, currentY, borderPaint)

        // 5. BOTTOM JURISDICTION & COMPUTER GENERATED NOTICE (Centered: Jurisdiction on top, Computer Generated below)
        val jurisdictionBannerHeight = 28f
        fillPaint.color = Color.parseColor("#EEF5F9")
        canvas.drawRect(margin, pageHeight - margin - jurisdictionBannerHeight, colEnd, pageHeight - margin, fillPaint)
        canvas.drawLine(margin, pageHeight - margin - jurisdictionBannerHeight, colEnd, pageHeight - margin - jurisdictionBannerHeight, linePaint)

        // Line 1: SUBJECT TO [JURISDICTION] JURISDICTION ONLY (Centered, Bold)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 8.5f
        textPaint.color = Color.parseColor("#1E293B")
        val jurisdictionText = DimensionCalculator.formatJurisdictionClause(company.jurisdiction)
        val jWidth = textPaint.measureText(jurisdictionText)
        val jX = margin + (contentWidth - jWidth) / 2f
        canvas.drawText(jurisdictionText, jX, pageHeight - margin - 15f, textPaint)

        // Line 2: This is a Computer Generated Invoice / Quotation (Centered, below Line 1)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7f
        textPaint.color = Color.parseColor("#64748B")
        val compGenText = if (bill.isQuotation) "This is a Computer Generated Quotation" else "This is a Computer Generated Invoice"
        val cWidth = textPaint.measureText(compGenText)
        val cX = margin + (contentWidth - cWidth) / 2f
        canvas.drawText(compGenText, cX, pageHeight - margin - 5f, textPaint)
    }

    /**
     * Generates a standard A4 PDF document for the Customer Ledger statement.
     */
    fun generateLedgerPdf(
        context: Context,
        customer: CustomerEntity,
        ledgerEntries: List<LedgerEntry>,
        totalBilled: Double,
        totalPaid: Double,
        balance: Double,
        company: CompanyProfileEntity
    ): File {
        val ledgerDir = File(context.cacheDir, "ledgers")
        if (!ledgerDir.exists()) {
            ledgerDir.mkdirs()
        }

        val safeCustomerName = customer.name.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val pdfFile = File(ledgerDir, "Ledger_${safeCustomerName}.pdf")

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        drawLedger(canvas, pageWidth.toFloat(), pageHeight.toFloat(), customer, ledgerEntries, totalBilled, totalPaid, balance, company, context)

        document.finishPage(page)

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    private fun drawLedger(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        customer: CustomerEntity,
        ledgerEntries: List<LedgerEntry>,
        totalBilled: Double,
        totalPaid: Double,
        balance: Double,
        company: CompanyProfileEntity,
        context: Context
    ) {
        val margin = 20f
        val contentWidth = pageWidth - (margin * 2)
        val colEnd = pageWidth - margin

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val linePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            strokeWidth = 0.8f
        }
        val fillPaint = Paint()

        // Outer Border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRect(margin, margin, colEnd, pageHeight - margin, borderPaint)

        // Header Background
        fillPaint.color = Color.parseColor("#0F172A")
        canvas.drawRect(margin, margin, colEnd, margin + 55f, fillPaint)

        // Company Name
        textPaint.color = Color.WHITE
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 15f
        canvas.drawText(company.businessName.ifBlank { "DOOR BILLING SYSTEM" }, margin + 12f, margin + 22f, textPaint)

        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#E2E8F0")
        canvas.drawText("${company.fullAddress} • Phone: ${company.displayMobile}", margin + 12f, margin + 35f, textPaint)
        if (company.gstNo.isNotBlank()) {
            canvas.drawText("GSTIN: ${company.gstNo}", margin + 12f, margin + 47f, textPaint)
        }

        // Header Right: Title
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 12f
        textPaint.color = Color.parseColor("#38BDF8")
        val titleText = "ACCOUNT STATEMENT"
        canvas.drawText(titleText, colEnd - textPaint.measureText(titleText) - 12f, margin + 24f, textPaint)

        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#E2E8F0")
        val dateText = "Date: ${DimensionCalculator.formatDate(System.currentTimeMillis())}"
        canvas.drawText(dateText, colEnd - textPaint.measureText(dateText) - 12f, margin + 38f, textPaint)

        var currentY = margin + 55f

        // Customer Info Card & Balance Summary Box
        val infoHeight = 60f
        fillPaint.color = Color.parseColor("#F8FAFC")
        canvas.drawRect(margin, currentY, colEnd, currentY + infoHeight, fillPaint)
        canvas.drawLine(margin, currentY + infoHeight, colEnd, currentY + infoHeight, linePaint)

        // Customer Details Left
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 8.5f
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText("STATEMENT FOR:", margin + 12f, currentY + 16f, textPaint)

        textPaint.textSize = 11f
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText(customer.name, margin + 12f, currentY + 30f, textPaint)

        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 8f
        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("Mobile: ${customer.mobile.ifBlank { "N/A" }}", margin + 12f, currentY + 43f, textPaint)
        if (customer.address.isNotBlank()) {
            canvas.drawText("Address: ${customer.address}", margin + 12f, currentY + 54f, textPaint)
        }

        // Summary Boxes Right: Total Billed, Total Paid, Due Balance
        val sumBoxStartX = margin + contentWidth * 0.52f
        canvas.drawLine(sumBoxStartX, currentY, sumBoxStartX, currentY + infoHeight, linePaint)

        val statWidth = (colEnd - sumBoxStartX) / 3f

        // Box 1: Billed
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7f
        textPaint.color = Color.parseColor("#64748B")
        canvas.drawText("TOTAL BILLED", sumBoxStartX + 6f, currentY + 18f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 9.5f
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText("₹${String.format(Locale.US, "%.2f", totalBilled)}", sumBoxStartX + 6f, currentY + 34f, textPaint)

        canvas.drawLine(sumBoxStartX + statWidth, currentY, sumBoxStartX + statWidth, currentY + infoHeight, linePaint)

        // Box 2: Paid
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7f
        textPaint.color = Color.parseColor("#64748B")
        canvas.drawText("TOTAL PAID", sumBoxStartX + statWidth + 6f, currentY + 18f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 9.5f
        textPaint.color = Color.parseColor("#16A34A")
        canvas.drawText("₹${String.format(Locale.US, "%.2f", totalPaid)}", sumBoxStartX + statWidth + 6f, currentY + 34f, textPaint)

        canvas.drawLine(sumBoxStartX + statWidth * 2, currentY, sumBoxStartX + statWidth * 2, currentY + infoHeight, linePaint)

        // Box 3: Due Balance
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7f
        textPaint.color = Color.parseColor("#64748B")
        canvas.drawText("BALANCE DUE", sumBoxStartX + statWidth * 2 + 6f, currentY + 18f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 10f
        textPaint.color = if (balance > 0) Color.parseColor("#DC2626") else Color.parseColor("#16A34A")
        canvas.drawText("₹${String.format(Locale.US, "%.2f", balance)}", sumBoxStartX + statWidth * 2 + 6f, currentY + 34f, textPaint)

        currentY += infoHeight

        // Transactions Table Header
        val thHeight = 18f
        fillPaint.color = Color.parseColor("#0F172A")
        canvas.drawRect(margin, currentY, colEnd, currentY + thHeight, fillPaint)

        textPaint.color = Color.WHITE
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 7.5f

        val cDate = margin + 8f
        val cDesc = margin + 70f
        val cDebit = colEnd - 180f
        val cCredit = colEnd - 110f
        val cBal = colEnd - 45f

        canvas.drawText("DATE", cDate, currentY + 12f, textPaint)
        canvas.drawText("TRANSACTION DETAILS / INVOICE NO.", cDesc, currentY + 12f, textPaint)
        canvas.drawText("DEBIT (+₹)", cDebit, currentY + 12f, textPaint)
        canvas.drawText("CREDIT (-₹)", cCredit, currentY + 12f, textPaint)
        canvas.drawText("BALANCE", cBal, currentY + 12f, textPaint)

        currentY += thHeight

        // Transactions Rows
        var runningBal = 0.0
        val rowHeight = 17f
        textPaint.textSize = 7.5f

        ledgerEntries.forEachIndexed { index, entry ->
            runningBal += (entry.debitAmount - entry.creditAmount)

            if (index % 2 == 1) {
                fillPaint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(margin, currentY, colEnd, currentY + rowHeight, fillPaint)
            }

            textPaint.typeface = Typeface.DEFAULT
            textPaint.color = Color.parseColor("#334155")
            canvas.drawText(DimensionCalculator.formatDate(entry.dateMillis), cDate, currentY + 11.5f, textPaint)

            val shortDesc = if (entry.description.length > 40) entry.description.take(38) + ".." else entry.description
            canvas.drawText(shortDesc, cDesc, currentY + 11.5f, textPaint)

            if (entry.debitAmount > 0) {
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.color = Color.parseColor("#0369A1")
                canvas.drawText(String.format(Locale.US, "%.2f", entry.debitAmount), cDebit, currentY + 11.5f, textPaint)
            } else {
                textPaint.color = Color.parseColor("#94A3B8")
                canvas.drawText("-", cDebit, currentY + 11.5f, textPaint)
            }

            if (entry.creditAmount > 0) {
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.color = Color.parseColor("#16A34A")
                canvas.drawText(String.format(Locale.US, "%.2f", entry.creditAmount), cCredit, currentY + 11.5f, textPaint)
            } else {
                textPaint.color = Color.parseColor("#94A3B8")
                canvas.drawText("-", cCredit, currentY + 11.5f, textPaint)
            }

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = if (runningBal > 0) Color.parseColor("#DC2626") else Color.parseColor("#16A34A")
            canvas.drawText(String.format(Locale.US, "%.2f", runningBal), cBal, currentY + 11.5f, textPaint)

            currentY += rowHeight
            canvas.drawLine(margin, currentY, colEnd, currentY, linePaint)
        }

        // Bank Details & Footer at bottom
        val footerHeight = 65f
        val footerY = pageHeight - margin - footerHeight - 22f

        fillPaint.color = Color.parseColor("#F8FAFC")
        canvas.drawRect(margin, footerY, colEnd, footerY + footerHeight, fillPaint)
        canvas.drawLine(margin, footerY, colEnd, footerY, borderPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 8f
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText("BANK ACCOUNT DETAILS FOR PAYMENT:", margin + 10f, footerY + 16f, textPaint)

        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#1E293B")
        canvas.drawText("Bank: ${company.bankName} | A/C: ${company.accountNo} | IFSC: ${company.ifscCode}", margin + 10f, footerY + 30f, textPaint)
        canvas.drawText("UPI / Mobile: ${if (company.upiId.isNotBlank()) company.upiId else company.mobile}", margin + 10f, footerY + 44f, textPaint)

        // Payment QR Code in statement if available
        val ledgerQrBitmap = QrCodeHelper.getPaymentQrBitmap(company, if (balance > 0) balance else null, size = 160)
        val hasLedgerQr = ledgerQrBitmap != null
        if (hasLedgerQr && ledgerQrBitmap != null) {
            val qrBoxX = margin + contentWidth * 0.52f
            canvas.drawLine(qrBoxX, footerY, qrBoxX, footerY + footerHeight, linePaint)
            val qrSize = 42f
            val qrLeft = qrBoxX + 10f
            val qrTop = footerY + 6f
            canvas.drawBitmap(ledgerQrBitmap, null, RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), null)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 5.5f
            textPaint.color = Color.parseColor("#0369A1")
            canvas.drawText("Scan to Pay", qrLeft + 4f, footerY + footerHeight - 4f, textPaint)
        }

        // Signatory Right
        val signX = margin + contentWidth * (if (hasLedgerQr) 0.68f else 0.65f)
        canvas.drawLine(signX, footerY, signX, footerY + footerHeight, linePaint)

        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("For ${company.businessName}", signX + 12f, footerY + 18f, textPaint)

        canvas.drawLine(signX + 12f, footerY + 44f, colEnd - 12f, footerY + 44f, linePaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText("AUTHORIZED SIGNATORY", signX + 14f, footerY + 54f, textPaint)

        // Bottom Jurisdiction Banner
        val jBannerHeight = 22f
        fillPaint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(margin, pageHeight - margin - jBannerHeight, colEnd, pageHeight - margin, fillPaint)
        canvas.drawLine(margin, pageHeight - margin - jBannerHeight, colEnd, pageHeight - margin - jBannerHeight, linePaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 8.5f
        textPaint.color = Color.parseColor("#1E293B")
        val jText = DimensionCalculator.formatJurisdictionClause(company.jurisdiction)
        val jW = textPaint.measureText(jText)
        canvas.drawText(jText, (pageWidth - jW) / 2f, pageHeight - margin - 8f, textPaint)
    }

    /**
     * Generates a crisp A4 PDF for Delivery Challan / Dispatch Slip.
     */
    fun generateDeliveryChallanPdf(
        context: Context,
        billWithItems: BillWithItems,
        company: CompanyProfileEntity,
        vehicleNo: String = "",
        transportName: String = ""
    ): File {
        val dir = File(context.cacheDir, "challans").apply { if (!exists()) mkdirs() }
        val safeChallanNo = billWithItems.bill.invoiceNo.replace("/", "_").replace("\\", "_")
        val pdfFile = File(dir, "Challan_$safeChallanNo.pdf")

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        drawDeliveryChallan(canvas, pageWidth.toFloat(), pageHeight.toFloat(), billWithItems, company, vehicleNo, transportName)

        document.finishPage(page)
        FileOutputStream(pdfFile).use { out -> document.writeTo(out) }
        document.close()
        return pdfFile
    }

    private fun drawDeliveryChallan(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        billWithItems: BillWithItems,
        company: CompanyProfileEntity,
        vehicleNo: String,
        transportName: String
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
        val fillPaint = Paint().apply { isAntiAlias = true }
        val linePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            strokeWidth = 0.8f
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#0F172A")
        }

        // Outer Border
        canvas.drawRect(margin, margin, pageWidth - margin, pageHeight - margin, borderPaint)

        // 1. Top Title Bar
        val titleH = 22f
        fillPaint.color = Color.parseColor("#DCEEF8")
        canvas.drawRect(margin, margin, pageWidth - margin, margin + titleH, fillPaint)
        canvas.drawLine(margin, margin + titleH, pageWidth - margin, margin + titleH, borderPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 10.5f
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText("DELIVERY CHALLAN / GATE PASS (DISPATCH SLIP)", margin + 10f, margin + 15f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        textPaint.textSize = 8f
        textPaint.color = Color.parseColor("#475569")
        val sub = "(Goods Movement & Material Dispatch)"
        canvas.drawText(sub, pageWidth - margin - textPaint.measureText(sub) - 10f, margin + 15f, textPaint)

        // 2. Header & Meta Row
        var curY = margin + titleH
        val colSplitX = margin + (contentWidth * 0.58f)
        val row1H = 75f

        fillPaint.color = Color.parseColor("#EEF5F9")
        canvas.drawRect(margin, curY, colSplitX, curY + row1H, fillPaint)
        fillPaint.color = Color.parseColor("#D3E3ED")
        canvas.drawRect(colSplitX, curY, pageWidth - margin, curY + row1H, fillPaint)
        canvas.drawLine(colSplitX, curY, colSplitX, curY + row1H, borderPaint)

        // Left: Manufacturer
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 12f
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText(company.businessName.uppercase(Locale.ROOT), margin + 8f, curY + 16f, textPaint)

        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#334155")
        canvas.drawText(company.fullAddress, margin + 8f, curY + 28f, textPaint)
        canvas.drawText("GSTIN: ${company.gstNo} | Mobile: ${company.displayMobile}", margin + 8f, curY + 40f, textPaint)

        // Consignee section inside row 1
        canvas.drawLine(margin + 8f, curY + 46f, colSplitX - 8f, curY + 46f, linePaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText("CONSIGNEE / DELIVER TO: ${bill.customerName}", margin + 8f, curY + 58f, textPaint)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("Site: ${bill.customerAddress.ifBlank { "As per order" }} | Mob: ${bill.customerMobile.ifBlank { "N/A" }}", margin + 8f, curY + 69f, textPaint)

        // Right: Dispatch Details
        val challanNo = "DC/" + bill.invoiceNo.removePrefix("INV/").removePrefix("EST/")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 8f
        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("Challan No:", colSplitX + 8f, curY + 16f, textPaint)
        textPaint.color = Color.parseColor("#0284C7")
        canvas.drawText(challanNo, colSplitX + 68f, curY + 16f, textPaint)

        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("Dispatch Date:", colSplitX + 8f, curY + 30f, textPaint)
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText(DimensionCalculator.formatDate(bill.dateMillis), colSplitX + 68f, curY + 30f, textPaint)

        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("Order / Ref:", colSplitX + 8f, curY + 44f, textPaint)
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText(bill.invoiceNo, colSplitX + 68f, curY + 44f, textPaint)

        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("Vehicle No:", colSplitX + 8f, curY + 58f, textPaint)
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText(vehicleNo.ifBlank { "Local Transport" }, colSplitX + 68f, curY + 58f, textPaint)

        textPaint.color = Color.parseColor("#334155")
        canvas.drawText("Transporter:", colSplitX + 8f, curY + 71f, textPaint)
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText(transportName.ifBlank { "Factory Direct" }, colSplitX + 68f, curY + 71f, textPaint)

        curY += row1H
        canvas.drawLine(margin, curY, pageWidth - margin, curY, borderPaint)

        // 3. Table Header
        val thH = 20f
        fillPaint.color = Color.parseColor("#0369A1")
        canvas.drawRect(margin, curY, pageWidth - margin, curY + thH, fillPaint)

        val col1 = margin + 28f
        val col2 = margin + 230f
        val col3 = margin + 280f
        val col4 = margin + 380f
        val col5 = margin + 445f
        val col6 = margin + 510f

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 8f
        textPaint.color = Color.WHITE
        canvas.drawText("S.N.", margin + 6f, curY + 13f, textPaint)
        canvas.drawText("Door Description / Model", col1 + 6f, curY + 13f, textPaint)
        canvas.drawText("HSN", col2 + 6f, curY + 13f, textPaint)
        canvas.drawText("Size (H × W)", col3 + 6f, curY + 13f, textPaint)
        canvas.drawText("Qty (Pcs)", col4 + 6f, curY + 13f, textPaint)
        canvas.drawText("Sq.Ft", col5 + 6f, curY + 13f, textPaint)
        canvas.drawText("Status", col6 + 6f, curY + 13f, textPaint)

        curY += thH

        // Items
        val itemRowH = 22f
        var totalQty = 0
        var totalSqFt = 0.0

        items.forEachIndexed { idx, item ->
            totalQty += item.qty
            totalSqFt += item.sqFt

            if (idx % 2 == 1) {
                fillPaint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(margin, curY, pageWidth - margin, curY + itemRowH, fillPaint)
            }
            canvas.drawLine(margin, curY + itemRowH, pageWidth - margin, curY + itemRowH, linePaint)

            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 8f
            textPaint.color = Color.parseColor("#0F172A")
            canvas.drawText(item.slNo.toString(), margin + 8f, curY + 14f, textPaint)

            val partFirstLine = item.particular.lines().firstOrNull() ?: item.particular
            canvas.drawText(partFirstLine.take(34), col1 + 6f, curY + 14f, textPaint)

            textPaint.color = Color.parseColor("#475569")
            canvas.drawText(item.hsnSac, col2 + 6f, curY + 14f, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.parseColor("#0F172A")
            canvas.drawText("${DimensionCalculator.formatDimension(item.height)} × ${DimensionCalculator.formatDimension(item.width)} ${bill.dimensionUnit}", col3 + 6f, curY + 14f, textPaint)

            textPaint.color = Color.parseColor("#0369A1")
            canvas.drawText("${item.qty} Pcs", col4 + 6f, curY + 14f, textPaint)

            textPaint.color = Color.parseColor("#0F172A")
            canvas.drawText(String.format(Locale.US, "%.2f", item.sqFt), col5 + 6f, curY + 14f, textPaint)

            textPaint.typeface = Typeface.DEFAULT
            textPaint.color = Color.parseColor("#16A34A")
            canvas.drawText("OK", col6 + 6f, curY + 14f, textPaint)

            curY += itemRowH
        }

        // Total Row
        val totH = 22f
        fillPaint.color = Color.parseColor("#EFF6FF")
        canvas.drawRect(margin, curY, pageWidth - margin, curY + totH, fillPaint)
        canvas.drawLine(margin, curY + totH, pageWidth - margin, curY + totH, borderPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 8.5f
        textPaint.color = Color.parseColor("#0369A1")
        canvas.drawText("TOTAL DISPATCHED:", col3 - 60f, curY + 14f, textPaint)
        canvas.drawText("$totalQty Doors", col4 + 6f, curY + 14f, textPaint)
        canvas.drawText(String.format(Locale.US, "%.2f Sq.Ft", totalSqFt), col5 + 6f, curY + 14f, textPaint)

        curY += totH + 14f

        // Notes box
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7f
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("1. Goods dispatched in sound & undamaged condition. Please verify sizes & count at the time of delivery.", margin + 12f, curY, textPaint)
        canvas.drawText("2. This Delivery Challan is for transport & verification purposes.", margin + 12f, curY + 12f, textPaint)

        // Signatures at bottom
        val signY = pageHeight - margin - 50f
        canvas.drawLine(margin + 16f, signY, margin + 140f, signY, linePaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText("RECEIVER'S SIGN & STAMP", margin + 16f, signY + 12f, textPaint)

        canvas.drawLine(margin + 190f, signY, margin + 330f, signY, linePaint)
        canvas.drawText("DRIVER / TRANSPORTER SIGN", margin + 190f, signY + 12f, textPaint)

        canvas.drawLine(pageWidth - margin - 150f, signY, pageWidth - margin - 16f, signY, linePaint)
        canvas.drawText("AUTHORIZED DISPATCHER", pageWidth - margin - 146f, signY + 12f, textPaint)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 6.5f
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("For ${company.businessName}", pageWidth - margin - 146f, signY - 8f, textPaint)
    }

    /**
     * Generates a crisp A4 PDF for Payment Receipt / Money Voucher.
     */
    fun generatePaymentReceiptPdf(
        context: Context,
        payment: PaymentEntity,
        customerName: String,
        customerMobile: String,
        company: CompanyProfileEntity,
        previousBalance: Double,
        remainingBalance: Double
    ): File {
        val dir = File(context.cacheDir, "receipts").apply { if (!exists()) mkdirs() }
        val pdfFile = File(dir, "Receipt_${payment.id}.pdf")

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        drawPaymentReceipt(canvas, pageWidth.toFloat(), pageHeight.toFloat(), payment, customerName, customerMobile, company, previousBalance, remainingBalance)

        document.finishPage(page)
        FileOutputStream(pdfFile).use { out -> document.writeTo(out) }
        document.close()
        return pdfFile
    }

    private fun drawPaymentReceipt(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        payment: PaymentEntity,
        customerName: String,
        customerMobile: String,
        company: CompanyProfileEntity,
        previousBalance: Double,
        remainingBalance: Double
    ) {
        val margin = 30f
        val contentWidth = pageWidth - (margin * 2)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#16A34A")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        val fillPaint = Paint().apply { isAntiAlias = true }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 0.8f
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#0F172A")
        }

        // Draw Card Border
        val cardBottom = margin + 460f
        canvas.drawRoundRect(margin, margin, pageWidth - margin, cardBottom, 8f, 8f, borderPaint)

        // Top Banner
        val topH = 34f
        fillPaint.color = Color.parseColor("#DCFCE7")
        canvas.drawRoundRect(margin, margin, pageWidth - margin, margin + topH, 8f, 8f, fillPaint)
        canvas.drawLine(margin, margin + topH, pageWidth - margin, margin + topH, borderPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 12f
        textPaint.color = Color.parseColor("#15803D")
        canvas.drawText("PAYMENT RECEIPT / MONEY VOUCHER", margin + 16f, margin + 22f, textPaint)

        val rcptNo = "REC-${payment.id.toString().padStart(4, '0')}"
        textPaint.textSize = 10f
        textPaint.color = Color.parseColor("#0F172A")
        val rcptW = textPaint.measureText("Receipt: $rcptNo")
        canvas.drawText("Receipt: $rcptNo", pageWidth - margin - rcptW - 16f, margin + 22f, textPaint)

        // Company Details
        var curY = margin + topH + 20f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 15f
        textPaint.color = Color.parseColor("#15803D")
        canvas.drawText(company.businessName.uppercase(Locale.ROOT), margin + 16f, curY, textPaint)

        curY += 14f
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 8.5f
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("${company.fullAddress} | Phone: ${company.displayMobile}", margin + 16f, curY, textPaint)
        curY += 12f
        canvas.drawText("GSTIN: ${company.gstNo} | Date: ${DimensionCalculator.formatDate(payment.dateMillis)}", margin + 16f, curY, textPaint)

        curY += 12f
        canvas.drawLine(margin + 16f, curY, pageWidth - margin - 16f, curY, linePaint)

        // Received From & Mode
        curY += 20f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 9.5f
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("Received With Thanks From:", margin + 16f, curY, textPaint)

        textPaint.color = Color.parseColor("#0F172A")
        textPaint.textSize = 12f
        canvas.drawText("$customerName ${if (customerMobile.isNotBlank()) "($customerMobile)" else ""}", margin + 160f, curY, textPaint)

        curY += 22f
        textPaint.textSize = 9.5f
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("Payment Mode & Reference:", margin + 16f, curY, textPaint)

        textPaint.color = Color.parseColor("#15803D")
        textPaint.textSize = 10f
        val modeRef = "${payment.paymentMode} ${if (payment.referenceNo.isNotBlank()) " (Ref / UTR: " + payment.referenceNo + ")" else ""}"
        canvas.drawText(modeRef, margin + 160f, curY, textPaint)

        curY += 20f
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("Remarks / Notes:", margin + 16f, curY, textPaint)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.color = Color.parseColor("#334155")
        canvas.drawText(payment.notes.ifBlank { "Account settlement" }, margin + 160f, curY, textPaint)

        // Amount Box
        curY += 24f
        fillPaint.color = Color.parseColor("#15803D")
        canvas.drawRoundRect(margin + 16f, curY, pageWidth - margin - 16f, curY + 44f, 6f, 6f, fillPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 9f
        textPaint.color = Color.WHITE
        canvas.drawText("AMOUNT RECEIVED:", margin + 28f, curY + 18f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        textPaint.textSize = 8f
        val inWords = DimensionCalculator.convertToIndianCurrencyWords(payment.amount)
        canvas.drawText(inWords.take(45), margin + 28f, curY + 34f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 18f
        val amtStr = "₹ " + String.format(Locale.US, "%,.2f", payment.amount)
        val amtW = textPaint.measureText(amtStr)
        canvas.drawText(amtStr, pageWidth - margin - 28f - amtW, curY + 28f, textPaint)

        // Ledger Balance Summary Table
        curY += 60f
        fillPaint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(margin + 16f, curY, pageWidth - margin - 16f, curY + 68f, 6f, 6f, fillPaint)
        canvas.drawRoundRect(margin + 16f, curY, pageWidth - margin - 16f, curY + 68f, 6f, 6f, linePaint)

        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 8.5f
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("Previous Outstanding Balance:", margin + 28f, curY + 18f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.parseColor("#0F172A")
        val pbStr = "₹ " + String.format(Locale.US, "%.2f", previousBalance)
        canvas.drawText(pbStr, pageWidth - margin - 28f - textPaint.measureText(pbStr), curY + 18f, textPaint)

        textPaint.typeface = Typeface.DEFAULT
        textPaint.color = Color.parseColor("#15803D")
        canvas.drawText("Amount Received Now:", margin + 28f, curY + 36f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val recStr = "- ₹ " + String.format(Locale.US, "%.2f", payment.amount)
        canvas.drawText(recStr, pageWidth - margin - 28f - textPaint.measureText(recStr), curY + 36f, textPaint)

        canvas.drawLine(margin + 24f, curY + 44f, pageWidth - margin - 24f, curY + 44f, linePaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 9.5f
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText("Current Remaining Balance:", margin + 28f, curY + 58f, textPaint)
        val remColor = if (remainingBalance > 0) Color.parseColor("#B91C1C") else Color.parseColor("#15803D")
        textPaint.color = remColor
        val remStr = "₹ " + String.format(Locale.US, "%.2f", remainingBalance)
        canvas.drawText(remStr, pageWidth - margin - 28f - textPaint.measureText(remStr), curY + 58f, textPaint)

        // Signatures
        val signY = cardBottom - 45f
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7f
        textPaint.color = Color.parseColor("#64748B")
        canvas.drawText("Thank you for your business!", margin + 16f, signY + 16f, textPaint)
        canvas.drawText("This is an authorized payment acknowledgement.", margin + 16f, signY + 28f, textPaint)

        canvas.drawLine(pageWidth - margin - 150f, signY + 16f, pageWidth - margin - 16f, signY + 16f, linePaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 8f
        textPaint.color = Color.parseColor("#0F172A")
        canvas.drawText("AUTHORIZED SIGNATORY", pageWidth - margin - 146f, signY + 28f, textPaint)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 7f
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("For ${company.businessName}", pageWidth - margin - 146f, signY + 6f, textPaint)
    }
}

