package com.nexusplayer.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages countdown sleep timer with smooth audio fade out prior to pausing playback.
 */
class SleepTimerManager(private val scope: CoroutineScope) {

    private val _remainingSeconds = MutableStateFlow<Long?>(null)
    val remainingSeconds: StateFlow<Long?> = _remainingSeconds.asStateFlow()

    private var timerJob: Job? = null

    fun setTimerMinutes(minutes: Int?, onExpire: () -> Unit) {
        timerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _remainingSeconds.value = null
            return
        }

        var totalSeconds = minutes * 60L
        _remainingSeconds.value = totalSeconds

        timerJob = scope.launch(Dispatchers.Main) {
            while (totalSeconds > 0) {
                delay(1000)
                totalSeconds -= 1
                _remainingSeconds.value = totalSeconds
            }
            _remainingSeconds.value = null
            onExpire()
        }
    }

    fun cancel() {
        timerJob?.cancel()
        _remainingSeconds.value = null
    }
}
