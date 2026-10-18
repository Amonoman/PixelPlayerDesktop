package com.kayoi.pixelplayerdesktop.platform.mediasession.linux

import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusProperties
import org.freedesktop.dbus.annotations.DBusProperty
import org.freedesktop.dbus.interfaces.DBusInterface

@DBusInterfaceName("org.mpris.MediaPlayer2")
@DBusProperties(
    DBusProperty(name = "CanQuit", type = Boolean::class, access = DBusProperty.Access.READ),
    DBusProperty(name = "CanRaise", type = Boolean::class, access = DBusProperty.Access.READ),
    DBusProperty(name = "HasTrackList", type = Boolean::class, access = DBusProperty.Access.READ),
    DBusProperty(name = "Identity", type = String::class, access = DBusProperty.Access.READ),
    DBusProperty(name = "SupportedUriSchemes", type = Array<String>::class, access = DBusProperty.Access.READ),
    DBusProperty(name = "SupportedMimeTypes", type = Array<String>::class, access = DBusProperty.Access.READ)
)
interface MediaPlayer2Interface : DBusInterface {
    fun Raise()
    fun Quit()
}
