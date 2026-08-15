package com.t4kash.app.ui.viewmodel

import com.t4kash.app.ui.model.AttachmentDto
import com.t4kash.app.ui.model.PendingAttachment
import com.t4kash.app.ui.repository.MarketplaceRepository
import com.t4kash.app.ui.service.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class AttachmentActions(
    private val repository: MarketplaceRepository,
    private val scope: CoroutineScope,
    private val state: MarketplaceStateProvider,
    private val updateState: MarketplaceStateUpdater
) {
    fun loadForTask(taskId: Int, force: Boolean = false) {
        val current = state()
        if (
            !shouldLoadResource(
                requestedId = taskId,
                loadedId = current.loadedTaskAttachmentsTaskId,
                loadingId = current.loadingTaskAttachmentsTaskId,
                force = force
            )
        ) {
            return
        }
        scope.launch {
            updateState {
                it.copy(
                    loadingTaskAttachmentsTaskId = taskId,
                    taskAttachments = if (
                        it.loadedTaskAttachmentsTaskId == taskId
                    ) {
                        it.taskAttachments
                    } else {
                        emptyList()
                    },
                    isLoadingTaskAttachments = true,
                    taskAttachmentsError = null
                )
            }
            when (val result = repository.loadTaskAttachments(taskId)) {
                is ApiResult.Success -> updateState {
                    if (it.loadingTaskAttachmentsTaskId != taskId) {
                        it
                    } else {
                        it.copy(
                            loadedTaskAttachmentsTaskId = taskId,
                            loadingTaskAttachmentsTaskId = null,
                            taskAttachments = result.data,
                            isLoadingTaskAttachments = false
                        )
                    }
                }

                is ApiResult.Error -> updateState {
                    if (it.loadingTaskAttachmentsTaskId != taskId) {
                        it
                    } else {
                        it.copy(
                            loadingTaskAttachmentsTaskId = null,
                            isLoadingTaskAttachments = false,
                            taskAttachmentsError = result.message
                        )
                    }
                }
            }
        }
    }

    fun loadForJob(jobId: Int, force: Boolean = false) {
        val current = state()
        if (
            !shouldLoadResource(
                requestedId = jobId,
                loadedId = current.loadedJobAttachmentsJobId,
                loadingId = current.loadingJobAttachmentsJobId,
                force = force
            )
        ) {
            return
        }
        scope.launch {
            updateState {
                it.copy(
                    loadingJobAttachmentsJobId = jobId,
                    jobAttachments = if (
                        it.loadedJobAttachmentsJobId == jobId
                    ) {
                        it.jobAttachments
                    } else {
                        emptyList()
                    },
                    isLoadingJobAttachments = true,
                    jobAttachmentsError = null
                )
            }
            when (val result = repository.loadJobAttachments(jobId)) {
                is ApiResult.Success -> updateState {
                    if (it.loadingJobAttachmentsJobId != jobId) {
                        it
                    } else {
                        it.copy(
                            loadedJobAttachmentsJobId = jobId,
                            loadingJobAttachmentsJobId = null,
                            jobAttachments = result.data,
                            isLoadingJobAttachments = false
                        )
                    }
                }

                is ApiResult.Error -> updateState {
                    if (it.loadingJobAttachmentsJobId != jobId) {
                        it
                    } else {
                        it.copy(
                            loadingJobAttachmentsJobId = null,
                            isLoadingJobAttachments = false,
                            jobAttachmentsError = result.message
                        )
                    }
                }
            }
        }
    }

    fun uploadForTask(
        taskId: Int,
        attachments: List<PendingAttachment>
    ) {
        scope.launch {
            updateState {
                it.copy(
                    isUploadingAttachments = true,
                    attachmentsError = null,
                    attachmentsUploadedTaskId = null
                )
            }
            val uploaded = mutableListOf<AttachmentDto>()
            for (attachment in attachments) {
                when (
                    val result = repository.uploadTaskAttachment(
                        taskId,
                        attachment
                    )
                ) {
                    is ApiResult.Success -> uploaded += result.data
                    is ApiResult.Error -> {
                        mergeTaskAttachments(taskId, uploaded) {
                            it.copy(
                                isUploadingAttachments = false,
                                attachmentsError = result.message
                            )
                        }
                        return@launch
                    }
                }
            }
            mergeTaskAttachments(taskId, uploaded) {
                it.copy(
                    isUploadingAttachments = false,
                    attachmentsUploadedTaskId = taskId
                )
            }
        }
    }

    private fun mergeTaskAttachments(
        taskId: Int,
        uploaded: List<AttachmentDto>,
        transform: (MarketplaceUiState) -> MarketplaceUiState
    ) {
        updateState { current ->
            val existingAttachments = if (
                current.loadedTaskAttachmentsTaskId == taskId
            ) {
                current.taskAttachments
            } else {
                emptyList()
            }
            transform(
                current.copy(
                    loadedTaskAttachmentsTaskId = taskId,
                    taskAttachments = uploaded + existingAttachments
                )
            )
        }
    }

    fun clearFeedback() {
        updateState {
            it.copy(
                taskAttachmentsError = null,
                jobAttachmentsError = null,
                attachmentsError = null,
                attachmentsUploadedTaskId = null
            )
        }
    }
}
