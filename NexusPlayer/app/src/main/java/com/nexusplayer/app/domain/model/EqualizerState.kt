package com.nexusplayer.app.domain.model

data class EqualizerBand(
    val index: Short,
    val centerFreqHz: Int,
    val currentLevelMillibels: Short,
    val minLevelMillibels: Short,
    val maxLevelMillibels: Short
) {
    val formattedFreq: String
        get() = if (centerFreqHz >= 1000) "${centerFreqHz / 1000}kHz" else "${centerFreqHz}Hz"
}

data class EqualizerState(
    val isEnabled: Boolean = false,
    val bands: List<EqualizerBand> = emptyList(),
    val bassBoostStrength: Short = 0, // 0 to 1000
    val virtualizerStrength: Short = 0, // 0 to 1000
    val currentPresetIndex: Int = -1,
    val presets: List<String> = listOf("Normal", "Classical", "Dance", "Flat", "Folk", "Heavy Metal", "Hip Hop", "Jazz", "Pop", "Rock")
)
