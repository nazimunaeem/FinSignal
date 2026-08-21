package com.finsignal.ui.cards

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finsignal.data.local.entity.CreditCard
import com.finsignal.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardsViewModel @Inject constructor(
    application: Application,
    private val cardRepository: CardRepository
) : AndroidViewModel(application) {

    val cards: StateFlow<List<CreditCard>> = cardRepository.getAllCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteCard(card: CreditCard) {
        viewModelScope.launch {
            cardRepository.deleteCard(card)
        }
    }
}
