package com.finsignal.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finsignal.ui.theme.StatusSafe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToDebug: () -> Unit = {}
) {
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val alertRules by viewModel.alertRules.collectAsStateWithLifecycle()
    val alertTime by viewModel.alertTime.collectAsStateWithLifecycle()
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
        if (isGranted) viewModel.triggerRescan()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Group: General & Permissions
            SettingsGroup(title = "General") {
                SettingsItem(
                    icon = Icons.Default.Sms,
                    title = "SMS Permission",
                    description = if (hasSmsPermission) "Permission granted" else "Required to detect bills",
                    trailing = {
                        if (!hasSmsPermission) {
                            TextButton(onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) }) {
                                Text("Grant")
                            }
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSafe)
                        }
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Due Date Notifications",
                    description = "Get alerts for upcoming bills",
                    trailing = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { viewModel.toggleNotifications(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = StatusSafe)
                        )
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                var showThemeDialog by remember { mutableStateOf(false) }

                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "App Theme",
                    description = when (darkMode) {
                        "LIGHT" -> "Light"
                        "DARK" -> "Dark"
                        else -> "System Default"
                    },
                    onClick = { showThemeDialog = true },
                    trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                )

                if (showThemeDialog) {
                    ThemeSelectionDialog(
                        currentMode = darkMode,
                        onDismiss = { showThemeDialog = false },
                        onSelect = {
                            viewModel.setDarkMode(it)
                            showThemeDialog = false
                        }
                    )
                }

                if (notificationsEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                    SettingsGroup(title = "Alert Rules", modifier = Modifier.padding(top = 16.dp)) {
                        AlertRuleItem("DAILY", "Daily (all unpaid bills)", alertRules, viewModel)
                        AlertRuleItem("DUE_4_DAILY", "4 days before: Daily", alertRules, viewModel)
                        AlertRuleItem("DUE_3_DAILY", "3 days before: Daily", alertRules, viewModel)
                        AlertRuleItem("DUE_2_12H", "2 days before: 12 hourly", alertRules, viewModel)
                        AlertRuleItem("DUE_2_6H", "2 days before: 6 hourly", alertRules, viewModel)
                        AlertRuleItem("DUE_1_3H", "1 day before: 3 hourly", alertRules, viewModel)
                        AlertRuleItem("DUE_1_1H", "1 day before: Hourly", alertRules, viewModel)
                    }

                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                    var showTimePicker by remember { mutableStateOf(false) }

                    SettingsItem(
                        icon = Icons.Default.AccessTime,
                        title = "Alert Time",
                        description = "Notify me at $alertTime",
                        onClick = { showTimePicker = true },
                        trailing = {
                            Text(
                                text = alertTime,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    )

                    if (showTimePicker) {
                        NotificationTimePickerDialog(
                            initialTime = alertTime,
                            onDismiss = { showTimePicker = false },
                            onConfirm = { hour, minute ->
                                viewModel.setAlertTime("%02d:%02d".format(hour, minute))
                                showTimePicker = false
                            }
                        )
                    }
                }
            }

            // Group: Data Management
            SettingsGroup(title = "Data Management") {
                SettingsItem(
                    icon = Icons.Default.Refresh,
                    title = "Rescan SMS",
                    description = "Search entire inbox for missed bills",
                    onClick = {
                        if (hasSmsPermission) viewModel.triggerRescan()
                        else permissionLauncher.launch(Manifest.permission.READ_SMS)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                SettingsItem(
                    icon = Icons.Default.Share,
                    title = "Export Data",
                    description = "Save all bills to a CSV file",
                    onClick = { if (exportState !is ExportState.Loading) viewModel.exportData() },
                    trailing = {
                        if (exportState is ExportState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                )
                
                if (exportState is ExportState.Success) {
                    Box(modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 16.dp, bottom = 12.dp)) {
                        Button(
                            onClick = {
                                val file = (exportState as ExportState.Success).file
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Export Bills"))
                                viewModel.resetExportState()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Share CSV File")
                        }
                    }
                }
            }

            // Group: Advanced
            SettingsGroup(title = "Advanced") {
                SettingsItem(
                    icon = Icons.Default.BugReport,
                    title = "Debug Tools",
                    description = "View internal logs and SMS records",
                    onClick = onNavigateToDebug,
                    trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                )
            }

            // Group: App Info & Updates
            SettingsGroup(title = "App Info & Updates") {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "GitHub Repository",
                    description = "View source code and contribute",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nazimunaeem/FinSignal"))
                        context.startActivity(intent)
                    },
                    trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                )

                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Check for Updates",
                    description = "Download the latest release",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nazimunaeem/FinSignal/releases"))
                        context.startActivity(intent)
                    },
                    trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                )
            }

            // App Info
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "FinSignal v1.0.4",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Nazim's Concept • AI Executed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val initialHour = initialTime.substringBefore(":").toIntOrNull() ?: 9
    val initialMinute = initialTime.substringAfter(":").toIntOrNull() ?: 0
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Select Alert Time") },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timePickerState)
            }
        }
    )
}

@Composable
fun ThemeSelectionDialog(
    currentMode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column {
                ThemeOption("SYSTEM", "Follow System", currentMode == "SYSTEM", onSelect)
                ThemeOption("LIGHT", "Light", currentMode == "LIGHT", onSelect)
                ThemeOption("DARK", "Dark", currentMode == "DARK", onSelect)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ThemeOption(
    mode: String,
    label: String,
    selected: Boolean,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = { onSelect(mode) })
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun AlertRuleItem(
    rule: String,
    label: String,
    activeRules: Set<String>,
    viewModel: SettingsViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.toggleAlertRule(rule) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = activeRules.contains(rule),
            onCheckedChange = { viewModel.toggleAlertRule(rule) },
            colors = CheckboxDefaults.colors(checkedColor = StatusSafe)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) {
            Box(modifier = Modifier.padding(start = 8.dp)) {
                trailing()
            }
        }
    }
}
