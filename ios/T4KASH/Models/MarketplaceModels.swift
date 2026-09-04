import Foundation

// Modelos del módulo `marketplace`.

/// `marketplace/dto/CategoriaResponse`
struct Category: Codable, Identifiable, Equatable, Hashable {
    let idCategoria: Int
    let nombreCategoria: String
    let descripcion: String?
    let estado: Bool?

    var id: Int { idCategoria }
}

/// `marketplace/dto/TaskResponse`
struct TaskItem: Codable, Identifiable, Equatable {
    let idTarea: Int
    let titulo: String
    let descripcion: String
    let presupuesto: Decimal
    let fechaPublicacion: Date?
    let fechaLimitePostulacion: Date?
    let fechaLimite: Date?
    let estadoTarea: String
    let idCategoria: Int
    let idCliente: Int
    let tipoOportunidad: String
    let modalidad: String?
    let visibilidad: String?
    let direccionReferencia: String?
    let latitud: Decimal?
    let longitud: Decimal?
    let cliente: PublicIdentity?

    var id: Int { idTarea }

    var esTareaRapida: Bool { tipoOportunidad == Domain.TipoOportunidad.rapida.rawValue }
    var estaPublicada: Bool { estadoTarea == Domain.EstadoTarea.publicada }
    var estadoLegible: String { Domain.EstadoTarea.label(estadoTarea) }

    var modalidadResuelta: Domain.Modalidad? {
        modalidad.flatMap(Domain.Modalidad.init(rawValue:))
    }

    /// Coordenadas solo cuando el backend envía ambas.
    var coordenadas: (latitude: Double, longitude: Double)? {
        guard let latitud, let longitud else { return nil }
        return (NSDecimalNumber(decimal: latitud).doubleValue,
                NSDecimalNumber(decimal: longitud).doubleValue)
    }
}

/// `marketplace/dto/QuickTaskResponse`
struct QuickTask: Codable, Identifiable, Equatable {
    let tarea: TaskItem
    let distanciaKm: Double
    let segundosRestantes: Int64

    var id: Int { tarea.idTarea }
    var expirada: Bool { segundosRestantes <= 0 }
}

/// `marketplace/dto/ApplicationResponse`
struct Application: Codable, Identifiable, Equatable {
    let idPostulacion: Int
    let idTarea: Int
    let idEstudiante: Int
    let mensaje: String?
    let precioPropuesto: Decimal?
    let fechaPostulacion: Date?
    let estadoPostulacion: String
    let numeroIntento: Int?
    let estudiante: PublicIdentity?

    var id: Int { idPostulacion }
    var estaPendiente: Bool { estadoPostulacion == Domain.EstadoPostulacion.pendiente }
    var estadoLegible: String { Domain.EstadoPostulacion.label(estadoPostulacion) }
}

/// `marketplace/dto/JobResponse`
struct Job: Codable, Identifiable, Equatable {
    let idTrabajo: Int
    let idTarea: Int
    let idEstudiante: Int
    let fechaInicio: Date?
    let fechaEntregaEsperada: Date?
    let estadoTrabajo: String
    let estudiante: PublicIdentity?
    let pago: Payment?

    var id: Int { idTrabajo }
    var estadoLegible: String { Domain.EstadoTrabajo.label(estadoTrabajo) }
    var finalizado: Bool { estadoTrabajo == Domain.EstadoTrabajo.finalizado }
}

/// `marketplace/dto/DeliveryCommentResponse`
struct DeliveryComment: Codable, Identifiable, Equatable {
    let idComentarioEntrega: Int
    let idEntrega: Int
    let idUsuario: Int
    let comentario: String
    let tipoComentario: String?
    let fechaComentario: Date?

    var id: Int { idComentarioEntrega }
}

/// `marketplace/dto/DeliveryReviewResponse`
struct DeliveryReview: Codable, Identifiable, Equatable {
    let idRevisionEntrega: Int
    let idEntrega: Int
    let idUsuarioRevisa: Int
    let resultadoRevision: String
    let observacion: String?
    let fechaRevision: Date?
    let estadoRevision: String?

    var id: Int { idRevisionEntrega }
}

/// `marketplace/dto/DeliveryResponse`
struct Delivery: Codable, Identifiable, Equatable {
    let idEntrega: Int
    let idTrabajo: Int
    let descripcionEntrega: String
    let fechaEntrega: Date?
    let estadoEntrega: String
    let comentarios: [DeliveryComment]
    let revisiones: [DeliveryReview]

    var id: Int { idEntrega }
    var estadoLegible: String { Domain.EstadoEntrega.label(estadoEntrega) }
    var aprobada: Bool { estadoEntrega == Domain.EstadoEntrega.aprobada }
    var requiereCambios: Bool { estadoEntrega == Domain.EstadoEntrega.cambiosSolicitados }
}

/// `marketplace/dto/AttachmentResponse`
struct Attachment: Codable, Identifiable, Equatable {
    let idArchivo: Int
    let idTarea: Int?
    let idEntrega: Int?
    let idVerificacion: Int?
    let idUsuarioSube: Int
    let nombreOriginal: String
    let tipoMime: String?
    let extension_: String?
    let tamanoBytes: Int64
    let fechaSubida: Date?
    let estadoArchivo: String?
    /// Ruta relativa que devuelve el backend: `attachments/{id}/download`.
    let rutaDescarga: String

    var id: Int { idArchivo }

    /// `extension` es palabra reservada en Swift, de ahí el mapeo explícito.
    enum CodingKeys: String, CodingKey {
        case idArchivo, idTarea, idEntrega, idVerificacion, idUsuarioSube
        case nombreOriginal, tipoMime, tamanoBytes, fechaSubida, estadoArchivo, rutaDescarga
        case extension_ = "extension"
    }

    var esImagen: Bool { (tipoMime ?? "").hasPrefix("image/") }

    var iconoSistema: String {
        guard let tipoMime else { return "doc" }
        if tipoMime.hasPrefix("image/") { return "photo" }
        if tipoMime.contains("pdf") { return "doc.richtext" }
        return "doc"
    }
}

// MARK: - Peticiones

struct CreateTaskRequest: Encodable {
    let titulo: String
    let descripcion: String
    let presupuesto: Decimal
    let fechaLimitePostulacion: Date?
    let fechaLimite: Date?
    let idCategoria: Int
    let tipoOportunidad: String
    let modalidad: String?
    let visibilidad: String
    let direccionReferencia: String?
    let latitud: Decimal?
    let longitud: Decimal?
}

struct CreateApplicationRequest: Encodable {
    let mensaje: String?
    let precioPropuesto: Decimal?
}

struct AcceptApplicationRequest: Encodable {
    let metodoPago: String
}

struct CreateDeliveryRequest: Encodable {
    let descripcionEntrega: String
}

struct RequestDeliveryChangesRequest: Encodable {
    let observacion: String
}

struct CreateDeliveryCommentRequest: Encodable {
    let comentario: String
}
