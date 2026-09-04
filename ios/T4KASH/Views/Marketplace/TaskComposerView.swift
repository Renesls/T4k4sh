import CoreLocation
import SwiftUI

/// Publicación y edición de oportunidades.
///
/// Aplica las reglas reales del backend (`TaskService`): la ubicación es
/// obligatoria en presencial e híbrida, y las tareas rápidas son presenciales,
/// con presupuesto acotado y ventana de entrega fija.
struct TaskComposerView: View {
    enum Mode {
        case create
        case edit(TaskItem)

        var isEditing: Bool {
            if case .edit = self { return true }
            return false
        }
    }

    let mode: Mode
    let onSaved: (TaskItem) -> Void

    @Environment(AppDependencies.self) private var dependencies
    @Environment(\.dismiss) private var dismiss

    @State private var titulo = ""
    @State private var descripcion = ""
    @State private var presupuestoTexto = ""
    @State private var idCategoria: Int?
    @State private var tipoOportunidad: Domain.TipoOportunidad = .tarea
    @State private var modalidad: Domain.Modalidad = .remota
    @State private var usaFechaLimitePostulacion = false
    @State private var fechaLimitePostulacion = Date().addingTimeInterval(60 * 60 * 24 * 3)
    @State private var usaFechaLimite = false
    @State private var fechaLimite = Date().addingTimeInterval(60 * 60 * 24 * 7)
    @State private var location: PickedLocation?

    @State private var categories: [Category] = []
    @State private var isLoadingCategories = true
    @State private var showLocationPicker = false
    @State private var errorMessage: String?
    @State private var isSaving = false

    private var presupuesto: Decimal? { Validation.decimal(from: presupuestoTexto) }

    /// Las tareas rápidas se publican siempre como presenciales.
    private var modalidadEfectiva: Domain.Modalidad {
        tipoOportunidad == .rapida ? .presencial : modalidad
    }

    private var requiresLocation: Bool { modalidadEfectiva.requiresLocation }

    private var presupuestoError: String? {
        guard !presupuestoTexto.isEmpty else { return nil }
        guard let presupuesto else { return "Escribe un monto válido." }
        if presupuesto < 0 { return "El presupuesto no puede ser negativo." }
        if tipoOportunidad == .rapida {
            if presupuesto <= 0 { return "Una tarea rápida necesita un monto mayor que cero." }
            if presupuesto > Domain.QuickTask.maximumBudget {
                return "El máximo para tareas rápidas es "
                    + DisplayFormatter.money(Domain.QuickTask.maximumBudget) + "."
            }
        }
        return nil
    }

    private var canSave: Bool {
        !titulo.trimmingCharacters(in: .whitespaces).isEmpty
            && titulo.count <= 150
            && !descripcion.trimmingCharacters(in: .whitespaces).isEmpty
            && presupuesto != nil
            && presupuestoError == nil
            && idCategoria != nil
            && (!requiresLocation || location != nil)
            && !isSaving
    }

    var body: some View {
        Form {
            if let errorMessage {
                Section {
                    InlineErrorBanner(message: errorMessage) { self.errorMessage = nil }
                        .listRowInsets(EdgeInsets())
                        .listRowBackground(Color.clear)
                }
            }

            Section("Tipo de oportunidad") {
                Picker("Tipo", selection: $tipoOportunidad) {
                    ForEach(Domain.TipoOportunidad.allCases) { tipo in
                        Text(tipo.label).tag(tipo)
                    }
                }
                .pickerStyle(.segmented)
                .disabled(mode.isEditing)

                Text(tipoOportunidad.detail)
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textMuted)

                if tipoOportunidad == .rapida {
                    Label(
                        "Disponible \(Domain.QuickTask.availabilityHours) h · entrega en "
                            + "\(Domain.QuickTask.deliveryHours) h · máximo "
                            + DisplayFormatter.money(Domain.QuickTask.maximumBudget),
                        systemImage: "bolt.fill"
                    )
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.warning)
                }
            }

            Section("Detalles") {
                TextField("Título", text: $titulo)
                    .onChange(of: titulo) { _, value in
                        if value.count > 150 { titulo = String(value.prefix(150)) }
                    }

                VStack(alignment: .leading, spacing: 4) {
                    TextEditor(text: $descripcion)
                        .frame(minHeight: 120)
                    Text("Describe qué necesitas, cuándo y qué esperas recibir.")
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textSoft)
                }

