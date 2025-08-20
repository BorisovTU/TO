package com.synergy.datahub.teleofis.model

import com.synergy.datahub.teleofis.utils.byteArrayToAscii
import com.synergy.datahub.teleofis.utils.fromLittleEndianToInt
import com.synergy.datahub.teleofis.utils.toByteArray
import com.synergy.datahub.utils.toByteArray
import com.synergy.datahub.utils.toHex
import com.synergy.datahub.utils.toInt
import com.synergy.device.model.WorkMode
import java.nio.ByteBuffer
import java.time.Instant

class ParameterData(val type: ParameterType, private val dataArr: ByteArray) {
    fun getDataSize() = dataArr.size
    fun getData() = dataArr
    fun getAsNumber(): Int =
        when (getDataSize()) {
            1 -> {
                dataArr[0].toInt() and 0xFF
            }

            4 -> {
                dataArr.fromLittleEndianToInt()
            }

            else -> {
                throw IllegalArgumentException("Неконсистентные данные для параметра ${type.name}.. hex: ${getData().toHex()}")
            }
        }

    fun getAsArray(): List<Int> {
        val values = mutableListOf<Int>()
        for (i in 0 until 4) {
            values.add(dataArr.toInt(i * 4))
        }
        return values
    }

    fun getAsString(): String {
        return byteArrayToAscii(getData())
    }

    fun getAsInstant(): Instant = Instant.ofEpochSecond(getAsNumber().toLong())

    override fun toString(): String {
        return "[${type.description}]:${getData().toHex()}"
    }

    companion object {
        fun changeWorkMode(workMode: WorkMode): ParameterData {
            return ParameterData(
                type = ParameterType.CHANGE_SPORADIC_MODE,
                dataArr = ByteArray(1).apply { set(0, if (workMode == WorkMode.Sporadic) 0 else 1) }
            )
        }

        fun archiveStopReceiving(): ParameterData {
            return ParameterData(
                type = ParameterType.ARCHIVE_END,
                dataArr = ByteArray(0)
            )
        }

        fun archiveStartReceiving(from: Instant, to: Instant): ParameterData {
            return ParameterData(
                type = ParameterType.ARCHIVE,
                dataArr = ByteBuffer.allocate(8).apply {
                    put(from.toByteArray())
                    put(to.toByteArray())
                    flip()
                }.toByteArray()
            )
        }

        fun updateDeviceTs(): ParameterData {
            return ParameterData(
                type = ParameterType.CURRENT_TIME,
                dataArr = Instant.now().epochSecond.toInt().toByteArray()
            )
        }
    }
}