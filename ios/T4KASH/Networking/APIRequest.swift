import Foundation

/// Descripción de una petición a la API de T4KASH.
struct APIRequest {
    /// Ruta relativa a la URL base, sin `/` inicial (`"tasks"`, `"auth/login"`).
    var path: String
    var method: HTTPMethod = .get
    /// Parámetros de consulta. Los valores `nil` se descartan.
    var query: [String: String?] = [:]
    var body: Data?
    var contentType: String?
    /// Si es `true`, se adjunta `Authorization: Bearer <token>`.
    var requiresAuth: Bool = true

    static func get(
        _ path: String,
        query: [String: String?] = [:],
        requiresAuth: Bool = true
    ) -> APIRequest {
        APIRequest(path: path, method: .get, query: query, requiresAuth: requiresAuth)
    }

    /// Petición sin cuerpo (varios `POST`/`DELETE` del backend no lo llevan).
    static func empty(
        _ path: String,
        method: HTTPMethod,
        query: [String: String?] = [:],
        requiresAuth: Bool = true
    ) -> APIRequest {
        APIRequest(path: path, method: method, query: query, requiresAuth: requiresAuth)
    }

    /// Petición con cuerpo JSON codificado con `JSONCoding.encoder`.
    static func json(
        _ path: String,
        method: HTTPMethod,
        body: some Encodable,
        query: [String: String?] = [:],
        requiresAuth: Bool = true
    ) throws -> APIRequest {
        APIRequest(
            path: path,
            method: method,
            query: query,
            body: try JSONCoding.encoder.encode(body),
            contentType: "application/json",
            requiresAuth: requiresAuth
        )
    }

    /// Petición `multipart/form-data` con un único archivo en la parte `file`.
    static func upload(
        _ path: String,
        filename: String,
        mimeType: String,
        fileData: Data
    ) -> APIRequest {
        var form = MultipartFormData()
        form.addFile(name: "file", filename: filename, mimeType: mimeType, data: fileData)
        return APIRequest(
            path: path,
            method: .post,
            body: form.finalized(),
            contentType: form.contentType,
            requiresAuth: true
        )
    }

    /// Construye la `URLRequest` final resolviendo la ruta contra la URL base.
    func urlRequest(baseURL: URL, token: String?) throws -> URLRequest {
        guard var components = URLComponents(
            url: baseURL.appendingPathComponent(path),
            resolvingAgainstBaseURL: false
        ) else {
            throw APIError.invalidURL
        }

        let items = query
            .compactMapValues { $0 }
            .map { URLQueryItem(name: $0.key, value: $0.value) }
            .sorted { $0.name < $1.name }
        if !items.isEmpty { components.queryItems = items }

        guard let url = components.url else { throw APIError.invalidURL }

        var request = URLRequest(url: url)
        request.httpMethod = method.rawValue
        request.httpBody = body
        request.timeoutInterval = AppConfig.requestTimeout
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let contentType {
            request.setValue(contentType, forHTTPHeaderField: "Content-Type")
        }
        if requiresAuth, let token, !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        return request
    }
}
