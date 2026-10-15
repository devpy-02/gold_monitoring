package com.example.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.MarketDataState
import com.example.model.MarketCategory
import com.example.model.MarketPriceItem
import com.example.ui.components.LuxuryCard
import com.example.ui.components.MarketTrendChart
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SilverPrimary
import com.example.ui.theme.TrendGreen
import com.example.ui.theme.TrendRed
import com.example.ui.viewmodel.MarketViewModel
import com.example.util.PersianUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    marketViewModel: MarketViewModel,
    onNavigateToCalculator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val marketUiState by marketViewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            HeaderSection(
                marketDataState = marketUiState.marketDataState,
                isRefreshing = marketUiState.isRefreshing,
                onRefresh = { marketViewModel.refresh() }
            )
        }

        item {
            QuickCalculatorBanner(onClick = onNavigateToCalculator)
        }

        item {
            KeyRatesSection(
                marketDataState = marketUiState.marketDataState,
                isToman = marketUiState.isToman,
                usePersianDigits = marketUiState.usePersianDigits
            )
        }

        item {
            MarketTrendSection(
                marketViewModel = marketViewModel,
                usePersianDigits = marketUiState.usePersianDigits
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeaderSection(
    marketDataState: MarketDataState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "ZarNegar Logo",
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "زرنگار",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "پیشخوان زنده بازار طلا و ارز (منبع: TGJU)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            when (marketDataState) {
                is MarketDataState.Success -> {
                    val statusText = when {
                        marketDataState.isOffline -> "آفلاین (نرخ ذخیره)"
                        marketDataState.isStale -> "نرخ‌های قدیمی"
                        marketDataState.hasManualOverrides -> "دستی"
                        else -> "زنده"
                    }
                    val statusColor = when {
                        marketDataState.isOffline -> MaterialTheme.colorScheme.error
                        marketDataState.isStale -> Color(0xFFF59E0B)
                        else -> TrendGreen
                    }
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor
                            )
                        }
                    }
                }
                else -> {}
            }

            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier.testTag("home_refresh_button")
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "به‌روزرسانی",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCalculatorBanner(onClick: () -> Unit) {
    LuxuryCard(
        borderColor = GoldPrimary.copy(alpha = 0.5f),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.testTag("quick_calculator_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "محاسبه‌گر پیشرفته طلا و نقره",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "محاسبه آنلاین اجرت، سود و مالیات قانونی",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_open_calculator")
            ) {
                Text("محاسبه", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun KeyRatesSection(
    marketDataState: MarketDataState,
    isToman: Boolean,
    usePersianDigits: Boolean
) {
    Column {
        Text(
            text = "نرخ‌های اصلی بازار",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        when (marketDataState) {
            is MarketDataState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            }
            is MarketDataState.Success -> {
                val items = marketDataState.items
                val gold18 = items.find { it.id == "gold_18k" }
                val gold24 = items.find { it.id == "gold_24k" }
                val silver999 = items.find { it.id == "silver_999" }
                val usdIrr = items.find { it.id == "usd_irr" }
                val emami = items.find { it.id == "coin_emami" }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        gold18?.let {
                            RateMiniCard(
                                item = it,
                                isToman = isToman,
                                usePersianDigits = usePersianDigits,
                                accentColor = GoldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        silver999?.let {
                            RateMiniCard(
                                item = it,
                                isToman = isToman,
                                usePersianDigits = usePersianDigits,
                                accentColor = SilverPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        gold24?.let {
                            RateMiniCard(
                                item = it,
                                isToman = isToman,
                                usePersianDigits = usePersianDigits,
                                accentColor = GoldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        usdIrr?.let {
                            RateMiniCard(
                                item = it,
                                isToman = isToman,
                                usePersianDigits = usePersianDigits,
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    emami?.let {
                        RateHorizontalCard(
                            item = it,
                            isToman = isToman,
                            usePersianDigits = usePersianDigits,
                            accentColor = GoldPrimary
                        )
                    }
                }
            }
            is MarketDataState.Error -> {
                Text(
                    text = marketDataState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RateMiniCard(
    item: MarketPriceItem,
    isToman: Boolean,
    usePersianDigits: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    LuxuryCard(
        modifier = modifier,
        borderColor = accentColor.copy(alpha = 0.3f),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.titleFa,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TrendBadge(changePercent = item.changePercent24h, usePersianDigits = usePersianDigits)
            }

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

            Text(
                text = item.unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RateHorizontalCard(
    item: MarketPriceItem,
    isToman: Boolean,
    usePersianDigits: Boolean,
    accentColor: Color
) {
    LuxuryCard(
        borderColor = accentColor.copy(alpha = 0.25f),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.titleFa,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = PersianUtils.formatMoney(
                        item.currentPriceToman,
                        isToman = isToman,
                        usePersianDigits = usePersianDigits,
                        includeUnit = true
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                TrendBadge(changePercent = item.changePercent24h, usePersianDigits = usePersianDigits)
            }
        }
    }
}

@Composable
fun TrendBadge(changePercent: Double, usePersianDigits: Boolean) {
    val isPositive = changePercent >= 0
    val color = if (isPositive) TrendGreen else TrendRed
    val formatted = String.format(Locale.US, "%.2f", Math.abs(changePercent))
    val text = (if (isPositive) "+" else "-") + (if (usePersianDigits) PersianUtils.toPersianDigits(formatted) else formatted) + "٪"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun MarketTrendSection(
    marketViewModel: MarketViewModel,
    usePersianDigits: Boolean
) {
    val uiState by marketViewModel.uiState.collectAsState()

    LuxuryCard(
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "روند مظنه طلای ۱۸ عیار",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            MarketTrendChart(
                points = uiState.chartPoints,
                selectedPeriod = uiState.selectedChartPeriod,
                onPeriodSelected = { marketViewModel.selectChartPeriod(it) },
                usePersianDigits = usePersianDigits,
                isPositiveTrend = true
            )
        }
    }
}
