import Foundation
import Observation

/// Feed universitario: publicaciones, reacciones, guardados y comentarios.
@MainActor
@Observable
final class FeedViewModel {
    private let repository: FeedRepository

    init(repository: FeedRepository) { self.repository = repository }

    var scope: Domain.FeedScope = .paraTi
    var showingSaved = false

    private(set) var state: LoadState<[Post]> = .idle
    private(set) var isLoadingMore = false
    private(set) var hasMorePages = true

    var actionError: String?

    private var page = 0
    private var posts: [Post] = []
    private let pageSize = 30

    func load() async {
        if posts.isEmpty { state = .loading }
        page = 0
        hasMorePages = true
        do {
            let loaded = showingSaved
                ? try await repository.saved(page: 0, size: pageSize)
                : try await repository.feed(scope: scope, page: 0, size: pageSize)
            posts = loaded
            hasMorePages = loaded.count >= pageSize
            state = .loaded(loaded)
        } catch {
            if posts.isEmpty {
                state = .failed(ErrorPresenter.message(for: error))
            } else {
                actionError = ErrorPresenter.message(for: error)
            }
        }
    }

    /// Cambia de alcance o al listado de guardados vaciando lo anterior,
    /// para que no se mezclen resultados de dos consultas distintas.
    func switchTo(scope newScope: Domain.FeedScope) async {
        guard newScope != scope || showingSaved else { return }
        scope = newScope
        showingSaved = false
        posts = []
        state = .loading
        await load()
    }

    func toggleSavedView() async {
        showingSaved.toggle()
        posts = []
        state = .loading
        await load()
    }

    func loadMoreIfNeeded(currentItem: Post) async {
        guard hasMorePages, !isLoadingMore,
              currentItem.idPublicacion == posts.last?.idPublicacion
        else { return }

        isLoadingMore = true
        defer { isLoadingMore = false }

        do {
            let next = showingSaved
                ? try await repository.saved(page: page + 1, size: pageSize)
                : try await repository.feed(scope: scope, page: page + 1, size: pageSize)
            page += 1
            hasMorePages = next.count >= pageSize

            let known = Set(posts.map(\.idPublicacion))
            posts.append(contentsOf: next.filter { !known.contains($0.idPublicacion) })
            state = .loaded(posts)
        } catch {
            hasMorePages = false
        }
    }

    // MARK: - Acciones sobre publicaciones

    /// Alterna la reacción. Si ya estaba puesta la misma, se retira.
    func toggleReaction(_ post: Post, reaccion: Domain.Reaccion) async {
        do {
            let updated = post.reaccionPropia == reaccion
                ? try await repository.removeReaction(postId: post.idPublicacion)
                : try await repository.setReaction(postId: post.idPublicacion, reaccion: reaccion)
            replace(updated)
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }

    func toggleSaved(_ post: Post) async {
        do {
            let updated = post.guardada
                ? try await repository.unsavePost(id: post.idPublicacion)
                : try await repository.savePost(id: post.idPublicacion)

            // En la vista de guardados, quitar el guardado saca la publicación.
            if showingSaved && !updated.guardada {
                posts.removeAll { $0.idPublicacion == updated.idPublicacion }
                state = .loaded(posts)
            } else {
                replace(updated)
            }
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }

    func delete(_ post: Post) async {
        do {
            _ = try await repository.deletePost(id: post.idPublicacion)
            posts.removeAll { $0.idPublicacion == post.idPublicacion }
            state = .loaded(posts)
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }

    func createPost(
        contenido: String,
        tipo: Domain.TipoPublicacion,
        visibilidad: Domain.VisibilidadPublicacion,
        permiteComentarios: Bool
    ) async -> Bool {
        do {
            let created = try await repository.createPost(
                contenido: contenido.trimmingCharacters(in: .whitespacesAndNewlines),
                tipo: tipo,
                visibilidad: visibilidad,
                permiteComentarios: permiteComentarios
            )
            posts.insert(created, at: 0)
            state = .loaded(posts)
            return true
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return false
        }
    }

    func updatePost(
        _ post: Post,
        contenido: String,
        tipo: Domain.TipoPublicacion,
        visibilidad: Domain.VisibilidadPublicacion,
        permiteComentarios: Bool
    ) async -> Bool {
        do {
            let updated = try await repository.updatePost(
                id: post.idPublicacion,
                contenido: contenido.trimmingCharacters(in: .whitespacesAndNewlines),
                tipo: tipo,
                visibilidad: visibilidad,
                permiteComentarios: permiteComentarios
            )
            replace(updated)
            return true
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return false
        }
    }

    /// Sincroniza un cambio hecho en la pantalla de comentarios.
    func replace(_ post: Post) {
        if let index = posts.firstIndex(where: { $0.idPublicacion == post.idPublicacion }) {
            posts[index] = post
            state = .loaded(posts)
        }
    }
}

/// Comentarios de una publicación.
@MainActor
@Observable
final class PostCommentsViewModel {
    private let repository: FeedRepository
    private(set) var post: Post

    init(post: Post, repository: FeedRepository) {
        self.post = post
        self.repository = repository
    }

    private(set) var state: LoadState<[PostComment]> = .idle
    var draft = ""
    var replyingTo: PostComment?
    var actionError: String?
    private(set) var isSending = false

    var canSend: Bool {
        let trimmed = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        return !trimmed.isEmpty && trimmed.count <= 2_000 && !isSending
    }

    /// Comentarios raíz, cada uno con sus respuestas, para pintar el hilo.
    var threads: [(comment: PostComment, replies: [PostComment])] {
        let all = state.value ?? []
        let roots = all.filter { !$0.esRespuesta }
        return roots.map { root in
            (root, all.filter { $0.idComentarioPadre == root.idComentario })
        }
    }

    func load() async {
        if state.value == nil { state = .loading }
        do {
            let comments = try await repository.comments(postId: post.idPublicacion)
            state = .loaded(
                comments.sorted {
                    ($0.fechaComentario ?? .distantPast) < ($1.fechaComentario ?? .distantPast)
                }
            )
        } catch {
            state = .failed(ErrorPresenter.message(for: error))
        }
    }

    func send() async {
        let contenido = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !contenido.isEmpty, !isSending else { return }

        isSending = true
        defer { isSending = false }

        do {
            _ = try await repository.createComment(
                postId: post.idPublicacion,
                contenido: contenido,
                idComentarioPadre: replyingTo?.idComentario
            )
            draft = ""
            replyingTo = nil
            await load()
            // El contador de comentarios vive en la publicación.
            if let refreshed = try? await repository.post(id: post.idPublicacion) {
                post = refreshed
            }
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }

    func delete(_ comment: PostComment) async {
        do {
            _ = try await repository.deleteComment(id: comment.idComentario)
            await load()
            if let refreshed = try? await repository.post(id: post.idPublicacion) {
                post = refreshed
            }
        } catch {
            actionError = ErrorPresenter.message(for: error)
        }
    }

    func update(_ comment: PostComment, contenido: String) async -> Bool {
        do {
            _ = try await repository.updateComment(
                id: comment.idComentario,
                contenido: contenido.trimmingCharacters(in: .whitespacesAndNewlines)
            )
            await load()
            return true
        } catch {
            actionError = ErrorPresenter.message(for: error)
            return false
        }
    }
}
