package com.synergy.datahub.teleofis.utils

import com.synergy.datahub.teleofis.model.EncryptedMessage
import com.synergy.datahub.utils.toByteArray
import com.synergy.datahub.utils.toInt
import com.synergy.datahub.utils.toLong
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant

const val BYTE_START = 0xc0.toByte()
const val BYTE_STOP = 0xc2.toByte()
const val BYTE_ESCAPE = 0xc4.toByte()

/**
 * Таблица для быстрого расчета CRC16 CCIT
 * Для полинома 0x1021
 */
private val codesCRC16: IntArray = intArrayOf(
    0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50a5, 0x60c6, 0x70e7, 0x8108, 0x9129, 0xa14a, 0xb16b, 0xc18c, 0xd1ad, 0xe1ce, 0xf1ef,
    0x1231, 0x0210, 0x3273, 0x2252, 0x52b5, 0x4294, 0x72f7, 0x62d6, 0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd, 0xc39c, 0xf3ff, 0xe3de,
    0x2462, 0x3443, 0x0420, 0x1401, 0x64e6, 0x74c7, 0x44a4, 0x5485, 0xa56a, 0xb54b, 0x8528, 0x9509, 0xe5ee, 0xf5cf, 0xc5ac, 0xd58d,
    0x3653, 0x2672, 0x1611, 0x0630, 0x76d7, 0x66f6, 0x5695, 0x46b4, 0xb75b, 0xa77a, 0x9719, 0x8738, 0xf7df, 0xe7fe, 0xd79d, 0xc7bc,
    0x48c4, 0x58e5, 0x6886, 0x78a7, 0x0840, 0x1861, 0x2802, 0x3823, 0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948, 0x9969, 0xa90a, 0xb92b,
    0x5af5, 0x4ad4, 0x7ab7, 0x6a96, 0x1a71, 0x0a50, 0x3a33, 0x2a12, 0xdbfd, 0xcbdc, 0xfbbf, 0xeb9e, 0x9b79, 0x8b58, 0xbb3b, 0xab1a,
    0x6ca6, 0x7c87, 0x4ce4, 0x5cc5, 0x2c22, 0x3c03, 0x0c60, 0x1c41, 0xedae, 0xfd8f, 0xcdec, 0xddcd, 0xad2a, 0xbd0b, 0x8d68, 0x9d49,
    0x7e97, 0x6eb6, 0x5ed5, 0x4ef4, 0x3e13, 0x2e32, 0x1e51, 0x0e70, 0xff9f, 0xefbe, 0xdfdd, 0xcffc, 0xbf1b, 0xaf3a, 0x9f59, 0x8f78,
    0x9188, 0x81a9, 0xb1ca, 0xa1eb, 0xd10c, 0xc12d, 0xf14e, 0xe16f, 0x1080, 0x00a1, 0x30c2, 0x20e3, 0x5004, 0x4025, 0x7046, 0x6067,
    0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d, 0xd31c, 0xe37f, 0xf35e, 0x02b1, 0x1290, 0x22f3, 0x32d2, 0x4235, 0x5214, 0x6277, 0x7256,
    0xb5ea, 0xa5cb, 0x95a8, 0x8589, 0xf56e, 0xe54f, 0xd52c, 0xc50d, 0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466, 0x6447, 0x5424, 0x4405,
    0xa7db, 0xb7fa, 0x8799, 0x97b8, 0xe75f, 0xf77e, 0xc71d, 0xd73c, 0x26d3, 0x36f2, 0x0691, 0x16b0, 0x6657, 0x7676, 0x4615, 0x5634,
    0xd94c, 0xc96d, 0xf90e, 0xe92f, 0x99c8, 0x89e9, 0xb98a, 0xa9ab, 0x5844, 0x4865, 0x7806, 0x6827, 0x18c0, 0x08e1, 0x3882, 0x28a3,
    0xcb7d, 0xdb5c, 0xeb3f, 0xfb1e, 0x8bf9, 0x9bd8, 0xabbb, 0xbb9a, 0x4a75, 0x5a54, 0x6a37, 0x7a16, 0x0af1, 0x1ad0, 0x2ab3, 0x3a92,
    0xfd2e, 0xed0f, 0xdd6c, 0xcd4d, 0xbdaa, 0xad8b, 0x9de8, 0x8dc9, 0x7c26, 0x6c07, 0x5c64, 0x4c45, 0x3ca2, 0x2c83, 0x1ce0, 0x0cc1,
    0xef1f, 0xff3e, 0xcf5d, 0xdf7c, 0xaf9b, 0xbfba, 0x8fd9, 0x9ff8, 0x6e17, 0x7e36, 0x4e55, 0x5e74, 0x2e93, 0x3eb2, 0x0ed1, 0x1ef0
)

