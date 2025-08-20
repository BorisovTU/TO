package com.synergy.datahub.teleofis.model

import com.synergy.datahub.utils.bitMask
import com.synergy.spring.starters.kotlin.EnumWithValue

enum class ErrorCodes(override val value: String, val code: UInt, val description: String) : EnumWithValue<String> {
    OK("OK", 0u, "Нет ошибки"),
    CONSUMPTION_LOW("CONSUMPTION_LOW", bitMask(1).toUInt(), "Расход теплоносителя по подающему трубопроводу меньше минимально допустимого"),
    CONSUMPTION_HIGH("CONSUMPTION_HIGH", bitMask(2).toUInt(), "Расход теплоносителя по подающему трубопроводу больше максимально допустимого"),
    LINE_SHORT_CIRCUIT("LINE_SHORT_CIRCUIT", bitMask(3).toUInt(), "Замыкание линии связи с ПУ ХВС/ГВС"),
    LINE_BREAK("LINE_BREAK", bitMask(4).toUInt(), "Обрыв линии связи с ПУ ХВС/ГВС"),
    REGISTER_OPENED("REGISTER_OPENED", bitMask(5).toUInt(), "Вскрытие регистратора импульсов"),
    MAGNETIC_FIELD_SENSOR("MAGNETIC_FIELD_SENSOR", bitMask(6).toUInt(), "Сработал датчик магнитного поля регистратора импульсов"),
    MANUAL_CONNECTION_ACTIVATION("MANUAL_CONNECTION_ACTIVATION", bitMask(7).toUInt(), "Ручная активация связи на регистраторе импульсов"),
    RESTART("RESTART", bitMask(8).toUInt(), "Рестарт устройства"),
    NO_POWER("NO_POWER", bitMask(9).toUInt(), "Отсутствие электропитания регистратора импульсов"),
    DATE_TIME_CORRECTION("DATE_TIME_CORRECTION", bitMask(10).toUInt(), "Коррекция даты/времени регистратора импульсов"),
    DATA_FAILURE("DATA_FAILURE", bitMask(11).toUInt(), "Сбой учета данных от ПУ ХВС/ГВС с импульсными входами"),
    BATTERY_LOW("BATTERY_LOW", bitMask(12).toUInt(), "Батарея регистратора импульсов разряжена"),
    INPUT_PULSE_RATE_EXCEEDED("INPUT_PULSE_RATE_EXCEEDED", bitMask(13).toUInt(), "Превышение частоты следования импульсов на входе");

    fun toInt(): Int = code.toInt()

    companion object {
        fun getByCode(code: UByte): EventType? = EventType.values().find { it.code == code }

        fun getMaskOf(errors: List<ErrorCodes>): UInt =
            errors.map(ErrorCodes::code).reduce { acc, i -> acc or i }
    }
}