package com.kayoi.pixelplayerdesktop.domain

import com.kayoi.pixelplayerdesktop.data.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DailyMixManagerTest {

    private val dailyMixManager = DailyMixManager()

    @Test
    fun testEmptySongsReturnEmptyList() {
        val result = dailyMixManager.generateDailyMix(emptyList())
        assertTrue(result.isEmpty())

        val yourMix = dailyMixManager.generateYourMix(emptyList())
        assertTrue(yourMix.isEmpty())
    }

    @Test
    fun testGenerateDailyMixLimitsAndDistinct() {
        val sampleSongs = (1..50).map { i ->
            Song(
                id = "song_$i",
                title = "Title $i",
                artist = "Artist ${i % 10}",
                artistId = (i % 10).toLong(),
                genre = "Genre ${i % 5}",
                path = "/music/song_$i.mp3",
                duration = 200000L,
                playCount = i,
                lastPlayedTimestamp = System.currentTimeMillis() - (i * 3600000L)
            )
        }

        val mix = dailyMixManager.generateDailyMix(sampleSongs, limit = 20)
        assertEquals(20, mix.size)
        assertEquals(20, mix.distinctBy { it.id }.size)
    }

    @Test
    fun testDeterministicForSameDay() {
        val sampleSongs = (1..20).map { i ->
            Song(
                id = "song_$i",
                title = "Title $i",
                artist = "Artist $i",
                path = "/music/song_$i.mp3"
            )
        }

        val mix1 = dailyMixManager.generateDailyMix(sampleSongs, limit = 10)
        val mix2 = dailyMixManager.generateDailyMix(sampleSongs, limit = 10)
        assertEquals(mix1.map { it.id }, mix2.map { it.id })
    }

    @Test
    fun testYourMixPrioritizesFavorites() {
        val sampleSongs = (1..30).map { i ->
            Song(
                id = "song_$i",
                title = "Title $i",
                artist = "Artist ${i % 5}",
                artistId = (i % 5).toLong(),
                path = "/music/song_$i.mp3"
            )
        }
        val favoriteIds = setOf("song_1", "song_2", "song_3", "song_4", "song_5")

        val yourMix = dailyMixManager.generateYourMix(sampleSongs, favoriteSongIds = favoriteIds, limit = 15)
        assertEquals(15, yourMix.size)
        val includedFavorites = yourMix.filter { favoriteIds.contains(it.id) }
        assertTrue(includedFavorites.isNotEmpty())
    }
}
