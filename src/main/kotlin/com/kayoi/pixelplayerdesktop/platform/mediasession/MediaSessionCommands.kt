package com.kayoi.pixelplayerdesktop.platform.mediasession

interface MediaSessionCommands {
    fun play()
    fun pause()
    fun playPause()
    fun next()
    fun previous()
    fun stop()
    fun seek(offsetMs: Long)
    fun setPosition(positionMs: Long)
}
