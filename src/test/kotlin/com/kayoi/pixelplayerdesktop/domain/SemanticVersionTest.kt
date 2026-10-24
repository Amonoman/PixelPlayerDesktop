package com.kayoi.pixelplayerdesktop.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import com.kayoi.pixelplayerdesktop.domain.model.SemanticVersion

class SemanticVersionTest {

    @Test
    fun testMajorUpdateIsNewer() {
        assertTrue(SemanticVersion.isNewer("v2.0.0", "1.0.0"))
        assertTrue(SemanticVersion.isNewer("2.0.0", "1.9.9"))
        assertFalse(SemanticVersion.isNewer("1.0.0", "2.0.0"))
    }

    @Test
    fun testMinorUpdateIsNewer() {
        assertTrue(SemanticVersion.isNewer("v1.1.0", "1.0.0"))
        assertTrue(SemanticVersion.isNewer("1.2.0", "1.1.9"))
        assertFalse(SemanticVersion.isNewer("1.0.0", "1.1.0"))
    }

    @Test
    fun testPatchUpdateIsNewer() {
        assertTrue(SemanticVersion.isNewer("v1.0.1", "1.0.0"))
        assertFalse(SemanticVersion.isNewer("1.0.0", "1.0.1"))
    }

    @Test
    fun testSameVersionIsNotNewer() {
        assertFalse(SemanticVersion.isNewer("v1.0.0", "1.0.0"))
        assertFalse(SemanticVersion.isNewer("1.0.0", "1.0.0"))
        assertFalse(SemanticVersion.isNewer("v1.0.0", "v1.0.0"))
    }

    @Test
    fun testPrereleaseTransitions() {
        assertTrue(SemanticVersion.isNewer("1.0.0", "1.0.0-alpha.1"))
        assertFalse(SemanticVersion.isNewer("1.0.0-alpha.1", "1.0.0"))
        assertTrue(SemanticVersion.isNewer("1.0.0-beta.1", "1.0.0-alpha.1"))
    }
}
