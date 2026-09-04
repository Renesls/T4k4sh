import SwiftUI

/// Panel de administración.
struct AdminView: View {
    @Environment(AppDependencies.self) private var dependencies

    @State private var viewModel: AdminViewModel?
    @State private var reportToReview: Report?
    @State private var disputeToResolve: PaymentDispute?
    @State private var verificationToReview: StudentVerification?

    var body: some View {
        Group {
            if let viewModel {
                content(viewModel: viewModel)
            } else {
                LoadingStateView()
            }
        }
        .screenBackground()
        .navigationTitle("Administración")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            if viewModel == nil {
                viewModel = AdminViewModel(repository: dependencies.moderation)
            }
        }
        .task { await viewModel?.load() }
        .sheet(item: $reportToReview) { report in
            if let viewModel {
                ReviewReportSheet(report: report) { estado, observacion, retirar in
                    await viewModel.reviewReport(
                        report,
                        estado: estado,
                        observacion: observacion,
                        retirarPublicacion: retirar
                    )
                }
                .presentationDetents([.medium, .large])
            }
        }
        .sheet(item: $disputeToResolve) { dispute in
            if let viewModel {
                ResolveDisputeSheet(dispute: dispute) { decision, resolucion in
                    await viewModel.resolveDispute(
                        dispute,
                        decision: decision,
                        resolucion: resolucion
                    )
                }
                .presentationDetents([.large])
            }
        }
        .sheet(item: $verificationToReview) { verification in
            if let viewModel {
                ReviewStudentVerificationSheet(
                    verification: verification,
                    attachmentRepository: dependencies.attachments
                ) { approve, observacion in
                    await viewModel.reviewStudentVerification(
                        verification,
                        approve: approve,
                        observacion: observacion
                    )
                }
                .presentationDetents([.large])
            }
        }
    }

    @ViewBuilder
    private func content(viewModel: AdminViewModel) -> some View {
        @Bindable var model = viewModel

        switch viewModel.state {
        case .idle, .loading:
            LoadingStateView()

        case let .failed(message):
            ErrorStateView(message: message) { await viewModel.load() }

        case .loaded:
            VStack(spacing: 0) {
                Picker("Sección", selection: $model.section) {
                    ForEach(AdminViewModel.Section.allCases) { section in
                        Text(section.rawValue).tag(section)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, Theme.Spacing.md)
                .padding(.bottom, Theme.Spacing.xs)

                ScrollView {
                    VStack(spacing: Theme.Spacing.md) {
                        if let error = viewModel.actionError {
                            InlineErrorBanner(message: error) { model.actionError = nil }
                        }
                        if let success = viewModel.successMessage {
                            InlineSuccessBanner(message: success)
                        }

                        switch viewModel.section {
                        case .resumen: summarySection(viewModel)
                        case .reportes: reportsSection(viewModel)
                        case .disputas: disputesSection(viewModel)
                        case .verificaciones: verificationsSection(viewModel)
                        }
                    }
                    .padding(Theme.Spacing.md)
                }
                .refreshable { await viewModel.load() }
            }
        }
    }

    // MARK: - Resumen

    @ViewBuilder
    private func summarySection(_ viewModel: AdminViewModel) -> some View {
        if let summary = viewModel.summary {
            LazyVGrid(
                columns: [GridItem(.flexible()), GridItem(.flexible())],
                spacing: Theme.Spacing.sm
            ) {
                metricTile("Usuarios", value: summary.usuarios, icon: "person.3", tint: Theme.Color.primary)
                metricTile("Publicaciones activas", value: summary.publicacionesActivas,
                           icon: "doc.text", tint: Theme.Color.success)
                metricTile("Trabajos asignados", value: summary.trabajosAsignados,
                           icon: "briefcase", tint: Theme.Color.amber)
                metricTile("Reportes pendientes", value: summary.reportesPendientes,
                           icon: "flag", tint: Theme.Color.warning)
                metricTile("Verificaciones pendientes", value: summary.verificacionesPendientes,
                           icon: "graduationcap", tint: Theme.Color.brand)
            }
        }
    }

    private func metricTile(
        _ title: String,
        value: Int,
        icon: String,
        tint: Color
    ) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(tint)
            Text("\(value)")
                .font(Theme.Font.numeric)
                .foregroundStyle(Theme.Color.text)
            Text(title)
                .font(Theme.Font.caption)
                .foregroundStyle(Theme.Color.textMuted)
                .multilineTextAlignment(.leading)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .cardSurface(padding: Theme.Spacing.sm)
    }

    // MARK: - Reportes

    @ViewBuilder
    private func reportsSection(_ viewModel: AdminViewModel) -> some View {
        if viewModel.reports.isEmpty {
            EmptyStateView(
                icon: "checkmark.shield",
                title: "Sin reportes",
                message: "No hay reportes registrados en la plataforma."
            )
        } else {
            ForEach(viewModel.reports) { report in
                VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
                    HStack {
                        Text(report.categoriaLegible)
                            .font(Theme.Font.headline)
                            .foregroundStyle(Theme.Color.text)
                        Spacer(minLength: 0)
                        StatusPill(
                            text: report.estadoLegible,
                            tint: report.pendiente ? Theme.Color.warning : Theme.Color.success
                        )
                    }

                    if let titulo = report.tituloTarea {
                        Text(titulo)
                            .font(Theme.Font.subheadline)
                            .foregroundStyle(Theme.Color.textMuted)
                    }
                    if let descripcion = report.descripcion, !descripcion.isEmpty {
                        Text(descripcion)
                            .font(Theme.Font.caption)
                            .foregroundStyle(Theme.Color.textSoft)
                    }
                    if let correo = report.correoReporta {
                        DetailRow(label: "Reportado por", value: correo, icon: "person")
                    }
                    DetailRow(
                        label: "Fecha",
                        value: DisplayFormatter.dateTime(report.fechaReporte),
                        icon: "calendar"
                    )

                    if report.pendiente {
                        Divider().overlay(Theme.Color.border)
                        Button("Revisar reporte") { reportToReview = report }
                            .font(Theme.Font.footnote.weight(.semibold))
                            .foregroundStyle(Theme.Color.primary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .cardSurface(padding: Theme.Spacing.sm)
            }
        }
    }

    // MARK: - Disputas

    @ViewBuilder
    private func disputesSection(_ viewModel: AdminViewModel) -> some View {
        if viewModel.disputes.isEmpty {
            EmptyStateView(
                icon: "checkmark.seal",
                title: "Sin disputas",
                message: "No hay disputas de pago abiertas."
            )
        } else {
            ForEach(viewModel.disputes) { dispute in
                VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
                    HStack {
                        Text(dispute.motivo)
                            .font(Theme.Font.headline)
                            .foregroundStyle(Theme.Color.text)
                        Spacer(minLength: 0)
                        StatusPill(
                            text: dispute.estadoDisputa.humanizedCode,
                            tint: dispute.resuelta ? Theme.Color.success : Theme.Color.warning
                        )
                    }

                    Text(dispute.descripcion)
                        .font(Theme.Font.subheadline)
                        .foregroundStyle(Theme.Color.textMuted)

                    DetailRow(label: "Solicitado", value: dispute.solucionLegible,
                              icon: "arrow.triangle.branch")
                    DetailRow(label: "Monto", value: DisplayFormatter.money(dispute.montoDisputado),
                              icon: "banknote")
                    DetailRow(label: "Abierta",
                              value: DisplayFormatter.dateTime(dispute.fechaApertura),
                              icon: "calendar")

                    if let resolucion = dispute.resolucion, !resolucion.isEmpty {
                        Divider().overlay(Theme.Color.border)
                        Text("Resolución: \(resolucion)")
                            .font(Theme.Font.caption)
                            .foregroundStyle(Theme.Color.text)
                    } else {
                        Divider().overlay(Theme.Color.border)
                        Button("Resolver disputa") { disputeToResolve = dispute }
                            .font(Theme.Font.footnote.weight(.semibold))
                            .foregroundStyle(Theme.Color.primary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .cardSurface(padding: Theme.Spacing.sm)
            }
        }
    }

    // MARK: - Verificaciones estudiantiles

    @ViewBuilder
    private func verificationsSection(_ viewModel: AdminViewModel) -> some View {
        if viewModel.verifications.isEmpty {
            EmptyStateView(
                icon: "graduationcap",
                title: "Sin verificaciones pendientes",
                message: "Todas las solicitudes de verificación estudiantil están revisadas."
            )
        } else {
            ForEach(viewModel.verifications) { verification in
                VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
                    HStack {
                        Text(verification.correo ?? "Usuario #\(verification.idUsuario)")
                            .font(Theme.Font.headline)
                            .foregroundStyle(Theme.Color.text)
                        Spacer(minLength: 0)
                        StatusPill(text: verification.estadoLegible, tint: Theme.Color.warning)
                    }

                    DetailRow(
                        label: "Solicitada",
                        value: DisplayFormatter.dateTime(verification.fechaSolicitud),
                        icon: "calendar"
                    )
                    DetailRow(
                        label: "Archivos",
                        value: "\(verification.archivos.count)",
                        icon: "paperclip"
                    )

                    Divider().overlay(Theme.Color.border)
                    Button("Revisar solicitud") { verificationToReview = verification }
                        .font(Theme.Font.footnote.weight(.semibold))
                        .foregroundStyle(Theme.Color.primary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .cardSurface(padding: Theme.Spacing.sm)
            }
        }
    }
}

// MARK: - Hojas de revisión

/// Revisión de un reporte.
struct ReviewReportSheet: View {
    let report: Report
    let onSubmit: (Domain.EstadoRevisionReporte, String, Bool) async -> Bool

    @Environment(\.dismiss) private var dismiss
    @State private var estado: Domain.EstadoRevisionReporte = .resuelto
    @State private var observacion = ""
    @State private var retirarPublicacion = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Reporte") {
                    LabeledContent("Categoría", value: report.categoriaLegible)
                    if let titulo = report.tituloTarea {
                        LabeledContent("Oportunidad", value: titulo)
                    }
                    if let descripcion = report.descripcion, !descripcion.isEmpty {
                        Text(descripcion)
                            .font(Theme.Font.subheadline)
                            .foregroundStyle(Theme.Color.textMuted)
                    }
                }

                Section("Resultado") {
                    Picker("Estado", selection: $estado) {
                        ForEach(Domain.EstadoRevisionReporte.allCases) { option in
                            Text(option.label).tag(option)
                        }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }

                Section {
                    Toggle("Retirar la publicación reportada", isOn: $retirarPublicacion)
                } footer: {
                    Text("Si lo activas, la oportunidad se cancelará y dejará de estar visible.")
                }

                Section {
                    TextEditor(text: $observacion)
                        .frame(minHeight: 100)
                } header: {
                    Text("Observación")
                } footer: {
                    Text("Opcional. Máximo 700 caracteres.")
                }

                Section {
                    AsyncButton {
                        if await onSubmit(estado, observacion, retirarPublicacion) { dismiss() }
                    } label: {
                        Text("Guardar revisión").frame(maxWidth: .infinity)
                    }
                    .disabled(observacion.count > 700)
                }
            }
            .navigationTitle("Revisar reporte")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { dismiss() }
                }
            }
        }
    }
}

