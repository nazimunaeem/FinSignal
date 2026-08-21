package com.finsignal.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finsignal.data.local.entity.BillWithCard
import com.finsignal.data.repository.BillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    application: Application,
    private val billRepository: BillRepository
) : AndroidViewModel(application) {

    private val _selectedMonth = MutableStateFlow(getCurrentMonth())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    val allBills: StateFlow<List<BillWithCard>> = billRepository.getAllBills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun getCurrentMonth(): String {
        return SimpleDateFormat("MMM yyyy", Locale.US).format(Date())
    }

    fun selectMonth(month: String) {
        _selectedMonth.value = month
    }

    fun getAvailableMonths(bills: List<BillWithCard>): List<String> {
        val months = bills.map { bill ->
            try {
                val input = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                val output = SimpleDateFormat("MMM yyyy", Locale.US)
                val date = input.parse(bill.dueDate)
                date?.let { output.format(it) } ?: ""
            } catch (e: Exception) {
                ""
            }
        }.filter { it.isNotBlank() }.distinct().sorted()

        return months.ifEmpty { listOf(getCurrentMonth()) }
    }

    fun markBillAsPaid(billId: Long) {
        viewModelScope.launch {
            billRepository.markAsPaid(billId)
        }
    }

    fun markBillAsUnpaid(billId: Long) {
        viewModelScope.launch {
            billRepository.markAsUnpaid(billId)
        }
    }

    fun editBill(billId: Long, totalDue: Double, minDue: Double, dueDate: String) {
        viewModelScope.launch {
            val normalizedDue = com.finsignal.data.sms.BankSmsParser.normalizeDate(dueDate)
            billRepository.updateBillDetails(billId, totalDue, minDue, normalizedDue)
        }
    }

    fun updatePartialPayment(billId: Long, amount: Double) {
        viewModelScope.launch {
            billRepository.updatePaidAmount(billId, amount)
        }
    }

    fun getFilteredBills(bills: List<BillWithCard>, month: String): List<BillWithCard> {
        return bills.filter { bill ->
            try {
                val input = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                val output = SimpleDateFormat("MMM yyyy", Locale.US)
                val date = input.parse(bill.dueDate)
                date?.let { output.format(it) == month } ?: false
            } catch (e: Exception) {
                false
            }
        }
    }
}
