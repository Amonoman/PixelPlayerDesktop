package com.kayoi.pixelplayerdesktop.data.network.musicbrainz

import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MusicBrainzClientTest {
    private val client = MusicBrainzClient(ioDispatcher = Dispatchers.Unconfined)

    @Test
    fun parseYear_extractsCorrectYear() {
        assertEquals(2024, client.parseYear("2024-03-15"))
        assertEquals(1999, client.parseYear("1999"))
        assertEquals(2010, client.parseYear("Release in 2010"))
        assertNull(client.parseYear(null))
        assertNull(client.parseYear(""))
        assertNull(client.parseYear("Unknown"))
    }

    @Test
    fun sanitizeLuceneQuery_escapesSpecialCharacters() {
        val sanitized = client.sanitizeLuceneQuery("AC/DC: Back In Black (Live!)")
        assertEquals("AC\\/DC\\: Back In Black \\(Live\\!\\)", sanitized)
    }
}
