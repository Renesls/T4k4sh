import SwiftUI

/// Detalle de un trabajo asignado: pago, entregas, revisiones y adjuntos.
struct JobDetailView: View {
    let job: Job
    let task: TaskItem?

    @Environment(AppDependencies.self) private var dependencies
    @Environment(SessionStore.self) private var session

    @State private var viewModel: JobDetailViewModel?
    @State private var showDeliveryComposer = false
    @State private var showDisputeSheet = false
    @State private var deliveryForChanges: Delivery?
    @State private var confirmApprove: Delivery?

    var body: some View {
        Group {
            if let viewModel {
                content(viewModel: viewModel)
            } else {
                LoadingStateView()
            }
        }
        .screenBackground()
        .navigationTitle("Trabajo")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            guard viewModel == nil, let user = session.user else { return }
            viewModel = JobDetailViewModel(
                job: job,
                task: task,
                marketplace: dependencies.marketplace,
                finance: dependencies.finance,
                attachments: dependencies.attachments,
                currentUserId: user.idUsuario
            )
        }
        .task { await viewModel?.load() }
    }

    @ViewBuilder
    private func content(viewModel: JobDetailViewModel) -> some View {
        @Bindable var model = viewModel

        ScrollView {
            VStack(alignment: .leading, spacing: Theme.Spacing.md) {
                if let error = viewModel.actionError {
                    InlineErrorBanner(message: error) { viewModel.actionError = nil }
                }
                if let success = viewModel.successMessage {
                    InlineSuccessBanner(message: success)
                }

                summaryCard(viewModel: viewModel)

                if let payment = viewModel.payment {
                    paymentCard(payment: payment, viewModel: viewModel)
                }

                deliveriesSection(viewModel: viewModel)

                if !viewModel.attachments.isEmpty {
                    VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
                        SectionHeader("Archivos del trabajo")
                        AttachmentList(
                            attachments: viewModel.attachments,
                            repository: dependencies.attachments
                        )
                    }
                    .cardSurface()
                }
            }
            .padding(Theme.Spacing.md)
        }
        .refreshable { await viewModel.load() }
        .sheet(isPresented: $showDeliveryComposer) {
            DeliveryComposerSheet { descripcion in
                await viewModel.createDelivery(descripcion: descripcion)
            }
            .presentationDetents([.medium, .large])
        }
        .sheet(item: $deliveryForChanges) { delivery in
            RequestChangesSheet { observacion in
                await viewModel.requestChanges(delivery, observacion: observacion)
            }
            .presentationDetents([.medium])
        }
        .sheet(isPresented: $showDisputeSheet) {
            DisputeComposerSheet { motivo, descripcion, solucion in
                await viewModel.openDispute(
                    motivo: motivo,
                    descripcion: descripcion,
                    solucion: solucion
                )
            }
            .presentationDetents([.large])
        }
        .sheet(item: $model.checkoutDestination) { destination in
            SafariView(url: destination.url) {
                Task { await viewModel.refreshPaymentAfterCheckout() }
            }
            .ignoresSafeArea()
        }
        .alert(
            "¿Aprobar la entrega?",
            isPresented: Binding(
                get: { confirmApprove != nil },
                set: { if !$0 { confirmApprove = nil } }
            )
        ) {
            Button("Cancelar", role: .cancel) { confirmApprove = nil }
            Button("Aprobar") {
                if let delivery = confirmApprove {
                    Task { await viewModel.approveDelivery(delivery) }
                }
                confirmApprove = nil
            }
        } message: {
            Text("Al aprobar, el monto retenido pasa al balance disponible del estudiante. "
                 + "Esta acción no se puede deshacer.")
        }
    }

    // MARK: - Secciones

    private func summaryCard(viewModel: JobDetailViewModel) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(viewModel.task?.titulo ?? "Oportunidad #\(viewModel.job.idTarea)")
                        .font(Theme.Font.title)
                        .foregroundStyle(Theme.Color.text)

                    Text(viewModel.isStudent ? "Eres el estudiante asignado" : "Eres el cliente")
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textMuted)
                }
                Spacer(minLength: Theme.Spacing.xs)
                StatusPill(
                    text: viewModel.job.estadoLegible,
                    tint: StatusTint.job(viewModel.job.estadoTrabajo)
                )
            }

            Divider().overlay(Theme.Color.border)

            if let inicio = viewModel.job.fechaInicio {
                DetailRow(
                    label: "Inicio",
                    value: DisplayFormatter.dateTime(inicio),
                    icon: "play.circle"
                )
            }
            if let entrega = viewModel.job.fechaEntregaEsperada {
                DetailRow(
                    label: "Entrega esperada",
                    value: DisplayFormatter.dateTime(entrega),
                    icon: "flag.checkered"
                )
            }
            if let estudiante = viewModel.job.estudiante {
                DetailRow(
                    label: "Estudiante",
                    value: estudiante.nombreCompleto,
                    icon: "person"
                )
            }

            if let taskId = viewModel.task?.idTarea {
                NavigationLink {
                    TaskDetailView(taskId: taskId)
                } label: {
                    Label("Ver la oportunidad", systemImage: "arrow.up.right.square")
                        .font(Theme.Font.footnote.weight(.semibold))
                        .foregroundStyle(Theme.Color.primary)
                }
                .buttonStyle(.plain)
            }
        }
        .cardSurface()
    }

    private func paymentCard(payment: Payment, viewModel: JobDetailViewModel) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            SectionHeader(
                "Pago",
                subtitle: payment.esEfectivo ? "Efectivo" : "Pago protegido"
            )

            HStack {
                StatusPill(
                    text: payment.estadoLegible,
                    tint: StatusTint.payment(payment.estadoPago)
                )
                if payment.esSandbox {
                    StatusPill(text: "Sandbox", tint: Theme.Color.textMuted)
                }
                Spacer(minLength: 0)
            }

            Divider().overlay(Theme.Color.border)

            DetailRow(
                label: viewModel.isStudent ? "Recibirás" : "Monto al estudiante",
                value: DisplayFormatter.money(
                    payment.montoEstudiante,
                    currencyCode: payment.monedaCobro
                ),
                icon: "person.badge.key"
            )

            if let comision = payment.comisionPlataforma, comision > 0 {
                DetailRow(
                    label: "Comisión de plataforma",
                    value: DisplayFormatter.money(comision, currencyCode: payment.monedaCobro),
                    icon: "percent"
                )
            }

            if viewModel.isClient {
                DetailRow(
                    label: "Total a pagar",
                    value: DisplayFormatter.money(
                        payment.montoTotalCliente,
                        currencyCode: payment.monedaCobro
                    ),
                    icon: "creditcard",
                    valueColor: Theme.Color.primaryDark
                )
            }

            if let expira = payment.fechaExpiracion,
               payment.estadoPago == Domain.EstadoPago.pendientePago {
                DetailRow(
                    label: "El enlace vence",
                    value: DisplayFormatter.dateTime(expira),
                    icon: "clock.badge.exclamationmark"
                )
            }

            paymentActions(payment: payment, viewModel: viewModel)
        }
        .cardSurface()
    }

    @ViewBuilder
    private func paymentActions(payment: Payment, viewModel: JobDetailViewModel) -> some View {
        VStack(spacing: Theme.Spacing.xs) {
            if viewModel.canPay {
                AsyncButton { await viewModel.startCheckout() } label: {
                    Label("Pagar ahora", systemImage: "lock.shield")
                }
                .buttonStyle(PrimaryButtonStyle())

                Text("Se abrirá Pagadito en una ventana segura. Al volver actualizamos el estado.")
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textSoft)
                    .multilineTextAlignment(.center)
            }

            if viewModel.canConfirmCash {
                AsyncButton { await viewModel.confirmCashReceipt() } label: {
                    Label(
                        viewModel.isClient ? "Confirmé la entrega del efectivo" : "Recibí el efectivo",
                        systemImage: "banknote"
                    )
                }
                .buttonStyle(SecondaryButtonStyle())

                Text("Ambas partes deben confirmar para dar el pago por recibido.")
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textSoft)
                    .multilineTextAlignment(.center)
            }

            HStack(spacing: Theme.Spacing.sm) {
                if payment.esProtegido {
                    AsyncButton { await viewModel.refreshPayment() } label: {
                        Label("Actualizar estado", systemImage: "arrow.clockwise")
                            .font(Theme.Font.footnote.weight(.semibold))
                            .foregroundStyle(Theme.Color.primary)
                    }
                }

                Spacer(minLength: 0)

                if viewModel.canOpenDispute {
                    Button {
                        showDisputeSheet = true
                    } label: {
                        Label("Abrir disputa", systemImage: "exclamationmark.bubble")
                            .font(Theme.Font.footnote.weight(.semibold))
                            .foregroundStyle(Theme.Color.warning)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    @ViewBuilder
    private func deliveriesSection(viewModel: JobDetailViewModel) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            SectionHeader("Entregas")

            switch viewModel.deliveries {
            case .idle, .loading:
                ProgressView().tint(Theme.Color.primary).frame(maxWidth: .infinity)

            case let .failed(message):
                InlineErrorBanner(message: message)

            case let .loaded(deliveries):
                if deliveries.isEmpty {
                    Text(viewModel.isStudent
                         ? "Todavía no registras ninguna entrega."
                         : "El estudiante todavía no ha registrado entregas.")
                        .font(Theme.Font.subheadline)
                        .foregroundStyle(Theme.Color.textMuted)
                } else {
                    ForEach(deliveries) { delivery in
                        DeliveryRow(
                            delivery: delivery,
                            attachmentRepository: dependencies.attachments,
                            canReview: viewModel.canReviewDelivery && delivery.id == viewModel.latestDelivery?.id,
                            canAttach: viewModel.isStudent && !delivery.aprobada,
                            onApprove: { confirmApprove = delivery },
                            onRequestChanges: { deliveryForChanges = delivery },
                            onComment: { texto in
                                await viewModel.comment(delivery, comentario: texto)
                            },
                            onAttach: { prepared in
                                await viewModel.uploadDeliveryAttachment(
                                    delivery,
                                    prepared: prepared
                                )
                            }
                        )
                    }
                }
            }

            if viewModel.canCreateDelivery {
                Button("Registrar entrega") { showDeliveryComposer = true }
                    .buttonStyle(PrimaryButtonStyle())
                    .padding(.top, Theme.Spacing.xs)
            }
        }
        .cardSurface()
    }
}

