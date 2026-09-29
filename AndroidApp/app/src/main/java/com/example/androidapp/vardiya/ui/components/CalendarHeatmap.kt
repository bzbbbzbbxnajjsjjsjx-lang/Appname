package com.example.androidapp.vardiya.ui.components

import androidx.compose.animation.animateColorAsState
import com.example.androidapp.theme.motion.VardiyaTheme
import com.example.androidapp.theme.motion.contract.DefaultVardiyaSelectionMotionContract
import com.example.androidapp.theme.motion.contract.SelectionSemanticState
import com.example.androidapp.theme.motion.contract.VardiyaSelectionMotionContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidapp.theme.VardiyaIcons
import com.example.androidapp.vardiya.domain.model.CalendarDate
import com.example.androidapp.vardiya.domain.model.DayShiftSummary
import java.util.Calendar
import java.util.Locale

private val WEEKDAY_NAMES_TR = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

/**
 * Material 3 Expressive Calendar Heatmap.
 * Renders an interactive monthly calendar where cell background intensity
 * corresponds to total shift duration and overtime.
 */
@Composable
fun CalendarHeatmap(
    displayedYear: Int,
    displayedMonth: Int, // 1..12
    monthData: Map<Int, DayShiftSummary>,
    selectedDate: CalendarDate?,
    onSelectDate: (CalendarDate?) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val turkishLocale = remember { Locale.forLanguageTag("tr-TR") }

    val monthTitle = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, turkishLocale) ?: "$displayedMonth"
        "$monthName $displayedYear"
    }

    // Days calculation for current month
    val daysInMonth = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Leading offset (Monday = 0, ..., Sunday = 6)
    val leadingEmptyDays = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        (dayOfWeek - Calendar.MONDAY + 7) % 7
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Month Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.semantics { contentDescription = "Önceki ay" }
                ) {
                    Icon(
                        imageVector = VardiyaIcons.ChevronLeft,
                        contentDescription = "Önceki ay",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.semantics { contentDescription = "Sonraki ay" }
                ) {
                    Icon(
                        imageVector = VardiyaIcons.ChevronRight,
                        contentDescription = "Sonraki ay",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weekday Headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WEEKDAY_NAMES_TR.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Month Grid
            val totalCells = leadingEmptyDays + daysInMonth
            val totalRows = (totalCells + 6) / 7

            for (row in 0 until totalRows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - leadingEmptyDays + 1

                        if (dayNumber in 1..daysInMonth) {
                            val currentDate = CalendarDate(displayedYear, displayedMonth, dayNumber)
                            val isSelected = selectedDate == currentDate
                            val summary = monthData[dayNumber]

                            CalendarDayCell(
                                dayNumber = dayNumber,
                                summary = summary,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        onSelectDate(null)
                                    } else {
                                        onSelectDate(currentDate)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // Empty placeholder cell
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Heatmap Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Yoğunluk:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = MaterialTheme.colorScheme.surfaceContainerHighest, label = "0s")
                    LegendItem(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), label = "<5s")
                    LegendItem(color = MaterialTheme.colorScheme.primaryContainer, label = "5-8s")
                    LegendItem(color = MaterialTheme.colorScheme.primary, label = "8s+")
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    summary: DayShiftSummary?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectionContract: VardiyaSelectionMotionContract = DefaultVardiyaSelectionMotionContract
    val semanticSelection = selectionContract.resolveSemanticState(isSelected)
    val motionScheme = VardiyaTheme.motionScheme

    val heatIntensity = summary?.heatIntensity ?: 0

    val targetContainerColor = when (heatIntensity) {
        3 -> MaterialTheme.colorScheme.primary
        2 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
    }

    val animatedColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = selectionContract.resolveColorSpec(motionScheme),
        label = "cellColor"
    )

    val textColor = when (heatIntensity) {
        3 -> MaterialTheme.colorScheme.onPrimary
        2 -> MaterialTheme.colorScheme.onPrimaryContainer
        1 -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderWidth = selectionContract.resolveBorderWidth(semanticSelection)
    val borderStroke = if (borderWidth > 0.dp) {
        BorderStroke(borderWidth, MaterialTheme.colorScheme.primary)
    } else {
        null
    }

    val motionPreference = VardiyaTheme.motionPreference
    val isReducedMotion = motionPreference == com.example.androidapp.theme.motion.MotionPreference.REDUCED

    val animatedCornerRadius by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isSelected && !isReducedMotion) 14.dp else 10.dp,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "cellCornerRadius"
    )

    val animatedCellScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isSelected && !isReducedMotion) 1.08f else 1.0f,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "cellScale"
    )

    val cellDescription = buildString {
        append("$dayNumber ")
        if (summary != null && summary.shiftCount > 0) {
            append("${summary.shiftCount} vardiya, ")
            val hours = String.format(Locale.US, "%.1f", summary.totalHours)
            append("$hours saat çalışma")
            if (summary.hasOvertime) append(", fazla mesai var")
            if (summary.hasNightShift) append(", gece vardiyası var")
        } else {
            append("vardiya yok")
        }
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = animatedCellScale
                scaleY = animatedCellScale
            }
            .semantics {
                contentDescription = cellDescription
                this.selected = isSelected
            },
        shape = RoundedCornerShape(animatedCornerRadius),
        color = animatedColor,
        border = borderStroke
    ) {
        Box(
            modifier = Modifier.padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$dayNumber",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    fontWeight = if (summary != null || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )

                if (summary != null && summary.shiftCount > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dot indicator
                        val dotColor = if (summary.hasOvertime) {
                            if (heatIntensity == 3) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.error
                        } else {
                            if (heatIntensity == 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
