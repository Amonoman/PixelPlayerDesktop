package com.kayoi.pixelplayerdesktop.platform.mediasession.linux

import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaMetadata
import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaPlaybackStatus
import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaSessionCommands
import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaSessionController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.exceptions.DBusExecutionException
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class LinuxMprisMediaSessionController(
    private val commands: MediaSessionCommands,
    private val coroutineScope: CoroutineScope,
    preferredBusName: String = DEFAULT_BUS_NAME
) : MediaSessionController {

    companion object {
        const val DEFAULT_BUS_NAME = "org.mpris.MediaPlayer2.pixelplayer"
        const val OBJECT_PATH = "/org/mpris/MediaPlayer2"
        const val INTERFACE_ROOT = "org.mpris.MediaPlayer2"
        const val INTERFACE_PLAYER = "org.mpris.MediaPlayer2.Player"
    }

    private val connection: DBusConnection
    private val claimedBusName: String
    private val isReleased = AtomicBoolean(false)
    private val stateLock = Any()

    private var currentMetadata = MediaMetadata()
    private var currentPlaybackStatus = MediaPlaybackStatus.STOPPED
    private var currentPositionMs = 0L
    private var canGoNext = false
    private var canGoPrevious = false
    private var canPlay = false
    private var canPause = false
    private var canSeek = false
    private var loopStatus = "None"
    private var isShuffle = false
    private var currentVolume = 1.0

    private val mprisAdapter = MprisAdapter()

    init {
        connection = DBusConnectionBuilder.forSessionBus().build()
        claimedBusName = try {
            connection.requestBusName(preferredBusName)
            preferredBusName
        } catch (_: Throwable) {
            val fallback = "$preferredBusName.instance${ProcessHandle.current().pid()}"
            connection.requestBusName(fallback)
            fallback
        }

        connection.exportObject(OBJECT_PATH, mprisAdapter)
    }

    override fun updateMetadata(metadata: MediaMetadata) {
        if (isReleased.get()) return
        val changed = synchronized(stateLock) {
            currentMetadata = metadata
            mapOf("Metadata" to Variant(buildMetadataMap(metadata), "a{sv}"))
        }
        emitPropertiesChanged(INTERFACE_PLAYER, changed)
    }

    override fun updatePlaybackStatus(status: MediaPlaybackStatus) {
        if (isReleased.get()) return
        val changed = synchronized(stateLock) {
            currentPlaybackStatus = status
            mapOf("PlaybackStatus" to Variant(toMprisPlaybackStatus(status)))
        }
        emitPropertiesChanged(INTERFACE_PLAYER, changed)
    }

    override fun updatePosition(positionMs: Long) {
        if (isReleased.get()) return
        synchronized(stateLock) {
            currentPositionMs = positionMs
        }
    }

    override fun updateCapabilities(
        canGoNext: Boolean,
        canGoPrevious: Boolean,
        canPlay: Boolean,
        canPause: Boolean,
        canSeek: Boolean
    ) {
        if (isReleased.get()) return
        val changed = synchronized(stateLock) {
            this.canGoNext = canGoNext
            this.canGoPrevious = canGoPrevious
            this.canPlay = canPlay
            this.canPause = canPause
            this.canSeek = canSeek
            mapOf(
                "CanGoNext" to Variant(canGoNext),
                "CanGoPrevious" to Variant(canGoPrevious),
                "CanPlay" to Variant(canPlay),
                "CanPause" to Variant(canPause),
                "CanSeek" to Variant(canSeek)
            )
        }
        emitPropertiesChanged(INTERFACE_PLAYER, changed)
    }

    override fun updateLoopAndShuffle(loopStatus: String, shuffle: Boolean) {
        if (isReleased.get()) return
        val changed = synchronized(stateLock) {
            this.loopStatus = loopStatus
            this.isShuffle = shuffle
            mapOf(
                "LoopStatus" to Variant(loopStatus),
                "Shuffle" to Variant(shuffle)
            )
        }
        emitPropertiesChanged(INTERFACE_PLAYER, changed)
    }

    override fun updateVolume(volume: Double) {
        if (isReleased.get()) return
        val changed = synchronized(stateLock) {
            currentVolume = volume.coerceIn(0.0, 1.0)
            mapOf("Volume" to Variant(currentVolume))
        }
        emitPropertiesChanged(INTERFACE_PLAYER, changed)
    }

    override fun release() {
        if (!isReleased.compareAndSet(false, true)) return
        runCatching { connection.unExportObject(OBJECT_PATH) }
        runCatching { connection.releaseBusName(claimedBusName) }
        runCatching { connection.close() }
    }

    private fun emitPropertiesChanged(interfaceName: String, changedProps: Map<String, Variant<*>>) {
        if (isReleased.get() || changedProps.isEmpty()) return
        runCatching {
            val signal = Properties.PropertiesChanged(
                OBJECT_PATH,
                interfaceName,
                changedProps,
                emptyList()
            )
            connection.sendMessage(signal)
        }
    }

    private fun toMprisPlaybackStatus(status: MediaPlaybackStatus): String {
        return when (status) {
            MediaPlaybackStatus.PLAYING -> "Playing"
            MediaPlaybackStatus.PAUSED -> "Paused"
            MediaPlaybackStatus.STOPPED -> "Stopped"
        }
    }

    private fun buildMetadataMap(metadata: MediaMetadata): Map<String, Variant<*>> {
        val map = mutableMapOf<String, Variant<*>>()
        val sanitizedId = metadata.trackId
            .replace(Regex("[^a-zA-Z0-9_]"), "_")
            .ifBlank { "NoTrack" }
        map["mpris:trackid"] = Variant(DBusPath("/org/mpris/MediaPlayer2/track/$sanitizedId"))
        map["mpris:length"] = Variant(metadata.durationMs * 1000L)

        val formattedArt = formatArtUrl(metadata.artUri)
        if (formattedArt != null) {
            map["mpris:artUrl"] = Variant(formattedArt)
        }

        if (metadata.title.isNotBlank()) {
            map["xesam:title"] = Variant(metadata.title)
        }
        if (metadata.album.isNotBlank()) {
            map["xesam:album"] = Variant(metadata.album)
        }
        if (metadata.artist.isNotBlank()) {
            map["xesam:artist"] = Variant(arrayOf(metadata.artist), "as")
        }
        if (metadata.albumArtist.isNotBlank()) {
            map["xesam:albumArtist"] = Variant(arrayOf(metadata.albumArtist), "as")
        }
        return map
    }

    private fun formatArtUrl(artUri: String?): String? {
        if (artUri.isNullOrBlank()) return null
        return when {
            artUri.startsWith("http://", ignoreCase = true) ||
            artUri.startsWith("https://", ignoreCase = true) ||
            artUri.startsWith("file://", ignoreCase = true) -> artUri
            artUri.startsWith("/") -> {
                val f = File(artUri)
                if (f.exists()) f.toURI().toString() else "file://$artUri"
            }
            else -> {
                val f = File(artUri)
                if (f.exists()) f.toURI().toString() else null
            }
        }
    }

    private inner class MprisAdapter : MediaPlayer2Interface, MediaPlayer2PlayerInterface, Properties {

        override fun isRemote(): Boolean = false
        override fun getObjectPath(): String = OBJECT_PATH

        override fun Raise() {}

        override fun Quit() {
            coroutineScope.launch { commands.stop() }
        }

        override fun Next() {
            coroutineScope.launch { commands.next() }
        }

        override fun Previous() {
            coroutineScope.launch { commands.previous() }
        }

        override fun Pause() {
            coroutineScope.launch { commands.pause() }
        }

        override fun PlayPause() {
            coroutineScope.launch { commands.playPause() }
        }

        override fun Stop() {
            coroutineScope.launch { commands.stop() }
        }

        override fun Play() {
            coroutineScope.launch { commands.play() }
        }

        override fun Seek(offsetUs: Long) {
            val offsetMs = offsetUs / 1000L
            coroutineScope.launch {
                commands.seek(offsetMs)
            }
        }

        override fun SetPosition(trackId: DBusPath, positionUs: Long) {
            val positionMs = positionUs / 1000L
            coroutineScope.launch {
                commands.setPosition(positionMs)
            }
        }

        override fun OpenUri(uri: String) {}

        @Suppress("UNCHECKED_CAST")
        override fun <A : Any?> Get(interfaceName: String, propertyName: String): A {
            val props = GetAll(interfaceName)
            val variant = props[propertyName]
                ?: throw DBusExecutionException("Unknown property: $propertyName on $interfaceName")
            return variant.value as A
        }

        override fun <A : Any?> Set(interfaceName: String, propertyName: String, value: A) {
            if (interfaceName == INTERFACE_PLAYER) {
                when (propertyName) {
                    "Rate" -> {}
                    "LoopStatus" -> {
                        val newLoop = value as? String ?: return
                        synchronized(stateLock) { loopStatus = newLoop }
                    }
                    "Shuffle" -> {
                        val newShuffle = value as? Boolean ?: return
                        synchronized(stateLock) { isShuffle = newShuffle }
                    }
                    "Volume" -> {
                        val newVol = (value as? Number)?.toDouble() ?: return
                        synchronized(stateLock) { currentVolume = newVol }
                    }
                    else -> throw DBusExecutionException("Property $propertyName is read-only")
                }
            } else {
                throw DBusExecutionException("Interface $interfaceName has no writable properties")
            }
        }

        override fun GetAll(interfaceName: String): Map<String, Variant<*>> {
            return when (interfaceName) {
                INTERFACE_ROOT -> mapOf(
                    "CanQuit" to Variant(true),
                    "CanRaise" to Variant(true),
                    "HasTrackList" to Variant(false),
                    "Identity" to Variant("PixelPlayer"),
                    "SupportedUriSchemes" to Variant(arrayOf("file"), "as"),
                    "SupportedMimeTypes" to Variant(arrayOf("audio/mpeg", "audio/flac", "audio/ogg", "audio/wav", "audio/x-vorbis+ogg"), "as")
                )
                INTERFACE_PLAYER -> synchronized(stateLock) {
                    mapOf(
                        "PlaybackStatus" to Variant(toMprisPlaybackStatus(currentPlaybackStatus)),
                        "LoopStatus" to Variant(loopStatus),
                        "Rate" to Variant(1.0),
                        "Shuffle" to Variant(isShuffle),
                        "Metadata" to Variant(buildMetadataMap(currentMetadata), "a{sv}"),
                        "Volume" to Variant(currentVolume),
                        "Position" to Variant(currentPositionMs * 1000L),
                        "MinimumRate" to Variant(1.0),
                        "MaximumRate" to Variant(1.0),
                        "CanGoNext" to Variant(canGoNext),
                        "CanGoPrevious" to Variant(canGoPrevious),
                        "CanPlay" to Variant(canPlay),
                        "CanPause" to Variant(canPause),
                        "CanSeek" to Variant(canSeek),
                        "CanControl" to Variant(true)
                    )
                }
                else -> emptyMap()
            }
        }
    }
}
