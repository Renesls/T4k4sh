import Foundation

/// Acceso al módulo `finance`: wallet, pagos, disputas.
struct FinanceRepository {
    private let client: APIClient

    init(client: APIClient) { self.client = client }

    /// `GET /wallet?page=&size=`
    func wallet(page: Int = 0, size: Int = AppConfig.defaultPageSize) async throws -> Wallet {
        try await client.send(
            .get("wallet", query: [
                "page": "\(page)",
                "size": "\(min(size, AppConfig.maximumPageSize))",
            ])
        )
    }

    /// `GET /jobs/{id}/payment`
    func payment(jobId: Int) async throws -> Payment {
        try await client.send(.get("jobs/\(jobId)/payment"))
    }

    /// `POST /jobs/{id}/payment/checkout` — devuelve la URL de Pagadito.
    func createCheckout(jobId: Int) async throws -> Checkout {
        try await client.send(.empty("jobs/\(jobId)/payment/checkout", method: .post))
    }

    /// `POST /jobs/{id}/payment/cash/confirm-receipt`
    /// Solo aplica a pagos en efectivo; ambas partes deben confirmar.
    func confirmCashReceipt(jobId: Int) async throws -> Payment {
        try await client.send(
            .empty("jobs/\(jobId)/payment/cash/confirm-receipt", method: .post)
        )
    }

    /// `POST /payments/{id}/refresh` — consulta el estado real en Pagadito.
    func refreshPayment(id: Int) async throws -> Payment {
        try await client.send(.empty("payments/\(id)/refresh", method: .post))
    }

    /// `POST /payments/{id}/disputes`
    func openDispute(
        paymentId: Int,
        motivo: String,
        descripcion: String,
        solucion: Domain.SolucionDisputa
    ) async throws -> PaymentDispute {
        try await client.send(
            try .json(
                "payments/\(paymentId)/disputes",
                method: .post,
                body: CreatePaymentDisputeRequest(
                    motivo: motivo,
                    descripcion: descripcion,
                    solucionSolicitada: solucion.rawValue
                )
            )
        )
    }

    /// `GET /disputes/me?page=&size=`
    func myDisputes(
        page: Int = 0,
        size: Int = AppConfig.defaultPageSize
    ) async throws -> [PaymentDispute] {
        try await client.send(
            .get("disputes/me", query: [
                "page": "\(page)",
                "size": "\(min(size, AppConfig.maximumPageSize))",
            ])
        )
    }
}
