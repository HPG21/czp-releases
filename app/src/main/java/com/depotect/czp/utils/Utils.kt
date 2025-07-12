package com.depotect.czp.utils

import java.time.LocalDate
import java.util.*

// Функция для фильтрации ввода - запрещает отрицательные значения и пробелы
fun filterNumericInput(input: String, maxValue: Double = Double.MAX_VALUE): String {
    val filtered = input.filter { it.isDigit() || it == '.' }
    val doubleValue = filtered.toDoubleOrNull() ?: 0.0
    return if (doubleValue <= maxValue) filtered else filtered.dropLast(1)
}

fun formatSmart(value: Double): String =
    if (value % 1.0 == 0.0) "%.0f".format(value) else "%.1f".format(value)

// Функция для правильного форматирования даты
fun formatDate(date: LocalDate): String {
    val monthNames = mapOf(
        1 to "Январь", 2 to "Февраль", 3 to "Март", 4 to "Апрель",
        5 to "Май", 6 to "Июнь", 7 to "Июль", 8 to "Август",
        9 to "Сентябрь", 10 to "Октябрь", 11 to "Ноябрь", 12 to "Декабрь"
    )
    val month = monthNames[date.monthValue] ?: ""
    return "$month ${date.year}"
} 