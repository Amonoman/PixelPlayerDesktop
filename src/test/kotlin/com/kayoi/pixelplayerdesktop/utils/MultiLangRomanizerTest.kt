package com.kayoi.pixelplayerdesktop.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import kotlin.test.assertFalse
import kotlin.test.assertNull

class MultiLangRomanizerTest {

    @Test
    fun testJapaneseRomanization() {
        val japanese = "こんにちは世界"
        assertTrue(MultiLangRomanizer.isJapanese(japanese))
        val romaji = MultiLangRomanizer.romanizeJapanese(japanese)
        assertNotNull(romaji)
        assertTrue(romaji.contains("konnichiwa") || romaji.contains("sekai"))
    }

    @Test
    fun testChineseRomanization() {
        val chinese = "你好世界"
        assertTrue(MultiLangRomanizer.isChinese(chinese))
        val pinyin = MultiLangRomanizer.romanizeChinese(chinese)
        assertNotNull(pinyin)
        assertEquals("ni hao shi jie", pinyin)
    }

    @Test
    fun testKoreanRomanization() {
        val korean = "안녕하세요"
        assertTrue(MultiLangRomanizer.isKorean(korean))
        val romaja = MultiLangRomanizer.romanizeKorean(korean)
        assertNotNull(romaja)
        assertTrue(romaja.startsWith("an"))
    }

    @Test
    fun testHindiRomanization() {
        val hindi = "नमस्ते"
        assertTrue(MultiLangRomanizer.isHindi(hindi))
        val romaji = MultiLangRomanizer.romanizeHindi(hindi)
        assertNotNull(romaji)
        assertTrue(romaji.isNotEmpty())
    }

    @Test
    fun testCyrillicRomanization() {
        val russian = "Привет мир"
        assertTrue(MultiLangRomanizer.isCyrillic(russian))
        val romaji = MultiLangRomanizer.romanizeCyrillic(russian)
        assertNotNull(romaji)
        assertTrue(romaji.contains("Privet") || romaji.contains("privet") || romaji.contains("mir"))
    }

    @Test
    fun testIsRussianCharacterization() {
        // Cyrillic-only
        assertTrue(MultiLangRomanizer.isRussian("Привет мир"))
        assertTrue(MultiLangRomanizer.isRussian("Съешь же ещё этих мягких французских булок, да выпей чаю"))
        assertFalse(MultiLangRomanizer.isRussian("Привіт")) // Contains Ukrainian 'і'
        assertFalse(MultiLangRomanizer.isRussian("Ђорђе")) // Contains Serbian 'ђ'

        // Latin-only
        assertFalse(MultiLangRomanizer.isRussian("Hello world"))
        assertFalse(MultiLangRomanizer.isRussian("The quick brown fox"))

        // Mixed
        assertTrue(MultiLangRomanizer.isRussian("Hello Привет"))
        assertTrue(MultiLangRomanizer.isRussian("Track 01 - Привет (Remix)"))
        assertFalse(MultiLangRomanizer.isRussian("Hello Привіт"))

        // Digits
        assertFalse(MultiLangRomanizer.isRussian("1234567890"))
        assertTrue(MultiLangRomanizer.isRussian("Привет 123"))

        // Whitespace
        assertFalse(MultiLangRomanizer.isRussian("   \t\r\n  "))
        assertTrue(MultiLangRomanizer.isRussian("  \t Привет \n "))

        // Empty string
        assertFalse(MultiLangRomanizer.isRussian(""))

        // Surrogate pairs & emojis
        assertFalse(MultiLangRomanizer.isRussian("😀🎉🚀🎧"))
        assertTrue(MultiLangRomanizer.isRussian("Привет 😀🎉"))
        // Cyrillic supplement outside U+0400..U+04FF passes because it does not match \u0400..\u04FF
        assertTrue(MultiLangRomanizer.isRussian("Привет \u0500"))
    }

    @Test
    fun testRomanizeCyrillicCharacterization() {
        // Cyrillic-only
        assertEquals("Privet mir", MultiLangRomanizer.romanizeCyrillic("Привет мир"))
        assertEquals("Privit", MultiLangRomanizer.romanizeCyrillic("Привіт"))
        assertEquals("Đorđe", MultiLangRomanizer.romanizeCyrillic("Ђорђе"))

        // Latin-only
        assertNull(MultiLangRomanizer.romanizeCyrillic("Hello world"))

        // Mixed
        assertEquals("Hello Privet", MultiLangRomanizer.romanizeCyrillic("Hello Привет"))

        // Digits
        assertNull(MultiLangRomanizer.romanizeCyrillic("12345"))
        assertEquals("123 Privet 456", MultiLangRomanizer.romanizeCyrillic("123 Привет 456"))

        // Whitespace
        assertNull(MultiLangRomanizer.romanizeCyrillic("   \t\n  "))
        assertEquals("   Privet   ", MultiLangRomanizer.romanizeCyrillic("   Привет   "))

        // Empty string
        assertNull(MultiLangRomanizer.romanizeCyrillic(""))

        // Surrogate pairs
        assertNull(MultiLangRomanizer.romanizeCyrillic("😀🎉"))
        assertEquals("Privet 😀🎉", MultiLangRomanizer.romanizeCyrillic("Привет 😀🎉"))
    }
}
