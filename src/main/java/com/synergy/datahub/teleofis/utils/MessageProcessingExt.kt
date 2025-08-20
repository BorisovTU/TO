package com.synergy.datahub.teleofis.utils

import com.synergy.datahub.device.api.DeviceMessage
import com.synergy.datahub.teleofis.model.PacketType
import com.synergy.datahub.teleofis.model.mapper.MessageMapper
import com.synergy.datahub.transport.DhMessage
import com.synergy.datahub.transport.DuplexChannel
import com.synergy.datahub.utils.toHex
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

object MessageProcessingExt {
    val log = KotlinLogging.logger {}
    lateinit var timeout: Duration
    lateinit var mappers: Map<PacketType, MessageMapper<*>>

    inline fun <reified T : DeviceMessage> DuplexChannel.getResponses(
        crossinline filter: (T) -> Boolean = { true },
    ): Flux<T> =
        responses()
            .flatMap { message ->
                Mono.fromCallable {
                    getMapper(message).map(message)
                }
            }
            .filter { rs -> rs is T && filter(rs) }
            .cast(T::class.java)
            .timeout(timeout)
            .doOnError { ex -> log.error(ex) { "Ошибка при обработке входящего сообщения" } }

    fun getMapper(message: DhMessage) = mappers[message.getType()]
        ?: throw IllegalStateException("[IMEI=${message.getImei()}]Пакет ${message.getType()} не поддерживается. Сообщение: ${message.payload.toHex()}")
}