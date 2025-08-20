package com.synergy.datahub.teleofis.service.handler

import com.synergy.common.device.DeviceId
import com.synergy.datahub.config.DataHubProperties
import com.synergy.datahub.device.DeviceStore
import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.device.model.TeleofisMeter
import com.synergy.spring.device.DeviceConfigStoreApi
import org.springframework.stereotype.Component

@Component
class TeleofisDeviceStore(
    dataHubProperties: DataHubProperties,
    private val configStore: DeviceConfigStoreApi<TeleofisMeter>,
) : DeviceStore<TeleofisDevice> {
    private val filters = dataHubProperties.filters

    override fun getAll(filter: (TeleofisDevice) -> Boolean): List<TeleofisDevice> =
        configStore.getAllMeters()
            .filter { meter -> isSuitableDeviceByConfig(meter) }
            .groupBy { it.registerImpulse }
            .map { (registrar, meters) -> TeleofisDevice(registrar, meters) }

    override fun get(transportKey: String, filter: (TeleofisDevice) -> Boolean): List<TeleofisDevice> {
        return getAll { device -> device.getTransportKey() == transportKey }.toList()
    }

    override fun get(deviceId: DeviceId): TeleofisDevice? {
        return getAll().find { device ->
            device.register.id == deviceId.transferId &&
                    device.meters.any { meter ->
                        meter.id == deviceId.meterId && meter.measurePoint.id == deviceId.measurePointId
                    }
        }
    }

    override fun getByTransferId(transferId: Long): List<TeleofisDevice> {
        return configStore.getMeterByTransferId(transferId)
            .filter { meter -> isSuitableDeviceByConfig(meter) }
            .groupBy { it.registerImpulse }
            .map { (registrar, meters) -> TeleofisDevice(registrar, meters) }
    }

    private fun isSuitableDeviceByConfig(meter: TeleofisMeter): Boolean {
        return filters.transfers.serials(meter.registerImpulse.serial)
                && filters.transfers.models(meter.registerImpulse.model)
                && filters.transfers.protocols(meter.registerImpulse.protocol ?: "undefined")
    }

}