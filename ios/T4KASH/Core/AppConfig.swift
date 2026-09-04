import Foundation

/// Configuración de entorno de la aplicación.
///
/// La URL base se resuelve desde `Info.plist` (`T4KASHAPIBaseURL`), que a su vez
/// toma el valor del build setting `T4KASH_API_BASE_URL`. Esto permite apuntar a
/// Render, a un backend local o a un entorno de pruebas sin tocar el código.
///
/// Aquí no se guarda ningún secreto: el backend concentra las claves de Supabase,
/// Brevo, Didit y Pagadito. El cliente solo conoce una URL pública.
enum AppConfig {
    /// URL base de la API, siempre terminada en `/`.
    static let apiBaseURL: URL = {
        let raw = (Bundle.main.object(forInfoDictionaryKey: "T4KASHAPIBaseURL") as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""

        let normalized = raw.hasSuffix("/") ? raw : raw + "/"

        guard !raw.isEmpty, let url = URL(string: normalized) else {
            // Mismo valor por defecto que usa Android en `mobile/app/build.gradle.kts`.
            return URL(string: "https://t4k4sh.onrender.com/api/")!
        }
        return url
    }()

    /// Tiempos de espera alineados con el cliente Android (OkHttp).
    /// El plan gratuito de Render puede tardar en despertar, de ahí el margen.
    static let requestTimeout: TimeInterval = 45
    static let resourceTimeout: TimeInterval = 90

    /// Límite de subida aceptado por el backend
    /// (`spring.servlet.multipart.max-file-size=10MB`).
    static let maxAttachmentBytes: Int = 10 * 1024 * 1024

    /// Intervalo de refresco del chat. El backend no expone websockets ni SSE,
    /// así que se replica el sondeo que ya hace Android.
    static let chatPollingInterval: TimeInterval = 5

    /// Tamaño de página por defecto. El backend limita `size` a 100
    /// (`config/PaginationSupport.MAXIMUM_SIZE`).
    static let defaultPageSize = 50
    static let maximumPageSize = 100
}
