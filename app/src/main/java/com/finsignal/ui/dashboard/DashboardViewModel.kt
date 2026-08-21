package com.finsignal.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finsignal.data.local.entity.BillWithCard
import com.finsignal.data.local.entity.DueStatus
import com.finsignal.data.repository.BillRepository
import com.finsignal.notification.SmsScheduler
import com.finsignal.data.network.UpdateInfo
import com.finsignal.data.network.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class DashboardState(
    val unpaidCount: Int = 0,
    val currentMonthTotals: Map<String, Double> = emptyMap(),
    val currentMonthMinTotals: Map<String, Double> = emptyMap(),
    val previousMonthsTotals: Map<String, Double> = emptyMap(),
    val overdueCount: Int = 0,
    val overdueBills: List<BillWithCard> = emptyList(),
    val dueSoonBills: List<BillWithCard> = emptyList(),
    val upcomingBills: List<BillWithCard> = emptyList(),
    val safeBills: List<BillWithCard> = emptyList(),
    val previousMonthsBills: List<BillWithCard> = emptyList(),
    val isLoading: Boolean = true,
    val updateInfo: UpdateInfo? = null
) {
    fun formatTotals(totals: Map<String, Double>): String {
        if (totals.isEmpty()) return "৳0.00"
        return totals.entries.joinToString(" + ") { (currency, amount) ->
            val symbol = when (currency.uppercase()) {
                "USD" -> "$"
                "BDT", "TK" -> "৳"
                else -> currency
            }
            "$symbol${String.format(Locale.US, "%,.2f", amount)}"
        }
    }
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val billRepository: BillRepository,
    private val updateManager: UpdateManager
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()
    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        loadDashboard()
        checkForUpdates()
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            val currentVersion = getApplication<Application>().packageManager
                .getPackageInfo(getApplication<Application>().packageName, 0).versionName
            val update = updateManager.checkForUpdate(currentVersion ?: "1.0.0")
            _state.value = _state.value.copy(updateInfo = update)
        }
    }

    fun dismissUpdate() {
        _state.value = _state.value.copy(updateInfo = null)
    }

    private fun loadDashboard() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                billRepository.getUnpaidBills(),
                billRepository.getUnpaidCount()
            ) { bills, count ->
                withContext(Dispatchers.Default) {
                    val overdueList = bills.filter { it.dueStatus == DueStatus.OVERDUE }

                    val now = Calendar.getInstance()
                    val currentMonth = now.get(Calendar.MONTH)
                    val currentYear = now.get(Calendar.YEAR)

                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                    val periodFormats = listOf("MMM-yyyy", "MMM yyyy", "MMM-yy", "MMM yy")

                    val thisMonthTotals = mutableMapOf<String, Double>()
                    val thisMonthMinTotals = mutableMapOf<String, Double>()
                    val prevMonthsTotals = mutableMapOf<String, Double>()

                    val thisMonthBillsList = mutableListOf<BillWithCard>()
                    val prevMonthBillsList = mutableListOf<BillWithCard>()

                    bills.forEach { bill ->
                        var isCurrentMonth = false
                        try {
                            val normalizedDueDate = com.finsignal.data.sms.BankSmsParser.normalizeDate(bill.dueDate)
                            val date = sdf.parse(normalizedDueDate)
                            if (date != null) {
                                val billCal = Calendar.getInstance().apply { time = date }
                                if (billCal.get(Calendar.YEAR) == currentYear && billCal.get(Calendar.MONTH) == currentMonth) {
                                    isCurrentMonth = true
                                }
                            }
                        } catch (_: Exception) {}

                        if (!isCurrentMonth) {
                            for (fmt in periodFormats) {
                                try {
                                    val sdfP = SimpleDateFormat(fmt, Locale.US)
                                    val periodDate = sdfP.parse(bill.billPeriod)
                                    if (periodDate != null) {
                                        val periodCal = Calendar.getInstance().apply { time = periodDate }
                                        if (periodCal.get(Calendar.YEAR) == currentYear && periodCal.get(Calendar.MONTH) == currentMonth) {
                                            isCurrentMonth = true
                                            break
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }

                        if (isCurrentMonth) {
                            val remaining = bill.totalDue - bill.paidAmount
                            thisMonthTotals[bill.currency] = (thisMonthTotals[bill.currency] ?: 0.0) + remaining
                            val remainingMin = maxOf(0.0, bill.minDue - bill.paidAmount)
                            thisMonthMinTotals[bill.currency] = (thisMonthMinTotals[bill.currency] ?: 0.0) + remainingMin
                            thisMonthBillsList.add(bill)
                        } else {
                            val remaining = bill.totalDue - bill.paidAmount
                            prevMonthsTotals[bill.currency] = (prevMonthsTotals[bill.currency] ?: 0.0) + remaining
                            prevMonthBillsList.add(bill)
                        }
                    }

                    DashboardState(
                        unpaidCount = count,
                        currentMonthTotals = thisMonthTotals,
                        currentMonthMinTotals = thisMonthMinTotals,
                        previousMonthsTotals = prevMonthsTotals,
                        overdueCount = overdueList.size,
                        overdueBills = thisMonthBillsList.filter { it.dueStatus == DueStatus.OVERDUE },
                        dueSoonBills = thisMonthBillsList.filter { it.dueStatus == DueStatus.DUE_SOON },
                        upcomingBills = thisMonthBillsList.filter { it.dueStatus == DueStatus.UPCOMING },
                        safeBills = thisMonthBillsList.filter { it.dueStatus == DueStatus.SAFE },
                        previousMonthsBills = prevMonthBillsList,
                        isLoading = false
                    )
                }
            }
            .distinctUntilChanged()
            .conflate()
            .collect { _state.value = it }
        }
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

    fun triggerScan() {
        SmsScheduler.scanNow(getApplication())
    }

    fun refresh() {
        _state.value = _state.value.copy(isLoading = true)
        SmsScheduler.scanNow(getApplication())
        loadDashboard()
    }
}
