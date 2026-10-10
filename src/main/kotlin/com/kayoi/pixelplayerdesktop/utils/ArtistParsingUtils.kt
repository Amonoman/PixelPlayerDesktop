package com.kayoi.pixelplayerdesktop.utils

val DEFAULT_ARTIST_DELIMITERS = listOf("/", ";", ",", "+", "&")

val DEFAULT_WORD_DELIMITERS = listOf("featuring", "feat.", "feat", "ft.", "ft", "vs.", "vs", "versus", "with", "prod.", "prod")

private const val ESCAPE_SEQUENCE = "\\\\"
private const val ESCAPE_PLACEHOLDER = "\u0000ESCAPED\u0000"

private val FEATURE_BRACKET_REGEX = Regex(
    """[\(\[]\\?\s*(?:featuring|feat\.|feat|ft\.|ft|with|prod\.|prod)\s+(.+?)\s*[\)\]]""",
    RegexOption.IGNORE_CASE
)

fun String.splitArtistsByDelimiters(
    delimiters: List<String>,
    wordDelimiters: List<String> = DEFAULT_WORD_DELIMITERS
): List<String> {
    if ((delimiters.isEmpty() && wordDelimiters.isEmpty()) || this.isBlank()) {
        return listOf(this.trim()).filter { it.isNotEmpty() }
    }

    val sortedDelimiters = delimiters.sortedByDescending { it.length }
    var working = this

    val escapedMappings = mutableMapOf<String, String>()
    sortedDelimiters.forEachIndexed { index, delimiter ->
        val escapedDelimiter = ESCAPE_SEQUENCE + delimiter
        val placeholder = "${ESCAPE_PLACEHOLDER}${index}${ESCAPE_PLACEHOLDER}"
        escapedMappings[placeholder] = delimiter
        working = working.replace(escapedDelimiter, placeholder)
    }

    val patternParts = mutableListOf<String>()
    val sortedWordDelimiters = wordDelimiters.sortedByDescending { it.length }
    for (wd in sortedWordDelimiters) {
        val escaped = Regex.escape(wd)
        if (wd.length == 1) {
            patternParts.add("\\s+$escaped\\s+")
        } else {
            patternParts.add("\\s+$escaped\\s+|\\s+$escaped$|^$escaped\\s+")
        }
    }

    if (sortedDelimiters.isNotEmpty()) {
        val charPattern = sortedDelimiters.joinToString("|") { Regex.escape(it) }
        patternParts.add(charPattern)
    }

    if (patternParts.isEmpty()) {
        return listOf(this.trim()).filter { it.isNotEmpty() }
    }

    val combinedPattern = patternParts.joinToString("|")
    val regex = Regex(combinedPattern, RegexOption.IGNORE_CASE)

    val parts = working.split(regex)

    return parts
        .map { part ->
            var restored = part
            escapedMappings.forEach { (placeholder, delimiter) ->
                restored = restored.replace(placeholder, delimiter)
            }
            restored.trim()
        }
        .filter { it.isNotEmpty() }
        .distinct()
        .ifEmpty { if (this.trim().isNotEmpty()) listOf(this.trim()) else emptyList() }
}

fun String.extractArtistsFromTitle(
    delimiters: List<String> = emptyList(),
    wordDelimiters: List<String> = DEFAULT_WORD_DELIMITERS
): Pair<String, List<String>> {
    if (this.isBlank()) return this to emptyList()

    val extractedArtists = mutableListOf<String>()
    var cleanedTitle = this

    FEATURE_BRACKET_REGEX.findAll(this).forEach { match ->
        val artistString = match.groupValues[1]
        val artists = artistString.splitArtistsByDelimiters(delimiters, wordDelimiters)
        extractedArtists.addAll(artists)
        cleanedTitle = cleanedTitle.replace(match.value, "")
    }

    return cleanedTitle.trim() to extractedArtists.distinct()
}

fun collectArtistNames(
    rawArtistName: String,
    title: String,
    artistDelimiters: List<String>,
    wordDelimiters: List<String> = emptyList(),
    extractFromTitle: Boolean = true
): List<String> {
    return collectArtistNames(
        rawArtistNames = listOf(rawArtistName),
        title = title,
        artistDelimiters = artistDelimiters,
        wordDelimiters = wordDelimiters,
        extractFromTitle = extractFromTitle
    )
}

fun collectArtistNames(
    rawArtistNames: List<String>,
    title: String,
    artistDelimiters: List<String>,
    wordDelimiters: List<String> = emptyList(),
    extractFromTitle: Boolean = true
): List<String> {
    val splitFromArtist = mutableListOf<String>()
    rawArtistNames.forEach { rawArtistName ->
        rawArtistName
            .splitArtistsByDelimiters(artistDelimiters, wordDelimiters)
            .forEach { artistName ->
                if (splitFromArtist.none { it.equals(artistName, ignoreCase = true) }) {
                    splitFromArtist += artistName
                }
            }
    }
    if (!extractFromTitle) {
        return splitFromArtist
    }

    val (_, titleArtists) = title.extractArtistsFromTitle(artistDelimiters, wordDelimiters)
    if (titleArtists.isEmpty()) {
        return splitFromArtist
    }

    val combined = splitFromArtist.toMutableList()
    titleArtists.forEach { titleArtist ->
        if (combined.none { it.equals(titleArtist, ignoreCase = true) }) {
            combined.add(titleArtist)
        }
    }
    return combined
}
