package com.nexusplayer.app.player.whisper

import android.content.Context
import com.nexusplayer.app.domain.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Orchestrator for On-Device Whisper Subtitle Auto-Generation.
 * Coordinates model downloading, 16kHz audio extraction, and acoustic transcription into SubRip (.srt).
 */
class WhisperSubtitleGenerator(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val modelManager = WhisperModelManager(context)
    private val audioExtractor = AudioExtractor(context)
    private val whisperEngine = WhisperEngine(context)

    private val _generationState = MutableStateFlow<WhisperGenerationState>(WhisperGenerationState.Idle)
    val generationState: StateFlow<WhisperGenerationState> = _generationState.asStateFlow()

    private var activeJob: Job? = null

    fun isModelReady(modelType: WhisperModelType): Boolean {
        return modelManager.isModelDownloaded(modelType)
    }

    fun startGeneration(
        videoItem: VideoItem,
        modelType: WhisperModelType = WhisperModelType.BASE,
        languageCode: String = "en",
        onSrtGenerated: (File) -> Unit
    ) {
        cancel()
        activeJob = scope.launch(Dispatchers.Main) {
            try {
                // Step 1: Ensure Whisper model is downloaded
                if (!modelManager.isModelDownloaded(modelType)) {
                    modelManager.downloadModelFlow(modelType).collect { dlState ->
                        _generationState.value = dlState
                    }
                }

                val modelFile = modelManager.getModelFile(modelType)

                // Step 2: Extract & resample 16kHz PCM mono audio
                var extractedAudioFile: File? = audioExtractor.getExtractedWavFile(videoItem)
                if (extractedAudioFile == null) {
                    audioExtractor.extractAndResample16kMono(videoItem).collect { exState ->
                        _generationState.value = exState
                    }
                    extractedAudioFile = audioExtractor.getExtractedWavFile(videoItem)
                }

                val audioFile = extractedAudioFile ?: File(context.cacheDir, "placeholder.wav")

                // Step 3: Run Whisper Engine
                whisperEngine.transcribeVideoToSrt(
                    videoItem = videoItem,
                    modelType = modelType,
                    audioFile = audioFile,
                    languageCode = languageCode
                ).collect { transState ->
                    _generationState.value = transState
                    if (transState is WhisperGenerationState.Completed) {
                        onSrtGenerated(transState.srtFile)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _generationState.value = WhisperGenerationState.Error(
                    message = "Whisper Engine Error: ${e.localizedMessage ?: "Unknown failure"}",
                    cause = e
                )
            }
        }
    }

    fun cancel() {
        activeJob?.cancel()
        activeJob = null
        if (_generationState.value !is WhisperGenerationState.Completed) {
            _generationState.value = WhisperGenerationState.Idle
        }
    }

    fun reset() {
        cancel()
        _generationState.value = WhisperGenerationState.Idle
    }
}
