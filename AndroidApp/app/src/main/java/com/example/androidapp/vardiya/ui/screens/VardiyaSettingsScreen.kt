package com.example.androidapp.vardiya.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.theme.VardiyaIcons
import com.example.androidapp.vardiya.domain.validator.SalaryConfigValidator
import com.example.androidapp.vardiya.domain.validator.SalaryValidationResult
import com.example.androidapp.vardiya.ui.VardiyaViewModel
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VardiyaSettingsScreen(
    viewModel: VardiyaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val initialConfig = uiState.salaryConfig

    var salaryText by remember(initialConfig) { mutableStateOf(initialConfig.monthlySalary.toPlainString()) }
    var workDaysText by remember(initialConfig) { mutableStateOf(initialConfig.monthlyWorkDays.toString()) }
    var workHoursText by remember(initialConfig) { mutableStateOf(initialConfig.dailyWorkHours.toPlainString()) }
    var breakMinutesText by remember(initialConfig) { mutableStateOf(initialConfig.breakMinutes.toString()) }
    var deductBreak by remember(initialConfig) { mutableStateOf(initialConfig.deductBreakFromSalary) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val validationResult = remember(salaryText, workDaysText, workHoursText, breakMinutesText, deductBreak) {
        SalaryConfigValidator.validate(
            salaryText = salaryText,
            workDaysText = workDaysText,
            workHoursText = workHoursText,
            breakMinutesText = breakMinutesText,
            deductBreak = deductBreak,
            currencySymbol = initialConfig.currencySymbol,
            currencyCode = initialConfig.currencyCode
        )
    }

    val previewConfig = remember(validationResult, salaryText, workDaysText, workHoursText, breakMinutesText, deductBreak) {
        if (validationResult is SalaryValidationResult.Valid) {
            SalaryConfigValidator.parseOrNull(
                salaryText = salaryText,
                workDaysText = workDaysText,
                workHoursText = workHoursText,
                breakMinutesText = breakMinutesText,
                deductBreak = deductBreak,
                currencySymbol = initialConfig.currencySymbol,
                currencyCode = initialConfig.currencyCode
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Validation Error Banner
            if (validationResult is SalaryValidationResult.Invalid) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationResult.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = deductBreak,
                    onCheckedChange = { deductBreak = it },
                    modifier = Modifier.semantics {
                        contentDescription = "Mola maaştan düşülsün mü seçeneği"
                    }
                )
                Text(
                    text = "Mola süresi ücretten kesilsin (ödenmeyen mola)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Live Preview Card
            if (previewConfig != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Hesaplanan Ücret Oranları",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Saatlik: ${previewConfig.currencySymbol}${rateFormat.format(previewConfig.hourlyRate.setScale(2, RoundingMode.HALF_UP))}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Dakikalık: ${previewConfig.currencySymbol}${rateFormat.format(previewConfig.minuteRate.setScale(4, RoundingMode.HALF_UP))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Saniyelik: ${previewConfig.currencySymbol}${secondRateFormat.format(previewConfig.secondRate.setScale(5, RoundingMode.HALF_UP))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (deductBreak) {
                            Text(
                                text = "Ücretli Günlük Çalışma: ${previewConfig.dailyPaidHours.setScale(1, RoundingMode.HALF_UP)} saat",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Action Button
            Button(
                onClick = {
                    if (previewConfig != null) {
                        viewModel.updateSalaryConfig(previewConfig)
                        scope.launch {
                            snackbarHostState.showSnackbar("Maaş ayarları başarıyla güncellendi.")
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
    }
}
