import Foundation

/// Acceso al módulo `network`: feed universitario, reacciones y comentarios.
struct FeedRepository {
    private let client: APIClient

    init(client: APIClient) { self.client = client }

    /// `GET /network/feed?alcance=&page=&size=`
    func feed(
        scope: Domain.FeedScope,
        page: Int = 0,
        size: Int = 30
    ) async throws -> [Post] {
        try await client.send(
            .get("network/feed", query: [
                "alcance": scope.rawValue,
                "page": "\(page)",
                "size": "\(min(size, AppConfig.maximumPageSize))",
            ])
        )
    }

    /// `GET /network/saved?page=&size=`
    func saved(page: Int = 0, size: Int = 30) async throws -> [Post] {
        try await client.send(
            .get("network/saved", query: [
                "page": "\(page)",
                "size": "\(min(size, AppConfig.maximumPageSize))",
            ])
        )
    }

    /// `GET /network/posts/{id}`
    func post(id: Int) async throws -> Post {
        try await client.send(.get("network/posts/\(id)"))
    }

    /// `POST /network/posts`
    func createPost(
        contenido: String?,
        tipo: Domain.TipoPublicacion,
        visibilidad: Domain.VisibilidadPublicacion,
        permiteComentarios: Bool,
        idPublicacionOrigen: Int? = nil
    ) async throws -> Post {
        try await client.send(
            try .json(
                "network/posts",
                method: .post,
                body: CreatePostRequest(
                    contenido: contenido,
                    tipoPublicacion: tipo.rawValue,
                    visibilidad: visibilidad.rawValue,
                    permiteComentarios: permiteComentarios,
                    idPublicacionOrigen: idPublicacionOrigen
                )
            )
        )
    }

    /// `PUT /network/posts/{id}`
    func updatePost(
        id: Int,
        contenido: String?,
        tipo: Domain.TipoPublicacion,
        visibilidad: Domain.VisibilidadPublicacion,
        permiteComentarios: Bool
    ) async throws -> Post {
        try await client.send(
            try .json(
                "network/posts/\(id)",
                method: .put,
                body: UpdatePostRequest(
                    contenido: contenido,
                    tipoPublicacion: tipo.rawValue,
                    visibilidad: visibilidad.rawValue,
                    permiteComentarios: permiteComentarios
                )
            )
        )
    }

    /// `DELETE /network/posts/{id}`
    func deletePost(id: Int) async throws -> Post {
        try await client.send(.empty("network/posts/\(id)", method: .delete))
    }

    /// `PUT /network/posts/{id}/reaction`
    func setReaction(postId: Int, reaccion: Domain.Reaccion) async throws -> Post {
        try await client.send(
            try .json(
                "network/posts/\(postId)/reaction",
                method: .put,
                body: ReactionRequest(tipoReaccion: reaccion.rawValue)
            )
        )
    }

    /// `DELETE /network/posts/{id}/reaction`
    func removeReaction(postId: Int) async throws -> Post {
        try await client.send(.empty("network/posts/\(postId)/reaction", method: .delete))
    }

    /// `PUT /network/posts/{id}/saved`
    func savePost(id: Int) async throws -> Post {
        try await client.send(.empty("network/posts/\(id)/saved", method: .put))
    }

    /// `DELETE /network/posts/{id}/saved`
    func unsavePost(id: Int) async throws -> Post {
        try await client.send(.empty("network/posts/\(id)/saved", method: .delete))
    }

    /// `GET /network/posts/{id}/comments?page=&size=`
    func comments(
        postId: Int,
        page: Int = 0,
        size: Int = AppConfig.maximumPageSize
    ) async throws -> [PostComment] {
        try await client.send(
            .get("network/posts/\(postId)/comments", query: [
                "page": "\(page)",
                "size": "\(min(size, AppConfig.maximumPageSize))",
            ])
        )
    }

    /// `POST /network/posts/{id}/comments`
    func createComment(
        postId: Int,
        contenido: String,
        idComentarioPadre: Int? = nil
    ) async throws -> PostComment {
        try await client.send(
            try .json(
                "network/posts/\(postId)/comments",
                method: .post,
                body: CreateCommentRequest(
                    contenido: contenido,
                    idComentarioPadre: idComentarioPadre
                )
            )
        )
    }

    /// `PUT /network/comments/{id}`
    func updateComment(id: Int, contenido: String) async throws -> PostComment {
        try await client.send(
            try .json(
                "network/comments/\(id)",
                method: .put,
                body: UpdateCommentRequest(contenido: contenido)
            )
        )
    }

    /// `DELETE /network/comments/{id}`
    func deleteComment(id: Int) async throws -> PostComment {
        try await client.send(.empty("network/comments/\(id)", method: .delete))
    }
}
