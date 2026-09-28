package com.example.androidapp.vardiya.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.validator.SalaryConfigValidator
import com.example.androidapp.vardiya.domain.validator.SalaryValidationResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun VardiyaSetupDialog(
    initialConfig: SalaryConfiguration,
    onDismiss: () -> Unit,
    onSave: (SalaryConfiguration) -> Unit
) {
    var salaryText by remember { mutableStateOf(initialConfig.monthlySalary.toPlainString()) }
    var workDaysText by remember { mutableStateOf(initialConfig.monthlyWorkDays.toString()) }
    var workHoursText by remember { mutableStateOf(initialConfig.dailyWorkHours.toPlainString()) }
    var breakMinutesText by remember { mutableStateOf(initialConfig.breakMinutes.toString()) }
    var deductBreak by remember { mutableStateOf(initialConfig.deductBreakFromSalary) }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Maaş & Çalışma Düzeni",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Validation error banner if any
                if (validationResult is SalaryValidationResult.Invalid) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationResult.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
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
                        .padding(top = 4.dp),
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Önizleme Oranları:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Saatlik: ${previewConfig.currencySymbol}${rateFormat.format(previewConfig.hourlyRate.setScale(2, RoundingMode.HALF_UP))}",
                                style = MaterialTheme.typography.bodySmall,
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (previewConfig != null) {
                        onSave(previewConfig)
                    }
                },
                enabled = validationResult is SalaryValidationResult.Valid && previewConfig != null
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
