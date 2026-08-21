package com.finsignal.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: DebugViewModel = hiltViewModel()
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val smsRecords by viewModel.smsRecords.collectAsStateWithLifecycle()
    val smsCount by viewModel.smsCount.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= smsRecords.size - 5 && smsRecords.size < smsCount && !isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && selectedTab == 1) {
            viewModel.loadMoreSms()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Activity Log") },
                actions = {
                    IconButton(onClick = { viewModel.scanAllSmsDebug() }) {
                        Icon(Icons.Default.Search, "Scan All SMS")
                    }
                    IconButton(onClick = {
                        if (selectedTab == 0) viewModel.clearLogs() else viewModel.clearSmsDatabase()
                    }) {
                        Icon(Icons.Default.DeleteSweep, "Clear")
                    }
                    IconButton(onClick = { viewModel.refreshLogs() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Logs", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("SMS Database ($smsCount)", modifier = Modifier.padding(12.dp))
                }
            }

            if (selectedTab == 0) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(logs) { log ->
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row {
                                Text(
                                    text = "[${log.level}]",
                                    color = if (log.level == "ERROR") Color.Red else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = log.tag ?: "", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Text(text = log.message, fontSize = 14.sp)
                            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(smsRecords) { record ->
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row {
                                Text(
                                    text = if (record.isParsed) "PARSED" else "NOT PARSED",
                                    color = if (record.isParsed) Color.Green else Color.Red,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = record.address ?: "Unknown", fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                                fontSize = 11.sp
                            )
                            Text(text = record.body, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                        }
                    }

                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    if (!isLoadingMore && smsRecords.isNotEmpty() && smsRecords.size >= smsCount) {
                        item {
                            Text(
                                text = "All $smsCount records loaded",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
