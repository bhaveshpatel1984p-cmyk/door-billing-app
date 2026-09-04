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
    private val purchasePaymentDao: PurchasePaymentDao
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

    // Payments
    fun getPaymentsForCustomer(customerId: Long): Flow<List<PaymentEntity>> =
        paymentDao.getPaymentsByCustomer(customerId)

    suspend fun addPayment(payment: PaymentEntity): Long {
        return paymentDao.insertPayment(payment)
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

    suspend fun deletePurchasePayment(paymentId: Long) =
        purchasePaymentDao.deletePaymentById(paymentId)

    // Supplier Balances & Ledger
    val supplierBalances: Flow<List<SupplierBalanceSummary>> =
        combine(
            supplierDao.getAllSuppliers(),
            purchaseDao.getAllPurchasesWithItems(),
            purchasePaymentDao.getAllPayments()
        ) { suppliers, purchases, payments ->
            suppliers.map { supplier ->
                val supplierPurchases = purchases.filter { it.purchase.supplierId == supplier.id }
                val supplierPayments = payments.filter { it.supplierId == supplier.id }
                val totalPurchased = supplierPurchases.sumOf { it.purchase.grandTotal }
                val totalPaid = supplierPayments.sumOf { it.amount }
                val balance = totalPurchased - totalPaid

                val lastBillDate = supplierPurchases.maxOfOrNull { it.purchase.dateMillis } ?: 0L
                val lastPayDate = supplierPayments.maxOfOrNull { it.dateMillis } ?: 0L
                val lastDate = maxOf(lastBillDate, lastPayDate)

                SupplierBalanceSummary(
                    supplier = supplier,
                    totalPurchased = totalPurchased,
                    totalPaid = totalPaid,
                    balance = balance,
                    billCount = supplierPurchases.size,
                    paymentCount = supplierPayments.size,
                    lastTransactionDate = lastDate
                )
            }.sortedByDescending { it.balance }
        }

    fun getSupplierLedger(supplierId: Long): Flow<List<SupplierLedgerEntry>> =
        combine(
            purchaseDao.getPurchasesBySupplier(supplierId),
            purchasePaymentDao.getPaymentsBySupplier(supplierId)
        ) { purchases, payments ->
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

            (purchaseEntries + paymentEntries).sortedBy { it.dateMillis }
        }
}
