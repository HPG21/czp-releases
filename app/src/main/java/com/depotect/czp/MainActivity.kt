package com.depotect.czp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.depotect.czp.ui.theme.CZPTheme
import com.depotect.czp.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.material3.NavigationBar // Убедитесь, что это Material 3 NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme // Для цвета
import androidx.compose.ui.res.painterResource
import java.util.Locale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.DisposableEffect
import com.depotect.czp.update.UpdateManager
import com.depotect.czp.update.UpdateState
import com.depotect.czp.update.UpdateDialog

private val Context.dataStore by preferencesDataStore(name = "salary_history")
private val HISTORY_KEY = stringPreferencesKey("history_json")
private val TAX_RATE_KEY = stringPreferencesKey("tax_rate")
private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
private val FIRST_LAUNCH_KEY = stringPreferencesKey("first_launch")

// Кастомный адаптер для LocalDate
class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    override fun serialize(src: LocalDate?, typeOfSrc: java.lang.reflect.Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.toString())
    }

    override fun deserialize(json: JsonElement?, typeOfT: java.lang.reflect.Type?, context: JsonDeserializationContext?): LocalDate? {
        return json?.asString?.let { LocalDate.parse(it) }
    }
}

// Создаем Gson с кастомным адаптером
private val gson = GsonBuilder()
    .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
    .create()

suspend fun saveHistory(context: Context, history: List<SalaryCalculation>) {
    try {
        android.util.Log.d("CZP", "Starting to save history: ${history.size} items")
        val json = gson.toJson(history)
        android.util.Log.d("CZP", "JSON generated, length: ${json.length}")
        
        // Основной способ через DataStore
        context.dataStore.edit { prefs ->
            prefs[HISTORY_KEY] = json
            android.util.Log.d("CZP", "Data written to DataStore preferences")
        }
        
        // Принудительная синхронизация для release-сборки
        val verification = context.dataStore.data.first()
        val savedJson = verification[HISTORY_KEY]
        android.util.Log.d("CZP", "Verification: saved JSON length: ${savedJson?.length ?: 0}")
        
        if (savedJson == json) {
            android.util.Log.d("CZP", "History saved successfully and verified: ${history.size} items")
        } else {
            android.util.Log.w("CZP", "History save verification failed - data mismatch")
            // Попытка повторного сохранения
            context.dataStore.edit { prefs ->
                prefs[HISTORY_KEY] = json
            }
            android.util.Log.d("CZP", "History saved on retry: ${history.size} items")
        }
        
        // Дополнительный fallback через SharedPreferences
        try {
            val sharedPrefs = context.getSharedPreferences("czp_fallback", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("history_json", json).apply()
            android.util.Log.d("CZP", "History also saved to SharedPreferences fallback")
        } catch (fallbackException: Exception) {
            android.util.Log.e("CZP", "Error saving to SharedPreferences fallback", fallbackException)
        }
        
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error saving history", e)
        // Попытка повторного сохранения
        try {
            val json = gson.toJson(history)
            context.dataStore.edit { prefs ->
                prefs[HISTORY_KEY] = json
            }
            android.util.Log.d("CZP", "History saved on retry: ${history.size} items")
        } catch (retryException: Exception) {
            android.util.Log.e("CZP", "Error saving history on retry", retryException)
        }
    }
}

suspend fun loadHistory(context: Context): List<SalaryCalculation> {
    return try {
        // Сначала пробуем DataStore
        val prefs = context.dataStore.data.first()
        val json = prefs[HISTORY_KEY] ?: return emptyList()
        
        if (json.isEmpty()) {
            android.util.Log.d("CZP", "History is empty in DataStore")
            // Пробуем fallback
            val sharedPrefs = context.getSharedPreferences("czp_fallback", Context.MODE_PRIVATE)
            val fallbackJson = sharedPrefs.getString("history_json", null)
            if (fallbackJson != null && fallbackJson.isNotEmpty()) {
                android.util.Log.d("CZP", "Loading from SharedPreferences fallback")
                val type = object : TypeToken<List<SalaryCalculation>>() {}.type
                val result = gson.fromJson<List<SalaryCalculation>>(fallbackJson, type) ?: emptyList()
                android.util.Log.d("CZP", "History loaded from fallback: ${result.size} items")
                return result
            }
            return emptyList()
        }
        
        val type = object : TypeToken<List<SalaryCalculation>>() {}.type
        val result = gson.fromJson<List<SalaryCalculation>>(json, type) ?: emptyList()
        android.util.Log.d("CZP", "History loaded successfully from DataStore: ${result.size} items")
        result
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error loading history from DataStore", e)
        // Попытка загрузки из fallback
        try {
            val sharedPrefs = context.getSharedPreferences("czp_fallback", Context.MODE_PRIVATE)
            val fallbackJson = sharedPrefs.getString("history_json", null)
            if (fallbackJson != null && fallbackJson.isNotEmpty()) {
                android.util.Log.d("CZP", "Loading from SharedPreferences fallback after error")
                val type = object : TypeToken<List<SalaryCalculation>>() {}.type
                val result = gson.fromJson<List<SalaryCalculation>>(fallbackJson, type) ?: emptyList()
                android.util.Log.d("CZP", "History loaded from fallback after error: ${result.size} items")
                return result
            }
        } catch (fallbackException: Exception) {
            android.util.Log.e("CZP", "Error loading from fallback", fallbackException)
        }
        emptyList()
    }
}

suspend fun saveTaxRate(context: Context, taxRate: String) {
    try {
        context.dataStore.edit { prefs ->
            prefs[TAX_RATE_KEY] = taxRate
        }
        android.util.Log.d("CZP", "Tax rate saved: $taxRate")
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error saving tax rate", e)
    }
}

suspend fun loadTaxRate(context: Context): String {
    return try {
        val prefs = context.dataStore.data.first()
        val result = prefs[TAX_RATE_KEY] ?: "13"
        android.util.Log.d("CZP", "Tax rate loaded: $result")
        result
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error loading tax rate", e)
        "13"
    }
}

suspend fun saveThemeMode(context: Context, themeMode: ThemeMode) {
    try {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = themeMode.name
        }
        android.util.Log.d("CZP", "Theme mode saved: ${themeMode.name}")
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error saving theme mode", e)
    }
}

