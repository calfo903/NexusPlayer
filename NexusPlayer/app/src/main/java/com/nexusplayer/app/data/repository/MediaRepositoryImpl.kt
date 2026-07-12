package com.nexusplayer.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.nexusplayer.app.data.local.BookmarkDao
import com.nexusplayer.app.data.local.BookmarkEntity
import com.nexusplayer.app.data.local.PlaylistDao
import com.nexusplayer.app.data.local.PlaylistEntity
import com.nexusplayer.app.data.local.PlaylistVideoCrossRef
import com.nexusplayer.app.data.local.VideoResumeDao
import com.nexusplayer.app.data.local.VideoResumeEntity
import com.nexusplayer.app.domain.model.Bookmark
import com.nexusplayer.app.domain.model.FolderInfo
import com.nexusplayer.app.domain.model.Playlist
import com.nexusplayer.app.domain.model.VideoChapter
import com.nexusplayer.app.domain.model.VideoItem
import com.nexusplayer.app.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepositoryImpl(
    private val context: Context,
    private val resumeDao: VideoResumeDao,
    private val bookmarkDao: BookmarkDao,
    private val playlistDao: PlaylistDao
) : MediaRepository {

    override suspend fun getAllVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val displayCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Untitled Video"
                    val displayName = cursor.getString(displayCol) ?: title
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val mimeType = cursor.getString(mimeCol) ?: "video/mp4"
                    val path = cursor.getString(dataCol) ?: ""
                    val dateAdded = cursor.getLong(dateAddedCol) * 1000L

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val file = File(path)
                    val folderName = file.parentFile?.name ?: "Internal Storage"
                    val folderPath = file.parentFile?.absolutePath ?: "/"

                    val resumeEntity = resumeDao.getResumePosition(contentUri.toString())

                    // Mock chapters if duration > 10 mins for demonstration
                    val chapters = if (duration > 600_000L) {
                        listOf(
                            VideoChapter("Intro & Opening", 0L, 120_000L),
                            VideoChapter("Main Act I", 120_000L, duration / 2),
                            VideoChapter("Climax & Outro", duration / 2, duration)
                        )
                    } else emptyList()

                    videoList.add(
                        VideoItem(
                            id = id,
                            title = title,
                            displayName = displayName,
                            uri = contentUri,
                            durationMs = duration,
                            sizeBytes = size,
                            width = width,
                            height = height,
                            mimeType = mimeType,
                            folderName = folderName,
                            folderPath = folderPath,
                            dateAdded = dateAdded,
                            resumePositionMs = resumeEntity?.resumePositionMs ?: 0L,
                            chapters = chapters
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If local storage is empty or testing inside mock environment, inject rich sample videos
        if (videoList.isEmpty()) {
            return@withContext getDemoHighQualityVideos()
        }

        return@withContext videoList
    }

    override suspend fun getFolders(): List<FolderInfo> = withContext(Dispatchers.IO) {
        val allVideos = getAllVideos()
        val folderMap = allVideos.groupBy { it.folderPath }
        val folders = mutableListOf<FolderInfo>()

        for ((path, videos) in folderMap) {
            if (videos.isEmpty()) continue
            val folderName = videos.first().folderName
            val totalSize = videos.sumOf { it.sizeBytes }
            val totalDuration = videos.sumOf { it.durationMs }
            val thumbnailUri = videos.firstOrNull { it.durationMs > 0 }?.uri ?: videos.first().uri

            folders.add(
                FolderInfo(
                    folderPath = path,
                    folderName = folderName,
                    videoCount = videos.size,
                    totalSize = totalSize,
                    totalDurationMs = totalDuration,
                    thumbnailUri = thumbnailUri
                )
            )
        }
        folders.sortByDescending { it.videoCount }
        return@withContext folders
    }

    override suspend fun getVideosInFolder(folderPath: String): List<VideoItem> = withContext(Dispatchers.IO) {
        getAllVideos().filter { it.folderPath == folderPath }
    }

    override suspend fun getVideoByUri(uriString: String): VideoItem? = withContext(Dispatchers.IO) {
        getAllVideos().firstOrNull { it.uri.toString() == uriString }
    }

    override suspend fun saveResumePosition(uriString: String, title: String, positionMs: Long, durationMs: Long) {
        withContext(Dispatchers.IO) {
            if (positionMs > 1000L && positionMs < durationMs - 2000L) {
                resumeDao.saveResumePosition(
                    VideoResumeEntity(
                        uriString = uriString,
                        title = title,
                        resumePositionMs = positionMs,
                        totalDurationMs = durationMs,
                        lastPlayedTimestamp = System.currentTimeMillis()
                    )
                )
            } else if (positionMs >= durationMs - 2000L) {
                resumeDao.clearResumePosition(uriString)
            }
        }
    }

    override suspend fun getResumePosition(uriString: String): Long = withContext(Dispatchers.IO) {
        resumeDao.getResumePosition(uriString)?.resumePositionMs ?: 0L
    }

    override fun getHistoryFlow(): Flow<List<VideoItem>> {
        return resumeDao.getAllHistoryFlow().map { entities ->
            val allVideos = getAllVideos()
            entities.mapNotNull { entity ->
                allVideos.firstOrNull { it.uri.toString() == entity.uriString }?.copy(
                    resumePositionMs = entity.resumePositionMs
                )
            }
        }
    }

    override fun getBookmarksFlow(videoUriString: String): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksForVideoFlow(videoUriString).map { entities ->
            entities.map { entity ->
                Bookmark(
                    id = entity.id,
                    videoUriString = entity.videoUriString,
                    videoTitle = entity.videoTitle,
                    timestampMs = entity.timestampMs,
                    note = entity.note,
                    dateAdded = entity.dateAdded
                )
            }
        }
    }

    override suspend fun addBookmark(videoUriString: String, title: String, timestampMs: Long, note: String) {
        withContext(Dispatchers.IO) {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    videoUriString = videoUriString,
                    videoTitle = title,
                    timestampMs = timestampMs,
                    note = note
                )
            )
        }
    }

    override suspend fun removeBookmark(bookmark: Bookmark) {
        withContext(Dispatchers.IO) {
            bookmarkDao.deleteBookmark(
                BookmarkEntity(
                    id = bookmark.id,
                    videoUriString = bookmark.videoUriString,
                    videoTitle = bookmark.videoTitle,
                    timestampMs = bookmark.timestampMs,
                    note = bookmark.note
                )
            )
        }
    }

    override fun getAllPlaylistsFlow(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylistsFlow().map { entities ->
            val allVideos = getAllVideos()
            entities.map { entity ->
                val uris = playlistDao.getVideoUrisForPlaylist(entity.id)
                val videos = uris.mapNotNull { uriStr -> allVideos.firstOrNull { it.uri.toString() == uriStr } }
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    dateCreated = entity.dateCreated,
                    videoCount = videos.size,
                    totalDurationMs = videos.sumOf { it.durationMs },
                    videos = videos
                )
            }
        }
    }

    override suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.createPlaylist(PlaylistEntity(name = name))
    }

    override suspend fun addVideoToPlaylist(playlistId: Long, video: VideoItem) {
        withContext(Dispatchers.IO) {
            val currentUris = playlistDao.getVideoUrisForPlaylist(playlistId)
            if (!currentUris.contains(video.uri.toString())) {
                playlistDao.addVideoToPlaylist(
                    PlaylistVideoCrossRef(
                        playlistId = playlistId,
                        videoUriString = video.uri.toString(),
                        orderIndex = currentUris.size
                    )
                )
            }
        }
    }

    override suspend fun removeVideoFromPlaylist(playlistId: Long, videoUriString: String) {
        withContext(Dispatchers.IO) {
            playlistDao.removeVideoFromPlaylist(playlistId, videoUriString)
        }
    }

    private fun getDemoHighQualityVideos(): List<VideoItem> {
        return listOf(
            VideoItem(
                id = 101,
                title = "Big Buck Bunny 4K 60FPS HDR",
                displayName = "Big_Buck_Bunny_4K_HDR.mp4",
                uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
                durationMs = 596000L,
                sizeBytes = 158_200_000L,
                width = 3840,
                height = 2160,
                mimeType = "video/mp4",
                folderName = "4K Showcase",
                folderPath = "/storage/emulated/0/Movies/4K Showcase",
                chapters = listOf(
                    VideoChapter("Opening & Forest Sunrise", 0L, 90_000L),
                    VideoChapter("Enter the Squirrels", 90_000L, 300_000L),
                    VideoChapter("Revenge of the Bunny", 300_000L, 596_000L)
                )
            ),
            VideoItem(
                id = 102,
                title = "Sintel Animated Epic (MKV 1080p)",
                displayName = "Sintel_Chapter_01_AV1.mkv",
                uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"),
                durationMs = 888000L,
                sizeBytes = 245_000_000L,
                width = 1920,
                height = 1080,
                mimeType = "video/mkv",
                folderName = "Movies",
                folderPath = "/storage/emulated/0/Movies",
                chapters = listOf(
                    VideoChapter("Snowy Trek", 0L, 180_000L),
                    VideoChapter("City of Thieves", 180_000L, 520_000L),
                    VideoChapter("The Dragon's Lair", 520_000L, 888_000L)
                )
            ),
            VideoItem(
                id = 103,
                title = "Tears of Steel Sci-Fi Short (HEVC/H.265)",
                displayName = "Tears_Of_Steel_HEVC_Surround.mp4",
                uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"),
                durationMs = 734000L,
                sizeBytes = 310_000_000L,
                width = 2560,
                height = 1440,
                mimeType = "video/mp4",
                folderName = "Sci-Fi Shorts",
                folderPath = "/storage/emulated/0/Movies/Sci-Fi Shorts"
            ),
            VideoItem(
                id = 104,
                title = "Cyberpunk Neo-Tokyo Stream (HLS m3u8)",
                displayName = "Live_Cyberpunk_Stream.m3u8",
                uri = Uri.parse("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
                durationMs = 3600_000L,
                sizeBytes = 0L,
                width = 1920,
                height = 1080,
                mimeType = "application/x-mpegURL",
                folderName = "Live Network Streams",
                folderPath = "/storage/emulated/0/Streams",
                isNetworkStream = true
            ),
            VideoItem(
                id = 105,
                title = "Elephants Dream 8K Remaster",
                displayName = "Elephants_Dream_8K.mp4",
                uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
                durationMs = 653000L,
                sizeBytes = 420_000_000L,
                width = 7680,
                height = 4320,
                mimeType = "video/mp4",
                folderName = "8K Showcase",
                folderPath = "/storage/emulated/0/Movies/8K Showcase"
            ),
            VideoItem(
                id = 106,
                title = "For Bigger Blazes (60FPS HDR Test)",
                displayName = "Blazes_HDR_Sample.mp4",
                uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
                durationMs = 15000L,
                sizeBytes = 15_000_000L,
                width = 1920,
                height = 1080,
                mimeType = "video/mp4",
                folderName = "Downloads",
                folderPath = "/storage/emulated/0/Download"
            )
        )
    }
}
