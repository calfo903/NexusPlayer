package com.nexusplayer.app.domain.repository

import com.nexusplayer.app.domain.model.Bookmark
import com.nexusplayer.app.domain.model.FolderInfo
import com.nexusplayer.app.domain.model.Playlist
import com.nexusplayer.app.domain.model.VideoItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    suspend fun getAllVideos(): List<VideoItem>
    suspend fun getFolders(): List<FolderInfo>
    suspend fun getVideosInFolder(folderPath: String): List<VideoItem>
    suspend fun getVideoByUri(uriString: String): VideoItem?

    // History / Resume
    suspend fun saveResumePosition(uriString: String, title: String, positionMs: Long, durationMs: Long)
    suspend fun getResumePosition(uriString: String): Long
    fun getHistoryFlow(): Flow<List<VideoItem>>

    // Bookmarks
    fun getBookmarksFlow(videoUriString: String): Flow<List<Bookmark>>
    suspend fun addBookmark(videoUriString: String, title: String, timestampMs: Long, note: String)
    suspend fun removeBookmark(bookmark: Bookmark)

    // Playlists
    fun getAllPlaylistsFlow(): Flow<List<Playlist>>
    suspend fun createPlaylist(name: String): Long
    suspend fun addVideoToPlaylist(playlistId: Long, video: VideoItem)
    suspend fun removeVideoFromPlaylist(playlistId: Long, videoUriString: String)
}
