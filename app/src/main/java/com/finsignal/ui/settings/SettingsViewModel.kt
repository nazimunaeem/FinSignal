package com.finsignal.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finsignal.data.repository.BillRepository
import com.finsignal.data.local.PreferenceManager
import com.finsignal.notification.DueDateReminderScheduler
import com.finsignal.notification.SmsScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

import kotlinx.coroutines.flow.first

sealed class ExportState {
    data object Idle : ExportState()
    data object Loading : ExportState()
    data class Success(val file: File) : ExportState()
    data class Error(val message: String) : ExportState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val billRepository: BillRepository,
    private val preferenceManager: PreferenceManager
) : AndroidViewModel(application) {

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    val notificationsEnabled: StateFlow<Boolean> = preferenceManager.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val alertRules: StateFlow<Set<String>> = preferenceManager.alertRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf("DAILY"))

    val alertTime: StateFlow<String> = preferenceManager.alertTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "09:00")

    fun triggerRescan() {
        viewModelScope.launch {
            com.finsignal.data.log.ActivityLogger(getApplication()).info("SettingsViewModel", "Manual full rescan triggered")
            // Force a full inbox scan so bills missed by earlier scans are re-detected
            preferenceManager.setFirstScanComplete(false)
            SmsScheduler.scanNow(getApplication(), force = true)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.setNotificationsEnabled(enabled)
            if (enabled) {
                SmsScheduler.schedule(getApplication())
                DueDateReminderScheduler.schedule(getApplication(), preferenceManager)
            } else {
                SmsScheduler.cancel(getApplication())
                DueDateReminderScheduler.cancel(getApplication())
            }
        }
    }

    fun toggleAlertRule(rule: String) {
        viewModelScope.launch {
            preferenceManager.toggleAlertRule(rule)
            DueDateReminderScheduler.schedule(getApplication(), preferenceManager)
        }
    }

    fun setAlertTime(time: String) {
        viewModelScope.launch {
            preferenceManager.setAlertTime(time)
            DueDateReminderScheduler.schedule(getApplication(), preferenceManager)
        }
    }

    fun exportData() {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val context = getApplication<Application>()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(context.filesDir, "cardbill_export_$timestamp.csv")

                val bills = billRepository.getAllBills().first()
                file.bufferedWriter().use { writer ->
                    writer.appendLine("Bank,Card Last4,Period,Currency,Total Due,Min Due,Due Date,Paid")
                    for (bill in bills) {
                        writer.appendLine(
                            "${bill.displayName},${bill.billPeriod},${bill.currency},${bill.totalDue}," +
                            "${bill.minDue},${bill.dueDate},${bill.isPaid}"
                        )
                    }
                }
                _exportState.value = ExportState.Success(file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }
}
