package com.folio.app.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateState(
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val updateInfo: UpdateInfo? = null,
    val isAvailable: Boolean = false,
    val errorMessage: String? = null
)

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val repoApi = "https://api.github.com/repos/pmprince2025-alt/reader/releases/latest"
    private var downloadedApk: File? = null

    val currentVersion: String
        get() {
            return try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
            } catch (e: Exception) {
                "1.0.0"
            }
        }

    suspend fun checkForUpdate(): UpdateState = withContext(Dispatchers.IO) {
        try {
            val conn = URL(repoApi).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode != 200) {
                return@withContext UpdateState(errorMessage = "Update check failed (${conn.responseCode})")
            }

            val response = conn.inputStream.bufferedReader().readText()
            val info = UpdateParser.parseRelease(response)

            if (info == null) {
                return@withContext UpdateState(errorMessage = "No APK found in latest release")
            }

            val current = currentVersion
            val available = compareVersions(current, info.latestVersion)

            UpdateState(
                updateInfo = info,
                isAvailable = available
            )
        } catch (e: Exception) {
            UpdateState(errorMessage = e.message ?: "Network error")
        }
    }

    suspend fun downloadUpdate(info: UpdateInfo, onProgress: (Float) -> Unit): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL(info.downloadUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connect()
            val totalLength = conn.contentLengthLong
            val inputStream = conn.inputStream
            val file = File(context.cacheDir, "folio_update.apk")
            file.deleteOnExit()

            FileOutputStream(file).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (totalLength > 0) {
                        onProgress(totalRead.toFloat() / totalLength)
                    }
                }
            }
            downloadedApk = file
            file
        } catch (e: Exception) {
            null
        }
    }

    fun installApk(): Boolean {
        val file = downloadedApk ?: return false
        return try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