/// Una entrega con sus comentarios, revisiones y acciones.
struct DeliveryRow: View {
    let delivery: Delivery
    let attachmentRepository: AttachmentRepository
    let canReview: Bool
    let canAttach: Bool
    let onApprove: () -> Void
    let onRequestChanges: () -> Void
    let onComment: (String) async -> Bool
    let onAttach: (PreparedAttachment) async -> Void

    @State private var comentario = ""
    @State private var attachments: [Attachment] = []

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
            HStack {
                StatusPill(
                    text: delivery.estadoLegible,
                    tint: StatusTint.delivery(delivery.estadoEntrega)
                )
                Spacer(minLength: 0)
                Text(DisplayFormatter.dateTime(delivery.fechaEntrega))
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textSoft)
            }

            Text(delivery.descripcionEntrega)
                .font(Theme.Font.subheadline)
                .foregroundStyle(Theme.Color.text)

            if !attachments.isEmpty {
                AttachmentList(attachments: attachments, repository: attachmentRepository)
            }

            if canAttach {
                AttachmentPickerMenu(title: "Adjuntar a la entrega") { prepared in
                    await onAttach(prepared)
                    await loadAttachments()
                }
            }

            if !delivery.revisiones.isEmpty {
                ForEach(delivery.revisiones) { revision in
                    HStack(alignment: .top, spacing: Theme.Spacing.xs) {
                        Image(systemName: "checkmark.seal")
                            .font(.caption)
                            .foregroundStyle(Theme.Color.textSoft)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(revision.resultadoRevision.humanizedCode)
                                .font(Theme.Font.caption.weight(.semibold))
                                .foregroundStyle(Theme.Color.text)
                            if let observacion = revision.observacion, !observacion.isEmpty {
                                Text(observacion)
                                    .font(Theme.Font.caption)
                                    .foregroundStyle(Theme.Color.textMuted)
                            }
                        }
                        Spacer(minLength: 0)
                    }
                }
            }

            if !delivery.comentarios.isEmpty {
                Divider().overlay(Theme.Color.border)
                ForEach(delivery.comentarios) { entrada in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(entrada.comentario)
                            .font(Theme.Font.caption)
                            .foregroundStyle(Theme.Color.text)
                        Text(DisplayFormatter.relative(entrada.fechaComentario))
                            .font(Theme.Font.caption)
                            .foregroundStyle(Theme.Color.textSoft)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }

            HStack(spacing: Theme.Spacing.xs) {
                TextField("Escribe un comentario", text: $comentario, axis: .vertical)
                    .font(Theme.Font.caption)
                    .lineLimit(1...3)
                    .padding(Theme.Spacing.xs)
                    .background(
                        RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                            .fill(Theme.Color.surface)
                    )

                AsyncButton {
                    guard !comentario.trimmingCharacters(in: .whitespaces).isEmpty else { return }
                    if await onComment(comentario) { comentario = "" }
                } label: {
                    Image(systemName: "paperplane.fill")
                        .foregroundStyle(Theme.Color.primary)
                }
                .disabled(comentario.trimmingCharacters(in: .whitespaces).count < 2)
            }

            if canReview {
                Divider().overlay(Theme.Color.border)
                HStack(spacing: Theme.Spacing.sm) {
                    Button("Solicitar cambios", action: onRequestChanges)
                        .font(Theme.Font.footnote.weight(.semibold))
                        .foregroundStyle(Theme.Color.warning)

                    Spacer(minLength: 0)

                    Button("Aprobar entrega", action: onApprove)
                        .font(Theme.Font.footnote.weight(.semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, Theme.Spacing.md)
                        .padding(.vertical, Theme.Spacing.xs)
                        .background(Capsule().fill(Theme.Color.success))
                }
            }
        }
        .padding(Theme.Spacing.sm)
        .background(
            RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous)
                .fill(Theme.Color.surfaceVariant)
        )
        .task { await loadAttachments() }
    }

    private func loadAttachments() async {
        attachments = (try? await attachmentRepository.deliveryAttachments(
            deliveryId: delivery.idEntrega
        )) ?? []
    }
}

