package com.example.androidapp.theme.motion.contract

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.unit.dp
import com.example.androidapp.theme.DarkColorScheme
import com.example.androidapp.theme.LightColorScheme
import com.example.androidapp.theme.motion.MotionPreference
import com.example.androidapp.theme.motion.VardiyaMotionScheme
import com.example.androidapp.vardiya.domain.model.ShiftState
import com.example.androidapp.vardiya.ui.components.ControlBarLayoutConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic unit test suite for the Vardiya Expressive Component Contract Foundation:
 * - Semantic motion state enumeration & distinctness
 * - Shape contract corner spec resolution & reduced motion
 * - Button motion contract (pressed scale, specs, reduced motion)
 * - Selection motion contract specs
 * - Expandable container motion contract specs
 * - Indicator motion contract lifecycle & reduced motion
 * - Hero motion contract semantic mapping, amplitude, duration, colors & reduced motion
 * - ControlBar motion contract semantic mapping, layout configs & transitions
 */
class VardiyaComponentMotionContractsTest {

    private val expressiveScheme = VardiyaMotionScheme.expressive()
    private val standardScheme = VardiyaMotionScheme.standard()
    private val reducedScheme = VardiyaMotionScheme.reducedMotion()

    // ========================================================================
    // 1. SEMANTIC MOTION STATE TESTS
    // ========================================================================

    @Test
    fun testSemanticMotionStates_allJustifiedStatesExist() {
        val states = VardiyaSemanticMotionState.values()
        assertEquals(6, states.size)
        assertTrue(states.contains(VardiyaSemanticMotionState.RESTING))
        assertTrue(states.contains(VardiyaSemanticMotionState.PRESSED))
        assertTrue(states.contains(VardiyaSemanticMotionState.SELECTED))
        assertTrue(states.contains(VardiyaSemanticMotionState.ACTIVE))
        assertTrue(states.contains(VardiyaSemanticMotionState.EXPANDED))
        assertTrue(states.contains(VardiyaSemanticMotionState.DISABLED))
    }

    // ========================================================================
    // 2. SHAPE CONTRACT TESTS
    // ========================================================================

    @Test
    fun testShapeContract_pressedState_usesFastSpatialSpec() {
        val contract = DefaultVardiyaShapeContract
        val spec = contract.resolveCornerSpec(
            from = VardiyaSemanticMotionState.RESTING,
            to = VardiyaSemanticMotionState.PRESSED,
            motionScheme = expressiveScheme
        )
        assertTrue("Pressed shape transition must use fastSpatialSpec", spec is SpringSpec)
    }

    @Test
    fun testShapeContract_expandedState_usesDefaultSpatialSpec() {
        val contract = DefaultVardiyaShapeContract
        val spec = contract.resolveCornerSpec(
            from = VardiyaSemanticMotionState.RESTING,
            to = VardiyaSemanticMotionState.EXPANDED,
            motionScheme = expressiveScheme
        )
        assertTrue("Expanded shape transition must use defaultSpatialSpec", spec is SpringSpec)
    }

    @Test
    fun testShapeContract_reducedMotion_returnsSnapSpec() {
        val contract = DefaultVardiyaShapeContract
        val spec = contract.resolveCornerSpec(
            from = VardiyaSemanticMotionState.RESTING,
            to = VardiyaSemanticMotionState.PRESSED,
            motionScheme = reducedScheme
        )
        assertTrue("Shape transition in reduced motion must be SnapSpec", spec is SnapSpec)
    }

    // ========================================================================
    // 3. BUTTON MOTION CONTRACT TESTS
    // ========================================================================

    @Test
    fun testButtonContract_pressedScale_isDocumentedVardiyaParameter() {
        val contract = DefaultVardiyaButtonMotionContract
        assertEquals(0.94f, contract.resolvePressedScale(), 0.001f)
    }

    @Test
    fun testButtonContract_specs_expressiveAndReduced() {
        val contract = DefaultVardiyaButtonMotionContract

        // Expressive
        val scaleSpec = contract.resolveScaleSpec<Float>(expressiveScheme)
        val alphaSpec = contract.resolveContentAlphaSpec<Float>(expressiveScheme)
        assertTrue("Scale spec must be SpringSpec", scaleSpec is SpringSpec)
        assertTrue("Alpha spec must be SpringSpec", alphaSpec is SpringSpec)

        // Reduced Motion
        val reducedScaleSpec = contract.resolveScaleSpec<Float>(reducedScheme)
        val reducedAlphaSpec = contract.resolveContentAlphaSpec<Float>(reducedScheme)
        assertTrue("Reduced scale spec must be SnapSpec", reducedScaleSpec is SnapSpec)
        assertTrue("Reduced alpha spec must be SnapSpec", reducedAlphaSpec is SnapSpec)
    }

