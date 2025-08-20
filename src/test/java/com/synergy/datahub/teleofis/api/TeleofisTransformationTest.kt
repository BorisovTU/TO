//package com.synergy.datahub.teleofis.api
//
//import com.synergy.datahub.model.Transfer
//import com.synergy.datahub.teleofis.ext.MessageProcessingExt
//import com.synergy.datahub.teleofis.model.EncryptedMessage
//import com.synergy.datahub.teleofis.model.PacketType
//import com.synergy.datahub.teleofis.model.ParameterData
//import com.synergy.datahub.teleofis.model.ParameterType
//import com.synergy.datahub.teleofis.model.message.ReadSettingsMessage
//import com.synergy.datahub.teleofis.service.DeviceProvider
//import com.synergy.datahub.teleofis.service.EncryptionService
//import com.synergy.datahub.teleofis.transport.*
//import com.synergy.datahub.transport.toByteArray
//import com.synergy.datahub.transport.toHex
//import com.synergy.transport.model.RecipientAndPayload
//import io.mockk.MockKAnnotations.init
//import io.mockk.every
//import io.mockk.impl.annotations.MockK
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.BeforeAll
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.TestInstance
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.time.ZonedDateTime
//import java.util.*
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class TeleofisTransformationTest {
//    @MockK
//    lateinit var transfer: Transfer
//
//    private val encryptionKey = "yuyuyuyuopopopop"
//    private val imei = "861694033595016".toULong()
//    private val encryptionService = EncryptionService().apply {
//        setEncryptionKeyProvider(
//            object : DeviceProvider {
//                override fun getDevice(imei: ULong): String {
//                    return encryptionKey
//                }
//            }
//        )
//    }
//
//    @BeforeAll
//    fun setup() {
//        init(this)
//        every { transfer.encriptionKey }.returns(encryptionKey)
//        every { transfer.serial }.returns(imei.toString())
//    }
//
//    @Test
//    fun testDecode() {
////        val telemetry = TeleofisDataMapper.decryptMessage(DhMessage(packetToDecode.hexToByteArray()))
////        assertNotNull(telemetry)
//    }
//
//    @Test
//    fun testEncode() {
//        val packets = teleofisByteStuffingInput(packetToDecode.hexToByteArray())
//        val packet = packets.first()
//        var decodedBody = xteaDecrypt(packet.payload, encryptionKey.toByteArray())
//        //удалить сrc16
//        decodedBody = decodedBody.sliceArray(0 until decodedBody.size - 2)
//        var encodedBody = decodedBody
//        //добить тело сообщения нулями если его длинна не кратна 8
//        encodedBody = encodedBody.padToMultipleOfEight()
//        //посчитать сrc16 и добавить в конец сообщения
//        encodedBody += calculateCRC16CCIT(encodedBody).toShort().toByteArray()
//        encodedBody = xteaEncrypt(encodedBody, encryptionKey.toByteArray())
//        val encodedResult = teleofisByteStuffingOutput(
//            listOf(
//                EncryptedMessage(
//                    packet.imei, encodedBody
//                )
//            )
//        )
//        assertEquals(encodedResult.toHex(), packetToDecode.hexToByteArray().toHex())
//    }
//
//    @Test
//    fun testTelemetryResponse() {
//        val response =
//            encryptionService.encryptMessage(
//                imei = imei,
//                PacketType.TELEMETRY_INFORMATION_TRANSFER
//            )
//
//        val packet = teleofisByteStuffingInput(response.payload).first()
//        val decodedBody = xteaDecrypt(packet.payload, encryptionKey.toByteArray())
//        assertEquals(packet.getImeiReadable(), imei)
//        assertEquals("090000000000F246".hexToByteArray().toHex(), decodedBody.toHex())
//    }
//
//    @Test
//    fun encodeDecode() {
//        val imeiBytes = ByteArray(imeiHex.size) { pos -> imeiHex[pos].toByte() }
//        val imeiBuffer = ByteBuffer.wrap(imeiBytes).order(ByteOrder.LITTLE_ENDIAN)
//        every { transfer.serial }.returns(imeiBuffer.long.toString())
//        every { transfer.encriptionKey }.returns(encryptionKey)
////        val telemetry = TeleofisDataMapper.decryptMessage(
////            message = DhMessage(packetToDecode.hexToByteArray())
////        ) as TelemetryMessage
////        assertEquals(telemetry.raw.toHex(), clearBodyAfterDecoding.hexToByteArray().toHex())
////        val encryptedBody = TeleofisDataMapper.encryptMessage(
////            transfer.serial.toULong(),
////            RawPacketToDevice(PacketType.TELEMETRY_INFORMATION_TRANSFER, data = telemetry.raw.copyOfRange(1, telemetry.raw.size))
////        ).payload
////        assertEquals(encryptedBody.toHex(), packetToDecode.hexToByteArray().toHex())
//    }
//
//    @Test
//    fun testExample() {
//        println("861694033595016".toULong().toByteArray().toHex())
//
//        //c040f3acbe30150300e8c697fccb68b730bf0b888a2c8b70cfc2
//        //текущее время
//        //c040f3acbe30150300d98cc871b8fde626517cde1b339d434bc2
//        //часовой пояс
//        //c040f3acbe301503003cb199cc6499fbc3c2
//        //COMMAND_SUCCESSFUL - команда выполнена текущее время
//        //c040f3acbe30150300e5f93d5eecf6b414c2
////        val bytes = "c0e16b99be3015030086a93ebfebac5201664da3ae6dab750429c9e563986aca35ecb0c85768f2f2dc31c9037dafd31f01ecb0c85768f2f2dc7b00be7e5a15fee1e78c63c58c2c686148d2ce8066754222893fed9e0766994fdc35f23993bdd2701512f0702a9ed84aa84fa0fd690d94e5900146786840dc0bc536ad6afb6e4e3267fb045dd9c7e670f1c4c3d2ac1fcc71ad06b7b194de4031f4046744610aafa7b9d9b60155f59813794670ebfa2528d7f2ecb0c85768f2f2dc58c807ad822cadd01334d8e00d6d037ea8a584c9ac3deb18ddb3366e076f6e470cc4c41df1eb3a8d930fd631a4a5328a39ecb0c85768f2f2dcecb0c85768f2f2dcecb0c85768f2f2dc82e715e7952a79c4c49a3ce64fc4c3b11c9cb5eabb873ae706b4c8b008128df0af80fece91741fc5f6411145aab35ac9f6e0f8a937baed012d00de386aaf6c5f2a2c0495b6431770a6ddf7b32acd33d6db83d53ce7e4740f4c24eb959359fab3c3e0deeeb816c6f4b0e4cba6b506821a726016aef5f3d62598c4c162744402bcf1c970c4c41b691dc4c10431af14cb84b2c31d9b729bc4c31a431a2085480478de47b20611a2370c2727be034711d74102d88730a7f8311c5ffb54298c2bdee03d98df67cf8c5e42c37506698d8860ceeb76554a45762d9a6a147e6f537add204f95db7cd7b2d2eec57a9712dadac2".hexToByteArray()
////        val bytes = "c0e16b99be30150300cf16eb2b8f90a5fc78cbfead6ab5522db49bd2494bb56a0fc2".hexToByteArray()
////        val bytes = "c0530e2633c50e0300d4f4ff174408220615fcde5c80ace2a53252ed3808fb7eabd5bcc310492f4a80b996d85d6439991fc7cdbeffc4c4bac374b3acc4c48e72523e24c2".hexToByteArray()
//        val bytes =
//            "c0530e2633c50e0300a2bc314ae5c638bcc2".hexToByteArray()
//        val message = MessageProcessingExt.resolveMessage(encryptionService.decryptMessage(RecipientAndPayload(payload = bytes))[0])
//
//////        (events[0] as TelemetryMessage)
//        println(message)
//    }
//
//    @Test
//    fun createExample() {
//        val dd = ReadSettingsMessage.parse("1".toULong(), ByteBuffer.wrap("0100046ed63065000000000000".hexToByteArray()))
//        val ff = ParameterData(
//            type = ParameterType.CURRENT_TIME,
//            dataArr = ZonedDateTime.now().toEpochSecond().toInt().toByteArray()
//        )
////        val sls = mapper.decryptMessage<ReadSettingsMessage>(
////            DhMessage(
////                "070100046ed63065000000000000".hexToByteArray()
////            ),
////        ) { teleofisService.getEncryptionKey(imei) }
//        //090000000000F246 - ответ на телеметрию
//        //0101041ECB4C5900000000000000F589 - установка времени
//        //013208FFFFFFFFFFFFFFFF0000000654 - команда 50, какие данные будут в телеметрии
//        //0137010000003E56 - окончание сеанса связи
//        encryptionService.encryptMessage(
//            "862095056955831".toULong(),
//            PacketType.TELEMETRY_INFORMATION_TRANSFER,
//            "0000000000".hexToByteArray()
//        ).payload.also { println(it.toHex()) }
//    }
//
//
//    companion object {
//        private val imeiHex = listOf(0xCB, 0x9B, 0x55, 0x88, 0x88, 0x11, 0x03, 0x00)
//        private val imeiString = imeiHex.joinToString("") { it.toString(16).uppercase(Locale.getDefault()).padStart(2, '0') }
//
//        private val startByte = "c0"
//        private val endByte = "c2"
//        private val packetToDecode = """
//    $startByte$imeiString
//    60614e680e705d0fefcf7ac8102c4452
//    ecb0c85768f2f2dc52415c43a36712f0
//    31c9037dafd31f01ecb0c85768f2f2dc
//    7b00be7e5a15fee1e78c63c58c2c6861
//    fef9a1c4c4130a354c846448512e6a97
//    ce4a9005690d1e3808f065c957538e1b
//    ac87e7228322ab39a6900146786840dc
//    0bc536ad6afb6e4e3267fb045dd9c7e6
//    70f1c4c3d2ac1fcc71ad06b7b194de40
//    31f4046744610aafa7b92fd3f392c3a5
//    eeb1474ffa60c4c1587e68ecb0c85768
//    f2f2dc2a88827461b41c99b2539b6bfd
//    cd4325be3ced59be7b594addb3366e07
//    6f6e470cc4c41df1eb3a8d93c99eb7bd
//    ad5a474c33659653762910d0ecb0c857
//    68f2f2dcecb0c85768f2f2dc82e715e7
//    952a79c4c4660074ccc50741cab5eabb
//    873ae706b4c8b008128df0af80fece91
//    741fc5f6411145aab35ac9f6e0f8a937
//    baed012d00c3be705a5e8c3440ddc1cd
//    4e0051cccc$endByte
//    """.trimIndent().cleanString()
//
//        private val clearBodyAfterDecoding = """
//        09300004100e00000104f47795590210
//        0000000000000000616161615d5d5d5d
//        09150000000000000000000000000000
//        000000000000000d1052545530322e30
//        312e3030303200000012040000000013
//        040000000014046161616115045d5d5d
//        5d1604010e00001704010e0000180401
//        0e00001904010e00001a04010e00001b
//        04010e00001c04010e00001d04010e00
//        001e01001f0100200100210100240100
//        25113235303032000000000000000000
//        0000002604ef1400002704930d00002d
//        01022e02e0012f05ffffffff00300103
//        3101003301003404050100003d20342e
//        3132382e323400000000000000000000
//        00000000000000000000000000004401
//        184f04960d0000500400000000570460
//        ea0000580422060000590460ea00005a
//        04e01500005b01005c01025d01005e01
//        005f010360010361010262010400
//    """.trimIndent().cleanString()
//
//        private fun String.cleanString(): String {
//            return this.replace("\n", "").replace(" ", "").replace("\t", "").trim()
//        }
//    }
//}