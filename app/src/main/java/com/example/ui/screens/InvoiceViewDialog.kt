package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.util.ShareHelper

@Composable
fun InvoiceViewDialog(
    billWithItems: BillWithItems,
    company: CompanyProfileEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val bill = billWithItems.bill
    val items = billWithItems.items
    val totalSqFt = items.sumOf { it.sqFt }
    val totalQty = items.sumOf { it.qty }

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
                        Text(
                            text = "Invoice Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
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
                    // Company Header inside bill
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = if (!company.logoUri.isNullOrBlank()) company.logoUri else com.example.R.drawable.img_nirmal_door_logo,
                                        contentDescription = "Logo",
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = company.businessName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            buildString {
                                                append("GST: ${company.gstNo} • Ph: ${company.mobile}")
                                                if (company.email.isNotBlank()) append(" • Email: ${company.email}")
                                            },
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("CUSTOMER:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(bill.customerName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        if (bill.customerMobile.isNotBlank()) Text("📞 ${bill.customerMobile}", fontSize = 11.sp)
                                        if (bill.customerAddress.isNotBlank()) Text("📍 ${bill.customerAddress}", fontSize = 11.sp)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Date: ${DimensionCalculator.formatDate(bill.dateMillis)}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Text("Unit: ${bill.dimensionUnit}", fontSize = 11.sp)
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
                                Text("${item.slNo}. ${item.particular}", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(2f))
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

                                if (bill.discountAmount > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Discount:", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                        Text("- " + DimensionCalculator.formatCurrency(bill.discountAmount), fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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

                                Text(
                                    text = DimensionCalculator.convertToIndianCurrencyWords(bill.grandTotal),
                                    fontSize = 10.5.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                                Text(
                                    text = "SUBJECT TO ${company.jurisdiction.ifBlank { "LOCAL" }.uppercase()} JURISDICTION",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action buttons: Print | WhatsApp | Edit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            InvoicePrinter.printInvoice(context, billWithItems, company)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print / PDF", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            ShareHelper.shareInvoiceWhatsApp(context, billWithItems, company)
                        },
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onEdit()
                        },
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
