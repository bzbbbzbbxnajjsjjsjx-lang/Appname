package com.example.androidapp.vardiya.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.theme.VardiyaIcons
import com.example.androidapp.vardiya.ui.VardiyaViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

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

    // Aggregate summary metrics from history using CompletedShiftRecord fields
    val totalShifts = uiState.history.size
    val totalEarned = remember(uiState.history) {
        uiState.history.fold(BigDecimal.ZERO) { acc, record -> acc.add(record.totalEarned) }
    }
    val totalDurationMs = remember(uiState.history) {
        uiState.history.fold(0L) { acc, record -> acc + record.activeDurationMs }
    }
    val avgEarned = remember(totalShifts, totalEarned) {
        if (totalShifts > 0) totalEarned.divide(BigDecimal(totalShifts), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
    }

    val totalHours = totalDurationMs / 3600000
    val totalMinutes = (totalDurationMs % 3600000) / 60000
    val totalDurationFormatted = "${totalHours}s ${totalMinutes}dk"

    val currencySymbol = uiState.salaryConfig.currencySymbol

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
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPI Summary Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Toplam Vardiya",
                    value = "$totalShifts",
                    subtitle = "Tamamlanan oturum",
                    modifier = Modifier.weight(1f)
                )

                KpiCard(
                    title = "Toplam Kazanç",
                    value = "$currencySymbol${currencyFormat.format(totalEarned)}",
                    subtitle = "Tüm geçmiş toplamı",
                    modifier = Modifier.weight(1f),
                    highlightColor = MaterialTheme.colorScheme.primary
                )
            }

            // KPI Summary Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Toplam Çalışma",
                    value = totalDurationFormatted,
                    subtitle = "Net kayıtlı süre",
                    modifier = Modifier.weight(1f)
                )

                KpiCard(
                    title = "Ortalama Kazanç",
                    value = "$currencySymbol${currencyFormat.format(avgEarned)}",
                    subtitle = "Vardiya başına ort.",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Informative Analytics Roadmap Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = VardiyaIcons.Info,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Gelişmiş Analitik & Mesai Grafikleri",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Vardiya 3.0 ile haftalık/aylık kazanç eğilimleri, fazla mesai ısı haritası ve CSV dökümü sonraki aşamalarda bu panel üzerinden sunulacaktır.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
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
