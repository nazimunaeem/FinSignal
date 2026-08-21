package com.finsignal.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsignal.data.local.entity.BillWithCard
import com.finsignal.data.repository.BillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val billRepository: BillRepository
) : ViewModel() {

    private val _selectedYear = MutableStateFlow(getCurrentYear())
    val selectedYear: StateFlow<String> = _selectedYear.asStateFlow()

    private val allBills = billRepository.getAllBills()

    /**
     * Exposes unique years present in the bill history for filtering.
     */
    val availableYears: StateFlow<List<String>> = allBills
        .map { bills ->
            bills.mapNotNull { extractYear(it.dueDate) }
                .distinct()
                .sortedDescending()
                .ifEmpty { listOf(getCurrentYear()) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(getCurrentYear())
        )

    /**
     * Exposes bills filtered by the selected year.
     */
    val filteredBills: StateFlow<List<BillWithCard>> = combine(allBills, _selectedYear) { bills, year ->
        bills.filter { extractYear(it.dueDate) == year }
            .sortedByDescending { it.detectedAt }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun getCurrentYear(): String {
        return Calendar.getInstance().get(Calendar.YEAR).toString()
    }

    private fun extractYear(dateStr: String): String? {
        // Date is stored as dd/MM/yyyy in DB
        return dateStr.substringAfterLast('/', "").takeIf { it.isNotEmpty() }
    }

    fun selectYear(year: String) {
        _selectedYear.value = year
    }

    fun markBillAsPaid(billId: Long) {
        viewModelScope.launch {
            billRepository.markAsPaid(billId)
        }
    }

    fun updatePartialPayment(billId: Long, amount: Double) {
        viewModelScope.launch {
            billRepository.updatePaidAmount(billId, amount)
        }
    }
}
