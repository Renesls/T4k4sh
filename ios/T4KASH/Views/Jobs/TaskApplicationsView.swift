import SwiftUI

/// Gestión de las postulaciones recibidas en una oportunidad.
///
/// Aceptar exige elegir método de pago, y el efectivo solo está disponible en
/// tareas presenciales o rápidas, tal como valida el backend.
struct TaskApplicationsView: View {
    let task: TaskItem

    @Environment(AppDependencies.self) private var dependencies

    @State private var state: LoadState<[Application]> = .idle
    @State private var actionError: String?
    @State private var successMessage: String?
    @State private var applicationToAccept: Application?

    /// El backend rechaza efectivo en tareas remotas.
    private var allowsCash: Bool {
        task.esTareaRapida || task.modalidad == Domain.Modalidad.presencial.rawValue
    }

    var body: some View {
        Group {
            switch state {
            case .idle, .loading:
                LoadingStateView()

            case let .failed(message):
                ErrorStateView(message: message) { await load() }

            case let .loaded(applications):
                if applications.isEmpty {
                    EmptyStateView(
                        icon: "person.crop.circle.badge.questionmark",
                        title: "Aún no hay postulaciones",
                        message: "Cuando alguien se postule a «\(task.titulo)» lo verás aquí."
                    )
                } else {
                    ScrollView {
                        LazyVStack(spacing: Theme.Spacing.md) {
                            if let actionError {
                                InlineErrorBanner(message: actionError) { self.actionError = nil }
                            }
                            if let successMessage {
                                InlineSuccessBanner(message: successMessage)
                            }

                            ForEach(applications) { application in
                                ApplicationCard(
                                    application: application,
                                    canDecide: application.estaPendiente && task.estaPublicada,
                                    onAccept: { applicationToAccept = application },
                                    onReject: { await reject(application) }
                                )
                            }
                        }
                        .padding(Theme.Spacing.md)
                    }
                    .refreshable { await load() }
                }
            }
        }
        .screenBackground()
        .navigationTitle("Postulaciones")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
        .sheet(item: $applicationToAccept) { application in
            AcceptApplicationSheet(
                application: application,
                allowsCash: allowsCash
            ) { metodo in
                await accept(application, metodo: metodo)
            }
            .presentationDetents([.medium])
        }
    }

    private func load() async {
        if state.value == nil { state = .loading }
        do {
            let applications = try await dependencies.marketplace.applications(
                taskId: task.idTarea
            )
            // Primero las pendientes: son las que requieren una decisión.
            state = .loaded(applications.sorted { lhs, rhs in
                if lhs.estaPendiente != rhs.estaPendiente { return lhs.estaPendiente }
                return (lhs.fechaPostulacion ?? .distantPast) > (rhs.fechaPostulacion ?? .distantPast)
            })
        } catch {
            state = .failed(ErrorPresenter.message(for: error))
        }
    }

    private func accept(_ application: Application, metodo: Domain.MetodoPago) async {
        do {
            _ = try await dependencies.marketplace.acceptApplication(
                id: application.idPostulacion,
                metodoPago: metodo
            )
            successMessage = metodo == .pagadito
                ? "Postulación aceptada. Completa el pago protegido desde el trabajo asignado."
                : "Postulación aceptada. Coordina el pago en efectivo con el estudiante."
            applicationToAccept = nil
            await load()
        } catch {
            actionError = ErrorPresenter.message(for: error)
            applicationToAccept = nil
        }
    }

    private func reject(_ application: Application) async {
        do {
            _ = try await dependencies.marketplace.rejectApplication(
                id: application.idPostulacion
            )
            successMessage = "Postulación rechazada."
            await load()
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }
}

/// Tarjeta de una postulación recibida.
struct ApplicationCard: View {
    let application: Application
    let canDecide: Bool
    let onAccept: () -> Void
    let onReject: () async -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            HStack(spacing: Theme.Spacing.sm) {
                if let estudiante = application.estudiante {
                    InitialsAvatar(
                        initials: estudiante.iniciales,
                        size: 44,
                        verified: estudiante.estudianteVerificado
                    )
                    VStack(alignment: .leading, spacing: 2) {
                        Text(estudiante.nombreCompleto)
                            .font(Theme.Font.headline)
                            .foregroundStyle(Theme.Color.text)
                        if !estudiante.arroba.isEmpty {
                            Text(estudiante.arroba)
                                .font(Theme.Font.caption)
                                .foregroundStyle(Theme.Color.primary)
                        }
                        if let carrera = estudiante.nombreCarrera {
                            Text(carrera)
                                .font(Theme.Font.caption)
                                .foregroundStyle(Theme.Color.textMuted)
                                .lineLimit(1)
                        }
                    }
                } else {
                    Text("Estudiante")
                        .font(Theme.Font.headline)
                        .foregroundStyle(Theme.Color.text)
                }

                Spacer(minLength: 0)

                StatusPill(
                    text: application.estadoLegible,
                    tint: StatusTint.application(application.estadoPostulacion)
                )
            }

