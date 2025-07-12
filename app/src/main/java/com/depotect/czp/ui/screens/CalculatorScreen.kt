package com.depotect.czp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.depotect.czp.data.loadHistory
import com.depotect.czp.models.SalaryCalculation
import com.depotect.czp.ui.components.*
import com.depotect.czp.utils.filterNumericInput
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    taxRate: String,
    baseSalary: String,
    baseSalaryEnabled: Boolean,
    onSaveCalculation: (SalaryCalculation) -> Unit,
    history: List<SalaryCalculation>,
    modifier: Modifier = Modifier
) {
    var quarterlyHours by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf(if (baseSalaryEnabled && baseSalary.isNotEmpty()) baseSalary else "") }
    var monthlyHours by remember { mutableStateOf("") }
    var nightHours by remember { mutableStateOf("") }
    var holidayHours by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showNormaDialog by remember { mutableStateOf(false) }
    val snackbarHostState = SnackbarHostState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var savedCalculations by remember { mutableStateOf<List<SalaryCalculation>>(emptyList()) }
    val focusManager = LocalFocusManager.current
    val focusQuarterly = remember { FocusRequester() }
    val focusSalary = remember { FocusRequester() }
    val focusMonthly = remember { FocusRequester() }
    val focusNight = remember { FocusRequester() }
    val focusHoliday = remember { FocusRequester() }
    
    // Валидация часов
    val monthlyHoursValue = monthlyHours.toDoubleOrNull() ?: 0.0
    val nightHoursValue = nightHours.toDoubleOrNull() ?: 0.0
    val holidayHoursValue = holidayHours.toDoubleOrNull() ?: 0.0
    
    val isNightHoursValid = nightHours.isEmpty() || nightHoursValue <= monthlyHoursValue
    val isHolidayHoursValid = holidayHours.isEmpty() || holidayHoursValue <= monthlyHoursValue
    val isTotalHoursValid = (nightHoursValue + holidayHoursValue) <= monthlyHoursValue
    
    // Загрузка истории при первом запуске
    LaunchedEffect(Unit) {
        savedCalculations = loadHistory(context)
    }
    
    // Обновляем поле оклада при изменении базового оклада
    LaunchedEffect(baseSalary, baseSalaryEnabled) {
        if (baseSalaryEnabled && baseSalary.isNotEmpty()) {
            salary = baseSalary
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.padding(bottom = 56.dp) // Добавлен отступ снизу для снекбара
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
                .padding(bottom = 56.dp)
                .padding(innerPadding)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Заголовок
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(1000)) + expandVertically(),
                modifier = Modifier.animateContentSize()
            ) {
                Text(
                    text = "Калькулятор зарплаты",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Объединённая карточка: Основные параметры + Часы по типам смен
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 300)) + slideInHorizontally(
                    animationSpec = tween(800, delayMillis = 300),
                    initialOffsetX = { -it }
                )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Секция "Основные параметры" (без заголовка)
                        InputFieldWithHint(
                            label = "Квартальные нормы часов",
                            value = quarterlyHours,
                            onValueChange = { quarterlyHours = filterNumericInput(it, 9999.0) },
                            leadingIcon = Icons.Default.Schedule,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            placeholder = "Например: 528",
                            hint = "Обычно 450-550 часов в квартал",
                            isValid = quarterlyHours.isEmpty() || quarterlyHours.toDoubleOrNull() != null,
                            trailingIcon = {
                                IconButton(onClick = { showNormaDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Нормы часов за 2025 год",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier.focusRequester(focusQuarterly)
                        )
                        if (showNormaDialog) {
                            AlertDialog(
                                onDismissRequest = { showNormaDialog = false },
                                title = { Text("Нормы часов за 2025 год", fontWeight = FontWeight.Bold) },
                                text = {
                                    Text(
                                        "1 квартал — 463 ч\n2 квартал — 470 ч\n3 квартал — 528 ч\n4 квартал — 511 ч",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { showNormaDialog = false },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text(
                                            text = "Ок",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        InputFieldWithHint(
                            label = "Оклад (руб.)",
                            value = salary,
                            onValueChange = { salary = filterNumericInput(it, 10000000.0) },
                            leadingIcon = Icons.Default.AttachMoney,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            placeholder = "Например: 145000",
                            hint = if (baseSalaryEnabled && baseSalary.isNotEmpty()) "Используется базовый оклад из настроек" else "Ваш месячный оклад до вычетов",
                            isValid = salary.isEmpty() || salary.toDoubleOrNull() != null,
                            trailingIcon = if (baseSalaryEnabled && baseSalary.isNotEmpty()) {
                                {
                                    IconButton(onClick = { /* Можно добавить подсказку */ }) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Используется базовый оклад",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else null,
                            modifier = Modifier.focusRequester(focusSalary)
                        )
                        
                        // Разделитель
                        Spacer(modifier = Modifier.height(16.dp))
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(500)) + expandHorizontally(
                                animationSpec = tween(500, easing = FastOutSlowInEasing)
                            )
                        ) {
                            Divider(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                thickness = 1.dp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Секция "Часы по типам смен" (без заголовка)
                        InputFieldWithHint(
                            label = "Рабочие часы",
                            value = monthlyHours,
                            onValueChange = { monthlyHours = filterNumericInput(it, 999.0) },
                            leadingIcon = Icons.Default.AccessTime,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            placeholder = "Например: 172",
                            hint = "Обычно 160-200 часов в месяц",
                            isValid = monthlyHours.isEmpty() || monthlyHours.toDoubleOrNull() != null,
                            modifier = Modifier.focusRequester(focusMonthly)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        InputFieldWithHint(
                            label = "Ночные часы",
                            value = nightHours,
                            onValueChange = { 
                                val newValue = filterNumericInput(it, 999.0)
                                val newValueDouble = newValue.toDoubleOrNull() ?: 0.0
                                if (newValue.isEmpty() || newValueDouble <= monthlyHoursValue) {
                                    nightHours = newValue
                                }
                            },
                            leadingIcon = Icons.Default.DarkMode,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            placeholder = "Например: 71",
                            hint = "Часы работы с 22:00 до 06:00 (коэффициент 0.4). Максимум: ${monthlyHoursValue.toInt()}ч",
                            isValid = isNightHoursValid,
                            modifier = Modifier.focusRequester(focusNight)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        InputFieldWithHint(
                            label = "Праздничные часы",
                            value = holidayHours,
                            onValueChange = { 
                                val newValue = filterNumericInput(it, 999.0)
                                val newValueDouble = newValue.toDoubleOrNull() ?: 0.0
                                val totalSpecialHours = nightHoursValue + newValueDouble
                                if (newValue.isEmpty() || totalSpecialHours <= monthlyHoursValue) {
                                    holidayHours = newValue
                                }
                            },
                            leadingIcon = Icons.Default.Celebration,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            placeholder = "Например: 11",
                            hint = "Часы работы в праздничные дни. Максимум: ${(monthlyHoursValue - nightHoursValue).toInt()}ч",
                            isValid = isHolidayHoursValid && isTotalHoursValid,
                            modifier = Modifier.focusRequester(focusHoliday)
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
                }
            }

            // Результаты расчета с анимацией
            AnimatedVisibility(
                visible = quarterlyHours.isNotEmpty() && salary.isNotEmpty() &&
                        monthlyHours.isNotEmpty() && nightHours.isNotEmpty() && holidayHours.isNotEmpty() &&
                        isTotalHoursValid,
                enter = fadeIn(animationSpec = tween(1000)) + expandVertically(
                    animationSpec = tween(1000, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(
                    animationSpec = tween(300)
                ),
                modifier = Modifier.animateContentSize()
            ) {
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

                Column {
                    ResultsCard(
                        quarterlyHours = hours,
                        salary = salaryAmount,
                        hourlyRate = hourlyRate,
                        netHourlyRate = netHourlyRate,
                        monthlyHours = monthlyHoursAmount,
                        nightHours = nightHoursAmount,
                        holidayHours = holidayHoursAmount,
                        regularSalary = regularSalary,
                        nightSalary = nightSalary,
                        holidaySalary = holidaySalary,
                        totalSalary = totalSalary,
                        grossSalary = grossSalary,
                        taxRate = taxRate
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Кнопка сохранения
                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp) // убираем тень
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Сохранить расчет",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Диалог выбора даты
            if (showSaveDialog) {
                DatePickerDialog(
                    onDismissRequest = { showSaveDialog = false },
                    history = history,
                    onDateSelected = { date ->
                        selectedDate = date
                        showSaveDialog = false
                        
                        // Сохраняем расчет
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
                        
                        val calculation = SalaryCalculation(
                            date = date,
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
                        
                        // Сохраняем расчет (проверка на существование уже в DatePickerDialog)
                        onSaveCalculation(calculation)
                    }
                )
            }
        }
    }
} 