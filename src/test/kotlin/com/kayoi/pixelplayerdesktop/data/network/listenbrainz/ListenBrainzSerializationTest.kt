package com.kayoi.pixelplayerdesktop.data.network.listenbrainz

import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzAdditionalInfo
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzListen
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzSubmission
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzTrackMetadata
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListenBrainzSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    @Test
    fun testPlayingNowPayloadDoesNotContainListenedAt() {
        val submission = ListenBrainzSubmission(
            listenType = ListenBrainzSubmission.TYPE_PLAYING_NOW,
            payload = listOf(
                ListenBrainzListen(
                    listenedAt = null,
                    trackMetadata = ListenBrainzTrackMetadata(
                        artistName = "Test Artist",
                        trackName = "Test Track",
                        releaseName = null,
                        additionalInfo = ListenBrainzAdditionalInfo(
                            durationMs = 120000L,
                            recordingMbid = null
                        )
                    )
                )
            )
        )

        val encoded = json.encodeToString(ListenBrainzSubmission.serializer(), submission)
        assertFalse(encoded.contains("listened_at"), "JSON must not contain listened_at: $encoded")
        assertFalse(encoded.contains("recording_mbid"), "JSON must not contain recording_mbid: $encoded")
        assertFalse(encoded.contains("release_name"), "JSON must not contain release_name: $encoded")
        assertTrue(encoded.contains("playing_now"), "JSON must contain playing_now: $encoded")
    }

    @Test
    fun testSingleListenPayloadContainsListenedAt() {
        val submission = ListenBrainzSubmission(
            listenType = ListenBrainzSubmission.TYPE_SINGLE,
            payload = listOf(
                ListenBrainzListen(
                    listenedAt = 1672531199L,
                    trackMetadata = ListenBrainzTrackMetadata(
                        artistName = "Test Artist",
                        trackName = "Test Track"
                    )
                )
            )
        )

        val encoded = json.encodeToString(ListenBrainzSubmission.serializer(), submission)
        assertTrue(encoded.contains("\"listened_at\":1672531199"), "JSON must contain listened_at: $encoded")
        assertTrue(encoded.contains("\"listen_type\":\"single\""), "JSON must contain single listen_type: $encoded")
    }
}
