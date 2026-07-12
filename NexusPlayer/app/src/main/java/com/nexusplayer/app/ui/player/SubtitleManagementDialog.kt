package com.nexusplayer.app.ui.player

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.Dialog
import com.nexusplayer.app.data.remote.model.SubtitleItemData
import com.nexusplayer.app.domain.model.SubtitleSettings
import com.nexusplayer.app.domain.model.SubtitleTrack
import com.nexusplayer.app.player.whisper.WhisperGenerationState
import com.nexusplayer.app.player.whisper.WhisperModelType
import java.io.File

@Composable
fun SubtitleManagementDialog(
    subtitleTracks: List<SubtitleTrack>,
    currentSettings: SubtitleSettings,
    videoTitle: String,
    whisperState: WhisperGenerationState = WhisperGenerationState.Idle,
    isWhisperModelReady: (WhisperModelType) -> Boolean = { false },
    onSelectSubtitleTrack: (String?) -> Unit,
    onSettingsChanged: (SubtitleSettings) -> Unit,
    onSearchOnlineSubtitles: suspend (String) -> List<SubtitleItemData>,
    onDownloadOnlineSubtitle: suspend (Int, String) -> File?,
    onAddExternalSubtitle: (File) -> Unit,
    onStartWhisperGeneration: (WhisperModelType, String) -> Unit = { _, _ -> },
    onCancelWhisperGeneration: () -> Unit = {},
    onResetWhisper: () -> Unit = {},
    onPickLocalFileClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf(videoTitle) }
    var searchResults by remember { mutableStateOf<List<SubtitleItemData>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var downloadingFileId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 2 && searchResults.isEmpty()) {
            isSearching = true
            searchResults = onSearchOnlineSubtitles(searchQuery)
            isSearching = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Subtitle Engine & OpenSubtitles",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                val tabs = listOf("Tracks", "Timing & Style", "Online API", "Whisper AI")
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF3B82F6)
                        )
                    }
                ) {
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                when (selectedTab) {
                    0 -> {
                        // Tracks & Local File Selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .clickable { onPickLocalFileClick() }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Local Subtitle", tint = Color(0xFF60A5FA))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Load Local File (.srt, .vtt, .ass)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Pick any file from device storage", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Disable option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (subtitleTracks.none { it.isSelected }) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { onSelectSubtitleTrack(null) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Disable Subtitles (Off)", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (subtitleTracks.none { it.isSelected }) {
                                Icon(Icons.Default.Check, contentDescription = "Disabled", tint = Color(0xFF3B82F6))
                            }
                        }

                        subtitleTracks.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (track.isSelected) Color(0xFF3B82F6).copy(alpha = 0.25f) else Color(0xFF1E293B))
                                    .clickable { onSelectSubtitleTrack(track.id) }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Language: ${track.language.uppercase()} · ${track.mimeType}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                                if (track.isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF3B82F6))
                                }
                            }
                        }
                    }

                    1 -> {
                        // Timing & Styling
                        Text("Timing Synchronization (±10 Seconds)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Offset", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text("${if (currentSettings.timeOffsetMs >= 0) "+" else ""}${currentSettings.timeOffsetMs} ms", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Slider(
                            value = currentSettings.timeOffsetMs.toFloat(),
                            onValueChange = { valFloat ->
                                onSettingsChanged(currentSettings.copy(timeOffsetMs = valFloat.toLong()))
                            },
                            valueRange = -10000f..10000f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF3B82F6), activeTrackColor = Color(0xFF3B82F6))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Font Size Adjuster", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Scale", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text("${currentSettings.fontSizeSp.toInt()} sp", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Slider(
                            value = currentSettings.fontSizeSp,
                            onValueChange = { valFloat ->
                                onSettingsChanged(currentSettings.copy(fontSizeSp = valFloat))
                            },
                            valueRange = 12f..48f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF3B82F6), activeTrackColor = Color(0xFF3B82F6))
                        )
                    }

                    2 -> {
                        // OpenSubtitles Online API Search
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search Title / Query", color = Color(0xFF94A3B8)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            trailingIcon = {
                                IconButton(onClick = {
                                    /* Trigger Search */
                                }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF60A5FA))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isSearching) {
                            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF3B82F6))
                            }
                        } else {
                            searchResults.forEach { item ->
                                val attr = item.attributes
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(attr.release, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Language: ${attr.language.uppercase()} · Downloads: ${attr.downloadCount} · ⭐ ${attr.ratings}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (downloadingFileId == attr.files.firstOrNull()?.fileId) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF3B82F6), strokeWidth = 2.dp)
                                    } else {
                                        IconButton(onClick = {
                                            val fileInfo = attr.files.firstOrNull() ?: return@IconButton
                                            downloadingFileId = fileInfo.fileId
                                            /* Download */
                                        }) {
                                            Icon(Icons.Default.CloudDownload, contentDescription = "Download", tint = Color(0xFF3B82F6))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        WhisperSubtitleCard(
                            state = whisperState,
                            isModelDownloaded = isWhisperModelReady,
                            onStartGeneration = onStartWhisperGeneration,
                            onCancelGeneration = onCancelWhisperGeneration,
                            onReset = onResetWhisper
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Close Dialog", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
