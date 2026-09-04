import MapKit
import SwiftUI

/// Mapa de oportunidades presenciales e híbridas.
///
/// Android usa MapLibre con teselas de OpenFreeMap; en iOS se usa MapKit, que
/// no requiere proveedor externo ni clave de API.
struct OpportunityMapView: View {
    @Environment(AppDependencies.self) private var dependencies

    @State private var state: LoadState<[TaskItem]> = .idle
    @State private var position: MapCameraPosition = .automatic
    @State private var selectedTaskId: Int?
    @State private var permissionMessage: String?

    /// Solo las tareas publicadas con coordenadas tienen sentido en el mapa.
    private var mappableTasks: [TaskItem] {
        (state.value ?? []).filter { $0.coordenadas != nil && $0.estaPublicada }
    }

    var body: some View {
        Group {
            switch state {
            case .idle, .loading:
                LoadingStateView(message: "Cargando el mapa…")

            case let .failed(message):
                ErrorStateView(message: message) { await load() }

            case .loaded:
                if mappableTasks.isEmpty {
                    EmptyStateView(
                        icon: "map",
                        title: "Sin oportunidades en el mapa",
                        message: "Todavía no hay oportunidades presenciales o híbridas con ubicación."
                    )
                } else {
                    mapContent
                }
            }
        }
        .screenBackground()
        .navigationTitle("Mapa")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    Task { await centerOnUser() }
                } label: {
                    Image(systemName: "location")
                }
                .accessibilityLabel("Centrar en mi ubicación")
            }
        }
        .task { await load() }
    }

    private var mapContent: some View {
        ZStack(alignment: .top) {
            Map(position: $position, selection: $selectedTaskId) {
                UserAnnotation()

                ForEach(mappableTasks) { task in
                    if let coordinates = task.coordenadas {
                        Marker(
                            task.titulo,
                            systemImage: task.esTareaRapida ? "bolt.fill" : "briefcase.fill",
                            coordinate: CLLocationCoordinate2D(
                                latitude: coordinates.latitude,
                                longitude: coordinates.longitude
                            )
                        )
                        .tint(task.esTareaRapida ? Theme.Color.warning : Theme.Color.primary)
                        .tag(task.idTarea)
                    }
                }
            }
            .mapControls {
                MapCompass()
                MapScaleView()
            }
            .ignoresSafeArea(edges: .bottom)

            if let permissionMessage {
                InlineErrorBanner(message: permissionMessage) {
                    self.permissionMessage = nil
                }
                .padding(Theme.Spacing.md)
            }
        }
        .safeAreaInset(edge: .bottom) {
            if let selectedTaskId,
               let task = mappableTasks.first(where: { $0.idTarea == selectedTaskId }) {
                selectedCard(task)
            }
        }
    }

    private func selectedCard(_ task: TaskItem) -> some View {
        NavigationLink {
            TaskDetailView(taskId: task.idTarea)
        } label: {
            VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
                HStack {
                    Text(task.titulo)
                        .font(Theme.Font.headline)
                        .foregroundStyle(Theme.Color.text)
                        .lineLimit(2)
                    Spacer(minLength: Theme.Spacing.xs)
                    Text(DisplayFormatter.money(task.presupuesto))
                        .font(Theme.Font.headline.monospacedDigit())
                        .foregroundStyle(Theme.Color.primaryDark)
                }

                if let direccion = task.direccionReferencia, !direccion.isEmpty {
                    Label(direccion, systemImage: "mappin")
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textMuted)
                        .lineLimit(1)
                }

                HStack(spacing: Theme.Spacing.xs) {
                    if task.esTareaRapida {
                        StatusPill(text: "Rápida", tint: Theme.Color.warning, icon: "bolt.fill")
                    }
                    if let modalidad = task.modalidadResuelta {
                        StatusPill(text: modalidad.label, tint: Theme.Color.textMuted)
                    }
                    Spacer(minLength: 0)
                    Text("Ver detalle")
                        .font(Theme.Font.caption.weight(.semibold))
                        .foregroundStyle(Theme.Color.primary)
                }
            }
            .cardSurface()
            .padding(Theme.Spacing.md)
        }
        .buttonStyle(.plain)
    }

    private func load() async {
        if state.value == nil { state = .loading }
        do {
            let tasks = try await dependencies.marketplace.tasks(
                size: AppConfig.maximumPageSize
            )
            state = .loaded(tasks)
            await centerOnUser()
        } catch {
            state = .failed(ErrorPresenter.message(for: error))
        }
    }

    private func centerOnUser() async {
        let outcome = await dependencies.location.requestCurrentLocation()
        switch outcome {
        case let .coordinate(coordinate):
            position = .region(MKCoordinateRegion(
                center: coordinate,
                span: MKCoordinateSpan(latitudeDelta: 0.08, longitudeDelta: 0.08)
            ))
        case .denied:
            // Sin permiso el mapa sigue siendo útil: se encuadra a los marcadores.
            permissionMessage = "Sin permiso de ubicación mostramos todas las oportunidades. "
                + "Actívalo en Ajustes para centrarte en tu zona."
            position = .automatic
        case .restricted:
            permissionMessage = "El acceso a la ubicación está restringido en este dispositivo."
            position = .automatic
        case .failed:
            position = .automatic
        }
    }
}
