package com.cardbill.data.repository

import com.cardbill.data.local.dao.CreditCardDao
import com.cardbill.data.local.entity.CreditCard
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepository @Inject constructor(
    private val cardDao: CreditCardDao
) {
    fun getActiveCards(): Flow<List<CreditCard>> = cardDao.getActiveCards()

    fun getAllCards(): Flow<List<CreditCard>> = cardDao.getAllCards()

    suspend fun getCardById(id: Long): CreditCard? = cardDao.getCardById(id)

    suspend fun findCard(bankName: String, last4: String): CreditCard? =
        cardDao.findCard(bankName, last4)

    suspend fun addCard(card: CreditCard): Long = cardDao.insertCard(card)

    suspend fun updateCard(card: CreditCard) = cardDao.updateCard(card)

    suspend fun deleteCard(card: CreditCard) = cardDao.deleteCard(card)

    suspend fun deactivateCard(cardId: Long) = cardDao.deactivateCard(cardId)
}
