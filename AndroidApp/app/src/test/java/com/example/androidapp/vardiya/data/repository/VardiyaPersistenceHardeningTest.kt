package com.example.androidapp.vardiya.data.repository

import com.example.androidapp.vardiya.domain.model.CompletedShiftRecord
import com.example.androidapp.vardiya.domain.model.SalaryConfiguration
import com.example.androidapp.vardiya.domain.model.Shift
import com.example.androidapp.vardiya.domain.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VardiyaPersistenceHardeningTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var repository: LocalVardiyaRepository

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        repository = LocalVardiyaRepository(fakePrefs)
    }

    @Test
    fun testEscapeAndUnescapeRoundtrip() {
        val testStrings = listOf(
            "normal_id",
            "id:::FLD:::with_field_delimiter",
            "id###REC###with_record_delimiter",
            "both:::FLD:::and###REC###together",
            "backslashes\\\\and\\colons:and#hashes",
            "₺ TL with spaces and symbols",
            "Emoji 🚀 and Turkish chars ğüşiöç ĞÜŞİÖÇ",
            "",
            "   ",
            ":::FLD::::::FLD::::::FLD:::"
        )

        for (original in testStrings) {
            val escaped = LocalVardiyaRepository.escapeField(original)
            // Delimiters must NEVER appear in escaped string
            assertTrue(!escaped.contains(":::FLD:::"))
            assertTrue(!escaped.contains("###REC###"))

            val unescaped = LocalVardiyaRepository.unescapeField(escaped)
            assertEquals(original, unescaped)
        }
    }

    @Test
    fun testDelimiterInjectionImmunityInHistoryRecords() {
        // Create a malicious or unusual record with delimiter strings in text fields
        val maliciousRecord = CompletedShiftRecord(
            id = "shift:::FLD:::malicious###REC###id",
            dateFormatted = "28:::FLD:::Eylül###REC###2026",
            timeRangeFormatted = "08:00:::FLD:::—###REC:::17:00",
            durationFormatted = "8s:::FLD:::0dk",
            earnedFormatted = "₺1.200:::FLD:::00",
            totalEarned = BigDecimal("1200.00"),
            activeDurationMs = 28800000L,
            totalDurationMs = 28800000L,
            startEpochMillis = 1700000000000L,
            finishEpochMillis = 1700028800000L,
            salaryConfigSnapshot = SalaryConfiguration(
                currencySymbol = "₺:::FLD:::#",
                currencyCode = "TRY###REC###"
            ),
            currencySymbol = "₺:::FLD:::#",
            currencyCode = "TRY###REC###"
        )

        repository.addShiftToHistory(maliciousRecord)

        val restoredHistory = repository.getShiftHistory()
        assertEquals(1, restoredHistory.size)

        val restored = restoredHistory.first()
        assertEquals("shift:::FLD:::malicious###REC###id", restored.id)
        assertEquals("28:::FLD:::Eylül###REC###2026", restored.dateFormatted)
        assertEquals("08:00:::FLD:::—###REC:::17:00", restored.timeRangeFormatted)
        assertEquals("₺:::FLD:::#", restored.currencySymbol)
        assertEquals("TRY###REC###", restored.currencyCode)
        assertEquals(BigDecimal("1200.00"), restored.totalEarned)
    }

    @Test
    fun testMalformedRecordsInHistoryRecovery() {
        // Manually inject a corrupted raw string with a mix of valid and corrupted records
        val validRecord1 = "rec-1:::FLD:::28 Eyl:::FLD:::08-17:::FLD:::8s:::FLD:::100TL:::FLD:::100.00:::FLD:::28800000:::FLD:::1700000000000:::FLD:::1700028800000"
        val corruptedRecordShort = "corrupted:::FLD:::only_two_fields"
        val corruptedRecordBadNumber = "rec-bad:::FLD:::28 Eyl:::FLD:::08-17:::FLD:::8s:::FLD:::100TL:::FLD:::NOT_A_NUMBER:::FLD:::28800000:::FLD:::1700000000000:::FLD:::1700028800000"
        val validRecord2 = "rec-2:::FLD:::29 Eyl:::FLD:::08-17:::FLD:::8s:::FLD:::150TL:::FLD:::150.00:::FLD:::28800000:::FLD:::1700100000000:::FLD:::1700128800000"

        val rawWithCorruptions = "$validRecord1###REC###$corruptedRecordShort###REC###$corruptedRecordBadNumber###REC###$validRecord2"
        fakePrefs.edit().putString("shift_history_list", rawWithCorruptions).apply()

        val history = repository.getShiftHistory()
        // Must recover 2 valid records without throwing any exception!
        assertEquals(2, history.size)
        assertEquals("rec-1", history[0].id)
        assertEquals("rec-2", history[1].id)
    }

    @Test
    fun testEmptyAndBlankHistorySafety() {
        fakePrefs.edit().putString("shift_history_list", "").apply()
        assertEquals(emptyList<CompletedShiftRecord>(), repository.getShiftHistory())

        fakePrefs.edit().putString("shift_history_list", "   ###REC###   ").apply()
        assertEquals(emptyList<CompletedShiftRecord>(), repository.getShiftHistory())

        fakePrefs.edit().remove("shift_history_list").apply()
        assertEquals(emptyList<CompletedShiftRecord>(), repository.getShiftHistory())
    }

    @Test
    fun testHistoryFifoTrimmingToFiftyItems() {
        // Add 65 records
        for (i in 1..65) {
            val record = CompletedShiftRecord(
                id = "shift-$i",
                dateFormatted = "Date $i",
                timeRangeFormatted = "08:00 — 17:00",
                durationFormatted = "8s",
                earnedFormatted = "100 TL",
                totalEarned = BigDecimal("100"),
                activeDurationMs = 28800000L,
                totalDurationMs = 28800000L,
                startEpochMillis = 1700000000000L + i * 1000,
                finishEpochMillis = 1700028800000L + i * 1000
            )
            repository.addShiftToHistory(record)
        }

        val history = repository.getShiftHistory()
        // Exactly 50 items preserved
        assertEquals(50, history.size)
        // Newest record added is shift-65 (first in list)
        assertEquals("shift-65", history.first().id)
        // Oldest preserved is shift-16 (records 1 to 15 were trimmed out)
        assertEquals("shift-16", history.last().id)
    }

    @Test
    fun testCorruptedActiveShiftRecovery() {
        // Corrupted shift state enum value
        fakePrefs.edit()
            .putString("shift_id", "shift-bad-state")
            .putString("shift_state", "TOTALLY_UNKNOWN_STATE")
            .apply()

        val shift = repository.getActiveShift()
        // Safe recovery without throwing IllegalArgumentException
        assertNull(shift)
    }
}
