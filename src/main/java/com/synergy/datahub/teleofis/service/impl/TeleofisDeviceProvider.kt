package com.synergy.datahub.teleofis.service.impl

import com.synergy.common.device.DeviceType
import com.synergy.datahub.service.status.TransferStatusService
import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.service.handler.TelemetryPackageHandler.Companion.getDeviceInfo
import com.synergy.datahub.teleofis.service.handler.TeleofisDeviceStore
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class TeleofisDeviceProvider(
    private val deviceStore: TeleofisDeviceStore,
    private val notificationService: TransferStatusService
) {
    private val log = KotlinLogging.logger {}

    fun getDevice(imei: ULong): TeleofisDevice {
        val serial = imei.toString()
        val device = deviceStore.getAll()
            .find { device ->
                device.register.serial == serial || device.register.serial == serial.trimStart('0')
            }
        device ?: throw IllegalStateException("Неизвестный тип регистратора импульсов. IMEI: $imei ")
        if (device.register.serial != serial) {
            //В случае, если считанный серийный номер с регистратора импульсов отличается от заданного на ВУ
            //(сравнение производится без учета начальных нулей в серийном номере регистратора импульсов),
            //то на ВУ передается событие (для последующего учета в модели Нештатных ситуаций),
            val transferId = device.register.id
            val errorMsg = "${device.getDeviceInfo()}Не совпадает фактический серийный номер(IMEI) регистратора импульсов и серийный номер УПД в БД"
            notificationService.transferConfigurationError(transferId, deviceType = DeviceType.Impulse, errorMsg)
            log.error { errorMsg }
        }
        return device
    }

    fun getDevice(transportKey: String): TeleofisDevice {
        return deviceStore.get(transportKey).first()
    }


}