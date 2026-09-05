import Foundation
@testable import T4KASH

/// Intercepta las peticiones de `URLSession` para poder probar `APIClient`
/// sin tocar la red.
final class MockURLProtocol: URLProtocol {
    /// Respuesta que devolverá la siguiente petición, según la ruta.
    nonisolated(unsafe) static var handler: ((URLRequest) throws -> (HTTPURLResponse, Data))?
    /// Peticiones observadas, para verificar cabeceras y cuerpos.
    nonisolated(unsafe) static var recorded: [URLRequest] = []

    static func reset() {
        handler = nil
        recorded = []
    }

    /// Configura una sesión que solo pasa por este protocolo.
    static func makeSession() -> URLSession {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [MockURLProtocol.self]
        return URLSession(configuration: configuration)
    }

    override class func canInit(with request: URLRequest) -> Bool { true }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        Self.recorded.append(request)

        guard let handler = Self.handler else {
            client?.urlProtocol(
                self,
                didFailWithError: URLError(.badServerResponse)
            )
            return
        }

        do {
            let (response, data) = try handler(request)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data)
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}
}

/// Doble del custodio de token, para no tocar el Keychain en las pruebas.
final class StubTokenProvider: TokenProviding, @unchecked Sendable {
    private let lock = NSLock()
    private var token: String?
    private(set) var invalidationCount = 0

    init(token: String? = nil) { self.token = token }

    var currentToken: String? {
        lock.lock()
        defer { lock.unlock() }
        return token
    }

    func invalidateSession() {
        lock.lock()
        token = nil
        invalidationCount += 1
        lock.unlock()
    }
}

extension HTTPURLResponse {
    /// Respuesta HTTP mínima para las pruebas.
    static func make(url: URL, status: Int) -> HTTPURLResponse {
        HTTPURLResponse(
            url: url,
            statusCode: status,
            httpVersion: "HTTP/1.1",
            headerFields: ["Content-Type": "application/json"]
        )!
    }
}

enum TestFixtures {
    static let baseURL = URL(string: "https://api.test.t4kash/api/")!

    /// Cliente apuntando al protocolo simulado.
    static func makeClient(tokenProvider: TokenProviding? = nil) -> APIClient {
        APIClient(
            baseURL: baseURL,
            tokenProvider: tokenProvider,
            session: MockURLProtocol.makeSession()
        )
    }

    /// Responde con el mismo JSON a cualquier petición.
    static func respond(status: Int = 200, json: String) {
        MockURLProtocol.handler = { request in
            (
                HTTPURLResponse.make(url: request.url ?? baseURL, status: status),
                Data(json.utf8)
            )
        }
    }
}
