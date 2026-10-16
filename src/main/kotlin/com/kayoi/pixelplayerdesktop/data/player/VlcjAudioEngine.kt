package com.kayoi.pixelplayerdesktop.data.player

import com.kayoi.pixelplayerdesktop.domain.player.AudioEngine
import com.kayoi.pixelplayerdesktop.domain.player.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import com.sun.jna.NativeLibrary
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.Equalizer
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class VlcjAudioEngine(
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : AudioEngine {

    companion object {
        private const val VLC_EQUALIZER_PREAMP_COMPENSATION_DB = 12.0f
    }

    private var factory: MediaPlayerFactory? = null
    private var playerA: MediaPlayer? = null
    private var playerB: MediaPlayer? = null
    private var equalizerA: Equalizer? = null
    private var equalizerB: Equalizer? = null
    private var activePlayer: MediaPlayer? = null
    private var standbyPlayer: MediaPlayer? = null

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

    private var positionTickerJob: Job? = null
    private var crossfadeJob: Job? = null
    private var playPauseFadeJob: Job? = null
    private val _isCrossfading = MutableStateFlow(false)
    override val isCrossfading: StateFlow<Boolean> = _isCrossfading.asStateFlow()
    private var isInitialized = false
    private var replayGainEnabled = false
    private var replayGainMode = "track"
    private val replayGainManager = ReplayGainManager()
    private var currentReplayGainMultiplier: Float = 1.0f
    private var currentTrackPath: String? = null
    private var equalizerEnabled = false
    private var equalizerBands: List<Float> = List(10) { 0f }
    private var equalizerPreamp: Float = 0f

    init {
        initVlcj()
    }

    private fun setupNativeDiscovery(): File? {
        try {
            val appResourceDir = System.getProperty("compose.application.resources.dir")?.let { File(it) }
            val userDir = File(System.getProperty("user.dir", "."))
            val codeSourceLocation = try {
                VlcjAudioEngine::class.java.protectionDomain?.codeSource?.location?.toURI()?.let { File(it) }
            } catch (_: Exception) {
                null
            }

            val candidates = listOfNotNull(
                File(userDir, "vlc"),
                File(userDir, "app/vlc"),
                File(userDir, "resources/vlc"),
                File(userDir, "app/resources/vlc"),
                appResourceDir?.resolve("vlc"),
                appResourceDir,
                codeSourceLocation?.parentFile?.resolve("vlc"),
                codeSourceLocation?.parentFile?.parentFile?.resolve("vlc"),
                codeSourceLocation?.parentFile?.parentFile?.resolve("resources/vlc"),
                codeSourceLocation?.parentFile?.parentFile?.resolve("app/resources/vlc"),
                File("vlc")
            )

            val vlcDir = candidates.firstOrNull { dir ->
                dir.exists() && (
                    File(dir, "libvlc.dll").exists() ||
                    File(dir, "libvlc.so").exists() ||
                    File(dir, "libvlc.so.5").exists()
                )
            }

            if (vlcDir != null) {
                val path = vlcDir.absolutePath
                NativeLibrary.addSearchPath("libvlc", path)
                NativeLibrary.addSearchPath("libvlccore", path)
                NativeLibrary.addSearchPath("vlc", path)
                NativeLibrary.addSearchPath("vlccore", path)
                System.setProperty("jna.library.path", path)

                val plugins = File(vlcDir, "plugins")
                if (plugins.exists()) {
                    System.setProperty("VLC_PLUGIN_PATH", plugins.absolutePath)
                }
            }
            return vlcDir
        } catch (_: Exception) {
            return null
        }
    }

    private fun createEventListener(): MediaPlayerEventAdapter {
        return object : MediaPlayerEventAdapter() {
            override fun playing(mp: MediaPlayer) {
                if (mp === activePlayer && mp.status().isPlaying) {
                    _playbackState.value = PlaybackState.PLAYING
                    startPositionTicker()
                }
            }

            override fun paused(mp: MediaPlayer) {
                if (mp === activePlayer && !mp.status().isPlaying) {
                    _playbackState.value = PlaybackState.PAUSED
                    stopPositionTicker()
                }
            }

            override fun stopped(mp: MediaPlayer) {
                if (mp === activePlayer && !_isCrossfading.value) {
                    _playbackState.value = PlaybackState.IDLE
                    stopPositionTicker()
                    _currentPositionMs.value = 0L
                }
            }

            override fun finished(mp: MediaPlayer) {
                if (mp === activePlayer && !_isCrossfading.value) {
                    _playbackState.value = PlaybackState.ENDED
                    stopPositionTicker()
                    _completionEvents.tryEmit(Unit)
                }
            }

            override fun lengthChanged(mp: MediaPlayer, newLength: Long) {
                if (mp === activePlayer && newLength > 0) {
                    _durationMs.value = newLength
                }
            }

            override fun timeChanged(mp: MediaPlayer, newTime: Long) {
                if (mp === activePlayer && newTime >= 0) {
                    _currentPositionMs.value = newTime
                }
            }

            override fun error(mp: MediaPlayer) {
                if (mp === activePlayer) {
                    _playbackState.value = PlaybackState.ERROR
                    stopPositionTicker()
                    _errorEvents.tryEmit("VLC playback error encountered")
                }
            }
        }
    }

    private fun initVlcj() {
        try {
            val vlcDir = setupNativeDiscovery()
            val factoryArgs = mutableListOf(
                "--no-video",
                "--no-video-title-show",
                "--no-osd",
                "--quiet"
            )
            val f = MediaPlayerFactory(*factoryArgs.toTypedArray())
            factory = f
            val pA = f.mediaPlayers().newMediaPlayer()
            val pB = f.mediaPlayers().newMediaPlayer()
            playerA = pA
            playerB = pB
            activePlayer = pA
            standbyPlayer = pB

            val eqA = f.equalizer().newEqualizer()
            val eqB = f.equalizer().newEqualizer()
            equalizerA = eqA
            equalizerB = eqB
            pA.audio().setEqualizer(eqA)
            pB.audio().setEqualizer(eqB)

            pA.events().addMediaPlayerEventListener(createEventListener())
            pB.events().addMediaPlayerEventListener(createEventListener())
            isInitialized = true
            applyEqualizerToPlayers()
        } catch (e: Throwable) {
            isInitialized = false
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

    private fun getEffectiveVolume(multiplier: Float = currentReplayGainMultiplier): Int {
        if (_isMuted.value) return 0
        val rawVol = _volume.value * multiplier
        return (rawVol * 100).toInt().coerceIn(0, 100)
    }

    private fun applyEffectiveVolume() {
        if (!_isCrossfading.value && playPauseFadeJob?.isActive != true) {
            val player = activePlayer
            if (player != null && isInitialized) {
                try {
                    player.audio().setVolume(getEffectiveVolume())
                } catch (_: Exception) {}
            }
        }
    }

    override fun play(mediaPathOrUri: String) {
        playPauseFadeJob?.cancel()
        crossfadeJob?.cancel()
        _isCrossfading.value = false
        try {
            standbyPlayer?.controls()?.pause()
            standbyPlayer?.controls()?.stop()
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

        val player = activePlayer
        if (player == null || !isInitialized) {
            _playbackState.value = PlaybackState.PLAYING
            return
        }

        try {
            _playbackState.value = PlaybackState.BUFFERING
            val targetVol = getEffectiveVolume()
            player.audio().setVolume(targetVol)
            player.media().play(validPath)
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
            _errorEvents.tryEmit(e.message ?: "Failed to play media")
        }
    }

    override fun crossfadeTo(mediaPathOrUri: String, durationMs: Long) {
        val outgoingPlayer = activePlayer
        val incomingPlayer = standbyPlayer

        if (!isInitialized || outgoingPlayer == null || incomingPlayer == null || _playbackState.value != PlaybackState.PLAYING) {
            play(mediaPathOrUri)
            return
        }

        if (_isCrossfading.value) {
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

        try {
            _isCrossfading.value = true
            _currentPositionMs.value = 0L
            _durationMs.value = 0L

            activePlayer = incomingPlayer
            standbyPlayer = outgoingPlayer

            incomingPlayer.audio().setVolume(0)
            incomingPlayer.media().play(validPath)

            val clampedDuration = durationMs.coerceIn(500L, 12000L)
            val stepMs = 50L

            crossfadeJob = coroutineScope.launch {
                var elapsedMs = 0L
                try {
                    while (isActive) {
                        if (_playbackState.value == PlaybackState.PLAYING) {
                            val progress = (elapsedMs.toFloat() / clampedDuration).coerceIn(0f, 1f)
                            val baseVol = _volume.value
                            val isMuted = _isMuted.value

                            if (!isMuted) {
                                val volOut = (cos(progress * PI / 2.0).toFloat() * baseVol * outgoingMultiplier * 100).toInt().coerceIn(0, 100)
                                val volIn = (sin(progress * PI / 2.0).toFloat() * baseVol * incomingMultiplier * 100).toInt().coerceIn(0, 100)
                                try {
                                    outgoingPlayer.audio().setVolume(volOut)
                                    incomingPlayer.audio().setVolume(volIn)
                                } catch (_: Exception) {}
                            }

                            if (progress >= 1f || elapsedMs >= clampedDuration) break
                            elapsedMs += stepMs
                        }
                        delay(stepMs)
                    }
                } finally {
                    currentReplayGainMultiplier = incomingMultiplier
                    try {
                        outgoingPlayer.audio().setVolume(0)
                        outgoingPlayer.controls().pause()
                        outgoingPlayer.controls().stop()
                        val normalVol = getEffectiveVolume(incomingMultiplier)
                        outgoingPlayer.audio().setVolume(normalVol)
                        incomingPlayer.audio().setVolume(normalVol)
                    } catch (_: Exception) {}
                    _isCrossfading.value = false
                }
            }
        } catch (e: Exception) {
            _isCrossfading.value = false
            play(mediaPathOrUri)
        }
    }

    override fun setReplayGain(enabled: Boolean, mode: String) {
        replayGainEnabled = enabled
        replayGainMode = mode
        val path = currentTrackPath
        if (path != null) {
            updateReplayGainMultiplier(path)
            applyEffectiveVolume()
        }
    }

    override fun setEqualizer(enabled: Boolean, bands: List<Float>, preamp: Float) {
        equalizerEnabled = enabled
        equalizerBands = bands
        equalizerPreamp = preamp
        applyEqualizerToPlayers()
    }

    private fun applyEqualizerToPlayers() {
        if (!isInitialized) return
        val eqA = equalizerA ?: return
        val eqB = equalizerB ?: return
        try {
            if (playerA?.audio()?.equalizer() == null) {
                playerA?.audio()?.setEqualizer(eqA)
            }
            if (playerB?.audio()?.equalizer() == null) {
                playerB?.audio()?.setEqualizer(eqB)
            }

            val targetPreamp: Float
            val targetBands: FloatArray
            if (equalizerEnabled) {
                targetPreamp = (equalizerPreamp + VLC_EQUALIZER_PREAMP_COMPENSATION_DB).coerceIn(-20f, 20f)
                val bandCount = eqA.bandCount()
                targetBands = FloatArray(bandCount) { i ->
                    if (i < equalizerBands.size) equalizerBands[i].coerceIn(-20f, 20f) else 0f
                }
            } else {
                targetPreamp = VLC_EQUALIZER_PREAMP_COMPENSATION_DB
                targetBands = FloatArray(eqA.bandCount()) { 0f }
            }

            eqA.setAmps(targetBands)
            eqA.setPreamp(targetPreamp)
            eqB.setAmps(targetBands)
            eqB.setPreamp(targetPreamp)
        } catch (_: Exception) {}
    }

    override fun play() {
        val player = activePlayer
        if (player == null || !isInitialized) {
            _playbackState.value = PlaybackState.PLAYING
            return
        }
        playPauseFadeJob?.cancel()
        try {
            _playbackState.value = PlaybackState.PLAYING
            startPositionTicker()

            if (_isCrossfading.value) {
                player.controls().play()
                standbyPlayer?.controls()?.play()
                return
            }

            val targetVol = getEffectiveVolume()
            if (targetVol <= 0) {
                player.controls().play()
                return
            }

            val wasPlaying = try { player.status().isPlaying } catch (_: Exception) { false }
            val currentVol = try { player.audio().volume() } catch (_: Exception) { 0 }
            val fromVol = if (wasPlaying) currentVol.coerceIn(0, targetVol) else 0

            if (!wasPlaying) {
                try { player.audio().setVolume(0) } catch (_: Exception) {}
            }
            player.controls().play()

            playPauseFadeJob = coroutineScope.launch {
                val durationMs = 300L
                val steps = 15
                val stepDelay = durationMs / steps
                for (i in 1..steps) {
                    if (!isActive) break
                    val progress = i.toFloat() / steps
                    val currentStepVol = (fromVol + progress * (targetVol - fromVol)).toInt().coerceIn(0, 100)
                    try {
                        player.audio().setVolume(currentStepVol)
                    } catch (_: Exception) {}
                    delay(stepDelay)
                }
                try {
                    player.audio().setVolume(targetVol)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            _errorEvents.tryEmit(e.message ?: "Failed to resume playback")
        }
    }

    override fun pause() {
        val player = activePlayer
        if (player == null || !isInitialized) {
            _playbackState.value = PlaybackState.PAUSED
            return
        }
        playPauseFadeJob?.cancel()
        try {
            _playbackState.value = PlaybackState.PAUSED

            if (_isCrossfading.value) {
                player.controls().pause()
                standbyPlayer?.controls()?.pause()
                return
            }

            val targetVol = getEffectiveVolume()
            val currentVol = try { player.audio().volume() } catch (_: Exception) { targetVol }
            val startVol = currentVol.coerceIn(0, 100)

            if (startVol <= 0) {
                player.controls().pause()
                return
            }

            playPauseFadeJob = coroutineScope.launch {
                val durationMs = 300L
                val steps = 15
                val stepDelay = durationMs / steps
                for (i in 1..steps) {
                    if (!isActive) break
                    val currentStepVol = (((steps - i).toFloat() / steps) * startVol).toInt().coerceIn(0, 100)
                    try {
                        player.audio().setVolume(currentStepVol)
                    } catch (_: Exception) {}
                    delay(stepDelay)
                }
                try {
                    player.audio().setVolume(0)
                    player.controls().pause()
                    player.audio().setVolume(targetVol)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            _errorEvents.tryEmit(e.message ?: "Failed to pause playback")
        }
    }

    override fun stop() {
        playPauseFadeJob?.cancel()
        crossfadeJob?.cancel()
        _isCrossfading.value = false
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
        val player = activePlayer
        if (player == null || !isInitialized) {
            _playbackState.value = PlaybackState.IDLE
            return
        }
        try {
            player.controls().stop()
            standbyPlayer?.controls()?.stop()
        } catch (e: Exception) {
            _errorEvents.tryEmit(e.message ?: "Failed to stop playback")
        }
    }

    override fun seekTo(positionMs: Long) {
        playPauseFadeJob?.cancel()
        if (_isCrossfading.value) {
            crossfadeJob?.cancel()
            _isCrossfading.value = false
            try {
                standbyPlayer?.controls()?.stop()
                val normalVol = getEffectiveVolume()
                activePlayer?.audio()?.setVolume(normalVol)
            } catch (_: Exception) {}
        }
        val player = activePlayer
        if (player == null || !isInitialized) {
            _currentPositionMs.value = positionMs
            return
        }
        try {
            player.controls().setTime(positionMs)
            _currentPositionMs.value = positionMs
        } catch (e: Exception) {
            _errorEvents.tryEmit(e.message ?: "Failed to seek")
        }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        _volume.value = clamped
        playPauseFadeJob?.cancel()
        applyEffectiveVolume()
    }

    override fun setMute(muted: Boolean) {
        _isMuted.value = muted
        playPauseFadeJob?.cancel()
        applyEffectiveVolume()
    }

    override fun release() {
        stopPositionTicker()
        playPauseFadeJob?.cancel()
        crossfadeJob?.cancel()
        try {
            equalizerA = null
            equalizerB = null
            playerA?.release()
            playerB?.release()
            factory?.release()
        } catch (_: Exception) {}
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = coroutineScope.launch {
            while (isActive && _playbackState.value == PlaybackState.PLAYING) {
                val player = activePlayer
                if (player != null && isInitialized) {
                    val time = player.status().time()
                    if (time >= 0) {
                        _currentPositionMs.value = time
                    }
                    val length = player.status().length()
                    if (length > 0 && _durationMs.value != length) {
                        _durationMs.value = length
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }
}
