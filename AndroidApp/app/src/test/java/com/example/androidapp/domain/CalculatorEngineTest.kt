package com.example.androidapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculatorEngineTest {

    private lateinit var engine: CalculatorEngine

    @Before
    fun setUp() {
        engine = CalculatorEngine()
    }

    @Test
    fun testSimpleAddition() {
        val result = engine.evaluate(listOf("2", "+", "2"))
        assertTrue(result is CalculationResult.Success)
        assertEquals("4", (result as CalculationResult.Success).value)
    }

    @Test
    fun testSimpleSubtraction() {
        val result = engine.evaluate(listOf("7", "-", "3"))
        assertTrue(result is CalculationResult.Success)
        assertEquals("4", (result as CalculationResult.Success).value)
    }

    @Test
    fun testSimpleMultiplication() {
        val result = engine.evaluate(listOf("6", "×", "7"))
        assertTrue(result is CalculationResult.Success)
        assertEquals("42", (result as CalculationResult.Success).value)
    }

    @Test
    fun testSimpleDivision() {
        val result = engine.evaluate(listOf("8", "÷", "2"))
        assertTrue(result is CalculationResult.Success)
        assertEquals("4", (result as CalculationResult.Success).value)
    }

    @Test
    fun testDivisionByZeroProducesError() {
        val result = engine.evaluate(listOf("5", "÷", "0"))
        assertTrue(result is CalculationResult.Error)
        assertEquals("Cannot divide by zero", (result as CalculationResult.Error).message)
    }

    @Test
    fun testDecimalAdditionPrecision() {
        // 0.1 + 0.2 must equal 0.3 without binary floating point artifacts
        val result = engine.evaluate(listOf("0.1", "+", "0.2"))
        assertTrue(result is CalculationResult.Success)
        assertEquals("0.3", (result as CalculationResult.Success).value)
    }

    @Test
    fun testOperatorPrecedenceMultiplicationBeforeAddition() {
        // 2 + 3 × 4 = 14, NOT 20
        val result = engine.evaluate(listOf("2", "+", "3", "×", "4"))
        assertTrue(result is CalculationResult.Success)
        assertEquals("14", (result as CalculationResult.Success).value)
    }

    @Test
    fun testLeftToRightAssociativityForEqualPrecedence() {
        // 8 - 3 - 2 = (8 - 3) - 2 = 3
        val result = engine.evaluate(listOf("8", "-", "3", "-", "2"))
        assertTrue(result is CalculationResult.Success)
        assertEquals("3", (result as CalculationResult.Success).value)
    }

    @Test
    fun testOperatorReplacementInExpressionBuilding() {
        // 5 + followed by × 4 should replace + with × to yield 5 × 4 = 20
        val tokens = engine.normalizeTokens(listOf("5", "+", "×", "4"))
        assertEquals(listOf("5", "×", "4"), tokens)
        val result = engine.evaluate(tokens)
        assertTrue(result is CalculationResult.Success)
        assertEquals("20", (result as CalculationResult.Success).value)
    }

    @Test
    fun testDecimalFormattingTrimsTrailingZeros() {
        val formatted = engine.formatNumber("4.000")
        assertEquals("4", formatted)
        val formattedZero = engine.formatNumber("0.00")
        assertEquals("0", formattedZero)
        val formattedFraction = engine.formatNumber("4.50")
        assertEquals("4.5", formattedFraction)
    }

    @Test
    fun testLeadingDecimalPoint() {
        val result = engine.evaluate(listOf(".5", "+", ".5"))
        assertTrue(result is CalculationResult.Success)
        assertEquals("1", (result as CalculationResult.Success).value)
    }

    @Test
    fun testCombinedPrecedenceAndAssociativity() {
        // 10 - 2 × 3 + 4 = 10 - 6 + 4 = 4 + 4 = 8
        val result = engine.evaluate(listOf("10", "-", "2", "×", "3", "+", "4"))
        assertTrue(result is CalculationResult.Success)
        assertEquals("8", (result as CalculationResult.Success).value)
    }

    @Test
    fun testEmptyExpressionReturnsZero() {
        val result = engine.evaluate(emptyList())
        assertTrue(result is CalculationResult.Success)
        assertEquals("0", (result as CalculationResult.Success).value)
    }

    @Test
    fun testSingleNumberReturnsFormattedNumber() {
        val result = engine.evaluate(listOf("42.0"))
        assertTrue(result is CalculationResult.Success)
        assertEquals("42", (result as CalculationResult.Success).value)
    }

    @Test
    fun testTrailingOperatorEvaluatesLeadingExpression() {
        // 5 + with no second operand evaluates to 5
        val result = engine.evaluate(listOf("5", "+"))
        assertTrue(result is CalculationResult.Success)
        assertEquals("5", (result as CalculationResult.Success).value)
    }

    @Test
    fun testDivisionBeforeSubtractionPrecedence() {
        // 20 - 10 ÷ 2 = 20 - 5 = 15
        val result = engine.evaluate(listOf("20", "-", "10", "÷", "2"))
        assertTrue(result is CalculationResult.Success)
        assertEquals("15", (result as CalculationResult.Success).value)
    }

    @Test
    fun testRepeatingDecimalDivision() {
        // 1 ÷ 3 = 0.333333333333
        val result = engine.evaluate(listOf("1", "÷", "3"))
        assertTrue(result is CalculationResult.Success)
        assertEquals("0.333333333333", (result as CalculationResult.Success).value)
    }

    @Test
    fun testVeryLargeMultiplication() {
        // 999999999 × 999999999 = 999999998000000001
        val result = engine.evaluate(listOf("999999999", "×", "999999999"))
        assertTrue(result is CalculationResult.Success)
        assertEquals("999999998000000001", (result as CalculationResult.Success).value)
    }

    @Test
    fun testExpressionWithParentheses() {
        // 16 × (0.5 + 2.5) = 48
        val result = engine.evaluateExpression("16 × (0.5 + 2.5)")
        assertTrue(result is CalculationResult.Success)
        assertEquals("48", (result as CalculationResult.Success).value)
    }

    @Test
    fun testExpressionWithImplicitMultiplication() {
        val result = engine.evaluateExpression("16(0.5 + 2.5)")
        assertTrue(result is CalculationResult.Success)
        assertEquals("48", (result as CalculationResult.Success).value)
    }

    @Test
    fun testExpressionPercentageAddition() {
        // 100 + 10% = 110
        val result = engine.evaluateExpression("100 + 10%")
        assertTrue(result is CalculationResult.Success)
        assertEquals("110", (result as CalculationResult.Success).value)
    }

    @Test
    fun testExpressionPercentageSubtraction() {
        // 100 - 20% = 80
        val result = engine.evaluateExpression("100 - 20%")
        assertTrue(result is CalculationResult.Success)
        assertEquals("80", (result as CalculationResult.Success).value)
    }

    @Test
    fun testExpressionPercentageMultiplication() {
        // 50 × 20% = 10
        val result = engine.evaluateExpression("50 × 20%")
        assertTrue(result is CalculationResult.Success)
        assertEquals("10", (result as CalculationResult.Success).value)
    }

    @Test
    fun testExpressionUnaryMinus() {
        // -5 + 3 = -2
        val result = engine.evaluateExpression("-5 + 3")
        assertTrue(result is CalculationResult.Success)
        assertEquals("-2", (result as CalculationResult.Success).value)
    }

    @Test
    fun testLivePreviewAutoClosesParentheses() {
        // 16 × (0.5 + 2.5 should evaluate as 48 even before user types ')'
        val result = engine.evaluateExpression("16 × (0.5 + 2.5")
        assertTrue(result is CalculationResult.Success)
        assertEquals("48", (result as CalculationResult.Success).value)
    }
}

