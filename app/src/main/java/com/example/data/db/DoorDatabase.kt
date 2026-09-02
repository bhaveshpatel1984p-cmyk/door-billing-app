package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
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

        fun getDatabase(context: Context): DoorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DoorDatabase::class.java,
                    "door_billing_database"
                )
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
