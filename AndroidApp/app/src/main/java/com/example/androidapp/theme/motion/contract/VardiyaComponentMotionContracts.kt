package com.example.androidapp.theme.motion.contract

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaMotionScheme
import com.example.androidapp.vardiya.domain.model.ShiftState
import com.example.androidapp.vardiya.ui.components.ControlBarLayoutConfig

// ============================================================================
// A. BUTTON / ACTION CONTROL CONTRACT
// ============================================================================

/**
 * Semantic states for tactile action buttons.
 */
enum class ButtonSemanticState {
    RESTING,
    PRESSED,
    DISABLED
}

/**
 * Contract governing tactile feedback and scale transitions for action buttons.
 *
 * UPSTREAM MATERIAL EXPRESSIVE PRINCIPLE:
 * Material 3 buttons express direct manipulation via spring-driven scale depression
 * (spatial spec) and instantaneous or critically-damped feedback (effects spec).
 */
interface VardiyaButtonMotionContract {
    fun resolveSemanticState(isPressed: Boolean, isEnabled: Boolean): ButtonSemanticState
    fun <T> resolveScaleSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T>
    fun <T> resolveContentAlphaSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T>
    fun resolveScale(semanticState: ButtonSemanticState): Float
    fun resolvePressedScale(): Float
}

object DefaultVardiyaButtonMotionContract : VardiyaButtonMotionContract {
    // VARDIYA DESIGN DECISION:
    // 0.94f pressed scale provides responsive tactile depression for primary actions.
    const val VARDIYA_BUTTON_PRESSED_SCALE = 0.94f
    const val VARDIYA_BUTTON_RESTING_SCALE = 1.0f
    const val VARDIYA_BUTTON_DISABLED_SCALE = 1.0f

    override fun resolveSemanticState(isPressed: Boolean, isEnabled: Boolean): ButtonSemanticState = when {
        !isEnabled -> ButtonSemanticState.DISABLED
        isPressed -> ButtonSemanticState.PRESSED
        else -> ButtonSemanticState.RESTING
    }

    override fun <T> resolveScaleSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T> =
        motionScheme.fastSpatialSpec()

    override fun <T> resolveContentAlphaSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T> =
        motionScheme.fastEffectsSpec()

    override fun resolveScale(semanticState: ButtonSemanticState): Float = when (semanticState) {
        ButtonSemanticState.PRESSED -> VARDIYA_BUTTON_PRESSED_SCALE
        ButtonSemanticState.RESTING, ButtonSemanticState.DISABLED -> VARDIYA_BUTTON_RESTING_SCALE
    }

    override fun resolvePressedScale(): Float = VARDIYA_BUTTON_PRESSED_SCALE
}

// ============================================================================
// B. SELECTION CONTROL CONTRACT
// ============================================================================

/**
 * Semantic states for selection indicators and selectable surfaces.
 */
enum class SelectionSemanticState {
    UNSELECTED,
    SELECTED
}

/**
 * Contract governing selection indicators (e.g. tabs, filter chips, calendar day cells).
 *
 * UPSTREAM MATERIAL EXPRESSIVE PRINCIPLE:
 * Selection changes trigger spatial bounds adjustments paired with critically-damped
 * tonal container color shifts.
 */
interface VardiyaSelectionMotionContract {
    fun resolveSemanticState(isSelected: Boolean): SelectionSemanticState
    fun <T> resolveSelectionSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T>
    fun <T> resolveColorSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T>
    fun resolveBorderWidth(semanticState: SelectionSemanticState): Dp
}

object DefaultVardiyaSelectionMotionContract : VardiyaSelectionMotionContract {
    // VARDIYA DESIGN DECISION:
    // 2.dp border highlight on selected calendar cell / selection surfaces preserves
    // clear contrast against heatmaps without layout displacement.
    val BORDER_WIDTH_SELECTED = 2.dp
    val BORDER_WIDTH_UNSELECTED = 0.dp

    override fun resolveSemanticState(isSelected: Boolean): SelectionSemanticState =
        if (isSelected) SelectionSemanticState.SELECTED else SelectionSemanticState.UNSELECTED

    override fun <T> resolveSelectionSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T> =
        motionScheme.fastSpatialSpec()

    override fun <T> resolveColorSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T> =
        motionScheme.fastEffectsSpec()

