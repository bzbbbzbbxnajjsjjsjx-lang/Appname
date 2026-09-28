package com.example.androidapp.vardiya.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidapp.vardiya.domain.analytics.DailyAnalyticsPoint
import com.example.androidapp.vardiya.domain.analytics.MonthWeekBucket
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Lightweight, animated Canvas-based Weekly Bar Chart.
 * Displays 7 daily bars (Monday to Sunday) with dual indicators for regular and overtime work.
 */
@Composable
fun WeeklyBarChart(
    dailyPoints: List<DailyAnalyticsPoint>,
    currencySymbol: String = "₺",
    modifier: Modifier = Modifier
) {
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    val turkishSymbols = remember {
        DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
            decimalSeparator = ','
            groupingSeparator = '.'
        }
    }
    val rateFormat = remember { DecimalFormat("#,##0", turkishSymbols) }

    val maxEarned = remember(dailyPoints) {
        val maxVal = dailyPoints.maxOfOrNull { it.totalEarned } ?: BigDecimal.ZERO
        if (maxVal > BigDecimal.ZERO) maxVal.toFloat() else 1000f
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Günlük Kazanç Eğilimi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegendDot(color = primaryColor, label = "Normal")
                    ChartLegendDot(color = tertiaryColor, label = "Fazla Mesai")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyPoints.forEachIndexed { index, point ->
                    val isSelected = selectedDayIndex == index
                    val regularEarnedVal = point.baseEarned.toFloat()
                    val otEarnedVal = point.overtimeEarned.toFloat()

                    val targetRatio = (point.totalEarned.toFloat() / maxEarned).coerceIn(0f, 1f)
                    val animatedRatio by animateFloatAsState(
                        targetValue = targetRatio,
                        animationSpec = tween(durationMillis = 400),
                        label = "barRatio_$index"
                    )

                    val pointDescription = buildString {
                        append("${point.dayOfWeekName}, ${point.date.dayOfMonth}/${point.date.month}: ")
                        if (point.shiftCount > 0) {
                            append("${point.shiftCount} vardiya, ")
                            append(String.format(Locale.US, "%.1f saat, ", point.totalHours))
                            append("$currencySymbol${rateFormat.format(point.totalEarned)}")
                            if (point.overtimeEarned > BigDecimal.ZERO) {
                                append(" (fazla mesai dahil)")
                            }
                        } else {
                            append("çalışma yok")
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                selectedDayIndex = if (isSelected) null else index
                            }
                            .semantics { contentDescription = pointDescription }
                    ) {
                        // Value label above bar
                        if (point.totalEarned > BigDecimal.ZERO) {
                            Text(
                                text = "${point.totalHours.toInt()}s",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Custom Canvas Bar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .width(22.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barWidth = size.width
                                val fullHeight = size.height

                                // Background track
                                drawRoundRect(
                                    color = trackColor,
                                    size = Size(barWidth, fullHeight),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )

                                if (animatedRatio > 0f) {
                                    val activeHeight = fullHeight * animatedRatio
                                    val totalVal = point.totalEarned.toFloat().coerceAtLeast(0.01f)
                                    val regularFraction = (regularEarnedVal / totalVal).coerceIn(0f, 1f)
                                    val otFraction = (otEarnedVal / totalVal).coerceIn(0f, 1f)

                                    val regularHeight = activeHeight * regularFraction
                                    val otHeight = activeHeight * otFraction

                                    val startY = fullHeight - activeHeight

                                    // If overtime exists, draw top portion with tertiary color
                                    if (otHeight > 0f) {
                                        drawRoundRect(
                                            color = tertiaryColor,
                                            topLeft = Offset(0f, startY),
                                            size = Size(barWidth, otHeight),
                                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                        )
                                    }

                                    // Draw base/regular portion
                                    drawRoundRect(
                                        color = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.85f),
                                        topLeft = Offset(0f, startY + otHeight),
                                        size = Size(barWidth, regularHeight),
                                        cornerRadius = if (otHeight > 0f) CornerRadius(0f, 0f) else CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Day of week & date text
                        Text(
                            text = point.dayOfWeekName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${point.date.dayOfMonth}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Selected Day Detail Card
            selectedDayIndex?.let { idx ->
                val selected = dailyPoints[idx]
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selected.date.formattedDisplay()} (${selected.dayOfWeekName})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val otInfo = if (selected.overtimeEarned > BigDecimal.ZERO) {
                                " • Fazla mesai: $currencySymbol${rateFormat.format(selected.overtimeEarned)}"
                            } else ""
                            Text(
                                text = "${selected.shiftCount} vardiya • ${String.format(Locale.US, "%.1f", selected.totalHours)} saat$otInfo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "$currencySymbol${rateFormat.format(selected.totalEarned)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Monthly trend bar chart representing 4-5 weekly buckets in a month.
 */
@Composable
fun MonthlyTrendChart(
    weeklyBuckets: List<MonthWeekBucket>,
    currencySymbol: String = "₺",
    modifier: Modifier = Modifier
) {
    val turkishSymbols = remember {
        DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
            decimalSeparator = ','
            groupingSeparator = '.'
        }
    }
    val rateFormat = remember { DecimalFormat("#,##0", turkishSymbols) }

    val maxEarned = remember(weeklyBuckets) {
        val maxVal = weeklyBuckets.maxOfOrNull { it.totalEarned } ?: BigDecimal.ZERO
        if (maxVal > BigDecimal.ZERO) maxVal.toFloat() else 1000f
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Haftalık Dağılım",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyBuckets.forEach { bucket ->
                    val targetRatio = (bucket.totalEarned.toFloat() / maxEarned).coerceIn(0f, 1f)
                    val animatedRatio by animateFloatAsState(
                        targetValue = targetRatio,
                        animationSpec = tween(durationMillis = 400),
                        label = "monthBucket_${bucket.weekIndex}"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .semantics {
                                contentDescription = "${bucket.weekIndex}. Hafta (${bucket.label}): ${bucket.shiftCount} vardiya, $currencySymbol${rateFormat.format(bucket.totalEarned)}"
                            }
                    ) {
                        if (bucket.totalEarned > BigDecimal.ZERO) {
                            Text(
                                text = "$currencySymbol${rateFormat.format(bucket.totalEarned)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .width(30.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barWidth = size.width
                                val fullHeight = size.height

                                drawRoundRect(
                                    color = trackColor,
                                    size = Size(barWidth, fullHeight),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )

                                if (animatedRatio > 0f) {
                                    val activeHeight = fullHeight * animatedRatio
                                    drawRoundRect(
                                        color = primaryColor,
                                        topLeft = Offset(0f, fullHeight - activeHeight),
                                        size = Size(barWidth, activeHeight),
                                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${bucket.weekIndex}. Hafta",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Breakdown progress bar showing proportional distribution of base, overtime, and night earnings.
 */
@Composable
fun EarningsCompositionCard(
    baseEarned: BigDecimal,
    overtimeEarned: BigDecimal,
    nightDifferentialEarned: BigDecimal,
    currencySymbol: String = "₺",
    modifier: Modifier = Modifier
) {
    val turkishSymbols = remember {
        DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
            decimalSeparator = ','
            groupingSeparator = '.'
        }
    }
    val rateFormat = remember { DecimalFormat("#,##0.00", turkishSymbols) }

    val total = baseEarned.add(overtimeEarned).add(nightDifferentialEarned)
    val totalFloat = total.toFloat().coerceAtLeast(0.01f)

    val baseFraction = (baseEarned.toFloat() / totalFloat).coerceIn(0f, 1f)
    val otFraction = (overtimeEarned.toFloat() / totalFloat).coerceIn(0f, 1f)
    val nightFraction = (nightDifferentialEarned.toFloat() / totalFloat).coerceIn(0f, 1f)

    val baseColor = MaterialTheme.colorScheme.primary
    val otColor = MaterialTheme.colorScheme.tertiary
    val nightColor = MaterialTheme.colorScheme.secondary

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Kazanç Bileşimi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Horizontal Segmented Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                if (baseFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(baseFraction)
                            .fillMaxHeight()
                            .background(baseColor)
                    )
                }
                if (otFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(otFraction)
                            .fillMaxHeight()
                            .background(otColor)
                    )
                }
                if (nightFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(nightFraction)
                            .fillMaxHeight()
                            .background(nightColor)
                    )
                }
            }

            // Legend Rows
            CompositionMetricRow(
                color = baseColor,
                label = "Normal Mesai",
                amount = "$currencySymbol${rateFormat.format(baseEarned)}",
                percentage = if (total > BigDecimal.ZERO) (baseFraction * 100).toInt() else 100
            )

            if (overtimeEarned > BigDecimal.ZERO) {
                CompositionMetricRow(
                    color = otColor,
                    label = "Fazla Mesai",
                    amount = "$currencySymbol${rateFormat.format(overtimeEarned)}",
                    percentage = (otFraction * 100).toInt()
                )
            }

            if (nightDifferentialEarned > BigDecimal.ZERO) {
                CompositionMetricRow(
                    color = nightColor,
                    label = "Gece Vardiyası Primi",
                    amount = "$currencySymbol${rateFormat.format(nightDifferentialEarned)}",
                    percentage = (nightFraction * 100).toInt()
                )
            }
        }
    }
}

@Composable
private fun CompositionMetricRow(
    color: Color,
    label: String,
    amount: String,
    percentage: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "(%$percentage)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
