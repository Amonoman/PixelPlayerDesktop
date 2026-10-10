package com.kayoi.pixelplayerdesktop.domain

import com.kayoi.pixelplayerdesktop.data.model.Song
import java.util.Calendar
import kotlin.random.Random

/**
 * Manages generation of personalized dynamic playlists based on user preferences and calendar date.
 */
class DailyMixManager {

    /**
     * Generates a curated mix blending user favorites with other library tracks.
     * Uses a deterministic daily seed combined with an offset so the mix stays consistent
     * throughout the day while offering fresh variety compared to other daily playlists.
     *
     * @param allSongs Complete collection of available library tracks.
     * @param favoriteSongIds Set of song identifiers/paths marked as favorite.
     * @param limit Maximum number of songs to include in the mix.
     * @return Deterministically randomized list with a ~40% favorites to 60% discovery ratio.
     */
    fun generateYourMix(
        allSongs: List<Song>,
        favoriteSongIds: Set<String> = emptySet(),
        limit: Int = 30
    ): List<Song> {
        if (allSongs.isEmpty()) return emptyList()

        // Generate a stable seed per calendar day with an offset to differentiate from general mixes
        val calendar = Calendar.getInstance()
        val seed = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR) + 17
        val random = Random(seed.toLong())

        val favorites = allSongs.filter { favoriteSongIds.contains(it.id) || favoriteSongIds.contains(it.path) }.shuffled(random)
        val others = allSongs.filterNot { favoriteSongIds.contains(it.id) || favoriteSongIds.contains(it.path) }.shuffled(random)

        // Target a 40% favorite / 60% discovery ratio
        val favCount = (limit * 0.4).toInt().coerceAtMost(favorites.size)
        val otherCount = (limit - favCount).coerceAtMost(others.size)

        val result = mutableListOf<Song>()
        result.addAll(favorites.take(favCount))
        result.addAll(others.take(otherCount))

        // Fill remaining slots if either favorites or discovery pools had fewer items than allocated
        if (result.size < limit) {
            val remaining = (favorites + others).filterNot { result.contains(it) }
            result.addAll(remaining.take(limit - result.size))
        }

        return result
    }

    /**
     * Generates a general daily shuffle mix refreshed once per calendar day.
     *
     * @param allSongs Complete collection of available library tracks.
     * @param limit Maximum number of songs to return.
     * @return Daily deterministic selection of tracks.
     */
    fun generateDailyMix(
        allSongs: List<Song>,
        limit: Int = 12
    ): List<Song> {
        if (allSongs.isEmpty()) return emptyList()

        val calendar = Calendar.getInstance()
        val seed = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
        val random = Random(seed.toLong())
        return allSongs.shuffled(random).take(limit.coerceAtMost(allSongs.size))
    }
}
