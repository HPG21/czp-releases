package com.depotect.czp.data

import androidx.datastore.preferences.core.stringPreferencesKey

// Ключи для DataStore
object Preferences {
    val HISTORY_KEY = stringPreferencesKey("history_json")
    val TAX_RATE_KEY = stringPreferencesKey("tax_rate")
    val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    val FIRST_LAUNCH_KEY = stringPreferencesKey("first_launch")
    val BASE_SALARY_KEY = stringPreferencesKey("base_salary")
    val BASE_SALARY_ENABLED_KEY = stringPreferencesKey("base_salary_enabled")
    val SHOW_QUARTERS_KEY = stringPreferencesKey("show_quarters")
} 