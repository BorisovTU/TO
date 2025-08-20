package com.synergy.datahub.teleofis.service

import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.api.MessageSender
import com.synergy.datahub.teleofis.utils.MessageProcessingExt.getResponses
import com.synergy.datahub.teleofis.model.PacketType.*
import com.synergy.datahub.teleofis.model.ParameterData
import com.synergy.datahub.teleofis.model.ParameterType
import com.synergy.datahub.teleofis.model.mapper.ReadSettingsMessage
import com.synergy.datahub.teleofis.model.mapper.SetSettingsMessageResponse
import com.synergy.datahub.teleofis.service.handler.TelemetryPackageHandler.Companion.getDeviceInfo
import com.synergy.datahub.teleofis.utils.toByteArray
import com.synergy.datahub.transport.DhTransport
import com.synergy.datahub.transport.DuplexChannel
import com.synergy.datahub.utils.toByteArray
import com.synergy.datahub.utils.toHex
import com.synergy.transport.ext.DuplexChannelExt.Companion.doAction
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

@Component
class ParameterService(
    private val encryptionService: EncryptionService
) {
    private val log = KotlinLogging.logger {}

    /**
     * Установить параметр устройства в существующем канале без ожидания ответа
     */
    fun setParameter(sender: MessageSender, device: TeleofisDevice, parameter: ParameterData) {
        (if (parameter.getDataSize() == 0) 1 else parameter.getDataSize()).let { parameterSize ->
            val buffer = ByteBuffer.allocate(2 + parameterSize).apply {
                put(parameter.type.num.toByte())
                put(parameterSize.toByte())
                put(parameter.getData())
                flip()
            }
            sender.send(device, DEVICE_SETUP_DATA, buffer.toByteArray())
        }
    }

    /**
     * Установить параметры устройства в новом канале
     */
    fun setParameter(session: DhTransport, device: TeleofisDevice, parameter: ParameterData, waitResponse: Boolean = true): Mono<Void> {
        return session.doAction(device) { channel ->
            if (waitResponse) {
                setParameterAndWaitResult(channel, device, parameter)
            } else {
                Mono.fromRunnable { setParameter(channel, device, parameter) }
            }
        }
    }

    /**
     * Установить параметр устройства в существующем канале без ожидания ответа
     */
    fun setParameter(channel: DuplexChannel, device: TeleofisDevice, parameter: ParameterData) {
        log.trace {
            "${device.getDeviceInfo()}Установка параметра устройства. Параметр: ${parameter.type.description}. Значение: ${
                parameter.getData().toHex()
            }"
        }
        channel.request(
            (if (parameter.getDataSize() == 0) 1 else parameter.getDataSize()).let { parameterSize ->
                val buffer = ByteBuffer.allocate(2 + parameterSize).apply {
                    put(parameter.type.num.toByte())
                    put(parameterSize.toByte())
                    put(parameter.getData())
                    flip()
                }
                encryptionService.encryptMessage(
                    device = device, type = DEVICE_SETUP_DATA, payload = buffer.toByteArray()
                )
            }
        )
    }

    fun acknowledgementArchive(channel: DuplexChannel, device: TeleofisDevice, packedId: UByte) {
        log.trace { "[УПД=${device.getTransferId()}][IMEI=${device.getImei()}]Установка параметра устройства. Параметр: ${COUNTER_DATA_ACKNOWLEDGEMENT.description}." }
        channel.request(
            encryptionService.encryptMessage(
                device = device, type = COUNTER_DATA_ACKNOWLEDGEMENT, payload = packedId.toByteArray()
            )
        )
    }

    /**
     * Установить параметр устройства в существующем канале
     */
    fun setParameterAndWaitResult(channel: DuplexChannel, device: TeleofisDevice, parameter: ParameterData): Mono<Void> {
        return Mono.fromRunnable<Void> {
            setParameter(channel, device, parameter)
        }.then(
            Mono.defer {
                channel.getResponses<SetSettingsMessageResponse> { response -> response.parameter.type == parameter.type.type }
                    .next()
                    .flatMap { parameter ->
                        if (!parameter.isOk()) {
                            Mono.error(IllegalStateException("Не установлен. Параметр: ${parameter.parameter.type}. Статус: ${parameter.statusCode}"))
                        } else {
                            log.trace { "${device.getDeviceInfo()}Параметр устройства установлен. Параметр: ${parameter.parameter.description}" }
                            Mono.empty<Void>()
                        }
                    }
                    .doOnError { ex ->
                        log.error(ex) { "${device.getDeviceInfo()}Ошибка при установке параметра: $parameter" }
                    }
            }
        )
    }

    /**
     * Чтение параметра с устройства через двухсторонний канал
     * таймаут 60сек
     */
    fun getParameter(
        session: DhTransport, device: TeleofisDevice, parameter: ParameterType
    ): Mono<ParameterData> {
        return session.doAction(device) { channel -> channel.getParameter(device, parameter) }
    }

    /**
     * Получить параметр устройства
     */
    private fun DuplexChannel.getParameter(device: TeleofisDevice, parameter: ParameterType): Mono<ParameterData> {
        return Mono.fromRunnable<Void> {
            log.trace { "${device.getDeviceInfo()}Чтение параметра $parameter устройства." }
            request(
                encryptionService.encryptMessage(
                    device = device, type = READ_SETTINGS_DATA,
                    payload = ByteBuffer.allocate(parameter.size + 2).apply {
                        put(parameter.num.toByte())
                        put(parameter.size.toByte())
                        flip()
                    }.toByteArray()
                )
            )
        }.then(
            Mono.defer {
                getResponses<ReadSettingsMessage> { response -> response.parameter.type == parameter }
                    .next()
                    .map { response ->
                        log.trace { "Получено значение параметра ${parameter.description}. Значение: ${response.parameter.getData().toHex()}" }
                        response.parameter
                    }
                    .doOnError { ex ->
                        log.error(ex) { "${device.getDeviceInfo()}Ошибка при чтении параметра: $parameter" }
                    }
            }
        )
    }
//    /**
//     * Команда установки списка параметров, которые будут переданы в пакете телеметрии
//     */
//    fun setSettingsMessage(imei: ULong, parameters: List<ParameterType>): Mono<Void> {
//        val command =
//            ByteArray(2).apply {
//                set(0, 0x32)
//                set(1, 0x08)
//            } +
//                    parameters
//                        .mapNotNull { it.bitMask }
//                        .reduce { acc, i -> acc or i }
//                        .toByteArray()
//        return encryptMessage(
//            imei = imei, packet = RawPacketToDevice(DEVICE_SETUP_DATA, data = command)
//        )
//    }
//раскомментировать неиспользуемые команды если потребуется
//        /**
//         * чтение нескольких параметров с утройства
//         * Неуспешное выполнение запроса. Код: 3 TODO что-то не так с командой
//         * команда для установки параметров внутри пакета телеметрии
//         */
//        private fun DuplexChannel.setMultipleSettings(imei: ULong, parameters: List<ParameterType>) =
//            this.request(
//                MessageProcessingExt.setSettingsMessage(
//                    imei = imei,
//                    parameters = parameters
//                )
//            )


//        /**
//         * получить данные со счетчиков
//         * команда не работает TODO()
//         */
//        private fun DuplexChannel.getDeviceData(imei: ULong) = request(
//            MessageProcessingExt.encryptMessage(
//                imei = imei,
//                RawPacketToDevice(PacketType.COUNTER_DATA),
//            )
//        )
}