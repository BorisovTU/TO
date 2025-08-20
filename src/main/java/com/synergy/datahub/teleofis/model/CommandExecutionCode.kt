package com.synergy.datahub.teleofis.model

/**
 * Коды выполнения команд настройки устройства
 */
enum class CommandExecutionCode(val code: Int, val description: String) {
    COMMAND_SUCCESSFUL(0, "Команда выполнена"),
    COMMAND_NOT_SUPPORTED(1, "Команда не поддерживается"),
    INVALID_DATA_FORMAT(2, "Неверный формат данных"),
    ERROR(3, "Ошибка"),
    COMMAND_BLOCKED(4, "Команда заблокирована");

    fun isError() = this != COMMAND_SUCCESSFUL

    companion object {
        fun fromCode(code: Int): CommandExecutionCode {
            return values().find { it.code == code } ?: throw IllegalStateException("Неподдерживаемый код: $code ответа на команду")
        }
    }
}