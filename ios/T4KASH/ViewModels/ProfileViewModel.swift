import Foundation
import Observation

/// Perfil propio: identidad pública, verificación estudiantil y KYC.
@MainActor
@Observable
final class ProfileViewModel {
    private let auth: AuthRepository
    private let identityVerification: IdentityVerificationRepository
    private let session: SessionStore

    init(
        auth: AuthRepository,
        identityVerification: IdentityVerificationRepository,
        session: SessionStore
    ) {
        self.auth = auth
        self.identityVerification = identityVerification
        self.session = session
    }

    private(set) var profile: PublicProfile?
    private(set) var studentVerification: StudentVerification?
    private(set) var identityStatus: IdentityVerificationStatus?
    private(set) var isLoading = false

    var actionError: String?
    var successMessage: String?

    var user: AuthenticatedUser? { session.user }

    func load() async {
        isLoading = profile == nil
        defer { isLoading = false }

        // Refresca el perfil autenticado por si cambiaron roles o estado.
        if let refreshed = try? await auth.currentUser() {
            session.update(user: refreshed)
        }

        if let username = session.user?.nombreUsuario, !username.isEmpty {
            profile = try? await auth.publicProfile(username: username)
        }
        // Un 404 aquí es normal: significa que aún no se solicitó verificación.
        studentVerification = try? await auth.studentVerification()
        identityStatus = try? await identityVerification.status()
    }

    func updateUsername(_ nuevo: String) async -> Bool {
        do {
            profile = try await auth.updateUsername(nuevo)
            // El nombre de usuario vive también en la sesión: hay que refrescarla.
            if let refreshed = try? await auth.currentUser() {
                session.update(user: refreshed)
            }
            successMessage = "Tu nombre de usuario se actualizó."
            return true
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return false
        }
    }

    func uploadStudentProof(_ prepared: PreparedAttachment) async {
        do {
            _ = try await auth.uploadStudentProof(prepared)
            studentVerification = try? await auth.studentVerification()
            successMessage = "Comprobante enviado. Un administrador lo revisará."
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }

    /// Crea la sesión de KYC en Didit y devuelve la URL web a abrir.
    func startIdentityVerification() async -> WebDestination? {
        do {
            let session = try await identityVerification.startSession()
            guard let destination = WebDestination(session.urlVerificacion) else {
                actionError = "El proveedor de verificación devolvió una dirección inválida."
                return nil
            }
            return destination
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return nil
        }
    }

    /// Consulta el estado real en el proveedor tras volver del navegador.
    func refreshIdentityStatus() async {
        do {
            identityStatus = try await identityVerification.refresh()
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }

    func logout() async {
        // Aunque el servidor falle, la sesión local debe quedar limpia.
        try? await auth.logout()
        session.clear()
    }
}
