package com.synergy.datahub.teleofis.utils

/**
 * Утилиты для работы с GSM (сигналы, Дб - децибелы и пр).
 */


/**
 * Алгоритм перевода значений регистратора импульсов (RSSI) в децибелы (дБ).
 */
fun gsmSignalLevel(rssi: Int): Int =
    if (rssi > 0 && rssi < 32) -113 + 2 * rssi else 0

/**
 * Замечающий алгоритм оценки разряда батарейки:
 * Напряжение на батарейке при выключенном GSM модеме, но включенной нагрузкой 33 Ом, перед сеансом связи:
 * 3600 мВ - 100%
 * 2800 мВ - 0%
 * Если значение более 3600 мВ или менее 2800 мВ  – значение не передается.
 */
fun voltageToPercentOrNull(voltage: Int): Int? {
    val minVoltage = 2800.0
    val maxVoltage = 3600.0
    if (voltage < minVoltage || voltage > maxVoltage) {
        return null
    }
    val percent = ((voltage - minVoltage) / (maxVoltage - minVoltage) * 100)
    return percent.toInt()
}