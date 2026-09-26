package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.BillWithItems
import com.example.data.db.LedgerEntry
import com.example.data.db.PaymentEntity
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.DoorBillingViewModel
import com.example.util.DimensionCalculator
import com.example.util.InvoicePrinter
import com.example.util.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerScreen(
    viewModel: DoorBillingViewModel,
    onViewBill: (BillWithItems) -> Unit
) {
    val context = LocalContext.current
    val customer by viewModel.selectedLedgerCustomer.collectAsStateWithLifecycle()
    val ledgerEntries by viewModel.ledgerEntries.collectAsStateWithLifecycle()
    val company by viewModel.companyProfile.collectAsStateWithLifecycle()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showPdfViewerDialog by remember { mutableStateOf(false) }
    var payAmountStr by remember { mutableStateOf("") }
    var payMode by remember { mutableStateOf("Cash") }
    var payRef by remember { mutableStateOf("") }
    var payNotes by remember { mutableStateOf("") }
    var payDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var editingPayment by remember { mutableStateOf<PaymentEntity?>(null) }
    var deletingPayment by remember { mutableStateOf<PaymentEntity?>(null) }
    var selectedPaymentForReceipt by remember { mutableStateOf<PaymentEntity?>(null) }

    val allBills by viewModel.allBills.collectAsStateWithLifecycle()
    val autoOpenPayment by viewModel.shouldAutoOpenPaymentDialog.collectAsStateWithLifecycle()
    var showLedgerShareOptions by remember { mutableStateOf(false) }
    var selectedBillForShare by remember { mutableStateOf<BillWithItems?>(null) }
    var selectedBillForViewModal by remember { mutableStateOf<BillWithItems?>(null) }
    var showEditOpeningBalanceDialog by remember { mutableStateOf(false) }
    var editOpeningBalanceInput by remember { mutableStateOf("") }

    LaunchedEffect(autoOpenPayment) {
        if (autoOpenPayment) {
            payDateMillis = System.currentTimeMillis()
            showPaymentDialog = true
            viewModel.shouldAutoOpenPaymentDialog.value = false
        }
    }

    if (customer == null) {
        viewModel.navigateTo(AppScreen.CUSTOMER_BALANCE)
        return
    }

    val cust = customer!!
    val openingBalanceAmount = ledgerEntries.filterIsInstance<LedgerEntry.OpeningBalanceEntry>().sumOf { it.openingAmount }
    val totalInvoicesBilled = ledgerEntries.filterIsInstance<LedgerEntry.BillEntry>().sumOf { it.grandTotal }
    val totalBilled = openingBalanceAmount + totalInvoicesBilled
    val totalPaid = ledgerEntries.filterIsInstance<LedgerEntry.PaymentRecord>().sumOf { it.payment.amount }
    val balance = totalBilled - totalPaid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Ledger / Balance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.CUSTOMER_BALANCE) },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Quick Add Payment in TopBar
                    IconButton(
                        onClick = {
                            payDateMillis = System.currentTimeMillis()
                            showPaymentDialog = true
                        },
                        modifier = Modifier.testTag("topbar_add_payment_button")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = "Add Payment", tint = Color.White)
                    }

                    // View PDF Button in TopBar
                    IconButton(
                        onClick = { showPdfViewerDialog = true }
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "View PDF Ledger", tint = Color.White)
                    }

                    // Print Button in TopBar
                    IconButton(
                        onClick = {
                            InvoicePrinter.printCustomerLedger(
                                context = context,
                                customer = cust,
                                ledgerEntries = ledgerEntries,
                                totalBilled = totalBilled,
                                totalPaid = totalPaid,
                                balance = balance,
                                company = company
                            )
                        }
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print Ledger", tint = Color.White)
                    }

                    // Share Button in TopBar
                    IconButton(
                        onClick = { showLedgerShareOptions = true }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF25D366))
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
            ExtendedFloatingActionButton(
                onClick = {
                    payDateMillis = System.currentTimeMillis()
                    showPaymentDialog = true
                },
                icon = { Icon(Icons.Default.Payment, contentDescription = null) },
                text = { Text("+ Add Payment (जमा)", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                containerColor = Color(0xFF16A34A),
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_payment")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("customer_ledger_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Customer Info Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = cust.primaryTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        if (!cust.subtitle.isNullOrBlank()) {
                            Text("👤 Contact Person: ${cust.subtitle}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        if (cust.mobile.isNotBlank()) {
                            Text("📞 Mobile: ${cust.mobile}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (cust.address.isNotBlank()) {
                            Text("📍 Address: ${cust.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (cust.gstNo.isNotBlank()) {
                            Text("🏛️ GSTIN: ${cust.gstNo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Ledger Totals Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (openingBalanceAmount > 0) {
                                Column(
                                    modifier = Modifier.clickable {
                                        editOpeningBalanceInput = DimensionCalculator.formatDimension(openingBalanceAmount)
                                        showEditOpeningBalanceDialog = true
                                    }
                                ) {
                                    Text("Opening Due ✎", fontSize = 11.sp, color = Color(0xFFC2410C), fontWeight = FontWeight.SemiBold)
                                    Text(
                                        DimensionCalculator.formatCurrency(openingBalanceAmount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFFC2410C)
                                    )
                                }
                            }
                            Column {
                                Text(if (openingBalanceAmount > 0) "Invoices" else "Total Invoices", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    DimensionCalculator.formatCurrency(totalInvoicesBilled),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0369A1)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Received", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    DimensionCalculator.formatCurrency(totalPaid),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF16A34A)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Due Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    DimensionCalculator.formatCurrency(balance),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = if (balance > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                                )
                            }
                        }
                    }
                }
            }

            // Prominent Customer Payment (जमा) Action Banner
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_payment_action_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF0FDF4))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFDCFCE7),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Payment,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Customer Payment (पेमेंट जमा)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    text = "Record Cash, UPI, Cheque or Bank payment",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                payDateMillis = System.currentTimeMillis()
                                showPaymentDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("record_payment_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Payment", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
                }
            }

            // Quick Action Buttons Bar: View (PDF) | Print | WhatsApp
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showPdfViewerDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("view_ledger_pdf_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View PDF", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            InvoicePrinter.printCustomerLedger(
                                context = context,
                                customer = cust,
                                ledgerEntries = ledgerEntries,
                                totalBilled = totalBilled,
                                totalPaid = totalPaid,
                                balance = balance,
                                company = company
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print", fontSize = 12.5.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { showLedgerShareOptions = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF25D366))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 12.5.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Transaction History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRANSACTION ENTRIES (${ledgerEntries.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    if (openingBalanceAmount <= 0.0) {
                        TextButton(
                            onClick = {
                                editOpeningBalanceInput = ""
                                showEditOpeningBalanceDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("+ Set Opening Due", fontSize = 11.5.sp, color = Color(0xFFC2410C), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (ledgerEntries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "No bills or payments recorded for this customer yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    payDateMillis = System.currentTimeMillis()
                                    showPaymentDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Add Customer Payment (पेमेंट जमा करें)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(ledgerEntries) { entry ->
                    when (entry) {
                        is LedgerEntry.OpeningBalanceEntry -> {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFFFEDD5),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("⚖️", fontSize = 18.sp)
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                Text(
                                                    text = "Opening Balance / Previous Due",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    color = Color(0xFFC2410C)
                                                )
                                                Text(
                                                    text = "${DimensionCalculator.formatDate(entry.dateMillis)} • Prior Outstanding Due",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "+ " + DimensionCalculator.formatCurrency(entry.debitAmount),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFFC2410C)
                                            )
                                            Text(
                                                text = "Prior Due (Debit)",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                editOpeningBalanceInput = DimensionCalculator.formatDimension(entry.openingAmount)
                                                showEditOpeningBalanceDialog = true
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFC2410C))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Edit Opening Balance", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC2410C))
                                        }
                                    }
                                }
                            }
                        }
                        is LedgerEntry.BillEntry -> {
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onViewBill(entry.billWithItems) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFE0F2FE),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Receipt,
                                                    contentDescription = null,
                                                    tint = Color(0xFF0369A1),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = "Invoice #${entry.invoiceNo}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${DimensionCalculator.formatDate(entry.dateMillis)} • ${entry.itemsCount} items (${String.format(java.util.Locale.US, "%.1f", entry.totalSqFt)} Sq.Ft)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (entry.billWithItems.bill.previousBalance > 0) {
                                                Text(
                                                    text = "➕ Bill included: ${DimensionCalculator.formatCurrency(entry.billWithItems.bill.previousBalance)} old due",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFC2410C)
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "+ " + DimensionCalculator.formatCurrency(entry.debitAmount),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0369A1)
                                        )
                                        Text(
                                            text = "Billed (Debit)",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                val billWithItems = entry.billWithItems
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // View Button
                                    TextButton(
                                        onClick = {
                                            if (billWithItems != null) {
                                                selectedBillForViewModal = billWithItems
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "View",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Print Button
                                    TextButton(
                                        onClick = {
                                            if (billWithItems != null) {
                                                InvoicePrinter.printInvoice(context, billWithItems, company)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Print,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = Color(0xFF0284C7)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Print",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0284C7)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Share Button
                                    TextButton(
                                        onClick = {
                                            if (billWithItems != null) {
                                                selectedBillForShare = billWithItems
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = Color(0xFF25D366)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Share",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                }
                            }
                        }

                        is LedgerEntry.PaymentRecord -> {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFDCFCE7),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Payment,
                                                        contentDescription = null,
                                                        tint = Color(0xFF16A34A),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                Text(
                                                    text = "Payment Received (${entry.payment.paymentMode})",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "${DimensionCalculator.formatDate(entry.dateMillis)}${if (entry.payment.referenceNo.isNotBlank()) " • Ref: " + entry.payment.referenceNo else ""}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "- " + DimensionCalculator.formatCurrency(entry.creditAmount),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF16A34A)
                                            )
                                            Text(
                                                text = "Received (Credit)",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (entry.payment.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Note: ${entry.payment.notes}",
                                            fontSize = 11.5.sp,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { selectedPaymentForReceipt = entry.payment },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Receipt,
                                                contentDescription = "Payment Receipt",
                                                modifier = Modifier.size(15.dp),
                                                tint = Color(0xFF16A34A)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Receipt",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16A34A)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        TextButton(
                                            onClick = { editingPayment = entry.payment },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit Payment",
                                                modifier = Modifier.size(15.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Edit Payment",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        TextButton(
                                            onClick = { deletingPayment = entry.payment },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Payment",
                                                modifier = Modifier.size(15.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Delete",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Record Payment Dialog
    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Record Customer Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Customer: ${cust.name}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Current Outstanding Due: ${DimensionCalculator.formatCurrency(balance)}", fontSize = 12.sp)

                    if (balance > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { payAmountStr = DimensionCalculator.formatDimension(balance) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Pay Full Due: ${DimensionCalculator.formatCurrency(balance)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1D4ED8)
                                )
                                Text("Auto-Fill ➔", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                            }
                        }
                    }

                    // Manual Date Entry / Selection for Payment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Payment Date:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.clickable {
                                val cal = java.util.Calendar.getInstance().apply { timeInMillis = payDateMillis }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = java.util.Calendar.getInstance().apply {
                                            set(year, month, dayOfMonth)
                                        }
                                        payDateMillis = newCal.timeInMillis
                                    },
                                    cal.get(java.util.Calendar.YEAR),
                                    cal.get(java.util.Calendar.MONTH),
                                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = "Select Date",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${DimensionCalculator.formatDate(payDateMillis)} ✎",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = payAmountStr,
                        onValueChange = { payAmountStr = it },
                        label = { Text("Payment Amount (₹) *") },
                        placeholder = { Text("e.g. 5000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Payment Mode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Cash", "UPI / GPay", "Bank Transfer", "Cheque").forEach { mode ->
                            FilterChip(
                                selected = payMode == mode,
                                onClick = { payMode = mode },
                                label = { Text(mode, fontSize = 10.5.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = payRef,
                        onValueChange = { payRef = it },
                        label = { Text("Reference / UTR / Cheque No (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = payNotes,
                        onValueChange = { payNotes = it },
                        label = { Text("Notes (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = payAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.recordCustomerPayment(
                                customerId = cust.id,
                                customerName = cust.name,
                                amount = amt,
                                mode = payMode,
                                reference = payRef.trim(),
                                notes = payNotes.trim(),
                                dateMillis = payDateMillis
                            )
                            showPaymentDialog = false
                            payAmountStr = ""
                            payRef = ""
                            payNotes = ""
                        }
                    }
                ) {
                    Text("Save Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Payment Dialog
    if (editingPayment != null) {
        val p = editingPayment!!
        EditCustomerPaymentDialog(
            payment = p,
            customerName = cust.name,
            onDismiss = { editingPayment = null },
            onSave = { updatedAmount, updatedMode, updatedRef, updatedNotes, updatedDateMillis ->
                viewModel.updateCustomerPayment(
                    paymentId = p.id,
                    customerId = cust.id,
                    customerName = cust.name,
                    amount = updatedAmount,
                    mode = updatedMode,
                    reference = updatedRef,
                    notes = updatedNotes,
                    dateMillis = updatedDateMillis
                ) {
                    editingPayment = null
                }
            }
        )
    }

    // Delete Payment Confirmation Dialog
    if (deletingPayment != null) {
        val p = deletingPayment!!
        AlertDialog(
            onDismissRequest = { deletingPayment = null },
            title = { Text("Delete Payment Entry?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete this payment of ${DimensionCalculator.formatCurrency(p.amount)} (${p.paymentMode}) recorded on ${DimensionCalculator.formatDate(p.dateMillis)}?\n\nThe customer's due balance will increase accordingly."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomerPayment(p.id) {
                            deletingPayment = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPayment = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PDF Viewer Dialog for Customer Ledger
    if (showPdfViewerDialog) {
        Dialog(
            onDismissRequest = { showPdfViewerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Dialog Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ledger PDF - ${cust.name}",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    InvoicePrinter.printCustomerLedger(
                                        context = context,
                                        customer = cust,
                                        ledgerEntries = ledgerEntries,
                                        totalBilled = totalBilled,
                                        totalPaid = totalPaid,
                                        balance = balance,
                                        company = company
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Print, contentDescription = "Print / Save PDF", tint = Color.White)
                            }
                            IconButton(
                                onClick = { showLedgerShareOptions = true }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share Options", tint = Color(0xFF25D366))
                            }
                            IconButton(onClick = { showPdfViewerDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }

                    // Rendered HTML Document (Simulating PDF print preview)
                    val htmlContent = remember(cust, ledgerEntries, totalBilled, totalPaid, balance, company) {
                        InvoicePrinter.generateLedgerHtml(
                            customer = cust,
                            ledgerEntries = ledgerEntries,
                            totalBilled = totalBilled,
                            totalPaid = totalPaid,
                            balance = balance,
                            company = company
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFFE2E8F0))
                            .padding(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    android.webkit.WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.builtInZoomControls = true
                                        settings.displayZoomControls = false
                                        settings.loadWithOverviewMode = true
                                        settings.useWideViewPort = true
                                        loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                                    }
                                },
                                update = { webView ->
                                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Bottom Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "A4 PDF Statement Preview",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showPdfViewerDialog = false }) {
                                Text("Close")
                            }
                            Button(
                                onClick = {
                                    InvoicePrinter.printCustomerLedger(
                                        context = context,
                                        customer = cust,
                                        ledgerEntries = ledgerEntries,
                                        totalBilled = totalBilled,
                                        totalPaid = totalPaid,
                                        balance = balance,
                                        company = company
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save PDF / Print")
                            }
                        }
                    }
                }
            }
        }
    }

    // Ledger Share Options Dialog (System Chooser, WhatsApp, WhatsApp Business, Text, Print)
    if (showLedgerShareOptions) {
        ShareOptionsDialog(
            title = "Share Account Statement",
            subtitle = "${cust.name} • Balance: ₹${String.format(java.util.Locale.US, "%.2f", balance)}",
            onDismiss = { showLedgerShareOptions = false },
            onShareWhatsApp = {
                ShareHelper.shareLedgerPdfWhatsApp(
                    context, cust, ledgerEntries, totalBilled, totalPaid, balance, company
                )
            },
            onShareWhatsAppBusiness = {
                ShareHelper.shareLedgerPdfWhatsAppBusiness(
                    context, cust, ledgerEntries, totalBilled, totalPaid, balance, company
                )
            },
            onSharePdf = {
                ShareHelper.shareLedgerPdfGeneral(
                    context, cust, ledgerEntries, totalBilled, totalPaid, balance, company
                )
            },
            onShareText = {
                ShareHelper.shareLedgerTextGeneral(
                    context, cust, ledgerEntries, totalBilled, totalPaid, balance, company
                )
            },
            onPrint = {
                InvoicePrinter.printCustomerLedger(
                    context, cust, ledgerEntries, totalBilled, totalPaid, balance, company
                )
            }
        )
    }

    // Invoice Share Options Dialog
    if (selectedBillForShare != null) {
        val b = selectedBillForShare!!
        ShareOptionsDialog(
            title = "Share Tax Invoice",
            subtitle = "Invoice #${b.bill.invoiceNo} • ${b.bill.customerName}",
            onDismiss = { selectedBillForShare = null },
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

    // Invoice Details View Dialog
    if (selectedBillForViewModal != null) {
        InvoiceViewDialog(
            billWithItems = selectedBillForViewModal!!,
            company = company,
            onDismiss = { selectedBillForViewModal = null },
            onEdit = {
                val billToEdit = selectedBillForViewModal!!
                selectedBillForViewModal = null
                viewModel.startEditBill(billToEdit)
            },
            onConvertToInvoice = {
                val b = selectedBillForViewModal
                selectedBillForViewModal = null
                if (b != null) {
                    viewModel.convertQuotationToInvoice(b)
                }
            }
        )
    }

    // Payment Receipt Voucher Dialog
    if (selectedPaymentForReceipt != null) {
        val payment = selectedPaymentForReceipt!!
        val priorBalance = balance + payment.amount
        PaymentReceiptDialog(
            payment = payment,
            customer = cust,
            company = company,
            previousBalance = Math.max(0.0, priorBalance),
            remainingBalance = Math.max(0.0, balance),
            onDismiss = { selectedPaymentForReceipt = null }
        )
    }

    // Edit Customer Opening / Prior Due Balance Dialog
    if (showEditOpeningBalanceDialog) {
        AlertDialog(
            onDismissRequest = { showEditOpeningBalanceDialog = false },
            title = { Text("Customer Opening / Prior Due", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Set the opening outstanding balance for ${cust.displayName}. This amount will appear in the customer ledger and be included in the total outstanding due balance.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editOpeningBalanceInput,
                        onValueChange = { editOpeningBalanceInput = it },
                        label = { Text("Opening Due Balance (₹)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = editOpeningBalanceInput.toDoubleOrNull() ?: 0.0
                        viewModel.updateCustomerOpeningBalance(cust.id, amt)
                        showEditOpeningBalanceDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2410C))
                ) {
                    Text("Apply & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditOpeningBalanceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun EditCustomerPaymentDialog(
    payment: PaymentEntity,
    customerName: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, mode: String, reference: String, notes: String, dateMillis: Long) -> Unit
) {
    val context = LocalContext.current
    var editAmountStr by remember { mutableStateOf(payment.amount.toString().removeSuffix(".0")) }
    var editMode by remember { mutableStateOf(payment.paymentMode) }
    var editRef by remember { mutableStateOf(payment.referenceNo) }
    var editNotes by remember { mutableStateOf(payment.notes) }
    var editDateMillis by remember { mutableStateOf(payment.dateMillis) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Payment Entry", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Customer: $customerName",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp
                )

                // Date Picker Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Payment Date:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.clickable {
                            val cal = java.util.Calendar.getInstance().apply { timeInMillis = editDateMillis }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = java.util.Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth)
                                    }
                                    editDateMillis = newCal.timeInMillis
                                },
                                cal.get(java.util.Calendar.YEAR),
                                cal.get(java.util.Calendar.MONTH),
                                cal.get(java.util.Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Select Date",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${DimensionCalculator.formatDate(editDateMillis)} ✎",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = editAmountStr,
                    onValueChange = { editAmountStr = it },
                    label = { Text("Payment Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Payment Mode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Cash", "UPI / GPay", "Bank Transfer", "Cheque").forEach { mode ->
                        FilterChip(
                            selected = editMode == mode,
                            onClick = { editMode = mode },
                            label = { Text(mode, fontSize = 10.5.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = editRef,
                    onValueChange = { editRef = it },
                    label = { Text("Reference / UTR / Cheque No (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editNotes,
                    onValueChange = { editNotes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = editAmountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSave(amt, editMode, editRef.trim(), editNotes.trim(), editDateMillis)
                    }
                }
            ) {
                Text("Update Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
