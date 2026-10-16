package com.kayoi.pixelplayerdesktop.domain

import com.kayoi.pixelplayerdesktop.data.model.AlbumPlaybackSummary
import com.kayoi.pixelplayerdesktop.data.model.ArtistPlaybackSummary
import com.kayoi.pixelplayerdesktop.data.model.GenrePlaybackSummary
import com.kayoi.pixelplayerdesktop.data.model.PlaybackEvent
import com.kayoi.pixelplayerdesktop.data.model.PlaybackStatsSummary
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SongPlaybackSummary
import com.kayoi.pixelplayerdesktop.data.model.StatsTimeRange
import com.kayoi.pixelplayerdesktop.data.model.TimelineEntry
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PlaybackStatsCalculator {

    private data class TimelineBucket(
        val label: String,
        val startMillis: Long,
        val endMillis: Long
    )

    fun calculateSummary(
        range: StatsTimeRange,
        events: List<PlaybackEvent>,
        songs: List<Song>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): PlaybackStatsSummary {
        val (startBound, endBound) = range.resolveBounds(nowMillis, zoneId)
        val songMap = songs.associateBy { it.id }

        val filteredEvents = events.mapNotNull { event ->
            val eventEnd = event.timestamp
            val rawDuration = if (event.durationMs > 0L) event.durationMs else (songMap[event.songId]?.duration ?: 0L)
            val eventStart = (eventEnd - rawDuration).coerceAtLeast(0L)

            val lowerBound = startBound ?: Long.MIN_VALUE
            if (eventEnd < lowerBound || eventStart > endBound) {
                return@mapNotNull null
            }

            val clippedStart = max(eventStart, lowerBound)
            val clippedEnd = min(eventEnd, endBound)
            val clippedDuration = (clippedEnd - clippedStart).coerceAtLeast(0L)
            if (clippedDuration <= 0L && rawDuration <= 0L) {
                return@mapNotNull null
            }

            event.copy(
                timestamp = clippedEnd,
                durationMs = if (clippedDuration > 0L) clippedDuration else rawDuration
            )
        }

        val totalPlays = filteredEvents.size
        val totalDuration = filteredEvents.sumOf { it.durationMs }
        val eventsBySong = filteredEvents.groupBy { it.songId }
        val uniqueSongs = eventsBySong.keys.size

        val effectiveStart = startBound
            ?: filteredEvents.minOfOrNull { it.timestamp - it.durationMs }
            ?: nowMillis

        val daySpan = if (effectiveStart < endBound) {
            val startDate = Instant.ofEpochMilli(effectiveStart).atZone(zoneId).toLocalDate()
            val endDate = Instant.ofEpochMilli(endBound).atZone(zoneId).toLocalDate()
            max(1L, ChronoUnit.DAYS.between(startDate, endDate) + 1)
        } else {
            1L
        }

        val averageDailyDuration = if (daySpan > 0) totalDuration / daySpan else totalDuration

        val allSongsSummary = eventsBySong.mapNotNull { (songId, songEvents) ->
            val song = songMap[songId] ?: return@mapNotNull null
            SongPlaybackSummary(
                songId = songId,
                title = song.title.ifBlank { song.path.substringAfterLast('/') },
                artist = song.displayArtist.ifBlank { "Unknown Artist" },
                albumArtUri = song.albumArtUriString,
                totalDurationMs = songEvents.sumOf { it.durationMs },
                playCount = songEvents.size
            )
        }.sortedWith(
            compareByDescending<SongPlaybackSummary> { it.totalDurationMs }
                .thenByDescending { it.playCount }
        )

        val topSongs = allSongsSummary.take(5)

        val topArtists = eventsBySong.entries.flatMap { (songId, songEvents) ->
            val song = songMap[songId]
            val artists = if (song != null && song.artists.isNotEmpty()) {
                song.artists.map { it.name.trim() }.filter { it.isNotBlank() }
            } else {
                listOf(song?.displayArtist?.trim()?.ifBlank { "Unknown Artist" } ?: "Unknown Artist")
            }
            artists.map { artist -> artist to songEvents }
        }.groupBy({ it.first }, { it.second })
            .map { (artist, eventLists) ->
                val allEventsForArtist = eventLists.flatten()
                ArtistPlaybackSummary(
                    artist = artist,
                    totalDurationMs = allEventsForArtist.sumOf { it.durationMs },
                    playCount = allEventsForArtist.size,
                    uniqueSongs = allEventsForArtist.map { it.songId }.distinct().size
                )
            }.sortedWith(
                compareByDescending<ArtistPlaybackSummary> { it.totalDurationMs }
                    .thenByDescending { it.playCount }
            ).take(5)

        val topAlbums = eventsBySong.entries.groupBy { (songId, _) ->
            songMap[songId]?.album?.takeIf { it.isNotBlank() } ?: "Unknown Album"
        }.map { (album, entries) ->
            val flattenedEvents = entries.flatMap { it.value }
            val firstSong = entries.firstNotNullOfOrNull { songMap[it.key] }
            AlbumPlaybackSummary(
                album = album,
                albumArtUri = firstSong?.albumArtUriString,
                totalDurationMs = flattenedEvents.sumOf { it.durationMs },
                playCount = flattenedEvents.size,
                uniqueSongs = entries.size
            )
        }.sortedWith(
            compareByDescending<AlbumPlaybackSummary> { it.totalDurationMs }
                .thenByDescending { it.playCount }
        ).take(5)

        val topGenres = eventsBySong.entries.groupBy { (songId, _) ->
            songMap[songId]?.genre?.takeIf { it.isNotBlank() } ?: "Unknown Genre"
        }.map { (genre, entries) ->
            val flattenedEvents = entries.flatMap { it.value }
            val uniqueArtists = entries.mapNotNull { songMap[it.key]?.displayArtist }.distinct().size
            GenrePlaybackSummary(
                genre = genre,
                totalDurationMs = flattenedEvents.sumOf { it.durationMs },
                playCount = flattenedEvents.size,
                uniqueArtists = uniqueArtists
            )
        }.sortedWith(
            compareByDescending<GenrePlaybackSummary> { it.totalDurationMs }
                .thenByDescending { it.playCount }
        ).take(5)

        val datesWithEvents = filteredEvents.map { event ->
            Instant.ofEpochMilli(event.timestamp).atZone(zoneId).toLocalDate()
        }.distinct().sorted()

        val activeDays = datesWithEvents.size
        var longestStreak = 0
        var currentStreak = 0
        var lastDate: LocalDate? = null
        datesWithEvents.forEach { date ->
            if (lastDate == null || date == lastDate.plusDays(1)) {
                currentStreak++
            } else {
                currentStreak = 1
            }
            if (currentStreak > longestStreak) {
                longestStreak = currentStreak
            }
            lastDate = date
        }

        val eventsByDayOfWeek = filteredEvents.groupBy { event ->
            Instant.ofEpochMilli(event.timestamp).atZone(zoneId).dayOfWeek
        }
        val peakDay = eventsByDayOfWeek.maxByOrNull { entry -> entry.value.sumOf { it.durationMs } }
        val peakDayLabel = peakDay?.key?.getDisplayName(TextStyle.FULL, Locale.US)
        val peakDayDuration = peakDay?.value?.sumOf { it.durationMs } ?: 0L

        val timelineBuckets = createTimelineBuckets(range, zoneId, Instant.ofEpochMilli(endBound))
        val timelineEntries = accumulateTimelineEntries(timelineBuckets, filteredEvents)

        return PlaybackStatsSummary(
            range = range,
            startTimestamp = startBound,
            endTimestamp = endBound,
            totalDurationMs = totalDuration,
            totalPlayCount = totalPlays,
            uniqueSongs = uniqueSongs,
            averageDailyDurationMs = averageDailyDuration,
            songs = allSongsSummary,
            topSongs = topSongs,
            topArtists = topArtists,
            topAlbums = topAlbums,
            topGenres = topGenres,
            timeline = timelineEntries,
            activeDays = activeDays,
            longestStreakDays = longestStreak,
            peakDayLabel = peakDayLabel,
            peakDayDurationMs = peakDayDuration
        )
    }

    private fun createTimelineBuckets(
        range: StatsTimeRange,
        zoneId: ZoneId,
        now: Instant
    ): List<TimelineBucket> {
        return when (range) {
            StatsTimeRange.DAY -> {
                val dayStart = now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
                val formatter = DateTimeFormatter.ofPattern("ha", Locale.US)
                (0 until 6).map { index ->
                    val bucketStart = dayStart.plus(Duration.ofHours((index * 4).toLong()))
                    val bucketEnd = bucketStart.plus(Duration.ofHours(4))
                    TimelineBucket(
                        label = formatter.format(bucketStart.atZone(zoneId)).lowercase(Locale.US),
                        startMillis = bucketStart.toEpochMilli(),
                        endMillis = bucketEnd.toEpochMilli()
                    )
                }
            }
            StatsTimeRange.WEEK -> {
                val startOfWeek = now.atZone(zoneId).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                (0 until 7).map { index ->
                    val day = startOfWeek.plusDays(index.toLong())
                    val start = day.atStartOfDay(zoneId).toInstant()
                    val end = day.plusDays(1).atStartOfDay(zoneId).toInstant()
                    TimelineBucket(
                        label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US),
                        startMillis = start.toEpochMilli(),
                        endMillis = end.toEpochMilli()
                    )
                }
            }
            StatsTimeRange.MONTH -> {
                val yearMonth = YearMonth.from(now.atZone(zoneId))
                val daysInMonth = yearMonth.lengthOfMonth()
                (0 until 4).mapNotNull { index ->
                    val startDay = index * 7 + 1
                    if (startDay > daysInMonth) return@mapNotNull null
                    val endDay = if (index == 3) daysInMonth else min(startDay + 6, daysInMonth)
                    val start = yearMonth.atDay(startDay).atStartOfDay(zoneId).toInstant()
                    val end = yearMonth.atDay(endDay).plusDays(1).atStartOfDay(zoneId).toInstant()
                    TimelineBucket(
                        label = "Week ${index + 1}",
                        startMillis = start.toEpochMilli(),
                        endMillis = end.toEpochMilli()
                    )
                }
            }
            StatsTimeRange.YEAR -> {
                val year = Year.from(now.atZone(zoneId))
                (1..12).map { monthIndex ->
                    val start = year.atMonth(monthIndex).atDay(1).atStartOfDay(zoneId).toInstant()
                    val end = year.atMonth(monthIndex).atEndOfMonth().plusDays(1).atStartOfDay(zoneId).toInstant()
                    TimelineBucket(
                        label = year.atMonth(monthIndex).month.getDisplayName(TextStyle.SHORT, Locale.US),
                        startMillis = start.toEpochMilli(),
                        endMillis = end.toEpochMilli()
                    )
                }
            }
            StatsTimeRange.ALL -> {
                val currentYear = now.atZone(zoneId).year
                (currentYear - 4..currentYear).map { y ->
                    val start = Year.of(y).atDay(1).atStartOfDay(zoneId).toInstant()
                    val end = Year.of(y + 1).atDay(1).atStartOfDay(zoneId).toInstant()
                    TimelineBucket(
                        label = y.toString(),
                        startMillis = start.toEpochMilli(),
                        endMillis = end.toEpochMilli()
                    )
                }
            }
        }
    }

    private fun accumulateTimelineEntries(
        buckets: List<TimelineBucket>,
        events: List<PlaybackEvent>
    ): List<TimelineEntry> {
        if (buckets.isEmpty()) return emptyList()
        val durationByBucket = LongArray(buckets.size)
        val playCountByBucket = IntArray(buckets.size)

        events.forEach { event ->
            val eventEnd = event.timestamp
            val duration = event.durationMs.coerceAtLeast(0L)
            val eventStart = (eventEnd - duration).coerceAtLeast(0L)

            buckets.forEachIndexed { index, bucket ->
                val overlapStart = max(eventStart, bucket.startMillis)
                val overlapEnd = min(eventEnd, bucket.endMillis)
                val overlap = (overlapEnd - overlapStart).coerceAtLeast(0L)
                if (overlap > 0 || (eventStart >= bucket.startMillis && eventStart < bucket.endMillis)) {
                    durationByBucket[index] += if (overlap > 0) overlap else duration
                    playCountByBucket[index] += 1
                }
            }
        }

        return buckets.mapIndexed { index, bucket ->
            TimelineEntry(
                label = bucket.label,
                totalDurationMs = durationByBucket[index],
                playCount = playCountByBucket[index]
            )
        }
    }
}
