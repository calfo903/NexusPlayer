package com.nexusplayer.app.util

import android.content.Context
import com.nexusplayer.app.data.local.NexusDatabase
import com.nexusplayer.app.data.repository.MediaRepositoryImpl
import com.nexusplayer.app.domain.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Smart Auto-Next Episode Detector for TV series and serial collections.
 * Uses regex patterns (S01E04, 1x04, Episode 4) to automatically locate the next episode in the folder.
 */
object AutoNextEpisodeDetector {

    private val seasonEpisodeRegex = Regex("(?i)S(\\d{1,2})E(\\d{1,2})")
    private val crossEpisodeRegex = Regex("(?i)(\\d{1,2})x(\\d{1,2})")
    private val episodeWordRegex = Regex("(?i)Episode\\s*(\\d{1,3})")

    suspend fun findNextEpisode(context: Context, currentVideo: VideoItem): VideoItem? = withContext(Dispatchers.IO) {
        try {
            val db = NexusDatabase.getInstance(context)
            val repo = MediaRepositoryImpl(context, db.videoResumeDao(), db.bookmarkDao(), db.playlistDao())
            val folderVideos = repo.getVideosInFolder(currentVideo.folderPath)
                .sortedBy { it.displayName }

            val currentName = currentVideo.displayName

            // 1. Try S01E04 pattern
            val seMatch = seasonEpisodeRegex.find(currentName)
            if (seMatch != null) {
                val seasonStr = seMatch.groupValues[1]
                val epNum = seMatch.groupValues[2].toIntOrNull() ?: return@withContext null
                val nextEpNum = epNum + 1
                val targetStr = String.format("S%sE%02d", seasonStr, nextEpNum)
                return@withContext folderVideos.firstOrNull {
                    it.id != currentVideo.id && it.displayName.contains(targetStr, ignoreCase = true)
                }
            }

            // 2. Try 1x04 pattern
            val crossMatch = crossEpisodeRegex.find(currentName)
            if (crossMatch != null) {
                val seasonStr = crossMatch.groupValues[1]
                val epNum = crossMatch.groupValues[2].toIntOrNull() ?: return@withContext null
                val nextEpNum = epNum + 1
                val targetStr = String.format("%sx%02d", seasonStr, nextEpNum)
                return@withContext folderVideos.firstOrNull {
                    it.id != currentVideo.id && it.displayName.contains(targetStr, ignoreCase = true)
                }
            }

            // 3. Fallback: Find exact next file in alphabetical order within the folder
            val currentIndex = folderVideos.indexOfFirst { it.id == currentVideo.id }
            if (currentIndex != -1 && currentIndex + 1 < folderVideos.size) {
                return@withContext folderVideos[currentIndex + 1]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}
