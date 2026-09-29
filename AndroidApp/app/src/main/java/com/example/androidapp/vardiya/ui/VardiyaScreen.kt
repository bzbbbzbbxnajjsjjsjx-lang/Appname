package com.example.androidapp.vardiya.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import com.example.androidapp.theme.motion.VardiyaTheme
import com.example.androidapp.vardiya.ui.components.StateBadgeMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import com.example.androidapp.vardiya.data.repository.LocalVardiyaRepository
import com.example.androidapp.vardiya.domain.model.ShiftState
import com.example.androidapp.vardiya.domain.model.ShiftTemplate
import com.example.androidapp.vardiya.ui.components.BreakManagementSheet
import com.example.androidapp.vardiya.ui.components.CircularWavyProgressHero
import com.example.androidapp.vardiya.ui.components.ShiftTemplatePicker
import com.example.androidapp.vardiya.ui.components.VardiyaControlBar
import com.example.androidapp.vardiya.ui.components.VardiyaHistoryDialog
import com.example.androidapp.vardiya.ui.components.VardiyaSetupDialog
import com.example.androidapp.vardiya.ui.navigation.WindowWidthSizeClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VardiyaScreen(
    modifier: Modifier = Modifier,
    onNavigateToCalculator: (() -> Unit)? = null,
    viewModel: VardiyaViewModel = run {
        val context = LocalContext.current
        viewModel { VardiyaViewModel(LocalVardiyaRepository(context)) }
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Vardiya",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                },
                actions = {
                    TextButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.openTemplatePicker()
                        }
                    ) {
                        Text("Şablon", style = MaterialTheme.typography.labelLarge)
                    }

                    TextButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.openHistory()
                        }
                    ) {
                        Text("Geçmiş", style = MaterialTheme.typography.labelLarge)
                    }

                    TextButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.openSetup()
                        }
                    ) {
                        Text("Maaş", style = MaterialTheme.typography.labelLarge)
                    }
                },
                navigationIcon = {
                    if (onNavigateToCalculator != null) {
                        TextButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onNavigateToCalculator()
                            }
                        ) {
                            Text("Hesap", style = MaterialTheme.typography.labelLarge)
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

            if (sizeClass.isCompact) {
                // Compact Screen (Phones): Vertical single-column layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top: State Badge (clickable for break management)
                    StateBadge(
                        shiftState = uiState.shiftState,
                        badgeText = uiState.stateBadgeText,
                        isBreakActive = uiState.isBreakActive,
                        onClick = if (uiState.isBreakActive || uiState.shiftState == ShiftState.RUNNING) {
                            {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.openBreakSheet()
                            }
                        } else null
                    )

                    // Template Badge / Quick Picker
                    TemplateSelectorChip(
                        selectedTemplate = uiState.selectedTemplate,
                        shiftState = uiState.shiftState,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.openTemplatePicker()
                        }
                    )

                    // Middle: Visual Hero with Circular Wavy Progress Indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        CircularWavyProgressHero(
                            progress = uiState.progress,
                            shiftState = uiState.shiftState,
                            heroAmountText = uiState.heroAmountText,
                            heroSubtitleText = uiState.heroSubtitleText,
                            durationText = if (uiState.shiftState != ShiftState.NOT_STARTED) {
                                uiState.earnings.formattedDuration
                            } else null,
                            isOvertimeActive = uiState.isOvertimeActive,
                            isBreakActive = uiState.isBreakActive,
                            breakDurationText = if (uiState.isBreakActive) uiState.activeBreakFormattedDuration else null,
                            isNightShiftActive = uiState.isNightShiftActive,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                    }

                    // Bottom: Material 3 Expressive Floating Control Bar
                    VardiyaControlBar(
                        shiftState = uiState.shiftState,
                        isBreakActive = uiState.isBreakActive,
                        onStart = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.startShift()
                        },
                        onPause = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.pauseShift()
                        },
                        onResume = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.resumeShift()
                        },
                        onToggleBreak = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.toggleBreak()
                        },
                        onFinish = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.finishShift()
                        },
                        onReset = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.resetShift()
                        }
                    )
                }
            } else {
                // Medium / Expanded (Foldable / Tablet / Large Screen): Responsive 2-column layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Visual hero + rates card
                    Column(
                        modifier = Modifier
                            .weight(0.48f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        StateBadge(
                            shiftState = uiState.shiftState,
                            badgeText = uiState.stateBadgeText,
                            isBreakActive = uiState.isBreakActive,
                            onClick = if (uiState.isBreakActive || uiState.shiftState == ShiftState.RUNNING) {
                                {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    viewModel.openBreakSheet()
                                }
                            } else null
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        TemplateSelectorChip(
                            selectedTemplate = uiState.selectedTemplate,
                            shiftState = uiState.shiftState,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.openTemplatePicker()
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CircularWavyProgressHero(
                            progress = uiState.progress,
                            shiftState = uiState.shiftState,
                            heroAmountText = uiState.heroAmountText,
                            heroSubtitleText = uiState.heroSubtitleText,
                            durationText = if (uiState.shiftState != ShiftState.NOT_STARTED) {
                                uiState.earnings.formattedDuration
                            } else null,
                            isOvertimeActive = uiState.isOvertimeActive,
                            isBreakActive = uiState.isBreakActive,
                            breakDurationText = if (uiState.isBreakActive) uiState.activeBreakFormattedDuration else null,
                            isNightShiftActive = uiState.isNightShiftActive,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                    }

                    // Right Column: Shift Status Overview & Control Center
                    Column(
                        modifier = Modifier
                            .weight(0.52f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth(0.92f)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Vardiya Kontrol Merkezi",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Vardiya Durumu:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(uiState.stateBadgeText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                if (uiState.shiftState != ShiftState.NOT_STARTED) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Aktif Süre:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(uiState.earnings.formattedDuration, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Kazanılan Tutar:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(uiState.heroAmountText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                if (uiState.isBreakActive) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Mola Süresi:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                                        Text(uiState.activeBreakFormattedDuration, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                                if (uiState.isOvertimeActive) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Fazla Mesai:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                        Text(uiState.earnings.formattedOvertimeEarned, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        VardiyaControlBar(
                            shiftState = uiState.shiftState,
                            isBreakActive = uiState.isBreakActive,
                            onStart = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.startShift()
                            },
                            onPause = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.pauseShift()
                            },
                            onResume = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.resumeShift()
                            },
                            onToggleBreak = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.toggleBreak()
                            },
                            onFinish = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.finishShift()
                            },
                            onReset = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.resetShift()
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs & Bottom Sheets
    if (uiState.isTemplatePickerVisible) {
        ShiftTemplatePicker(
            availableTemplates = uiState.availableTemplates,
            selectedTemplateId = uiState.selectedTemplateId,
            onSelectTemplate = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                viewModel.applyShiftTemplate(it)
            },
            onClearTemplate = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                viewModel.clearSelectedTemplate()
            },
            onDismiss = { viewModel.closeTemplatePicker() }
        )
    }

    if (uiState.isBreakSheetVisible) {
        BreakManagementSheet(
            isBreakActive = uiState.isBreakActive,
            activeBreakDurationFormatted = uiState.activeBreakFormattedDuration,
            ongoingBreak = uiState.ongoingBreak,
            breaks = uiState.currentShiftBreaks,
            isShiftRunning = uiState.shiftState == ShiftState.RUNNING,
            onStartBreak = { isDeducted, note ->
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                viewModel.startBreak(isDeducted, note)
            },
            onEndBreak = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                viewModel.endBreak()
            },
            onDismiss = { viewModel.closeBreakSheet() }
        )
    }

    if (uiState.isSetupVisible) {
        VardiyaSetupDialog(
            initialConfig = uiState.salaryConfig,
            onDismiss = { viewModel.closeSetup() },
            onSave = { viewModel.updateSalaryConfig(it) }
        )
    }

    if (uiState.isHistoryVisible) {
        VardiyaHistoryDialog(
            history = uiState.history,
            onDismiss = { viewModel.closeHistory() },
            onClearHistory = { viewModel.clearHistory() }
        )
    }
}

