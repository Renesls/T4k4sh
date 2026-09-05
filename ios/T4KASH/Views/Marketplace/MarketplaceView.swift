import SwiftUI

/// Listado de oportunidades: pantalla principal de la aplicación.
struct MarketplaceView: View {
    @Binding var unreadNotifications: Int

    @Environment(AppDependencies.self) private var dependencies
    @Environment(SessionStore.self) private var session

    @State private var viewModel: MarketplaceViewModel?
    @State private var showComposer = false
    @State private var showFilters = false

    var body: some View {
        Group {
            if let viewModel {
                content(viewModel: viewModel)
            } else {
                LoadingStateView()
            }
        }
        .screenBackground()
        .navigationTitle("Explorar")
        .navigationBarTitleDisplayMode(.large)
        .toolbar { toolbarContent }
        .onAppear {
            if viewModel == nil {
                viewModel = MarketplaceViewModel(repository: dependencies.marketplace)
            }
        }
        .task { await viewModel?.load() }
        .sheet(isPresented: $showComposer) {
            NavigationStack {
                TaskComposerView(mode: .create) { created in
                    viewModel?.apply(updated: created)
                }
            }
        }
        .sheet(isPresented: $showFilters) {
            if let viewModel {
                MarketplaceFiltersSheet(viewModel: viewModel)
                    .presentationDetents([.medium, .large])
            }
        }
    }

    @ViewBuilder
    private func content(viewModel: MarketplaceViewModel) -> some View {
        @Bindable var model = viewModel

        switch viewModel.state {
        case .idle, .loading:
            LoadingStateView(message: "Buscando oportunidades…")

        case let .failed(message):
            ErrorStateView(message: message) { await viewModel.reload() }

        case .loaded:
            ScrollView {
                LazyVStack(spacing: Theme.Spacing.md, pinnedViews: []) {
                    quickAccessRow

                    if !viewModel.categories.isEmpty {
                        ChipRow(
                            items: viewModel.categories,
                            title: \.nombreCategoria,
                            selection: $model.selectedCategory
                        )
                        .padding(.horizontal, -Theme.Spacing.md)
                    }

                    if viewModel.visibleTasks.isEmpty {
                        EmptyStateView(
                            icon: viewModel.hasActiveFilters ? "line.3.horizontal.decrease.circle" : "tray",
                            title: viewModel.hasActiveFilters
                                ? "Sin resultados con estos filtros"
                                : "Todavía no hay oportunidades publicadas",
                            message: viewModel.hasActiveFilters
                                ? "Prueba con otra categoría o limpia los filtros."
                                : "Sé el primero en publicar una oportunidad para la comunidad.",
                            actionTitle: viewModel.hasActiveFilters ? "Limpiar filtros" : "Publicar oportunidad",
                            action: {
                                if viewModel.hasActiveFilters {
                                    viewModel.clearFilters()
                                } else {
                                    showComposer = true
                                }
                            }
                        )
                        .padding(.top, Theme.Spacing.xl)
                    } else {
                        ForEach(viewModel.visibleTasks) { task in
                            NavigationLink(value: task.idTarea) {
                                TaskCard(
                                    task: task,
                                    categoryName: viewModel.categoryName(for: task.idCategoria)
                                )
                            }
                            .buttonStyle(.plain)
                            .task { await viewModel.loadMoreIfNeeded(currentItem: task) }
                        }

                        if viewModel.isLoadingMore {
                            ProgressView()
                                .tint(Theme.Color.primary)
                                .padding(.vertical, Theme.Spacing.md)
                        }
                    }
                }
                .padding(Theme.Spacing.md)
            }
            .refreshable { await viewModel.reload() }
            .searchable(
                text: $model.searchText,
                placement: .navigationBarDrawer(displayMode: .always),
                prompt: "Buscar oportunidades"
            )
            .navigationDestination(for: Int.self) { taskId in
                TaskDetailView(taskId: taskId) { updated in
                    viewModel.apply(updated: updated)
                }
            }
        }
    }

    /// Accesos al mapa y al radar de tareas rápidas.
    private var quickAccessRow: some View {
        HStack(spacing: Theme.Spacing.sm) {
            NavigationLink {
                OpportunityMapView()
            } label: {
                QuickAccessTile(
                    icon: "map",
                    title: "Mapa",
                    subtitle: "Oportunidades presenciales",
                    tint: Theme.Color.primary
                )
            }
            .buttonStyle(.plain)

            NavigationLink {
                QuickTasksView()
            } label: {
                QuickAccessTile(
                    icon: "bolt.fill",
                    title: "Tareas rápidas",
                    subtitle: "Radar cercano",
                    tint: Theme.Color.warning
                )
            }
            .buttonStyle(.plain)
        }
    }

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .topBarLeading) {
            NavigationLink {
                NotificationsView(unreadCount: $unreadNotifications)
            } label: {
                Image(systemName: unreadNotifications > 0 ? "bell.badge" : "bell")
                    .symbolRenderingMode(.hierarchical)
            }
            .accessibilityLabel(
                unreadNotifications > 0
                    ? "Notificaciones, \(unreadNotifications) sin leer"
                    : "Notificaciones"
            )
        }

        ToolbarItemGroup(placement: .topBarTrailing) {
            Button {
                showFilters = true
            } label: {
                Image(systemName: viewModel?.hasActiveFilters == true
                    ? "line.3.horizontal.decrease.circle.fill"
                    : "line.3.horizontal.decrease.circle")
            }
            .accessibilityLabel("Filtros")

            Button {
                showComposer = true
            } label: {
                Image(systemName: "plus.circle.fill")
            }
            .accessibilityLabel("Publicar oportunidad")
        }
    }
}

