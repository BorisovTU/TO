package com.synergy.datahub.teleofis.api

import com.synergy.datahub.device.api.DeviceMessage
import com.synergy.datahub.service.status.TransferStatusService
import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.ex.TeleofisException
import com.synergy.datahub.teleofis.model.PacketType
import com.synergy.datahub.teleofis.service.EncryptionService
import com.synergy.datahub.teleofis.service.handler.PackageHandler
import com.synergy.datahub.teleofis.service.handler.TelemetryPackageHandler.Companion.getDeviceInfo
import com.synergy.datahub.teleofis.utils.MessageProcessingExt
import com.synergy.datahub.teleofis.utils.PACKAGE_TYPE_HEADER
import com.synergy.datahub.transport.DhMessage
import com.synergy.datahub.utils.toHex
import com.synergy.transport.InboundTcpChannelProcessor
import com.synergy.transport.config.ServerType
import com.synergy.transport.config.TcpTransportProperties
import com.synergy.transport.ext.addRecipient
import com.synergy.transport.ext.getDevice
import com.synergy.transport.ext.getRecipient
import com.synergy.transport.model.RecipientAndPayload
import com.synergy.transport.session.channel.MessageChannel
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toMono
import java.net.InetSocketAddress
import java.util.concurrent.TimeoutException

@Component
class InboundChannelMessages(
    private val encryptionService: EncryptionService,
    private val transferStatusService: TransferStatusService,
    private val tcpTransportProperties: TcpTransportProperties,
    handlers: List<PackageHandler<*>>,
) : InboundTcpChannelProcessor {
    private val log = KotlinLogging.logger {}
    private val handlersMap = handlers.associateBy { it.messageType }
    private val excludeList = listOf("2b|2b|2b", "41|54|2b|43|49|50|43|4c|4f|53|45|0d")

    override fun doMap(inbound: Flux<RecipientAndPayload>, outboundChannel: MessageChannel, serverType: ServerType): Flux<DhMessage> {
        return inbound
            .filter { recipientAndPayload -> recipientAndPayload.payload.isNotEmpty() && !excludeList.contains(recipientAndPayload.payload.toHex()) }
            .doOnNext { recipientAndPayload ->
                log.info { "Зашифрованное сообщение: ${recipientAndPayload.payload.toHex()}. ${recipientAndPayload.recipient}" }
            }
            .groupBy { it.recipient }
            .flatMap { groupedFlux ->
                var packetReceive = false
                groupedFlux
                    .bufferUntil { senderAndPayload -> senderAndPayload.payload[senderAndPayload.payload.size - 1] == 0xc2u.toByte() }
                    .cleanBuffer(serverType)
                    .onErrorResume {
                        if (packetReceive) {
                            Flux.empty()
                        } else {
                            TimeoutException("Закончилось ожидание получения оставшегося пакета").toMono()
                        }
                    }
                    .map { messageParts -> messageParts.reduce { acc, next -> acc.copy(payload = acc.payload + next.payload) } }
                    .skipUntil { senderAndPayload ->
                        senderAndPayload.payload[0] == 0xc0u.toByte()
                    }
                    .flatMapIterable { senderAndPayload ->
                        packetReceive = true
                        encryptionService.decryptMessage(senderAndPayload)
                            .onEach { message ->
                                try {
                                    resolveAndHandle(
                                        message,
                                        MessageSenderImpl(encryptionService, message.getRecipient(), outboundChannel)
                                    )
                                } catch (ex: Exception) {
                                    val device = message.getDevice() as TeleofisDevice
                                    val msg =
                                        "${device.getDeviceInfo()}Ошибка при обработке расшифрованного сообщения c типом ${message.getHeaders()[PACKAGE_TYPE_HEADER]}: ${message.payload.toHex()}"
                                    log.error(ex) { msg }
                                    throw TeleofisException(device = device, msg = msg, cause = ex)
                                }
                            }
                    }
            }
            .doOnError { ex ->
                if (ex is TeleofisException) {
                    transferStatusService.transferConnectionError(ex.device.getTransferId(), ex)
                }
            }
            .handleError(serverType)
    }

    private fun <T> Flux<T>.handleError(serverType: ServerType): Flux<T> {
        return if (serverType == ServerType.Udp) {
            onErrorContinue { ex, _ ->
                log.error(ex) { "Ошибка обработки входящего Udp канала с регистратором" }
            }
        } else {
            this
        }
    }

    fun Flux<List<RecipientAndPayload>>.cleanBuffer(serverType: ServerType): Flux<List<RecipientAndPayload>> {
        return if (serverType == ServerType.Udp) {
            this
                .timeout(tcpTransportProperties.requestStrategy.timeout)
                .onErrorComplete()
        } else {
            this.timeout(tcpTransportProperties.requestStrategy.timeout)
        }
    }

    private fun resolveAndHandle(message: DhMessage, sender: MessageSender) {
        val deviceMessage = MessageProcessingExt.getMapper(message).map(message)
        val handler = handlersMap[deviceMessage.javaClass] as? PackageHandler<DeviceMessage>
        handler?.handle(message.getDevice() as TeleofisDevice, deviceMessage, sender)
    }

    class MessageSenderImpl(
        private val encryptionService: EncryptionService,
        private val recipient: InetSocketAddress,
        private val outboundChannel: MessageChannel
    ) : MessageSender {
        override fun send(device: TeleofisDevice, type: PacketType, payload: ByteArray) {
            outboundChannel.send(
                encryptionService.encryptMessage(device, type, payload).apply {
                    addRecipient(recipient)
                }
            )
        }
    }
}

interface MessageSender {
    fun send(device: TeleofisDevice, type: PacketType, payload: ByteArray = ByteArray(0))
}

