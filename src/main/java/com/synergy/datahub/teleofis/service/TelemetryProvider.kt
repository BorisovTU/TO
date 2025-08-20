package com.synergy.datahub.teleofis.service

import com.synergy.datahub.teleofis.model.mapper.TelemetryMessage

interface TelemetryProvider {
    /**
     * Получить последний полученный пакет телеметрии
     */
    fun get(imei: ULong): TelemetryMessage?
}