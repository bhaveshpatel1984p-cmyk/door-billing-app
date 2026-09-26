package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.db.BillWithItems
import com.example.data.db.CompanyProfileEntity
import com.example.util.DimensionCalculator
import com.example.util.InvoicePrinter
import com.example.util.QrCodeHelper
import com.example.util.ShareHelper

@Composable
fun InvoiceViewDialog(
    billWithItems: BillWithItems,
    company: CompanyProfileEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onConvertToInvoice: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val bill = billWithItems.bill
    val items = billWithItems.items
    val totalSqFt = items.sumOf { it.sqFt }
    val totalQty = items.sumOf { it.qty }
    var showShareOptions by remember { mutableStateOf(false) }
    var showDeliveryChallanDialog by remember { mutableStateOf(false) }
    var showConvertConfirmDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with title and close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (bill.isQuotation) "Quotation / Estimate Preview" else "Invoice Preview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (bill.isQuotation) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        "ESTIMATE",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }
                        Text(
                            text = bill.invoiceNo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Scrollable Bill Paper
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Company Header inside bill (Matches Reference Layout)
                    item {
                        val totalQty = items.sumOf { it.qty }
                        val totalSqFt = items.sumOf { it.sqFt }
                        val totalWithOldBalance = bill.grandTotal + bill.previousBalance
                        val netPayableAmount = if (bill.netPayable > 0.0) bill.netPayable else totalWithOldBalance
                        val payStatus = if (bill.paidAmount >= netPayableAmount) "PAID" else if (bill.paidAmount > 0) "PARTIAL" else "UNPAID"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, Color(0xFF0369A1), RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column {
                                // Top Title Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFDCEEF8))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (bill.isQuotation) "ESTIMATE / QUOTATION" else "TAX INVOICE / BILL OF SUPPLY",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF0369A1),
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "(Door Manufacturing & Joinery Billing)",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF475569),
                                        fontStyle = FontStyle.Italic
                                    )
                                }

                                HorizontalDivider(thickness = 1.5.dp, color = Color(0xFF0369A1))

                                // Row 1: Company Header (Left) & Invoice Meta (Right)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                ) {
                                    // Left: Company Info
                                    Column(
                                        modifier = Modifier
                                            .weight(1.6f)
                                            .background(Color(0xFFEEF5F9))
                                            .padding(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(
                                                model = if (!company.logoUri.isNullOrBlank()) company.logoUri else com.example.R.drawable.img_nirmal_door_logo,
                                                contentDescription = "Logo",
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = company.businessName.uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF0369A1)
                                                )
                                                Text(
                                                    text = company.fullAddress,
                                                    fontSize = 9.5.sp,
                                                    color = Color(0xFF334155),
                                                    lineHeight = 12.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "GSTIN/UIN: ${company.gstNo}${if (company.pan.isNotBlank()) " | PAN: ${company.pan}" else ""}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "Contact : ${company.displayMobile}",
                                            fontSize = 9.sp,
                                            color = Color(0xFF334155)
                                        )
                                        Text(
                                            text = "E-Mail : ${company.email.ifBlank { "N/A" }}",
                                            fontSize = 9.sp,
                                            color = Color(0xFF334155)
                                        )
                                        Text(
                                            text = "State Name : ${company.state}${if (company.stateCode.isNotBlank()) ", Code : ${company.stateCode}" else ""}",
                                            fontSize = 9.sp,
                                            color = Color(0xFF334155)
                                        )
                                    }

                                    // Vertical Divider
                                    VerticalDivider(thickness = 1.5.dp, color = Color(0xFF0369A1))

                                    // Right: Invoice Details
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(Color(0xFFD3E3ED))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(if (bill.isQuotation) "Quote No:" else "Invoice No:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                            Text(bill.invoiceNo, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Date:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                            Text(DimensionCalculator.formatDate(bill.dateMillis), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Unit:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(bill.dimensionUnit, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                Text("Dimension", fontSize = 9.sp, color = Color(0xFF475569))
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(thickness = 1.5.dp, color = Color(0xFF0369A1))

                                // Row 2: Customer Details (Left) & Payment/Delivery Summary (Right)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                ) {
                                    // Left: Billed to
                                    Column(
                                        modifier = Modifier
                                            .weight(1.6f)
                                            .background(Color(0xFFEEF5F9))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "BILLED TO (CUSTOMER DETAILS):",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0369A1)
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 3.dp), thickness = 1.dp, color = Color(0xFFCBD5E1))

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

                                        Text(firmName, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF0F172A))
                                        if (!contactPerson.isNullOrBlank()) {
                                            Text("Customer: $contactPerson", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                        }
                                        Text("Address: ${bill.customerAddress.ifBlank { "N/A" }}", fontSize = 10.sp, color = Color(0xFF334155))
                                        Text("Mobile: ${bill.customerMobile.ifBlank { "N/A" }}", fontSize = 10.sp, color = Color(0xFF334155))
                                        Text("GSTIN: ${bill.customerGstNo.ifBlank { "Unregistered" }}", fontSize = 10.sp, color = Color(0xFF334155))
                                    }

                                    // Vertical Divider
                                    VerticalDivider(thickness = 1.5.dp, color = Color(0xFF0369A1))

                                    // Right: Payment & Delivery Summary
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(Color(0xFFD3E3ED))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "PAYMENT & DELIVERY SUMMARY:",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0369A1)
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 3.dp), thickness = 1.dp, color = Color(0xFFCBD5E1))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Total Quantity:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                            Text("$totalQty Doors", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Total Area:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                            Text("${String.format(java.util.Locale.US, "%.2f", totalSqFt)} Sq.Ft", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Payment Status:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                            Text(
                                                text = payStatus,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (payStatus == "PAID") Color(0xFF16A34A) else Color(0xFFD97706)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Table Header
                    item {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sl. Particular", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                                Text("Size (${bill.dimensionUnit.take(2)})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                                Text("Qty", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
                                Text("Sq.Ft", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text("Amount", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                            }
                        }
                    }

                    // Item rows
                    items(items) { item ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2f)) {
                                    val partLines = item.particular.split("\n")
                                    Text("${item.slNo}. ${partLines[0].trim()}", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                    if (partLines.size > 1 && partLines[1].isNotBlank()) {
                                        Text(partLines[1].trim(), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Text("${DimensionCalculator.formatDimension(item.height)}x${DimensionCalculator.formatDimension(item.width)}", fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                                Text("${item.qty}", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
                                Text("${String.format(java.util.Locale.US, "%.2f", item.sqFt)}", fontSize = 11.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text("₹${String.format(java.util.Locale.US, "%.2f", item.amount)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                            }
                        }
                    }

                    // Totals
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Subtotal ($totalQty Doors | ${String.format(java.util.Locale.US, "%.2f", totalSqFt)} Sq.Ft):", fontSize = 12.sp)
                                    Text(DimensionCalculator.formatCurrency(bill.subTotal), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }

                                if (bill.isGstIncluded && bill.taxRate > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("CGST (${bill.taxRate / 2}%):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(DimensionCalculator.formatCurrency(bill.cgstAmount), fontSize = 11.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("SGST (${bill.taxRate / 2}%):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(DimensionCalculator.formatCurrency(bill.sgstAmount), fontSize = 11.sp)
                                    }
                                }

                                if (bill.otherCharges > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${bill.otherChargesDescription.ifBlank { "Cutting Charges" }}:", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Medium)
                                        Text("+ " + DimensionCalculator.formatCurrency(bill.otherCharges), fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (bill.discountAmount > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Discount:", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                        Text("- " + DimensionCalculator.formatCurrency(bill.discountAmount), fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }

                                if (bill.previousBalance > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("(+) Previous Balance / Due Balance:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))
                                        Text("+ " + DimensionCalculator.formatCurrency(bill.previousBalance), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                val totalWithOldBal = bill.grandTotal + bill.previousBalance
                                val netPayableAmt = if (bill.netPayable > 0.0) bill.netPayable else totalWithOldBal
                                val finalRemainingDue = if (bill.paidAmount > 0.0) Math.max(0.0, netPayableAmt - bill.paidAmount) else netPayableAmt

                                if (bill.previousBalance > 0 && bill.paidAmount <= 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Current Bill Total:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                        Text(DimensionCalculator.formatCurrency(bill.grandTotal), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("TOTAL DUE:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            DimensionCalculator.formatCurrency(netPayableAmt),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = Color(0xFF0369A1)
                                        )
                                    }
                                } else if (bill.paidAmount > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Current Bill Total:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                        Text(DimensionCalculator.formatCurrency(bill.grandTotal), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                    if (bill.previousBalance > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Total Due Amount:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                            Text(DimensionCalculator.formatCurrency(netPayableAmt), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("(-) Paid / Received:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                        Text("- " + DimensionCalculator.formatCurrency(bill.paidAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("REMAINING DUE:", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFFB91C1C))
                                        Text(
                                            DimensionCalculator.formatCurrency(finalRemainingDue),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = Color(0xFFB91C1C)
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("GRAND TOTAL:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            DimensionCalculator.formatCurrency(bill.grandTotal),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = Color(0xFF0369A1)
                                        )
                                    }
                                }

                                val effectivePayable = if (bill.paidAmount > 0.0 && finalRemainingDue > 0.0) finalRemainingDue else if (bill.previousBalance > 0) netPayableAmt else bill.grandTotal

                                Text(
                                    text = DimensionCalculator.convertToIndianCurrencyWords(effectivePayable),
                                    fontSize = 10.5.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val qrBitmap = remember(company, effectivePayable) {
                                    QrCodeHelper.getPaymentQrBitmap(company, effectivePayable, size = 180)
                                }
                                if (qrBitmap != null || company.bankName.isNotBlank() || company.accountNo.isNotBlank()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("BANK & PAYMENT DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                if (company.bankName.isNotBlank()) {
                                                    Text("Bank: ${company.bankName}", fontSize = 10.sp)
                                                    Text("A/C: ${company.accountNo}", fontSize = 10.sp)
                                                    Text("IFSC: ${company.ifscCode}", fontSize = 10.sp)
                                                }
                                                if (company.upiId.isNotBlank()) {
                                                    Text("UPI: ${company.upiId}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0369A1))
                                                }
                                            }
                                            if (qrBitmap != null) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(start = 8.dp)
                                                ) {
                                                    Text(
                                                        "SCAN TO PAY",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF0369A1),
                                                        letterSpacing = 0.5.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Image(
                                                        bitmap = qrBitmap.asImageBitmap(),
                                                        contentDescription = "Payment QR",
                                                        modifier = Modifier
                                                            .size(72.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(6.dp))
                                                            .background(Color.White)
                                                            .padding(3.dp)
                                                    )
                                                    Text(
                                                        "PhonePe • GPay • Paytm",
                                                        fontSize = 7.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Declaration Box (Reference format: Underlined Declaration + Full text)
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "Declaration",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = company.declaration,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFEEF5F9), RoundedCornerShape(4.dp))
                                        .padding(vertical = 6.dp, horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = DimensionCalculator.formatJurisdictionClause(company.jurisdiction),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "This is a Computer Generated ${if (bill.isQuotation) "Quotation" else "Invoice"}",
                                        fontSize = 8.5.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = Color(0xFF64748B),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Convert to Invoice Banner Button (if bill is quotation)
                if (bill.isQuotation && onConvertToInvoice != null) {
                    Button(
                        onClick = { showConvertConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Convert Quotation to Tax Invoice", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Bottom Action buttons: Print | Share | Challan | Edit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            InvoicePrinter.printInvoice(context, billWithItems, company)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print", fontSize = 11.5.sp)
                    }

                    Button(
                        onClick = {
                            showShareOptions = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 11.5.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            showDeliveryChallanDialog = true
                        },
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Challan", fontSize = 11.5.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onEdit()
                        },
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 11.5.sp)
                    }
                }
            }
        }

        if (showShareOptions) {
            val docTitle = if (bill.isQuotation) "Share Quotation / Estimate" else "Share Tax Invoice"
            val docSubtitle = "${if (bill.isQuotation) "Quote" else "Invoice"} #${bill.invoiceNo} • ${bill.customerName}"
            ShareOptionsDialog(
                title = docTitle,
                subtitle = docSubtitle,
                onDismiss = { showShareOptions = false },
                onShareWhatsApp = {
                    ShareHelper.shareInvoicePdfWhatsApp(context, billWithItems, company)
                },
                onShareWhatsAppBusiness = {
                    ShareHelper.shareInvoicePdfWhatsAppBusiness(context, billWithItems, company)
                },
                onSharePdf = {
                    ShareHelper.shareInvoicePdfGeneral(context, billWithItems, company)
                },
                onShareText = {
                    ShareHelper.shareInvoiceTextGeneral(context, billWithItems, company)
                },
                onPrint = {
                    InvoicePrinter.printInvoice(context, billWithItems, company)
                },
                onDeliveryChallan = {
                    showDeliveryChallanDialog = true
                }
            )
        }

        if (showDeliveryChallanDialog) {
            DeliveryChallanDialog(
                billWithItems = billWithItems,
                company = company,
                onDismiss = { showDeliveryChallanDialog = false }
            )
        }

        if (showConvertConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConvertConfirmDialog = false },
                title = { Text("Convert to Tax Invoice?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("This will convert Quotation #${bill.invoiceNo} into an official Tax Invoice, assign a new sequential Invoice Number, and register it in Sales & Customer Ledgers.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConvertConfirmDialog = false
                            onDismiss()
                            onConvertToInvoice?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Text("Convert Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConvertConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