/// Formulario para registrar una entrega.
struct DeliveryComposerSheet: View {
    let onSubmit: (String) async -> Bool

    @Environment(\.dismiss) private var dismiss
    @State private var descripcion = ""

    /// `CreateDeliveryRequest` exige entre 10 y 1000 caracteres.
    private var isValid: Bool {
        let count = descripcion.trimmingCharacters(in: .whitespaces).count
        return (10...1_000).contains(count)
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: Theme.Spacing.md) {
                Text("Describe qué estás entregando y cómo acceder al resultado.")
                    .font(Theme.Font.subheadline)
                    .foregroundStyle(Theme.Color.textMuted)

                TextEditor(text: $descripcion)
                    .frame(minHeight: 160)
                    .padding(Theme.Spacing.xs)
                    .background(
                        RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                            .fill(Theme.Color.surface)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                            .stroke(Theme.Color.border, lineWidth: 1)
                    )

                Text("\(descripcion.count)/1000 · mínimo 10 caracteres")
                    .font(Theme.Font.caption)
                    .foregroundStyle(isValid ? Theme.Color.textSoft : Theme.Color.warning)

                Spacer()

                AsyncButton {
                    if await onSubmit(descripcion) { dismiss() }
                } label: {
                    Text("Registrar entrega")
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(!isValid)
            }
            .padding(Theme.Spacing.md)
            .screenBackground()
            .navigationTitle("Nueva entrega")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { dismiss() }
                }
            }
        }
    }
}

