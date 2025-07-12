package com.depotect.czp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.view.WindowManager
import com.depotect.czp.navigation.MainApp
import com.depotect.czp.ui.theme.CZPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Переключаемся с splash темы на основную тему
        setTheme(R.style.Theme_CZP)
        
        // Устанавливаем правильные флаги для окна
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        
        setContent {
            MainApp()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Правильно обрабатываем паузу
    }
    
    override fun onStop() {
        super.onStop()
        // Правильно обрабатываем остановку
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Очищаем ресурсы
    }
}