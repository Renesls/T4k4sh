import MapKit
import SwiftUI

/// Detalle de una oportunidad, con postulación, adjuntos y acciones del dueño.
struct TaskDetailView: View {
    let taskId: Int
    var onTaskChanged: ((TaskItem) -> Void)?

    @Environment(AppDependencies.self) private var dependencies
    @Environment(SessionStore.self) private var session

    @State private var viewModel: TaskDetailViewModel?
    @State private var showApplySheet = false
    @State private var showReportSheet = false
    @State private var showEditor = false
    @State private var confirmCancel = false

    var body: some View {
        Group {
            if let viewModel {
                content(viewModel: viewModel)
            } else {
                LoadingStateView()
            }
        }
        .screenBackground()
        .navigationTitle("Oportunidad")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear(perform: buildViewModel)
        .task { await viewModel?.load() }
    }

    private func buildViewModel() {
        guard viewModel == nil, let user = session.user else { return }
        viewModel = TaskDetailViewModel(
            taskId: taskId,
            marketplace: dependencies.marketplace,
            attachments: dependencies.attachments,
            moderation: dependencies.moderation,
            currentUserId: user.idUsuario
        )
    }

    @ViewBuilder
    private func content(viewModel: TaskDetailViewModel) -> some View {
        switch viewModel.state {
        case .idle, .loading:
            LoadingStateView()

        case let .failed(message):
            ErrorStateView(message: message) { await viewModel.load() }

        case let .loaded(task):
            ScrollView {
                VStack(alignment: .leading, spacing: Theme.Spacing.md) {
                    if let error = viewModel.actionError {
                        InlineErrorBanner(message: error) { viewModel.actionError = nil }
                    }
                    if let success = viewModel.successMessage {
                        InlineSuccessBanner(message: success)
                    }

                    header(task: task, viewModel: viewModel)
                    descriptionCard(task: task)

                    if let coordinates = task.coordenadas {
                        locationCard(task: task, coordinates: coordinates)
                    }

                    attachmentsCard(task: task, viewModel: viewModel)

                    if let client = task.cliente {
                        clientCard(client)
                    }

                    if let application = viewModel.myApplication {
                        myApplicationCard(application)
                    }

                    actions(task: task, viewModel: viewModel)
                }
                .padding(Theme.Spacing.md)
            }
            .refreshable { await viewModel.load() }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        if viewModel.isOwner {
                            Button {
                                showEditor = true
                            } label: {
                                Label("Editar oportunidad", systemImage: "pencil")
                            }
                            .disabled(!task.estaPublicada)

                            Button(role: .destructive) {
                                confirmCancel = true
                            } label: {
                                Label("Cancelar oportunidad", systemImage: "xmark.circle")
                            }
                            .disabled(!task.estaPublicada)
                        } else {
                            Button {
                                showReportSheet = true
                            } label: {
                                Label("Reportar", systemImage: "flag")
                            }
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                    .accessibilityLabel("Más acciones")
                }
            }
            .sheet(isPresented: $showApplySheet) {
                ApplyToTaskSheet(task: task) { mensaje, precio in
                    await viewModel.apply(mensaje: mensaje, precio: precio)
                }
                .presentationDetents([.medium, .large])
            }
            .sheet(isPresented: $showReportSheet) {
                ReportTaskSheet { categoria, descripcion in
                    await viewModel.report(categoria: categoria, descripcion: descripcion)
                }
                .presentationDetents([.medium])
            }
            .sheet(isPresented: $showEditor) {
                NavigationStack {
                    TaskComposerView(mode: .edit(task)) { updated in
                        viewModel.apply(updated: updated)
                        onTaskChanged?(updated)
                    }
                }
            }
            .alert("¿Cancelar esta oportunidad?", isPresented: $confirmCancel) {
                Button("No, volver", role: .cancel) {}
                Button("Sí, cancelar", role: .destructive) {
                    Task {
                        if let cancelled = await viewModel.cancelTask() {
                            onTaskChanged?(cancelled)
                        }
                    }
                }
            } message: {
                Text("Las postulaciones pendientes quedarán canceladas. Esta acción no se puede deshacer.")
            }
        }
    }

    private func header(task: TaskItem, viewModel: TaskDetailViewModel) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(task.titulo)
                        .font(Theme.Font.title)
                        .foregroundStyle(Theme.Color.text)

                    Text(viewModel.categoryName)
                        .font(Theme.Font.footnote)
                        .foregroundStyle(Theme.Color.textMuted)
                }
                Spacer(minLength: Theme.Spacing.xs)
            }

            HStack(spacing: Theme.Spacing.xs) {
                if task.esTareaRapida {
                    StatusPill(text: "Tarea rápida", tint: Theme.Color.warning, icon: "bolt.fill")
                }
                StatusPill(text: task.estadoLegible, tint: StatusTint.task(task.estadoTarea))
                if let modalidad = task.modalidadResuelta {
                    StatusPill(text: modalidad.label, tint: Theme.Color.textMuted, icon: modalidad.icon)
                }
                Spacer(minLength: 0)
            }

            Divider().overlay(Theme.Color.border)

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Presupuesto")
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textMuted)
                    Text(DisplayFormatter.money(task.presupuesto))
                        .font(Theme.Font.numeric)
                        .foregroundStyle(Theme.Color.primaryDark)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Publicada")
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textMuted)
                    Text(DisplayFormatter.relative(task.fechaPublicacion))
                        .font(Theme.Font.subheadline)
                        .foregroundStyle(Theme.Color.text)
                }
            }
        }
        .cardSurface()
    }

    private func descriptionCard(task: TaskItem) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
            SectionHeader("Descripción")
            Text(task.descripcion)
                .font(Theme.Font.body)
                .foregroundStyle(Theme.Color.text)
                .frame(maxWidth: .infinity, alignment: .leading)

            if task.fechaLimitePostulacion != nil || task.fechaLimite != nil {
                Divider().overlay(Theme.Color.border)
                if let limite = task.fechaLimitePostulacion {
                    DetailRow(
                        label: "Cierre de postulaciones",
                        value: DisplayFormatter.dateTime(limite),
                        icon: "calendar.badge.clock"
                    )
                }
                if let entrega = task.fechaLimite {
                    DetailRow(
                        label: "Fecha límite de entrega",
                        value: DisplayFormatter.dateTime(entrega),
                        icon: "flag.checkered"
                    )
                }
            }
        }
        .cardSurface()
    }

    private func locationCard(
        task: TaskItem,
        coordinates: (latitude: Double, longitude: Double)
    ) -> some View {
        let coordinate = CLLocationCoordinate2D(
            latitude: coordinates.latitude,
            longitude: coordinates.longitude
        )

        return VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
            SectionHeader("Ubicación")

            if let direccion = task.direccionReferencia, !direccion.isEmpty {
                Text(direccion)
                    .font(Theme.Font.subheadline)
                    .foregroundStyle(Theme.Color.textMuted)
            }

            Map(initialPosition: .region(
                MKCoordinateRegion(
                    center: coordinate,
                    span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
                )
            )) {
                Marker(task.titulo, coordinate: coordinate)
                    .tint(Theme.Color.primary)
            }
            .frame(height: 180)
            .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous))
            .allowsHitTesting(false)

            Button {
                openInMaps(coordinate: coordinate, name: task.titulo)
            } label: {
                Label("Abrir en Mapas", systemImage: "arrow.triangle.turn.up.right.circle")
                    .font(Theme.Font.footnote.weight(.semibold))
            }
            .buttonStyle(.plain)
            .foregroundStyle(Theme.Color.primary)
        }
        .cardSurface()
    }

    private func attachmentsCard(
        task: TaskItem,
        viewModel: TaskDetailViewModel
    ) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            SectionHeader(
                "Archivos",
                subtitle: viewModel.attachments.isEmpty
                    ? "Esta oportunidad no tiene archivos adjuntos."
                    : "\(viewModel.attachments.count) archivo(s)"
            )

            if !viewModel.attachments.isEmpty {
                AttachmentList(
                    attachments: viewModel.attachments,
                    repository: dependencies.attachments
                )
            }

            if viewModel.isOwner && task.estaPublicada {
                AttachmentPickerMenu(title: "Adjuntar archivo") { prepared in
                    await viewModel.uploadAttachment(prepared)
                }
            }
        }
        .cardSurface()
    }

    private func clientCard(_ client: PublicIdentity) -> some View {
        NavigationLink {
            PublicProfileView(username: client.nombreUsuario ?? "")
        } label: {
            HStack(spacing: Theme.Spacing.sm) {
                InitialsAvatar(
                    initials: client.iniciales,
                    size: 46,
                    verified: client.estudianteVerificado
                )
                VStack(alignment: .leading, spacing: 2) {
                    Text(client.nombreCompleto)
                        .font(Theme.Font.headline)
                        .foregroundStyle(Theme.Color.text)
                    if !client.arroba.isEmpty {
                        Text(client.arroba)
                            .font(Theme.Font.caption)
                            .foregroundStyle(Theme.Color.primary)
                    }
                    if let universidad = client.nombreUniversidad {
                        Text(universidad)
                            .font(Theme.Font.caption)
                            .foregroundStyle(Theme.Color.textMuted)
                            .lineLimit(1)
                    }
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Theme.Color.textSoft)
            }
            .cardSurface()
        }
        .buttonStyle(.plain)
        .disabled((client.nombreUsuario ?? "").isEmpty)
    }

    private func myApplicationCard(_ application: Application) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
            SectionHeader("Tu postulación")

            StatusPill(
                text: application.estadoLegible,
                tint: StatusTint.application(application.estadoPostulacion)
            )

            if let mensaje = application.mensaje, !mensaje.isEmpty {
                Text(mensaje)
                    .font(Theme.Font.subheadline)
                    .foregroundStyle(Theme.Color.textMuted)
            }

            if let precio = application.precioPropuesto {
                DetailRow(
                    label: "Precio propuesto",
                    value: DisplayFormatter.money(precio),
                    icon: "tag"
                )
            }

            DetailRow(
                label: "Enviada",
                value: DisplayFormatter.dateTime(application.fechaPostulacion),
                icon: "clock"
            )
        }
        .cardSurface()
    }

    @ViewBuilder
    private func actions(task: TaskItem, viewModel: TaskDetailViewModel) -> some View {
        VStack(spacing: Theme.Spacing.sm) {
            if viewModel.isOwner {
                NavigationLink {
                    TaskApplicationsView(task: task)
                } label: {
                    Text("Ver postulaciones")
                        .font(Theme.Font.headline)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(minHeight: 50)
                        .background(
                            RoundedRectangle(cornerRadius: Theme.Radius.md, style: .continuous)
                                .fill(Theme.Color.primary)
                        )
                }
                .buttonStyle(.plain)
            } else if task.esTareaRapida && task.estaPublicada {
                AsyncButton {
                    _ = await viewModel.claimQuickTask()
                } label: {
                    Label("Reclamar tarea rápida", systemImage: "bolt.fill")
                }
                .buttonStyle(PrimaryButtonStyle())

                Text("Al reclamarla queda asignada a tu nombre de inmediato y tienes "
                     + "\(Domain.QuickTask.deliveryHours) horas para entregarla.")
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textSoft)
                    .multilineTextAlignment(.center)
            } else if viewModel.canApply {
                Button("Postularme") { showApplySheet = true }
                    .buttonStyle(PrimaryButtonStyle())
            } else if viewModel.myApplication != nil {
                Text("Ya te postulaste a esta oportunidad.")
                    .font(Theme.Font.footnote)
                    .foregroundStyle(Theme.Color.textMuted)
            } else if !task.estaPublicada {
                Text("Esta oportunidad ya no acepta postulaciones.")
                    .font(Theme.Font.footnote)
                    .foregroundStyle(Theme.Color.textMuted)
            }
        }
    }

    private func openInMaps(coordinate: CLLocationCoordinate2D, name: String) {
        let placemark = MKPlacemark(coordinate: coordinate)
        let item = MKMapItem(placemark: placemark)
        item.name = name
        item.openInMaps(launchOptions: [
            MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeDriving,
        ])
    }
}