/// Formulario para solicitar cambios en una entrega.
struct RequestChangesSheet: View {
    let onSubmit: (String) async -> Bool

    @Environment(\.dismiss) private var dismiss
    @State private var observacion = ""

    /// `RequestDeliveryChangesRequest` exige entre 10 y 700 caracteres.
    private var isValid: Bool {
        let count = observacion.trimmingCharacters(in: .whitespaces).count
        return (10...700).contains(count)
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: Theme.Spacing.md) {
                Text("Explica con detalle qué debe corregirse para que el estudiante pueda reenviar la entrega.")
                    .font(Theme.Font.subheadline)
                    .foregroundStyle(Theme.Color.textMuted)

                TextEditor(text: $observacion)
                    .frame(minHeight: 140)
                    .padding(Theme.Spacing.xs)
                    .background(
                        RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                            .fill(Theme.Color.surface)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: Theme.Radius.sm, style: .continuous)
                            .stroke(Theme.Color.border, lineWidth: 1)
                    )

                Text("\(observacion.count)/700 · mínimo 10 caracteres")
                    .font(Theme.Font.caption)
                    .foregroundStyle(isValid ? Theme.Color.textSoft : Theme.Color.warning)

                Spacer()

                AsyncButton {
                    if await onSubmit(observacion) { dismiss() }
                } label: {
                    Text("Solicitar cambios")
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(!isValid)
            }
            .padding(Theme.Spacing.md)
            .screenBackground()
            .navigationTitle("Solicitar cambios")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { dismiss() }
                }
            }
        }
    }
}
