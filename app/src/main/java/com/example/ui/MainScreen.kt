package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DownloadManager
import com.example.model.DownloadStatus
import com.example.player.VidSmartPlayer
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.PurpleAccent

enum class NavTab {
    BROWSER,
    DOWNLOADS,
    PLAYER,
    GUIDE
}

data class ActivePlayingVideo(
    val url: String,
    val title: String
)

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val downloadManager = remember { DownloadManager(context) }
    val downloads by downloadManager.downloads.collectAsState()

    var currentTab by remember { mutableStateOf(NavTab.BROWSER) }
    var activePlayingVideo by remember { mutableStateOf<ActivePlayingVideo?>(null) }

    val activeCount = remember(downloads) {
        downloads.count { it.status == DownloadStatus.DOWNLOADING }
    }

    // Playback transition handler
    fun startPlaying(url: String, title: String) {
        activePlayingVideo = ActivePlayingVideo(url = url, title = title)
    }

    if (activePlayingVideo != null) {
        BackHandler {
            activePlayingVideo = null
        }

        VidSmartPlayer(
            videoUrl = activePlayingVideo!!.url,
            title = activePlayingVideo!!.title,
            onClose = { activePlayingVideo = null }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().testTag("main_bottom_nav")
                ) {
                    // Browser Tab
                    NavigationBarItem(
                        selected = currentTab == NavTab.BROWSER,
                        onClick = { currentTab = NavTab.BROWSER },
                        icon = {
                            Icon(Icons.Filled.Language, contentDescription = "Browser")
                        },
                        label = { Text(stringResource(R.string.nav_browser), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OrangePrimary,
                            selectedTextColor = OrangePrimary,
                            indicatorColor = OrangePrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_item_browser")
                    )

                    // Downloads Tab
                    NavigationBarItem(
                        selected = currentTab == NavTab.DOWNLOADS,
                        onClick = { currentTab = NavTab.DOWNLOADS },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (activeCount > 0) {
                                        Badge(
                                            containerColor = OrangePrimary,
                                            contentColor = Color.White
                                        ) {
                                            Text("$activeCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = "Downloads")
                            }
                        },
                        label = { Text(stringResource(R.string.nav_downloads), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OrangePrimary,
                            selectedTextColor = OrangePrimary,
                            indicatorColor = OrangePrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_item_downloads")
                    )

                    // Player / Quick Sample Video Tab
                    NavigationBarItem(
                        selected = currentTab == NavTab.PLAYER,
                        onClick = { currentTab = NavTab.PLAYER },
                        icon = {
                            Icon(Icons.Filled.PlayCircleFilled, contentDescription = "Player")
                        },
                        label = { Text(stringResource(R.string.nav_player), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OrangePrimary,
                            selectedTextColor = OrangePrimary,
                            indicatorColor = OrangePrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_item_player")
                    )

                    // Guide Tab (Arabic Build & Install Guide)
                    NavigationBarItem(
                        selected = currentTab == NavTab.GUIDE,
                        onClick = { currentTab = NavTab.GUIDE },
                        icon = {
                            Icon(Icons.Filled.Help, contentDescription = "Guide")
                        },
                        label = { Text(stringResource(R.string.nav_help), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OrangePrimary,
                            selectedTextColor = OrangePrimary,
                            indicatorColor = OrangePrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_item_guide")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    NavTab.BROWSER -> {
                        BrowserScreen(
                            downloadManager = downloadManager,
                            onPlayVideo = { url, title -> startPlaying(url, title) }
                        )
                    }

                    NavTab.DOWNLOADS -> {
                        DownloadsScreen(
                            downloadManager = downloadManager,
                            onPlayVideo = { url, title -> startPlaying(url, title) }
                        )
                    }

                    NavTab.PLAYER -> {
                        // Direct Player hub (test stream or last downloaded video)
                        PlayerHubScreen(
                            downloads = downloads,
                            onPlayVideo = { url, title -> startPlaying(url, title) },
                            onGoToBrowser = { currentTab = NavTab.BROWSER }
                        )
                    }

                    NavTab.GUIDE -> {
                        GuideScreen()
                    }
                }
            }
        }
    }
}
