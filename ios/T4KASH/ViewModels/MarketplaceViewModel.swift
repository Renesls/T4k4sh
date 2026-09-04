import Foundation
import Observation

/// Estado del marketplace: catálogo de categorías y listado de oportunidades.
@MainActor
@Observable
final class MarketplaceViewModel {
    private let repository: MarketplaceRepository

    init(repository: MarketplaceRepository) {
        self.repository = repository
    }

    private(set) var state: LoadState<[TaskItem]> = .idle
    private(set) var categories: [Category] = []
    private(set) var isLoadingMore = false
    private(set) var hasMorePages = true

    var searchText = ""
    var selectedCategory: Category?
    var selectedModalidad: Domain.Modalidad?
    var onlyQuickTasks = false

    private var page = 0
    private var allTasks: [TaskItem] = []

    /// Oportunidades tras aplicar búsqueda y filtros locales.
    ///
    /// El backend no expone filtros en `GET /tasks`, así que se filtra en el
    /// cliente exactamente igual que hace Android.
    var visibleTasks: [TaskItem] {
        let query = searchText
            .trimmingCharacters(in: .whitespaces)
            .lowercased()

        return allTasks.filter { task in
            if onlyQuickTasks && !task.esTareaRapida { return false }
            if let selectedCategory, task.idCategoria != selectedCategory.idCategoria {
                return false
            }
            if let selectedModalidad, task.modalidad != selectedModalidad.rawValue {
                return false
            }
            guard !query.isEmpty else { return true }
            return task.titulo.lowercased().contains(query)
                || task.descripcion.lowercased().contains(query)
                || (task.cliente?.nombreCompleto.lowercased().contains(query) ?? false)
        }
    }

    var hasActiveFilters: Bool {
        selectedCategory != nil || selectedModalidad != nil || onlyQuickTasks
            || !searchText.trimmingCharacters(in: .whitespaces).isEmpty
    }

    func clearFilters() {
        selectedCategory = nil
        selectedModalidad = nil
        onlyQuickTasks = false
        searchText = ""
    }

    /// Carga inicial. Categorías y primera página en paralelo.
    func load() async {
        guard state.value == nil else { return }
        state = .loading
        await reload()
    }

    /// Recarga desde cero, usada también por el gesto de arrastrar para refrescar.
    func reload() async {
        page = 0
        hasMorePages = true
        do {
            async let categoriesTask = repository.categories()
            async let tasksTask = repository.tasks(page: 0, size: AppConfig.defaultPageSize)

            let (loadedCategories, loadedTasks) = try await (categoriesTask, tasksTask)
            categories = loadedCategories.filter { $0.estado ?? true }
            allTasks = loadedTasks
            hasMorePages = loadedTasks.count >= AppConfig.defaultPageSize
            state = .loaded(loadedTasks)
        } catch {
            // Si ya había datos en pantalla, se conservan y el error se muestra aparte.
            if allTasks.isEmpty {
                state = .failed(ErrorPresenter.message(for: error))
            } else {
                state = .loaded(allTasks)
            }
        }
    }

    /// Paginación por convención: si llegan menos elementos que el tamaño de
    /// página, no hay más. El backend devuelve arrays planos sin metadatos.
    func loadMoreIfNeeded(currentItem: TaskItem) async {
        guard hasMorePages, !isLoadingMore,
              currentItem.idTarea == allTasks.last?.idTarea
        else { return }

        isLoadingMore = true
        defer { isLoadingMore = false }

        do {
            let next = try await repository.tasks(
                page: page + 1,
                size: AppConfig.defaultPageSize
            )
            page += 1
            hasMorePages = next.count >= AppConfig.defaultPageSize

            // El backend puede reordenar entre páginas; se evitan duplicados.
            let knownIds = Set(allTasks.map(\.idTarea))
            allTasks.append(contentsOf: next.filter { !knownIds.contains($0.idTarea) })
            state = .loaded(allTasks)
        } catch {
            hasMorePages = false
        }
    }

    /// Refleja localmente un cambio hecho en otra pantalla (publicar, editar, cancelar).
    func apply(updated task: TaskItem) {
        if let index = allTasks.firstIndex(where: { $0.idTarea == task.idTarea }) {
            allTasks[index] = task
        } else {
            allTasks.insert(task, at: 0)
        }
        state = .loaded(allTasks)
    }

    func categoryName(for id: Int) -> String {
        categories.first { $0.idCategoria == id }?.nombreCategoria ?? "Categoría"
    }
}
