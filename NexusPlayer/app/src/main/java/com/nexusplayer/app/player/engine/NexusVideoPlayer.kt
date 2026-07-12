package com.nexusplayer.app.player.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import com.nexusplayer.app.domain.model.AspectRatioMode
import com.nexusplayer.app.domain.model.AudioTrackInfo
import com.nexusplayer.app.domain.model.CodecInfo
import com.nexusplayer.app.domain.model.DecoderPriority
import com.nexusplayer.app.domain.model.EqualizerBand
import com.nexusplayer.app.domain.model.EqualizerState
import com.nexusplayer.app.domain.model.SubtitleSettings
import com.nexusplayer.app.domain.model.SubtitleTrack
import com.nexusplayer.app.domain.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.media3.ui.PlayerView
import java.io.File

/**
 * NexusVideoPlayer: The core high-performance hybrid media engine.
 * Powered by ExoPlayer (Media3) with full hardware acceleration preference,
 * multi-container extraction, equalizer effects, and multi-track management.
 */
@OptIn(UnstableApi::class)
class NexusVideoPlayer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var playerView: PlayerView? = null

    // Audio Effects (VLC Style Equalizer Engine & 200% Loudness Enhancer)
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _volumeBoostGainMb = MutableStateFlow(0)
    val volumeBoostGainMb: StateFlow<Int> = _volumeBoostGainMb.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _videoSize = MutableStateFlow(VideoSize.UNKNOWN)
    val videoSize: StateFlow<VideoSize> = _videoSize.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<AudioTrackInfo>>(emptyList())
    val audioTracks: StateFlow<List<AudioTrackInfo>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<SubtitleTrack>>(emptyList())
    val subtitleTracks: StateFlow<List<SubtitleTrack>> = _subtitleTracks.asStateFlow()

    private val _subtitleSettings = MutableStateFlow(SubtitleSettings())
    val subtitleSettings: StateFlow<SubtitleSettings> = _subtitleSettings.asStateFlow()

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private val _codecInfo = MutableStateFlow<CodecInfo?>(null)
    val codecInfo: StateFlow<CodecInfo?> = _codecInfo.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _activeSubtitleText = MutableStateFlow<String?>("")
    val activeSubtitleText: StateFlow<String?> = _activeSubtitleText.asStateFlow()

    // A-B Repeat (Loop Mode)
    private val _abRepeatEnabled = MutableStateFlow(false)
    val abRepeatEnabled: StateFlow<Boolean> = _abRepeatEnabled.asStateFlow()

    private val _abRepeatA = MutableStateFlow<Long?>(null)
    val abRepeatA: StateFlow<Long?> = _abRepeatA.asStateFlow()

    private val _abRepeatB = MutableStateFlow<Long?>(null)
    val abRepeatB: StateFlow<Long?> = _abRepeatB.asStateFlow()

    // Night Mode / Dialog Boost
    private val _nightModeEnabled = MutableStateFlow(false)
    val nightModeEnabled: StateFlow<Boolean> = _nightModeEnabled.asStateFlow()

    private var originalBandLevels: Map<Short, Short>? = null

    // Audio-Only Mode
    private val _audioOnlyMode = MutableStateFlow(false)
    val audioOnlyMode: StateFlow<Boolean> = _audioOnlyMode.asStateFlow()

    // Playback Queue
    private val _playQueue = MutableStateFlow<List<VideoItem>>(emptyList())
    val playQueue: StateFlow<List<VideoItem>> = _playQueue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(-1)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private var progressJob: Job? = null
    private var currentVideoItem: VideoItem? = null

    init {
        initPlayer(DecoderPriority.AUTO)
    }

    fun initPlayer(decoderPriority: DecoderPriority) {
        release()

        val renderersFactory = DefaultRenderersFactory(context).apply {
            val mode = when (decoderPriority) {
                DecoderPriority.AUTO -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                DecoderPriority.HW_ONLY -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                DecoderPriority.SW_PREFERRED -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
            }
            setExtensionRendererMode(mode)
            setEnableDecoderFallback(true)
        }

        val extractorsFactory = DefaultExtractorsFactory().apply {
            setConstantBitrateSeekingEnabled(true)
        }

        val selector = DefaultTrackSelector(context)
        trackSelector = selector

        val cacheDataSourceFactory = NexusCacheManager.buildCacheDataSourceFactory(context)
        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory, extractorsFactory)

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(selector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekParameters(SeekParameters.EXACT)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startProgressLoop() else stopProgressLoop()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        _durationMs.value = player.duration.coerceAtLeast(0L)
                        updateTracksInfo()
                        initAudioEffects(player.audioSessionId)
                    }
                    Player.STATE_ENDED -> {
                        val a = _abRepeatA.value
                        val b = _abRepeatB.value
                        if (_abRepeatEnabled.value && a != null && b != null) {
                            player.seekTo(a)
                            player.playWhenReady = true
                            _isPlaying.value = true
                            startProgressLoop()
                        } else {
                            _isPlaying.value = false
                            stopProgressLoop()
                        }
                    }
                    else -> {}
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                _videoSize.value = videoSize
                updateCodecInfo()
            }

            override fun onPlayerError(error: PlaybackException) {
                _errorMessage.value = "Playback Error: ${error.localizedMessage} (Code: ${error.errorCodeName})"
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracksInfo()
                updateCodecInfo()
            }
        })

        exoPlayer = player
    }

    fun getPlayerInstance(): ExoPlayer? = exoPlayer

    fun prepareAndPlay(videoItem: VideoItem, startPositionMs: Long = 0L, externalSubtitles: List<File> = emptyList()) {
        val player = exoPlayer ?: return
        currentVideoItem = videoItem
        _errorMessage.value = null

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(videoItem.uri)
            .setMediaId(videoItem.id.toString())

        if (externalSubtitles.isNotEmpty()) {
            val subtitleConfigs = externalSubtitles.map { file ->
                val mimeType = when (file.extension.lowercase()) {
                    "srt" -> MimeTypes.APPLICATION_SUBRIP
                    "vtt" -> MimeTypes.TEXT_VTT
                    "ass", "ssa" -> MimeTypes.TEXT_SSA
                    else -> MimeTypes.APPLICATION_SUBRIP
                }
                MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(file))
                    .setMimeType(mimeType)
                    .setLanguage("en")
                    .setLabel(file.nameWithoutExtension)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
            }
            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
        }

        player.setMediaItem(mediaItemBuilder.build())
        player.prepare()
        if (startPositionMs > 0L) {
            player.seekTo(startPositionMs)
            _currentPositionMs.value = startPositionMs
        }
        player.playWhenReady = true
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val clamped = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(clamped)
        _currentPositionMs.value = clamped
    }

    fun seekRelative(offsetMs: Long) {
        val player = exoPlayer ?: return
        seekTo(player.currentPosition + offsetMs)
    }

    fun stepFrameForward() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause()
        // Standard video frame at ~30fps is ~33.3ms, 60fps is ~16.6ms
        val target = (player.currentPosition + 33L).coerceAtMost(player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        _currentPositionMs.value = target
    }

    fun stepFrameBackward() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause()
        val target = (player.currentPosition - 33L).coerceAtLeast(0L)
        player.seekTo(target)
        _currentPositionMs.value = target
    }

    fun setPlaybackSpeed(speed: Float) {
        val player = exoPlayer ?: return
        val clamped = speed.coerceIn(0.25f, 4.0f)
        player.playbackParameters = PlaybackParameters(clamped, 1.0f)
        _playbackSpeed.value = clamped
    }

    fun setAspectRatioMode(mode: AspectRatioMode) {
        _aspectRatioMode.value = mode
    }

    fun setSubtitleSettings(settings: SubtitleSettings) {
        _subtitleSettings.value = settings
    }

    fun selectAudioTrack(trackId: String) {
        val player = exoPlayer ?: return
        val selector = trackSelector ?: return
        val currentTracks = player.currentTracks

        for (group in currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    if ((format.id ?: "${format.language}_$i") == trackId) {
                        val override = TrackSelectionOverride(group.mediaTrackGroup, i)
                        selector.parameters = selector.buildUponParameters()
                            .setOverrideForType(override)
                            .build()
                        updateTracksInfo()
                        return
                    }
                }
            }
        }
    }

    fun selectSubtitleTrack(trackId: String?) {
        val selector = trackSelector ?: return
        if (trackId == null) {
            // Disable subtitles
            selector.parameters = selector.buildUponParameters()
                .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            val player = exoPlayer ?: return
            for (group in player.currentTracks.groups) {
                if (group.type == C.TRACK_TYPE_TEXT) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val id = format.id ?: format.label ?: "sub_$i"
                        if (id == trackId) {
                            val override = TrackSelectionOverride(group.mediaTrackGroup, i)
                            selector.parameters = selector.buildUponParameters()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setOverrideForType(override)
                                .build()
                            updateTracksInfo()
                            return
                        }
                    }
                }
            }
        }
        updateTracksInfo()
    }

    fun addExternalSubtitle(file: File) {
        val video = currentVideoItem ?: return
        val pos = exoPlayer?.currentPosition ?: 0L
        prepareAndPlay(video, pos, listOf(file))
    }

    // --- Audio Equalizer & Effects ---
    private fun initAudioEffects(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()

            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
            }
            equalizer = eq

            val bb = BassBoost(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
            }
            bassBoost = bb

            val vt = Virtualizer(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
            }
            virtualizer = vt

            val le = LoudnessEnhancer(audioSessionId).apply {
                enabled = true
                setTargetGain(_volumeBoostGainMb.value)
            }
            loudnessEnhancer = le

            val numBands = eq.numberOfBands
            val bandRange = eq.bandLevelRange
            val bands = mutableListOf<EqualizerBand>()
            for (i in 0 until numBands) {
                val index = i.toShort()
                bands.add(
                    EqualizerBand(
                        index = index,
                        centerFreqHz = eq.getCenterFreq(index) / 1000,
                        currentLevelMillibels = eq.getBandLevel(index),
                        minLevelMillibels = bandRange[0],
                        maxLevelMillibels = bandRange[1]
                    )
                )
            }

            val presets = mutableListOf<String>()
            for (i in 0 until eq.numberOfPresets) {
                presets.add(eq.getPresetName(i.toShort()))
            }

            _equalizerState.value = _equalizerState.value.copy(
                bands = bands,
                presets = presets.ifEmpty { _equalizerState.value.presets }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            _equalizerState.value = _equalizerState.value.copy(isEnabled = enabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBandLevel(bandIndex: Short, levelMillibels: Short) {
        try {
            equalizer?.setBandLevel(bandIndex, levelMillibels)
            val updatedBands = _equalizerState.value.bands.map {
                if (it.index == bandIndex) it.copy(currentLevelMillibels = levelMillibels) else it
            }
            _equalizerState.value = _equalizerState.value.copy(bands = updatedBands, currentPresetIndex = -1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBassBoost(strength: Short) {
        try {
            bassBoost?.setStrength(strength)
            _equalizerState.value = _equalizerState.value.copy(bassBoostStrength = strength)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setVirtualizer(strength: Short) {
        try {
            virtualizer?.setStrength(strength)
            _equalizerState.value = _equalizerState.value.copy(virtualizerStrength = strength)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun usePreset(presetIndex: Int) {
        try {
            equalizer?.usePreset(presetIndex.toShort())
            val eq = equalizer ?: return
            val updatedBands = _equalizerState.value.bands.map { band ->
                band.copy(currentLevelMillibels = eq.getBandLevel(band.index))
            }
            _equalizerState.value = _equalizerState.value.copy(
                currentPresetIndex = presetIndex,
                bands = updatedBands
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sets software/hardware loudness gain up to 200% (+2000mB / +20dB).
     * 0mB = 100% normal volume, 1000mB = 150% volume, 2000mB = 200% volume.
     */
    fun setVolumeBoostGain(gainMillibels: Int) {
        val clamped = gainMillibels.coerceIn(0, 2000)
        _volumeBoostGainMb.value = clamped
        try {
            loudnessEnhancer?.setTargetGain(clamped)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateTracksInfo() {
        val player = exoPlayer ?: return
        val audioList = mutableListOf<AudioTrackInfo>()
        val subtitleList = mutableListOf<SubtitleTrack>()

        for (group in player.currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val isSelected = group.isTrackSelected(i)
                    audioList.add(
                        AudioTrackInfo(
                            id = format.id ?: "${format.language}_$i",
                            language = format.language ?: "Undetermined",
                            label = format.label ?: "Audio Track ${audioList.size + 1}",
                            channels = format.channelCount.coerceAtLeast(2),
                            sampleRate = format.sampleRate.coerceAtLeast(44100),
                            bitrate = format.bitrate,
                            isSelected = isSelected
                        )
                    )
                }
            } else if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val isSelected = group.isTrackSelected(i)
                    subtitleList.add(
                        SubtitleTrack(
                            id = format.id ?: format.label ?: "sub_$i",
                            language = format.language ?: "En",
                            label = format.label ?: "Subtitle Track ${subtitleList.size + 1}",
                            mimeType = format.sampleMimeType ?: MimeTypes.APPLICATION_SUBRIP,
                            isSelected = isSelected
                        )
                    )
                }
            }
        }
        _audioTracks.value = audioList
        _subtitleTracks.value = subtitleList
    }

    private fun updateCodecInfo(decoderName: String? = null) {
        val player = exoPlayer ?: return
        val format = player.videoFormat
        val audioFormat = player.audioFormat

        val vidCodec = decoderName ?: format?.sampleMimeType ?: "Unknown Video Codec"
        val isHw = decoderName?.let {
            it.contains("OMX.qcom", true) ||
            it.contains("c2.qti", true) ||
            it.contains("c2.exynos", true) ||
            it.contains("c2.mediatek", true) ||
            !it.contains("c2.android", true)
        } ?: true

        val resolution = if (format != null && format.width > 0) "${format.width}x${format.height}" else "1920x1080"
        val fps = if (format != null && format.frameRate > 0f) format.frameRate else 60.0f
        val bitrate = (format?.bitrate ?: 15_000_000) / 1000

        val audCodec = audioFormat?.sampleMimeType ?: "AAC / AC3 Surround"
        val audSample = audioFormat?.sampleRate ?: 48000
        val audChannels = audioFormat?.channelCount ?: 6

        val container = currentVideoItem?.mimeType ?: "video/mp4"

        _codecInfo.value = CodecInfo(
            videoCodec = vidCodec,
            videoResolution = resolution,
            videoFrameRate = fps,
            videoBitrateKbps = bitrate,
            isHardwareAccelerated = isHw,
            audioCodec = audCodec,
            audioSampleRateHz = audSample,
            audioChannels = audChannels,
            containerFormat = container
        )
    }

    private fun startProgressLoop() {
        stopProgressLoop()
        progressJob = scope.launch(Dispatchers.Main) {
            while (true) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition
                    _currentPositionMs.value = pos
                    _bufferedPositionMs.value = player.bufferedPosition
                    _durationMs.value = player.duration.coerceAtLeast(0L)

                    val a = _abRepeatA.value
                    val b = _abRepeatB.value
                    if (_abRepeatEnabled.value && b != null && a != null && pos >= b) {
                        player.seekTo(a)
                        _currentPositionMs.value = a
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressLoop() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressLoop()
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        exoPlayer?.release()
        exoPlayer = null
    }

    // --- A-B Repeat (Loop Mode) ---

    fun setAbPointA() {
        val player = exoPlayer ?: return
        _abRepeatA.value = player.currentPosition
    }

    fun setAbPointB() {
        val player = exoPlayer ?: return
        val pos = player.currentPosition
        val a = _abRepeatA.value
        if (a == null || pos > a) {
            _abRepeatB.value = pos
        }
    }

    fun toggleAbRepeat() {
        if (_abRepeatEnabled.value) {
            _abRepeatEnabled.value = false
        } else {
            if (_abRepeatA.value == null) {
                val player = exoPlayer
                if (player != null) {
                    _abRepeatA.value = player.currentPosition
                }
            }
            _abRepeatEnabled.value = true
        }
    }

    fun clearAbRepeat() {
        _abRepeatA.value = null
        _abRepeatB.value = null
        _abRepeatEnabled.value = false
    }

    // --- Night Mode / Dialog Boost ---

    fun setNightModeEnabled(enabled: Boolean) {
        val eq = equalizer ?: return
        try {
            if (enabled && !_nightModeEnabled.value) {
                // Store original band levels before modification
                val saved = mutableMapOf<Short, Short>()
                for (i in 0 until eq.numberOfBands) {
                    val index = i.toShort()
                    saved[index] = eq.getBandLevel(index)
                }
                originalBandLevels = saved

                // Boost center frequencies in the 1kHz-4kHz dialog range (~6dB = 600mB)
                val boostLevel: Short = 600
                for (i in 0 until eq.numberOfBands) {
                    val index = i.toShort()
                    val centerFreqHz = eq.getCenterFreq(index) / 1000
                    if (centerFreqHz in 1000..4000) {
                        val currentLevel = eq.getBandLevel(index)
                        val boosted = (currentLevel + boostLevel).toShort().coerceAtMost(eq.bandLevelRange[1])
                        eq.setBandLevel(index, boosted)
                    }
                }

                val updatedBands = _equalizerState.value.bands.map { band ->
                    val newLevel = eq.getBandLevel(band.index)
                    band.copy(currentLevelMillibels = newLevel)
                }
                _equalizerState.value = _equalizerState.value.copy(bands = updatedBands)
                _nightModeEnabled.value = true

            } else if (!enabled && _nightModeEnabled.value) {
                // Restore original band levels
                val original = originalBandLevels
                if (original != null) {
                    for ((index, level) in original) {
                        eq.setBandLevel(index, level)
                    }
                }

                val updatedBands = _equalizerState.value.bands.map { band ->
                    val restoredLevel = original?.get(band.index) ?: band.currentLevelMillibels
                    band.copy(currentLevelMillibels = restoredLevel)
                }
                _equalizerState.value = _equalizerState.value.copy(bands = updatedBands)
                originalBandLevels = null
                _nightModeEnabled.value = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Audio-Only Mode ---

    fun setAudioOnlyMode(enabled: Boolean) {
        _audioOnlyMode.value = enabled
    }

    // --- Frame Export ---

    fun setPlayerView(view: PlayerView) {
        playerView = view
    }

    fun captureCurrentFrame(): Bitmap? {
        val view = playerView ?: return null
        return try {
            val bitmap = android.graphics.Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- Playback Queue ---

    fun setPlayQueue(items: List<VideoItem>, startIndex: Int = 0) {
        _playQueue.value = items
        _currentQueueIndex.value = startIndex.coerceIn(-1, items.size - 1)
    }

    fun playNextInQueue(): VideoItem? {
        val queue = _playQueue.value
        val currentIdx = _currentQueueIndex.value
        val nextIdx = currentIdx + 1
        if (nextIdx >= queue.size) return null
        _currentQueueIndex.value = nextIdx
        val item = queue[nextIdx]
        prepareAndPlay(item)
        return item
    }

    fun playPreviousInQueue(): VideoItem? {
        val queue = _playQueue.value
        val currentIdx = _currentQueueIndex.value
        val prevIdx = currentIdx - 1
        if (prevIdx < 0) return null
        _currentQueueIndex.value = prevIdx
        val item = queue[prevIdx]
        prepareAndPlay(item)
        return item
    }
}
