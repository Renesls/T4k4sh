package com.t4kash.app.ui

import com.t4kash.app.ui.model.UniversityDto
import java.util.Locale

fun extractEmailDomain(email: String): String? {
    val normalized = email.trim().lowercase(Locale.ROOT)
    val separator = normalized.lastIndexOf('@')
    if (separator <= 0 || separator == normalized.lastIndex) return null

    val domain = normalized.substring(separator + 1)
    return domain.takeIf {
        it.contains('.') &&
            !it.startsWith('.') &&
            !it.endsWith('.') &&
            it.none(Char::isWhitespace)
    }
}

fun detectUniversityFromEmail(
    email: String,
    universities: List<UniversityDto>
): UniversityDto? {
    val domain = extractEmailDomain(email) ?: return null
    return universities.firstOrNull { university ->
        university.dominiosCorreo.orEmpty().any { registeredDomain ->
            domain.equals(registeredDomain.trim(), ignoreCase = true)
        }
    }
}
