package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DownloadManager
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.PurpleAccent
import java.io.File

@Composable
fun DownloadsScreen(
    downloadManager: DownloadManager,
    onPlayVideo: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val downloads by downloadManager.downloads.collectAsState()
    var selectedFilterIndex by remember { mutableIntStateOf(0) }

    // Dialog state for renaming
    var renameItem by remember { mutableStateOf<DownloadItem?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val activeDownloads = remember(downloads) {
        downloads.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PAUSED || it.status == DownloadStatus.QUEUED }
    }
    val completedDownloads = remember(downloads) {
        downloads.filter { it.status == DownloadStatus.COMPLETED }
    }

    val displayedList = when (selectedFilterIndex) {
        1 -> activeDownloads
        2 -> completedDownloads
        else -> downloads
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Downloads",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${downloads.size} files • ${activeDownloads.size} active • Public Movies/VidSmart",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Filter Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedFilterIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.background,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedFilterIndex]),
                    color = OrangePrimary
                )
            }
        ) {
            listOf("All (${downloads.size})", "Active (${activeDownloads.size})", "Completed (${completedDownloads.size})").forEachIndexed { index, title ->
                Tab(
                    selected = selectedFilterIndex == index,
                    onClick = { selectedFilterIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedFilterIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedFilterIndex == index) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (displayedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FileDownload,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Downloads Found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "When you browse web pages and tap the detected video button, your downloads will appear here with pause, resume, and playback support.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedList, key = { it.id }) { item ->
                    DownloadItemCard(
                        item = item,
                        onPlay = { onPlayVideo(item.localFilePath, item.title) },
                        onPause = { downloadManager.pauseDownload(item.id) },
                        onResume = { downloadManager.resumeDownload(item.id) },
                        onDelete = { downloadManager.deleteDownload(item.id, deleteLocalFile = true) },
                        onRename = {
                            renameItem = item
                            renameInput = item.title
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Rename Dialog
        renameItem?.let { currentItem ->
            AlertDialog(
                onDismissRequest = { renameItem = null },
                title = { Text("Rename Video", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Enter new file name:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (renameInput.isNotBlank()) {
                                downloadManager.renameDownload(currentItem.id, renameInput.trim())
                            }
                            renameItem = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renameItem = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun DownloadItemCard(
    item: DownloadItem,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().testTag("download_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (item.status) {
                                DownloadStatus.COMPLETED -> Color(0x334CAF50)
                                DownloadStatus.DOWNLOADING -> OrangePrimary.copy(alpha = 0.2f)
                                DownloadStatus.PAUSED -> PurpleAccent.copy(alpha = 0.2f)
                                else -> Color(0x33F44336)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.status) {
                            DownloadStatus.COMPLETED -> Icons.Filled.FileDownloadDone
                            DownloadStatus.DOWNLOADING -> Icons.Filled.FileDownload
                            DownloadStatus.PAUSED -> Icons.Filled.Pause
                            else -> Icons.Filled.ErrorOutline
                        },
                        contentDescription = null,
                        tint = when (item.status) {
                            DownloadStatus.COMPLETED -> Color(0xFF4CAF50)
                            DownloadStatus.DOWNLOADING -> OrangePrimary
                            DownloadStatus.PAUSED -> PurpleAccent
                            else -> Color(0xFFF44336)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and progress info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val statusText = when (item.status) {
                        DownloadStatus.COMPLETED -> {
                            val file = File(item.localFilePath)
                            val sizeStr = if (file.exists()) DownloadManager.formatBytes(file.length()) else DownloadManager.formatBytes(item.totalBytes)
                            "Completed • $sizeStr • Saved to Movies/VidSmart"
                        }
                        DownloadStatus.DOWNLOADING -> {
                            val dlStr = DownloadManager.formatBytes(item.downloadedBytes)
                            val totalStr = if (item.totalBytes > 0) DownloadManager.formatBytes(item.totalBytes) else "..."
                            val speedStr = DownloadManager.formatSpeed(item.speedBytesPerSec)
                            "$dlStr / $totalStr • $speedStr"
                        }
                        DownloadStatus.PAUSED -> "Paused • ${DownloadManager.formatBytes(item.downloadedBytes)}"
                        DownloadStatus.FAILED -> "Failed: ${item.errorMessage ?: "Unknown error"}"
                        DownloadStatus.QUEUED -> "Queued..."
                    }

                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Primary Quick Action (Play if completed, Pause/Resume if active)
                if (item.status == DownloadStatus.COMPLETED) {
                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .background(OrangePrimary, CircleShape)
                            .size(36.dp)
                            .testTag("play_download_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                } else if (item.status == DownloadStatus.DOWNLOADING) {
                    IconButton(
                        onClick = onPause,
                        modifier = Modifier.size(36.dp).testTag("pause_download_button")
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = "Pause", tint = OrangePrimary)
                    }
                } else if (item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.FAILED) {
                    IconButton(
                        onClick = onResume,
                        modifier = Modifier.size(36.dp).testTag("resume_download_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Resume", tint = PurpleAccent)
                    }
                }
            }

            // Progress bar if downloading or paused
            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { item.progress },
                    color = OrangePrimary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                )
            }

            // Bottom action row (Rename, Delete)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onRename) {
                    Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rename", fontSize = 12.sp)
                }

                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", color = Color(0xFFEF5350), fontSize = 12.sp)
                }
            }
        }
    }
}
