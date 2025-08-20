package com.synergy.datahub.teleofis.service

import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.api.MessageSender
import com.synergy.datahub.teleofis.model.ParameterData
import com.synergy.datahub.teleofis.service.handler.TelemetryPackageHandler.Companion.getDeviceInfo
import com.synergy.device.model.RegisterImpulse
import com.synergy.device.model.WorkMode
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Сервис позволяет менять рабочий режим устройства
 */
@Component
class WorkModeService(
    private val parameterService: ParameterService,
) {
    private val log = KotlinLogging.logger {}
    private val mapActualWorkMode = ConcurrentHashMap<Long, WorkMode>()

    /**
     * Изменить режим работы спорадический или не спорадический
     */
    fun changeWorkMode(device: TeleofisDevice, sender: MessageSender, actualWorkMode: WorkMode): Boolean {
        val transfer = device.register
        val expectedWorkMode = transfer.workMode
        setWorkMode(transfer, actualWorkMode)
        if (expectedWorkMode == actualWorkMode) {
            return false
        }
        parameterService.setParameter(
            sender, device, ParameterData.changeWorkMode(expectedWorkMode)
        )
        log.trace { "${device.getDeviceInfo()}Режим работы прибора переключен c $actualWorkMode на $expectedWorkMode режим работы" }
        return true
    }

    fun isSporadic(device: TeleofisDevice): Boolean = getWorkMode(device.register) == WorkMode.Sporadic

    fun isNotSporadic(device: TeleofisDevice): Boolean = !isSporadic(device)

    private fun setWorkMode(registerImpulse: RegisterImpulse, workMode: WorkMode) {
        mapActualWorkMode[registerImpulse.id] = workMode
    }

    private fun getWorkMode(registerImpulse: RegisterImpulse): WorkMode {
        return mapActualWorkMode[registerImpulse.id] ?: registerImpulse.workMode
    }
}