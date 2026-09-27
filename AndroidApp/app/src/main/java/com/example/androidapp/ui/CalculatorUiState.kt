package com.example.androidapp.ui

data class CalculatorUiState(
    val displayText: String = "0",
    val expressionPreview: String = "",
    val liveExpression: String = "",
    val liveResult: String = "",
    val isCommitted: Boolean = false,
    val isError: Boolean = false
)
