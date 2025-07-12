package com.depotect.czp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.depotect.czp.ui.theme.AccentOrange
import com.depotect.czp.ui.theme.SuccessGreen
import com.depotect.czp.utils.formatSmart

// 1. ResultsCard — итоговая карточка
@Composable
fun ResultsCard(
    quarterlyHours: Double,
    salary: Double,
    hourlyRate: Double,
    netHourlyRate: Double,
    monthlyHours: Double,
    nightHours: Double,
    holidayHours: Double,
    regularSalary: Double,
    nightSalary: Double,
    holidaySalary: Double,
    totalSalary: Double,
    grossSalary: Double,
    taxRate: String
) {
    val cardBg = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp), // внешний отступ для видимой тени
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Результаты расчета",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Основные параметры",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            ResultRow(
                label = "Квартальные нормы часов",
                value = quarterlyHours,
                icon = Icons.Default.Schedule,
                color = MaterialTheme.colorScheme.primary,
                unit = "ч",
                textColor = textColor
            )
            ResultRow(
                label = "Оклад",
                value = salary,
                icon = Icons.Default.AttachMoney,
                color = MaterialTheme.colorScheme.primary,
                unit = "₽",
                textColor = textColor
            )
            ResultRow(
                label = "Ставка за час",
                value = hourlyRate,
                icon = Icons.Default.AccessTime,
                color = MaterialTheme.colorScheme.primary,
                unit = "₽/ч",
                textColor = textColor
            )
            ResultRow(
                label = "Чистая ставка за час",
                value = netHourlyRate,
                icon = Icons.Default.AccessTime,
                color = SuccessGreen,
                unit = "₽/ч",
                textColor = textColor
            )
            ResultRow(
                label = "Ставка НДФЛ",
                value = taxRate.toDouble(),
                icon = Icons.Default.AccountBalance,
                color = MaterialTheme.colorScheme.tertiary,
                unit = "%",
                textColor = textColor
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Расчеты по сменам",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            ResultRow(
                label = "Рабочие часы",
                value = monthlyHours,
                icon = Icons.Default.AccessTime,
                color = MaterialTheme.colorScheme.primary,
                unit = "ч",
                textColor = textColor
            )
            ResultRow(
                label = "Зарплата за рабочие часы",
                value = regularSalary,
                icon = Icons.Default.AccessTime,
                color = MaterialTheme.colorScheme.primary,
                unit = "₽",
                textColor = textColor
            )
            ResultRow(
                label = "Ночные часы",
                value = nightHours,
                icon = Icons.Default.DarkMode,
                color = MaterialTheme.colorScheme.tertiary,
                unit = "ч",
                textColor = textColor
            )
            ResultRow(
                label = "Зарплата за ночные часы (×0.4)",
                value = nightSalary,
                icon = Icons.Default.DarkMode,
                color = MaterialTheme.colorScheme.tertiary,
                unit = "₽",
                textColor = textColor
            )
            ResultRow(
                label = "Праздничные часы",
                value = holidayHours,
                icon = Icons.Default.Celebration,
                color = AccentOrange,
                unit = "ч",
                textColor = textColor
            )
            ResultRow(
                label = "Зарплата за праздничные часы",
                value = holidaySalary,
                icon = Icons.Default.Celebration,
                color = AccentOrange,
                unit = "₽",
                textColor = textColor
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline
            )
            // Итоговая секция с "грязной" зарплатой
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Общая зарплата за месяц",
                        style = MaterialTheme.typography.titleMedium,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = String.format("%,.0f ₽", totalSalary),
                        style = MaterialTheme.typography.headlineLarge,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                // "Грязная" зарплата мелким шрифтом
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "до вычета НДФЛ: ${String.format("%,.0f ₽", grossSalary)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }
        }
    }
}

// 2. ResultRow — добавляю параметр textColor
@Composable
fun ResultRow(
    label: String,
    value: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    unit: String = "",
    isTotal: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.weight(1f),
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = if (unit.contains("/ч")) {
                "%.2f %s".format(value, unit)
            } else if (unit == "₽") {
                "%.0f %s".format(value, unit)
            } else if (unit == "ч") {
                "${formatSmart(value)} %s".format(unit)
            } else {
                formatSmart(value)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Medium
        )
    }
} 