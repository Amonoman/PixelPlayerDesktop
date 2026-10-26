package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.player.QueueManager
import com.kayoi.pixelplayerdesktop.domain.player.AudioEngine
import com.kayoi.pixelplayerdesktop.domain.player.SleepTimerSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SleepTimerStateHolder(
    private val session: SleepTimerSession,
    private val audioEngine: AudioEngine,
    private val queueManager: QueueManager,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    val timer: StateFlow<SleepTimerSession.Timer?> = session.timer
    val countedPlay: StateFlow<SleepTimerSession.CountedPlay?> = session.countedPlay
    val events = session.events

    private val _isEndOfTrackTimerActive = MutableStateFlow(false)
    val isEndOfTrackTimerActive: StateFlow<Boolean> = _isEndOfTrackTimerActive.asStateFlow()

    private val _activeTimerDurationMinutes = MutableStateFlow<Int?>(null)
    val activeTimerDurationMinutes: StateFlow<Int?> = _activeTimerDurationMinutes.asStateFlow()

    private val _activeTimerValueDisplay = MutableStateFlow<String?>(null)
    val activeTimerValueDisplay: StateFlow<String?> = _activeTimerValueDisplay.asStateFlow()

    private val _remainingSeconds = MutableStateFlow<Long?>(null)
    val remainingSeconds: StateFlow<Long?> = _remainingSeconds.asStateFlow()

    private val _playCount = MutableStateFlow(1f)
    val playCount: StateFlow<Float> = _playCount.asStateFlow()

    private var durationJob: Job? = null
    private var repeatModeBeforeCountedPlay: Int? = null

    init {
        coroutineScope.launch {
            session.timer.collect { currentTimer ->
                _isEndOfTrackTimerActive.value = currentTimer is SleepTimerSession.Timer.EndOfTrack
                _activeTimerDurationMinutes.value = (currentTimer as? SleepTimerSession.Timer.Duration)?.minutes
                _activeTimerValueDisplay.value = when (currentTimer) {
                    is SleepTimerSession.Timer.Duration -> "${currentTimer.minutes} min"
                    is SleepTimerSession.Timer.EndOfTrack -> "End of track"
                    null -> null
                }
                if (currentTimer !is SleepTimerSession.Timer.Duration) {
                    _remainingSeconds.value = null
                }
            }
        }

        coroutineScope.launch {
            session.countedPlay.collect { counted ->
                _playCount.value = counted?.total?.toFloat() ?: 1f
            }
        }
    }

    fun setSleepTimer(durationMinutes: Int) {
        cancelTimer()
        if (durationMinutes <= 0) return

        val durationMs = durationMinutes * 60_000L
        val endsAtMillis = System.currentTimeMillis() + durationMs
        session.setTimer(SleepTimerSession.Timer.Duration(endsAtMillis, durationMinutes))

        durationJob = coroutineScope.launch {
            while (isActive) {
                val remainingMs = endsAtMillis - System.currentTimeMillis()
                if (remainingMs <= 0L) {
                    _remainingSeconds.value = 0L
                    break
                }
                _remainingSeconds.value = (remainingMs + 999L) / 1000L
                val delayTime = remainingMs.coerceAtMost(1000L)
                delay(delayTime)
            }
            onDurationExpired()
        }
    }

    private fun onDurationExpired() {
        if (session.timer.value !is SleepTimerSession.Timer.Duration) return
        cancelTimer()
        audioEngine.pause()
        session.emit(SleepTimerSession.Event.DurationExpired)
    }

    fun setEndOfTrackTimer(enabled: Boolean) {
        if (!enabled) {
            if (session.timer.value is SleepTimerSession.Timer.EndOfTrack) {
                cancelTimer()
            }
            return
        }

        val currentSong = queueManager.currentSong.value ?: return
        cancelTimer()
        stopCountedPlay(restoreRepeatMode = true)
        session.setTimer(SleepTimerSession.Timer.EndOfTrack(currentSong.id))
    }

    fun playCounted(count: Int) {
        val currentSong = queueManager.currentSong.value ?: return
        val validCount = count.coerceIn(1, 10)
        stopCountedPlay(restoreRepeatMode = true)
        if (session.timer.value is SleepTimerSession.Timer.EndOfTrack) {
            cancelTimer()
        }

        repeatModeBeforeCountedPlay = queueManager.repeatMode.value
        session.setCountedPlay(SleepTimerSession.CountedPlay(currentSong.id, validCount, 1))
        queueManager.setRepeatMode(2)
    }

    fun stopCountedPlay(restoreRepeatMode: Boolean = true) {
        if (session.countedPlay.value == null) return
        val prevMode = repeatModeBeforeCountedPlay
        session.setCountedPlay(null)
        repeatModeBeforeCountedPlay = null
        if (restoreRepeatMode && prevMode != null) {
            queueManager.setRepeatMode(prevMode)
        }
    }

    fun cancelTimer() {
        durationJob?.cancel()
        durationJob = null
        _remainingSeconds.value = null
        if (session.timer.value != null) {
            session.setTimer(null)
        }
    }

    fun cancelAll() {
        cancelTimer()
        stopCountedPlay(restoreRepeatMode = true)
    }
}
