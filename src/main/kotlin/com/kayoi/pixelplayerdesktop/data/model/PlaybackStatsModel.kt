package com.kayoi.pixelplayerdesktop.data.model

data class SongPlaybackSummary(
    val songId: String,
    val title: String,
    val artist: String,
    val albumArtUri: String?,
    val totalDurationMs: Long,
    val playCount: Int
)

data class ArtistPlaybackSummary(
    val artist: String,
    val totalDurationMs: Long,
    val playCount: Int,
    val uniqueSongs: Int
)

data class GenrePlaybackSummary(
    val genre: String,
    val totalDurationMs: Long,
    val playCount: Int,
    val uniqueArtists: Int
)

data class AlbumPlaybackSummary(
    val album: String,
    val albumArtUri: String?,
    val totalDurationMs: Long,
    val playCount: Int,
    val uniqueSongs: Int
)

data class TimelineEntry(
    val label: String,
    val totalDurationMs: Long,
    val playCount: Int = 0
)

data class PlaybackStatsSummary(
    val range: StatsTimeRange,
    val startTimestamp: Long?,
    val endTimestamp: Long,
    val totalDurationMs: Long,
    val totalPlayCount: Int,
    val uniqueSongs: Int,
    val averageDailyDurationMs: Long,
    val songs: List<SongPlaybackSummary> = emptyList(),
    val topSongs: List<SongPlaybackSummary> = emptyList(),
    val topArtists: List<ArtistPlaybackSummary> = emptyList(),
    val topAlbums: List<AlbumPlaybackSummary> = emptyList(),
    val topGenres: List<GenrePlaybackSummary> = emptyList(),
    val timeline: List<TimelineEntry> = emptyList(),
    val activeDays: Int = 0,
    val longestStreakDays: Int = 0,
    val totalSessions: Int = 0,
    val averageSessionDurationMs: Long = 0L,
    val longestSessionDurationMs: Long = 0L,
    val peakDayLabel: String? = null,
    val peakDayDurationMs: Long = 0L
) {
    fun hasListeningActivity(): Boolean = totalDurationMs > 0 || totalPlayCount > 0
}
