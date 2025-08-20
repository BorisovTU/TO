package com.synergy.datahub.teleofis.model

import com.synergy.datahub.utils.toLong


/**
 * Пакет Teleofis (imei + зашифрованное содержимое), без байтстаффинга
 */
data class EncryptedMessage(
    val imei: ByteArray,
    //зашифрованные данные
    val payload: ByteArray,
) {
    /**
     * Получение IMEI в читабельном виде
     */
    fun getImeiReadable(): ULong = imei.toLong().toULong()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedMessage
        if (!imei.contentEquals(other.imei)) return false
        if (!payload.contentEquals(other.payload)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = imei.contentHashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}