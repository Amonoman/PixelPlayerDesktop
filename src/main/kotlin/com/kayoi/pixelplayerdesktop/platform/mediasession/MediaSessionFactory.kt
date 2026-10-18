package com.kayoi.pixelplayerdesktop.platform.mediasession

import com.kayoi.pixelplayerdesktop.platform.mediasession.linux.LinuxMprisMediaSessionController
import com.kayoi.pixelplayerdesktop.platform.mediasession.windows.WindowsMediaKeySessionController
import kotlinx.coroutines.CoroutineScope
import java.util.Locale

object MediaSessionFactory {
    fun create(
        commands: MediaSessionCommands,
        coroutineScope: CoroutineScope
    ): MediaSessionController {
        val os = System.getProperty("os.name", "").lowercase(Locale.ROOT)
        return when {
            os.contains("linux") || os.contains("nix") -> {
                AsyncMediaSessionController(coroutineScope) {
                    LinuxMprisMediaSessionController(commands, coroutineScope)
                }
            }
            os.contains("win") -> {
                AsyncMediaSessionController(coroutineScope) {
                    WindowsMediaKeySessionController(commands, coroutineScope)
                }
            }
            else -> NoOpMediaSessionController
        }
    }
}
