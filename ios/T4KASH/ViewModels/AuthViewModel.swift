import Foundation
import Observation

/// Estado y acciones del flujo de autenticación.
///
/// Cubre el login de dos pasos real del backend, el registro con detección de
/// universidad y la recuperación de contraseña.
@MainActor
@Observable
final class AuthViewModel {
    private let repository: AuthRepository
    private let session: SessionStore

    init(repository: AuthRepository, session: SessionStore) {
        self.repository = repository
        self.session = session
    }

    // MARK: - Estado compartido

    var errorMessage: String?
    var infoMessage: String?
    var isBusy = false

    /// Correo que arrastra el flujo entre pantallas (login → código, registro → activación).
    var pendingEmail = ""
    /// Momento en que expira el código vigente, informado por el backend.
    var challengeExpiresAt: Date?

    // MARK: - Catálogos institucionales

    var universities: [University] = []
    var careers: [Career] = []
    var detectedUniversity: University?
    var isLoadingCareers = false

    /// Carga las universidades una sola vez. Es un endpoint público.
    func loadUniversities() async {
        guard universities.isEmpty else { return }
        do {
            universities = try await repository.universities()
        } catch {
            // No bloquea el registro: el backend acepta `idUniversidad` nulo.
            errorMessage = ErrorPresenter.message(for: error)
        }
    }

    /// Detecta la universidad por dominio y carga sus carreras.
    /// Solo se muestran carreras cuando hay coincidencia institucional activa,
    /// igual que en Android.
    func updateDetectedUniversity(for email: String) async {
        let match = EmailDomainMatcher.detectUniversity(
            email: email,
            universities: universities
        )
        guard match?.idUniversidad != detectedUniversity?.idUniversidad else { return }

        detectedUniversity = match
        careers = []

        guard let match else { return }

        isLoadingCareers = true
        defer { isLoadingCareers = false }
        do {
            careers = try await repository.careers(universityId: match.idUniversidad)
        } catch {
            errorMessage = ErrorPresenter.message(for: error)
        }
    }

    // MARK: - Login en dos pasos

    /// Primer paso. Devuelve `true` si el backend envió el código.
    func login(correo: String, password: String) async -> Bool {
        await run {
            let challenge = try await repository.login(correo: correo, password: password)
            pendingEmail = challenge.correo
            challengeExpiresAt = challenge.fechaExpiracion
            infoMessage = challenge.mensaje
            return true
        }
    }

    /// Segundo paso: canjea el código por la sesión.
    func verifyLogin(codigo: String) async -> Bool {
        await run {
            let response = try await repository.verifyLogin(
                correo: pendingEmail,
                codigo: codigo
            )
            session.start(with: response)
            return true
        }
    }

    func resendLoginCode() async {
        _ = await run {
            let challenge = try await repository.resendLoginCode(correo: pendingEmail)
            challengeExpiresAt = challenge.fechaExpiracion
            infoMessage = challenge.mensaje
            return true
        }
    }

    // MARK: - Registro

    func register(
        nombre: String,
        apellido: String,
        correo: String,
        password: String,
        idCarrera: Int?,
        carnet: String?
    ) async -> Bool {
        await run {
            let challenge = try await repository.register(
                RegisterRequest(
                    nombre: nombre,
                    apellido: apellido,
                    correo: correo,
                    password: password,
                    idUniversidad: detectedUniversity?.idUniversidad,
                    idCarrera: idCarrera,
                    carnetUniversitario: carnet?.isEmpty == true ? nil : carnet
                )
            )
            pendingEmail = challenge.correo
            challengeExpiresAt = challenge.fechaExpiracion
            infoMessage = challenge.mensaje
            return true
        }
    }

    /// Activa la cuenta. El backend devuelve sesión directamente.
    func verifyEmail(codigo: String) async -> Bool {
        await run {
            let response = try await repository.verifyEmail(
                correo: pendingEmail,
                codigo: codigo
            )
            session.start(with: response)
            return true
        }
    }

    func resendVerification() async {
        _ = await run {
            let challenge = try await repository.resendVerification(correo: pendingEmail)
            challengeExpiresAt = challenge.fechaExpiracion
            infoMessage = challenge.mensaje
            return true
        }
    }

    // MARK: - Recuperación de contraseña

    func forgotPassword(correo: String) async -> Bool {
        await run {
            let response = try await repository.forgotPassword(correo: correo)
            pendingEmail = correo
            infoMessage = response.mensaje
            return true
        }
    }

    func resetPassword(codigo: String, nuevaPassword: String) async -> Bool {
        await run {
            let response = try await repository.resetPassword(
                correo: pendingEmail,
                codigo: codigo,
                nuevaPassword: nuevaPassword
            )
            infoMessage = response.mensaje
            return true
        }
    }

    // MARK: - Utilidades

    func clearMessages() {
        errorMessage = nil
        infoMessage = nil
    }

    /// Ejecuta una operación controlando `isBusy` y traduciendo el error.
    private func run(_ operation: () async throws -> Bool) async -> Bool {
        guard !isBusy else { return false }
        isBusy = true
        errorMessage = nil
        defer { isBusy = false }

        do {
            return try await operation()
        } catch {
            errorMessage = ErrorPresenter.message(for: error)
            return false
        }
    }
}
