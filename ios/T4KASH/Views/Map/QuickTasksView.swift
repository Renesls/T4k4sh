import MapKit
import SwiftUI

/// Radar de tareas rápidas cercanas.
///
/// Consume `GET /quick-tasks/nearby`, que exige coordenadas: sin permiso de
/// ubicación no hay radar posible, y la pantalla lo explica en vez de quedarse
/// en blanco.
struct QuickTasksView: View {
    @Environment(AppDependencies.self) private var dependencies

    @State private var state: LoadState<[QuickTask]> = .idle
    @State private var radiusKm: Double = 1.0
    @State private var coordinate: CLLocationCoordinate2D?
    @State private var permissionIssue: String?
    @State private var claimError: String?
    @State private var claimedJob: Job?

    var body: some View {
        Group {
            if let permissionIssue {
                EmptyStateView(
                    icon: "location.slash",
                    title: "Necesitamos tu ubicación",
                    message: permissionIssue,
                    actionTitle: "Abrir Ajustes",
                    action: openSettings
                )
            } else {
                content
            }
        }
        .screenBackground()
        .navigationTitle("Tareas rápidas")
        .navigationBarTitleDisplayMode(.inline)
        .task { await locateAndLoad() }
        .alert(
            "Tarea reclamada",
            isPresented: Binding(get: { claimedJob != nil }, set: { if !$0 { claimedJob = nil } })
        ) {
            Button("Entendido") { claimedJob = nil }
        } message: {
            Text("La tarea quedó asignada a tu nombre. Tienes "
                 + "\(Domain.QuickTask.deliveryHours) horas para entregarla. "
                 + "Coordina los detalles desde la pestaña Trabajos.")
        }
    }

    @ViewBuilder
    private var content: some View {
        VStack(spacing: 0) {
            radiusControl

            switch state {
            case .idle, .loading:
                LoadingStateView(message: "Buscando tareas cerca de ti…")

            case let .failed(message):
                ErrorStateView(message: message) { await locateAndLoad() }

            case let .loaded(items):
                if items.isEmpty {
                    EmptyStateView(
                        icon: "bolt.slash",
                        title: "Nada cerca por ahora",
                        message: "No hay tareas rápidas activas en \(DisplayFormatter.distance(radiusKm)) "
                            + "a la redonda. Prueba a ampliar el radio.",
                        actionTitle: "Volver a buscar",
                        action: { Task { await load() } }
                    )
                } else {
                    ScrollView {
                        LazyVStack(spacing: Theme.Spacing.md) {
                            if let claimError {
                                InlineErrorBanner(message: claimError) { self.claimError = nil }
                            }

                            ForEach(items) { quickTask in
                                QuickTaskCard(quickTask: quickTask) {
                                    await claim(quickTask)
                                }
                            }
                        }
                        .padding(Theme.Spacing.md)
                    }
                    .refreshable { await load() }
                }
            }
        }
    }

