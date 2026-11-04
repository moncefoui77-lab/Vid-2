package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.AdBlocker
import com.example.data.DownloadManager
import com.example.data.PreferencesManager
import com.example.data.SearchSuggestionRepository
import com.example.data.VideoDetector
import com.example.model.BrowserTab
import com.example.model.DetectedVideo
import com.example.model.SearchEngine
import com.example.model.WebShortcut
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.VidSmartGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private const val HOME_URL = "vidsmart://home"

private val DEFAULT_SHORTCUTS = listOf(
    WebShortcut("yt", "YouTube", "https://m.youtube.com", "Video Streaming", 0xFFFF0000),
    WebShortcut("dm", "Dailymotion", "https://www.dailymotion.com", "Video Platform", 0xFF0066DC),
    WebShortcut("vm", "Vimeo", "https://vimeo.com/watch", "HD Creative Videos", 0xFF1AB7EA),
    WebShortcut("ia", "Archive.org", "https://archive.org/details/movies", "Free Public Movies", 0xFF2C3E50),
    WebShortcut("rd", "Reddit Videos", "https://www.reddit.com/r/videos", "Community Clips", 0xFFFF4500),
    WebShortcut("tt", "TikTok", "https://www.tiktok.com", "Short Videos", 0xFF000000),
    WebShortcut("fb", "Facebook", "https://m.facebook.com/watch", "Social Videos", 0xFF1877F2),
    WebShortcut("wp", "Wikipedia", "https://en.m.wikipedia.org", "Knowledge Base", 0xFF607D8B)
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    downloadManager: DownloadManager,
    onPlayVideo: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }

    // Search Engine State
    var selectedEngine by remember {
        mutableStateOf(SearchEngine.fromId(prefs.getSearchEngineId()))
    }
    var showEnginePickerSheet by remember { mutableStateOf(false) }

    // Search Autocomplete Suggestions State
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearchFocused by remember { mutableStateOf(false) }

    // Tabs
    val tabs = remember {
        mutableStateListOf(
            BrowserTab(id = UUID.randomUUID().toString(), title = "Home", url = HOME_URL)
        )
    }
    var activeTabIndex by remember { mutableIntStateOf(0) }
    var showTabsSheet by remember { mutableStateOf(false) }

    // Web view state
    var urlInput by remember { mutableStateOf("") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Settings
    var isAdBlockActive by remember { mutableStateOf(prefs.isAdBlockEnabled()) }
    var isDesktopMode by remember { mutableStateOf(prefs.isDesktopMode()) }
    var blockedAdsSessionCount by remember { mutableIntStateOf(0) }

    // Video Sniffer
    val detectedVideos = remember { mutableStateListOf<DetectedVideo>() }
    var showDetectedSheet by remember { mutableStateOf(false) }
    var lastSniffedUrl by remember { mutableStateOf("") }

    val currentTab = tabs.getOrNull(activeTabIndex) ?: tabs.first()

    // Query debounce for search suggestions
    LaunchedEffect(urlInput, selectedEngine, isSearchFocused) {
        val trimmed = urlInput.trim()
        if (isSearchFocused && trimmed.length >= 2 && !trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            delay(250) // debounce typing
            val results = SearchSuggestionRepository.fetchSuggestions(trimmed, selectedEngine)
            suggestions = results
        } else {
            suggestions = emptyList()
        }
    }

    // Pulse animation for detected videos button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    fun navigateTo(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        focusManager.clearFocus()
        isSearchFocused = false
        suggestions = emptyList()

        val targetUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> selectedEngine.buildSearchUrl(trimmed)
        }

        urlInput = targetUrl
        tabs[activeTabIndex] = tabs[activeTabIndex].copy(url = targetUrl, title = targetUrl)

        if (targetUrl == HOME_URL) {
            detectedVideos.clear()
        } else {
            webViewInstance?.loadUrl(targetUrl)
        }
    }

    // Back handler
    BackHandler(enabled = isSearchFocused || (canGoBack && currentTab.url != HOME_URL)) {
        if (isSearchFocused) {
            isSearchFocused = false
            suggestions = emptyList()
            focusManager.clearFocus()
        } else if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            tabs[activeTabIndex] = tabs[activeTabIndex].copy(url = HOME_URL, title = "Home")
            urlInput = ""
            detectedVideos.clear()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top App Bar / Search Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home button
                        IconButton(
                            onClick = {
                                isSearchFocused = false
                                suggestions = emptyList()
                                tabs[activeTabIndex] = tabs[activeTabIndex].copy(url = HOME_URL, title = "Home")
                                urlInput = ""
                                detectedVideos.clear()
                            },
                            modifier = Modifier.testTag("browser_home_button")
                        ) {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = "Home",
                                tint = if (currentTab.url == HOME_URL) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Search Box with Engine Icon Selector Inside
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            placeholder = {
                                Text(
                                    stringResource(R.string.search_hint, selectedEngine.displayName),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            leadingIcon = {
                                // Search Engine Badge Icon (Tappable to change engine)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(selectedEngine.badgeColorHex),
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(34.dp)
                                        .clickable { showEnginePickerSheet = true }
                                        .testTag("search_engine_badge_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = selectedEngine.iconLetter,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (urlInput.isNotEmpty()) {
                                        IconButton(onClick = {
                                            urlInput = ""
                                            suggestions = emptyList()
                                        }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    IconButton(onClick = { navigateTo(urlInput) }) {
                                        Icon(
                                            Icons.Filled.Search,
                                            contentDescription = "Search",
                                            tint = OrangePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                navigateTo(urlInput)
                            }),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .onFocusChanged { focusState ->
                                    isSearchFocused = focusState.isFocused
                                }
                                .testTag("browser_search_input")
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Tabs button with badge
                        IconButton(
                            onClick = { showTabsSheet = true },
                            modifier = Modifier.testTag("browser_tabs_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = PurpleAccent,
                                        contentColor = Color.White
                                    ) {
                                        Text(text = "${tabs.size}")
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Tab, contentDescription = "Tabs", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    // Progress bar
                    if (isLoading && currentTab.url != HOME_URL) {
                        LinearProgressIndicator(
                            progress = { loadProgress / 100f },
                            color = OrangePrimary,
                            trackColor = Color.Transparent,
                            modifier = Modifier.fillMaxWidth().height(2.5.dp)
                        )
                    }

                    // Quick controls row (Back, Forward, Refresh, Search Engine Chip, Adblock badge, Desktop site)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { webViewInstance?.goBack() },
                                enabled = canGoBack,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { webViewInstance?.goForward() },
                                enabled = canGoForward,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Forward",
                                    tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { webViewInstance?.reload() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "Reload",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Status pills (Engine indicator, AdBlock and Desktop)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Search Engine quick switcher pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(selectedEngine.badgeColorHex).copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(selectedEngine.badgeColorHex).copy(alpha = 0.5f)),
                                modifier = Modifier.clickable { showEnginePickerSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedEngine.iconLetter,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(selectedEngine.badgeColorHex)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = selectedEngine.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Ad blocker toggle pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAdBlockActive) Color(0x334CAF50) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    isAdBlockActive = !isAdBlockActive
                                    prefs.setAdBlockEnabled(isAdBlockActive)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Shield,
                                        contentDescription = "AdBlock",
                                        tint = if (isAdBlockActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isAdBlockActive) "$blockedAdsSessionCount" else "Off",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isAdBlockActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Desktop mode toggle
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDesktopMode) Color(0x33FF6D00) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    isDesktopMode = !isDesktopMode
                                    prefs.setDesktopMode(isDesktopMode)
                                    webViewInstance?.let { wv ->
                                        val ws = wv.settings
                                        if (isDesktopMode) {
                                            ws.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                                        } else {
                                            ws.userAgentString = null
                                        }
                                        wv.reload()
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.DesktopWindows,
                                        contentDescription = "Desktop",
                                        tint = if (isDesktopMode) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Main View Area: Either Suggestions Overlay, Homepage Shortcuts, or WebView
            Box(modifier = Modifier.fillMaxSize()) {
                if (currentTab.url == HOME_URL) {
                    // Home page shortcuts
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(VidSmartGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.SmartDisplay,
                                    contentDescription = "VidSmart",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "VidSmart Web & Stream",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Text(
                                text = "Browse with ${selectedEngine.displayName} — auto video sniffer ready for 1-tap play & download!",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Search engine banner on home screen
                        item {
                            Card(
                                onClick = { showEnginePickerSheet = true },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(selectedEngine.badgeColorHex)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = selectedEngine.iconLetter,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "محرك البحث الافتراضي: ${selectedEngine.displayName}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = selectedEngine.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "تغيير",
                                        color = OrangePrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Top Video Sites",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )
                        }

                        item {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier.fillMaxWidth().height(220.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                userScrollEnabled = false
                            ) {
                                items(DEFAULT_SHORTCUTS) { shortcut ->
                                    Card(
                                        onClick = { navigateTo(shortcut.url) },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(shortcut.colorHex)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = shortcut.title.take(2).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = shortcut.title,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color(0x334CAF50), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Shield, contentDescription = null, tint = Color(0xFF4CAF50))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Smart Protection & Multi-Engine",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Intrusive ads blocked + Auto video stream sniffer + 11 Search Engines supported.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Actual WebView
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    allowFileAccess = true
                                    mediaPlaybackRequiresUserGesture = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    setSupportMultipleWindows(false)
                                    javaScriptCanOpenWindowsAutomatically = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                                    if (isDesktopMode) {
                                        userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                                    }
                                }

                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onVideoTagFound(src: String, poster: String) {
                                        if (src.isNotBlank() && VideoDetector.isPotentialVideoRequest(src)) {
                                            coroutineScope.launch(Dispatchers.Main) {
                                                val parsed = VideoDetector.parseVideo(src, url ?: "", title ?: "")
                                                if (parsed != null && detectedVideos.none { it.url == parsed.url }) {
                                                    detectedVideos.add(0, parsed)
                                                }
                                            }
                                        }
                                    }
                                }, "VidSmartDetector")

                                webViewClient = object : WebViewClient() {
                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): WebResourceResponse? {
                                        val reqUrl = request?.url?.toString() ?: return null

                                        // AdBlock Check
                                        if (isAdBlockActive && AdBlocker.isAd(reqUrl)) {
                                            coroutineScope.launch(Dispatchers.Main) {
                                                blockedAdsSessionCount++
                                                prefs.incrementBlockedAdsCount()
                                            }
                                            return AdBlocker.createEmptyResponse()
                                        }

                                        // Video Sniffing
                                        if (VideoDetector.isPotentialVideoRequest(reqUrl)) {
                                            val headers = request.requestHeaders ?: emptyMap()
                                            val pageUrl = view?.url ?: ""
                                            val pageTitle = view?.title ?: ""

                                            coroutineScope.launch(Dispatchers.Main) {
                                                val parsed = VideoDetector.parseVideo(reqUrl, pageUrl, pageTitle, headers)
                                                if (parsed != null && detectedVideos.none { it.url == parsed.url }) {
                                                    detectedVideos.add(0, parsed)
                                                    coroutineScope.launch {
                                                        val size = VideoDetector.probeContentLength(parsed.url)
                                                        if (size > 0) {
                                                            withContext(Dispatchers.Main) {
                                                                val idx = detectedVideos.indexOfFirst { it.id == parsed.id }
                                                                if (idx != -1) {
                                                                    detectedVideos[idx] = detectedVideos[idx].copy(sizeBytes = size)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        return super.shouldInterceptRequest(view, request)
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                        url?.let {
                                            if (it != lastSniffedUrl) {
                                                detectedVideos.clear()
                                                lastSniffedUrl = it
                                            }
                                            urlInput = it
                                            tabs[activeTabIndex] = tabs[activeTabIndex].copy(url = it)
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                        view?.title?.let { t ->
                                            tabs[activeTabIndex] = tabs[activeTabIndex].copy(title = t)
                                        }

                                        val script = """
                                            (function() {
                                                var vids = document.querySelectorAll('video');
                                                vids.forEach(function(v) {
                                                    if (v.src) { window.VidSmartDetector.onVideoTagFound(v.src, v.poster || ''); }
                                                    var sources = v.querySelectorAll('source');
                                                    sources.forEach(function(s) {
                                                        if (s.src) { window.VidSmartDetector.onVideoTagFound(s.src, v.poster || ''); }
                                                    });
                                                });
                                            })();
                                        """.trimIndent()
                                        view?.evaluateJavascript(script, null)
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        loadProgress = newProgress
                                        if (newProgress == 100) {
                                            isLoading = false
                                        }
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        super.onReceivedTitle(view, title)
                                        title?.let {
                                            tabs[activeTabIndex] = tabs[activeTabIndex].copy(title = it)
                                        }
                                    }
                                }

                                webViewInstance = this
                                if (currentTab.url != HOME_URL) {
                                    loadUrl(currentTab.url)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Autocomplete Suggestions Overlay (appears under search bar when focused & typing)
                if (isSearchFocused && suggestions.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "اقتراحات ${selectedEngine.displayName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(selectedEngine.badgeColorHex)
                                )
                                Text(
                                    text = "إغلاق",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable {
                                        isSearchFocused = false
                                        focusManager.clearFocus()
                                    }
                                )
                            }
                            HorizontalDivider()

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().height((suggestions.size * 48).coerceAtMost(280).dp)
                            ) {
                                items(suggestions) { itemText ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                navigateTo(itemText)
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = itemText,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        // Quick paste arrow to put in search bar without executing
                                        IconButton(
                                            onClick = { urlInput = itemText },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.NorthWest,
                                                contentDescription = "Put in search bar",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button for Detected Videos
        AnimatedVisibility(
            visible = detectedVideos.isNotEmpty() && currentTab.url != HOME_URL,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .scale(pulseScale)
                    .clip(RoundedCornerShape(32.dp))
                    .background(VidSmartGradient)
                    .clickable { showDetectedSheet = true }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .testTag("floating_detected_videos_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Detected Videos",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Videos (${detectedVideos.size})",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Search Engine Selector Bottom Sheet
        if (showEnginePickerSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showEnginePickerSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.search_engine_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "اختر محرك البحث المفضل لعمليات البحث والاقتراحات التلقائية",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showEnginePickerSheet = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        items(SearchEngine.entries) { engine ->
                            val isSelected = engine == selectedEngine
                            Card(
                                onClick = {
                                    selectedEngine = engine
                                    prefs.setSearchEngineId(engine.id)
                                    showEnginePickerSheet = false
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(engine.badgeColorHex).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) Color(engine.badgeColorHex) else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Engine icon badge
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(engine.badgeColorHex)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = engine.iconLetter,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = engine.displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (engine.suggestUrl != null) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0x224CAF50)
                                                ) {
                                                    Text(
                                                        text = "اقتراحات حية",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF4CAF50),
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = engine.description,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = Color(engine.badgeColorHex),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Detected Videos Bottom Sheet
        if (showDetectedSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showDetectedSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detected Streams (${detectedVideos.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showDetectedSheet = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (detectedVideos.isEmpty()) {
                        Text(
                            text = "No media detected on this page yet. Play the video or reload to capture the stream.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .padding(bottom = 16.dp)
                        ) {
                            items(detectedVideos) { video ->
                                DetectedVideoCard(
                                    video = video,
                                    onPlay = {
                                        showDetectedSheet = false
                                        onPlayVideo(video.url, video.title)
                                    },
                                    onDownload = {
                                        showDetectedSheet = false
                                        val ext = when (video.format) {
                                            com.example.model.VideoFormat.MP4 -> "mp4"
                                            com.example.model.VideoFormat.WEBM -> "webm"
                                            com.example.model.VideoFormat.HLS -> "mp4"
                                            com.example.model.VideoFormat.DASH -> "mp4"
                                            else -> "mp4"
                                        }
                                        downloadManager.enqueueDownload(video.url, video.title, ext)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tabs Bottom Sheet
        if (showTabsSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showTabsSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tabs (${tabs.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = {
                                val newId = UUID.randomUUID().toString()
                                tabs.add(BrowserTab(id = newId, title = "New Tab", url = HOME_URL))
                                activeTabIndex = tabs.lastIndex
                                urlInput = ""
                                detectedVideos.clear()
                                showTabsSheet = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Tab")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().height(320.dp).padding(bottom = 16.dp)
                    ) {
                        items(tabs.indices.toList()) { index ->
                            val tab = tabs[index]
                            val isSelected = index == activeTabIndex
                            Card(
                                onClick = {
                                    activeTabIndex = index
                                    urlInput = if (tab.url == HOME_URL) "" else tab.url
                                    showTabsSheet = false
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) OrangePrimary else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tab.title.ifBlank { "Untitled" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (tab.url == HOME_URL) "Home Screen" else tab.url,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (tabs.size > 1) {
                                        IconButton(onClick = {
                                            tabs.removeAt(index)
                                            if (activeTabIndex >= tabs.size) {
                                                activeTabIndex = tabs.lastIndex
                                            }
                                            val current = tabs[activeTabIndex]
                                            urlInput = if (current.url == HOME_URL) "" else current.url
                                        }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Close Tab", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetectedVideoCard(
    video: DetectedVideo,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Title & DRM badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Badges row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = OrangePrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = video.format.label,
                                color = OrangePrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PurpleAccent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = video.quality,
                                color = PurpleAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (video.sizeBytes > 0) {
                            Text(
                                text = DownloadManager.formatBytes(video.sizeBytes),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // DRM Protected warning or Action buttons
            if (video.isDrm) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x33E53935),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DRM Protected Stream: This video is protected by DRM encryption (Widevine) and cannot be captured or downloaded.",
                            fontSize = 11.sp,
                            color = Color(0xFFFFCDD2)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPlay,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("play_detected_video_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = OrangePrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play", color = MaterialTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = onDownload,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        modifier = Modifier.weight(1f).testTag("download_detected_video_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
