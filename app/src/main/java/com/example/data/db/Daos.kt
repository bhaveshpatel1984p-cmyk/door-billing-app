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
    @Query("SELECT * FROM customers ORDER BY CASE WHEN firmName != '' THEN firmName ELSE name END ASC")
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

    @Query("SELECT * FROM customers WHERE firmName LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' OR mobile LIKE '%' || :query || '%'")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersDirect(): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
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

    @Query("UPDATE bills SET isQuotation = 0, invoiceNo = :newInvoiceNo WHERE id = :billId")
    suspend fun convertQuotationToInvoice(billId: Long, newInvoiceNo: String)

    @Query("SELECT COALESCE(SUM(grandTotal), 0.0) FROM bills WHERE customerId = :customerId")
    suspend fun getCustomerTotalBilled(customerId: Long): Double

    @Query("SELECT COALESCE(SUM(grandTotal), 0.0) FROM bills WHERE customerId = :customerId AND id != :excludeBillId")
    suspend fun getCustomerTotalBilledExcluding(customerId: Long, excludeBillId: Long): Double

    @Query("SELECT MAX(id) FROM bills")
    suspend fun getMaxBillId(): Long?

    @Query("SELECT * FROM bills")
    suspend fun getAllBillsDirect(): List<BillEntity>

    @Query("SELECT * FROM bills WHERE customerId = :customerId ORDER BY dateMillis ASC, id ASC")
    suspend fun getBillsByCustomerDirect(customerId: Long): List<BillEntity>

    @Query("SELECT * FROM bills WHERE customerId = :customerId AND id != :excludeBillId ORDER BY dateMillis ASC, id ASC")
    suspend fun getBillsByCustomerDirectExcluding(customerId: Long, excludeBillId: Long): List<BillEntity>

    @Query("SELECT * FROM bill_items")
    suspend fun getAllBillItemsDirect(): List<BillItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBills(bills: List<BillEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBillItems(items: List<BillItemEntity>)

    @Query("DELETE FROM bills")
    suspend fun deleteAllBills()

    @Query("DELETE FROM bill_items")
    suspend fun deleteAllBillItems()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY dateMillis DESC, id DESC")
    fun getPaymentsByCustomer(customerId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY dateMillis DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments")
    suspend fun getAllPaymentsDirect(): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<PaymentEntity>)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE id = :paymentId")
    suspend fun deletePaymentById(paymentId: Long)

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE customerId = :customerId")
    suspend fun getCustomerTotalPaid(customerId: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE customerId = :customerId AND (billId IS NULL OR billId != :excludeBillId)")
    suspend fun getCustomerTotalPaidExcluding(customerId: Long, excludeBillId: Long): Double

    @Query("DELETE FROM payments")
    suspend fun deleteAll()
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

    @Query("SELECT * FROM suppliers")
    suspend fun getAllSuppliersDirect(): List<SupplierEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(suppliers: List<SupplierEntity>)

    @Query("DELETE FROM suppliers")
    suspend fun deleteAll()
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

    @Query("SELECT * FROM purchases")
    suspend fun getAllPurchasesDirect(): List<PurchaseEntity>

    @Query("SELECT * FROM purchase_items")
    suspend fun getAllPurchaseItemsDirect(): List<PurchaseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPurchases(purchases: List<PurchaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPurchaseItems(items: List<PurchaseItemEntity>)

    @Query("DELETE FROM purchases")
    suspend fun deleteAllPurchases()

    @Query("DELETE FROM purchase_items")
    suspend fun deleteAllPurchaseItems()
}

@Dao
interface PurchasePaymentDao {
    @Query("SELECT * FROM purchase_payments WHERE supplierId = :supplierId ORDER BY dateMillis DESC, id DESC")
    fun getPaymentsBySupplier(supplierId: Long): Flow<List<PurchasePaymentEntity>>

    @Query("SELECT * FROM purchase_payments ORDER BY dateMillis DESC")
    fun getAllPayments(): Flow<List<PurchasePaymentEntity>>

    @Query("SELECT * FROM purchase_payments")
    suspend fun getAllPurchasePaymentsDirect(): List<PurchasePaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PurchasePaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<PurchasePaymentEntity>)

    @Update
    suspend fun updatePayment(payment: PurchasePaymentEntity)

    @Delete
    suspend fun deletePayment(payment: PurchasePaymentEntity)

    @Query("DELETE FROM purchase_payments WHERE id = :paymentId")
    suspend fun deletePaymentById(paymentId: Long)

    @Query("DELETE FROM purchase_payments")
    suspend fun deleteAll()
}

@Dao
interface DoorPresetDao {
    @Query("SELECT * FROM door_presets ORDER BY name ASC")
    fun getAllPresets(): Flow<List<DoorPresetEntity>>

    @Query("SELECT COUNT(*) FROM door_presets")
    suspend fun getPresetsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: DoorPresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<DoorPresetEntity>)

    @Update
    suspend fun updatePreset(preset: DoorPresetEntity)

    @Delete
    suspend fun deletePreset(preset: DoorPresetEntity)
}

@Dao
interface PurchaseReturnDao {
    @Transaction
    @Query("SELECT * FROM purchase_returns ORDER BY dateMillis DESC, id DESC")
    fun getAllReturnsWithItems(): Flow<List<PurchaseReturnWithItems>>

    @Transaction
    @Query("SELECT * FROM purchase_returns WHERE supplierId = :supplierId ORDER BY dateMillis DESC, id DESC")
    fun getReturnsBySupplier(supplierId: Long): Flow<List<PurchaseReturnWithItems>>

    @Transaction
    @Query("SELECT * FROM purchase_returns WHERE id = :returnId")
    suspend fun getReturnWithItemsById(returnId: Long): PurchaseReturnWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(returnEntity: PurchaseReturnEntity): Long

    @Update
    suspend fun updateReturn(returnEntity: PurchaseReturnEntity)

    @Query("DELETE FROM purchase_returns WHERE id = :returnId")
    suspend fun deleteReturnById(returnId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnItems(items: List<PurchaseReturnItemEntity>)

    @Query("DELETE FROM purchase_return_items WHERE returnId = :returnId")
    suspend fun deleteItemsByReturnId(returnId: Long)

    @Transaction
    suspend fun saveReturnWithItems(returnEntity: PurchaseReturnEntity, items: List<PurchaseReturnItemEntity>): Long {
        val returnId = if (returnEntity.id == 0L) {
            insertReturn(returnEntity)
        } else {
            updateReturn(returnEntity)
            deleteItemsByReturnId(returnEntity.id)
            returnEntity.id
        }
        val itemsWithId = items.map { it.copy(returnId = returnId) }
        insertReturnItems(itemsWithId)
        return returnId
    }

    @Query("SELECT COUNT(*) FROM purchase_returns")
    suspend fun getTotalReturnsCount(): Int
}

@Dao
interface RawMaterialCatalogDao {
    @Query("SELECT * FROM raw_materials ORDER BY name ASC")
    fun getAllMaterials(): Flow<List<RawMaterialCatalogEntity>>

    @Query("SELECT COUNT(*) FROM raw_materials")
    suspend fun getMaterialsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: RawMaterialCatalogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(materials: List<RawMaterialCatalogEntity>)

    @Update
    suspend fun updateMaterial(material: RawMaterialCatalogEntity)

    @Delete
    suspend fun deleteMaterial(material: RawMaterialCatalogEntity)
}

