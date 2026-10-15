package com.kayoi.pixelplayerdesktop.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
}
