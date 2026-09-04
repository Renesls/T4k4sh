import Foundation

// Modelos del módulo `finance`.

/// `finance/dto/PaymentResponse`
struct Payment: Codable, Identifiable, Equatable {
    let idPago: Int
    let idTrabajo: Int
    let idCliente: Int
    let idEstudiante: Int
    let proveedorPago: String?
    let entornoPago: String?
    let metodoPago: String
    let monedaCobro: String
    let montoEstudiante: Decimal
    let porcentajeComisionPlataforma: Decimal?
    let comisionPlataforma: Decimal?
    let comisionProcesador: Decimal?
    let impuestoProcesador: Decimal?
    let montoTotalCliente: Decimal
    let estadoPago: String
    let referenciaComercio: String?
    let fechaCreacion: Date?
    let fechaActualizacion: Date?
    let fechaExpiracion: Date?
    let fechaConfirmacion: Date?
    let fechaLiberacion: Date?
    /// Lo calcula el backend: indica si el cliente puede abrir checkout ahora.
    let puedePagar: Bool

    var id: Int { idPago }

    var estadoLegible: String { Domain.EstadoPago.label(estadoPago) }
    var esEfectivo: Bool { metodoPago == Domain.MetodoPago.efectivo.rawValue }
    var esProtegido: Bool { metodoPago == Domain.MetodoPago.pagadito.rawValue }
    var fondosRetenidos: Bool { estadoPago == Domain.EstadoPago.fondosRetenidos }
    var enDisputa: Bool { estadoPago == Domain.EstadoPago.enDisputa }
    var esSandbox: Bool { entornoPago == "SANDBOX" }
}

/// `finance/dto/CheckoutResponse`
struct Checkout: Codable {
    let idPago: Int
    let checkoutUrl: String
    let estadoPago: String?
}

/// `finance/dto/WalletMovementResponse`
struct WalletMovement: Codable, Identifiable, Equatable {
    let idTransaccion: Int64
    let idPago: Int?
    let tipoMovimiento: String
    let saldoAfectado: String
    let monto: Decimal
    let moneda: String
    let estadoMovimiento: String?
    let proveedorPago: String?
    let descripcion: String?
    let fechaRegistro: Date?

    var id: Int64 { idTransaccion }

    /// `saldo_afectado` distingue entre balance disponible y fondos retenidos.
    var afectaDisponible: Bool { saldoAfectado.uppercased().contains("DISPONIBLE") }
}

/// `finance/dto/PaymentDisputeResponse`
struct PaymentDispute: Codable, Identifiable, Equatable {
    let idDisputa: Int
    let idPago: Int
    let idUsuarioAbre: Int
    let idAdminAsignado: Int?
    let motivo: String
    let descripcion: String
    let solucionSolicitada: String
    let montoDisputado: Decimal
    let estadoDisputa: String
    let prioridad: String?
    let fechaApertura: Date?
    let fechaLimiteRespuesta: Date?
    let fechaActualizacion: Date?
    let fechaResolucion: Date?
    let resolucion: String?

    var id: Int { idDisputa }
    var resuelta: Bool { fechaResolucion != nil }
    var solucionLegible: String {
        Domain.SolucionDisputa(rawValue: solucionSolicitada)?.label
            ?? solucionSolicitada.humanizedCode
    }
}

/// `finance/dto/RefundResponse`
struct Refund: Codable, Identifiable, Equatable {
    let idReembolso: Int
    let idPago: Int
    let idDisputa: Int?
    let montoReembolso: Decimal
    let moneda: String
    let motivo: String?
    let estadoReembolso: String
    let fechaSolicitud: Date?
    let fechaConfirmacion: Date?

    var id: Int { idReembolso }
}

/// `finance/dto/PayoutResponse`
struct Payout: Codable, Identifiable, Equatable {
    let idDesembolso: Int
    let idPago: Int
    let idEstudiante: Int
    let montoDesembolso: Decimal
    let moneda: String
    let proveedorDesembolso: String?
    let estadoDesembolso: String
    let fechaCreacion: Date?
    let fechaConfirmacion: Date?

    var id: Int { idDesembolso }
}

/// `finance/dto/WalletResponse`
struct Wallet: Codable, Equatable {
    let moneda: String
    let balanceDisponible: Decimal
    let fondosRetenidos: Decimal
    let totalGanado: Decimal
    let pagos: [Payment]
    let movimientos: [WalletMovement]
    let disputas: [PaymentDispute]
    let reembolsos: [Refund]
    let desembolsos: [Payout]

    /// Estado vacío coherente para el primer render, antes de la primera carga.
    static let empty = Wallet(
        moneda: "NIO",
        balanceDisponible: 0,
        fondosRetenidos: 0,
        totalGanado: 0,
        pagos: [],
        movimientos: [],
        disputas: [],
        reembolsos: [],
        desembolsos: []
    )
}

// MARK: - Peticiones

struct CreatePaymentDisputeRequest: Encodable {
    let motivo: String
    let descripcion: String
    let solucionSolicitada: String
}

struct ResolvePaymentDisputeRequest: Encodable {
    let decision: String
    let resolucion: String
}
