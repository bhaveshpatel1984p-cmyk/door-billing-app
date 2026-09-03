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
        CompanyProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DoorDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun billDao(): BillDao
    abstract fun paymentDao(): PaymentDao
    abstract fun companyProfileDao(): CompanyProfileDao

    companion object {
        @Volatile
        private var INSTANCE: DoorDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bills ADD COLUMN otherCharges REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE bills ADD COLUMN otherChargesDescription TEXT NOT NULL DEFAULT 'Cutting Charges'")
            }
        }

        fun getDatabase(context: Context): DoorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DoorDatabase::class.java,
                    "door_billing_database"
                )
                    .addMigrations(MIGRATION_1_2)
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
