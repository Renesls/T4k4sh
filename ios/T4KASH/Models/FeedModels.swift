import Foundation

// Modelos del módulo `network` (red universitaria / feed).

/// `network/dto/PostResponse`
struct Post: Codable, Identifiable, Equatable {
    let idPublicacion: Int
    let autor: PublicIdentity
    let idPublicacionOrigen: Int?
    let contenido: String?
    let tipoPublicacion: String
    let visibilidad: String
    let permiteComentarios: Bool
    let fechaPublicacion: Date?
    let fechaEdicion: Date?
    let estadoPublicacion: String
    /// Conteo por tipo de reacción, tal como lo agrega el backend.
    let reacciones: [String: Int]
    let totalReacciones: Int
    let totalComentarios: Int
    let totalCompartidas: Int
    let miReaccion: String?
    let guardada: Bool
    let propia: Bool

    var id: Int { idPublicacion }

    var tipo: Domain.TipoPublicacion? { Domain.TipoPublicacion(rawValue: tipoPublicacion) }
    var visibilidadResuelta: Domain.VisibilidadPublicacion? {
        Domain.VisibilidadPublicacion(rawValue: visibilidad)
    }
    var reaccionPropia: Domain.Reaccion? { miReaccion.flatMap(Domain.Reaccion.init(rawValue:)) }
    var editada: Bool { fechaEdicion != nil }
    var esCompartida: Bool { idPublicacionOrigen != nil }

    /// Reacciones presentes ordenadas por cantidad, para la fila de resumen.
    var reaccionesOrdenadas: [(reaccion: Domain.Reaccion, total: Int)] {
        reacciones
            .compactMap { key, value in
                guard let reaccion = Domain.Reaccion(rawValue: key), value > 0 else { return nil }
                return (reaccion, value)
            }
            .sorted { $0.1 > $1.1 }
    }
}

/// `network/dto/CommentResponse`
struct PostComment: Codable, Identifiable, Equatable {
    let idComentario: Int
    let idPublicacion: Int
    let idComentarioPadre: Int?
    let autor: PublicIdentity
    let contenido: String
    let fechaComentario: Date?
    let fechaEdicion: Date?
    let propio: Bool

    var id: Int { idComentario }
    var esRespuesta: Bool { idComentarioPadre != nil }
}

// MARK: - Peticiones

struct CreatePostRequest: Encodable {
    let contenido: String?
    let tipoPublicacion: String
    let visibilidad: String
    let permiteComentarios: Bool
    let idPublicacionOrigen: Int?
}

struct UpdatePostRequest: Encodable {
    let contenido: String?
    let tipoPublicacion: String
    let visibilidad: String
    let permiteComentarios: Bool
}

struct ReactionRequest: Encodable {
    let tipoReaccion: String
}

struct CreateCommentRequest: Encodable {
    let contenido: String
    let idComentarioPadre: Int?
}

struct UpdateCommentRequest: Encodable {
    let contenido: String
}
