package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.PurchaseEntity
import com.example.data.db.PurchasePaymentEntity
import com.example.data.db.PurchaseWithItems
import com.example.data.db.SupplierBalanceSummary
import com.example.data.db.SupplierEntity
import com.example.data.db.SupplierLedgerEntry
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.DoorBillingViewModel
import com.example.util.DimensionCalculator
import com.example.util.InvoicePrinter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseHubScreen(
    viewModel: DoorBillingViewModel
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("New Purchase", "Invoices", "Due Balances", "Suppliers")

    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val purchases by viewModel.allPurchases.collectAsStateWithLifecycle()
    val supplierBalances by viewModel.supplierBalances.collectAsStateWithLifecycle()
    val company by viewModel.companyProfile.collectAsStateWithLifecycle()

    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var viewingPurchaseDetails by remember { mutableStateOf<PurchaseWithItems?>(null) }
    var viewingSupplierLedger by remember { mutableStateOf<SupplierEntity?>(null) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    var supplierToPay by remember { mutableStateOf<SupplierEntity?>(null) }
    var presetPaymentAmount by remember { mutableStateOf<Double?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Purchase Entry & Suppliers", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        modifier = Modifier.testTag("purchase_hub_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            supplierToPay = null
                            presetPaymentAmount = null
                            showRecordPaymentDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.22f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pay Supplier", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = {
                            viewModel.prepareNewSupplier()
                            showAddSupplierDialog = true
                        }
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Supplier", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F766E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFF0FDFA),
                contentColor = Color(0xFF0F766E)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.5.sp
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> PurchaseEntryTab(
                    viewModel = viewModel,
                    onOpenAddSupplier = {
                        viewModel.prepareNewSupplier()
                        showAddSupplierDialog = true
                    },
                    onSaved = {
                        selectedTab = 1
                    }
                )
                1 -> PurchaseInvoicesListTab(
                    purchases = purchases,
                    onViewPurchase = { viewingPurchaseDetails = it },
                    onEditPurchase = {
                        viewModel.startEditPurchase(it)
                        selectedTab = 0
                    },
                    onDeletePurchase = { viewModel.deletePurchase(it.purchase.id) },
                    onNewPurchaseClick = {
                        viewModel.startNewPurchase()
                        selectedTab = 0
                    },
                    onPayInvoice = { p ->
                        val sup = suppliers.find { it.id == p.purchase.supplierId } ?: SupplierEntity(
                            id = p.purchase.supplierId,
                            name = p.purchase.supplierName,
                            mobile = p.purchase.supplierMobile,
                            address = p.purchase.supplierAddress,
                            gstNo = p.purchase.supplierGstNo
                        )
                        supplierToPay = sup
                        val due = Math.max(0.0, p.purchase.grandTotal - p.purchase.paidAmount)
                        presetPaymentAmount = if (due > 0) due else null
                        showRecordPaymentDialog = true
                    }
                )
                2 -> SupplierBalancesTab(
                    supplierBalances = supplierBalances,
                    onOpenLedger = { supplier ->
                        viewModel.openSupplierLedger(supplier)
                        viewingSupplierLedger = supplier
                    },
                    onRecordPayment = { supplier, due ->
                        supplierToPay = supplier
                        presetPaymentAmount = if (due > 0) due else null
                        showRecordPaymentDialog = true
                    }
                )
                3 -> SuppliersManageTab(
                    suppliers = suppliers,
                    onAddSupplier = {
                        viewModel.prepareNewSupplier()
                        showAddSupplierDialog = true
                    },
                    onEditSupplier = { supplier ->
                        viewModel.prepareEditSupplier(supplier)
                        showAddSupplierDialog = true
                    },
                    onDeleteSupplier = { supplier ->
                        viewModel.deleteSupplier(supplier)
                    },
                    onStartPurchaseForSupplier = { supplier ->
                        viewModel.startNewPurchase(supplier)
                        selectedTab = 0
                    },
                    onRecordPaymentForSupplier = { supplier ->
                        supplierToPay = supplier
                        presetPaymentAmount = null
                        showRecordPaymentDialog = true
                    }
                )
            }
        }
    }

    // Record Supplier Payment Dialog
    if (showRecordPaymentDialog) {
        RecordSupplierPaymentDialog(
            initialSupplier = supplierToPay,
            allSuppliers = suppliers,
            supplierBalances = supplierBalances,
            defaultAmount = presetPaymentAmount,
            onDismiss = {
                showRecordPaymentDialog = false
                supplierToPay = null
                presetPaymentAmount = null
            },
            onConfirm = { supId, supName, amt, mode, ref, notes, dateMillis ->
                viewModel.recordSupplierPayment(
                    supplierId = supId,
                    supplierName = supName,
                    amount = amt,
                    mode = mode,
                    reference = ref,
                    notes = notes,
                    dateMillis = dateMillis
                )
                showRecordPaymentDialog = false
                supplierToPay = null
                presetPaymentAmount = null
            }
        )
    }

    // Add / Edit Supplier Dialog
    if (showAddSupplierDialog) {
        val editingId by viewModel.editingSupplierId.collectAsStateWithLifecycle()
        val supName by viewModel.supplierNameInput.collectAsStateWithLifecycle()
        val supMobile by viewModel.supplierMobileInput.collectAsStateWithLifecycle()
        val supAddress by viewModel.supplierAddressInput.collectAsStateWithLifecycle()
        val supGst by viewModel.supplierGstInput.collectAsStateWithLifecycle()

        AlertDialog(
            onDismissRequest = { showAddSupplierDialog = false },
            title = { Text(if (editingId == 0L) "Add New Supplier / Party" else "Edit Supplier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = supName,
                        onValueChange = { viewModel.supplierNameInput.value = it },
                        label = { Text("Supplier / Party Name *") },
                        placeholder = { Text("e.g. Mahavir Plywood & Doors") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("supplier_name_input")
                    )
                    OutlinedTextField(
                        value = supMobile,
                        onValueChange = { viewModel.supplierMobileInput.value = it },
                        label = { Text("Mobile Number") },
                        placeholder = { Text("e.g. 9876543210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = supGst,
                        onValueChange = { viewModel.supplierGstInput.value = it },
                        label = { Text("GSTIN / Tax ID (Optional)") },
                        placeholder = { Text("24AAAAA0000A1Z5") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = supAddress,
                        onValueChange = { viewModel.supplierAddressInput.value = it },
                        label = { Text("Factory / Shop Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveSupplier {
                            showAddSupplierDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                ) {
                    Text("Save Supplier")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSupplierDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Viewing Purchase Bill Details Dialog
    viewingPurchaseDetails?.let { purchaseWithItems ->
        PurchaseDetailsDialog(
            purchaseWithItems = purchaseWithItems,
            onDismiss = { viewingPurchaseDetails = null },
            onEdit = {
                viewingPurchaseDetails = null
                viewModel.startEditPurchase(purchaseWithItems)
                selectedTab = 0
            }
        )
    }

    // Viewing Supplier Ledger Dialog
    viewingSupplierLedger?.let { supplier ->
        SupplierLedgerDialog(
            supplier = supplier,
            viewModel = viewModel,
            onDismiss = { viewingSupplierLedger = null }
        )
    }
}

@Composable
fun PurchaseEntryTab(
    viewModel: DoorBillingViewModel,
    onOpenAddSupplier: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()

    val purchaseId by viewModel.purchaseIdDraft.collectAsStateWithLifecycle()
    val invoiceNo by viewModel.purchaseInvoiceNoDraft.collectAsStateWithLifecycle()
    val selectedSupplier by viewModel.purchaseSupplierDraft.collectAsStateWithLifecycle()
    val dateMillis by viewModel.purchaseDateMillisDraft.collectAsStateWithLifecycle()
    val dimensionUnit by viewModel.purchaseDimensionUnitDraft.collectAsStateWithLifecycle()
    val taxRate by viewModel.purchaseTaxRateDraft.collectAsStateWithLifecycle()
    val isGstIncluded by viewModel.purchaseIsGstIncludedDraft.collectAsStateWithLifecycle()
    val discount by viewModel.purchaseDiscountDraft.collectAsStateWithLifecycle()
    val otherCharges by viewModel.purchaseOtherChargesDraft.collectAsStateWithLifecycle()
    val otherChargesDesc by viewModel.purchaseOtherChargesDescDraft.collectAsStateWithLifecycle()
    val isRoundOffAuto by viewModel.purchaseIsRoundOffAutoDraft.collectAsStateWithLifecycle()
    val roundOffAmount by viewModel.purchaseRoundOffDraft.collectAsStateWithLifecycle()
    val paidAmount by viewModel.purchasePaidAmountDraft.collectAsStateWithLifecycle()
    val notes by viewModel.purchaseNotesDraft.collectAsStateWithLifecycle()
    val items by viewModel.purchaseItemsDraft.collectAsStateWithLifecycle()

    val particular by viewModel.purchaseItemParticularInput.collectAsStateWithLifecycle()
    val hsn by viewModel.purchaseItemHsnInput.collectAsStateWithLifecycle()
    val heightStr by viewModel.purchaseItemHeightInput.collectAsStateWithLifecycle()
    val widthStr by viewModel.purchaseItemWidthInput.collectAsStateWithLifecycle()
    val qtyStr by viewModel.purchaseItemQtyInput.collectAsStateWithLifecycle()
    val rateStr by viewModel.purchaseItemRateInput.collectAsStateWithLifecycle()

    val height = heightStr.toDoubleOrNull() ?: 0.0
    val width = widthStr.toDoubleOrNull() ?: 0.0
    val qty = qtyStr.toIntOrNull() ?: 1
    val rate = rateStr.toDoubleOrNull() ?: 0.0
    val liveSqFt = if (height > 0 && width > 0) DimensionCalculator.calculateSqFt(height, width, qty, dimensionUnit) else qty.toDouble()
    val liveAmount = if (height > 0 && width > 0) DimensionCalculator.calculateAmount(liveSqFt, rate) else (qty * rate)

    val subTotal = items.sumOf { it.amount }
    val effectiveTaxRate = if (isGstIncluded) taxRate else 0.0
    val gstTotal = if (effectiveTaxRate > 0) (subTotal * effectiveTaxRate / 100.0) else 0.0
    val rawGrandTotal = Math.max(0.0, (subTotal + gstTotal + otherCharges) - discount)
    val calculatedRoundOff = if (isRoundOffAuto) {
        val rounded = Math.round(rawGrandTotal).toDouble()
        rounded - rawGrandTotal
    } else {
        roundOffAmount
    }
    val grandTotal = Math.max(0.0, rawGrandTotal + calculatedRoundOff)

    var showSupplierDropdown by remember { mutableStateOf(false) }
    var showDimensions by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section 1: Supplier & Date
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (purchaseId == 0L) "New Purchase Bill" else "Edit Purchase Bill",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F766E)
                        )

                        // Manual Date Selection
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFCCFBF1),
                            modifier = Modifier.clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance().apply {
                                            set(year, month, dayOfMonth)
                                        }
                                        viewModel.setPurchaseDate(newCal.timeInMillis)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = "Date", tint = Color(0xFF0F766E), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${DimensionCalculator.formatDate(dateMillis)} ✎",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F766E)
                                )
                            }
                        }
                    }

                    // Supplier Selection Box
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedSupplier?.let { "${it.name}${if (it.mobile.isNotBlank()) " (${it.mobile})" else ""}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Supplier / Vendor *") },
                            placeholder = { Text("Tap to select supplier") },
                            leadingIcon = {
                                Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF0F766E))
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = {
                                            viewModel.prepareNewSupplier()
                                            onOpenAddSupplier()
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp)
                                    ) {
                                        Text("+ New", fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                                    }
                                    IconButton(onClick = { showSupplierDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color(0xFF0F766E))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Transparent click interceptor covering the text area to guarantee tap opens dialog
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(end = 85.dp)
                                .clickable { showSupplierDropdown = true }
                        )
                    }

                    if (showSupplierDropdown) {
                        var searchQuery by remember { mutableStateOf("") }
                        val filteredSuppliers = remember(suppliers, searchQuery) {
                            if (searchQuery.isBlank()) suppliers
                            else suppliers.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                it.mobile.contains(searchQuery, ignoreCase = true) ||
                                it.gstNo.contains(searchQuery, ignoreCase = true)
                            }
                        }

                        AlertDialog(
                            onDismissRequest = { showSupplierDropdown = false },
                            title = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Select Supplier", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F766E))
                                    }
                                    IconButton(onClick = { showSupplierDropdown = false }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                                    }
                                }
                            },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Search Bar
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search supplier / mobile...") },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF0F766E)) },
                                        trailingIcon = {
                                            if (searchQuery.isNotBlank()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    if (filteredSuppliers.isEmpty() && suppliers.isEmpty()) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDFA)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "Abhi koi supplier add nahi hai.",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF0F766E)
                                                )
                                                Text(
                                                    text = "Naya supplier create karne ke liye niche button par tap karein.",
                                                    fontSize = 11.5.sp,
                                                    color = Color.Gray,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                                Button(
                                                    onClick = {
                                                        showSupplierDropdown = false
                                                        viewModel.prepareNewSupplier()
                                                        onOpenAddSupplier()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("+ Create New Supplier")
                                                }
                                            }
                                        }
                                    } else if (filteredSuppliers.isEmpty()) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("No supplier matches '$searchQuery'", fontSize = 12.5.sp, color = Color.Gray)
                                                Button(
                                                    onClick = {
                                                        showSupplierDropdown = false
                                                        viewModel.prepareNewSupplier()
                                                        viewModel.supplierNameInput.value = searchQuery.trim()
                                                        onOpenAddSupplier()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("+ Create '$searchQuery'")
                                                }
                                            }
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 280.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(filteredSuppliers) { sup ->
                                                val isSelected = selectedSupplier?.id == sup.id
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            viewModel.purchaseSupplierDraft.value = sup
                                                            showSupplierDropdown = false
                                                        },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isSelected) Color(0xFFCCFBF1) else Color(0xFFF8FAFC),
                                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0F766E)) else androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = if (isSelected) Color(0xFF0F766E) else Color(0xFF0F766E).copy(alpha = 0.12f),
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    Icons.Default.Business,
                                                                    contentDescription = null,
                                                                    tint = if (isSelected) Color.White else Color(0xFF0F766E),
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = sup.name,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.5.sp,
                                                                color = if (isSelected) Color(0xFF0F766E) else Color(0xFF0F172A)
                                                            )
                                                            if (sup.mobile.isNotBlank()) {
                                                                Text("📞 ${sup.mobile}", fontSize = 11.5.sp, color = Color.DarkGray)
                                                            }
                                                            if (sup.gstNo.isNotBlank()) {
                                                                Text("GST: ${sup.gstNo}", fontSize = 10.5.sp, color = Color.Gray)
                                                            }
                                                            if (sup.address.isNotBlank()) {
                                                                Text(sup.address, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                                            }
                                                        }
                                                        if (isSelected) {
                                                            Text("✓", color = Color(0xFF0F766E), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Quick pick from customer list if available
                                    if (allCustomers.isNotEmpty()) {
                                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                                        Text("Or select from Customers:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                        LazyColumn(
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            items(allCustomers.take(5)) { cust ->
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            val existing = suppliers.find { it.name.equals(cust.name, ignoreCase = true) }
                                                            if (existing != null) {
                                                                viewModel.purchaseSupplierDraft.value = existing
                                                            } else {
                                                                val newSup = com.example.data.db.SupplierEntity(
                                                                    name = cust.name,
                                                                    mobile = cust.mobile,
                                                                    address = cust.address,
                                                                    gstNo = cust.gstNo
                                                                )
                                                                viewModel.saveSupplierDirect(newSup) { saved ->
                                                                    viewModel.purchaseSupplierDraft.value = saved
                                                                }
                                                            }
                                                            showSupplierDropdown = false
                                                        },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFF1F5F9)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(cust.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                        Text("Customer ➔", fontSize = 10.sp, color = Color(0xFF0F766E))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showSupplierDropdown = false
                                        viewModel.prepareNewSupplier()
                                        onOpenAddSupplier()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ New Supplier")
                                }
                            },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = { showSupplierDropdown = false },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Close")
                                }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = invoiceNo,
                            onValueChange = { viewModel.purchaseInvoiceNoDraft.value = it },
                            label = { Text("Purchase Bill No") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )

                        // Dimension Unit toggle
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Inches", "MM", "Feet").forEach { u ->
                                    FilterChip(
                                        selected = dimensionUnit == u,
                                        onClick = { viewModel.purchaseDimensionUnitDraft.value = u },
                                        label = { Text(u, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Item Entry (Sl.no | Particular | Qty | Rate | Amount)
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add Item (Sl.no: ${items.size + 1})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F766E)
                        )
                        Text(
                            text = "Format: Sl.no | Particular | Qty | Rate | Amount",
                            fontSize = 10.5.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 1. Particular
                    OutlinedTextField(
                        value = particular,
                        onValueChange = { viewModel.purchaseItemParticularInput.value = it },
                        label = { Text("Particular / Item Name *") },
                        placeholder = { Text("e.g. Lamination Door, Flush Door 30mm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("purchase_particular_input")
                    )

                    // Quick Item Suggestions (Including Lamination Door)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Lamination Door", "Lamination Door 30mm", "Flush Door 30mm", "Flush Door 35mm", "Teak Wood Door", "Pine Wood Door", "Core Door", "Plywood 18mm", "Door Skin").forEach { sug ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { viewModel.purchaseItemParticularInput.value = sug }
                            ) {
                                Text(
                                    text = sug,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // 2. Qty & Rate (Sl.no | Particular | Qty | Rate | Amount)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = qtyStr,
                            onValueChange = { viewModel.purchaseItemQtyInput.value = it },
                            label = { Text("Qty (Pcs) *") },
                            placeholder = { Text("e.g. 10") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("purchase_qty_input")
                        )
                        OutlinedTextField(
                            value = rateStr,
                            onValueChange = { viewModel.purchaseItemRateInput.value = it },
                            label = { Text("Rate (₹) *") },
                            placeholder = { Text("Rate per piece") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("purchase_rate_input")
                        )
                    }

                    // Optional Dimensions toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showDimensions = !showDimensions },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (showDimensions) "▲ Hide Dimensions" else "▼ Add Height & Width (Optional)",
                                fontSize = 11.5.sp,
                                color = Color(0xFF0F766E)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showDimensions) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = heightStr,
                                onValueChange = { viewModel.purchaseItemHeightInput.value = it },
                                label = { Text("Height ($dimensionUnit)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = widthStr,
                                onValueChange = { viewModel.purchaseItemWidthInput.value = it },
                                label = { Text("Width ($dimensionUnit)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Live Amount Strip: Sl.no | Particular | Qty | Rate | Amount
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Qty: $qty pcs × ₹${if (rate > 0) String.format(java.util.Locale.US, "%.2f", rate) else "0"}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (height > 0 && width > 0) {
                                    Text(
                                        text = "Size: $height x $width ($dimensionUnit) | ${String.format(java.util.Locale.US, "%.2f", liveSqFt)} Sq.Ft",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF0F766E)
                                    )
                                }
                            }
                            Text(
                                text = "Amount: ₹${String.format(java.util.Locale.US, "%.2f", liveAmount)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E)
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.addOrUpdateItemToPurchase() },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Item (Sl.no ${items.size + 1})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 3: Added Items Table (Sl.no | Particular | Qty | Rate | Amount)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PURCHASE ITEMS TABLE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F766E),
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "${items.size} Items",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (items.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Format: Sl.no | Particular | Qty | Rate | Amount",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color(0xFF0F766E)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No items added yet. Fill Particular, Qty & Rate above to add items to bill.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Table Header Row: Sl.no | Particular | Qty | Rate | Amount
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F766E))
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sl.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.width(32.dp))
                            Text("Particular", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.weight(2f))
                            Text("Qty", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.weight(0.7f))
                            Text("Rate (₹)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.weight(1.1f))
                            Text("Amount (₹)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.weight(1.3f))
                            Spacer(modifier = Modifier.width(28.dp))
                        }

                        // Table Data Rows
                        items.forEachIndexed { index, item ->
                            val rowBg = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.slNo}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(32.dp)
                                )
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(
                                        text = item.particular,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (item.height > 0 && item.width > 0) {
                                        Text(
                                            text = "${DimensionCalculator.formatDimension(item.height)}x${DimensionCalculator.formatDimension(item.width)} (${String.format(java.util.Locale.US, "%.1f", item.sqFt)}sqft)",
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "${item.qty}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(0.7f)
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%.2f", item.rate),
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1.1f)
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%.2f", item.amount),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F766E),
                                    modifier = Modifier.weight(1.3f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeItemFromPurchase(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }

                        // Table Summary Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F766E).copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF0F766E), modifier = Modifier.width(32.dp))
                            Text("${items.size} items", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF0F766E), modifier = Modifier.weight(2f))
                            Text("${items.sumOf { it.qty }}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F766E), modifier = Modifier.weight(0.7f))
                            Text("-", fontSize = 11.5.sp, color = Color(0xFF0F766E), modifier = Modifier.weight(1.1f))
                            Text(
                                "₹${String.format(java.util.Locale.US, "%.2f", subTotal)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                color = Color(0xFF0F766E),
                                modifier = Modifier.weight(1.3f)
                            )
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                    }
                }
            }
        }

        // Section 4: Taxes, Round off & Grand Total
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // GST Toggle & Rates
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = isGstIncluded,
                                onCheckedChange = { viewModel.purchaseIsGstIncludedDraft.value = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GST Applicable", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (isGstIncluded) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(5.0, 12.0, 18.0, 28.0).forEach { r ->
                                    FilterChip(
                                        selected = taxRate == r,
                                        onClick = { viewModel.purchaseTaxRateDraft.value = r },
                                        label = { Text("${r.toInt()}%", fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // Other Charges & Discount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (otherCharges == 0.0) "" else otherCharges.toString(),
                            onValueChange = { viewModel.purchaseOtherChargesDraft.value = it.toDoubleOrNull() ?: 0.0 },
                            label = { Text("Transportation / Other (₹)", color = Color.Black) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = if (discount == 0.0) "" else discount.toString(),
                            onValueChange = { viewModel.purchaseDiscountDraft.value = it.toDoubleOrNull() ?: 0.0 },
                            label = { Text("Discount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Round Off Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = isRoundOffAuto,
                                onCheckedChange = { viewModel.purchaseIsRoundOffAutoDraft.value = it },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Auto Round Off Total",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        Text(
                            text = (if (calculatedRoundOff >= 0) "+ " else "- ") + DimensionCalculator.formatCurrency(Math.abs(calculatedRoundOff)),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Grand Total Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F766E),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("GRAND TOTAL", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = DimensionCalculator.formatCurrency(grandTotal),
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Supplier Balance", color = Color(0xFFFEF3C7), fontSize = 11.sp)
                                Text(
                                    text = DimensionCalculator.formatCurrency(Math.max(0.0, grandTotal - paidAmount)),
                                    color = Color(0xFFFDE68A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    // Paid / Advance Amount
                    OutlinedTextField(
                        value = if (paidAmount == 0.0) "" else paidAmount.toString(),
                        onValueChange = { viewModel.purchasePaidAmountDraft.value = it.toDoubleOrNull() ?: 0.0 },
                        label = { Text("Payment Made Now (₹)") },
                        placeholder = { Text("Amount paid to supplier") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.purchaseNotesDraft.value = it },
                        label = { Text("Notes / Vehicle / Gate Pass No (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.saveCurrentPurchase {
                                onSaved()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Purchase Bill", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseInvoicesListTab(
    purchases: List<PurchaseWithItems>,
    onViewPurchase: (PurchaseWithItems) -> Unit,
    onEditPurchase: (PurchaseWithItems) -> Unit,
    onDeletePurchase: (PurchaseWithItems) -> Unit,
    onNewPurchaseClick: () -> Unit,
    onPayInvoice: (PurchaseWithItems) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(purchases, searchQuery) {
        if (searchQuery.isBlank()) purchases
        else purchases.filter {
            it.purchase.invoiceNo.contains(searchQuery, ignoreCase = true) ||
            it.purchase.supplierName.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search supplier or purchase invoice...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onNewPurchaseClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("+ Bill", fontSize = 12.sp)
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No purchase bills found", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tap '+ Bill' or 'New Purchase' tab to record purchases.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        } else {
            items(filtered) { p ->
                val purchase = p.purchase
                val due = Math.max(0.0, purchase.grandTotal - purchase.paidAmount)
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().clickable { onViewPurchase(p) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(purchase.invoiceNo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F766E))
                            Text(DimensionCalculator.formatDate(purchase.dateMillis), fontSize = 12.sp, color = Color.Gray)
                        }
                        Text("Supplier: ${purchase.supplierName}", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        Text("${p.items.size} items | Unit: ${purchase.dimensionUnit}", fontSize = 11.5.sp, color = Color.Gray)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Total: ${DimensionCalculator.formatCurrency(purchase.grandTotal)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F766E)
                                )
                                if (due > 0) {
                                    Text(
                                        "Due: ${DimensionCalculator.formatCurrency(due)}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFFDC2626)
                                    )
                                } else {
                                    Text(
                                        "Paid in Full ✓",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (due > 0) {
                                    Button(
                                        onClick = { onPayInvoice(p) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                OutlinedButton(
                                    onClick = { onViewPurchase(p) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("View", fontSize = 11.5.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = { onEditPurchase(p) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Edit", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupplierBalancesTab(
    supplierBalances: List<SupplierBalanceSummary>,
    onOpenLedger: (SupplierEntity) -> Unit,
    onRecordPayment: (SupplierEntity, Double) -> Unit
) {
    val totalPurchasedAll = supplierBalances.sumOf { it.totalPurchased }
    val totalPaidAll = supplierBalances.sumOf { it.totalPaid }
    val totalOutstanding = supplierBalances.sumOf { it.balance }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Banner
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (totalOutstanding > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "TOTAL SUPPLIER OUTSTANDING DUE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalOutstanding > 0) Color(0xFFDC2626) else Color(0xFF0F766E)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DimensionCalculator.formatCurrency(totalOutstanding),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (totalOutstanding > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Purchased: ${DimensionCalculator.formatCurrency(totalPurchasedAll)}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Total Paid: ${DimensionCalculator.formatCurrency(totalPaidAll)}", fontSize = 11.5.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("SUPPLIER BALANCES (${supplierBalances.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F766E))
        }

        if (supplierBalances.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text("No supplier balances found yet. Create a supplier and add purchase bills to track dues.", modifier = Modifier.padding(16.dp), fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(supplierBalances) { summary ->
                val sup = summary.supplier
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sup.name, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                if (sup.mobile.isNotBlank()) Text("📞 ${sup.mobile}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Purchased: ${DimensionCalculator.formatCurrency(summary.totalPurchased)} | Paid: ${DimensionCalculator.formatCurrency(summary.totalPaid)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Balance Due", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = DimensionCalculator.formatCurrency(summary.balance),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (summary.balance > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onRecordPayment(sup, summary.balance) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (summary.balance > 0) "Pay ₹" else "Record Payment", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onOpenLedger(sup) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ledger Statement", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuppliersManageTab(
    suppliers: List<SupplierEntity>,
    onAddSupplier: () -> Unit,
    onEditSupplier: (SupplierEntity) -> Unit,
    onDeleteSupplier: (SupplierEntity) -> Unit,
    onStartPurchaseForSupplier: (SupplierEntity) -> Unit,
    onRecordPaymentForSupplier: (SupplierEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ALL SUPPLIERS / PARTIES (${suppliers.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F766E))
                Button(
                    onClick = onAddSupplier,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Supplier", fontSize = 12.sp)
                }
            }
        }

        if (suppliers.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text("No suppliers added yet. Tap '+ Add Supplier' above to create suppliers for purchase entries.", modifier = Modifier.padding(16.dp), fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(suppliers) { sup ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sup.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F766E))
                            Row {
                                IconButton(onClick = { onEditSupplier(sup) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { onDeleteSupplier(sup) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        if (sup.mobile.isNotBlank()) Text("📞 Mobile: ${sup.mobile}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        if (sup.gstNo.isNotBlank()) Text("🏛️ GSTIN: ${sup.gstNo}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (sup.address.isNotBlank()) Text("📍 Address: ${sup.address}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onRecordPaymentForSupplier(sup) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pay Supplier", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onStartPurchaseForSupplier(sup) },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Purchase Bill", color = Color(0xFF0F766E), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseDetailsDialog(
    purchaseWithItems: PurchaseWithItems,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val purchase = purchaseWithItems.purchase
    val items = purchaseWithItems.items

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF0F766E)).padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Purchase Bill Details", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Text(purchase.invoiceNo, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("Supplier: ${purchase.supplierName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (purchase.supplierMobile.isNotBlank()) Text("Mobile: ${purchase.supplierMobile}", fontSize = 12.sp)
                        Text("Date: ${DimensionCalculator.formatDate(purchase.dateMillis)}", fontSize = 12.sp)
                        Text("Unit: ${purchase.dimensionUnit}", fontSize = 12.sp)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Header: Sl.no | Particular | Qty | Rate | Amount
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F766E))
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Sl.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                                    Text("Particular", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.8f))
                                    Text("Qty", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.7f))
                                    Text("Rate (₹)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.1f))
                                    Text("Amount (₹)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.3f))
                                }

                                items.forEachIndexed { index, item ->
                                    val rowBg = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(rowBg)
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${item.slNo}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.width(32.dp)
                                        )
                                        Column(modifier = Modifier.weight(2f)) {
                                            Text(
                                                text = item.particular,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (item.height > 0 && item.width > 0) {
                                                Text(
                                                    text = "${DimensionCalculator.formatDimension(item.height)}x${DimensionCalculator.formatDimension(item.width)}",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${item.qty}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(0.7f)
                                        )
                                        Text(
                                            text = String.format(java.util.Locale.US, "%.2f", item.rate),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1.1f)
                                        )
                                        Text(
                                            text = String.format(java.util.Locale.US, "%.2f", item.amount),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F766E),
                                            modifier = Modifier.weight(1.3f)
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
                                }

                                // Total Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F766E).copy(alpha = 0.14f))
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Total", fontWeight = FontWeight.ExtraBold, fontSize = 11.5.sp, color = Color(0xFF0F766E), modifier = Modifier.width(32.dp))
                                    Text("${items.size} items", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F766E), modifier = Modifier.weight(2f))
                                    Text("${items.sumOf { it.qty }}", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF0F766E), modifier = Modifier.weight(0.7f))
                                    Text("-", fontSize = 11.sp, color = Color(0xFF0F766E), modifier = Modifier.weight(1.1f))
                                    Text(
                                        "₹${String.format(java.util.Locale.US, "%.2f", purchase.subTotal)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF0F766E),
                                        modifier = Modifier.weight(1.3f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sub Total", fontSize = 12.sp)
                            Text("₹${String.format(java.util.Locale.US, "%.2f", purchase.subTotal)}", fontSize = 12.sp)
                        }
                        if (purchase.isGstIncluded && purchase.taxRate > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("GST (${purchase.taxRate.toInt()}%)", fontSize = 12.sp)
                                Text("₹${String.format(java.util.Locale.US, "%.2f", purchase.cgstAmount + purchase.sgstAmount + purchase.igstAmount)}", fontSize = 12.sp)
                            }
                        }
                        if (purchase.otherCharges > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Other Charges (${purchase.otherChargesDescription})", fontSize = 12.sp)
                                Text("₹${String.format(java.util.Locale.US, "%.2f", purchase.otherCharges)}", fontSize = 12.sp)
                            }
                        }
                        if (purchase.discountAmount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount", fontSize = 12.sp)
                                Text("- ₹${String.format(java.util.Locale.US, "%.2f", purchase.discountAmount)}", fontSize = 12.sp)
                            }
                        }
                        if (purchase.roundOffAmount != 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Round Off", fontSize = 12.sp)
                                Text((if (purchase.roundOffAmount >= 0) "+ " else "") + DimensionCalculator.formatCurrency(purchase.roundOffAmount), fontSize = 12.sp)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("GRAND TOTAL", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F766E))
                            Text(DimensionCalculator.formatCurrency(purchase.grandTotal), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F766E))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Paid Amount", fontSize = 12.sp, color = Color(0xFF16A34A))
                            Text(DimensionCalculator.formatCurrency(purchase.paidAmount), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF16A34A))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Close") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Bill")
                    }
                }
            }
        }
    }
}

@Composable
fun SupplierLedgerDialog(
    supplier: SupplierEntity,
    viewModel: DoorBillingViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val ledgerEntries by viewModel.supplierLedgerEntries.collectAsStateWithLifecycle()
    val company by viewModel.companyProfile.collectAsStateWithLifecycle()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showPdfViewerDialog by remember { mutableStateOf(false) }
    var payAmountStr by remember { mutableStateOf("") }
    var payMode by remember { mutableStateOf("Bank Transfer") }
    var payRef by remember { mutableStateOf("") }
    var payNotes by remember { mutableStateOf("") }
    var payDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var editingSupplierPayment by remember { mutableStateOf<PurchasePaymentEntity?>(null) }
    var deletingSupplierPayment by remember { mutableStateOf<PurchasePaymentEntity?>(null) }

    val totalPurchased = ledgerEntries.filterIsInstance<SupplierLedgerEntry.PurchaseBillEntry>().sumOf { it.grandTotal }
    val totalPaid = ledgerEntries.filterIsInstance<SupplierLedgerEntry.PaymentRecord>().sumOf { it.payment.amount }
    val balance = totalPurchased - totalPaid

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF0F766E)).padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Supplier Ledger: ${supplier.name}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Text("Due Balance: ${DimensionCalculator.formatCurrency(balance)}", color = Color(0xFFFEF3C7), fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { showPdfViewerDialog = true }) {
                            Icon(Icons.Default.Visibility, contentDescription = "View PDF", tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                InvoicePrinter.printSupplierLedger(
                                    context = context,
                                    supplier = supplier,
                                    ledgerEntries = ledgerEntries,
                                    totalPurchased = totalPurchased,
                                    totalPaid = totalPaid,
                                    balance = balance,
                                    company = company
                                )
                            }
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Print", tint = Color.White)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Action Bar: + Record Payment Out | View PDF | Print
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            payDateMillis = System.currentTimeMillis()
                            showPaymentDialog = true
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Pay Out", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showPdfViewerDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            InvoicePrinter.printSupplierLedger(
                                context = context,
                                supplier = supplier,
                                ledgerEntries = ledgerEntries,
                                totalPurchased = totalPurchased,
                                totalPaid = totalPaid,
                                balance = balance,
                                company = company
                            )
                        },
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Print", fontSize = 12.sp)
                    }
                }

                // Entries List
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("TRANSACTIONS (${ledgerEntries.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    }

                    if (ledgerEntries.isEmpty()) {
                        item {
                            Text("No purchase bills or payments found for this supplier.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                        }
                    } else {
                        items(ledgerEntries) { entry ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(entry.description, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                            Text(DimensionCalculator.formatDate(entry.dateMillis), fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            if (entry.creditAmount > 0) {
                                                Text("+ ${DimensionCalculator.formatCurrency(entry.creditAmount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F766E))
                                                Text("Purchase Bill", fontSize = 10.sp, color = Color.Gray)
                                            } else {
                                                Text("- ${DimensionCalculator.formatCurrency(entry.debitAmount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF16A34A))
                                                Text("Paid Out", fontSize = 10.sp, color = Color(0xFF16A34A))
                                            }
                                        }
                                    }

                                    if (entry is SupplierLedgerEntry.PaymentRecord) {
                                        if (entry.payment.notes.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Note: ${entry.payment.notes}",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { editingSupplierPayment = entry.payment },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(13.dp), tint = Color(0xFF0F766E))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Edit", fontSize = 11.sp, color = Color(0xFF0F766E), fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            TextButton(
                                                onClick = { deletingSupplierPayment = entry.payment },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.error)
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Delete", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
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
    }

    // Payment Out Dialog with Manual Date Picker
    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Record Payment to Supplier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Supplier: ${supplier.name}", fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                    Text("Due Balance: ${DimensionCalculator.formatCurrency(balance)}", fontSize = 12.sp)

                    // Date Picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Payment Date:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFCCFBF1),
                            modifier = Modifier.clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = payDateMillis }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                                        payDateMillis = newCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0F766E))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${DimensionCalculator.formatDate(payDateMillis)} ✎", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = payAmountStr,
                        onValueChange = { payAmountStr = it },
                        label = { Text("Payment Amount (₹) *") },
                        placeholder = { Text("e.g. 10000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Payment Mode", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Bank Transfer", "UPI / GPay", "Cheque", "Cash").forEach { m ->
                            FilterChip(
                                selected = payMode == m,
                                onClick = { payMode = m },
                                label = { Text(m, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = payRef,
                        onValueChange = { payRef = it },
                        label = { Text("Reference / UTR / Cheque No") },
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
                            viewModel.recordSupplierPayment(
                                supplierId = supplier.id,
                                supplierName = supplier.name,
                                amount = amt,
                                mode = payMode,
                                reference = payRef.trim(),
                                notes = payNotes.trim(),
                                dateMillis = payDateMillis,
                                onSuccess = {
                                    showPaymentDialog = false
                                    payAmountStr = ""
                                    payRef = ""
                                    payNotes = ""
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Save Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Supplier Payment Dialog
    if (editingSupplierPayment != null) {
        val sp = editingSupplierPayment!!
        var editPayAmountStr by remember { mutableStateOf(sp.amount.toString().removeSuffix(".0")) }
        var editPayMode by remember { mutableStateOf(sp.paymentMode) }
        var editPayRef by remember { mutableStateOf(sp.referenceNo) }
        var editPayNotes by remember { mutableStateOf(sp.notes) }
        var editPayDateMillis by remember { mutableStateOf(sp.dateMillis) }

        AlertDialog(
            onDismissRequest = { editingSupplierPayment = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Supplier Payment", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Supplier: ${supplier.name}", fontWeight = FontWeight.Bold, color = Color(0xFF0F766E), fontSize = 13.sp)

                    // Date Picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Payment Date:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFCCFBF1),
                            modifier = Modifier.clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = editPayDateMillis }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                                        editPayDateMillis = newCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0F766E))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${DimensionCalculator.formatDate(editPayDateMillis)} ✎", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editPayAmountStr,
                        onValueChange = { editPayAmountStr = it },
                        label = { Text("Payment Amount (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Payment Mode", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Bank Transfer", "UPI / GPay", "Cheque", "Cash").forEach { m ->
                            FilterChip(
                                selected = editPayMode == m,
                                onClick = { editPayMode = m },
                                label = { Text(m, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editPayRef,
                        onValueChange = { editPayRef = it },
                        label = { Text("Reference / UTR / Cheque No") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPayNotes,
                        onValueChange = { editPayNotes = it },
                        label = { Text("Notes (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = editPayAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.updateSupplierPayment(
                                paymentId = sp.id,
                                supplierId = supplier.id,
                                supplierName = supplier.name,
                                amount = amt,
                                mode = editPayMode,
                                reference = editPayRef.trim(),
                                notes = editPayNotes.trim(),
                                dateMillis = editPayDateMillis,
                                onSuccess = {
                                    editingSupplierPayment = null
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                ) {
                    Text("Update Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSupplierPayment = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Supplier Payment Dialog
    if (deletingSupplierPayment != null) {
        val sp = deletingSupplierPayment!!
        AlertDialog(
            onDismissRequest = { deletingSupplierPayment = null },
            title = { Text("Delete Payment Entry?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete this payment of ${DimensionCalculator.formatCurrency(sp.amount)} to ${supplier.name} recorded on ${DimensionCalculator.formatDate(sp.dateMillis)}? Outstanding due balance will increase.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSupplierPayment(sp.id) {
                            deletingSupplierPayment = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSupplierPayment = null }) { Text("Cancel") }
            }
        )
    }

    // PDF Viewer Dialog for Supplier Ledger
    if (showPdfViewerDialog) {
        Dialog(
            onDismissRequest = { showPdfViewerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF0F766E)).padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Supplier Ledger PDF - ${supplier.name}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    InvoicePrinter.printSupplierLedger(
                                        context = context,
                                        supplier = supplier,
                                        ledgerEntries = ledgerEntries,
                                        totalPurchased = totalPurchased,
                                        totalPaid = totalPaid,
                                        balance = balance,
                                        company = company
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Print, contentDescription = "Print", tint = Color.White)
                            }
                            IconButton(onClick = { showPdfViewerDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }

                    val htmlContent = remember(supplier, ledgerEntries, totalPurchased, totalPaid, balance, company) {
                        InvoicePrinter.generateSupplierLedgerHtml(
                            supplier = supplier,
                            ledgerEntries = ledgerEntries,
                            totalPurchased = totalPurchased,
                            totalPaid = totalPaid,
                            balance = balance,
                            company = company
                        )
                    }

                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFFE2E8F0)).padding(8.dp)
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

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("A4 Statement Preview", fontSize = 12.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showPdfViewerDialog = false }) { Text("Close") }
                            Button(
                                onClick = {
                                    InvoicePrinter.printSupplierLedger(
                                        context = context,
                                        supplier = supplier,
                                        ledgerEntries = ledgerEntries,
                                        totalPurchased = totalPurchased,
                                        totalPaid = totalPaid,
                                        balance = balance,
                                        company = company
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
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
}

@Composable
fun RecordSupplierPaymentDialog(
    initialSupplier: SupplierEntity?,
    allSuppliers: List<SupplierEntity>,
    supplierBalances: List<SupplierBalanceSummary>,
    defaultAmount: Double?,
    onDismiss: () -> Unit,
    onConfirm: (supplierId: Long, supplierName: String, amount: Double, mode: String, reference: String, notes: String, dateMillis: Long) -> Unit
) {
    var selectedSupplier by remember { mutableStateOf(initialSupplier ?: allSuppliers.firstOrNull()) }
    var amountStr by remember { mutableStateOf(if (defaultAmount != null && defaultAmount > 0) String.format(java.util.Locale.US, "%.2f", defaultAmount) else "") }
    var selectedMode by remember { mutableStateOf("Cash") }
    var referenceStr by remember { mutableStateOf("") }
    var notesStr by remember { mutableStateOf("") }
    var supplierDropdownExpanded by remember { mutableStateOf(false) }

    val currentSummary = remember(selectedSupplier, supplierBalances) {
        supplierBalances.find { it.supplier.id == selectedSupplier?.id }
    }
    val currentDue = currentSummary?.balance ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Record Supplier Payment", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Supplier Selection
                Column {
                    Text("Select Supplier / Party *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth().clickable { supplierDropdownExpanded = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedSupplier?.name ?: "Tap to choose supplier",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (selectedSupplier != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = supplierDropdownExpanded,
                            onDismissRequest = { supplierDropdownExpanded = false }
                        ) {
                            if (allSuppliers.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No suppliers found. Please add a supplier first.") },
                                    onClick = { supplierDropdownExpanded = false }
                                )
                            } else {
                                allSuppliers.forEach { sup ->
                                    val bal = supplierBalances.find { it.supplier.id == sup.id }?.balance ?: 0.0
                                    DropdownMenuItem(
                                        text = {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(sup.name, fontWeight = FontWeight.SemiBold)
                                                if (bal > 0) {
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text("Due: ₹${String.format(java.util.Locale.US, "%.2f", bal)}", color = Color(0xFFDC2626), fontSize = 12.sp)
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedSupplier = sup
                                            supplierDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Balance indicator
                if (selectedSupplier != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (currentDue > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Outstanding Balance Due", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = DimensionCalculator.formatCurrency(currentDue),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentDue > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                                )
                            }
                            if (currentDue > 0) {
                                OutlinedButton(
                                    onClick = { amountStr = String.format(java.util.Locale.US, "%.2f", currentDue) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Pay Full Due", fontSize = 10.5.sp)
                                }
                            }
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Payment Amount (₹) *") },
                    placeholder = { Text("e.g. 5000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Payment Mode chips
                Column {
                    Text("Payment Mode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Cash", "UPI", "Bank / NEFT", "Cheque").forEach { mode ->
                            val isSelected = selectedMode == mode
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFF0F766E) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedMode = mode }
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Reference / Cheque No
                OutlinedTextField(
                    value = referenceStr,
                    onValueChange = { referenceStr = it },
                    label = { Text("Reference / UTR / Cheque No. (Optional)") },
                    placeholder = { Text("e.g. UPI transaction ID or cheque #") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notesStr,
                    onValueChange = { notesStr = it },
                    label = { Text("Notes / Remarks (Optional)") },
                    placeholder = { Text("e.g. Paid advance or partial payment") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sup = selectedSupplier
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (sup != null && amt > 0) {
                        onConfirm(
                            sup.id,
                            sup.name,
                            amt,
                            selectedMode,
                            referenceStr.trim(),
                            notesStr.trim(),
                            System.currentTimeMillis()
                        )
                    }
                },
                enabled = selectedSupplier != null && (amountStr.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
            ) {
                Text("Confirm Payment", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
