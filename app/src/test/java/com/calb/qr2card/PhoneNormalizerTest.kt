package com.calb.qr2card

import com.calb.qr2card.domain.PhoneNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNormalizerTest {
    private val normalizer = PhoneNormalizer()

    @Test
    fun normalizesUsNumber() {
        val result = normalizer.normalize("2135897421", "US")

        assertTrue(result.isValid)
        assertEquals("+12135897421", result.e164)
        assertEquals("+1 (213) 589-7421", result.display)
        assertEquals("US", result.regionIso)
    }

    @Test
    fun normalizesChinaNumber() {
        val result = normalizer.normalize("13967258941", "CN")

        assertTrue(result.isValid)
        assertEquals("+8613967258941", result.e164)
        assertEquals("+86 139 6725 8941", result.display)
        assertEquals("CN", result.regionIso)
    }

    @Test
    fun acceptsBrazilNumberRegardlessOfCountryFieldWhenPlusPrefixed() {
        // Country field says CN, but the leading "+55" carries Brazil's country code and wins.
        val result = normalizer.normalize("+55 11 98765-4321", "CN")

        assertTrue(result.isValid)
        assertEquals("BR", result.regionIso)
        assertTrue(result.e164.startsWith("+55"))
    }

    @Test
    fun acceptsBrazilNumberFromRegionHint() {
        val result = normalizer.normalize("11987654321", "BR")

        assertTrue(result.isValid)
        assertEquals("BR", result.regionIso)
        assertTrue(result.e164.startsWith("+55"))
    }

    @Test
    fun keepsUnrecognisedNumberInsteadOfBlockingExport() {
        val result = normalizer.normalize("123", "US")

        assertTrue(result.isValid)
        assertEquals("123", result.display)
    }

    @Test
    fun requiresANonBlankNumber() {
        val result = normalizer.normalize("   ", "US")

        assertFalse(result.isValid)
    }
}
