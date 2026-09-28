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
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHelper {

    private fun escapeCsv(value: Any?): String {
        if (value == null) return "\"\""
        val str = value.toString().replace("\"", "\"\"")
        return "\"$str\""
    }

    /**
     * Exports Customer Ledger to a CSV file (opens in Microsoft Excel, Google Sheets, etc.)
     */
    fun exportCustomerLedgerCsv(
        context: Context,
        customer: CustomerEntity,
        ledgerEntries: List<LedgerEntry>,
        totalBilled: Double,
        totalPaid: Double,
        balance: Double,
        company: CompanyProfileEntity,
        periodLabel: String? = null
    ): Uri? {
        return try {
            val fileName = "Ledger_${customer.name.replace(Regex("[^a-zA-Z0-9]"), "_")}_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            writer.appendLine("${escapeCsv(company.businessName)},${escapeCsv("Customer Account Statement")}")
            writer.appendLine("${escapeCsv("Customer Name:")},${escapeCsv(customer.name)}")
            writer.appendLine("${escapeCsv("Mobile:")},${escapeCsv(customer.mobile)}")
            writer.appendLine("${escapeCsv("GSTIN:")},${escapeCsv(customer.gstNo)}")
            if (!periodLabel.isNullOrBlank()) {
                writer.appendLine("${escapeCsv("Statement Period:")},${escapeCsv(periodLabel)}")
            }
            writer.appendLine("${escapeCsv("Date:")},${escapeCsv(SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US).format(Date()))}")
            writer.appendLine("")

            // Table Header
            writer.appendLine("Date,Type,Reference / Invoice No,Description,Debit (+Billed),Credit (-Received),Running Balance")

            var running = 0.0
            ledgerEntries.forEach { entry ->
                running += (entry.debitAmount - entry.creditAmount)
                val dateStr = DimensionCalculator.formatDate(entry.dateMillis)
                val typeStr = when (entry) {
                    is LedgerEntry.OpeningBalanceEntry -> "Opening Due"
                    is LedgerEntry.BillEntry -> if (entry.billWithItems.bill.isQuotation) "Estimate" else "Tax Invoice"
                    is LedgerEntry.PaymentRecord -> "Payment Received"
                }
                val refStr = when (entry) {
                    is LedgerEntry.OpeningBalanceEntry -> "OPN"
                    is LedgerEntry.BillEntry -> entry.invoiceNo
                    is LedgerEntry.PaymentRecord -> entry.payment.referenceNo.ifBlank { "PAY-${entry.payment.id}" }
                }
                val descStr = entry.description
                val debitStr = if (entry.debitAmount > 0) String.format(Locale.US, "%.2f", entry.debitAmount) else "0.00"
                val creditStr = if (entry.creditAmount > 0) String.format(Locale.US, "%.2f", entry.creditAmount) else "0.00"
                val runningStr = String.format(Locale.US, "%.2f", running)

                writer.appendLine(
                    listOf(
                        escapeCsv(dateStr),
                        escapeCsv(typeStr),
                        escapeCsv(refStr),
                        escapeCsv(descStr),
                        debitStr,
                        creditStr,
                        runningStr
                    ).joinToString(",")
                )
            }

            writer.appendLine("")
            writer.appendLine(",,,Summary Totals,,")
            writer.appendLine(",,,Total Billed:,${String.format(Locale.US, "%.2f", totalBilled)},")
            writer.appendLine(",,,Total Received:,,${String.format(Locale.US, "%.2f", totalPaid)}")
            writer.appendLine(",,,Current Net Due:,,,${String.format(Locale.US, "%.2f", balance)}")

            writer.flush()
            writer.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * Shares generated CSV file via Android share sheet
     */
    fun shareCsvFile(context: Context, uri: Uri, title: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title (Excel compatible file)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share $title via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share CSV: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
