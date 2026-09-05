import Foundation

// Modelos del módulo `communication`.

/// `communication/dto/ConversationResponse`
struct Conversation: Codable, Identifiable, Equatable {
    let idConversacion: Int
    let idTarea: Int?
    let idTrabajo: Int?
    let tituloTarea: String?
    let idParticipante: Int?
    let nombreParticipante: String?
    let nombreUsuarioParticipante: String?
    let estadoConversacion: String?
    let ultimoMensaje: String?
    let fechaUltimoMensaje: Date?
    let mensajesNoLeidos: Int

    var id: Int { idConversacion }

    var tituloVisible: String {
        tituloTarea?.isEmpty == false ? tituloTarea! : "Conversación"
    }

    var participanteVisible: String {
        nombreParticipante?.isEmpty == false ? nombreParticipante! : "Participante"
    }

    var arrobaParticipante: String {
        guard let nombreUsuarioParticipante, !nombreUsuarioParticipante.isEmpty else { return "" }
        return nombreUsuarioParticipante.hasPrefix("@")
            ? nombreUsuarioParticipante
            : "@\(nombreUsuarioParticipante)"
    }

    var iniciales: String {
        let letters = participanteVisible
            .split(separator: " ")
            .prefix(2)
            .compactMap(\.first)
            .map(String.init)
            .joined()
            .uppercased()
        return letters.isEmpty ? "TK" : letters
    }
}

/// `communication/dto/MessageResponse`
struct Message: Codable, Identifiable, Equatable {
    let idMensaje: Int
    let idConversacion: Int
    let idUsuarioEmisor: Int
    let nombreEmisor: String?
    let nombreUsuarioEmisor: String?
    let contenido: String
    let fechaEnvio: Date?
    let leido: Bool
    let fechaLectura: Date?
    /// Lo calcula el backend comparando con el usuario autenticado.
    let propio: Bool

    var id: Int { idMensaje }
}

/// `communication/dto/NotificationResponse`
struct AppNotification: Codable, Identifiable, Equatable {
    let idNotificacion: Int
    let titulo: String
    let mensaje: String?
    let leida: Bool
    let fechaCreacion: Date?

    var id: Int { idNotificacion }
}

// MARK: - Peticiones

struct CreateMessageRequest: Encodable {
    let contenido: String
}
