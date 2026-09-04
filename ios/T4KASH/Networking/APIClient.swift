import Foundation

/// Proveedor del token de sesión para el cliente HTTP.
///
/// Se declara como protocolo para poder inyectar un doble en las pruebas sin
/// tocar el Keychain real.
protocol TokenProviding: AnyObject, Sendable {
    var currentToken: String? { get }
    /// Se invoca cuando el backend responde 401 con un token presente.
    func invalidateSession()
}

/// Cliente HTTP único de la aplicación.
///
/// Toda llamada a la API pasa por aquí: no hay `URLSession` suelto en ninguna
/// vista ni repositorio. Centraliza autenticación, decodificación, mapeo de
/// errores, subidas multipart y descargas.
final class APIClient: @unchecked Sendable {
    private let baseURL: URL
    private let session: URLSession
    private let tokenProvider: TokenProviding?

    init(
        baseURL: URL = AppConfig.apiBaseURL,
        tokenProvider: TokenProviding?,
        session: URLSession? = nil
    ) {
        self.baseURL = baseURL
        self.tokenProvider = tokenProvider

        if let session {
            self.session = session
        } else {
            let configuration = URLSessionConfiguration.default
            configuration.timeoutIntervalForRequest = AppConfig.requestTimeout
            configuration.timeoutIntervalForResource = AppConfig.resourceTimeout
            configuration.waitsForConnectivity = false
            configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
            self.session = URLSession(configuration: configuration)
        }
    }

    // MARK: - Envío

    /// Ejecuta la petición y decodifica la respuesta.
    @discardableResult
    func send<T: Decodable>(_ request: APIRequest, as type: T.Type = T.self) async throws -> T {
        let data = try await perform(request)
        do {
            return try JSONCoding.decoder.decode(T.self, from: data)
        } catch let error as DecodingError {
            APILogger.failure(error, path: request.path)
            throw APIError.decoding(Self.describe(error))
        } catch {
            throw APIError.decoding(error.localizedDescription)
        }
    }

    /// Ejecuta la petición descartando el cuerpo de la respuesta.
    ///
    /// Tiene nombre propio, y no una sobrecarga de `send`, para que la elección
    /// entre devolver un modelo o nada sea explícita en cada llamada.
    func sendIgnoringResponse(_ request: APIRequest) async throws {
        _ = try await perform(request)
    }

    /// Sube un archivo como `multipart/form-data`, validando antes el límite del backend.
    func upload<T: Decodable>(
        path: String,
        filename: String,
        mimeType: String,
        fileData: Data,
        as type: T.Type = T.self
    ) async throws -> T {
        guard fileData.count <= AppConfig.maxAttachmentBytes else {
            throw APIError.attachmentTooLarge(limitBytes: AppConfig.maxAttachmentBytes)
        }
        let request = APIRequest.upload(
            path,
            filename: filename,
            mimeType: mimeType,
            fileData: fileData
        )
        return try await send(request, as: T.self)
    }

    /// Descarga los bytes crudos de un adjunto
    /// (`GET /attachments/{id}/download` devuelve el archivo, no JSON).
    func download(path: String) async throws -> Data {
        try await perform(APIRequest.get(path))
    }

    // MARK: - Núcleo

    private func perform(_ request: APIRequest) async throws -> Data {
        let urlRequest = try request.urlRequest(
            baseURL: baseURL,
            token: tokenProvider?.currentToken
        )
        APILogger.request(urlRequest)

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: urlRequest)
        } catch {
            APILogger.failure(error, path: request.path)
            throw APIError.fromTransport(error)
        }

        guard let http = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        APILogger.response(http, path: request.path, body: data)

        guard (200..<300).contains(http.statusCode) else {
            let apiError = APIError.fromResponse(status: http.statusCode, body: data)
            // Mismo comportamiento que el interceptor de Android: un 401 con token
            // presente significa que la sesión dejó de ser válida.
            if case .unauthorized = apiError, tokenProvider?.currentToken != nil {
                tokenProvider?.invalidateSession()
            }
            throw apiError
        }
        return data
    }

    private static func describe(_ error: DecodingError) -> String {
        switch error {
        case let .keyNotFound(key, _):
            "Falta el campo «\(key.stringValue)»."
        case let .typeMismatch(type, context):
            "El campo «\(context.codingPath.map(\.stringValue).joined(separator: "."))» no es \(type)."
        case let .valueNotFound(_, context):
            "Valor nulo inesperado en «\(context.codingPath.map(\.stringValue).joined(separator: "."))»."
        case let .dataCorrupted(context):
            context.debugDescription
        @unknown default:
            "Formato inesperado."
        }
    }
}
