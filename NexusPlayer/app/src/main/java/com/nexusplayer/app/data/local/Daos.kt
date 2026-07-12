package com.nexusplayer.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoResumeDao {
    @Query("SELECT * FROM video_resume WHERE uriString = :uriString LIMIT 1")
    suspend fun getResumePosition(uriString: String): VideoResumeEntity?

    @Query("SELECT * FROM video_resume ORDER BY lastPlayedTimestamp DESC")
    fun getAllHistoryFlow(): Flow<List<VideoResumeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveResumePosition(entity: VideoResumeEntity)

    @Query("DELETE FROM video_resume WHERE uriString = :uriString")
    suspend fun clearResumePosition(uriString: String)

    @Query("DELETE FROM video_resume")
    suspend fun clearAllHistory()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE videoUriString = :uriString ORDER BY timestampMs ASC")
    fun getBookmarksForVideoFlow(uriString: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE videoUriString = :uriString ORDER BY timestampMs ASC")
    suspend fun getBookmarksForVideo(uriString: String): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks ORDER BY dateAdded DESC")
    fun getAllBookmarksFlow(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY dateCreated DESC")
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addVideoToPlaylist(crossRef: PlaylistVideoCrossRef)

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId AND videoUriString = :videoUriString")
    suspend fun removeVideoFromPlaylist(playlistId: Long, videoUriString: String)

    @Query("SELECT videoUriString FROM playlist_videos WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    suspend fun getVideoUrisForPlaylist(playlistId: Long): List<String>
}
