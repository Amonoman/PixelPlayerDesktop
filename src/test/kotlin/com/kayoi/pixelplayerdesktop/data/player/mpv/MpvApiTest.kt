package com.kayoi.pixelplayerdesktop.data.player.mpv

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MpvApiTest {

    @Test
    fun testMpvNativeApiLoadsAndInitializes() {
        val api = runCatching { MpvApi.INSTANCE }.getOrNull()
        if (api == null) {
            println("libmpv not found on test environment, skipping test.")
            return
        }

        val version = api.mpv_client_api_version()
        assertTrue(version > 0L, "mpv client api version should be > 0")

        val handle = api.mpv_create()
        assertNotNull(handle, "mpv_create() returned null handle")

        val initStatus = api.mpv_initialize(handle)
        assertTrue(initStatus >= 0, "mpv_initialize() failed with error: ${api.mpv_error_string(initStatus)}")

        api.mpv_terminate_destroy(handle)
    }
}
