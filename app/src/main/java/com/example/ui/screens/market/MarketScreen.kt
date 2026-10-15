package com.example.ui.screens.market

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.MarketDataState
import com.example.model.MarketCategory
import com.example.model.MarketPriceItem
import com.example.ui.components.LuxuryCard
import com.example.ui.components.MarketTrendChart
import com.example.ui.components.PersianNumberField
import com.example.ui.screens.home.TrendBadge
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SilverPrimary
import com.example.ui.viewmodel.MarketViewModel
import com.example.util.PersianUtils
import kotlinx.coroutines.flow.collectLatest
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MarketScreen(
    viewModel: MarketViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val categories = listOf("همه", "طلا و سکه", "نقره", "ارز و اونس")

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                MarketTopBar(
                    marketDataState = uiState.marketDataState,
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() }
                )
            }

            // Category Tab Row
            item {
                TabRow(
                    selectedTabIndex = selectedCategoryIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    categories.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            // Historical Chart Card
            item {
                HistoricalChartCard(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }

            // List of Prices
            when (val state = uiState.marketDataState) {
                is MarketDataState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = GoldPrimary)
                        }
                    }
                }
                is MarketDataState.Success -> {
                    val filteredItems = state.items.filter { item ->
                        when (selectedCategoryIndex) {
                            1 -> item.category == MarketCategory.GOLD
                            2 -> item.category == MarketCategory.SILVER
                            3 -> item.category == MarketCategory.CURRENCY
                            else -> true
                        }
                    }

                    items(filteredItems, key = { it.id }) { item ->
                        MarketPriceDetailCard(
                            item = item,
                            isToman = uiState.isToman,
                            usePersianDigits = uiState.usePersianDigits,
                            isSelectedForChart = item.id == uiState.selectedChartItemId,
                            onSelectForChart = { viewModel.selectChartItem(item.id) },
                            onEdit = { viewModel.openEditPriceDialog(item.id) }
                        )
                    }
                }
                is MarketDataState.Error -> {
                    item {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Manual Edit Price Dialog
        val editingId = uiState.editingItemId
        if (editingId != null) {
            val item = (uiState.marketDataState as? MarketDataState.Success)?.items?.find { it.id == editingId }
            if (item != null) {
                ManualPriceEditDialog(
                    item = item,
                    usePersianDigits = uiState.usePersianDigits,
                    onDismiss = { viewModel.dismissEditPriceDialog() },
                    onSave = { newPrice -> viewModel.saveManualPrice(item.id, newPrice) },
                    onResetToLive = { viewModel.resetToLive(item.id) }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun MarketTopBar(
    marketDataState: MarketDataState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "نرخ‌های زنده و مظنه بازار",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (marketDataState is MarketDataState.Success) {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timeStr = timeFormat.format(Date(marketDataState.lastUpdated))
                Text(
                    text = "منبع: شبکه اطلاع‌رسانی طلا و ارز (TGJU) • $timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = onRefresh,
            enabled = !isRefreshing,
            modifier = Modifier.testTag("market_screen_refresh")
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "به‌روزرسانی",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun HistoricalChartCard(
    uiState: com.example.ui.viewmodel.MarketUiState,
    viewModel: MarketViewModel
) {
    val selectedItem = (uiState.marketDataState as? MarketDataState.Success)
        ?.items?.find { it.id == uiState.selectedChartItemId }

    LuxuryCard(containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نمودار تحلیلی: ${selectedItem?.titleFa ?: "طلا"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                selectedItem?.let {
                    TrendBadge(changePercent = it.changePercent24h, usePersianDigits = uiState.usePersianDigits)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            MarketTrendChart(
                points = uiState.chartPoints,
                selectedPeriod = uiState.selectedChartPeriod,
                onPeriodSelected = { viewModel.selectChartPeriod(it) },
                usePersianDigits = uiState.usePersianDigits,
                isPositiveTrend = (selectedItem?.changePercent24h ?: 0.0) >= 0.0
            )
        }
    }
}

@Composable
private fun MarketPriceDetailCard(
    item: MarketPriceItem,
    isToman: Boolean,
    usePersianDigits: Boolean,
    isSelectedForChart: Boolean,
    onSelectForChart: () -> Unit,
    onEdit: () -> Unit
) {
    val borderColor = if (isSelectedForChart) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    LuxuryCard(
        borderColor = borderColor,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .clickable { onSelectForChart() }
            .testTag("market_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.titleFa,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (item.isManualOverride) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "دستی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = PersianUtils.formatMoney(
                            item.currentPriceToman,
                            isToman = isToman,
                            usePersianDigits = usePersianDigits,
                            includeUnit = false
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    TrendBadge(changePercent = item.changePercent24h, usePersianDigits = usePersianDigits)
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .testTag("edit_price_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "ویرایش نرخ",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualPriceEditDialog(
    item: MarketPriceItem,
    usePersianDigits: Boolean,
    onDismiss: () -> Unit,
    onSave: (BigDecimal) -> Unit,
    onResetToLive: () -> Unit
) {
    var inputPrice by remember { mutableStateOf(item.currentPriceToman.toPlainString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ویرایش دستی نرخ ${item.titleFa}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "می‌توانید نرخ روز بازار شهر یا اتحادیه محلی خود را وارد کنید تا در تمام محاسبات زرنگار اعمال شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PersianNumberField(
                    value = inputPrice,
                    onValueChange = { inputPrice = it },
                    label = "قیمت جدید",
                    unitSuffix = item.unit,
                    allowDecimal = false,
                    usePersianDigits = usePersianDigits,
                    testTag = "dialog_input_manual_price"
                )

                if (item.isManualOverride) {
                    OutlinedButton(
                        onClick = onResetToLive,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بازگشت به نرخ زنده بازار")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = PersianUtils.parseBigDecimal(inputPrice)
                    if (parsed > BigDecimal.ZERO) {
                        onSave(parsed)
                    }
                }
            ) {
                Text("ثبت نرخ", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
