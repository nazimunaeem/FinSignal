package com.finsignal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finsignal.data.local.entity.BillWithCard
import com.finsignal.data.local.entity.DueStatus
import com.finsignal.ui.theme.StatusDueSoon
import com.finsignal.ui.theme.StatusDueSoonBg
import com.finsignal.ui.theme.StatusOverdue
import com.finsignal.ui.theme.StatusOverdueBg
import com.finsignal.ui.theme.StatusPaid
import com.finsignal.ui.theme.StatusPaidBg
import com.finsignal.ui.theme.StatusSafe
import com.finsignal.ui.theme.StatusSafeBg
import com.finsignal.ui.theme.StatusUpcoming
import com.finsignal.ui.theme.StatusUpcomingBg

@Composable
fun BillCard(
    modifier: Modifier = Modifier,
    bill: BillWithCard,
    onMarkPaid: () -> Unit,
    onUpdatePayment: (Double) -> Unit = {},
    onMarkUnpaid: () -> Unit = {},
    onEditBill: (totalDue: Double, minDue: Double, dueDate: String) -> Unit = { _, _, _ -> }
) {
    var isVisible by remember { mutableStateOf(true) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            kotlinx.coroutines.delay(500)
            onMarkPaid()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = modifier
    ) {
        val statusColor = when (bill.dueStatus) {
            DueStatus.PAID -> StatusPaid
            DueStatus.OVERDUE -> StatusOverdue
            DueStatus.DUE_SOON -> StatusDueSoon
            DueStatus.UPCOMING -> StatusUpcoming
            DueStatus.SAFE -> StatusSafe
        }

        val bgColor = when (bill.dueStatus) {
            DueStatus.PAID -> StatusPaidBg
            DueStatus.OVERDUE -> StatusOverdueBg
            DueStatus.DUE_SOON -> StatusDueSoonBg
            DueStatus.UPCOMING -> StatusUpcomingBg
            DueStatus.SAFE -> StatusSafeBg
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = bill.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Period: ${bill.billPeriod}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    StatusChip(status = bill.dueStatus, statusColor = statusColor, bgColor = bgColor)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (bill.paidAmount > 0) "Remaining" else "Total Due",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (bill.paidAmount > 0) bill.formattedRemaining else bill.formattedTotal,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Min Due",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val remainingMin = maxOf(0.0, bill.minDue - bill.paidAmount)
                        val formattedMin = if (remainingMin > 0) {
                            val symbol = when (bill.currency.uppercase()) {
                                "USD" -> "$"
                                else -> "৳"
                            }
                            "$symbol${String.format(java.util.Locale.US, "%,.2f", remainingMin)}"
                        } else "Cleared"

                        Text(
                            text = formattedMin,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingMin > 0) statusColor else StatusSafe
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Due Date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = bill.formattedDueDate,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusOverdue
                        )
                    }

                    if (bill.paidAmount > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Paid",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val symbol = when (bill.currency.uppercase()) {
                                "USD" -> "$"
                                else -> "৳"
                            }
                            Text(
                                text = "$symbol${String.format(java.util.Locale.US, "%,.2f", bill.paidAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusSafe
                            )
                        }
                    } else if (!bill.isPaid && bill.daysUntilDue >= 0) {
                        Text(
                            text = "${bill.daysUntilDue} day(s) left",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                HorizontalDivider(modifier = Modifier.alpha(0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!bill.isPaid) {
                        TextButton(
                            onClick = { showPaymentDialog = true },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Partial", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Create,
                                contentDescription = "Edit bill",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        if (!bill.isPaid) {
                            Spacer(modifier = Modifier.width(6.dp))
                            val checkedState = bill.isPaid
                            Switch(
                                checked = checkedState,
                                onCheckedChange = { isVisible = false },
                                modifier = Modifier.scale(0.6f),
                                thumbContent = {
                                    if (checkedState) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Paid",
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = StatusSafe,
                                    checkedTrackColor = StatusSafe.copy(alpha = 0.3f)
                                )
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Paid",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPaymentDialog) {
        PaymentAmountDialog(
            totalDue = bill.totalDue,
            minDue = bill.minDue,
            currentPaid = bill.paidAmount,
            currency = bill.currency,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount ->
                onUpdatePayment(amount)
                showPaymentDialog = false
            }
        )
    }

    if (showEditDialog) {
        EditBillDialog(
            totalDue = bill.totalDue,
            minDue = bill.minDue,
            dueDate = bill.dueDate,
            currency = bill.currency,
            isPaid = bill.isPaid,
            onDismiss = { showEditDialog = false },
            onConfirm = { total, min, due ->
                onEditBill(total, min, due)
                showEditDialog = false
            },
            onMarkUnpaid = {
                showEditDialog = false
                isVisible = true
                onMarkUnpaid()
            }
        )
    }
}

@Composable
fun EditBillDialog(
    totalDue: Double,
    minDue: Double,
    dueDate: String,
    currency: String,
    isPaid: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (totalDue: Double, minDue: Double, dueDate: String) -> Unit,
    onMarkUnpaid: () -> Unit = {}
) {
    var totalText by remember { mutableStateOf(totalDue.toString()) }
    var minText by remember { mutableStateOf(minDue.toString()) }
    var dueText by remember { mutableStateOf(dueDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Bill") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = totalText,
                    onValueChange = { totalText = it },
                    label = { Text("Total Due ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = minText,
                    onValueChange = { minText = it },
                    label = { Text("Minimum Due ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dueText,
                    onValueChange = { dueText = it },
                    label = { Text("Due Date (dd/mm/yyyy)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (isPaid) {
                    TextButton(
                        onClick = onMarkUnpaid,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Unpaid", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val total = totalText.replace(",", "").toDoubleOrNull() ?: return@TextButton
                    val min = minText.replace(",", "").toDoubleOrNull() ?: return@TextButton
                    if (dueText.isBlank()) return@TextButton
                    onConfirm(total, min, dueText.trim())
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PaymentAmountDialog(
    totalDue: Double,
    minDue: Double,
    currentPaid: Double,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(if (currentPaid > 0) currentPaid.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Paid Amount") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Total: ${currency} $totalDue", style = MaterialTheme.typography.bodySmall)
                Text("Minimum: ${currency} $minDue", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount Paid") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { amountText = minDue.toString() },
                        label = { Text("Min Due") }
                    )
                    AssistChip(
                        onClick = { amountText = totalDue.toString() },
                        label = { Text("Full Due") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    onConfirm(amount)
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StatusChip(
    status: DueStatus,
    statusColor: Color,
    bgColor: Color
) {
    val text = when (status) {
        DueStatus.PAID -> "Paid"
        DueStatus.OVERDUE -> "Overdue"
        DueStatus.DUE_SOON -> "Soon"
        DueStatus.UPCOMING -> "Upcoming"
        DueStatus.SAFE -> "Safe"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SummaryCard(
    thisMonthTotal: String,
    thisMonthMinTotal: String,
    prevMonthTotal: String,
    unpaidCount: Int,
    overdueCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Due (This Month)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = thisMonthTotal,
                        style = if (thisMonthTotal.length > 15) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Min Due",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = thisMonthMinTotal,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = "Previous Months Balance",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
            Text(
                text = prevMonthTotal,
                style = if (prevMonthTotal.length > 15) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row {
                Text(
                    text = "$unpaidCount card(s) unpaid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
                if (overdueCount > 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$overdueCount overdue",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF8A80),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SmsPermissionCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SMS Permission Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "FinSignal needs SMS access to detect bills automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(10.dp))
            androidx.compose.material3.Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text("Grant Permission", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
