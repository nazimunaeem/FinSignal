package com.finsignal.ui.cards

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finsignal.data.local.entity.BillWithCard
import com.finsignal.data.local.entity.CreditCard
import com.finsignal.data.repository.BillRepository
import com.finsignal.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardWithStats(
    val card: CreditCard,
    val totalPaid: Double
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CardsViewModel @Inject constructor(
    application: Application,
    private val cardRepository: CardRepository,
    private val billRepository: BillRepository
) : AndroidViewModel(application) {

    val cards: StateFlow<List<CardWithStats>> = cardRepository.getAllCards()
        .flatMapLatest { cardList ->
            val flows = cardList.map { card ->
                billRepository.getPaidTotalForCard(card.id).map { paid ->
                    CardWithStats(card, paid ?: 0.0)
                }
            }
            if (flows.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                combine(flows) { it.toList() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getBillsForCard(cardId: Long): Flow<List<BillWithCard>> {
        return billRepository.getBillsForCard(cardId)
    }

    fun updateNickname(card: CreditCard, nickname: String) {
        viewModelScope.launch {
            cardRepository.updateCard(card.copy(cardNickname = nickname))
        }
    }

    fun deleteCard(card: CreditCard) {
        viewModelScope.launch {
            cardRepository.deleteCard(card)
        }
    }
}
