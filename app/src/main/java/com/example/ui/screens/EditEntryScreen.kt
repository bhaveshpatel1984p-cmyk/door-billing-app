package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.BillWithItems
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.DoorBillingViewModel
import com.example.util.DimensionCalculator
import com.example.util.InvoicePrinter
import com.example.util.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryScreen(
    viewModel: DoorBillingViewModel,
    onViewBill: (BillWithItems) -> Unit
) {
    val context = LocalContext.current
    val allBills by viewModel.allBills.collectAsStateWithLifecycle()
    val company by viewModel.companyProfile.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var billToDelete by remember { mutableStateOf<BillWithItems?>(null) }
    var billToShare by remember { mutableStateOf<BillWithItems?>(null) }

    val filteredBills = remember(allBills, searchQuery) {
        if (searchQuery.isBlank()) allBills
        else allBills.filter {
            it.bill.invoiceNo.contains(searchQuery, ignoreCase = true) ||
                    it.bill.customerName.contains(searchQuery, ignoreCase = true) ||
                    it.bill.customerMobile.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit / Manage Bill Entries", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.startNewBill() },
                containerColor = Color(0xFF16A34A),
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_new_bill")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Bill")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("edit_entry_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Invoice No or Customer Name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAVED BILLS (${filteredBills.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "Total: " + DimensionCalculator.formatCurrency(filteredBills.sumOf { it.bill.grandTotal }),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            if (filteredBills.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "No bill entries found" else "No bills match '$searchQuery'",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                items(filteredBills, key = { it.bill.id }) { billWithItems ->
                    val bill = billWithItems.bill
                    val totalSqFt = billWithItems.items.sumOf { it.sqFt }
                    val totalQty = billWithItems.items.sumOf { it.qty }

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bill_entry_card_${bill.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Invoice No & Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = bill.invoiceNo,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0369A1)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "${bill.dimensionUnit} Unit",
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = DimensionCalculator.formatDate(bill.dateMillis),
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Customer name & mobile
                            val rawCust = bill.customerName.trim()
                            val parenMatch = Regex("^(.*?)\\s*\\((.*?)\\)$").find(rawCust)
                            if (parenMatch != null) {
                                val firm = parenMatch.groupValues[1].trim()
                                val contact = parenMatch.groupValues[2].trim()
                                Text(
                                    text = firm,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "👤 Contact: $contact",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = bill.customerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (bill.customerMobile.isNotBlank()) {
                                Text(
                                    text = "📞 ${bill.customerMobile}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Door count, Sq.Ft & Amount Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "$totalQty Doors (${billWithItems.items.size} types)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${String.format(java.util.Locale.US, "%.2f", totalSqFt)} Sq.Ft Area",
                                        fontSize = 12.sp,
                                        color = Color(0xFF0284C7),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = DimensionCalculator.formatCurrency(bill.grandTotal),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0C4A6E)
                                        )
                                    )
                                    Text(
                                        text = if (bill.isGstIncluded) "Incl. GST (${bill.taxRate.toInt()}%)" else "Excl. GST",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons row: View | Edit | Delete | Print | WhatsApp
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // View Details
                                OutlinedButton(
                                    onClick = { onViewBill(billWithItems) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View", fontSize = 12.sp)
                                }

                                // Edit Button
                                OutlinedButton(
                                    onClick = { viewModel.startEditBill(billWithItems) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }

                                // Print Quick Button
                                IconButton(
                                    onClick = { InvoicePrinter.printInvoice(context, billWithItems, company) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = "Print", tint = Color(0xFF0284C7))
                                }

                                // Share Quick Button
                                IconButton(
                                    onClick = { billToShare = billWithItems },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Options", tint = Color(0xFF25D366))
                                }

                                // Delete Button
                                IconButton(
                                    onClick = { billToDelete = billWithItems },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Bill confirmation dialog
    billToDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { billToDelete = null },
            title = { Text("Delete Bill Entry?") },
            text = { Text("Are you sure you want to permanently delete Invoice '${b.bill.invoiceNo}' for customer '${b.bill.customerName}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBill(b.bill.id)
                        billToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { billToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Share Options Dialog
    billToShare?.let { b ->
        ShareOptionsDialog(
            title = "Share Tax Invoice",
            subtitle = "Invoice #${b.bill.invoiceNo} • ${b.bill.customerName}",
            onDismiss = { billToShare = null },
            onShareWhatsApp = {
                ShareHelper.shareInvoicePdfWhatsApp(context, b, company)
            },
            onShareWhatsAppBusiness = {
                ShareHelper.shareInvoicePdfWhatsAppBusiness(context, b, company)
            },
            onSharePdf = {
                ShareHelper.shareInvoicePdfGeneral(context, b, company)
            },
            onShareText = {
                ShareHelper.shareInvoiceTextGeneral(context, b, company)
            },
            onPrint = {
                InvoicePrinter.printInvoice(context, b, company)
            }
        )
    }
}
