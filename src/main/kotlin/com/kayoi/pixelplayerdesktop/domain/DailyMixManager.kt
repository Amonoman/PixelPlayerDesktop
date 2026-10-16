package com.kayoi.pixelplayerdesktop.domain

import com.kayoi.pixelplayerdesktop.data.model.Song
import java.util.Calendar
import java.util.LinkedHashSet
import java.util.Random
import java.util.concurrent.TimeUnit

class DailyMixManager {

    fun generateDailyMix(
        allSongs: List<Song>,
        favoriteSongIds: Set<String> = emptySet(),
        limit: Int = 30
    ): List<Song> {
        if (allSongs.isEmpty()) {
            return emptyList()
        }

        val calendar = Calendar.getInstance()
        val seed = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
        val random = Random(seed.toLong())

        val rankedSongs = computeRankedSongs(allSongs, favoriteSongIds, random)
        if (rankedSongs.isEmpty()) {
            return allSongs.shuffled(kotlin.random.Random(seed.toLong())).take(limit.coerceAtMost(allSongs.size))
        }

        val diversityState = DiversityState()
        val selected = pickWithDiversity(rankedSongs, favoriteSongIds, limit, diversityState)
        if (selected.size >= limit || selected.size == rankedSongs.size) {
            return selected
        }

        val remainingRanked = rankedSongs.filterNot { candidate ->
            selected.any { it.id == candidate.song.id }
        }
        val quotaFill = pickWithDiversity(remainingRanked, favoriteSongIds, limit - selected.size, diversityState)
        val combined = (selected + quotaFill).distinctBy { it.id }.toMutableList()

        if (combined.size < limit) {
            val remaining = allSongs
                .filterNot { song -> combined.any { it.id == song.id } }
                .shuffled(kotlin.random.Random(seed.toLong()))
            for (song in remaining) {
                combined.add(song)
                if (combined.size >= limit) break
            }
        }

        return combined.take(limit.coerceAtMost(combined.size))
    }

    fun generateYourMix(
        allSongs: List<Song>,
        favoriteSongIds: Set<String> = emptySet(),
        limit: Int = 60
    ): List<Song> {
        if (allSongs.isEmpty()) {
            return emptyList()
        }

        val calendar = Calendar.getInstance()
        val seed = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR) + 17
        val random = Random(seed.toLong())
        val rankedSongs = computeRankedSongs(allSongs, favoriteSongIds, random)

        if (rankedSongs.isEmpty()) {
            return allSongs.shuffled(kotlin.random.Random(seed.toLong())).take(limit.coerceAtMost(allSongs.size))
        }

        val favoriteSectionSize = (limit * 0.3).toInt().coerceAtLeast(5).coerceAtMost(limit)
        val coreSectionSize = (limit * 0.45).toInt().coerceAtLeast(10).coerceAtMost(limit)
        val discoverySectionSize = (limit - favoriteSectionSize - coreSectionSize).coerceAtLeast(0)

        val diversityState = DiversityState()

        val favoriteSection = pickWithDiversity(
            rankedSongs.filter { favoriteSongIds.contains(it.song.id) },
            favoriteSongIds,
            favoriteSectionSize,
            diversityState
        )

        val alreadySelectedIds = favoriteSection.map { it.id }.toMutableSet()

        val coreSection = pickWithDiversity(
            rankedSongs.filterNot { alreadySelectedIds.contains(it.song.id) },
            favoriteSongIds,
            coreSectionSize,
            diversityState
        )

        alreadySelectedIds.addAll(coreSection.map { it.id })

        val discoveryCandidates = rankedSongs
            .filterNot { alreadySelectedIds.contains(it.song.id) }
            .sortedWith(compareByDescending<RankedSong> { it.discoveryScore }.thenBy { it.song.id })

        val discoverySection = pickWithDiversity(
            discoveryCandidates,
            favoriteSongIds,
            discoverySectionSize,
            diversityState
        )

        val orderedResult = LinkedHashSet<Song>()
        orderedResult.addAll(favoriteSection)
        orderedResult.addAll(coreSection)
        orderedResult.addAll(discoverySection)

        if (orderedResult.size < limit) {
            val quotaFill = pickWithDiversity(
                rankedSongs.filterNot { orderedResult.any { selected -> selected.id == it.song.id } },
                favoriteSongIds,
                limit - orderedResult.size,
                diversityState
            )
            orderedResult.addAll(quotaFill)
        }

