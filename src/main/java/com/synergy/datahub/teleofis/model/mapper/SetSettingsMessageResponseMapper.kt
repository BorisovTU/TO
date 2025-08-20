package com.synergy.datahub.teleofis.model.mapper

import com.synergy.datahub.device.api.DeviceMessage
import com.synergy.datahub.teleofis.model.CommandExecutionCode
import com.synergy.datahub.teleofis.model.PacketType
import com.synergy.datahub.teleofis.model.PacketType.DEVICE_SETUP_RESPONSE
import com.synergy.datahub.teleofis.model.ParameterType
import com.synergy.datahub.transport.DhMessage
import org.springframework.stereotype.Component
import java.nio.ByteBuffer

@Component
class SetSettingsMessageResponseMapper : MessageMapper<SetSettingsMessageResponse> {
    override fun map(message: DhMessage): SetSettingsMessageResponse {
        val buffer = ByteBuffer.wrap(message.payload)
        val parameterNumber = buffer.get().toUByte()
        val commandStatusCode = buffer.get().toInt() and 0xFF
        return SetSettingsMessageResponse(
            statusCode = CommandExecutionCode.fromCode(commandStatusCode),
            parameter = requireNotNull(ParameterType.fromNumber(parameterNumber)) { "Неизвестный параметр $parameterNumber" }
        )
    }

    override fun getType() = DEVICE_SETUP_RESPONSE
}

//Формат передачи ответа на команду настройки
class SetSettingsMessageResponse(
    //номер параметра + данные
    val parameter: ParameterType,
    val statusCode: CommandExecutionCode,
) : DeviceMessage {
    fun isOk(): Boolean = !statusCode.isError()
}
