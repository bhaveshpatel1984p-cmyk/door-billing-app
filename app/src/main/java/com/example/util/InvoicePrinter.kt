package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.db.BillItemEntity
import com.example.data.db.BillWithItems
import com.example.data.db.CompanyProfileEntity
import com.example.data.db.CustomerEntity
import com.example.data.db.LedgerEntry

object InvoicePrinter {

    /**
     * Prints or Saves Invoice as PDF using Android PrintManager.
     */
    fun printInvoice(
        context: Context,
        billWithItems: BillWithItems,
        company: CompanyProfileEntity
    ) {
        val htmlContent = generateInvoiceHtml(billWithItems, company)
        printHtml(context, htmlContent, "Invoice_${billWithItems.bill.invoiceNo.replace("/", "_")}")
    }

    /**
     * Prints or Saves Customer Ledger as PDF using Android PrintManager.
     */
    fun printCustomerLedger(
        context: Context,
        customer: CustomerEntity,
        ledgerEntries: List<LedgerEntry>,
        totalBilled: Double,
        totalPaid: Double,
        balance: Double,
        company: CompanyProfileEntity
    ) {
        val htmlContent = generateLedgerHtml(customer, ledgerEntries, totalBilled, totalPaid, balance, company)
        printHtml(context, htmlContent, "Ledger_${customer.name.replace(" ", "_")}")
    }

