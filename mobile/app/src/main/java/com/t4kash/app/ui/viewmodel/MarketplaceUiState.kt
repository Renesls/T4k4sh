package com.t4kash.app.ui.viewmodel

import com.t4kash.app.ui.model.ApplicationDto
import com.t4kash.app.ui.model.AttachmentDto
import com.t4kash.app.ui.model.CategoryDto
import com.t4kash.app.ui.model.DeliveryDto
import com.t4kash.app.ui.model.JobDto
import com.t4kash.app.ui.model.TaskDto

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
    val sentApplication: ApplicationDto? = null,
    val myApplications: List<ApplicationDto> = emptyList(),
    val isLoadingMyApplications: Boolean = false,
    val myApplicationsError: String? = null,
    val managedTaskId: Int? = null,
    val loadedApplicationsTaskId: Int? = null,
    val applications: List<ApplicationDto> = emptyList(),
    val isLoadingApplications: Boolean = false,
    val applicationsError: String? = null,
    val updatingApplicationId: Int? = null,
    val applicationActionMessage: String? = null,
    val jobs: List<JobDto> = emptyList(),
    val isLoadingJobs: Boolean = false,
    val jobsError: String? = null,
    val managedJobId: Int? = null,
    val loadedDeliveriesJobId: Int? = null,
    val deliveries: List<DeliveryDto> = emptyList(),
    val isLoadingDeliveries: Boolean = false,
    val deliveriesError: String? = null,
    val isSendingDelivery: Boolean = false,
    val approvingDeliveryId: Int? = null,
    val deliveryActionMessage: String? = null,
    val loadedTaskAttachmentsTaskId: Int? = null,
    val loadingTaskAttachmentsTaskId: Int? = null,
    val taskAttachments: List<AttachmentDto> = emptyList(),
    val loadedJobAttachmentsJobId: Int? = null,
    val loadingJobAttachmentsJobId: Int? = null,
    val jobAttachments: List<AttachmentDto> = emptyList(),
    val isLoadingTaskAttachments: Boolean = false,
    val isLoadingJobAttachments: Boolean = false,
    val isUploadingAttachments: Boolean = false,
    val taskAttachmentsError: String? = null,
    val jobAttachmentsError: String? = null,
    val attachmentsError: String? = null,
    val attachmentsUploadedTaskId: Int? = null,
    val isUpdatingTask: Boolean = false,
    val updatedTask: TaskDto? = null,
    val taskMutationError: String? = null,
    val cancelledTaskId: Int? = null,
    val isUploadingStudentProof: Boolean = false,
    val studentProofMessage: String? = null,
    val studentProofError: String? = null
)
