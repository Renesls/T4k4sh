import Foundation

/// Constantes de dominio verificadas contra el backend.
///
/// Cada valor está tomado del código Java real (`TaskService`, `NetworkService`,
/// `ReportService`, las anotaciones `@Pattern` de los DTO y las restricciones
/// `CHECK` de `database/schema-postgresql.sql`). No hay valores inventados: si un
/// estado desconocido llega desde la API, se muestra tal cual en vez de romper.
enum Domain {

    // MARK: - Marketplace

    /// `TaskService.MODALIDADES_VALIDAS`
    enum Modalidad: String, CaseIterable, Identifiable {
        case remota = "REMOTA"
        case presencial = "PRESENCIAL"
        case hibrida = "HIBRIDA"

        var id: String { rawValue }

        var label: String {
            switch self {
            case .remota: "Remota"
            case .presencial: "Presencial"
            case .hibrida: "Híbrida"
            }
        }

        var icon: String {
            switch self {
            case .remota: "laptopcomputer"
            case .presencial: "mappin.and.ellipse"
            case .hibrida: "arrow.triangle.2.circlepath"
            }
        }

        /// El backend exige ubicación para presencial e híbrida.
        var requiresLocation: Bool { self != .remota }
    }

    /// `TaskService.TIPO_TAREA_RAPIDA` y `PostTaskScreen.kt`
    enum TipoOportunidad: String, CaseIterable, Identifiable {
        case tarea = "TAREA"
        case rapida = "RAPIDA"

        var id: String { rawValue }

        var label: String {
            switch self {
            case .tarea: "Oportunidad"
            case .rapida: "Tarea rápida"
            }
        }

        var detail: String {
            switch self {
            case .tarea: "Recibe postulaciones y elige a quién asignar."
            case .rapida: "Presencial, se asigna al primer estudiante que la reclame."
            }
        }
    }

    /// Reglas de tarea rápida tomadas de `TaskService`.
    enum QuickTask {
        static let minimumRadiusKm = 0.25
        static let maximumRadiusKm = 5.0
        static let availabilityHours = 24
        static let deliveryHours = 3
        static let maximumBudget: Decimal = 1_000
    }

    /// Estados de `tareas.estado_tarea`.
    enum EstadoTarea {
        static let publicada = "PUBLICADA"
        static let asignada = "ASIGNADA"
        static let cerrada = "CERRADA"
        static let cancelada = "CANCELADA"

        static func label(_ raw: String) -> String {
            switch raw {
            case publicada: "Publicada"
            case asignada: "Asignada"
            case cerrada: "Cerrada"
            case cancelada: "Cancelada"
            default: raw.humanizedCode
            }
        }
    }

    /// Estados de `postulaciones.estado_postulacion`.
    enum EstadoPostulacion {
        static let pendiente = "PENDIENTE"
        static let aceptada = "ACEPTADA"
        static let rechazada = "RECHAZADA"
        static let canceladaTarea = "CANCELADA_TAREA"

        static func label(_ raw: String) -> String {
            switch raw {
            case pendiente: "Pendiente"
            case aceptada: "Aceptada"
            case rechazada: "Rechazada"
            case canceladaTarea: "Tarea cancelada"
            default: raw.humanizedCode
            }
        }
    }

    /// Estados de `trabajos_asignados.estado_trabajo`.
    enum EstadoTrabajo {
        static let pendientePago = "PENDIENTE_PAGO"
        static let enProceso = "EN_PROCESO"
        static let finalizado = "FINALIZADO"

        static func label(_ raw: String) -> String {
            switch raw {
            case pendientePago: "Pendiente de pago"
            case enProceso: "En proceso"
            case finalizado: "Finalizado"
            default: raw.humanizedCode
            }
        }
    }

    /// Estados de `entregas.estado_entrega`.
    enum EstadoEntrega {
        static let enviada = "ENVIADA"
        static let aprobada = "APROBADA"
        static let cambiosSolicitados = "CAMBIOS_SOLICITADOS"

        static func label(_ raw: String) -> String {
            switch raw {
            case enviada: "Enviada"
            case aprobada: "Aprobada"
            case cambiosSolicitados: "Cambios solicitados"
            default: raw.humanizedCode
            }
        }
    }

    // MARK: - Finanzas

    /// `AcceptApplicationRequest` acepta exactamente estos dos valores.
    enum MetodoPago: String, CaseIterable, Identifiable {
        case pagadito = "PAGADITO"
        case efectivo = "EFECTIVO"

        var id: String { rawValue }

