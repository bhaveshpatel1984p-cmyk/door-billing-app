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

object ShareHelper {

    /**
     * Generates and Shares invoice PDF directly via WhatsApp or system share.
     */
    fun shareInvoiceWhatsApp(
        context: Context,
        billWithItems: BillWithItems,
        company: CompanyProfileEntity,
        targetMobile: String? = null
    ) {
        val bill = billWithItems.bill
        try {
            // Generate standard PDF file
            val pdfFile = PdfInvoiceGenerator.generateInvoicePdf(context, billWithItems, company)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val caption = buildString {
                appendLine("🧾 *TAX INVOICE / BILL*")
                appendLine("*${company.businessName}*")
                appendLine("📄 *Invoice No:* ${bill.invoiceNo}")
                appendLine("👤 *Customer:* ${bill.customerName}")
                appendLine("💰 *Grand Total:* ₹${String.format(java.util.Locale.US, "%.2f", bill.grandTotal)}")
                appendLine("Please find attached official PDF Tax Invoice.")
                appendLine("Thank you for your business! 🙏")
            }

            sharePdfToWhatsAppOrGeneral(context, uri, caption)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sharePdfToWhatsAppOrGeneral(context: Context, pdfUri: Uri, caption: String) {
        try {
            val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.whatsapp")
            }

            try {
                context.startActivity(whatsappIntent)
            } catch (e1: Exception) {
                // Try WhatsApp Business
                try {
                    val w4bIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, pdfUri)
                        putExtra(Intent.EXTRA_TEXT, caption)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setPackage("com.whatsapp.w4b")
                    }
                    context.startActivity(w4bIntent)
                } catch (e2: Exception) {
                    // Fallback to generic chooser
                    val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, pdfUri)
                        putExtra(Intent.EXTRA_TEXT, caption)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(chooserIntent, "Share PDF Invoice"))
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares customer ledger statement via WhatsApp.
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
        val sb = StringBuilder()
        sb.appendLine("📊 *CUSTOMER ACCOUNT LEDGER*")
        sb.appendLine("*${company.businessName}*")
        sb.appendLine("📞 ${company.mobile}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("👤 *Customer:* ${customer.name}")
        if (customer.mobile.isNotBlank()) sb.appendLine("📱 Mobile: ${customer.mobile}")
        sb.appendLine("📅 *Statement Date:* ${DimensionCalculator.formatDate(System.currentTimeMillis())}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("📜 *TRANSACTIONS:*")

        var running = 0.0
        ledgerEntries.takeLast(10).forEach { entry ->
            running += (entry.debitAmount - entry.creditAmount)
            val dateStr = DimensionCalculator.formatDate(entry.dateMillis)
            if (entry.debitAmount > 0) {
                sb.appendLine("▫️ $dateStr: ${entry.description} => +₹${String.format(java.util.Locale.US, "%.2f", entry.debitAmount)}")
            } else {
                sb.appendLine("▫️ $dateStr: ${entry.description} => -₹${String.format(java.util.Locale.US, "%.2f", entry.creditAmount)} [Received]")
            }
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("📈 *Total Billed:* ₹${String.format(java.util.Locale.US, "%.2f", totalBilled)}")
        sb.appendLine("💳 *Total Received:* ₹${String.format(java.util.Locale.US, "%.2f", totalPaid)}")
        sb.appendLine(
            "⚠️ *CURRENT BALANCE DUE:* *₹${String.format(java.util.Locale.US, "%.2f", balance)}*"
        )
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("🏦 *Bank Details:* ${company.bankName} | A/C: ${company.accountNo} | IFSC: ${company.ifscCode}")
        sb.appendLine("Please clear the outstanding balance at the earliest. Thank you!")

        shareToWhatsAppOrGeneral(context, sb.toString(), customer.mobile)
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

            // Verify if WhatsApp package is installed or use browser/chooser
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
