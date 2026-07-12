package com.nexusplayer.app.domain.model

enum class AspectRatioMode(val label: String) {
    FIT("Fit Screen"),
    FILL("Fill / Crop"),
    SIXTEEN_NINE("16:9"),
    FOUR_THREE("4:3"),
    ZOOM("Zoom 150%"),
    ORIGINAL("Original Size")
}

enum class DecoderPriority(val label: String) {
    AUTO("Auto (Hardware Preferred)"),
    HW_ONLY("Hardware Only (MediaCodec)"),
    SW_PREFERRED("Software Fallback (FFmpeg / Exoplayer SW)")
}

data class PlayerSettings(
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val playbackSpeed: Float = 1.0f,
    val decoderPriority: DecoderPriority = DecoderPriority.AUTO,
    val autoEnterPip: Boolean = true,
    val backgroundPlaybackEnabled: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val gestureBrightnessEnabled: Boolean = true,
    val gestureVolumeEnabled: Boolean = true,
    val gestureSeekScrubbingEnabled: Boolean = true,
    val gestureSpeedSwipeEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val autoNextEpisodeEnabled: Boolean = true,
    val rememberResumePosition: Boolean = true
)
