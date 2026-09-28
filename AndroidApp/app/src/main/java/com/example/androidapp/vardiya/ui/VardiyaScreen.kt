package com.example.androidapp.vardiya.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import com.example.androidapp.vardiya.data.repository.LocalVardiyaRepository
import com.example.androidapp.vardiya.domain.model.ShiftState
import com.example.androidapp.vardiya.ui.components.CircularWavyProgressHero
import com.example.androidapp.vardiya.ui.components.VardiyaHistoryDialog
import com.example.androidapp.vardiya.ui.components.VardiyaSetupDialog

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: State Badge
            StateBadge(shiftState = uiState.shiftState, badgeText = uiState.stateBadgeText)

            // Middle: Visual Hero with Circular Wavy Progress Indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Organic Wavy Circular Progress Hero
                CircularWavyProgressHero(
                    progress = uiState.progress,
                    shiftState = uiState.shiftState,
                    heroAmountText = uiState.heroAmountText,
                    heroSubtitleText = uiState.heroSubtitleText,
                    durationText = if (uiState.shiftState != ShiftState.NOT_STARTED) {
                        uiState.earnings.formattedDuration
                    } else null,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Rates breakdown card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = uiState.earnings.formattedHourlyRate,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = uiState.earnings.formattedMinuteRate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = uiState.earnings.formattedSecondRate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Bottom: Action Buttons
            ActionButtons(
                shiftState = uiState.shiftState,
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

    // Dialogs
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
private fun StateBadge(shiftState: ShiftState, badgeText: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        color = when (shiftState) {
            ShiftState.RUNNING -> MaterialTheme.colorScheme.primaryContainer
            ShiftState.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer
            ShiftState.FINISHED -> MaterialTheme.colorScheme.secondaryContainer
            ShiftState.NOT_STARTED -> MaterialTheme.colorScheme.surfaceContainerHigh
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
                        .background(MaterialTheme.colorScheme.primary)
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
                color = when (shiftState) {
                    ShiftState.RUNNING -> MaterialTheme.colorScheme.onPrimaryContainer
                    ShiftState.PAUSED -> MaterialTheme.colorScheme.onTertiaryContainer
                    ShiftState.FINISHED -> MaterialTheme.colorScheme.onSecondaryContainer
                    ShiftState.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun ActionButtons(
    shiftState: ShiftState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit
) {
    val buttonHeight = 64.dp
    val buttonShape = RoundedCornerShape(percent = 50)

    when (shiftState) {
        ShiftState.NOT_STARTED -> {
            Button(
                onClick = onStart,
                shape = buttonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight)
                    .semantics { contentDescription = "Vardiyayı başlat" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "VARDİYAYI BAŞLAT",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
        ShiftState.RUNNING -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPause,
                    shape = buttonShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight)
                        .semantics { contentDescription = "Vardiyayı duraklat" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        text = "DURAKLAT",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Button(
                    onClick = onFinish,
                    shape = buttonShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight)
                        .semantics { contentDescription = "Vardiyayı bitir" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(
                        text = "BİTİR",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
        ShiftState.PAUSED -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onResume,
                    shape = buttonShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight)
                        .semantics { contentDescription = "Vardiyayı devam ettir" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "DEVAM ET",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Button(
                    onClick = onFinish,
                    shape = buttonShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight)
                        .semantics { contentDescription = "Vardiyayı bitir" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(
                        text = "BİTİR",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
        ShiftState.FINISHED -> {
            Button(
                onClick = onReset,
                shape = buttonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight)
                    .semantics { contentDescription = "Yeni vardiyaya hazırlan" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "YENİ VARDİYA",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}
