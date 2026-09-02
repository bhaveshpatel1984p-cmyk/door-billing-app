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
import com.example.data.repository.DoorBillingRepository
import com.example.util.DimensionCalculator
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
    COMPANY_PROFILE
}

class DoorBillingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DoorBillingRepository

    init {
        val db = DoorDatabase.getDatabase(application)
        repository = DoorBillingRepository(
            customerDao = db.customerDao(),
            billDao = db.billDao(),
            paymentDao = db.paymentDao(),
            companyProfileDao = db.companyProfileDao()
        )
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
    val paidAmountDraft = MutableStateFlow(0.0)
    val notesDraft = MutableStateFlow("")
    val billItemsDraft = MutableStateFlow<List<BillItemEntity>>(emptyList())

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
        val grandTotal = Math.max(0.0, (subTotal + gstTotal) - discountDraft.value)

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
        notes: String
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
}
