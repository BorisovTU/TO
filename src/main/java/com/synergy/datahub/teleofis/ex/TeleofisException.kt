package com.synergy.datahub.teleofis.ex

import com.synergy.datahub.teleofis.TeleofisDevice

class TeleofisException(val device: TeleofisDevice, private val msg: String, cause: Throwable?) : RuntimeException(msg, cause) {
    override val message: String
        get() {
            val causeMessage = cause?.message
            return if (causeMessage != null) {
                "$msg (caused by: $causeMessage)"
            } else {
                msg
            }
        }
}