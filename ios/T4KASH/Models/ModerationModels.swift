import Foundation

// Modelos de los módulos `moderation` y `admin`.

/// `moderation/dto/ReportResponse`
struct Report: Codable, Identifiable, Equatable {
    let idReporte: Int
    let idUsuarioReporta: Int
    let correoReporta: String?
    let idUsuarioReportado: Int?
    let correoReportado: String?
    let idTarea: Int?
    let tituloTarea: String?
    let motivo: String
    let descripcion: String?
    let estadoReporte: String
    let fechaReporte: Date?
    let tipoReporte: String?
    let categoriaReporte: String

    var id: Int { idReporte }

    var pendiente: Bool { estadoReporte == "PENDIENTE" }

    var categoriaLegible: String {
        Domain.CategoriaReporte(rawValue: categoriaReporte)?.label
            ?? categoriaReporte.humanizedCode
    }

    var estadoLegible: String {
        Domain.EstadoRevisionReporte(rawValue: estadoReporte)?.label
            ?? estadoReporte.humanizedCode
    }
}

/// `admin/dto/AdminSummaryResponse`
struct AdminSummary: Codable, Equatable {
    let usuarios: Int
    let verificacionesPendientes: Int
    let reportesPendientes: Int
    let publicacionesActivas: Int
    let trabajosAsignados: Int

    static let empty = AdminSummary(
        usuarios: 0,
        verificacionesPendientes: 0,
        reportesPendientes: 0,
        publicacionesActivas: 0,
        trabajosAsignados: 0
    )
}

// MARK: - Peticiones

struct CreateTaskReportRequest: Encodable {
    let categoriaReporte: String
    let descripcion: String?
}

struct ReviewReportRequest: Encodable {
    let estadoReporte: String
    let observacion: String?
    let retirarPublicacion: Bool
}