        var label: String {
            switch self {
            case .pagadito: "Pago protegido"
            case .efectivo: "Efectivo"
            }
        }

        var detail: String {
            switch self {
            case .pagadito:
                "El monto queda retenido y se libera cuando apruebas la entrega."
            case .efectivo:
                "Solo para tareas presenciales o rápidas. Ambas partes confirman la entrega del dinero."
            }
        }

        var icon: String {
            switch self {
            case .pagadito: "lock.shield"
            case .efectivo: "banknote"
            }
        }
    }

    /// Estados de `pagos.estado_pago`.
    enum EstadoPago {
        static let pendientePago = "PENDIENTE_PAGO"
        static let fondosRetenidos = "FONDOS_RETENIDOS"
        static let liberado = "LIBERADO"
        static let pagoFallido = "PAGO_FALLIDO"
        static let pagoCancelado = "PAGO_CANCELADO"
        static let pagoExpirado = "PAGO_EXPIRADO"
        static let pagoRevocado = "PAGO_REVOCADO"
        static let enDisputa = "EN_DISPUTA"
        static let reembolsado = "REEMBOLSADO"

        static func label(_ raw: String) -> String {
            switch raw {
            case pendientePago: "Pendiente de pago"
            case fondosRetenidos: "Fondos retenidos"
            case liberado: "Liberado"
            case pagoFallido: "Pago fallido"
            case pagoCancelado: "Pago cancelado"
            case pagoExpirado: "Pago expirado"
            case pagoRevocado: "Pago revocado"
            case enDisputa: "En disputa"
            case reembolsado: "Reembolsado"
            default: raw.humanizedCode
            }
        }
    }

    /// `CreatePaymentDisputeRequest.solucionSolicitada`
    enum SolucionDisputa: String, CaseIterable, Identifiable {
        case pagoEstudiante = "PAGO_ESTUDIANTE"
        case reembolsoCliente = "REEMBOLSO_CLIENTE"

        var id: String { rawValue }

        var label: String {
            switch self {
            case .pagoEstudiante: "Liberar el pago al estudiante"
            case .reembolsoCliente: "Reembolsar al cliente"
            }
        }
    }

    /// `ResolvePaymentDisputeRequest.decision`
    enum DecisionDisputa: String, CaseIterable, Identifiable {
        case liberarEstudiante = "LIBERAR_ESTUDIANTE"
        case reembolsarCliente = "REEMBOLSAR_CLIENTE"

        var id: String { rawValue }

        var label: String {
            switch self {
            case .liberarEstudiante: "Liberar al estudiante"
            case .reembolsarCliente: "Reembolsar al cliente"
            }
        }
    }

    // MARK: - Red universitaria

    /// `NetworkService.FEED_SCOPES`
    enum FeedScope: String, CaseIterable, Identifiable {
        case paraTi = "PARA_TI"
        case conexiones = "CONEXIONES"
        case universidad = "UNIVERSIDAD"

        var id: String { rawValue }

        var label: String {
            switch self {
            case .paraTi: "Para ti"
            case .conexiones: "Conexiones"
            case .universidad: "Universidad"
            }
        }
    }

    /// `NetworkService.POST_TYPES`. `COMPARTIDA` la genera el backend al compartir,
    /// por eso no aparece en el selector de creación.
    enum TipoPublicacion: String, CaseIterable, Identifiable {
        case texto = "TEXTO"
        case imagen = "IMAGEN"
        case video = "VIDEO"
        case proyecto = "PROYECTO"
        case logro = "LOGRO"
        case pregunta = "PREGUNTA"
        case recurso = "RECURSO"
        case evento = "EVENTO"
        case compartida = "COMPARTIDA"

        var id: String { rawValue }

        static var composable: [TipoPublicacion] {
            allCases.filter { $0 != .compartida }
        }

        var label: String {
            switch self {
            case .texto: "Texto"
            case .imagen: "Imagen"
            case .video: "Video"
            case .proyecto: "Proyecto"
            case .logro: "Logro"
            case .pregunta: "Pregunta"
            case .recurso: "Recurso"
            case .evento: "Evento"
            case .compartida: "Compartida"
            }
        }

        var icon: String {
            switch self {
            case .texto: "text.alignleft"
            case .imagen: "photo"
            case .video: "video"
            case .proyecto: "hammer"
            case .logro: "trophy"
            case .pregunta: "questionmark.circle"
            case .recurso: "book"
            case .evento: "calendar"
            case .compartida: "arrowshape.turn.up.right"
            }
        }
    }

