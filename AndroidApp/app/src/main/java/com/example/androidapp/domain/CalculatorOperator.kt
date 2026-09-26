package com.example.androidapp.domain

enum class CalculatorOperator(val symbol: String, val precedence: Int) {
    ADD("+", 1),
    SUBTRACT("-", 1),
    MULTIPLY("×", 2),
    DIVIDE("÷", 2);

    companion object {
        fun fromSymbol(symbol: String): CalculatorOperator? =
            entries.firstOrNull { it.symbol == symbol }

        fun isOperator(symbol: String): Boolean =
            fromSymbol(symbol) != null
    }
}
