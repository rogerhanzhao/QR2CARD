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
        val result = normalizer.normalize("4015927928", "US")

        assertTrue(result.isValid)
        assertEquals("+14015927928", result.e164)
        assertEquals("+1 401 592 7928 (US)", result.display)
    }

    @Test
    fun normalizesChinaNumber() {
        val result = normalizer.normalize("13800138000", "CN")

        assertTrue(result.isValid)
        assertEquals("+8613800138000", result.e164)
        assertEquals("+86 138 0013 8000 (CN)", result.display)
    }

    @Test
    fun rejectsInvalidPhone() {
        val result = normalizer.normalize("123", "US")

        assertFalse(result.isValid)
    }
}
