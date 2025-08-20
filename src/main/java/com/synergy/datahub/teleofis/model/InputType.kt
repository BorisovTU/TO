package com.synergy.datahub.teleofis.model

import com.synergy.spring.starters.kotlin.EnumWithValue

//Тип входа
enum class InputType(override val value: Int, val description: String): EnumWithValue<Int> {
    COUNTING(0, "Счётный"),
    SIGNAL(1, "Сигнальный"),
    LEAK_SENSOR(2, "Датчик протечки"),
    TEMPERATURE_SENSOR(3, "Датчик температуры"),
    TAMPER_SENSOR(4, "Датчик вскрытия"),
    DISABLED(5, "Выключен"),
    DS18B20_SENSOR(6, "Датчик DS18B20"),
    HOUR_METER(7, "Счётчик моточасов"),
    HIGH_FREQ_COUNTER(8, "Высокочастотный счётчик"),
    CURRENT_LOOP(9, "Токовая петля"),
    GAS_METER(10, "Счётчик газа СГМ"),
    CO2_SENSOR(11, "Датчик газа CO2");

}