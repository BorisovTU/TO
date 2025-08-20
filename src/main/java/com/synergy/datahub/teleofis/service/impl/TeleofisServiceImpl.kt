package com.synergy.datahub.teleofis.service.impl

import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.config.TeleofisProperties
import com.synergy.datahub.teleofis.model.ParameterData
import com.synergy.datahub.teleofis.model.ParameterType
import com.synergy.datahub.teleofis.model.mapper.ArchiveEventMessage
import com.synergy.datahub.teleofis.service.ArchiveEventProvider
import com.synergy.datahub.teleofis.service.ParameterService
import com.synergy.datahub.teleofis.service.TelemetryProvider
import com.synergy.datahub.teleofis.service.TeleofisService
import com.synergy.datahub.transport.DhTransport
import com.synergy.datahub.transport.DuplexChannel
import com.synergy.datahub.utils.toByteArray
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Component
class TeleofisServiceImpl(
    private val teleofisProperties: TeleofisProperties,
    private val parameterService: ParameterService,
    private val telemetryProvider: TelemetryProvider,
    private val archiveEventProvider: ArchiveEventProvider,
) : TeleofisService {
    private val log = KotlinLogging.logger {}

    override fun getEvents(
        transport: DhTransport, device: TeleofisDevice, from: ZonedDateTime, to: ZonedDateTime,
    ): Flux<ArchiveEventMessage> {
        return archiveEventProvider.get(transport, device, from, to)
    }

    override fun updateDeviceDateTime(transport: DhTransport, device: TeleofisDevice): Mono<Void> {
        return parameterService.setParameter(
            session = transport, device = device,
            parameter = ParameterData.updateDeviceTs()
        )
    }

    override fun getParameters(transport: DhTransport, device: TeleofisDevice, vararg types: ParameterType): Mono<Map<ParameterType, ParameterData>> {
        val result = mutableMapOf<ParameterType, ParameterData>()
        val imei = device.getImei()
        if (teleofisProperties.cache.useCache) {
            telemetryProvider.get(imei)?.also { telemetry ->
                types.forEach { type -> telemetry.getParameter(type)?.also { result[type] = it } }
            }
        }
        val toGetParameters = types.toList() - result.keys
        if (toGetParameters.isEmpty()) {
            return result.toMono()
        }
        return toGetParameters.toFlux()
            .flatMap { parameter ->
                parameterService.getParameter(transport, device, parameter)
            }
            .collectList()
            .map { parameters ->
                parameters.associateBy { it.type } + result
            }
    }

    override fun restartDevice(transport: DhTransport, device: TeleofisDevice, restartAfter: Duration): Mono<Void> =
        parameterService.setParameter(
            session = transport, device = device,
            parameter = ParameterData(
                type = ParameterType.RESTART,
                dataArr = restartAfter.seconds.toInt().toByteArray()
            )
        )

    override fun sleepDevice(transport: DhTransport, device: TeleofisDevice): Mono<Void> {
        return parameterService.setParameter(
            session = transport, device = device,
            parameter = ParameterData(
                type = ParameterType.CLOSE_SESSION, dataArr = ByteArray(0)
            ),
            waitResponse = false
        )
    }

    override fun getDeviceDateTime(transport: DhTransport, device: TeleofisDevice): Mono<ZonedDateTime> {
        return getParameter(transport, ParameterType.CURRENT_TIME, device)
            .map { parameter -> parameter.getAsInstant() }
            .flatMap { deviceTs ->
                getParameter(transport, ParameterType.TIME_ZONE, device)
                    .map { parameter -> deviceTs.atZone(ZoneOffset.ofHours(parameter.getAsNumber())) }
            }
    }

    override fun stopReceivingArchive(channel: DuplexChannel, device: TeleofisDevice) {
        parameterService.setParameter(channel, device, ParameterData.archiveStopReceiving())
    }

    override fun acknowledgementArchive(channel: DuplexChannel, device: TeleofisDevice, packetId: UByte) {
        parameterService.acknowledgementArchive(channel, device, packetId)
    }

    companion object {
        fun TeleofisService.getParameter(session: DhTransport, type: ParameterType, device: TeleofisDevice): Mono<ParameterData> {
            return getParameters(session, device, type).mapNotNull { parameters -> parameters[type] }
        }

    }
}