    private var radiusControl: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xxs) {
            HStack {
                Label("Radio de búsqueda", systemImage: "dot.radiowaves.left.and.right")
                    .font(Theme.Font.footnote.weight(.semibold))
                    .foregroundStyle(Theme.Color.text)
                Spacer()
                Text(DisplayFormatter.distance(radiusKm))
                    .font(Theme.Font.footnote.monospacedDigit())
                    .foregroundStyle(Theme.Color.primary)
            }

            Slider(
                value: $radiusKm,
                in: Domain.QuickTask.minimumRadiusKm...Domain.QuickTask.maximumRadiusKm,
                step: 0.25
            )
            .tint(Theme.Color.primary)
            .onChange(of: radiusKm) { _, _ in
                Task {
                    // Espera a que el usuario suelte el control antes de consultar.
                    try? await Task.sleep(for: .milliseconds(400))
                    guard !Task.isCancelled else { return }
                    await load()
                }
            }
        }
        .padding(Theme.Spacing.md)
        .background(Theme.Color.surface)
        .overlay(alignment: .bottom) {
            Rectangle().fill(Theme.Color.border).frame(height: 1)
        }
    }

    private func locateAndLoad() async {
        let outcome = await dependencies.location.requestCurrentLocation()
        switch outcome {
        case let .coordinate(value):
            permissionIssue = nil
            coordinate = value
            await load()
        case .denied:
            permissionIssue = "El radar de tareas rápidas usa tu ubicación para encontrar "
                + "oportunidades a pocos metros. Actívala en Ajustes › Privacidad › Localización."
        case .restricted:
            permissionIssue = "El acceso a la ubicación está restringido en este dispositivo."
        case let .failed(message):
            permissionIssue = message
        }
    }

    private func load() async {
        guard let coordinate else { return }
        if state.value == nil { state = .loading }
        do {
            let items = try await dependencies.marketplace.nearbyQuickTasks(
                latitude: coordinate.latitude,
                longitude: coordinate.longitude,
                radiusKm: radiusKm
            )
            state = .loaded(items.sorted { $0.distanciaKm < $1.distanciaKm })
        } catch {
            state = .failed(ErrorPresenter.message(for: error))
        }
    }

    private func claim(_ quickTask: QuickTask) async {
        do {
            claimedJob = try await dependencies.marketplace.claimQuickTask(
                id: quickTask.tarea.idTarea
            )
            await load()
        } catch {
            claimError = ErrorPresenter.message(for: error)
        }
    }

    private func openSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }
}

/// Tarjeta de una tarea rápida con distancia, cuenta atrás y reclamo.
struct QuickTaskCard: View {
    let quickTask: QuickTask
    let onClaim: () async -> Void

    private var task: TaskItem { quickTask.tarea }

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(task.titulo)
                        .font(Theme.Font.headline)
                        .foregroundStyle(Theme.Color.text)
                        .lineLimit(2)

                    HStack(spacing: Theme.Spacing.xs) {
                        Label(
                            DisplayFormatter.distance(quickTask.distanciaKm),
                            systemImage: "location.fill"
                        )
                        Label(
                            DisplayFormatter.countdown(seconds: quickTask.segundosRestantes),
                            systemImage: "timer"
                        )
                    }
                    .font(Theme.Font.caption)
                    .foregroundStyle(
                        quickTask.expirada ? Theme.Color.danger : Theme.Color.warning
                    )
                }
                Spacer(minLength: Theme.Spacing.xs)
                Text(DisplayFormatter.money(task.presupuesto))
                    .font(Theme.Font.headline.monospacedDigit())
                    .foregroundStyle(Theme.Color.primaryDark)
            }

            Text(task.descripcion)
                .font(Theme.Font.subheadline)
                .foregroundStyle(Theme.Color.textMuted)
                .lineLimit(3)

            if let direccion = task.direccionReferencia, !direccion.isEmpty {
                Label(direccion, systemImage: "mappin.and.ellipse")
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textMuted)
                    .lineLimit(1)
            }

            HStack(spacing: Theme.Spacing.sm) {
                NavigationLink {
                    TaskDetailView(taskId: task.idTarea)
                } label: {
                    Text("Ver detalle")
                        .font(Theme.Font.footnote.weight(.semibold))
                        .foregroundStyle(Theme.Color.primary)
                }
                .buttonStyle(.plain)

                Spacer(minLength: 0)

                AsyncButton(action: onClaim) {
                    Label("Reclamar", systemImage: "bolt.fill")
                        .font(Theme.Font.footnote.weight(.semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, Theme.Spacing.md)
                        .padding(.vertical, Theme.Spacing.xs)
                        .background(
                            Capsule().fill(
                                quickTask.expirada ? Theme.Color.textSoft : Theme.Color.primary
                            )
                        )
                }
                .disabled(quickTask.expirada)
            }
        }
        .cardSurface()
    }
}
