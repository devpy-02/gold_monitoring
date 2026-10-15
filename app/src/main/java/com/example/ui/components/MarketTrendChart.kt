package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.PriceHistoryPoint
import com.example.model.TimePeriod
import com.example.ui.theme.TrendGreen
import com.example.ui.theme.TrendRed
import com.example.util.PersianUtils
import java.math.BigDecimal

@Composable
fun MarketTrendChart(
    points: List<PriceHistoryPoint>,
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier,
    isPositiveTrend: Boolean = true,
    usePersianDigits: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("market_trend_chart")
    ) {
        // Time period selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimePeriod.entries.forEach { period ->
                val isSelected = period == selectedPeriod
                FilterChip(
                    selected = isSelected,
                    onClick = { onPeriodSelected(period) },
                    label = {
                        Text(
                            text = period.labelPersian,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("period_chip_${period.name}")
                )
            }
        }

        if (points.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "اطلاعات تاریخی برای این دوره موجود نیست",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        // Min & Max calculations
        val minPrice = remember(points) { points.minOf { it.priceToman } }
        val maxPrice = remember(points) { points.maxOf { it.priceToman } }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "بیشترین: ${PersianUtils.formatMoney(maxPrice, isToman = true, usePersianDigits = usePersianDigits, includeUnit = false)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "کمترین: ${PersianUtils.formatMoney(minPrice, isToman = true, usePersianDigits = usePersianDigits, includeUnit = false)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Line Chart Canvas
        val lineColor = if (isPositiveTrend) TrendGreen else TrendRed
        val gradientColor = lineColor.copy(alpha = 0.22f)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(vertical = 4.dp)
        ) {
            val width = size.width
            val height = size.height
            val range = (maxPrice.subtract(minPrice)).toDouble().coerceAtLeast(1.0)

            val coords = points.mapIndexed { index, point ->
                val x = (index.toFloat() / (points.size - 1).coerceAtLeast(1)) * width
                val normalizedY = ((point.priceToman.subtract(minPrice)).toDouble() / range).toFloat()
                val y = height - (normalizedY * (height * 0.78f) + (height * 0.11f))
                Offset(x, y)
            }

            if (coords.size >= 2) {
                val strokePath = Path().apply {
                    moveTo(coords[0].x, coords[0].y)
                    for (i in 1 until coords.size) {
                        val prev = coords[i - 1]
                        val curr = coords[i]
                        val cX = (prev.x + curr.x) / 2f
                        cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                // Fill area gradient
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(gradientColor, Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )

                // Stroke line
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Endpoint indicator
                val lastPoint = coords.last()
                drawCircle(
                    color = lineColor,
                    radius = 4.dp.toPx(),
                    center = lastPoint
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = lastPoint
                )
            }
        }
    }
}
