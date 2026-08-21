package com.finsignal.di

import android.content.Context
import com.finsignal.data.local.AppDatabase
import com.finsignal.data.local.dao.ActivityLogDao
import com.finsignal.data.local.dao.BillDao
import com.finsignal.data.local.dao.CreditCardDao
import com.finsignal.data.local.dao.SmsRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideCreditCardDao(database: AppDatabase): CreditCardDao {
        return database.creditCardDao()
    }

    @Provides
    fun provideBillDao(database: AppDatabase): BillDao {
        return database.billDao()
    }

    @Provides
    fun provideActivityLogDao(database: AppDatabase): ActivityLogDao {
        return database.activityLogDao()
    }

    @Provides
    fun provideSmsRecordDao(database: AppDatabase): SmsRecordDao {
        return database.smsRecordDao()
    }
}
