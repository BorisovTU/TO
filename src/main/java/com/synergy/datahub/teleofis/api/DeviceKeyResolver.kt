package com.synergy.datahub.teleofis.api

import com.synergy.datahub.device.api.Device
import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.transport.DhMessage
import com.synergy.datahub.transport.DhTransport
import com.synergy.transport.TransportKeyResolver
import com.synergy.transport.ext.getDevice
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * f2 - режим(часовой) каждые 5 минут, в этом режиме регистратор не реагирует на команды запроса архивов за период (нет реакции на любые команды после получения последнего архива)
 * f1 - режим(недельный) приходит в 9мск каждый день, в этом режиме регистратор реагирует на команды запроса архивов за период после получения всех пакетов (телеметрия+архив событий)
 * режим месячный(можно выставить каждый день и каждый час)
 * справедливо как для udp так и для tcp
 */
@Component
class TeleofisDeviceIdentifyService : TransportKeyResolver {
    private val log = KotlinLogging.logger {}

    override fun resolve(transport: DhTransport, message: DhMessage?): Mono<Device> {
        requireNotNull(message) { "teleofis message is null" }
        val device = message.getDevice() as TeleofisDevice
        return Mono.just(device)
    }
}
