package com.depotect.czp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

// Навигация
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Calculator : Screen("calculator", "Калькулятор", Icons.Default.Calculate)
    object History : Screen("history", "История", Icons.Default.History)
    object Analytics : Screen("analytics", "Аналитика", Icons.Default.Analytics)
    object Settings : Screen("settings", "Настройки", Icons.Default.Settings)
} 