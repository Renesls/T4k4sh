import Foundation

/// Acceso al módulo `marketplace`: catálogo, tareas, postulaciones,
/// trabajos, entregas y adjuntos.
struct MarketplaceRepository {
    private let client: APIClient

    init(client: APIClient) { self.client = client }

    // MARK: - Catálogo y tareas

    /// `GET /categories`
    func categories() async throws -> [Category] {
        try await client.send(.get("categories", requiresAuth: false))
    }

    /// `GET /tasks?page=&size=`
    func tasks(page: Int = 0, size: Int = AppConfig.defaultPageSize) async throws -> [TaskItem] {
        try await client.send(
            .get("tasks", query: ["page": "\(page)", "size": "\(min(size, AppConfig.maximumPageSize))"])
        )
    }

    /// `GET /tasks/{id}`
    func task(id: Int) async throws -> TaskItem {
        try await client.send(.get("tasks/\(id)"))
    }

    /// `POST /tasks`
    func createTask(_ request: CreateTaskRequest) async throws -> TaskItem {
        try await client.send(try .json("tasks", method: .post, body: request))
    }

    /// `PUT /tasks/{id}`
    func updateTask(id: Int, _ request: CreateTaskRequest) async throws -> TaskItem {
        try await client.send(try .json("tasks/\(id)", method: .put, body: request))
    }

    /// `DELETE /tasks/{id}` — el backend cancela la tarea, no la borra.
    func cancelTask(id: Int) async throws -> TaskItem {
        try await client.send(.empty("tasks/\(id)", method: .delete))
    }

    // MARK: - Tareas rápidas

    /// `GET /quick-tasks/nearby?latitude=&longitude=&radiusKm=`
    func nearbyQuickTasks(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ) async throws -> [QuickTask] {
        try await client.send(
            .get("quick-tasks/nearby", query: [
                "latitude": "\(latitude)",
                "longitude": "\(longitude)",
                "radiusKm": "\(radiusKm)",
            ])
        )
    }

    /// `POST /quick-tasks/{id}/claim` — asignación inmediata.
    func claimQuickTask(id: Int) async throws -> Job {
        try await client.send(.empty("quick-tasks/\(id)/claim", method: .post))
    }

    // MARK: - Postulaciones

    /// `POST /tasks/{id}/applications`
    func apply(
        taskId: Int,
        mensaje: String?,
        precioPropuesto: Decimal?
    ) async throws -> Application {
        try await client.send(
            try .json(
                "tasks/\(taskId)/applications",
                method: .post,
                body: CreateApplicationRequest(
                    mensaje: mensaje,
                    precioPropuesto: precioPropuesto
                )
            )
        )
    }

    /// `GET /tasks/{id}/applications` — solo el dueño de la tarea.
    func applications(taskId: Int) async throws -> [Application] {
        try await client.send(.get("tasks/\(taskId)/applications"))
    }

    /// `GET /applications/me`
    func myApplications() async throws -> [Application] {
        try await client.send(.get("applications/me"))
    }

    /// `POST /applications/{id}/accept` — exige el método de pago.
    func acceptApplication(id: Int, metodoPago: Domain.MetodoPago) async throws -> Job {
        try await client.send(
            try .json(
                "applications/\(id)/accept",
                method: .post,
                body: AcceptApplicationRequest(metodoPago: metodoPago.rawValue)
            )
        )
    }

    /// `POST /applications/{id}/reject`
    func rejectApplication(id: Int) async throws -> Application {
        try await client.send(.empty("applications/\(id)/reject", method: .post))
    }

    // MARK: - Trabajos y entregas

    /// `GET /jobs`
    func jobs() async throws -> [Job] {
        try await client.send(.get("jobs"))
    }

    /// `GET /jobs/{id}/deliveries`
    func deliveries(jobId: Int) async throws -> [Delivery] {
        try await client.send(.get("jobs/\(jobId)/deliveries"))
    }

    /// `POST /jobs/{id}/deliveries`
    func createDelivery(jobId: Int, descripcion: String) async throws -> Delivery {
        try await client.send(
            try .json(
                "jobs/\(jobId)/deliveries",
                method: .post,
                body: CreateDeliveryRequest(descripcionEntrega: descripcion)
            )
        )
    }

    /// `POST /deliveries/{id}/approve` — libera el pago retenido.
    func approveDelivery(id: Int) async throws -> Delivery {
        try await client.send(.empty("deliveries/\(id)/approve", method: .post))
    }

    /// `POST /deliveries/{id}/request-changes`
    func requestDeliveryChanges(id: Int, observacion: String) async throws -> Delivery {
        try await client.send(
            try .json(
                "deliveries/\(id)/request-changes",
                method: .post,
                body: RequestDeliveryChangesRequest(observacion: observacion)
            )
        )
    }

    /// `POST /deliveries/{id}/comments`
    func commentDelivery(id: Int, comentario: String) async throws -> Delivery {
        try await client.send(
            try .json(
                "deliveries/\(id)/comments",
                method: .post,
                body: CreateDeliveryCommentRequest(comentario: comentario)
            )
        )
    }
}
