package com.nexusplayer.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusplayer.app.domain.model.StorageInfo

@Composable
fun StorageBreakdownScreen(
    storageInfos: List<StorageInfo>,
    modifier: Modifier = Modifier
) {
    val totalSize = storageInfos.sumOf { it.totalSizeBytes }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(20.dp)
    ) {
        item {
            Text("Storage Breakdown", color = Color(0xFF60A5FA), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Total: ${formatSize(totalSize)} across ${storageInfos.sumOf { it.videoCount }} videos",
                color = Color(0xFF94A3B8), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(storageInfos.sortedByDescending { it.totalSizeBytes }) { info ->
            val progress = if (totalSize > 0) info.totalSizeBytes.toFloat() / totalSize.toFloat() else 0f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(info.folderName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${info.videoCount} videos · ${formatSize(info.totalSizeBytes)}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF3B82F6),
                        trackColor = Color(0xFF0F172A)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) { size /= 1024; unitIndex++ }
    return String.format("%.1f %s", size, units[unitIndex])
}
