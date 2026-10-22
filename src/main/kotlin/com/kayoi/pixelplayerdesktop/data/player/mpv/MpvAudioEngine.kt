package com.kayoi.pixelplayerdesktop.data.player.mpv

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

import com.sun.jna.Pointer
import com.sun.jna.Structure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.kayoi.pixelplayerdesktop.data.player.ReplayGainManager
import com.kayoi.pixelplayerdesktop.domain.player.AudioEngine
import com.kayoi.pixelplayerdesktop.domain.player.PlaybackState

class MpvAudioEngine(
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : AudioEngine {

    private data class PlayerPair(
        val active: Pointer?,
        val standby: Pointer?
    )

    private class CrossfadeSession(
        val outgoingPlayer: Pointer,
        val incomingPlayer: Pointer,
        val incomingMultiplier: Float
    ) {
        var job: Job? = null
    }

    private val isReleased = AtomicBoolean(false)
    private val engineJob = SupervisorJob(coroutineScope.coroutineContext[Job])
    private val engineScope = CoroutineScope(coroutineScope.coroutineContext + engineJob)

    override fun isClosed(): Boolean = isReleased.get()

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    override val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    override val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private val _completionEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    override val completionEvents: SharedFlow<Unit> = _completionEvents.asSharedFlow()

    private val _isCrossfading = MutableStateFlow(false)
    override val isCrossfading: StateFlow<Boolean> = _isCrossfading.asStateFlow()

    private val api: MpvApi = MpvApi.INSTANCE
    private val playerPair = AtomicReference(PlayerPair(null, null))
    private val commandLock = Any()
    private var activeCrossfadeSession: CrossfadeSession? = null

    private var handleA: Pointer? = null
    private var handleB: Pointer? = null
    private var eventThreadA: Thread? = null
    private var eventThreadB: Thread? = null

    private var positionTickerJob: Job? = null
    private var playPauseFadeJob: Job? = null

    private val replayGainManager = ReplayGainManager()
    private var replayGainEnabled = false
    private var replayGainMode = "track"
    private var currentReplayGainMultiplier = 1.0f
    private var currentTrackPath: String? = null

    private var equalizerEnabled = false
    private var equalizerBands: List<Float> = List(10) { 0f }
    private var equalizerPreamp = 0f

    init {
        initMpvPair()
    }

    private fun initMpvPair() {
        val pA = createConfiguredHandle()
        val pB = createConfiguredHandle()
        handleA = pA
        handleB = pB
        playerPair.set(PlayerPair(active = pA, standby = pB))

        eventThreadA = startEventLoop(pA, "PixelPlayer-MPV-A")
        eventThreadB = startEventLoop(pB, "PixelPlayer-MPV-B")
    }

    private fun createConfiguredHandle(): Pointer {
        val handle = api.mpv_create() ?: throw IllegalStateException("mpv_create() returned null pointer")

        api.mpv_set_property_string(handle, "vo", "null")
        api.mpv_set_property_string(handle, "video", "no")
        api.mpv_set_property_string(handle, "terminal", "no")
        api.mpv_set_property_string(handle, "msg-level", "all=no")
        api.mpv_set_property_string(handle, "gapless-audio", "yes")
        api.mpv_set_property_string(handle, "audio-client-name", "PixelPlayer")
        api.mpv_set_property_string(handle, "audio-pitch-correction", "yes")
        api.mpv_set_property_string(handle, "ao", "pipewire,pulse,alsa,wasapi,coreaudio,null")

        val status = api.mpv_initialize(handle)
        if (status < 0) {
            val errStr = api.mpv_error_string(status)
            api.mpv_terminate_destroy(handle)
            throw IllegalStateException("mpv_initialize failed: $errStr")
        }

        api.mpv_observe_property(handle, 1L, "time-pos", MpvApi.MPV_FORMAT_DOUBLE)
        api.mpv_observe_property(handle, 2L, "duration", MpvApi.MPV_FORMAT_DOUBLE)
        api.mpv_observe_property(handle, 3L, "pause", MpvApi.MPV_FORMAT_FLAG)
        api.mpv_observe_property(handle, 4L, "idle-active", MpvApi.MPV_FORMAT_FLAG)
        api.mpv_observe_property(handle, 5L, "paused-for-cache", MpvApi.MPV_FORMAT_FLAG)

        setVolumeOnHandle(handle, getEffectiveVolume())
        return handle
    }

    private fun startEventLoop(handle: Pointer, threadName: String): Thread {
        return thread(name = threadName, isDaemon = true) {
            while (!isClosed()) {
                val eventRef = api.mpv_wait_event(handle, 0.1) ?: continue
                val eventId = eventRef.eventId
                if (eventId == MpvApi.MPV_EVENT_NONE) continue
                if (eventId == MpvApi.MPV_EVENT_SHUTDOWN) break

                handleMpvEvent(handle, eventRef)
            }
        }
    }

    private fun handleMpvEvent(sourceHandle: Pointer, event: MpvEvent.ByReference) {
        val isActivePlayer = sourceHandle == playerPair.get().active

        when (event.eventId) {
            MpvApi.MPV_EVENT_PROPERTY_CHANGE -> {
                if (!isActivePlayer) return
                val dataPtr = event.data ?: return
                val prop = Structure.newInstance(MpvEventProperty::class.java, dataPtr).apply { read() }
                when (prop.name) {
                    "time-pos" -> {
                        if (prop.format == MpvApi.MPV_FORMAT_DOUBLE && prop.data != null) {
                            val seconds = prop.data!!.getDouble(0)
                            _currentPositionMs.value = (seconds * 1000).toLong().coerceAtLeast(0L)
                        }
                    }
                    "duration" -> {
                        if (prop.format == MpvApi.MPV_FORMAT_DOUBLE && prop.data != null) {
                            val seconds = prop.data!!.getDouble(0)
                            _durationMs.value = (seconds * 1000).toLong().coerceAtLeast(0L)
                        }
                    }
                    "pause" -> {
                        if (prop.format == MpvApi.MPV_FORMAT_FLAG && prop.data != null) {
                            val isPaused = prop.data!!.getInt(0) != 0
                            if (isPaused && _playbackState.value == PlaybackState.PLAYING) {
                                _playbackState.value = PlaybackState.PAUSED
                                stopPositionTicker()
                            } else if (!isPaused && _playbackState.value == PlaybackState.PAUSED) {
                                _playbackState.value = PlaybackState.PLAYING
                                startPositionTicker()
                            }
                        }
                    }
                    "idle-active" -> {
                        if (prop.format == MpvApi.MPV_FORMAT_FLAG && prop.data != null) {
                            val isIdle = prop.data!!.getInt(0) != 0
                            if (isIdle && _playbackState.value != PlaybackState.ENDED && _playbackState.value != PlaybackState.ERROR) {
                                _playbackState.value = PlaybackState.IDLE
                                stopPositionTicker()
                            }
                        }
                    }
                    "paused-for-cache" -> {
                        if (prop.format == MpvApi.MPV_FORMAT_FLAG && prop.data != null) {
                            val isBuffering = prop.data!!.getInt(0) != 0
                            if (isBuffering && _playbackState.value == PlaybackState.PLAYING) {
                                _playbackState.value = PlaybackState.BUFFERING
                            } else if (!isBuffering && _playbackState.value == PlaybackState.BUFFERING) {
                                _playbackState.value = PlaybackState.PLAYING
                            }
                        }
                    }
                }
            }
            MpvApi.MPV_EVENT_START_FILE -> {
                if (isActivePlayer) {
                    _playbackState.value = PlaybackState.BUFFERING
                }
            }
            MpvApi.MPV_EVENT_FILE_LOADED -> {
                if (isActivePlayer) {
                    _playbackState.value = PlaybackState.PLAYING
                    startPositionTicker()
                }
            }
            MpvApi.MPV_EVENT_END_FILE -> {
                if (isActivePlayer && !_isCrossfading.value) {
                    val dataPtr = event.data
                    val reason = if (dataPtr != null) {
                        val endFile = Structure.newInstance(MpvEventEndFile::class.java, dataPtr).apply { read() }
                        endFile.reason
                    } else {
                        MpvApi.MPV_END_FILE_REASON_EOF
                    }
                    if (reason == MpvApi.MPV_END_FILE_REASON_EOF) {
                        _playbackState.value = PlaybackState.ENDED
                        _currentPositionMs.value = _durationMs.value
                        stopPositionTicker()
                        _completionEvents.tryEmit(Unit)
                    } else if (reason == MpvApi.MPV_END_FILE_REASON_ERROR) {
                        _playbackState.value = PlaybackState.ERROR
                        stopPositionTicker()
                        _errorEvents.tryEmit("Playback error encountered in audio track")
                    }
                }
            }
        }
    }

    private fun updateReplayGainMultiplier(filePath: String) {
        if (!replayGainEnabled) {
            currentReplayGainMultiplier = 1.0f
            return
        }
        val values = replayGainManager.readReplayGain(filePath)
        val useAlbum = replayGainMode.equals("album", ignoreCase = true)
        currentReplayGainMultiplier = replayGainManager.getVolumeMultiplier(values, useAlbum)
    }

    private fun getEffectiveVolume(multiplier: Float = currentReplayGainMultiplier): Float {
        if (_isMuted.value) return 0f
        val rawVol = _volume.value * multiplier
        return (rawVol * 100f).coerceIn(0f, 100f)
    }

    private fun setVolumeOnHandle(handle: Pointer?, volumePercent: Float) {
        if (handle == null || isClosed()) return
        api.mpv_set_property_string(handle, "volume", volumePercent.coerceIn(0f, 100f).toString())
    }

    private fun getVolumeOnHandle(handle: Pointer?): Float {
        if (handle == null || isClosed()) return 0f
        val str = api.mpv_get_property_string(handle, "volume") ?: return 0f
        return str.toFloatOrNull() ?: 0f
    }

    private fun cancelCrossfade() {
        val session = synchronized(commandLock) {
            val s = activeCrossfadeSession ?: return
            activeCrossfadeSession = null
            s
        }
        session.job?.cancel()
        currentReplayGainMultiplier = session.incomingMultiplier
        if (!isClosed()) {
            try {
                setVolumeOnHandle(session.outgoingPlayer, 0f)
                api.mpv_set_property_string(session.outgoingPlayer, "pause", "yes")
                api.mpv_command(session.outgoingPlayer, arrayOf("stop"))
                val normalVol = getEffectiveVolume(session.incomingMultiplier)
                setVolumeOnHandle(session.outgoingPlayer, normalVol)
                setVolumeOnHandle(session.incomingPlayer, normalVol)
            } catch (_: Exception) {}
        }
        _isCrossfading.value = false
    }

    override fun play(mediaPathOrUri: String): Unit = synchronized(commandLock) {
        if (isClosed()) return
        cancelCrossfade()
        playPauseFadeJob?.cancel()
        _isCrossfading.value = false

        val (player, standby) = playerPair.get()
        try {
            standby?.let {
                api.mpv_set_property_string(it, "pause", "yes")
                api.mpv_command(it, arrayOf("stop"))
            }
        } catch (_: Exception) {}

        _currentPositionMs.value = 0L
        _durationMs.value = 0L

        val file = File(mediaPathOrUri)
        val validPath = if (file.exists()) file.absolutePath else mediaPathOrUri

        if (file.exists() && !file.canRead()) {
            _playbackState.value = PlaybackState.ERROR
            _errorEvents.tryEmit("File is not readable: $mediaPathOrUri")
            return
        }

        currentTrackPath = validPath
        updateReplayGainMultiplier(validPath)

        if (player == null) return

        try {
            _playbackState.value = PlaybackState.BUFFERING
            val targetVol = getEffectiveVolume()
            setVolumeOnHandle(player, targetVol)

            api.mpv_command(player, arrayOf("loadfile", validPath, "replace"))
            api.mpv_set_property_string(player, "pause", "no")
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
            _errorEvents.tryEmit(e.message ?: "Failed to play media")
        }
    }

    override fun play(): Unit = synchronized(commandLock) {
        if (isClosed()) return
        val (player, standby) = playerPair.get()
        if (player == null) return
        playPauseFadeJob?.cancel()

        try {
            _playbackState.value = PlaybackState.PLAYING
            startPositionTicker()

            if (_isCrossfading.value) {
                api.mpv_set_property_string(player, "pause", "no")
                standby?.let { api.mpv_set_property_string(it, "pause", "no") }
                return
            }

            val targetVol = getEffectiveVolume()
            if (targetVol <= 0f) {
                api.mpv_set_property_string(player, "pause", "no")
                return
            }

            setVolumeOnHandle(player, 0f)
            api.mpv_set_property_string(player, "pause", "no")

            playPauseFadeJob = engineScope.launch {
                val durationMs = 300L
                val steps = 15
                val stepDelay = durationMs / steps
                for (i in 1..steps) {
                    if (!isActive || isClosed()) break
                    val progress = i.toFloat() / steps
                    val currentStepVol = progress * targetVol
                    setVolumeOnHandle(player, currentStepVol)
                    delay(stepDelay)
                }
                if (!isClosed()) {
                    setVolumeOnHandle(player, targetVol)
                }
            }
        } catch (e: Exception) {
            _errorEvents.tryEmit(e.message ?: "Failed to resume playback")
        }
    }

    override fun pause(): Unit = synchronized(commandLock) {
        if (isClosed()) return
        val (player, _) = playerPair.get()
        if (player == null || _playbackState.value == PlaybackState.IDLE) {
            return
        }
        playPauseFadeJob?.cancel()

        try {
            _playbackState.value = PlaybackState.PAUSED
            stopPositionTicker()

            if (_isCrossfading.value || activeCrossfadeSession != null) {
                cancelCrossfade()
                api.mpv_set_property_string(player, "pause", "yes")
                return
            }

            val targetVol = getEffectiveVolume()
            val currentVol = getVolumeOnHandle(player)
            val startVol = currentVol.coerceIn(0f, 100f)

            playPauseFadeJob = engineScope.launch {
                val durationMs = 300L
                val steps = 15
                val stepDelay = durationMs / steps
                for (i in 1..steps) {
                    if (!isActive || isClosed()) break
                    val progress = i.toFloat() / steps
                    val currentStepVol = startVol - progress * startVol
                    setVolumeOnHandle(player, currentStepVol)
                    delay(stepDelay)
                }
                if (!isClosed()) {
                    api.mpv_set_property_string(player, "pause", "yes")
                    setVolumeOnHandle(player, targetVol)
                }
            }
        } catch (e: Exception) {
            _errorEvents.tryEmit(e.message ?: "Failed to pause playback")
        }
    }

    override fun stop(): Unit = synchronized(commandLock) {
        if (isClosed()) return
        cancelCrossfade()
        playPauseFadeJob?.cancel()
        stopPositionTicker()

        val (player, standby) = playerPair.get()
        try {
            if (player != null) {
                api.mpv_command(player, arrayOf("stop"))
            }
            if (standby != null) {
                api.mpv_command(standby, arrayOf("stop"))
            }
        } catch (_: Exception) {}

        _playbackState.value = PlaybackState.IDLE
        _currentPositionMs.value = 0L
    }

    override fun seekTo(positionMs: Long): Unit = synchronized(commandLock) {
        if (isClosed()) return
        playPauseFadeJob?.cancel()
        if (_isCrossfading.value || activeCrossfadeSession != null) {
            cancelCrossfade()
        }
        val (player, _) = playerPair.get() ?: return
        if (player == null) return
        val effectiveDuration = _durationMs.value
        val clampedMs = if (effectiveDuration > 500L) {
            positionMs.coerceIn(0L, effectiveDuration - 300L)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        val seconds = (clampedMs / 1000.0).toString()
        api.mpv_command(player, arrayOf("seek", seconds, "absolute"))
        _currentPositionMs.value = clampedMs
    }

    override fun setVolume(volume: Float): Unit = synchronized(commandLock) {
        if (isClosed()) return
        _volume.value = volume.coerceIn(0.0f, 1.0f)
        playPauseFadeJob?.cancel()
        applyEffectiveVolume()
    }

    override fun setMute(muted: Boolean): Unit = synchronized(commandLock) {
        if (isClosed()) return
        _isMuted.value = muted
        playPauseFadeJob?.cancel()
        val (player, standby) = playerPair.get()
        val muteStr = if (muted) "yes" else "no"
        player?.let { api.mpv_set_property_string(it, "mute", muteStr) }
        standby?.let { api.mpv_set_property_string(it, "mute", muteStr) }
    }

    private fun applyEffectiveVolume() {
        if (!_isCrossfading.value && playPauseFadeJob?.isActive != true) {
            val (player, standby) = playerPair.get()
            if (!isClosed()) {
                val targetVol = getEffectiveVolume()
                player?.let { setVolumeOnHandle(it, targetVol) }
                standby?.let { setVolumeOnHandle(it, targetVol) }
            }
        }
    }

    override fun setReplayGain(enabled: Boolean, mode: String): Unit = synchronized(commandLock) {
        if (isClosed()) return
        replayGainEnabled = enabled
        replayGainMode = mode
        val path = currentTrackPath
        if (path != null) {
            updateReplayGainMultiplier(path)
            applyEffectiveVolume()
        }
    }

    override fun setEqualizer(enabled: Boolean, bands: List<Float>, preamp: Float): Unit = synchronized(commandLock) {
        equalizerEnabled = enabled
        equalizerBands = bands
        equalizerPreamp = preamp
        applyEqualizerToPlayers()
    }

    private fun applyEqualizerToPlayers() {
        if (isClosed()) return
        val (player, standby) = playerPair.get()

        val afString = if (!equalizerEnabled) {
            ""
        } else {
            val frequencies = listOf(31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
            val filterList = mutableListOf<String>()

            if (equalizerPreamp != 0f) {
                filterList.add("volume=volume=${equalizerPreamp}dB")
            }

            frequencies.forEachIndexed { index, freq ->
                val gain = equalizerBands.getOrElse(index) { 0f }
                if (gain != 0f) {
                    filterList.add("equalizer=f=$freq:width_type=o:w=1:g=$gain")
                }
            }
            if (filterList.isEmpty()) "" else filterList.joinToString(",")
        }

        player?.let { api.mpv_set_property_string(it, "af", afString) }
        standby?.let { api.mpv_set_property_string(it, "af", afString) }
    }

    override fun crossfadeTo(mediaPathOrUri: String, durationMs: Long): Unit = synchronized(commandLock) {
        if (isClosed()) return
        if (activeCrossfadeSession != null || _isCrossfading.value) {
            cancelCrossfade()
        }
        playPauseFadeJob?.cancel()

        val (outgoingPlayer, incomingPlayer) = playerPair.get()
        if (outgoingPlayer == null || incomingPlayer == null || _playbackState.value != PlaybackState.PLAYING) {
            play(mediaPathOrUri)
            return
        }

        val file = File(mediaPathOrUri)
        val validPath = if (file.exists()) file.absolutePath else mediaPathOrUri

        if (file.exists() && !file.canRead()) {
            _playbackState.value = PlaybackState.ERROR
            _errorEvents.tryEmit("File is not readable: $mediaPathOrUri")
            return
        }

        val outgoingMultiplier = currentReplayGainMultiplier
        val incomingValues = if (replayGainEnabled) replayGainManager.readReplayGain(validPath) else null
        val incomingMultiplier = if (replayGainEnabled) {
            replayGainManager.getVolumeMultiplier(incomingValues, replayGainMode.equals("album", ignoreCase = true))
        } else 1.0f
        currentTrackPath = validPath

        val session = CrossfadeSession(
            outgoingPlayer = outgoingPlayer,
            incomingPlayer = incomingPlayer,
            incomingMultiplier = incomingMultiplier
        )

        try {
            _isCrossfading.value = true
            _currentPositionMs.value = 0L
            _durationMs.value = 0L

            playerPair.set(PlayerPair(active = incomingPlayer, standby = outgoingPlayer))

            setVolumeOnHandle(incomingPlayer, 0f)
            api.mpv_command(incomingPlayer, arrayOf("loadfile", validPath, "replace"))
            api.mpv_set_property_string(incomingPlayer, "pause", "no")

            val clampedDuration = durationMs.coerceIn(500L, 12000L)
            val stepMs = 50L

            activeCrossfadeSession = session

            val job = engineScope.launch {
                var elapsedMs = 0L
                try {
                    while (isActive) {
                        if (_playbackState.value == PlaybackState.PLAYING) {
                            val progress = (elapsedMs.toFloat() / clampedDuration).coerceIn(0f, 1f)
                            val baseVol = _volume.value
                            val isMuted = _isMuted.value

                            val outTarget = if (isMuted) 0f else (1f - progress) * baseVol * outgoingMultiplier * 100f
                            val inTarget = if (isMuted) 0f else progress * baseVol * incomingMultiplier * 100f

                            setVolumeOnHandle(outgoingPlayer, outTarget)
                            setVolumeOnHandle(incomingPlayer, inTarget)

                            if (progress >= 1f) break
                            elapsedMs += stepMs
                        }
                        delay(stepMs)
                    }
                } finally {
                    val shouldCleanUp = synchronized(commandLock) {
                        if (activeCrossfadeSession === session) {
                            activeCrossfadeSession = null
                            true
                        } else {
                            false
                        }
                    }
                    if (shouldCleanUp) {
                        currentReplayGainMultiplier = incomingMultiplier
                        if (!isClosed()) {
                            try {
                                setVolumeOnHandle(outgoingPlayer, 0f)
                                api.mpv_set_property_string(outgoingPlayer, "pause", "yes")
                                api.mpv_command(outgoingPlayer, arrayOf("stop"))
                                val normalVol = getEffectiveVolume(incomingMultiplier)
                                setVolumeOnHandle(outgoingPlayer, normalVol)
                                setVolumeOnHandle(incomingPlayer, normalVol)
                            } catch (_: Exception) {}
                        }
                        _isCrossfading.value = false
                    }
                }
            }
            session.job = job
        } catch (e: Exception) {
            if (activeCrossfadeSession === session) {
                activeCrossfadeSession = null
            }
            _isCrossfading.value = false
            play(mediaPathOrUri)
        }
    }

    private fun startPositionTicker() {
        if (isClosed()) return
        positionTickerJob?.cancel()
        positionTickerJob = engineScope.launch {
            while (isActive && !isClosed()) {
                val active = playerPair.get().active
                if (active != null && _playbackState.value == PlaybackState.PLAYING) {
                    val timeStr = api.mpv_get_property_string(active, "time-pos")
                    val sec = timeStr?.toDoubleOrNull()
                    if (sec != null) {
                        _currentPositionMs.value = (sec * 1000).toLong().coerceAtLeast(0L)
                    }
                    if (_durationMs.value == 0L) {
                        val durStr = api.mpv_get_property_string(active, "duration")
                        val durSec = durStr?.toDoubleOrNull()
                        if (durSec != null && durSec > 0.0) {
                            _durationMs.value = (durSec * 1000).toLong().coerceAtLeast(0L)
                        }
                    }
                }
                delay(100L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    internal fun getActivePlayerForTest(): Pointer? = playerPair.get().active
    internal fun getStandbyPlayerForTest(): Pointer? = playerPair.get().standby

    override fun release(): Unit = synchronized(commandLock) {
        if (!isReleased.compareAndSet(false, true)) return
        cancelCrossfade()
        playPauseFadeJob?.cancel()
        stopPositionTicker()

        val pA = handleA
        val pB = handleB
        handleA = null
        handleB = null
        playerPair.set(PlayerPair(null, null))

        try {
            if (pA != null) {
                api.mpv_command(pA, arrayOf("stop"))
                api.mpv_terminate_destroy(pA)
            }
            if (pB != null) {
                api.mpv_command(pB, arrayOf("stop"))
                api.mpv_terminate_destroy(pB)
            }
        } catch (_: Exception) {}

        _playbackState.value = PlaybackState.IDLE
        _currentPositionMs.value = 0L
        _durationMs.value = 0L

        engineJob.cancel()
    }
}
