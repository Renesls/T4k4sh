import Foundation
import Observation

/// Detalle de un trabajo asignado: pago, entregas y adjuntos.
///
/// Concentra el flujo completo que en el backend reparten `MarketplaceController`
/// y `PaymentController`.
@MainActor
@Observable
final class JobDetailViewModel {
    private let marketplace: MarketplaceRepository
    private let finance: FinanceRepository
    private let attachmentsRepository: AttachmentRepository
    private let currentUserId: Int

    private(set) var job: Job
    private(set) var task: TaskItem?

    init(
        job: Job,
        task: TaskItem?,
        marketplace: MarketplaceRepository,
        finance: FinanceRepository,
        attachments: AttachmentRepository,
        currentUserId: Int
    ) {
        self.job = job
        self.task = task
        self.marketplace = marketplace
        self.finance = finance
        self.attachmentsRepository = attachments
        self.currentUserId = currentUserId
    }

    private(set) var deliveries: LoadState<[Delivery]> = .idle
    private(set) var payment: Payment?
    private(set) var attachments: [Attachment] = []

    var actionError: String?
    var successMessage: String?
    var checkoutDestination: WebDestination?

    /// Roles dentro del trabajo: definen qué acciones se ofrecen.
    var isStudent: Bool { job.idEstudiante == currentUserId }
    var isClient: Bool { task?.idCliente == currentUserId }

    /// La última entrega manda: es la que se aprueba o devuelve.
    var latestDelivery: Delivery? { deliveries.value?.last }

    /// El estudiante puede entregar mientras el trabajo siga abierto y no haya
    /// una entrega esperando revisión.
    var canCreateDelivery: Bool {
        guard isStudent, !job.finalizado else { return false }
        guard let latest = latestDelivery else { return true }
        return latest.requiereCambios
    }

    /// El cliente decide sobre una entrega que está esperando revisión.
    var canReviewDelivery: Bool {
        guard isClient, let latest = latestDelivery else { return false }
        return latest.estadoEntrega == Domain.EstadoEntrega.enviada
    }

    /// El backend calcula `puedePagar`; el cliente solo ve el botón si aplica.
    var canPay: Bool { isClient && (payment?.puedePagar ?? false) }

    /// El efectivo lo confirman ambas partes.
    var canConfirmCash: Bool {
        guard let payment, payment.esEfectivo else { return false }
        return (isClient || isStudent) && !payment.fondosRetenidos
    }

    /// Una disputa solo tiene sentido con dinero comprometido.
    var canOpenDispute: Bool {
        guard let payment else { return false }
        guard isClient || isStudent else { return false }
        return payment.fondosRetenidos && !payment.enDisputa
    }

    func load() async {
        if deliveries.value == nil { deliveries = .loading }

        // El pago puede no existir todavía y los adjuntos dependen de permisos:
        // ninguno de los dos debe tumbar la pantalla.
        async let deliveriesRequest = marketplace.deliveries(jobId: job.idTrabajo)
        async let paymentRequest = try? await finance.payment(jobId: job.idTrabajo)
        async let attachmentsRequest = try? await attachmentsRepository.jobAttachments(
            jobId: job.idTrabajo
        )

        payment = (await paymentRequest) ?? job.pago
        attachments = (await attachmentsRequest) ?? []

        do {
            let loaded = try await deliveriesRequest
            deliveries = .loaded(
                loaded.sorted { ($0.fechaEntrega ?? .distantPast) < ($1.fechaEntrega ?? .distantPast) }
            )
        } catch {
            deliveries = .failed(ErrorPresenter.message(for: error))
        }

        // Refresca los datos del trabajo por si cambió de estado.
        if let refreshed = try? await marketplace.jobs()
            .first(where: { $0.idTrabajo == job.idTrabajo }) {
            job = refreshed
        }
        if task == nil {
            task = try? await marketplace.task(id: job.idTarea)
        }
    }

    // MARK: - Pagos

    /// Abre el checkout de Pagadito. El backend devuelve la URL de la pasarela.
    func startCheckout() async {
        do {
            let checkout = try await finance.createCheckout(jobId: job.idTrabajo)
            guard let destination = WebDestination(checkout.checkoutUrl) else {
                actionError = "El proveedor devolvió una dirección de pago inválida."
                return
            }
            checkoutDestination = destination
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }

    /// Tras volver del navegador se consulta el estado real en el backend.
    func refreshPaymentAfterCheckout() async {
        guard let payment else { return }
        do {
            self.payment = try await finance.refreshPayment(id: payment.idPago)
            await load()
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }

    func refreshPayment() async {
        guard let payment else { return }
        do {
            self.payment = try await finance.refreshPayment(id: payment.idPago)
            successMessage = "Estado del pago actualizado."
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }

    func confirmCashReceipt() async {
        do {
            payment = try await finance.confirmCashReceipt(jobId: job.idTrabajo)
            successMessage = "Confirmación registrada."
            await load()
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }

    func openDispute(
        motivo: String,
        descripcion: String,
        solucion: Domain.SolucionDisputa
    ) async -> Bool {
        guard let payment else { return false }
        do {
            _ = try await finance.openDispute(
                paymentId: payment.idPago,
                motivo: motivo,
                descripcion: descripcion,
                solucion: solucion
            )
            successMessage = "Disputa abierta. Un administrador la revisará."
            await load()
            return true
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return false
        }
    }

    // MARK: - Entregas

    func createDelivery(descripcion: String) async -> Bool {
        do {
            _ = try await marketplace.createDelivery(
                jobId: job.idTrabajo,
                descripcion: descripcion.trimmingCharacters(in: .whitespaces)
            )
            successMessage = "Entrega registrada. El cliente la revisará."
            await load()
            return true
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return false
        }
    }

    func approveDelivery(_ delivery: Delivery) async {
        do {
            _ = try await marketplace.approveDelivery(id: delivery.idEntrega)
            successMessage = "Entrega aprobada. El monto pasó al balance del estudiante."
            await load()
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }

    func requestChanges(_ delivery: Delivery, observacion: String) async -> Bool {
        do {
            _ = try await marketplace.requestDeliveryChanges(
                id: delivery.idEntrega,
                observacion: observacion.trimmingCharacters(in: .whitespaces)
            )
            successMessage = "Solicitaste cambios en la entrega."
            await load()
            return true
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return false
        }
    }

    func comment(_ delivery: Delivery, comentario: String) async -> Bool {
        do {
            _ = try await marketplace.commentDelivery(
                id: delivery.idEntrega,
                comentario: comentario.trimmingCharacters(in: .whitespaces)
            )
            await load()
            return true
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return false
        }
    }

    func uploadDeliveryAttachment(
        _ delivery: Delivery,
        prepared: PreparedAttachment
    ) async {
        do {
            _ = try await attachmentsRepository.uploadDeliveryAttachment(
                deliveryId: delivery.idEntrega,
                prepared
            )
            successMessage = "Archivo adjuntado a la entrega."
            await load()
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }
}
