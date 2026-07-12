package com.nexusplayer.app.domain.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val videoCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val videos: List<VideoItem> = emptyList()
)

data class Bookmark(
    val id: Long = 0,
    val videoUriString: String,
    val videoTitle: String,
    val timestampMs: Long,
    val note: String,
    val dateAdded: Long = System.currentTimeMillis()
) {
    val formattedTimestamp: String
        get() = VideoItem.formatDuration(timestampMs)
}
