package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerByIdFlow(id: Long): Flow<CustomerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR mobile LIKE '%' || :query || '%'")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>
}

@Dao
interface BillDao {
    @Transaction
    @Query("SELECT * FROM bills ORDER BY dateMillis DESC, id DESC")
    fun getAllBillsWithItems(): Flow<List<BillWithItems>>

    @Transaction
    @Query("SELECT * FROM bills WHERE customerId = :customerId ORDER BY dateMillis DESC, id DESC")
    fun getBillsByCustomer(customerId: Long): Flow<List<BillWithItems>>

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :billId")
    suspend fun getBillWithItemsById(billId: Long): BillWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Delete
    suspend fun deleteBill(bill: BillEntity)

    @Query("DELETE FROM bills WHERE id = :billId")
    suspend fun deleteBillById(billId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItems(items: List<BillItemEntity>)

    @Query("DELETE FROM bill_items WHERE billId = :billId")
    suspend fun deleteItemsByBillId(billId: Long)

    @Transaction
    suspend fun saveBillWithItems(bill: BillEntity, items: List<BillItemEntity>): Long {
        val billId = if (bill.id == 0L) {
            insertBill(bill)
        } else {
            updateBill(bill)
            deleteItemsByBillId(bill.id)
            bill.id
        }
        val itemsWithBillId = items.map { it.copy(billId = billId) }
        insertBillItems(itemsWithBillId)
        return billId
    }

    @Query("SELECT COUNT(*) FROM bills")
    suspend fun getTotalBillsCount(): Int

    @Query("SELECT MAX(id) FROM bills")
    suspend fun getMaxBillId(): Long?
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY dateMillis DESC, id DESC")
    fun getPaymentsByCustomer(customerId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY dateMillis DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE id = :paymentId")
    suspend fun deletePaymentById(paymentId: Long)
}

@Dao
interface CompanyProfileDao {
    @Query("SELECT * FROM company_profile WHERE id = 1")
    fun getCompanyProfile(): Flow<CompanyProfileEntity?>

    @Query("SELECT * FROM company_profile WHERE id = 1")
    suspend fun getCompanyProfileSync(): CompanyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCompanyProfile(profile: CompanyProfileEntity)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: Long): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Delete
    suspend fun deleteSupplier(supplier: SupplierEntity)

    @Query("SELECT * FROM suppliers WHERE name LIKE '%' || :query || '%' OR mobile LIKE '%' || :query || '%'")
    fun searchSuppliers(query: String): Flow<List<SupplierEntity>>

    @Query("SELECT COUNT(*) FROM suppliers")
    suspend fun getSuppliersCount(): Int
}

@Dao
interface PurchaseDao {
    @Transaction
    @Query("SELECT * FROM purchases ORDER BY dateMillis DESC, id DESC")
    fun getAllPurchasesWithItems(): Flow<List<PurchaseWithItems>>

    @Transaction
    @Query("SELECT * FROM purchases WHERE supplierId = :supplierId ORDER BY dateMillis DESC, id DESC")
    fun getPurchasesBySupplier(supplierId: Long): Flow<List<PurchaseWithItems>>

    @Transaction
    @Query("SELECT * FROM purchases WHERE id = :purchaseId")
    suspend fun getPurchaseWithItemsById(purchaseId: Long): PurchaseWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    @Update
    suspend fun updatePurchase(purchase: PurchaseEntity)

    @Query("DELETE FROM purchases WHERE id = :purchaseId")
    suspend fun deletePurchaseById(purchaseId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItemEntity>)

    @Query("DELETE FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun deleteItemsByPurchaseId(purchaseId: Long)

    @Transaction
    suspend fun savePurchaseWithItems(purchase: PurchaseEntity, items: List<PurchaseItemEntity>): Long {
        val purchaseId = if (purchase.id == 0L) {
            insertPurchase(purchase)
        } else {
            updatePurchase(purchase)
            deleteItemsByPurchaseId(purchase.id)
            purchase.id
        }
        val itemsWithId = items.map { it.copy(purchaseId = purchaseId) }
        insertPurchaseItems(itemsWithId)
        return purchaseId
    }

    @Query("SELECT COUNT(*) FROM purchases")
    suspend fun getTotalPurchasesCount(): Int
}

@Dao
interface PurchasePaymentDao {
    @Query("SELECT * FROM purchase_payments WHERE supplierId = :supplierId ORDER BY dateMillis DESC, id DESC")
    fun getPaymentsBySupplier(supplierId: Long): Flow<List<PurchasePaymentEntity>>

    @Query("SELECT * FROM purchase_payments ORDER BY dateMillis DESC")
    fun getAllPayments(): Flow<List<PurchasePaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PurchasePaymentEntity): Long

    @Delete
    suspend fun deletePayment(payment: PurchasePaymentEntity)

    @Query("DELETE FROM purchase_payments WHERE id = :paymentId")
    suspend fun deletePaymentById(paymentId: Long)
}

