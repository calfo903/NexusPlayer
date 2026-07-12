package com.nexusplayer.app.domain.model

data class WatchAnalytics(
    val videoUriString: String,
    val videoTitle: String,
    val totalWatchTimeMs: Long,
    val sessionCount: Int,
    val lastWatchedTimestamp: Long,
    val totalCompletionPercentage: Float,
    val averagePlaybackSpeed: Float
)
