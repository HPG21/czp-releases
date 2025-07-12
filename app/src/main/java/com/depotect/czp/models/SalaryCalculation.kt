package com.depotect.czp.models

import java.time.LocalDate
import java.util.*

// Модель данных для сохранения расчетов
data class SalaryCalculation(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val quarterlyHours: Double,
    val salary: Double,
    val monthlyHours: Double,
    val nightHours: Double,
    val holidayHours: Double,
    val taxRate: String,
    val hourlyRate: Double,
    val netHourlyRate: Double,
    val regularSalary: Double,
    val nightSalary: Double,
    val holidaySalary: Double,
    val totalSalary: Double,
    val grossSalary: Double = 0.0, // "Грязная" зарплата (до вычета налогов)
    val totalTaxes: Double = 0.0, // Сумма уплаченных налогов
    val timestamp: Long = System.currentTimeMillis()
) 