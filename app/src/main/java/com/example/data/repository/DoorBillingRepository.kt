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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DoorBillingRepository(
    private val customerDao: CustomerDao,
    private val billDao: BillDao,
    private val paymentDao: PaymentDao,
    private val companyProfileDao: CompanyProfileDao
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
}
