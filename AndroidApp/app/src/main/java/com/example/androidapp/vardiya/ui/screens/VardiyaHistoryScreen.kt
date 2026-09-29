package com.example.androidapp.vardiya.ui.screens

import androidx.compose.animation.AnimatedVisibility
import com.example.androidapp.theme.motion.VardiyaTheme
import com.example.androidapp.vardiya.ui.components.VardiyaExpandableMotion
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.theme.VardiyaIcons
import com.example.androidapp.vardiya.domain.model.CalendarDate
import com.example.androidapp.vardiya.domain.model.CompletedShiftRecord
import com.example.androidapp.vardiya.domain.model.DayShiftSummary
import com.example.androidapp.vardiya.domain.model.ShiftHistoryFilterEngine
import com.example.androidapp.vardiya.domain.model.ShiftHistoryFilterType
import com.example.androidapp.vardiya.domain.model.ShiftHistorySummary
import com.example.androidapp.vardiya.ui.VardiyaViewModel
import com.example.androidapp.vardiya.ui.adaptive.VardiyaTwoPaneLayout
import com.example.androidapp.vardiya.ui.components.CalendarHeatmap
import com.example.androidapp.vardiya.ui.components.ShiftDetailContent
import com.example.androidapp.vardiya.ui.components.ShiftDetailEmptyState
import com.example.androidapp.vardiya.ui.components.ShiftDetailSheet
import com.example.androidapp.vardiya.ui.navigation.WindowWidthSizeClass
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale

