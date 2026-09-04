package com.example.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mobile: String,
    val address: String,
    val gstNo: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bills",
    indices = [Index(value = ["customerId"]), Index(value = ["invoiceNo"], unique = true)]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNo: String,
    val customerId: Long,
    val customerName: String,
    val customerMobile: String,
    val customerAddress: String,
    val customerGstNo: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val dimensionUnit: String = "Inches", // "Inches" or "Feet"
    val taxRate: Double = 18.0, // Default 18% GST (9% CGST + 9% SGST)
    val isGstIncluded: Boolean = true,
    val subTotal: Double = 0.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val igstAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val otherCharges: Double = 0.0,
    val otherChargesDescription: String = "Cutting Charges",
    val roundOffAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paidAmount: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bill_items",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["billId"])]
)
data class BillItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billId: Long = 0,
    val slNo: Int,
    val particular: String, // Item / Door description (e.g. Flush Door 30mm, Teak Panel Door)
    val hsnSac: String = "4418", // Standard HSN code for Wooden Doors & Builders' Joinery
    val height: Double, // Height dimension (in inches or feet based on invoice unit)
    val width: Double, // Width dimension
    val qty: Int = 1,
    val sqFt: Double, // Calculated Sq.Ft = if Inches: (H * W / 144) * Qty, if Feet: (H * W) * Qty
    val rate: Double, // Rate per Sq.Ft
    val amount: Double // Amount = sqFt * rate
)

@Entity(
    tableName = "payments",
    indices = [Index(value = ["customerId"])]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val billId: Long? = null,
    val amount: Double,
    val dateMillis: Long = System.currentTimeMillis(),
    val paymentMode: String = "Cash", // "Cash", "UPI / GPay", "Bank Transfer", "Cheque"
    val referenceNo: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "company_profile")
data class CompanyProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val businessName: String = "NIRMAL DOOR",
    val address: String = "Plot No. 12, Industrial Area, Timber Market",
    val gstNo: String = "24AAAAA0000A1Z5",
    val mobile: String = "9876543210",
    val email: String = "doorbusiness@example.com",
    val pan: String = "AAAAA0000A",
    val state: String = "Gujarat",
    val stateCode: String = "24",
    val bankName: String = "State Bank of India",
    val accountNo: String = "12345678901234",
    val ifscCode: String = "SBIN0001234",
    val jurisdiction: String = "Kudachi",
    val declaration: String = "We declare that this invoice shows the actual price of the goods described and that all particulars are true and correct. Goods once sold will not be taken back.",
    val logoUri: String? = null // Stored local Uri or null to use default vector logo
)

// Data class with bill + items relation
data class BillWithItems(
    @Embedded val bill: BillEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "billId"
    )
    val items: List<BillItemEntity>
)

// Data class representing customer balance overview
data class CustomerBalanceSummary(
    val customer: CustomerEntity,
    val totalBilled: Double,
    val totalPaid: Double,
    val balance: Double, // totalBilled - totalPaid
    val billCount: Int,
    val paymentCount: Int,
    val lastTransactionDate: Long
)

// Ledger entry item for a customer
sealed class LedgerEntry {
    abstract val id: Long
    abstract val dateMillis: Long
    abstract val description: String
    abstract val debitAmount: Double // Bill amount (increases balance)
    abstract val creditAmount: Double // Payment amount (reduces balance)

    data class BillEntry(
        override val id: Long,
        override val dateMillis: Long,
        val invoiceNo: String,
        val grandTotal: Double,
        val itemsCount: Int,
        val totalSqFt: Double,
        val billWithItems: BillWithItems
    ) : LedgerEntry() {
        override val description: String = "Invoice #$invoiceNo ($itemsCount items, ${String.format(java.util.Locale.US, "%.2f", totalSqFt)} Sq.Ft)"
        override val debitAmount: Double = grandTotal
        override val creditAmount: Double = 0.0
    }

    data class PaymentRecord(
        override val id: Long,
        override val dateMillis: Long,
        val payment: PaymentEntity
    ) : LedgerEntry() {
        override val description: String = "Payment (${payment.paymentMode}${if (payment.referenceNo.isNotBlank()) " - Ref: " + payment.referenceNo else ""})"
        override val debitAmount: Double = 0.0
        override val creditAmount: Double = payment.amount
    }
}

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mobile: String = "",
    val address: String = "",
    val gstNo: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchases",
    indices = [Index(value = ["supplierId"])]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNo: String,
    val supplierId: Long,
    val supplierName: String,
    val supplierMobile: String = "",
    val supplierAddress: String = "",
    val supplierGstNo: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val dimensionUnit: String = "Inches",
    val taxRate: Double = 18.0,
    val isGstIncluded: Boolean = true,
    val subTotal: Double = 0.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val igstAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val otherCharges: Double = 0.0,
    val otherChargesDescription: String = "Transportation",
    val roundOffAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paidAmount: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["purchaseId"])]
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purchaseId: Long = 0,
    val slNo: Int,
    val particular: String,
    val hsnSac: String = "4418",
    val height: Double = 0.0,
    val width: Double = 0.0,
    val qty: Int = 1,
    val sqFt: Double = 0.0,
    val rate: Double = 0.0,
    val amount: Double = 0.0
)

@Entity(
    tableName = "purchase_payments",
    indices = [Index(value = ["supplierId"])]
)
data class PurchasePaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val supplierId: Long,
    val supplierName: String,
    val purchaseId: Long? = null,
    val amount: Double,
    val dateMillis: Long = System.currentTimeMillis(),
    val paymentMode: String = "Bank Transfer",
    val referenceNo: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class PurchaseWithItems(
    @Embedded val purchase: PurchaseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "purchaseId"
    )
    val items: List<PurchaseItemEntity>
)

data class SupplierBalanceSummary(
    val supplier: SupplierEntity,
    val totalPurchased: Double,
    val totalPaid: Double,
    val balance: Double, // totalPurchased - totalPaid
    val billCount: Int,
    val paymentCount: Int,
    val lastTransactionDate: Long
)

sealed class SupplierLedgerEntry {
    abstract val id: Long
    abstract val dateMillis: Long
    abstract val description: String
    abstract val debitAmount: Double // Payment made to supplier (reduces balance)
    abstract val creditAmount: Double // Purchase invoice (increases balance)

    data class PurchaseBillEntry(
        override val id: Long,
        override val dateMillis: Long,
        val invoiceNo: String,
        val grandTotal: Double,
        val itemsCount: Int,
        val purchaseWithItems: PurchaseWithItems
    ) : SupplierLedgerEntry() {
        override val description: String = "Purchase Bill #$invoiceNo ($itemsCount items)"
        override val debitAmount: Double = 0.0
        override val creditAmount: Double = grandTotal
    }

    data class PaymentRecord(
        override val id: Long,
        override val dateMillis: Long,
        val payment: PurchasePaymentEntity
    ) : SupplierLedgerEntry() {
        override val description: String = "Payment Made (${payment.paymentMode}${if (payment.referenceNo.isNotBlank()) " - Ref: " + payment.referenceNo else ""})"
        override val debitAmount: Double = payment.amount
        override val creditAmount: Double = 0.0
    }
}

