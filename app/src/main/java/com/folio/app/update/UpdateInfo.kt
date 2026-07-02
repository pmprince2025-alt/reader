package com.folio.app.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class GitHubRelease(
    val tag_name: String,
    val assets: List<ReleaseAsset> = emptyList(),
    val body: String? = null
)

@Serializable
data class ReleaseAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long = 0
)

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String? = null,
    val fileSize: Long
)

object UpdateParser {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun parseRelease(response: String): UpdateInfo? {
        return try {
            val release = json.decodeFromString<GitHubRelease>(response)
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return null
            UpdateInfo(
                latestVersion = release.tag_name.removePrefix("v"),
                downloadUrl = apkAsset.browser_download_url,
                releaseNotes = release.body,
                fileSize = apkAsset.size
            )
        } catch (e: Exception) {
            null
        }
    }
}

fun compareVersions(current: String, latest: String): Boolean {
    val curParts = current.split(".").map { it.toIntOrNull() ?: 0 }
    val latParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(curParts.size, latParts.size)) {
        val c = curParts.getOrElse(i) { 0 }
        val l = latParts.getOrElse(i) { 0 }
        if (l > c) return true
        if (l < c) return false
    }
    return false
}
