package com.nexusplayer.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.nexusplayer.app.data.local.NexusDatabase
import com.nexusplayer.app.data.repository.MediaRepositoryImpl
import com.nexusplayer.app.domain.model.FolderInfo
import com.nexusplayer.app.domain.model.PlayerSettings
import com.nexusplayer.app.domain.model.Playlist
import com.nexusplayer.app.domain.model.VideoItem
import com.nexusplayer.app.player.engine.NexusVideoPlayer
import com.nexusplayer.app.ui.library.MainNavigation
import com.nexusplayer.app.ui.theme.NexusPlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private var playerEngine: NexusVideoPlayer? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Reload library if granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            val scope = rememberCoroutineScope()
            val context = this@MainActivity

            var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
            var folders by remember { mutableStateOf<List<FolderInfo>>(emptyList()) }
            var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
            var playerSettings by remember { mutableStateOf(PlayerSettings()) }
            var externalIntentVideo by remember { mutableStateOf<VideoItem?>(null) }

            val engine = remember {
                val p = NexusVideoPlayer(context, scope)
                playerEngine = p
                p
            }

            // Load media library on launch
            LaunchedEffect(Unit) {
                val db = NexusDatabase.getInstance(context)
                val repo = MediaRepositoryImpl(
                    context = context,
                    resumeDao = db.videoResumeDao(),
                    bookmarkDao = db.bookmarkDao(),
                    playlistDao = db.playlistDao()
                )

                videos = repo.getAllVideos()
                folders = repo.getFolders()

                // Listen to playlists flow
                launch {
                    repo.getAllPlaylistsFlow().collect { list ->
                        playlists = list
                    }
                }

                // Check intent if opened from file manager
                handleOpenIntent(intent)?.let { item ->
                    externalIntentVideo = item
                }
            }

            NexusPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val db = remember { NexusDatabase.getInstance(context) }
                    val repo = remember {
                        MediaRepositoryImpl(
                            context = context,
                            resumeDao = db.videoResumeDao(),
                            bookmarkDao = db.bookmarkDao(),
                            playlistDao = db.playlistDao()
                        )
                    }

                    MainNavigation(
                        videos = videos,
                        folders = folders,
                        playlists = playlists,
                        playerSettings = playerSettings,
                        playerEngine = engine,
                        onVideoSelected = { video, startPos ->
                            engine.prepareAndPlay(video, startPos)
                        },
                        onSaveResume = { uriStr, title, pos, dur ->
                            scope.launch {
                                repo.saveResumePosition(uriStr, title, pos, dur)
                                // Refresh videos to reflect new resume position
                                videos = repo.getAllVideos()
                            }
                        },
                        onCreatePlaylist = { name ->
                            scope.launch {
                                repo.createPlaylist(name)
                            }
                        },
                        onAddVideoToPlaylist = { playlistId, video ->
                            scope.launch {
                                repo.addVideoToPlaylist(playlistId, video)
                            }
                        },
                        onSettingsChanged = { updated ->
                            playerSettings = updated
                            engine.initPlayer(updated.decoderPriority)
                        },
                        onPlayNetworkStream = { url, userAgent, headers ->
                            val streamItem = VideoItem(
                                id = System.currentTimeMillis(),
                                title = "Live Network Stream",
                                displayName = url.substringAfterLast('/'),
                                uri = Uri.parse(url),
                                isNetworkStream = true,
                                customHeaders = headers
                            )
                            engine.prepareAndPlay(streamItem, 0L)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpenIntent(intent)?.let { item ->
            playerEngine?.prepareAndPlay(item, 0L)
        }
    }

    private fun handleOpenIntent(intent: Intent?): VideoItem? {
        if (intent == null || intent.action != Intent.ACTION_VIEW) return null
        val dataUri = intent.data ?: return null

        val title = dataUri.lastPathSegment ?: "External Video"
        return VideoItem(
            id = System.currentTimeMillis(),
            title = title,
            displayName = title,
            uri = dataUri,
            mimeType = intent.type ?: "video/mp4",
            isNetworkStream = dataUri.scheme?.startsWith("http") == true || dataUri.scheme?.startsWith("rtmp") == true
        )
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerEngine?.release()
    }
}
