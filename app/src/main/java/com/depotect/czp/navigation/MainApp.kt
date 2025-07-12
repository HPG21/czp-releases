package com.depotect.czp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import com.depotect.czp.data.*
import com.depotect.czp.models.SalaryCalculation
import com.depotect.czp.models.ThemeMode
import com.depotect.czp.ui.screens.*
import com.depotect.czp.ui.theme.CZPTheme
import com.depotect.czp.update.UpdateManager
import com.depotect.czp.update.UpdateState
import com.depotect.czp.update.UpdateDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Calculator) }
    var savedCalculations by remember { mutableStateOf<List<SalaryCalculation>>(emptyList()) }
    var currentTheme by remember { mutableStateOf(ThemeMode.AUTO) }
    var taxRate by remember { mutableStateOf("13") }
    val scope = rememberCoroutineScope()
    var isHistoryLoaded by remember { mutableStateOf(false) }
    var isSettingsLoaded by remember { mutableStateOf(false) }
    var showDisclaimer by remember { mutableStateOf(false) }
    var isFirstLaunchChecked by remember { mutableStateOf(false) }
    
    // Состояния для базового оклада
    var baseSalary by remember { mutableStateOf("") }
    var baseSalaryEnabled by remember { mutableStateOf(false) }
    var showBaseSalaryDialog by remember { mutableStateOf(false) }
    
    // Состояние для отображения кварталов в истории
    var showQuarters by remember { mutableStateOf(true) }
    
    // Состояния для настроек карточек аналитики
    var analyticsCardSettings by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isCardSettingsLoaded by remember { mutableStateOf(false) }
    
    // Состояние для системы обновлений
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.NoUpdate) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isManualUpdateCheck by remember { mutableStateOf(false) }
    val updateManager = remember { UpdateManager(context) }

    // Загрузка истории и настроек при первом запуске
    LaunchedEffect(Unit) {
        try {
            if (!isHistoryLoaded) {
                val loadedHistory = loadHistory(context)
                savedCalculations = loadedHistory
                isHistoryLoaded = true
                android.util.Log.d("CZP", "Initial history load completed: ${loadedHistory.size} items")
            }
            if (!isSettingsLoaded) {
                val loadedTaxRate = loadTaxRate(context)
                val loadedTheme = loadThemeMode(context)
                val loadedBaseSalary = loadBaseSalary(context)
                val loadedBaseSalaryEnabled = loadBaseSalaryEnabled(context)
                val loadedShowQuarters = loadShowQuarters(context)
                taxRate = loadedTaxRate
                currentTheme = loadedTheme
                baseSalary = loadedBaseSalary
                baseSalaryEnabled = loadedBaseSalaryEnabled
                showQuarters = loadedShowQuarters
                isSettingsLoaded = true
                android.util.Log.d("CZP", "Initial settings load completed: taxRate=$loadedTaxRate, theme=${loadedTheme.name}, baseSalary=$loadedBaseSalary, baseSalaryEnabled=$loadedBaseSalaryEnabled, showQuarters=$loadedShowQuarters")
            }
            if (!isCardSettingsLoaded) {
                val loadedCardSettings = loadAnalyticsCardSettings(context)
                analyticsCardSettings = loadedCardSettings
                isCardSettingsLoaded = true
                android.util.Log.d("CZP", "Initial card settings load completed: $loadedCardSettings")
            }
            if (!isFirstLaunchChecked) {
                val isFirst = isFirstLaunch(context)
                if (isFirst) {
                    showDisclaimer = true
                }
                isFirstLaunchChecked = true
                android.util.Log.d("CZP", "First launch check completed: $isFirst")
            }
            
            // Убираем автоматическую проверку обновлений при запуске
            // Теперь обновления проверяются только при ручном нажатии в настройках
            
        } catch (e: Exception) {
            android.util.Log.e("CZP", "Error during initial load", e)
        }
    }

    // Сохраняем историю при каждом изменении
    LaunchedEffect(savedCalculations) {
        if (isHistoryLoaded) {
            try {
                saveHistory(context, savedCalculations)
                android.util.Log.d("CZP", "History auto-saved: ${savedCalculations.size} items")
            } catch (e: Exception) {
                android.util.Log.e("CZP", "Error auto-saving history", e)
            }
        }
    }

    // Сохраняем настройки при изменении
    LaunchedEffect(taxRate, currentTheme, baseSalary, baseSalaryEnabled, showQuarters) {
        if (isSettingsLoaded) {
            try {
                saveTaxRate(context, taxRate)
                saveThemeMode(context, currentTheme)
                saveBaseSalary(context, baseSalary)
                saveBaseSalaryEnabled(context, baseSalaryEnabled)
                saveShowQuarters(context, showQuarters)
                android.util.Log.d("CZP", "Settings auto-saved: taxRate=$taxRate, theme=${currentTheme.name}, baseSalary=$baseSalary, baseSalaryEnabled=$baseSalaryEnabled, showQuarters=$showQuarters")
            } catch (e: Exception) {
                android.util.Log.e("CZP", "Error auto-saving settings", e)
            }
        }
    }

    // Сохраняем настройки карточек при изменении
    LaunchedEffect(analyticsCardSettings) {
        if (isCardSettingsLoaded && analyticsCardSettings.isNotEmpty()) {
            try {
                saveAnalyticsCardSettings(
                    context = context,
                    keyMetrics = analyticsCardSettings["key_metrics"] ?: true,
                    salaryTrend = analyticsCardSettings["salary_trend"] ?: true,
                    hoursDistribution = analyticsCardSettings["hours_distribution"] ?: true,
                    topMonths = analyticsCardSettings["top_months"] ?: true,
                    hourlyEfficiency = analyticsCardSettings["hourly_efficiency"] ?: true,
                    yearComparison = analyticsCardSettings["year_comparison"] ?: true,
                    salaryGrowth = analyticsCardSettings["salary_growth"] ?: true,
                    salaryRaise = analyticsCardSettings["salary_raise"] ?: true
                )
                android.util.Log.d("CZP", "Card settings auto-saved: $analyticsCardSettings")
            } catch (e: Exception) {
                android.util.Log.e("CZP", "Error auto-saving card settings", e)
            }
        }
    }

    // Определяем текущую тему на основе выбора пользователя
    val isDarkTheme = when (currentTheme) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }

    CZPTheme(darkTheme = isDarkTheme, dynamicColor = true) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(28.dp),
                                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            ),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    RoundedCornerShape(28.dp)
                                )
                                .padding(horizontal = 8.dp)
                        ) {
                            listOf(Screen.Calculator, Screen.History, Screen.Analytics, Screen.Settings).forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title) },
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    colors = if (!isDarkTheme) NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                    ) else NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = Color.White,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentScreen) {
                    Screen.Calculator -> CalculatorScreen(
                        taxRate = taxRate,
                        baseSalary = baseSalary,
                        baseSalaryEnabled = baseSalaryEnabled,
                        onSaveCalculation = { calculation ->
                            val newList = savedCalculations + calculation
                            savedCalculations = newList
                            // Принудительно сохраняем сразу после добавления
                            scope.launch {
                                try {
                                    saveHistory(context, newList)
                                    android.util.Log.d("CZP", "Calculation saved immediately: ${calculation.id}")
                                } catch (e: Exception) {
                                    android.util.Log.e("CZP", "Error saving calculation immediately", e)
                                }
                            }
                        },
                        history = savedCalculations,
                        modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                    )
                    Screen.History -> HistoryScreen(
                        calculations = savedCalculations,
                        showQuarters = showQuarters,
                        onDeleteCalculation = { calculation ->
                            val newList = savedCalculations.filter { it.id != calculation.id }
                            savedCalculations = newList
                            // Принудительно сохраняем сразу после удаления
                            scope.launch {
                                try {
                                    saveHistory(context, newList)
                                    android.util.Log.d("CZP", "Calculation deleted immediately: ${calculation.id}")
                                } catch (e: Exception) {
                                    android.util.Log.e("CZP", "Error saving after deletion", e)
                                }
                            }
                        },
                        onUpdateCalculation = { updatedCalculation ->
                            val newList = savedCalculations.map { 
                                if (it.id == updatedCalculation.id) updatedCalculation else it 
                            }
                            savedCalculations = newList
                            // Принудительно сохраняем сразу после обновления
                            scope.launch {
                                try {
                                    saveHistory(context, newList)
                                    android.util.Log.d("CZP", "Calculation updated immediately: ${updatedCalculation.id}")
                                } catch (e: Exception) {
                                    android.util.Log.e("CZP", "Error saving after update", e)
                                }
                            }
                        },
                        modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                    )
                    Screen.Analytics -> AnalyticsScreen(
                        calculations = savedCalculations,
                        cardSettings = analyticsCardSettings,
                        onCardSettingChange = { key, value ->
                            analyticsCardSettings = analyticsCardSettings.toMutableMap().apply { put(key, value) }
                        },
                        modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                    )
                    Screen.Settings -> SettingsScreen(
                        currentTheme = currentTheme,
                        onThemeChange = { currentTheme = it },
                        taxRate = taxRate,
                        onTaxRateChange = { taxRate = it },
                        baseSalary = baseSalary,
                        baseSalaryEnabled = baseSalaryEnabled,
                        onBaseSalaryChange = { baseSalary = it },
                        onBaseSalaryEnabledChange = { baseSalaryEnabled = it },
                        showQuarters = showQuarters,
                        onShowQuartersChange = { showQuarters = it },
                        onCheckUpdates = {
                            isManualUpdateCheck = true
                            scope.launch {
                                updateManager.checkForUpdates()
                            }
                        },
                        calculationsCount = savedCalculations.size,
                        onClearAllHistory = {
                            savedCalculations = emptyList()
                            // Принудительно сохраняем пустую историю
                            scope.launch {
                                try {
                                    saveHistory(context, emptyList())
                                    android.util.Log.d("CZP", "All history cleared")
                                } catch (e: Exception) {
                                    android.util.Log.e("CZP", "Error clearing history", e)
                                }
                            }
                        },
                        modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                    )
                }
            }
        }
    }
    
    // Показ дисклеймера при первом запуске
    if (showDisclaimer) {
        CZPTheme(darkTheme = isDarkTheme, dynamicColor = true) {
            com.depotect.czp.ui.components.DisclaimerDialog(
                onDismiss = {
                    showDisclaimer = false
                    scope.launch {
                        saveFirstLaunch(context)
                    }
                    // После дисклеймера показываем диалог настройки базового оклада
                    showBaseSalaryDialog = true
                }
            )
        }
    }
    
    // Диалог настройки базового оклада при первом запуске
    if (showBaseSalaryDialog) {
        CZPTheme(darkTheme = isDarkTheme, dynamicColor = true) {
            com.depotect.czp.ui.components.BaseSalarySetupDialog(
                baseSalary = baseSalary,
                baseSalaryEnabled = baseSalaryEnabled,
                onBaseSalaryChange = { baseSalary = it },
                onBaseSalaryEnabledChange = { baseSalaryEnabled = it },
                onDismiss = {
                    showBaseSalaryDialog = false
                }
            )
        }
    }
    
    // Принудительное сохранение при выходе из приложения
    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                try {
                    // Принудительно сохраняем все данные при выходе
                    if (isHistoryLoaded) {
                        saveHistory(context, savedCalculations)
                        android.util.Log.d("CZP", "Final history save on dispose: ${savedCalculations.size} items")
                    }
                    if (isSettingsLoaded) {
                        saveTaxRate(context, taxRate)
                        saveThemeMode(context, currentTheme)
                        saveBaseSalary(context, baseSalary)
                        saveBaseSalaryEnabled(context, baseSalaryEnabled)
                        saveShowQuarters(context, showQuarters)
                        android.util.Log.d("CZP", "Final settings save on dispose: taxRate=$taxRate, theme=${currentTheme.name}, baseSalary=$baseSalary, baseSalaryEnabled=$baseSalaryEnabled, showQuarters=$showQuarters")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CZP", "Error during final save on dispose", e)
                }
            }
        }
    }
    
    // Наблюдаем за состоянием обновлений
    LaunchedEffect(Unit) {
        updateManager.updateState.collect { state ->
            android.util.Log.d("CZP_UPDATE", "Update state changed: $state")
            updateState = state
            when (state) {
                is UpdateState.Checking -> {
                    android.util.Log.d("CZP_UPDATE", "Showing checking dialog")
                    showUpdateDialog = true
                }
                is UpdateState.UpdateAvailable -> {
                    android.util.Log.d("CZP_UPDATE", "Showing update dialog for available update")
                    showUpdateDialog = true
                }
                is UpdateState.Downloading -> {
                    android.util.Log.d("CZP_UPDATE", "Showing downloading dialog with progress: ${state.progress}%")
                    showUpdateDialog = true
                }
                is UpdateState.DownloadComplete -> {
                    android.util.Log.d("CZP_UPDATE", "Showing download complete dialog")
                    showUpdateDialog = true
                }
                is UpdateState.NoUpdateAvailable -> {
                    if (isManualUpdateCheck) {
                        android.util.Log.d("CZP_UPDATE", "Showing update dialog for no update (manual check)")
                        showUpdateDialog = true
                    } else {
                        android.util.Log.d("CZP_UPDATE", "Not showing dialog for no update (not manual check)")
                    }
                }
                is UpdateState.Error -> {
                    android.util.Log.d("CZP_UPDATE", "Showing error dialog: ${state.message}")
                    showUpdateDialog = true
                }
                else -> {
                    android.util.Log.d("CZP_UPDATE", "NoUpdate state - not showing dialog")
                }
            }
        }
    }
    
    // Показ диалога обновления
    if (showUpdateDialog) {
        android.util.Log.d("CZP_UPDATE", "Rendering UpdateDialog with state: $updateState")
        CZPTheme(darkTheme = isDarkTheme, dynamicColor = true) {
            UpdateDialog(
                updateState = updateState,
                onDismiss = { 
                    android.util.Log.d("CZP_UPDATE", "UpdateDialog dismissed")
                    showUpdateDialog = false
                    isManualUpdateCheck = false
                },
                onUpdate = {
                    android.util.Log.d("CZP_UPDATE", "UpdateDialog onUpdate called")
                    scope.launch {
                        updateManager.checkForUpdates()
                    }
                },
                onDownload = {
                    android.util.Log.d("CZP_UPDATE", "UpdateDialog onDownload called")
                    scope.launch {
                        val currentState = updateState
                        if (currentState is UpdateState.UpdateAvailable) {
                            updateManager.downloadUpdate(currentState.updateInfo)
                        }
                    }
                },
                onInstall = { file ->
                    android.util.Log.d("CZP_UPDATE", "UpdateDialog onInstall called with file: ${file.absolutePath}")
                    updateManager.installUpdate(file)
                    showUpdateDialog = false
                    isManualUpdateCheck = false
                }
            )
        }
    }
} 