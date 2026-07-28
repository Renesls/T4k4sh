package com.t4kash.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t4kash.app.ui.model.ApplicationDto
import com.t4kash.app.ui.model.CreateApplicationRequest
import com.t4kash.app.ui.model.CreateTaskRequest
import com.t4kash.app.ui.model.DeliveryDto
import com.t4kash.app.ui.model.PendingAttachment
import com.t4kash.app.ui.repository.MarketplaceRepository
import com.t4kash.app.ui.service.ApiResult
import kotlinx.coroutines.launch

class MarketplaceViewModel(
    private val repository: MarketplaceRepository = MarketplaceRepository()
) : ViewModel() {
    var uiState by mutableStateOf(MarketplaceUiState())
        private set

    private val refreshPolicy = RefreshPolicy()
    private val applicationActions = ApplicationActions(
        repository = repository,
        scope = viewModelScope,
        state = { uiState },
        updateState = ::updateState
    )
    private val deliveryActions = DeliveryActions(
        repository = repository,
        scope = viewModelScope,
        state = { uiState },
        updateState = ::updateState
    )
    private val attachmentActions = AttachmentActions(
        repository = repository,
        scope = viewModelScope,
        state = { uiState },
        updateState = ::updateState
    )

    init {
        refresh()
    }

    fun refresh(force: Boolean = false) {
        if (
            !refreshPolicy.shouldRefresh(
                target = RefreshTarget.HOME,
                force = force,
                isLoading = uiState.isLoading
            )
        ) {
            return
        }
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.loadHomeData()) {
                is ApiResult.Success -> {
                    refreshPolicy.markSuccessful(RefreshTarget.HOME)
                    updateState {
                        it.copy(
                            isLoading = false,
                            categories = result.data.categories,
                            tasks = result.data.tasks,
                            errorMessage = null
                        )
                    }
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun publishTask(request: CreateTaskRequest) {
        viewModelScope.launch {
            updateState {
                it.copy(
                    isPublishing = true,
                    publishError = null,
                    publishedTask = null
                )
            }
            when (val result = repository.createTask(request)) {
                is ApiResult.Success -> updateState { current ->
                    current.copy(
                        isPublishing = false,
                        tasks = listOf(result.data) + current.tasks.filterNot {
                            it.idTarea == result.data.idTarea
                        },
                        publishedTask = result.data
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        isPublishing = false,
                        publishError = result.message
                    )
                }
            }
        }
    }

    fun clearPublishFeedback() {
        updateState {
            it.copy(publishError = null, publishedTask = null)
        }
    }

    fun applyToTask(taskId: Int, request: CreateApplicationRequest) {
        applicationActions.applyToTask(taskId, request)
    }

    fun clearApplicationFeedback() {
        applicationActions.clearFeedback()
    }

    fun loadApplications(taskId: Int, force: Boolean = false) {
        applicationActions.load(taskId, force)
    }

    fun acceptApplication(application: ApplicationDto) {
        applicationActions.accept(application)
    }

    fun rejectApplication(application: ApplicationDto) {
        applicationActions.reject(application)
    }

    fun clearApplicationActionMessage() {
        applicationActions.clearActionMessage()
    }

    fun refreshJobs(force: Boolean = false) {
        if (
            !refreshPolicy.shouldRefresh(
                target = RefreshTarget.JOBS,
                force = force,
                isLoading = uiState.isLoadingJobs
            )
        ) {
            return
        }
        viewModelScope.launch {
            updateState { it.copy(isLoadingJobs = true, jobsError = null) }
            when (val result = repository.loadJobs()) {
                is ApiResult.Success -> {
                    refreshPolicy.markSuccessful(RefreshTarget.JOBS)
                    updateState {
                        it.copy(
                            jobs = result.data,
                            isLoadingJobs = false
                        )
                    }
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        isLoadingJobs = false,
                        jobsError = result.message
                    )
                }
            }
        }
    }

    fun loadDeliveries(jobId: Int, force: Boolean = false) {
        deliveryActions.load(jobId, force)
    }

    fun submitDelivery(
        jobId: Int,
        description: String,
        attachments: List<PendingAttachment> = emptyList()
    ) {
        deliveryActions.submit(jobId, description, attachments)
    }

    fun approveDelivery(delivery: DeliveryDto) {
        deliveryActions.approve(delivery)
    }

    fun clearDeliveryFeedback() {
        deliveryActions.clearFeedback()
    }

    fun loadTaskAttachments(taskId: Int, force: Boolean = false) {
        attachmentActions.loadForTask(taskId, force)
    }

    fun loadJobAttachments(jobId: Int, force: Boolean = false) {
        attachmentActions.loadForJob(jobId, force)
    }

    fun uploadTaskAttachments(
        taskId: Int,
        attachments: List<PendingAttachment>
    ) {
        attachmentActions.uploadForTask(taskId, attachments)
    }

    fun clearAttachmentFeedback() {
        attachmentActions.clearFeedback()
    }

    private fun updateState(
        transform: (MarketplaceUiState) -> MarketplaceUiState
    ) {
        uiState = transform(uiState)
    }
}
