package com.kayoi.pixelplayerdesktop.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArtistParsingUtilsTest {

    @Test
    fun `DEFAULT_ARTIST_DELIMITERS contains default delimiters`() {
        assertEquals(listOf("/", ";", ",", "+", "&"), DEFAULT_ARTIST_DELIMITERS)
    }

    @Test
    fun `splitArtistsByDelimiters correctly splits with character and word delimiters`() {
        val result = "Drake feat. Rihanna; Travis Scott".splitArtistsByDelimiters(
            delimiters = listOf(";", ","),
            wordDelimiters = listOf("feat.", "ft.")
        )
        assertEquals(listOf("Drake", "Rihanna", "Travis Scott"), result)
    }

    @Test
    fun `splitArtistsByDelimiters respects escaped delimiters`() {
        val result = "AC\\\\+DC + Queen".splitArtistsByDelimiters(
            delimiters = listOf("+"),
            wordDelimiters = emptyList()
        )
        assertEquals(listOf("AC+DC", "Queen"), result)
    }

    @Test
    fun `extractArtistsFromTitle extracts featured artists from title brackets`() {
        val (cleanedTitle, artists) = "Feels (feat. Katy Perry, Big Sean)".extractArtistsFromTitle(
            delimiters = listOf(","),
            wordDelimiters = listOf("feat.")
        )
        assertEquals("Feels", cleanedTitle)
        assertEquals(listOf("Katy Perry", "Big Sean"), artists)
    }

    @Test
    fun `collectArtistNames merges artist tag and title features without duplicates`() {
        val result = collectArtistNames(
            rawArtistName = "Calvin Harris, Pharrell Williams",
            title = "Feels (feat. Katy Perry, Big Sean)",
            artistDelimiters = listOf(",", ";"),
            wordDelimiters = listOf("feat."),
            extractFromTitle = true
        )
        assertEquals(
            listOf("Calvin Harris", "Pharrell Williams", "Katy Perry", "Big Sean"),
            result
        )
    }

    @Test
    fun `collectArtistNames respects extractFromTitle false`() {
        val result = collectArtistNames(
            rawArtistName = "Calvin Harris, Pharrell Williams",
            title = "Feels (feat. Katy Perry, Big Sean)",
            artistDelimiters = listOf(",", ";"),
            wordDelimiters = listOf("feat."),
            extractFromTitle = false
        )
        assertEquals(
            listOf("Calvin Harris", "Pharrell Williams"),
            result
        )
    }
}