    override fun resolveBorderWidth(semanticState: SelectionSemanticState): Dp = when (semanticState) {
        SelectionSemanticState.SELECTED -> BORDER_WIDTH_SELECTED
        SelectionSemanticState.UNSELECTED -> BORDER_WIDTH_UNSELECTED
    }
}

// ============================================================================
// C. EXPANDABLE CONTAINER CONTRACT
// ============================================================================

/**
 * Semantic states for expandable containers.
 */
enum class ExpandableSemanticState {
    COLLAPSED,
    EXPANDED
}

/**
 * Contract governing expandable containers (e.g. settings accordions, breakdown drawers).
 *
 * UPSTREAM MATERIAL EXPRESSIVE PRINCIPLE:
 * Container transformations coordinate spatial expansion with effects opacity fades.
 */
interface VardiyaExpandableContainerMotionContract {
    fun resolveSemanticState(isExpanded: Boolean): ExpandableSemanticState
    fun <T> resolveContainerSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T>
    fun <T> resolveContentFadeSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T>
    fun createEnterTransition(motionScheme: VardiyaMotionScheme, motionPreference: MotionPreference): EnterTransition
    fun createExitTransition(motionScheme: VardiyaMotionScheme, motionPreference: MotionPreference): ExitTransition
}

object DefaultVardiyaExpandableContainerMotionContract : VardiyaExpandableContainerMotionContract {
    override fun resolveSemanticState(isExpanded: Boolean): ExpandableSemanticState =
        if (isExpanded) ExpandableSemanticState.EXPANDED else ExpandableSemanticState.COLLAPSED

    override fun <T> resolveContainerSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T> =
        motionScheme.defaultSpatialSpec()

    override fun <T> resolveContentFadeSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T> =
        motionScheme.defaultEffectsSpec()

    override fun createEnterTransition(
        motionScheme: VardiyaMotionScheme,
        motionPreference: MotionPreference
    ): EnterTransition =
        if (motionPreference == MotionPreference.REDUCED) {
            fadeIn(animationSpec = motionScheme.defaultEffectsSpec())
        } else {
            expandVertically(animationSpec = motionScheme.defaultSpatialSpec()) +
                fadeIn(animationSpec = motionScheme.defaultEffectsSpec())
        }

    override fun createExitTransition(
        motionScheme: VardiyaMotionScheme,
        motionPreference: MotionPreference
    ): ExitTransition =
        if (motionPreference == MotionPreference.REDUCED) {
            fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
        } else {
            shrinkVertically(animationSpec = motionScheme.defaultSpatialSpec()) +
                fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
        }
}

// ============================================================================
// D. STATUS / INDICATOR COMPONENT CONTRACT
// ============================================================================

/**
 * Contract governing status indicator components (e.g. StateBadge).
 *
 * UPSTREAM MATERIAL EXPRESSIVE PRINCIPLE:
 * Continuous ambient motion must be strictly bound to active lifecycle states and
 * entirely suppressed during idle, paused, or reduced-motion conditions.
 */
interface VardiyaIndicatorMotionContract {
    fun isContinuousPulseActive(
        shiftState: ShiftState,
        isBreakActive: Boolean,
        motionPreference: MotionPreference
    ): Boolean

    fun <T> resolveTransitionSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T>
}

object DefaultVardiyaIndicatorMotionContract : VardiyaIndicatorMotionContract {
    override fun isContinuousPulseActive(
        shiftState: ShiftState,
        isBreakActive: Boolean,
        motionPreference: MotionPreference
    ): Boolean =
        motionPreference != MotionPreference.REDUCED &&
        shiftState == ShiftState.RUNNING &&
        !isBreakActive

    override fun <T> resolveTransitionSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T> =
        motionScheme.fastEffectsSpec()
}

// ============================================================================
// E. HERO / PROGRESS COMPONENT CONTRACT
// ============================================================================

/**
 * Semantic states for the Vardiya Hero Wavy Progress element.
 *
 * UPSTREAM MATERIAL EXPRESSIVE PRINCIPLE:
 * Wavy progress indicators communicate lifecycle and workload intensity through
 * geometry (amplitude) and animation activity (traveling wave phase).
 *
 * VARDIYA IMPLEMENTATION DECISION:
 * We map shift tracking domain states into 6 discrete semantic hero states:
 * NOT_STARTED, RUNNING, PAUSED, BREAK, OVERTIME, FINISHED.
 */
enum class HeroSemanticState {
    NOT_STARTED,
    RUNNING,
    PAUSED,
    BREAK,
    OVERTIME,
    FINISHED
}