fun Instant.toByteArray() =
    ByteBuffer.allocate(Int.SIZE_BYTES).apply {
        order(ByteOrder.LITTLE_ENDIAN)
        putInt(epochSecond.toInt())
        flip()
    }.array()

/**
 * Получить замену для второго байта после "сигнального"
 */
fun getReplacement(byte: Byte) =
    when (byte) {
        0xc1.toByte() -> BYTE_START
        0xc3.toByte() -> BYTE_STOP
        0xc4.toByte() -> BYTE_ESCAPE
        else -> {
            throw Exception("Ошибка замены байт-стаффинга")
        }
    }


/**
 * Разбивка сообщения, с одновременным байт-стаффингом (в один проход)
 */
fun teleofisByteStuffingInput(input: ByteArray): List<EncryptedMessage> {
    val res = mutableListOf<EncryptedMessage>()
    var i = 0 // Очередной байт в исходном массиве
    var chunkOffset = 0 // Смещение относительно начала куска
    var scanning = false // Признак, что мы находимся внутри распознанного куска
    var chunk = ByteArray(0) // Очередной кусок с данными

    while (i < input.size) {
        when (input[i]) {
            BYTE_START -> {
                chunkOffset = 0
                chunk = ByteArray(input.size)
                scanning = true
                i++
                continue
            }

            BYTE_ESCAPE -> {
                if (scanning) {
                    chunk[chunkOffset++] = getReplacement(input[++i])
                }
                ++i
            }

            BYTE_STOP -> {
                if (scanning) {
                    scanning = false
                    if (chunkOffset < 8) throw Exception("Ошибка разбора байт-стаффинга. Блок слишком мал")
                    res.add(
                        EncryptedMessage(
                            chunk.copyOfRange(0, 8),
                            chunk.copyOfRange(8, chunkOffset)
                        )
                    )
                }
                ++i
                continue
            }

            else -> {
                if (!scanning) {
                    i++; continue
                }
                chunk[chunkOffset++] = input[i++]
            }
        }
    }

    return res
}

/**
 * Обратный байт-стаффинг с одновременным объединением нескольких пакетов в один (для отправки сообщения)
 * Пакеты должны быть подготовлены (зашифрованы, подсчитана CRC и т.д.)
 */
fun teleofisByteStuffingOutput(input: List<EncryptedMessage>): ByteArray {
    //val size = input.map { it.payload.size + 10 }.reduce { acc, i -> acc + i }
    //var res = ByteArray(if (size < 32748) size + (size shr 7) else size + 256)
    val size = input.map { (it.payload.size + 12) * 2 }.reduce { acc, i -> acc + i }
    var res = ByteArray(size + 1)

    var offset = 0
    input.forEach {
        //if (res.size - offset < 16) res += ByteArray(128) // Если в массиве мало места, добавим

        res[offset++] = BYTE_START

        (it.imei + it.payload).forEach { c ->
            if (res.size - offset < 2) res += ByteArray(128)
            when (c) {
                BYTE_START -> {
                    res[offset++] = 0xC4.toByte()
                    res[offset++] = 0xC1.toByte()
                }

                BYTE_STOP -> {
                    res[offset++] = 0xC4.toByte()
                    res[offset++] = 0xC3.toByte()
                }

                BYTE_ESCAPE -> {
                    res[offset++] = 0xC4.toByte()
                    res[offset++] = 0xC4.toByte()
                }

                else -> {
                    res[offset++] = c
                }
            }
        }
        res[offset++] = BYTE_STOP
    }
    return res.copyOf(offset)
}

