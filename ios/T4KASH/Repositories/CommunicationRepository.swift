import Foundation

/// Acceso al módulo `communication`: conversaciones, mensajes y notificaciones.
struct CommunicationRepository {
    private let client: APIClient

    init(client: APIClient) { self.client = client }

    /// `GET /conversations?page=&size=`
    func conversations(
        page: Int = 0,
        size: Int = AppConfig.defaultPageSize
    ) async throws -> [Conversation] {
        try await client.send(
            .get("conversations", query: [
                "page": "\(page)",
                "size": "\(min(size, AppConfig.maximumPageSize))",
            ])
        )
    }

    /// `GET /conversations/{id}/messages?page=&size=`
    func messages(
        conversationId: Int,
        page: Int = 0,
        size: Int = AppConfig.maximumPageSize
    ) async throws -> [Message] {
        try await client.send(
            .get("conversations/\(conversationId)/messages", query: [
                "page": "\(page)",
                "size": "\(min(size, AppConfig.maximumPageSize))",
            ])
        )
    }

    /// `POST /conversations/{id}/messages`
    func sendMessage(conversationId: Int, contenido: String) async throws -> Message {
        try await client.send(
            try .json(
                "conversations/\(conversationId)/messages",
                method: .post,
                body: CreateMessageRequest(contenido: contenido)
            )
        )
    }

    /// `POST /conversations/{id}/read` — sin cuerpo de respuesta.
    func markConversationRead(conversationId: Int) async throws {
        try await client.sendIgnoringResponse(
            .empty("conversations/\(conversationId)/read", method: .post)
        )
    }

    /// `GET /notifications?page=&size=`
    func notifications(
        page: Int = 0,
        size: Int = AppConfig.defaultPageSize
    ) async throws -> [AppNotification] {
        try await client.send(
            .get("notifications", query: [
                "page": "\(page)",
                "size": "\(min(size, AppConfig.maximumPageSize))",
            ])
        )
    }

    /// `POST /notifications/{id}/read`
    func markNotificationRead(id: Int) async throws -> AppNotification {
        try await client.send(.empty("notifications/\(id)/read", method: .post))
    }

    /// `POST /notifications/read-all` — sin cuerpo de respuesta.
    func markAllNotificationsRead() async throws {
        try await client.sendIgnoringResponse(
            .empty("notifications/read-all", method: .post)
        )
    }
}
