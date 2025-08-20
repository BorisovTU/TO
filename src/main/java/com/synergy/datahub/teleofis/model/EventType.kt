package com.synergy.datahub.teleofis.model

import com.synergy.datahub.utils.bitMask

enum class EventType(val code: UByte, val description: String, val classificationMask: ULong = 0uL) {
    TIME_INTERVAL(1u, "Событие по времени (прошел временной интервал)"),
    ADC_EVENT(2u, "Событие по АЦП (разрыв или КЗ шлейфа)"),
    DEVICE_RESTART(3u, "Рестарт устройства", bitMask(8)),
    DRY_CONTACT_TRIGGERED(4u, "Сработал сухой контакт"),
    BUTTON_PRESSED(8u, "Нажата кнопка", bitMask(7)),
    CONTACTS_LEARNING_START_OR_END(10u, "Событие начала (конца) обучения контактов"),
    DRY_CONTACT_LEARNED(11u, "Обучен один из сухих контактов"),
    GPRS_CONNECTION_FAIL(12u, "Не удалось установить сеанс связи по GPRS"),
    POWER_LOST(13u, "Пропадание внешнего питания"),
    POWER_ON(14u, "Появление внешнего питания"),
    INPUT_PULSE_RATE_EXCEEDED(15u, "Превышение частоты следования импульсов на входе"),
    ARCHIVE_TRANSFER_END(16u, "Конец передачи архива (в журнале не сохраняется)"),
    SIM_CONNECTION_LOST_PERIOD_EXCEEDED(17u, "Превышен период отсутствия связи на SIM карте"),
    INPUT_VALUE_MONITORING_EVENT(19u, "Событие по контролю значений на входах"),
    INPUT_VALUE_CHANGE_EXCEEDED(20u, "Событие по превышению изменения контролируемых значений на входах", bitMask(2)),
    BATTERY_DEACTIVATED(22u, "Батарея депассивирована"),
    BATTERY_DISCHARGED(23u, "Батарея разряжена", bitMask(12)),
    HIGH_INPUT_CURRENT(24u, "Высокое значение тока на входе (для типа входа токовая петля)");

    companion object {
        fun getByCode(code: UByte): EventType? = values().find { it.code == code }
    }
}