/**
 * Расчет CRC-16 CCIT для полинома 0x1021
 *
 * @param source Массив байт
 */
fun calculateCRC16CCIT(source: ByteArray): UShort {
    var r: UInt = 0xffffu
    var index: Int

    for (element in source) {
        index = (((r shr 8) xor element.toUInt()) and 0xffu).toInt()
        r = (r shl 8) and 0xffffu xor codesCRC16[index].toUInt()
    }
    return r.toUShort()
}

/**
 * Непосредственная реализация алгоритма расшифровки xtea для 8-байтового сообщения со 128-битным ключом
 */
private fun xteaDecryptL(v: ULong, key: IntArray, rounds: Int = 32): ULong {
    var v0: UInt = (v and 0xffffffffu).toUInt()
    var v1: UInt = (v and 0xffffffff00000000u shr 32).toUInt()
    val delta = 0x9E3779B9.toUInt()
    var sum: UInt = delta * rounds.toUInt();

    for (i in 0 until rounds) {
        v1 -= (((v0 shl 4) xor (v0 shr 5)) + v0) xor (sum + key[((sum shr 11) and 3u).toInt()].toUInt())
        sum -= delta
        v0 -= (((v1 shl 4) xor (v1 shr 5)) + v1) xor (sum + key[(sum and 3u).toInt()].toUInt());
    }

    return v0.toULong() xor (v1.toULong() shl 32)
}

/**
 * Расшифровать сообщение по алгоритму xtea
 *
 * @param value Сообщение
 * @param key Ключ
 * @param rounds Число циклов алгоритма (по умолчанию - 32)
 */
fun xteaDecrypt(value: ByteArray, key: ByteArray, rounds: Int = 32): ByteArray {
    var res = ByteArray(0)
    val k = IntArray(key.size / 4)
    for (i in key.indices step 4) {
        k[i / 4] = key.toInt(i)
    }
    for (i in value.indices step 8) {
        res += xteaDecryptL(value.toLong(i).toULong(), k, rounds).toByteArray()
    }
    return res
}

/**
 * Непосредственная реализация алгоритма шифрования xtea для 8-байтового сообщения со 128-битным ключом
 */
private fun xteaEncryptL(v: ULong, key: IntArray, rounds: Int = 32): ULong {
    var v0: UInt = (v and 0xffffffffu).toUInt()
    var v1: UInt = (v and 0xffffffff00000000u shr 32).toUInt()
    val delta = 0x9E3779B9.toUInt()
    var sum: UInt = 0u

    for (i in 0 until rounds) {
        v0 += (((v1 shl 4) xor (v1 shr 5)) + v1) xor (sum + key[(sum and 3u).toInt()].toUInt())
        sum += delta
        v1 += (((v0 shl 4) xor (v0 shr 5)) + v0) xor (sum + key[((sum shr 11) and 3u).toInt()].toUInt());
    }

    return v0.toULong() xor (v1.toULong() shl 32)
}

/**
 * Зашифровать сообщение по алгоритму xtea
 *
 * @param value Сообщение
 * @param key Ключ
 * @param rounds Число циклов алгоритма (по умолчанию - 32)
 */
fun xteaEncrypt(value: ByteArray, key: ByteArray, rounds: Int = 32): ByteArray {

    var res = ByteArray(0)
    val k = IntArray(key.size / 4)
    for (i in key.indices step 4) {
        k[i / 4] = key.toInt(i)
    }

    for (i in value.indices step 8) {
        res += xteaEncryptL(value.toLong(i).toULong(), k, rounds).toByteArray()
    }

    return res
}
