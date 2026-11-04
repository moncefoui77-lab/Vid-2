package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vidsmart_prefs", Context.MODE_PRIVATE)

    fun savePlaybackPosition(videoKey: String, positionMs: Long) {
        prefs.edit().putLong("pos_$videoKey", positionMs).apply()
    }

    fun getPlaybackPosition(videoKey: String): Long {
        return prefs.getLong("pos_$videoKey", 0L)
    }

    fun clearPlaybackPosition(videoKey: String) {
        prefs.edit().remove("pos_$videoKey").apply()
    }

    fun isAdBlockEnabled(): Boolean {
        return prefs.getBoolean("adblock_enabled", true)
    }

    fun setAdBlockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("adblock_enabled", enabled).apply()
    }

    fun getBlockedAdsCount(): Int {
        return prefs.getInt("blocked_ads_count", 0)
    }

    fun incrementBlockedAdsCount() {
        val current = getBlockedAdsCount()
        prefs.edit().putInt("blocked_ads_count", current + 1).apply()
    }

    fun isDesktopMode(): Boolean {
        return prefs.getBoolean("desktop_mode", false)
    }

    fun setDesktopMode(enabled: Boolean) {
        prefs.edit().putBoolean("desktop_mode", enabled).apply()
    }

    fun getSearchEngineId(): String {
        return prefs.getString("selected_search_engine", "google") ?: "google"
    }

    fun setSearchEngineId(id: String) {
        prefs.edit().putString("selected_search_engine", id).apply()
    }
}
