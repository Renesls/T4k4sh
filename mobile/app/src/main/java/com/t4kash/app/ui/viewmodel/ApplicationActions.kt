package com.t4kash.app.ui.viewmodel

import com.t4kash.app.ui.model.ApplicationDto
import com.t4kash.app.ui.model.CreateApplicationRequest
import com.t4kash.app.ui.repository.MarketplaceRepository
import com.t4kash.app.ui.service.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class ApplicationActions(
    private val repository: MarketplaceRepository,
    private val scope: CoroutineScope,
    private val state: MarketplaceStateProvider,
    private val updateState: MarketplaceStateUpdater
) {
    fun applyToTask(taskId: Int, request: CreateApplicationRequest) {
        scope.launch {
            updateState {
                it.copy(
                    isApplying = true,
                    applicationError = null,
                    sentApplication = null
                )
            }
            when (val result = repository.applyToTask(taskId, request)) {
                is ApiResult.Success -> updateState {
                    it.copy(
                        isApplying = false,
                        sentApplication = result.data
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        isApplying = false,
                        applicationError = result.message
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        updateState {
            it.copy(
                applicationError = null,
                sentApplication = null
            )
        }
    }

    fun load(taskId: Int, force: Boolean = false) {
        val current = state()
        val loadingId = current.managedTaskId.takeIf {
            current.isLoadingApplications
        }
        if (
            !shouldLoadResource(
                requestedId = taskId,
                loadedId = current.loadedApplicationsTaskId,
                loadingId = loadingId,
                force = force
            )
        ) {
            return
        }
        scope.launch {
            updateState {
                it.copy(
                    managedTaskId = taskId,
                    applications = if (
                        it.loadedApplicationsTaskId == taskId
                    ) {
                        it.applications
                    } else {
                        emptyList()
                    },
                    isLoadingApplications = true,
                    applicationsError = null,
                    applicationActionMessage = null
                )
            }
            when (val result = repository.loadApplications(taskId)) {
                is ApiResult.Success -> updateState {
                    if (it.managedTaskId != taskId) {
                        it
                    } else {
                        it.copy(
                            loadedApplicationsTaskId = taskId,
                            applications = result.data,
                            isLoadingApplications = false
                        )
                    }
                }

                is ApiResult.Error -> updateState {
                    if (it.managedTaskId != taskId) {
                        it
                    } else {
                        it.copy(
                            isLoadingApplications = false,
                            applicationsError = result.message
                        )
                    }
                }
            }
        }
    }

    fun accept(application: ApplicationDto) {
        scope.launch {
            updateState {
                it.copy(
                    updatingApplicationId = application.idPostulacion,
                    applicationsError = null,
                    applicationActionMessage = null
                )
            }
            when (
                val result = repository.acceptApplication(
                    application.idPostulacion
                )
            ) {
                is ApiResult.Success -> updateState { current ->
                    current.copy(
                        updatingApplicationId = null,
                        applications = current.applications.map {
                            when {
                                it.idPostulacion == application.idPostulacion ->
                                    it.copy(estadoPostulacion = "ACEPTADA")

                                it.idTarea == application.idTarea &&
                                    it.estadoPostulacion.equals(
                                        "PENDIENTE",
                                        ignoreCase = true
                                    ) -> it.copy(estadoPostulacion = "RECHAZADA")

                                else -> it
                            }
                        },
                        tasks = current.tasks.map {
                            if (it.idTarea == result.data.idTarea) {
                                it.copy(estadoTarea = "ASIGNADA")
                            } else {
                                it
                            }
                        },
                        jobs = listOf(result.data) + current.jobs.filterNot {
                            it.idTrabajo == result.data.idTrabajo
                        },
                        applicationActionMessage =
                            "Postulación aceptada. Trabajo #${result.data.idTrabajo} creado."
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        updatingApplicationId = null,
                        applicationsError = result.message
                    )
                }
            }
        }
    }

    fun reject(application: ApplicationDto) {
        scope.launch {
            updateState {
                it.copy(
                    updatingApplicationId = application.idPostulacion,
                    applicationsError = null,
                    applicationActionMessage = null
                )
            }
            when (
                val result = repository.rejectApplication(
                    application.idPostulacion
                )
            ) {
                is ApiResult.Success -> updateState { current ->
                    current.copy(
                        updatingApplicationId = null,
                        applications = current.applications.map {
                            if (it.idPostulacion == result.data.idPostulacion) {
                                result.data
                            } else {
                                it
                            }
                        },
                        applicationActionMessage = "Postulación rechazada."
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        updatingApplicationId = null,
                        applicationsError = result.message
                    )
                }
            }
        }
    }

    fun clearActionMessage() {
        updateState { it.copy(applicationActionMessage = null) }
    }
}
