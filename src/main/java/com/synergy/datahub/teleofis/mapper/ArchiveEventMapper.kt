package com.synergy.datahub.teleofis.mapper

import com.synergy.common.device.DeviceDataParameter
import com.synergy.common.device.DeviceDataPoint
import com.synergy.common.device.DeviceDataType
import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.model.DataType
import com.synergy.datahub.teleofis.model.ErrorCodes
import com.synergy.datahub.teleofis.model.EventType
import com.synergy.datahub.teleofis.model.mapper.ArchiveEventMessage
import com.synergy.datahub.teleofis.model.mapper.DataEvent
import com.synergy.datahub.teleofis.service.TelemetryProvider
import com.synergy.datahub.teleofis.service.impl.DeviceDateTimeService
import com.synergy.device.model.TeleofisMeasurePoint
import com.synergy.device.model.TeleofisMeter
import com.synergy.spring.starters.kotlin.toZonedDateTime
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Component
class ArchiveEventMapper(
    private val deviceDateTimeService: DeviceDateTimeService,
    @Qualifier("telemetryPackageHandler")
    private val telemetryProvider: ObjectProvider<TelemetryProvider>,
) {
    fun mapToArchiveEvent(device: TeleofisDevice, message: ArchiveEventMessage): List<Pair<TeleofisMeasurePoint, DeviceDataPoint>> {
        //подразумевается что время устройства будет получено и сохранено
        //из первого пакета телеметрии,
        val deviceDateTime = deviceDateTimeService.getDeviceDateTime(device) ?: ZonedDateTime.now()
        return device.meters.flatMap { meter ->
            val measurePoint = meter.measurePoint
            message.events
                .flatMap { event ->
                    // Если срезы данных
                    val result = mutableListOf<DeviceDataPoint>()
                    if (event.code == EventType.TIME_INTERVAL) {
                        val value: Int? = getCounterValue(event, meter)
                        // Если суточный срез
                        if (event.dateTime.deviceTimeToSystemTime(deviceDateTime)?.truncatedTo(ChronoUnit.DAYS) == event.dateTime.deviceTimeToSystemTime(deviceDateTime)
                        ) {
                            result.add(
                                DeviceDataPoint(
                                    deviceId = measurePoint.id.toString(),
                                    dataTs = event.dateTime.deviceTimeToSystemTime(deviceDateTime)!!,
                                    type = DeviceDataType.DAY,
                                    receiveTs = ZonedDateTime.now(),
                                    deviceTs = deviceDateTime
                                ).apply {
                                    addParameter("A111", value?.toDouble())
                                }
                            )
                        }
                        // Если часовой срез
                        if (event.dateTime.deviceTimeToSystemTime(deviceDateTime)?.truncatedTo(ChronoUnit.HOURS) == event.dateTime.deviceTimeToSystemTime(
                                deviceDateTime
                            )
                        ) {
                            val deviceDataPoint = DeviceDataPoint(
                                deviceId = measurePoint.id.toString(),
                                dataTs = event.dateTime.deviceTimeToSystemTime(deviceDateTime)!!,
                                type = DeviceDataType.HOUR,
                                receiveTs = ZonedDateTime.now(),
                                deviceTs = deviceDateTime
                            )
                            result.add(
                                deviceDataPoint.apply {
                                    addParameter("A102", value?.toDouble())
                                }
                            )
                        }
                    } else {
                        // Если события регистратора
                        val ddp = DeviceDataPoint(
                            deviceId = measurePoint.id.toString(),
                            dataTs = event.dateTime.deviceTimeToSystemTime(deviceDateTime)!!,
                            type = DeviceDataType.EVENT,
                            receiveTs = ZonedDateTime.now(),
                            deviceTs = deviceDateTime
                        )
                        val temperatureInput = telemetryProvider.getObject().get(device.getImei())?.getTemperatureInput()
                        getParameters(event, meter.impulseRegChannel, temperatureInput).forEach { parameter ->
                            ddp.addParameter(parameter, true)
                        }
                        result.add(ddp)
                    }
                    result
                }
                .map { dp -> measurePoint to dp }
        }

    }

    private fun getParameters(event: DataEvent, impulseRegChannel: Int, temperatureInput: DataType?): List<DeviceDataParameter> {
        val result = mutableListOf<DeviceDataParameter>()
        // Обработка событий АЦП
        if (event.code == EventType.ADC_EVENT) {
            event.data.filter { eventData ->
                eventData.type == DataType.INPUT_STATE_1 ||
                        eventData.type == DataType.INPUT_STATE_2 ||
                        eventData.type == DataType.INPUT_STATE_3 ||
                        eventData.type == DataType.INPUT_STATE_4 ||
                        eventData.type == DataType.INPUT_STATE_6
            }.forEach { data ->
                when {
                    //Вскрытие регистратора импульсов
                    data.type == DataType.INPUT_STATE_6 -> {
                        //Датчик вскрытия
                        // 0 – Сработал датчик отрыва от стены.
                        // 1 – Не интерпретируется.
                        // 2 – Вскрытие корпуса (когда вскрыт корпус, отрыв от стены не определяется).
                        // 3 – Восстановление в нормальное состояние.
                        if (data.value == 3) {
                            result.add(
                                DeviceDataParameter(
                                    name = ErrorCodes.REGISTER_OPENED.value,
                                    value = "CLOSED",
                                    dataTs = event.dateTime.toZonedDateTime(),
                                )
                            )
                        } else {
                            result.add(
                                DeviceDataParameter(
                                    name = ErrorCodes.REGISTER_OPENED.value,
                                    value = "OPEN",
                                    dataTs = event.dateTime.toZonedDateTime(),
                                )
                            )
                        }
                    }
                    //Сработал датчик магнитного поля регистратора импульсов
                    temperatureInput != null && data.type == temperatureInput ->
                        if (data.value == 1)
                            result.add(
                                DeviceDataParameter(
                                    name = ErrorCodes.MAGNETIC_FIELD_SENSOR.value,
                                    value = "OPEN",
                                    dataTs = event.dateTime.toZonedDateTime(),
                                )
                            )
                        else
                            result.add(
                                DeviceDataParameter(
                                    name = ErrorCodes.MAGNETIC_FIELD_SENSOR.value,
                                    value = "CLOSED",
                                    dataTs = event.dateTime.toZonedDateTime(),
                                )
                            )

                    (impulseRegChannel == 1 && data.type == DataType.INPUT_STATE_1) -> result.addAll(getEvent(data.value))
                    (impulseRegChannel == 2 && data.type == DataType.INPUT_STATE_2) -> result.addAll(getEvent(data.value))
                    (impulseRegChannel == 3 && data.type == DataType.INPUT_STATE_3) -> result.addAll(getEvent(data.value))
                    else -> {}
                }
            }
        }
        when (event.code) {
            EventType.INPUT_PULSE_RATE_EXCEEDED -> result.add(
                DeviceDataParameter(
                    name = ErrorCodes.INPUT_PULSE_RATE_EXCEEDED.value,
                    value = "SINGLE",
                    dataTs = event.dateTime.toZonedDateTime(),
                )
            )

            EventType.BUTTON_PRESSED -> result.add(
                DeviceDataParameter(
                    name = ErrorCodes.MANUAL_CONNECTION_ACTIVATION.value,
                    value = "SINGLE",
                    dataTs = event.dateTime.toZonedDateTime(),
                )
            )

            EventType.DEVICE_RESTART -> result.add(
                DeviceDataParameter(
                    name = ErrorCodes.RESTART.value,
                    value = "SINGLE",
                    dataTs = event.dateTime.toZonedDateTime(),
                )
            )

            else -> {}
        }
        return result
    }

    private fun getEvent(value: Int): List<DeviceDataParameter> {
        return when (value) {
            //Замыкание линии связи с ПУ ХВС/ГВС
            1 -> listOf(DeviceDataParameter(name = ErrorCodes.LINE_SHORT_CIRCUIT.value, value = "OPEN"))
            //Обрыв линии связи с ПУ ХВС/ГВС
            2 -> listOf(DeviceDataParameter(name = ErrorCodes.LINE_BREAK.value, value = "OPEN"))
            else -> {
                listOf(
                    DeviceDataParameter(name = ErrorCodes.LINE_SHORT_CIRCUIT.value, value = "CLOSED"),
                    DeviceDataParameter(name = ErrorCodes.LINE_BREAK.value, value = "CLOSED")
                )
            }
        }
    }

    private fun getCounterValue(
        events: DataEvent,
        meter: TeleofisMeter,
    ): Int? {
        return when (meter.impulseRegChannel) {
            1 -> events.data.firstOrNull { eventData -> eventData.type == DataType.COUNTER_VALUE_1 }?.value
            2 -> events.data.firstOrNull { eventData -> eventData.type == DataType.COUNTER_VALUE_2 }?.value
            3 -> events.data.firstOrNull { eventData -> eventData.type == DataType.COUNTER_VALUE_3 }?.value
            4 -> events.data.firstOrNull { eventData -> eventData.type == DataType.COUNTER_VALUE_4 }?.value
            else -> null
        }
    }

    private fun Instant?.deviceTimeToSystemTime(deviceTime: ZonedDateTime): ZonedDateTime? {
        val millisDeviceTimeZone = TimeZone.getTimeZone(deviceTime.zone).rawOffset.toLong()
        return this?.minusMillis(millisDeviceTimeZone)?.atZone(deviceTime.zone)?.withZoneSameInstant(ZoneId.systemDefault())
    }

}