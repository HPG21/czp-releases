package com.depotect.czp.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.depotect.czp.R

class UpdateCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val CHANNEL_ID = "update_channel"
        const val NOTIFICATION_ID = 1001
    }
    
    override suspend fun doWork(): Result {
        return try {
            val updateManager = UpdateManager(context)
            val updateInfo = updateManager.checkForUpdates()
            
            if (updateInfo != null) {
                showUpdateNotification(updateInfo)
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    private fun showUpdateNotification(updateInfo: UpdateInfo) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Создаем канал для Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Обновления приложения",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о новых версиях приложения"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_czp_logo)
            .setContentTitle("Доступно обновление")
            .setContentText("Новая версия ${updateInfo.versionName}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(updateInfo.releaseNotes))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
} 