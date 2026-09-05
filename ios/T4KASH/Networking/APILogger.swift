import Foundation
import OSLog

/// Registro de red.
///
/// Solo se activa en compilaciones Debug y **nunca** imprime cabeceras de
/// autorización, tokens, contraseñas ni cuerpos de peticiones de autenticación.
enum APILogger {
    private static let logger = Logger(subsystem: "com.t4kash.app", category: "network")

    /// Rutas cuyo cuerpo jamás debe registrarse.
    private static let sensitivePaths = [
        "auth/login", "auth/register", "auth/password", "auth/verify-email",
        "auth/resend-verification", "identity-verifications",
    ]

    static func request(_ request: URLRequest) {
        #if DEBUG
        let method = request.httpMethod ?? "?"
        let path = request.url?.path ?? "?"
        logger.debug("→ \(method, privacy: .public) \(path, privacy: .public)")
        #endif
    }

    static func response(_ response: HTTPURLResponse, path: String, body: Data) {
        #if DEBUG
        logger.debug("← \(response.statusCode, privacy: .public) \(path, privacy: .public)")
        guard response.statusCode >= 400,
              !sensitivePaths.contains(where: { path.contains($0) }),
              let text = String(data: body, encoding: .utf8),
              !text.isEmpty
        else { return }
        logger.debug("   detalle: \(text.prefix(500), privacy: .public)")
        #endif
    }

    static func failure(_ error: Error, path: String) {
        #if DEBUG
        logger.error("✗ \(path, privacy: .public): \(error.localizedDescription, privacy: .public)")
        #endif
    }
}
