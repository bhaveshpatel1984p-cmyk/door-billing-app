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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.db.PurchaseReturnEntity
import com.example.data.db.PurchaseReturnItemEntity
import com.example.data.db.PurchaseReturnWithItems
import com.example.data.db.RawMaterialCatalogEntity
import java.io.File
import android.net.Uri
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
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateListOf
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
    val tabs = listOf("New Purchase", "Invoices", "Debit Notes", "Due Balances", "Suppliers")

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
    var showRecordReturnDialog by remember { mutableStateOf(false) }
    var returnPresetPurchase by remember { mutableStateOf<PurchaseWithItems?>(null) }
    var returnPresetSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var fullPhotoViewUri by remember { mutableStateOf<String?>(null) }

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
                    },
                    onViewPhoto = { photoUri ->
                        fullPhotoViewUri = photoUri
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
                2 -> PurchaseReturnsListTab(
                    viewModel = viewModel,
                    company = company,
                    onNewReturnClick = {
                        returnPresetPurchase = null
                        returnPresetSupplier = null
                        showRecordReturnDialog = true
                    }
                )
                3 -> SupplierBalancesTab(
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
                4 -> SuppliersManageTab(
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
            company = company,
            onDismiss = { viewingPurchaseDetails = null },
            onEdit = {
                viewingPurchaseDetails = null
                viewModel.startEditPurchase(purchaseWithItems)
                selectedTab = 0
            },
            onCreateReturn = {
                val sup = suppliers.find { it.id == purchaseWithItems.purchase.supplierId } ?: SupplierEntity(
                    id = purchaseWithItems.purchase.supplierId,
                    name = purchaseWithItems.purchase.supplierName,
                    mobile = purchaseWithItems.purchase.supplierMobile,
                    address = purchaseWithItems.purchase.supplierAddress,
                    gstNo = purchaseWithItems.purchase.supplierGstNo
                )
                returnPresetPurchase = purchaseWithItems
                returnPresetSupplier = sup
                showRecordReturnDialog = true
                viewingPurchaseDetails = null
            },
            onViewPhoto = { photoUri ->
                fullPhotoViewUri = photoUri
            }
        )
    }

    // Record Debit Note / Return Dialog
    if (showRecordReturnDialog) {
        RecordPurchaseReturnDialog(
            initialSupplier = returnPresetSupplier,
            initialPurchase = returnPresetPurchase,
            allSuppliers = suppliers,
            viewModel = viewModel,
            onDismiss = {
                showRecordReturnDialog = false
                returnPresetPurchase = null
                returnPresetSupplier = null
            },
            onSaved = {
                showRecordReturnDialog = false
                returnPresetPurchase = null
                returnPresetSupplier = null
                selectedTab = 2 // Switch to Debit Notes tab
            }
        )
    }

    // Full Screen Photo Preview Dialog
    fullPhotoViewUri?.let { photoUri ->
        PhotoPreviewDialog(
            photoUri = photoUri,
            onDismiss = { fullPhotoViewUri = null }
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
    onSaved: () -> Unit,
    onViewPhoto: (String) -> Unit = {}
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
    val discountType by viewModel.purchaseDiscountTypeDraft.collectAsStateWithLifecycle()
    val discountPercent by viewModel.purchaseDiscountPercentDraft.collectAsStateWithLifecycle()
    val otherCharges by viewModel.purchaseOtherChargesDraft.collectAsStateWithLifecycle()
    val otherChargesDesc by viewModel.purchaseOtherChargesDescDraft.collectAsStateWithLifecycle()
    val isRoundOffAuto by viewModel.purchaseIsRoundOffAutoDraft.collectAsStateWithLifecycle()
    val roundOffAmount by viewModel.purchaseRoundOffDraft.collectAsStateWithLifecycle()
    val paidAmount by viewModel.purchasePaidAmountDraft.collectAsStateWithLifecycle()
    val notes by viewModel.purchaseNotesDraft.collectAsStateWithLifecycle()
    val items by viewModel.purchaseItemsDraft.collectAsStateWithLifecycle()

    val billPhotoUri by viewModel.purchaseBillPhotoUriDraft.collectAsStateWithLifecycle()
    val transportName by viewModel.purchaseTransportNameDraft.collectAsStateWithLifecycle()
    val vehicleNo by viewModel.purchaseVehicleNoDraft.collectAsStateWithLifecycle()
    val lrBiltyNo by viewModel.purchaseLrBiltyNoDraft.collectAsStateWithLifecycle()
    val rawMaterials by viewModel.allRawMaterials.collectAsStateWithLifecycle()

    val particular by viewModel.purchaseItemParticularInput.collectAsStateWithLifecycle()
    val hsn by viewModel.purchaseItemHsnInput.collectAsStateWithLifecycle()
    val heightStr by viewModel.purchaseItemHeightInput.collectAsStateWithLifecycle()
    val widthStr by viewModel.purchaseItemWidthInput.collectAsStateWithLifecycle()
    val qtyStr by viewModel.purchaseItemQtyInput.collectAsStateWithLifecycle()
    val rateStr by viewModel.purchaseItemRateInput.collectAsStateWithLifecycle()
    val itemUnit by viewModel.purchaseItemUnitInput.collectAsStateWithLifecycle()

    val height = heightStr.toDoubleOrNull() ?: 0.0
    val width = widthStr.toDoubleOrNull() ?: 0.0
    val qty = qtyStr.toDoubleOrNull() ?: 1.0
    val rate = rateStr.toDoubleOrNull() ?: 0.0
    val liveSqFt = if (height > 0 && width > 0) DimensionCalculator.calculateSqFt(height, width, qty, dimensionUnit) else qty
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
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var showCustomUnitDialog by remember { mutableStateOf(false) }
    var customUnitText by remember { mutableStateOf("") }
    var showUnitMenu by remember { mutableStateOf(false) }
    var showRawMaterialCatalogDialog by remember { mutableStateOf(false) }
    var showTransportDetails by remember { mutableStateOf(transportName.isNotBlank() || vehicleNo.isNotBlank() || lrBiltyNo.isNotBlank()) }

    // Photo picker launcher (0-permissions Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val billsDir = File(context.filesDir, "purchase_bills").apply { mkdirs() }
                val billFile = File(billsDir, "pb_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    billFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.setPurchaseBillPhoto(billFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

                    OutlinedTextField(
                        value = invoiceNo,
                        onValueChange = { viewModel.purchaseInvoiceNoDraft.value = it },
                        label = { Text("Purchase Bill No") },
                        placeholder = { Text("e.g. PB-2026-001 or Supplier Bill No") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("purchase_invoice_no_input")
                    )

                    // 1. Attached Paper Bill Photo (Android Photo Picker - 0 permissions)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (billPhotoUri.isNotBlank()) Color(0xFF0F766E).copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (billPhotoUri.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(54.dp).clickable { onViewPhoto(billPhotoUri) }
                                ) {
                                    AsyncImage(
                                        model = File(billPhotoUri),
                                        contentDescription = "Bill Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Bill Photo Attached ✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                                    }
                                    Text("Tap to view full screen", fontSize = 10.5.sp, color = Color.Gray)
                                }
                                Row {
                                    IconButton(
                                        onClick = { onViewPhoto(billPhotoUri) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom", tint = Color(0xFF0F766E), modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.setPurchaseBillPhoto("") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Attach Original Bill / Invoice Photo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F766E))
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFCCFBF1)
                                ) {
                                    Text("+ Select", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }

                    // 2. Transport & Bilty Details (Optional)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTransportDetails = !showTransportDetails },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Transport & Bilty / LR Details (Optional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F766E))
                                }
                                Text(
                                    text = if (showTransportDetails) "▲ Hide" else "▼ Add",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F766E)
                                )
                            }

                            AnimatedVisibility(visible = showTransportDetails) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                    OutlinedTextField(
                                        value = transportName,
                                        onValueChange = { viewModel.purchaseTransportNameDraft.value = it },
                                        label = { Text("Transport / Courier Company") },
                                        placeholder = { Text("e.g. VRL Logistics, ARC Transport, Self") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = vehicleNo,
                                            onValueChange = { viewModel.purchaseVehicleNoDraft.value = it },
                                            label = { Text("Vehicle No") },
                                            placeholder = { Text("e.g. GJ-01-AB-1234") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = lrBiltyNo,
                                            onValueChange = { viewModel.purchaseLrBiltyNoDraft.value = it },
                                            label = { Text("LR / Bilty No") },
                                            placeholder = { Text("e.g. LR-987452") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Item Entry (Sl.no | Particular | Qty & Unit | Rate | Amount)
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
                            text = if (editingItemIndex != null) "Edit Item (Sl.no: ${editingItemIndex!! + 1})" else "Add Item (Sl.no: ${items.size + 1})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F766E)
                        )
                        Text(
                            text = "Format: Sl.no | Particular | Qty & Unit | Rate | Amount",
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
                        placeholder = { Text("e.g. Lamination Door, Flush Door 30mm, Fevicol, Hinges") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("purchase_particular_input")
                    )

                    // Quick Raw Material Catalog / Frequent Items Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Quick Material Fill:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F766E),
                            modifier = Modifier.clickable { showRawMaterialCatalogDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Category, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("📦 Catalog (${rawMaterials.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Quick Chips from top materials (autofills Particular, HSN, Unit, Rate)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val popularMaterials: List<RawMaterialCatalogEntity> = if (rawMaterials.isNotEmpty()) {
                            rawMaterials.take(8)
                        } else {
                            listOf(
                                RawMaterialCatalogEntity(name = "Flush Door 30mm", defaultUnit = "Pcs", defaultRate = 1850.0),
                                RawMaterialCatalogEntity(name = "Lamination Door", defaultUnit = "Pcs", defaultRate = 2800.0),
                                RawMaterialCatalogEntity(name = "Plywood 18mm", defaultUnit = "Pcs", defaultRate = 1950.0),
                                RawMaterialCatalogEntity(name = "Fevicol Marine", defaultUnit = "Kg", defaultRate = 220.0),
                                RawMaterialCatalogEntity(name = "SS Hinges", defaultUnit = "Pair", defaultRate = 85.0),
                                RawMaterialCatalogEntity(name = "Wood Polish", defaultUnit = "Ltr", defaultRate = 380.0)
                            )
                        }
                        popularMaterials.forEach { mat ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFCCFBF1),
                                border = BorderStroke(0.5.dp, Color(0xFF0F766E).copy(alpha = 0.4f)),
                                modifier = Modifier.clickable {
                                    viewModel.selectRawMaterialToItem(mat)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${mat.name} (${mat.defaultUnit})",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F766E)
                                    )
                                    if (mat.defaultRate > 0) {
                                        Text(
                                            text = " ₹${mat.defaultRate.toInt()}",
                                            fontSize = 9.5.sp,
                                            color = Color(0xFF0F766E).copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Unit Selection Bar (Pcs, Kg, Ltr, Meter, Box, Sq.Ft, Bundle, Set, Ton, + Other)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Select Measuring Unit:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                            Text(
                                text = "Current: $itemUnit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F766E)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Pcs", "Kg", "Ltr", "Meter", "Box", "Sq.Ft", "Bundle", "Set", "Ton").forEach { u ->
                                val isSelected = itemUnit.equals(u, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setPurchaseItemUnit(u) },
                                    label = {
                                        Text(
                                            text = u,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0F766E),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }

                            // Custom Unit chip
                            val isStandard = listOf("Pcs", "Kg", "Ltr", "Meter", "Box", "Sq.Ft", "Bundle", "Set", "Ton").any { it.equals(itemUnit, ignoreCase = true) }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (!isStandard) Color(0xFF0F766E) else Color(0xFFCCFBF1),
                                modifier = Modifier.clickable {
                                    customUnitText = if (!isStandard) itemUnit else ""
                                    showCustomUnitDialog = true
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (!isStandard) "Unit: $itemUnit ✎" else "+ Other Unit",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isStandard) Color.White else Color(0xFF0F766E)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Qty & Rate (Sl.no | Particular | Qty & Unit | Rate | Amount)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = qtyStr,
                            onValueChange = { viewModel.purchaseItemQtyInput.value = it },
                            label = { Text("Qty ($itemUnit) *") },
                            placeholder = { Text("e.g. 10 or 2.5") },
                            trailingIcon = {
                                Box {
                                    TextButton(
                                        onClick = { showUnitMenu = true },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$itemUnit ▼",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF0F766E)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showUnitMenu,
                                        onDismissRequest = { showUnitMenu = false }
                                    ) {
                                        viewModel.standardPurchaseUnits.forEach { u ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(u, fontWeight = if (itemUnit == u) FontWeight.Bold else FontWeight.Normal)
                                                        if (itemUnit == u) {
                                                            Text("✓", color = Color(0xFF0F766E), fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.setPurchaseItemUnit(u)
                                                    showUnitMenu = false
                                                }
                                            )
                                        }
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = {
                                                Text("+ Other Custom Unit...", color = Color(0xFF0F766E), fontWeight = FontWeight.Bold)
                                            },
                                            onClick = {
                                                showUnitMenu = false
                                                customUnitText = ""
                                                showCustomUnitDialog = true
                                            }
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1.15f).testTag("purchase_qty_input")
                        )

                        OutlinedTextField(
                            value = rateStr,
                            onValueChange = { viewModel.purchaseItemRateInput.value = it },
                            label = { Text("Rate (₹/$itemUnit) *") },
                            placeholder = { Text("Rate per $itemUnit") },
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
                                text = if (showDimensions) "▲ Hide Dimensions" else "▼ Add Height & Width (Optional for Doors/Sheets)",
                                fontSize = 11.5.sp,
                                color = Color(0xFF0F766E)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showDimensions) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Dimension Measurement Unit:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F766E)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("Inches", "MM", "Feet").forEach { u ->
                                        FilterChip(
                                            selected = dimensionUnit == u,
                                            onClick = { viewModel.purchaseDimensionUnitDraft.value = u },
                                            label = { Text(u, fontSize = 10.5.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF0F766E),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                    }

                    // Live Amount Strip: Sl.no | Particular | Qty & Unit | Rate | Amount
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
                                    text = "Qty: ${DimensionCalculator.formatQtyWithUnit(qty, itemUnit)} × ₹${if (rate > 0) String.format(java.util.Locale.US, "%.2f", rate) else "0"} / $itemUnit",
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (editingItemIndex != null) {
                            OutlinedButton(
                                onClick = {
                                    editingItemIndex = null
                                    viewModel.resetPurchaseItemInputs()
                                },
                                modifier = Modifier.weight(0.8f).height(46.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel")
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.addOrUpdateItemToPurchase(editingItemIndex)
                                editingItemIndex = null
                            },
                            modifier = Modifier.weight(if (editingItemIndex != null) 1.2f else 1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                        ) {
                            Icon(if (editingItemIndex != null) Icons.Default.Edit else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (editingItemIndex != null) "Update Item #${editingItemIndex!! + 1}" else "Add Item (Sl.no ${items.size + 1})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Added Items Table (Sl.no | Particular | Qty & Unit | Rate | Amount)
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
                            text = "Format: Sl.no | Particular | Qty & Unit | Rate | Amount",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color(0xFF0F766E)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No items added yet. Fill Particular, Qty & Unit (Pcs, Kg, Ltr, etc.) and Rate above.",
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
                        // Table Header Row: Sl.no | Particular | Qty & Unit | Rate | Amount
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F766E))
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sl.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.width(28.dp))
                            Text("Particular", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.weight(1.8f))
                            Text("Qty & Unit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                            Text("Rate (₹)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                            Text("Amount (₹)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.weight(1.2f))
                            Spacer(modifier = Modifier.width(52.dp))
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
                                    modifier = Modifier.width(28.dp)
                                )
                                Column(modifier = Modifier.weight(1.8f)) {
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
                                    text = item.formattedQtyWithUnit,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F766E),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%.2f", item.rate),
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%.2f", item.amount),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F766E),
                                    modifier = Modifier.weight(1.2f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.width(52.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            editingItemIndex = index
                                            viewModel.startEditPurchaseItem(index)
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = Color(0xFF0F766E),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (editingItemIndex == index) {
                                                editingItemIndex = null
                                                viewModel.resetPurchaseItemInputs()
                                            }
                                            viewModel.removeItemFromPurchase(index)
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
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
                            Text("Total", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF0F766E), modifier = Modifier.width(28.dp))
                            Text("${items.size} items", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF0F766E), modifier = Modifier.weight(1.8f))
                            Text("-", fontSize = 11.5.sp, color = Color(0xFF0F766E), modifier = Modifier.weight(1f))
                            Text("-", fontSize = 11.5.sp, color = Color(0xFF0F766E), modifier = Modifier.weight(1f))
                            Text(
                                "₹${String.format(java.util.Locale.US, "%.2f", subTotal)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                color = Color(0xFF0F766E),
                                modifier = Modifier.weight(1.2f)
                            )
                            Spacer(modifier = Modifier.width(52.dp))
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

                    // Freight / Other Charges
                    OutlinedTextField(
                        value = if (otherCharges == 0.0) "" else otherCharges.toString(),
                        onValueChange = { viewModel.purchaseOtherChargesDraft.value = it.toDoubleOrNull() ?: 0.0 },
                        label = { Text("Freight / Other Charges (₹)", color = Color.Black) },
                        placeholder = { Text("e.g. 500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Discount Section (Flat ₹ or Percent %)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Discount, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bill Discount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilterChip(
                                        selected = discountType == "FLAT",
                                        onClick = { viewModel.setPurchaseDiscountType("FLAT") },
                                        label = { Text("Flat ₹", fontSize = 10.5.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF0F766E),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                    FilterChip(
                                        selected = discountType == "PERCENT",
                                        onClick = { viewModel.setPurchaseDiscountType("PERCENT") },
                                        label = { Text("Percent %", fontSize = 10.5.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF0F766E),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            if (discountType == "PERCENT") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = if (discountPercent == 0.0) "" else discountPercent.toString(),
                                        onValueChange = {
                                            val p = it.toDoubleOrNull() ?: 0.0
                                            viewModel.setPurchaseDiscountPercent(p)
                                        },
                                        label = { Text("Discount (%)") },
                                        placeholder = { Text("e.g. 5") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "- ₹${String.format(java.util.Locale.US, "%.2f", discount)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF16A34A)
                                        )
                                        Text(
                                            text = "(${discountPercent}% off on ₹${String.format(java.util.Locale.US, "%.2f", subTotal)})",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                // Quick percentage chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(2.0, 3.0, 5.0, 10.0, 15.0, 20.0).forEach { p ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (discountPercent == p) Color(0xFF0F766E) else Color(0xFFCCFBF1),
                                            modifier = Modifier.clickable { viewModel.setPurchaseDiscountPercent(p) }
                                        ) {
                                            Text(
                                                text = "${p.toInt()}%",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (discountPercent == p) Color.White else Color(0xFF0F766E),
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = if (discount == 0.0) "" else discount.toString(),
                                    onValueChange = {
                                        val amt = it.toDoubleOrNull() ?: 0.0
                                        viewModel.setPurchaseDiscountFlat(amt)
                                    },
                                    label = { Text("Discount Amount (₹)") },
                                    placeholder = { Text("e.g. 250") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
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

    if (showCustomUnitDialog) {
        AlertDialog(
            onDismissRequest = { showCustomUnitDialog = false },
            title = {
                Text(
                    text = "Custom Measuring Unit",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F766E)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter a custom unit of measurement (e.g. Kg, Ltr, Drum, Roll, Bag, Pair, Gram, Tin, Packet):",
                        fontSize = 12.5.sp,
                        color = Color.DarkGray
                    )
                    OutlinedTextField(
                        value = customUnitText,
                        onValueChange = { customUnitText = it },
                        label = { Text("Unit Name *") },
                        placeholder = { Text("e.g. Drum, Roll, Bag") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = customUnitText.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.setPurchaseItemUnit(trimmed)
                        }
                        showCustomUnitDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Apply Unit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomUnitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRawMaterialCatalogDialog) {
        RawMaterialCatalogDialog(
            viewModel = viewModel,
            onDismiss = { showRawMaterialCatalogDialog = false }
        )
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
                        val itemsSummary = if (p.items.isNotEmpty()) {
                            "${p.items.size} items: " + p.items.take(3).joinToString(", ") { "${it.particular} (${it.formattedQtyWithUnit})" } + (if (p.items.size > 3) " +${p.items.size - 3} more" else "")
                        } else "0 items"
                        Text(itemsSummary, fontSize = 11.sp, color = Color.Gray, maxLines = 1)

                        // Indicators for Bill Photo & Transport
                        if (purchase.billPhotoUri.isNotBlank() || purchase.transportName.isNotBlank() || purchase.vehicleNo.isNotBlank()) {
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (purchase.billPhotoUri.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFE0F2FE)
                                    ) {
                                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Bill Photo Attached", fontSize = 10.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                if (purchase.transportName.isNotBlank() || purchase.vehicleNo.isNotBlank()) {
                                    Text("🚚 ${listOf(purchase.transportName, purchase.vehicleNo).filter { it.isNotBlank() }.joinToString(" • ")}", fontSize = 10.sp, color = Color(0xFF475569))
                                }
                            }
                        }

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
    company: com.example.data.db.CompanyProfileEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onCreateReturn: () -> Unit = {},
    onViewPhoto: (String) -> Unit = {}
) {
    val context = LocalContext.current
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                InvoicePrinter.printPurchaseBill(context, purchaseWithItems, company)
                            }
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Print", tint = Color.White)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
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

                        // Transport & Bilty Details (if present)
                        if (purchase.transportName.isNotBlank() || purchase.vehicleNo.isNotBlank() || purchase.lrBiltyNo.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Transport & Bilty Details", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                                    }
                                    if (purchase.transportName.isNotBlank()) Text("Transport: ${purchase.transportName}", fontSize = 11.sp, color = Color.DarkGray)
                                    if (purchase.vehicleNo.isNotBlank()) Text("Vehicle No: ${purchase.vehicleNo}", fontSize = 11.sp, color = Color.DarkGray)
                                    if (purchase.lrBiltyNo.isNotBlank()) Text("LR / Bilty No: ${purchase.lrBiltyNo}", fontSize = 11.sp, color = Color.DarkGray)
                                }
                            }
                        }

                        // Attached Paper Bill Photo (if present)
                        if (purchase.billPhotoUri.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFF0F766E).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clickable { onViewPhoto(purchase.billPhotoUri) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(6.dp), modifier = Modifier.size(50.dp)) {
                                        AsyncImage(
                                            model = File(purchase.billPhotoUri),
                                            contentDescription = "Bill Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Original Paper Bill Attached ✓", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F766E))
                                        Text("Tap photo to view full size 🔍", fontSize = 10.5.sp, color = Color.Gray)
                                    }
                                    Icon(Icons.Default.ZoomIn, contentDescription = "View Photo", tint = Color(0xFF0F766E))
                                }
                            }
                        }

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
                                // Header: Sl.no | Particular | Qty & Unit | Rate | Amount
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F766E))
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Sl.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                                    Text("Particular", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.8f))
                                    Text("Qty & Unit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Text("Rate (₹)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Text("Amount (₹)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
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
                                            modifier = Modifier.width(28.dp)
                                        )
                                        Column(modifier = Modifier.weight(1.8f)) {
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
                                            text = item.formattedQtyWithUnit,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F766E),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = String.format(java.util.Locale.US, "%.2f", item.rate),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = String.format(java.util.Locale.US, "%.2f", item.amount),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F766E),
                                            modifier = Modifier.weight(1.2f)
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
                                    Text("Total", fontWeight = FontWeight.ExtraBold, fontSize = 11.5.sp, color = Color(0xFF0F766E), modifier = Modifier.width(28.dp))
                                    Text("${items.size} items", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F766E), modifier = Modifier.weight(1.8f))
                                    Text("-", fontSize = 11.sp, color = Color(0xFF0F766E), modifier = Modifier.weight(1f))
                                    Text("-", fontSize = 11.sp, color = Color(0xFF0F766E), modifier = Modifier.weight(1f))
                                    Text(
                                        "₹${String.format(java.util.Locale.US, "%.2f", purchase.subTotal)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF0F766E),
                                        modifier = Modifier.weight(1.2f)
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
                        if (Math.max(0.0, purchase.grandTotal - purchase.paidAmount) > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Balance Due", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                Text(DimensionCalculator.formatCurrency(Math.max(0.0, purchase.grandTotal - purchase.paidAmount)), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onCreateReturn,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Return Items (Debit Note)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onDismiss) { Text("Close") }
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedButton(
                            onClick = {
                                InvoicePrinter.printPurchaseBill(context, purchaseWithItems, company)
                            }
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Print / PDF", fontSize = 11.5.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onEdit,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Edit", fontSize = 11.5.sp)
                        }
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

// ---------------------------------------------------------------------------
// 1. FULL-SCREEN PHOTO PREVIEW DIALOG
// ---------------------------------------------------------------------------
@Composable
fun PhotoPreviewDialog(
    photoUri: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F766E))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Original Bill Photo / Invoice Attachment", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = File(photoUri),
                        contentDescription = "Bill Photo Full View",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Text("Close Photo")
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. RAW MATERIAL CATALOG DIALOG (QUICK AUTOFILL)
// ---------------------------------------------------------------------------
@Composable
fun RawMaterialCatalogDialog(
    viewModel: DoorBillingViewModel,
    onDismiss: () -> Unit
) {
    val materials by viewModel.allRawMaterials.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf("All", "Doors", "Plywood", "Hardware", "Adhesive", "Finishing", "Sheets", "Timber", "General")

    val filteredMaterials = remember(materials, searchQuery, selectedCategory) {
        materials.filter { mat ->
            val matchQuery = searchQuery.isBlank() ||
                    mat.name.contains(searchQuery, ignoreCase = true) ||
                    mat.category.contains(searchQuery, ignoreCase = true) ||
                    mat.hsnSac.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategory == "All" || mat.category.equals(selectedCategory, ignoreCase = true)
            matchQuery && matchCat
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F766E))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Raw Material Catalog", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Text("Tap material to autofill Particular, Unit & Rate", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add New", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search material (e.g. Flush Door, Plywood, Fevicol)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF0F766E)) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0F766E),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Materials list
                    if (filteredMaterials.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No materials match '$searchQuery'", color = Color.Gray, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showAddDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("+ Add '$searchQuery' to Catalog")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredMaterials) { mat ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectRawMaterialToItem(mat)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(mat.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFCCFBF1)
                                                ) {
                                                    Text(mat.defaultUnit, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E), modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.padding(top = 2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("HSN: ${mat.hsnSac}", fontSize = 11.sp, color = Color.Gray)
                                                Text("• Category: ${mat.category}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (mat.defaultRate > 0) {
                                                Text(
                                                    "₹${String.format(java.util.Locale.US, "%.2f", mat.defaultRate)}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.5.sp,
                                                    color = Color(0xFF0F766E)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteRawMaterial(mat) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(16.dp))
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

    if (showAddDialog) {
        var newName by remember { mutableStateOf(searchQuery) }
        var newUnit by remember { mutableStateOf("Pcs") }
        var newRate by remember { mutableStateOf("") }
        var newHsn by remember { mutableStateOf("4418") }
        var newCategory by remember { mutableStateOf("Doors") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Material to Catalog", fontWeight = FontWeight.Bold, color = Color(0xFF0F766E)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Material Name *") },
                        placeholder = { Text("e.g. Flush Door 30mm, Fevicol Marine") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newUnit,
                            onValueChange = { newUnit = it },
                            label = { Text("Unit (Pcs/Kg/Ltr)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newRate,
                            onValueChange = { newRate = it },
                            label = { Text("Default Rate (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newHsn,
                            onValueChange = { newHsn = it },
                            label = { Text("HSN / SAC") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = { newCategory = it },
                            label = { Text("Category") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newName.trim()
                        if (trimmed.isNotBlank()) {
                            val entity = RawMaterialCatalogEntity(
                                name = trimmed,
                                defaultUnit = newUnit.trim().ifBlank { "Pcs" },
                                defaultRate = newRate.toDoubleOrNull() ?: 0.0,
                                hsnSac = newHsn.trim().ifBlank { "4418" },
                                category = newCategory.trim().ifBlank { "General" }
                            )
                            viewModel.saveRawMaterial(entity)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                ) {
                    Text("Save Material")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// 3. PURCHASE RETURNS / DEBIT NOTES TAB
// ---------------------------------------------------------------------------
@Composable
fun PurchaseReturnsListTab(
    viewModel: DoorBillingViewModel,
    company: com.example.data.db.CompanyProfileEntity,
    onNewReturnClick: () -> Unit
) {
    val context = LocalContext.current
    val returns by viewModel.allPurchaseReturns.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var viewingReturnDetails by remember { mutableStateOf<PurchaseReturnWithItems?>(null) }
    var returnToDelete by remember { mutableStateOf<PurchaseReturnWithItems?>(null) }

    val filtered = remember(returns, searchQuery) {
        if (searchQuery.isBlank()) returns
        else returns.filter {
            it.returnNote.returnNo.contains(searchQuery, ignoreCase = true) ||
            it.returnNote.supplierName.contains(searchQuery, ignoreCase = true) ||
            it.returnNote.purchaseInvoiceNo.contains(searchQuery, ignoreCase = true) ||
            it.returnNote.reason.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalDebitAmount = remember(returns) { returns.sumOf { it.returnNote.totalAmount } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Banner & Button
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF0F766E))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PURCHASE RETURNS & DEBIT NOTES", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            DimensionCalculator.formatCurrency(totalDebitAmount),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text("${returns.size} Debit Note(s) Recorded", color = Color(0xFFCCFBF1), fontSize = 11.5.sp)
                    }
                    Button(
                        onClick = onNewReturnClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Debit Note", color = Color(0xFF0F766E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by DN No, supplier, or reason...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF0F766E)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AssignmentReturn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Debit Notes / Returns Recorded", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Debit notes automatically reduce the supplier's balance due.", fontSize = 11.5.sp, color = Color.Gray)
                    }
                }
            }
        } else {
            items(filtered) { retWithItems ->
                val note = retWithItems.returnNote
                val returnItemEntries = retWithItems.items
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().clickable { viewingReturnDetails = retWithItems },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFEF3C7)) {
                                    Text(note.returnNo, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFFD97706), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                if (note.purchaseInvoiceNo.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ref Bill: ${note.purchaseInvoiceNo}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Text(DimensionCalculator.formatDate(note.dateMillis), fontSize = 11.5.sp, color = Color.Gray)
                        }

                        Text("Supplier: ${note.supplierName}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text("Reason: ${note.reason}", fontSize = 11.sp, color = Color(0xFF475569), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }

                        val itemsSummary = if (returnItemEntries.isNotEmpty()) {
                            "${returnItemEntries.size} item(s) returned: " + returnItemEntries.take(2).joinToString(", ") { "${it.particular} (${it.formattedQtyWithUnit})" } + (if (returnItemEntries.size > 2) " +${returnItemEntries.size - 2} more" else "")
                        } else "0 items"
                        Text(itemsSummary, fontSize = 11.sp, color = Color.DarkGray)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("DEBIT AMOUNT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(
                                    DimensionCalculator.formatCurrency(note.totalAmount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F766E)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = {
                                        InvoicePrinter.printDebitNote(context, retWithItems, company)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Print", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { returnToDelete = retWithItems },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    returnToDelete?.let { ret ->
        AlertDialog(
            onDismissRequest = { returnToDelete = null },
            title = { Text("Delete Debit Note?") },
            text = { Text("Are you sure you want to delete Debit Note '${ret.returnNote.returnNo}' of ${DimensionCalculator.formatCurrency(ret.returnNote.totalAmount)}? Supplier ledger balance will adjust accordingly.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePurchaseReturn(ret.returnNote.id)
                        returnToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { returnToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Viewing Debit Note Details Dialog
    viewingReturnDetails?.let { ret ->
        DebitNoteDetailsDialog(
            returnWithItems = ret,
            company = company,
            onDismiss = { viewingReturnDetails = null }
        )
    }
}

// ---------------------------------------------------------------------------
// 4. DEBIT NOTE DETAILS DIALOG
// ---------------------------------------------------------------------------
@Composable
fun DebitNoteDetailsDialog(
    returnWithItems: PurchaseReturnWithItems,
    company: com.example.data.db.CompanyProfileEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val note = returnWithItems.returnNote
    val items = returnWithItems.items

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(12.dp),
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
                        Text("Debit Note Voucher", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Text(note.returnNo, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                InvoicePrinter.printDebitNote(context, returnWithItems, company)
                            }
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Print", tint = Color.White)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("Supplier: ${note.supplierName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Date: ${DimensionCalculator.formatDate(note.dateMillis)}", fontSize = 12.sp)
                        if (note.purchaseInvoiceNo.isNotBlank()) {
                            Text("Against Bill No: ${note.purchaseInvoiceNo}", fontSize = 12.sp, color = Color(0xFF0F766E), fontWeight = FontWeight.SemiBold)
                        }
                        Text("Reason: ${note.reason}", fontSize = 12.sp, color = Color.DarkGray)
                        if (note.notes.isNotBlank()) {
                            Text("Remarks: ${note.notes}", fontSize = 11.5.sp, color = Color.Gray)
                        }
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F766E))
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Sl.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                                    Text("Particular / Item", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.8f))
                                    Text("Qty & Unit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Text("Rate (₹)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Text("Amount (₹)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
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
                                        Text("${item.slNo}", fontSize = 11.5.sp, modifier = Modifier.width(28.dp))
                                        Text(item.particular, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f))
                                        Text(item.formattedQtyWithUnit, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F766E), modifier = Modifier.weight(1f))
                                        Text(String.format(java.util.Locale.US, "%.2f", item.rate), fontSize = 11.sp, modifier = Modifier.weight(1f))
                                        Text(String.format(java.util.Locale.US, "%.2f", item.amount), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E), modifier = Modifier.weight(1.2f))
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL DEBIT AMOUNT", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F766E))
                            Text(DimensionCalculator.formatCurrency(note.totalAmount), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F766E))
                        }
                        Text("Note: This amount has been debited and subtracted from ${note.supplierName}'s ledger balance.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Close") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            InvoicePrinter.printDebitNote(context, returnWithItems, company)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print / PDF Debit Note")
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 5. RECORD PURCHASE RETURN / DEBIT NOTE DIALOG
// ---------------------------------------------------------------------------
@Composable
fun RecordPurchaseReturnDialog(
    initialSupplier: SupplierEntity?,
    initialPurchase: PurchaseWithItems?,
    allSuppliers: List<SupplierEntity>,
    viewModel: DoorBillingViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var selectedSupplier by remember { mutableStateOf(initialSupplier ?: allSuppliers.firstOrNull()) }
    var showSupplierDropdown by remember { mutableStateOf(false) }
    var returnNo by remember { mutableStateOf(viewModel.getNextDebitNoteNo()) }
    var purchaseInvoiceNo by remember { mutableStateOf(initialPurchase?.purchase?.invoiceNo ?: "") }
    var reason by remember { mutableStateOf("Defective / Damaged Material") }
    var notes by remember { mutableStateOf("") }

    // Returned items state
    val returnItems = remember {
        val list = mutableStateListOf<PurchaseReturnItemEntity>()
        if (initialPurchase != null && initialPurchase.items.isNotEmpty()) {
            initialPurchase.items.forEachIndexed { idx, itm ->
                list.add(
                    PurchaseReturnItemEntity(
                        slNo = idx + 1,
                        particular = itm.particular,
                        qty = itm.qty,
                        unit = itm.unit.ifBlank { "Pcs" },
                        rate = itm.rate,
                        amount = itm.amount
                    )
                )
            }
        } else {
            list.add(
                PurchaseReturnItemEntity(
                    slNo = 1,
                    particular = "",
                    qty = 1.0,
                    unit = "Pcs",
                    rate = 0.0,
                    amount = 0.0
                )
            )
        }
        list
    }

    // New item inputs
    var itemParticular by remember { mutableStateOf("") }
    var itemQtyStr by remember { mutableStateOf("1") }
    var itemUnit by remember { mutableStateOf("Pcs") }
    var itemRateStr by remember { mutableStateOf("") }

    val reasonsList = listOf(
        "Defective / Damaged Material",
        "Quality Mismatch",
        "Excess Quantity Supplied",
        "Rate Difference / Billing Error",
        "Order Cancelled",
        "Transport Damage",
        "Other"
    )

    val grandTotal = remember(returnItems.toList()) { returnItems.sumOf { it.amount } }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AssignmentReturn, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Create Debit Note / Return", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Text("Reduces supplier balance due", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Supplier selection
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedSupplier?.name ?: "Select Supplier",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Supplier / Vendor *") },
                                trailingIcon = {
                                    IconButton(onClick = { showSupplierDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier.matchParentSize().clickable { showSupplierDropdown = true }
                            )

                            DropdownMenu(
                                expanded = showSupplierDropdown,
                                onDismissRequest = { showSupplierDropdown = false }
                            ) {
                                allSuppliers.forEach { sup ->
                                    DropdownMenuItem(
                                        text = { Text(sup.name, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            selectedSupplier = sup
                                            showSupplierDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Return No & Original Bill No
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = returnNo,
                                onValueChange = { returnNo = it },
                                label = { Text("Debit Note No *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = purchaseInvoiceNo,
                                onValueChange = { purchaseInvoiceNo = it },
                                label = { Text("Original Bill No") },
                                placeholder = { Text("e.g. PB-001") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Return Reason
                    item {
                        Text("Reason for Return:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            reasonsList.forEach { r ->
                                val isSelected = reason == r
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { reason = r },
                                    label = { Text(r, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0F766E),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Items Table
                    item {
                        Text("Returned Items List:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F766E))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Particular", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.6f))
                                    Text("Qty & Unit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.1f))
                                    Text("Rate (₹)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Text("Amount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Spacer(modifier = Modifier.width(28.dp))
                                }

                                if (returnItems.isEmpty()) {
                                    Text("No items added. Add items below.", fontSize = 11.5.sp, color = Color.Gray, modifier = Modifier.padding(12.dp))
                                } else {
                                    returnItems.forEachIndexed { idx, itm ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (idx % 2 == 0) Color.White else Color(0xFFF8FAFC))
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(itm.particular.ifBlank { "Item #${idx + 1}" }, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.6f))
                                            Text(itm.formattedQtyWithUnit, fontSize = 11.sp, color = Color(0xFF0F766E), modifier = Modifier.weight(1.1f))
                                            Text(String.format(java.util.Locale.US, "%.2f", itm.rate), fontSize = 11.sp, modifier = Modifier.weight(1f))
                                            Text(String.format(java.util.Locale.US, "%.2f", itm.amount), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E), modifier = Modifier.weight(1f))
                                            IconButton(
                                                onClick = { returnItems.removeAt(idx) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
                                            }
                                        }
                                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }

                    // Add Returned Item Row
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("+ Add Item to Return:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))

                                OutlinedTextField(
                                    value = itemParticular,
                                    onValueChange = { itemParticular = it },
                                    label = { Text("Particular / Item Name *") },
                                    placeholder = { Text("e.g. Flush Door 30mm (Damaged)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = itemQtyStr,
                                        onValueChange = { itemQtyStr = it },
                                        label = { Text("Qty") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(0.9f)
                                    )
                                    OutlinedTextField(
                                        value = itemUnit,
                                        onValueChange = { itemUnit = it },
                                        label = { Text("Unit") },
                                        placeholder = { Text("Pcs/Kg/Ltr") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.9f)
                                    )
                                    OutlinedTextField(
                                        value = itemRateStr,
                                        onValueChange = { itemRateStr = it },
                                        label = { Text("Rate (₹)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }

                                Button(
                                    onClick = {
                                        val p = itemParticular.trim()
                                        val q = itemQtyStr.toDoubleOrNull() ?: 1.0
                                        val u = itemUnit.trim().ifBlank { "Pcs" }
                                        val r = itemRateStr.toDoubleOrNull() ?: 0.0
                                        if (p.isNotBlank() && q > 0) {
                                            returnItems.add(
                                                PurchaseReturnItemEntity(
                                                    slNo = returnItems.size + 1,
                                                    particular = p,
                                                    qty = q,
                                                    unit = u,
                                                    rate = r,
                                                    amount = q * r
                                                )
                                            )
                                            itemParticular = ""
                                            itemQtyStr = "1"
                                            itemRateStr = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add to Return List")
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes / Transport Remarks") },
                            placeholder = { Text("e.g. Sent back via tempo / courier") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Total & Save
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F766E),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("TOTAL RETURN AMOUNT", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        DimensionCalculator.formatCurrency(grandTotal),
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                }
                                Text("Will reduce balance", color = Color(0xFFCCFBF1), fontSize = 11.5.sp)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val sup = selectedSupplier
                            if (sup == null) {
                                viewModel.showMessage("Please select a supplier")
                                return@Button
                            }
                            if (returnItems.isEmpty()) {
                                viewModel.showMessage("Please add at least one returned item")
                                return@Button
                            }
                            val returnEntity = PurchaseReturnEntity(
                                returnNo = returnNo.trim().ifBlank { viewModel.getNextDebitNoteNo() },
                                purchaseId = initialPurchase?.purchase?.id ?: 0L,
                                purchaseInvoiceNo = purchaseInvoiceNo.trim(),
                                supplierId = sup.id,
                                supplierName = sup.name,
                                dateMillis = System.currentTimeMillis(),
                                reason = reason,
                                totalAmount = grandTotal,
                                notes = notes.trim()
                            )
                            viewModel.savePurchaseReturn(returnEntity, returnItems.toList()) {
                                onSaved()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Adjust Balance", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
