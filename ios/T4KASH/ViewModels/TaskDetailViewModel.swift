import Foundation
import Observation

/// Detalle de una oportunidad: datos, adjuntos, postulación y acciones del dueño.
@MainActor
@Observable
final class TaskDetailViewModel {
    private let marketplace: MarketplaceRepository
    private let attachmentsRepository: AttachmentRepository
    private let moderation: ModerationRepository
    private let currentUserId: Int

    let taskId: Int

    init(
        taskId: Int,
        marketplace: MarketplaceRepository,
        attachments: AttachmentRepository,
        moderation: ModerationRepository,
        currentUserId: Int
    ) {
        self.taskId = taskId
        self.marketplace = marketplace
        self.attachmentsRepository = attachments
        self.moderation = moderation
        self.currentUserId = currentUserId
    }

    private(set) var state: LoadState<TaskItem> = .idle
    private(set) var attachments: [Attachment] = []
    private(set) var myApplication: Application?
    private(set) var categories: [Category] = []

    var actionError: String?
    var successMessage: String?

    var task: TaskItem? { state.value }

    /// El dueño de la tarea gestiona postulaciones; el resto puede postular.
    var isOwner: Bool { task?.idCliente == currentUserId }

    var canApply: Bool {
        guard let task, !isOwner else { return false }
        guard task.estaPublicada, !task.esTareaRapida else { return false }
        return myApplication == nil
    }

    /// El backend permite hasta 3 intentos por tarea (`ck_postulaciones_numero_intento`).
    var applicationAttempt: Int { (myApplication?.numeroIntento ?? 0) }

    var categoryName: String {
        guard let task else { return "" }
        return categories.first { $0.idCategoria == task.idCategoria }?.nombreCategoria
            ?? "Categoría"
    }

    func load() async {
        if state.value == nil { state = .loading }
        do {
            async let taskRequest = marketplace.task(id: taskId)
            async let attachmentsRequest = attachmentsRepository.taskAttachments(taskId: taskId)
            async let applicationsRequest = marketplace.myApplications()
            async let categoriesRequest = marketplace.categories()

            let loadedTask = try await taskRequest
            state = .loaded(loadedTask)

            // Los complementos no deben tumbar la pantalla si fallan por permisos.
            attachments = (try? await attachmentsRequest) ?? []
            categories = (try? await categoriesRequest) ?? []
            myApplication = (try? await applicationsRequest)?
                .first { $0.idTarea == taskId }
        } catch {
            state = .failed(ErrorPresenter.message(for: error))
        }
    }

    /// Envía una postulación. Devuelve `true` si el backend la aceptó.
    func apply(mensaje: String, precio: Decimal?) async -> Bool {
        do {
            let trimmed = mensaje.trimmingCharacters(in: .whitespaces)
            myApplication = try await marketplace.apply(
                taskId: taskId,
                mensaje: trimmed.isEmpty ? nil : trimmed,
                precioPropuesto: precio
            )
            successMessage = "Postulación enviada. El cliente la revisará pronto."
            return true
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return false
        }
    }

    /// Cancela la tarea (solo el dueño). El backend la marca `CANCELADA`.
    func cancelTask() async -> TaskItem? {
        do {
            let cancelled = try await marketplace.cancelTask(id: taskId)
            state = .loaded(cancelled)
            successMessage = "La oportunidad fue cancelada."
            return cancelled
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return nil
        }
    }

    /// Reclama una tarea rápida: asignación inmediata al primero que llega.
    func claimQuickTask() async -> Job? {
        do {
            let job = try await marketplace.claimQuickTask(id: taskId)
            successMessage = "Reclamaste la tarea. Coordina la entrega con el cliente."
            await load()
            return job
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return nil
        }
    }

    func uploadAttachment(_ prepared: PreparedAttachment) async {
        do {
            let uploaded = try await attachmentsRepository.uploadTaskAttachment(
                taskId: taskId,
                prepared
            )
            attachments.append(uploaded)
            successMessage = "Archivo adjuntado."
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }

    func report(categoria: Domain.CategoriaReporte, descripcion: String) async -> Bool {
        do {
            let trimmed = descripcion.trimmingCharacters(in: .whitespaces)
            _ = try await moderation.reportTask(
                taskId: taskId,
                categoria: categoria,
                descripcion: trimmed.isEmpty ? nil : trimmed
            )
            successMessage = "Gracias. Un moderador revisará el reporte."
            return true
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return false
        }
    }

    func apply(updated task: TaskItem) {
        state = .loaded(task)
    }
}