suspend fun loadThemeMode(context: Context): ThemeMode {
    return try {
        val prefs = context.dataStore.data.first()
        val themeName = prefs[THEME_MODE_KEY] ?: ThemeMode.AUTO.name
        val result = try {
            ThemeMode.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            ThemeMode.AUTO
        }
        android.util.Log.d("CZP", "Theme mode loaded: ${result.name}")
        result
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error loading theme mode", e)
        ThemeMode.AUTO
    }
}

suspend fun saveFirstLaunch(context: Context) {
    try {
        context.dataStore.edit { prefs ->
            prefs[FIRST_LAUNCH_KEY] = "false"
        }
        android.util.Log.d("CZP", "First launch flag saved")
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error saving first launch flag", e)
    }
}

suspend fun isFirstLaunch(context: Context): Boolean {
    return try {
        val prefs = context.dataStore.data.first()
        val result = prefs[FIRST_LAUNCH_KEY] != "false"
        android.util.Log.d("CZP", "First launch check: $result")
        result
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error checking first launch", e)
        true
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Переключаемся с splash темы на основную тему
        setTheme(R.style.Theme_CZP)
        
        setContent {
            MainApp()
        }
    }
}

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
    val timestamp: Long = System.currentTimeMillis()
)

// Навигация
sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Calculator : Screen("calculator", "Калькулятор", Icons.Default.Calculate)
    object History : Screen("history", "История", Icons.Default.History)
    object Settings : Screen("settings", "Настройки", Icons.Default.Settings)
}

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
    
    // Состояние для системы обновлений
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.NoUpdate) }
    var showUpdateDialog by remember { mutableStateOf(false) }
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
                taxRate = loadedTaxRate
                currentTheme = loadedTheme
                isSettingsLoaded = true
                android.util.Log.d("CZP", "Initial settings load completed: taxRate=$loadedTaxRate, theme=${loadedTheme.name}")
            }
            if (!isFirstLaunchChecked) {
                val isFirst = isFirstLaunch(context)
                if (isFirst) {
                    showDisclaimer = true
                }
                isFirstLaunchChecked = true
                android.util.Log.d("CZP", "First launch check completed: $isFirst")
            }
            
            // Проверяем обновления при запуске
            try {
                val updateInfo = updateManager.checkForUpdates()
                if (updateInfo != null) {
                    showUpdateDialog = true
                }
            } catch (e: Exception) {
                android.util.Log.e("CZP", "Error checking updates on startup", e)
            }
            
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
    LaunchedEffect(taxRate, currentTheme) {
        if (isSettingsLoaded) {
            try {
                saveTaxRate(context, taxRate)
                saveThemeMode(context, currentTheme)
                android.util.Log.d("CZP", "Settings auto-saved: taxRate=$taxRate, theme=${currentTheme.name}")
            } catch (e: Exception) {
                android.util.Log.e("CZP", "Error auto-saving settings", e)
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
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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
                        ) {
                            listOf(Screen.Calculator, Screen.History, Screen.Settings).forEach { screen ->
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
                                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
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
                        history = savedCalculations, // <--- добавлено
                        modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                    )
                    Screen.History -> HistoryScreen(
                        calculations = savedCalculations,
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
                        modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                    )
                    Screen.Settings -> SettingsScreen(
                        currentTheme = currentTheme,
                        onThemeChange = { currentTheme = it },
                        taxRate = taxRate,
                        onTaxRateChange = { taxRate = it },
                        onCheckUpdates = {
                            scope.launch {
                                updateManager.checkForUpdates()
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
            DisclaimerDialog(
                onDismiss = {
                    showDisclaimer = false
                    scope.launch {
                        saveFirstLaunch(context)
                    }
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
                        android.util.Log.d("CZP", "Final settings save on dispose: taxRate=$taxRate, theme=${currentTheme.name}")
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
            if (state is UpdateState.UpdateAvailable || state is UpdateState.NoUpdateAvailable) {
                android.util.Log.d("CZP_UPDATE", "Showing update dialog for state: $state")
                showUpdateDialog = true
            }
        }
    }
    
    // Показ диалога обновления
    if (showUpdateDialog && updateState !is UpdateState.NoUpdate) {
        CZPTheme(darkTheme = isDarkTheme, dynamicColor = true) {
            UpdateDialog(
                updateState = updateState,
                onDismiss = { showUpdateDialog = false },
                onUpdate = {
                    scope.launch {
                        updateManager.checkForUpdates()
                    }
                },
                onDownload = {
                    scope.launch {
                        val currentState = updateState
                        if (currentState is UpdateState.UpdateAvailable) {
                            updateManager.downloadUpdate(currentState.updateInfo)
                        }
                    }
                },
                onInstall = { file ->
                    updateManager.installUpdate(file)
                    showUpdateDialog = false
                }
            )
        }
    }
}

// Перечисление для режимов темы
enum class ThemeMode(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    LIGHT("Светлая", Icons.Default.LightMode),
    DARK("Темная", Icons.Default.DarkMode),
    AUTO("Авто", Icons.Default.BrightnessAuto)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    taxRate: String,
    onSaveCalculation: (SalaryCalculation) -> Unit,
    history: List<SalaryCalculation>, // <--- добавлено
    modifier: Modifier = Modifier
) {
    var quarterlyHours by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var monthlyHours by remember { mutableStateOf("") }
    var nightHours by remember { mutableStateOf("") }
    var holidayHours by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showNormaDialog by remember { mutableStateOf(false) }
    val snackbarHostState = SnackbarHostState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    // Получаем сохранённые расчёты из истории (через CompositionLocal или параметр, если нужно)
    val context = LocalContext.current
    var savedCalculations by remember { mutableStateOf<List<SalaryCalculation>>(emptyList()) }
    // Загрузка истории при первом запуске
    LaunchedEffect(Unit) {
        savedCalculations = loadHistory(context)
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

            // Основные параметры
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
                        InputFieldWithHint(
                            label = "Квартальные нормы часов",
                            value = quarterlyHours,
                            onValueChange = { quarterlyHours = filterNumericInput(it, 9999.0) },
                            leadingIcon = Icons.Default.Schedule,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            }
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = "Например: 145000",
                            hint = "Ваш месячный оклад до вычетов",
                            isValid = salary.isEmpty() || salary.toDoubleOrNull() != null
                        )
                    }
                }
            }

            // Часы по типам смен
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 500)) + slideInHorizontally(
                    animationSpec = tween(800, delayMillis = 500),
                    initialOffsetX = { it }
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
                        InputFieldWithHint(
                            label = "Обычные часы в месяц",
                            value = monthlyHours,
                            onValueChange = { monthlyHours = filterNumericInput(it, 999.0) },
                            leadingIcon = Icons.Default.AccessTime,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = "Например: 172",
                            hint = "Обычно 160-200 часов в месяц",
                            isValid = monthlyHours.isEmpty() || monthlyHours.toDoubleOrNull() != null
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        InputFieldWithHint(
                            label = "Ночные часы",
                            value = nightHours,
                            onValueChange = { nightHours = filterNumericInput(it, 999.0) },
                            leadingIcon = Icons.Default.DarkMode,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = "Например: 71",
                            hint = "Часы работы с 22:00 до 06:00 (коэффициент 0.4)",
                            isValid = nightHours.isEmpty() || nightHours.toDoubleOrNull() != null
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        InputFieldWithHint(
                            label = "Праздничные часы",
                            value = holidayHours,
                            onValueChange = { holidayHours = filterNumericInput(it, 999.0) },
                            leadingIcon = Icons.Default.Celebration,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = "Например: 11",
                            hint = "Часы работы в праздничные дни",
                            isValid = holidayHours.isEmpty() || holidayHours.toDoubleOrNull() != null
                        )
                    }
                }
            }

            // Результаты расчета с анимацией
            AnimatedVisibility(
                visible = quarterlyHours.isNotEmpty() && salary.isNotEmpty() &&
                        monthlyHours.isNotEmpty() && nightHours.isNotEmpty() && holidayHours.isNotEmpty(),
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

                // Расчеты по типам смен
                val regularSalary = monthlyHoursAmount * netHourlyRate
                val nightSalary = nightHoursAmount * netHourlyRate * 0.4
                val holidaySalary = holidayHoursAmount * netHourlyRate
                val totalSalary = regularSalary + nightSalary + holidaySalary

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
                        val regularSalary = monthlyHoursAmount * netHourlyRate
                        val nightSalary = nightHoursAmount * netHourlyRate * 0.4
                        val holidaySalary = holidayHoursAmount * netHourlyRate
                        val totalSalary = regularSalary + nightSalary + holidaySalary
                        
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
                            totalSalary = totalSalary
                        )
                        
                        // Сохраняем расчет (проверка на существование уже в DatePickerDialog)
                        onSaveCalculation(calculation)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    placeholder: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun TaxRateButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary 
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputFieldWithHint(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    placeholder: String = "",
    hint: String = "",
    isValid: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isValid) MaterialTheme.colorScheme.primary 
                          else MaterialTheme.colorScheme.error
                )
            },
            trailingIcon = trailingIcon,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = keyboardOptions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isValid) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.error,
                unfocusedBorderColor = if (isValid) MaterialTheme.colorScheme.outline 
                                     else MaterialTheme.colorScheme.error,
                focusedLabelColor = if (isValid) MaterialTheme.colorScheme.primary 
                                  else MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        if (hint.isNotEmpty()) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300))
            ) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }
}

