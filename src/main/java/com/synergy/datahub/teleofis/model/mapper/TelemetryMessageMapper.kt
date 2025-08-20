package com.synergy.datahub.teleofis.model.mapper

import com.synergy.datahub.device.api.DeviceMessage
import com.synergy.datahub.teleofis.model.DataType
import com.synergy.datahub.teleofis.model.InputType
import com.synergy.datahub.teleofis.model.PacketType.TELEMETRY_INFORMATION_TRANSFER
import com.synergy.datahub.teleofis.model.ParameterData
import com.synergy.datahub.teleofis.model.ParameterType
import com.synergy.datahub.teleofis.utils.getN
import com.synergy.datahub.transport.DhMessage
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Component
class TelemetryMessageMapper : MessageMapper<TelemetryMessage> {
    override fun map(message: DhMessage): TelemetryMessage {
        val buffer = ByteBuffer.wrap(message.payload)
        //Количество параметров
        val numOfParameters = buffer.get().toInt() and 0xFF
        val parameters = mutableListOf<ParameterData>()
        repeat(numOfParameters) {
            //Номер параметра
            val paramNumber = buffer.get().toUByte()
            //Длина данных
            val dataSize = buffer.get().toInt() and 0xFF
            require(dataSize in 1..64) { "Размер данных должен быть в диапазоне 1-64" }
            val parameterData = buffer.getN(dataSize)
            val parameterType = ParameterType.fromNumber(paramNumber)
            if (parameterType != null) {
                parameters.add(
                    ParameterData(
                        type = parameterType, dataArr = parameterData
                    )
                )
            } else {
//                    log.warn {"Нет обработчика для параметра телеметрии: $paramNumber"}
            }
        }
        return TelemetryMessage(parameters)
    }

    override fun getType() = TELEMETRY_INFORMATION_TRANSFER
}

//Формат передачи телеметрической информации
data class TelemetryMessage(
    //номер параметра + данные
    val parameters: List<ParameterData>,
) : DeviceMessage {
    private val parameterMap = parameters.associateBy { it.type }

    fun getParameter(type: ParameterType) = parameterMap[type]

    fun getCurrentTime(): ZonedDateTime = requireNotNull(parameterMap[ParameterType.CURRENT_TIME]).getAsInstant().atZone(getTimeZone())

    fun getValues(): List<Int> = requireNotNull(parameterMap[ParameterType.VALUES]).getAsArray()

    fun getValue1(): Int = requireNotNull(parameterMap[ParameterType.VALUE_1]).getAsNumber()

    fun getValue2(): Int = requireNotNull(parameterMap[ParameterType.VALUE_2]).getAsNumber()

    fun getValue3(): Int = requireNotNull(parameterMap[ParameterType.VALUE_3]).getAsNumber()

    fun getValue4(): Int = requireNotNull(parameterMap[ParameterType.VALUE_4]).getAsNumber()

    fun getInputState1(): Int = requireNotNull(parameterMap[ParameterType.INPUT_STATE_1]).getAsNumber()

    fun getInputType1(): Int = requireNotNull(parameterMap[ParameterType.INPUT_TYPE_1]).getAsNumber()

    fun getInputState2(): Int = requireNotNull(parameterMap[ParameterType.INPUT_STATE_2]).getAsNumber()

    fun getInputType2(): Int = requireNotNull(parameterMap[ParameterType.INPUT_TYPE_2]).getAsNumber()

    fun getInputState3(): Int = requireNotNull(parameterMap[ParameterType.INPUT_STATE_3]).getAsNumber()

    fun getInputType3(): Int = requireNotNull(parameterMap[ParameterType.INPUT_TYPE_3]).getAsNumber()

    fun getInputState4(): Int = requireNotNull(parameterMap[ParameterType.INPUT_STATE_4]).getAsNumber()

    fun getInputType4(): Int = requireNotNull(parameterMap[ParameterType.INPUT_TYPE_4]).getAsNumber()

    fun getInputState6(): Int = requireNotNull(parameterMap[ParameterType.INPUT_STATE_6]).getAsNumber()

    fun getInputType6(): Int = requireNotNull(parameterMap[ParameterType.INPUT_TYPE_6]).getAsNumber()

    fun getBatteryVoltage(): Int = requireNotNull(parameterMap[ParameterType.BATTERY_VOLTAGE]).getAsNumber()

    fun getTimeZone(): ZoneId = ZoneOffset.ofHours(requireNotNull(parameterMap[ParameterType.TIME_ZONE]).getAsNumber())

    fun getBatteryVoltageBeforeConnection(): Int = requireNotNull(parameterMap[ParameterType.BATTERY_VOLTAGE_BEFORE_CONNECTION]).getAsNumber()

    fun getGSMSignalLevel(): Int = requireNotNull(parameterMap[ParameterType.GSM_SIGNAL_LEVEL]).getAsNumber()

    fun getTemperatureInput(): DataType? {
        //тип "температурный датчик" может быть не более, чем у 1 входа
        return when (InputType.TEMPERATURE_SENSOR.value) {
            getInputType1() -> DataType.INPUT_STATE_1
            getInputType2() -> DataType.INPUT_STATE_2
            getInputType3() -> DataType.INPUT_STATE_3
            getInputType4() -> DataType.INPUT_STATE_4
            getInputType6() -> DataType.INPUT_STATE_6
            else -> null
        }
    }

}