    /// `NetworkService.VISIBILITIES`
    enum VisibilidadPublicacion: String, CaseIterable, Identifiable {
        case publica = "PUBLICA"
        case conexiones = "CONEXIONES"
        case universidad = "UNIVERSIDAD"

        var id: String { rawValue }

        var label: String {
            switch self {
            case .publica: "Pública"
            case .conexiones: "Conexiones"
            case .universidad: "Universidad"
            }
        }

        var icon: String {
            switch self {
            case .publica: "globe"
            case .conexiones: "person.2"
            case .universidad: "building.columns"
            }
        }
    }

    /// `NetworkService.REACTIONS`
    enum Reaccion: String, CaseIterable, Identifiable {
        case meGusta = "ME_GUSTA"
        case apoyo = "APOYO"
        case celebrar = "CELEBRAR"
        case interesa = "INTERESA"

        var id: String { rawValue }

        var label: String {
            switch self {
            case .meGusta: "Me gusta"
            case .apoyo: "Apoyo"
            case .celebrar: "Celebrar"
            case .interesa: "Me interesa"
            }
        }

        var emoji: String {
            switch self {
            case .meGusta: "👍"
            case .apoyo: "🤝"
            case .celebrar: "🎉"
            case .interesa: "💡"
            }
        }
    }

    // MARK: - Moderación

    /// `ReportService.CATEGORY_LABELS`
    enum CategoriaReporte: String, CaseIterable, Identifiable {
        case contenidoInapropiado = "CONTENIDO_INAPROPIADO"
        case posibleEstafa = "POSIBLE_ESTAFA"
        case informacionFalsa = "INFORMACION_FALSA"
        case publicacionDuplicada = "PUBLICACION_DUPLICADA"
        case otro = "OTRO"

        var id: String { rawValue }

        var label: String {
            switch self {
            case .contenidoInapropiado: "Contenido inapropiado"
            case .posibleEstafa: "Posible estafa"
            case .informacionFalsa: "Información falsa"
            case .publicacionDuplicada: "Publicación duplicada"
            case .otro: "Otro motivo"
            }
        }
    }

    /// `ReportService.VALID_REVIEW_STATES`
    enum EstadoRevisionReporte: String, CaseIterable, Identifiable {
        case resuelto = "RESUELTO"
        case descartado = "DESCARTADO"

        var id: String { rawValue }

        var label: String {
            switch self {
            case .resuelto: "Resuelto"
            case .descartado: "Descartado"
            }
        }
    }

    // MARK: - Identidad

    /// Estados devueltos por `IdentityVerificationStatusResponse`.
    enum EstadoVerificacionIdentidad {
        static let noIniciada = "NO_INICIADA"
        static let pendiente = "PENDIENTE"
        static let enProceso = "EN_PROCESO"
        static let enRevision = "EN_REVISION"
        static let aprobada = "APROBADA"
        static let rechazada = "RECHAZADA"
        static let expirada = "EXPIRADA"
        static let vencida = "VENCIDA"
        static let abandonada = "ABANDONADA"
        static let requiereAccion = "REQUIERE_ACCION"
        static let cancelada = "CANCELADA"

        static func label(_ raw: String) -> String {
            switch raw {
            case noIniciada: "No iniciada"
            case pendiente: "Lista para comenzar"
            case enProceso: "En proceso"
            case enRevision: "En revisión"
            case aprobada: "Aprobada"
            case rechazada: "Rechazada"
            case expirada, vencida: "Vencida"
            case abandonada: "Sin completar"
            case requiereAccion: "Requiere acción"
            case cancelada: "Cancelada"
            default: raw.humanizedCode
            }
        }
    }

    /// Estados de `verificaciones_usuario.estado_verificacion` (verificación estudiantil).
    enum EstadoVerificacionEstudiantil {
        static let pendiente = "PENDIENTE"
        static let aprobada = "APROBADA"
        static let rechazada = "RECHAZADA"

        static func label(_ raw: String) -> String {
            switch raw {
            case pendiente: "Pendiente de revisión"
            case aprobada: "Aprobada"
            case rechazada: "Rechazada"
            default: raw.humanizedCode
            }
        }
    }

    /// Roles que el backend asigna por correo (`app.auth.admin-emails`).
    enum Rol {
        static let admin = "ADMIN"
        static let evaluador = "EVALUADOR"
    }
}

extension String {
    /// Convierte un código del backend (`CAMBIOS_SOLICITADOS`) en texto legible
    /// cuando no hay una etiqueta explícita para él.
    var humanizedCode: String {
        replacingOccurrences(of: "_", with: " ").lowercased().capitalizedFirstLetter
    }
}
