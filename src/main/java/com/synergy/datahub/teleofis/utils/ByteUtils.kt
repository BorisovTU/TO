package com.synergy.datahub.teleofis.utils

import java.nio.ByteBuffer
import java.time.Instant

fun ByteBuffer.getN(n: Int): ByteArray {
    val firstFour = ByteArray(n)
    get(firstFour, 0, n)
    return firstFour
}

fun ByteBuffer.getDateTime(): Instant {
    return getN(4).fromLittleEndianToInt().let { Instant.ofEpochSecond(it.toLong()) }
}

fun byteArrayToAscii(bytes: ByteArray): String {
    return bytes.joinToString("") { byte ->
        val intValue = byte.toInt() and 0xFF // Преобразование байта в положительное целое число
        if (intValue != 0) {
            intValue.toChar().toString()
        } else {
            "" // Пропустить NULL-символы
        }
    }
}

fun ByteArray.toUShortLittleEndian(): UShort {
    if (size != 2) throw IllegalArgumentException("ByteArray size should be exactly 2 bytes")
    return ((this[1].toInt() and 0xFF) shl 8 or (this[0].toInt() and 0xFF)).toUShort()
}

fun ByteArray.fromLittleEndianToInt(): Int {
    if (size != 4) throw IllegalArgumentException("ByteArray size should be exactly 4 bytes")
    return (this[3].toInt() and 0xFF shl 24) or
            (this[2].toInt() and 0xFF shl 16) or
            (this[1].toInt() and 0xFF shl 8) or
            (this[0].toInt() and 0xFF)
}

/**
 * Добавить в конец сообщения нули если его длинна не станет кратна 8 после добавления 2х байт с crc16
 */
fun ByteArray.padToMultipleOfEight(): ByteArray {
    val remainder = (size + 2) % 8
    if (remainder == 0) return this
    val padding = ByteArray(8 - remainder) { 0 }
    return this + padding
}

fun ByteBuffer.toByteArray(): ByteArray {
    return this.array()
}