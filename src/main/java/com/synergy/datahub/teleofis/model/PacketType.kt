package com.synergy.datahub.teleofis.model

/**
 * Идентификатор данных
 */
enum class PacketType(val packetId: UByte, val description: String) {
    DEVICE_SETUP_DATA(1u, "Команда настройки устройства"),
    DEVICE_SETUP_RESPONSE(2u, "Ответ на команду настройки устройства"),
    COUNTER_DATA(3u, "Данные со счетчиков"),
    COUNTER_DATA_ACKNOWLEDGEMENT(4u, "Подтверждение о приеме данных со счетчиков"),
    TRANSPARENT_CHANNEL_DATA(5u, "Данные прозрачного канала"),
    READ_SETTINGS_DATA(6u, "Команда чтения настроек"),
    READ_SETTINGS_RESPONSE(7u, "Ответ на команду чтения настроек"),
    SERVER_AUTHORIZATION_WHEN_SPORADIC_DATA_DISABLED(8u, "Авторизация на сервере при отключенной передаче спорадических данных"),
    TELEMETRY_INFORMATION_TRANSFER(9u, "Передача телеметрической информации");
    companion object {
        fun ofId(packetId: UByte) =
            requireNotNull(PacketType.values().find { dt -> dt.packetId == packetId }) { "Неподдерживаемый идентификатор данных. Id: $packetId" }
    }
}