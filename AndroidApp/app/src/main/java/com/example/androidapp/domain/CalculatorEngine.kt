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

    fun evaluateExpression(rawExpr: String): CalculationResult {
        if (rawExpr.isBlank()) {
            return CalculationResult.Success("0")
        }

        // Normalize unicode symbols & remove whitespace
        var expr = rawExpr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(" ", "")

        if (expr.isEmpty()) {
            return CalculationResult.Success("0")
        }

        // Auto-close open parentheses for live preview
        val openCount = expr.count { it == '(' }
        val closeCount = expr.count { it == ')' }
        if (openCount > closeCount) {
            expr += ")".repeat(openCount - closeCount)
        }

        // Drop trailing operators for evaluation
        while (expr.isNotEmpty() && (expr.last() in "+-*/(")) {
            expr = expr.dropLast(1)
        }
        if (expr.isEmpty()) {
            return CalculationResult.Success("0")
        }

        return try {
            val tokens = tokenizeExpression(expr)
            if (tokens.isEmpty()) {
                return CalculationResult.Success("0")
            }
            val rpn = shuntingYardExpression(tokens)
            evaluateRpnExpression(rpn)
        } catch (e: ArithmeticException) {
            CalculationResult.Error(e.message ?: "Calculation error")
        } catch (e: Exception) {
            CalculationResult.Error("Invalid expression")
        }
    }

    private fun tokenizeExpression(expr: String): List<String> {
        val rawTokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    rawTokens.add(sb.toString())
                }
                c in "+-*/()%" -> {
                    rawTokens.add(c.toString())
                    i++
                }
                else -> i++
            }
        }

        // Handle implicit multiplication (e.g. 16(0.5+2.5) or (3+4)(5) or 50% * 2)
        val withImplicitMul = mutableListOf<String>()
        for (idx in rawTokens.indices) {
            val t = rawTokens[idx]
            withImplicitMul.add(t)
            if (idx + 1 < rawTokens.size) {
                val next = rawTokens[idx + 1]
                val isNumOrParenOrPct = t.first().isDigit() || t.startsWith(".") || t == ")" || t == "%"
                if (isNumOrParenOrPct && next == "(") {
                    withImplicitMul.add("*")
                } else if (t == ")" && (next.first().isDigit() || next.startsWith("."))) {
                    withImplicitMul.add("*")
                }
            }
        }

        // Handle unary minus
        val tokens = mutableListOf<String>()
        for (idx in withImplicitMul.indices) {
            val t = withImplicitMul[idx]
            if (t == "-") {
                val isUnary = idx == 0 || withImplicitMul[idx - 1] in listOf("(", "+", "-", "*", "/")
                if (isUnary) {
                    tokens.add("NEG")
                } else {
                    tokens.add("-")
                }
            } else {
                tokens.add(t)
            }
        }

        return tokens
    }

    private fun shuntingYardExpression(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val ops = ArrayDeque<String>()

        fun precedence(op: String): Int = when (op) {
            "+", "-" -> 1
            "*", "/" -> 2
            "NEG" -> 3
            else -> 0
        }

        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            val isNumber = t.isNotEmpty() && (t[0].isDigit() || (t[0] == '.' && t.length > 1) || (t == "."))
            if (isNumber) {
                // Check if followed by %
                if (i + 1 < tokens.size && tokens[i + 1] == "%") {
                    val rawNum = if (t.startsWith(".")) "0$t" else t
                    val numBd = BigDecimal(rawNum)
                    // If preceding binary operator in ops is + or -: A + B% = A + (A * B / 100)
                    val hasPrecedingAddSub = ops.isNotEmpty() && (ops.peek() == "+" || ops.peek() == "-")
                    if (hasPrecedingAddSub && output.isNotEmpty()) {
                        try {
                            val baseBd = BigDecimal(output.last())
                            val pctVal = baseBd.multiply(numBd).divide(BigDecimal("100"), DIVISION_SCALE, RoundingMode.HALF_UP)
                            output.add(formatBigDecimal(pctVal))
                        } catch (e: Exception) {
                            val pctVal = numBd.divide(BigDecimal("100"), DIVISION_SCALE, RoundingMode.HALF_UP)
                            output.add(formatBigDecimal(pctVal))
                        }
                    } else {
                        val pctVal = numBd.divide(BigDecimal("100"), DIVISION_SCALE, RoundingMode.HALF_UP)
                        output.add(formatBigDecimal(pctVal))
                    }
                    i += 2 // skip number and %
                    continue
                } else {
                    output.add(t)
                }
            } else if (t == "NEG") {
                ops.push(t)
            } else if (t in listOf("+", "-", "*", "/")) {
                while (ops.isNotEmpty() && ops.peek() != "(" && precedence(ops.peek() ?: "") >= precedence(t)) {
                    output.add(ops.pop())
                }
                ops.push(t)
            } else if (t == "(") {
                ops.push(t)
            } else if (t == ")") {
                while (ops.isNotEmpty() && ops.peek() != "(") {
                    output.add(ops.pop())
                }
                if (ops.isNotEmpty() && ops.peek() == "(") {
                    ops.pop()
                }
            }
            i++
        }

        while (ops.isNotEmpty()) {
            output.add(ops.pop())
        }

        return output
    }

    private fun evaluateRpnExpression(rpn: List<String>): CalculationResult {
        val stack = ArrayDeque<BigDecimal>()

        for (token in rpn) {
            when (token) {
                "NEG" -> {
                    if (stack.isEmpty()) return CalculationResult.Error("Invalid expression")
                    val v = stack.pop()
                    stack.push(v.negate())
                }
                "+", "-", "*", "/" -> {
                    if (stack.size < 2) return CalculationResult.Error("Invalid expression")
                    val b = stack.pop()
                    val a = stack.pop()
                    val res = when (token) {
                        "+" -> a.add(b)
                        "-" -> a.subtract(b)
                        "*" -> a.multiply(b)
                        "/" -> {
                            if (b.compareTo(BigDecimal.ZERO) == 0) {
                                return CalculationResult.Error("Cannot divide by zero")
                            }
                            try {
                                a.divide(b)
                            } catch (e: ArithmeticException) {
                                a.divide(b, DIVISION_SCALE, RoundingMode.HALF_UP)
                            }
                        }
                        else -> throw IllegalStateException()
                    }
                    stack.push(res)
                }
                else -> {
                    val clean = if (token.startsWith(".")) "0$token" else token
                    stack.push(BigDecimal(clean))
                }
            }
        }

        return if (stack.size == 1) {
            CalculationResult.Success(formatBigDecimal(stack.pop()))
        } else {
            CalculationResult.Error("Invalid expression")
        }
    }
}
