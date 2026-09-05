import Foundation
import Observation

/// Listado de conversaciones.
@MainActor
@Observable
final class ConversationsViewModel {
    private let repository: CommunicationRepository

    init(repository: CommunicationRepository) { self.repository = repository }

    private(set) var state: LoadState<[Conversation]> = .idle

    var totalUnread: Int {
        (state.value ?? []).reduce(0) { $0 + $1.mensajesNoLeidos }
    }

    func load() async {
        if state.value == nil { state = .loading }
        do {
            let conversations = try await repository.conversations()
            state = .loaded(
                conversations.sorted {
                    ($0.fechaUltimoMensaje ?? .distantPast) > ($1.fechaUltimoMensaje ?? .distantPast)
                }
            )
        } catch {
            state = .failed(ErrorPresenter.message(for: error))
        }
    }

    /// Refresco silencioso: mantiene el contenido si falla, para no parpadear.
    func refreshQuietly() async {
        guard let conversations = try? await repository.conversations() else { return }
        state = .loaded(
            conversations.sorted {
                ($0.fechaUltimoMensaje ?? .distantPast) > ($1.fechaUltimoMensaje ?? .distantPast)
            }
        )
    }
}

/// Mensajes de una conversación.
///
/// El backend no ofrece tiempo real (ni websockets ni SSE), así que se replica
/// el sondeo de Android. El bucle vive atado a la vista con `.task`, de modo que
/// se cancela solo al salir de la pantalla.
@MainActor
@Observable
final class ConversationViewModel {
    private let repository: CommunicationRepository
    let conversation: Conversation

    init(conversation: Conversation, repository: CommunicationRepository) {
        self.conversation = conversation
        self.repository = repository
    }

    private(set) var state: LoadState<[Message]> = .idle
    private(set) var isSending = false
    var draft = ""
    var errorMessage: String?

    var messages: [Message] { state.value ?? [] }

    var canSend: Bool {
        let trimmed = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        return !trimmed.isEmpty && trimmed.count <= 2_000 && !isSending
    }

    func load() async {
        if state.value == nil { state = .loading }
        do {
            let loaded = try await repository.messages(
                conversationId: conversation.idConversacion
            )
            state = .loaded(sorted(loaded))
            await markRead()
        } catch {
            state = .failed(ErrorPresenter.message(for: error))
        }
    }

    /// Bucle de sondeo mientras la conversación esté visible.
    func startPolling() async {
        while !Task.isCancelled {
            try? await Task.sleep(for: .seconds(AppConfig.chatPollingInterval))
            guard !Task.isCancelled else { return }

            guard let loaded = try? await repository.messages(
                conversationId: conversation.idConversacion
            ) else { continue }

            let ordered = sorted(loaded)
            // Solo se reemplaza si hay cambios reales, para no reiniciar el scroll.
            if ordered != messages {
                state = .loaded(ordered)
                await markRead()
            }
        }
    }

    func send() async {
        let contenido = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !contenido.isEmpty, !isSending else { return }

        isSending = true
        defer { isSending = false }

        do {
            let sent = try await repository.sendMessage(
                conversationId: conversation.idConversacion,
                contenido: contenido
            )
            draft = ""
            state = .loaded(sorted(messages + [sent]))
        } catch {
            errorMessage = ErrorPresenter.message(for: error)
        }
    }

    private func markRead() async {
        try? await repository.markConversationRead(
            conversationId: conversation.idConversacion
        )
    }

    private func sorted(_ messages: [Message]) -> [Message] {
        messages.sorted { ($0.fechaEnvio ?? .distantPast) < ($1.fechaEnvio ?? .distantPast) }
    }

    /// `true` cuando el mensaje abre un día distinto al anterior, para pintar
    /// el separador de fecha del chat.
    func startsNewDay(at index: Int) -> Bool {
        guard index > 0 else { return true }
        guard let current = messages[index].fechaEnvio,
              let previous = messages[index - 1].fechaEnvio
        else { return false }
        return !Calendar.current.isDate(current, inSameDayAs: previous)
    }
}
