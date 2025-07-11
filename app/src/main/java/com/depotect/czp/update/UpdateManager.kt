package com.depotect.czp.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.work.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class UpdateManager(private val context: Context) {
    
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.NoUpdate)
    val updateState: Flow<UpdateState> = _updateState.asStateFlow()
    
    private val api: GitHubApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", UpdateConfig.USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .build()
        
        Retrofit.Builder()
            .baseUrl(UpdateConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApi::class.java)
    }
    
    suspend fun checkForUpdates(): UpdateInfo? {
        try {
            _updateState.value = UpdateState.Checking
            
            val release = api.getLatestRelease()
            val currentVersionCode = context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            
            // Парсим версию из tag_name (например, "v1.5" -> 1.5)
            val newVersionCode = parseVersionCode(release.tagName)
            
            android.util.Log.d("CZP_UPDATE", "Current version code: $currentVersionCode")
            android.util.Log.d("CZP_UPDATE", "Latest release tag: ${release.tagName}")
            android.util.Log.d("CZP_UPDATE", "Parsed new version code: $newVersionCode")
            
            if (newVersionCode > currentVersionCode) {
                android.util.Log.d("CZP_UPDATE", "Update available! New version is higher")
                val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                if (apkAsset != null) {
                    val updateInfo = UpdateInfo(
                        versionName = release.name,
                        versionCode = newVersionCode,
                        downloadUrl = apkAsset.downloadUrl,
                        releaseNotes = release.body,
                        fileSize = apkAsset.size,
                        isForceUpdate = release.body.contains("[FORCE_UPDATE]")
                    )
                    
                    _updateState.value = UpdateState.UpdateAvailable(updateInfo)
                    return updateInfo
                } else {
                    android.util.Log.w("CZP_UPDATE", "No APK asset found in release")
                }
            } else {
                android.util.Log.d("CZP_UPDATE", "No update available. Current: $currentVersionCode, Latest: $newVersionCode")
            }
            
            _updateState.value = UpdateState.NoUpdateAvailable
            return null
            
        } catch (e: Exception) {
            android.util.Log.e("CZP_UPDATE", "Error checking updates", e)
            _updateState.value = UpdateState.Error("Ошибка проверки обновлений: ${e.message}")
            return null
        }
    }
    
    suspend fun downloadUpdate(updateInfo: UpdateInfo): File? {
        try {
            val downloadDir = File(context.externalCacheDir, "updates")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            
            val apkFile = File(downloadDir, "czp-update-${updateInfo.versionName}.apk")
            
            val response = api.downloadApk(updateInfo.downloadUrl)
            val body = response.body() ?: throw IOException("Пустой ответ")
            
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            
            FileOutputStream(apkFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                        _updateState.value = UpdateState.Downloading(progress)
                    }
                }
            }
            
            _updateState.value = UpdateState.DownloadComplete(apkFile)
            return apkFile
            
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Ошибка загрузки: ${e.message}")
            return null
        }
    }
    
    fun installUpdate(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            context.startActivity(intent)
            
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Ошибка установки: ${e.message}")
        }
    }
    
    private fun parseVersionCode(tagName: String): Int {
        return try {
            // Убираем "v" из начала и парсим версию
            val version = tagName.removePrefix("v").split(".")
            when (version.size) {
                1 -> version[0].toInt() * 10000 // major.0.0
                2 -> version[0].toInt() * 10000 + version[1].toInt() * 100 // major.minor.0
                3 -> version[0].toInt() * 10000 + version[1].toInt() * 100 + version[2].toInt() // major.minor.patch
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    companion object {
        fun scheduleUpdateCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val updateWork = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                1, TimeUnit.DAYS
            ).setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "update_check",
                ExistingPeriodicWorkPolicy.KEEP,
                updateWork
            )
        }
    }
}

// Расширение для API - добавляем метод загрузки APK
suspend fun GitHubApi.downloadApk(url: String): retrofit2.Response<ResponseBody> {
    val client = OkHttpClient.Builder().build()
    val retrofit = Retrofit.Builder()
        .baseUrl("https://github.com/")
        .client(client)
        .build()
    
    val downloadApi = retrofit.create(DownloadApi::class.java)
    return downloadApi.downloadFile(url)
}

interface DownloadApi {
    @retrofit2.http.GET
    suspend fun downloadFile(@retrofit2.http.Url url: String): retrofit2.Response<ResponseBody>
} 