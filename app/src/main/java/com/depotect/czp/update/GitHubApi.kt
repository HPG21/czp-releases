package com.depotect.czp.update

import retrofit2.http.GET
import retrofit2.http.Headers

interface GitHubApi {
    @Headers("Accept: application/vnd.github.v3+json")
    @GET("repos/HPG21/czp-releases/releases/latest")
    suspend fun getLatestRelease(): ReleaseInfo
}

// Константы для API
object UpdateConfig {
    const val GITHUB_OWNER = "HPG21"
    const val GITHUB_REPO = "czp-releases"
    const val BASE_URL = "https://api.github.com/"
    const val USER_AGENT = "CZP-Update-Checker/1.0"
} 