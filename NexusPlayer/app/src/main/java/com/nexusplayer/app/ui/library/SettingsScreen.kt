package com.nexusplayer.app.ui.library

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusplayer.app.domain.model.DecoderPriority
import com.nexusplayer.app.domain.model.PlayerSettings

@Composable
fun SettingsScreen(
    settings: PlayerSettings,
    onSettingsChanged: (PlayerSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDecoderMenu by remember { mutableStateOf(false) }
    var showDoubleTapMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Engine & Decoder Settings",
            color = Color(0xFF60A5FA),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Decoder Priority Dropdown Card
        Box {
            SettingsCard(
                title = "Hardware Acceleration Priority",
                subtitle = settings.decoderPriority.label,
                onClick = { showDecoderMenu = true }
            )
            DropdownMenu(
                expanded = showDecoderMenu,
                onDismissRequest = { showDecoderMenu = false },
                modifier = Modifier.background(Color(0xFF1E293B))
            ) {
                DecoderPriority.values().forEach { priority ->
                    DropdownMenuItem(
                        text = { Text(priority.label, color = Color.White) },
                        onClick = {
                            showDecoderMenu = false
                            onSettingsChanged(settings.copy(decoderPriority = priority))
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggleCard(
            title = "Background Audio / Video Playback",
            subtitle = "Keep playing video sound in background or notification bar when minimized",
            checked = settings.backgroundPlaybackEnabled,
            onCheckedChange = { chk -> onSettingsChanged(settings.copy(backgroundPlaybackEnabled = chk)) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggleCard(
            title = "Auto Picture-in-Picture (PiP)",
            subtitle = "Automatically switch to mini PiP floating window when pressing Home during playback",
            checked = settings.autoEnterPip,
            onCheckedChange = { chk -> onSettingsChanged(settings.copy(autoEnterPip = chk)) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Touch Gestures & Haptic Controls",
            color = Color(0xFF60A5FA),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Box {
            SettingsCard(
                title = "Double Tap Seek Duration",
                subtitle = "Fast-forward or rewind by ${settings.doubleTapSeekSeconds} seconds per double tap",
                onClick = { showDoubleTapMenu = true }
            )
            DropdownMenu(
                expanded = showDoubleTapMenu,
                onDismissRequest = { showDoubleTapMenu = false },
                modifier = Modifier.background(Color(0xFF1E293B))
            ) {
                listOf(5, 10, 15, 30).forEach { sec ->
                    DropdownMenuItem(
                        text = { Text("$sec Seconds", color = Color.White) },
                        onClick = {
                            showDoubleTapMenu = false
                            onSettingsChanged(settings.copy(doubleTapSeekSeconds = sec))
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggleCard(
            title = "Vertical Swipe for Brightness & Volume",
            subtitle = "Swipe vertically on left side for brightness, right side for volume",
            checked = settings.gestureBrightnessEnabled && settings.gestureVolumeEnabled,
            onCheckedChange = { chk ->
                onSettingsChanged(settings.copy(gestureBrightnessEnabled = chk, gestureVolumeEnabled = chk))
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggleCard(
            title = "Horizontal Swipe Scrubbing",
            subtitle = "Swipe horizontally to smoothly seek across timestamps with live previews",
            checked = settings.gestureSeekScrubbingEnabled,
            onCheckedChange = { chk -> onSettingsChanged(settings.copy(gestureSeekScrubbingEnabled = chk)) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggleCard(
            title = "Haptic Vibration Feedback",
            subtitle = "Trigger subtle haptic pulse on button taps and gesture locks",
            checked = settings.hapticFeedbackEnabled,
            onCheckedChange = { chk -> onSettingsChanged(settings.copy(hapticFeedbackEnabled = chk)) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Smart Automation & Storage",
            color = Color(0xFF60A5FA),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggleCard(
            title = "Auto-Next Episode Detection",
            subtitle = "Automatically detect TV show series order (S01E04 -> S01E05) and prompt countdown",
            checked = settings.autoNextEpisodeEnabled,
            onCheckedChange = { chk -> onSettingsChanged(settings.copy(autoNextEpisodeEnabled = chk)) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggleCard(
            title = "Remember Video Resume Position",
            subtitle = "Save exact playback timestamp per file and auto-resume on re-opening",
            checked = settings.rememberResumePosition,
            onCheckedChange = { chk -> onSettingsChanged(settings.copy(rememberResumePosition = chk)) }
        )
    }
}

@Composable
fun SettingsCard(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF60A5FA))
    }
}

@Composable
fun SettingsToggleCard(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF3B82F6),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF0F172A)
            )
        )
    }
}
