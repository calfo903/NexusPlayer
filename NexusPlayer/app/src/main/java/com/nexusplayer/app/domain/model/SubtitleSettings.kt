package com.nexusplayer.app.domain.model

/**
 * Customizable styling options for the VLC/MX Player hybrid subtitle rendering engine.
 */
data class SubtitleSettings(
    val fontSizeSp: Float = 22f,
    val textColorHex: Long = 0xFFFFFFFF, // Pure White
    val backgroundColorHex: Long = 0x80000000, // Semi-transparent black
    val strokeColorHex: Long = 0xFF000000, // Black outline
    val strokeWidthPx: Float = 4f,
    val timeOffsetMs: Long = 0L, // ±10 seconds adjustment (-10000 to +10000)
    val bottomMarginDp: Float = 32f,
    val isEnabled: Boolean = true
)
