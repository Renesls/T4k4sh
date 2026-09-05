import Foundation

/// Detección de universidad a partir del dominio del correo.
///
/// Réplica exacta de `ui/EmailDomainMatcher.kt` en Android: mismo criterio de
/// dominio válido y misma comparación exacta contra `dominiosCorreo`.
enum EmailDomainMatcher {
    /// Extrae el dominio de un correo, o `nil` si no tiene forma válida.
    static func domain(from email: String) -> String? {
        let normalized = email.trimmingCharacters(in: .whitespaces).lowercased()

        guard let separatorIndex = normalized.lastIndex(of: "@"),
              separatorIndex != normalized.startIndex,
              separatorIndex != normalized.index(before: normalized.endIndex)
        else { return nil }

        let domain = String(normalized[normalized.index(after: separatorIndex)...])

        guard domain.contains("."),
              !domain.hasPrefix("."),
              !domain.hasSuffix("."),
              !domain.contains(where: \.isWhitespace)
        else { return nil }

        return domain
    }

    /// Busca la universidad cuyo dominio institucional coincide con el correo.
    static func detectUniversity(
        email: String,
        universities: [University]
    ) -> University? {
        guard let domain = domain(from: email) else { return nil }
        return universities.first { university in
            university.dominiosCorreo.contains {
                $0.trimmingCharacters(in: .whitespaces).caseInsensitiveCompare(domain)
                    == .orderedSame
            }
        }
    }
}

/// Validaciones de formulario alineadas con las restricciones del backend
/// (anotaciones `@Size`, `@Pattern` y `@Email` de los DTO).
enum Validation {
    static func isValidEmail(_ email: String) -> Bool {
        let trimmed = email.trimmingCharacters(in: .whitespaces)
        guard trimmed.count <= 150, !trimmed.isEmpty else { return false }
        // Comprobación estructural mínima; el backend es la autoridad final.
        let parts = trimmed.split(separator: "@")
        guard parts.count == 2, !parts[0].isEmpty else { return false }
        return EmailDomainMatcher.domain(from: trimmed) != nil
    }

    /// `RegisterRequest.password`: entre 8 y 72 caracteres.
    static func isValidPassword(_ password: String) -> Bool {
        (8...72).contains(password.count)
    }

    /// `VerifyEmailRequest.codigo`: exactamente 6 dígitos.
    static func isValidCode(_ code: String) -> Bool {
        code.count == 6 && code.allSatisfy(\.isNumber)
    }

    /// `UpdateUsernameRequest.nombreUsuario`: `^@?[A-Za-z0-9][A-Za-z0-9._]{2,29}$`
    static func isValidUsername(_ username: String) -> Bool {
        let clean = username.hasPrefix("@") ? String(username.dropFirst()) : username
        guard (3...30).contains(clean.count) else { return false }
        guard let first = clean.first, first.isLetter || first.isNumber else { return false }
        return clean.allSatisfy { $0.isLetter || $0.isNumber || $0 == "." || $0 == "_" }
    }

    /// Convierte texto de un campo de moneda en `Decimal`, aceptando coma o punto.
    static func decimal(from text: String) -> Decimal? {
        let normalized = text
            .replacingOccurrences(of: ",", with: ".")
            .trimmingCharacters(in: .whitespaces)
        guard !normalized.isEmpty else { return nil }
        return Decimal(string: normalized, locale: Locale(identifier: "en_US"))
    }
}
