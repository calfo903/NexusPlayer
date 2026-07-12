package com.nexusplayer.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "video_resume",
    indices = [Index(value = ["uriString"], unique = true)]
)
data class VideoResumeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uriString: String,
    val title: String,
    val resumePositionMs: Long,
    val totalDurationMs: Long,
    val lastPlayedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoUriString: String,
    val videoTitle: String,
    val timestampMs: Long,
    val note: String,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_videos",
    primaryKeys = ["playlistId", "videoUriString"]
)
data class PlaylistVideoCrossRef(
    val playlistId: Long,
    val videoUriString: String,
    val orderIndex: Int
)

@Entity(tableName = "watch_analytics")
data class WatchAnalyticsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoUriString: String,
    val videoTitle: String,
    val totalWatchTimeMs: Long = 0L,
    val sessionCount: Int = 0,
    val lastWatchedTimestamp: Long = System.currentTimeMillis(),
    val totalCompletionPercentage: Float = 0f,
    val averagePlaybackSpeed: Float = 1.0f
)

@Entity(tableName = "video_tags")
data class VideoTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoUriString: String,
    val tag: String,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "video_tags_cross_ref",
    primaryKeys = ["videoUriString", "tagId"]
)
data class VideoTagCrossRef(
    val videoUriString: String,
    val tagId: Long
)
