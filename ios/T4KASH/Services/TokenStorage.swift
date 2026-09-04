import Foundation

/// Custodia del token de sesión.
///
/// Vive fuera de `SessionStore` porque `APIClient` lo consulta desde cualquier
/// hilo, mientras que `SessionStore` está confinado al hilo principal.
/// El acceso se protege con un `NSLock` y el valor persiste en el Keychain.
final class TokenStorage: TokenProviding, @unchecked Sendable {
    private static let account = "session-token"

    private let lock = NSLock()
    private var cachedToken: String?
    /// Se dispara cuando el backend invalida la sesión (HTTP 401).
    private var onInvalidate: (@Sendable () -> Void)?

    init() {
        cachedToken = KeychainStore.read(account: Self.account)
    }

    var currentToken: String? {
        lock.lock()
        defer { lock.unlock() }
        return cachedToken
    }

    var hasToken: Bool { currentToken != nil }

    func store(_ token: String) {
        lock.lock()
        cachedToken = token
        lock.unlock()
        KeychainStore.save(token, account: Self.account)
    }

    func clear() {
        lock.lock()
        cachedToken = nil
        lock.unlock()
        KeychainStore.delete(account: Self.account)
    }

    func setInvalidationHandler(_ handler: @escaping @Sendable () -> Void) {
        lock.lock()
        onInvalidate = handler
        lock.unlock()
    }

    /// Llamado por `APIClient` ante un 401. Limpia el token y avisa a la interfaz.
    func invalidateSession() {
        lock.lock()
        let handler = onInvalidate
        cachedToken = nil
        lock.unlock()

        KeychainStore.delete(account: Self.account)
        handler?()
    }
}
