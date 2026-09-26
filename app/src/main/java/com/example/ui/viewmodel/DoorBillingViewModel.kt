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
import com.example.data.db.PurchaseReturnEntity
import com.example.data.db.PurchaseReturnItemEntity
import com.example.data.db.PurchaseReturnWithItems
import com.example.data.db.RawMaterialCatalogEntity
import com.example.data.db.DoorPresetEntity
import java.util.Calendar
import com.example.data.repository.DoorBillingRepository
import com.example.util.DimensionCalculator
import android.content.Context
import android.content.Intent
import com.example.data.backup.AppBackupData
import com.example.data.backup.BackupSummary
import com.example.data.backup.DriveFileInfo
import com.example.data.backup.GoogleDriveManager
import com.example.util.LocalBackupHelper
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
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
import kotlinx.coroutines.withContext

enum class AppScreen {
    DASHBOARD,
    CREATE_CUSTOMER,
    NEW_ENTRY,
    EDIT_ENTRY,
    CUSTOMER_BALANCE,
    CUSTOMER_LEDGER,
    COMPANY_PROFILE,
    FINANCIAL_STATS,
    PURCHASE_HUB,
    BACKUP_SYNC
}

class DoorBillingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DoorDatabase.getDatabase(application)
    val googleDriveManager = GoogleDriveManager(application)
    private val repository: DoorBillingRepository

    // Cloud Backup & Multi-Device Sync State
    private val prefs = application.getSharedPreferences("door_billing_backup_prefs", Context.MODE_PRIVATE)

    private val _googleAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val googleAccount: StateFlow<GoogleSignInAccount?> = _googleAccount.asStateFlow()

    private val _driveBackupInfo = MutableStateFlow<DriveFileInfo?>(null)
    val driveBackupInfo: StateFlow<DriveFileInfo?> = _driveBackupInfo.asStateFlow()

    private val _isBackupOperating = MutableStateFlow(false)
    val isBackupOperating: StateFlow<Boolean> = _isBackupOperating.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(prefs.getLong("last_backup_time", 0L))
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private val _lastBackupType = MutableStateFlow(prefs.getString("last_backup_type", "") ?: "")
    val lastBackupType: StateFlow<String> = _lastBackupType.asStateFlow()

    init {
        repository = DoorBillingRepository(
            customerDao = database.customerDao(),
            billDao = database.billDao(),
            paymentDao = database.paymentDao(),
            companyProfileDao = database.companyProfileDao(),
            supplierDao = database.supplierDao(),
            purchaseDao = database.purchaseDao(),
            purchasePaymentDao = database.purchasePaymentDao(),
            doorPresetDao = database.doorPresetDao(),
            purchaseReturnDao = database.purchaseReturnDao(),
            rawMaterialCatalogDao = database.rawMaterialCatalogDao()
        )

        // Initialize default raw material catalog if empty
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.initDefaultRawMaterialsIfEmpty()
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Initialize Google Account state
        refreshGoogleAccount()

        // Ensure company profile has a valid UPI ID for automatic QR code generation
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profile = repository.getCompanyProfileSync()
                if (profile.upiId.isBlank()) {
                    val defaultUpi = if (profile.mobile.isNotBlank()) "${profile.mobile.trim()}@upi" else "nirmaldoor@upi"
                    repository.updateCompanyProfile(profile.copy(upiId = defaultUpi))
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Pre-populate standard door presets if empty
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (repository.getPresetsCount() == 0) {
                    repository.insertDefaultPresets(
                        listOf(
                            DoorPresetEntity(name = "Laminated Flush Door 30mm", defaultRate = 145.0, defaultHsn = "4418", defaultHeight = 78.0, defaultWidth = 30.0),
                            DoorPresetEntity(name = "Pine Wood Door Frame", defaultRate = 180.0, defaultHsn = "4418", defaultHeight = 84.0, defaultWidth = 36.0),
                            DoorPresetEntity(name = "Teak Finish Moulded Door", defaultRate = 165.0, defaultHsn = "4418", defaultHeight = 78.0, defaultWidth = 32.0),
                            DoorPresetEntity(name = "Membrane Designer Door", defaultRate = 195.0, defaultHsn = "4418", defaultHeight = 80.0, defaultWidth = 32.0),
                            DoorPresetEntity(name = "Waterproof PVC Panel Door", defaultRate = 130.0, defaultHsn = "3925", defaultHeight = 72.0, defaultWidth = 27.0),
                            DoorPresetEntity(name = "Veneer Polished Door 32mm", defaultRate = 260.0, defaultHsn = "4418", defaultHeight = 81.0, defaultWidth = 36.0),
                            DoorPresetEntity(name = "Commercial Flush Door 25mm", defaultRate = 110.0, defaultHsn = "4418", defaultHeight = 78.0, defaultWidth = 30.0),
                            DoorPresetEntity(name = "Glass Cutout Flush Door", defaultRate = 175.0, defaultHsn = "4418", defaultHeight = 78.0, defaultWidth = 32.0)
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore seed error
            }
        }

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
    val customerFirmNameInput = MutableStateFlow("")
    val customerNameInput = MutableStateFlow("")
    val customerMobileInput = MutableStateFlow("")
    val customerAddressInput = MutableStateFlow("")
    val customerGstInput = MutableStateFlow("")
    val editingCustomerId = MutableStateFlow<Long?>(null)

    fun prepareNewCustomer() {
        customerFirmNameInput.value = ""
        customerNameInput.value = ""
        customerMobileInput.value = ""
        customerAddressInput.value = ""
        customerGstInput.value = ""
        editingCustomerId.value = null
    }

    fun prepareEditCustomer(customer: CustomerEntity) {
        customerFirmNameInput.value = if (customer.firmName.isNotBlank()) customer.firmName else customer.name
        customerNameInput.value = if (customer.firmName.isNotBlank()) customer.name else ""
        customerMobileInput.value = customer.mobile
        customerAddressInput.value = customer.address
        customerGstInput.value = customer.gstNo
        editingCustomerId.value = customer.id
    }

    fun saveCustomer(onSuccess: (CustomerEntity) -> Unit = {}) {
        val firmName = customerFirmNameInput.value.trim()
        val contactName = customerNameInput.value.trim()
        if (firmName.isBlank() && contactName.isBlank()) {
            showMessage("Please enter Firm Name")
            return
        }

        val finalFirmName = if (firmName.isNotBlank()) firmName else contactName
        val finalContactName = if (firmName.isNotBlank()) contactName else ""

        viewModelScope.launch {
            val customer = CustomerEntity(
                id = editingCustomerId.value ?: 0L,
                firmName = finalFirmName,
                name = finalContactName,
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
            showMessage("Customer ${customer.displayName} deleted")
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
    val isGstIncludedDraft = MutableStateFlow(false)
    val discountDraft = MutableStateFlow(0.0)
    val otherChargesDraft = MutableStateFlow(0.0)
    val otherChargesDescDraft = MutableStateFlow("Cutting Charges")
    val isRoundOffAutoDraft = MutableStateFlow(true)
    val roundOffDraft = MutableStateFlow(0.0)
    val paidAmountDraft = MutableStateFlow(0.0)
    val previousBalanceDraft = MutableStateFlow(0.0)
    val includePreviousBalanceDraft = MutableStateFlow(false)
    val notesDraft = MutableStateFlow("")
    val billItemsDraft = MutableStateFlow<List<BillItemEntity>>(emptyList())
    val isQuotationDraft = MutableStateFlow(false)

    // Door Presets
    val allDoorPresets: StateFlow<List<DoorPresetEntity>> = repository.allDoorPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveDoorPreset(preset: DoorPresetEntity) {
        viewModelScope.launch {
            repository.saveDoorPreset(preset)
            showMessage("Preset '${preset.name}' saved!")
        }
    }

    fun deleteDoorPreset(preset: DoorPresetEntity) {
        viewModelScope.launch {
            repository.deleteDoorPreset(preset)
            showMessage("Preset removed")
        }
    }

    fun applyDoorPreset(preset: DoorPresetEntity) {
        itemParticularInput.value = preset.name
        itemHsnInput.value = preset.defaultHsn
        itemRateInput.value = DimensionCalculator.formatDimension(preset.defaultRate)
        if (itemHeightInput.value.isBlank() && preset.defaultHeight > 0) {
            itemHeightInput.value = DimensionCalculator.formatDimension(preset.defaultHeight)
        }
        if (itemWidthInput.value.isBlank() && preset.defaultWidth > 0) {
            itemWidthInput.value = DimensionCalculator.formatDimension(preset.defaultWidth)
        }
        showMessage("Applied ${preset.name}")
    }

    fun toggleBillType(isQuotation: Boolean) {
        isQuotationDraft.value = isQuotation
        viewModelScope.launch {
            if (isQuotation) {
                if (invoiceNoDraft.value.startsWith("INV/")) {
                    invoiceNoDraft.value = repository.generateNextQuotationNumber()
                }
            } else {
                if (invoiceNoDraft.value.startsWith("EST/")) {
                    invoiceNoDraft.value = repository.generateNextInvoiceNumber()
                }
            }
        }
    }

    fun convertQuotationToInvoice(billWithItems: BillWithItems, onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            val newInv = repository.convertQuotationToInvoice(billWithItems.bill.id)
            showMessage("Quotation successfully converted to Tax Invoice $newInv")
            onComplete(newInv)
        }
    }

    // App Security PIN & Lock Settings
    private val securityPrefs = application.getSharedPreferences("door_billing_security_prefs", Context.MODE_PRIVATE)
    val isPinLockEnabled = MutableStateFlow(securityPrefs.getBoolean("pin_lock_enabled", true))
    val appSecurityPin = MutableStateFlow(securityPrefs.getString("app_pin", "1100") ?: "1100")

    fun updateSecurityPin(oldPin: String, newPin: String): Boolean {
        if (oldPin != appSecurityPin.value) {
            showMessage("Current PIN is incorrect!")
            return false
        }
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            showMessage("PIN must be exactly 4 digits!")
            return false
        }
        securityPrefs.edit().putString("app_pin", newPin).apply()
        appSecurityPin.value = newPin
        showMessage("Security PIN updated successfully!")
        return true
    }

    fun setPinLockEnabled(enabled: Boolean) {
        securityPrefs.edit().putBoolean("pin_lock_enabled", enabled).apply()
        isPinLockEnabled.value = enabled
        showMessage(if (enabled) "Security PIN Lock enabled" else "Security PIN Lock disabled")
    }

    fun resetPinWithMasterOrMobile(enteredVerification: String): Boolean {
        val cleanVerification = enteredVerification.trim()
        val allCompanyMobiles = companyProfile.value?.allMobiles ?: emptyList()
        val matchesAnyMobile = allCompanyMobiles.any { mob ->
            cleanVerification == mob.trim() || (mob.length >= 10 && cleanVerification == mob.takeLast(10))
        }
        if (cleanVerification == "9876" || matchesAnyMobile) {
            securityPrefs.edit().putString("app_pin", "1100").apply()
            appSecurityPin.value = "1100"
            showMessage("PIN has been reset to default: 1100")
            return true
        }
        showMessage("Verification failed! Enter registered mobile number or master PIN.")
        return false
    }

    fun selectCustomerForBill(customer: CustomerEntity?) {
        selectedCustomerDraft.value = customer
        if (customer != null) {
            viewModelScope.launch {
                val prevBal = repository.getCustomerDueBalance(customer.id)
                previousBalanceDraft.value = prevBal
                includePreviousBalanceDraft.value = (prevBal > 0)
            }
        } else {
            previousBalanceDraft.value = 0.0
            includePreviousBalanceDraft.value = false
        }
    }

    fun setPreviousBalance(amount: Double) {
        val positiveAmount = Math.max(0.0, amount)
        previousBalanceDraft.value = positiveAmount
        includePreviousBalanceDraft.value = (positiveAmount > 0)
    }

    fun toggleIncludePreviousBalance(include: Boolean) {
        includePreviousBalanceDraft.value = include
    }

    fun setBillDate(dateMillis: Long) {
        billDateMillisDraft.value = dateMillis
    }

    // Temporary Item Line Inputs
    val itemParticularInput = MutableStateFlow("")
    val itemDoorSizeInput = MutableStateFlow("")
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
            if (presetCustomer != null) {
                val prevBal = repository.getCustomerDueBalance(presetCustomer.id)
                previousBalanceDraft.value = prevBal
                includePreviousBalanceDraft.value = (prevBal > 0)
            } else {
                previousBalanceDraft.value = 0.0
                includePreviousBalanceDraft.value = false
            }
            dimensionUnitDraft.value = "Inches"
            taxRateDraft.value = 18.0
            isGstIncludedDraft.value = false
            discountDraft.value = 0.0
            otherChargesDraft.value = 0.0
            otherChargesDescDraft.value = "Cutting Charges"
            isRoundOffAutoDraft.value = true
            roundOffDraft.value = 0.0
            paidAmountDraft.value = 0.0
            isQuotationDraft.value = false
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
                firmName = bill.customerName,
                name = "",
                mobile = bill.customerMobile,
                address = bill.customerAddress,
                gstNo = bill.customerGstNo
            )
        selectedCustomerDraft.value = foundCustomer
        previousBalanceDraft.value = bill.previousBalance
        includePreviousBalanceDraft.value = (bill.previousBalance > 0)

        dimensionUnitDraft.value = bill.dimensionUnit
        taxRateDraft.value = bill.taxRate
        isGstIncludedDraft.value = bill.isGstIncluded
        discountDraft.value = bill.discountAmount
        otherChargesDraft.value = bill.otherCharges
        otherChargesDescDraft.value = bill.otherChargesDescription.ifBlank { "Cutting Charges" }
        roundOffDraft.value = bill.roundOffAmount
        isRoundOffAutoDraft.value = (bill.roundOffAmount != 0.0)
        paidAmountDraft.value = bill.paidAmount
        isQuotationDraft.value = bill.isQuotation
        notesDraft.value = bill.notes
        billItemsDraft.value = billWithItems.items
        resetItemInputs()
        navigateTo(AppScreen.NEW_ENTRY)
    }

    fun resetItemInputs() {
        itemParticularInput.value = ""
        itemDoorSizeInput.value = ""
        itemHsnInput.value = "4418"
        itemHeightInput.value = ""
        itemWidthInput.value = ""
        itemQtyInput.value = "1"
        itemRateInput.value = ""
    }

    fun prepareEditItem(item: BillItemEntity) {
        if (item.particular.contains("\n")) {
            val parts = item.particular.split("\n", limit = 2)
            itemParticularInput.value = parts[0].trim()
            itemDoorSizeInput.value = parts[1].trim()
        } else {
            itemParticularInput.value = item.particular
            itemDoorSizeInput.value = ""
        }
        itemHsnInput.value = item.hsnSac
        itemHeightInput.value = DimensionCalculator.formatDimension(item.height)
        itemWidthInput.value = DimensionCalculator.formatDimension(item.width)
        itemQtyInput.value = item.qty.toString()
        itemRateInput.value = DimensionCalculator.formatDimension(item.rate)
    }

    fun addOrUpdateItemToBill(editingIndex: Int? = null) {
        val rawPart = itemParticularInput.value.trim()
        val sizeNote = itemDoorSizeInput.value.trim()
        val particular = when {
            rawPart.contains("\n") -> rawPart
            sizeNote.isNotBlank() -> "$rawPart\n$sizeNote"
            else -> rawPart
        }
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
        val prevBalance = if (includePreviousBalanceDraft.value) previousBalanceDraft.value else 0.0
        val netPayable = grandTotal + prevBalance

        viewModelScope.launch {
            val billEntity = BillEntity(
                id = billIdDraft.value,
                invoiceNo = invoiceNoDraft.value.ifBlank {
                    if (isQuotationDraft.value) repository.generateNextQuotationNumber()
                    else repository.generateNextInvoiceNumber()
                },
                customerId = customer.id,
                customerName = customer.displayName,
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
                previousBalance = prevBalance,
                netPayable = netPayable,
                paidAmount = paidAmountDraft.value,
                isQuotation = isQuotationDraft.value,
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
                        customerName = customer.displayName,
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
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    fun updateCustomerPayment(
        paymentId: Long,
        customerId: Long,
        customerName: String,
        amount: Double,
        mode: String,
        reference: String,
        notes: String,
        dateMillis: Long,
        onSuccess: () -> Unit = {}
    ) {
        if (amount <= 0) {
            showMessage("Please enter a valid payment amount")
            return
        }
        viewModelScope.launch {
            repository.updatePayment(
                PaymentEntity(
                    id = paymentId,
                    customerId = customerId,
                    customerName = customerName,
                    amount = amount,
                    dateMillis = dateMillis,
                    paymentMode = mode,
                    referenceNo = reference,
                    notes = notes
                )
            )
            showMessage("Payment entry updated successfully!")
            onSuccess()
        }
    }

    fun deleteCustomerPayment(paymentId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deletePayment(paymentId)
            showMessage("Payment entry deleted")
            onSuccess()
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

    val allPurchaseReturns: StateFlow<List<PurchaseReturnWithItems>> = repository.allPurchaseReturns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRawMaterials: StateFlow<List<RawMaterialCatalogEntity>> = repository.allRawMaterials
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
    val purchaseIsGstIncludedDraft = MutableStateFlow(false)
    val purchaseDiscountDraft = MutableStateFlow(0.0)
    val purchaseDiscountTypeDraft = MutableStateFlow("FLAT") // "FLAT" or "PERCENT"
    val purchaseDiscountPercentDraft = MutableStateFlow(0.0)
    val purchaseOtherChargesDraft = MutableStateFlow(0.0)
    val purchaseOtherChargesDescDraft = MutableStateFlow("Transportation")
    val purchaseIsRoundOffAutoDraft = MutableStateFlow(true)
    val purchaseRoundOffDraft = MutableStateFlow(0.0)
    val purchasePaidAmountDraft = MutableStateFlow(0.0)
    val purchaseNotesDraft = MutableStateFlow("")
    val purchaseBillPhotoUriDraft = MutableStateFlow("")
    val purchaseTransportNameDraft = MutableStateFlow("")
    val purchaseVehicleNoDraft = MutableStateFlow("")
    val purchaseLrBiltyNoDraft = MutableStateFlow("")
    val purchaseItemsDraft = MutableStateFlow<List<PurchaseItemEntity>>(emptyList())

    fun setPurchaseDate(dateMillis: Long) {
        purchaseDateMillisDraft.value = dateMillis
    }

    fun setPurchaseDiscountType(type: String) {
        purchaseDiscountTypeDraft.value = type
        recalculatePurchaseDiscount()
    }

    fun setPurchaseDiscountPercent(percent: Double) {
        purchaseDiscountPercentDraft.value = percent
        recalculatePurchaseDiscount()
    }

    fun setPurchaseDiscountFlat(amount: Double) {
        purchaseDiscountDraft.value = amount
    }

    fun recalculatePurchaseDiscount() {
        if (purchaseDiscountTypeDraft.value == "PERCENT") {
            val subTotal = purchaseItemsDraft.value.sumOf { it.amount }
            val computed = (subTotal * purchaseDiscountPercentDraft.value) / 100.0
            purchaseDiscountDraft.value = Math.round(computed * 100.0) / 100.0
        }
    }

    fun setPurchaseBillPhoto(uri: String) {
        purchaseBillPhotoUriDraft.value = uri
    }

    // Purchase Item Inputs
    val purchaseItemParticularInput = MutableStateFlow("")
    val purchaseItemHsnInput = MutableStateFlow("4418")
    val purchaseItemHeightInput = MutableStateFlow("")
    val purchaseItemWidthInput = MutableStateFlow("")
    val purchaseItemQtyInput = MutableStateFlow("1")
    val purchaseItemRateInput = MutableStateFlow("")
    val purchaseItemUnitInput = MutableStateFlow("Pcs")

    val standardPurchaseUnits = listOf(
        "Pcs",
        "Kg",
        "Ltr",
        "Meter",
        "Box",
        "Sq.Ft",
        "Bundle",
        "Packet",
        "Set",
        "Ton",
        "Roll",
        "Bag",
        "Pair"
    )

    fun setPurchaseItemUnit(unit: String) {
        val trimmed = unit.trim()
        if (trimmed.isNotBlank()) {
            purchaseItemUnitInput.value = trimmed
        }
    }

    fun resetPurchaseItemInputs() {
        purchaseItemParticularInput.value = ""
        purchaseItemHsnInput.value = "4418"
        purchaseItemHeightInput.value = ""
        purchaseItemWidthInput.value = ""
        purchaseItemQtyInput.value = "1"
        purchaseItemRateInput.value = ""
        purchaseItemUnitInput.value = "Pcs"
    }

    fun startEditPurchaseItem(index: Int) {
        val currentList = purchaseItemsDraft.value
        if (index in currentList.indices) {
            val item = currentList[index]
            purchaseItemParticularInput.value = item.particular
            purchaseItemHsnInput.value = item.hsnSac
            purchaseItemHeightInput.value = if (item.height > 0) DimensionCalculator.formatDimension(item.height) else ""
            purchaseItemWidthInput.value = if (item.width > 0) DimensionCalculator.formatDimension(item.width) else ""
            purchaseItemQtyInput.value = DimensionCalculator.formatQtyOnly(item.qty)
            purchaseItemRateInput.value = if (item.rate > 0) String.format(java.util.Locale.US, "%.2f", item.rate) else ""
            purchaseItemUnitInput.value = item.unit.ifBlank { "Pcs" }
        }
    }

    fun startNewPurchase(presetSupplier: SupplierEntity? = null) {
        purchaseIdDraft.value = 0L
        purchaseInvoiceNoDraft.value = "PUR-" + System.currentTimeMillis().toString().takeLast(5)
        purchaseDateMillisDraft.value = System.currentTimeMillis()
        purchaseSupplierDraft.value = presetSupplier
        purchaseDimensionUnitDraft.value = "Inches"
        purchaseTaxRateDraft.value = 18.0
        purchaseIsGstIncludedDraft.value = false
        purchaseDiscountDraft.value = 0.0
        purchaseDiscountTypeDraft.value = "FLAT"
        purchaseDiscountPercentDraft.value = 0.0
        purchaseOtherChargesDraft.value = 0.0
        purchaseOtherChargesDescDraft.value = "Transportation"
        purchaseIsRoundOffAutoDraft.value = true
        purchaseRoundOffDraft.value = 0.0
        purchasePaidAmountDraft.value = 0.0
        purchaseNotesDraft.value = ""
        purchaseBillPhotoUriDraft.value = ""
        purchaseTransportNameDraft.value = ""
        purchaseVehicleNoDraft.value = ""
        purchaseLrBiltyNoDraft.value = ""
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
        purchaseDiscountTypeDraft.value = purchase.discountType
        purchaseDiscountPercentDraft.value = purchase.discountPercent
        purchaseOtherChargesDraft.value = purchase.otherCharges
        purchaseOtherChargesDescDraft.value = purchase.otherChargesDescription
        purchaseRoundOffDraft.value = purchase.roundOffAmount
        purchaseIsRoundOffAutoDraft.value = (purchase.roundOffAmount != 0.0)
        purchasePaidAmountDraft.value = purchase.paidAmount
        purchaseNotesDraft.value = purchase.notes
        purchaseBillPhotoUriDraft.value = purchase.billPhotoUri
        purchaseTransportNameDraft.value = purchase.transportName
        purchaseVehicleNoDraft.value = purchase.vehicleNo
        purchaseLrBiltyNoDraft.value = purchase.lrBiltyNo
        purchaseItemsDraft.value = purchaseWithItems.items
        resetPurchaseItemInputs()
    }

    fun addOrUpdateItemToPurchase(editingIndex: Int? = null) {
        val particular = purchaseItemParticularInput.value.trim()
        val height = purchaseItemHeightInput.value.toDoubleOrNull() ?: 0.0
        val width = purchaseItemWidthInput.value.toDoubleOrNull() ?: 0.0
        val qty = purchaseItemQtyInput.value.toDoubleOrNull() ?: 1.0
        val rate = purchaseItemRateInput.value.toDoubleOrNull() ?: 0.0
        val unit = purchaseItemUnitInput.value.trim().ifBlank { "Pcs" }

        if (particular.isBlank()) {
            showMessage("Please enter item description / material")
            return
        }
        if (qty <= 0.0) {
            showMessage("Quantity must be greater than 0")
            return
        }
        if (rate <= 0) {
            showMessage("Please enter valid Rate")
            return
        }

        val sqFt = if (height > 0 && width > 0) {
            DimensionCalculator.calculateSqFt(height, width, qty, purchaseDimensionUnitDraft.value)
        } else {
            qty
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
            amount = amount,
            unit = unit
        )

        if (editingIndex != null && editingIndex in currentList.indices) {
            currentList[editingIndex] = newItem
            showMessage("Item updated in purchase bill")
        } else {
            currentList.add(newItem)
            showMessage("Item added to purchase bill")
        }
        purchaseItemsDraft.value = currentList.mapIndexed { idx, itm -> itm.copy(slNo = idx + 1) }
        recalculatePurchaseDiscount()
        resetPurchaseItemInputs()
    }

    fun removeItemFromPurchase(index: Int) {
        val currentList = purchaseItemsDraft.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            purchaseItemsDraft.value = currentList.mapIndexed { idx, itm -> itm.copy(slNo = idx + 1) }
            recalculatePurchaseDiscount()
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
                discountType = purchaseDiscountTypeDraft.value,
                discountPercent = purchaseDiscountPercentDraft.value,
                otherCharges = otherCharges,
                otherChargesDescription = otherChargesDesc,
                roundOffAmount = roundOff,
                grandTotal = grandTotal,
                paidAmount = purchasePaidAmountDraft.value,
                notes = purchaseNotesDraft.value.trim(),
                billPhotoUri = purchaseBillPhotoUriDraft.value,
                transportName = purchaseTransportNameDraft.value.trim(),
                vehicleNo = purchaseVehicleNoDraft.value.trim(),
                lrBiltyNo = purchaseLrBiltyNoDraft.value.trim()
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

    // Purchase Returns / Debit Notes
    fun getNextDebitNoteNo(): String {
        val count = allPurchaseReturns.value.size + 1
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        return "DN-$year-${String.format(java.util.Locale.US, "%03d", count)}"
    }

    fun savePurchaseReturn(
        returnEntity: PurchaseReturnEntity,
        items: List<PurchaseReturnItemEntity>,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val savedId = repository.savePurchaseReturn(returnEntity, items)
            showMessage("Debit Note #${returnEntity.returnNo} recorded! Supplier balance updated.")
            onSaved()
        }
    }

    fun deletePurchaseReturn(returnId: Long) {
        viewModelScope.launch {
            repository.deletePurchaseReturn(returnId)
            showMessage("Debit note deleted")
        }
    }

    // Raw Material Catalog Management
    fun selectRawMaterialToItem(material: RawMaterialCatalogEntity) {
        purchaseItemParticularInput.value = material.name
        purchaseItemHsnInput.value = material.hsnSac
        purchaseItemUnitInput.value = material.defaultUnit.ifBlank { "Pcs" }
        if (material.defaultRate > 0) {
            purchaseItemRateInput.value = String.format(java.util.Locale.US, "%.2f", material.defaultRate)
        }
        showMessage("Selected '${material.name}' (${material.defaultUnit})")
    }

    fun saveRawMaterial(material: RawMaterialCatalogEntity) {
        viewModelScope.launch {
            repository.saveRawMaterial(material)
            showMessage("Material '${material.name}' saved to catalog")
        }
    }

    fun deleteRawMaterial(material: RawMaterialCatalogEntity) {
        viewModelScope.launch {
            repository.deleteRawMaterial(material)
            showMessage("Material '${material.name}' removed from catalog")
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

    fun updateSupplierPayment(
        paymentId: Long,
        supplierId: Long,
        supplierName: String,
        amount: Double,
        mode: String,
        reference: String,
        notes: String,
        dateMillis: Long,
        onSuccess: () -> Unit = {}
    ) {
        if (amount <= 0) {
            showMessage("Please enter a valid payment amount")
            return
        }
        viewModelScope.launch {
            repository.updatePurchasePayment(
                PurchasePaymentEntity(
                    id = paymentId,
                    supplierId = supplierId,
                    supplierName = supplierName,
                    amount = amount,
                    dateMillis = dateMillis,
                    paymentMode = mode,
                    referenceNo = reference,
                    notes = notes
                )
            )
            showMessage("Supplier payment entry updated successfully!")
            onSuccess()
        }
    }

    fun deleteSupplierPayment(paymentId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deletePurchasePayment(paymentId)
            showMessage("Payment entry deleted")
            onSuccess()
        }
    }

    // -------------------------------------------------------------
    // BACKUP & RESTORE METHODS
    // -------------------------------------------------------------
    fun refreshGoogleAccount() {
        val account = googleDriveManager.getSignedInAccount()
        _googleAccount.value = account
        if (account != null) {
            checkDriveBackup()
        }
    }

    fun onGoogleSignInResult(data: Intent?) {
        val account = googleDriveManager.handleSignInResult(data)
        _googleAccount.value = account
        if (account != null) {
            showMessage("Google Drive connected: ${account.email}")
            checkDriveBackup()
        } else {
            showMessage("Google Sign-In was cancelled")
        }
    }

    fun signOutGoogleDrive() {
        viewModelScope.launch {
            googleDriveManager.signOut()
            _googleAccount.value = null
            _driveBackupInfo.value = null
            showMessage("Disconnected from Google Drive")
        }
    }

    fun checkDriveBackup() {
        val account = _googleAccount.value ?: return
        viewModelScope.launch {
            val token = googleDriveManager.fetchOAuthToken(account)
            if (token != null) {
                val info = googleDriveManager.findBackupFile(token)
                _driveBackupInfo.value = info
            }
        }
    }

    fun backupToGoogleDrive(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val account = _googleAccount.value
        if (account == null) {
            onResult(false, "Please connect Google Drive account first")
            return
        }
        _isBackupOperating.value = true
        viewModelScope.launch {
            try {
                val token = googleDriveManager.fetchOAuthToken(account)
                if (token == null) {
                    _isBackupOperating.value = false
                    val msg = "Google authorization needed. Please reconnect your account."
                    showMessage(msg)
                    onResult(false, msg)
                    return@launch
                }

                val backupData = repository.exportAllData()
                val jsonStr = backupData.toJsonString()
                val result = googleDriveManager.uploadBackup(token, jsonStr)

                _isBackupOperating.value = false
                result.fold(
                    onSuccess = { info ->
                        _driveBackupInfo.value = info
                        val now = System.currentTimeMillis()
                        _lastBackupTime.value = now
                        _lastBackupType.value = "Google Drive"
                        prefs.edit()
                            .putLong("last_backup_time", now)
                            .putString("last_backup_type", "Google Drive")
                            .apply()
                        val msg = "Backup saved to Google Drive! (${backupData.bills.size} bills, ${backupData.customers.size} customers, ${backupData.purchases.size} purchases)"
                        showMessage(msg)
                        onResult(true, msg)
                    },
                    onFailure = { err ->
                        val msg = "Drive upload failed: ${err.localizedMessage ?: "Unknown error"}"
                        showMessage(msg)
                        onResult(false, msg)
                    }
                )
            } catch (e: Exception) {
                _isBackupOperating.value = false
                val msg = "Backup error: ${e.localizedMessage}"
                showMessage(msg)
                onResult(false, msg)
            }
        }
    }

    fun restoreFromGoogleDrive(clearExisting: Boolean, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val account = _googleAccount.value
        val driveInfo = _driveBackupInfo.value
        if (account == null || driveInfo == null) {
            onResult(false, "No backup file found in Google Drive")
            return
        }
        _isBackupOperating.value = true
        viewModelScope.launch {
            try {
                val token = googleDriveManager.fetchOAuthToken(account)
                if (token == null) {
                    _isBackupOperating.value = false
                    val msg = "Authorization failed. Please reconnect Google Drive."
                    showMessage(msg)
                    onResult(false, msg)
                    return@launch
                }

                val downloadResult = googleDriveManager.downloadBackup(token, driveInfo.id)
                downloadResult.fold(
                    onSuccess = { jsonContent ->
                        restoreFromJsonString(jsonContent, clearExisting, onResult)
                    },
                    onFailure = { err ->
                        _isBackupOperating.value = false
                        val msg = "Download failed: ${err.localizedMessage}"
                        showMessage(msg)
                        onResult(false, msg)
                    }
                )
            } catch (e: Exception) {
                _isBackupOperating.value = false
                val msg = "Restore failed: ${e.localizedMessage}"
                showMessage(msg)
                onResult(false, msg)
            }
        }
    }

    suspend fun getExportBackupJson(): String = withContext(Dispatchers.IO) {
        repository.exportAllData().toJsonString()
    }

    fun restoreFromJsonString(jsonString: String, clearExisting: Boolean, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        _isBackupOperating.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backupData = AppBackupData.fromJsonString(jsonString)
                repository.restoreAllData(database, backupData, clearExisting)
                val now = System.currentTimeMillis()
                _lastBackupTime.value = now
                _lastBackupType.value = "Restored Data"
                prefs.edit()
                    .putLong("last_backup_time", now)
                    .putString("last_backup_type", "Restored Data")
                    .apply()
                _isBackupOperating.value = false
                val msg = "Restore Complete! ${backupData.bills.size} Bills, ${backupData.customers.size} Customers, ${backupData.purchases.size} Purchases restored."
                showMessage(msg)
                withContext(Dispatchers.Main) {
                    onResult(true, msg)
                }
            } catch (e: Exception) {
                _isBackupOperating.value = false
                val msg = "Error restoring data: ${e.localizedMessage ?: "Invalid backup file"}"
                showMessage(msg)
                withContext(Dispatchers.Main) {
                    onResult(false, msg)
                }
            }
        }
    }
}
