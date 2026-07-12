package com.nexusplayer.app.domain.repository

import com.nexusplayer.app.data.remote.model.SubtitleItemData
import java.io.File

interface OpenSubtitlesRepository {
    suspend fun searchSubtitles(videoTitle: String, languages: String = "en,es,fr,de"): List<SubtitleItemData>
    suspend fun downloadSubtitleFile(fileId: Int, fileName: String): File?
    suspend fun scanLocalSubtitlesForVideo(videoPath: String): List<File>
}
