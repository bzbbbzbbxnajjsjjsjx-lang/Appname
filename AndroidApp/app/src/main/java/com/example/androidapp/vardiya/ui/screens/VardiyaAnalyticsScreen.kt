package com.example.androidapp.vardiya.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.theme.VardiyaIcons
import com.example.androidapp.vardiya.domain.analytics.AnalyticsPeriod
import com.example.androidapp.vardiya.ui.VardiyaViewModel
import com.example.androidapp.vardiya.ui.components.EarningsCompositionCard
import com.example.androidapp.vardiya.ui.components.MonthlyTrendChart
import com.example.androidapp.vardiya.ui.components.WeeklyBarChart
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Vardiya 3.0 Analytics Screen (Phase G).
 * Features:
 * - Deterministic, testable weekly and monthly analytics derived from frozen historical records
 * - Lightweight Compose Canvas animated weekly bar chart and monthly trend chart
 * - Dynamic period switching (Weekly / Monthly) with previous/next navigation
 * - Complete breakdown of regular, overtime, and night differential earnings
 * - All-time overall metrics summary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VardiyaAnalyticsScreen(
    viewModel: VardiyaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val turkishSymbols = remember {
        DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
            decimalSeparator = ','
            groupingSeparator = '.'
        }
    }
    val currencyFormat = remember { DecimalFormat("#,##0.00", turkishSymbols) }
    val currencySymbol = uiState.salaryConfig.currencySymbol

    val weeklyData = uiState.weeklyAnalytics
    val monthlyData = uiState.monthlyAnalytics
    val overallData = uiState.overallAnalytics

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Vardiya Analitiği",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Geri dön" }
                    ) {
                        Icon(
                            imageVector = VardiyaIcons.ArrowBack,
                            contentDescription = "Geri dön"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.resetAnalyticsToCurrent() },
                        modifier = Modifier.semantics { contentDescription = "Güncel döneme dön" }
                    ) {
                        Icon(
                            imageVector = VardiyaIcons.Refresh,
                            contentDescription = "Güncel döneme dön",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.history.isEmpty()) {
                // Empty State Illustration & Message
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = VardiyaIcons.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Henüz Analitik Verisi Yok",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tamamlanan vardiyalarınız kaydedildikçe haftalık ve aylık kazanç eğilimleriniz, fazla mesai dökümünüz ve verim analizleriniz burada listelenecektir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Period Selector Tabs & Navigation Bar
                PeriodSelectorAndNavigator(
                    currentPeriod = uiState.analyticsPeriod,
                    periodTitle = if (uiState.analyticsPeriod == AnalyticsPeriod.WEEKLY) {
                        weeklyData.weekRangeFormatted
                    } else {
                        monthlyData.monthName
                    },
                    onSelectPeriod = { viewModel.setAnalyticsPeriod(it) },
                    onPrevious = { viewModel.navigateAnalyticsPrevious() },
                    onNext = { viewModel.navigateAnalyticsNext() }
                )

                // Animated Chart View
                AnimatedContent(
                    targetState = uiState.analyticsPeriod,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "analyticsChartTransition"
                ) { targetPeriod ->
                    if (targetPeriod == AnalyticsPeriod.WEEKLY) {
                        WeeklyBarChart(
                            dailyPoints = weeklyData.dailyPoints,
                            currencySymbol = currencySymbol
                        )
                    } else {
                        MonthlyTrendChart(
                            weeklyBuckets = monthlyData.weeklyBuckets,
                            currencySymbol = currencySymbol
                        )
                    }
                }

                // Period Summary Cards
                if (uiState.analyticsPeriod == AnalyticsPeriod.WEEKLY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnalyticsKpiCard(
                            title = "Haftalık Kazanç",
                            value = "$currencySymbol${currencyFormat.format(weeklyData.totalEarned)}",
                            subtitle = "${weeklyData.totalShifts} vardiya",
                            modifier = Modifier.weight(1f),
                            highlightColor = MaterialTheme.colorScheme.primary
                        )

                        AnalyticsKpiCard(
                            title = "Çalışma Süresi",
                            value = weeklyData.formattedTotalDuration,
                            subtitle = "Ort. $currencySymbol${currencyFormat.format(weeklyData.averageHourlyRate)}/saat",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (weeklyData.totalOvertimeEarned > BigDecimal.ZERO || weeklyData.totalNightDifferentialEarned > BigDecimal.ZERO) {
                        EarningsCompositionCard(
                            baseEarned = weeklyData.totalEarned.subtract(weeklyData.totalOvertimeEarned).subtract(weeklyData.totalNightDifferentialEarned),
                            overtimeEarned = weeklyData.totalOvertimeEarned,
                            nightDifferentialEarned = weeklyData.totalNightDifferentialEarned,
                            currencySymbol = currencySymbol
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnalyticsKpiCard(
                            title = "Aylık Kazanç",
                            value = "$currencySymbol${currencyFormat.format(monthlyData.totalEarned)}",
                            subtitle = "${monthlyData.totalShifts} vardiya",
                            modifier = Modifier.weight(1f),
                            highlightColor = MaterialTheme.colorScheme.primary
                        )

                        AnalyticsKpiCard(
                            title = "Çalışma Süresi",
                            value = monthlyData.formattedTotalDuration,
                            subtitle = "Ort. $currencySymbol${currencyFormat.format(monthlyData.averageHourlyRate)}/saat",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (monthlyData.totalOvertimeEarned > BigDecimal.ZERO || monthlyData.totalNightDifferentialEarned > BigDecimal.ZERO) {
                        EarningsCompositionCard(
                            baseEarned = monthlyData.totalEarned.subtract(monthlyData.totalOvertimeEarned).subtract(monthlyData.totalNightDifferentialEarned),
                            overtimeEarned = monthlyData.totalOvertimeEarned,
                            nightDifferentialEarned = monthlyData.totalNightDifferentialEarned,
                            currencySymbol = currencySymbol
                        )
                    }

                    if (monthlyData.totalBreakDeductionMs > 0L) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ücret Kesintili Toplam Mola:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = monthlyData.formattedBreakDeduction,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // All-Time Summary Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Tüm Zamanlar İstatistikleri",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        MetricSummaryRow(
                            label = "Toplam Kayıtlı Vardiya",
                            value = "${overallData.totalShifts} adet"
                        )
                        MetricSummaryRow(
                            label = "Toplam Net Kazanç",
                            value = "$currencySymbol${currencyFormat.format(overallData.totalEarned)}",
                            isBold = true,
                            highlightColor = MaterialTheme.colorScheme.primary
                        )
                        MetricSummaryRow(
                            label = "Toplam Çalışılan Süre",
                            value = overallData.formattedTotalDuration
                        )
                        MetricSummaryRow(
                            label = "Vardiya Başına Ortalama",
                            value = "$currencySymbol${currencyFormat.format(overallData.averageShiftEarned)}"
                        )
                        MetricSummaryRow(
                            label = "Ortalama Saatlik Verim",
                            value = "$currencySymbol${currencyFormat.format(overallData.averageHourlyRate)} / saat"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PeriodSelectorAndNavigator(
    currentPeriod: AnalyticsPeriod,
    periodTitle: String,
    onSelectPeriod: (AnalyticsPeriod) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Segmented Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsPeriod.values().forEach { period ->
                    FilterChip(
                        selected = currentPeriod == period,
                        onClick = { onSelectPeriod(period) },
                        label = { Text(period.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Period Navigator (< [Period Title] >)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.semantics { contentDescription = "Önceki dönem" }
                ) {
                    Icon(
                        imageVector = VardiyaIcons.ChevronLeft,
                        contentDescription = "Önceki dönem",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = periodTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.semantics { contentDescription = "Sonraki dönem" }
                ) {
                    Icon(
                        imageVector = VardiyaIcons.ChevronRight,
                        contentDescription = "Sonraki dönem",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsKpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    highlightColor: androidx.compose.ui.graphics.Color? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = highlightColor ?: MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun MetricSummaryRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    highlightColor: androidx.compose.ui.graphics.Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}
