package com.synergy.datahub.teleofis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class TeleofisDataHubApp

fun main(args: Array<String>) {
    runApplication<TeleofisDataHubApp>(*args)
}