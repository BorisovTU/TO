package com.synergy.datahub.teleofis.model

import com.synergy.datahub.teleofis.model.ParameterValueType.ArrayOfNumber
import com.synergy.datahub.teleofis.model.ParameterValueType.Number
import com.synergy.datahub.utils.bitMask

/**
 * Параметры устройства
 * bitMask битовая маска параметра, через функцию bitMask можно указать номер бита (как в документации)
 */
enum class ParameterType(val num: UByte, val size: Int, val type: ParameterValueType, val description: String, val bitMask: ULong? = null) {
    CURRENT_TIME(1u, 4, Number, "Текущее время", bitMask(1)),
    VALUES(2u, 16, ArrayOfNumber, "Массив текущих значений счетчиков", bitMask(2)),
    MODEM_IMEI(11u, 16, ParameterValueType.String, "IMEI модема"),
    RESTART(17u, 4, Number, "Рестарт устройства, число указывает через какое время – произвести рестарт"),

    //первые 4 байта — время начала, остальные 4 байта – время окончания запроса
    ARCHIVE(53u, 8, Number, "Запрос архива"),
    ARCHIVE_END(54u, 1, Number, "Команда прекращения передачи архива"),

    TIME_ZONE(48u, 1, Number, "Часовой пояс", bitMask(28)),
    CLOSE_SESSION(55u, 1, Number, "Команда окончания запросов – с сервера"),
    READ_MANY_SETTINGS(50u, 1, Number, "Команда получения сразу нескольких настроек устройства"),

    GET_ARCHIVE_EVENTS(53u, 8, Number, "Запрос архива"),
    CHANGE_SPORADIC_MODE(69u, 1, Number, "Отключение передачи спорадических сообщений (1 - выключить)"),
    STOP_GETTING_ARCHIVE_EVENTS(54u, 1, Number, "Команда прекращения передачи архива"),

    FIRMWARE_VERSION(13u, 16, ParameterValueType.String, "Версия программного обеспечения", bitMask(4)),
    VALUE_1(18u, 4, Number, "Значение счетчика 1", bitMask(5)),
    VALUE_2(19u, 4, Number, "Значение счетчика 2", bitMask(6)),
    VALUE_3(20u, 4, Number, "Значение счетчика 3", bitMask(7)),
    VALUE_4(21u, 4, Number, "Значение счетчика 4", bitMask(8)),
    INPUT_STATE_1(30u, 1, Number, "Состояние входа 1", bitMask(17)),
    INPUT_STATE_2(31u, 1, Number, "Состояние входа 2", bitMask(18)),
    INPUT_STATE_3(32u, 1, Number, "Состояние входа 3", bitMask(19)),
    INPUT_STATE_4(33u, 1, Number, "Состояние входа 4", bitMask(20)),
    BATTERY_VOLTAGE(39u, 4, Number, "Напряжение на батарейке в милливольтах", bitMask(24)),
    INPUT_STATE_6(92u, 1, Number, "Состояние входа 6", bitMask(41)),
    INPUT_TYPE_1(93u, 1, Number, "Тип входа 1", bitMask(42)),
    INPUT_TYPE_2(94u, 1, Number, "Тип входа 2", bitMask(43)),
    INPUT_TYPE_3(95u, 1, Number, "Тип входа 3", bitMask(44)),
    INPUT_TYPE_4(96u, 1, Number, "Тип входа 4", bitMask(45)),
    INPUT_TYPE_6(98u, 1, Number, "Тип входа 6", bitMask(47)),
    MODEL(130u, 128, ParameterValueType.String, "Команда чтения имени устройства"),
    BATTERY_VOLTAGE_BEFORE_CONNECTION(79u, 4, Number, "Напряжение на батарейке при выключенном GSM модеме, но включенной нагрузкой 33 Ом, перед сеансом связи (милливольт)", bitMask(34)),
    GSM_SIGNAL_LEVEL(36u, 1, Number, "Уровень сигнала GSM (команда только для чтения)", bitMask(21));

    companion object {
        fun fromNumber(num: UByte): ParameterType? =
            ParameterType.values().find { dt -> dt.num == num }
    }
}

enum class ParameterValueType {
    Number, String, ArrayOfNumber
}
