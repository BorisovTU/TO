package com.synergy.datahub.teleofis.service.handler

import com.synergy.datahub.device.api.DeviceMessage
import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.api.MessageSender
import org.springframework.core.GenericTypeResolver

/**
 * Обработчик пакетов полученных с регистратора импульсов
 */
interface PackageHandler<T : DeviceMessage> {
    @Suppress("UNCHECKED_CAST")
    val messageType: Class<T> get() = GenericTypeResolver.resolveTypeArgument(javaClass, PackageHandler::class.java) as Class<T>

    fun handle(device: TeleofisDevice, message: T, sender: MessageSender)
}