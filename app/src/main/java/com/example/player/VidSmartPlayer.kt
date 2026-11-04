package com.example.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.PreferencesManager
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class ScaleMode(val label: String, val modeInt: Int) {
    FIT("Fit Screen", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Crop / Zoom (Fill)", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    STRETCH("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL)
}

@OptIn(UnstableApi::class)
@Composable
fun VidSmartPlayer(
    videoUrl: String,
    title: String = "VidSmart Video",
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { PreferencesManager(context) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var isControlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var subtitleUrl by remember { mutableStateOf("") }
    var hasSubtitle by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Resize / Scale Aspect Ratio modes: Fit -> Zoom/Crop -> Stretch
    var currentScaleModeIndex by remember { mutableIntStateOf(0) }
    val scaleModes = listOf(ScaleMode.FIT, ScaleMode.FILL, ScaleMode.STRETCH)
    val currentScaleMode = scaleModes[currentScaleModeIndex]

    // Double tap feedback
    var showSeekForwardAnim by remember { mutableStateOf(false) }
    var showSeekBackwardAnim by remember { mutableStateOf(false) }

    // Volume & Brightness HUD gestures
    var volumeGestureLevel by remember { mutableIntStateOf(-1) }
    var brightnessGestureLevel by remember { mutableFloatStateOf(-1f) }

    // Auto-hide controls timer trigger
    var controlsTimeoutKey by remember { mutableIntStateOf(0) }

    // Universal Player View reference
    var playerViewInstance by remember { mutableStateOf<PlayerView?>(null) }

    // Build Universal ExoPlayer with OkHttp DataSource & Universal Demuxers
    val exoPlayer = remember {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")

        val upstreamDataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)

        // Universal extractors: supports MP4, MKV, WebM, FLV, AVI, TS, M3U8, MPD, MP3, AAC, FLAC
        val extractorsFactory = DefaultExtractorsFactory().apply {
            setConstantBitrateSeekingEnabled(true)
        }

        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(upstreamDataSourceFactory, extractorsFactory)

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = true
            }
    }

    // Set up media source
    LaunchedEffect(videoUrl, subtitleUrl) {
        errorMessage = null
        isBuffering = true
        val uri = if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://") || videoUrl.startsWith("content://")) {
            Uri.parse(videoUrl)
        } else {
            Uri.fromFile(File(videoUrl))
        }

        val mediaItemBuilder = MediaItem.Builder().setUri(uri)

        // Infer MIME type if possible for stream protocols
        when {
            videoUrl.contains(".m3u8", ignoreCase = true) -> mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            videoUrl.contains(".mpd", ignoreCase = true) -> mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
            videoUrl.contains(".webm", ignoreCase = true) -> mediaItemBuilder.setMimeType(MimeTypes.VIDEO_WEBM)
            videoUrl.contains(".mkv", ignoreCase = true) -> mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MATROSKA)
            videoUrl.contains(".mp4", ignoreCase = true) -> mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
        }

        if (subtitleUrl.isNotBlank()) {
            val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .setLanguage("ar")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
            hasSubtitle = true
        }

        val item = mediaItemBuilder.build()
        exoPlayer.setMediaItem(item)
        exoPlayer.prepare()

        // Check for resume position
        val savedPos = prefs.getPlaybackPosition(videoUrl)
        if (savedPos > 3000L) {
            exoPlayer.seekTo(savedPos)
        }
    }

    // Player listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                    errorMessage = null
                } else if (playbackState == Player.STATE_ENDED) {
                    prefs.clearPlaybackPosition(videoUrl)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMessage = "خطأ في تشغيل الفيديو: ${error.localizedMessage ?: "تنسيق غير مدعوم أو الرابط غير صالح"}"
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            val currentPos = exoPlayer.currentPosition
            if (currentPos > 0L) {
                prefs.savePlaybackPosition(videoUrl, currentPos)
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Progress updates
    LaunchedEffect(isPlaying) {
        while (true) {
            if (isPlaying) {
                currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                duration = exoPlayer.duration.coerceAtLeast(0L)
                if (currentPosition > 0L) {
                    prefs.savePlaybackPosition(videoUrl, currentPosition)
                }
            }
            delay(1000)
        }
    }

    LaunchedEffect(controlsTimeoutKey, isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying && !isLocked) {
            delay(4000)
            isControlsVisible = false
        }
    }

    fun toggleOrientation() {
        val act = activity ?: return
        if (isFullScreen) {
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isFullScreen = false
        } else {
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            isFullScreen = true
        }
    }

    fun enterPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity?.enterPictureInPictureMode(params)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Universal Android PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = currentScaleMode.modeInt
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    playerViewInstance = this
                }
            },
            update = { view ->
                view.resizeMode = currentScaleMode.modeInt
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked) {
                    if (isLocked) {
                        detectTapGestures(onTap = {
                            isControlsVisible = !isControlsVisible
                        })
                    } else {
                        detectTapGestures(
                            onTap = {
                                isControlsVisible = !isControlsVisible
                                controlsTimeoutKey++
                            },
                            onDoubleTap = { offset ->
                                val isRightSide = offset.x > (size.width / 2)
                                if (isRightSide) {
                                    val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                    exoPlayer.seekTo(newPos)
                                    showSeekForwardAnim = true
                                    showSeekBackwardAnim = false
                                } else {
                                    val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                    exoPlayer.seekTo(newPos)
                                    showSeekBackwardAnim = true
                                    showSeekForwardAnim = false
                                }
                                controlsTimeoutKey++
                            }
                        )
                    }
                }
                .pointerInput(isLocked) {
                    if (!isLocked) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                val isRight = offset.x > (size.width / 2)
                                if (isRight) {
                                    volumeGestureLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                } else {
                                    val lp = activity?.window?.attributes
                                    brightnessGestureLevel = if (lp != null && lp.screenBrightness >= 0f) {
                                        lp.screenBrightness
                                    } else 0.5f
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val isRight = change.position.x > (size.width / 2)
                                if (isRight) {
                                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val delta = if (dragAmount < 0) 1 else -1
                                    val newVol = (volumeGestureLevel + delta).coerceIn(0, maxVol)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                    volumeGestureLevel = newVol
                                } else {
                                    activity?.let { act ->
                                        val lp = act.window.attributes
                                        val newBright = (brightnessGestureLevel - (dragAmount / 500f)).coerceIn(0.05f, 1.0f)
                                        lp.screenBrightness = newBright
                                        act.window.attributes = lp
                                        brightnessGestureLevel = newBright
                                    }
                                }
                            },
                            onDragEnd = {
                                volumeGestureLevel = -1
                                brightnessGestureLevel = -1f
                            }
                        )
                    }
                }
        )

        // Loading / Buffering Spinner
        if (isBuffering && errorMessage == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0x99000000), CircleShape)
                    .padding(20.dp)
            ) {
                CircularProgressIndicator(
                    color = OrangePrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Error message card with Retry button
        if (errorMessage != null) {
            Surface(
                color = Color(0xDD211520),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            errorMessage = null
                            exoPlayer.prepare()
                            exoPlayer.play()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعادة المحاولة")
                    }
                }
            }
        }

        // Volume HUD indicator
        if (volumeGestureLevel >= 0) {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val percent = (volumeGestureLevel * 100) / maxVol
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xCC1A162B), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Volume",
                        tint = OrangePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "الصوت: $percent%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Brightness HUD indicator
        if (brightnessGestureLevel >= 0f) {
            val percent = (brightnessGestureLevel * 100).toInt()
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xCC1A162B), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.BrightnessMedium,
                        contentDescription = "Brightness",
                        tint = PurpleAccent,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "السطوع: $percent%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Double-tap Seek Animation Overlays
        LaunchedEffect(showSeekForwardAnim) {
            if (showSeekForwardAnim) {
                delay(700)
                showSeekForwardAnim = false
            }
        }
        LaunchedEffect(showSeekBackwardAnim) {
            if (showSeekBackwardAnim) {
                delay(700)
                showSeekBackwardAnim = false
            }
        }

        if (showSeekForwardAnim) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 48.dp)
                    .background(Color(0x99FF6D00), CircleShape)
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(36.dp))
                    Text("+10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (showSeekBackwardAnim) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 48.dp)
                    .background(Color(0x997C4DFF), CircleShape)
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(36.dp))
                    Text("-10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Locked Screen HUD button
        if (isLocked) {
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                IconButton(
                    onClick = {
                        isLocked = false
                        controlsTimeoutKey++
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Color(0xAA000000), CircleShape)
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = "Unlock", tint = OrangePrimary)
                }
            }
        }

        // Full Controls Overlay
        AnimatedVisibility(
            visible = isControlsVisible && !isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xCC000000),
                                Color(0x22000000),
                                Color(0xDD000000)
                            )
                        )
                    )
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )

                    // Aspect Ratio / Fit-Zoom toggle button
                    IconButton(onClick = {
                        currentScaleModeIndex = (currentScaleModeIndex + 1) % scaleModes.size
                    }) {
                        Icon(
                            Icons.Filled.AspectRatio,
                            contentDescription = "Aspect Ratio: ${currentScaleMode.label}",
                            tint = if (currentScaleMode != ScaleMode.FIT) OrangePrimary else Color.White
                        )
                    }

                    // Subtitle Button
                    IconButton(onClick = { showSubtitleDialog = true }) {
                        Icon(
                            Icons.Filled.ClosedCaption,
                            contentDescription = "Subtitles",
                            tint = if (hasSubtitle) OrangePrimary else Color.White
                        )
                    }

                    // Speed Selector
                    Box {
                        IconButton(onClick = { showSpeedMenu = true }) {
                            Icon(Icons.Filled.Speed, contentDescription = "Speed", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x", fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        playbackSpeed = speed
                                        exoPlayer.playbackParameters = PlaybackParameters(speed)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Picture-in-picture
                    IconButton(onClick = { enterPiP() }) {
                        Icon(
                            Icons.Filled.PictureInPictureAlt,
                            contentDescription = "PiP Mode",
                            tint = Color.White
                        )
                    }

                    // Lock Controls
                    IconButton(onClick = { isLocked = true }) {
                        Icon(Icons.Filled.LockOpen, contentDescription = "Lock", tint = Color.White)
                    }
                }

                // Center Play/Pause button
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(target)
                            controlsTimeoutKey++
                        },
                        modifier = Modifier.background(Color(0x55FFFFFF), CircleShape).size(48.dp)
                    ) {
                        Icon(Icons.Filled.Replay10, contentDescription = "-10s", tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                            controlsTimeoutKey++
                        },
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(listOf(OrangePrimary, PurpleAccent)),
                                CircleShape
                            )
                            .size(68.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                            exoPlayer.seekTo(target)
                            controlsTimeoutKey++
                        },
                        modifier = Modifier.background(Color(0x55FFFFFF), CircleShape).size(48.dp)
                    ) {
                        Icon(Icons.Filled.Forward10, contentDescription = "+10s", tint = Color.White)
                    }
                }

                // Bottom Controls (Seek bar, time, fullscreen)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    // Time row & Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() else 0f,
                            onValueChange = { newPos ->
                                exoPlayer.seekTo(newPos.toLong())
                                currentPosition = newPos.toLong()
                                controlsTimeoutKey++
                            },
                            valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                            colors = SliderDefaults.colors(
                                thumbColor = OrangePrimary,
                                activeTrackColor = OrangePrimary,
                                inactiveTrackColor = Color(0x66FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        Text(
                            text = formatTime(duration),
                            color = Color(0xCCFFFFFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { toggleOrientation() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullScreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = "Toggle Fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Subtitle Dialog
        if (showSubtitleDialog) {
            AlertDialog(
                onDismissRequest = { showSubtitleDialog = false },
                title = { Text("تحميل ملف ترجمة (SRT)", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("أدخل رابط مباشر لملف ترجمة بصيغة SRT لعرضها على الفيديو مباشرة:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = subtitleUrl,
                            onValueChange = { subtitleUrl = it },
                            label = { Text("رابط SRT") },
                            placeholder = { Text("https://example.com/subtitles.srt") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showSubtitleDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Text("تطبيق الترجمة")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        subtitleUrl = ""
                        hasSubtitle = false
                        showSubtitleDialog = false
                    }) {
                        Text("إلغاء / إيقاف")
                    }
                }
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0) return "00:00"
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