interface VardiyaHeroMotionContract {
    fun resolveSemanticState(
        shiftState: ShiftState,
        isOvertimeActive: Boolean,
        isBreakActive: Boolean
    ): HeroSemanticState

    fun resolveTargetAmplitude(semanticState: HeroSemanticState): Dp

    fun isWavePhaseActive(
        semanticState: HeroSemanticState,
        motionPreference: MotionPreference
    ): Boolean

    fun resolveWaveCycleDurationMs(semanticState: HeroSemanticState): Long

    fun resolveHeroActiveColor(
        semanticState: HeroSemanticState,
        colorScheme: ColorScheme
    ): Color

    fun resolveContainerScale(semanticState: HeroSemanticState): Float

    fun <T> resolveProgressSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T>
    fun <T> resolveAmplitudeSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T>
    fun <T> resolveColorSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T>
}

object DefaultVardiyaHeroMotionContract : VardiyaHeroMotionContract {
    // ========================================================================
    // VARDIYA DESIGN DECISIONS (Preserving established v3.0.2 production geometry)
    // ========================================================================
    val AMPLITUDE_NOT_STARTED = 5.0.dp
    val AMPLITUDE_RUNNING = 6.0.dp
    val AMPLITUDE_PAUSED = 4.5.dp
    val AMPLITUDE_BREAK = 5.0.dp
    val AMPLITUDE_OVERTIME = 7.0.dp
    val AMPLITUDE_FINISHED = 5.0.dp

    const val CYCLE_DURATION_NORMAL_MS = 2400L
    const val CYCLE_DURATION_OVERTIME_MS = 1800L

    override fun resolveSemanticState(
        shiftState: ShiftState,
        isOvertimeActive: Boolean,
        isBreakActive: Boolean
    ): HeroSemanticState = when {
        isBreakActive -> HeroSemanticState.BREAK
        isOvertimeActive -> HeroSemanticState.OVERTIME
        shiftState == ShiftState.RUNNING -> HeroSemanticState.RUNNING
        shiftState == ShiftState.PAUSED -> HeroSemanticState.PAUSED
        shiftState == ShiftState.FINISHED -> HeroSemanticState.FINISHED
        else -> HeroSemanticState.NOT_STARTED
    }

    override fun resolveTargetAmplitude(semanticState: HeroSemanticState): Dp = when (semanticState) {
        HeroSemanticState.BREAK -> AMPLITUDE_BREAK
        HeroSemanticState.OVERTIME -> AMPLITUDE_OVERTIME
        HeroSemanticState.RUNNING -> AMPLITUDE_RUNNING
        HeroSemanticState.PAUSED -> AMPLITUDE_PAUSED
        HeroSemanticState.FINISHED -> AMPLITUDE_FINISHED
        HeroSemanticState.NOT_STARTED -> AMPLITUDE_NOT_STARTED
    }

    override fun isWavePhaseActive(
        semanticState: HeroSemanticState,
        motionPreference: MotionPreference
    ): Boolean {
        // UPSTREAM MATERIAL EXPRESSIVE PRINCIPLE:
        // Motion preference REDUCED suppresses continuous cyclic motion to prevent vestibular discomfort.
        if (motionPreference == MotionPreference.REDUCED) return false

        // VARDIYA IMPLEMENTATION DECISION:
        // Wave travels only during active work without an ongoing break.
        return semanticState == HeroSemanticState.RUNNING || semanticState == HeroSemanticState.OVERTIME
    }

    override fun resolveWaveCycleDurationMs(semanticState: HeroSemanticState): Long =
        if (semanticState == HeroSemanticState.OVERTIME) CYCLE_DURATION_OVERTIME_MS else CYCLE_DURATION_NORMAL_MS

    override fun resolveHeroActiveColor(
        semanticState: HeroSemanticState,
        colorScheme: ColorScheme
    ): Color = when (semanticState) {
        HeroSemanticState.FINISHED -> colorScheme.secondary
        HeroSemanticState.PAUSED, HeroSemanticState.BREAK, HeroSemanticState.OVERTIME -> colorScheme.tertiary
        HeroSemanticState.RUNNING, HeroSemanticState.NOT_STARTED -> colorScheme.primary
    }

    override fun resolveContainerScale(semanticState: HeroSemanticState): Float = when (semanticState) {
        HeroSemanticState.NOT_STARTED -> 0.96f
        HeroSemanticState.PAUSED -> 0.97f
        HeroSemanticState.BREAK -> 0.98f
        HeroSemanticState.FINISHED -> 1.00f
        HeroSemanticState.RUNNING -> 1.00f
        HeroSemanticState.OVERTIME -> 1.03f
    }

