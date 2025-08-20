package com.synergy.datahub.teleofis.mapper

import com.synergy.common.device.DeviceDataPoint
import com.synergy.common.device.DeviceDataType
import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.model.ErrorCodes
import com.synergy.datahub.teleofis.model.mapper.TelemetryMessage
import com.synergy.device.model.TeleofisMeasurePoint
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class InstantMapper {
    fun mapToInstant(device: TeleofisDevice, telemetryMessage: TelemetryMessage): List<Pair<TeleofisMeasurePoint, DeviceDataPoint>> {
        val values = telemetryMessage.getValues()
        return device.meters
            .map { meter ->
                val channel = if (meter.impulseRegChannel > 0) meter.impulseRegChannel - 1 else 0
                val state = getDeviceState(telemetryMessage, meter.impulseRegChannel)
                val imp = values[channel].toDouble()
                val measurePoint = meter.measurePoint
                measurePoint to DeviceDataPoint(
                    deviceId = measurePoint.id.toString(),
                    dataTs = ZonedDateTime.now(),
                    type = DeviceDataType.INSTANT,
                    receiveTs = ZonedDateTime.now(),
                    deviceTs = telemetryMessage.getCurrentTime()
                ).apply {
                    /** Нарастающий итог объема в трубопроводе холодного водоснабжения, импульсов */
                    addParameter("VW_IMP", imp)
                    /** Текущие ошибки прибора учета */
                    addParameter("ER", state.toUInt())
                }
            }
    }

    private fun getDeviceState(telemetryMessage: TelemetryMessage, regChannel: Int): Int {
        var result = if (telemetryMessage.getInputState6() == 2) ErrorCodes.REGISTER_OPENED.toInt() else ErrorCodes.OK.toInt()
        when (regChannel) {
            1 -> telemetryMessage.getInputState1().let { state -> result = result or getCode(state) }
            2 -> telemetryMessage.getInputState2().let { state -> result = result or getCode(state) }
            3 -> telemetryMessage.getInputState3().let { state -> result = result or getCode(state) }
        }
        result = result or if (telemetryMessage.getInputState4() == 1) ErrorCodes.MAGNETIC_FIELD_SENSOR.toInt() else ErrorCodes.OK.toInt()
        result =
            result or ErrorCodes.OK.toInt() or if (telemetryMessage.getBatteryVoltageBeforeConnection() <= 3000) ErrorCodes.BATTERY_LOW.toInt() else ErrorCodes.OK.toInt()
        return result
    }

    private fun getCode(state: Int) =
        when (state) {
            1 -> ErrorCodes.LINE_SHORT_CIRCUIT.toInt()//КЗ
            2 -> ErrorCodes.LINE_BREAK.toInt()//Обрыв линии связи с ПУ ХВС/ГВС
            else -> ErrorCodes.OK.toInt()
        }

}
