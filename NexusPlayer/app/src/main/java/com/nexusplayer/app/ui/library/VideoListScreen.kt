package com.nexusplayer.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusplayer.app.domain.model.VideoItem

@Composable
fun VideoListScreen(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem, Long) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onVideoInfoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.VideoFile,
                    contentDescription = "No Videos",
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No media found in this category.",
                    color = Color(0xFF94A3B8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(0.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(videos, key = { it.id }) { video ->
            VideoCard(
                video = video,
                onClick = {
                    onVideoClick(video, video.resumePositionMs)
                },
                onAddToPlaylist = { onAddToPlaylistClick(video) },
                onInfo = { onVideoInfoClick(video) }
            )
            Divider(color = Color(0xFF1E293B), thickness = 1.dp)
        }
    }
}

@Composable
fun VideoCard(
    video: VideoItem,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onInfo: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Simple Thumbnail
        Box(
            modifier = Modifier
                .size(width = 90.dp, height = 60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(28.dp)
            )

            // Duration Badge Bottom Right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = video.formattedDuration,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Metadata & Title Area
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            val sizeMb = if (video.sizeBytes > 0) "${video.sizeBytes / (1024 * 1024)} MB" else "Stream"
            Text(
                text = "$sizeMb · ${video.resolutionLabel}",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )

            // Resume Position Progress Bar if partially watched
            if (video.resumePositionMs > 1000L && video.durationMs > 0L) {
                Spacer(modifier = Modifier.height(6.dp))
                val frac = (video.resumePositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { frac },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(CircleShape),
                    color = Color(0xFF3B82F6),
                    trackColor = Color(0xFF334155)
                )
            }
        }

        // More Options Button & Dropdown
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color(0xFF94A3B8)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF1E293B))
            ) {
                DropdownMenuItem(
                    text = { Text("Play from Beginning", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF60A5FA)) },
                    onClick = {
                        showMenu = false
                        onClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, tint = Color(0xFF10B981)) },
                    onClick = {
                        showMenu = false
                        onAddToPlaylist()
                    }
                )
                DropdownMenuItem(
                    text = { Text("File Info", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Info, null, tint = Color(0xFFF59E0B)) },
                    onClick = {
                        showMenu = false
                        onInfo()
                    }
                )
            }
        }
    }
}