    override fun <T> resolveProgressSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T> =
        motionScheme.defaultSpatialSpec()

    override fun <T> resolveAmplitudeSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T> =
        motionScheme.defaultSpatialSpec()

    override fun <T> resolveColorSpec(motionScheme: VardiyaMotionScheme): FiniteAnimationSpec<T> =
        motionScheme.defaultEffectsSpec()
}

// ============================================================================
// F. CONTROLBAR CONTRACT
// ============================================================================

/**
 * Semantic states for the Vardiya Floating Control Bar.
 *
 * UPSTREAM MATERIAL EXPRESSIVE PRINCIPLE:
 * Floating action toolbars morph between layout configurations based on current primary task context.
 *
 * VARDIYA IMPLEMENTATION DECISION:
 * We map shift states into 5 semantic control states:
 * START, RUNNING, PAUSED, BREAK, FINISHED.
 */
enum class ControlBarSemanticState {
    START,
    RUNNING,
    PAUSED,
    BREAK,
    FINISHED
}

interface VardiyaControlBarMotionContract {
    fun resolveSemanticState(
        shiftState: ShiftState,
        isBreakActive: Boolean
    ): ControlBarSemanticState

    fun resolveLayoutConfig(semanticState: ControlBarSemanticState): ControlBarLayoutConfig

    fun createTransition(
        motionScheme: VardiyaMotionScheme,
        motionPreference: MotionPreference
    ): ContentTransform
}

object DefaultVardiyaControlBarMotionContract : VardiyaControlBarMotionContract {
    // ========================================================================
    // VARDIYA DESIGN DECISIONS (Preserving established v3.0.3 motion scale)
    // ========================================================================
    const val VARDIYA_CONTROL_BAR_INITIAL_SCALE = 0.96f

    override fun resolveSemanticState(
        shiftState: ShiftState,
        isBreakActive: Boolean
    ): ControlBarSemanticState = when (shiftState) {
        ShiftState.NOT_STARTED -> ControlBarSemanticState.START
        ShiftState.RUNNING -> if (isBreakActive) ControlBarSemanticState.BREAK else ControlBarSemanticState.RUNNING
        ShiftState.PAUSED -> ControlBarSemanticState.PAUSED
        ShiftState.FINISHED -> ControlBarSemanticState.FINISHED
    }

    override fun resolveLayoutConfig(semanticState: ControlBarSemanticState): ControlBarLayoutConfig =
        when (semanticState) {
            ControlBarSemanticState.START -> ControlBarLayoutConfig.START_ONLY
            ControlBarSemanticState.RUNNING -> ControlBarLayoutConfig.ACTIVE_CONTROLS
            ControlBarSemanticState.BREAK -> ControlBarLayoutConfig.BREAK_CONTROLS
            ControlBarSemanticState.PAUSED -> ControlBarLayoutConfig.PAUSED_CONTROLS
            ControlBarSemanticState.FINISHED -> ControlBarLayoutConfig.RESET_ONLY
        }

    override fun createTransition(
        motionScheme: VardiyaMotionScheme,
        motionPreference: MotionPreference
    ): ContentTransform {
        return if (motionPreference == MotionPreference.REDUCED) {
            ContentTransform(
                targetContentEnter = fadeIn(animationSpec = motionScheme.defaultEffectsSpec()),
                initialContentExit = fadeOut(animationSpec = motionScheme.defaultEffectsSpec()),
                sizeTransform = null
            )
        } else {
            ContentTransform(
                targetContentEnter = fadeIn(animationSpec = motionScheme.fastEffectsSpec()) +
                    scaleIn(
                        initialScale = VARDIYA_CONTROL_BAR_INITIAL_SCALE,
                        animationSpec = motionScheme.fastSpatialSpec()
                    ),
                initialContentExit = fadeOut(animationSpec = motionScheme.fastEffectsSpec()) +
                    scaleOut(
                        targetScale = VARDIYA_CONTROL_BAR_INITIAL_SCALE,
                        animationSpec = motionScheme.fastSpatialSpec()
                    ),
                sizeTransform = SizeTransform(
                    clip = true,
                    sizeAnimationSpec = { _, _ -> motionScheme.fastSpatialSpec() }
                )
            )
        }
    }
}
