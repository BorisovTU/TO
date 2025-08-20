package com.synergy.datahub.teleofis.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GsmSignalLevelTest {

    @Test
    fun `RSSI is 0 returns 0`() {
        assertEquals(0, gsmSignalLevel(0))
    }

    @Test
    fun `RSSI is in range 1 to 31`() {
        assertEquals(-111, gsmSignalLevel(1))   // -113 + 2*1
        assertEquals(-85, gsmSignalLevel(14))   // -113 + 2*14
        assertEquals(-51, gsmSignalLevel(31))   // -113 + 2*31
    }

    @Test
    fun `RSSI is greater than 31 returns 0`() {
        assertEquals(0, gsmSignalLevel(32))
        assertEquals(0, gsmSignalLevel(99))
    }

    @Test
    fun `RSSI is negative returns 0`() {
        assertEquals(0, gsmSignalLevel(-1))
        assertEquals(0, gsmSignalLevel(-100))
    }
}
