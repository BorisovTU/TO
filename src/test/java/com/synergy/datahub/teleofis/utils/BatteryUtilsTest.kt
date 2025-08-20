package com.synergy.datahub.teleofis.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

class BatteryUtilsTest {

    @Test
    fun `возвращает null если напряжение меньше 2800 мВ`() {
        assertNull(voltageToPercentOrNull(2799))
    }

    @Test
    fun `возвращает 0 процентов при ровно 2800 мВ`() {
        assertEquals(0, voltageToPercentOrNull(2800))
    }

    @Test
    fun `возвращает 100 процентов при ровно 3600 мВ`() {
        assertEquals(100, voltageToPercentOrNull(3600))
    }

    @Test
    fun `возвращает null если напряжение выше 3600 мВ`() {
        assertNull(voltageToPercentOrNull(3601))
    }

    @Test
    fun `возвращает 50 процентов при 3200 мВ`() {
        assertEquals(50, voltageToPercentOrNull(3200))
    }
}