package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.db.BillWithItems
import com.example.data.db.CompanyProfileEntity
import com.example.data.db.CustomerEntity
import com.example.data.db.LedgerEntry
import java.util.Locale

object ShareHelper {

    /**
     * Standard Android Share Chooser for Invoice PDF (shows all apps: WhatsApp, Gmail, Drive, Bluetooth, etc.)
     */
    fun shareInvoicePdfGeneral(
        context: Context,
        billWithItems: BillWithItems,
        company: CompanyProfileEntity
    ) {
        val bill = billWithItems.bill
        try {
            val pdfFile = PdfInvoiceGenerator.generateInvoicePdf(context, billWithItems, company)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val caption = buildInvoiceCaption(billWithItems, company)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                putExtra(Intent.EXTRA_SUBJECT, "Tax Invoice #${bill.invoiceNo} - ${bill.customerName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Tax Invoice via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shares invoice PDF directly to regular WhatsApp
     */
    fun shareInvoicePdfWhatsApp(
        context: Context,
        billWithItems: BillWithItems,
        company: CompanyProfileEntity
    ) {
        try {
            val pdfFile = PdfInvoiceGenerator.generateInvoicePdf(context, billWithItems, company)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val caption = buildInvoiceCaption(billWithItems, company)
            sharePdfDirect(
                context = context,
                pdfUri = uri,
                caption = caption,
                targetPackage = "com.whatsapp",
                appName = "WhatsApp",
                chooserTitle = "Share Invoice #${billWithItems.bill.invoiceNo}"
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shares invoice PDF directly to WhatsApp Business
     */
    fun shareInvoicePdfWhatsAppBusiness(
        context: Context,
        billWithItems: BillWithItems,
        company: CompanyProfileEntity
    ) {
        try {
            val pdfFile = PdfInvoiceGenerator.generateInvoicePdf(context, billWithItems, company)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val caption = buildInvoiceCaption(billWithItems, company)
            sharePdfDirect(
                context = context,
                pdfUri = uri,
                caption = caption,
                targetPackage = "com.whatsapp.w4b",
                appName = "WhatsApp Business",
                chooserTitle = "Share Invoice #${billWithItems.bill.invoiceNo}"
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shares invoice PDF directly via WhatsApp (kept for backward compatibility)
     */
    fun shareInvoiceWhatsApp(
        context: Context,
        billWithItems: BillWithItems,
        company: CompanyProfileEntity,
        targetMobile: String? = null
    ) {
        shareInvoicePdfWhatsApp(context, billWithItems, company)
    }

    /**
     * Shares text summary of invoice via Android Chooser
     */
    fun shareInvoiceTextGeneral(
        context: Context,
        billWithItems: BillWithItems,
        company: CompanyProfileEntity
    ) {
        try {
            val caption = buildInvoiceCaption(billWithItems, company)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, caption)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice #${billWithItems.bill.invoiceNo}")
            }
            context.startActivity(Intent.createChooser(intent, "Share Invoice Summary via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing summary: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildInvoiceCaption(billWithItems: BillWithItems, company: CompanyProfileEntity): String {
        val bill = billWithItems.bill
        val totalSqFt = billWithItems.items.sumOf { it.sqFt }
        return buildString {
            appendLine("🧾 *TAX INVOICE / BILL*")
            appendLine("*${company.businessName}*")
            appendLine("📄 *Invoice No:* ${bill.invoiceNo}")
            appendLine("👤 *Customer:* ${bill.customerName}")
            appendLine("📅 *Date:* ${DimensionCalculator.formatDate(bill.dateMillis)}")
            appendLine("🚪 *Items:* ${billWithItems.items.size} doors (${String.format(Locale.US, "%.2f", totalSqFt)} Sq.Ft)")
            appendLine("💰 *Grand Total:* ₹${String.format(Locale.US, "%.2f", bill.grandTotal)}")
            if (bill.paidAmount > 0.0) {
                appendLine("💵 *Received:* ₹${String.format(Locale.US, "%.2f", bill.paidAmount)}")
                val due = bill.grandTotal - bill.paidAmount
                if (due > 0.0) {
                    appendLine("⚠️ *Balance Due:* ₹${String.format(Locale.US, "%.2f", due)}")
                }
            }
            appendLine("Please find attached official PDF Tax Invoice.")
            appendLine("Thank you for your business! 🙏")
        }
    }

    private fun sharePdfDirect(
        context: Context,
        pdfUri: Uri,
        caption: String,
        targetPackage: String,
        appName: String,
        chooserTitle: String
    ) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage(targetPackage)
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "$appName is not installed on this device. Opening other apps...",
                    Toast.LENGTH_SHORT
                ).show()

                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, chooserTitle))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Standard Android Share Chooser for Customer Ledger Statement PDF (shows all apps)
     */
    fun shareLedgerPdfGeneral(
        context: Context,
        customer: CustomerEntity,
        ledgerEntries: List<LedgerEntry>,
        totalBilled: Double,
        totalPaid: Double,
        balance: Double,
        company: CompanyProfileEntity
    ) {
        try {
            val pdfFile = PdfInvoiceGenerator.generateLedgerPdf(
                context, customer, ledgerEntries, totalBilled, totalPaid, balance, company
            )
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val summaryText = buildLedgerCaption(customer, ledgerEntries, totalBilled, totalPaid, balance, company)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, summaryText)
                putExtra(Intent.EXTRA_SUBJECT, "Account Statement - ${customer.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Account Statement via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not generate Ledger PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares customer ledger statement as PDF directly to WhatsApp
     */
    fun shareLedgerPdfWhatsApp(
        context: Context,
        customer: CustomerEntity,
        ledgerEntries: List<LedgerEntry>,
        totalBilled: Double,
        totalPaid: Double,
        balance: Double,
        company: CompanyProfileEntity
    ) {
        try {
            val pdfFile = PdfInvoiceGenerator.generateLedgerPdf(
                context, customer, ledgerEntries, totalBilled, totalPaid, balance, company
            )
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val summaryText = buildLedgerCaption(customer, ledgerEntries, totalBilled, totalPaid, balance, company)
            sharePdfDirect(
                context = context,
                pdfUri = uri,
                caption = summaryText,
                targetPackage = "com.whatsapp",
                appName = "WhatsApp",
                chooserTitle = "Share Statement - ${customer.name}"
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Could not generate Ledger PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares customer ledger statement as PDF directly to WhatsApp Business
     */
    fun shareLedgerPdfWhatsAppBusiness(
        context: Context,
        customer: CustomerEntity,
        ledgerEntries: List<LedgerEntry>,
        totalBilled: Double,
        totalPaid: Double,
        balance: Double,
        company: CompanyProfileEntity
    ) {
        try {
            val pdfFile = PdfInvoiceGenerator.generateLedgerPdf(
                context, customer, ledgerEntries, totalBilled, totalPaid, balance, company
            )
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val summaryText = buildLedgerCaption(customer, ledgerEntries, totalBilled, totalPaid, balance, company)
            sharePdfDirect(
                context = context,
                pdfUri = uri,
                caption = summaryText,
                targetPackage = "com.whatsapp.w4b",
                appName = "WhatsApp Business",
                chooserTitle = "Share Statement - ${customer.name}"
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Could not generate Ledger PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares customer ledger statement via WhatsApp (kept for backward compatibility).
     */
    fun shareLedgerWhatsApp(
        context: Context,
        customer: CustomerEntity,
        ledgerEntries: List<LedgerEntry>,
        totalBilled: Double,
        totalPaid: Double,
        balance: Double,
        company: CompanyProfileEntity
    ) {
        shareLedgerPdfWhatsApp(context, customer, ledgerEntries, totalBilled, totalPaid, balance, company)
    }

    /**
     * Shares customer ledger text summary via Android Chooser
     */
    fun shareLedgerTextGeneral(
        context: Context,
        customer: CustomerEntity,
        ledgerEntries: List<LedgerEntry>,
        totalBilled: Double,
        totalPaid: Double,
        balance: Double,
        company: CompanyProfileEntity
    ) {
        val summaryText = buildLedgerCaption(customer, ledgerEntries, totalBilled, totalPaid, balance, company)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, summaryText)
            putExtra(Intent.EXTRA_SUBJECT, "Statement - ${customer.name}")
        }
        context.startActivity(Intent.createChooser(intent, "Share Statement Summary via"))
    }

    private fun buildLedgerCaption(
        customer: CustomerEntity,
        ledgerEntries: List<LedgerEntry>,
        totalBilled: Double,
        totalPaid: Double,
        balance: Double,
        company: CompanyProfileEntity
    ): String {
        val sb = StringBuilder()
        sb.appendLine("📊 *CUSTOMER ACCOUNT STATEMENT*")
        sb.appendLine("*${company.businessName}*")
        sb.appendLine("📞 Phone: ${company.mobile}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("👤 *Customer:* ${customer.name}")
        if (customer.mobile.isNotBlank()) sb.appendLine("📱 Mobile: ${customer.mobile}")
        sb.appendLine("📅 *Date:* ${DimensionCalculator.formatDate(System.currentTimeMillis())}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("📜 *RECENT TRANSACTIONS:*")

        var running = 0.0
        ledgerEntries.takeLast(10).forEach { entry ->
            running += (entry.debitAmount - entry.creditAmount)
            val dateStr = DimensionCalculator.formatDate(entry.dateMillis)
            if (entry.debitAmount > 0) {
                sb.appendLine("▫️ $dateStr: ${entry.description} => +₹${String.format(Locale.US, "%.2f", entry.debitAmount)}")
            } else {
                sb.appendLine("▫️ $dateStr: ${entry.description} => -₹${String.format(Locale.US, "%.2f", entry.creditAmount)} [Paid]")
            }
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("📈 *Total Billed:* ₹${String.format(Locale.US, "%.2f", totalBilled)}")
        sb.appendLine("💳 *Total Received:* ₹${String.format(Locale.US, "%.2f", totalPaid)}")
        sb.appendLine(
            "⚠️ *CURRENT BALANCE DUE:* *₹${String.format(Locale.US, "%.2f", balance)}*"
        )
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("🏦 *Bank Details:* ${company.bankName} | A/C: ${company.accountNo} | IFSC: ${company.ifscCode}")
        sb.appendLine("Please clear the balance at your earliest convenience. Thank you!")
        return sb.toString()
    }

    private fun shareToWhatsAppOrGeneral(context: Context, message: String, mobileNumber: String) {
        try {
            val cleanPhone = mobileNumber.replace(Regex("[^0-9]"), "")
            val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = if (formattedPhone.isNotBlank()) {
                    Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=" + Uri.encode(message))
                } else {
                    Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(message))
                }
                setPackage("com.whatsapp")
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Try WhatsApp Business or Generic Intent
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Share Bill"))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
