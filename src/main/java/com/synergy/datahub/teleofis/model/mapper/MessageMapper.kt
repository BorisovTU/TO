package com.synergy.datahub.teleofis.model.mapper

import com.synergy.datahub.device.api.DeviceMessage
import com.synergy.datahub.teleofis.model.AuthMessage
import com.synergy.datahub.teleofis.model.PacketType
import com.synergy.datahub.teleofis.model.PacketType.*
import com.synergy.datahub.transport.DhMessage
import org.springframework.stereotype.Component

interface MessageMapper<T : DeviceMessage> {
    fun map(message: DhMessage): T
    fun getType(): PacketType
}

@Component
class AuthMessageMapper : MessageMapper<AuthMessage> {
    override fun map(message: DhMessage) = AuthMessage()

    override fun getType() = SERVER_AUTHORIZATION_WHEN_SPORADIC_DATA_DISABLED
}