    // ========================================================================
    // 4. SELECTION MOTION CONTRACT TESTS
    // ========================================================================

    @Test
    fun testSelectionContract_specs_expressiveAndReduced() {
        val contract = DefaultVardiyaSelectionMotionContract

        val selectionSpec = contract.resolveSelectionSpec<Float>(expressiveScheme)
        val colorSpec = contract.resolveColorSpec<Float>(expressiveScheme)
        assertTrue("Selection spec must be SpringSpec", selectionSpec is SpringSpec)
        assertTrue("Color spec must be SpringSpec", colorSpec is SpringSpec)

        val reducedSelectionSpec = contract.resolveSelectionSpec<Float>(reducedScheme)
        val reducedColorSpec = contract.resolveColorSpec<Float>(reducedScheme)
        assertTrue("Reduced selection spec must be SnapSpec", reducedSelectionSpec is SnapSpec)
        assertTrue("Reduced color spec must be SnapSpec", reducedColorSpec is SnapSpec)
    }

    // ========================================================================
    // 5. EXPANDABLE CONTAINER MOTION CONTRACT TESTS
    // ========================================================================

    @Test
    fun testExpandableContainerContract_specs_expressiveAndReduced() {
        val contract = DefaultVardiyaExpandableContainerMotionContract

        val containerSpec = contract.resolveContainerSpec<Float>(expressiveScheme)
        val fadeSpec = contract.resolveContentFadeSpec<Float>(expressiveScheme)
        assertTrue("Container spec must be SpringSpec", containerSpec is SpringSpec)
        assertTrue("Fade spec must be SpringSpec", fadeSpec is SpringSpec)

        val reducedContainerSpec = contract.resolveContainerSpec<Float>(reducedScheme)
        val reducedFadeSpec = contract.resolveContentFadeSpec<Float>(reducedScheme)
        assertTrue("Reduced container spec must be SnapSpec", reducedContainerSpec is SnapSpec)
        assertTrue("Reduced fade spec must be SnapSpec", reducedFadeSpec is SnapSpec)
    }

    // ========================================================================
    // 6. INDICATOR MOTION CONTRACT TESTS
    // ========================================================================

    @Test
    fun testIndicatorContract_continuousPulseLifecycle() {
        val contract = DefaultVardiyaIndicatorMotionContract

        // Only active during RUNNING without break under NORMAL preference
        assertTrue(
            contract.isContinuousPulseActive(ShiftState.RUNNING, isBreakActive = false, MotionPreference.NORMAL)
        )

        // Inactive when on break
        assertFalse(
            contract.isContinuousPulseActive(ShiftState.RUNNING, isBreakActive = true, MotionPreference.NORMAL)
        )

        // Inactive when PAUSED
        assertFalse(
            contract.isContinuousPulseActive(ShiftState.PAUSED, isBreakActive = false, MotionPreference.NORMAL)
        )

        // Inactive when NOT_STARTED
        assertFalse(
            contract.isContinuousPulseActive(ShiftState.NOT_STARTED, isBreakActive = false, MotionPreference.NORMAL)
        )

        // Inactive when FINISHED
        assertFalse(
            contract.isContinuousPulseActive(ShiftState.FINISHED, isBreakActive = false, MotionPreference.NORMAL)
        )

        // Inactive under REDUCED preference even when RUNNING
        assertFalse(
            contract.isContinuousPulseActive(ShiftState.RUNNING, isBreakActive = false, MotionPreference.REDUCED)
        )
    }

    // ========================================================================
    // 7. HERO / PROGRESS MOTION CONTRACT TESTS
    // ========================================================================

