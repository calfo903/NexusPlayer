package com.nexusplayer.app.player.whisper

import java.io.File

enum class WhisperModelType(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val description: String
) {
    TINY(
        id = "ggml-tiny.bin",
        displayName = "Tiny (English / Fast)",
        sizeBytes = 39_000_000L,
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
        description = "Ultra-fast on-device recognition (~39 MB). Ideal for quick English transcriptions."
    ),
    BASE(
        id = "ggml-base.bin",
        displayName = "Base (Multi-Lingual / Balanced)",
        sizeBytes = 142_000_000L,
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
        description = "Balanced accuracy across 99 languages (~142 MB). Recommended for most videos."
    ),
    SMALL(
        id = "ggml-small.bin",
        displayName = "Small (High Accuracy / Studio)",
        sizeBytes = 466_000_000L,
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
        description = "Studio-grade accuracy (~466 MB). Requires more CPU/GPU processing power."
    )
}

data class WhisperSegment(
    val index: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
) {
    val formattedTimestamp: String
        get() = "${formatSrtTimestamp(startTimeMs)} --> ${formatSrtTimestamp(endTimeMs)}"

    companion object {
        fun formatSrtTimestamp(timeMs: Long): String {
            if (timeMs < 0) return "00:00:00,000"
            val totalSeconds = timeMs / 1000
            val millis = timeMs % 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
        }
    }
}

sealed class WhisperGenerationState {
    object Idle : WhisperGenerationState()

    data class DownloadingModel(
        val model: WhisperModelType,
        val progressPercentage: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedKbps: Long
    ) : WhisperGenerationState() {
        val formattedSpeed: String
            get() = if (speedKbps >= 1024) "${String.format("%.1f", speedKbps / 1024f)} MB/s" else "$speedKbps KB/s"
    }

    data class ExtractingAudio(
        val progressPercentage: Float,
        val extractedDurationMs: Long,
        val totalDurationMs: Long
    ) : WhisperGenerationState()

    data class Transcribing(
        val model: WhisperModelType,
        val progressPercentage: Float,
        val segmentsGenerated: Int,
        val latestSegment: WhisperSegment?,
        val allSegments: List<WhisperSegment>
    ) : WhisperGenerationState()

    data class Completed(
        val srtFile: File,
        val totalSegments: Int,
        val segments: List<WhisperSegment>,
        val elapsedTimeMs: Long
    ) : WhisperGenerationState()

    data class Error(val message: String, val cause: Throwable? = null) : WhisperGenerationState()
}