/**
 * Vardiya 3.0 History Screen (Phase F).
 * Features:
 * - Interactive Calendar Heatmap with month navigation and day intensity
 * - Full text search across notes, dates, and times
 * - Filter chips (All, This Month, This Week, Overtime, Night Shift)
 * - Specific date filtering via calendar selection
 * - Summary aggregation metrics (shift count, total earnings, active duration)
 * - Rich ShiftDetailSheet with decomposed base/overtime/night earnings and break logs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VardiyaHistoryScreen(
    viewModel: VardiyaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(ShiftHistoryFilterType.ALL) }
    var selectedCalendarDate by remember { mutableStateOf<CalendarDate?>(null) }
    var isHeatmapVisible by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    // Calendar month/year navigation state
    val initialCal = remember { Calendar.getInstance() }
    var calYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var calMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH) + 1) }

    val turkishSymbols = remember {
        DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
            decimalSeparator = ','
            groupingSeparator = '.'
        }
    }
    val rateFormat = remember { DecimalFormat("#,##0.00", turkishSymbols) }

    // Filtered shifts calculation
    val filteredShifts = remember(uiState.history, searchQuery, filterType, selectedCalendarDate) {
        ShiftHistoryFilterEngine.filter(
            history = uiState.history,
            query = searchQuery,
            filterType = filterType,
            selectedDate = selectedCalendarDate
        )
    }

    // Heatmap month aggregation
    val monthHeatmapData = remember(uiState.history, calYear, calMonth) {
        ShiftHistoryFilterEngine.aggregateMonth(uiState.history, calYear, calMonth)
    }

    // Aggregated summary for current filtered view
    val summary = remember(filteredShifts) {
        ShiftHistoryFilterEngine.summarize(filteredShifts)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Vardiya Geçmişi",
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
                    // Toggle Calendar Heatmap
                    IconButton(
                        onClick = { isHeatmapVisible = !isHeatmapVisible },
                        modifier = Modifier.semantics {
                            contentDescription = if (isHeatmapVisible) "Takvimi gizle" else "Takvimi göster"
                        }
                    ) {
                        Icon(
                            imageVector = VardiyaIcons.Calendar,
                            contentDescription = if (isHeatmapVisible) "Takvimi gizle" else "Takvimi göster",
                            tint = if (isHeatmapVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (uiState.history.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmation = true },
                            modifier = Modifier.semantics { contentDescription = "Geçmişi temizle" }
                        ) {
                            Icon(
                                imageVector = VardiyaIcons.Delete,
                                contentDescription = "Geçmişi temizle",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val sizeClass = WindowWidthSizeClass.fromWidth(maxWidth)

            if (uiState.history.isEmpty()) {
                // Empty State Illustration & Message
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = VardiyaIcons.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Henüz Tamamlanan Vardiya Yok",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "İlk vardiyanızı tamamladığınızda kayıtlarınız, takvim ısı haritası ve detaylı çalışma dökümünüz burada listelenecektir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (sizeClass.isCompact) {
                // Compact Layout: Single-column list with ModalBottomSheet detail
                HistoryListContent(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    filterType = filterType,
                    onFilterTypeChange = { filterType = it },
                    selectedCalendarDate = selectedCalendarDate,
                    onSelectCalendarDate = { selectedCalendarDate = it },
                    isHeatmapVisible = isHeatmapVisible,
                    calYear = calYear,
                    calMonth = calMonth,
                    monthHeatmapData = monthHeatmapData,
                    onPreviousMonth = {
                        if (calMonth == 1) {
                            calMonth = 12
                            calYear -= 1
                        } else {
                            calMonth -= 1
                        }
                    },
                    onNextMonth = {
                        if (calMonth == 12) {
                            calMonth = 1
                            calYear += 1
                        } else {
                            calMonth += 1
                        }
                    },
                    summary = summary,
                    filteredShifts = filteredShifts,
                    selectedRecordId = uiState.selectedHistoryRecord?.id,
                    onSelectRecord = { viewModel.selectHistoryRecord(it) },
                    onClearFilters = {
                        searchQuery = ""
                        filterType = ShiftHistoryFilterType.ALL
                        selectedCalendarDate = null
                    },
                    rateFormat = rateFormat,
                    currencySymbol = uiState.history.firstOrNull()?.currencySymbol ?: "₺",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                )

                // ModalBottomSheet only on compact screens
                uiState.selectedHistoryRecord?.let { selectedRecord ->
                    ShiftDetailSheet(
                        record = selectedRecord,
                        onDismiss = { viewModel.selectHistoryRecord(null) }
                    )
                }
            } else {
                // Medium / Expanded: Responsive Two-Pane Layout
                VardiyaTwoPaneLayout(
                    sizeClass = sizeClass,
                    listPaneWeight = 0.44f,
                    detailPaneWeight = 0.56f,
                    listPane = {
                        HistoryListContent(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            filterType = filterType,
                            onFilterTypeChange = { filterType = it },
                            selectedCalendarDate = selectedCalendarDate,
                            onSelectCalendarDate = { selectedCalendarDate = it },
                            isHeatmapVisible = isHeatmapVisible,
                            calYear = calYear,
                            calMonth = calMonth,
                            monthHeatmapData = monthHeatmapData,
                            onPreviousMonth = {
                                if (calMonth == 1) {
                                    calMonth = 12
                                    calYear -= 1
                                } else {
                                    calMonth -= 1
                                }
                            },
                            onNextMonth = {
                                if (calMonth == 12) {
                                    calMonth = 1
                                    calYear += 1
                                } else {
                                    calMonth += 1
                                }
                            },
                            summary = summary,
                            filteredShifts = filteredShifts,
                            selectedRecordId = uiState.selectedHistoryRecord?.id,
                            onSelectRecord = { viewModel.selectHistoryRecord(it) },
                            onClearFilters = {
                                searchQuery = ""
                                filterType = ShiftHistoryFilterType.ALL
                                selectedCalendarDate = null
                            },
                            rateFormat = rateFormat,
                            currencySymbol = uiState.history.firstOrNull()?.currencySymbol ?: "₺",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 20.dp, end = 12.dp)
                        )
                    },
                    detailPane = {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 12.dp, end = 20.dp, top = 4.dp, bottom = 16.dp)
                        ) {
                            val selectedRecord = uiState.selectedHistoryRecord
                            if (selectedRecord != null) {
                                ShiftDetailContent(
                                    record = selectedRecord,
                                    onClose = { viewModel.selectHistoryRecord(null) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                ShiftDetailEmptyState(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                )
            }
        }
    }

    // Clear History Confirmation Dialog
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Geçmiş Temizlensin mi?") },
            text = { Text("Tüm kayıtlı vardiya geçmişiniz kalıcı olarak silinecektir.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirmation = false
                    }
                ) {
                    Text("Evet, Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun HistoryListContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filterType: ShiftHistoryFilterType,
    onFilterTypeChange: (ShiftHistoryFilterType) -> Unit,
    selectedCalendarDate: CalendarDate?,
    onSelectCalendarDate: (CalendarDate?) -> Unit,
    isHeatmapVisible: Boolean,
    calYear: Int,
    calMonth: Int,
    monthHeatmapData: Map<Int, DayShiftSummary>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    summary: ShiftHistorySummary,
    filteredShifts: List<CompletedShiftRecord>,
    selectedRecordId: String?,
    onSelectRecord: (CompletedShiftRecord) -> Unit,
    onClearFilters: () -> Unit,
    rateFormat: DecimalFormat,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Vardiya veya not ara...") },
            leadingIcon = {
                Icon(
                    imageVector = VardiyaIcons.Search,
                    contentDescription = "Ara",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = VardiyaIcons.Close,
                            contentDescription = "Aramayı temizle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp)
        )

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShiftHistoryFilterType.values().forEach { type ->
                FilterChip(
                    selected = filterType == type,
                    onClick = { onFilterTypeChange(type) },
                    label = { Text(type.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            // Selected Calendar Date Chip (if active)
            if (selectedCalendarDate != null) {
                FilterChip(
                    selected = true,
                    onClick = { onSelectCalendarDate(null) },
                    label = { Text("${selectedCalendarDate.dayOfMonth}/${selectedCalendarDate.month} ✕") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )
            }
        }

        // Expandable Calendar Heatmap
        AnimatedVisibility(
            visible = isHeatmapVisible,
            enter = VardiyaExpandableMotion.createEnterTransition(
                VardiyaTheme.motionScheme,
                VardiyaTheme.motionPreference
            ),
            exit = VardiyaExpandableMotion.createExitTransition(
                VardiyaTheme.motionScheme,
                VardiyaTheme.motionPreference
            )
        ) {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                CalendarHeatmap(
                    displayedYear = calYear,
                    displayedMonth = calMonth,
                    monthData = monthHeatmapData,
                    selectedDate = selectedCalendarDate,
                    onSelectDate = onSelectCalendarDate,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth
                )
            }
        }

        // Summary Statistics Bar
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FİLTRELENEN KAZANÇ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol${rateFormat.format(summary.totalEarned)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${summary.totalCount} vardiya",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = summary.formattedTotalDuration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Shifts List / Empty Filtered State
        if (filteredShifts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Filtrelere Uygun Vardiya Bulunamadı",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Arama kriterlerinizi veya filtrelerinizi değiştirerek tekrar deneyebilirsiniz.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Filtreleri Temizle")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredShifts, key = { it.id }) { item ->
                    ShiftListItemCard(
                        item = item,
                        isSelected = item.id == selectedRecordId,
                        onClick = { onSelectRecord(item) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ShiftListItemCard(
    item: CompletedShiftRecord,
    onClick: () -> Unit,
    isSelected: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        border = if (isSelected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else null,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${item.dateFormatted}, süre: ${item.durationFormatted}, kazanç: ${item.earnedFormatted}"
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Date & Earned
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.dateFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.earnedFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Time range & Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.timeRangeFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.durationFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Badges row (Overtime, Night shift, Breaks count, Note snippet)
            val hasOvertime = item.overtimeEarned > BigDecimal.ZERO || item.overtimeDurationMs > 0L
            val hasNight = item.nightDifferentialEarned > BigDecimal.ZERO || item.nightShiftDurationMs > 0L
            val hasBreaks = item.breaks.isNotEmpty()

            if (hasOvertime || hasNight || hasBreaks || !item.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasOvertime) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "Fazla Mesai",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (hasNight) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "Gece",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (hasBreaks) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = "${item.breaks.size} Mola",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (!item.note.isNullOrBlank()) {
                        Text(
                            text = "“${item.note}”",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }
    }
}
