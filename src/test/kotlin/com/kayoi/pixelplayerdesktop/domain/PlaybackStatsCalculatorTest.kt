package com.kayoi.pixelplayerdesktop.domain

import com.kayoi.pixelplayerdesktop.data.model.PlaybackEvent
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.StatsTimeRange
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaybackStatsCalculatorTest {

    private val calculator = PlaybackStatsCalculator()
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun testDayTimelineGeneratesSixFourHourBuckets() {
        val nowMillis = Instant.parse("2026-10-16T12:00:00Z").toEpochMilli()
        val summary = calculator.calculateSummary(
            range = StatsTimeRange.DAY,
            events = emptyList(),
            songs = emptyList(),
            nowMillis = nowMillis,
            zoneId = zoneId
        )

        assertEquals(6, summary.timeline.size)
        assertEquals("12am", summary.timeline[0].label)
        assertEquals("4am", summary.timeline[1].label)
        assertEquals("8am", summary.timeline[2].label)
        assertEquals("12pm", summary.timeline[3].label)
        assertEquals("4pm", summary.timeline[4].label)
        assertEquals("8pm", summary.timeline[5].label)
    }

    @Test
    fun testWeekTimelineGeneratesSevenDailyBuckets() {
        val nowMillis = Instant.parse("2026-10-16T12:00:00Z").toEpochMilli()
        val summary = calculator.calculateSummary(
            range = StatsTimeRange.WEEK,
            events = emptyList(),
            songs = emptyList(),
            nowMillis = nowMillis,
            zoneId = zoneId
        )

        assertEquals(7, summary.timeline.size)
        assertEquals("Mon", summary.timeline[0].label)
        assertEquals("Sun", summary.timeline[6].label)
    }

    @Test
    fun testCalculateSummaryAggregatesCorrectly() {
        val now = Instant.parse("2026-10-16T15:00:00Z")
        val nowMillis = now.toEpochMilli()
        val sampleSongs = listOf(
            Song(
                id = "song_1",
                title = "Song One",
                artist = "Artist A",
                album = "Album Alpha",
                genre = "Rock",
                duration = 180000L
            ),
            Song(
                id = "song_2",
                title = "Song Two",
                artist = "Artist B",
                album = "Album Beta",
                genre = "Pop",
                duration = 240000L
            )
        )

        val events = listOf(
            PlaybackEvent(
                id = 1L,
                songId = "song_1",
                timestamp = now.minusSeconds(3600).toEpochMilli(),
                durationMs = 60000L
            ),
            PlaybackEvent(
                id = 2L,
                songId = "song_1",
                timestamp = now.minusSeconds(1800).toEpochMilli(),
                durationMs = 120000L
            ),
            PlaybackEvent(
                id = 3L,
                songId = "song_2",
                timestamp = now.minusSeconds(600).toEpochMilli(),
                durationMs = 90000L
            )
        )

        val summary = calculator.calculateSummary(
            range = StatsTimeRange.DAY,
            events = events,
            songs = sampleSongs,
            nowMillis = nowMillis,
            zoneId = zoneId
        )

        assertEquals(270000L, summary.totalDurationMs)
        assertEquals(3, summary.totalPlayCount)
        assertEquals(2, summary.uniqueSongs)
        assertTrue(summary.hasListeningActivity())

        assertEquals(2, summary.topArtists.size)
        assertEquals("Artist A", summary.topArtists[0].artist)
        assertEquals(180000L, summary.topArtists[0].totalDurationMs)
        assertEquals(2, summary.topArtists[0].playCount)

        assertEquals("song_1", summary.topSongs[0].songId)
        assertEquals(2, summary.topSongs[0].playCount)
    }
}
