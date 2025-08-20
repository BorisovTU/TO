package com.synergy.datahub.teleofis.model.mapper

import com.synergy.datahub.device.api.DeviceMessage
import com.synergy.datahub.teleofis.model.CommandExecutionCode
import com.synergy.datahub.teleofis.model.PacketType
import com.synergy.datahub.teleofis.model.PacketType.*
import com.synergy.datahub.teleofis.model.ParameterData
import com.synergy.datahub.teleofis.model.ParameterType
import com.synergy.datahub.transport.DhMessage
import org.springframework.stereotype.Component
import java.nio.ByteBuffer

@Component
class ReadSettingsMessageMapper : MessageMapper<ReadSettingsMessage> {
    override fun map(message: DhMessage): ReadSettingsMessage {
        val buffer = ByteBuffer.wrap(message.payload)
        require(buffer.remaining() >= 3) { "Не хватает данных для разбора" }
        val parameterNumber = buffer.get().toUByte()
        val commandStatusCode = buffer.get().toInt() and 0xFF
        val dataLength = buffer.get().toInt() and 0xFF
        require(buffer.remaining() != dataLength) { "Несоответствие длины данных" }
        val commandStatus = CommandExecutionCode.fromCode(commandStatusCode)
        if (commandStatus != CommandExecutionCode.COMMAND_SUCCESSFUL) {
            throw IllegalStateException("Неуспешное выполнение запроса. Код: ${commandStatus.code}")
        }
        val data = ByteArray(dataLength)
        buffer.get(data)
        val type = ParameterType.fromNumber(parameterNumber)
            ?: throw IllegalStateException("Неизвестный параметр устройства. НомерПараметра: $parameterNumber")
        return ReadSettingsMessage(
            parameter = ParameterData(type = type, dataArr = data)
        )
    }

    override fun getType() = READ_SETTINGS_RESPONSE
}

class ReadSettingsMessage(
    //номер параметра + данные
    val parameter: ParameterData,
) : DeviceMessage