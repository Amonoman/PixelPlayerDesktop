package com.kayoi.pixelplayerdesktop.platform.mediasession.windows

import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaMetadata
import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaPlaybackStatus
import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaSessionCommands
import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaSessionController
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class WindowsMediaKeySessionController(
    private val commands: MediaSessionCommands,
    private val coroutineScope: CoroutineScope
) : MediaSessionController {

    companion object {
        const val VK_MEDIA_NEXT_TRACK = 0xB0
        const val VK_MEDIA_PREV_TRACK = 0xB1
        const val VK_MEDIA_STOP = 0xB2
        const val VK_MEDIA_PLAY_PAUSE = 0xB3
    }

    private val isReleased = AtomicBoolean(false)
    private var hookThread: Thread? = null
    private var hookThreadId: Int = 0
    private var hook: WinUser.HHOOK? = null
    private var keyboardProc: WinUser.LowLevelKeyboardProc? = null

    init {
        val startupLatch = CountDownLatch(1)
        hookThread = thread(name = "PixelPlayer-WindowsMediaKeys", isDaemon = true) {
            hookThreadId = Kernel32.INSTANCE.GetCurrentThreadId()
            val hMod = Kernel32.INSTANCE.GetModuleHandle(null)

            val proc = WinUser.LowLevelKeyboardProc { nCode, wParam, info ->
                if (nCode >= 0) {
                    val msg = wParam.toInt()
                    if (msg == WinUser.WM_KEYDOWN || msg == WinUser.WM_SYSKEYDOWN) {
                        when (info.vkCode) {
                            VK_MEDIA_PLAY_PAUSE -> coroutineScope.launch { commands.playPause() }
                            VK_MEDIA_NEXT_TRACK -> coroutineScope.launch { commands.next() }
                            VK_MEDIA_PREV_TRACK -> coroutineScope.launch { commands.previous() }
                            VK_MEDIA_STOP -> coroutineScope.launch { commands.stop() }
                        }
                    }
                }
                val lParam = WinDef.LPARAM(com.sun.jna.Pointer.nativeValue(info.pointer))
                User32.INSTANCE.CallNextHookEx(hook, nCode, wParam, lParam)
            }
            keyboardProc = proc

            hook = User32.INSTANCE.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, proc, hMod, 0)
            startupLatch.countDown()

            val msg = WinUser.MSG()
            while (!isReleased.get()) {
                val result = User32.INSTANCE.GetMessage(msg, null, 0, 0)
                if (result == 0 || result == -1) break
                User32.INSTANCE.TranslateMessage(msg)
                User32.INSTANCE.DispatchMessage(msg)
            }

            hook?.let {
                User32.INSTANCE.UnhookWindowsHookEx(it)
                hook = null
            }
        }

        startupLatch.await(1500, TimeUnit.MILLISECONDS)
    }

    override fun updateMetadata(metadata: MediaMetadata) {}
    override fun updatePlaybackStatus(status: MediaPlaybackStatus) {}
    override fun updatePosition(positionMs: Long) {}
    override fun updateCapabilities(
        canGoNext: Boolean,
        canGoPrevious: Boolean,
        canPlay: Boolean,
        canPause: Boolean,
        canSeek: Boolean
    ) {}
    override fun updateLoopAndShuffle(loopStatus: String, shuffle: Boolean) {}
    override fun updateVolume(volume: Double) {}

    override fun release() {
        if (!isReleased.compareAndSet(false, true)) return
        val targetThreadId = hookThreadId
        if (targetThreadId != 0) {
            User32.INSTANCE.PostThreadMessage(
                targetThreadId,
                WinUser.WM_QUIT,
                WinDef.WPARAM(0),
                WinDef.LPARAM(0)
            )
        }
        hook?.let {
            User32.INSTANCE.UnhookWindowsHookEx(it)
            hook = null
        }
        hookThread?.interrupt()
        hookThread = null
        keyboardProc = null
    }
}
