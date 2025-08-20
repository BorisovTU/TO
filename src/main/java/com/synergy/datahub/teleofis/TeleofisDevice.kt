package com.synergy.datahub.teleofis

import com.synergy.datahub.device.api.Device
import com.synergy.datahub.ext.MetricNames
import com.synergy.device.model.RegisterImpulse
import com.synergy.device.model.TeleofisMeter

class TeleofisDevice(
    val register: RegisterImpulse,
    val meters: List<TeleofisMeter>
) : Device {
    private val deviceKey = register.id.toString()

    override fun getId(): Long = register.id

    override fun getSerialNumber(): String = register.serial

    override fun getModel(): String = register.model

    override fun getTransportKey(): String = getTransferId().toString()

    override fun getTags(): Map<String, String> {
        return mapOf(
            MetricNames.TRANSPORT_KEY to deviceKey,
            MetricNames.TRANSFER_ID to register.id.toString(),
        ) + register.tags
    }

    override fun getTransferId(): Long = register.id

    override fun getMeasurePoints(): List<Long> = meters.map { it.measurePoint.id }

    override fun getLogInfo(): String  = "[УПД=${getTransferId()}]"

    fun getImei(): ULong = register.serial.toULong()

    fun getEncryptionKey(): ByteArray = register.encryptionKey.toByteArray()
}