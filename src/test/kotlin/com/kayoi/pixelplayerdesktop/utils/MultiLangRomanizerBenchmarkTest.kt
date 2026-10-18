package com.kayoi.pixelplayerdesktop.utils

import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

class MultiLangRomanizerBenchmarkTest {

    private val sampleStrings = listOf(
        "Привет мир",
        "Съешь же ещё этих мягких французских булок, да выпей чаю",
        "Привіт світе",
        "Ђорђе иде у школу",
        "The quick brown fox jumps over the lazy dog",
        "Track 01 - Привет (Remix)",
        "12345 67890",
        "   \t\r\n   ",
        "Привет 😀🎉🚀🎧",
        "Популярная песня 2024 года"
    )

    private val benchmarkDataset = (0 until 5000).map { i ->
        sampleStrings[i % sampleStrings.size]
    }

    @Test
    fun benchmarkPredicates5000Strings() {
        // Warmup
        repeat(500) {
            MultiLangRomanizer.isRussian(sampleStrings[it % sampleStrings.size])
        }

        var matchCount = 0
        val durationMs = measureTimeMillis {
            for (str in benchmarkDataset) {
                if (MultiLangRomanizer.isRussian(str)) {
                    matchCount++
                }
            }
        }

        println("BENCHMARK_IS_RUSSIAN_5000: ${durationMs} ms (matches: $matchCount / 5000)")
        assertTrue(matchCount > 0)
    }

    @Test
    fun benchmarkAllPredicates5000Strings() {
        // Warmup
        repeat(500) {
            MultiLangRomanizer.isRussian(sampleStrings[it % sampleStrings.size])
        }

        var matchCount = 0
        val durationMs = measureTimeMillis {
            for (str in benchmarkDataset) {
                if (MultiLangRomanizer.isRussian(str) ||
                    MultiLangRomanizer.isUkrainian(str) ||
                    MultiLangRomanizer.isSerbian(str) ||
                    MultiLangRomanizer.isBulgarian(str) ||
                    MultiLangRomanizer.isBelarusian(str) ||
                    MultiLangRomanizer.isKyrgyz(str) ||
                    MultiLangRomanizer.isMacedonian(str)
                ) {
                    matchCount++
                }
            }
        }

        println("BENCHMARK_ALL_PREDICATES_5000: ${durationMs} ms (matches: $matchCount / 5000)")
        assertTrue(matchCount > 0)
    }

    @Test
    fun benchmarkRomanizeCyrillic5000Strings() {
        // Warmup
        repeat(500) {
            MultiLangRomanizer.romanizeCyrillic(sampleStrings[it % sampleStrings.size])
        }

        var successCount = 0
        val durationMs = measureTimeMillis {
            for (str in benchmarkDataset) {
                if (MultiLangRomanizer.romanizeCyrillic(str) != null) {
                    successCount++
                }
            }
        }

        println("BENCHMARK_ROMANIZE_CYRILLIC_5000: ${durationMs} ms (romanized: $successCount / 5000)")
        assertTrue(successCount > 0)
    }
}
