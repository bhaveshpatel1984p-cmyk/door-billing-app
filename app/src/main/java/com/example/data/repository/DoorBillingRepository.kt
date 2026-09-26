package com.example.data.repository

import com.example.data.db.BillDao
import com.example.data.db.BillEntity
import com.example.data.db.BillItemEntity
import com.example.data.db.BillWithItems
import com.example.data.db.CompanyProfileDao
import com.example.data.db.CompanyProfileEntity
import com.example.data.db.CustomerBalanceSummary
import com.example.data.db.CustomerDao
import com.example.data.db.CustomerEntity
import com.example.data.db.LedgerEntry
import com.example.data.db.PaymentDao
import com.example.data.db.PaymentEntity
import com.example.data.db.PurchaseDao
import com.example.data.db.PurchaseEntity
import com.example.data.db.PurchaseItemEntity
import com.example.data.db.PurchasePaymentDao
import com.example.data.db.PurchasePaymentEntity
import com.example.data.db.PurchaseWithItems
import com.example.data.db.SupplierBalanceSummary
import com.example.data.db.SupplierDao
import com.example.data.db.SupplierEntity
import com.example.data.db.SupplierLedgerEntry
import com.example.data.db.DoorPresetDao
import com.example.data.db.DoorPresetEntity
import com.example.data.db.PurchaseReturnDao
import com.example.data.db.PurchaseReturnEntity
import com.example.data.db.PurchaseReturnItemEntity
import com.example.data.db.PurchaseReturnWithItems
import com.example.data.db.RawMaterialCatalogDao
import com.example.data.db.RawMaterialCatalogEntity
import com.example.data.backup.AppBackupData
import com.example.data.db.DoorDatabase
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DoorBillingRepository(
    private val customerDao: CustomerDao,
    private val billDao: BillDao,
    private val paymentDao: PaymentDao,
    private val companyProfileDao: CompanyProfileDao,
    private val supplierDao: SupplierDao,
    private val purchaseDao: PurchaseDao,
    private val purchasePaymentDao: PurchasePaymentDao,
    private val doorPresetDao: DoorPresetDao,
    private val purchaseReturnDao: PurchaseReturnDao,
    private val rawMaterialCatalogDao: RawMaterialCatalogDao
) {
    // Customers
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    fun getCustomerById(id: Long): Flow<CustomerEntity?> = customerDao.getCustomerByIdFlow(id)
    suspend fun getCustomerByIdDirect(id: Long): CustomerEntity? = customerDao.getCustomerById(id)

    suspend fun saveCustomer(customer: CustomerEntity): Long {
        return if (customer.id == 0L) {
            customerDao.insertCustomer(customer)
        } else {
            customerDao.updateCustomer(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(customer: CustomerEntity) {
        customerDao.deleteCustomer(customer)
    }

    // Bills
    val allBillsWithItems: Flow<List<BillWithItems>> = billDao.getAllBillsWithItems()

    fun getBillsForCustomer(customerId: Long): Flow<List<BillWithItems>> =
        billDao.getBillsByCustomer(customerId)

    suspend fun getBillById(billId: Long): BillWithItems? =
        billDao.getBillWithItemsById(billId)

    suspend fun saveBill(bill: BillEntity, items: List<BillItemEntity>): Long {
        return billDao.saveBillWithItems(bill, items)
    }

    suspend fun deleteBill(billId: Long) {
        billDao.deleteBillById(billId)
    }

    suspend fun generateNextInvoiceNumber(): String {
        val count = billDao.getTotalBillsCount()
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val seq = (count + 1).toString().padStart(4, '0')
        return "INV/$year/$seq"
    }

    suspend fun generateNextQuotationNumber(): String {
        val count = billDao.getTotalBillsCount()
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val seq = (count + 1).toString().padStart(4, '0')
        return "EST/$year/$seq"
    }

    suspend fun convertQuotationToInvoice(billId: Long): String {
        val newInvoiceNo = generateNextInvoiceNumber()
        billDao.convertQuotationToInvoice(billId, newInvoiceNo)
        return newInvoiceNo
    }

    // Door Presets
    val allDoorPresets: Flow<List<DoorPresetEntity>> = doorPresetDao.getAllPresets()

    suspend fun getPresetsCount(): Int = doorPresetDao.getPresetsCount()

    suspend fun saveDoorPreset(preset: DoorPresetEntity): Long {
        return if (preset.id == 0L) {
            doorPresetDao.insertPreset(preset)
        } else {
            doorPresetDao.updatePreset(preset)
            preset.id
        }
    }

    suspend fun insertDefaultPresets(presets: List<DoorPresetEntity>) {
        doorPresetDao.insertPresets(presets)
    }

    suspend fun deleteDoorPreset(preset: DoorPresetEntity) {
        doorPresetDao.deletePreset(preset)
    }

    // Payments
    fun getPaymentsForCustomer(customerId: Long): Flow<List<PaymentEntity>> =
        paymentDao.getPaymentsByCustomer(customerId)

    suspend fun addPayment(payment: PaymentEntity): Long {
        return paymentDao.insertPayment(payment)
    }

    suspend fun updatePayment(payment: PaymentEntity) {
        paymentDao.updatePayment(payment)
    }

    suspend fun deletePayment(paymentId: Long) {
        paymentDao.deletePaymentById(paymentId)
    }

    // Company Profile
    val companyProfile: Flow<CompanyProfileEntity?> = companyProfileDao.getCompanyProfile()

    suspend fun getCompanyProfileSync(): CompanyProfileEntity {
        return companyProfileDao.getCompanyProfileSync() ?: CompanyProfileEntity()
    }

    suspend fun updateCompanyProfile(profile: CompanyProfileEntity) {
        companyProfileDao.insertOrUpdateCompanyProfile(profile)
    }

    // Customer Balances Summary (Combines customers, bills, payments)
    val customerBalancesSummary: Flow<List<CustomerBalanceSummary>> =
        combine(
            customerDao.getAllCustomers(),
            billDao.getAllBillsWithItems(),
            paymentDao.getAllPayments()
        ) { customers, bills, payments ->
            customers.map { customer ->
                val customerBills = bills.filter { it.bill.customerId == customer.id }
                val customerPayments = payments.filter { it.customerId == customer.id }

                val totalBilled = customerBills.sumOf { it.bill.grandTotal }
                // Direct payments plus any initial paid amounts recorded in bills
                val totalDirectPayments = customerPayments.sumOf { it.amount }
                val totalPaid = totalDirectPayments
                val balance = totalBilled - totalPaid

                val latestBillDate = customerBills.maxOfOrNull { it.bill.dateMillis } ?: 0L
                val latestPayDate = customerPayments.maxOfOrNull { it.dateMillis } ?: 0L
                val lastTransactionDate = maxOf(customer.createdAt, latestBillDate, latestPayDate)

                CustomerBalanceSummary(
                    customer = customer,
                    totalBilled = totalBilled,
                    totalPaid = totalPaid,
                    balance = balance,
                    billCount = customerBills.size,
                    paymentCount = customerPayments.size,
                    lastTransactionDate = lastTransactionDate
                )
            }.sortedByDescending { it.balance }
        }

    suspend fun getCustomerDueBalance(customerId: Long, excludeBillId: Long = 0L): Double {
        val totalBilled = if (excludeBillId > 0L) {
            billDao.getCustomerTotalBilledExcluding(customerId, excludeBillId)
        } else {
            billDao.getCustomerTotalBilled(customerId)
        }
        val totalPaid = if (excludeBillId > 0L) {
            paymentDao.getCustomerTotalPaidExcluding(customerId, excludeBillId)
        } else {
            paymentDao.getCustomerTotalPaid(customerId)
        }
        return maxOf(0.0, totalBilled - totalPaid)
    }

    // Combined Ledger for a customer
    fun getCustomerLedger(customerId: Long): Flow<List<LedgerEntry>> =
        combine(
            billDao.getBillsByCustomer(customerId),
            paymentDao.getPaymentsByCustomer(customerId)
        ) { bills, payments ->
            val billEntries: List<LedgerEntry> = bills.map { billWithItems ->
                val totalSqFt = billWithItems.items.sumOf { it.sqFt }
                LedgerEntry.BillEntry(
                    id = billWithItems.bill.id,
                    dateMillis = billWithItems.bill.dateMillis,
                    invoiceNo = billWithItems.bill.invoiceNo,
                    grandTotal = billWithItems.bill.grandTotal,
                    itemsCount = billWithItems.items.size,
                    totalSqFt = totalSqFt,
                    billWithItems = billWithItems
                )
            }

            val paymentEntries: List<LedgerEntry> = payments.map { payment ->
                LedgerEntry.PaymentRecord(
                    id = payment.id,
                    dateMillis = payment.dateMillis,
                    payment = payment
                )
            }

            (billEntries + paymentEntries).sortedBy { it.dateMillis }
        }

    // Suppliers
    val allSuppliers: Flow<List<SupplierEntity>> = supplierDao.getAllSuppliers()

    suspend fun getSuppliersCount(): Int = supplierDao.getSuppliersCount()

    suspend fun getSupplierById(id: Long): SupplierEntity? = supplierDao.getSupplierById(id)

    suspend fun saveSupplier(supplier: SupplierEntity): Long {
        return if (supplier.id == 0L) {
            supplierDao.insertSupplier(supplier)
        } else {
            supplierDao.updateSupplier(supplier)
            supplier.id
        }
    }

    suspend fun deleteSupplier(supplier: SupplierEntity) {
        supplierDao.deleteSupplier(supplier)
    }

    // Purchases
    val allPurchasesWithItems: Flow<List<PurchaseWithItems>> = purchaseDao.getAllPurchasesWithItems()

    fun getPurchasesForSupplier(supplierId: Long): Flow<List<PurchaseWithItems>> =
        purchaseDao.getPurchasesBySupplier(supplierId)

    suspend fun getPurchaseById(purchaseId: Long): PurchaseWithItems? =
        purchaseDao.getPurchaseWithItemsById(purchaseId)

    suspend fun savePurchase(purchase: PurchaseEntity, items: List<PurchaseItemEntity>): Long =
        purchaseDao.savePurchaseWithItems(purchase, items)

    suspend fun deletePurchase(purchaseId: Long) {
        purchaseDao.deletePurchaseById(purchaseId)
    }

    // Purchase Payments
    val allPurchasePayments: Flow<List<PurchasePaymentEntity>> = purchasePaymentDao.getAllPayments()

    fun getPurchasePaymentsForSupplier(supplierId: Long): Flow<List<PurchasePaymentEntity>> =
        purchasePaymentDao.getPaymentsBySupplier(supplierId)

    suspend fun recordPurchasePayment(payment: PurchasePaymentEntity): Long =
        purchasePaymentDao.insertPayment(payment)

    suspend fun updatePurchasePayment(payment: PurchasePaymentEntity) {
        purchasePaymentDao.updatePayment(payment)
    }

    suspend fun deletePurchasePayment(paymentId: Long) =
        purchasePaymentDao.deletePaymentById(paymentId)

    // Purchase Returns / Debit Notes
    val allPurchaseReturns: Flow<List<PurchaseReturnWithItems>> = purchaseReturnDao.getAllReturnsWithItems()

    fun getReturnsForSupplier(supplierId: Long): Flow<List<PurchaseReturnWithItems>> =
        purchaseReturnDao.getReturnsBySupplier(supplierId)

    suspend fun savePurchaseReturn(returnEntity: PurchaseReturnEntity, items: List<PurchaseReturnItemEntity>): Long =
        purchaseReturnDao.saveReturnWithItems(returnEntity, items)

    suspend fun deletePurchaseReturn(returnId: Long) =
        purchaseReturnDao.deleteReturnById(returnId)

    // Raw Material Catalog
    val allRawMaterials: Flow<List<RawMaterialCatalogEntity>> = rawMaterialCatalogDao.getAllMaterials()

    suspend fun saveRawMaterial(material: RawMaterialCatalogEntity): Long =
        if (material.id == 0L) rawMaterialCatalogDao.insertMaterial(material) else {
            rawMaterialCatalogDao.updateMaterial(material)
            material.id
        }

    suspend fun deleteRawMaterial(material: RawMaterialCatalogEntity) =
        rawMaterialCatalogDao.deleteMaterial(material)

    suspend fun initDefaultRawMaterialsIfEmpty() {
        if (rawMaterialCatalogDao.getMaterialsCount() == 0) {
            val defaults = listOf(
                RawMaterialCatalogEntity(name = "Flush Door 30mm", defaultUnit = "Pcs", defaultRate = 1850.0, hsnSac = "4418", category = "Doors"),
                RawMaterialCatalogEntity(name = "Flush Door 35mm", defaultUnit = "Pcs", defaultRate = 2200.0, hsnSac = "4418", category = "Doors"),
                RawMaterialCatalogEntity(name = "Lamination Door", defaultUnit = "Pcs", defaultRate = 2800.0, hsnSac = "4418", category = "Doors"),
                RawMaterialCatalogEntity(name = "Teak Wood Door", defaultUnit = "Pcs", defaultRate = 6500.0, hsnSac = "4418", category = "Doors"),
                RawMaterialCatalogEntity(name = "Pine Wood Door", defaultUnit = "Pcs", defaultRate = 3400.0, hsnSac = "4418", category = "Doors"),
                RawMaterialCatalogEntity(name = "Plywood 18mm (8x4)", defaultUnit = "Pcs", defaultRate = 1950.0, hsnSac = "4412", category = "Plywood"),
                RawMaterialCatalogEntity(name = "Door Skin Sheet", defaultUnit = "Pcs", defaultRate = 450.0, hsnSac = "4418", category = "Sheets"),
                RawMaterialCatalogEntity(name = "Fevicol Marine Adhesive", defaultUnit = "Kg", defaultRate = 220.0, hsnSac = "3506", category = "Adhesive"),
                RawMaterialCatalogEntity(name = "Wood Polish / Varnish", defaultUnit = "Ltr", defaultRate = 380.0, hsnSac = "3208", category = "Finishing"),
                RawMaterialCatalogEntity(name = "SS Hinges 4x12", defaultUnit = "Pair", defaultRate = 85.0, hsnSac = "8302", category = "Hardware"),
                RawMaterialCatalogEntity(name = "Tower Bolts 8\"", defaultUnit = "Pcs", defaultRate = 95.0, hsnSac = "8302", category = "Hardware"),
                RawMaterialCatalogEntity(name = "Mortise Lock & Handle Set", defaultUnit = "Set", defaultRate = 850.0, hsnSac = "8301", category = "Hardware"),
                RawMaterialCatalogEntity(name = "PVC Edge Banding Roll (50m)", defaultUnit = "Roll", defaultRate = 320.0, hsnSac = "3920", category = "Hardware"),
                RawMaterialCatalogEntity(name = "Timber Planks", defaultUnit = "Sq.Ft", defaultRate = 110.0, hsnSac = "4407", category = "Timber")
            )
            rawMaterialCatalogDao.insertMaterials(defaults)
        }
    }

    // Supplier Balances & Ledger
    val supplierBalances: Flow<List<SupplierBalanceSummary>> =
        combine(
            supplierDao.getAllSuppliers(),
            purchaseDao.getAllPurchasesWithItems(),
            purchasePaymentDao.getAllPayments(),
            purchaseReturnDao.getAllReturnsWithItems()
        ) { suppliers, purchases, payments, returns ->
            suppliers.map { supplier ->
                val supplierPurchases = purchases.filter { it.purchase.supplierId == supplier.id }
                val supplierPayments = payments.filter { it.supplierId == supplier.id }
                val supplierReturns = returns.filter { it.returnNote.supplierId == supplier.id }
                val totalPurchased = supplierPurchases.sumOf { it.purchase.grandTotal }
                val totalPaid = supplierPayments.sumOf { it.amount }
                val totalReturned = supplierReturns.sumOf { it.returnNote.totalAmount }
                val balance = totalPurchased - totalPaid - totalReturned

                val lastBillDate = supplierPurchases.maxOfOrNull { it.purchase.dateMillis } ?: 0L
                val lastPayDate = supplierPayments.maxOfOrNull { it.dateMillis } ?: 0L
                val lastReturnDate = supplierReturns.maxOfOrNull { it.returnNote.dateMillis } ?: 0L
                val lastDate = maxOf(lastBillDate, maxOf(lastPayDate, lastReturnDate))

                SupplierBalanceSummary(
                    supplier = supplier,
                    totalPurchased = totalPurchased,
                    totalPaid = totalPaid,
                    totalReturned = totalReturned,
                    balance = balance,
                    billCount = supplierPurchases.size,
                    paymentCount = supplierPayments.size,
                    returnCount = supplierReturns.size,
                    lastTransactionDate = lastDate
                )
            }.sortedByDescending { it.balance }
        }

    fun getSupplierLedger(supplierId: Long): Flow<List<SupplierLedgerEntry>> =
        combine(
            purchaseDao.getPurchasesBySupplier(supplierId),
            purchasePaymentDao.getPaymentsBySupplier(supplierId),
            purchaseReturnDao.getReturnsBySupplier(supplierId)
        ) { purchases, payments, returns ->
            val purchaseEntries: List<SupplierLedgerEntry> = purchases.map { purchaseWithItems ->
                SupplierLedgerEntry.PurchaseBillEntry(
                    id = purchaseWithItems.purchase.id,
                    dateMillis = purchaseWithItems.purchase.dateMillis,
                    invoiceNo = purchaseWithItems.purchase.invoiceNo,
                    grandTotal = purchaseWithItems.purchase.grandTotal,
                    itemsCount = purchaseWithItems.items.size,
                    purchaseWithItems = purchaseWithItems
                )
            }

            val paymentEntries: List<SupplierLedgerEntry> = payments.map { payment ->
                SupplierLedgerEntry.PaymentRecord(
                    id = payment.id,
                    dateMillis = payment.dateMillis,
                    payment = payment
                )
            }

            val returnEntries: List<SupplierLedgerEntry> = returns.map { returnWithItems ->
                SupplierLedgerEntry.DebitNoteEntry(
                    id = returnWithItems.returnNote.id,
                    dateMillis = returnWithItems.returnNote.dateMillis,
                    returnWithItems = returnWithItems
                )
            }

            (purchaseEntries + paymentEntries + returnEntries).sortedBy { it.dateMillis }
        }

    suspend fun exportAllData(): AppBackupData {
        val profile = companyProfileDao.getCompanyProfileSync()
        val customers = customerDao.getAllCustomersDirect()
        val bills = billDao.getAllBillsDirect()
        val billItems = billDao.getAllBillItemsDirect()
        val payments = paymentDao.getAllPaymentsDirect()
        val suppliers = supplierDao.getAllSuppliersDirect()
        val purchases = purchaseDao.getAllPurchasesDirect()
        val purchaseItems = purchaseDao.getAllPurchaseItemsDirect()
        val purchasePayments = purchasePaymentDao.getAllPurchasePaymentsDirect()

        return AppBackupData(
            companyProfile = profile,
            customers = customers,
            bills = bills,
            billItems = billItems,
            payments = payments,
            suppliers = suppliers,
            purchases = purchases,
            purchaseItems = purchaseItems,
            purchasePayments = purchasePayments
        )
    }

    suspend fun restoreAllData(database: DoorDatabase, backupData: AppBackupData, clearExisting: Boolean) {
        database.withTransaction {
            if (clearExisting) {
                billDao.deleteAllBillItems()
                billDao.deleteAllBills()
                paymentDao.deleteAll()
                purchaseDao.deleteAllPurchaseItems()
                purchaseDao.deleteAllPurchases()
                purchasePaymentDao.deleteAll()
                customerDao.deleteAll()
                supplierDao.deleteAll()
            }

            backupData.companyProfile?.let { cp ->
                companyProfileDao.insertOrUpdateCompanyProfile(cp)
            }

            if (backupData.customers.isNotEmpty()) {
                customerDao.insertAll(backupData.customers)
            }

            if (backupData.suppliers.isNotEmpty()) {
                supplierDao.insertAll(backupData.suppliers)
            }

            if (backupData.bills.isNotEmpty()) {
                billDao.insertAllBills(backupData.bills)
            }

            if (backupData.billItems.isNotEmpty()) {
                billDao.insertAllBillItems(backupData.billItems)
            }

            if (backupData.payments.isNotEmpty()) {
                paymentDao.insertAll(backupData.payments)
            }

            if (backupData.purchases.isNotEmpty()) {
                purchaseDao.insertAllPurchases(backupData.purchases)
            }

            if (backupData.purchaseItems.isNotEmpty()) {
                purchaseDao.insertAllPurchaseItems(backupData.purchaseItems)
            }

            if (backupData.purchasePayments.isNotEmpty()) {
                purchasePaymentDao.insertAll(backupData.purchasePayments)
            }
        }
    }
}
