package com.finsignal.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finsignal.ui.components.BillCard
import com.finsignal.ui.components.SectionHeader
import com.finsignal.ui.components.SmsPermissionCard
import com.finsignal.ui.components.SummaryCard
import com.finsignal.ui.theme.StatusDueSoon
import com.finsignal.ui.theme.StatusOverdue
import com.finsignal.ui.theme.StatusSafe
import com.finsignal.ui.theme.StatusUpcoming

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
        if (isGranted) {
            viewModel.refresh()
        }
    }

    val showLoading = state.isLoading && state.unpaidCount == 0

    if (showLoading) {
        FirstTimeLoadingScreen()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FinSignal",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = {
                if (hasSmsPermission) {
                    viewModel.refresh()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_SMS)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (!hasSmsPermission) {
                    item {
                        SmsPermissionCard(
                            onRequestPermission = {
                                permissionLauncher.launch(Manifest.permission.READ_SMS)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                item {
                    SummaryCard(
                        thisMonthTotal = state.formatTotals(state.currentMonthTotals),
                        thisMonthMinTotal = state.formatTotals(state.currentMonthMinTotals),
                        prevMonthTotal = state.formatTotals(state.previousMonthsTotals),
                        unpaidCount = state.unpaidCount,
                        overdueCount = state.overdueCount,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (state.unpaidCount == 0 && !state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No bills found",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Credit card bills will appear here\nafter SMS is detected",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                if (state.overdueBills.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "OVERDUE",
                            count = state.overdueBills.size,
                            color = StatusOverdue
                        )
                    }
                    items(state.overdueBills, key = { it.billId }) { bill ->
                        BillCard(
                            bill = bill,
                            onMarkPaid = { viewModel.markBillAsPaid(bill.billId) },
                            onUpdatePayment = { viewModel.updatePartialPayment(bill.billId, it) },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem(fadeOutSpec = tween(500))
                        )
                    }
                }

                if (state.dueSoonBills.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "DUE SOON",
                            count = state.dueSoonBills.size,
                            color = StatusDueSoon
                        )
                    }
                    items(state.dueSoonBills, key = { it.billId }) { bill ->
                        BillCard(
                            bill = bill,
                            onMarkPaid = { viewModel.markBillAsPaid(bill.billId) },
                            onUpdatePayment = { viewModel.updatePartialPayment(bill.billId, it) },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem(fadeOutSpec = tween(500))
                        )
                    }
                }

                if (state.upcomingBills.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "UPCOMING",
                            count = state.upcomingBills.size,
                            color = StatusUpcoming
                        )
                    }
                    items(state.upcomingBills, key = { it.billId }) { bill ->
                        BillCard(
                            bill = bill,
                            onMarkPaid = { viewModel.markBillAsPaid(bill.billId) },
                            onUpdatePayment = { viewModel.updatePartialPayment(bill.billId, it) },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem(fadeOutSpec = tween(500))
                        )
                    }
                }

                if (state.safeBills.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "SAFE",
                            count = state.safeBills.size,
                            color = StatusSafe
                        )
                    }
                    items(state.safeBills, key = { it.billId }) { bill ->
                        BillCard(
                            bill = bill,
                            onMarkPaid = { viewModel.markBillAsPaid(bill.billId) },
                            onUpdatePayment = { viewModel.updatePartialPayment(bill.billId, it) },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem(fadeOutSpec = tween(500))
                        )
                    }
                }

                if (state.previousMonthsBills.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "PREVIOUS MONTHS",
                            count = state.previousMonthsBills.size,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(state.previousMonthsBills, key = { it.billId }) { bill ->
                        BillCard(
                            bill = bill,
                            onMarkPaid = { viewModel.markBillAsPaid(bill.billId) },
                            onUpdatePayment = { viewModel.updatePartialPayment(bill.billId, it) },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem(fadeOutSpec = tween(500))
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun FirstTimeLoadingScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val fadeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fade"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                )

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                )

                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(40.dp)
                        .alpha(fadeAlpha)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Scanning Your SMS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Reading bank messages to find your credit card bills...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                LoadingStepRow(Icons.Default.CreditCard, "Reading SMS messages", true, fadeAlpha)
                LoadingStepRow(Icons.Default.Search, "Identifying bank messages", true, fadeAlpha * 0.7f)
                LoadingStepRow(Icons.Default.CheckCircle, "Extracting bill information", false, 0.4f)
            }

            Spacer(modifier = Modifier.height(48.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This may take a moment on first launch",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun LoadingStepRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isActive: Boolean,
    alpha: Float
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isActive) {
            Spacer(modifier = Modifier.weight(1f))
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
