package com.example.data

import android.net.Uri
import com.example.model.DetectedVideo
import com.example.model.VideoFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object VideoDetector {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val DRM_DOMAINS = listOf(
        "netflix.com",
        "disneyplus.com",
        "primevideo.com",
        "hbomax.com",
        "max.com",
        "hulu.com",
        "tv.apple.com",
        "peacocktv.com"
    )

    private val DRM_PATTERNS = listOf(
        "widevine",
        "clearkey",
        "playready",
        "fairplay",
        "/drm/",
        "license_server",
        "encrypted-media"
    )

    private val EXTENSION_PATTERN = Pattern.compile(
        "\\.(mp4|m3u8|mpd|webm|m4v)(\\?.*)?$",
        Pattern.CASE_INSENSITIVE
    )

    fun isDrmUrl(url: String, pageUrl: String): Boolean {
        val lowerUrl = url.lowercase()
        val lowerPage = pageUrl.lowercase()

        for (domain in DRM_DOMAINS) {
            if (lowerPage.contains(domain) || lowerUrl.contains(domain)) {
                return true
            }
        }

        for (pattern in DRM_PATTERNS) {
            if (lowerUrl.contains(pattern)) {
                return true
            }
        }

        return false
    }

    fun isPotentialVideoRequest(url: String): Boolean {
        val lower = url.lowercase()

        // Ignore small analytics, icons, or audio chunks if not video
        if (lower.contains("favicon") || lower.contains(".jpg") || lower.contains(".png") ||
            lower.contains(".css") || lower.contains(".gif") || lower.contains(".svg")
        ) {
            return false
        }

        if (lower.contains(".mp4") || lower.contains(".m3u8") ||
            lower.contains(".mpd") || lower.contains(".webm") ||
            lower.contains(".m4v") || lower.contains("videoplayback") ||
            lower.contains("mime=video") || lower.contains("video/mp4") ||
            lower.contains(".googlevideo.com/videoplayback")
        ) {
            return true
        }

        return EXTENSION_PATTERN.matcher(lower).find()
    }

    fun parseVideo(
        url: String,
        pageUrl: String,
        pageTitle: String,
        headers: Map<String, String>? = null
    ): DetectedVideo? {
        if (!isPotentialVideoRequest(url)) return null

        val isDrm = isDrmUrl(url, pageUrl)

        val format = when {
            url.contains(".m3u8", ignoreCase = true) ||
                    headers?.get("content-type")?.contains("application/x-mpegurl", true) == true ||
                    headers?.get("content-type")?.contains("application/vnd.apple.mpegurl", true) == true ->
                VideoFormat.HLS

            url.contains(".mpd", ignoreCase = true) ||
                    headers?.get("content-type")?.contains("application/dash+xml", true) == true ->
                VideoFormat.DASH

            url.contains(".webm", ignoreCase = true) ||
                    headers?.get("content-type")?.contains("video/webm", true) == true ->
                VideoFormat.WEBM

            url.contains(".mp4", ignoreCase = true) ||
                    headers?.get("content-type")?.contains("video/mp4", true) == true ||
                    url.contains("videoplayback", ignoreCase = true) ->
                VideoFormat.MP4

            else -> VideoFormat.UNKNOWN
        }

        val quality = detectQuality(url)

        val cleanTitle = buildTitle(url, pageTitle, format)

        val id = UUID.nameUUIDFromBytes(url.take(200).toByteArray()).toString()

        return DetectedVideo(
            id = id,
            url = url,
            title = cleanTitle,
            format = format,
            quality = quality,
            isDrm = isDrm,
            sourcePageUrl = pageUrl
        )
    }

    private fun detectQuality(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains("2160p") || lower.contains("4k") -> "4K UHD"
            lower.contains("1440p") || lower.contains("2k") -> "1440p QHD"
            lower.contains("1080p") -> "1080p Full HD"
            lower.contains("720p") -> "720p HD"
            lower.contains("480p") -> "480p SD"
            lower.contains("360p") -> "360p"
            lower.contains("240p") -> "240p"
            lower.contains(".m3u8") -> "Adaptive HLS"
            lower.contains(".mpd") -> "Adaptive DASH"
            else -> "Auto / High Quality"
        }
    }

    private fun buildTitle(url: String, pageTitle: String, format: VideoFormat): String {
        if (pageTitle.isNotBlank() && !pageTitle.startsWith("http", ignoreCase = true)) {
            val sanitized = pageTitle.trim().take(60)
            return "$sanitized (${format.label})"
        }

        return try {
            val uri = Uri.parse(url)
            val lastPath = uri.lastPathSegment
            if (!lastPath.isNullOrBlank() && lastPath.length > 3) {
                lastPath.take(50)
            } else {
                "Video_${format.label}_${System.currentTimeMillis() % 10000}"
            }
        } catch (_: Exception) {
            "Video_${format.label}"
        }
    }

    suspend fun probeContentLength(url: String): Long = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val lengthStr = response.header("Content-Length")
                lengthStr?.toLongOrNull() ?: -1L
            }
        } catch (_: Exception) {
            -1L
        }
    }
}
