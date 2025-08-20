package com.synergy.datahub.teleofis.utils

import com.synergy.datahub.teleofis.model.PacketType
import com.synergy.datahub.transport.DhMessage

const val IMEI_HEADER = "imei"
const val PACKAGE_TYPE_HEADER = "type"

fun DhMessage.getImei() = requireNotNull(getHeaders()[IMEI_HEADER]) { "imei must not be null" } as ULong
fun DhMessage.getType() = requireNotNull(getHeaders()[PACKAGE_TYPE_HEADER]) { "package type must not be null" } as PacketType