    private fun printHtml(context: Context, htmlContent: String, jobName: String) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null && view != null) {
                    val printAdapter = view.createPrintDocumentAdapter(jobName)
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("res1", "Default", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager.print(jobName, printAdapter, attributes)
                }
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
    }

    fun generateInvoiceHtml(
        billWithItems: BillWithItems,
        company: CompanyProfileEntity
    ): String {
        val bill = billWithItems.bill
        val items = billWithItems.items
        val totalSqFt = items.sumOf { it.sqFt }
        val totalQty = items.sumOf { it.qty }
        val amountInWords = DimensionCalculator.convertToIndianCurrencyWords(bill.grandTotal)

        val itemRows = items.joinToString("") { item ->
            """
            <tr>
                <td style="text-align:center; padding:6px 4px; border:1px solid #333;">${item.slNo}</td>
                <td style="padding:6px 6px; border:1px solid #333; font-weight:500;">${item.particular}</td>
                <td style="text-align:center; padding:6px 4px; border:1px solid #333;">${item.hsnSac}</td>
                <td style="text-align:right; padding:6px 4px; border:1px solid #333;">${DimensionCalculator.formatDimension(item.height)}</td>
                <td style="text-align:right; padding:6px 4px; border:1px solid #333;">${DimensionCalculator.formatDimension(item.width)}</td>
                <td style="text-align:center; padding:6px 4px; border:1px solid #333; font-weight:bold;">${item.qty}</td>
                <td style="text-align:right; padding:6px 4px; border:1px solid #333; font-weight:bold; background-color:#f0f9ff;">${String.format(java.util.Locale.US, "%.2f", item.sqFt)}</td>
                <td style="text-align:right; padding:6px 4px; border:1px solid #333;">${String.format(java.util.Locale.US, "%.2f", item.rate)}</td>
                <td style="text-align:right; padding:6px 6px; border:1px solid #333; font-weight:bold;">₹${String.format(java.util.Locale.US, "%.2f", item.amount)}</td>
            </tr>
            """.trimIndent()
        }

        // Base64 Logo representation so WebView can render offline without file permission issues
        val logoBase64 = try {
            if (!company.logoUri.isNullOrBlank()) {
                val f = java.io.File(company.logoUri)
                if (f.exists()) {
                    android.util.Base64.encodeToString(f.readBytes(), android.util.Base64.NO_WRAP)
                } else null
            } else null
        } catch (e: Exception) {
            null
        }

        val logoHtml = if (!logoBase64.isNullOrBlank()) {
            """<img src="data:image/png;base64,$logoBase64" style="max-height: 65px; max-width: 140px; object-fit: contain; margin-bottom: 4px; margin-right: 10px;" alt="Logo" />"""
        } else if (!company.logoUri.isNullOrBlank()) {
            """<img src="${company.logoUri}" style="max-height: 65px; max-width: 140px; object-fit: contain; margin-bottom: 4px; margin-right: 10px;" alt="Logo" />"""
        } else {
            """
            <div style="display:inline-block; vertical-align:middle; margin-right:10px;">
                <svg width="64" height="44" viewBox="0 0 512 360" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M 220,40 L 320,40 C 420,40 450,95 450,170 C 450,245 420,300 320,300 L 200,300 L 220,40 Z" fill="#0C1A30"/>
                    <path d="M 255,100 L 315,100 C 375,100 395,135 395,170 C 395,205 375,240 315,240 L 235,240 Z" fill="#FFFFFF"/>
                    <polygon points="80,300 120,40 185,40 145,300" fill="#E11D2A"/>
                    <polygon points="120,40 205,40 270,300 185,300" fill="#E11D2A"/>
                    <polygon points="190,300 250,100 305,115 250,300" stroke="#FFFFFF" stroke-width="12" fill="#E11D2A"/>
                </svg>
            </div>
            """.trimIndent()
        }

        val gstRows = if (bill.isGstIncluded && bill.taxRate > 0) {
            val halfRate = bill.taxRate / 2.0
            """
            <tr>
                <td colspan="7" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold;">CGST ($halfRate%):</td>
                <td colspan="2" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold;">₹${String.format(java.util.Locale.US, "%.2f", bill.cgstAmount)}</td>
            </tr>
            <tr>
                <td colspan="7" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold;">SGST ($halfRate%):</td>
                <td colspan="2" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold;">₹${String.format(java.util.Locale.US, "%.2f", bill.sgstAmount)}</td>
            </tr>
            """.trimIndent()
        } else ""

        val otherChargesRow = if (bill.otherCharges > 0) {
            val chargeName = bill.otherChargesDescription.ifBlank { "Cutting Charges" }
            """
            <tr>
                <td colspan="7" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold; color:#0369A1;">$chargeName:</td>
                <td colspan="2" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold; color:#0369A1;">+₹${String.format(java.util.Locale.US, "%.2f", bill.otherCharges)}</td>
            </tr>
            """.trimIndent()
        } else ""

        val discountRow = if (bill.discountAmount > 0) {
            """
            <tr>
                <td colspan="7" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold;">Discount:</td>
                <td colspan="2" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold; color:#dc2626;">-₹${String.format(java.util.Locale.US, "%.2f", bill.discountAmount)}</td>
            </tr>
            """.trimIndent()
        } else ""

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Tax Invoice - ${bill.invoiceNo}</title>
            <style>
                @page { size: A4; margin: 12mm; }
                body { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 11px; color: #111; margin: 0; padding: 0; }
                .container { border: 2px solid #0369A1; padding: 0; box-sizing: border-box; }
                .header-table { width: 100%; border-collapse: collapse; border-bottom: 2px solid #0369A1; }
                .header-table td { padding: 8px 12px; }
                .title-badge { background-color: #0369A1; color: white; padding: 4px 12px; font-weight: bold; font-size: 13px; text-transform: uppercase; letter-spacing: 1px; }
                .meta-table { width: 100%; border-collapse: collapse; border-bottom: 1px solid #333; }
                .meta-table td { width: 50%; vertical-align: top; padding: 8px 12px; }
                .items-table { width: 100%; border-collapse: collapse; }
                .items-table th { background-color: #0369A1; color: white; padding: 6px 4px; font-size: 10px; text-align: center; border: 1px solid #0369A1; }
                .terms-table { width: 100%; border-collapse: collapse; border-top: 1px solid #333; }
                .terms-table td { width: 50%; vertical-align: top; padding: 8px 12px; }
                .total-row { background-color: #f8fafc; font-weight: bold; }
                .grand-total-box { background-color: #0369A1; color: white; font-size: 14px; font-weight: bold; padding: 6px 10px; }
            </style>
        </head>
        <body>
            <div class="container">
                <!-- Top Title -->
                <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #0369A1; background-color:#f0f9ff; padding:6px 12px;">
                    <div style="font-weight:bold; color:#0369A1; font-size:13px;">TAX INVOICE / BILL OF SUPPLY</div>
                    <div style="font-size:10px; color:#555;">(Door Manufacturing & Joinery Billing)</div>
                </div>

                <!-- Company Header -->
                <table class="header-table">
                    <tr>
                        <td style="width:65%;">
                            <div style="display:flex; align-items:center;">
                                $logoHtml
                                <div>
                                    <h1 style="margin:0; font-size:18px; color:#0369A1; text-transform:uppercase; letter-spacing:0.5px;">${company.businessName}</h1>
                                    <div style="margin-top:2px; font-size:10.5px; color:#333;">${company.address}</div>
                                    <div style="margin-top:3px; font-size:10.5px;">
                                        <strong>GSTIN:</strong> ${company.gstNo} &nbsp;|&nbsp; <strong>PAN:</strong> ${company.pan}
                                    </div>
                                    <div style="font-size:10.5px; color:#333;">
                                        <strong>Mobile:</strong> ${company.mobile} &nbsp;|&nbsp; <strong>Email:</strong> ${company.email.ifBlank { "N/A" }}
                                    </div>
                                    <div style="font-size:10.5px; color:#333;">
                                        <strong>State:</strong> ${company.state} (${company.stateCode})
                                    </div>
                                </div>
                            </div>
                        </td>
                        <td style="width:35%; text-align:right; border-left:1px solid #ddd; background-color:#fafafa;">
                            <table style="width:100%; font-size:11px; text-align:left;">
                                <tr>
                                    <td><strong>Invoice No:</strong></td>
                                    <td style="font-weight:bold; color:#0369A1; font-size:12px;">${bill.invoiceNo}</td>
                                </tr>
                                <tr>
                                    <td><strong>Date:</strong></td>
                                    <td style="font-weight:bold;">${DimensionCalculator.formatDate(bill.dateMillis)}</td>
                                </tr>
                                <tr>
                                    <td><strong>Unit:</strong></td>
                                    <td>${bill.dimensionUnit} Dimension</td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>

                <!-- Customer Details -->
                <table class="meta-table">
                    <tr>
                        <td style="background-color:#fff;">
                            <div style="font-weight:bold; color:#0369A1; margin-bottom:4px; border-bottom:1px solid #eee; padding-bottom:2px;">BILLED TO (CUSTOMER DETAILS):</div>
                            <div style="font-size:13px; font-weight:bold; color:#111;">${bill.customerName}</div>
                            <div style="margin-top:2px;"><strong>Address:</strong> ${bill.customerAddress.ifBlank { "N/A" }}</div>
                            <div style="margin-top:2px;"><strong>Mobile:</strong> ${bill.customerMobile.ifBlank { "N/A" }}</div>
                            <div style="margin-top:2px;"><strong>GSTIN:</strong> ${bill.customerGstNo.ifBlank { "Unregistered" }}</div>
                        </td>
                        <td style="border-left:1px solid #ddd; background-color:#fafafa;">
                            <div style="font-weight:bold; color:#0369A1; margin-bottom:4px; border-bottom:1px solid #eee; padding-bottom:2px;">PAYMENT & DELIVERY SUMMARY:</div>
                            <table style="width:100%; font-size:11px;">
                                <tr><td><strong>Total Quantity:</strong></td><td><strong>$totalQty Doors</strong></td></tr>
                                <tr><td><strong>Total Area:</strong></td><td><strong>${String.format(java.util.Locale.US, "%.2f", totalSqFt)} Sq.Ft</strong></td></tr>
                                <tr><td><strong>Payment Status:</strong></td><td style="font-weight:bold; color:${if (bill.paidAmount >= bill.grandTotal) "#16a34a" else "#d97706"};">${if (bill.paidAmount >= bill.grandTotal) "PAID" else if (bill.paidAmount > 0) "PARTIAL" else "UNPAID"}</td></tr>
                            </table>
                        </td>
                    </tr>
                </table>

                <!-- Items Table -->
                <table class="items-table">
                    <thead>
                        <tr>
                            <th style="width:5%;">Sl.</th>
                            <th style="width:29%;">Particular (Door Description)</th>
                            <th style="width:10%;">HSN/SAC</th>
                            <th style="width:8%;">High (${bill.dimensionUnit.take(2)})</th>
                            <th style="width:8%;">Width (${bill.dimensionUnit.take(2)})</th>
                            <th style="width:6%;">Qty</th>
                            <th style="width:11%;">Sq.Ft</th>
                            <th style="width:10%;">Rate/SqFt</th>
                            <th style="width:13%;">Amount (₹)</th>
                        </tr>
                    </thead>
                    <tbody>
                        $itemRows
                        <!-- Totals Breakdown -->
                        <tr class="total-row">
                            <td colspan="5" style="border:1px solid #333; text-align:right; padding:5px 8px; font-weight:bold;">Sub Total (${totalQty} Qty | ${String.format(java.util.Locale.US, "%.2f", totalSqFt)} Sq.Ft):</td>
                            <td style="border:1px solid #333; text-align:center; padding:5px 4px; font-weight:bold;">$totalQty</td>
                            <td style="border:1px solid #333; text-align:right; padding:5px 4px; font-weight:bold;">${String.format(java.util.Locale.US, "%.2f", totalSqFt)}</td>
                            <td style="border:1px solid #333;"></td>
                            <td style="border:1px solid #333; text-align:right; padding:5px 6px; font-weight:bold;">₹${String.format(java.util.Locale.US, "%.2f", bill.subTotal)}</td>
                        </tr>
                        $gstRows
                        $otherChargesRow
                        $discountRow
                        <tr>
                            <td colspan="7" style="border:1px solid #333; text-align:right; padding:6px 8px; font-size:12px; font-weight:bold; background-color:#e0f2fe; color:#0369A1;">GRAND TOTAL:</td>
                            <td colspan="2" style="border:1px solid #333; text-align:right; padding:6px 8px; font-size:13px; font-weight:bold; background-color:#0369A1; color:white;">₹${String.format(java.util.Locale.US, "%.2f", bill.grandTotal)}</td>
                        </tr>
                    </tbody>
                </table>

                <!-- Amount in words -->
                <div style="padding:6px 12px; background-color:#fafafa; border-bottom:1px solid #333; font-size:11px;">
                    <strong>Amount in Words:</strong> <span style="font-style:italic; font-weight:bold; color:#1e293b;">$amountInWords</span>
                </div>

                <!-- Terms, Bank Details, Signature -->
                <table class="terms-table">
                    <tr>
                        <td style="width:55%;">
                            <div style="font-weight:bold; color:#0369A1; margin-bottom:3px;">BANK ACCOUNT DETAILS:</div>
                            <div style="font-size:10.5px; line-height:1.4;">
                                <strong>Bank:</strong> ${company.bankName}<br/>
                                <strong>A/C No:</strong> ${company.accountNo}<br/>
                                <strong>IFSC Code:</strong> ${company.ifscCode}<br/>
                            </div>
                            <div style="margin-top:6px; font-weight:bold; color:#0369A1; font-size:10px;">DECLARATION & TERMS:</div>
                            <div style="font-size:9.5px; color:#555; line-height:1.3;">${company.declaration}</div>
                        </td>
                        <td style="width:45%; border-left:1px solid #ddd; text-align:center; vertical-align:bottom; padding-bottom:16px;">
                            <div style="font-size:10px; color:#666; margin-bottom:45px;">For <strong>${company.businessName}</strong></div>
                            <div style="border-top:1px dashed #777; width:75%; margin:0 auto; padding-top:4px; font-weight:bold; font-size:10px;">
                                AUTHORIZED SIGNATORY
                            </div>
                        </td>
                    </tr>
                </table>

                <!-- Bottom Jurisdiction Banner (Requirement 4) -->
                <div style="text-align:center; padding:6px 12px; background-color:#f1f5f9; border-top:1px solid #333; font-size:10px; font-weight:bold; color:#1e293b; letter-spacing:0.5px;">
                    ${DimensionCalculator.formatJurisdictionClause(company.jurisdiction)}
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    fun generateLedgerHtml(
        customer: CustomerEntity,
        ledgerEntries: List<LedgerEntry>,
        totalBilled: Double,
        totalPaid: Double,
        balance: Double,
        company: CompanyProfileEntity
    ): String {
        var runningBal = 0.0
        val rows = ledgerEntries.joinToString("") { entry ->
            runningBal += (entry.debitAmount - entry.creditAmount)
            val debitStr = if (entry.debitAmount > 0) "₹${String.format(java.util.Locale.US, "%.2f", entry.debitAmount)}" else "-"
            val creditStr = if (entry.creditAmount > 0) "₹${String.format(java.util.Locale.US, "%.2f", entry.creditAmount)}" else "-"
            """
            <tr>
                <td style="text-align:center; padding:6px; border:1px solid #ccc;">${DimensionCalculator.formatDate(entry.dateMillis)}</td>
                <td style="padding:6px; border:1px solid #ccc; font-weight:500;">${entry.description}</td>
                <td style="text-align:right; padding:6px; border:1px solid #ccc; color:#0369A1; font-weight:bold;">$debitStr</td>
                <td style="text-align:right; padding:6px; border:1px solid #ccc; color:#16a34a; font-weight:bold;">$creditStr</td>
                <td style="text-align:right; padding:6px; border:1px solid #ccc; font-weight:bold; background-color:#f8fafc;">₹${String.format(java.util.Locale.US, "%.2f", runningBal)}</td>
            </tr>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Customer Ledger - ${customer.name}</title>
            <style>
                @page { size: A4; margin: 15mm; }
                body { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 11px; color: #111; margin: 0; padding: 0; }
                .container { border: 2px solid #0369A1; padding: 0; }
                .header { background-color:#0369A1; color:white; padding:12px; text-align:center; }
                .info-table { width:100%; border-collapse:collapse; border-bottom:1px solid #333; }
                .info-table td { padding:8px 12px; vertical-align:top; }
                .ledger-table { width:100%; border-collapse:collapse; }
                .ledger-table th { background-color:#f0f9ff; color:#0369A1; padding:8px 6px; border:1px solid #0369A1; text-align:center; font-size:10.5px; }
                .summary-box { background-color:#f8fafc; padding:10px 14px; display:flex; justify-content:space-between; border-top:2px solid #0369A1; font-size:12px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h2 style="margin:0; font-size:18px; text-transform:uppercase;">${company.businessName}</h2>
                    <div style="font-size:11px; margin-top:2px;">${company.address} | GST: ${company.gstNo} | Mobile: ${company.mobile}</div>
                    <div style="font-size:13px; font-weight:bold; margin-top:6px; letter-spacing:1px; background-color:rgba(255,255,255,0.2); padding:3px 0; border-radius:3px;">
                        CUSTOMER ACCOUNT STATEMENT / LEDGER
                    </div>
                </div>

                <table class="info-table">
                    <tr>
                        <td style="width:60%;">
                            <div style="font-weight:bold; color:#0369A1;">ACCOUNT OF:</div>
                            <div style="font-size:14px; font-weight:bold; margin-top:2px;">${customer.name}</div>
                            <div>Mobile: ${customer.mobile.ifBlank { "N/A" }}</div>
                            <div>Address: ${customer.address.ifBlank { "N/A" }}</div>
                            <div>GSTIN: ${customer.gstNo.ifBlank { "Unregistered" }}</div>
                        </td>
                        <td style="width:40%; text-align:right; border-left:1px solid #ddd;">
                            <div><strong>Statement Date:</strong> ${DimensionCalculator.formatDate(System.currentTimeMillis())}</div>
                            <div style="margin-top:4px;"><strong>Total Bills:</strong> ₹${String.format(java.util.Locale.US, "%.2f", totalBilled)}</div>
                            <div><strong>Total Received:</strong> ₹${String.format(java.util.Locale.US, "%.2f", totalPaid)}</div>
                            <div style="font-size:13px; font-weight:bold; color:${if (balance > 0) "#dc2626" else "#16a34a"}; margin-top:4px;">
                                Outstanding: ₹${String.format(java.util.Locale.US, "%.2f", balance)}
                            </div>
                        </td>
                    </tr>
                </table>

                <table class="ledger-table">
                    <thead>
                        <tr>
                            <th style="width:15%;">Date</th>
                            <th style="width:45%;">Particulars / Description</th>
                            <th style="width:13%;">Debit (Bill ₹)</th>
                            <th style="width:13%;">Credit (Paid ₹)</th>
                            <th style="width:14%;">Balance (₹)</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                </table>

                <div class="summary-box">
                    <div><strong>Total Billed:</strong> ₹${String.format(java.util.Locale.US, "%.2f", totalBilled)}</div>
                    <div><strong>Total Paid:</strong> ₹${String.format(java.util.Locale.US, "%.2f", totalPaid)}</div>
                    <div style="color:${if (balance > 0) "#dc2626" else "#16a34a"}; font-weight:bold; font-size:13px;">
                        <strong>Net Due Balance:</strong> ₹${String.format(java.util.Locale.US, "%.2f", balance)}
                    </div>
                </div>

                <div style="padding:16px 12px; display:flex; justify-content:space-between; align-items:flex-end; font-size:10px; color:#555;">
                    <div>
                        <strong>Bank Details:</strong> ${company.bankName} | A/C: ${company.accountNo} | IFSC: ${company.ifscCode}
                    </div>
                    <div style="text-align:center;">
                        <div style="margin-bottom:30px;">For ${company.businessName}</div>
                        <div style="border-top:1px dashed #777; padding-top:2px;">Authorized Signatory</div>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