        if (orderedResult.size < limit) {
            val resultIds = orderedResult.mapTo(HashSet()) { it.id }
            val filler = allSongs
                .filterNot { it.id in resultIds }
                .shuffled(kotlin.random.Random(seed.toLong()))
            for (song in filler) {
                orderedResult.add(song)
                if (orderedResult.size >= limit) break
            }
        }

        return orderedResult.take(limit.coerceAtMost(orderedResult.size))
    }

    private fun computeRankedSongs(
        allSongs: List<Song>,
        favoriteSongIds: Set<String>,
        random: Random
    ): List<RankedSong> {
        if (allSongs.isEmpty()) return emptyList()

        val songById = allSongs.associateBy { it.id }
        val now = System.currentTimeMillis()

        val artistAffinity = mutableMapOf<Long, Double>()
        val genreAffinity = mutableMapOf<String, Double>()

        allSongs.forEach { song ->
            val playCount = song.playCount
            val durationMs = if (song.duration > 0L) playCount * song.duration else playCount * 180000L
            val weight = playCount.toDouble() + (durationMs / 60000.0)
            if (weight > 0) {
                artistAffinity.merge(song.artistId, weight, Double::plus)
                normalizeGenreKey(song.genre)?.let { genreAffinity.merge(it, weight, Double::plus) }
            }
        }

        val favoriteArtistWeights = mutableMapOf<Long, Int>()
        favoriteSongIds.forEach { id ->
            val song = songById[id] ?: return@forEach
            favoriteArtistWeights.merge(song.artistId, 1, Int::plus)
        }

        val maxPlayCount = allSongs.maxOfOrNull { it.playCount }?.takeIf { it > 0 } ?: 1
        val maxDuration = allSongs.maxOfOrNull {
            if (it.duration > 0L) it.playCount * it.duration else it.playCount * 180000L
        }?.takeIf { it > 0L } ?: 1L
        val maxArtistAffinity = artistAffinity.values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        val maxGenreAffinity = genreAffinity.values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        val maxFavoriteArtist = favoriteArtistWeights.values.maxOrNull()?.takeIf { it > 0 } ?: 1

        return allSongs.map { song ->
            val playCount = song.playCount
            val durationMs = if (song.duration > 0L) playCount * song.duration else playCount * 180000L
            val playCountScore = playCount.toDouble() / maxPlayCount
            val durationScore = durationMs.toDouble() / maxDuration
            val affinityScore = (playCountScore * 0.7 + durationScore * 0.3).coerceIn(0.0, 1.0)

            val genreKey = normalizeGenreKey(song.genre)
            val artistPreference = artistAffinity[song.artistId]?.div(maxArtistAffinity) ?: 0.0
            val genrePreference = genreKey?.let { (genreAffinity[it] ?: 0.0) / maxGenreAffinity } ?: 0.0
            val favoriteArtistPreference = favoriteArtistWeights[song.artistId]?.toDouble()?.div(maxFavoriteArtist) ?: 0.0
            val preferenceScore = if (genreKey == null) {
                (artistPreference * 0.6) + (favoriteArtistPreference * 0.4)
            } else {
                (artistPreference * 0.45) +
                    (genrePreference * 0.35) +
                    (favoriteArtistPreference * 0.20)
            }

            val recencyScore = computeRecencyScore(song.lastPlayedTimestamp, now)
            val noveltyScore = computeNoveltyScore(song.dateAdded, now)
            val favoriteScore = if (favoriteSongIds.contains(song.id)) 1.0 else 0.0
            val baselineScore = if (playCount == 0 && song.lastPlayedTimestamp == 0L) 0.1 else 0.0
            val noise = random.nextDouble() * 0.005

            val finalScore = (preferenceScore * 0.45) +
                (affinityScore * 0.25) +
                (recencyScore * 0.15) +
                (favoriteScore * 0.1) +
                (noveltyScore * 0.05) +
                baselineScore +
                noise

            val discoveryScore = ((1.0 - affinityScore).coerceIn(0.0, 1.0) * 0.6) +
                (noveltyScore * 0.25) +
                (preferenceScore * 0.15)

            RankedSong(
                song = song,
                finalScore = if (finalScore.isNaN() || finalScore.isInfinite()) 0.0 else finalScore,
                discoveryScore = if (discoveryScore.isNaN() || discoveryScore.isInfinite()) 0.0 else discoveryScore,
                affinityScore = affinityScore,
                recencyScore = recencyScore,
                noveltyScore = noveltyScore,
                favoriteScore = favoriteScore
            )
        }.sortedWith(compareByDescending<RankedSong> { it.finalScore }.thenBy { it.song.id })
    }

    private fun pickWithDiversity(
        rankedSongs: List<RankedSong>,
        favoriteSongIds: Set<String>,
        limit: Int,
        state: DiversityState = DiversityState()
    ): List<Song> {
        if (limit <= 0 || rankedSongs.isEmpty()) return emptyList()

        val selected = mutableListOf<Song>()

        for (candidate in rankedSongs) {
            if (selected.size >= limit) break
            val artistId = candidate.song.artistId
            val isFavorite = favoriteSongIds.contains(candidate.song.id)
            val maxPerArtist = if (isFavorite) 3 else 2
            val currentCount = state.artistCounts.getOrDefault(artistId, 0)
            if (currentCount >= maxPerArtist) continue

            val genreKey = normalizeGenreKey(candidate.song.genre)
            val maxPerGenre = if (genreKey == null) {
                maxUnknownGenreCount(limit, isFavorite)
            } else {
                maxKnownGenreCount(limit, isFavorite)
            }
            if (genreKey == null) {
                if (state.unknownGenreCount >= maxPerGenre) continue
            } else {
                val currentGenreCount = state.genreCounts.getOrDefault(genreKey, 0)
                if (currentGenreCount >= maxPerGenre) continue
            }

            selected += candidate.song
            state.artistCounts[artistId] = currentCount + 1
            if (genreKey == null) {
                state.unknownGenreCount += 1
            } else {
                state.genreCounts[genreKey] = state.genreCounts.getOrDefault(genreKey, 0) + 1
            }
        }

        if (selected.size < limit) {
            val selectedIds = selected.mapTo(HashSet()) { it.id }
            for (candidate in rankedSongs) {
                if (selected.size >= limit) break
                if (candidate.song.id in selectedIds) continue
                selected += candidate.song
                selectedIds += candidate.song.id
            }
        }

        return selected.take(limit)
    }

    private fun normalizeGenreKey(rawGenre: String?): String? {
        val normalized = rawGenre
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return if (normalized.contains("unknown")) {
            null
        } else {
            normalized
        }
    }

    private fun maxKnownGenreCount(limit: Int, isFavorite: Boolean): Int {
        val baseCap = when {
            limit <= 12 -> 2
            limit <= 30 -> 3
            else -> 4
        }
        return baseCap + if (isFavorite) 1 else 0
    }

    private fun maxUnknownGenreCount(limit: Int, isFavorite: Boolean): Int {
        val baseCap = when {
            limit <= 12 -> 1
            limit <= 30 -> 2
            else -> 3
        }
        return baseCap + if (isFavorite) 1 else 0
    }

    private fun computeRecencyScore(lastPlayedTimestamp: Long?, now: Long): Double {
        if (lastPlayedTimestamp == null || lastPlayedTimestamp <= 0L) return 0.6
        val daysSinceLastPlay = ((now - lastPlayedTimestamp).coerceAtLeast(0L) / TimeUnit.DAYS.toMillis(1)).toDouble()
        return when {
            daysSinceLastPlay < 1 -> 0.2
            daysSinceLastPlay < 3 -> 0.5
            daysSinceLastPlay < 7 -> 0.7
            daysSinceLastPlay < 14 -> 0.85
            else -> 1.0
        }
    }

    private fun computeNoveltyScore(dateAdded: Long, now: Long): Double {
        if (dateAdded <= 0L) return 0.0
        val dateAddedMillis = if (dateAdded < 10_000_000_000L) {
            TimeUnit.SECONDS.toMillis(dateAdded)
        } else {
            dateAdded
        }
        val daysSinceAdded = ((now - dateAddedMillis).coerceAtLeast(0L) / TimeUnit.DAYS.toMillis(1)).toDouble()
        return (1.0 - (daysSinceAdded / 60.0)).coerceIn(0.0, 1.0)
    }

    private data class RankedSong(
        val song: Song,
        val finalScore: Double,
        val discoveryScore: Double,
        val affinityScore: Double,
        val recencyScore: Double,
        val noveltyScore: Double,
        val favoriteScore: Double
    )

    private data class DiversityState(
        val artistCounts: MutableMap<Long, Int> = mutableMapOf(),
        val genreCounts: MutableMap<String, Int> = mutableMapOf(),
        var unknownGenreCount: Int = 0
    )
}
