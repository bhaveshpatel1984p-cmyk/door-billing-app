package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.db.BillWithItems
import com.example.data.db.CompanyProfileEntity
import com.example.data.db.CustomerEntity
import com.example.data.db.LedgerEntry

object ShareHelper {

    /**
     * Shares invoice details directly via WhatsApp or system share.
     */
    fun shareInvoiceWhatsApp(
        context: Context,
        billWithItems: BillWithItems,
        company: CompanyProfileEntity,
        targetMobile: String? = null
    ) {
        val bill = billWithItems.bill
        val items = billWithItems.items
        val totalSqFt = items.sumOf { it.sqFt }
        val totalQty = items.sumOf { it.qty }

        val sb = StringBuilder()
        sb.appendLine("🧾 *TAX INVOICE / BILL*")
        sb.appendLine("*${company.businessName}*")
        sb.appendLine("📞 ${company.mobile} | GST: ${company.gstNo}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("👤 *Customer:* ${bill.customerName}")
        if (bill.customerMobile.isNotBlank()) sb.appendLine("📱 Mobile: ${bill.customerMobile}")
        sb.appendLine("📄 *Invoice No:* ${bill.invoiceNo}")
        sb.appendLine("📅 *Date:* ${DimensionCalculator.formatDate(bill.dateMillis)}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("🚪 *ITEMS DETAILS (${bill.dimensionUnit}):*")

        items.forEachIndexed { index, item ->
            sb.appendLine(
                "${index + 1}. *${item.particular}*" +
                        "\n   Size: ${DimensionCalculator.formatDimension(item.height)} x ${DimensionCalculator.formatDimension(item.width)} | Qty: ${item.qty}" +
                        "\n   Area: ${String.format(java.util.Locale.US, "%.2f", item.sqFt)} Sq.Ft @ ₹${String.format(java.util.Locale.US, "%.2f", item.rate)} = *₹${String.format(java.util.Locale.US, "%.2f", item.amount)}*"
            )
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("📦 *Total Doors:* $totalQty")
        sb.appendLine("📐 *Total Area:* ${String.format(java.util.Locale.US, "%.2f", totalSqFt)} Sq.Ft")
        sb.appendLine("💵 *Sub Total:* ₹${String.format(java.util.Locale.US, "%.2f", bill.subTotal)}")

        if (bill.isGstIncluded && bill.taxRate > 0) {
            val halfRate = bill.taxRate / 2.0
            sb.appendLine("🏛️ *CGST ($halfRate%):* ₹${String.format(java.util.Locale.US, "%.2f", bill.cgstAmount)}")
            sb.appendLine("🏛️ *SGST ($halfRate%):* ₹${String.format(java.util.Locale.US, "%.2f", bill.sgstAmount)}")
        }

        if (bill.discountAmount > 0) {
            sb.appendLine("🏷️ *Discount:* -₹${String.format(java.util.Locale.US, "%.2f", bill.discountAmount)}")
        }

        sb.appendLine("💰 *GRAND TOTAL:* *₹${String.format(java.util.Locale.US, "%.2f", bill.grandTotal)}*")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("🏦 *Bank Details for Payment:*")
        sb.appendLine("Bank: ${company.bankName}")
        sb.appendLine("A/C: ${company.accountNo}")
        sb.appendLine("IFSC: ${company.ifscCode}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("Thank you for your business! 🙏")

        shareToWhatsAppOrGeneral(context, sb.toString(), targetMobile ?: bill.customerMobile)
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
