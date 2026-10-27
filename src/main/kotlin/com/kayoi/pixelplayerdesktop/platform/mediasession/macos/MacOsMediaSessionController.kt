package com.kayoi.pixelplayerdesktop.platform.mediasession.macos

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

import com.sun.jna.Callback
import com.sun.jna.Function
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaMetadata
import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaPlaybackStatus
import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaSessionCommands
import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaSessionController

class MacOsMediaSessionController(
    private val commands: MediaSessionCommands,
    private val coroutineScope: CoroutineScope
) : MediaSessionController {

    private interface LibObjC : Library {
        fun objc_getClass(name: String): Pointer?
        fun sel_registerName(name: String): Pointer?
        fun objc_allocateClassPair(superClass: Pointer?, name: String, extraBytes: Int): Pointer?
        fun class_addMethod(cls: Pointer?, sel: Pointer?, imp: Callback, types: String): Boolean
        fun objc_registerClassPair(cls: Pointer?)
        fun objc_autoreleasePoolPush(): Pointer?
        fun objc_autoreleasePoolPop(pool: Pointer?)
    }

    private interface CommandCallback : Callback {
        fun invoke(self: Pointer, cmd: Pointer, event: Pointer): Long
    }

    private val isReleased = AtomicBoolean(false)
    private var isInitialized = false

    private var libObjC: LibObjC? = null
    private var objcMsgSend: Function? = null

    private var targetObject: Pointer? = null
    private var sharedCommandCenter: Pointer? = null
    private var defaultInfoCenter: Pointer? = null

    private var playCommand: Pointer? = null
    private var pauseCommand: Pointer? = null
    private var togglePlayPauseCommand: Pointer? = null
    private var nextTrackCommand: Pointer? = null
    private var previousTrackCommand: Pointer? = null
    private var changePlaybackPositionCommand: Pointer? = null

    private var selAddTargetAction: Pointer? = null
    private var selRemoveTarget: Pointer? = null
    private var selSetEnabled: Pointer? = null
    private var selPositionTime: Pointer? = null
    private var selSetNowPlayingInfo: Pointer? = null
    private var selSetPlaybackState: Pointer? = null

    private var keyTitle: Pointer? = null
    private var keyArtist: Pointer? = null
    private var keyAlbum: Pointer? = null
    private var keyDuration: Pointer? = null
    private var keyElapsedPlaybackTime: Pointer? = null
    private var keyPlaybackRate: Pointer? = null

    private var lastMetadata = MediaMetadata()
    private var lastStatus = MediaPlaybackStatus.STOPPED
    private var lastPositionMs: Long = 0L

    private val retainedCallbacks = mutableListOf<CommandCallback>()

    init {
        runCatching {
            initNativeBridges()
        }
    }

    private fun initNativeBridges() {
        NativeLibrary.getInstance("Foundation")
        NativeLibrary.getInstance("/System/Library/Frameworks/MediaPlayer.framework/MediaPlayer")

        val objcLib = Native.load("objc", LibObjC::class.java, mapOf(Library.OPTION_STRING_ENCODING to StandardCharsets.UTF_8.name()))
        libObjC = objcLib
        objcMsgSend = NativeLibrary.getInstance("objc").getFunction("objc_msgSend")

        val selAlloc = objcLib.sel_registerName("alloc") ?: return
        val selInit = objcLib.sel_registerName("init") ?: return
        val selRetain = objcLib.sel_registerName("retain")
        selAddTargetAction = objcLib.sel_registerName("addTarget:action:")
        selRemoveTarget = objcLib.sel_registerName("removeTarget:")
        selSetEnabled = objcLib.sel_registerName("setEnabled:")
        selPositionTime = objcLib.sel_registerName("positionTime")
        selSetNowPlayingInfo = objcLib.sel_registerName("setNowPlayingInfo:")
        selSetPlaybackState = objcLib.sel_registerName("setPlaybackState:")

        val mediaPlayerLib = runCatching {
            NativeLibrary.getInstance("/System/Library/Frameworks/MediaPlayer.framework/MediaPlayer")
        }.getOrNull()

        fun resolveKey(symbolName: String): Pointer? {
            val constantPtr = if (mediaPlayerLib != null) {
                runCatching {
                    val addr = mediaPlayerLib.getGlobalVariableAddress(symbolName)
                    addr.getPointer(0)
                }.getOrNull()
            } else {
                null
            }
            val ptr = constantPtr ?: createNSString(symbolName)
            if (ptr != null && selRetain != null) {
                msgSendPointer(ptr, selRetain)
            }
            return ptr
        }

        keyTitle = resolveKey("MPMediaItemPropertyTitle")
        keyArtist = resolveKey("MPMediaItemPropertyArtist")
        keyAlbum = resolveKey("MPMediaItemPropertyAlbumTitle")
        keyDuration = resolveKey("MPMediaItemPropertyPlaybackDuration")
        keyElapsedPlaybackTime = resolveKey("MPNowPlayingInfoPropertyElapsedPlaybackTime")
        keyPlaybackRate = resolveKey("MPNowPlayingInfoPropertyPlaybackRate")

        val className = "PixelPlayerMediaRemoteTarget"
        val nsObjectClass = objcLib.objc_getClass("NSObject") ?: return
        var targetClass = objcLib.objc_getClass(className)

        val selPlay = objcLib.sel_registerName("handlePlay:") ?: return
        val selPause = objcLib.sel_registerName("handlePause:") ?: return
        val selToggle = objcLib.sel_registerName("handleTogglePlayPause:") ?: return
        val selNext = objcLib.sel_registerName("handleNext:") ?: return
        val selPrev = objcLib.sel_registerName("handlePrevious:") ?: return
        val selChangePos = objcLib.sel_registerName("handleChangePosition:") ?: return

        val cbPlay = object : CommandCallback {
            override fun invoke(self: Pointer, cmd: Pointer, event: Pointer): Long {
                coroutineScope.launch { commands.play() }
                return 0L
            }
        }
        val cbPause = object : CommandCallback {
            override fun invoke(self: Pointer, cmd: Pointer, event: Pointer): Long {
                coroutineScope.launch { commands.pause() }
                return 0L
            }
        }
        val cbToggle = object : CommandCallback {
            override fun invoke(self: Pointer, cmd: Pointer, event: Pointer): Long {
                coroutineScope.launch { commands.playPause() }
                return 0L
            }
        }
        val cbNext = object : CommandCallback {
            override fun invoke(self: Pointer, cmd: Pointer, event: Pointer): Long {
                coroutineScope.launch { commands.next() }
                return 0L
            }
        }
        val cbPrev = object : CommandCallback {
            override fun invoke(self: Pointer, cmd: Pointer, event: Pointer): Long {
                coroutineScope.launch { commands.previous() }
                return 0L
            }
        }
        val cbChangePos = object : CommandCallback {
            override fun invoke(self: Pointer, cmd: Pointer, event: Pointer): Long {
                val posSec = msgSendDouble(event, selPositionTime)
                val posMs = (posSec * 1000.0).toLong()
                lastPositionMs = posMs
                syncNowPlayingInfo()
                coroutineScope.launch { commands.setPosition(posMs) }
                return 0L
            }
        }

        retainedCallbacks.addAll(listOf(cbPlay, cbPause, cbToggle, cbNext, cbPrev, cbChangePos))

        if (targetClass == null) {
            val allocated = objcLib.objc_allocateClassPair(nsObjectClass, className, 0) ?: return
            val types = "q@:@"
            objcLib.class_addMethod(allocated, selPlay, cbPlay, types)
            objcLib.class_addMethod(allocated, selPause, cbPause, types)
            objcLib.class_addMethod(allocated, selToggle, cbToggle, types)
            objcLib.class_addMethod(allocated, selNext, cbNext, types)
            objcLib.class_addMethod(allocated, selPrev, cbPrev, types)
            objcLib.class_addMethod(allocated, selChangePos, cbChangePos, types)
            objcLib.objc_registerClassPair(allocated)
            targetClass = allocated
        }

        val rawAlloc = msgSendPointer(targetClass, selAlloc) ?: return
        targetObject = msgSendPointer(rawAlloc, selInit) ?: return

        val commandCenterClass = objcLib.objc_getClass("MPRemoteCommandCenter") ?: return
        val selSharedCenter = objcLib.sel_registerName("sharedCommandCenter") ?: return
        val center = msgSendPointer(commandCenterClass, selSharedCenter) ?: return
        sharedCommandCenter = center

        val infoCenterClass = objcLib.objc_getClass("MPNowPlayingInfoCenter") ?: return
        val selDefaultCenter = objcLib.sel_registerName("defaultCenter") ?: return
        defaultInfoCenter = msgSendPointer(infoCenterClass, selDefaultCenter)

        playCommand = msgSendPointer(center, objcLib.sel_registerName("playCommand"))
        pauseCommand = msgSendPointer(center, objcLib.sel_registerName("pauseCommand"))
        togglePlayPauseCommand = msgSendPointer(center, objcLib.sel_registerName("togglePlayPauseCommand"))
        nextTrackCommand = msgSendPointer(center, objcLib.sel_registerName("nextTrackCommand"))
        previousTrackCommand = msgSendPointer(center, objcLib.sel_registerName("previousTrackCommand"))
        changePlaybackPositionCommand = msgSendPointer(center, objcLib.sel_registerName("changePlaybackPositionCommand"))

        attachCommand(playCommand, selPlay)
        attachCommand(pauseCommand, selPause)
        attachCommand(togglePlayPauseCommand, selToggle)
        attachCommand(nextTrackCommand, selNext)
        attachCommand(previousTrackCommand, selPrev)
        attachCommand(changePlaybackPositionCommand, selChangePos)

        isInitialized = true
    }

    private fun attachCommand(command: Pointer?, actionSel: Pointer?) {
        if (command == null || actionSel == null || targetObject == null) return
        msgSendVoid(command, selAddTargetAction, targetObject, actionSel)
        msgSendVoid(command, selSetEnabled, true)
    }

    private fun detachCommand(command: Pointer?) {
        if (command == null || targetObject == null) return
        msgSendVoid(command, selRemoveTarget, targetObject)
        msgSendVoid(command, selSetEnabled, false)
    }

    override fun updateMetadata(metadata: MediaMetadata) {
        if (lastMetadata.trackId != metadata.trackId) {
            lastPositionMs = 0L
        }
        lastMetadata = metadata
        syncNowPlayingInfo()
    }

    override fun updatePlaybackStatus(status: MediaPlaybackStatus) {
        lastStatus = status
        val infoCenter = defaultInfoCenter ?: return
        val stateValue = when (status) {
            MediaPlaybackStatus.PLAYING -> 1L
            MediaPlaybackStatus.PAUSED -> 2L
            MediaPlaybackStatus.STOPPED -> 3L
        }
        msgSendVoid(infoCenter, selSetPlaybackState, stateValue)
        syncNowPlayingInfo()
    }

    override fun updatePosition(positionMs: Long) {
        val wasJump = abs(positionMs - lastPositionMs) > 2000L
        val wasPausedSeek = lastStatus != MediaPlaybackStatus.PLAYING && abs(positionMs - lastPositionMs) > 500L
        lastPositionMs = positionMs
        if (wasJump || wasPausedSeek) {
            syncNowPlayingInfo()
        }
    }

    override fun updateCapabilities(
        canGoNext: Boolean,
        canGoPrevious: Boolean,
        canPlay: Boolean,
        canPause: Boolean,
        canSeek: Boolean
    ) {
        if (!isInitialized) return
        msgSendVoid(playCommand, selSetEnabled, canPlay)
        msgSendVoid(pauseCommand, selSetEnabled, canPause)
        msgSendVoid(togglePlayPauseCommand, selSetEnabled, canPlay || canPause)
        msgSendVoid(nextTrackCommand, selSetEnabled, canGoNext)
        msgSendVoid(previousTrackCommand, selSetEnabled, canGoPrevious)
        msgSendVoid(changePlaybackPositionCommand, selSetEnabled, canSeek)
    }

    override fun updateLoopAndShuffle(loopStatus: String, shuffle: Boolean) {}
    override fun updateVolume(volume: Double) {}

    private fun syncNowPlayingInfo() {
        val infoCenter = defaultInfoCenter ?: return
        val objcLib = libObjC ?: return

        withAutoreleasePool {
            val dictClass = objcLib.objc_getClass("NSMutableDictionary") ?: return@withAutoreleasePool
            val selDictionary = objcLib.sel_registerName("dictionary") ?: return@withAutoreleasePool
            val dict = msgSendPointer(dictClass, selDictionary) ?: return@withAutoreleasePool
            val selSetObject = objcLib.sel_registerName("setObject:forKey:") ?: return@withAutoreleasePool

            fun putEntry(keyObj: Pointer?, valueObj: Pointer?) {
                if (keyObj == null || valueObj == null) return
                msgSendVoid(dict, selSetObject, valueObj, keyObj)
            }

            if (lastMetadata.title.isNotBlank()) {
                putEntry(keyTitle, createNSString(lastMetadata.title))
            }
            if (lastMetadata.artist.isNotBlank()) {
                putEntry(keyArtist, createNSString(lastMetadata.artist))
            }
            if (lastMetadata.album.isNotBlank()) {
                putEntry(keyAlbum, createNSString(lastMetadata.album))
            }

            val durationSec = (lastMetadata.durationMs.coerceAtLeast(0L) / 1000.0)
            putEntry(keyDuration, createNSNumber(durationSec))

            val elapsedSec = (lastPositionMs.coerceAtLeast(0L) / 1000.0)
            putEntry(keyElapsedPlaybackTime, createNSNumber(elapsedSec))

            val rate = if (lastStatus == MediaPlaybackStatus.PLAYING) 1.0 else 0.0
            putEntry(keyPlaybackRate, createNSNumber(rate))

            msgSendVoid(infoCenter, selSetNowPlayingInfo, dict)
        }
    }

    private inline fun <T> withAutoreleasePool(block: () -> T): T {
        val objcLib = libObjC
        val pool = runCatching { objcLib?.objc_autoreleasePoolPush() }.getOrNull()
        return try {
            block()
        } finally {
            if (pool != null) {
                runCatching { objcLib?.objc_autoreleasePoolPop(pool) }
            }
        }
    }

    private fun createNSString(str: String): Pointer? {
        val objcLib = libObjC ?: return null
        val nsStringClass = objcLib.objc_getClass("NSString") ?: return null
        val sel = objcLib.sel_registerName("stringWithUTF8String:") ?: return null
        return msgSendPointer(nsStringClass, sel, str)
    }

    private fun createNSNumber(value: Double): Pointer? {
        val objcLib = libObjC ?: return null
        val nsNumberClass = objcLib.objc_getClass("NSNumber") ?: return null
        val sel = objcLib.sel_registerName("numberWithDouble:") ?: return null
        return msgSendPointer(nsNumberClass, sel, value)
    }

    private fun msgSendPointer(receiver: Pointer?, selector: Pointer?, vararg args: Any?): Pointer? {
        if (receiver == null || selector == null) return null
        val fn = objcMsgSend ?: return null
        val fullArgs = arrayOfNulls<Any>(args.size + 2)
        fullArgs[0] = receiver
        fullArgs[1] = selector
        System.arraycopy(args, 0, fullArgs, 2, args.size)
        return fn.invokePointer(fullArgs)
    }

    private fun msgSendVoid(receiver: Pointer?, selector: Pointer?, vararg args: Any?) {
        if (receiver == null || selector == null) return
        val fn = objcMsgSend ?: return
        val fullArgs = arrayOfNulls<Any>(args.size + 2)
        fullArgs[0] = receiver
        fullArgs[1] = selector
        System.arraycopy(args, 0, fullArgs, 2, args.size)
        fn.invokeVoid(fullArgs)
    }

    private fun msgSendDouble(receiver: Pointer?, selector: Pointer?, vararg args: Any?): Double {
        if (receiver == null || selector == null) return 0.0
        val fn = objcMsgSend ?: return 0.0
        val fullArgs = arrayOfNulls<Any>(args.size + 2)
        fullArgs[0] = receiver
        fullArgs[1] = selector
        System.arraycopy(args, 0, fullArgs, 2, args.size)
        return fn.invokeDouble(fullArgs)
    }

    override fun release() {
        if (!isReleased.compareAndSet(false, true)) return
        runCatching {
            detachCommand(playCommand)
            detachCommand(pauseCommand)
            detachCommand(togglePlayPauseCommand)
            detachCommand(nextTrackCommand)
            detachCommand(previousTrackCommand)
            detachCommand(changePlaybackPositionCommand)

            val infoCenter = defaultInfoCenter
            if (infoCenter != null) {
                msgSendVoid(infoCenter, selSetNowPlayingInfo, null)
                msgSendVoid(infoCenter, selSetPlaybackState, 0L)
            }

            val selRelease = libObjC?.sel_registerName("release")
            fun releasePointer(ptr: Pointer?) {
                if (ptr != null && selRelease != null) {
                    msgSendVoid(ptr, selRelease)
                }
            }
            releasePointer(keyTitle)
            releasePointer(keyArtist)
            releasePointer(keyAlbum)
            releasePointer(keyDuration)
            releasePointer(keyElapsedPlaybackTime)
            releasePointer(keyPlaybackRate)
            keyTitle = null
            keyArtist = null
            keyAlbum = null
            keyDuration = null
            keyElapsedPlaybackTime = null
            keyPlaybackRate = null

            retainedCallbacks.clear()
            targetObject = null
        }
    }
}
