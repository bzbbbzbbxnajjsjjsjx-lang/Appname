package com.example.androidapp.ui

data class CalculatorUiState(
    val displayText: String = "0",
    val expressionPreview: String = "",
    val isError: Boolean = false
)
