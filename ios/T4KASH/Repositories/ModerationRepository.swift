import Foundation

/// Acceso a los módulos `moderation` y `admin`.
struct ModerationRepository {
    private let client: APIClient

    init(client: APIClient) { self.client = client }

    // MARK: - Reportes del usuario

    /// `POST /tasks/{id}/reports`
    func reportTask(
        taskId: Int,
        categoria: Domain.CategoriaReporte,
        descripcion: String?
    ) async throws -> Report {
        try await client.send(
            try .json(
                "tasks/\(taskId)/reports",
                method: .post,
                body: CreateTaskReportRequest(
                    categoriaReporte: categoria.rawValue,
                    descripcion: descripcion
                )
            )
        )
    }

    /// `GET /reports/me?page=&size=`
    func myReports(page: Int = 0, size: Int = AppConfig.defaultPageSize) async throws -> [Report] {
        try await client.send(
            .get("reports/me", query: [
                "page": "\(page)",
                "size": "\(min(size, AppConfig.maximumPageSize))",
            ])
        )
    }

    // MARK: - Administración (rol ADMIN)

    /// `GET /admin/summary`
    func adminSummary() async throws -> AdminSummary {
        try await client.send(.get("admin/summary"))
    }

    /// `GET /admin/tasks?page=&size=`
    func adminTasks(page: Int = 0, size: Int = AppConfig.defaultPageSize) async throws -> [TaskItem] {
        try await client.send(
            .get("admin/tasks", query: [
                "page": "\(page)",
                "size": "\(min(size, AppConfig.maximumPageSize))",
            ])
        )
    }

    /// `DELETE /admin/tasks/{id}`
    func adminCancelTask(id: Int) async throws -> TaskItem {
        try await client.send(.empty("admin/tasks/\(id)", method: .delete))
    }

    /// `GET /admin/reports?page=&size=`
    func adminReports(page: Int = 0, size: Int = AppConfig.defaultPageSize) async throws -> [Report] {
        try await client.send(
            .get("admin/reports", query: [
                "page": "\(page)",
                "size": "\(min(size, AppConfig.maximumPageSize))",
            ])
        )
    }

    /// `POST /admin/reports/{id}/review`
    func reviewReport(
        id: Int,
        estado: Domain.EstadoRevisionReporte,
        observacion: String?,
        retirarPublicacion: Bool
    ) async throws -> Report {
        try await client.send(
            try .json(
                "admin/reports/\(id)/review",
                method: .post,
                body: ReviewReportRequest(
                    estadoReporte: estado.rawValue,
                    observacion: observacion,
                    retirarPublicacion: retirarPublicacion
                )
            )
        )
    }

    /// `GET /admin/payment-disputes`
    func adminPaymentDisputes() async throws -> [PaymentDispute] {
        try await client.send(.get("admin/payment-disputes"))
    }

    /// `POST /admin/payment-disputes/{id}/resolve`
    func resolveDispute(
        id: Int,
        decision: Domain.DecisionDisputa,
        resolucion: String
    ) async throws -> PaymentDispute {
        try await client.send(
            try .json(
                "admin/payment-disputes/\(id)/resolve",
                method: .post,
                body: ResolvePaymentDisputeRequest(
                    decision: decision.rawValue,
                    resolucion: resolucion
                )
            )
        )
    }

    /// `GET /student-verifications/pending`
    func pendingStudentVerifications() async throws -> [StudentVerification] {
        try await client.send(.get("student-verifications/pending"))
    }

    /// `POST /student-verifications/{userId}/approve`
    func approveStudentVerification(
        userId: Int,
        observacion: String?
    ) async throws -> StudentVerification {
        try await client.send(
            try .json(
                "student-verifications/\(userId)/approve",
                method: .post,
                body: ReviewStudentVerificationRequest(observacion: observacion)
            )
        )
    }

    /// `POST /student-verifications/{userId}/reject`
    func rejectStudentVerification(
        userId: Int,
        observacion: String?
    ) async throws -> StudentVerification {
        try await client.send(
            try .json(
                "student-verifications/\(userId)/reject",
                method: .post,
                body: ReviewStudentVerificationRequest(observacion: observacion)
            )
        )
    }
}
