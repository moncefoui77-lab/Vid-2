package com.example.model

enum class VideoFormat(val label: String) {
    MP4("MP4"),
    HLS("HLS (m3u8)"),
    DASH("DASH (mpd)"),
    WEBM("WebM"),
    UNKNOWN("Video")
}

data class DetectedVideo(
    val id: String,
    val url: String,
    val title: String,
    val format: VideoFormat,
    val mimeType: String? = null,
    val sizeBytes: Long = -1L,
    val quality: String = "Auto / HD",
    val isDrm: Boolean = false,
    val sourcePageUrl: String = "",
    val detectedAt: Long = System.currentTimeMillis()
)
