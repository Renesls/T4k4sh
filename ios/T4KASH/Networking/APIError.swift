import Foundation

/// Cuerpo de error RFC 7807 que devuelve `GlobalExceptionHandler` del backend.
struct ProblemDetail: Decodable {
    let type: String?
    let title: String?
    let status: Int?
    let detail: String?
    let instance: String?
}

/// Errores del cliente HTTP, con mensajes listos para mostrar al usuario.
enum APIError: LocalizedError, Equatable {
    /// La URL construida no es válida (error de programación).
    case invalidURL
    /// No hubo respuesta HTTP utilizable.
    case invalidResponse
    /// Sin conexión o red inalcanzable.
    case offline
    /// La petición superó el tiempo de espera.
    case timeout
    /// La sesión expiró o el token dejó de ser válido (HTTP 401).
    case unauthorized
    /// El usuario no tiene permiso para la operación (HTTP 403).
    case forbidden(String)
    /// El recurso no existe (HTTP 404).
    case notFound(String)
    /// Conflicto de negocio (HTTP 409), por ejemplo postular dos veces.
    case conflict(String)
    /// Validación rechazada por el backend (HTTP 400/422).
    case validation(String)
    /// Demasiados intentos (HTTP 429).
    case tooManyAttempts(String)
    /// Cualquier otro error del servidor con su detalle.
    case server(status: Int, message: String)
    /// La respuesta no coincide con el modelo esperado.
    case decoding(String)
    /// El archivo supera el límite del backend.
    case attachmentTooLarge(limitBytes: Int)
    /// Error no clasificado.
    case unknown(String)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            "No se pudo construir la dirección de la solicitud."
        case .invalidResponse:
            "El servidor devolvió una respuesta que no se pudo interpretar."
        case .offline:
            "Sin conexión a internet. Revisa tu red e inténtalo de nuevo."
        case .timeout:
            "El servidor tardó demasiado en responder. Inténtalo de nuevo."
        case .unauthorized:
            "Tu sesión expiró. Inicia sesión de nuevo."
        case let .forbidden(message):
            message.isEmpty ? "No tienes permiso para realizar esta acción." : message
        case let .notFound(message):
            message.isEmpty ? "No encontramos lo que buscabas." : message
        case let .conflict(message):
            message.isEmpty ? "La operación entra en conflicto con el estado actual." : message
        case let .validation(message):
            message.isEmpty ? "Revisa los datos ingresados." : message
        case let .tooManyAttempts(message):
            message.isEmpty ? "Demasiados intentos. Espera unos minutos." : message
        case let .server(status, message):
            message.isEmpty ? "Error del servidor (\(status))." : message
        case let .decoding(message):
            "No pudimos leer la respuesta del servidor. \(message)"
        case let .attachmentTooLarge(limit):
            "El archivo supera el límite de \(DisplayFormatter.fileSize(Int64(limit)))."
        case let .unknown(message):
            message.isEmpty ? "Ocurrió un error inesperado." : message
        }
    }

    /// `true` cuando reintentar tiene sentido sin cambiar nada de la petición.
    var isRetryable: Bool {
        switch self {
        case .offline, .timeout, .server: true
        default: false
        }
    }

    /// Construye el error a partir del código HTTP y el `ProblemDetail` del backend.
    static func fromResponse(status: Int, body: Data) -> APIError {
        let detail = Self.detailMessage(from: body)
        switch status {
        case 400, 422: return .validation(detail)
        case 401: return .unauthorized
        case 403: return .forbidden(detail)
        case 404: return .notFound(detail)
        case 409: return .conflict(detail)
        case 429: return .tooManyAttempts(detail)
        default: return .server(status: status, message: detail)
        }
    }

    /// Extrae el mensaje legible. El backend siempre lo pone en `detail`.
    private static func detailMessage(from body: Data) -> String {
        guard !body.isEmpty else { return "" }
        if let problem = try? JSONDecoder().decode(ProblemDetail.self, from: body) {
            if let detail = problem.detail, !detail.isEmpty { return detail }
            if let title = problem.title, !title.isEmpty { return title }
        }
        // Algunos endpoints antiguos devuelven `{"mensaje": "..."}`.
        if let object = try? JSONSerialization.jsonObject(with: body) as? [String: Any],
           let message = object["mensaje"] as? String, !message.isEmpty {
            return message
        }
        return ""
    }

    /// Traduce errores de `URLSession` a casos propios.
    static func fromTransport(_ error: Error) -> APIError {
        let nsError = error as NSError
        guard nsError.domain == NSURLErrorDomain else {
            return .unknown(error.localizedDescription)
        }
        switch nsError.code {
        case NSURLErrorNotConnectedToInternet,
             NSURLErrorNetworkConnectionLost,
             NSURLErrorDataNotAllowed,
             NSURLErrorCannotConnectToHost,
             NSURLErrorCannotFindHost:
            return .offline
        case NSURLErrorTimedOut:
            return .timeout
        case NSURLErrorCancelled:
            return .unknown("Solicitud cancelada.")
        default:
            return .unknown(error.localizedDescription)
        }
    }
}
