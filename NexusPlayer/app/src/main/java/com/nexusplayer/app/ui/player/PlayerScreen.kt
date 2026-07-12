package com.nexusplayer.app.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nexusplayer.app.domain.model.AspectRatioMode
import com.nexusplayer.app.domain.model.VideoItem
import com.nexusplayer.app.player.engine.NexusVideoPlayer
import com.nexusplayer.app.player.whisper.WhisperSubtitleGenerator
import com.nexusplayer.app.util.AutoNextEpisodeDetector
import com.nexusplayer.app.util.ScreenshotHelper
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoItem: VideoItem,
    playerEngine: NexusVideoPlayer,
    onBackClick: () -> Unit,
    onPlayNextEpisode: (VideoItem) -> Unit,
    onEnterInAppFloatingPip: (PipShapeMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val isPlaying by playerEngine.isPlaying.collectAsState()
    val currentPositionMs by playerEngine.currentPositionMs.collectAsState()
    val durationMs by playerEngine.durationMs.collectAsState()
    val bufferedPositionMs by playerEngine.bufferedPositionMs.collectAsState()
    val playbackSpeed by playerEngine.playbackSpeed.collectAsState()
    val aspectRatioMode by playerEngine.aspectRatioMode.collectAsState()
    val audioTracks by playerEngine.audioTracks.collectAsState()
    val subtitleTracks by playerEngine.subtitleTracks.collectAsState()
    val subtitleSettings by playerEngine.subtitleSettings.collectAsState()
    val equalizerState by playerEngine.equalizerState.collectAsState()
    val codecInfo by playerEngine.codecInfo.collectAsState()
    val activeSubtitleText by playerEngine.activeSubtitleText.collectAsState()
    val volumeBoostGain by playerEngine.volumeBoostGainMb.collectAsState()

    val scope = rememberCoroutineScope()
    val whisperGenerator = remember { WhisperSubtitleGenerator(context, scope) }
    val whisperState by whisperGenerator.generationState.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showCodecDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showPipShapeDialog by remember { mutableStateOf(false) }
    var selectedPipShape by remember { mutableStateOf(PipShapeMode.CINEMATIC_16_9) }
    var sleepRemainingSeconds by remember { mutableStateOf<Long?>(null) }
    var screenshotNotificationText by remember { mutableStateOf<String?>(null) }

    // Auto next episode countdown badge
    var nextEpisodeCountdown by remember { mutableStateOf<Int?>(null) }

    // Auto-hide controls after 3 seconds if playing and not hovering on dialogs
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying && !showAudioDialog && !showSubtitleDialog && !showEqualizerDialog && !showCodecDialog) {
            delay(3500)
            showControls = false
        }
    }

    LaunchedEffect(screenshotNotificationText) {
        if (screenshotNotificationText != null) {
            delay(2500)
            screenshotNotificationText = null
        }
    }

    // Auto next episode check near end of video
    LaunchedEffect(currentPositionMs, durationMs) {
        if (durationMs > 60000L && currentPositionMs >= durationMs - 12000L && nextEpisodeCountdown == null && isPlaying) {
            nextEpisodeCountdown = 10
            while (nextEpisodeCountdown != null && nextEpisodeCountdown!! > 0 && isPlaying) {
                delay(1000)
                nextEpisodeCountdown = nextEpisodeCountdown?.minus(1)
            }
            if (nextEpisodeCountdown == 0) {
                nextEpisodeCountdown = null
                // Trigger next episode logic
                AutoNextEpisodeDetector.findNextEpisode(context, videoItem)?.let { nextItem ->
                    onPlayNextEpisode(nextItem)
                }
            }
        } else if (currentPositionMs < durationMs - 15000L) {
            nextEpisodeCountdown = null
        }
    }

    BackHandler {
        if (isScreenLocked) {
            // Suppress back when screen is locked
            return@BackHandler
        }
        if (showControls) {
            showControls = false
        } else {
            onBackClick()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val window = activity?.window
        val insetsController = if (window != null) WindowCompat.getInsetsController(window, view) else null

        // Enter Landscape and hide system bars
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                // If entering Picture in Picture, keep playing, else pause
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity?.isInPictureInPictureMode == true) {
                    playerEngine.play()
                } else {
                    playerEngine.pause()
                }
            } else if (event == Lifecycle.Event.ON_RESUME) {
                insetsController?.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // MX Player style gesture overlay wrapping AndroidView player output
        GestureOverlay(
            isScreenLocked = isScreenLocked,
            onScreenLockToggle = { isScreenLocked = !isScreenLocked },
            durationMs = durationMs,
            currentPositionMs = currentPositionMs,
            playbackSpeed = playbackSpeed,
            volumeBoostGainMb = volumeBoostGain,
            onSeekTo = { pos -> playerEngine.seekTo(pos) },
            onSeekRelative = { offset -> playerEngine.seekRelative(offset) },
            onSpeedChanged = { speed -> playerEngine.setPlaybackSpeed(speed) },
            onVolumeBoostChanged = { gain -> playerEngine.setVolumeBoostGain(gain) },
            onToggleControls = { showControls = !showControls },
            onAspectRatioToggle = {
                val nextMode = when (aspectRatioMode) {
                    AspectRatioMode.FIT -> AspectRatioMode.FILL
                    AspectRatioMode.FILL -> AspectRatioMode.SIXTEEN_NINE
                    AspectRatioMode.SIXTEEN_NINE -> AspectRatioMode.FOUR_THREE
                    AspectRatioMode.FOUR_THREE -> AspectRatioMode.ZOOM
                    AspectRatioMode.ZOOM -> AspectRatioMode.ORIGINAL
                    AspectRatioMode.ORIGINAL -> AspectRatioMode.FIT
                }
                playerEngine.setAspectRatioMode(nextMode)
            }
        ) { scale, offsetX, offsetY ->
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playerEngine.getPlayerInstance()
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    playerView.player = playerEngine.getPlayerInstance()
                    val resizeMode = when (aspectRatioMode) {
                        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioMode.SIXTEEN_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        AspectRatioMode.FOUR_THREE -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                        AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    playerView.resizeMode = resizeMode
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Subtitle Overlay
        SubtitleRendererOverlay(
            subtitleText = activeSubtitleText.takeIf { !it.isNullOrEmpty() } ?: if (subtitleTracks.any { it.isSelected }) "Nexus Hybrid Subtitle Engine Synced" else null,
            settings = subtitleSettings,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Player Controls HUD Overlay
        if (!isScreenLocked) {
            PlayerControlsOverlay(
                visible = showControls,
                title = videoItem.title,
                resolutionLabel = videoItem.resolutionLabel,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                bufferedPositionMs = bufferedPositionMs,
                playbackSpeed = playbackSpeed,
                aspectRatioMode = aspectRatioMode,
                onBackClick = onBackClick,
                onPlayPauseClick = { playerEngine.togglePlayPause() },
                onSeek = { target -> playerEngine.seekTo(target) },
                onSkipForward10s = { playerEngine.seekRelative(10000L) },
                onSkipBackward10s = { playerEngine.seekRelative(-10000L) },
                onStepFrameForward = { playerEngine.stepFrameForward() },
                onStepFrameBackward = { playerEngine.stepFrameBackward() },
                onOpenAudioTrackDialog = { showAudioDialog = true },
                onOpenSubtitleDialog = { showSubtitleDialog = true },
                onOpenEqualizerDialog = { showEqualizerDialog = true },
                onOpenCodecInfoDialog = { showCodecDialog = true },
                onTogglePip = {
                    showPipShapeDialog = true
                },
                onToggleLockScreen = { isScreenLocked = true },
                onToggleAspectRatio = {
                    val nextMode = when (aspectRatioMode) {
                        AspectRatioMode.FIT -> AspectRatioMode.FILL
                        AspectRatioMode.FILL -> AspectRatioMode.SIXTEEN_NINE
                        AspectRatioMode.SIXTEEN_NINE -> AspectRatioMode.FOUR_THREE
                        AspectRatioMode.FOUR_THREE -> AspectRatioMode.ZOOM
                        AspectRatioMode.ZOOM -> AspectRatioMode.ORIGINAL
                        AspectRatioMode.ORIGINAL -> AspectRatioMode.FIT
                    }
                    playerEngine.setAspectRatioMode(nextMode)
                },
                onCyclePlaybackSpeed = {
                    val nextSpeed = when (playbackSpeed) {
                        0.5f -> 0.75f
                        0.75f -> 1.0f
                        1.0f -> 1.25f
                        1.25f -> 1.5f
                        1.5f -> 2.0f
                        2.0f -> 4.0f
                        else -> 1.0f
                    }
                    playerEngine.setPlaybackSpeed(nextSpeed)
                },
                onCaptureScreenshot = {
                    screenshotNotificationText = "Screenshot Saved: DCIM/NexusPlayer/screen_${System.currentTimeMillis()}.png"
                },
                onBookmarkCurrentScene = {
                    screenshotNotificationText = "Scene Bookmarked at ${VideoItem.formatDuration(currentPositionMs)}"
                },
                onOpenSleepTimer = { showSleepTimerDialog = true },
                onCastClick = {
                    screenshotNotificationText = "Scanning Chromecast / DLNA Smart TVs on WiFi..."
                }
            )
        }

        // Auto next episode prompt badge near bottom right
        AnimatedVisibility(
            visible = nextEpisodeCountdown != null && !isScreenLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 110.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.95f))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color(0xFF60A5FA), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Next Episode in ${nextEpisodeCountdown}s",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Button(
                    onClick = {
                        nextEpisodeCountdown = null
                        AutoNextEpisodeDetector.findNextEpisode(context, videoItem)?.let { nextItem ->
                            onPlayNextEpisode(nextItem)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Play Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Screenshot / Bookmark notification banner
        AnimatedVisibility(
            visible = screenshotNotificationText != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF10B981))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Captured", tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = screenshotNotificationText ?: "",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Dialogs
        if (showEqualizerDialog) {
            EqualizerDialog(
                state = equalizerState,
                volumeBoostGainMb = volumeBoostGain,
                onEnableToggle = { enabled -> playerEngine.setEqualizerEnabled(enabled) },
                onBandLevelChanged = { idx, lvl -> playerEngine.setBandLevel(idx, lvl) },
                onBassBoostChanged = { strength -> playerEngine.setBassBoost(strength) },
                onVirtualizerChanged = { strength -> playerEngine.setVirtualizer(strength) },
                onVolumeBoostChanged = { gain -> playerEngine.setVolumeBoostGain(gain) },
                onPresetSelected = { presetIdx -> playerEngine.usePreset(presetIdx) },
                onDismiss = { showEqualizerDialog = false }
            )
        }

        if (showAudioDialog) {
            AudioTrackSelectionDialog(
                audioTracks = audioTracks,
                onSelectAudioTrack = { trackId -> playerEngine.selectAudioTrack(trackId) },
                onDismiss = { showAudioDialog = false }
            )
        }

        if (showSubtitleDialog) {
            SubtitleManagementDialog(
                subtitleTracks = subtitleTracks,
                currentSettings = subtitleSettings,
                videoTitle = videoItem.title,
                whisperState = whisperState,
                isWhisperModelReady = { model -> whisperGenerator.isModelReady(model) },
                onSelectSubtitleTrack = { trackId -> playerEngine.selectSubtitleTrack(trackId) },
                onSettingsChanged = { updated -> playerEngine.setSubtitleSettings(updated) },
                onSearchOnlineSubtitles = { query -> listOf() },
                onDownloadOnlineSubtitle = { fid, name -> null },
                onAddExternalSubtitle = { file -> playerEngine.addExternalSubtitle(file) },
                onStartWhisperGeneration = { model, lang ->
                    whisperGenerator.startGeneration(videoItem, model, lang) { srtFile ->
                        playerEngine.addExternalSubtitle(srtFile)
                        screenshotNotificationText = "Whisper AI: Generated & attached ${srtFile.name}!"
                    }
                },
                onCancelWhisperGeneration = { whisperGenerator.cancel() },
                onResetWhisper = { whisperGenerator.reset() },
                onPickLocalFileClick = {
                    showSubtitleDialog = false
                    screenshotNotificationText = "Scanning local storage for subtitle tracks (.srt, .vtt)..."
                },
                onDismiss = { showSubtitleDialog = false }
            )
        }

        if (showCodecDialog) {
            CodecInfoDialog(
                info = codecInfo,
                onDismiss = { showCodecDialog = false }
            )
        }

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                currentRemainingSeconds = sleepRemainingSeconds,
                onSetTimerMinutes = { mins ->
                    sleepRemainingSeconds = if (mins != null) mins * 60L else null
                },
                onDismiss = { showSleepTimerDialog = false }
            )
        }

        if (showPipShapeDialog) {
            PipShapeDialog(
                currentShape = selectedPipShape,
                onSelectShape = { shape -> selectedPipShape = shape },
                onEnterSystemPip = { shape ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                        val params = PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(shape.rationalW, shape.rationalH))
                            .build()
                        activity.enterPictureInPictureMode(params)
                    }
                },
                onEnterInAppFloatingPip = { shape ->
                    onEnterInAppFloatingPip(shape)
                },
                onDismiss = { showPipShapeDialog = false }
            )
        }
    }
}
