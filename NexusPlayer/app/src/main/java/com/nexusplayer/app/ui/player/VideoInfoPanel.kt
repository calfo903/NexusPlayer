package com.nexusplayer.app.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nexusplayer.app.domain.model.CodecInfo
import com.nexusplayer.app.domain.model.VideoItem

@Composable
fun VideoInfoPanel(
    videoItem: VideoItem,
    codecInfo: CodecInfo?,
    currentPositionMs: Long,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())
            ) {
                Text("Video Information", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                InfoSection("File", listOf(
                    "Name" to videoItem.displayName,
                    "Format" to videoItem.mimeType,
                    "Size" to formatFileSize(videoItem.sizeBytes),
                    "Folder" to videoItem.folderName
                ))

                Spacer(modifier = Modifier.height(12.dp))
                InfoSection("Playback", listOf(
                    "Duration" to videoItem.formattedDuration,
                    "Position" to VideoItem.formatDuration(currentPositionMs),
                    "Progress" to if (videoItem.durationMs > 0) "${(currentPositionMs * 100 / videoItem.durationMs)}%" else "N/A"
                ))

                codecInfo?.let { info ->
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoSection("Video Codec", listOf(
                        "Codec" to info.videoCodec,
                        "Resolution" to info.videoResolution,
                        "Frame Rate" to "${info.videoFrameRate} fps",
                        "Bitrate" to "${info.videoBitrateKbps} kbps",
                        "Hardware" to if (info.isHardwareAccelerated) "HW Accelerated" else "Software"
                    ))
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoSection("Audio Codec", listOf(
                        "Codec" to info.audioCodec,
                        "Sample Rate" to "${info.audioSampleRateHz} Hz",
                        "Channels" to "${info.audioChannels}"
                    ))
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoSection("Container", listOf(
                        "Format" to info.containerFormat
                    ))
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun InfoSection(title: String, items: List<Pair<String, String>>) {
    Text(title, color = Color(0xFF60A5FA), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    Spacer(modifier = Modifier.height(6.dp))
    items.forEach { (label, value) ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp)
            Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return String.format("%.1f %s", size, units[unitIndex])
}
