package com.synergy.datahub.teleofis.service.monitoring

import com.synergy.datahub.ext.MetricNames
import com.synergy.datahub.teleofis.TeleofisDevice
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.springframework.stereotype.Component

@Component
class TeleofisMonitoringService(
    private val meterRegistry: MeterRegistry,
) {
    fun incrementLowLvlBatteryCounter(device: TeleofisDevice, voltage: Int) {
        meterRegistry.counter(
            MetricNames.of("low_battery_percentage"),
            listOf(
                Tag.of(MetricNames.TRANSFER_ID, device.getId().toString()),
                Tag.of("imei", device.getImei().toString()),
                Tag.of("key", device.getTransportKey())
            )
        ).increment(voltage.toDouble())
    }

}