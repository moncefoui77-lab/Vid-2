package com.example.data

import android.content.Context
import android.os.Environment
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val metaFile: File by lazy {
        File(context.filesDir, "downloads_meta.json")
    }

    init {
        loadPersistedDownloads()
    }

    private fun getStorageDir(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun loadPersistedDownloads() {
        if (!metaFile.exists()) return
        try {
            val jsonStr = metaFile.readText()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<DownloadItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val statusStr = obj.optString("status", DownloadStatus.FAILED.name)
                val status = try {
                    val parsed = DownloadStatus.valueOf(statusStr)
                    // If app was killed while downloading, treat as paused
                    if (parsed == DownloadStatus.DOWNLOADING || parsed == DownloadStatus.QUEUED) {
                        DownloadStatus.PAUSED
                    } else parsed
                } catch (_: Exception) {
                    DownloadStatus.FAILED
                }

                list.add(
                    DownloadItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        url = obj.getString("url"),
                        localFilePath = obj.getString("localFilePath"),
                        totalBytes = obj.optLong("totalBytes", -1L),
                        downloadedBytes = obj.optLong("downloadedBytes", 0L),
                        status = status,
                        speedBytesPerSec = 0L,
                        errorMessage = obj.optString("errorMessage", null),
                        addedAt = obj.optLong("addedAt", System.currentTimeMillis()),
                        completedAt = if (obj.has("completedAt")) obj.optLong("completedAt") else null
                    )
                )
            }
            _downloads.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun savePersistedDownloads() {
        try {
            val jsonArray = JSONArray()
            _downloads.value.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("url", item.url)
                    put("localFilePath", item.localFilePath)
                    put("totalBytes", item.totalBytes)
                    put("downloadedBytes", item.downloadedBytes)
                    put("status", item.status.name)
                    put("addedAt", item.addedAt)
                    if (item.completedAt != null) put("completedAt", item.completedAt)
                    if (item.errorMessage != null) put("errorMessage", item.errorMessage)
                }
                jsonArray.put(obj)
            }
            metaFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun enqueueDownload(url: String, title: String, extension: String = "mp4"): String {
        val id = UUID.randomUUID().toString()
        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9._ -]"), "_").take(60)
        val fileName = "${sanitizedTitle}_${System.currentTimeMillis() % 10000}.$extension"
        val targetFile = File(getStorageDir(), fileName)

        val item = DownloadItem(
            id = id,
            title = title,
            url = url,
            localFilePath = targetFile.absolutePath,
            status = DownloadStatus.DOWNLOADING,
            downloadedBytes = 0L,
            totalBytes = -1L
        )

        _downloads.value = listOf(item) + _downloads.value
        savePersistedDownloads()

        startDownloadJob(item)
        return id
    }

    fun pauseDownload(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        updateItem(id) { it.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L) }
        savePersistedDownloads()
    }

    fun resumeDownload(id: String) {
        val item = _downloads.value.find { it.id == id } ?: return
        if (item.status == DownloadStatus.DOWNLOADING) return

        updateItem(id) { it.copy(status = DownloadStatus.DOWNLOADING, errorMessage = null) }
        savePersistedDownloads()
        startDownloadJob(item)
    }

    fun deleteDownload(id: String, deleteLocalFile: Boolean = true) {
        pauseDownload(id)
        val item = _downloads.value.find { it.id == id }
        if (deleteLocalFile && item != null) {
            val file = File(item.localFilePath)
            if (file.exists()) file.delete()
        }
        _downloads.value = _downloads.value.filter { it.id != id }
        savePersistedDownloads()
    }

    fun renameDownload(id: String, newTitle: String) {
        val item = _downloads.value.find { it.id == id } ?: return
        val currentFile = File(item.localFilePath)
        val ext = currentFile.extension.ifBlank { "mp4" }
        val sanitized = newTitle.replace(Regex("[^a-zA-Z0-9._ -]"), "_")
        val newFile = File(currentFile.parentFile, "$sanitized.$ext")

        val finalPath = if (currentFile.exists() && currentFile != newFile) {
            if (currentFile.renameTo(newFile)) newFile.absolutePath else item.localFilePath
        } else {
            newFile.absolutePath
        }

        updateItem(id) { it.copy(title = newTitle, localFilePath = finalPath) }
        savePersistedDownloads()
    }

    private fun startDownloadJob(item: DownloadItem) {
        val job = scope.launch {
            val file = File(item.localFilePath)
            var currentBytes = if (file.exists()) file.length() else 0L

            try {
                val requestBuilder = Request.Builder()
                    .url(item.url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")

                if (currentBytes > 0) {
                    requestBuilder.header("Range", "bytes=$currentBytes-")
                }

                val response = httpClient.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful && response.code != 206) {
                    // If range not satisfiable, restart from 0
                    if (response.code == 416) {
                        file.delete()
                        currentBytes = 0L
                    } else {
                        throw Exception("HTTP Error: ${response.code} ${response.message}")
                    }
                }

                val body = response.body ?: throw Exception("Empty response body")
                val responseLength = body.contentLength()
                val totalBytes = if (response.code == 206 && currentBytes > 0) {
                    currentBytes + responseLength
                } else if (responseLength > 0) {
                    responseLength
                } else {
                    item.totalBytes
                }

                updateItem(item.id) {
                    it.copy(
                        totalBytes = totalBytes,
                        downloadedBytes = currentBytes,
                        status = DownloadStatus.DOWNLOADING
                    )
                }

                val raf = RandomAccessFile(file, "rw")
                raf.seek(currentBytes)

                val buffer = ByteArray(32 * 1024)
                val inputStream = body.byteStream()
                var read: Int
                var lastSpeedCalcTime = System.currentTimeMillis()
                var bytesSinceLastCalc = 0L

                while (inputStream.read(buffer).also { read = it } != -1) {
                    raf.write(buffer, 0, read)
                    currentBytes += read
                    bytesSinceLastCalc += read

                    val now = System.currentTimeMillis()
                    val timeDelta = now - lastSpeedCalcTime
                    if (timeDelta >= 800) {
                        val speed = (bytesSinceLastCalc * 1000) / timeDelta
                        bytesSinceLastCalc = 0L
                        lastSpeedCalcTime = now

                        updateItem(item.id) {
                            it.copy(
                                downloadedBytes = currentBytes,
                                totalBytes = totalBytes,
                                speedBytesPerSec = speed
                            )
                        }
                    }
                }

                raf.close()
                inputStream.close()
                response.close()

                updateItem(item.id) {
                    it.copy(
                        downloadedBytes = currentBytes,
                        totalBytes = if (totalBytes > 0) totalBytes else currentBytes,
                        status = DownloadStatus.COMPLETED,
                        speedBytesPerSec = 0L,
                        completedAt = System.currentTimeMillis()
                    )
                }
                savePersistedDownloads()
            } catch (e: Exception) {
                if (activeJobs[item.id]?.isCancelled == true) {
                    updateItem(item.id) { it.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L) }
                } else {
                    updateItem(item.id) {
                        it.copy(
                            status = DownloadStatus.FAILED,
                            speedBytesPerSec = 0L,
                            errorMessage = e.localizedMessage ?: "Download failed"
                        )
                    }
                }
                savePersistedDownloads()
            } finally {
                activeJobs.remove(item.id)
            }
        }

        activeJobs[item.id] = job
    }

    private fun updateItem(id: String, transform: (DownloadItem) -> DownloadItem) {
        _downloads.value = _downloads.value.map {
            if (it.id == id) transform(it) else it
        }
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.0f KB", kb)
                else -> "$bytes B"
            }
        }

        fun formatSpeed(bytesPerSec: Long): String {
            if (bytesPerSec <= 0) return "0 KB/s"
            val kb = bytesPerSec / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format("%.1f MB/s", mb)
            } else {
                String.format("%.0f KB/s", kb)
            }
        }
    }
}
