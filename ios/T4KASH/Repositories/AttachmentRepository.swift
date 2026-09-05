import Foundation

/// Adjuntos de tareas, entregas y trabajos.
struct AttachmentRepository {
    private let client: APIClient

    init(client: APIClient) { self.client = client }

    /// `GET /tasks/{id}/attachments`
    func taskAttachments(taskId: Int) async throws -> [Attachment] {
        try await client.send(.get("tasks/\(taskId)/attachments"))
    }

    /// `POST /tasks/{id}/attachments` (multipart, parte `file`)
    func uploadTaskAttachment(
        taskId: Int,
        _ attachment: PreparedAttachment
    ) async throws -> Attachment {
        try await client.upload(
            path: "tasks/\(taskId)/attachments",
            filename: attachment.filename,
            mimeType: attachment.mimeType,
            fileData: attachment.data
        )
    }

    /// `GET /jobs/{id}/attachments`
    func jobAttachments(jobId: Int) async throws -> [Attachment] {
        try await client.send(.get("jobs/\(jobId)/attachments"))
    }

    /// `GET /deliveries/{id}/attachments`
    func deliveryAttachments(deliveryId: Int) async throws -> [Attachment] {
        try await client.send(.get("deliveries/\(deliveryId)/attachments"))
    }

    /// `POST /deliveries/{id}/attachments` (multipart, parte `file`)
    func uploadDeliveryAttachment(
        deliveryId: Int,
        _ attachment: PreparedAttachment
    ) async throws -> Attachment {
        try await client.upload(
            path: "deliveries/\(deliveryId)/attachments",
            filename: attachment.filename,
            mimeType: attachment.mimeType,
            fileData: attachment.data
        )
    }

    /// `GET /attachments/{id}/download` — devuelve los bytes del archivo.
    ///
    /// Se escribe en el directorio temporal para poder previsualizarlo o
    /// compartirlo con la hoja del sistema.
    func download(_ attachment: Attachment) async throws -> URL {
        let data = try await client.download(path: attachment.rutaDescarga)

        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("adjuntos", isDirectory: true)
        try FileManager.default.createDirectory(
            at: directory,
            withIntermediateDirectories: true
        )

        // El id evita colisiones entre archivos con el mismo nombre original.
        let destination = directory
            .appendingPathComponent("\(attachment.idArchivo)-\(attachment.nombreOriginal)")
        try data.write(to: destination, options: .atomic)
        return destination
    }
}
