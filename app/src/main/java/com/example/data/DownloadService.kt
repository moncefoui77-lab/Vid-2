package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var observerJob: Job? = null

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private var lastCompletedIds = mutableSetOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val downloadId = intent?.getStringExtra(EXTRA_DOWNLOAD_ID)

        val downloadManager = DownloadManager.getInstance(applicationContext)

        when (action) {
            ACTION_PAUSE -> {
                downloadId?.let { downloadManager.pauseDownload(it) }
            }
            ACTION_RESUME -> {
                downloadId?.let { downloadManager.resumeDownload(it) }
            }
            ACTION_CANCEL -> {
                downloadId?.let { downloadManager.deleteDownload(it, deleteLocalFile = true) }
            }
        }

        startObservingDownloads()
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VidSmart Video Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time progress for downloading videos"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startObservingDownloads() {
        if (observerJob != null) return

        observerJob = serviceScope.launch {
            val downloadManager = DownloadManager.getInstance(applicationContext)
            downloadManager.downloads.collect { downloads ->
                val activeDownloads = downloads.filter { it.status == DownloadStatus.DOWNLOADING }
                val completedDownloads = downloads.filter { it.status == DownloadStatus.COMPLETED }

                // Check for newly completed downloads to show finished notification
                completedDownloads.forEach { completed ->
                    if (!lastCompletedIds.contains(completed.id) && completed.completedAt != null) {
                        lastCompletedIds.add(completed.id)
                        showCompletionNotification(completed)
                    }
                }

                if (activeDownloads.isNotEmpty()) {
                    val primaryItem = activeDownloads.first()
                    val totalActive = activeDownloads.size
                    showProgressNotification(primaryItem, totalActive)
                } else {
                    // No active downloads running, stop foreground service
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                    observerJob?.cancel()
                    observerJob = null
                    stopSelf()
                }
            }
        }
    }

    private fun showProgressNotification(item: DownloadItem, totalActiveCount: Int) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_PAUSE
            putExtra(EXTRA_DOWNLOAD_ID, item.id)
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            item.id.hashCode() + 1,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL
            putExtra(EXTRA_DOWNLOAD_ID, item.id)
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            item.id.hashCode() + 2,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val percent = (item.progress * 100).toInt()
        val downloadedStr = DownloadManager.formatBytes(item.downloadedBytes)
        val totalStr = if (item.totalBytes > 0) DownloadManager.formatBytes(item.totalBytes) else "..."
        val speedStr = DownloadManager.formatSpeed(item.speedBytesPerSec)

        val subtext = if (totalActiveCount > 1) {
            "Downloading 1 of $totalActiveCount files"
        } else {
            "Downloading video"
        }

        val contentText = "$downloadedStr / $totalStr ($percent%) • $speedStr"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(item.title)
            .setContentText(contentText)
            .setSubText(subtext)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent, item.totalBytes <= 0)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)

        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID_PROGRESS,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID_PROGRESS, notification)
        }
    }

    private fun showCompletionNotification(item: DownloadItem) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            item.id.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download Complete")
            .setContentText("${item.title} saved to Movies/VidSmart")
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = NOTIFICATION_ID_BASE_COMPLETED + (item.id.hashCode() % 1000)
        notificationManager.notify(notificationId, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        observerJob?.cancel()
        observerJob = null
    }

    companion object {
        const val CHANNEL_ID = "vidsmart_downloads_channel"
        const val NOTIFICATION_ID_PROGRESS = 1001
        const val NOTIFICATION_ID_BASE_COMPLETED = 2000

        const val ACTION_START = "com.example.action.START_DOWNLOAD"
        const val ACTION_PAUSE = "com.example.action.PAUSE_DOWNLOAD"
        const val ACTION_RESUME = "com.example.action.RESUME_DOWNLOAD"
        const val ACTION_CANCEL = "com.example.action.CANCEL_DOWNLOAD"

        const val EXTRA_DOWNLOAD_ID = "extra_download_id"

        fun startService(context: Context, downloadId: String? = null) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
                if (downloadId != null) {
                    putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
