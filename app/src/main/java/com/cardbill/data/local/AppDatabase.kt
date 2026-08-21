package com.cardbill.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cardbill.data.local.dao.BillDao
import com.cardbill.data.local.dao.CreditCardDao
import com.cardbill.data.local.dao.ActivityLogDao
import com.cardbill.data.local.dao.SmsRecordDao
import com.cardbill.data.local.entity.Bill
import com.cardbill.data.local.entity.CreditCard
import com.cardbill.data.local.entity.ActivityLog
import com.cardbill.data.local.entity.SmsRecord

@Database(
    entities = [CreditCard::class, Bill::class, ActivityLog::class, SmsRecord::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creditCardDao(): CreditCardDao
    abstract fun billDao(): BillDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun smsRecordDao(): SmsRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bills ADD COLUMN isSuperseded INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cardbill.db"
                )
                .addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build().also { INSTANCE = it }
            }
        }
    }
}
