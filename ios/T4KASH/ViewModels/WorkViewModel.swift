import Foundation
import Observation

/// Estado del centro de trabajo: trabajos asignados, postulaciones enviadas y
/// publicaciones propias.
@MainActor
@Observable
final class WorkViewModel {
    private let marketplace: MarketplaceRepository
    private let currentUserId: Int

    init(marketplace: MarketplaceRepository, currentUserId: Int) {
        self.marketplace = marketplace
        self.currentUserId = currentUserId
    }

    enum Section: String, CaseIterable, Identifiable {
        case jobs = "Trabajos"
        case applications = "Postulaciones"
        case publications = "Publicaciones"

        var id: String { rawValue }
    }

    var section: Section = .jobs

    private(set) var jobs: LoadState<[Job]> = .idle
    private(set) var applications: LoadState<[Application]> = .idle
    private(set) var publications: LoadState<[TaskItem]> = .idle
    private(set) var tasksById: [Int: TaskItem] = [:]
    private(set) var categories: [Category] = []

    /// Publicaciones filtradas por estado, como el filtro de Android.
    var publicationFilter: String?

    var visiblePublications: [TaskItem] {
        let all = publications.value ?? []
        guard let publicationFilter else { return all }
        return all.filter { $0.estadoTarea == publicationFilter }
    }

    func loadAll() async {
        async let jobsTask: Void = loadJobs()
        async let applicationsTask: Void = loadApplications()
        async let publicationsTask: Void = loadPublications()
        _ = await (jobsTask, applicationsTask, publicationsTask)
    }

    func loadJobs() async {
        if jobs.value == nil { jobs = .loading }
        do {
            let loaded = try await marketplace.jobs()
            jobs = .loaded(loaded.sorted { ($0.fechaInicio ?? .distantPast) > ($1.fechaInicio ?? .distantPast) })
            await cacheTasks(ids: loaded.map(\.idTarea))
        } catch {
            jobs = .failed(ErrorPresenter.message(for: error))
        }
    }

    func loadApplications() async {
        if applications.value == nil { applications = .loading }
        do {
            let loaded = try await marketplace.myApplications()
            applications = .loaded(
                loaded.sorted { ($0.fechaPostulacion ?? .distantPast) > ($1.fechaPostulacion ?? .distantPast) }
            )
            await cacheTasks(ids: loaded.map(\.idTarea))
        } catch {
            applications = .failed(ErrorPresenter.message(for: error))
        }
    }

    /// El backend no expone "mis tareas": se derivan del listado general
    /// filtrando por `idCliente`, igual que hace Android.
    func loadPublications() async {
        if publications.value == nil { publications = .loading }
        do {
            async let tasksRequest = marketplace.tasks(size: AppConfig.maximumPageSize)
            async let categoriesRequest = marketplace.categories()

            let (allTasks, loadedCategories) = try await (tasksRequest, categoriesRequest)
            categories = loadedCategories

            let mine = allTasks
                .filter { $0.idCliente == currentUserId }
                .sorted { ($0.fechaPublicacion ?? .distantPast) > ($1.fechaPublicacion ?? .distantPast) }

            for task in allTasks { tasksById[task.idTarea] = task }
            publications = .loaded(mine)
        } catch {
            publications = .failed(ErrorPresenter.message(for: error))
        }
    }

    /// Carga los detalles de las tareas referenciadas por trabajos y postulaciones,
    /// que solo traen el `idTarea`.
    private func cacheTasks(ids: [Int]) async {
        let missing = Set(ids).subtracting(tasksById.keys)
        guard !missing.isEmpty else { return }

        let repository = marketplace
        await withTaskGroup(of: TaskItem?.self) { group in
            for id in missing {
                group.addTask { try? await repository.task(id: id) }
            }
            for await task in group {
                if let task { tasksById[task.idTarea] = task }
            }
        }
    }

    func taskTitle(for id: Int) -> String {
        tasksById[id]?.titulo ?? "Oportunidad #\(id)"
    }

    func task(for id: Int) -> TaskItem? { tasksById[id] }

    func categoryName(for id: Int) -> String {
        categories.first { $0.idCategoria == id }?.nombreCategoria ?? "Categoría"
    }

    func apply(updated task: TaskItem) {
        tasksById[task.idTarea] = task
        if case let .loaded(current) = publications,
           let index = current.firstIndex(where: { $0.idTarea == task.idTarea }) {
            var updatedList = current
            updatedList[index] = task
            publications = .loaded(updatedList)
        }
    }
}
