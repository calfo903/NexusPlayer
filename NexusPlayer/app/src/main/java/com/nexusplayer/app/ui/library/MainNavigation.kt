package com.nexusplayer.app.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusplayer.app.domain.model.FolderInfo
import com.nexusplayer.app.domain.model.PlayerSettings
import com.nexusplayer.app.domain.model.Playlist
import com.nexusplayer.app.domain.model.VideoItem
import com.nexusplayer.app.player.engine.NexusVideoPlayer
import com.nexusplayer.app.ui.player.FloatingPipWindow
import com.nexusplayer.app.ui.player.PipShapeMode
import com.nexusplayer.app.ui.player.PlayerScreen
import kotlinx.coroutines.launch

enum class LibraryTab(val label: String) {
    FOLDERS("Folders"),
    ALL_VIDEOS("All Videos"),
    PLAYLISTS("Playlists"),
    SETTINGS("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    videos: List<VideoItem>,
    folders: List<FolderInfo>,
    playlists: List<Playlist>,
    playerSettings: PlayerSettings,
    playerEngine: NexusVideoPlayer,
    onVideoSelected: (VideoItem, Long) -> Unit,
    onSaveResume: (String, String, Long, Long) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onAddVideoToPlaylist: (Long, VideoItem) -> Unit,
    onSettingsChanged: (PlayerSettings) -> Unit,
    onPlayNetworkStream: (String, String?, Map<String, String>) -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(LibraryTab.FOLDERS) }
    var selectedFolder by remember { mutableStateOf<FolderInfo?>(null) }
    var activePlayerVideoItem by remember { mutableStateOf<VideoItem?>(null) }
    var activeFloatingPipShape by remember { mutableStateOf<PipShapeMode?>(null) }
    var showNetworkDialog by remember { mutableStateOf(false) }

    val isPlaying by playerEngine.isPlaying.collectAsState()
    val currentPositionMs by playerEngine.currentPositionMs.collectAsState()
    val durationMs by playerEngine.durationMs.collectAsState()

