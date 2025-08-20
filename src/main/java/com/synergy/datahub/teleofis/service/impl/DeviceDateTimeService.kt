package com.synergy.datahub.teleofis.service.impl

import com.synergy.datahub.device.DeviceHistory
import com.synergy.datahub.device.DeviceInteractionService
import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.api.MessageSender
import com.synergy.datahub.teleofis.model.ParameterData
import com.synergy.datahub.teleofis.service.ParameterService
import com.synergy.datahub.teleofis.service.handler.TelemetryPackageHandler.Companion.getDeviceInfo
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Обновляет в системе последнее полученное с регистратора время
 * обновляет время регистратора если сильно отличается от системного
 */
@Component
class DeviceDateTimeService(
    private val dataHistory: DeviceHistory,
    private val deviceInteractionService: DeviceInteractionService,
    private val parameterService: ParameterService,
) {
    private val log = KotlinLogging.logger {}

    /**
     * Обновить время регистратора при получении пакета телеметрии
     */
    fun updateDeviceDateTime(device: TeleofisDevice, currentDeviceTs: ZonedDateTime, sender: MessageSender) {
        if (abs(ChronoUnit.MINUTES.between(Instant.now(), currentDeviceTs.toInstant())) > 5) {
            val afterUpdateTs = Instant.now().atZone(currentDeviceTs.zone)
            parameterService.setParameter(sender, device, ParameterData.updateDeviceTs())
            log.info { "${device.getDeviceInfo()}Время на приборе было обновлено. Время до: $currentDeviceTs. Время после обновления: $afterUpdateTs" }
        }
        dataHistory.updateDeviceTime(device, currentDeviceTs)
    }

    fun getDeviceDateTime(device: TeleofisDevice): ZonedDateTime? {
        return deviceInteractionService.getDeviceTs(device)
    }

}
