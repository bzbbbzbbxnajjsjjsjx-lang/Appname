package com.example.androidapp.ui

import com.example.androidapp.domain.CalculatorEngine
import com.example.androidapp.domain.CalculatorOperator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculatorViewModelTest {

    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setUp() {
        viewModel = CalculatorViewModel(CalculatorEngine())
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertEquals("0", state.displayText)
        assertEquals("", state.expressionPreview)
        assertFalse(state.isError)
    }

    @Test
    fun testDigitInput() {
        viewModel.onDigitClicked('5')
        assertEquals("5", viewModel.uiState.value.displayText)
        viewModel.onDigitClicked('2')
        assertEquals("52", viewModel.uiState.value.displayText)
    }

    @Test
    fun testDecimalInput() {
        viewModel.onDecimalClicked()
        assertEquals("0.", viewModel.uiState.value.displayText)
        viewModel.onDigitClicked('5')
        assertEquals("0.5", viewModel.uiState.value.displayText)
        // Repeated decimal point should be ignored
        viewModel.onDecimalClicked()
        assertEquals("0.5", viewModel.uiState.value.displayText)
    }

    @Test
    fun testOperatorSelection() {
        viewModel.onDigitClicked('8')
        viewModel.onOperatorClicked(CalculatorOperator.ADD)
        val state = viewModel.uiState.value
        assertEquals("8 +", state.expressionPreview)
        assertEquals("8", state.displayText)
    }

    @Test
    fun testOperatorReplacement() {
        viewModel.onDigitClicked('8')
        viewModel.onOperatorClicked(CalculatorOperator.ADD)
        assertEquals("8 +", viewModel.uiState.value.expressionPreview)
        // Replace with MULTIPLY
        viewModel.onOperatorClicked(CalculatorOperator.MULTIPLY)
        assertEquals("8 ×", viewModel.uiState.value.expressionPreview)
    }

    @Test
    fun testSimpleCalculationEquals() {
        viewModel.onDigitClicked('6')
        viewModel.onOperatorClicked(CalculatorOperator.MULTIPLY)
        viewModel.onDigitClicked('7')
        viewModel.onEqualsClicked()
        val state = viewModel.uiState.value
        assertEquals("42", state.displayText)
        assertEquals("6 × 7 =", state.expressionPreview)
        assertFalse(state.isError)
    }

    @Test
    fun testDivisionByZeroState() {
        viewModel.onDigitClicked('5')
        viewModel.onOperatorClicked(CalculatorOperator.DIVIDE)
        viewModel.onDigitClicked('0')
        viewModel.onEqualsClicked()
        val state = viewModel.uiState.value
        assertEquals("Cannot divide by zero", state.displayText)
        assertTrue(state.isError)
    }

    @Test
    fun testClearResetsToInitialState() {
        viewModel.onDigitClicked('9')
        viewModel.onOperatorClicked(CalculatorOperator.ADD)
        viewModel.onDigitClicked('9')
        viewModel.onClearClicked()
        val state = viewModel.uiState.value
        assertEquals("0", state.displayText)
        assertEquals("", state.expressionPreview)
        assertFalse(state.isError)
    }

    @Test
    fun testResultFollowedByOperatorChainsCalculation() {
        viewModel.onDigitClicked('4')
        viewModel.onOperatorClicked(CalculatorOperator.ADD)
        viewModel.onDigitClicked('6')
        viewModel.onEqualsClicked()
        assertEquals("10", viewModel.uiState.value.displayText)

        // Pressing + should continue with 10 as first operand
        viewModel.onOperatorClicked(CalculatorOperator.MULTIPLY)
        assertEquals("10 ×", viewModel.uiState.value.expressionPreview)
        viewModel.onDigitClicked('2')
        viewModel.onEqualsClicked()
        assertEquals("20", viewModel.uiState.value.displayText)
    }

    @Test
    fun testResultFollowedByDigitStartsNewCalculation() {
        viewModel.onDigitClicked('4')
        viewModel.onOperatorClicked(CalculatorOperator.ADD)
        viewModel.onDigitClicked('6')
        viewModel.onEqualsClicked()
        assertEquals("10", viewModel.uiState.value.displayText)

        // Pressing a new digit starts fresh
        viewModel.onDigitClicked('7')
        assertEquals("7", viewModel.uiState.value.displayText)
        assertEquals("", viewModel.uiState.value.expressionPreview)
    }

    @Test
    fun testDecimalFilteringMultipleDecimalsIgnored() {
        // Entering 5 . 2 . 3 should produce 5.23
        viewModel.onDigitClicked('5')
        viewModel.onDecimalClicked()
        viewModel.onDigitClicked('2')
        viewModel.onDecimalClicked() // should be ignored
        viewModel.onDigitClicked('3')
        assertEquals("5.23", viewModel.uiState.value.displayText)
    }

    @Test
    fun testImmediateEqualsDoesNothing() {
        // Pressing equals on clean initial state remains 0
        viewModel.onEqualsClicked()
        val state = viewModel.uiState.value
        assertEquals("0", state.displayText)
        assertEquals("", state.expressionPreview)
        assertFalse(state.isError)
    }

    @Test
    fun testClearAfterErrorResetsCleanly() {
        // 5 ÷ 0 -> error -> AC -> 0
        viewModel.onDigitClicked('5')
        viewModel.onOperatorClicked(CalculatorOperator.DIVIDE)
        viewModel.onDigitClicked('0')
        viewModel.onEqualsClicked()
        assertTrue(viewModel.uiState.value.isError)

        viewModel.onClearClicked()
        val state = viewModel.uiState.value
        assertEquals("0", state.displayText)
        assertEquals("", state.expressionPreview)
        assertFalse(state.isError)
    }

    @Test
    fun testDigitInputAfterErrorResetsAndAcceptsDigit() {
        // 5 ÷ 0 -> error -> press '8' -> resets and shows 8
        viewModel.onDigitClicked('5')
        viewModel.onOperatorClicked(CalculatorOperator.DIVIDE)
        viewModel.onDigitClicked('0')
        viewModel.onEqualsClicked()
        assertTrue(viewModel.uiState.value.isError)

        viewModel.onDigitClicked('8')
        val state = viewModel.uiState.value
        assertEquals("8", state.displayText)
        assertEquals("", state.expressionPreview)
        assertFalse(state.isError)
    }

    @Test
    fun testOperatorPrecedenceMultiplicationOverAddition() {
        // 2 + 3 × 4 = 14
        viewModel.onDigitClicked('2')
        viewModel.onOperatorClicked(CalculatorOperator.ADD)
        viewModel.onDigitClicked('3')
        viewModel.onOperatorClicked(CalculatorOperator.MULTIPLY)
        viewModel.onDigitClicked('4')
        viewModel.onEqualsClicked()
        assertEquals("14", viewModel.uiState.value.displayText)
        assertFalse(viewModel.uiState.value.isError)
    }
}
