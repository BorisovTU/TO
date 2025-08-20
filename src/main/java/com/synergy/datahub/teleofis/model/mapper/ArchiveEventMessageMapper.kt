package com.synergy.datahub.teleofis.model.mapper

import com.synergy.datahub.device.api.DeviceMessage
import com.synergy.datahub.teleofis.model.*
import com.synergy.datahub.teleofis.model.PacketType.COUNTER_DATA
import com.synergy.datahub.teleofis.utils.fromLittleEndianToInt
import com.synergy.datahub.teleofis.utils.getDateTime
import com.synergy.datahub.teleofis.utils.getImei
import com.synergy.datahub.teleofis.utils.getN
import com.synergy.datahub.transport.DhMessage
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.time.Instant

@Component
class ArchiveEventMessageMapper : MessageMapper<ArchiveEventMessage> {
    private val log = KotlinLogging.logger {}

    override fun map(message: DhMessage): ArchiveEventMessage {
        val imei = message.getImei()
        val buffer = ByteBuffer.wrap(message.payload)
        val packetId: UByte = buffer.get().toUByte()
        val dataEventList = mutableListOf<DataEvent>()
        while (buffer.hasRemaining()) {
            val eventCode = buffer.get().toUByte()
            if (eventCode == 0.toUByte()) {
                break
            }
            val eventType = EventType.getByCode(eventCode)
            val eventTime = buffer.getDateTime()
            //Длина данных для события (и тип данных, и сами данные)
            val eventDataLength = buffer.get().toInt() and 0xFF
            val eventDataList = mutableListOf<DataEvent.EventData>()
            val eventData = ByteBuffer.wrap(buffer.getN(eventDataLength))
            while (eventData.hasRemaining()) {
                //тип данных (значения счетчиков)
                val dataTypeCode = eventData.get().toInt() and 0xFF
                val dataType = DataType.fromNumber(dataTypeCode)
                var dataValue: Int
                if (dataType != null) {
                    dataValue = if (dataType.size == 1) {
                        eventData.get().toInt() and 0xFF
                    } else if (dataType.size == 4) {
                        eventData.getN(4).fromLittleEndianToInt()
                    } else {
                        log.error("Unsupported EventData Length ${dataType.size} imei $imei")
                        break
                    }
                    eventDataList.add(DataEvent.EventData(dataType, dataValue))
                } else {
                    log.error("Unknown Data Code $dataTypeCode imei $imei")
                    break
                }
            }
            dataEventList.add(DataEvent(eventType!!, eventTime, eventDataList))
        }
        return ArchiveEventMessage(events = dataEventList, packetId = packetId)
    }

    override fun getType() = COUNTER_DATA
}

//Формат передачи данных со счетчиков
data class ArchiveEventMessage(
    val packetId: UByte,
    val events: List<DataEvent>,
) : DeviceMessage

data class DataEvent(
    val code: EventType,
    val dateTime: Instant,
    val data: List<EventData>,
) {
    data class EventData(
        val type: DataType, val value: Int,
    )
}