/// Acceso destacado en la parte superior del marketplace.
struct QuickAccessTile: View {
    let icon: String
    let title: String
    let subtitle: String
    let tint: Color

    var body: some View {
        HStack(spacing: Theme.Spacing.xs) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(tint)
                .frame(width: 36, height: 36)
                .background(Circle().fill(tint.opacity(0.12)))

            VStack(alignment: .leading, spacing: 1) {
                Text(title)
                    .font(Theme.Font.footnote.weight(.semibold))
                    .foregroundStyle(Theme.Color.text)
                Text(subtitle)
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textMuted)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .cardSurface(padding: Theme.Spacing.sm)
    }
}

/// Tarjeta de oportunidad en el listado.
struct TaskCard: View {
    let task: TaskItem
    let categoryName: String

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            HStack(alignment: .top, spacing: Theme.Spacing.xs) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(task.titulo)
                        .font(Theme.Font.headline)
                        .foregroundStyle(Theme.Color.text)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)

                    Text(categoryName)
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textMuted)
                }
                Spacer(minLength: Theme.Spacing.xs)

                Text(DisplayFormatter.money(task.presupuesto))
                    .font(Theme.Font.headline.monospacedDigit())
                    .foregroundStyle(Theme.Color.primaryDark)
            }

            Text(task.descripcion)
                .font(Theme.Font.subheadline)
                .foregroundStyle(Theme.Color.textMuted)
                .lineLimit(2)
                .multilineTextAlignment(.leading)

            HStack(spacing: Theme.Spacing.xs) {
                if task.esTareaRapida {
                    StatusPill(text: "Rápida", tint: Theme.Color.warning, icon: "bolt.fill")
                }

                StatusPill(
                    text: task.estadoLegible,
                    tint: StatusTint.task(task.estadoTarea)
                )

                if let modalidad = task.modalidadResuelta {
                    StatusPill(
                        text: modalidad.label,
                        tint: Theme.Color.textMuted,
                        icon: modalidad.icon
                    )
                }

                Spacer(minLength: 0)
            }

            Divider().overlay(Theme.Color.border)

            HStack(spacing: Theme.Spacing.xs) {
                if let cliente = task.cliente {
                    InitialsAvatar(
                        initials: cliente.iniciales,
                        size: 26,
                        verified: cliente.estudianteVerificado
                    )
                    Text(cliente.nombreCompleto)
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textMuted)
                        .lineLimit(1)
                }

                Spacer(minLength: 0)

                Text(DisplayFormatter.relative(task.fechaPublicacion))
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textSoft)
            }
        }
        .cardSurface()
    }
}

/// Hoja de filtros del marketplace.
struct MarketplaceFiltersSheet: View {
    @Bindable var viewModel: MarketplaceViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section("Categoría") {
                    Picker("Categoría", selection: $viewModel.selectedCategory) {
                        Text("Todas").tag(Category?.none)
                        ForEach(viewModel.categories) { category in
                            Text(category.nombreCategoria).tag(Category?.some(category))
                        }
                    }
                    .pickerStyle(.navigationLink)
                }

                Section("Modalidad") {
                    Picker("Modalidad", selection: $viewModel.selectedModalidad) {
                        Text("Todas").tag(Domain.Modalidad?.none)
                        ForEach(Domain.Modalidad.allCases) { modalidad in
                            Label(modalidad.label, systemImage: modalidad.icon)
                                .tag(Domain.Modalidad?.some(modalidad))
                        }
                    }
                    .pickerStyle(.inline)
                }

                Section {
                    Toggle("Solo tareas rápidas", isOn: $viewModel.onlyQuickTasks)
                } footer: {
                    Text("Las tareas rápidas son presenciales, se asignan al instante y "
                         + "tienen \(Domain.QuickTask.deliveryHours) horas de entrega.")
                }

                Section {
                    Button("Limpiar filtros", role: .destructive) {
                        viewModel.clearFilters()
                    }
                    .disabled(!viewModel.hasActiveFilters)
                }
            }
            .navigationTitle("Filtros")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Listo") { dismiss() }
                }
            }
        }
    }
}
