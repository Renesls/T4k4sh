import Foundation

/// Formateo compartido de fechas y montos.
///
/// El backend serializa `LocalDateTime` **sin zona horaria** (`yyyy-MM-dd'T'HH:mm:ss`,
/// con fracción de segundo opcional). Android lo interpreta con `SimpleDateFormat`
/// en la zona del dispositivo; iOS hace exactamente lo mismo para que ambas
/// aplicaciones muestren la misma hora.
enum APIDateFormatter {
    /// Formato canónico que acepta y devuelve el backend.
    private static let canonical: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = .current
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        formatter.isLenient = false
        return formatter
    }()

    /// Convierte un `LocalDateTime` del backend en `Date`.
    /// Descarta la fracción de segundo, igual que `parseApiDateTime` en Android.
    static func date(from raw: String) -> Date? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }

        let withoutFraction = trimmed.split(separator: ".", maxSplits: 1).first.map(String.init) ?? trimmed
        // Algunos valores podrían llegar con offset ("+00:00"); el backend no lo hace,
        // pero recortar a la longitud del patrón lo deja igualmente parseable.
        let normalized = String(withoutFraction.prefix(19))
        return canonical.date(from: normalized)
    }

    /// Serializa una fecha en el formato que espera Jackson (`ISO_LOCAL_DATE_TIME`).
    static func string(from date: Date) -> String {
        canonical.string(from: date)
    }
}

/// Presentación de valores en pantalla.
enum DisplayFormatter {
    /// Moneda de la plataforma. El backend trabaja en NIO (`C$`), igual que Android
    /// en `formatNioCurrency`.
    private static let currency: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.locale = Locale(identifier: "en_US")
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        formatter.usesGroupingSeparator = true
        return formatter
    }()

    static func money(_ amount: Decimal?, currencyCode: String = "NIO") -> String {
        let value = amount ?? 0
        let symbol = currencyCode == "NIO" ? "C$" : currencyCode + " "
        let number = currency.string(from: value as NSDecimalNumber) ?? "0.00"
        return symbol + (currencyCode == "NIO" ? " " : "") + number
    }

    private static let dateTime: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "es_NI")
        formatter.dateFormat = "dd/MM/yyyy · HH:mm"
        return formatter
    }()

    private static let longDay: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "es_NI")
        formatter.dateFormat = "dd 'de' MMMM 'de' yyyy"
        return formatter
    }()

    private static let clock: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "es_NI")
        formatter.dateFormat = "HH:mm"
        return formatter
    }()

    static func dateTime(_ date: Date?, fallback: String = "Sin fecha definida") -> String {
        guard let date else { return fallback }
        return dateTime.string(from: date)
    }

    static func time(_ date: Date?) -> String {
        guard let date else { return "" }
        return clock.string(from: date)
    }

    /// Separador de día para el chat: "Hoy", "Ayer" o la fecha larga.
    static func daySeparator(_ date: Date?) -> String {
        guard let date else { return "Fecha desconocida" }
        let calendar = Calendar.current
        if calendar.isDateInToday(date) { return "Hoy" }
        if calendar.isDateInYesterday(date) { return "Ayer" }
        return longDay.string(from: date).capitalizedFirstLetter
    }

    /// Tiempo relativo compacto para el feed y las notificaciones.
    static func relative(_ date: Date?) -> String {
        guard let date else { return "" }
        let seconds = Date().timeIntervalSince(date)
        switch seconds {
        case ..<60: return "ahora"
        case ..<3_600: return "hace \(Int(seconds / 60)) min"
        case ..<86_400: return "hace \(Int(seconds / 3_600)) h"
        case ..<604_800: return "hace \(Int(seconds / 86_400)) d"
        default: return dateTime.string(from: date)
        }
    }

    static func fileSize(_ bytes: Int64) -> String {
        let safe = max(bytes, 0)
        let kilobyte: Int64 = 1_024
        let megabyte = kilobyte * 1_024
        if safe >= megabyte {
            return String(format: "%.1f MB", Double(safe) / Double(megabyte))
        }
        if safe >= kilobyte {
            return String(format: "%.1f KB", Double(safe) / Double(kilobyte))
        }
        return "\(safe) B"
    }

    static func distance(_ kilometers: Double) -> String {
        kilometers < 1
            ? "\(Int((kilometers * 1_000).rounded())) m"
            : String(format: "%.1f km", kilometers)
    }

    /// Cuenta regresiva de una tarea rápida, a partir de `segundosRestantes`.
    static func countdown(seconds: Int64) -> String {
        guard seconds > 0 else { return "Expirada" }
        let hours = seconds / 3_600
        let minutes = (seconds % 3_600) / 60
        if hours > 0 { return "\(hours) h \(minutes) min" }
        if minutes > 0 { return "\(minutes) min" }
        return "\(seconds) s"
    }
}

extension String {
    var capitalizedFirstLetter: String {
        guard let first else { return self }
        return first.uppercased() + dropFirst()
    }
}
