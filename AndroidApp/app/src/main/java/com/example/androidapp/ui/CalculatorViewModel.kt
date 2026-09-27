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
    private var isNewOperand = true
    private var isCalculated = false

    fun onDigitClicked(digit: Char) {
        if (_uiState.value.isError || isCalculated) {
            onClearClicked()
        }

        _uiState.update { current ->
            val newDisplay = if (isNewOperand || current.displayText == "0") {
                digit.toString()
            } else {
                current.displayText + digit
            }
            current.copy(displayText = newDisplay, isError = false)
        }
        isNewOperand = false
        isCalculated = false
    }

    fun onDecimalClicked() {
        if (_uiState.value.isError || isCalculated) {
            onClearClicked()
        }

        _uiState.update { current ->
            val newDisplay = if (isNewOperand) {
                "0."
            } else if (!current.displayText.contains(".")) {
                current.displayText + "."
            } else {
                current.displayText
            }
            current.copy(displayText = newDisplay, isError = false)
        }
        isNewOperand = false
        isCalculated = false
    }

    fun onOperatorClicked(operator: CalculatorOperator) {
        if (_uiState.value.isError) {
            return
        }

        val currentDisplay = _uiState.value.displayText

        if (isNewOperand && tokens.isNotEmpty() && CalculatorOperator.isOperator(tokens.last())) {
            tokens[tokens.lastIndex] = operator.symbol
        } else {
            tokens.add(currentDisplay)
            tokens.add(operator.symbol)
        }

        isNewOperand = true
        isCalculated = false

        _uiState.update {
            it.copy(
                expressionPreview = tokens.joinToString(" "),
                isError = false
            )
        }
    }

    fun onEqualsClicked() {
        if (_uiState.value.isError || isCalculated || tokens.isEmpty()) {
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
                        isError = false
                    )
                }
                tokens.clear()
                isNewOperand = true
                isCalculated = true
            }
            is CalculationResult.Error -> {
                _uiState.update {
                    it.copy(
                        displayText = result.message,
                        expressionPreview = fullExpression,
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
    }

    fun onClearClicked() {
        tokens.clear()
        isNewOperand = true
        isCalculated = false
        _uiState.value = CalculatorUiState()
    }
}
