package com.synergy.datahub.teleofis.service.handler

import com.synergy.common.device.DeviceDataType
import com.synergy.datahub.data.DeviceDataListener
import com.synergy.datahub.device.api.model.DataPeriod
import com.synergy.datahub.service.data.resend.ResendRequestService
import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.api.MessageSender
import com.synergy.datahub.teleofis.mapper.ArchiveEventMapper
import com.synergy.datahub.teleofis.model.EventType
import com.synergy.datahub.teleofis.model.PacketType.COUNTER_DATA_ACKNOWLEDGEMENT
import com.synergy.datahub.teleofis.model.ParameterData
import com.synergy.datahub.teleofis.model.mapper.ArchiveEventMessage
import com.synergy.datahub.teleofis.service.ArchiveEventProvider
import com.synergy.datahub.teleofis.service.ParameterService
import com.synergy.datahub.teleofis.service.WorkModeService
import com.synergy.datahub.teleofis.service.handler.TelemetryPackageHandler.Companion.getDeviceInfo
import com.synergy.datahub.transport.DhTransport
import com.synergy.datahub.transport.DuplexChannel
import com.synergy.datahub.utils.toByteArray
import com.synergy.spring.starters.kotlin.serverZoneId
import com.synergy.transport.config.TcpTransportProperties
import com.synergy.transport.ext.DuplexChannelExt.Companion.doActionFlux
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Обработка архивов событий полученных с регистратора импульсов
 */
@Component
class ArchiveEventHandler(
    private val parameterService: ParameterService,
    private val archiveEventMapper: ArchiveEventMapper,
    private val deviceDataListener: DeviceDataListener,
    private val workModeService: WorkModeService,
    private val transportProperties: TcpTransportProperties
) : PackageHandler<ArchiveEventMessage>, ArchiveEventProvider {
    private val log = KotlinLogging.logger {}
    private val sinks = mutableMapOf<ULong, MutableList<FluxSink<ArchiveEventMessage>>>()

    private lateinit var resendRequestService: ResendRequestService

    override fun startReceiving(device: TeleofisDevice, sender: MessageSender, period: DataPeriod) {
        val imei = device.getImei()
        val millisTimeZone = getOffsetTimezoneInMillis(imei)
        val truncatedFrom = period.from.truncatedTo(ChronoUnit.MINUTES)
        val truncatedTo = period.to.truncatedTo(ChronoUnit.MINUTES)
        log.trace { "${device.getDeviceInfo()}Запущено получение событий за период: $truncatedFrom - $truncatedTo" }
        parameterService.setParameter(
            sender, device,
            ParameterData.archiveStartReceiving(
                from = truncatedFrom.toInstant().plusMillis(millisTimeZone),
                to = truncatedTo.toInstant().plusMillis(millisTimeZone),
            )
        )
    }

    override fun get(session: DhTransport, device: TeleofisDevice, from: ZonedDateTime, to: ZonedDateTime): Flux<ArchiveEventMessage> {
        val imei = device.getImei()
        val millisTimeZone = getOffsetTimezoneInMillis(imei)
        val truncatedFrom = from.truncatedTo(ChronoUnit.MINUTES)
        val truncatedTo = to.truncatedTo(ChronoUnit.MINUTES)
        return session.doActionFlux(device) { channel ->
            log.trace { "${device.getDeviceInfo()}Запущено получение событий за период: $truncatedFrom - $truncatedTo" }
            channel.startReceivingArchives(
                device = device,
                from = truncatedFrom.toInstant().plusMillis(millisTimeZone),
                to = truncatedTo.toInstant().plusMillis(millisTimeZone),
            )
            get(imei)
                .takeUntil { archive ->
                    archive.events.findLast { event -> event.code == EventType.ARCHIVE_TRANSFER_END } != null
                }
                .timeout(transportProperties.requestStrategy.timeout)
        }
    }

    override fun handle(device: TeleofisDevice, message: ArchiveEventMessage, sender: MessageSender) {
        val imei = device.getImei()
        log.trace { "${device.getDeviceInfo()}Получен архив событий №${message.packetId} с ${message.events.firstOrNull()?.dateTime} по ${message.events.lastOrNull()?.dateTime}" }
        if (workModeService.isSporadic(device)) {
            log.trace { "${device.getDeviceInfo()}[PackageId=${message.packetId}]Получены данные с прибора $message" }
            archiveEventMapper.mapToArchiveEvent(device, message)
                .forEach { (mp, ddp) ->
                    deviceDataListener.receivedData(device, ddp)
                    if (ddp.type in listOf(DeviceDataType.INSTANT, DeviceDataType.DAY, DeviceDataType.HOUR)) {
                        //обновить статус по всем переопросов из трансформированных архивов
                        resendRequestService.updateTaskStatus(mp.id, ddp.type, ddp)
                    }
                }
        }
        sinks[imei]?.forEach { sink -> sink.next(message) }
        //отправить подтверждение на пакет архива событий
        sender.send(device, COUNTER_DATA_ACKNOWLEDGEMENT, message.packetId.toByteArray())
        log.trace { "${device.getDeviceInfo()}Отправлено подтверждение на пакет событий. PacketId: ${message.packetId}" }
    }

    override fun get(imei: ULong): Flux<ArchiveEventMessage> {
        lateinit var currentSink: FluxSink<ArchiveEventMessage>
        return Flux.create { sink ->
            currentSink = sink
            sinks.getOrPut(imei) { CopyOnWriteArrayList() }.add(currentSink)
        }
            .doOnCancel {
                sinks[imei]?.remove(currentSink)
            }
            .doOnTerminate {
                sinks[imei]?.remove(currentSink)
                log.error { "Завершение ArchiveEventHandler imei: $imei" }
            }
    }

    fun DuplexChannel.stopReceivingArchives(device: TeleofisDevice) {
        parameterService.setParameter(this, device, ParameterData.archiveStopReceiving())
    }

    private fun DuplexChannel.startReceivingArchives(device: TeleofisDevice, from: Instant, to: Instant) =
        parameterService.setParameter(this, device, ParameterData.archiveStartReceiving(from = from, to = to))

    private fun getOffsetTimezoneInMillis(imei: ULong): Long =
        TimeZone.getTimeZone(serverZoneId).rawOffset.toLong()


    @Autowired
    fun setResendRequestService(@org.springframework.context.annotation.Lazy resendRequestService: ResendRequestService) {
        this.resendRequestService = resendRequestService
    }

}
