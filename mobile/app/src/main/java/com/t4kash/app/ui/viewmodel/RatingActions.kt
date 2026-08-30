package com.t4kash.app.ui.viewmodel

import com.t4kash.app.ui.model.CreateRatingRequest
import com.t4kash.app.ui.repository.MarketplaceRepository
import com.t4kash.app.ui.service.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class RatingActions(
    private val repository: MarketplaceRepository,
    private val scope: CoroutineScope,
    private val state: MarketplaceStateProvider,
    private val updateState: MarketplaceStateUpdater
) {
    fun load(jobId: Int, force: Boolean = false) {
        val current = state()
        val loadingId = current.managedJobId.takeIf { current.isLoadingRatings }
        if (
            !shouldLoadResource(
                requestedId = jobId,
                loadedId = current.loadedRatingsJobId,
                loadingId = loadingId,
                force = force
            )
        ) {
            return
        }
        scope.launch {
            updateState {
                it.copy(
                    managedJobId = jobId,
                    ratings = if (it.loadedRatingsJobId == jobId) it.ratings else emptyList(),
                    isLoadingRatings = true,
                    ratingsError = null
                )
            }
            when (val result = repository.loadRatings(jobId)) {
                is ApiResult.Success -> updateState {
                    if (it.managedJobId != jobId) {
                        it
                    } else {
                        it.copy(
                            loadedRatingsJobId = jobId,
                            ratings = result.data,
                            isLoadingRatings = false
                        )
                    }
                }

                is ApiResult.Error -> updateState {
                    if (it.managedJobId != jobId) {
                        it
                    } else {
                        it.copy(
                            isLoadingRatings = false,
                            ratingsError = result.message
                        )
                    }
                }
            }
        }
    }

    fun submit(jobId: Int, puntuacion: Int, comentario: String?) {
        if (state().isSubmittingRating) return
        scope.launch {
            updateState {
                it.copy(
                    isSubmittingRating = true,
                    ratingsError = null,
                    ratingActionMessage = null
                )
            }
            val request = CreateRatingRequest(
                puntuacion = puntuacion,
                comentario = comentario?.trim()?.takeIf { it.isNotEmpty() }
            )
            when (val result = repository.createRating(jobId, request)) {
                is ApiResult.Success -> updateState { current ->
                    val existingRatings = if (current.loadedRatingsJobId == jobId) {
                        current.ratings
                    } else {
                        emptyList()
                    }
                    current.copy(
                        loadedRatingsJobId = jobId,
                        managedJobId = jobId,
                        ratings = listOf(result.data) + existingRatings,
                        isSubmittingRating = false,
                        ratingActionMessage = "Calificacion enviada correctamente."
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        isSubmittingRating = false,
                        ratingsError = result.message
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        updateState {
            it.copy(ratingsError = null, ratingActionMessage = null)
        }
    }
}
