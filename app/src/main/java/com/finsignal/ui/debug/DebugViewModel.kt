package com.finsignal.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsignal.data.local.dao.ActivityLogDao
import com.finsignal.data.local.dao.SmsRecordDao
import com.finsignal.data.local.entity.ActivityLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val activityLogDao: ActivityLogDao,
    private val smsRecordDao: SmsRecordDao,
    private val smsReader: com.finsignal.data.sms.SmsReader
) : ViewModel() {

    private val _logs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val logs: StateFlow<List<ActivityLog>> = _logs.asStateFlow()

    private val _smsRecords = MutableStateFlow<List<com.finsignal.data.local.entity.SmsRecord>>(emptyList())
    val smsRecords: StateFlow<List<com.finsignal.data.local.entity.SmsRecord>> = _smsRecords.asStateFlow()

    private val _smsCount = MutableStateFlow(0)
    val smsCount: StateFlow<Int> = _smsCount.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    companion object {
        private const val PAGE_SIZE = 50
    }

    init {
        refreshLogs()
        loadSmsPage()
        observeSmsCount()
    }

    private fun observeSmsCount() {
        viewModelScope.launch {
            smsRecordDao.getSmsCount().collect { _smsCount.value = it }
        }
    }

    private fun loadSmsPage(offset: Int = 0) {
        viewModelScope.launch {
            if (offset == 0) {
                _smsRecords.value = emptyList()
            }
            _isLoadingMore.value = true
            smsRecordDao.getSmsPage(PAGE_SIZE, offset).collect { page ->
                _smsRecords.value = if (offset == 0) page else _smsRecords.value + page
                _isLoadingMore.value = false
            }
        }
    }

    fun loadMoreSms() {
        if (_isLoadingMore.value) return
        val currentSize = _smsRecords.value.size
        if (currentSize < _smsCount.value) {
            loadSmsPage(currentSize)
        }
    }

    fun refreshLogs() {
        viewModelScope.launch {
            _logs.value = activityLogDao.recent(100)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            activityLogDao.clearAll()
            refreshLogs()
        }
    }

    fun clearSmsDatabase() {
        viewModelScope.launch {
            smsRecordDao.clearAll()
            _smsRecords.value = emptyList()
            _smsCount.value = 0
        }
    }

    fun scanAllSmsDebug() {
        viewModelScope.launch {
            val allSms = smsReader.readRecentSms(100)
            for (sms in allSms) {
                if (smsRecordDao.findByBody(sms.body) == null) {
                    smsRecordDao.insert(
                        com.finsignal.data.local.entity.SmsRecord(
                            address = sms.address,
                            body = sms.body,
                            timestamp = sms.date,
                            isParsed = com.finsignal.data.sms.BankSmsParser.identifyBank(sms.address, sms.body) != com.finsignal.data.sms.BankSmsParser.BankType.UNKNOWN
                        )
                    )
                }
            }
            loadSmsPage(0)
        }
    }
}
