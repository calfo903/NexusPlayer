package com.nexusplayer.app.domain.model

data class SubtitleTrack(
    val id: String,
    val language: String,
    val label: String,
    val mimeType: String,
    val isExternal: Boolean = false,
    val externalUri: String? = null,
    val isSelected: Boolean = false
)

data class AudioTrackInfo(
    val id: String,
    val language: String,
    val label: String,
    val channels: Int,
    val sampleRate: Int,
    val bitrate: Int,
    val isSelected: Boolean = false
) {
    val channelLabel: String
        get() = when (channels) {
            1 -> "Mono"
            2 -> "Stereo"
            6 -> "5.1 Surround"
            8 -> "7.1 Surround"
            else -> "$channels ch"
        }
}

data class CodecInfo(
    val videoCodec: String,
    val videoResolution: String,
    val videoFrameRate: Float,
    val videoBitrateKbps: Int,
    val isHardwareAccelerated: Boolean,
    val audioCodec: String,
    val audioSampleRateHz: Int,
    val audioChannels: Int,
    val containerFormat: String
)