/// Resolución de una disputa de pago.
struct ResolveDisputeSheet: View {
    let dispute: PaymentDispute
    let onSubmit: (Domain.DecisionDisputa, String) async -> Bool

    @Environment(\.dismiss) private var dismiss
    @State private var decision: Domain.DecisionDisputa = .liberarEstudiante
    @State private var resolucion = ""

    /// `ResolvePaymentDisputeRequest.resolucion`: entre 10 y 1000 caracteres.
    private var isValid: Bool {
        (10...1_000).contains(resolucion.trimmingCharacters(in: .whitespaces).count)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Disputa") {
                    LabeledContent("Motivo", value: dispute.motivo)
                    LabeledContent("Solicitado", value: dispute.solucionLegible)
                    LabeledContent(
                        "Monto",
                        value: DisplayFormatter.money(dispute.montoDisputado)
                    )
                    Text(dispute.descripcion)
                        .font(Theme.Font.subheadline)
                        .foregroundStyle(Theme.Color.textMuted)
                }

                Section {
                    Picker("Decisión", selection: $decision) {
                        ForEach(Domain.DecisionDisputa.allCases) { option in
                            Text(option.label).tag(option)
                        }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                } header: {
                    Text("Decisión")
                } footer: {
                    Text("Liberar transfiere el monto retenido al estudiante. "
                         + "Reembolsar lo devuelve al cliente.")
                }

                Section {
                    TextEditor(text: $resolucion)
                        .frame(minHeight: 120)
                } header: {
                    Text("Resolución")
                } footer: {
                    Text("\(resolucion.count)/1000 · mínimo 10 caracteres. "
                         + "Queda registrada y visible para ambas partes.")
                }

                Section {
                    AsyncButton {
                        if await onSubmit(decision, resolucion) { dismiss() }
                    } label: {
                        Text("Resolver disputa").frame(maxWidth: .infinity)
                    }
                    .disabled(!isValid)
                }
            }
            .navigationTitle("Resolver disputa")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { dismiss() }
                }
            }
        }
    }
}

