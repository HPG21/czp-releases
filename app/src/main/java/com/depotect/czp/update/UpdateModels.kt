package com.depotect.czp.update

import com.google.gson.annotations.SerializedName

// Модели для GitHub API
data class ReleaseInfo(
    @SerializedName("tag_name")
    val tagName: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("body")
    val body: String,
    @SerializedName("assets")
    val assets: List<Asset>,
    @SerializedName("published_at")
    val publishedAt: String
)

data class Asset(
    @SerializedName("name")
    val name: String,
    @SerializedName("browser_download_url")
    val downloadUrl: String,
    @SerializedName("size")
    val size: Long,
    @SerializedName("content_type")
    val contentType: String
)

// Модель для информации об обновлении
data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val fileSize: Long,
    val isForceUpdate: Boolean = false
)

// Состояние обновления
sealed class UpdateState {
    object Checking : UpdateState()
    object NoUpdate : UpdateState()
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class DownloadComplete(val file: java.io.File) : UpdateState()
    data class Error(val message: String) : UpdateState()
} 