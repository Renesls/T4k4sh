import Foundation
import Observation

/// Raíz de composición de la aplicación.
///
/// Se crea una sola vez en `T4KASHApp` y se inyecta en el entorno de SwiftUI.
/// Evita singletons dispersos y permite sustituir el cliente HTTP en pruebas.
@MainActor
@Observable
final class AppDependencies {
    let tokenStorage: TokenStorage
    let client: APIClient
    let session: SessionStore
    let location: LocationService

    let auth: AuthRepository
    let marketplace: MarketplaceRepository
    let attachments: AttachmentRepository
    let finance: FinanceRepository
    let communication: CommunicationRepository
    let feed: FeedRepository
    let moderation: ModerationRepository
    let identityVerification: IdentityVerificationRepository

    init(client: APIClient? = nil, tokenStorage: TokenStorage = TokenStorage()) {
        self.tokenStorage = tokenStorage
        let resolvedClient = client ?? APIClient(tokenProvider: tokenStorage)
        self.client = resolvedClient

        session = SessionStore(tokenStorage: tokenStorage)
        location = LocationService()

        auth = AuthRepository(client: resolvedClient)
        marketplace = MarketplaceRepository(client: resolvedClient)
        attachments = AttachmentRepository(client: resolvedClient)
        finance = FinanceRepository(client: resolvedClient)
        communication = CommunicationRepository(client: resolvedClient)
        feed = FeedRepository(client: resolvedClient)
        moderation = ModerationRepository(client: resolvedClient)
        identityVerification = IdentityVerificationRepository(client: resolvedClient)
    }
}