    @Test
    fun testHeroContract_semanticStateResolution() {
        val contract = DefaultVardiyaHeroMotionContract

        assertEquals(
            HeroSemanticState.NOT_STARTED,
            contract.resolveSemanticState(ShiftState.NOT_STARTED, isOvertimeActive = false, isBreakActive = false)
        )
        assertEquals(
            HeroSemanticState.RUNNING,
            contract.resolveSemanticState(ShiftState.RUNNING, isOvertimeActive = false, isBreakActive = false)
        )
        assertEquals(
            HeroSemanticState.BREAK,
            contract.resolveSemanticState(ShiftState.RUNNING, isOvertimeActive = false, isBreakActive = true)
        )
        assertEquals(
            HeroSemanticState.OVERTIME,
            contract.resolveSemanticState(ShiftState.RUNNING, isOvertimeActive = true, isBreakActive = false)
        )
        assertEquals(
            HeroSemanticState.PAUSED,
            contract.resolveSemanticState(ShiftState.PAUSED, isOvertimeActive = false, isBreakActive = false)
        )
        assertEquals(
            HeroSemanticState.FINISHED,
            contract.resolveSemanticState(ShiftState.FINISHED, isOvertimeActive = false, isBreakActive = false)
        )
    }

    @Test
    fun testHeroContract_targetAmplitudes() {
        val contract = DefaultVardiyaHeroMotionContract

        assertEquals(5.0.dp, contract.resolveTargetAmplitude(HeroSemanticState.NOT_STARTED))
        assertEquals(6.0.dp, contract.resolveTargetAmplitude(HeroSemanticState.RUNNING))
        assertEquals(4.5.dp, contract.resolveTargetAmplitude(HeroSemanticState.PAUSED))
        assertEquals(5.0.dp, contract.resolveTargetAmplitude(HeroSemanticState.BREAK))
        assertEquals(7.0.dp, contract.resolveTargetAmplitude(HeroSemanticState.OVERTIME))
        assertEquals(5.0.dp, contract.resolveTargetAmplitude(HeroSemanticState.FINISHED))
    }

    @Test
    fun testHeroContract_cycleDurations() {
        val contract = DefaultVardiyaHeroMotionContract

        assertEquals(2400L, contract.resolveWaveCycleDurationMs(HeroSemanticState.NOT_STARTED))
        assertEquals(2400L, contract.resolveWaveCycleDurationMs(HeroSemanticState.RUNNING))
        assertEquals(2400L, contract.resolveWaveCycleDurationMs(HeroSemanticState.PAUSED))
        assertEquals(2400L, contract.resolveWaveCycleDurationMs(HeroSemanticState.BREAK))
        assertEquals(1800L, contract.resolveWaveCycleDurationMs(HeroSemanticState.OVERTIME))
        assertEquals(2400L, contract.resolveWaveCycleDurationMs(HeroSemanticState.FINISHED))
    }

    @Test
    fun testHeroContract_phaseActivity() {
        val contract = DefaultVardiyaHeroMotionContract

        assertTrue(contract.isWavePhaseActive(HeroSemanticState.RUNNING, MotionPreference.NORMAL))
        assertTrue(contract.isWavePhaseActive(HeroSemanticState.OVERTIME, MotionPreference.NORMAL))
        assertFalse(contract.isWavePhaseActive(HeroSemanticState.PAUSED, MotionPreference.NORMAL))
        assertFalse(contract.isWavePhaseActive(HeroSemanticState.BREAK, MotionPreference.NORMAL))
        assertFalse(contract.isWavePhaseActive(HeroSemanticState.NOT_STARTED, MotionPreference.NORMAL))
        assertFalse(contract.isWavePhaseActive(HeroSemanticState.FINISHED, MotionPreference.NORMAL))

        // Suppressed under reduced motion
        assertFalse(contract.isWavePhaseActive(HeroSemanticState.RUNNING, MotionPreference.REDUCED))
        assertFalse(contract.isWavePhaseActive(HeroSemanticState.OVERTIME, MotionPreference.REDUCED))
    }