/// Formulario de postulación.
struct ApplyToTaskSheet: View {
    let task: TaskItem
    let onSubmit: (String, Decimal?) async -> Bool

    @Environment(\.dismiss) private var dismiss
    @State private var mensaje = ""
    @State private var precioTexto = ""

    private var precio: Decimal? { Validation.decimal(from: precioTexto) }

    private var precioError: String? {
        guard !precioTexto.isEmpty else { return nil }
        guard let precio else { return "Escribe un monto válido." }
        return precio < 0 ? "El monto no puede ser negativo." : nil
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Theme.Spacing.md) {
                    Text(task.titulo)
                        .font(Theme.Font.headline)
                        .foregroundStyle(Theme.Color.text)

                    DetailRow(
                        label: "Presupuesto publicado",
                        value: DisplayFormatter.money(task.presupuesto),
                        icon: "tag"
                    )

                    LabeledField(
                        label: "Mensaje",
                        hint: "Opcional. Máximo 500 caracteres."
                    ) {
                        TextEditor(text: $mensaje)
                            .frame(minHeight: 120)
                            .scrollContentBackground(.hidden)
                    }

                    LabeledField(
                        label: "Precio propuesto",
                        hint: "Opcional. Déjalo vacío para aceptar el presupuesto publicado.",
                        error: precioError
                    ) {
                        HStack {
                            Text("C$").foregroundStyle(Theme.Color.textMuted)
                            TextField("0.00", text: $precioTexto)
                                .keyboardType(.decimalPad)
                        }
                    }

                    Spacer(minLength: Theme.Spacing.md)

                    AsyncButton {
                        guard precioError == nil else { return }
                        if await onSubmit(mensaje, precio) { dismiss() }
                    } label: {
                        Text("Enviar postulación")
                    }
                    .buttonStyle(PrimaryButtonStyle())
                    .disabled(mensaje.count > 500 || precioError != nil)
                }
                .padding(Theme.Spacing.md)
            }
            .scrollDismissesKeyboard(.interactively)
            .screenBackground()
            .navigationTitle("Postularme")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { dismiss() }
                }
            }
        }
    }
}

/// Formulario de reporte de una oportunidad.
struct ReportTaskSheet: View {
    let onSubmit: (Domain.CategoriaReporte, String) async -> Bool

    @Environment(\.dismiss) private var dismiss
    @State private var categoria: Domain.CategoriaReporte = .contenidoInapropiado
    @State private var descripcion = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("Motivo") {
                    Picker("Categoría", selection: $categoria) {
                        ForEach(Domain.CategoriaReporte.allCases) { option in
                            Text(option.label).tag(option)
                        }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }

                Section {
                    TextEditor(text: $descripcion)
                        .frame(minHeight: 100)
                } header: {
                    Text("Descripción")
                } footer: {
                    Text("Opcional. Máximo 700 caracteres. Ayuda al equipo de moderación a entender el caso.")
                }

                Section {
                    AsyncButton {
                        if await onSubmit(categoria, descripcion) { dismiss() }
                    } label: {
                        Text("Enviar reporte")
                            .frame(maxWidth: .infinity)
                    }
                    .disabled(descripcion.count > 700)
                }
            }
            .navigationTitle("Reportar")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { dismiss() }
                }
            }
        }
    }
}
