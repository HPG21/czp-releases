package com.depotect.czp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.depotect.czp.models.SalaryCalculation
import com.depotect.czp.ui.components.AnalyticsCardsSettingsDialog
import com.depotect.czp.ui.theme.AccentOrange
import com.depotect.czp.ui.theme.SuccessGreen
import com.depotect.czp.utils.formatSmart
import java.time.LocalDate
import java.util.*

@Composable
fun AnalyticsScreen(
    calculations: List<SalaryCalculation>,
    cardSettings: Map<String, Boolean>,
    onCardSettingChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // Состояния фильтрации
    var selectedPeriod by remember { mutableStateOf("Все время") }
    var selectedYear by remember { mutableStateOf("Все годы") }
    var showFilters by remember { mutableStateOf(false) }
    var showCardSettings by remember { mutableStateOf(false) }
    
    // Получаем доступные годы
    val availableYears = remember(calculations) {
        calculations.map { it.date.year }.distinct().sortedDescending()
    }
    
    // Фильтруем данные
    val filteredCalculations = remember(calculations, selectedPeriod, selectedYear) {
        var filtered = calculations
        
        // Фильтр по году
        if (selectedYear != "Все годы") {
            val year = selectedYear.toInt()
            filtered = filtered.filter { it.date.year == year }
        }
        
        // Фильтр по периоду
        val currentDate = LocalDate.now()
        filtered = when (selectedPeriod) {
            "Последние 3 месяца" -> {
                val threeMonthsAgo = currentDate.minusMonths(3)
                filtered.filter { it.date.isAfter(threeMonthsAgo) || it.date.isEqual(threeMonthsAgo) }
            }
            "Последние 6 месяцев" -> {
                val sixMonthsAgo = currentDate.minusMonths(6)
                filtered.filter { it.date.isAfter(sixMonthsAgo) || it.date.isEqual(sixMonthsAgo) }
            }
            "Последний год" -> {
                val oneYearAgo = currentDate.minusYears(1)
                filtered.filter { it.date.isAfter(oneYearAgo) || it.date.isEqual(oneYearAgo) }
            }
            "Последние 2 года" -> {
                val twoYearsAgo = currentDate.minusYears(2)
                filtered.filter { it.date.isAfter(twoYearsAgo) || it.date.isEqual(twoYearsAgo) }
            }
            "Последние 3 года" -> {
                val threeYearsAgo = currentDate.minusYears(3)
                filtered.filter { it.date.isAfter(threeYearsAgo) || it.date.isEqual(threeYearsAgo) }
            }
            else -> filtered // "Все время"
        }
        
        filtered
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Заголовок с фильтрами и настройками
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(1000)) + expandVertically(),
            modifier = Modifier.animateContentSize()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Аналитика зарплаты",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row {
                        IconButton(
                            onClick = { showCardSettings = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = "Настройка карточек",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { showFilters = !showFilters }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Фильтры",
                                tint = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Панель фильтров
                AnimatedVisibility(
                    visible = showFilters,
                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically()
                ) {
                    FiltersPanel(
                        selectedPeriod = selectedPeriod,
                        onPeriodChange = { selectedPeriod = it },
                        selectedYear = selectedYear,
                        onYearChange = { selectedYear = it },
                        availableYears = availableYears,
                        filteredCount = filteredCalculations.size,
                        totalCount = calculations.size
                    )
                }
            }
        }
        
        if (calculations.isEmpty()) {
            EmptyAnalyticsCard()
        } else {
            // Показываем карточки только если они включены в настройках
            if (cardSettings["key_metrics"] == true) {
                KeyMetricsCard(filteredCalculations)
            }
            
            if (cardSettings["salary_trend"] == true) {
                SalaryTrendAndChartCard(filteredCalculations)
            }
            
            if (cardSettings["hours_distribution"] == true) {
                HoursDistributionAndStatsCard(filteredCalculations)
            }
            
            if (cardSettings["top_months"] == true) {
                TopMonthsCard(filteredCalculations)
            }
            
            if (cardSettings["hourly_efficiency"] == true) {
                HourlyEfficiencyCard(filteredCalculations)
            }
            
            if (cardSettings["year_comparison"] == true) {
                YearComparisonCard(filteredCalculations)
            }
            
            if (cardSettings["salary_growth"] == true) {
                SalaryGrowthAndTaxesCard(filteredCalculations)
            }
            
            if (cardSettings["salary_raise"] == true) {
                SalaryRaiseCard(filteredCalculations)
            }
            
            // Если все карточки отключены, показываем сообщение
            if (cardSettings.values.all { !it }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Все карточки отключены",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Включите карточки в настройках, чтобы увидеть аналитику",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showCardSettings = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Настроить карточки")
                        }
                    }
                }
            }
        }
    }
    
    // Диалог настройки карточек
    if (showCardSettings) {
        AnalyticsCardsSettingsDialog(
            cardSettings = cardSettings,
            onCardSettingChange = onCardSettingChange,
            onDismiss = { showCardSettings = false }
        )
    }
}