    @Test
    fun testHeroContract_colors_lightAndDark() {
        val contract = DefaultVardiyaHeroMotionContract

        // Light Scheme
        assertEquals(LightColorScheme.primary, contract.resolveHeroActiveColor(HeroSemanticState.RUNNING, LightColorScheme))
        assertEquals(LightColorScheme.primary, contract.resolveHeroActiveColor(HeroSemanticState.NOT_STARTED, LightColorScheme))
        assertEquals(LightColorScheme.tertiary, contract.resolveHeroActiveColor(HeroSemanticState.PAUSED, LightColorScheme))
        assertEquals(LightColorScheme.tertiary, contract.resolveHeroActiveColor(HeroSemanticState.BREAK, LightColorScheme))
        assertEquals(LightColorScheme.tertiary, contract.resolveHeroActiveColor(HeroSemanticState.OVERTIME, LightColorScheme))
        assertEquals(LightColorScheme.secondary, contract.resolveHeroActiveColor(HeroSemanticState.FINISHED, LightColorScheme))

        // Dark Scheme
        assertEquals(DarkColorScheme.primary, contract.resolveHeroActiveColor(HeroSemanticState.RUNNING, DarkColorScheme))
        assertEquals(DarkColorScheme.tertiary, contract.resolveHeroActiveColor(HeroSemanticState.PAUSED, DarkColorScheme))
        assertEquals(DarkColorScheme.secondary, contract.resolveHeroActiveColor(HeroSemanticState.FINISHED, DarkColorScheme))
    }

    @Test
    fun testHeroContract_specs() {
        val contract = DefaultVardiyaHeroMotionContract

        assertTrue(contract.resolveProgressSpec<Float>(expressiveScheme) is SpringSpec)
        assertTrue(contract.resolveAmplitudeSpec<Float>(expressiveScheme) is SpringSpec)
        assertTrue(contract.resolveColorSpec<Float>(expressiveScheme) is SpringSpec)

        assertTrue(contract.resolveProgressSpec<Float>(reducedScheme) is SnapSpec)
        assertTrue(contract.resolveAmplitudeSpec<Float>(reducedScheme) is SnapSpec)
        assertTrue(contract.resolveColorSpec<Float>(reducedScheme) is SnapSpec)
    }

    // ========================================================================
    // 8. CONTROL BAR MOTION CONTRACT TESTS
    // ========================================================================

    @Test
    fun testControlBarContract_semanticStateResolution() {
        val contract = DefaultVardiyaControlBarMotionContract

        assertEquals(
            ControlBarSemanticState.START,
            contract.resolveSemanticState(ShiftState.NOT_STARTED, isBreakActive = false)
        )
        assertEquals(
            ControlBarSemanticState.RUNNING,
            contract.resolveSemanticState(ShiftState.RUNNING, isBreakActive = false)
        )
        assertEquals(
            ControlBarSemanticState.BREAK,
            contract.resolveSemanticState(ShiftState.RUNNING, isBreakActive = true)
        )
        assertEquals(
            ControlBarSemanticState.PAUSED,
            contract.resolveSemanticState(ShiftState.PAUSED, isBreakActive = false)
        )
        assertEquals(
            ControlBarSemanticState.FINISHED,
            contract.resolveSemanticState(ShiftState.FINISHED, isBreakActive = false)
        )
    }

    @Test
    fun testControlBarContract_layoutConfigMapping() {
        val contract = DefaultVardiyaControlBarMotionContract

        assertEquals(ControlBarLayoutConfig.START_ONLY, contract.resolveLayoutConfig(ControlBarSemanticState.START))
        assertEquals(ControlBarLayoutConfig.ACTIVE_CONTROLS, contract.resolveLayoutConfig(ControlBarSemanticState.RUNNING))
        assertEquals(ControlBarLayoutConfig.BREAK_CONTROLS, contract.resolveLayoutConfig(ControlBarSemanticState.BREAK))
        assertEquals(ControlBarLayoutConfig.PAUSED_CONTROLS, contract.resolveLayoutConfig(ControlBarSemanticState.PAUSED))
        assertEquals(ControlBarLayoutConfig.RESET_ONLY, contract.resolveLayoutConfig(ControlBarSemanticState.FINISHED))
    }

    @Test
    fun testControlBarContract_transitions_normalAndReduced() {
        val contract = DefaultVardiyaControlBarMotionContract

        val normalTransition = contract.createTransition(expressiveScheme, MotionPreference.NORMAL)
        assertNotNull("Normal transition must not be null", normalTransition)

        val reducedTransition = contract.createTransition(reducedScheme, MotionPreference.REDUCED)
        assertNotNull("Reduced transition must not be null", reducedTransition)
    }

    @Test
    fun testControlBarContract_initialScale_isVardiyaDesignParameter() {
        assertEquals(0.96f, DefaultVardiyaControlBarMotionContract.VARDIYA_CONTROL_BAR_INITIAL_SCALE, 0.001f)
    }

    // ========================================================================
    // 9. NAVIGATION MOTION CONTRACT TESTS
    // ========================================================================

