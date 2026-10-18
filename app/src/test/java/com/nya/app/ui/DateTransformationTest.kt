package com.nya.app.ui

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateTransformationTest {
    private fun shown(digits: String) = DateTransformation.filter(AnnotatedString(digits)).text.text

    @Test
    fun fullDateGetsSlashes() = assertEquals("09/02/2004", shown("09022004"))

    @Test
    fun partialInputOnlyShowsSlashesOnceDigitsFollow() {
        assertEquals("09", shown("09"))
        assertEquals("09/0", shown("090"))
        assertEquals("09/02", shown("0902"))
        assertEquals("09/02/2", shown("09022"))
    }

    @Test
    fun offsetMappingRoundTripsAndStaysInBounds() {
        for (length in 0..8) {
            val result = DateTransformation.filter(AnnotatedString("12345678".take(length)))
            for (offset in 0..length) {
                val transformed = result.offsetMapping.originalToTransformed(offset)
                assertTrue(transformed in 0..result.text.length)
                assertEquals(offset, result.offsetMapping.transformedToOriginal(transformed))
            }
        }
    }
}
