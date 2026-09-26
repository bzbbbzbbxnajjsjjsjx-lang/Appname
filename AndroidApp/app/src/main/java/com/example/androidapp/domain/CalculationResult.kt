package com.example.androidapp.domain

sealed interface CalculationResult {
    data class Success(val value: String) : CalculationResult
    data class Error(val message: String) : CalculationResult
}
