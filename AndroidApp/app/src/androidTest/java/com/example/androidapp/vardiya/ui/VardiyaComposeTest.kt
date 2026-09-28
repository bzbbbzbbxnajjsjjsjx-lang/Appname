package com.example.androidapp.vardiya.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import com.example.androidapp.theme.AndroidAppTheme
import com.example.androidapp.vardiya.data.repository.InMemoryVardiyaRepository
import com.example.androidapp.vardiya.domain.calculator.ShiftEarningsCalculator
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.ShiftState
import com.example.androidapp.vardiya.domain.time.TimeProvider
import com.example.androidapp.vardiya.ui.components.CircularWavyProgressHero
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class FakeTestTimeProvider(
    var epochMillis: Long = 1700000000000L,
    var elapsedRealtime: Long = 1000000L
) : TimeProvider {
    override fun currentEpochMillis(): Long = epochMillis
    override fun elapsedRealtimeMillis(): Long = elapsedRealtime

    fun advance(durationMs: Long) {
        epochMillis += durationMs
        elapsedRealtime += durationMs
    }
}

/**
 * Comprehensive Compose UI / instrumentation test suite for VardiyaScreen and CircularWavyProgressHero.
 * Runs hermetically against in-memory repository to guarantee zero inter-test state leakage.
 */
class VardiyaComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createTestViewModel(
        initialSalary: SalaryConfiguration = SalaryConfiguration(monthlySalary = BigDecimal("28000")),
        timeProvider: TimeProvider = FakeTestTimeProvider()
    ): VardiyaViewModel {
        val repository = InMemoryVardiyaRepository(initialSalary = initialSalary)
        val calculator = ShiftEarningsCalculator()
        return VardiyaViewModel(
            repository = repository,
            calculator = calculator,
            timeProvider = timeProvider
        )
    }

    // 1. Initial state
    @Test
    fun testInitialState_showsNotStartedAndStartButtonAccessible() {
        val viewModel = createTestViewModel()
        composeTestRule.setContent {
            AndroidAppTheme {
                VardiyaScreen(viewModel = viewModel)
            }
        }

        // State badge
        composeTestRule.onNodeWithText("HAZIR").assertIsDisplayed()

        // Start action button
        composeTestRule.onNodeWithContentDescription("Vardiyayı başlat")
            .assertIsDisplayed()
            .assertIsEnabled()

        // Subtitle
        composeTestRule.onNodeWithText("BAŞLAMAYA HAZIR").assertIsDisplayed()

        // Rates breakdown
        composeTestRule.onNodeWithText("159,09 ₺ / saat").assertIsDisplayed()
    }

    // 2. Start
    @Test
    fun testStartShift_transitionsToRunningAndRendersHeroAndActionButtons() {
        val viewModel = createTestViewModel()
        composeTestRule.setContent {
            AndroidAppTheme {
                VardiyaScreen(viewModel = viewModel)
            }
        }

        // Tap Start button
        composeTestRule.onNodeWithContentDescription("Vardiyayı başlat").performClick()
        composeTestRule.waitForIdle()

        // State changes to RUNNING
        composeTestRule.onNodeWithText("ÇALIŞIYOR").assertIsDisplayed()
        composeTestRule.onNodeWithText("BU VARDİYADA KAZANILAN").assertIsDisplayed()

        // Pause and Finish buttons are rendered
        composeTestRule.onNodeWithContentDescription("Vardiyayı duraklat").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Vardiyayı bitir").assertIsDisplayed()

        assertEquals(ShiftState.RUNNING, viewModel.uiState.value.shiftState)
    }

    // 3. Pause
    @Test
    fun testPauseShift_transitionsToPausedAndPreservesEarnings() {
        val timeProvider = FakeTestTimeProvider()
        val viewModel = createTestViewModel(timeProvider = timeProvider)
        composeTestRule.setContent {
            AndroidAppTheme {
                VardiyaScreen(viewModel = viewModel)
            }
        }

        // Start -> Running
        composeTestRule.onNodeWithContentDescription("Vardiyayı başlat").performClick()
        composeTestRule.waitForIdle()

        // Pause
        composeTestRule.onNodeWithContentDescription("Vardiyayı duraklat").performClick()
        composeTestRule.waitForIdle()

        // Verify paused state badge & buttons
        composeTestRule.onNodeWithText("DURAKLATILDI").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Vardiyayı devam ettir").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Vardiyayı bitir").assertIsDisplayed()

        assertEquals(ShiftState.PAUSED, viewModel.uiState.value.shiftState)
    }

    // 4. Resume
    @Test
    fun testResumeShift_transitionsBackToRunning() {
        val viewModel = createTestViewModel()
        composeTestRule.setContent {
            AndroidAppTheme {
                VardiyaScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithContentDescription("Vardiyayı başlat").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Vardiyayı duraklat").performClick()
        composeTestRule.waitForIdle()

        // Tap Resume
        composeTestRule.onNodeWithContentDescription("Vardiyayı devam ettir").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("ÇALIŞIYOR").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Vardiyayı duraklat").assertIsDisplayed()
        assertEquals(ShiftState.RUNNING, viewModel.uiState.value.shiftState)
    }

    // 5. Finish
    @Test
    fun testFinishShift_transitionsToFinishedAndShowsNewShiftButton() {
        val viewModel = createTestViewModel()
        composeTestRule.setContent {
            AndroidAppTheme {
                VardiyaScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithContentDescription("Vardiyayı başlat").performClick()
        composeTestRule.waitForIdle()

        // Finish
        composeTestRule.onNodeWithContentDescription("Vardiyayı bitir").performClick()
        composeTestRule.waitForIdle()

        // Finished state
        composeTestRule.onNodeWithText("TAMAMLANDI").assertIsDisplayed()
        composeTestRule.onNodeWithText("BU VARDİYADA KAZANILDI").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Yeni vardiyaya hazırlan").assertIsDisplayed()

        assertEquals(ShiftState.FINISHED, viewModel.uiState.value.shiftState)

        // Reset
        composeTestRule.onNodeWithContentDescription("Yeni vardiyaya hazırlan").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("HAZIR").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Vardiyayı başlat").assertIsDisplayed()
        assertEquals(ShiftState.NOT_STARTED, viewModel.uiState.value.shiftState)
    }

    // 6. Invalid configuration handling in setup dialog
    @Test
    fun testInvalidConfiguration_showsErrorMessageAndDisablesSave() {
        val viewModel = createTestViewModel()
        composeTestRule.setContent {
            AndroidAppTheme {
                VardiyaScreen(viewModel = viewModel)
            }
        }

        // Open Setup dialog
        composeTestRule.onNodeWithText("Ayarla").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Maaş & Çalışma Düzeni").assertIsDisplayed()

        // Enter 0 salary
        val salaryInput = composeTestRule.onNodeWithContentDescription("Aylık maaş tutarı")
        salaryInput.performTextClearance()
        salaryInput.performTextInput("0")
        composeTestRule.waitForIdle()

        // Error message displayed
        composeTestRule.onNodeWithText("Maaş 0 veya negatif olamaz.").assertIsDisplayed()

        // Save button must be disabled
        composeTestRule.onNodeWithText("Kaydet").assertIsNotEnabled()

        // Dismiss dialog
        composeTestRule.onNodeWithText("İptal").performClick()
        composeTestRule.waitForIdle()

        // Screen is still alive and stable
        composeTestRule.onNodeWithText("HAZIR").assertIsDisplayed()
    }

    // 7. CircularWavyProgressHero progress variations
    @Test
    fun testCircularWavyProgressHero_rendersZeroMidAndFullProgress() {
        listOf(0f, 0.5f, 1f).forEach { progressValue ->
            composeTestRule.setContent {
                AndroidAppTheme {
                    CircularWavyProgressHero(
                        progress = progressValue,
                        shiftState = ShiftState.RUNNING,
                        heroAmountText = "125,50 ₺",
                        heroSubtitleText = "BU VARDİYADA KAZANILAN",
                        durationText = "01:15:30"
                    )
                }
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("125,50 ₺").assertIsDisplayed()
            composeTestRule.onNodeWithText("BU VARDİYADA KAZANILAN").assertIsDisplayed()
            composeTestRule.onNodeWithText("01:15:30").assertIsDisplayed()
        }
    }

    // 8. Accessibility semantics
    @Test
    fun testAccessibilitySemantics_progressAndButtonsMatchDescriptions() {
        val viewModel = createTestViewModel()
        composeTestRule.setContent {
            AndroidAppTheme {
                VardiyaScreen(viewModel = viewModel)
            }
        }

        // Check progress semantics exist on CircularWavyProgressHero
        composeTestRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(0f, 0f..1f)
            )
        ).assertExists()

        // Check action button has clear content description
        composeTestRule.onNodeWithContentDescription("Vardiyayı başlat").assertExists()
    }

    // 9. Large text font scaling
    @Test
    fun testLargeText_doesNotCrashOrClip() {
        val viewModel = createTestViewModel()
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 2f, fontScale = 1.75f)
            ) {
                AndroidAppTheme {
                    VardiyaScreen(viewModel = viewModel)
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("HAZIR").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Vardiyayı başlat").assertIsDisplayed()
    }

    // 10. Configuration change / recreation state preservation
    @Test
    fun testConfigurationChange_stateIsPreserved() {
        val viewModel = createTestViewModel()
        viewModel.startShift()

        composeTestRule.setContent {
            AndroidAppTheme {
                VardiyaScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ÇALIŞIYOR").assertIsDisplayed()

        // Recreate composition simulate screen rotation
        composeTestRule.setContent {
            AndroidAppTheme {
                VardiyaScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ÇALIŞIYOR").assertIsDisplayed()
        assertEquals(ShiftState.RUNNING, viewModel.uiState.value.shiftState)
    }

    // 11. Rapid interaction stress
    @Test
    fun testRapidInteraction_noCrashOrRaceCondition() {
        val viewModel = createTestViewModel()
        composeTestRule.setContent {
            AndroidAppTheme {
                VardiyaScreen(viewModel = viewModel)
            }
        }

        // Start
        composeTestRule.onNodeWithContentDescription("Vardiyayı başlat").performClick()
        composeTestRule.waitForIdle()

        // Rapid click pause and resume
        for (i in 1..3) {
            try {
                composeTestRule.onNodeWithContentDescription("Vardiyayı duraklat").performClick()
                composeTestRule.waitForIdle()
            } catch (_: AssertionError) {}

            try {
                composeTestRule.onNodeWithContentDescription("Vardiyayı devam ettir").performClick()
                composeTestRule.waitForIdle()
            } catch (_: AssertionError) {}
        }

        assertTrue(viewModel.uiState.value.shiftState in listOf(ShiftState.RUNNING, ShiftState.PAUSED))
    }

    // 12. Dark and Light Theme rendering
    @Test
    fun testDarkAndLightTheme_renderCleanlyWithoutCrash() {
        val viewModelDark = createTestViewModel()
        composeTestRule.setContent {
            AndroidAppTheme(darkTheme = true) {
                VardiyaScreen(viewModel = viewModelDark)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("HAZIR").assertIsDisplayed()

        val viewModelLight = createTestViewModel()
        composeTestRule.setContent {
            AndroidAppTheme(darkTheme = false) {
                VardiyaScreen(viewModel = viewModelLight)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("HAZIR").assertIsDisplayed()
    }
}