    if (activePlayerVideoItem != null && activeFloatingPipShape == null) {
        // Render Fullscreen Player Screen
        PlayerScreen(
            videoItem = activePlayerVideoItem!!,
            playerEngine = playerEngine,
            onBackClick = {
                val currentPos = playerEngine.getPlayerInstance()?.currentPosition ?: 0L
                val dur = playerEngine.getPlayerInstance()?.duration ?: 0L
                onSaveResume(activePlayerVideoItem!!.uri.toString(), activePlayerVideoItem!!.title, currentPos, dur)
                activePlayerVideoItem = null
            },
            onPlayNextEpisode = { nextItem ->
                activePlayerVideoItem = nextItem
                playerEngine.prepareAndPlay(nextItem, nextItem.resumePositionMs)
            },
            onEnterInAppFloatingPip = { shape ->
                activeFloatingPipShape = shape
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("N", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (selectedFolder != null) selectedFolder!!.folderName else "Nexus Player",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (selectedFolder != null) "${selectedFolder!!.videoCount} videos in folder" else "Hybrid Media Engine (ExoPlayer + HW)",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showNetworkDialog = true }) {
                        Icon(Icons.Default.Link, contentDescription = "Network Stream", tint = Color(0xFF60A5FA))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        bottomBar = {
            Column {
                // Mini Player Bottom Bar (If playing in background)
                AnimatedVisibility(
                    visible = playerEngine.getPlayerInstance() != null && durationMs > 0,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B))
                            .clickable {
                                // Resume fullscreen view for active item if tracked
                                videos.firstOrNull { it.id.toString() == playerEngine.getPlayerInstance()?.currentMediaItem?.mediaId }?.let {
                                    activePlayerVideoItem = it
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF3B82F6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Playing Background Audio/Video",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${VideoItem.formatDuration(currentPositionMs)} / ${VideoItem.formatDuration(durationMs)}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = { playerEngine.togglePlayPause() }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Toggle",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { playerEngine.release() }) {
                                Icon(Icons.Default.Close, contentDescription = "Stop", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }

                // Main Bottom Navigation
                NavigationBar(
                    containerColor = Color(0xFF0F172A),
                    tonalElevation = 8.dp
                ) {
                    LibraryTab.values().forEach { tab ->
                        val icon = when (tab) {
                            LibraryTab.FOLDERS -> Icons.Default.Folder
                            LibraryTab.ALL_VIDEOS -> Icons.Default.VideoLibrary
                            LibraryTab.PLAYLISTS -> Icons.Default.PlaylistPlay
                            LibraryTab.SETTINGS -> Icons.Default.Settings
                        }
                        NavigationBarItem(
                            selected = currentTab == tab && selectedFolder == null,
                            onClick = {
                                selectedFolder = null
                                currentTab = tab
                            },
                            icon = { Icon(icon, contentDescription = tab.label) },
                            label = { Text(tab.label, fontSize = 11.sp, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = Color(0xFF3B82F6),
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            )
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedFolder != null) {
                val folderVideos = videos.filter { it.folderPath == selectedFolder!!.folderPath }
                VideoListScreen(
                    videos = folderVideos,
                    onVideoClick = { video, startPos ->
                        activeFloatingPipShape = null
                        activePlayerVideoItem = video
                        playerEngine.prepareAndPlay(video, startPos)
                    },
                    onAddToPlaylistClick = { video ->
                        if (playlists.isNotEmpty()) {
                            onAddVideoToPlaylist(playlists.first().id, video)
                        } else {
                            onCreatePlaylist("My Favorites")
                        }
                    },
                    onVideoInfoClick = { /* Show info */ }
                )
            } else {
                when (currentTab) {
                    LibraryTab.FOLDERS -> {
                        FolderListScreen(
                            folders = folders,
                            onFolderClick = { folder -> selectedFolder = folder }
                        )
                    }
                    LibraryTab.ALL_VIDEOS -> {
                        VideoListScreen(
                            videos = videos,
                            onVideoClick = { video, startPos ->
                                activeFloatingPipShape = null
                                activePlayerVideoItem = video
                                playerEngine.prepareAndPlay(video, startPos)
                            },
                            onAddToPlaylistClick = { video ->
                                if (playlists.isNotEmpty()) {
                                    onAddVideoToPlaylist(playlists.first().id, video)
                                } else {
                                    onCreatePlaylist("My Favorites")
                                }
                            },
                            onVideoInfoClick = { /* Show info */ }
                        )
                    }
                    LibraryTab.PLAYLISTS -> {
                        PlaylistScreen(
                            playlists = playlists,
                            onCreatePlaylist = { name -> onCreatePlaylist(name) },
                            onPlaylistClick = { playlist ->
                                if (playlist.videos.isNotEmpty()) {
                                    val firstVideo = playlist.videos.first()
                                    activeFloatingPipShape = null
                                    activePlayerVideoItem = firstVideo
                                    playerEngine.prepareAndPlay(firstVideo, 0L)
                                }
                            }
                        )
                    }
                    LibraryTab.SETTINGS -> {
                        SettingsScreen(
                            settings = playerSettings,
                            onSettingsChanged = onSettingsChanged
                        )
                    }
                }
            }

            if (showNetworkDialog) {
                NetworkStreamDialog(
                    onPlayStream = { url, userAgent, headers ->
                        val streamVideo = VideoItem(
                            id = System.currentTimeMillis(),
                            title = "Direct Network Stream",
                            displayName = url.substringAfterLast('/'),
                            uri = android.net.Uri.parse(url),
                            isNetworkStream = true,
                            customHeaders = headers
                        )
                        activeFloatingPipShape = null
                        activePlayerVideoItem = streamVideo
                        playerEngine.prepareAndPlay(streamVideo, 0L)
                    },
                    onDismiss = { showNetworkDialog = false }
                )
            }

            if (activeFloatingPipShape != null && activePlayerVideoItem != null) {
                FloatingPipWindow(
                    playerEngine = playerEngine,
                    shapeMode = activeFloatingPipShape!!,
                    isPlaying = isPlaying,
                    onRestoreFullscreen = {
                        activeFloatingPipShape = null
                    },
                    onClosePip = {
                        activeFloatingPipShape = null
                        activePlayerVideoItem = null
                        playerEngine.release()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                )
            }
        }
    }
}
