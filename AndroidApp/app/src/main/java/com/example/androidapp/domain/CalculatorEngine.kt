package com.example.androidapp.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.ArrayDeque

class CalculatorEngine {

    companion object {
        private const val DIVISION_SCALE = 12
    }

    fun evaluate(tokens: List<String>): CalculationResult {
        if (tokens.isEmpty()) {
            return CalculationResult.Success("0")
        }

        // Normalize consecutive operators
        val normalized = normalizeTokens(tokens)

        // Drop any trailing operator for evaluation
        val evaluatable = if (normalized.isNotEmpty() && CalculatorOperator.isOperator(normalized.last())) {
            normalized.dropLast(1)
        } else {
            normalized
        }

        if (evaluatable.isEmpty()) {
            return CalculationResult.Success("0")
        }

        if (evaluatable.size == 1) {
            return try {
                CalculationResult.Success(formatNumber(evaluatable.first()))
            } catch (e: Exception) {
                CalculationResult.Error("Invalid input")
            }
        }

        return try {
            val rpn = shuntingYard(evaluatable)
            evaluateRpn(rpn)
        } catch (e: ArithmeticException) {
            CalculationResult.Error(e.message ?: "Calculation error")
        } catch (e: Exception) {
            CalculationResult.Error("Invalid expression")
        }
    }

    fun normalizeTokens(tokens: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (token in tokens) {
            if (CalculatorOperator.isOperator(token)) {
                if (result.isNotEmpty() && CalculatorOperator.isOperator(result.last())) {
                    result[result.lastIndex] = token
                } else if (result.isNotEmpty()) {
                    result.add(token)
                }
            } else {
                result.add(token)
            }
        }
        return result
    }

    fun formatNumber(raw: String): String {
        val clean = if (raw.startsWith(".")) "0$raw" else raw
        val bd = BigDecimal(clean)
        return formatBigDecimal(bd)
    }

    private fun formatBigDecimal(bd: BigDecimal): String {
        return if (bd.compareTo(BigDecimal.ZERO) == 0) {
            "0"
        } else {
            bd.stripTrailingZeros().toPlainString()
        }
    }

    private fun shuntingYard(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val operatorStack = ArrayDeque<CalculatorOperator>()

        for (token in tokens) {
            val op = CalculatorOperator.fromSymbol(token)
            if (op != null) {
                // Left-associative: pop operators with greater or equal precedence
                while (operatorStack.isNotEmpty() && (operatorStack.peek()?.precedence ?: 0) >= op.precedence) {
                    output.add(operatorStack.pop().symbol)
                }
                operatorStack.push(op)
            } else {
                output.add(token)
            }
        }

        while (operatorStack.isNotEmpty()) {
            output.add(operatorStack.pop().symbol)
        }

        return output
    }

    private fun evaluateRpn(rpn: List<String>): CalculationResult {
        val stack = ArrayDeque<BigDecimal>()

        for (token in rpn) {
            val op = CalculatorOperator.fromSymbol(token)
            if (op != null) {
                if (stack.size < 2) {
                    return CalculationResult.Error("Invalid expression")
                }
                val b = stack.pop()
                val a = stack.pop()

                val result = when (op) {
                    CalculatorOperator.ADD -> a.add(b)
                    CalculatorOperator.SUBTRACT -> a.subtract(b)
                    CalculatorOperator.MULTIPLY -> a.multiply(b)
                    CalculatorOperator.DIVIDE -> {
                        if (b.compareTo(BigDecimal.ZERO) == 0) {
                            return CalculationResult.Error("Cannot divide by zero")
                        }
                        try {
                            a.divide(b)
                        } catch (e: ArithmeticException) {
                            a.divide(b, DIVISION_SCALE, RoundingMode.HALF_UP)
                        }
                    }
                }
                stack.push(result)
            } else {
                val clean = if (token.startsWith(".")) "0$token" else token
                stack.push(BigDecimal(clean))
            }
        }

        return if (stack.size == 1) {
            CalculationResult.Success(formatBigDecimal(stack.pop()))
        } else {
            CalculationResult.Error("Invalid expression")
        }
    }
}
