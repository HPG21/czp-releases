package com.depotect.czp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.depotect.czp.models.SalaryCalculation
import com.depotect.czp.utils.formatDate
import com.depotect.czp.utils.formatSmart
import java.time.LocalDate
import java.util.*
import androidx.compose.ui.graphics.Color

@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    history: List<SalaryCalculation>,
    onDateSelected: (LocalDate) -> Unit
) {
    var selectedYear by remember { mutableStateOf(LocalDate.now().year) }
    var selectedMonth by remember { mutableStateOf(LocalDate.now().monthValue) }
    var errorText by remember { mutableStateOf("") }
    
    // LazyListState для прокрутки к выбранному месяцу
    val monthsListState = rememberLazyListState()
    
    // Автоматическая прокрутка к выбранному месяцу при открытии диалога
    LaunchedEffect(Unit) {
        // Прокручиваем к выбранному месяцу (месяц - 1, так как индексы начинаются с 0)
        monthsListState.animateScrollToItem(selectedMonth - 1)
    }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Выберите месяц и год",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "За какой период сохранить расчет?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Выбор месяца
                Text(
                    text = "Месяц",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    state = monthsListState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val months = listOf(
                        "Янв", "Фев", "Мар", "Апр", "Май", "Июн",
                        "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"
                    )
                    items(12) { monthIndex ->
                        val month = monthIndex + 1
                        FilterChip(
                            onClick = { selectedMonth = month },
                            label = { Text(months[monthIndex]) },
                            selected = selectedMonth == month,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Выбор года
                Text(
                    text = "Год",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ограничиваю выбор годов: только 2025 и позже (например, 2025–2027)
                    val years = (2025..2030).toList()
                    items(years.size) { yearIndex ->
                        val year = years[yearIndex]
                        FilterChip(
                            onClick = { selectedYear = year },
                            label = { Text(year.toString()) },
                            selected = selectedYear == year,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                
                // Отображение ошибки, если расчет за выбранный месяц и год уже существует
                val alreadyExists = history.any { it.date.year == selectedYear && it.date.monthValue == selectedMonth }
                errorText = if (alreadyExists) "Расчет за этот месяц уже сохранён" else ""
                
                if (errorText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = errorText,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedDate = LocalDate.of(selectedYear, selectedMonth, 1)
                    // Проверяем, существует ли уже расчет за этот месяц и год
                    val alreadyExists = history.any { it.date.year == selectedDate.year && it.date.monthValue == selectedDate.monthValue }
                    if (!alreadyExists) {
                        onDateSelected(selectedDate)
                    }
                },
                enabled = errorText.isEmpty()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun DisclaimerDialog(
    onDismiss: () -> Unit
) {
    var timeLeft by remember { mutableStateOf(5) }
    
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            kotlinx.coroutines.delay(1000)
            timeLeft--
        }
    }
    
    AlertDialog(
        onDismissRequest = { /* Нельзя закрыть до истечения таймера */ },
        title = {
            Text(
                text = "Важная информация",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Добро пожаловать в CZp!",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = "Все расчеты в приложении предоставляются исключительно в ознакомительных целях и не являются официальной зарплатой.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Для получения точной информации о вашей зарплате обращайтесь к работодателю или в бухгалтерию.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (timeLeft > 0) {
                    Text(
                        text = "Кнопка станет активной через $timeLeft секунд",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                enabled = timeLeft == 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timeLeft == 0) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Понятно")
            }
        },
        modifier = Modifier.fillMaxWidth(0.95f)
    )
}

@Composable
fun BaseSalarySetupDialog(
    baseSalary: String,
    baseSalaryEnabled: Boolean,
    onBaseSalaryChange: (String) -> Unit,
    onBaseSalaryEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var tempBaseSalary by remember { mutableStateOf(baseSalary) }
    var tempBaseSalaryEnabled by remember { mutableStateOf(baseSalaryEnabled) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Настройка базового оклада",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Хотите установить базовый оклад для автоматического заполнения?",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Это упростит расчеты, если у вас стабильный оклад",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                
                // Переключатель
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Использовать базовый оклад",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Автоматически заполнять поле оклада",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = tempBaseSalaryEnabled,
                        onCheckedChange = { tempBaseSalaryEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Поле ввода
                AnimatedVisibility(
                    visible = tempBaseSalaryEnabled,
                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(
                        animationSpec = tween(300)
                    )
                ) {
                    InputFieldWithHint(
                        label = "Базовый оклад (руб.)",
                        value = tempBaseSalary,
                        onValueChange = { tempBaseSalary = com.depotect.czp.utils.filterNumericInput(it, 10000000.0) },
                        leadingIcon = Icons.Default.AttachMoney,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = "Например: 145000",
                        hint = "Будет автоматически подставляться в калькулятор",
                        isValid = tempBaseSalary.isEmpty() || tempBaseSalary.toDoubleOrNull() != null
                    )
                }
                
                if (!tempBaseSalaryEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Вы всегда сможете изменить это в настройках",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onBaseSalaryEnabledChange(tempBaseSalaryEnabled)
                    if (tempBaseSalaryEnabled) {
                        onBaseSalaryChange(tempBaseSalary)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (tempBaseSalaryEnabled) "Сохранить" else "Пропустить",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    // Если пользователь нажал "Отмена", то отключаем базовый оклад
                    onBaseSalaryEnabledChange(false)
                    onBaseSalaryChange("")
                    onDismiss()
                }
            ) {
                Text("Отмена")
            }
        },
        modifier = Modifier.fillMaxWidth(0.95f)
    )
}

@Composable
fun HistoryDetailDialog(
    calculation: SalaryCalculation,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(calculation.date),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // Основные параметры
                Text(
                    text = "Основные параметры",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ResultRow(
                    label = "Квартальные нормы часов",
                    value = calculation.quarterlyHours,
                    icon = Icons.Default.Schedule,
                    color = MaterialTheme.colorScheme.primary,
                    unit = "ч"
                )
                
                ResultRow(
                    label = "Оклад",
                    value = calculation.salary,
                    icon = Icons.Default.AttachMoney,
                    color = MaterialTheme.colorScheme.primary,
                    unit = "₽"
                )
                
                ResultRow(
                    label = "Ставка за час",
                    value = calculation.hourlyRate,
                    icon = Icons.Default.AccessTime,
                    color = MaterialTheme.colorScheme.primary,
                    unit = "₽/ч"
                )
                
                ResultRow(
                    label = "Чистая ставка за час",
                    value = calculation.netHourlyRate,
                    icon = Icons.Default.AccessTime,
                    color = com.depotect.czp.ui.theme.SuccessGreen,
                    unit = "₽/ч"
                )
                
                ResultRow(
                    label = "Ставка НДФЛ",
                    value = calculation.taxRate.toDouble(),
                    icon = Icons.Default.AccountBalance,
                    color = MaterialTheme.colorScheme.tertiary,
                    unit = "%"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline
                )
                
                // Расчеты по сменам
                Text(
                    text = "Расчеты по сменам",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary // как 'Основные параметры'
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ResultRow(
                    label = "Рабочие часы",
                    value = calculation.monthlyHours,
                    icon = Icons.Default.AccessTime,
                    color = MaterialTheme.colorScheme.primary,
                    unit = "ч"
                )
                
                ResultRow(
                    label = "Зарплата за рабочие часы",
                    value = calculation.regularSalary,
                    icon = Icons.Default.AccessTime,
                    color = MaterialTheme.colorScheme.primary,
                    unit = "₽"
                )
                
                ResultRow(
                    label = "Ночные часы",
                    value = calculation.nightHours,
                    icon = Icons.Default.DarkMode,
                    color = MaterialTheme.colorScheme.tertiary,
                    unit = "ч"
                )
                
                ResultRow(
                    label = "Зарплата за ночные часы (×0.4)",
                    value = calculation.nightSalary,
                    icon = Icons.Default.DarkMode,
                    color = MaterialTheme.colorScheme.tertiary,
                    unit = "₽"
                )
                
                ResultRow(
                    label = "Праздничные часы",
                    value = calculation.holidayHours,
                    icon = Icons.Default.Celebration,
                    color = com.depotect.czp.ui.theme.AccentOrange,
                    unit = "ч"
                )
                
                ResultRow(
                    label = "Зарплата за праздничные часы",
                    value = calculation.holidaySalary,
                    icon = Icons.Default.Celebration,
                    color = com.depotect.czp.ui.theme.AccentOrange,
                    unit = "₽"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline
                )
                
                // Вместо центрального блока с итогом делаю строку, как в CompactHistoryCard:
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
                            text = "Итого",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
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
                            text = String.format("%,.0f ₽", calculation.totalSalary),
                            style = MaterialTheme.typography.headlineLarge,
                            color = com.depotect.czp.ui.theme.SuccessGreen,
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
                            text = "до вычета НДФЛ: ${String.format("%,.0f ₽", calculation.grossSalary)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Закрыть",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        modifier = Modifier.fillMaxWidth(0.98f)
    )
}

@Composable
fun EditCalculationDialog(
    calculation: SalaryCalculation,
    onDismiss: () -> Unit,
    onSave: (SalaryCalculation) -> Unit,
    onDelete: () -> Unit
) {
    var quarterlyHours by remember { mutableStateOf(calculation.quarterlyHours.toString()) }
    var salary by remember { mutableStateOf(calculation.salary.toString()) }
    var monthlyHours by remember { mutableStateOf(calculation.monthlyHours.toString()) }
    var nightHours by remember { mutableStateOf(calculation.nightHours.toString()) }
    var holidayHours by remember { mutableStateOf(calculation.holidayHours.toString()) }
    var taxRate by remember { mutableStateOf(calculation.taxRate) }
    
    // Валидация часов
    val monthlyHoursValue = monthlyHours.toDoubleOrNull() ?: 0.0
    val nightHoursValue = nightHours.toDoubleOrNull() ?: 0.0
    val holidayHoursValue = holidayHours.toDoubleOrNull() ?: 0.0
    
    val isNightHoursValid = nightHours.isEmpty() || nightHoursValue <= monthlyHoursValue
    val isHolidayHoursValid = holidayHours.isEmpty() || holidayHoursValue <= monthlyHoursValue
    val isTotalHoursValid = (nightHoursValue + holidayHoursValue) <= monthlyHoursValue
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Редактировать расчет",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Измените параметры расчета",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Основные параметры
                Text(
                    text = "Основные параметры",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                com.depotect.czp.ui.components.InputFieldWithHint(
                    label = "Квартальные нормы часов",
                    value = quarterlyHours,
                    onValueChange = { quarterlyHours = com.depotect.czp.utils.filterNumericInput(it, 9999.0) },
                    leadingIcon = Icons.Default.Schedule,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    placeholder = "Например: 528",
                    hint = "Обычно 450-550 часов в квартал",
                    isValid = quarterlyHours.isEmpty() || quarterlyHours.toDoubleOrNull() != null
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                com.depotect.czp.ui.components.InputFieldWithHint(
                    label = "Оклад (руб.)",
                    value = salary,
                    onValueChange = { salary = com.depotect.czp.utils.filterNumericInput(it, 10000000.0) },
                    leadingIcon = Icons.Default.AttachMoney,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    placeholder = "Например: 145000",
                    hint = "Ваш месячный оклад до вычетов",
                    isValid = salary.isEmpty() || salary.toDoubleOrNull() != null
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Ставка НДФЛ
                Text(
                    text = "Ставка НДФЛ",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    com.depotect.czp.ui.components.TaxRateButton(
                        text = "13%",
                        selected = taxRate == "13",
                        onClick = { taxRate = "13" },
                        modifier = Modifier.weight(1f)
                    )
                    com.depotect.czp.ui.components.TaxRateButton(
                        text = "15%",
                        selected = taxRate == "15",
                        onClick = { taxRate = "15" },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline
                )
                
                // Часы по типам смен
                Text(
                    text = "Часы по типам смен",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                com.depotect.czp.ui.components.InputFieldWithHint(
                    label = "Рабочие часы",
                    value = monthlyHours,
                    onValueChange = { monthlyHours = com.depotect.czp.utils.filterNumericInput(it, 999.0) },
                    leadingIcon = Icons.Default.AccessTime,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    placeholder = "Например: 172",
                    hint = "Обычно 160-200 часов в месяц",
                    isValid = monthlyHours.isEmpty() || monthlyHours.toDoubleOrNull() != null
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                com.depotect.czp.ui.components.InputFieldWithHint(
                    label = "Ночные часы",
                    value = nightHours,
                    onValueChange = { 
                        val newValue = com.depotect.czp.utils.filterNumericInput(it, 999.0)
                        val newValueDouble = newValue.toDoubleOrNull() ?: 0.0
                        if (newValue.isEmpty() || newValueDouble <= monthlyHoursValue) {
                            nightHours = newValue
                        }
                    },
                    leadingIcon = Icons.Default.DarkMode,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    placeholder = "Например: 71",
                    hint = "Часы работы с 22:00 до 06:00 (коэффициент 0.4). Максимум: ${monthlyHoursValue.toInt()}ч",
                    isValid = isNightHoursValid
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                com.depotect.czp.ui.components.InputFieldWithHint(
                    label = "Праздничные часы",
                    value = holidayHours,
                    onValueChange = { 
                        val newValue = com.depotect.czp.utils.filterNumericInput(it, 999.0)
                        val newValueDouble = newValue.toDoubleOrNull() ?: 0.0
                        val totalSpecialHours = nightHoursValue + newValueDouble
                        if (newValue.isEmpty() || totalSpecialHours <= monthlyHoursValue) {
                            holidayHours = newValue
                        }
                    },
                    leadingIcon = Icons.Default.Celebration,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    placeholder = "Например: 11",
                    hint = "Часы работы в праздничные дни. Максимум: ${(monthlyHoursValue - nightHoursValue).toInt()}ч",
                    isValid = isHolidayHoursValid && isTotalHoursValid
                )
                
                // Предупреждение о превышении часов
                if (!isTotalHoursValid && monthlyHours.isNotEmpty() && (nightHours.isNotEmpty() || holidayHours.isNotEmpty())) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Сумма ночных и праздничных часов не может превышать рабочие часы",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val hours = quarterlyHours.toDoubleOrNull() ?: 0.0
                        val salaryAmount = salary.toDoubleOrNull() ?: 0.0
                        val monthlyHoursAmount = monthlyHours.toDoubleOrNull() ?: 0.0
                        val nightHoursAmount = nightHours.toDoubleOrNull() ?: 0.0
                        val holidayHoursAmount = holidayHours.toDoubleOrNull() ?: 0.0
                        
                        val hourlyRate = if (hours > 0) (salaryAmount * 3) / hours else 0.0
                        val tax = (hourlyRate * taxRate.toDouble() / 100.0)
                        val netHourlyRate = hourlyRate - tax
                        
                        // Расчеты по типам смен (в "грязных" деньгах)
                        val regularSalaryGross = monthlyHoursAmount * hourlyRate
                        val nightSalaryGross = nightHoursAmount * hourlyRate * 0.4
                        val holidaySalaryGross = holidayHoursAmount * hourlyRate
                        val grossSalary = regularSalaryGross + nightSalaryGross + holidaySalaryGross
                        
                        // Расчеты в "чистых" деньгах
                        val regularSalary = monthlyHoursAmount * netHourlyRate
                        val nightSalary = nightHoursAmount * netHourlyRate * 0.4
                        val holidaySalary = holidayHoursAmount * netHourlyRate
                        val totalSalary = regularSalary + nightSalary + holidaySalary
                        
                        // Расчет уплаченных налогов (разность между "грязной" и "чистой" зарплатой)
                        val totalTaxes = grossSalary - totalSalary
                        
                        val updatedCalculation = calculation.copy(
                            quarterlyHours = hours,
                            salary = salaryAmount,
                            monthlyHours = monthlyHoursAmount,
                            nightHours = nightHoursAmount,
                            holidayHours = holidayHoursAmount,
                            taxRate = taxRate,
                            hourlyRate = hourlyRate,
                            netHourlyRate = netHourlyRate,
                            regularSalary = regularSalary,
                            nightSalary = nightSalary,
                            holidaySalary = holidaySalary,
                            totalSalary = totalSalary,
                            grossSalary = grossSalary,
                            totalTaxes = totalTaxes
                        )
                        
                        onSave(updatedCalculation)
                    },
                    enabled = quarterlyHours.isNotEmpty() && salary.isNotEmpty() &&
                            monthlyHours.isNotEmpty() && nightHours.isNotEmpty() && holidayHours.isNotEmpty() &&
                            isTotalHoursValid
                ) {
                    Text("Сохранить")
                }
            }
        },
        modifier = Modifier.fillMaxWidth(0.98f)
    )
} 

@Composable
fun AnalyticsCardsSettingsDialog(
    cardSettings: Map<String, Boolean>,
    onCardSettingChange: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Настройка карточек",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Выберите карточки, которые хотите видеть в аналитике",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val cards = listOf(
                    CardSetting(
                        key = "key_metrics",
                        title = "Ключевые показатели",
                        description = "Средняя, максимум, минимум и общая зарплата",
                        icon = Icons.Default.TrendingUp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    CardSetting(
                        key = "salary_trend",
                        title = "Динамика зарплаты",
                        description = "График изменения зарплаты по месяцам",
                        icon = Icons.Default.ShowChart,
                        color = MaterialTheme.colorScheme.secondary
                    ),
                    CardSetting(
                        key = "hours_distribution",
                        title = "Распределение часов",
                        description = "Дневные, ночные и праздничные часы",
                        icon = Icons.Default.PieChart,
                        color = MaterialTheme.colorScheme.tertiary
                    ),
                    CardSetting(
                        key = "top_months",
                        title = "Топ месяцев",
                        description = "Лучший и худший месяц по зарплате",
                        icon = Icons.Default.Star,
                        color = com.depotect.czp.ui.theme.AccentOrange
                    ),
                    CardSetting(
                        key = "hourly_efficiency",
                        title = "Эффективность часов",
                        description = "Ставка за час и стабильность",
                        icon = Icons.Default.Speed,
                        color = com.depotect.czp.ui.theme.SuccessGreen
                    ),
                    CardSetting(
                        key = "year_comparison",
                        title = "Сравнение по годам",
                        description = "Анализ доходов по годам",
                        icon = Icons.Default.Compare,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    CardSetting(
                        key = "salary_growth",
                        title = "Уплаченные налоги",
                        description = "Общая сумма уплаченных налогов за период",
                        icon = Icons.Default.AccountBalance,
                        color = MaterialTheme.colorScheme.secondary
                    ),
                    CardSetting(
                        key = "salary_raise",
                        title = "Динамика оклада",
                        description = "Прирост оклада по годам и общий прирост",
                        icon = Icons.Default.TrendingUp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                
                cards.forEach { card ->
                    CardSettingItem(
                        card = card,
                        isEnabled = cardSettings[card.key] ?: true,
                        onToggle = { onCardSettingChange(card.key, it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Готово",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        modifier = Modifier.fillMaxWidth(1f)
    )
}

data class CardSetting(
    val key: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@Composable
fun CardSettingItem(
    card: CardSetting,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(300)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) 
                card.color.copy(alpha = 0.08f) 
            else 
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(if (isEnabled) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isEnabled) card.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = card.icon,
                    contentDescription = null,
                    tint = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Текст
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = card.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Переключатель
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = card.color,
                    checkedTrackColor = card.color.copy(alpha = 0.3f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            )
        }
    }
} 

@Composable
fun ClearAllHistoryDialog(
    calculationsCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Удалить всю историю?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Вы собираетесь удалить все сохраненные расчеты ($calculationsCount шт.).",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = "Это действие:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Column(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Нельзя отменить",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Удалит все расчеты навсегда",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Очистит всю аналитику",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Настройки приложения (тема, оклад, НДФЛ) останутся без изменений.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Удалить всё",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Отмена",
                    fontWeight = FontWeight.Medium
                )
            }
        },
        modifier = Modifier.fillMaxWidth(0.95f)
    )
} 