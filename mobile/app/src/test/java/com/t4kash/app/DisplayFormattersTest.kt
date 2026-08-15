package com.t4kash.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DisplayFormattersTest {

    @Test
    fun formatNioCurrency_usesSymbolThousandsAndDecimals() {
        assertEquals("C$ 1,500.00", formatNioCurrency(1500.0))
    }

    @Test
    fun formatApiDateTime_formatsBackendValue() {
        assertEquals(
            "27/07/2026 · 18:30",
            formatApiDateTime("2026-07-27T18:30:00.123456")
        )
    }

    @Test
    fun formatApiDateTime_usesConfiguredEmptyValue() {
        assertEquals(
            "Por confirmar",
            formatApiDateTime(null, emptyValue = "Por confirmar")
        )
    }

    @Test
    fun parseApiDateTime_rejectsInvalidDates() {
        assertNotNull(parseApiDateTime("2026-07-27T18:30:00"))
        assertNull(parseApiDateTime("2026-15-99T18:30:00"))
    }

    @Test
    fun formatFileSize_usesReadableUnits() {
        assertEquals("0 B", formatFileSize(-1))
        assertEquals("512 B", formatFileSize(512))
        assertEquals("1.0 KB", formatFileSize(1024))
        assertEquals("1.0 MB", formatFileSize(1024 * 1024))
    }
}
