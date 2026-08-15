package com.t4kash.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun geoPoint_validatesCoordinateRanges() {
        assertTrue(GeoPoint(12.11499, -86.23617).isValid())
        assertFalse(GeoPoint(0.0, 0.0).isValid())
        assertFalse(GeoPoint(95.0, -86.0).isValid())
    }

    @Test
    fun distanceInKilometers_returnsZeroForSamePoint() {
        val point = GeoPoint(12.11499, -86.23617)

        assertEquals(0.0, distanceInKilometers(point, point), 0.001)
    }

    @Test
    fun distanceInKilometers_calculatesKnownManaguaDistance() {
        val university = GeoPoint(12.13282, -86.27115)
        val cityCenter = GeoPoint(12.11499, -86.23617)

        assertEquals(
            4.3,
            distanceInKilometers(university, cityCenter),
            0.3
        )
    }

    @Test
    fun formatDistance_usesMetersAndKilometers() {
        assertEquals("450 m", formatDistance(0.45))
        assertEquals("4.3 km", formatDistance(4.26))
    }
}
