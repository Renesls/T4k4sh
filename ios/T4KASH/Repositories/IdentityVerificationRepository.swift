import Foundation

/// Verificación de identidad (KYC) alojada en Didit.
///
/// El backend crea la sesión y devuelve una URL web; la app la abre en Safari y
/// después consulta el estado real. No hay SDK de Didit para el cliente.
struct IdentityVerificationRepository {
    private let client: APIClient

    init(client: APIClient) { self.client = client }

    /// `GET /identity-verifications/me`
    func status() async throws -> IdentityVerificationStatus {
        try await client.send(.get("identity-verifications/me"))
    }

    /// `POST /identity-verifications/me/session?origen=PERFIL`
    func startSession(origen: String = "PERFIL") async throws -> IdentityVerificationSession {
        try await client.send(
            .empty(
                "identity-verifications/me/session",
                method: .post,
                query: ["origen": origen]
            )
        )
    }

    /// `POST /identity-verifications/me/refresh` — consulta el estado en Didit.
    func refresh() async throws -> IdentityVerificationStatus {
        try await client.send(.empty("identity-verifications/me/refresh", method: .post))
    }
}