@Composable
fun EmptyAnalyticsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Нет данных для анализа",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Сохраните несколько расчетов, чтобы увидеть аналитику",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun KeyMetricsCard(calculations: List<SalaryCalculation>) {
    val totalSalary = calculations.sumOf { it.totalSalary }
    val avgSalary = if (calculations.isNotEmpty()) totalSalary / calculations.size else 0.0
    val maxSalary = calculations.maxOfOrNull { it.totalSalary } ?: 0.0
    val minSalary = calculations.minOfOrNull { it.totalSalary } ?: 0.0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Ключевые показатели",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricItem(
                    title = "Средняя",
                    value = avgSalary,
                    icon = Icons.Default.Analytics,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    title = "Максимум",
                    value = maxSalary,
                    icon = Icons.Default.TrendingUp,
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricItem(
                    title = "Минимум",
                    value = minSalary,
                    icon = Icons.Default.TrendingDown,
                    color = AccentOrange,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    title = "Всего",
                    value = totalSalary,
                    icon = Icons.Default.AccountBalance,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricItem(
    title: String,
    value: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format(Locale.getDefault(), "%,.0f ₽", value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun SalaryTrendAndChartCard(calculations: List<SalaryCalculation>) {
    val sortedCalculations = calculations.sortedBy { it.date }
    val hasGrowth = if (sortedCalculations.size >= 2) {
        sortedCalculations.last().totalSalary > sortedCalculations.first().totalSalary
    } else false
    
    // Ограничиваем количество точек на графике для читаемости
    val displayCalculations = if (sortedCalculations.size > 12) {
        // Показываем только последние 12 месяцев или каждый 3-й месяц для больших периодов
        val step = if (sortedCalculations.size > 24) sortedCalculations.size / 12 else 1
        sortedCalculations.filterIndexed { index, _ -> index % step == 0 || index >= sortedCalculations.size - 12 }
    } else {
        sortedCalculations
    }
    
    // Состояние для раскрытия/скрытия списка
    var isExpanded by remember { mutableStateOf(false) }
    val showExpandButton = displayCalculations.size > 6
    val visibleCalculations = if (!isExpanded && showExpandButton) displayCalculations.take(6) else displayCalculations
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Динамика зарплаты",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Функциональная стрелочка справа
                if (showExpandButton) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Свернуть" else "Показать больше",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (sortedCalculations.size >= 2) {
                val firstSalary = sortedCalculations.first().totalSalary
                val lastSalary = sortedCalculations.last().totalSalary
                val change = lastSalary - firstSalary
                val changePercent = if (firstSalary > 0) (change / firstSalary) * 100 else 0.0
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Изменение за период",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%+.0f ₽", change),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (hasGrowth) SuccessGreen else AccentOrange
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%+.1f%%", changePercent),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasGrowth) SuccessGreen else AccentOrange
                        )
                    }
                    Icon(
                        imageVector = if (hasGrowth) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (hasGrowth) SuccessGreen else AccentOrange,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // График динамики зарплаты по месяцам
                val maxSalary = displayCalculations.maxOf { it.totalSalary }
                val monthNames = listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")
                
                // Показываем предупреждение если данные сжаты
                if (sortedCalculations.size > 12 && displayCalculations.size < sortedCalculations.size) {
                    Text(
                        text = "Показаны ${displayCalculations.size} из ${sortedCalculations.size} месяцев для читаемости",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                // Список месяцев с прогресс-барами с анимацией
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
                ) {
                    Column {
                        visibleCalculations.forEach { calculation ->
                            val monthName = monthNames[calculation.date.monthValue - 1]
                            val percentage = (calculation.totalSalary / maxSalary).toFloat()
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$monthName ${calculation.date.year}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "%,.0f ₽", calculation.totalSalary),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = percentage,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Нужно минимум 2 расчета для анализа динамики",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HoursDistributionAndStatsCard(calculations: List<SalaryCalculation>) {
    val totalDayHours = calculations.sumOf { it.monthlyHours - it.nightHours - it.holidayHours }
    val totalNightHours = calculations.sumOf { it.nightHours }
    val totalHolidayHours = calculations.sumOf { it.holidayHours }
    // Общие часы = месячные часы (включают дневные, ночные и праздничные)
    val totalHours = calculations.sumOf { it.monthlyHours }
    
    // Статистика часов
    val avgHoursPerMonth = if (calculations.isNotEmpty()) totalHours / calculations.size else 0.0
    // Максимум за месяц = месячные часы
    val maxHours = calculations.maxOfOrNull { it.monthlyHours } ?: 0.0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Распределение часов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (totalHours > 0) {
                val dayPercent = (totalDayHours / totalHours) * 100
                val nightPercent = (totalNightHours / totalHours) * 100
                val holidayPercent = (totalHolidayHours / totalHours) * 100
                
                ShiftDistributionBar(
                    label = "Дневные часы",
                    value = totalDayHours,
                    percentage = dayPercent,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                ShiftDistributionBar(
                    label = "Ночные (доплата)",
                    value = totalNightHours,
                    percentage = nightPercent,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(12.dp))
                ShiftDistributionBar(
                    label = "Праздничные",
                    value = totalHolidayHours,
                    percentage = holidayPercent,
                    color = AccentOrange
                )
            } else {
                Text(
                    text = "Нет данных о часах",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Всего часов",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatSmart(totalHours)}ч",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Среднее в месяц",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatSmart(avgHoursPerMonth)}ч",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Максимум за месяц: ${formatSmart(maxHours)}ч",
                style = MaterialTheme.typography.bodyMedium,
                color = AccentOrange,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ShiftDistributionBar(
    label: String,
    value: Double,
    percentage: Double,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${formatSmart(value)}ч (${String.format("%.1f", percentage)}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = (percentage / 100).toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun YearComparisonCard(calculations: List<SalaryCalculation>) {
    val groupedByYear = calculations.groupBy { it.date.year }
    val sortedYears = groupedByYear.keys.sorted()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Compare,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Сравнение по годам",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (sortedYears.size >= 2) {
                sortedYears.forEach { year ->
                    val yearCalculations = groupedByYear[year] ?: emptyList()
                    val yearTotal = yearCalculations.sumOf { it.totalSalary }
                    val yearAvg = if (yearCalculations.isNotEmpty()) yearTotal / yearCalculations.size else 0.0
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%,.0f ₽", yearTotal),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = SuccessGreen
                            )
                            Text(
                                text = "Среднее: ${String.format(Locale.getDefault(), "%,.0f ₽", yearAvg)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (year != sortedYears.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }
            } else {
                Text(
                    text = "Нужны данные за несколько лет для сравнения",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TopMonthsCard(calculations: List<SalaryCalculation>) {
    val sortedBySalary = calculations.sortedByDescending { it.totalSalary }
    val bestMonth = sortedBySalary.firstOrNull()
    val worstMonth = sortedBySalary.lastOrNull()
    val monthNames = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Топ месяцев",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (bestMonth != null && worstMonth != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Лучший месяц
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SuccessGreen.copy(alpha = 0.1f)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Лучший",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = monthNames[bestMonth.date.monthValue - 1],
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%,.0f ₽", bestMonth.totalSalary),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    
                    // Худший месяц
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AccentOrange.copy(alpha = 0.1f)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = AccentOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Худший",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = monthNames[worstMonth.date.monthValue - 1],
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%,.0f ₽", worstMonth.totalSalary),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AccentOrange,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                val difference = bestMonth.totalSalary - worstMonth.totalSalary
                Text(
                    text = "Разница: ${String.format(Locale.getDefault(), "%,.0f ₽", difference)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "Нужно минимум 2 расчета для сравнения",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HourlyEfficiencyCard(calculations: List<SalaryCalculation>) {
    val avgHourlyRate = if (calculations.isNotEmpty()) {
        calculations.sumOf { it.netHourlyRate } / calculations.size
    } else 0.0
    
    val bestHourlyRate = calculations.maxOfOrNull { it.netHourlyRate } ?: 0.0
    val worstHourlyRate = calculations.minOfOrNull { it.netHourlyRate } ?: 0.0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Эффективность часов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Средняя ставка",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.0f ₽/ч", avgHourlyRate),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Максимум",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.0f ₽/ч", bestHourlyRate),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Минимум: ${String.format(Locale.getDefault(), "%.0f ₽/ч", worstHourlyRate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = AccentOrange,
                fontWeight = FontWeight.Medium
            )
            
            if (calculations.size >= 2) {
                Spacer(modifier = Modifier.height(8.dp))
                val stability = if (avgHourlyRate > 0) {
                    val variance = calculations.map { (it.netHourlyRate - avgHourlyRate) * (it.netHourlyRate - avgHourlyRate) }.average()
                    val stdDev = kotlin.math.sqrt(variance)
                    val stabilityPercent = ((1 - stdDev / avgHourlyRate) * 100).coerceIn(0.0, 100.0)
                    stabilityPercent
                } else 0.0
                
                Text(
                    text = "Стабильность ставки: ${String.format("%.0f", stability)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 

@Composable
fun SalaryGrowthAndTaxesCard(calculations: List<SalaryCalculation>) {
    val totalTaxes = calculations.sumOf { it.totalTaxes }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Уплаченные налоги",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Налоги
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Общая сумма налогов",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%,.0f ₽", totalTaxes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                    Text(
                        text = "За весь период",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            if (calculations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                val avgTaxesPerMonth = totalTaxes / calculations.size
                Text(
                    text = "Средние налоги в месяц: ${String.format(Locale.getDefault(), "%,.0f ₽", avgTaxesPerMonth)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SalaryRaiseCard(calculations: List<SalaryCalculation>) {
    if (calculations.isEmpty()) return
    val sorted = calculations.sortedBy { it.date }
    val groupedByYear = sorted.groupBy { it.date.year }
    val firstSalary = sorted.first().salary
    val lastSalary = sorted.last().salary
    val totalRaise = lastSalary - firstSalary
    val totalRaisePercent = if (firstSalary > 0) (totalRaise / firstSalary) * 100 else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Динамика оклада",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Общий прирост с начала работы:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = String.format(Locale.getDefault(), "%+.0f ₽ (%+.1f%%)", totalRaise, totalRaisePercent),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (totalRaise >= 0) SuccessGreen else AccentOrange
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Годовой прирост:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            groupedByYear.entries.sortedBy { it.key }.forEach { (year, list) ->
                val yearFirst = list.first().salary
                val yearLast = list.last().salary
                val yearRaise = yearLast - yearFirst
                val yearRaisePercent = if (yearFirst > 0) (yearRaise / yearFirst) * 100 else 0.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%+.0f ₽ (%+.1f%%)", yearRaise, yearRaisePercent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (yearRaise >= 0) SuccessGreen else AccentOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FiltersPanel(
    selectedPeriod: String,
    onPeriodChange: (String) -> Unit,
    selectedYear: String,
    onYearChange: (String) -> Unit,
    availableYears: List<Int>,
    filteredCount: Int,
    totalCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Фильтры",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$filteredCount из $totalCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Фильтр по периоду
            Text(
                text = "Период",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                val periods = listOf("Все время", "Последние 3 месяца", "Последние 6 месяцев", "Последний год", "Последние 2 года", "Последние 3 года")
                items(periods.size) { index ->
                    val period = periods[index]
                    FilterChip(
                        onClick = { onPeriodChange(period) },
                        label = { Text(period) },
                        selected = selectedPeriod == period,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Фильтр по году
            Text(
                text = "Год",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        onClick = { onYearChange("Все годы") },
                        label = { Text("Все годы") },
                        selected = selectedYear == "Все годы",
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                items(availableYears.size) { index ->
                    val year = availableYears[index]
                    FilterChip(
                        onClick = { onYearChange(year.toString()) },
                        label = { Text(year.toString()) },
                        selected = selectedYear == year.toString(),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }
} 