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
import kotlinx.coroutines.Job

class MarketplaceViewModel(
    private val repository: MarketplaceRepository = MarketplaceRepository()
) : ViewModel() {
    var uiState by mutableStateOf(MarketplaceUiState())
        private set

    private val refreshPolicy = RefreshPolicy()
    private var quickTaskSearchJob: Job? = null
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
            it.copy(
                publishError = null,
                publishedTask = null,
                taskMutationError = null,
                updatedTask = null,
                cancelledTaskId = null
            )
        }
    }

    fun updateTask(taskId: Int, request: CreateTaskRequest) {
        viewModelScope.launch {
            updateState {
                it.copy(
                    isUpdatingTask = true,
                    taskMutationError = null,
                    updatedTask = null
                )
            }
            when (val result = repository.updateTask(taskId, request)) {
                is ApiResult.Success -> updateState { current ->
                    current.copy(
                        isUpdatingTask = false,
                        updatedTask = result.data,
                        tasks = current.tasks.map {
                            if (it.idTarea == result.data.idTarea) result.data else it
                        }
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        isUpdatingTask = false,
                        taskMutationError = result.message
                    )
                }
            }
        }
    }

    fun cancelTask(taskId: Int) {
        viewModelScope.launch {
            updateState {
                it.copy(
                    isUpdatingTask = true,
                    taskMutationError = null,
                    cancelledTaskId = null
                )
            }
            when (val result = repository.cancelTask(taskId)) {
                is ApiResult.Success -> updateState { current ->
                    current.copy(
                        isUpdatingTask = false,
                        cancelledTaskId = taskId,
                        tasks = current.tasks.map {
                            if (it.idTarea == taskId) result.data else it
                        }
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        isUpdatingTask = false,
                        taskMutationError = result.message
                    )
                }
            }
        }
    }

    fun applyToTask(taskId: Int, request: CreateApplicationRequest) {
        applicationActions.applyToTask(taskId, request)
    }

    fun clearApplicationFeedback() {
        applicationActions.clearFeedback()
    }

    fun loadMyApplications(force: Boolean = false) {
        applicationActions.loadMine(force)
    }

    fun loadApplications(taskId: Int, force: Boolean = false) {
        applicationActions.load(taskId, force)
    }

    fun acceptApplication(application: ApplicationDto, paymentMethod: String) {
        applicationActions.accept(application, paymentMethod)
    }

    fun searchQuickTasks(latitude: Double, longitude: Double, radiusKm: Double) {
        quickTaskSearchJob?.cancel()
        quickTaskSearchJob = viewModelScope.launch {
            updateState {
                it.copy(isLoadingQuickTasks = true, quickTasksError = null)
            }
            when (
                val result = repository.loadNearbyQuickTasks(
                    latitude,
                    longitude,
                    radiusKm
                )
            ) {
                is ApiResult.Success -> updateState {
                    it.copy(
                        isLoadingQuickTasks = false,
                        quickTasks = result.data,
                        quickTasksError = null
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        isLoadingQuickTasks = false,
                        quickTasksError = result.message
                    )
                }
            }
        }
    }

    fun claimQuickTask(taskId: Int) {
        if (uiState.claimingQuickTaskId != null) return
        viewModelScope.launch {
            updateState {
                it.copy(
                    claimingQuickTaskId = taskId,
                    claimedQuickJob = null,
                    quickTasksError = null
                )
            }
            when (val result = repository.claimQuickTask(taskId)) {
                is ApiResult.Success -> updateState { current ->
                    current.copy(
                        claimingQuickTaskId = null,
                        claimedQuickJob = result.data,
                        quickTasks = current.quickTasks.filterNot {
                            it.tarea.idTarea == taskId
                        },
                        tasks = current.tasks.map { task ->
                            if (task.idTarea == taskId) {
                                task.copy(estadoTarea = "ASIGNADA")
                            } else {
                                task
                            }
                        }
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        claimingQuickTaskId = null,
                        quickTasksError = result.message
                    )
                }
            }
        }
    }

    fun clearQuickTaskFeedback() {
        updateState {
            it.copy(quickTasksError = null, claimedQuickJob = null)
        }
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

    fun loadWallet() {
        if (uiState.isLoadingWallet) return
        viewModelScope.launch {
            updateState {
                it.copy(isLoadingWallet = true, walletError = null)
            }
            when (val result = repository.loadWallet()) {
                is ApiResult.Success -> updateState {
                    it.copy(
                        wallet = result.data,
                        isLoadingWallet = false,
                        walletError = null
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        isLoadingWallet = false,
                        walletError = result.message
                    )
                }
            }
        }
    }

    fun openPaymentCheckout(jobId: Int, paymentId: Int) {
        if (uiState.processingPaymentId != null) return
        viewModelScope.launch {
            updateState {
                it.copy(
                    processingPaymentId = paymentId,
                    walletError = null,
                    paymentMessage = null
                )
            }
            when (val result = repository.createPaymentCheckout(jobId)) {
                is ApiResult.Success -> updateState {
                    it.copy(
                        processingPaymentId = null,
                        checkoutUrl = result.data.checkoutUrl,
                        paymentMessage = "Checkout de Pagadito abierto."
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        processingPaymentId = null,
                        walletError = result.message
                    )
                }
            }
        }
    }

    fun refreshPayment(paymentId: Int) {
        if (uiState.processingPaymentId != null) return
        viewModelScope.launch {
            updateState {
                it.copy(
                    processingPaymentId = paymentId,
                    walletError = null,
                    paymentMessage = null
                )
            }
            when (val result = repository.refreshPayment(paymentId)) {
                is ApiResult.Success -> {
                    updateState {
                        it.copy(
                            processingPaymentId = null,
                            paymentMessage = "Estado del pago actualizado."
                        )
                    }
                    loadWallet()
                    refreshJobs(force = true)
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        processingPaymentId = null,
                        walletError = result.message
                    )
                }
            }
        }
    }

    fun confirmCashReceipt(jobId: Int, paymentId: Int) {
        if (uiState.processingPaymentId != null) return
        viewModelScope.launch {
            updateState {
                it.copy(
                    processingPaymentId = paymentId,
                    walletError = null,
                    paymentMessage = null
                )
            }
            when (val result = repository.confirmCashReceipt(jobId)) {
                is ApiResult.Success -> updateState { current ->
                    current.copy(
                        processingPaymentId = null,
                        jobs = current.jobs.map { job ->
                            if (job.idTrabajo == jobId) {
                                job.copy(
                                    estadoTrabajo = "FINALIZADO",
                                    pago = result.data
                                )
                            } else {
                                job
                            }
                        },
                        paymentMessage = "Pago en efectivo confirmado. Trabajo finalizado."
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        processingPaymentId = null,
                        walletError = result.message
                    )
                }
            }
        }
    }

    fun clearCheckoutUrl() {
        updateState { it.copy(checkoutUrl = null) }
    }

    fun clearPaymentFeedback() {
        updateState { it.copy(paymentMessage = null, walletError = null) }
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

    fun uploadStudentVerificationProof(attachment: PendingAttachment) {
        viewModelScope.launch {
            updateState {
                it.copy(
                    isUploadingStudentProof = true,
                    studentProofMessage = null,
                    studentProofError = null
                )
            }
            when (val result = repository.uploadStudentVerificationAttachment(attachment)) {
                is ApiResult.Success -> updateState {
                    it.copy(
                        isUploadingStudentProof = false,
                        studentProofMessage =
                            "Documento enviado. Tu perfil quedo pendiente de revision."
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        isUploadingStudentProof = false,
                        studentProofError = result.message
                    )
                }
            }
        }
    }

    fun reportTask(
        taskId: Int,
        category: String,
        description: String?
    ) {
        viewModelScope.launch {
            updateState {
                it.copy(
                    isReportingTask = true,
                    reportedTaskId = null,
                    taskReportMessage = null,
                    taskReportError = null
                )
            }
            when (
                val result = repository.createTaskReport(
                    taskId,
                    category,
                    description
                )
            ) {
                is ApiResult.Success -> updateState {
                    it.copy(
                        isReportingTask = false,
                        reportedTaskId = taskId,
                        taskReportMessage =
                            "Reporte enviado. El equipo de moderacion lo revisara."
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        isReportingTask = false,
                        taskReportError = result.message
                    )
                }
            }
        }
    }

    fun clearTaskReportFeedback() {
        updateState {
            it.copy(
                reportedTaskId = null,
                taskReportMessage = null,
                taskReportError = null
            )
        }
    }

    fun loadAdminDashboard(force: Boolean = false) {
        if (uiState.isLoadingAdmin || (!force && uiState.adminSummary != null)) {
            return
        }
        viewModelScope.launch {
            updateState {
                it.copy(
                    isLoadingAdmin = true,
                    adminError = null,
                    adminMessage = null
                )
            }
            when (val result = repository.loadAdminDashboard()) {
                is ApiResult.Success -> updateState {
                    it.copy(
                        isLoadingAdmin = false,
                        adminSummary = result.data.summary,
                        adminTasks = result.data.tasks,
                        adminReports = result.data.reports,
                        pendingStudentVerifications = result.data.verifications
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        isLoadingAdmin = false,
                        adminError = result.message
                    )
                }
            }
        }
    }

    fun approveStudentVerification(userId: Int) {
        reviewStudentVerification(userId, approved = true, observation = null)
    }

    fun rejectStudentVerification(userId: Int, observation: String?) {
        reviewStudentVerification(userId, approved = false, observation = observation)
    }

    fun cancelTaskAsAdmin(taskId: Int) {
        viewModelScope.launch {
            updateState {
                it.copy(
                    adminActionKey = "task:$taskId",
                    adminError = null,
                    adminMessage = null
                )
            }
            when (val result = repository.cancelTaskAsAdmin(taskId)) {
                is ApiResult.Success -> updateState { current ->
                    current.copy(
                        adminActionKey = null,
                        adminTasks = current.adminTasks.map {
                            if (it.idTarea == taskId) result.data else it
                        },
                        adminSummary = current.adminSummary?.copy(
                            publicacionesActivas =
                                (current.adminSummary.publicacionesActivas - 1).coerceAtLeast(0)
                        ),
                        adminMessage = "Publicacion retirada del marketplace."
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        adminActionKey = null,
                        adminError = result.message
                    )
                }
            }
        }
    }

    fun reviewReport(
        reportId: Int,
        status: String,
        observation: String?,
        removeTask: Boolean
    ) {
        viewModelScope.launch {
            updateState {
                it.copy(
                    adminActionKey = "report:$reportId",
                    adminError = null,
                    adminMessage = null
                )
            }
            when (
                val result = repository.reviewReport(
                    reportId,
                    status,
                    observation,
                    removeTask
                )
            ) {
                is ApiResult.Success -> updateState { current ->
                    val affectedTaskId = result.data.idTarea
                    val removedActiveTask = removeTask &&
                        current.adminTasks.any {
                            it.idTarea == affectedTaskId &&
                                it.estadoTarea.equals("PUBLICADA", true)
                        }
                    current.copy(
                        adminActionKey = null,
                        adminReports = current.adminReports.map {
                            if (it.idReporte == reportId) result.data else it
                        },
                        adminTasks = if (removeTask && affectedTaskId != null) {
                            current.adminTasks.map {
                                if (it.idTarea == affectedTaskId) {
                                    it.copy(estadoTarea = "CANCELADA")
                                } else {
                                    it
                                }
                            }
                        } else {
                            current.adminTasks
                        },
                        adminSummary = current.adminSummary?.copy(
                            reportesPendientes =
                                (current.adminSummary.reportesPendientes - 1)
                                    .coerceAtLeast(0),
                            publicacionesActivas =
                                if (removedActiveTask) {
                                    (current.adminSummary.publicacionesActivas - 1)
                                        .coerceAtLeast(0)
                                } else {
                                    current.adminSummary.publicacionesActivas
                                }
                        ),
                        adminMessage = if (removeTask) {
                            "Reporte resuelto y publicacion retirada."
                        } else if (status.equals("RESUELTO", true)) {
                            "Reporte marcado como resuelto."
                        } else {
                            "Reporte descartado."
                        }
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        adminActionKey = null,
                        adminError = result.message
                    )
                }
            }
        }
    }

    fun clearAdminFeedback() {
        updateState { it.copy(adminMessage = null, adminError = null) }
    }

    private fun reviewStudentVerification(
        userId: Int,
        approved: Boolean,
        observation: String?
    ) {
        viewModelScope.launch {
            updateState {
                it.copy(
                    adminActionKey = "verification:$userId",
                    adminError = null,
                    adminMessage = null
                )
            }
            val result = if (approved) {
                repository.approveStudentVerification(userId, observation)
            } else {
                repository.rejectStudentVerification(userId, observation)
            }
            when (result) {
                is ApiResult.Success -> updateState { current ->
                    current.copy(
                        adminActionKey = null,
                        pendingStudentVerifications =
                            current.pendingStudentVerifications.filterNot {
                                it.idUsuario == userId
                            },
                        adminSummary = current.adminSummary?.copy(
                            verificacionesPendientes =
                                (current.adminSummary.verificacionesPendientes - 1)
                                    .coerceAtLeast(0)
                        ),
                        adminMessage = if (approved) {
                            "Perfil estudiantil aprobado."
                        } else {
                            "Solicitud estudiantil rechazada."
                        }
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        adminActionKey = null,
                        adminError = result.message
                    )
                }
            }
        }
    }

    private fun updateState(
        transform: (MarketplaceUiState) -> MarketplaceUiState
    ) {
        uiState = transform(uiState)
    }
}
