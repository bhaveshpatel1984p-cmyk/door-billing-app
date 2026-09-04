package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.BillEntity
import com.example.data.db.BillItemEntity
import com.example.data.db.BillWithItems
import com.example.data.db.CompanyProfileEntity
import com.example.data.db.CustomerBalanceSummary
import com.example.data.db.CustomerEntity
import com.example.data.db.DoorDatabase
import com.example.data.db.LedgerEntry
import com.example.data.db.PaymentEntity
import com.example.data.db.PurchaseEntity
import com.example.data.db.PurchaseItemEntity
import com.example.data.db.PurchasePaymentEntity
import com.example.data.db.PurchaseWithItems
import com.example.data.db.SupplierBalanceSummary
import com.example.data.db.SupplierEntity
import com.example.data.db.SupplierLedgerEntry
import com.example.data.repository.DoorBillingRepository
import com.example.util.DimensionCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    DASHBOARD,
    CREATE_CUSTOMER,
    NEW_ENTRY,
    EDIT_ENTRY,
    CUSTOMER_BALANCE,
    CUSTOMER_LEDGER,
    COMPANY_PROFILE,
    FINANCIAL_STATS,
    PURCHASE_HUB
}

class DoorBillingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DoorBillingRepository

    init {
        val db = DoorDatabase.getDatabase(application)
        repository = DoorBillingRepository(
            customerDao = db.customerDao(),
            billDao = db.billDao(),
            paymentDao = db.paymentDao(),
            companyProfileDao = db.companyProfileDao(),
            supplierDao = db.supplierDao(),
            purchaseDao = db.purchaseDao(),
            purchasePaymentDao = db.purchasePaymentDao()
        )

        // Pre-populate standard supplier vendors if database is empty so dropdown is never blank
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (repository.getSuppliersCount() == 0) {
                    repository.saveSupplier(
                        SupplierEntity(
                            name = "Shree Balaji Timber & Plywood",
                            mobile = "9825012345",
                            address = "GIDC Industrial Estate, Plot 14",
                            gstNo = "24AAACB1234F1Z1"
                        )
                    )
                    repository.saveSupplier(
                        SupplierEntity(
                            name = "National Laminates & Doors",
                            mobile = "9879054321",
                            address = "Timber Market Yard",
                            gstNo = "24AACCN5678H1Z5"
                        )
                    )
                    repository.saveSupplier(
                        SupplierEntity(
                            name = "Gujarat Veneer & Flush Doors",
                            mobile = "9426098765",
                            address = "Ring Road Timber Hub",
                            gstNo = "24AADCG9012K1Z9"
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore initialization seeding error
            }
        }
    }

    // Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // Message events
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()
    val uiMessages: SharedFlow<String> get() = userMessage

    fun showMessage(msg: String) {
        viewModelScope.launch { _userMessage.emit(msg) }
    }

    // -------------------------------------------------------------
    // 1) CUSTOMERS
    // -------------------------------------------------------------
    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected customer for editing or creating
    val customerNameInput = MutableStateFlow("")
    val customerMobileInput = MutableStateFlow("")
    val customerAddressInput = MutableStateFlow("")
    val customerGstInput = MutableStateFlow("")
    val editingCustomerId = MutableStateFlow<Long?>(null)

    fun prepareNewCustomer() {
        customerNameInput.value = ""
        customerMobileInput.value = ""
        customerAddressInput.value = ""
        customerGstInput.value = ""
        editingCustomerId.value = null
    }

    fun prepareEditCustomer(customer: CustomerEntity) {
        customerNameInput.value = customer.name
        customerMobileInput.value = customer.mobile
        customerAddressInput.value = customer.address
        customerGstInput.value = customer.gstNo
        editingCustomerId.value = customer.id
    }

    fun saveCustomer(onSuccess: (CustomerEntity) -> Unit = {}) {
        val name = customerNameInput.value.trim()
        if (name.isBlank()) {
            showMessage("Please enter customer name")
            return
        }

        viewModelScope.launch {
            val customer = CustomerEntity(
                id = editingCustomerId.value ?: 0L,
                name = name,
                mobile = customerMobileInput.value.trim(),
                address = customerAddressInput.value.trim(),
                gstNo = customerGstInput.value.trim().uppercase()
            )
            val newId = repository.saveCustomer(customer)
            val savedCustomer = customer.copy(id = if (customer.id == 0L) newId else customer.id)
            showMessage(if (editingCustomerId.value != null) "Customer updated successfully" else "Customer added successfully")
            prepareNewCustomer()
            onSuccess(savedCustomer)
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            showMessage("Customer ${customer.name} deleted")
        }
    }

    // -------------------------------------------------------------
    // 2) BILL / ENTRY CREATION & EDITING
    // -------------------------------------------------------------
    val allBills: StateFlow<List<BillWithItems>> = repository.allBillsWithItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Bill Draft State
    val billIdDraft = MutableStateFlow(0L)
    val invoiceNoDraft = MutableStateFlow("")
    val billDateMillisDraft = MutableStateFlow(System.currentTimeMillis())
    val selectedCustomerDraft = MutableStateFlow<CustomerEntity?>(null)
    val dimensionUnitDraft = MutableStateFlow("Inches") // "Inches" or "Feet"
    val taxRateDraft = MutableStateFlow(18.0) // 18% GST default
    val isGstIncludedDraft = MutableStateFlow(true)
    val discountDraft = MutableStateFlow(0.0)
    val otherChargesDraft = MutableStateFlow(0.0)
    val otherChargesDescDraft = MutableStateFlow("Cutting Charges")
    val isRoundOffAutoDraft = MutableStateFlow(true)
    val roundOffDraft = MutableStateFlow(0.0)
    val paidAmountDraft = MutableStateFlow(0.0)
    val notesDraft = MutableStateFlow("")
    val billItemsDraft = MutableStateFlow<List<BillItemEntity>>(emptyList())

    fun setBillDate(dateMillis: Long) {
        billDateMillisDraft.value = dateMillis
    }

    // Temporary Item Line Inputs
    val itemParticularInput = MutableStateFlow("")
    val itemHsnInput = MutableStateFlow("4418")
    val itemHeightInput = MutableStateFlow("")
    val itemWidthInput = MutableStateFlow("")
    val itemQtyInput = MutableStateFlow("1")
    val itemRateInput = MutableStateFlow("")

    fun startNewBill(presetCustomer: CustomerEntity? = null) {
        viewModelScope.launch {
            billIdDraft.value = 0L
            invoiceNoDraft.value = repository.generateNextInvoiceNumber()
            billDateMillisDraft.value = System.currentTimeMillis()
            selectedCustomerDraft.value = presetCustomer
            dimensionUnitDraft.value = "Inches"
            taxRateDraft.value = 18.0
            isGstIncludedDraft.value = true
            discountDraft.value = 0.0
            otherChargesDraft.value = 0.0
            otherChargesDescDraft.value = "Cutting Charges"
            isRoundOffAutoDraft.value = true
            roundOffDraft.value = 0.0
            paidAmountDraft.value = 0.0
            notesDraft.value = ""
            billItemsDraft.value = emptyList()
            resetItemInputs()
            navigateTo(AppScreen.NEW_ENTRY)
        }
    }

    fun startEditBill(billWithItems: BillWithItems) {
        val bill = billWithItems.bill
        billIdDraft.value = bill.id
        invoiceNoDraft.value = bill.invoiceNo
        billDateMillisDraft.value = bill.dateMillis
        // Match existing customer or construct snapshot customer
        val foundCustomer = allCustomers.value.find { it.id == bill.customerId }
            ?: CustomerEntity(
                id = bill.customerId,
                name = bill.customerName,
                mobile = bill.customerMobile,
                address = bill.customerAddress,
                gstNo = bill.customerGstNo
            )
        selectedCustomerDraft.value = foundCustomer
        dimensionUnitDraft.value = bill.dimensionUnit
        taxRateDraft.value = bill.taxRate
        isGstIncludedDraft.value = bill.isGstIncluded
        discountDraft.value = bill.discountAmount
        otherChargesDraft.value = bill.otherCharges
        otherChargesDescDraft.value = bill.otherChargesDescription.ifBlank { "Cutting Charges" }
        roundOffDraft.value = bill.roundOffAmount
        isRoundOffAutoDraft.value = (bill.roundOffAmount != 0.0)
        paidAmountDraft.value = bill.paidAmount
        notesDraft.value = bill.notes
        billItemsDraft.value = billWithItems.items
        resetItemInputs()
        navigateTo(AppScreen.NEW_ENTRY)
    }

    fun resetItemInputs() {
        itemParticularInput.value = ""
        itemHsnInput.value = "4418"
        itemHeightInput.value = ""
        itemWidthInput.value = ""
        itemQtyInput.value = "1"
        itemRateInput.value = ""
    }

    fun addOrUpdateItemToBill(editingIndex: Int? = null) {
        val particular = itemParticularInput.value.trim()
        val height = itemHeightInput.value.toDoubleOrNull() ?: 0.0
        val width = itemWidthInput.value.toDoubleOrNull() ?: 0.0
        val qty = itemQtyInput.value.toIntOrNull() ?: 1
        val rate = itemRateInput.value.toDoubleOrNull() ?: 0.0

        if (particular.isBlank()) {
            showMessage("Please enter item description / particular")
            return
        }
        if (height <= 0 || width <= 0) {
            showMessage("Please enter valid Height and Width")
            return
        }
        if (qty <= 0) {
            showMessage("Quantity must be at least 1")
            return
        }
        if (rate <= 0) {
            showMessage("Please enter Rate per Sq.Ft")
            return
        }

        val sqFt = DimensionCalculator.calculateSqFt(height, width, qty, dimensionUnitDraft.value)
        val amount = DimensionCalculator.calculateAmount(sqFt, rate)

        val currentList = billItemsDraft.value.toMutableList()
        val slNo = if (editingIndex != null && editingIndex in currentList.indices) {
            currentList[editingIndex].slNo
        } else {
            currentList.size + 1
        }

        val newItem = BillItemEntity(
            slNo = slNo,
            particular = particular,
            hsnSac = itemHsnInput.value.trim().ifBlank { "4418" },
            height = height,
            width = width,
            qty = qty,
            sqFt = sqFt,
            rate = rate,
            amount = amount
        )

        if (editingIndex != null && editingIndex in currentList.indices) {
            currentList[editingIndex] = newItem
        } else {
            currentList.add(newItem)
        }

        // Re-index serial numbers
        billItemsDraft.value = currentList.mapIndexed { idx, itm -> itm.copy(slNo = idx + 1) }
        resetItemInputs()
        showMessage("Item added to bill")
    }

    fun removeItemFromBill(index: Int) {
        val currentList = billItemsDraft.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            billItemsDraft.value = currentList.mapIndexed { idx, itm -> itm.copy(slNo = idx + 1) }
            showMessage("Item removed")
        }
    }

    fun recalculateBillItemsForUnit(newUnit: String) {
        dimensionUnitDraft.value = newUnit
        val updated = billItemsDraft.value.map { item ->
            val newSqFt = DimensionCalculator.calculateSqFt(item.height, item.width, item.qty, newUnit)
            val newAmount = DimensionCalculator.calculateAmount(newSqFt, item.rate)
            item.copy(sqFt = newSqFt, amount = newAmount)
        }
        billItemsDraft.value = updated
    }

    fun saveCurrentBill(onSaved: (BillWithItems) -> Unit = {}) {
        val customer = selectedCustomerDraft.value
        if (customer == null) {
            showMessage("Please select or add a customer for this bill")
            return
        }
        val items = billItemsDraft.value
        if (items.isEmpty()) {
            showMessage("Please add at least one item (door) to the bill")
            return
        }

        val subTotal = items.sumOf { it.amount }
        val taxRate = if (isGstIncludedDraft.value) taxRateDraft.value else 0.0
        val gstTotal = if (taxRate > 0) (subTotal * taxRate / 100.0) else 0.0
        val halfGst = gstTotal / 2.0
        val otherCharges = otherChargesDraft.value
        val otherChargesDesc = otherChargesDescDraft.value.trim().ifBlank { "Cutting Charges" }
        val rawGrandTotal = Math.max(0.0, (subTotal + gstTotal + otherCharges) - discountDraft.value)
        val roundOff = if (isRoundOffAutoDraft.value) {
            val rounded = Math.round(rawGrandTotal).toDouble()
            rounded - rawGrandTotal
        } else {
            roundOffDraft.value
        }
        val grandTotal = Math.max(0.0, rawGrandTotal + roundOff)

        viewModelScope.launch {
            val billEntity = BillEntity(
                id = billIdDraft.value,
                invoiceNo = invoiceNoDraft.value.ifBlank { repository.generateNextInvoiceNumber() },
                customerId = customer.id,
                customerName = customer.name,
                customerMobile = customer.mobile,
                customerAddress = customer.address,
                customerGstNo = customer.gstNo,
                dateMillis = billDateMillisDraft.value,
                dimensionUnit = dimensionUnitDraft.value,
                taxRate = taxRate,
                isGstIncluded = isGstIncludedDraft.value,
                subTotal = subTotal,
                cgstAmount = halfGst,
                sgstAmount = halfGst,
                igstAmount = 0.0,
                discountAmount = discountDraft.value,
                otherCharges = otherCharges,
                otherChargesDescription = otherChargesDesc,
                roundOffAmount = roundOff,
                grandTotal = grandTotal,
                paidAmount = paidAmountDraft.value,
                notes = notesDraft.value.trim()
            )

            val savedBillId = repository.saveBill(billEntity, items)
            val finalBill = billEntity.copy(id = savedBillId)
            val finalBillWithItems = BillWithItems(finalBill, items.map { it.copy(billId = savedBillId) })

            // If an initial paid amount was provided upon bill creation and it's a new bill, record it as payment
            if (billIdDraft.value == 0L && paidAmountDraft.value > 0) {
                repository.addPayment(
                    PaymentEntity(
                        customerId = customer.id,
                        customerName = customer.name,
                        billId = savedBillId,
                        amount = paidAmountDraft.value,
                        dateMillis = billDateMillisDraft.value,
                        paymentMode = "Cash",
                        notes = "Initial payment against Invoice #${finalBill.invoiceNo}"
                    )
                )
            }

            showMessage("Bill #${finalBill.invoiceNo} saved successfully!")
            onSaved(finalBillWithItems)
            navigateTo(AppScreen.EDIT_ENTRY)
        }
    }

    fun deleteBill(billId: Long) {
        viewModelScope.launch {
            repository.deleteBill(billId)
            showMessage("Bill deleted successfully")
        }
    }

    // -------------------------------------------------------------
    // 3) CUSTOMER BALANCE & LEDGER
    // -------------------------------------------------------------
    val customerBalances: StateFlow<List<CustomerBalanceSummary>> = repository.customerBalancesSummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedLedgerCustomer = MutableStateFlow<CustomerEntity?>(null)
    val ledgerEntries = MutableStateFlow<List<LedgerEntry>>(emptyList())

    fun openCustomerLedger(customer: CustomerEntity) {
        selectedLedgerCustomer.value = customer
        viewModelScope.launch {
            repository.getCustomerLedger(customer.id).collect { entries ->
                ledgerEntries.value = entries
            }
        }
        navigateTo(AppScreen.CUSTOMER_LEDGER)
    }

    fun recordCustomerPayment(
        customerId: Long,
        customerName: String,
        amount: Double,
        mode: String,
        reference: String,
        notes: String,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        if (amount <= 0) {
            showMessage("Please enter a valid payment amount")
            return
        }
        viewModelScope.launch {
            repository.addPayment(
                PaymentEntity(
                    customerId = customerId,
                    customerName = customerName,
                    amount = amount,
                    dateMillis = dateMillis,
                    paymentMode = mode,
                    referenceNo = reference,
                    notes = notes
                )
            )
            showMessage("Payment of ₹${DimensionCalculator.formatDimension(amount)} recorded!")
        }
    }

    // -------------------------------------------------------------
    // 4) COMPANY PROFILE
    // -------------------------------------------------------------
    val companyProfile: StateFlow<CompanyProfileEntity> = repository.companyProfile
        .map { it ?: CompanyProfileEntity() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CompanyProfileEntity()
        )

    fun updateCompanyProfile(profile: CompanyProfileEntity) {
        viewModelScope.launch {
            repository.updateCompanyProfile(profile)
            showMessage("Company profile updated successfully")
        }
    }

    // -------------------------------------------------------------
    // 5) PURCHASE MODULE: SUPPLIER, INVOICE, PAYMENT & BALANCES
    // -------------------------------------------------------------
    val allSuppliers: StateFlow<List<SupplierEntity>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchases: StateFlow<List<PurchaseWithItems>> = repository.allPurchasesWithItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supplierBalances: StateFlow<List<SupplierBalanceSummary>> = repository.supplierBalances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Supplier Form Inputs
    val supplierNameInput = MutableStateFlow("")
    val supplierMobileInput = MutableStateFlow("")
    val supplierAddressInput = MutableStateFlow("")
    val supplierGstInput = MutableStateFlow("")
    val editingSupplierId = MutableStateFlow(0L)

    fun prepareNewSupplier() {
        editingSupplierId.value = 0L
        supplierNameInput.value = ""
        supplierMobileInput.value = ""
        supplierAddressInput.value = ""
        supplierGstInput.value = ""
    }

    fun prepareEditSupplier(supplier: SupplierEntity) {
        editingSupplierId.value = supplier.id
        supplierNameInput.value = supplier.name
        supplierMobileInput.value = supplier.mobile
        supplierAddressInput.value = supplier.address
        supplierGstInput.value = supplier.gstNo
    }

    fun saveSupplier(onSuccess: () -> Unit = {}) {
        val name = supplierNameInput.value.trim()
        if (name.isBlank()) {
            showMessage("Supplier / Party name is required")
            return
        }
        viewModelScope.launch {
            val entity = SupplierEntity(
                id = editingSupplierId.value,
                name = name,
                mobile = supplierMobileInput.value.trim(),
                address = supplierAddressInput.value.trim(),
                gstNo = supplierGstInput.value.trim().uppercase()
            )
            repository.saveSupplier(entity)
            showMessage("Supplier '$name' saved successfully")
            prepareNewSupplier()
            onSuccess()
        }
    }

    fun deleteSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
            showMessage("Supplier '${supplier.name}' deleted")
        }
    }

    fun saveSupplierDirect(supplier: SupplierEntity, onSaved: (SupplierEntity) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveSupplier(supplier)
            val saved = if (supplier.id == 0L) supplier.copy(id = id) else supplier
            onSaved(saved)
        }
    }

    // Purchase Invoice Draft State
    val purchaseIdDraft = MutableStateFlow(0L)
    val purchaseInvoiceNoDraft = MutableStateFlow("")
    val purchaseSupplierDraft = MutableStateFlow<SupplierEntity?>(null)
    val purchaseDateMillisDraft = MutableStateFlow(System.currentTimeMillis())
    val purchaseDimensionUnitDraft = MutableStateFlow("Inches")
    val purchaseTaxRateDraft = MutableStateFlow(18.0)
    val purchaseIsGstIncludedDraft = MutableStateFlow(true)
    val purchaseDiscountDraft = MutableStateFlow(0.0)
    val purchaseOtherChargesDraft = MutableStateFlow(0.0)
    val purchaseOtherChargesDescDraft = MutableStateFlow("Transportation")
    val purchaseIsRoundOffAutoDraft = MutableStateFlow(true)
    val purchaseRoundOffDraft = MutableStateFlow(0.0)
    val purchasePaidAmountDraft = MutableStateFlow(0.0)
    val purchaseNotesDraft = MutableStateFlow("")
    val purchaseItemsDraft = MutableStateFlow<List<PurchaseItemEntity>>(emptyList())

    fun setPurchaseDate(dateMillis: Long) {
        purchaseDateMillisDraft.value = dateMillis
    }

    // Purchase Item Inputs
    val purchaseItemParticularInput = MutableStateFlow("")
    val purchaseItemHsnInput = MutableStateFlow("4418")
    val purchaseItemHeightInput = MutableStateFlow("")
    val purchaseItemWidthInput = MutableStateFlow("")
    val purchaseItemQtyInput = MutableStateFlow("1")
    val purchaseItemRateInput = MutableStateFlow("")

    fun resetPurchaseItemInputs() {
        purchaseItemParticularInput.value = ""
        purchaseItemHsnInput.value = "4418"
        purchaseItemHeightInput.value = ""
        purchaseItemWidthInput.value = ""
        purchaseItemQtyInput.value = "1"
        purchaseItemRateInput.value = ""
    }

    fun startNewPurchase(presetSupplier: SupplierEntity? = null) {
        purchaseIdDraft.value = 0L
        purchaseInvoiceNoDraft.value = "PUR-" + System.currentTimeMillis().toString().takeLast(5)
        purchaseDateMillisDraft.value = System.currentTimeMillis()
        purchaseSupplierDraft.value = presetSupplier
        purchaseDimensionUnitDraft.value = "Inches"
        purchaseTaxRateDraft.value = 18.0
        purchaseIsGstIncludedDraft.value = true
        purchaseDiscountDraft.value = 0.0
        purchaseOtherChargesDraft.value = 0.0
        purchaseOtherChargesDescDraft.value = "Transportation"
        purchaseIsRoundOffAutoDraft.value = true
        purchaseRoundOffDraft.value = 0.0
        purchasePaidAmountDraft.value = 0.0
        purchaseNotesDraft.value = ""
        purchaseItemsDraft.value = emptyList()
        resetPurchaseItemInputs()
    }

    fun startEditPurchase(purchaseWithItems: PurchaseWithItems) {
        val purchase = purchaseWithItems.purchase
        purchaseIdDraft.value = purchase.id
        purchaseInvoiceNoDraft.value = purchase.invoiceNo
        purchaseDateMillisDraft.value = purchase.dateMillis
        val found = allSuppliers.value.find { it.id == purchase.supplierId }
            ?: SupplierEntity(
                id = purchase.supplierId,
                name = purchase.supplierName,
                mobile = purchase.supplierMobile,
                address = purchase.supplierAddress,
                gstNo = purchase.supplierGstNo
            )
        purchaseSupplierDraft.value = found
        purchaseDimensionUnitDraft.value = purchase.dimensionUnit
        purchaseTaxRateDraft.value = purchase.taxRate
        purchaseIsGstIncludedDraft.value = purchase.isGstIncluded
        purchaseDiscountDraft.value = purchase.discountAmount
        purchaseOtherChargesDraft.value = purchase.otherCharges
        purchaseOtherChargesDescDraft.value = purchase.otherChargesDescription
        purchaseRoundOffDraft.value = purchase.roundOffAmount
        purchaseIsRoundOffAutoDraft.value = (purchase.roundOffAmount != 0.0)
        purchasePaidAmountDraft.value = purchase.paidAmount
        purchaseNotesDraft.value = purchase.notes
        purchaseItemsDraft.value = purchaseWithItems.items
        resetPurchaseItemInputs()
    }

    fun addOrUpdateItemToPurchase(editingIndex: Int? = null) {
        val particular = purchaseItemParticularInput.value.trim()
        val height = purchaseItemHeightInput.value.toDoubleOrNull() ?: 0.0
        val width = purchaseItemWidthInput.value.toDoubleOrNull() ?: 0.0
        val qty = purchaseItemQtyInput.value.toIntOrNull() ?: 1
        val rate = purchaseItemRateInput.value.toDoubleOrNull() ?: 0.0

        if (particular.isBlank()) {
            showMessage("Please enter item description / material")
            return
        }
        if (qty <= 0) {
            showMessage("Quantity must be at least 1")
            return
        }
        if (rate <= 0) {
            showMessage("Please enter valid Rate")
            return
        }

        val sqFt = if (height > 0 && width > 0) {
            DimensionCalculator.calculateSqFt(height, width, qty, purchaseDimensionUnitDraft.value)
        } else {
            qty.toDouble()
        }
        val amount = if (height > 0 && width > 0) {
            DimensionCalculator.calculateAmount(sqFt, rate)
        } else {
            qty * rate
        }

        val currentList = purchaseItemsDraft.value.toMutableList()
        val slNo = if (editingIndex != null && editingIndex in currentList.indices) editingIndex + 1 else currentList.size + 1
        val newItem = PurchaseItemEntity(
            id = if (editingIndex != null && editingIndex in currentList.indices) currentList[editingIndex].id else 0L,
            purchaseId = purchaseIdDraft.value,
            slNo = slNo,
            particular = particular,
            hsnSac = purchaseItemHsnInput.value.trim().ifBlank { "4418" },
            height = height,
            width = width,
            qty = qty,
            sqFt = sqFt,
            rate = rate,
            amount = amount
        )

        if (editingIndex != null && editingIndex in currentList.indices) {
            currentList[editingIndex] = newItem
        } else {
            currentList.add(newItem)
        }
        purchaseItemsDraft.value = currentList.mapIndexed { idx, itm -> itm.copy(slNo = idx + 1) }
        resetPurchaseItemInputs()
        showMessage("Item added to purchase bill")
    }

    fun removeItemFromPurchase(index: Int) {
        val currentList = purchaseItemsDraft.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            purchaseItemsDraft.value = currentList.mapIndexed { idx, itm -> itm.copy(slNo = idx + 1) }
            showMessage("Item removed")
        }
    }

    fun saveCurrentPurchase(onSaved: (PurchaseWithItems) -> Unit = {}) {
        val supplier = purchaseSupplierDraft.value
        if (supplier == null) {
            showMessage("Please select or add a supplier for this purchase bill")
            return
        }
        val items = purchaseItemsDraft.value
        if (items.isEmpty()) {
            showMessage("Please add at least one item to the purchase bill")
            return
        }

        val subTotal = items.sumOf { it.amount }
        val taxRate = if (purchaseIsGstIncludedDraft.value) purchaseTaxRateDraft.value else 0.0
        val gstTotal = if (taxRate > 0) (subTotal * taxRate / 100.0) else 0.0
        val halfGst = gstTotal / 2.0
        val otherCharges = purchaseOtherChargesDraft.value
        val otherChargesDesc = purchaseOtherChargesDescDraft.value.trim().ifBlank { "Transportation" }
        val rawGrandTotal = Math.max(0.0, (subTotal + gstTotal + otherCharges) - purchaseDiscountDraft.value)
        val roundOff = if (purchaseIsRoundOffAutoDraft.value) {
            val rounded = Math.round(rawGrandTotal).toDouble()
            rounded - rawGrandTotal
        } else {
            purchaseRoundOffDraft.value
        }
        val grandTotal = Math.max(0.0, rawGrandTotal + roundOff)

        viewModelScope.launch {
            val purchaseEntity = PurchaseEntity(
                id = purchaseIdDraft.value,
                invoiceNo = purchaseInvoiceNoDraft.value.ifBlank { "PUR-" + System.currentTimeMillis().toString().takeLast(5) },
                supplierId = supplier.id,
                supplierName = supplier.name,
                supplierMobile = supplier.mobile,
                supplierAddress = supplier.address,
                supplierGstNo = supplier.gstNo,
                dateMillis = purchaseDateMillisDraft.value,
                dimensionUnit = purchaseDimensionUnitDraft.value,
                taxRate = taxRate,
                isGstIncluded = purchaseIsGstIncludedDraft.value,
                subTotal = subTotal,
                cgstAmount = halfGst,
                sgstAmount = halfGst,
                igstAmount = 0.0,
                discountAmount = purchaseDiscountDraft.value,
                otherCharges = otherCharges,
                otherChargesDescription = otherChargesDesc,
                roundOffAmount = roundOff,
                grandTotal = grandTotal,
                paidAmount = purchasePaidAmountDraft.value,
                notes = purchaseNotesDraft.value.trim()
            )

            val savedId = repository.savePurchase(purchaseEntity, items)
            val finalPurchase = purchaseEntity.copy(id = savedId)
            val finalWithItems = PurchaseWithItems(finalPurchase, items.map { it.copy(purchaseId = savedId) })

            // If initial payment given on purchase creation
            if (purchaseIdDraft.value == 0L && purchasePaidAmountDraft.value > 0) {
                repository.recordPurchasePayment(
                    PurchasePaymentEntity(
                        supplierId = supplier.id,
                        supplierName = supplier.name,
                        purchaseId = savedId,
                        amount = purchasePaidAmountDraft.value,
                        dateMillis = purchaseDateMillisDraft.value,
                        paymentMode = "Bank Transfer",
                        notes = "Initial payment against Purchase #${finalPurchase.invoiceNo}"
                    )
                )
            }

            showMessage("Purchase Bill #${finalPurchase.invoiceNo} saved successfully!")
            onSaved(finalWithItems)
        }
    }

    fun deletePurchase(purchaseId: Long) {
        viewModelScope.launch {
            repository.deletePurchase(purchaseId)
            showMessage("Purchase bill deleted")
        }
    }

    // Supplier Ledger & Payment Recording
    val selectedLedgerSupplier = MutableStateFlow<SupplierEntity?>(null)
    val supplierLedgerEntries = MutableStateFlow<List<SupplierLedgerEntry>>(emptyList())

    fun openSupplierLedger(supplier: SupplierEntity) {
        selectedLedgerSupplier.value = supplier
        viewModelScope.launch {
            repository.getSupplierLedger(supplier.id).collect { entries ->
                supplierLedgerEntries.value = entries
            }
        }
    }

    fun recordSupplierPayment(
        supplierId: Long,
        supplierName: String,
        amount: Double,
        mode: String,
        reference: String,
        notes: String,
        dateMillis: Long = System.currentTimeMillis(),
        onSuccess: () -> Unit = {}
    ) {
        if (amount <= 0) {
            showMessage("Please enter a valid payment amount")
            return
        }
        viewModelScope.launch {
            repository.recordPurchasePayment(
                PurchasePaymentEntity(
                    supplierId = supplierId,
                    supplierName = supplierName,
                    amount = amount,
                    dateMillis = dateMillis,
                    paymentMode = mode,
                    referenceNo = reference,
                    notes = notes
                )
            )
            showMessage("Payment of ₹${DimensionCalculator.formatDimension(amount)} to $supplierName recorded!")
            onSuccess()
        }
    }

    fun deleteSupplierPayment(paymentId: Long) {
        viewModelScope.launch {
            repository.deletePurchasePayment(paymentId)
            showMessage("Payment entry deleted")
        }
    }
}
