package com.kayoi.pixelplayerdesktop.platform.mediasession.linux

import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusProperties
import org.freedesktop.dbus.annotations.DBusProperty
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.messages.DBusSignal

@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
@DBusProperties(
    DBusProperty(name = "PlaybackStatus", type = String::class, access = DBusProperty.Access.READ),
    DBusProperty(name = "LoopStatus", type = String::class, access = DBusProperty.Access.READ_WRITE),
    DBusProperty(name = "Rate", type = Double::class, access = DBusProperty.Access.READ_WRITE),
    DBusProperty(name = "Shuffle", type = Boolean::class, access = DBusProperty.Access.READ_WRITE),
    DBusProperty(name = "Metadata", type = Map::class, access = DBusProperty.Access.READ),
    DBusProperty(name = "Volume", type = Double::class, access = DBusProperty.Access.READ_WRITE),
    DBusProperty(name = "Position", type = Long::class, access = DBusProperty.Access.READ),
    DBusProperty(name = "MinimumRate", type = Double::class, access = DBusProperty.Access.READ),
    DBusProperty(name = "MaximumRate", type = Double::class, access = DBusProperty.Access.READ),
    DBusProperty(name = "CanGoNext", type = Boolean::class, access = DBusProperty.Access.READ),
    DBusProperty(name = "CanGoPrevious", type = Boolean::class, access = DBusProperty.Access.READ),
    DBusProperty(name = "CanPlay", type = Boolean::class, access = DBusProperty.Access.READ),
    DBusProperty(name = "CanPause", type = Boolean::class, access = DBusProperty.Access.READ),
    DBusProperty(name = "CanSeek", type = Boolean::class, access = DBusProperty.Access.READ),
    DBusProperty(name = "CanControl", type = Boolean::class, access = DBusProperty.Access.READ)
)
interface MediaPlayer2PlayerInterface : DBusInterface {
    fun Next()
    fun Previous()
    fun Pause()
    fun PlayPause()
    fun Stop()
    fun Play()
    fun Seek(offsetUs: Long)
    fun SetPosition(trackId: DBusPath, positionUs: Long)
    fun OpenUri(uri: String)

    class Seeked(path: String, val positionUs: Long) : DBusSignal(path, positionUs)
}
