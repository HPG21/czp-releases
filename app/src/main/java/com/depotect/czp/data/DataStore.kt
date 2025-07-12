package com.depotect.czp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.depotect.czp.models.SalaryCalculation
import com.depotect.czp.models.ThemeMode
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
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "salary_history")

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
            prefs[Preferences.HISTORY_KEY] = json
            android.util.Log.d("CZP", "Data written to DataStore preferences")
        }
        
        // Принудительная синхронизация для release-сборки
        val verification = context.dataStore.data.first()
        val savedJson = verification[Preferences.HISTORY_KEY]
        android.util.Log.d("CZP", "Verification: saved JSON length: ${savedJson?.length ?: 0}")
        
        if (savedJson == json) {
            android.util.Log.d("CZP", "History saved successfully and verified: ${history.size} items")
        } else {
            android.util.Log.w("CZP", "History save verification failed - data mismatch")
            // Попытка повторного сохранения
            context.dataStore.edit { prefs ->
                prefs[Preferences.HISTORY_KEY] = json
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
                prefs[Preferences.HISTORY_KEY] = json
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
        val json = prefs[Preferences.HISTORY_KEY] ?: return emptyList()
        
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
            prefs[Preferences.TAX_RATE_KEY] = taxRate
        }
        android.util.Log.d("CZP", "Tax rate saved: $taxRate")
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error saving tax rate", e)
    }
}

suspend fun loadTaxRate(context: Context): String {
    return try {
        val prefs = context.dataStore.data.first()
        val result = prefs[Preferences.TAX_RATE_KEY] ?: "13"
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
            prefs[Preferences.THEME_MODE_KEY] = themeMode.name
        }
        android.util.Log.d("CZP", "Theme mode saved: ${themeMode.name}")
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error saving theme mode", e)
    }
}

suspend fun loadThemeMode(context: Context): ThemeMode {
    return try {
        val prefs = context.dataStore.data.first()
        val themeName = prefs[Preferences.THEME_MODE_KEY] ?: ThemeMode.AUTO.name
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
            prefs[Preferences.FIRST_LAUNCH_KEY] = "false"
        }
        android.util.Log.d("CZP", "First launch flag saved")
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error saving first launch flag", e)
    }
}

suspend fun isFirstLaunch(context: Context): Boolean {
    return try {
        val prefs = context.dataStore.data.first()
        val result = prefs[Preferences.FIRST_LAUNCH_KEY] != "false"
        android.util.Log.d("CZP", "First launch check: $result")
        result
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error checking first launch", e)
        true
    }
}

suspend fun saveBaseSalary(context: Context, baseSalary: String) {
    try {
        context.dataStore.edit { prefs ->
            prefs[Preferences.BASE_SALARY_KEY] = baseSalary
        }
        android.util.Log.d("CZP", "Base salary saved: $baseSalary")
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error saving base salary", e)
    }
}

suspend fun loadBaseSalary(context: Context): String {
    return try {
        val prefs = context.dataStore.data.first()
        val result = prefs[Preferences.BASE_SALARY_KEY] ?: ""
        android.util.Log.d("CZP", "Base salary loaded: $result")
        result
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error loading base salary", e)
        ""
    }
}

suspend fun saveBaseSalaryEnabled(context: Context, enabled: Boolean) {
    try {
        context.dataStore.edit { prefs ->
            prefs[Preferences.BASE_SALARY_ENABLED_KEY] = if (enabled) "true" else "false"
        }
        android.util.Log.d("CZP", "Base salary enabled saved: $enabled")
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error saving base salary enabled", e)
    }
}

suspend fun loadBaseSalaryEnabled(context: Context): Boolean {
    return try {
        val prefs = context.dataStore.data.first()
        val result = prefs[Preferences.BASE_SALARY_ENABLED_KEY] == "true"
        android.util.Log.d("CZP", "Base salary enabled loaded: $result")
        result
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error loading base salary enabled", e)
        false
    }
}

