package com.example.androidapp.vardiya.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.example.androidapp.theme.VardiyaIcons
import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaMotionScheme
import com.example.androidapp.theme.motion.VardiyaTheme
import com.example.androidapp.theme.motion.contract.ControlBarSemanticState
import com.example.androidapp.theme.motion.contract.DefaultVardiyaControlBarMotionContract
import com.example.androidapp.theme.motion.contract.VardiyaControlBarMotionContract
import com.example.androidapp.vardiya.domain.model.ShiftState

/**
 * Semantic layout configurations for the Vardiya Control Bar.
 */
enum class ControlBarLayoutConfig {
    START_ONLY,        // 1 button: Vardiyayı Başlat
    ACTIVE_CONTROLS,   // 3 buttons: Duraklat, Mola, Bitir
    BREAK_CONTROLS,    // 2 buttons: Molayı Bitir, Bitir
    PAUSED_CONTROLS,   // 2 buttons: Devam Et, Bitir
    RESET_ONLY         // 1 button: Yeni Vardiya
}

/**
 * Pure motion and layout mapping for [VardiyaControlBar].
 * Driven by [VardiyaControlBarMotionContract] to separate semantic state decisions
 * from UI composable layout.
 */
object VardiyaControlBarMotion {
    val contract: VardiyaControlBarMotionContract = DefaultVardiyaControlBarMotionContract

    fun resolveSemanticState(shiftState: ShiftState, isBreakActive: Boolean): ControlBarSemanticState =
        contract.resolveSemanticState(shiftState, isBreakActive)

    fun resolveLayoutConfig(shiftState: ShiftState, isBreakActive: Boolean): ControlBarLayoutConfig =
        contract.resolveLayoutConfig(contract.resolveSemanticState(shiftState, isBreakActive))

    fun createTransition(
        motionScheme: VardiyaMotionScheme,
        motionPreference: MotionPreference
    ): ContentTransform = contract.createTransition(motionScheme, motionPreference)
}

/**
 * Modern Material 3 Expressive Floating Control Bar for Vardiya 3.0.
 *
 * Implements one-touch rapid state transitions:
 * - Start / Pause / Resume / Finish
 * - Fast Break toggle (Mola Ver / Molayı Bitir)
 * - Tonal feedback, accessibility semantics, and haptic response.
 * - Single-line text layout preventing awkward syllable/word breaks on all screen sizes.
 * - Coordinated Expressive motion with fast spatial spring morphing and effects fade.
 * - Zero spatial overshoot under reduced motion.
 */
@Composable
fun VardiyaControlBar(
    shiftState: ShiftState,
    isBreakActive: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onToggleBreak: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val buttonHeight = 56.dp
    val pillShape = RoundedCornerShape(percent = 50)
    val motionScheme = VardiyaTheme.motionScheme
    val motionPreference = VardiyaTheme.motionPreference
    val contract = VardiyaControlBarMotion.contract

    val semanticState = remember(shiftState, isBreakActive) {
        contract.resolveSemanticState(shiftState, isBreakActive)
    }
    val layoutConfig = contract.resolveLayoutConfig(semanticState)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        AnimatedContent(
            targetState = layoutConfig,
            transitionSpec = {
                contract.createTransition(motionScheme, motionPreference)
            },
            label = "control_bar_state_anim",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
        ) { config ->
            when (config) {
                ControlBarLayoutConfig.START_ONLY -> {
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onStart()
                        },
                        shape = pillShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight)
                            .semantics { contentDescription = "Vardiyayı başlat" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = VardiyaIcons.Play,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "VARDİYAYI BAŞLAT",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                ControlBarLayoutConfig.ACTIVE_CONTROLS -> {
                    // Normal active: Pause, Quick Break, Finish (single-line, no text breaking)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onPause()
                            },
                            shape = pillShape,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1.15f)
                                .height(buttonHeight)
                                .semantics { contentDescription = "Vardiyayı duraklat" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = VardiyaIcons.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "DURAKLAT",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.2).sp
                                )
                            )
                        }

                        Button(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onToggleBreak()
                            },
                            shape = pillShape,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(buttonHeight)
                                .semantics { contentDescription = "Mola başlat" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = VardiyaIcons.Coffee,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MOLA",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.sp
                                )
                            )
                        }

                        Button(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onFinish()
                            },
                            shape = pillShape,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(0.95f)
                                .height(buttonHeight)
                                .semantics { contentDescription = "Vardiyayı bitir" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                imageVector = VardiyaIcons.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "BİTİR",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.sp
                                )
                            )
                        }
                    }
                }

                ControlBarLayoutConfig.BREAK_CONTROLS -> {
                    // In break: primary option is "Molayı Bitir", secondary is "Bitir"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onToggleBreak()
                            },
                            shape = pillShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(buttonHeight)
                                .semantics { contentDescription = "Molayı bitir ve çalışmaya dön" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = VardiyaIcons.Play,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MOLAYI BİTİR",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Button(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onFinish()
                            },
                            shape = pillShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(buttonHeight)
                                .semantics { contentDescription = "Vardiyayı bitir" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                imageVector = VardiyaIcons.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BİTİR",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                ControlBarLayoutConfig.PAUSED_CONTROLS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onResume()
                            },
                            shape = pillShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(buttonHeight)
                                .semantics { contentDescription = "Vardiyayı devam ettir" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = VardiyaIcons.Play,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DEVAM ET",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Button(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onFinish()
                            },
                            shape = pillShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(buttonHeight)
                                .semantics { contentDescription = "Vardiyayı bitir" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                imageVector = VardiyaIcons.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BİTİR",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                ControlBarLayoutConfig.RESET_ONLY -> {
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onReset()
                        },
                        shape = pillShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight)
                            .semantics { contentDescription = "Yeni vardiyaya hazırlan" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = VardiyaIcons.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "YENİ VARDİYA",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
