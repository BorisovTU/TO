//package com.synergy.datahub.teleofis.api
//
//import com.synergy.datahub.teleofis.mapper.ArchiveEventMapper
//import com.synergy.datahub.teleofis.model.DataType
//import com.synergy.datahub.teleofis.model.EventType
//import com.synergy.datahub.teleofis.model.message.ArchiveEventMessage
//import com.synergy.datahub.teleofis.model.message.DataEvent
//import com.synergy.datahub.teleofis.service.ParameterService
//import com.synergy.datahub.teleofis.service.TeleofisMonitoringService
//import com.synergy.datahub.teleofis.service.impl.DeviceDateTimeService
//import com.synergy.datahub.teleofis.service.impl.TeleofisServiceImpl
//import com.synergy.datahub.transport.DhTransportSession
//import io.mockk.MockKAnnotations
//import io.mockk.every
//import io.mockk.impl.annotations.MockK
//import io.mockk.mockkClass
//import org.junit.jupiter.api.BeforeAll
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.TestInstance
//import org.springframework.data.redis.core.RedisTemplate
//import reactor.core.publisher.Flux
//import reactor.core.publisher.Mono
//import java.time.ZonedDateTime
//import java.time.temporal.ChronoUnit
//import java.util.concurrent.TimeUnit
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class TeleofisDeviceApiTest {
//    @MockK
//    lateinit var teleofisService: TeleofisServiceImpl
//
//    @MockK
//    lateinit var parameterService: ParameterService
//
//    @MockK
//    lateinit var teleofisMonitoringService: TeleofisMonitoringService
//
//    @MockK
//    lateinit var meter: Meter
//
//    @MockK
//    lateinit var redisTemplate: RedisTemplate<String, Long>
//
//    @MockK
//    lateinit var session: DhTransportSession
//
//    lateinit var api: TeleofisDeviceApi
//
//    @MockK
//    lateinit var transfer: Transfer
//
//    @BeforeAll
//    fun setup() {
//        MockKAnnotations.init(this)
//        every { meter.transfer }.returns(transfer)
//        every { meter.transfer.encriptionKey }.returns("encriptionKey")
//        every { meter.impulseRegChannel }.returns(1)
//        every { meter.initialReading }.returns(1.0)
//        every { meter.initialImpulseReading }.returns(1.0)
//        every { meter.impulseWeight }.returns(0.01)
//        every { meter.transfer.serial }.returns("132")
//        every { teleofisService.getDeviceDateTime(any(), any()) }.returns(Mono.just(ZonedDateTime.now()))
//        val mock = mockkClass(Device::class)
//        every { mock.meter }.returns(meter)
//        every { mock.getDeviceKey() }.returns("3223")
//        val archiveEventMapper = ArchiveEventMapper(DeviceDateTimeService(parameterService,redisTemplate))
//        api = TeleofisDeviceApi(teleofisMonitoringService, mock, session, teleofisService, archiveEventMapper)
//    }
//
//    @Test
//    fun readDailyData() {
//        val from = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(2)
//        val to = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
//        val eventData = DataEvent.EventData(DataType.COUNTER_VALUE_1, 100)
//        val listArchiveEventMessage = listOf(
//            //DataEvent(EventType.TIME_INTERVAL, ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(2).plusHours(3).toInstant(), listOf(eventData)),
//            //DataEvent(EventType.TIME_INTERVAL, ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(1).plusHours(3).toInstant(), listOf(eventData)),
//            //DataEvent(EventType.TIME_INTERVAL, ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).plusHours(3).toInstant(), listOf(eventData)),
//            //Данные на момент передачи - отправляются всегда
//            DataEvent(EventType.TIME_INTERVAL, ZonedDateTime.now().plusHours(3).toInstant(), listOf(eventData)),
//        )
//
//
//        val archiveEventMessage = ArchiveEventMessage(imei = 123u, packetId = 1u, listArchiveEventMessage)
//
//        every { teleofisService.getEvents(any(), any(), any(), any()) }.returns(Flux.fromIterable(listOf(archiveEventMessage)))
//
//        api.getDailyData(123L, from, to)
//            .doOnNext { println(it) }
//            .subscribe()
//
//        TimeUnit.MILLISECONDS.sleep(300)
//    }
//}
