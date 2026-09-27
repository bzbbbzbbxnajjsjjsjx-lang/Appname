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

    // Live preview computation
    val previewConfig = remember(salaryText, workDaysText, workHoursText, breakMinutesText, deductBreak) {
        try {
            val salary = BigDecimal(salaryText.replace(",", ".").trim()).coerceAtLeast(BigDecimal("1"))
            val days = workDaysText.trim().toIntOrNull()?.coerceAtLeast(1) ?: 22
            val hours = BigDecimal(workHoursText.replace(",", ".").trim()).coerceAtLeast(BigDecimal("0.5"))
            val breakMin = breakMinutesText.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
            SalaryConfiguration(
                monthlySalary = salary,
                monthlyWorkDays = days,
                dailyWorkHours = hours,
                breakMinutes = breakMin,
                deductBreakFromSalary = deductBreak,
                currencySymbol = initialConfig.currencySymbol
            )
        } catch (e: Exception) {
            null
        }
    }

    val turkishSymbols = remember {
        DecimalFormatSymbols(Locale("tr", "TR")).apply {
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
                OutlinedTextField(
                    value = salaryText,
                    onValueChange = { salaryText = it },
                    label = { Text("Aylık Maaş (${initialConfig.currencySymbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
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
                        modifier = Modifier.semantics { contentDescription = "Mola çalışma süresinden düşülsün seçeneği" }
                    )
                    Text(
                        text = "Mola süresi çalışma saatinden düşülsün",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Live rate preview card
                if (previewConfig != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Hesaplanan Ücret:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Saatlik: ${rateFormat.format(previewConfig.hourlyRate.setScale(2, RoundingMode.HALF_UP))} ${previewConfig.currencySymbol}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Saniyelik: ${secondRateFormat.format(previewConfig.secondRate.setScale(5, RoundingMode.HALF_UP))} ${previewConfig.currencySymbol}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    previewConfig?.let { onSave(it) }
                },
                enabled = previewConfig != null
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
