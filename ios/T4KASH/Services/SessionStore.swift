import Foundation
import Observation

/// Estado de sesión observable por la interfaz.
///
/// El token vive en el Keychain (`TokenStorage`); aquí solo se guarda el perfil
/// visible del usuario, que no es sensible, en `UserDefaults` para poder pintar
/// la interfaz de inmediato al abrir la app.
@MainActor
@Observable
final class SessionStore {
    private enum Keys {
        static let user = "t4kash.session.user"
        static let expiresAt = "t4kash.session.expiresAt"
    }

    /// Usuario autenticado, o `nil` si no hay sesión.
    private(set) var user: AuthenticatedUser?
    /// Fecha de expiración informada por el backend.
    private(set) var expiresAt: Date?
    /// Se activa cuando la sesión caduca sola (401), para avisar en pantalla.
    var sessionExpiredNotice: String?

    private let tokenStorage: TokenStorage
    private let defaults: UserDefaults

    init(tokenStorage: TokenStorage, defaults: UserDefaults = .standard) {
        self.tokenStorage = tokenStorage
        self.defaults = defaults
        restore()

        tokenStorage.setInvalidationHandler { [weak self] in
            Task { @MainActor [weak self] in
                self?.handleRemoteInvalidation()
            }
        }
    }

    var isAuthenticated: Bool { user != nil && tokenStorage.hasToken }

    /// `true` cuando hay token guardado pero aún no se ha validado contra
    /// `/auth/me`; el arranque decide si la sesión sigue viva.
    var hasStoredToken: Bool { tokenStorage.hasToken }

    // MARK: - Ciclo de vida

    func start(with response: AuthResponse) {
        tokenStorage.store(response.token)
        expiresAt = response.fechaExpiracion
        user = response.usuario
        sessionExpiredNotice = nil
        persist()
    }

    /// Refresca el perfil tras `/auth/me` o un cambio de nombre de usuario.
    func update(user updated: AuthenticatedUser) {
        user = updated
        persist()
    }

    /// Cierre de sesión iniciado por el usuario.
    func clear() {
        tokenStorage.clear()
        user = nil
        expiresAt = nil
        defaults.removeObject(forKey: Keys.user)
        defaults.removeObject(forKey: Keys.expiresAt)
    }

    /// Cierre de sesión provocado por el backend (401).
    private func handleRemoteInvalidation() {
        guard user != nil else { return }
        clear()
        sessionExpiredNotice = "Tu sesión expiró. Inicia sesión de nuevo."
    }

    // MARK: - Persistencia del perfil (datos no sensibles)

    private func persist() {
        guard let user, let data = try? JSONEncoder().encode(user) else { return }
        defaults.set(data, forKey: Keys.user)
        if let expiresAt {
            defaults.set(expiresAt.timeIntervalSince1970, forKey: Keys.expiresAt)
        }
    }

    private func restore() {
        // Sin token no hay sesión, aunque quede el perfil de una instalación previa.
        guard tokenStorage.hasToken else {
            defaults.removeObject(forKey: Keys.user)
            defaults.removeObject(forKey: Keys.expiresAt)
            return
        }
        if let data = defaults.data(forKey: Keys.user),
           let stored = try? JSONDecoder().decode(AuthenticatedUser.self, from: data) {
            user = stored
        }
        let timestamp = defaults.double(forKey: Keys.expiresAt)
        if timestamp > 0 { expiresAt = Date(timeIntervalSince1970: timestamp) }
    }
}
