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
import com.example.data.db.PaymentEntity
import java.util.Locale

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
        val totalWithOldBalance = bill.grandTotal + if (bill.previousBalance > 0.0) bill.previousBalance else 0.0
        val netPayableAmount = if (bill.netPayable > 0.0) bill.netPayable else totalWithOldBalance
        val finalRemainingDue = if (bill.paidAmount > 0.0) Math.max(0.0, netPayableAmount - bill.paidAmount) else netPayableAmount
        val amountInWords = DimensionCalculator.convertToIndianCurrencyWords(if (bill.paidAmount > 0.0 && finalRemainingDue > 0.0) finalRemainingDue else netPayableAmount)

        val itemRows = items.joinToString("") { item ->
            val partLines = item.particular.split("\n")
            val partHtml = if (partLines.size > 1 && partLines[1].isNotBlank()) {
                """
                <div style="font-weight:600; color:#0f172a;">${partLines[0].trim()}</div>
                <div style="font-size:10px; color:#475569; font-weight:600; margin-top:2px;">${partLines[1].trim()}</div>
                """.trimIndent()
            } else {
                """<div style="font-weight:500;">${item.particular}</div>"""
            }

            """
            <tr>
                <td style="text-align:center; padding:6px 4px; border:1px solid #333;">${item.slNo}</td>
                <td style="padding:5px 6px; border:1px solid #333;">$partHtml</td>
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

        val previousBalanceRow = if (bill.previousBalance > 0.0) {
            val paidRowHtml = if (bill.paidAmount > 0.0) {
                """
                <tr>
                    <td colspan="7" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold; color:#16a34a;">(-) Paid / Advance Received:</td>
                    <td colspan="2" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold; color:#16a34a;">-₹${String.format(java.util.Locale.US, "%.2f", bill.paidAmount)}</td>
                </tr>
                """.trimIndent()
            } else ""

            """
            <tr>
                <td colspan="7" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold; color:#0369A1;">Current Bill Grand Total:</td>
                <td colspan="2" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold; color:#0369A1;">₹${String.format(java.util.Locale.US, "%.2f", bill.grandTotal)}</td>
            </tr>
            <tr style="background-color:#fffbeb;">
                <td colspan="7" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold; color:#b45309;">(+) Previous Balance / Due Bal (Purana Baaki):</td>
                <td colspan="2" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold; color:#b45309;">+₹${String.format(java.util.Locale.US, "%.2f", bill.previousBalance)}</td>
            </tr>
            $paidRowHtml
            <tr>
                <td colspan="7" style="border:1px solid #333; text-align:right; padding:6px 8px; font-size:12px; font-weight:bold; background-color:#e0f2fe; color:#0369A1;">${if (bill.paidAmount > 0.0) "REMAINING DUE BALANCE:" else "TOTAL DUE / NET PAYABLE:"}</td>
                <td colspan="2" style="border:1px solid #333; text-align:right; padding:6px 8px; font-size:13px; font-weight:bold; background-color:#0369A1; color:white;">₹${String.format(java.util.Locale.US, "%.2f", finalRemainingDue)}</td>
            </tr>
            """.trimIndent()
        } else if (bill.paidAmount > 0.0) {
            """
            <tr>
                <td colspan="7" style="border:1px solid #333; text-align:right; padding:6px 8px; font-size:12px; font-weight:bold; background-color:#e0f2fe; color:#0369A1;">GRAND TOTAL:</td>
                <td colspan="2" style="border:1px solid #333; text-align:right; padding:6px 8px; font-size:13px; font-weight:bold; background-color:#0369A1; color:white;">₹${String.format(java.util.Locale.US, "%.2f", bill.grandTotal)}</td>
            </tr>
            <tr>
                <td colspan="7" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold; color:#16a34a;">(-) Paid / Received Amount:</td>
                <td colspan="2" style="border:1px solid #333; border-top:none; text-align:right; padding:4px 8px; font-weight:bold; color:#16a34a;">-₹${String.format(java.util.Locale.US, "%.2f", bill.paidAmount)}</td>
            </tr>
            <tr style="background-color:#fef2f2;">
                <td colspan="7" style="border:1px solid #333; text-align:right; padding:6px 8px; font-size:12px; font-weight:bold; color:#b91c1c;">REMAINING DUE:</td>
                <td colspan="2" style="border:1px solid #333; text-align:right; padding:6px 8px; font-size:13px; font-weight:bold; background-color:#b91c1c; color:white;">₹${String.format(java.util.Locale.US, "%.2f", finalRemainingDue)}</td>
            </tr>
            """.trimIndent()
        } else {
            """
            <tr>
                <td colspan="7" style="border:1px solid #333; text-align:right; padding:6px 8px; font-size:12px; font-weight:bold; background-color:#e0f2fe; color:#0369A1;">GRAND TOTAL:</td>
                <td colspan="2" style="border:1px solid #333; text-align:right; padding:6px 8px; font-size:13px; font-weight:bold; background-color:#0369A1; color:white;">₹${String.format(java.util.Locale.US, "%.2f", bill.grandTotal)}</td>
            </tr>
            """.trimIndent()
        }

        val qrBase64 = QrCodeHelper.getPaymentQrBase64(company, netPayableAmount)
        val qrHtml = if (!qrBase64.isNullOrBlank()) {
            """
            <td style="width:22%; border-left:1px solid #ddd; text-align:center; vertical-align:middle; padding:6px; background-color:#fafafa;">
                <div style="font-size:9px; font-weight:bold; color:#0369A1; margin-bottom:3px; letter-spacing:0.5px;">SCAN TO PAY UPI</div>
                <img src="data:image/png;base64,$qrBase64" style="width:82px; height:82px; object-fit:contain; border:1px solid #cbd5e1; padding:2px; border-radius:4px; background:#fff; display:inline-block;" alt="Payment QR Code" />
                <div style="font-size:7.5px; color:#475569; font-weight:bold; margin-top:2px;">PhonePe • GPay • Paytm</div>
            </td>
            """.trimIndent()
        } else ""
        val bankWidth = if (!qrBase64.isNullOrBlank()) "50%" else "55%"
        val signWidth = if (!qrBase64.isNullOrBlank()) "28%" else "45%"

        val rawCust = bill.customerName.trim()
        val parenMatch = Regex("^(.*?)\\s*\\((.*?)\\)$").find(rawCust)
        val (firmName, contactPerson) = when {
            parenMatch != null -> Pair(parenMatch.groupValues[1].trim(), parenMatch.groupValues[2].trim())
            rawCust.contains("\n") -> {
                val parts = rawCust.split("\n", limit = 2)
                Pair(parts[0].trim(), parts[1].trim())
            }
            else -> Pair(rawCust, null)
        }
        val customerHtml = if (!contactPerson.isNullOrBlank()) {
            """
            <div style="font-size:13px; font-weight:bold; color:#111;">$firmName</div>
            <div style="font-size:11px; font-weight:600; color:#334155; margin-top:2px;">Customer: $contactPerson</div>
            """.trimIndent()
        } else {
            """
            <div style="font-size:13px; font-weight:bold; color:#111;">$firmName</div>
            """.trimIndent()
        }

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
                .top-title-bar { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #0369A1; background-color: #DCEEF8; padding: 7px 12px; }
                .header-table { width: 100%; border-collapse: collapse; border-bottom: 2px solid #0369A1; }
                .header-table td { padding: 9px 12px; vertical-align: top; }
                .meta-table { width: 100%; border-collapse: collapse; border-bottom: 2px solid #0369A1; }
                .meta-table td { vertical-align: top; padding: 9px 12px; }
                .section-header-title { font-weight: bold; color: #0369A1; font-size: 11px; margin-bottom: 5px; border-bottom: 1.5px solid #CBD5E1; padding-bottom: 3px; letter-spacing: 0.3px; }
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
                <div class="top-title-bar">
                    <div style="font-weight:bold; color:#0369A1; font-size:13.5px; letter-spacing:0.5px;">${if (bill.isQuotation) "ESTIMATE / QUOTATION" else "TAX INVOICE / BILL OF SUPPLY"}</div>
                    <div style="font-size:11px; color:#475569; font-style:italic;">(Door Manufacturing & Joinery Billing)</div>
                </div>

                <!-- Company Header & Invoice Details Row -->
                <table class="header-table">
                    <tr>
                        <td style="width:62%; background-color:#EEF5F9;">
                            <div style="display:flex; align-items:center;">
                                $logoHtml
                                <div>
                                    <h1 style="margin:0; font-size:18px; color:#0369A1; text-transform:uppercase; letter-spacing:0.5px;">${company.businessName}</h1>
                                    <div style="margin-top:2px; font-size:10.5px; color:#333;">${company.address}${if (company.addressLine2.isNotBlank()) "<br/>" + company.addressLine2 else ""}</div>
                                    <div style="margin-top:3px; font-size:10.5px;">
                                        <strong>GSTIN:</strong> ${company.gstNo} &nbsp;|&nbsp; <strong>PAN:</strong> ${company.pan}
                                    </div>
                                    <div style="font-size:10.5px; color:#333;">
                                        <strong>Mobile:</strong> ${company.displayMobile} &nbsp;|&nbsp; <strong>Email:</strong> ${company.email.ifBlank { "N/A" }}
                                    </div>
                                    <div style="font-size:10.5px; color:#333;">
                                        <strong>State:</strong> ${company.state} (${company.stateCode})
                                    </div>
                                </div>
                            </div>
                        </td>
                        <td style="width:38%; border-left:2px solid #0369A1; background-color:#D3E3ED;">
                            <table style="width:100%; font-size:11px; text-align:left; border-collapse:collapse;">
                                <tr>
                                    <td style="width:36%; padding:3px 0; font-weight:bold; color:#334155;">${if (bill.isQuotation) "Quotation No:" else "Invoice No:"}</td>
                                    <td style="padding:3px 0; font-weight:bold; color:#0369A1; font-size:13px;">${bill.invoiceNo}</td>
                                </tr>
                                <tr>
                                    <td style="padding:3px 0; font-weight:bold; color:#334155;">Date:</td>
                                    <td style="padding:3px 0; font-weight:bold; color:#0F172A;">${DimensionCalculator.formatDate(bill.dateMillis)}</td>
                                </tr>
                                <tr>
                                    <td style="padding:3px 0; font-weight:bold; color:#334155; vertical-align:top;">Unit:</td>
                                    <td style="padding:3px 0; font-weight:bold; color:#0F172A;">
                                        ${bill.dimensionUnit}<br>
                                        <span style="font-weight:normal; font-size:10px; color:#475569;">Dimension</span>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>

                <!-- Customer Details & Summary Row -->
                <table class="meta-table">
                    <tr>
                        <td style="width:62%; background-color:#EEF5F9;">
                            <div class="section-header-title">BILLED TO (CUSTOMER DETAILS):</div>
                            $customerHtml
                            <div style="margin-top:3px; color:#333;"><strong>Address:</strong> ${bill.customerAddress.ifBlank { "N/A" }}</div>
                            <div style="margin-top:2px; color:#333;"><strong>Mobile:</strong> ${bill.customerMobile.ifBlank { "N/A" }}</div>
                            <div style="margin-top:2px; color:#333;"><strong>GSTIN:</strong> ${bill.customerGstNo.ifBlank { "Unregistered" }}</div>
                        </td>
                        <td style="width:38%; border-left:2px solid #0369A1; background-color:#D3E3ED;">
                            <div class="section-header-title">PAYMENT & DELIVERY SUMMARY:</div>
                            <table style="width:100%; font-size:11px; border-collapse:collapse;">
                                <tr>
                                    <td style="width:48%; padding:3px 0; font-weight:bold; color:#334155;">Total Quantity:</td>
                                    <td style="padding:3px 0; font-weight:bold; color:#0F172A;">$totalQty Doors</td>
                                </tr>
                                <tr>
                                    <td style="padding:3px 0; font-weight:bold; color:#334155;">Total Area:</td>
                                    <td style="padding:3px 0; font-weight:bold; color:#0F172A;">${String.format(java.util.Locale.US, "%.2f", totalSqFt)} Sq.Ft</td>
                                </tr>
                                <tr>
                                    <td style="padding:3px 0; font-weight:bold; color:#334155;">Payment Status:</td>
                                    <td style="padding:3px 0; font-weight:bold; color:${if (bill.paidAmount >= netPayableAmount) "#16a34a" else "#d97706"};">
                                        ${if (bill.paidAmount >= netPayableAmount) "PAID" else if (bill.paidAmount > 0) "PARTIAL" else "UNPAID"}
                                    </td>
                                </tr>
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
                        $previousBalanceRow
                    </tbody>
                </table>

                <!-- Amount in words -->
                <div style="padding:6px 12px; background-color:#fafafa; border-bottom:1px solid #333; font-size:11px;">
                    <strong>Amount in Words:</strong> <span style="font-style:italic; font-weight:bold; color:#1e293b;">$amountInWords</span>
                </div>

                <!-- Terms, Bank Details, Signature -->
                <table class="terms-table">
                    <tr>
                        <td style="width:$bankWidth;">
                            <div style="font-weight:bold; color:#0369A1; margin-bottom:3px;">BANK ACCOUNT DETAILS:</div>
                            <div style="font-size:10.5px; line-height:1.4;">
                                <strong>Bank:</strong> ${company.bankName}<br/>
                                <strong>A/C No:</strong> ${company.accountNo}<br/>
                                <strong>IFSC Code:</strong> ${company.ifscCode}<br/>
                                ${if (company.upiId.isNotBlank()) "<strong>UPI ID:</strong> ${company.upiId}<br/>" else ""}
                            </div>
                            <div style="margin-top:6px; font-weight:bold; color:#0369A1; font-size:10px;">DECLARATION & TERMS:</div>
                            <div style="font-size:9.5px; color:#555; line-height:1.3;">${company.declaration}</div>
                        </td>
                        $qrHtml
                        <td style="width:$signWidth; border-left:1px solid #ddd; text-align:center; vertical-align:bottom; padding-bottom:16px;">
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

        val ledgerQrBase64 = QrCodeHelper.getPaymentQrBase64(company, if (balance > 0) balance else null)
        val ledgerQrHtml = if (!ledgerQrBase64.isNullOrBlank()) {
            """
            <div style="text-align:center; margin:0 16px;">
                <img src="data:image/png;base64,$ledgerQrBase64" style="width:70px; height:70px; object-fit:contain; border:1px solid #cbd5e1; padding:2px; border-radius:4px; background:#fff;" alt="Payment QR" />
                <div style="font-size:8px; font-weight:bold; color:#0369A1; margin-top:2px;">Scan to Pay Due</div>
            </div>
            """.trimIndent()
        } else ""

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
                    <div style="font-size:11px; margin-top:2px;">${company.fullAddress} | GST: ${company.gstNo} | Mobile: ${company.displayMobile}</div>
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

                <div style="padding:14px 12px; display:flex; justify-content:space-between; align-items:center; border-top:1px solid #333; font-size:10px; color:#555;">
                    <div style="flex:1;">
                        <strong>Bank Details:</strong> ${company.bankName} | A/C: ${company.accountNo} | IFSC: ${company.ifscCode}
                        ${if (company.upiId.isNotBlank()) "<br/><strong>UPI ID:</strong> ${company.upiId}" else ""}
                    </div>
                    $ledgerQrHtml
                    <div style="text-align:center; min-width:140px;">
                        <div style="margin-bottom:28px;">For ${company.businessName}</div>
                        <div style="border-top:1px dashed #777; padding-top:2px; font-weight:bold;">Authorized Signatory</div>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    fun printSupplierLedger(
        context: Context,
        supplier: com.example.data.db.SupplierEntity,
        ledgerEntries: List<com.example.data.db.SupplierLedgerEntry>,
        totalPurchased: Double,
        totalPaid: Double,
        balance: Double,
        company: CompanyProfileEntity
    ) {
        val htmlContent = generateSupplierLedgerHtml(supplier, ledgerEntries, totalPurchased, totalPaid, balance, company)
        printHtml(context, htmlContent, "SupplierLedger_${supplier.name.replace(" ", "_")}")
    }

    fun generateSupplierLedgerHtml(
        supplier: com.example.data.db.SupplierEntity,
        ledgerEntries: List<com.example.data.db.SupplierLedgerEntry>,
        totalPurchased: Double,
        totalPaid: Double,
        balance: Double,
        company: CompanyProfileEntity
    ): String {
        var runningBal = 0.0
        val rows = ledgerEntries.joinToString("") { entry ->
            runningBal += (entry.creditAmount - entry.debitAmount)
            val debitStr = if (entry.debitAmount > 0) "₹${String.format(java.util.Locale.US, "%.2f", entry.debitAmount)}" else "-"
            val creditStr = if (entry.creditAmount > 0) "₹${String.format(java.util.Locale.US, "%.2f", entry.creditAmount)}" else "-"
            """
            <tr>
                <td style="text-align:center; padding:6px; border:1px solid #ccc;">${DimensionCalculator.formatDate(entry.dateMillis)}</td>
                <td style="padding:6px; border:1px solid #ccc; font-weight:500;">${entry.description}</td>
                <td style="text-align:right; padding:6px; border:1px solid #ccc; color:#0f766e; font-weight:bold;">$creditStr</td>
                <td style="text-align:right; padding:6px; border:1px solid #ccc; color:#16a34a; font-weight:bold;">$debitStr</td>
                <td style="text-align:right; padding:6px; border:1px solid #ccc; font-weight:bold; background-color:#f8fafc;">₹${String.format(java.util.Locale.US, "%.2f", runningBal)}</td>
            </tr>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Supplier Ledger - ${supplier.name}</title>
            <style>
                @page { size: A4; margin: 15mm; }
                body { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 11px; color: #111; margin: 0; padding: 0; }
                .container { border: 2px solid #0f766e; padding: 0; }
                .header { background-color:#0f766e; color:white; padding:12px; text-align:center; }
                .info-table { width:100%; border-collapse:collapse; border-bottom:1px solid #333; }
                .info-table td { padding:8px 12px; vertical-align:top; }
                .ledger-table { width:100%; border-collapse:collapse; }
                .ledger-table th { background-color:#f0fdfa; color:#0f766e; padding:8px 6px; border:1px solid #0f766e; text-align:center; font-size:10.5px; }
                .summary-box { background-color:#f8fafc; padding:10px 14px; display:flex; justify-content:space-between; border-top:2px solid #0f766e; font-size:12px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h2 style="margin:0; font-size:18px; text-transform:uppercase;">${company.businessName}</h2>
                    <div style="font-size:11px; margin-top:2px;">${company.fullAddress} | GST: ${company.gstNo} | Mobile: ${company.displayMobile}</div>
                    <div style="font-size:13px; font-weight:bold; margin-top:6px; letter-spacing:1px; background-color:rgba(255,255,255,0.2); padding:3px 0; border-radius:3px;">
                        SUPPLIER / VENDOR ACCOUNT STATEMENT / LEDGER
                    </div>
                </div>

                <table class="info-table">
                    <tr>
                        <td style="width:60%;">
                            <div style="font-weight:bold; color:#0f766e;">SUPPLIER / PARTY:</div>
                            <div style="font-size:14px; font-weight:bold; margin-top:2px;">${supplier.name}</div>
                            <div>Mobile: ${supplier.mobile.ifBlank { "N/A" }}</div>
                            <div>Address: ${supplier.address.ifBlank { "N/A" }}</div>
                            <div>GSTIN: ${supplier.gstNo.ifBlank { "Unregistered" }}</div>
                        </td>
                        <td style="width:40%; text-align:right; border-left:1px solid #ddd;">
                            <div><strong>Statement Date:</strong> ${DimensionCalculator.formatDate(System.currentTimeMillis())}</div>
                            <div style="margin-top:4px;"><strong>Total Purchased:</strong> ₹${String.format(java.util.Locale.US, "%.2f", totalPurchased)}</div>
                            <div><strong>Total Paid Out:</strong> ₹${String.format(java.util.Locale.US, "%.2f", totalPaid)}</div>
                            <div style="font-size:13px; font-weight:bold; color:${if (balance > 0) "#dc2626" else "#16a34a"}; margin-top:4px;">
                                Net Balance Due: ₹${String.format(java.util.Locale.US, "%.2f", balance)}
                            </div>
                        </td>
                    </tr>
                </table>

                <table class="ledger-table">
                    <thead>
                        <tr>
                            <th style="width:15%;">Date</th>
                            <th style="width:45%;">Particulars / Description</th>
                            <th style="width:13%;">Purchase (Credit ₹)</th>
                            <th style="width:13%;">Paid Out (Debit ₹)</th>
                            <th style="width:14%;">Balance Due (₹)</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                </table>

                <div class="summary-box">
                    <div><strong>Total Purchased:</strong> ₹${String.format(java.util.Locale.US, "%.2f", totalPurchased)}</div>
                    <div><strong>Total Paid Out:</strong> ₹${String.format(java.util.Locale.US, "%.2f", totalPaid)}</div>
                    <div style="color:${if (balance > 0) "#dc2626" else "#16a34a"}; font-weight:bold; font-size:13px;">
                        <strong>Net Due Balance:</strong> ₹${String.format(java.util.Locale.US, "%.2f", balance)}
                    </div>
                </div>

                <div style="padding:16px 12px; display:flex; justify-content:space-between; align-items:flex-end; font-size:10px; color:#555;">
                    <div>
                        Statement generated by <strong>${company.businessName}</strong>
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

    /**
     * Prints Delivery Challan / Gate Pass
     */
    fun printDeliveryChallan(
        context: Context,
        billWithItems: BillWithItems,
        company: CompanyProfileEntity,
        vehicleNo: String = "",
        transportName: String = ""
    ) {
        val htmlContent = generateDeliveryChallanHtml(billWithItems, company, vehicleNo, transportName)
        printHtml(context, htmlContent, "Challan_${billWithItems.bill.invoiceNo.replace("/", "_")}")
    }

    /**
     * Prints Payment Receipt / Money Voucher
     */
    fun printPaymentReceipt(
        context: Context,
        payment: PaymentEntity,
        customerName: String,
        customerMobile: String,
        company: CompanyProfileEntity,
        previousBalance: Double,
        remainingBalance: Double
    ) {
        val htmlContent = generatePaymentReceiptHtml(payment, customerName, customerMobile, company, previousBalance, remainingBalance)
        printHtml(context, htmlContent, "Receipt_${payment.id}")
    }

    /**
     * Generates Delivery Challan / Gate Pass HTML (No rates/prices shown, focused on Door sizes & quantity)
     */
    fun generateDeliveryChallanHtml(
        billWithItems: BillWithItems,
        company: CompanyProfileEntity,
        vehicleNo: String = "",
        transportName: String = ""
    ): String {
        val bill = billWithItems.bill
        val items = billWithItems.items
        val totalQty = items.sumOf { it.qty }
        val totalSqFt = items.sumOf { it.sqFt }
        val challanNo = "DC/" + bill.invoiceNo.removePrefix("INV/").removePrefix("EST/")

        val itemRows = items.joinToString("\n") { item ->
            val particularFormatted = item.particular.replace("\n", "<br><span style='font-size:10px; color:#475569;'>").let {
                if (item.particular.contains("\n")) "$it</span>" else it
            }
            """
            <tr>
                <td style="text-align:center; padding:7px 4px; border:1px solid #333;">${item.slNo}</td>
                <td style="padding:7px 8px; border:1px solid #333; font-weight:600;">$particularFormatted</td>
                <td style="text-align:center; padding:7px 4px; border:1px solid #333; color:#555;">${item.hsnSac}</td>
                <td style="text-align:center; padding:7px 4px; border:1px solid #333; font-weight:bold;">${DimensionCalculator.formatDimension(item.height)} × ${DimensionCalculator.formatDimension(item.width)} ${bill.dimensionUnit}</td>
                <td style="text-align:center; padding:7px 4px; border:1px solid #333; font-weight:bold; background-color:#eff6ff;">${item.qty} Pcs</td>
                <td style="text-align:right; padding:7px 6px; border:1px solid #333; font-weight:bold; background-color:#f0f9ff;">${String.format(Locale.US, "%.2f", item.sqFt)}</td>
                <td style="text-align:center; padding:7px 4px; border:1px solid #333; color:#64748b; font-size:10px;">Sound Condition</td>
            </tr>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Delivery Challan - $challanNo</title>
            <style>
                @page { size: A4; margin: 12mm; }
                body { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 11px; color: #111; margin: 0; padding: 0; }
                .container { border: 2px solid #0369A1; padding: 0; box-sizing: border-box; }
                .top-title-bar { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #0369A1; background-color: #DCEEF8; padding: 7px 12px; }
                .header-table { width: 100%; border-collapse: collapse; border-bottom: 2px solid #0369A1; }
                .header-table td { padding: 9px 12px; vertical-align: top; }
                .items-table { width: 100%; border-collapse: collapse; }
                .items-table th { background-color: #0369A1; color: white; padding: 7px 4px; font-size: 10.5px; text-align: center; border: 1px solid #0369A1; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="top-title-bar">
                    <div style="font-weight:bold; color:#0369A1; font-size:14px; letter-spacing:0.5px;">DELIVERY CHALLAN / GATE PASS (DISPATCH SLIP)</div>
                    <div style="font-size:11px; color:#475569; font-style:italic;">(Goods Movement & Material Dispatch)</div>
                </div>

                <table class="header-table">
                    <tr>
                        <td style="width:58%; background-color:#EEF5F9;">
                            <h2 style="margin:0; font-size:17px; color:#0369A1; text-transform:uppercase;">${company.businessName}</h2>
                            <div style="font-size:10.5px; color:#333; margin-top:2px;">${company.fullAddress}</div>
                            <div style="font-size:10.5px; margin-top:2px;"><strong>GSTIN:</strong> ${company.gstNo} &nbsp;|&nbsp; <strong>Mobile:</strong> ${company.displayMobile}</div>
                            <div style="margin-top:8px; padding-top:6px; border-top:1px dashed #cbd5e1;">
                                <div style="font-weight:bold; color:#0369A1; font-size:11px;">CONSIGNEE / DELIVER TO:</div>
                                <div style="font-size:13px; font-weight:bold; color:#111; margin-top:2px;">${bill.customerName}</div>
                                <div style="font-size:10.5px; color:#333;"><strong>Site / Address:</strong> ${bill.customerAddress.ifBlank { "As per order" }}</div>
                                <div style="font-size:10.5px; color:#333;"><strong>Contact:</strong> ${bill.customerMobile.ifBlank { "N/A" }} &nbsp;|&nbsp; <strong>GSTIN:</strong> ${bill.customerGstNo.ifBlank { "Unregistered" }}</div>
                            </div>
                        </td>
                        <td style="width:42%; border-left:2px solid #0369A1; background-color:#D3E3ED;">
                            <div style="font-weight:bold; color:#0369A1; font-size:11px; margin-bottom:5px; border-bottom:1.5px solid #CBD5E1; padding-bottom:3px;">DISPATCH DETAILS:</div>
                            <table style="width:100%; font-size:11px; border-collapse:collapse;">
                                <tr>
                                    <td style="width:42%; padding:3px 0; font-weight:bold; color:#334155;">Challan No:</td>
                                    <td style="padding:3px 0; font-weight:bold; color:#0369A1; font-size:13px;">$challanNo</td>
                                </tr>
                                <tr>
                                    <td style="padding:3px 0; font-weight:bold; color:#334155;">Dispatch Date:</td>
                                    <td style="padding:3px 0; font-weight:bold; color:#0F172A;">${DimensionCalculator.formatDate(bill.dateMillis)}</td>
                                </tr>
                                <tr>
                                    <td style="padding:3px 0; font-weight:bold; color:#334155;">Order / Bill Ref:</td>
                                    <td style="padding:3px 0; font-weight:bold; color:#0F172A;">${bill.invoiceNo}</td>
                                </tr>
                                <tr>
                                    <td style="padding:3px 0; font-weight:bold; color:#334155;">Vehicle No:</td>
                                    <td style="padding:3px 0; font-weight:bold; color:#0F172A;">${vehicleNo.ifBlank { "Self / Local Transport" }}</td>
                                </tr>
                                <tr>
                                    <td style="padding:3px 0; font-weight:bold; color:#334155;">Transporter:</td>
                                    <td style="padding:3px 0; font-weight:bold; color:#0F172A;">${transportName.ifBlank { "Factory Direct Delivery" }}</td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>

                <table class="items-table">
                    <thead>
                        <tr>
                            <th style="width:5%;">S.N.</th>
                            <th style="width:36%;">Door Description / Particular</th>
                            <th style="width:9%;">HSN</th>
                            <th style="width:20%;">Size (H × W)</th>
                            <th style="width:10%;">Qty</th>
                            <th style="width:10%;">Total Sq.Ft</th>
                            <th style="width:10%;">Remarks</th>
                        </tr>
                    </thead>
                    <tbody>
                        $itemRows
                        <tr style="background-color:#f8fafc; font-weight:bold; border-top:2px solid #0369A1;">
                            <td colspan="4" style="text-align:right; padding:8px; border:1px solid #333; font-size:12px; color:#0369A1;">TOTAL DISPATCHED:</td>
                            <td style="text-align:center; padding:8px; border:1px solid #333; font-size:12px; background-color:#eff6ff; color:#0369A1;">$totalQty Doors</td>
                            <td style="text-align:right; padding:8px; border:1px solid #333; font-size:12px; background-color:#f0f9ff; color:#0369A1;">${String.format(Locale.US, "%.2f", totalSqFt)} Sq.Ft</td>
                            <td style="text-align:center; padding:8px; border:1px solid #333; font-size:11px; color:#16a34a;">Verified</td>
                        </tr>
                    </tbody>
                </table>

                <div style="padding:10px 12px; background-color:#f8fafc; border-top:1px solid #cbd5e1; font-size:10px; color:#475569; line-height:1.4;">
                    <strong>Dispatch Terms & Notes:</strong><br>
                    1. Goods listed above are dispatched in good and undamaged condition from our workshop.<br>
                    2. Consignee/Receiver must verify the number of doors and physical dimensions upon delivery before sign-off.<br>
                    3. This delivery challan is issued for transport and delivery verification.
                </div>

                <div style="padding:32px 14px 14px 14px; display:flex; justify-content:space-between; align-items:flex-end; font-size:11px; border-top:1px solid #cbd5e1;">
                    <div style="text-align:center; width:28%;">
                        <div style="border-top:1.5px solid #333; padding-top:4px; font-weight:bold;">RECEIVER'S SIGN & STAMP</div>
                        <div style="font-size:9.5px; color:#666; margin-top:2px;">(Received in good condition)</div>
                    </div>
                    <div style="text-align:center; width:28%;">
                        <div style="border-top:1.5px solid #333; padding-top:4px; font-weight:bold;">DRIVER / TRANSPORTER SIGN</div>
                        <div style="font-size:9.5px; color:#666; margin-top:2px;">(Delivered safely)</div>
                    </div>
                    <div style="text-align:center; width:34%;">
                        <div style="font-size:11px; margin-bottom:28px; color:#333;">For <strong>${company.businessName}</strong></div>
                        <div style="border-top:1.5px solid #333; padding-top:4px; font-weight:bold; color:#0369A1;">AUTHORIZED DISPATCHER</div>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * Generates Payment Receipt / Money Voucher HTML
     */
    fun generatePaymentReceiptHtml(
        payment: PaymentEntity,
        customerName: String,
        customerMobile: String,
        company: CompanyProfileEntity,
        previousBalance: Double,
        remainingBalance: Double
    ): String {
        val receiptNo = "REC-${payment.id.toString().padStart(4, '0')}"
        val amountInWords = DimensionCalculator.convertToIndianCurrencyWords(payment.amount)

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Payment Receipt - $receiptNo</title>
            <style>
                @page { size: A4; margin: 15mm; }
                body { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 11.5px; color: #111; margin: 0; padding: 0; }
                .container { border: 2px solid #16a34a; padding: 0; box-sizing: border-box; max-width: 680px; margin: 0 auto; }
                .top-bar { background-color: #dcfce7; border-bottom: 2px solid #16a34a; padding: 10px 16px; display: flex; justify-content: space-between; align-items: center; }
                .content-box { padding: 18px 20px; }
                .amount-banner { background-color: #15803d; color: white; padding: 12px 18px; border-radius: 8px; display: flex; justify-content: space-between; align-items: center; margin: 16px 0; }
                .detail-table { width: 100%; border-collapse: collapse; margin: 14px 0; }
                .detail-table td { padding: 7px 10px; border-bottom: 1px solid #e2e8f0; vertical-align: top; }
                .balance-table { width: 100%; border-collapse: collapse; background-color: #f8fafc; border: 1px solid #cbd5e1; border-radius: 6px; margin: 14px 0; }
                .balance-table td { padding: 9px 12px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="top-bar">
                    <div>
                        <span style="font-size:15px; font-weight:bold; color:#15803d; letter-spacing:0.5px;">PAYMENT RECEIPT / VOUCHER</span>
                    </div>
                    <div style="font-size:12px; font-weight:bold; color:#1e293b;">
                        Receipt No: <span style="color:#15803d;">$receiptNo</span>
                    </div>
                </div>

                <div class="content-box">
                    <div style="display:flex; justify-content:space-between; align-items:flex-start; border-bottom:1.5px solid #e2e8f0; padding-bottom:12px;">
                        <div>
                            <h2 style="margin:0; font-size:19px; color:#15803d; text-transform:uppercase;">${company.businessName}</h2>
                            <div style="font-size:11px; color:#475569; margin-top:2px;">${company.fullAddress}</div>
                            <div style="font-size:11px; color:#475569;"><strong>GSTIN:</strong> ${company.gstNo} &nbsp;|&nbsp; <strong>Phone:</strong> ${company.displayMobile}</div>
                        </div>
                        <div style="text-align:right;">
                            <div style="font-size:11.5px; color:#475569;"><strong>Receipt Date:</strong></div>
                            <div style="font-size:13px; font-weight:bold; color:#0f172a; margin-top:2px;">${DimensionCalculator.formatDate(payment.dateMillis)}</div>
                        </div>
                    </div>

                    <table class="detail-table">
                        <tr>
                            <td style="width:28%; font-weight:bold; color:#475569;">Received From:</td>
                            <td style="font-size:14px; font-weight:bold; color:#0f172a;">$customerName ${if (customerMobile.isNotBlank()) "($customerMobile)" else ""}</td>
                        </tr>
                        <tr>
                            <td style="font-weight:bold; color:#475569;">Payment Mode:</td>
                            <td style="font-weight:bold; color:#15803d; font-size:12.5px;">${payment.paymentMode} ${if (payment.referenceNo.isNotBlank()) " (Ref / UTR: " + payment.referenceNo + ")" else ""}</td>
                        </tr>
                        <tr>
                            <td style="font-weight:bold; color:#475569;">Notes / Remarks:</td>
                            <td style="color:#334155;">${payment.notes.ifBlank { "Payment received against account settlement" }}</td>
                        </tr>
                    </table>

                    <div class="amount-banner">
                        <div>
                            <div style="font-size:10.5px; text-transform:uppercase; letter-spacing:0.5px; opacity:0.9;">AMOUNT RECEIVED</div>
                            <div style="font-size:12px; font-style:italic; margin-top:3px; opacity:0.95;">$amountInWords</div>
                        </div>
                        <div style="font-size:24px; font-weight:bold;">
                            ₹${String.format(Locale.US, "%,.2f", payment.amount)}
                        </div>
                    </div>

                    <table class="balance-table">
                        <tr>
                            <td style="width:40%; color:#475569;">Previous Outstanding Balance:</td>
                            <td style="font-weight:600; text-align:right;">₹${String.format(Locale.US, "%.2f", previousBalance)}</td>
                        </tr>
                        <tr>
                            <td style="color:#15803d; font-weight:bold;">Amount Received Now:</td>
                            <td style="font-weight:bold; color:#15803d; text-align:right;">- ₹${String.format(Locale.US, "%.2f", payment.amount)}</td>
                        </tr>
                        <tr style="border-top:1.5px solid #cbd5e1; background-color:#f1f5f9;">
                            <td style="font-size:12px; font-weight:bold; color:#0f172a;">Current Remaining Balance:</td>
                            <td style="font-size:13px; font-weight:bold; text-align:right; color:${if (remainingBalance > 0) "#b91c1c" else "#15803d"};">
                                ₹${String.format(Locale.US, "%.2f", remainingBalance)}
                            </td>
                        </tr>
                    </table>

                    <div style="margin-top:36px; display:flex; justify-content:space-between; align-items:flex-end;">
                        <div style="font-size:10px; color:#64748b;">
                            Thank you for your business!<br>
                            This is an authorized electronic payment acknowledgement.
                        </div>
                        <div style="text-align:center;">
                            <div style="font-size:11px; color:#475569; margin-bottom:28px;">For <strong>${company.businessName}</strong></div>
                            <div style="border-top:1.5px solid #333; padding-top:3px; font-size:11px; font-weight:bold;">AUTHORIZED SIGNATORY</div>
                        </div>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}