            if let mensaje = application.mensaje, !mensaje.isEmpty {
                Text(mensaje)
                    .font(Theme.Font.subheadline)
                    .foregroundStyle(Theme.Color.textMuted)
            }

            HStack {
                if let precio = application.precioPropuesto {
                    DetailRow(
                        label: "Precio propuesto",
                        value: DisplayFormatter.money(precio),
                        icon: "tag"
                    )
                } else {
                    Text("Acepta el presupuesto publicado")
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textMuted)
                }
            }

            HStack {
                if let intento = application.numeroIntento, intento > 1 {
                    StatusPill(text: "Intento \(intento) de 3", tint: Theme.Color.textMuted)
                }
                Spacer(minLength: 0)
                Text(DisplayFormatter.relative(application.fechaPostulacion))
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textSoft)
            }

            if canDecide {
                Divider().overlay(Theme.Color.border)

                HStack(spacing: Theme.Spacing.sm) {
                    AsyncButton(role: .destructive, action: onReject) {
                        Text("Rechazar")
                            .font(Theme.Font.footnote.weight(.semibold))
                            .foregroundStyle(Theme.Color.danger)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, Theme.Spacing.xs)
                            .background(
                                RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                                    .fill(Theme.Color.danger.opacity(0.08))
                            )
                    }

                    Button(action: onAccept) {
                        Text("Aceptar")
                            .font(Theme.Font.footnote.weight(.semibold))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, Theme.Spacing.xs)
                            .background(
                                RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                                    .fill(Theme.Color.primary)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .cardSurface()
    }
}

/// Elección del método de pago al aceptar una postulación.
struct AcceptApplicationSheet: View {
    let application: Application
    let allowsCash: Bool
    let onConfirm: (Domain.MetodoPago) async -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var metodo: Domain.MetodoPago = .pagadito

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: Theme.Spacing.md) {
                Text("Elige cómo se pagará este trabajo")
                    .font(Theme.Font.headline)
                    .foregroundStyle(Theme.Color.text)

                ForEach(Domain.MetodoPago.allCases) { option in
                    let disabled = option == .efectivo && !allowsCash

                    Button {
                        guard !disabled else { return }
                        metodo = option
                    } label: {
                        HStack(alignment: .top, spacing: Theme.Spacing.sm) {
                            Image(systemName: metodo == option
                                  ? "largecircle.fill.circle"
                                  : "circle")
                                .foregroundStyle(disabled ? Theme.Color.textSoft : Theme.Color.primary)

                            VStack(alignment: .leading, spacing: 4) {
                                Label(option.label, systemImage: option.icon)
                                    .font(Theme.Font.headline)
                                    .foregroundStyle(disabled ? Theme.Color.textSoft : Theme.Color.text)
                                Text(disabled
                                     ? "No disponible: el efectivo solo aplica a tareas presenciales o rápidas."
                                     : option.detail)
                                    .font(Theme.Font.caption)
                                    .foregroundStyle(Theme.Color.textMuted)
                                    .multilineTextAlignment(.leading)
                            }
                            Spacer(minLength: 0)
                        }
                        .padding(Theme.Spacing.sm)
                        .background(
                            RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous)
                                .fill(metodo == option
                                      ? Theme.Color.primaryContainer
                                      : Theme.Color.surfaceVariant)
                        )
                    }
                    .buttonStyle(.plain)
                    .disabled(disabled)
                }

                Spacer()

                AsyncButton { await onConfirm(metodo) } label: {
                    Text("Aceptar postulación")
                }
                .buttonStyle(PrimaryButtonStyle())
            }
            .padding(Theme.Spacing.md)
            .screenBackground()
            .navigationTitle("Aceptar")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { dismiss() }
                }
            }
            .onAppear {
                if !allowsCash { metodo = .pagadito }
            }
        }
    }
}
