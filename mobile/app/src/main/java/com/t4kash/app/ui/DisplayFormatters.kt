package com.t4kash.app.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val API_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"
private const val DISPLAY_DATE_TIME_PATTERN = "dd/MM/yyyy · HH:mm"

fun formatNioCurrency(amount: Double): String {
    return "C$ " + String.format(Locale.US, "%,.2f", amount)
}

fun formatApiDateTime(
    value: String?,
    emptyValue: String = "Sin fecha definida"
): String {
    if (value.isNullOrBlank()) {
        return emptyValue
    }
    val parsed = parseApiDateTime(value)
        ?: return value.substringBefore('.').replace('T', ' ')
    return SimpleDateFormat(
        DISPLAY_DATE_TIME_PATTERN,
        Locale.US
    ).format(parsed)
}

fun parseApiDateTime(value: String?): Date? {
    if (value.isNullOrBlank()) {
        return null
    }
    val normalized = value.substringBefore('.').take(API_DATE_TIME_PATTERN.length)
    return runCatching {
        SimpleDateFormat(API_DATE_TIME_PATTERN, Locale.US).apply {
            isLenient = false
        }.parse(normalized)
    }.getOrNull()
}

fun formatFileSize(bytes: Long): String {
    val safeBytes = bytes.coerceAtLeast(0)
    return when {
        safeBytes >= BYTES_PER_MEGABYTE ->
            String.format(
                Locale.US,
                "%.1f MB",
                safeBytes / BYTES_PER_MEGABYTE.toFloat()
            )

        safeBytes >= BYTES_PER_KILOBYTE ->
            String.format(
                Locale.US,
                "%.1f KB",
                safeBytes / BYTES_PER_KILOBYTE.toFloat()
            )

        else -> "$safeBytes B"
    }
}

private const val BYTES_PER_KILOBYTE = 1024L
private const val BYTES_PER_MEGABYTE = BYTES_PER_KILOBYTE * 1024L
