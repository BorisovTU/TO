package com.synergy.datahub.teleofis.config

import com.synergy.datahub.teleofis.model.mapper.MessageMapper
import com.synergy.datahub.teleofis.utils.MessageProcessingExt
import com.synergy.transport.config.TcpTransportProperties
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Configuration

@Configuration
class TeleofisConfiguration(
    private val transportProperties: TcpTransportProperties,
    private val messageMappers: List<MessageMapper<*>>
) : CommandLineRunner {

    override fun run(vararg args: String) {
        with(MessageProcessingExt) {
            timeout = transportProperties.requestStrategy.timeout
            mappers = messageMappers.associateBy { it.getType() }
        }
    }

}