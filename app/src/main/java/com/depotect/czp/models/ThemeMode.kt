package com.depotect.czp.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.ui.graphics.vector.ImageVector

// Перечисление для режимов темы
enum class ThemeMode(val title: String, val icon: ImageVector) {
    LIGHT("Светлая", Icons.Default.LightMode),
    DARK("Темная", Icons.Default.DarkMode),
    AUTO("Авто", Icons.Default.BrightnessAuto)
} 