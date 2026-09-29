package com.example.androidapp.vardiya.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.theme.VardiyaIcons
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.validator.SalaryConfigValidator
import com.example.androidapp.vardiya.domain.validator.SalaryValidationResult
import com.example.androidapp.vardiya.ui.VardiyaViewModel
import com.example.androidapp.vardiya.ui.components.VardiyaTimePickerDialog
import com.example.androidapp.vardiya.ui.navigation.WindowWidthSizeClass
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Modern Material 3 Expressive Settings and Advanced Work Configuration screen for Vardiya 3.0.
 *
 * Sections:
 * 1. Maaş & Temel Mesai (Salary, working days, daily hours, break minutes, deduct break switch)
 * 2. Fazla Mesai Ayarları (Overtime toggle, quick multiplier chips, custom input)
 * 3. Gece Vardiyası Ayarları (Night toggle, differential rate chips, custom input, start/end hours with M3 TimePicker)
 * 4. Görünüm & Tema (Dynamic Color Material You switch)
 * 5. Canlı Ücret Oranları Kartı (Live rate preview card with hourly, minute, second, overtime, and night rates)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VardiyaSettingsScreen(
    viewModel: VardiyaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val initialConfig = uiState.salaryConfig

    // Section 1: Maaş & Temel Mesai State
    var salaryText by remember(initialConfig) { mutableStateOf(initialConfig.monthlySalary.toPlainString()) }
    var workDaysText by remember(initialConfig) { mutableStateOf(initialConfig.monthlyWorkDays.toString()) }
    var workHoursText by remember(initialConfig) { mutableStateOf(initialConfig.dailyWorkHours.toPlainString()) }
    var breakMinutesText by remember(initialConfig) { mutableStateOf(initialConfig.breakMinutes.toString()) }
    var deductBreak by remember(initialConfig) { mutableStateOf(initialConfig.deductBreakFromSalary) }

    // Section 2: Fazla Mesai State
    var isOvertimeEnabled by remember(initialConfig) { mutableStateOf(initialConfig.isOvertimeEnabled) }
    var overtimeMultiplierText by remember(initialConfig) { mutableStateOf(initialConfig.overtimeMultiplier.toPlainString()) }

    // Section 3: Gece Vardiyası State
    var isNightDifferentialEnabled by remember(initialConfig) { mutableStateOf(initialConfig.isNightDifferentialEnabled) }
    var nightDifferentialRateText by remember(initialConfig) { mutableStateOf(initialConfig.nightDifferentialRate.toPlainString()) }
    var nightShiftStartHour by remember(initialConfig) { mutableStateOf(initialConfig.nightShiftStartHour) }
    var nightShiftEndHour by remember(initialConfig) { mutableStateOf(initialConfig.nightShiftEndHour) }

    // Dialog state for TimePicker
    var showStartHourPicker by remember { mutableStateOf(false) }
    var showEndHourPicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Validation
    val validationResult = remember(
        salaryText, workDaysText, workHoursText, breakMinutesText, deductBreak,
        overtimeMultiplierText, nightDifferentialRateText,
        nightShiftStartHour, nightShiftEndHour
    ) {
        SalaryConfigValidator.validate(
            salaryText = salaryText,
            workDaysText = workDaysText,
            workHoursText = workHoursText,
            breakMinutesText = breakMinutesText,
            deductBreak = deductBreak,
            currencySymbol = initialConfig.currencySymbol,
            currencyCode = initialConfig.currencyCode,
            overtimeMultiplierText = overtimeMultiplierText,
            nightDifferentialRateText = nightDifferentialRateText,
            nightShiftStartHour = nightShiftStartHour,
            nightShiftEndHour = nightShiftEndHour
        )
    }

    // Live preview configuration
    val previewConfig = remember(
        validationResult,
        salaryText, workDaysText, workHoursText, breakMinutesText, deductBreak,
        overtimeMultiplierText, isOvertimeEnabled,
        nightDifferentialRateText, isNightDifferentialEnabled,
        nightShiftStartHour, nightShiftEndHour
    ) {
        if (validationResult is SalaryValidationResult.Valid) {
            SalaryConfigValidator.parseOrNull(
                salaryText = salaryText,
                workDaysText = workDaysText,
                workHoursText = workHoursText,
                breakMinutesText = breakMinutesText,
                deductBreak = deductBreak,
                currencySymbol = initialConfig.currencySymbol,
                currencyCode = initialConfig.currencyCode,
                overtimeMultiplierText = overtimeMultiplierText,
                isOvertimeEnabled = isOvertimeEnabled,
                nightDifferentialRateText = nightDifferentialRateText,
                isNightDifferentialEnabled = isNightDifferentialEnabled,
                nightShiftStartHour = nightShiftStartHour,
                nightShiftEndHour = nightShiftEndHour
            )
        } else {
            null
        }
    }

    val turkishSymbols = remember {
        DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
            decimalSeparator = ','
            groupingSeparator = '.'
        }
    }
    val rateFormat = remember { DecimalFormat("#,##0.00", turkishSymbols) }
    val secondRateFormat = remember { DecimalFormat("0.00000", turkishSymbols) }

    // TimePicker Dialogs
    if (showStartHourPicker) {
        VardiyaTimePickerDialog(
            title = "Gece Başlangıç Saati",
            initialHour = nightShiftStartHour,
            initialMinute = 0,
            is24Hour = true,
            onConfirm = { hour, _ ->
                nightShiftStartHour = hour
                showStartHourPicker = false
            },
            onDismiss = { showStartHourPicker = false }
        )
    }

    if (showEndHourPicker) {
        VardiyaTimePickerDialog(
            title = "Gece Bitiş Saati",
            initialHour = nightShiftEndHour,
            initialMinute = 0,
            is24Hour = true,
            onConfirm = { hour, _ ->
                nightShiftEndHour = hour
                showEndHourPicker = false
            },
            onDismiss = { showEndHourPicker = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Maaş & Çalışma Ayarları",
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
        @Composable
        fun SaveButton() {
            Button(
                onClick = {
                    if (previewConfig != null) {
                        viewModel.updateSalaryConfig(previewConfig)
                        scope.launch {
                            snackbarHostState.showSnackbar("Maaş ve çalışma ayarları başarıyla güncellendi.")
                        }
                    }
                },
                enabled = validationResult is SalaryValidationResult.Valid && previewConfig != null,
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { contentDescription = "Maaş ayarlarını kaydet" }
            ) {
                Text(
                    text = "AYARLARI KAYDET",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        @Composable
        fun SettingsFormContent(showSaveButton: Boolean) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Validation Error Banner
                if (validationResult is SalaryValidationResult.Invalid) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = VardiyaIcons.Info,
                                contentDescription = "Hata",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = validationResult.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // ==========================================
                // SECTION 1: MAAŞ & TEMEL MESAİ
                // ==========================================
                Text(
                    text = "Maaş & Temel Mesai",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = salaryText,
                            onValueChange = { salaryText = it },
                            label = { Text("Aylık Maaş (${initialConfig.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = validationResult is SalaryValidationResult.Invalid &&
                                    validationResult.field == SalaryValidationResult.Field.SALARY,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Aylık maaş tutarı" }
                        )

                        OutlinedTextField(
                            value = workDaysText,
                            onValueChange = { workDaysText = it },
                            label = { Text("Ayda kaç gün çalışıyorsun?") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = validationResult is SalaryValidationResult.Invalid &&
                                    validationResult.field == SalaryValidationResult.Field.WORK_DAYS,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Aylık çalışma gün sayısı" }
                        )

                        OutlinedTextField(
                            value = workHoursText,
                            onValueChange = { workHoursText = it },
                            label = { Text("Günde kaç saat çalışıyorsun?") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            isError = validationResult is SalaryValidationResult.Invalid &&
                                    validationResult.field == SalaryValidationResult.Field.WORK_HOURS,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Günlük çalışma saat süresi" }
                        )

                        OutlinedTextField(
                            value = breakMinutesText,
                            onValueChange = { breakMinutesText = it },
                            label = { Text("Mola süresi (dakika)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = validationResult is SalaryValidationResult.Invalid &&
                                    validationResult.field == SalaryValidationResult.Field.BREAK_MINUTES,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Günlük mola süresi dakika" }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mola Ücretten Kesilsin",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Mola süreleri net çalışma süresinden ve kazançtan düşülür.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = deductBreak,
                                onCheckedChange = { deductBreak = it },
                                modifier = Modifier.semantics {
                                    contentDescription = "Mola maaştan düşülsün mü seçeneği"
                                }
                            )
                        }
                    }
                }

                // ==========================================
                // SECTION 2: FAZLA MESAİ AYARLARI
                // ==========================================
                Text(
                    text = "Fazla Mesai Ayarları",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Fazla Mesaiyi Etkinleştir",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Normal günlük çalışma saatini aşan süreler çarpanla hesaplanır.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isOvertimeEnabled,
                                onCheckedChange = { isOvertimeEnabled = it },
                                modifier = Modifier.semantics {
                                    contentDescription = "Fazla mesai anahtarı"
                                }
                            )
                        }

                        if (isOvertimeEnabled) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Text(
                                text = "Hızlı Çarpan Seçimi",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val overtimePresets = listOf("1.25", "1.50", "1.75", "2.00")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                overtimePresets.forEach { preset ->
                                    val isSelected = overtimeMultiplierText.trim() == preset
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { overtimeMultiplierText = preset },
                                        label = { Text("${preset}x") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = overtimeMultiplierText,
                                onValueChange = { overtimeMultiplierText = it },
                                label = { Text("Özel Fazla Mesai Çarpanı (örn: 1.50)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                isError = validationResult is SalaryValidationResult.Invalid &&
                                        validationResult.field == SalaryValidationResult.Field.OVERTIME_MULTIPLIER,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Fazla mesai çarpanı" }
                            )
                        }
                    }
                }

                // ==========================================
                // SECTION 3: GECE VARDİYASI AYARLARI
                // ==========================================
                Text(
                    text = "Gece Vardiyası Ayarları",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gece Farkını Etkinleştir",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Gece saat aralığındaki çalışma için ek ücret uygulanır.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isNightDifferentialEnabled,
                                onCheckedChange = { isNightDifferentialEnabled = it },
                                modifier = Modifier.semantics {
                                    contentDescription = "Gece vardiyası anahtarı"
                                }
                            )
                        }

                        if (isNightDifferentialEnabled) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Text(
                                text = "Gece Farkı Ek Oranı",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val nightPresets = listOf("0.10" to "%10", "0.15" to "%15", "0.20" to "%20", "0.25" to "%25")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                nightPresets.forEach { (value, label) ->
                                    val isSelected = nightDifferentialRateText.trim() == value ||
                                            nightDifferentialRateText.trim() == label
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { nightDifferentialRateText = value },
                                        label = { Text(label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = nightDifferentialRateText,
                                onValueChange = { nightDifferentialRateText = it },
                                label = { Text("Özel Gece Farkı Oranı (örn: 0.15 veya %15)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                isError = validationResult is SalaryValidationResult.Invalid &&
                                        validationResult.field == SalaryValidationResult.Field.NIGHT_DIFFERENTIAL_RATE,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Gece farkı oranı" }
                            )

                            Text(
                                text = "Gece Saat Aralığı (Saat Seçimi)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Start Hour Selector
                                OutlinedCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showStartHourPicker = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Başlangıç",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = String.format(Locale.getDefault(), "%02d:00", nightShiftStartHour),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // End Hour Selector
                                OutlinedCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showEndHourPicker = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Bitiş",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = String.format(Locale.getDefault(), "%02d:00", nightShiftEndHour),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // SECTION 4: GÖRÜNÜM & TEMA (DYNAMIC COLOR)
                // ==========================================
                Text(
                    text = "Görünüm & Tema",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dinamik Renk (Material You)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Sistem duvar kağıdı renklerine göre dinamik tema uygular (Android 12+).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.isDynamicColorEnabled,
                                onCheckedChange = { viewModel.setDynamicColorEnabled(it) },
                                modifier = Modifier.semantics {
                                    contentDescription = "Dinamik renk anahtarı"
                                }
                            )
                        }
                    }
                }

                if (showSaveButton) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SaveButton()
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val sizeClass = WindowWidthSizeClass.fromWidth(maxWidth)

            if (sizeClass.isCompact) {
                // Compact Screen (Phones): Single scrollable column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingsFormContent(showSaveButton = false)

                    if (previewConfig != null) {
                        LiveRatesPreviewCard(
                            previewConfig = previewConfig,
                            deductBreak = deductBreak,
                            rateFormat = rateFormat,
                            secondRateFormat = secondRateFormat
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    SaveButton()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // Medium / Expanded Screen (Tablets / Foldables): Responsive 2-column layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // Left Column: Configuration Form & Save Action
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SettingsFormContent(showSaveButton = true)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Right Column: Live Rates Preview Card & Useful Information
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (previewConfig != null) {
                            LiveRatesPreviewCard(
                                previewConfig = previewConfig,
                                deductBreak = deductBreak,
                                rateFormat = rateFormat,
                                secondRateFormat = secondRateFormat
                            )
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = VardiyaIcons.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Geçerli Değerler Giriniz",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Canlı ücret oranlarınızı hesaplayabilmemiz için lütfen soldaki formda geçerli parametreler belirleyiniz.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Helpful Tips Card on Large Screen
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "💡 Çalışma Parametresi İpuçları",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "• Saatlik ve saniyelik ücretleriniz, girdiğiniz aylık maaş ve net çalışma saatinize göre tam duyarlılıkla hesaplanır.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "• Yapılan değişiklikler 'Ayarları Kaydet' butonuna bastığınız anda aktif vardiyanıza ve sonraki vardiyalarınıza hemen uygulanır.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveRatesPreviewCard(
    previewConfig: SalaryConfiguration,
    deductBreak: Boolean,
    rateFormat: DecimalFormat,
    secondRateFormat: DecimalFormat,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Hesaplanan Ücret Oranları (Canlı)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Saatlik Ücret:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${previewConfig.currencySymbol}${rateFormat.format(previewConfig.hourlyRate.setScale(2, RoundingMode.HALF_UP))}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Dakikalık Ücret:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${previewConfig.currencySymbol}${rateFormat.format(previewConfig.minuteRate.setScale(4, RoundingMode.HALF_UP))}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Saniyelik Ücret:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${previewConfig.currencySymbol}${secondRateFormat.format(previewConfig.secondRate.setScale(5, RoundingMode.HALF_UP))}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (previewConfig.isOvertimeEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Fazla Mesai Saatlik (${previewConfig.overtimeMultiplier}x):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${previewConfig.currencySymbol}${rateFormat.format(previewConfig.overtimeHourlyRate.setScale(2, RoundingMode.HALF_UP))}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (previewConfig.isNightDifferentialEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gece Farkı Ek Saatlik (+%${previewConfig.nightDifferentialRate.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)}):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "+${previewConfig.currencySymbol}${rateFormat.format(previewConfig.nightDifferentialHourlyRate.setScale(2, RoundingMode.HALF_UP))}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gece Saat Aralığı:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format(Locale.getDefault(), "%02d:00", previewConfig.nightShiftStartHour)} - ${String.format(Locale.getDefault(), "%02d:00", previewConfig.nightShiftEndHour)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (deductBreak) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ücretli Günlük Çalışma:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${previewConfig.dailyPaidHours.setScale(1, RoundingMode.HALF_UP)} saat",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
