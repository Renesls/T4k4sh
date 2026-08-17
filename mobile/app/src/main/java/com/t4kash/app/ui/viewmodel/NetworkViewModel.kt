package com.t4kash.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t4kash.app.ui.model.CreateNetworkPostRequest
import com.t4kash.app.ui.model.NetworkCommentDto
import com.t4kash.app.ui.model.NetworkFeedScope
import com.t4kash.app.ui.model.NetworkPostDto
import com.t4kash.app.ui.model.UpdateNetworkPostRequest
import com.t4kash.app.ui.repository.NetworkRepository
import com.t4kash.app.ui.service.ApiResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class NetworkUiState(
    val posts: List<NetworkPostDto> = emptyList(),
    val selectedScope: NetworkFeedScope = NetworkFeedScope.FOR_YOU,
    val showingSaved: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val busyPostIds: Set<Int> = emptySet(),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val activeCommentsPost: NetworkPostDto? = null,
    val comments: List<NetworkCommentDto> = emptyList(),
    val isLoadingComments: Boolean = false,
    val isSubmittingComment: Boolean = false,
    val commentsError: String? = null
)

class NetworkViewModel(
    private val repository: NetworkRepository = NetworkRepository()
) : ViewModel() {
    var uiState by mutableStateOf(NetworkUiState())
        private set

    private var feedJob: Job? = null
    private var commentsJob: Job? = null

    fun refresh(showLoading: Boolean = uiState.posts.isEmpty()) {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = showLoading,
                isRefreshing = !showLoading,
                errorMessage = null
            )
            val result = if (uiState.showingSaved) {
                repository.loadSavedPosts()
            } else {
                repository.loadFeed(uiState.selectedScope.apiValue)
            }
            when (result) {
                is ApiResult.Success -> uiState = uiState.copy(
                    posts = result.data,
                    isLoading = false,
                    isRefreshing = false
                )

                is ApiResult.Error -> uiState = uiState.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun selectScope(scope: NetworkFeedScope) {
        if (!uiState.showingSaved && uiState.selectedScope == scope) {
            refresh(showLoading = false)
            return
        }
        uiState = uiState.copy(
            selectedScope = scope,
            showingSaved = false,
            posts = emptyList()
        )
        refresh(showLoading = true)
    }

    fun showSavedPosts() {
        if (uiState.showingSaved) {
            uiState = uiState.copy(showingSaved = false, posts = emptyList())
        } else {
            uiState = uiState.copy(showingSaved = true, posts = emptyList())
        }
        refresh(showLoading = true)
    }

    fun submitPost(
        editingPostId: Int?,
        content: String,
        type: String,
        visibility: String,
        commentsEnabled: Boolean,
        onSuccess: () -> Unit
    ) {
        val cleanContent = content.trim()
        if (cleanContent.isBlank() || uiState.isSubmitting) return
        viewModelScope.launch {
            uiState = uiState.copy(
                isSubmitting = true,
                errorMessage = null,
                infoMessage = null
            )
            val result = if (editingPostId == null) {
                repository.createPost(
                    CreateNetworkPostRequest(
                        contenido = cleanContent,
                        tipoPublicacion = type,
                        visibilidad = visibility,
                        permiteComentarios = commentsEnabled
                    )
                )
            } else {
                repository.updatePost(
                    editingPostId,
                    UpdateNetworkPostRequest(
                        contenido = cleanContent,
                        tipoPublicacion = type,
                        visibilidad = visibility,
                        permiteComentarios = commentsEnabled
                    )
                )
            }
            when (result) {
                is ApiResult.Success -> {
                    val updatedPosts = if (editingPostId == null) {
                        if (uiState.showingSaved) {
                            uiState.posts
                        } else {
                            listOf(result.data) + uiState.posts
                        }
                    } else {
                        uiState.posts.replacePost(result.data)
                    }
                    uiState = uiState.copy(
                        posts = updatedPosts.distinctBy { it.idPublicacion },
                        isSubmitting = false,
                        infoMessage = if (editingPostId == null) {
                            "Publicacion creada."
                        } else {
                            "Publicacion actualizada."
                        }
                    )
                    onSuccess()
                }

                is ApiResult.Error -> uiState = uiState.copy(
                    isSubmitting = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun deletePost(postId: Int) {
        runPostAction(postId) {
            when (val result = repository.deletePost(postId)) {
                is ApiResult.Success -> uiState = uiState.copy(
                    posts = uiState.posts.filterNot { it.idPublicacion == postId },
                    infoMessage = "Publicacion eliminada."
                )

                is ApiResult.Error -> showActionError(result.message)
            }
        }
    }

    fun react(post: NetworkPostDto, reaction: String) {
        runPostAction(post.idPublicacion) {
            val result = if (post.miReaccion == reaction) {
                repository.removeReaction(post.idPublicacion)
            } else {
                repository.setReaction(post.idPublicacion, reaction)
            }
            applyPostResult(result)
        }
    }

    fun toggleSaved(post: NetworkPostDto) {
        runPostAction(post.idPublicacion) {
            when (
                val result = repository.setSaved(
                    post.idPublicacion,
                    !post.guardada
                )
            ) {
                is ApiResult.Success -> {
                    val posts = if (uiState.showingSaved && !result.data.guardada) {
                        uiState.posts.filterNot {
                            it.idPublicacion == result.data.idPublicacion
                        }
                    } else {
                        uiState.posts.replacePost(result.data)
                    }
                    uiState = uiState.copy(
                        posts = posts,
                        infoMessage = if (result.data.guardada) {
                            "Publicacion guardada."
                        } else {
                            "Se quito de guardadas."
                        }
                    )
                }

                is ApiResult.Error -> showActionError(result.message)
            }
        }
    }

    fun sharePost(post: NetworkPostDto) {
        runPostAction(post.idPublicacion) {
            when (
                val result = repository.createPost(
                    CreateNetworkPostRequest(
                        contenido = null,
                        tipoPublicacion = "COMPARTIDA",
                        visibilidad = "PUBLICA",
                        permiteComentarios = true,
                        idPublicacionOrigen = post.idPublicacionOrigen
                            ?: post.idPublicacion
                    )
                )
            ) {
                is ApiResult.Success -> uiState = uiState.copy(
                    posts = uiState.posts.replacePost(
                        post.copy(totalCompartidas = post.totalCompartidas + 1)
                    ).let { currentPosts ->
                        if (uiState.showingSaved) {
                            currentPosts
                        } else {
                            listOf(result.data) + currentPosts
                        }
                    },
                    infoMessage = "Publicacion compartida."
                )

                is ApiResult.Error -> showActionError(result.message)
            }
        }
    }

    fun openComments(post: NetworkPostDto) {
        commentsJob?.cancel()
        uiState = uiState.copy(
            activeCommentsPost = post,
            comments = emptyList(),
            isLoadingComments = true,
            commentsError = null
        )
        commentsJob = viewModelScope.launch {
            when (val result = repository.loadComments(post.idPublicacion)) {
                is ApiResult.Success -> {
                    if (uiState.activeCommentsPost?.idPublicacion != post.idPublicacion) {
                        return@launch
                    }
                    uiState = uiState.copy(
                        comments = result.data,
                        isLoadingComments = false
                    )
                }

                is ApiResult.Error -> uiState = uiState.copy(
                    isLoadingComments = false,
                    commentsError = result.message
                )
            }
        }
    }

    fun closeComments() {
        commentsJob?.cancel()
        uiState = uiState.copy(
            activeCommentsPost = null,
            comments = emptyList(),
            commentsError = null
        )
    }

    fun submitComment(
        content: String,
        parentCommentId: Int? = null,
        onSuccess: () -> Unit = {}
    ) {
        val post = uiState.activeCommentsPost ?: return
        val cleanContent = content.trim()
        if (cleanContent.isBlank() || uiState.isSubmittingComment) return
        viewModelScope.launch {
            uiState = uiState.copy(
                isSubmittingComment = true,
                commentsError = null
            )
            when (
                val result = repository.createComment(
                    post.idPublicacion,
                    cleanContent,
                    parentCommentId
                )
            ) {
                is ApiResult.Success -> {
                    val updatedPost = post.copy(
                        totalComentarios = post.totalComentarios + 1
                    )
                    uiState = uiState.copy(
                        comments = uiState.comments + result.data,
                        posts = uiState.posts.replacePost(updatedPost),
                        activeCommentsPost = updatedPost,
                        isSubmittingComment = false
                    )
                    onSuccess()
                }

                is ApiResult.Error -> uiState = uiState.copy(
                    isSubmittingComment = false,
                    commentsError = result.message
                )
            }
        }
    }

    fun updateComment(commentId: Int, content: String, onSuccess: () -> Unit) {
        val cleanContent = content.trim()
        if (cleanContent.isBlank() || uiState.isSubmittingComment) return
        viewModelScope.launch {
            uiState = uiState.copy(isSubmittingComment = true, commentsError = null)
            when (val result = repository.updateComment(commentId, cleanContent)) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(
                        comments = uiState.comments.map {
                            if (it.idComentario == commentId) result.data else it
                        },
                        isSubmittingComment = false
                    )
                    onSuccess()
                }

                is ApiResult.Error -> uiState = uiState.copy(
                    isSubmittingComment = false,
                    commentsError = result.message
                )
            }
        }
    }

    fun deleteComment(comment: NetworkCommentDto) {
        if (uiState.isSubmittingComment) return
        viewModelScope.launch {
            uiState = uiState.copy(isSubmittingComment = true, commentsError = null)
            when (val result = repository.deleteComment(comment.idComentario)) {
                is ApiResult.Success -> {
                    val post = uiState.activeCommentsPost
                    val updatedPost = post?.copy(
                        totalComentarios = (post.totalComentarios - 1).coerceAtLeast(0)
                    )
                    uiState = uiState.copy(
                        comments = uiState.comments.filterNot {
                            it.idComentario == comment.idComentario ||
                                it.idComentarioPadre == comment.idComentario
                        },
                        posts = updatedPost?.let {
                            uiState.posts.replacePost(it)
                        } ?: uiState.posts,
                        activeCommentsPost = updatedPost,
                        isSubmittingComment = false
                    )
                }

                is ApiResult.Error -> uiState = uiState.copy(
                    isSubmittingComment = false,
                    commentsError = result.message
                )
            }
        }
    }

    fun clearFeedback() {
        uiState = uiState.copy(errorMessage = null, infoMessage = null)
    }

    fun clearSession() {
        feedJob?.cancel()
        commentsJob?.cancel()
        uiState = NetworkUiState()
    }

    private fun runPostAction(postId: Int, action: suspend () -> Unit) {
        if (postId in uiState.busyPostIds) return
        viewModelScope.launch {
            uiState = uiState.copy(
                busyPostIds = uiState.busyPostIds + postId,
                errorMessage = null,
                infoMessage = null
            )
            action()
            uiState = uiState.copy(busyPostIds = uiState.busyPostIds - postId)
        }
    }

    private fun applyPostResult(result: ApiResult<NetworkPostDto>) {
        when (result) {
            is ApiResult.Success -> uiState = uiState.copy(
                posts = uiState.posts.replacePost(result.data),
                activeCommentsPost = uiState.activeCommentsPost?.let {
                    if (it.idPublicacion == result.data.idPublicacion) result.data else it
                }
            )

            is ApiResult.Error -> showActionError(result.message)
        }
    }

    private fun showActionError(message: String) {
        uiState = uiState.copy(errorMessage = message)
    }
}

private fun List<NetworkPostDto>.replacePost(
    updated: NetworkPostDto
): List<NetworkPostDto> = map {
    if (it.idPublicacion == updated.idPublicacion) updated else it
}
