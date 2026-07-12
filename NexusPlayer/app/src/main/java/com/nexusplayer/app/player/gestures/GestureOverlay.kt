package com.nexusplayer.app.player.gestures

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusplayer.app.domain.model.VideoItem
import kotlinx.coroutines.delay
import kotlin.math.abs

enum class GestureHudType {
    NONE, BRIGHTNESS, VOLUME, SEEK, SPEED, ZOOM
}

@Composable
fun GestureOverlay(
    isScreenLocked: Boolean,
    onScreenLockToggle: () -> Unit,
    durationMs: Long,
    currentPositionMs: Long,
    playbackSpeed: Float,
    volumeBoostGainMb: Int = 0,
    doubleTapSeekSeconds: Int = 10,
    hapticFeedbackEnabled: Boolean = true,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onVolumeBoostChanged: (Int) -> Unit = {},
    onToggleControls: () -> Unit,
    onAspectRatioToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (scale: Float, offsetX: Float, offsetY: Float) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    // Transform states (Zoom / Pan)
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // HUD Indicator state
    var activeHudType by remember { mutableStateOf(GestureHudType.NONE) }
    var hudText by remember { mutableStateOf("") }
    var hudProgress by remember { mutableFloatStateOf(0f) }

    // Seek scrubbing state
    var scrubSeekTargetMs by remember { mutableLongStateOf(currentPositionMs) }

    // Quick Tap / Seek Animation State
    var showDoubleTapSeekLeft by remember { mutableStateOf(false) }
    var showDoubleTapSeekRight by remember { mutableStateOf(false) }
    var doubleTapSeekText by remember { mutableStateOf("-${doubleTapSeekSeconds}s") }

    // Auto-hide HUD after 1.2 seconds of inactivity
    LaunchedEffect(activeHudType, hudText) {
        if (activeHudType != GestureHudType.NONE) {
            delay(1200)
            activeHudType = GestureHudType.NONE
        }
    }

    LaunchedEffect(showDoubleTapSeekLeft, showDoubleTapSeekRight) {
        if (showDoubleTapSeekLeft || showDoubleTapSeekRight) {
            delay(800)
            showDoubleTapSeekLeft = false
            showDoubleTapSeekRight = false
        }
    }

    fun triggerHaptic() {
        if (hapticFeedbackEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun adjustBrightness(deltaY: Float) {
        val activity = context as? Activity ?: return
        val window = activity.window ?: return
        val lp = window.attributes
        var currentBrightness = lp.screenBrightness
        if (currentBrightness < 0f) {
            currentBrightness = 0.5f // Default
        }
        val newBrightness = (currentBrightness - (deltaY / 600f)).coerceIn(0.01f, 1.0f)
        lp.screenBrightness = newBrightness
        window.attributes = lp

        activeHudType = GestureHudType.BRIGHTNESS
        hudProgress = newBrightness
        hudText = "Brightness: ${(newBrightness * 100).toInt()}%"
    }

    fun adjustVolume(deltaY: Float) {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (deltaY < 0 && currentVolume == maxVolume) {
            // Swiping up past 100% -> Activate VLC-style 200% Loudness Boost!
            val deltaGain = (-deltaY * 6f).toInt()
            val newGain = (volumeBoostGainMb + deltaGain).coerceIn(0, 2000)
            if (newGain != volumeBoostGainMb) {
                onVolumeBoostChanged(newGain)
                triggerHaptic()
            }
            activeHudType = GestureHudType.VOLUME
            val totalPerc = 100 + (newGain / 20) // 100% to 200%
            hudProgress = 1.0f
            hudText = "Volume Boost: ${totalPerc}% (DSP Gain)"
        } else if (deltaY > 0 && volumeBoostGainMb > 0) {
            // Swiping down when in volume boost -> reduce boost first before system volume
            val deltaGain = (-deltaY * 6f).toInt()
            val newGain = (volumeBoostGainMb + deltaGain).coerceIn(0, 2000)
            onVolumeBoostChanged(newGain)
            activeHudType = GestureHudType.VOLUME
            val totalPerc = 100 + (newGain / 20)
            hudProgress = 1.0f
            hudText = "Volume Boost: ${totalPerc}% (DSP Gain)"
        } else {
            val deltaVol = (-deltaY / 40f).toInt()
            val newVol = (currentVolume + deltaVol).coerceIn(0, maxVolume)
            if (newVol != currentVolume) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                triggerHaptic()
            }
            activeHudType = GestureHudType.VOLUME
            hudProgress = newVol.toFloat() / maxVolume.toFloat()
            hudText = "Volume: ${(hudProgress * 100).toInt()}%"
        }
    }

    fun handleHorizontalScrub(deltaX: Float) {
        val deltaMs = (deltaX * 120L).toLong()
        val newTarget = (scrubSeekTargetMs + deltaMs).coerceIn(0L, durationMs.coerceAtLeast(1L))
        scrubSeekTargetMs = newTarget
        activeHudType = GestureHudType.SEEK
        val diff = newTarget - currentPositionMs
        val sign = if (diff >= 0) "+" else "-"
        val formattedTarget = VideoItem.formatDuration(newTarget)
        val formattedTotal = VideoItem.formatDuration(durationMs)
        hudText = "$sign${VideoItem.formatDuration(abs(diff))} ($formattedTarget / $formattedTotal)"
        hudProgress = if (durationMs > 0) (newTarget.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Video render content with applied scale and offset
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        ) {
            content(scale, offsetX, offsetY)
        }

        // Lock Screen Floating Button & Unlock confirmation handler
        if (isScreenLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                triggerHaptic()
                                onScreenLockToggle()
                            }
                        )
                    },
                contentAlignment = Alignment.TopStart
            ) {
                Row(
                    modifier = Modifier
                        .padding(24.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Screen Locked. Double Tap anywhere to Unlock.",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            return@Box
        }

        // Active Gesture Touch Detector Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1f || scale > 1f) {
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = newScale
                            if (newScale == 1f) {
                                offsetX = 0f
                                offsetY = 0f
                                activeHudType = GestureHudType.NONE
                            } else {
                                offsetX += pan.x
                                offsetY += pan.y
                                activeHudType = GestureHudType.ZOOM
                                hudText = "Zoom: ${String.format("%.1f", newScale)}x"
                            }
                        }
                    }
                }
                .pointerInput(durationMs, currentPositionMs, playbackSpeed) {
                    var initialTouchX = 0f
                    var initialTouchY = 0f
                    var isVerticalDrag = false
                    var isHorizontalDrag = false
                    var dragStarted = false

                    detectTapGestures(
                        onTap = {
                            if (scale > 1f) {
                                // Reset zoom on single/double tap when zoomed
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                onToggleControls()
                            }
                        },
                        onDoubleTap = { offset: Offset ->
                            val screenW = size.width
                            if (offset.x < screenW * 0.35f) {
                                // Double tap left -> Rewind
                                triggerHaptic()
                                showDoubleTapSeekLeft = true
                                doubleTapSeekText = "-${doubleTapSeekSeconds}s"
                                onSeekRelative(-(doubleTapSeekSeconds * 1000L))
                            } else if (offset.x > screenW * 0.65f) {
                                // Double tap right -> Forward
                                triggerHaptic()
                                showDoubleTapSeekRight = true
                                doubleTapSeekText = "+${doubleTapSeekSeconds}s"
                                onSeekRelative(doubleTapSeekSeconds * 1000L)
                            } else {
                                // Double tap center -> Toggle aspect ratio or reset zoom
                                triggerHaptic()
                                if (scale > 1f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    onAspectRatioToggle()
                                }
                            }
                        },
                        onPress = {
                            // Reset scrubbing target to current exact position when press begins
                            scrubSeekTargetMs = currentPositionMs
                        }
                    )
                }
                // Separate pointerInput for drag gestures so tap & drag don't conflict
                .pointerInput(durationMs, currentPositionMs) {
                    var dragDirection: String? = null
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var startX = 0f

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val changes = event.changes
                            if (changes.size == 1) {
                                val change = changes.first()
                                if (change.pressed && change.previousPressed) {
                                    val delta = change.position - change.previousPosition
                                    totalDragX += delta.x
                                    totalDragY += delta.y

                                    if (dragDirection == null) {
                                        if (abs(totalDragX) > 20f || abs(totalDragY) > 20f) {
                                            startX = change.position.x
                                            dragDirection = if (abs(totalDragX) > abs(totalDragY)) "HORIZONTAL" else "VERTICAL"
                                            if (dragDirection == "HORIZONTAL") {
                                                scrubSeekTargetMs = currentPositionMs
                                            }
                                        }
                                    }

                                    if (dragDirection == "VERTICAL") {
                                        val screenW = size.width
                                        if (startX < screenW * 0.48f) {
                                            adjustBrightness(delta.y)
                                        } else {
                                            adjustVolume(delta.y)
                                        }
                                        change.consume()
                                    } else if (dragDirection == "HORIZONTAL") {
                                        handleHorizontalScrub(delta.x)
                                        change.consume()
                                    }
                                } else if (!change.pressed && change.previousPressed) {
                                    if (dragDirection == "HORIZONTAL") {
                                        onSeekTo(scrubSeekTargetMs)
                                        triggerHaptic()
                                    }
                                    dragDirection = null
                                    totalDragX = 0f
                                    totalDragY = 0f
                                }
                            } else if (changes.size == 2) {
                                // Two finger swipe for playback speed
                                val change = changes.first()
                                if (change.pressed && change.previousPressed) {
                                    val deltaY = change.position.y - change.previousPosition.y
                                    if (abs(deltaY) > 5f) {
                                        val deltaSpeed = (-deltaY / 200f)
                                        val newSpeed = (playbackSpeed + deltaSpeed).coerceIn(0.25f, 4.0f)
                                        onSpeedChanged(newSpeed)
                                        activeHudType = GestureHudType.SPEED
                                        hudText = "Speed: ${String.format("%.2f", newSpeed)}x"
                                    }
                                }
                            }
                        }
                    }
                }
        )

        // Double Tap Rewind Left Ripple / Banner
        AnimatedVisibility(
            visible = showDoubleTapSeekLeft,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 64.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FastRewind,
                    contentDescription = "Rewind",
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = doubleTapSeekText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Double Tap Forward Right Ripple / Banner
        AnimatedVisibility(
            visible = showDoubleTapSeekRight,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 64.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = "Forward",
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = doubleTapSeekText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Floating Center HUD Banner (Brightness, Volume, Seek Scrubbing, Speed, Zoom)
        AnimatedVisibility(
            visible = activeHudType != GestureHudType.NONE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.88f))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                val icon = when (activeHudType) {
                    GestureHudType.BRIGHTNESS -> Icons.Default.BrightnessMedium
                    GestureHudType.VOLUME -> Icons.Default.VolumeUp
                    GestureHudType.SEEK -> Icons.Default.FastForward
                    GestureHudType.SPEED -> Icons.Default.Speed
                    GestureHudType.ZOOM -> Icons.Default.BrightnessMedium
                    else -> Icons.Default.BrightnessMedium
                }
                Icon(
                    imageVector = icon,
                    contentDescription = hudText,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = hudText,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    if (activeHudType in listOf(GestureHudType.BRIGHTNESS, GestureHudType.VOLUME, GestureHudType.SEEK)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { hudProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .width(140.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF3B82F6),
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}
