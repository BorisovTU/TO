package com.synergy.datahub.teleofis.service

import com.synergy.datahub.device.api.model.DataPeriod
import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.api.MessageSender
import com.synergy.datahub.teleofis.model.mapper.ArchiveEventMessage
import com.synergy.datahub.transport.DhTransport
import reactor.core.publisher.Flux
import java.time.ZonedDateTime

interface ArchiveEventProvider {
    /**
     * Запустить получение архивов
     */
    fun startReceiving(device: TeleofisDevice, sender: MessageSender, period: DataPeriod)

    /**
     * Получить архивы за период
     */
    fun get(session: DhTransport, device: TeleofisDevice, from: ZonedDateTime, to: ZonedDateTime): Flux<ArchiveEventMessage>

    /**
     * Получить всех архивов проходящих через dh (переопросы, опросы по расписанию и тд)
     */
    fun get(imei: ULong): Flux<ArchiveEventMessage>
}