// --- Исправление цветов карточек и текста для светлой темы ---

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
            Divider(
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
                label = "Обычные часы",
                value = monthlyHours,
                icon = Icons.Default.AccessTime,
                color = MaterialTheme.colorScheme.primary,
                unit = "ч",
                textColor = textColor
            )
            ResultRow(
                label = "Зарплата за обычные часы",
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
            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline
            )
            // Вместо двух блоков с итогом оставляю только одну строку:
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

@Preview(showBackground = true)
@Composable
fun SalaryCalculatorPreview() {
    CZPTheme {
        CalculatorScreen(
            taxRate = "13",
            onSaveCalculation = { /* Preview */ },
            history = emptyList()
        )
    }
}

@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    history: List<SalaryCalculation>,
    onDateSelected: (LocalDate) -> Unit
) {
    var selectedYear by remember { mutableStateOf(LocalDate.now().year) }
    var selectedMonth by remember { mutableStateOf(LocalDate.now().monthValue) }
    var errorText by remember { mutableStateOf("") }
    
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
            Column {
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
            }
            
            // Отображение ошибки, если расчет за выбранный месяц и год уже существует
            val alreadyExists = history.any { it.date.year == selectedYear && it.date.monthValue == selectedMonth }
            errorText = if (alreadyExists) "Расчет за этот месяц уже сохранён" else ""
            
            if (errorText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
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
fun HistoryScreen(
    calculations: List<SalaryCalculation>,
    onDeleteCalculation: (SalaryCalculation) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCalculation by remember { mutableStateOf<SalaryCalculation?>(null) }
    val listState = rememberLazyListState()
    var isScrolled by remember { mutableStateOf(false) }
    
    LaunchedEffect(listState.firstVisibleItemIndex) {
        isScrolled = listState.firstVisibleItemIndex > 0
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        AnimatedVisibility(
            visible = !isScrolled,
            enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
            exit = fadeOut(animationSpec = tween(300)) + shrinkVertically()
        ) {
            Text(
                text = "История расчетов",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
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
        } else {
            val grouped = calculations.groupBy { it.date.year }.toSortedMap(compareByDescending { it })
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
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
                    items(yearList.sortedByDescending { it.date.monthValue }) { calculation ->
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            SwipeableHistoryCard(
                                calculation = calculation,
                                onDelete = { onDeleteCalculation(calculation) },
                                onClick = { selectedCalculation = calculation }
                            )
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
            }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeableHistoryCard(
    calculation: SalaryCalculation,
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

    // Сброс состояния при изменении calculation
    LaunchedEffect(calculation.id) {
        visible = true
        isDeleting = false
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
            }
            // Итоговая зарплата
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = String.format(Locale("ru"), "%,.0f ₽", calculation.totalSalary),
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

@Composable
fun HistoryDetailDialog(
    calculation: SalaryCalculation,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
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
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
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
                    color = SuccessGreen,
                    unit = "₽/ч"
                )
                
                ResultRow(
                    label = "Ставка НДФЛ",
                    value = calculation.taxRate.toDouble(),
                    icon = Icons.Default.AccountBalance,
                    color = MaterialTheme.colorScheme.tertiary,
                    unit = "%"
                )
                
                Divider(
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
                    label = "Обычные часы",
                    value = calculation.monthlyHours,
                    icon = Icons.Default.AccessTime,
                    color = MaterialTheme.colorScheme.primary,
                    unit = "ч"
                )
                
                ResultRow(
                    label = "Зарплата за обычные часы",
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
                    color = AccentOrange,
                    unit = "ч"
                )
                
                ResultRow(
                    label = "Зарплата за праздничные часы",
                    value = calculation.holidaySalary,
                    icon = Icons.Default.Celebration,
                    color = AccentOrange,
                    unit = "₽"
                )
                
                Divider(
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
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
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
        modifier = Modifier.fillMaxWidth(0.95f)
    )
}

@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    taxRate: String,
    onTaxRateChange: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 100.dp),
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Настройки темы
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Внешний вид",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Выберите тему приложения",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Переключатели темы
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TaxRateButton(
                        text = "Светлая",
                        selected = currentTheme == ThemeMode.LIGHT,
                        onClick = { onThemeChange(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = ThemeMode.LIGHT.icon
                    )
                    TaxRateButton(
                        text = "Тёмная",
                        selected = currentTheme == ThemeMode.DARK,
                        onClick = { onThemeChange(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = ThemeMode.DARK.icon
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                TaxRateButton(
                    text = "Авто",
                    selected = currentTheme == ThemeMode.AUTO,
                    onClick = { onThemeChange(ThemeMode.AUTO) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = ThemeMode.AUTO.icon
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Настройки ставки НДФЛ
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Ставка НДФЛ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Выберите ставку налога для расчетов",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TaxRateButton(
                        text = "13%",
                        selected = taxRate == "13",
                        onClick = { onTaxRateChange("13") },
                        modifier = Modifier.weight(1f)
                    )
                    TaxRateButton(
                        text = "15%",
                        selected = taxRate == "15",
                        onClick = { onTaxRateChange("15") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Ставка 15% применяется к доходам от 2.4 до 5 млн рублей в год",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Информация о приложении
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_czp_logo),
                        contentDescription = "CZp Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "О приложении",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "CZp v1.8.5",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Умный калькулятор зарплаты со сменным графиком",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Расчеты по обычным, ночным и праздничным часам с учетом НДФЛ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "© 2025 Depotect",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Проверка обновлений
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Обновления",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Проверьте наличие новых версий приложения",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onCheckUpdates,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Проверить обновления",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Функция для фильтрации ввода - запрещает отрицательные значения и пробелы
fun filterNumericInput(input: String, maxValue: Double = Double.MAX_VALUE): String {
    val filtered = input.filter { it.isDigit() || it == '.' }
    val doubleValue = filtered.toDoubleOrNull() ?: 0.0
    return if (doubleValue <= maxValue) filtered else filtered.dropLast(1)
}

fun formatSmart(value: Double): String =
    if (value % 1.0 == 0.0) "%.0f".format(value) else "%.1f".format(value)

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