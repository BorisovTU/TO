package com.synergy.datahub.teleofis.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "data-hub.teleofis")
class TeleofisProperties(
    val connectionInterval: Duration,
    val checkActiveInterval: Duration,
    val closeAfter2periodsPlus: Duration,
    val workMode: WorkModeProperties,
    val cache: Cache
) {
    data class WorkModeProperties(
        val forceChange: Boolean,
    )

    data class Cache(
        val useCache: Boolean,
        val expireAfter: Duration,
    )
}