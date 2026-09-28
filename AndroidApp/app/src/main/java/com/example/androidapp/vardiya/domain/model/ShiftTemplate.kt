package com.example.androidapp.vardiya.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Domain entity representing a preset shift schedule template.
 * Allows quick shift selection (e.g. Morning, Evening, Night).
 */
data class ShiftTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startHour: Int,
    val startMinute: Int = 0,
    val durationHours: BigDecimal = BigDecimal("8.0"),
    val breakMinutes: Int = 60,
    val deductBreakFromSalary: Boolean = false,
    val colorTag: Long = 0xFF2196F3
) {
    init {
        require(name.isNotBlank()) { "Template name cannot be blank" }
        require(startHour in 0..23) { "Start hour must be between 0 and 23" }
        require(startMinute in 0..59) { "Start minute must be between 0 and 59" }
        require(durationHours > BigDecimal.ZERO && durationHours <= BigDecimal("24")) {
            "Duration hours must be positive and at most 24"
        }
        require(breakMinutes >= 0) { "Break minutes cannot be negative" }
        val breakHours = BigDecimal(breakMinutes).divide(BigDecimal(60), 6, RoundingMode.HALF_UP)
        if (deductBreakFromSalary) {
            require(breakHours < durationHours) {
                "Break duration cannot exceed or equal shift duration when deducted"
            }
        }
    }

    companion object {
        /**
         * Standard system preset templates.
         */
        val PRESETS = listOf(
            ShiftTemplate(
                id = "preset_morning",
                name = "Sabah Vardiyası",
                startHour = 8,
                startMinute = 0,
                durationHours = BigDecimal("8.0"),
                breakMinutes = 60,
                deductBreakFromSalary = false,
                colorTag = 0xFF4CAF50 // Green
            ),
            ShiftTemplate(
                id = "preset_evening",
                name = "Akşam Vardiyası",
                startHour = 16,
                startMinute = 0,
                durationHours = BigDecimal("8.0"),
                breakMinutes = 60,
                deductBreakFromSalary = false,
                colorTag = 0xFFFF9800 // Orange
            ),
            ShiftTemplate(
                id = "preset_night",
                name = "Gece Vardiyası",
                startHour = 0,
                startMinute = 0,
                durationHours = BigDecimal("8.0"),
                breakMinutes = 60,
                deductBreakFromSalary = false,
                colorTag = 0xFF9C27B0 // Purple
            )
        )
    }
}