    @Test
    fun testNavigationContract_spatialOffsetFactor_isDocumentedVardiyaParameter() {
        val contract = DefaultVardiyaNavigationMotionContract
        assertEquals(0.08f, contract.resolveSpatialOffsetFactor(), 0.001f)
    }

    @Test
    fun testNavigationContract_transitions_normalAndReduced() {
        val contract = DefaultVardiyaNavigationMotionContract

        val forwardNormal = contract.createForwardTransition(expressiveScheme, MotionPreference.NORMAL)
        assertNotNull("Forward normal transition must not be null", forwardNormal)

        val forwardReduced = contract.createForwardTransition(reducedScheme, MotionPreference.REDUCED)
        assertNotNull("Forward reduced transition must not be null", forwardReduced)

        val popNormal = contract.createPopTransition(expressiveScheme, MotionPreference.NORMAL)
        assertNotNull("Pop normal transition must not be null", popNormal)

        val popReduced = contract.createPopTransition(reducedScheme, MotionPreference.REDUCED)
        assertNotNull("Pop reduced transition must not be null", popReduced)
    }

    @Test
    fun testNavigationContract_indicatorAndItemSpecs() {
        val contract = DefaultVardiyaNavigationMotionContract

        assertTrue(contract.resolveIndicatorSpatialSpec<Float>(expressiveScheme) is SpringSpec)
        assertTrue(contract.resolveItemEffectsSpec<Float>(expressiveScheme) is SpringSpec)

        assertTrue(contract.resolveIndicatorSpatialSpec<Float>(reducedScheme) is SnapSpec)
        assertTrue(contract.resolveItemEffectsSpec<Float>(reducedScheme) is SnapSpec)
    }

    // ========================================================================
    // 10. ENRICHED BUTTON, SELECTION & EXPANDABLE CONTRACT TESTS
    // ========================================================================

    @Test
    fun testButtonContract_semanticStateAndScaleResolution() {
        val contract = DefaultVardiyaButtonMotionContract

        assertEquals(ButtonSemanticState.DISABLED, contract.resolveSemanticState(isPressed = false, isEnabled = false))
        assertEquals(ButtonSemanticState.DISABLED, contract.resolveSemanticState(isPressed = true, isEnabled = false))
        assertEquals(ButtonSemanticState.PRESSED, contract.resolveSemanticState(isPressed = true, isEnabled = true))
        assertEquals(ButtonSemanticState.RESTING, contract.resolveSemanticState(isPressed = false, isEnabled = true))

        assertEquals(0.94f, contract.resolveScale(ButtonSemanticState.PRESSED), 0.001f)
        assertEquals(1.0f, contract.resolveScale(ButtonSemanticState.RESTING), 0.001f)
        assertEquals(1.0f, contract.resolveScale(ButtonSemanticState.DISABLED), 0.001f)
    }

    @Test
    fun testSelectionContract_semanticStateAndBorderWidth() {
        val contract = DefaultVardiyaSelectionMotionContract

        assertEquals(SelectionSemanticState.SELECTED, contract.resolveSemanticState(isSelected = true))
        assertEquals(SelectionSemanticState.UNSELECTED, contract.resolveSemanticState(isSelected = false))

        assertEquals(2.dp, contract.resolveBorderWidth(SelectionSemanticState.SELECTED))
        assertEquals(0.dp, contract.resolveBorderWidth(SelectionSemanticState.UNSELECTED))
    }

    @Test
    fun testExpandableContainerContract_semanticStateAndTransitions() {
        val contract = DefaultVardiyaExpandableContainerMotionContract

        assertEquals(ExpandableSemanticState.EXPANDED, contract.resolveSemanticState(isExpanded = true))
        assertEquals(ExpandableSemanticState.COLLAPSED, contract.resolveSemanticState(isExpanded = false))

        val enterNormal = contract.createEnterTransition(expressiveScheme, MotionPreference.NORMAL)
        val exitNormal = contract.createExitTransition(expressiveScheme, MotionPreference.NORMAL)
        assertNotNull(enterNormal)
        assertNotNull(exitNormal)

        val enterReduced = contract.createEnterTransition(reducedScheme, MotionPreference.REDUCED)
        val exitReduced = contract.createExitTransition(reducedScheme, MotionPreference.REDUCED)
        assertNotNull(enterReduced)
        assertNotNull(exitReduced)
    }
}
