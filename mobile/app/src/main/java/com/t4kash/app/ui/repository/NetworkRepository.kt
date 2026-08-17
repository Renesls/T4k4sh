package com.t4kash.app.ui.repository

import com.t4kash.app.ui.model.CreateNetworkCommentRequest
import com.t4kash.app.ui.model.CreateNetworkPostRequest
import com.t4kash.app.ui.model.NetworkCommentDto
import com.t4kash.app.ui.model.NetworkPostDto
import com.t4kash.app.ui.model.NetworkReactionRequest
import com.t4kash.app.ui.model.UpdateNetworkCommentRequest
import com.t4kash.app.ui.model.UpdateNetworkPostRequest
import com.t4kash.app.ui.service.ApiResult
import com.t4kash.app.ui.service.NetworkApiService
import com.t4kash.app.ui.service.RetrofitClient
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import retrofit2.HttpException

class NetworkRepository(
    private val api: NetworkApiService = RetrofitClient.networkApiService
) {
    suspend fun loadFeed(scope: String): ApiResult<List<NetworkPostDto>> =
        execute("No se pudo cargar el Network.") {
            api.getFeed(scope)
        }

    suspend fun loadSavedPosts(): ApiResult<List<NetworkPostDto>> =
        execute("No se pudieron cargar tus publicaciones guardadas.") {
            api.getSavedPosts()
        }

    suspend fun createPost(
        request: CreateNetworkPostRequest
    ): ApiResult<NetworkPostDto> = execute("No se pudo publicar el contenido.") {
        api.createPost(request)
    }

    suspend fun updatePost(
        postId: Int,
        request: UpdateNetworkPostRequest
    ): ApiResult<NetworkPostDto> = execute("No se pudo editar la publicacion.") {
        api.updatePost(postId, request)
    }

    suspend fun deletePost(postId: Int): ApiResult<NetworkPostDto> =
        execute("No se pudo eliminar la publicacion.") {
            api.deletePost(postId)
        }

    suspend fun setReaction(
        postId: Int,
        reaction: String
    ): ApiResult<NetworkPostDto> = execute("No se pudo registrar tu reaccion.") {
        api.setReaction(postId, NetworkReactionRequest(reaction))
    }

    suspend fun removeReaction(postId: Int): ApiResult<NetworkPostDto> =
        execute("No se pudo quitar tu reaccion.") {
            api.removeReaction(postId)
        }

    suspend fun setSaved(
        postId: Int,
        saved: Boolean
    ): ApiResult<NetworkPostDto> = execute("No se pudo actualizar el guardado.") {
        if (saved) api.savePost(postId) else api.removeSavedPost(postId)
    }

    suspend fun loadComments(
        postId: Int
    ): ApiResult<List<NetworkCommentDto>> = execute(
        "No se pudieron cargar los comentarios."
    ) {
        api.getComments(postId)
    }

    suspend fun createComment(
        postId: Int,
        content: String,
        parentCommentId: Int?
    ): ApiResult<NetworkCommentDto> = execute("No se pudo publicar el comentario.") {
        api.createComment(
            postId,
            CreateNetworkCommentRequest(content, parentCommentId)
        )
    }

    suspend fun updateComment(
        commentId: Int,
        content: String
    ): ApiResult<NetworkCommentDto> = execute("No se pudo editar el comentario.") {
        api.updateComment(commentId, UpdateNetworkCommentRequest(content))
    }

    suspend fun deleteComment(
        commentId: Int
    ): ApiResult<NetworkCommentDto> = execute("No se pudo eliminar el comentario.") {
        api.deleteComment(commentId)
    }

    private suspend fun <T> execute(
        fallback: String,
        block: suspend () -> T
    ): ApiResult<T> {
        return try {
            ApiResult.Success(block())
        } catch (exception: Exception) {
            ApiResult.Error(exception.networkApiMessage(fallback))
        }
    }
}

private fun Exception.networkApiMessage(fallback: String): String {
    if (this is CancellationException) throw this
    if (this is HttpException) {
        val detail = runCatching {
            val body = response()?.errorBody()?.string().orEmpty()
            val json = JSONObject(body)
            json.optString("detail").ifBlank {
                json.optString("message")
            }
        }.getOrNull()
        if (!detail.isNullOrBlank()) return detail
    }
    return message?.takeIf { it.isNotBlank() } ?: fallback
}
