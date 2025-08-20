package com.synergy.datahub.teleofis.model

/**
 * Коды ошибок при неудачном соединении с сервером
 */
enum class ConnectionErrorCode(val code: Int, val description: String) {
    NO_ERRORS(0, "Сеанс прошел без ошибок"),
    INVALID_PIN(1, "Неверный пин-код"),
    SIM_CARD_MISSING(2, "Не вставлена SIM-карта"),
    NETWORK_REGISTRATION_FAILED(3, "Не удалось зарегистрироваться в сети"),
    GPRS_CONNECTION_FAILED(4, "Не удалось подключиться по GPRS"),
    SERVER_CONNECTION_MISSING(5, "Нет соединения с сервером");
}
