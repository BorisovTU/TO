//package com.synergy.datahub.teleofis.api
//
//import com.synergy.datahub.teleofis.config.TeleofisProperties
//import com.synergy.datahub.teleofis.service.DeviceProvider
//import com.synergy.datahub.teleofis.service.EncryptionService
//import com.synergy.datahub.teleofis.transport.hexToByteArray
//import com.synergy.transport.config.ServerType
//import com.synergy.transport.model.RecipientAndPayload
//import com.synergy.transport.session.channel.MessageChannel
//import org.junit.Test
//import org.mockito.Mockito
//import reactor.kotlin.core.publisher.toFlux
//
//class InboundChannelMessagesTest {
//    private val encryptionKey = "yuyuyuyuopopopop"
//
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
//    @Test
//    fun testChannel() {
//        val teleofisProperties: TeleofisProperties = Mockito.mock(TeleofisProperties::class.java)
//        InboundChannelMessages(encryptionService, emptyList()).doMap(
////            listOf(RecipientAndPayload(payload = "c0530e2633c50e0300c95098859e81fe6c6627856a549df5ea3252ed3808fb7eabcd0396f891704565c4c3f9f686737dab8ec2".hexToByteArray())).toFlux(), MessageChannel()
////            c0530e2633c50e0300c95098859e81fe6c6627856a549df5ea3252ed3808fb7eabcd0396f891704565c4c3f9f686737 dab8e
//            listOf(
//                RecipientAndPayload(payload = "c0530e2633c50e03006a8da31d89e9102fe1699e3bfe5f1bf722fe35d528d0d4a88d8f4c10a3cded2842de9521509a3a721849e21de6b621ddaa0f36e295158802e78c63c58c2c6861f0eadb1cb42d1bb65aef9a47c4c130db7adc35f23993bdd270e39d18cee6ba7ffaa84fa0fd690d94e5900146786840dc0bc536ad6afb6e4e3267fb045dd9c7e670f1c4c3d2ac1fcc71ad06b7b194de4031f4046744610aafa7b9da65d0b9e341b59053f646786179f84decb0c85768f2f2dc10ce3ceeee47871367fd91c4c13c8d1b035decfdabb37bf779b2f3ef25e57adfd40cc4c41df1eb3a8d939a378918f6bb4d4665f5c4c4378a86a56eecb0c85768f2f2dcecb0c85768f2f2dc82e715e7952a79c4c473417620f62a946729113879667b5fbfc8b008128df0af80fece91741fc5f6411145aab35ac9f6e0f8a937baed012d00de386aaf6c5f2a2c0495b6431770a6ddf7b32acd33d6db833a78c8ece4dfd771a8f18cc626a9b663aa880c4635230fa301e2c334f9d9118ed0589c0be4bf7bfabf24260e47dd246171b3ae57a819b89d351204b27b717644d516ebce6aa55975da4f9553c4c12dfefa01492e1661466a0475c758d76ce61ef3c2c0530e2633c50e03006a8da31d89e9102fe1699e3bfe5f1bf722fe35d528d0d4a88d8f4c10a3cded2842de9521509a3a721849e21de6b621ddaa0f36e295158802e78c63c58c2c6861f0eadb1cb42d1bb65aef9a47".hexToByteArray()),
//                RecipientAndPayload(payload = "c4c130db7adc35f23993bdd270e39d18cee6ba7ffaa84fa0fd690d94e5900146786840dc0bc536ad6afb6e4e3267fb045dd9c7e670f1c4c3d2ac1fcc71ad06b7b194de4031f4046744610aafa7b9da65d0b9e341b59053f646786179f84decb0c85768f2f2dc10ce3ceeee47871367fd91c4c13c8d1b035decfdabb37bf779b2f3ef25e57adfd40cc4c41df1eb3a8d939a378918f6bb4d4665f5c4c4378a86a56eecb0c85768f2f2dcecb0c85768f2f2dc82e715e7952a79c4c473417620f62a946729113879667b5fbfc8b008128df0af80fece91741fc5f6411145aab35ac9f6e0f8a937baed012d00de386aaf6c5f2a2c0495b6431770a6ddf7b32acd33d6db833a78c8ece4dfd771a8f18cc626a9b663aa880c4635230fa301e2c334f9d9118ed0589c0be4bf7bfabf24260e47dd246171b3ae57a819b89d351204b27b717644d516ebce6aa55975da4f9553c4c12dfefa01492e1661466a0475c758d76ce61ef3c2c0530e2633c50e03006a8da31d89e9102fe1699e3bfe5f1bf722fe35d528d0d4a88d8f4c10a3cded2842de9521509a3a721849e21de6b621ddaa0f36e295158802e78c63c58c2c6861f0eadb1cb42d1bb65aef9a47c4c130db7adc35f23993bdd270e39d18cee6ba7ffaa84fa0fd690d94e5900146786840dc0bc536ad6afb6e4e3267fb045dd9c7e670f1c4c3d2ac1fcc71ad06b7b194de4031f4046744610aafa7b9da65d0b9e341b59053f646786179f84decb0c85768f2f2dc10ce3ceeee47871367fd91c4c13c8d1b035decfdabb37bf779b2f3ef25e57adfd40cc4c41df1eb3a8d939a378918f6bb4d4665f5c4c4378a86a56eecb0c85768f2f2dcecb0c85768f2f2dc82e715e7952a79c4c473417620f62a946729113879667b5fbfc8b008128df0af80fece91741fc5f6411145aab35ac9f6e0f8a937baed012d00de386aaf6c5f2a2c0495b6431770a6ddf7b32acd33d6db833a78c8ece4dfd771a8f18cc626a9b663aa880c4635230fa301e2c334f9d9118ed0589c0be4bf7bfabf24260e47dd246171b3ae57a819b89d351204b27b717644d516ebce6aa55975da4f9553c4c12dfefa01492e1661466a0475c758d76ce61ef3c2c0530e2633c50e03006a8da31d89e9102fe1699e3bfe5f1bf722fe35d528d0d4a88d8f4c10a3cded2842de9521509a3a721849e21de6b621ddaa0f36e295158802e78c63c58c2c6861f0eadb1cb42d1bb65aef9a47c4c130db7adc35f23993bdd270e39d18cee6ba7ffaa84fa0fd690d94e5900146786840dc0bc536ad6afb6e4e3267fb045dd9c7e670f1c4c3d2ac1fcc71ad06b7b194de4031f4046744610aafa7b9da65d0b9e341b59053f646786179f84decb0c85768f2f2dc10ce3ceeee47871367fd91c4c13c8d1b035decfdabb37bf779b2f3ef25e57adfd40cc4c41df1eb3a".hexToByteArray()),
//                RecipientAndPayload(payload = "8d939a378918f6bb4d4665f5c4c4378a86a56eecb0c85768f2f2dcecb0c85768f2f2dc82e715e7952a79c4c473417620f62a946729113879667b5fbfc8b008128df0af80fece91741fc5f6411145aab35ac9f6e0f8a937baed012d00de386aaf6c5f2a2c0495b6431770a6ddf7b32acd33d6db833a78c8ece4dfd771a8f18cc626a9b663aa880c4635230fa301e2c334f9d9118ed0589c0be4bf7bfabf24260e47dd246171b3ae57a819b89d351204b27b717644d516ebce6aa55975da4f9553c4c12dfefa01492e1661466a0475c758d76ce61ef3c2".hexToByteArray())
//            ).toFlux(), MessageChannel(), ServerType.Tcp
//        )
//            .doOnNext {
//                println(it)
//            }
//            .subscribe()
//        Thread.sleep(10 * 1000)
//
//    }
//}
