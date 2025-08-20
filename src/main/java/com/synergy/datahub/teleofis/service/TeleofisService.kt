package com.synergy.datahub.teleofis.service

import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.model.ParameterData
import com.synergy.datahub.teleofis.model.ParameterType
import com.synergy.datahub.teleofis.model.mapper.ArchiveEventMessage
import com.synergy.datahub.transport.DhTransport
import com.synergy.datahub.transport.DuplexChannel
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.ZonedDateTime

interface TeleofisService {
    /**
     * Получить параметры с регистратора
     */
    fun getParameters(transport: DhTransport, device: TeleofisDevice, vararg types: ParameterType): Mono<Map<ParameterType, ParameterData>>

    /**
     * Снять архивы с регистратора за выбранный интервал
     */
    fun getEvents(transport: DhTransport, device: TeleofisDevice, from: ZonedDateTime, to: ZonedDateTime): Flux<ArchiveEventMessage>

    /**
     * Обновить время на регистраторе
     */
    fun updateDeviceDateTime(transport: DhTransport, device: TeleofisDevice): Mono<Void>

    /**
     * Перезапустить регистратор
     */
    fun restartDevice(transport: DhTransport, device: TeleofisDevice, restartAfter: Duration = Duration.ofMinutes(5)): Mono<Void>

    /**
     * Отправить команду окончания запросов
     */
    fun sleepDevice(transport: DhTransport, device: TeleofisDevice): Mono<Void>

    /**
     * Получить текущее время на регистраторе
     */
    fun getDeviceDateTime(transport: DhTransport, device: TeleofisDevice): Mono<ZonedDateTime>

    fun stopReceivingArchive(channel: DuplexChannel, device: TeleofisDevice)

    fun acknowledgementArchive(channel: DuplexChannel, device: TeleofisDevice, packetId: UByte)

}