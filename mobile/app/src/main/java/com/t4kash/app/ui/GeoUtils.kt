package com.t4kash.app.ui

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
) {
    fun isValid(): Boolean {
        return latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)
    }
}

fun distanceInKilometers(start: GeoPoint, end: GeoPoint): Double {
    val latitudeDelta = Math.toRadians(end.latitude - start.latitude)
    val longitudeDelta = Math.toRadians(end.longitude - start.longitude)
    val startLatitude = Math.toRadians(start.latitude)
    val endLatitude = Math.toRadians(end.latitude)
    val haversine =
        sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(startLatitude) * cos(endLatitude) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
    return EARTH_RADIUS_KM * 2 *
        atan2(sqrt(haversine), sqrt(1 - haversine))
}

fun formatDistance(distanceKilometers: Double): String {
    return if (distanceKilometers < 1.0) {
        "${(distanceKilometers * 1000).roundToInt()} m"
    } else {
        String.format(Locale.US, "%.1f km", distanceKilometers)
    }
}

private const val EARTH_RADIUS_KM = 6371.0
