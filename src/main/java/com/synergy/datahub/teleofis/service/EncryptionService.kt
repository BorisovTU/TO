package com.synergy.datahub.teleofis.service

import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.ex.TeleofisException
import com.synergy.datahub.teleofis.model.EncryptedMessage
import com.synergy.datahub.teleofis.model.PacketType
import com.synergy.datahub.teleofis.model.PacketType.DEVICE_SETUP_DATA
import com.synergy.datahub.teleofis.model.PacketType.DEVICE_SETUP_RESPONSE
import com.synergy.datahub.teleofis.model.ParameterType
import com.synergy.datahub.teleofis.service.handler.TelemetryPackageHandler.Companion.getDeviceInfo
import com.synergy.datahub.teleofis.service.impl.TeleofisDeviceProvider
import com.synergy.datahub.teleofis.utils.*
import com.synergy.datahub.transport.DhMessage
import com.synergy.datahub.utils.toByteArray
import com.synergy.datahub.utils.toHex
import com.synergy.transport.ext.DEVICE_HEADER
import com.synergy.transport.ext.RECIPIENT_HEADER
import com.synergy.transport.model.RecipientAndPayload
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.util.*

@Component
class EncryptionService {
    private val log = KotlinLogging.logger {}
    private lateinit var deviceProvider: TeleofisDeviceProvider

    fun encryptMessage(device: TeleofisDevice, type: PacketType, payload: ByteArray = ByteArray(0)): DhMessage {
        val buffer = ByteBuffer.allocate(1 + payload.size).apply {
            put(type.packetId.toByte())
            if (payload.isNotEmpty()) {
                put(payload)
            }
        }
        //добить тело сообщения нулями если его длинна не станет кратна 8 после добавления 2х байт с crc16
        val modifiedPayload = buffer.array().padToMultipleOfEight()
        //посчитать crc16
        val crc16 = calculateCRC16CCIT(modifiedPayload).toShort().toByteArray()
        val payloadWithCrc16 = modifiedPayload + crc16
        return teleofisByteStuffingOutput(
            listOf(
                EncryptedMessage(
                    imei = device.getImei().toByteArray(),
                    //добавить crc16 в конец сообщения
                    payload = xteaEncrypt(
                        value = payloadWithCrc16,
                        key = device.getEncryptionKey(),
                    ),
                )
            )
        ).let { encryptedPayload ->
            DhMessage(
                encryptedPayload,
                headers = mutableMapOf(
                    IMEI_HEADER to device.getImei(), PACKAGE_TYPE_HEADER to type,
                    DEVICE_HEADER to device
                )
            )
        }.also { dhMessage ->
            val packetType = getPacketTypeDesc(payloadWithCrc16, type)
            log.info { "${device.getDeviceInfo()}[Сервер->регистратор][Тип=$packetType] Пакет: `${payloadWithCrc16.toHex()}`. MessageId=${dhMessage.messageId}" }
        }
    }

    fun decryptMessage(senderAndPayload: RecipientAndPayload): List<DhMessage> {
        return teleofisByteStuffingInput(senderAndPayload.payload)
            .map { encryptedMessage: EncryptedMessage -> senderAndPayload.recipient to encryptedMessage }
            .also { packs ->
                if (packs.isEmpty()) throw IllegalArgumentException("Ошибка разбора байт-стаффинга. После байт-стаффинга данных нет. Sender: ${senderAndPayload.recipient}")
            }
            .map { (recipient, encryptedMessage) ->
                val messageId = UUID.randomUUID()
                val imei = encryptedMessage.getImeiReadable()
                val device = deviceProvider.getDevice(imei)
                val encryptionKey = device.register.encryptionKey
                val decodedBody = cutAndCheckCrc16(
                    device = device,
                    decodedBody = xteaDecrypt(
                        value = encryptedMessage.payload,
                        key = encryptionKey.toByteArray()
                    )
                )
                val type = PacketType.ofId(decodedBody[0].toUByte())
                log.info { "${device.getDeviceInfo()}[Регистратор->сервер]Получен и расшифрован пакет с типом: `${type.description}`. Содержимое пакета: `${decodedBody.toHex()}`. MessageId=$messageId" }
                DhMessage(
                    messageId = messageId,
                    payload = decodedBody.sliceArray(1 until decodedBody.size),
                    headers = mutableMapOf(
                        RECIPIENT_HEADER to recipient,
                        IMEI_HEADER to imei,
                        PACKAGE_TYPE_HEADER to type,
                        DEVICE_HEADER to device,
                        RECIPIENT_HEADER to senderAndPayload.recipient
                    )
                )
            }
    }

    private fun getPacketTypeDesc(payload: ByteArray, packetType: PacketType): String {
        var packetTypeStr = packetType.description
        if (packetType == DEVICE_SETUP_DATA || packetType == DEVICE_SETUP_RESPONSE) {
            packetTypeStr += "/${ParameterType.fromNumber(payload[1].toUByte())?.description ?: ""}"
        }
        return packetTypeStr
    }

    private fun cutAndCheckCrc16(device: TeleofisDevice, decodedBody: ByteArray): ByteArray {
        val imei = device.getImei()
        val crc16 = decodedBody.sliceArray(decodedBody.size - 2 until decodedBody.size).toUShortLittleEndian()
        val bodyWithoutCrc16 = decodedBody.sliceArray(0 until decodedBody.size - 2)
        val calculatedCrc16 = calculateCRC16CCIT(bodyWithoutCrc16)
        if (crc16 != calculatedCrc16) {
            val errorMessage = "[$imei]Illegal crc16. Actual: $calculatedCrc16. Expected: $crc16."
            log.error { errorMessage + " DecodedPayload: ${decodedBody.toHex()}" }
            throw TeleofisException(device, msg = errorMessage, null)
        }
        return bodyWithoutCrc16
    }

    @Autowired
    fun setEncryptionKeyProvider(@org.springframework.context.annotation.Lazy encryptionKeyProvider: TeleofisDeviceProvider) {
        this.deviceProvider = encryptionKeyProvider
    }

}