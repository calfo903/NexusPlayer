package com.nexusplayer.app.player.subtitle

import android.content.Context
import com.nexusplayer.app.domain.model.VideoItem
import com.nexusplayer.app.domain.repository.OpenSubtitlesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SubtitleAutoDownloader(
    private val context: Context,
    private val repository: OpenSubtitlesRepository
) {

    private val subtitleCacheDir = File(context.cacheDir, "auto_subtitles").apply { mkdirs() }

    suspend fun autoDownloadSubtitle(videoItem: VideoItem): File? = withContext(Dispatchers.IO) {
        try {
            val query = videoItem.displayName
                .replace(Regex("[\\[\\](){}]"), " ")
                .replace(Regex("\\.(mkv|mp4|avi|mov|wmv|flv|webm)$", RegexOption.IGNORE_CASE), "")
                .trim()

            if (query.isBlank()) return@withContext null

            val cacheFile = File(subtitleCacheDir, "${videoItem.id}_${query.hashCode()}.srt")
            if (cacheFile.exists() && cacheFile.length() > 10) {
                return@withContext cacheFile
            }

            val results = repository.searchSubtitles(videoTitle = query, languages = "en")
            if (results.isEmpty()) return@withContext null

            val best = results.maxByOrNull { it.attributes.downloadCount } ?: return@withContext null
            val fileId = best.attributes.files.firstOrNull()?.fileId ?: return@withContext null
            val fileName = best.attributes.files.firstOrNull()?.fileName ?: "subtitle.srt"

            val downloadedFile = repository.downloadSubtitleFile(fileId = fileId, fileName = fileName)
            if (downloadedFile != null && downloadedFile.exists() && downloadedFile.length() > 10) {
                downloadedFile.copyTo(cacheFile, overwrite = true)
                cacheFile
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getCachedSubtitle(videoItem: VideoItem): File? {
        val files = subtitleCacheDir.listFiles()?.filter {
            it.name.startsWith("${videoItem.id}_") && it.length() > 10
        }?.sortedByDescending { it.lastModified() }
        return files?.firstOrNull()
    }

    fun clearCache() {
        subtitleCacheDir.listFiles()?.forEach { it.delete() }
    }
}
