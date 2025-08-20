package com.synergy.datahub.teleofis.service

import com.synergy.common.device.DeviceType
import com.synergy.datahub.device.DeviceInteractionService
import com.synergy.datahub.device.api.Device
import com.synergy.datahub.service.status.TransferStatusService
import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.config.TeleofisProperties
import com.synergy.datahub.teleofis.ex.TeleofisException
import com.synergy.datahub.teleofis.service.handler.TelemetryPackageHandler.Companion.getDeviceInfo
import com.synergy.datahub.teleofis.service.handler.TeleofisDeviceStore
import com.synergy.datahub.transport.DhTransport
import com.synergy.datahub.transport.DhTransportCloseAction
import com.synergy.datahub.transport.DhTransportListener
import mu.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Отвечает за уведомление о возможной недоступности регистраторов импульсов.
 * Не выходил на связь 2 периода опроса
 */
@Component
class TeleofisStatusService(
    private val properties: TeleofisProperties,
    private val transferStatusService: TransferStatusService,
    private val deviceStore: TeleofisDeviceStore,
    private val teleofisService: TeleofisService,
    private val deviceInteractionService: DeviceInteractionService,
) : ApplicationRunner, DhTransportListener, DhTransportCloseAction {
    private val log = KotlinLogging.logger {}

    private val lastPushErrorTs = ConcurrentHashMap<Long, Instant>()

    override fun run(args: ApplicationArguments) {
        Flux.interval(properties.checkActiveInterval)
            .doOnNext {
                deviceStore.getAll().toList().forEach { device ->
                    val transferId = device.getTransferId()
                    val lastInteractionDateTime = deviceInteractionService.getLastInteractionTs(device) ?: ZonedDateTime.now().minusYears(1)
                    //При отсутствии подключения регистратора импульсов более 2-х периодов опроса мгновенных данных подряд + 60 минут
                    val now = ZonedDateTime.now()
                    val leftDuration = Duration.between(lastInteractionDateTime, now)
                    val lastPushError = Duration.between(lastPushErrorTs.getOrPut(transferId) { now.minusYears(1).toInstant() }, now)
                    val maxTimeWithoutInteraction = device.getMaxTimeWithoutInteraction()
                    if (leftDuration > maxTimeWithoutInteraction && lastPushError > maxTimeWithoutInteraction) {
                        val msg = "Нет связи с регистратором импульсов больше $leftDuration"
                        log.trace { "${device.getDeviceInfo()}$msg" }
                        val ex = TeleofisException(device, msg, null)
                        transferStatusService.transferNoConnection(device.getTransferId(), device.meters.map { it.id }, deviceType = DeviceType.Impulse, ex)
                        lastPushErrorTs[transferId] = now.toInstant()
                    }
                }
            }
            .onErrorContinue { ex, _ -> log.error(ex) { "Ошибка мониторинга устройств teleofis." } }
            .subscribe()
    }

    override fun opened(device: Device) {

    }

    override fun closed(device: Device) {
    }

    override fun notOpened(device: Device, ex: Throwable?) {
    }


    override fun transportClosed(transport: DhTransport): Mono<Void> {
        val device = deviceStore.get(transport.getTransportKey()).firstOrNull() ?: return Mono.empty()
        return Flux.merge(
            //отправку команду завершения сеанса при закрытии подключения
            teleofisService.sleepDevice(transport, device)
                .doOnSubscribe {
                    log.info { "${device.getDeviceInfo()}Команда окончания сеанса связи с устройством Телеофис. Transport: $transport" }
                }
                .doOnSuccess {
                    log.info { "${device.getDeviceInfo()}Завершена работа с устройством Телеофис. Transport: $transport" }
                },
            Mono.fromRunnable {
                transferStatusService.transferDisconnected(device.getTransferId())
            }
        ).then()
    }

    /**
     * Допустимое время в секундах перед изменением состояния связи на "Нет связи"
     */
    private fun TeleofisDevice.getMaxTimeWithoutInteraction(): Duration {
        val periodContact = register.requestRateInstant ?: properties.connectionInterval
        return periodContact.multipliedBy(2) + properties.closeAfter2periodsPlus
    }
}