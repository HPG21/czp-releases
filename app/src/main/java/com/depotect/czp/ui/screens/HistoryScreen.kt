package com.depotect.czp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.depotect.czp.models.SalaryCalculation
import com.depotect.czp.ui.components.HistoryDetailDialog
import com.depotect.czp.ui.theme.AccentOrange
import com.depotect.czp.ui.theme.SuccessGreen
import com.depotect.czp.utils.formatSmart
import java.util.*

@Composable
fun HistoryScreen(
    calculations: List<SalaryCalculation>,
    showQuarters: Boolean,
    onDeleteCalculation: (SalaryCalculation) -> Unit,
    onUpdateCalculation: (SalaryCalculation) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCalculation by remember { mutableStateOf<SalaryCalculation?>(null) }
    var editingCalculation by remember { mutableStateOf<SalaryCalculation?>(null) }
    val listState = rememberLazyListState()
    var isScrolled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Состояния фильтрации
    var searchQuery by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf("Все годы") }
    var showFilters by remember { mutableStateOf(false) }
    
    // Получаем доступные годы
    val availableYears = remember(calculations) {
        calculations.map { it.date.year }.distinct().sortedDescending()
    }
    
    // Фильтруем данные
    val filteredCalculations = remember(calculations, searchQuery, selectedYear) {
        var filtered = calculations
        
        // Фильтр по году
        if (selectedYear != "Все годы") {
            val year = selectedYear.toInt()
            filtered = filtered.filter { it.date.year == year }
        }
        
        // Поиск по месяцу (название месяца) или году
        if (searchQuery.isNotEmpty()) {
            val monthNames = listOf(
                "январь", "февраль", "март", "апрель", "май", "июнь",
                "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
            )
            val monthNamesShort = listOf(
                "янв", "фев", "мар", "апр", "май", "июн",
                "июл", "авг", "сен", "окт", "ноя", "дек"
            )
            val monthNumbers = listOf("1", "01", "2", "02", "3", "03", "4", "04", "5", "05", "6", "06",
                                     "7", "07", "8", "08", "9", "09", "10", "11", "12")
            
            filtered = filtered.filter { calculation ->
                val monthName = monthNames[calculation.date.monthValue - 1]
                val monthNameShort = monthNamesShort[calculation.date.monthValue - 1]
                val yearStr = calculation.date.year.toString()
                val monthStr = calculation.date.monthValue.toString()
                val monthStrPadded = String.format("%02d", calculation.date.monthValue)
                
                monthName.contains(searchQuery.lowercase()) ||
                monthNameShort.contains(searchQuery.lowercase()) ||
                yearStr.contains(searchQuery) ||
                monthStr.contains(searchQuery) ||
                monthStrPadded.contains(searchQuery) ||
                "$monthStr.$yearStr".contains(searchQuery) ||
                "$monthStrPadded.$yearStr".contains(searchQuery)
            }
        }
        
        filtered
    }
    
    // Отслеживаем изменения списка для сброса состояний
    LaunchedEffect(calculations.size) {
        // При изменении размера списка сбрасываем выбранный элемент
        if (selectedCalculation != null && !calculations.contains(selectedCalculation)) {
            selectedCalculation = null
        }
    }
    
    LaunchedEffect(listState.firstVisibleItemIndex) {
        isScrolled = listState.firstVisibleItemIndex > 0
    }
    
    // Показываем индикатор загрузки при большом количестве данных
    LaunchedEffect(calculations.size) {
        if (calculations.size > 50) {
            isLoading = true
            kotlinx.coroutines.delay(100) // Небольшая задержка для плавности
            isLoading = false
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Заголовок с фильтрами
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = !isScrolled,
                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically()
                ) {
                    Column {
                        Text(
                            text = "История расчетов",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (calculations.size > 20) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${calculations.size} расчетов за ${calculations.map { it.date.year }.distinct().size} лет",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
            
            // Панель фильтров
            AnimatedVisibility(
                visible = showFilters,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically()
            ) {
                HistoryFiltersPanel(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedYear = selectedYear,
                    onYearChange = { selectedYear = it },
                    availableYears = availableYears,
                    filteredCount = filteredCalculations.size,
                    totalCount = calculations.size
                )
            }
        }
        
        if (calculations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "История пуста",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Сохраните расчеты, чтобы они появились здесь",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Загрузка данных...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (filteredCalculations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Ничего не найдено",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Попробуйте изменить параметры поиска",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val grouped = filteredCalculations.groupBy { it.date.year }.toSortedMap(compareByDescending { it })
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                // Оптимизации для больших списков
                flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = listState)
            ) {
                grouped.forEach { (year, yearList) ->
                    item {
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp, start = 4.dp)
                        )
                        val yearSum = yearList.sumOf { it.totalSalary }
                        Text(
                            text = "Всего за $year: %,.0f ₽".format(yearSum),
                            style = MaterialTheme.typography.titleMedium,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                    }
                    
                    // Группируем по кварталам для лучшей навигации
                    val quarterlyGrouped = yearList.groupBy { (it.date.monthValue - 1) / 3 + 1 }
                    // Сортируем кварталы в обратном порядке (4, 3, 2, 1) - последний добавленный в начале
                    quarterlyGrouped.toSortedMap(compareByDescending { it }).forEach { (quarter, quarterList) ->
                        // Показываем заголовки кварталов только если включено в настройках
                        if (showQuarters) {
                            item {
                                val quarterNames = listOf("", "I квартал", "II квартал", "III квартал", "IV квартал")
                                val quarterSum = quarterList.sumOf { it.totalSalary }
                                Text(
                                    text = quarterNames[quarter],
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 8.dp)
                                )
                                Text(
                                    text = "Квартал $year: %,.0f ₽".format(quarterSum),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                                )
                            }
                        }
                        items(
                            items = quarterList.sortedByDescending { it.date.monthValue },
                            key = { it.id }
                        ) { calculation ->
                            Box(
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                SwipeableHistoryCard(
                                    calculation = calculation,
                                    showQuarters = showQuarters,
                                    onDelete = { onDeleteCalculation(calculation) },
                                    onClick = { selectedCalculation = calculation }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Диалог детального просмотра
    selectedCalculation?.let { calculation ->
        HistoryDetailDialog(
            calculation = calculation,
            onDismiss = { selectedCalculation = null },
            onDelete = {
                onDeleteCalculation(calculation)
                selectedCalculation = null
            },
            onEdit = {
                editingCalculation = calculation
                selectedCalculation = null
            }
        )
    }
    
    // Диалог редактирования
    editingCalculation?.let { calculation ->
        com.depotect.czp.ui.components.EditCalculationDialog(
            calculation = calculation,
            onDismiss = { editingCalculation = null },
            onSave = { updatedCalculation ->
                onUpdateCalculation(updatedCalculation)
                editingCalculation = null
            },
            onDelete = {
                onDeleteCalculation(calculation)
                editingCalculation = null
            }
        )
    }
}

@Composable
fun HistoryFiltersPanel(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
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
            
            // Поиск
            Text(
                text = "Поиск",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + 
                        androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.tween(300))
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Поиск: месяц, год, 01.2025...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = searchQuery.isNotEmpty(),
                            enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) + 
                                    androidx.compose.animation.scaleIn(animationSpec = androidx.compose.animation.core.tween(200)),
                            exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) + 
                                   androidx.compose.animation.scaleOut(animationSpec = androidx.compose.animation.core.tween(200))
                        ) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Очистить",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = androidx.compose.animation.core.tween(300)),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            // Подсказки для поиска
            if (searchQuery.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Примеры: январь, янв, 2025, 1.2025, 01.2025",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeableHistoryCard(
    calculation: SalaryCalculation,
    showQuarters: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberDismissState()
    var visible by remember { mutableStateOf(true) }
    var isDeleting by remember { mutableStateOf(false) }

    // Отслеживаем состояние свайпа
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == DismissValue.DismissedToStart && !isDeleting && visible) {
            isDeleting = true
            visible = false
            // Ждем анимацию исчезновения
            kotlinx.coroutines.delay(300)
            onDelete()
        }
    }

    // Сброс состояния при изменении calculation.id
    LaunchedEffect(calculation.id) {
        visible = true
        isDeleting = false
        dismissState.reset()
    }

    // Дополнительная проверка для предотвращения "залипания" состояния
    LaunchedEffect(Unit) {
        if (isDeleting) {
            kotlinx.coroutines.delay(500)
            if (isDeleting) {
                isDeleting = false
                visible = true
                dismissState.reset()
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300)),
    ) {
        SwipeToDismiss(
            state = dismissState,
            background = {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Удалить",
                                color = MaterialTheme.colorScheme.onError,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            dismissContent = {
                CompactHistoryCard(
                    calculation = calculation,
                    showQuarters = showQuarters,
                    onClick = onClick
                )
            },
            directions = setOf(DismissDirection.EndToStart)
        )
    }
}

// 2. CompactHistoryCard — карточка истории
@Composable
fun CompactHistoryCard(
    calculation: SalaryCalculation,
    showQuarters: Boolean,
    onClick: () -> Unit
) {
    val isLight = !isSystemInDarkTheme()
    val cardBg = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Дата и информация
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Вместо formatDate(calculation.date) делаю только месяц:
                    val monthNames = listOf(
                        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
                    )
                    val month = monthNames[calculation.date.monthValue - 1]
                    Text(
                        text = month,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatSmart(calculation.monthlyHours),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatSmart(calculation.nightHours),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = null,
                            tint = AccentOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatSmart(calculation.holidayHours),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = AccentOrange
                        )
                    }
                }
                
                // Показываем квартальные часы всегда
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Квартал: ${formatSmart(calculation.quarterlyHours)}ч",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Итоговая зарплата
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%,.0f ₽", calculation.totalSalary),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen
                )
                Text(
                    text = "Итого",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
            }
        }
    }
} 