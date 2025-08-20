package com.synergy.datahub.teleofis.service.handler

import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.api.MessageSender
import com.synergy.datahub.teleofis.model.AuthMessage
import com.synergy.datahub.teleofis.model.PacketType.SERVER_AUTHORIZATION_WHEN_SPORADIC_DATA_DISABLED
import com.synergy.datahub.teleofis.service.WorkModeService
import com.synergy.datahub.teleofis.service.handler.TelemetryPackageHandler.Companion.getDeviceInfo
import com.synergy.device.model.WorkMode
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Первый пакет в режиме отключенных спордический данных.
 * В текущей версии не поддерживается!
 */
@Component
class AuthHandler(
    private val changeWorkModeService: WorkModeService,
) : PackageHandler<AuthMessage> {
    private val log = KotlinLogging.logger {}

    override fun handle(device: TeleofisDevice, message: AuthMessage, sender: MessageSender) {
        val deviceInfo = device.getDeviceInfo()
        log.trace { "Получен пакет авторизации" }
        log.warn { "${deviceInfo}Прибор работает режиме отключенных спорадических данных " }
        changeWorkModeService.changeWorkMode(device, sender, WorkMode.SporadicOff)
        sender.send(device, SERVER_AUTHORIZATION_WHEN_SPORADIC_DATA_DISABLED)
        log.trace { "${deviceInfo}Отправлено подтверждение пакет авторизации" }
    }


}