suspend fun saveShowQuarters(context: Context, showQuarters: Boolean) {
    try {
        context.dataStore.edit { prefs ->
            prefs[Preferences.SHOW_QUARTERS_KEY] = if (showQuarters) "true" else "false"
        }
        android.util.Log.d("CZP", "Show quarters saved: $showQuarters")
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error saving show quarters", e)
    }
}

suspend fun loadShowQuarters(context: Context): Boolean {
    return try {
        val prefs = context.dataStore.data.first()
        val result = prefs[Preferences.SHOW_QUARTERS_KEY] == "true"
        android.util.Log.d("CZP", "Show quarters loaded: $result")
        result
    } catch (e: Exception) {
        android.util.Log.e("CZP", "Error loading show quarters", e)
        true // По умолчанию показываем кварталы
    }
}

// Настройки карточек аналитики
object AnalyticsCardsKeys {
    val KEY_METRICS = booleanPreferencesKey("analytics_key_metrics")
    val SALARY_TREND = booleanPreferencesKey("analytics_salary_trend")
    val HOURS_DISTRIBUTION = booleanPreferencesKey("analytics_hours_distribution")
    val TOP_MONTHS = booleanPreferencesKey("analytics_top_months")
    val HOURLY_EFFICIENCY = booleanPreferencesKey("analytics_hourly_efficiency")
    val YEAR_COMPARISON = booleanPreferencesKey("analytics_year_comparison")
    val SALARY_GROWTH = booleanPreferencesKey("analytics_salary_growth")
    val SALARY_RAISE = booleanPreferencesKey("analytics_salary_raise")
}

suspend fun saveAnalyticsCardSettings(
    context: Context,
    keyMetrics: Boolean,
    salaryTrend: Boolean,
    hoursDistribution: Boolean,
    topMonths: Boolean,
    hourlyEfficiency: Boolean,
    yearComparison: Boolean,
    salaryGrowth: Boolean,
    salaryRaise: Boolean
) {
    context.dataStore.edit { preferences ->
        preferences[AnalyticsCardsKeys.KEY_METRICS] = keyMetrics
        preferences[AnalyticsCardsKeys.SALARY_TREND] = salaryTrend
        preferences[AnalyticsCardsKeys.HOURS_DISTRIBUTION] = hoursDistribution
        preferences[AnalyticsCardsKeys.TOP_MONTHS] = topMonths
        preferences[AnalyticsCardsKeys.HOURLY_EFFICIENCY] = hourlyEfficiency
        preferences[AnalyticsCardsKeys.YEAR_COMPARISON] = yearComparison
        preferences[AnalyticsCardsKeys.SALARY_GROWTH] = salaryGrowth
        preferences[AnalyticsCardsKeys.SALARY_RAISE] = salaryRaise
    }
}

suspend fun loadAnalyticsCardSettings(context: Context): Map<String, Boolean> {
    val preferences = context.dataStore.data.first()
    return mapOf(
        "key_metrics" to (preferences[AnalyticsCardsKeys.KEY_METRICS] ?: true),
        "salary_trend" to (preferences[AnalyticsCardsKeys.SALARY_TREND] ?: true),
        "hours_distribution" to (preferences[AnalyticsCardsKeys.HOURS_DISTRIBUTION] ?: true),
        "top_months" to (preferences[AnalyticsCardsKeys.TOP_MONTHS] ?: true),
        "hourly_efficiency" to (preferences[AnalyticsCardsKeys.HOURLY_EFFICIENCY] ?: true),
        "year_comparison" to (preferences[AnalyticsCardsKeys.YEAR_COMPARISON] ?: true),
        "salary_growth" to (preferences[AnalyticsCardsKeys.SALARY_GROWTH] ?: true),
        "salary_raise" to (preferences[AnalyticsCardsKeys.SALARY_RAISE] ?: true)
    )
} 