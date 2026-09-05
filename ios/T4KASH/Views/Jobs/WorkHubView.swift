import SwiftUI

/// Centro de trabajo: reúne trabajos asignados, postulaciones enviadas y
/// publicaciones propias, que en Android son tres pantallas separadas.
struct WorkHubView: View {
    @Environment(AppDependencies.self) private var dependencies
    @Environment(SessionStore.self) private var session

    @State private var viewModel: WorkViewModel?

    var body: some View {
        Group {
            if let viewModel {
                content(viewModel: viewModel)
            } else {
                LoadingStateView()
            }
        }
        .screenBackground()
        .navigationTitle("Trabajos")
        .navigationBarTitleDisplayMode(.large)
        .onAppear {
            guard viewModel == nil, let user = session.user else { return }
            viewModel = WorkViewModel(
                marketplace: dependencies.marketplace,
                currentUserId: user.idUsuario
            )
        }
        .task { await viewModel?.loadAll() }
    }

    @ViewBuilder
    private func content(viewModel: WorkViewModel) -> some View {
        @Bindable var model = viewModel

        VStack(spacing: 0) {
            Picker("Sección", selection: $model.section) {
                ForEach(WorkViewModel.Section.allCases) { section in
                    Text(section.rawValue).tag(section)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, Theme.Spacing.md)
            .padding(.bottom, Theme.Spacing.xs)

            switch viewModel.section {
            case .jobs: jobsSection(viewModel: viewModel)
            case .applications: applicationsSection(viewModel: viewModel)
            case .publications: publicationsSection(viewModel: viewModel)
            }
        }
    }

    // MARK: - Trabajos

    @ViewBuilder
    private func jobsSection(viewModel: WorkViewModel) -> some View {
        switch viewModel.jobs {
        case .idle, .loading:
            LoadingStateView()

        case let .failed(message):
            ErrorStateView(message: message) { await viewModel.loadJobs() }

        case let .loaded(jobs):
            if jobs.isEmpty {
                EmptyStateView(
                    icon: "briefcase",
                    title: "Todavía no tienes trabajos",
                    message: "Cuando aceptes una postulación o te asignen una tarea, aparecerá aquí."
                )
            } else {
                ScrollView {
                    LazyVStack(spacing: Theme.Spacing.md) {
                        ForEach(jobs) { job in
                            NavigationLink {
                                JobDetailView(
                                    job: job,
                                    task: viewModel.task(for: job.idTarea)
                                )
                            } label: {
                                JobCard(job: job, taskTitle: viewModel.taskTitle(for: job.idTarea))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(Theme.Spacing.md)
                }
                .refreshable { await viewModel.loadJobs() }
            }
        }
    }

    // MARK: - Postulaciones

    @ViewBuilder
    private func applicationsSection(viewModel: WorkViewModel) -> some View {
        switch viewModel.applications {
        case .idle, .loading:
            LoadingStateView()

        case let .failed(message):
            ErrorStateView(message: message) { await viewModel.loadApplications() }

        case let .loaded(applications):
            if applications.isEmpty {
                EmptyStateView(
                    icon: "paperplane",
                    title: "Sin postulaciones enviadas",
                    message: "Explora el marketplace y postúlate a las oportunidades que te interesen."
                )
            } else {
                ScrollView {
                    LazyVStack(spacing: Theme.Spacing.md) {
                        ForEach(applications) { application in
                            NavigationLink {
                                TaskDetailView(taskId: application.idTarea)
                            } label: {
                                SentApplicationCard(
                                    application: application,
                                    taskTitle: viewModel.taskTitle(for: application.idTarea)
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(Theme.Spacing.md)
                }
                .refreshable { await viewModel.loadApplications() }
            }
        }
    }

    // MARK: - Publicaciones

    @ViewBuilder
    private func publicationsSection(viewModel: WorkViewModel) -> some View {
        @Bindable var model = viewModel

        switch viewModel.publications {
        case .idle, .loading:
            LoadingStateView()

        case let .failed(message):
            ErrorStateView(message: message) { await viewModel.loadPublications() }

        case let .loaded(all):
            if all.isEmpty {
                EmptyStateView(
                    icon: "square.and.pencil",
                    title: "Todavía no publicas nada",
                    message: "Publica una oportunidad desde la pestaña Explorar para recibir postulaciones."
                )
            } else {
                ScrollView {
                    LazyVStack(spacing: Theme.Spacing.md) {
                        publicationFilters(viewModel: viewModel)

                        if viewModel.visiblePublications.isEmpty {
                            EmptyStateView(
                                icon: "line.3.horizontal.decrease.circle",
                                title: "Sin publicaciones con ese estado",
                                actionTitle: "Ver todas",
                                action: { model.publicationFilter = nil }
                            )
                        } else {
                            ForEach(viewModel.visiblePublications) { task in
                                NavigationLink {
                                    TaskDetailView(taskId: task.idTarea) { updated in
                                        viewModel.apply(updated: updated)
                                    }
                                } label: {
                                    MyPublicationCard(
                                        task: task,
                                        categoryName: viewModel.categoryName(for: task.idCategoria)
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .padding(Theme.Spacing.md)
                }
                .refreshable { await viewModel.loadPublications() }
            }
        }
    }

    private func publicationFilters(viewModel: WorkViewModel) -> some View {
        @Bindable var model = viewModel
        let states = [
            Domain.EstadoTarea.publicada,
            Domain.EstadoTarea.asignada,
            Domain.EstadoTarea.cerrada,
            Domain.EstadoTarea.cancelada,
        ]

        return ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Theme.Spacing.xs) {
                filterChip(title: "Todas", isSelected: model.publicationFilter == nil) {
                    model.publicationFilter = nil
                }
                ForEach(states, id: \.self) { state in
                    filterChip(
                        title: Domain.EstadoTarea.label(state),
                        isSelected: model.publicationFilter == state
                    ) {
                        model.publicationFilter = model.publicationFilter == state ? nil : state
                    }
                }
            }
        }
        .scrollClipDisabled()
    }

    private func filterChip(
        title: String,
        isSelected: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Text(title)
                .font(Theme.Font.footnote.weight(.medium))
                .foregroundStyle(isSelected ? .white : Theme.Color.text)
                .padding(.horizontal, Theme.Spacing.sm)
                .padding(.vertical, Theme.Spacing.xs)
                .background(Capsule().fill(isSelected ? Theme.Color.primary : Theme.Color.surface))
                .overlay(
                    Capsule().stroke(isSelected ? Color.clear : Theme.Color.border, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Tarjetas

struct JobCard: View {
    let job: Job
    let taskTitle: String

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            HStack(alignment: .top) {
                Text(taskTitle)
                    .font(Theme.Font.headline)
                    .foregroundStyle(Theme.Color.text)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                Spacer(minLength: Theme.Spacing.xs)
                StatusPill(text: job.estadoLegible, tint: StatusTint.job(job.estadoTrabajo))
            }

            if let pago = job.pago {
                HStack {
                    Label(
                        DisplayFormatter.money(pago.montoEstudiante, currencyCode: pago.monedaCobro),
                        systemImage: pago.esEfectivo ? "banknote" : "lock.shield"
                    )
                    .font(Theme.Font.subheadline.monospacedDigit())
                    .foregroundStyle(Theme.Color.primaryDark)

                    Spacer()

                    StatusPill(
                        text: pago.estadoLegible,
                        tint: StatusTint.payment(pago.estadoPago)
                    )
                }
            }

            HStack(spacing: Theme.Spacing.xs) {
                if let estudiante = job.estudiante {
                    InitialsAvatar(initials: estudiante.iniciales, size: 24)
                    Text(estudiante.nombreCompleto)
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textMuted)
                        .lineLimit(1)
                }
                Spacer(minLength: 0)
                if let entrega = job.fechaEntregaEsperada {
                    Label(DisplayFormatter.dateTime(entrega), systemImage: "calendar")
                        .font(Theme.Font.caption)
                        .foregroundStyle(Theme.Color.textSoft)
                }
            }
        }
        .cardSurface()
    }
}

struct SentApplicationCard: View {
    let application: Application
    let taskTitle: String

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
            HStack(alignment: .top) {
                Text(taskTitle)
                    .font(Theme.Font.headline)
                    .foregroundStyle(Theme.Color.text)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                Spacer(minLength: Theme.Spacing.xs)
                StatusPill(
                    text: application.estadoLegible,
                    tint: StatusTint.application(application.estadoPostulacion)
                )
            }

            if let mensaje = application.mensaje, !mensaje.isEmpty {
                Text(mensaje)
                    .font(Theme.Font.subheadline)
                    .foregroundStyle(Theme.Color.textMuted)
                    .lineLimit(2)
            }

            HStack {
                if let precio = application.precioPropuesto {
                    Text("Propuesta: " + DisplayFormatter.money(precio))
                        .font(Theme.Font.caption.monospacedDigit())
                        .foregroundStyle(Theme.Color.primaryDark)
                }
                Spacer(minLength: 0)
                Text(DisplayFormatter.relative(application.fechaPostulacion))
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textSoft)
            }
        }
        .cardSurface()
    }
}

struct MyPublicationCard: View {
    let task: TaskItem
    let categoryName: String

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
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
                StatusPill(text: task.estadoLegible, tint: StatusTint.task(task.estadoTarea))
            }

            HStack {
                Text(DisplayFormatter.money(task.presupuesto))
                    .font(Theme.Font.subheadline.monospacedDigit())
                    .foregroundStyle(Theme.Color.primaryDark)
                Spacer(minLength: 0)
                Text(DisplayFormatter.relative(task.fechaPublicacion))
                    .font(Theme.Font.caption)
                    .foregroundStyle(Theme.Color.textSoft)
            }
        }
        .cardSurface()
    }
}