                if isLoadingCategories {
                    HStack {
                        ProgressView().controlSize(.small)
                        Text("Cargando categorías…")
                            .font(Theme.Font.footnote)
                            .foregroundStyle(Theme.Color.textMuted)
                    }
                } else {
                    Picker("Categoría", selection: $idCategoria) {
                        Text("Selecciona una").tag(Int?.none)
                        ForEach(categories) { category in
                            Text(category.nombreCategoria).tag(Int?.some(category.idCategoria))
                        }
                    }
                    .pickerStyle(.navigationLink)
                }
            }

            Section {
                HStack {
                    Text("C$")
                        .foregroundStyle(Theme.Color.textMuted)
                    TextField("0.00", text: $presupuestoTexto)
                        .keyboardType(.decimalPad)
                }
                if let presupuestoError {
                    Text(presupuestoError)
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.danger)
                }
            } header: {
                Text("Presupuesto")
            } footer: {
                Text("El monto que estás dispuesto a pagar. Los estudiantes pueden proponer otro precio al postularse.")
            }

            if tipoOportunidad == .tarea {
                Section("Modalidad") {
                    Picker("Modalidad", selection: $modalidad) {
                        ForEach(Domain.Modalidad.allCases) { option in
                            Label(option.label, systemImage: option.icon).tag(option)
                        }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }
            }

            if requiresLocation {
                Section {
                    Button {
                        showLocationPicker = true
                    } label: {
                        HStack {
                            Image(systemName: "mappin.and.ellipse")
                                .foregroundStyle(Theme.Color.primary)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(location == nil ? "Elegir en el mapa" : "Cambiar ubicación")
                                    .foregroundStyle(Theme.Color.text)
                                if let location {
                                    Text(location.address.isEmpty
                                         ? String(format: "Lat %.5f · Lon %.5f",
                                                  location.coordinate.latitude,
                                                  location.coordinate.longitude)
                                         : location.address)
                                        .font(Theme.Font.caption)
                                        .foregroundStyle(Theme.Color.textMuted)
                                        .lineLimit(2)
                                }
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Theme.Color.textSoft)
                        }
                    }
                } header: {
                    Text("Ubicación")
                } footer: {
                    Text(location == nil
                         ? "Obligatoria para oportunidades presenciales e híbridas."
                         : "Aparecerá en el mapa y, si es rápida, en el radar de estudiantes cercanos.")
                }
            }

            if tipoOportunidad == .tarea {
                Section("Fechas") {
                    Toggle("Cierre de postulaciones", isOn: $usaFechaLimitePostulacion)
                    if usaFechaLimitePostulacion {
                        DatePicker(
                            "Cierra el",
                            selection: $fechaLimitePostulacion,
                            in: Date()...,
                            displayedComponents: [.date, .hourAndMinute]
                        )
                    }

                    Toggle("Fecha límite de entrega", isOn: $usaFechaLimite)
                    if usaFechaLimite {
                        DatePicker(
                            "Entrega antes del",
                            selection: $fechaLimite,
                            in: Date()...,
                            displayedComponents: [.date, .hourAndMinute]
                        )
                    }
                }
            }

            Section {
                AsyncButton {
                    await save()
                } label: {
                    Text(mode.isEditing ? "Guardar cambios" : "Publicar oportunidad")
                        .frame(maxWidth: .infinity)
                }
                .disabled(!canSave)
            }
        }
        .navigationTitle(mode.isEditing ? "Editar oportunidad" : "Nueva oportunidad")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancelar") { dismiss() }
            }
        }
        .sheet(isPresented: $showLocationPicker) {
            LocationPickerView(initialLocation: location) { picked in
                location = picked
            }
        }
        .task { await loadCategories() }
        .onAppear(perform: prefillIfEditing)
    }

    private func prefillIfEditing() {
        guard case let .edit(task) = mode, titulo.isEmpty else { return }

        titulo = task.titulo
        descripcion = task.descripcion
        presupuestoTexto = NSDecimalNumber(decimal: task.presupuesto).stringValue
        idCategoria = task.idCategoria
        tipoOportunidad = Domain.TipoOportunidad(rawValue: task.tipoOportunidad) ?? .tarea
        modalidad = task.modalidadResuelta ?? .remota

        if let fecha = task.fechaLimitePostulacion {
            usaFechaLimitePostulacion = true
            fechaLimitePostulacion = fecha
        }
        if let fecha = task.fechaLimite {
            usaFechaLimite = true
            fechaLimite = fecha
        }
        if let coordinates = task.coordenadas {
            location = PickedLocation(
                coordinate: CLLocationCoordinate2D(
                    latitude: coordinates.latitude,
                    longitude: coordinates.longitude
                ),
                address: task.direccionReferencia ?? ""
            )
        }
    }

    private func loadCategories() async {
        isLoadingCategories = true
        defer { isLoadingCategories = false }
        do {
            categories = try await dependencies.marketplace.categories()
                .filter { $0.estado ?? true }
        } catch {
            errorMessage = ErrorPresenter.message(for: error)
        }
    }

    private func save() async {
        guard canSave, let presupuesto, let idCategoria else { return }

        isSaving = true
        defer { isSaving = false }
        errorMessage = nil

        let request = CreateTaskRequest(
            titulo: titulo.trimmingCharacters(in: .whitespaces),
            descripcion: descripcion.trimmingCharacters(in: .whitespaces),
            presupuesto: presupuesto,
            fechaLimitePostulacion: tipoOportunidad == .tarea && usaFechaLimitePostulacion
                ? fechaLimitePostulacion : nil,
            fechaLimite: tipoOportunidad == .tarea && usaFechaLimite ? fechaLimite : nil,
            idCategoria: idCategoria,
            tipoOportunidad: tipoOportunidad.rawValue,
            modalidad: modalidadEfectiva.rawValue,
            // El backend solo acepta `PUBLICA` para tareas.
            visibilidad: "PUBLICA",
            direccionReferencia: location?.address.isEmpty == false ? location?.address : nil,
            latitud: location.map { Decimal($0.coordinate.latitude) },
            longitud: location.map { Decimal($0.coordinate.longitude) }
        )

        do {
            let saved: TaskItem
            if case let .edit(task) = mode {
                saved = try await dependencies.marketplace.updateTask(
                    id: task.idTarea,
                    request
                )
            } else {
                saved = try await dependencies.marketplace.createTask(request)
            }
            onSaved(saved)
            dismiss()
        } catch {
            errorMessage = ErrorPresenter.message(for: error)
        }
    }
}
