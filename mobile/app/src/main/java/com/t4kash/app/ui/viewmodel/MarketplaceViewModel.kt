package com.t4kash.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t4kash.app.ui.model.ApplicationDto
import com.t4kash.app.ui.model.CategoryDto
import com.t4kash.app.ui.model.CreateApplicationRequest
import com.t4kash.app.ui.model.CreateTaskRequest
import com.t4kash.app.ui.model.TaskDto
import com.t4kash.app.ui.repository.MarketplaceRepository
import com.t4kash.app.ui.service.ApiResult
import kotlinx.coroutines.launch

data class MarketplaceUiState(
    val isLoading: Boolean = false,
    val categories: List<CategoryDto> = emptyList(),
    val tasks: List<TaskDto> = emptyList(),
    val errorMessage: String? = null,
    val isPublishing: Boolean = false,
    val publishError: String? = null,
    val publishedTask: TaskDto? = null,
    val isApplying: Boolean = false,
    val applicationError: String? = null,
    val sentApplication: ApplicationDto? = null
)

class MarketplaceViewModel(
    private val repository: MarketplaceRepository = MarketplaceRepository()
) : ViewModel() {
    var uiState by mutableStateOf(MarketplaceUiState(isLoading = true))
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            when (val result = repository.loadHomeData()) {
                is ApiResult.Success -> {
                    uiState = MarketplaceUiState(
                        categories = result.data.categories,
                        tasks = result.data.tasks
                    )
                }

                is ApiResult.Error -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun publishTask(request: CreateTaskRequest) {
        viewModelScope.launch {
            uiState = uiState.copy(
                isPublishing = true,
                publishError = null,
                publishedTask = null
            )
            when (val result = repository.createTask(request)) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(
                        isPublishing = false,
                        tasks = listOf(result.data) + uiState.tasks.filterNot {
                            it.idTarea == result.data.idTarea
                        },
                        publishedTask = result.data
                    )
                }

                is ApiResult.Error -> {
                    uiState = uiState.copy(
                        isPublishing = false,
                        publishError = result.message
                    )
                }
            }
        }
    }

    fun clearPublishFeedback() {
        uiState = uiState.copy(publishError = null, publishedTask = null)
    }

    fun applyToTask(taskId: Int, request: CreateApplicationRequest) {
        viewModelScope.launch {
            uiState = uiState.copy(
                isApplying = true,
                applicationError = null,
                sentApplication = null
            )
            when (val result = repository.applyToTask(taskId, request)) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(
                        isApplying = false,
                        sentApplication = result.data
                    )
                }

                is ApiResult.Error -> {
                    uiState = uiState.copy(
                        isApplying = false,
                        applicationError = result.message
                    )
                }
            }
        }
    }

    fun clearApplicationFeedback() {
        uiState = uiState.copy(
            applicationError = null,
            sentApplication = null
        )
    }
}
