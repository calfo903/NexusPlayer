package com.nexusplayer.app.domain.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain model representing a video file (local storage or network stream).
 * Contains comprehensive metadata, resolution, resume state, and file details.
 */
@Parcelize
data class VideoItem(
    val id: Long,
    val title: String,
    val displayName: String,
    val uri: Uri,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "video/mp4",
    val folderName: String = "Unknown",
    val folderPath: String = "",
    val dateAdded: Long = System.currentTimeMillis(),
    val resumePositionMs: Long = 0L,
    val isNetworkStream: Boolean = false,
    val customHeaders: Map<String, String> = emptyMap(),
    val chapters: List<VideoChapter> = emptyList()
) : Parcelable {

    val resolutionLabel: String
        get() = when {
            width >= 7680 || height >= 4320 -> "8K"
            width >= 3840 || height >= 2160 -> "4K"
            width >= 2560 || height >= 1440 -> "2K"
            width >= 1920 || height >= 1080 -> "1080p"
            width >= 1280 || height >= 720 -> "720p"
            width > 0 -> "${height}p"
            else -> "Stream"
        }

    val formattedDuration: String
        get() = formatDuration(durationMs)

    companion object {
        fun formatDuration(ms: Long): String {
            if (ms <= 0) return "00:00"
            val totalSeconds = ms / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }
}

@Parcelize
data class VideoChapter(
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long
) : Parcelable

@Parcelize
data class FolderInfo(
    val folderPath: String,
    val folderName: String,
    val videoCount: Int,
    val totalSize: Long,
    val totalDurationMs: Long,
    val thumbnailUri: Uri?
) : Parcelable
