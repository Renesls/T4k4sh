import Foundation
import Observation

/// Panel de administración: métricas, reportes, disputas y verificaciones.
///
/// Todos los endpoints exigen el rol `ADMIN`, que el backend asigna por correo
/// mediante `app.auth.admin-emails`.
@MainActor
@Observable
final class AdminViewModel {
    private let repository: ModerationRepository

    init(repository: ModerationRepository) { self.repository = repository }

    enum Section: String, CaseIterable, Identifiable {
        case resumen = "Resumen"
        case reportes = "Reportes"
        case disputas = "Disputas"
        case verificaciones = "Verificaciones"

        var id: String { rawValue }
    }

    var section: Section = .resumen

    private(set) var summary: AdminSummary?
    private(set) var reports: [Report] = []
    private(set) var disputes: [PaymentDispute] = []
    private(set) var verifications: [StudentVerification] = []
    private(set) var state: LoadState<Bool> = .idle

    var actionError: String?
    var successMessage: String?

    func load() async {
        if state.value == nil { state = .loading }
        do {
            async let summaryRequest = repository.adminSummary()
            async let reportsRequest = repository.adminReports()
            async let disputesRequest = repository.adminPaymentDisputes()
            async let verificationsRequest = repository.pendingStudentVerifications()

            summary = try await summaryRequest
            reports = try await reportsRequest
            disputes = try await disputesRequest
            verifications = try await verificationsRequest
            state = .loaded(true)
        } catch {
            state = .failed(ErrorPresenter.message(for: error))
        }
    }

    func reviewReport(
        _ report: Report,
        estado: Domain.EstadoRevisionReporte,
        observacion: String,
        retirarPublicacion: Bool
    ) async -> Bool {
        do {
            let trimmed = observacion.trimmingCharacters(in: .whitespaces)
            _ = try await repository.reviewReport(
                id: report.idReporte,
                estado: estado,
                observacion: trimmed.isEmpty ? nil : trimmed,
                retirarPublicacion: retirarPublicacion
            )
            successMessage = "Reporte marcado como \(estado.label.lowercased())."
            await load()
            return true
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return false
        }
    }

    func resolveDispute(
        _ dispute: PaymentDispute,
        decision: Domain.DecisionDisputa,
        resolucion: String
    ) async -> Bool {
        do {
            _ = try await repository.resolveDispute(
                id: dispute.idDisputa,
                decision: decision,
                resolucion: resolucion.trimmingCharacters(in: .whitespaces)
            )
            successMessage = "Disputa resuelta."
            await load()
            return true
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return false
        }
    }

    func reviewStudentVerification(
        _ verification: StudentVerification,
        approve: Bool,
        observacion: String
    ) async -> Bool {
        do {
            let trimmed = observacion.trimmingCharacters(in: .whitespaces)
            let comment = trimmed.isEmpty ? nil : trimmed
            _ = approve
                ? try await repository.approveStudentVerification(
                    userId: verification.idUsuario,
                    observacion: comment
                )
                : try await repository.rejectStudentVerification(
                    userId: verification.idUsuario,
                    observacion: comment
                )
            successMessage = approve
                ? "Verificación aprobada."
                : "Verificación rechazada."
            await load()
            return true
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return false
        }
    }

    func cancelTask(id: Int) async {
        do {
            _ = try await repository.adminCancelTask(id: id)
            successMessage = "Oportunidad cancelada."
            await load()
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }
}