/// Revisión de una verificación estudiantil, con acceso a los comprobantes.
struct ReviewStudentVerificationSheet: View {
    let verification: StudentVerification
    let attachmentRepository: AttachmentRepository
    let onSubmit: (Bool, String) async -> Bool

    @Environment(\.dismiss) private var dismiss
    @State private var observacion = ""

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Theme.Spacing.md) {
                    VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
                        SectionHeader("Solicitud")
                        DetailRow(
                            label: "Correo",
                            value: verification.correo ?? "—",
                            icon: "envelope"
                        )
                        DetailRow(
                            label: "Solicitada",
                            value: DisplayFormatter.dateTime(verification.fechaSolicitud),
                            icon: "calendar"
                        )
                    }
                    .cardSurface()

                    VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
                        SectionHeader(
                            "Comprobantes",
                            subtitle: verification.archivos.isEmpty
                                ? "La solicitud no tiene archivos."
                                : "Toca un archivo para descargarlo y revisarlo."
                        )
                        if !verification.archivos.isEmpty {
                            AttachmentList(
                                attachments: verification.archivos,
                                repository: attachmentRepository
                            )
                        }
                    }
                    .cardSurface()

                    LabeledField(
                        label: "Observación",
                        hint: "Opcional. Máximo 300 caracteres."
                    ) {
                        TextEditor(text: $observacion)
                            .frame(minHeight: 90)
                            .scrollContentBackground(.hidden)
                    }

                    VStack(spacing: Theme.Spacing.sm) {
                        AsyncButton {
                            if await onSubmit(true, observacion) { dismiss() }
                        } label: {
                            Label("Aprobar verificación", systemImage: "checkmark.seal")
                        }
                        .buttonStyle(PrimaryButtonStyle())
                        .disabled(observacion.count > 300)

                        AsyncButton(role: .destructive) {
                            if await onSubmit(false, observacion) { dismiss() }
                        } label: {
                            Label("Rechazar", systemImage: "xmark.seal")
                        }
                        .buttonStyle(DestructiveButtonStyle())
                        .disabled(observacion.count > 300)
                    }
                }
                .padding(Theme.Spacing.md)
            }
            .screenBackground()
            .navigationTitle("Verificación estudiantil")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { dismiss() }
                }
            }
        }
    }
}
