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

@Dao
interface WatchAnalyticsDao {
    @Query("SELECT * FROM watch_analytics WHERE videoUriString = :uriString LIMIT 1")
    suspend fun getAnalytics(uriString: String): WatchAnalyticsEntity?

    @Query("SELECT * FROM watch_analytics ORDER BY lastWatchedTimestamp DESC")
    fun getAllAnalyticsFlow(): Flow<List<WatchAnalyticsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnalytics(entity: WatchAnalyticsEntity)

    @Query("SELECT SUM(totalWatchTimeMs) FROM watch_analytics")
    suspend fun getTotalWatchTimeMs(): Long?

    @Query("SELECT COUNT(*) FROM watch_analytics")
    suspend fun getTotalVideosWatched(): Int

    @Query("DELETE FROM watch_analytics")
    suspend fun clearAll()
}

@Dao
interface VideoTagDao {
    @Query("SELECT * FROM video_tags ORDER BY tag ASC")
    fun getAllTagsFlow(): Flow<List<VideoTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: VideoTagEntity): Long

    @Query("SELECT * FROM video_tags WHERE videoUriString = :uriString")
    suspend fun getTagsForVideo(uriString: String): List<VideoTagEntity>

    @Query("SELECT vt.* FROM video_tags vt INNER JOIN video_tags_cross_ref ref ON vt.id = ref.tagId WHERE ref.videoUriString = :uriString")
    fun getTagsForVideoFlow(uriString: String): Flow<List<VideoTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTagToVideo(crossRef: VideoTagCrossRef)

    @Query("DELETE FROM video_tags_cross_ref WHERE videoUriString = :uriString AND tagId = :tagId")
    suspend fun removeTagFromVideo(uriString: String, tagId: Long)

    @Query("DELETE FROM video_tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: Long)

    @Query("SELECT ref.videoUriString FROM video_tags_cross_ref ref INNER JOIN video_tags t ON ref.tagId = t.id WHERE t.tag = :tag")
    suspend fun getVideoUrisWithTag(tag: String): List<String>
}
