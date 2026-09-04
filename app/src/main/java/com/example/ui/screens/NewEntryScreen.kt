package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.BillItemEntity
import com.example.data.db.CustomerEntity
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.DoorBillingViewModel
import com.example.util.DimensionCalculator
import com.example.util.InvoicePrinter
import com.example.util.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEntryScreen(
    viewModel: DoorBillingViewModel
) {
    val context = LocalContext.current
    val billId by viewModel.billIdDraft.collectAsStateWithLifecycle()
    val invoiceNo by viewModel.invoiceNoDraft.collectAsStateWithLifecycle()
    val billDateMillis by viewModel.billDateMillisDraft.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomerDraft.collectAsStateWithLifecycle()
    val dimensionUnit by viewModel.dimensionUnitDraft.collectAsStateWithLifecycle()
    val taxRate by viewModel.taxRateDraft.collectAsStateWithLifecycle()
    val isGstIncluded by viewModel.isGstIncludedDraft.collectAsStateWithLifecycle()
    val discount by viewModel.discountDraft.collectAsStateWithLifecycle()
    val otherCharges by viewModel.otherChargesDraft.collectAsStateWithLifecycle()
    val otherChargesDesc by viewModel.otherChargesDescDraft.collectAsStateWithLifecycle()
    val isRoundOffAuto by viewModel.isRoundOffAutoDraft.collectAsStateWithLifecycle()
    val manualRoundOff by viewModel.roundOffDraft.collectAsStateWithLifecycle()
    val paidAmount by viewModel.paidAmountDraft.collectAsStateWithLifecycle()
    val notes by viewModel.notesDraft.collectAsStateWithLifecycle()
    val items by viewModel.billItemsDraft.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val company by viewModel.companyProfile.collectAsStateWithLifecycle()

    // Item line inputs
    val particular by viewModel.itemParticularInput.collectAsStateWithLifecycle()
    val hsn by viewModel.itemHsnInput.collectAsStateWithLifecycle()
    val heightStr by viewModel.itemHeightInput.collectAsStateWithLifecycle()
    val widthStr by viewModel.itemWidthInput.collectAsStateWithLifecycle()
    val qtyStr by viewModel.itemQtyInput.collectAsStateWithLifecycle()
    val rateStr by viewModel.itemRateInput.collectAsStateWithLifecycle()

    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var showQuickCustomerDialog by remember { mutableStateOf(false) }
    var quickCustomerName by remember { mutableStateOf("") }
    var quickCustomerMobile by remember { mutableStateOf("") }
    var quickCustomerAddress by remember { mutableStateOf("") }
    var quickCustomerGst by remember { mutableStateOf("") }

    // Live calculations for current line item
    val liveHeight = heightStr.toDoubleOrNull() ?: 0.0
    val liveWidth = widthStr.toDoubleOrNull() ?: 0.0
    val liveQty = qtyStr.toIntOrNull() ?: 1
    val liveRate = rateStr.toDoubleOrNull() ?: 0.0

    val liveSqFt = remember(liveHeight, liveWidth, liveQty, dimensionUnit) {
        DimensionCalculator.calculateSqFt(liveHeight, liveWidth, liveQty, dimensionUnit)
    }
    val liveAmount = remember(liveSqFt, liveRate) {
        DimensionCalculator.calculateAmount(liveSqFt, liveRate)
    }

    // Bill totals
    val subTotal = items.sumOf { it.amount }
    val totalSqFt = items.sumOf { it.sqFt }
    val totalQty = items.sumOf { it.qty }
    val gstAmount = if (isGstIncluded && taxRate > 0) (subTotal * taxRate / 100.0) else 0.0
    val rawGrandTotal = Math.max(0.0, (subTotal + gstAmount + otherCharges) - discount)
    val roundOffAmount = if (isRoundOffAuto) {
        val rounded = Math.round(rawGrandTotal).toDouble()
        rounded - rawGrandTotal
    } else {
        manualRoundOff
    }
    val grandTotal = Math.max(0.0, rawGrandTotal + roundOffAmount)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (billId != 0L) "Edit Bill #$invoiceNo" else "New Entry / Bill Creation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("new_entry_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Customer Selection Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1. SELECT CUSTOMER",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            TextButton(
                                onClick = { showQuickCustomerDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ New Customer", fontSize = 12.sp)
                            }
                        }

                        // Customer Dropdown
                        ExposedDropdownMenuBox(
                            expanded = customerDropdownExpanded,
                            onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedCustomer?.name ?: "Select Customer *",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .fillMaxWidth()
                                    .testTag("customer_dropdown_selector")
                            )

                            ExposedDropdownMenu(
                                expanded = customerDropdownExpanded,
                                onDismissRequest = { customerDropdownExpanded = false }
                            ) {
                                if (allCustomers.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No customers yet. Click '+ New Customer'") },
                                        onClick = {
                                            customerDropdownExpanded = false
                                            showQuickCustomerDialog = true
                                        }
                                    )
                                } else {
                                    allCustomers.forEach { cust ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(cust.name, fontWeight = FontWeight.Bold)
                                                    if (cust.mobile.isNotBlank()) {
                                                        Text("📞 ${cust.mobile}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.selectedCustomerDraft.value = cust
                                                customerDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Selected Customer Details Summary
                        selectedCustomer?.let { cust ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Billed to: ${cust.name}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    if (cust.mobile.isNotBlank()) Text("Phone: ${cust.mobile}", fontSize = 12.sp)
                                    if (cust.address.isNotBlank()) Text("Address: ${cust.address}", fontSize = 12.sp)
                                    if (cust.gstNo.isNotBlank()) Text("GSTIN: ${cust.gstNo}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Invoice Meta & Dimension Unit Toggle
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "2. INVOICE SETTINGS & UNIT",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.clickable {
                                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = billDateMillis }
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val newCal = java.util.Calendar.getInstance().apply {
                                                set(year, month, dayOfMonth)
                                            }
                                            viewModel.setBillDate(newCal.timeInMillis)
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
                                        text = "${DimensionCalculator.formatDate(billDateMillis)} ✎",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = invoiceNo,
                                onValueChange = { viewModel.invoiceNoDraft.value = it },
                                label = { Text("Invoice No") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            // Unit Selector (Inches vs Feet)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Dimension Unit", style = MaterialTheme.typography.labelSmall)
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = dimensionUnit == "Inches",
                                        onClick = { viewModel.recalculateBillItemsForUnit("Inches") },
                                        label = { Text("Inches (in)", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                    FilterChip(
                                        selected = dimensionUnit == "Feet",
                                        onClick = { viewModel.recalculateBillItemsForUnit("Feet") },
                                        label = { Text("Feet (ft)", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (dimensionUnit == "Inches")
                                "ℹ️ Formula: (High × Width ÷ 144) × Qty = Sq.Ft"
                            else
                                "ℹ️ Formula: (High × Width) × Qty = Sq.Ft",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // 3. Item Entry Form (Particular, HSN, High, Width, Qty, Sq.ft, Rate, Amount)
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "3. ADD DOOR / BILL ITEM",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        // Particular & HSN/SAC
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = particular,
                                onValueChange = { viewModel.itemParticularInput.value = it },
                                label = { Text("Particular / Item Name *") },
                                placeholder = { Text("e.g. Flush Door 30mm") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier
                                    .weight(2.2f)
                                    .testTag("item_particular_input")
                            )

                            OutlinedTextField(
                                value = hsn,
                                onValueChange = { viewModel.itemHsnInput.value = it },
                                label = { Text("HSN/SAC") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("item_hsn_input")
                            )
                        }

                        // Quick Particular suggestions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Lamination Door", "Lamination Door 30mm", "Flush Door 30mm", "Flush Door 35mm", "Teak Wood Door", "Panel Door", "Laminated Door", "Moulded Door", "Pine Wood Door").forEach { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { viewModel.itemParticularInput.value = suggestion }
                                ) {
                                    Text(
                                        text = suggestion,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Dimensions: High, Width, Qty
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = heightStr,
                                onValueChange = { viewModel.itemHeightInput.value = it },
                                label = { Text("High ($dimensionUnit)") },
                                placeholder = { Text(if (dimensionUnit == "Inches") "78" else "6.5") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("item_height_input")
                            )

                            OutlinedTextField(
                                value = widthStr,
                                onValueChange = { viewModel.itemWidthInput.value = it },
                                label = { Text("Width ($dimensionUnit)") },
                                placeholder = { Text(if (dimensionUnit == "Inches") "32" else "2.67") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("item_width_input")
                            )

                            OutlinedTextField(
                                value = qtyStr,
                                onValueChange = { viewModel.itemQtyInput.value = it },
                                label = { Text("Qty") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("item_qty_input")
                            )
                        }

                        // Rate & Live Sq.Ft Calculation Banner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = rateStr,
                                onValueChange = { viewModel.itemRateInput.value = it },
                                label = { Text("Rate / Sq.Ft (₹) *") },
                                placeholder = { Text("120") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("item_rate_input")
                            )

                            // Live Sq.ft and Total Amount Card
                            Surface(
                                modifier = Modifier.weight(1.8f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE0F2FE),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Sq.Ft: ${String.format(java.util.Locale.US, "%.2f", liveSqFt)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0369A1)
                                    )
                                    Text(
                                        text = "Amount: ₹${String.format(java.util.Locale.US, "%.2f", liveAmount)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0C4A6E)
                                    )
                                }
                            }
                        }

                        // Add Button
                        Button(
                            onClick = { viewModel.addOrUpdateItemToBill() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("add_item_to_bill_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Item to Bill Table", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. Items Table (Sl - Particular - HSN - High - Width - Qty - Sq.ft - Rate - Amount)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "4. BILL ITEMS TABLE (${items.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            if (items.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No items added yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Fill High, Width, Qty & Rate above, then tap 'Add Item to Bill Table'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(items) { index, item ->
                    BillItemCard(
                        item = item,
                        unit = dimensionUnit,
                        onDelete = { viewModel.removeItemFromBill(index) }
                    )
                }
            }

            // 5. Taxes, Discount & Grand Total Summary
            if (items.isNotEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "5. TAX & BILL TOTAL SUMMARY",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )

                            // Subtotal Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal ($totalQty Doors | ${String.format(java.util.Locale.US, "%.2f", totalSqFt)} Sq.Ft):")
                                Text(
                                    DimensionCalculator.formatCurrency(subTotal),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // GST Toggle & Rate Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Apply GST (${taxRate.toInt()}%)", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (isGstIncluded) "CGST ${(taxRate / 2).toInt()}% + SGST ${(taxRate / 2).toInt()}%" else "No GST applied",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isGstIncluded,
                                    onCheckedChange = { viewModel.isGstIncludedDraft.value = it }
                                )
                            }

                            if (isGstIncluded) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(0.0, 5.0, 12.0, 18.0, 28.0).forEach { rate ->
                                        FilterChip(
                                            selected = taxRate == rate,
                                            onClick = { viewModel.taxRateDraft.value = rate },
                                            label = { Text("${rate.toInt()}%", fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("CGST (${(taxRate / 2)}%):", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(DimensionCalculator.formatCurrency(gstAmount / 2.0))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("SGST (${(taxRate / 2)}%):", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(DimensionCalculator.formatCurrency(gstAmount / 2.0))
                                }
                            }

                            // Other Charges (e.g. Cutting Charges)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Other Charges (Cutting / Transport)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (otherCharges > 0) {
                                        Text(
                                            text = "+ " + DimensionCalculator.formatCurrency(otherCharges),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = Color(0xFF0284C7)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = otherChargesDesc,
                                        onValueChange = { viewModel.otherChargesDescDraft.value = it },
                                        label = { Text("Charge Name") },
                                        placeholder = { Text("Cutting Charges") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.3f)
                                    )
                                    OutlinedTextField(
                                        value = if (otherCharges == 0.0) "" else otherCharges.toString(),
                                        onValueChange = { viewModel.otherChargesDraft.value = it.toDoubleOrNull() ?: 0.0 },
                                        label = { Text("Amount (₹)") },
                                        placeholder = { Text("0.00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Cutting Charges", "Transportation", "Loading Charges", "Polish Charges").forEach { chipName ->
                                        FilterChip(
                                            selected = otherChargesDesc.equals(chipName, ignoreCase = true),
                                            onClick = { viewModel.otherChargesDescDraft.value = chipName },
                                            label = { Text(chipName, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            // Discount Input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = if (discount == 0.0) "" else discount.toString(),
                                    onValueChange = { viewModel.discountDraft.value = it.toDoubleOrNull() ?: 0.0 },
                                    label = { Text("Discount (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = if (paidAmount == 0.0) "" else paidAmount.toString(),
                                    onValueChange = { viewModel.paidAmountDraft.value = it.toDoubleOrNull() ?: 0.0 },
                                    label = { Text("Advance / Paid (₹)") },
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
                                        onCheckedChange = { viewModel.isRoundOffAutoDraft.value = it },
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text("Auto Round Off Total", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    text = (if (roundOffAmount >= 0) "+ " else "- ") + DimensionCalculator.formatCurrency(Math.abs(roundOffAmount)),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (roundOffAmount != 0.0) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Grand Total Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0369A1),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("GRAND TOTAL", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = DimensionCalculator.formatCurrency(grandTotal),
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 22.sp
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Balance Due", color = Color(0xFFFEF3C7), fontSize = 11.sp)
                                        Text(
                                            text = DimensionCalculator.formatCurrency(Math.max(0.0, grandTotal - paidAmount)),
                                            color = Color(0xFFFDE68A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "In Words: ${DimensionCalculator.convertToIndianCurrencyWords(grandTotal)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            // Notes
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { viewModel.notesDraft.value = it },
                                label = { Text("Terms / Notes for Customer (Optional)") },
                                placeholder = { Text("e.g. Delivery in 4 days. Polish extra.") },
                                singleLine = false,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 6. Action Buttons: Save, Print, WhatsApp
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.saveCurrentBill()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_bill_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Bill Entry", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.saveCurrentBill { savedBillWithItems ->
                                        InvoicePrinter.printInvoice(context, savedBillWithItems, company)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF0284C7))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save & Print", color = Color(0xFF0284C7))
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.saveCurrentBill { savedBillWithItems ->
                                        ShareHelper.shareInvoiceWhatsApp(context, savedBillWithItems, company)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF25D366))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp", color = Color(0xFF16A34A))
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick New Customer Dialog
    if (showQuickCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showQuickCustomerDialog = false },
            title = { Text("Quick Add Customer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quickCustomerName,
                        onValueChange = { quickCustomerName = it },
                        label = { Text("Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = quickCustomerMobile,
                        onValueChange = { quickCustomerMobile = it },
                        label = { Text("Mobile") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = quickCustomerAddress,
                        onValueChange = { quickCustomerAddress = it },
                        label = { Text("Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = quickCustomerGst,
                        onValueChange = { quickCustomerGst = it.uppercase() },
                        label = { Text("GSTIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (quickCustomerName.isNotBlank()) {
                            viewModel.customerNameInput.value = quickCustomerName
                            viewModel.customerMobileInput.value = quickCustomerMobile
                            viewModel.customerAddressInput.value = quickCustomerAddress
                            viewModel.customerGstInput.value = quickCustomerGst
                            viewModel.saveCustomer { savedCust ->
                                viewModel.selectedCustomerDraft.value = savedCust
                                showQuickCustomerDialog = false
                                quickCustomerName = ""
                                quickCustomerMobile = ""
                                quickCustomerAddress = ""
                                quickCustomerGst = ""
                            }
                        }
                    }
                ) {
                    Text("Add & Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickCustomerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BillItemCard(
    item: BillItemEntity,
    unit: String,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${item.slNo}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.particular,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Grid of fields: HSN | High x Width | Qty | Sq.Ft | Rate | Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("HSN: ${item.hsnSac}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Size: ${DimensionCalculator.formatDimension(item.height)} x ${DimensionCalculator.formatDimension(item.width)} $unit",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Qty: ${item.qty}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Area: ${String.format(java.util.Locale.US, "%.2f", item.sqFt)} Sq.Ft",
                        fontSize = 11.5.sp,
                        color = Color(0xFF0369A1),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Rate: ₹${DimensionCalculator.formatDimension(item.rate)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = DimensionCalculator.formatCurrency(item.amount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0C4A6E)
                    )
                }
            }
        }
    }
}
