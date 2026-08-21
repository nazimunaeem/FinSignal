package com.finsignal.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finsignal.data.local.entity.CreditCard
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {

    @Query("SELECT * FROM credit_cards WHERE isActive = 1 ORDER BY bankName ASC")
    fun getActiveCards(): Flow<List<CreditCard>>

    @Query("SELECT * FROM credit_cards ORDER BY bankName ASC")
    fun getAllCards(): Flow<List<CreditCard>>

    @Query("SELECT * FROM credit_cards WHERE id = :cardId")
    suspend fun getCardById(cardId: Long): CreditCard?

    @Query("SELECT * FROM credit_cards WHERE UPPER(TRIM(bankName)) = UPPER(TRIM(:bankName)) AND cardLast4 = :last4 LIMIT 1")
    suspend fun findCard(bankName: String, last4: String): CreditCard?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCard(card: CreditCard): Long

    @Update
    suspend fun updateCard(card: CreditCard)

    @Delete
    suspend fun deleteCard(card: CreditCard)

    @Query("UPDATE credit_cards SET isActive = 0 WHERE id = :cardId")
    suspend fun deactivateCard(cardId: Long)
}
