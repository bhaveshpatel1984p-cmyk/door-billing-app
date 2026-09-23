package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CustomerEntity::class,
        BillEntity::class,
        BillItemEntity::class,
        PaymentEntity::class,
        CompanyProfileEntity::class,
        SupplierEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        PurchasePaymentEntity::class,
        DoorPresetEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class DoorDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun billDao(): BillDao
    abstract fun paymentDao(): PaymentDao
    abstract fun companyProfileDao(): CompanyProfileDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun purchasePaymentDao(): PurchasePaymentDao
    abstract fun doorPresetDao(): DoorPresetDao

    companion object {
        @Volatile
        private var INSTANCE: DoorDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bills ADD COLUMN otherCharges REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE bills ADD COLUMN otherChargesDescription TEXT NOT NULL DEFAULT 'Cutting Charges'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bills ADD COLUMN roundOffAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS suppliers (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "mobile TEXT NOT NULL, " +
                            "address TEXT NOT NULL, " +
                            "gstNo TEXT NOT NULL, " +
                            "createdAt INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS purchases (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "invoiceNo TEXT NOT NULL, " +
                            "supplierId INTEGER NOT NULL, " +
                            "supplierName TEXT NOT NULL, " +
                            "supplierMobile TEXT NOT NULL, " +
                            "supplierAddress TEXT NOT NULL, " +
                            "supplierGstNo TEXT NOT NULL, " +
                            "dateMillis INTEGER NOT NULL, " +
                            "dimensionUnit TEXT NOT NULL, " +
                            "taxRate REAL NOT NULL, " +
                            "isGstIncluded INTEGER NOT NULL, " +
                            "subTotal REAL NOT NULL, " +
                            "cgstAmount REAL NOT NULL, " +
                            "sgstAmount REAL NOT NULL, " +
                            "igstAmount REAL NOT NULL, " +
                            "discountAmount REAL NOT NULL, " +
                            "otherCharges REAL NOT NULL, " +
                            "otherChargesDescription TEXT NOT NULL, " +
                            "roundOffAmount REAL NOT NULL, " +
                            "grandTotal REAL NOT NULL, " +
                            "paidAmount REAL NOT NULL, " +
                            "notes TEXT NOT NULL, " +
                            "createdAt INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchases_supplierId ON purchases(supplierId)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS purchase_items (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "purchaseId INTEGER NOT NULL, " +
                            "slNo INTEGER NOT NULL, " +
                            "particular TEXT NOT NULL, " +
                            "hsnSac TEXT NOT NULL, " +
                            "height REAL NOT NULL, " +
                            "width REAL NOT NULL, " +
                            "qty INTEGER NOT NULL, " +
                            "sqFt REAL NOT NULL, " +
                            "rate REAL NOT NULL, " +
                            "amount REAL NOT NULL, " +
                            "FOREIGN KEY(purchaseId) REFERENCES purchases(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_items_purchaseId ON purchase_items(purchaseId)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS purchase_payments (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "supplierId INTEGER NOT NULL, " +
                            "supplierName TEXT NOT NULL, " +
                            "purchaseId INTEGER, " +
                            "amount REAL NOT NULL, " +
                            "dateMillis INTEGER NOT NULL, " +
                            "paymentMode TEXT NOT NULL, " +
                            "referenceNo TEXT NOT NULL, " +
                            "notes TEXT NOT NULL, " +
                            "createdAt INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_payments_supplierId ON purchase_payments(supplierId)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE company_profile ADD COLUMN qrCodeUri TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE company_profile ADD COLUMN upiId TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bills ADD COLUMN previousBalance REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE bills ADD COLUMN netPayable REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN firmName TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bills ADD COLUMN isQuotation INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS door_presets (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "defaultRate REAL NOT NULL, " +
                            "defaultHsn TEXT NOT NULL, " +
                            "defaultHeight REAL NOT NULL, " +
                            "defaultWidth REAL NOT NULL, " +
                            "createdAt INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE company_profile ADD COLUMN addressLine2 TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE company_profile ADD COLUMN alternateMobile TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): DoorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DoorDatabase::class.java,
                    "door_billing_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initialize default company profile on first launch
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context)
                                database.companyProfileDao().insertOrUpdateCompanyProfile(
                                    CompanyProfileEntity()
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