@Composable
private fun StateBadge(
    shiftState: ShiftState,
    badgeText: String,
    isBreakActive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val motionPreference = VardiyaTheme.motionPreference
    val isPulseActive = StateBadgeMotion.isPulseActive(shiftState, isBreakActive, motionPreference)

    val pulseAlpha = if (isPulseActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = StateBadgeMotion.PulseInitialAlpha,
            targetValue = StateBadgeMotion.PulseTargetAlpha,
            animationSpec = infiniteRepeatable(
                animation = StateBadgeMotion.pulseAnimationSpec(),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        alpha
    } else {
        StateBadgeMotion.PulseTargetAlpha
    }

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        color = when {
            shiftState == ShiftState.RUNNING && isBreakActive -> MaterialTheme.colorScheme.tertiaryContainer
            shiftState == ShiftState.RUNNING -> MaterialTheme.colorScheme.primaryContainer
            shiftState == ShiftState.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer
            shiftState == ShiftState.FINISHED -> MaterialTheme.colorScheme.secondaryContainer
            shiftState == ShiftState.NOT_STARTED -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        shape = RoundedCornerShape(percent = 50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (shiftState == ShiftState.RUNNING) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isBreakActive) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary
                        )
                        .alpha(pulseAlpha)
                )
            } else if (shiftState == ShiftState.PAUSED) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }

            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = when {
                    shiftState == ShiftState.RUNNING && isBreakActive -> MaterialTheme.colorScheme.onTertiaryContainer
                    shiftState == ShiftState.RUNNING -> MaterialTheme.colorScheme.onPrimaryContainer
                    shiftState == ShiftState.PAUSED -> MaterialTheme.colorScheme.onTertiaryContainer
                    shiftState == ShiftState.FINISHED -> MaterialTheme.colorScheme.onSecondaryContainer
                    shiftState == ShiftState.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun TemplateSelectorChip(
    selectedTemplate: ShiftTemplate?,
    shiftState: ShiftState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedTemplate != null) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(selectedTemplate.colorTag))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedTemplate.name,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    } else if (shiftState == ShiftState.NOT_STARTED) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
            modifier = modifier
        ) {
            Text(
                text = "+ Şablon Seç",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}
