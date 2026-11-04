package com.example.model

enum class SearchEngine(
    val id: String,
    val displayName: String,
    val shortName: String,
    val searchUrl: String,      // %s placeholder for query
    val suggestUrl: String?,    // URL for fetch suggestions, or null
    val homeUrl: String,
    val iconLetter: String,
    val badgeColorHex: Long,
    val description: String
) {
    GOOGLE(
        id = "google",
        displayName = "Google",
        shortName = "G",
        searchUrl = "https://www.google.com/search?q=%s",
        suggestUrl = "https://suggestqueries.google.com/complete/search?client=chrome&q=%s",
        homeUrl = "https://www.google.com",
        iconLetter = "G",
        badgeColorHex = 0xFF4285F4,
        description = "الأكثر شمولاً وسرعة مع نتائج دقيقة"
    ),
    BING(
        id = "bing",
        displayName = "Microsoft Bing",
        shortName = "B",
        searchUrl = "https://www.bing.com/search?q=%s",
        suggestUrl = "https://api.bing.com/osjson.aspx?query=%s",
        homeUrl = "https://www.bing.com",
        iconLetter = "B",
        badgeColorHex = 0xFF008373,
        description = "بحث مايكروسوفت وميزات الوسائط"
    ),
    DUCKDUCKGO(
        id = "duckduckgo",
        displayName = "DuckDuckGo",
        shortName = "DDG",
        searchUrl = "https://duckduckgo.com/?q=%s",
        suggestUrl = "https://duckduckgo.com/ac/?q=%s&type=list",
        homeUrl = "https://duckduckgo.com",
        iconLetter = "🦆",
        badgeColorHex = 0xFFDE5833,
        description = "خصوصية فائقة بدون تتبع أو إعلانات موجهة"
    ),
    YOUTUBE(
        id = "youtube",
        displayName = "YouTube Videos",
        shortName = "YT",
        searchUrl = "https://m.youtube.com/results?search_query=%s",
        suggestUrl = "https://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=%s",
        homeUrl = "https://m.youtube.com",
        iconLetter = "▶",
        badgeColorHex = 0xFFFF0000,
        description = "بحث مباشر في ملايين الفيديوهات وقنوات المحتوى"
    ),
    YAHOO(
        id = "yahoo",
        displayName = "Yahoo",
        shortName = "Y!",
        searchUrl = "https://search.yahoo.com/search?p=%s",
        suggestUrl = "https://search.yahoo.com/sugg/gcl?output=json&command=%s",
        homeUrl = "https://www.yahoo.com",
        iconLetter = "Y!",
        badgeColorHex = 0xFF6001D2,
        description = "بحث ياهو الكلاسيكي مع الأخبار والطقس"
    ),
    YANDEX(
        id = "yandex",
        displayName = "Yandex",
        shortName = "Y",
        searchUrl = "https://yandex.com/search/?text=%s",
        suggestUrl = "https://suggest.yandex.com/suggest-ya.cgi?v=4&part=%s",
        homeUrl = "https://yandex.com",
        iconLetter = "Я",
        badgeColorHex = 0xFFFF0000,
        description = "قوي جداً في البحث عن الصور ومصادر الفيديو"
    ),
    BRAVE(
        id = "brave",
        displayName = "Brave Search",
        shortName = "Br",
        searchUrl = "https://search.brave.com/search?q=%s",
        suggestUrl = "https://search.brave.com/api/suggest?q=%s",
        homeUrl = "https://search.brave.com",
        iconLetter = "🦁",
        badgeColorHex = 0xFFFF5500,
        description = "محرك بحث مستقل بدون تتبع يعتمد على فهرس خاص"
    ),
    ECOSIA(
        id = "ecosia",
        displayName = "Ecosia",
        shortName = "Eco",
        searchUrl = "https://www.ecosia.org/search?q=%s",
        suggestUrl = "https://ac.ecosia.org/autocomplete?q=%s&type=list",
        homeUrl = "https://www.ecosia.org",
        iconLetter = "🌱",
        badgeColorHex = 0xFF00897B,
        description = "محرك يزرع الأشجار مع كل عملية بحث"
    ),
    QWANT(
        id = "qwant",
        displayName = "Qwant",
        shortName = "Q",
        searchUrl = "https://www.qwant.com/?q=%s",
        suggestUrl = "https://api.qwant.com/v3/suggest?q=%s",
        homeUrl = "https://www.qwant.com",
        iconLetter = "Q",
        badgeColorHex = 0xFF5C6BC0,
        description = "المحرك الأوروبي الحامي للخصوصية والبيانات الشخصية"
    ),
    WIKIPEDIA(
        id = "wikipedia",
        displayName = "Wikipedia",
        shortName = "W",
        searchUrl = "https://en.m.wikipedia.org/w/index.php?search=%s",
        suggestUrl = "https://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=%s",
        homeUrl = "https://en.m.wikipedia.org",
        iconLetter = "W",
        badgeColorHex = 0xFF37474F,
        description = "الموسوعة العالمية الحرة بكافة اللغات"
    ),
    ARCHIVE(
        id = "archive",
        displayName = "Internet Archive",
        shortName = "IA",
        searchUrl = "https://archive.org/search?query=%s",
        suggestUrl = null,
        homeUrl = "https://archive.org",
        iconLetter = "🏛",
        badgeColorHex = 0xFF455A64,
        description = "أرشيف الإنترنت لملايين الأفلام والكتب المجانية"
    );

    fun buildSearchUrl(query: String): String {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        return String.format(searchUrl, encoded)
    }

    companion object {
        fun fromId(id: String): SearchEngine {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: GOOGLE
        }
    }
}
