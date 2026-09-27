package com.example.androidapp.vardiya.data.repository

import android.content.SharedPreferences
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

class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (values[key] as? MutableSet<String>) ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = (values[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (values[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (values[key] as? Float) ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (values[key] as? Boolean) ?: defValue
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(values)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val target: MutableMap<String, Any>) : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) pending[key] = values
            return this
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) pending[key] = null
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            target.clear()
            pending.clear()
            return this
        }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            for ((k, v) in pending) {
                if (v == null) {
                    target.remove(k)
                } else {
                    target[k] = v
                }
            }
            pending.clear()
        }
    }
}

class LocalVardiyaRepositoryTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var repository: LocalVardiyaRepository

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        repository = LocalVardiyaRepository(fakePrefs)
    }

    @Test
    fun testDefaultSalaryConfiguration() {
        val config = repository.getSalaryConfiguration()
        assertEquals(BigDecimal("28000"), config.monthlySalary)
        assertEquals(22, config.monthlyWorkDays)
        assertEquals(BigDecimal("8"), config.dailyWorkHours)
        assertEquals("₺", config.currencySymbol)
    }

    @Test
    fun testSaveAndRestoreSalaryConfiguration() {
        val customConfig = SalaryConfiguration(
            monthlySalary = BigDecimal("35000"),
            monthlyWorkDays = 20,
            dailyWorkHours = BigDecimal("7.5"),
            breakMinutes = 45,
            deductBreakFromSalary = true,
            currencySymbol = "$",
            currencyCode = "USD"
        )

        repository.saveSalaryConfiguration(customConfig)
        val restored = repository.getSalaryConfiguration()

        assertEquals(BigDecimal("35000"), restored.monthlySalary)
        assertEquals(20, restored.monthlyWorkDays)
        assertEquals(BigDecimal("7.5"), restored.dailyWorkHours)
        assertEquals(45, restored.breakMinutes)
        assertTrue(restored.deductBreakFromSalary)
        assertEquals("$", restored.currencySymbol)
        assertEquals("USD", restored.currencyCode)
    }

    @Test
    fun testSaveAndRestoreActiveShift() {
        val shift = Shift(
            id = "shift-12345",
            startEpochMillis = 1700000000000L,
            startElapsedRealtime = 5000000L,
            lastResumeEpochMillis = 1700000500000L,
            accumulatedActiveElapsedMs = 120000L,
            lastResumeElapsedRealtime = 5120000L,
            state = ShiftState.RUNNING
        )

        repository.saveActiveShift(shift)
        val restored = repository.getActiveShift()

        assertNotNull(restored)
        assertEquals("shift-12345", restored?.id)
        assertEquals(ShiftState.RUNNING, restored?.state)
        assertEquals(1700000000000L, restored?.startEpochMillis)
        assertEquals(1700000500000L, restored?.lastResumeEpochMillis)
        assertEquals(120000L, restored?.accumulatedActiveElapsedMs)
    }

    @Test
    fun testClearActiveShift() {
        val shift = Shift(id = "shift-1", state = ShiftState.RUNNING)
        repository.saveActiveShift(shift)
        assertNotNull(repository.getActiveShift())

        repository.saveActiveShift(null)
        assertNull(repository.getActiveShift())
    }

    @Test
    fun testHistorySnapshotPreservation() {
        val snapshotConfig = SalaryConfiguration(
            monthlySalary = BigDecimal("24000"),
            monthlyWorkDays = 20,
            dailyWorkHours = BigDecimal("8")
        )

        val record = CompletedShiftRecord(
            id = "rec-1",
            dateFormatted = "28 Eylül 2026",
            timeRangeFormatted = "08:00 — 17:00",
            durationFormatted = "8s 0dk",
            earnedFormatted = "₺1.200,00",
            totalEarned = BigDecimal("1200.00"),
            activeDurationMs = 28800000L,
            totalDurationMs = 32400000L,
            startEpochMillis = 1700000000000L,
            finishEpochMillis = 1700032400000L,
            salaryConfigSnapshot = snapshotConfig
        )

        repository.addShiftToHistory(record)

        val history = repository.getShiftHistory()
        assertEquals(1, history.size)
        val loaded = history.first()

        assertEquals("rec-1", loaded.id)
        assertEquals(BigDecimal("1200.00"), loaded.totalEarned)
        assertEquals(BigDecimal("24000"), loaded.salaryConfigSnapshot.monthlySalary)

        // Modify active salary config to 60,000 TL
        repository.saveSalaryConfiguration(SalaryConfiguration(monthlySalary = BigDecimal("60000")))

        // Loaded history snapshot must remain 24,000 TL!
        val historyAfterConfigUpdate = repository.getShiftHistory().first()
        assertEquals(BigDecimal("24000"), historyAfterConfigUpdate.salaryConfigSnapshot.monthlySalary)
        assertEquals(BigDecimal("1200.00"), historyAfterConfigUpdate.totalEarned)
    }
}
