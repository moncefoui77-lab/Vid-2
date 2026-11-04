package com.example.model

data class BrowserTab(
    val id: String,
    val title: String = "New Tab",
    val url: String = "vidsmart://home",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0
)

data class WebShortcut(
    val id: String,
    val title: String,
    val url: String,
    val description: String,
    val colorHex: Long
)
