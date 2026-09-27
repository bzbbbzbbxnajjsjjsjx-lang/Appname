package com.example.androidapp.ui

import androidx.lifecycle.ViewModel
import com.example.androidapp.domain.CalculationResult
import com.example.androidapp.domain.CalculatorEngine
import com.example.androidapp.domain.CalculatorOperator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CalculatorViewModel(
    private val engine: CalculatorEngine = CalculatorEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private val tokens = mutableListOf<String>()
    private val rawExpression = StringBuilder()
    private var isNewOperand = true
    private var isCalculated = false

    fun onDigitClicked(digit: Char) {
        if (_uiState.value.isError || isCalculated) {
            onClearClicked()
        }

        rawExpression.append(digit)

        _uiState.update { current ->
            val newDisplay = if (isNewOperand || current.displayText == "0") {
                digit.toString()
            } else {
                current.displayText + digit
            }
            current.copy(displayText = newDisplay, isError = false, isCommitted = false)
        }
        isNewOperand = false
        isCalculated = false

        updateLiveState()
    }

    fun onDecimalClicked() {
        if (_uiState.value.isError || isCalculated) {
            onClearClicked()
        }

        if (rawExpression.isEmpty() || isNewOperand) {
            rawExpression.append("0.")
        } else {
            // Avoid duplicate decimal in current number segment
            val currentSeg = rawExpression.takeLastWhile { it.isDigit() || it == '.' }
            if (!currentSeg.contains('.')) {
                rawExpression.append(".")
            }
        }

        _uiState.update { current ->
            val newDisplay = if (isNewOperand) {
                "0."
            } else if (!current.displayText.contains(".")) {
                current.displayText + "."
            } else {
                current.displayText
            }
            current.copy(displayText = newDisplay, isError = false, isCommitted = false)
        }
        isNewOperand = false
        isCalculated = false

        updateLiveState()
    }

    fun onOperatorClicked(operator: CalculatorOperator) {
        if (_uiState.value.isError) {
            return
        }

        if (isCalculated) {
            // Continue calculating from result
            isCalculated = false
            tokens.clear()
            tokens.add(_uiState.value.displayText)
            tokens.add(operator.symbol)
            rawExpression.clear()
            rawExpression.append(_uiState.value.displayText).append(" ").append(operator.symbol).append(" ")
            isNewOperand = true
            _uiState.update {
                it.copy(
                    expressionPreview = tokens.joinToString(" "),
                    isCommitted = false,
                    isError = false
                )
            }
            updateLiveState()
            return
        }

        val currentDisplay = _uiState.value.displayText

        if (isNewOperand && tokens.isNotEmpty() && CalculatorOperator.isOperator(tokens.last())) {
            tokens[tokens.lastIndex] = operator.symbol
            // Replace operator in rawExpression
            val s = rawExpression.toString().trimEnd()
            val lastOpIdx = s.lastIndexOfAny(charArrayOf('+', '-', '×', '÷'))
            if (lastOpIdx >= 0) {
                rawExpression.clear()
                rawExpression.append(s.substring(0, lastOpIdx)).append(operator.symbol).append(" ")
            }
        } else {
            tokens.add(currentDisplay)
            tokens.add(operator.symbol)
            if (rawExpression.isNotEmpty() && !rawExpression.endsWith(" ")) {
                rawExpression.append(" ")
            }
            rawExpression.append(operator.symbol).append(" ")
        }

        isNewOperand = true
        isCalculated = false

        _uiState.update {
            it.copy(
                expressionPreview = tokens.joinToString(" "),
                isCommitted = false,
                isError = false
            )
        }

        updateLiveState()
    }

    fun onParenthesisClicked() {
        if (_uiState.value.isError || isCalculated) {
            onClearClicked()
        }

        val expr = rawExpression.toString().trim()
        val openCount = expr.count { it == '(' }
        val closeCount = expr.count { it == ')' }

        val canClose = openCount > closeCount && expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == ')' || expr.last() == '%')

        val toAppend = if (canClose) {
            ")"
        } else {
            if (expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == ')')) {
                " × ("
            } else if (expr.isNotEmpty() && expr.last() != '(' && !expr.last().isWhitespace()) {
                " ("
            } else {
                "("
            }
        }

        rawExpression.append(toAppend)
        updateLiveState()
    }

    fun onPercentClicked() {
        if (_uiState.value.isError || isCalculated) {
            return
        }

        val expr = rawExpression.toString().trim()
        if (expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == ')')) {
            rawExpression.append("%")
            updateLiveState()
        }
    }

    fun onEqualsClicked() {
        if (_uiState.value.isError || isCalculated) {
            return
        }

        val expr = rawExpression.toString().trim()
        if (expr.isNotEmpty() && (expr.any { it in "+-×÷*/%" } || expr.contains('('))) {
            val evalResult = engine.evaluateExpression(expr)
            val fullExpr = "$expr ="

            when (evalResult) {
                is CalculationResult.Success -> {
                    tokens.clear()
                    isNewOperand = true
                    isCalculated = true
                    rawExpression.clear()
                    rawExpression.append(evalResult.value)
                    _uiState.update {
                        it.copy(
                            displayText = evalResult.value,
                            expressionPreview = fullExpr,
                            liveExpression = fullExpr,
                            liveResult = evalResult.value,
                            isCommitted = true,
                            isError = false
                        )
                    }
                    return
                }
                is CalculationResult.Error -> {
                    tokens.clear()
                    isNewOperand = true
                    isCalculated = true
                    _uiState.update {
                        it.copy(
                            displayText = evalResult.message,
                            expressionPreview = fullExpr,
                            liveExpression = fullExpr,
                            liveResult = evalResult.message,
                            isCommitted = true,
                            isError = true
                        )
                    }
                    return
                }
            }
        }

        if (tokens.isEmpty()) {
            return
        }

        val currentDisplay = _uiState.value.displayText
        val evaluationTokens = ArrayList(tokens)
        evaluationTokens.add(currentDisplay)

        val fullExpression = evaluationTokens.joinToString(" ") + " ="

        when (val result = engine.evaluate(evaluationTokens)) {
            is CalculationResult.Success -> {
                _uiState.update {
                    it.copy(
                        displayText = result.value,
                        expressionPreview = fullExpression,
                        liveExpression = fullExpression,
                        liveResult = result.value,
                        isCommitted = true,
                        isError = false
                    )
                }
                tokens.clear()
                isNewOperand = true
                isCalculated = true
                rawExpression.clear()
                rawExpression.append(result.value)
            }
            is CalculationResult.Error -> {
                _uiState.update {
                    it.copy(
                        displayText = result.message,
                        expressionPreview = fullExpression,
                        liveExpression = fullExpression,
                        liveResult = result.message,
                        isCommitted = true,
                        isError = true
                    )
                }
                tokens.clear()
                isNewOperand = true
                isCalculated = true
            }
        }
    }

    fun onBackspaceClicked() {
        if (_uiState.value.isError || isCalculated) {
            onClearClicked()
            return
        }

        if (rawExpression.isNotEmpty()) {
            var s = rawExpression.toString().trimEnd()
            if (s.isNotEmpty()) {
                s = s.dropLast(1).trimEnd()
            }
            rawExpression.clear()
            rawExpression.append(s)
        }

        _uiState.update { current ->
            val text = current.displayText
            val newText = when {
                text.length > 1 && text.startsWith("-") && text.length == 2 -> "0"
                text.length > 1 -> text.dropLast(1)
                else -> "0"
            }
            if (newText == "0") {
                isNewOperand = true
            }
            current.copy(displayText = newText, isError = false)
        }

        updateLiveState()
    }

    fun onNegateClicked() {
        if (_uiState.value.isError) {
            return
        }

        _uiState.update { current ->
            val text = current.displayText
            val newText = when {
                text == "0" -> "0"
                text.startsWith("-") -> text.drop(1)
                else -> "-$text"
            }
            current.copy(displayText = newText, isError = false)
        }

        if (rawExpression.isNotEmpty()) {
            val expr = rawExpression.toString()
            if (expr.startsWith("-")) {
                rawExpression.clear()
                rawExpression.append(expr.drop(1))
            } else {
                rawExpression.insert(0, "-")
            }
        }

        updateLiveState()
    }

    fun onClearClicked() {
        tokens.clear()
        rawExpression.clear()
        isNewOperand = true
        isCalculated = false
        _uiState.value = CalculatorUiState()
    }

    private fun updateLiveState() {
        val expr = rawExpression.toString().trim()
        val livePreview = if (expr.isNotEmpty() && (expr.any { it in "+-×÷*/%" } || expr.contains('('))) {
            when (val res = engine.evaluateExpression(expr)) {
                is CalculationResult.Success -> {
                    if (res.value != expr && res.value.isNotEmpty()) res.value else ""
                }
                is CalculationResult.Error -> ""
            }
        } else {
            ""
        }

        _uiState.update {
            it.copy(
                liveExpression = expr,
                liveResult = livePreview
            )
        }
    }
}
