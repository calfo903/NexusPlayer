package com.nexusplayer.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusplayer.app.player.whisper.WhisperGenerationState
import com.nexusplayer.app.player.whisper.WhisperModelType

@Composable
fun WhisperSubtitleCard(
    state: WhisperGenerationState,
    isModelDownloaded: (WhisperModelType) -> Boolean,
    onStartGeneration: (WhisperModelType, String) -> Unit,
    onCancelGeneration: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedModel by remember { mutableStateOf(WhisperModelType.BASE) }
    var selectedLanguage by remember { mutableStateOf("en") }
    var showModelMenu by remember { mutableStateOf(false) }
    var showLangMenu by remember { mutableStateOf(false) }

    val languages = listOf(
        "en" to "English (Default)",
        "es" to "Spanish / Español",
        "fr" to "French / Français",
        "de" to "German / Deutsch",
        "ja" to "Japanese / 日本語"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Whisper AI", tint = Color(0xFF60A5FA), modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "On-Device Whisper AI Engine",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Zero Cloud Latency · 100% Offline Privacy",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            if (state !is WhisperGenerationState.Idle && state !is WhisperGenerationState.Completed) {
                IconButton(onClick = onCancelGeneration) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFFEF4444))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (state) {
            is WhisperGenerationState.Idle -> {
                // Model Picker
                Text(text = "Select Neural Model:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF334155))
                            .clickable { showModelMenu = true }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(selectedModel.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(selectedModel.description, color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        }
                        val ready = isModelDownloaded(selectedModel)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (ready) Color(0xFF10B981) else Color(0xFFF59E0B))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (ready) "READY" else "DOWNLOAD",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        WhisperModelType.values().forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(model.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("${model.sizeBytes / (1024 * 1024)} MB", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    selectedModel = model
                                    showModelMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Language Picker
                Text(text = "Target Audio Language:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF334155))
                            .clickable { showLangMenu = true }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val langLabel = languages.firstOrNull { it.first == selectedLanguage }?.second ?: "English"
                        Text(langLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Icon(Icons.Default.Memory, contentDescription = null, tint = Color(0xFF60A5FA))
                    }
                    DropdownMenu(
                        expanded = showLangMenu,
                        onDismissRequest = { showLangMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        languages.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = Color.White) },
                                onClick = {
                                    selectedLanguage = code
                                    showLangMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { onStartGeneration(selectedModel, selectedLanguage) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Subtitles Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            is WhisperGenerationState.DownloadingModel -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF60A5FA), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Downloading Whisper Model (${state.model.displayName})...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${(state.progressPercentage).toInt()}% · ${state.formattedSpeed}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { state.progressPercentage / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = Color(0xFF3B82F6),
                    trackColor = Color(0xFF334155)
                )
            }

            is WhisperGenerationState.ExtractingAudio -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = "Extracting", tint = Color(0xFF10B981), modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Extracting & Resampling 16kHz PCM Audio...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Processed ${state.extractedDurationMs}s of ${state.totalDurationMs}s audio stream", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { state.progressPercentage / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFF334155)
                )
            }

            is WhisperGenerationState.Transcribing -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF3B82F6), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Transcribing Audio with Whisper (${state.model.displayName})...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Generated ${state.segmentsGenerated} speech segments (${(state.progressPercentage).toInt()}%)", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { state.progressPercentage / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = Color(0xFF3B82F6),
                    trackColor = Color(0xFF334155)
                )

                if (state.latestSegment != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Live Transcript Stream · ${state.latestSegment.formattedTimestamp}",
                                color = Color(0xFF60A5FA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.latestSegment.text,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            is WhisperGenerationState.Completed -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Subtitles Successfully Generated & Synced!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Attached ${state.totalSegments} exact SubRip (.srt) segments in ${(state.elapsedTimeMs / 1000f)}s", color = Color(0xFF34D399), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("SRT File Location on Disk:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(state.srtFile.absolutePath, color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Text("Generate Another / Change Model", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            is WhisperGenerationState.Error -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Transcription Interrupted", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(state.message, color = Color(0xFFF87171), fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Reset & Try Again", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
