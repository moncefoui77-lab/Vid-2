package com.example.data

import com.example.model.SearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object SearchSuggestionRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    suspend fun fetchSuggestions(query: String, engine: SearchEngine): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val endpoint = engine.suggestUrl ?: SearchEngine.GOOGLE.suggestUrl ?: return@withContext emptyList()
        val url = String.format(endpoint, URLEncoder.encode(trimmed, "UTF-8"))

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
                .header("Accept", "*/*")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                parseSuggestions(bodyStr, engine)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseSuggestions(jsonStr: String, engine: SearchEngine): List<String> {
        val list = mutableListOf<String>()
        try {
            val trimmed = jsonStr.trim()
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                // Standard OpenSearch / Chrome format: ["query", ["sugg1", "sugg2", ...]]
                if (array.length() >= 2) {
                    val secondItem = array.get(1)
                    if (secondItem is JSONArray) {
                        for (i in 0 until secondItem.length()) {
                            val item = secondItem.get(i)
                            if (item is String && item.isNotBlank()) {
                                list.add(item)
                            } else if (item is org.json.JSONObject) {
                                // e.g. DuckDuckGo: [{"phrase": "foo"}]
                                val phrase = item.optString("phrase", "")
                                if (phrase.isNotBlank()) list.add(phrase)
                            }
                        }
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val obj = org.json.JSONObject(trimmed)
                // Yahoo format or others
                if (obj.has("gcls")) {
                    val gcls = obj.getJSONObject("gcls")
                    val cl = gcls.optJSONArray("cl")
                    if (cl != null) {
                        for (i in 0 until cl.length()) {
                            val k = cl.getJSONObject(i).optString("k", "")
                            if (k.isNotBlank()) list.add(k)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore parse errors and return whatever succeeded
        }
        return list.distinct().take(8)
    }
}
