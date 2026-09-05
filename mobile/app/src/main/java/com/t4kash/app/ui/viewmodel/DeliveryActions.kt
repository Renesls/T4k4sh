package com.t4kash.app.ui.viewmodel

import com.t4kash.app.ui.model.AttachmentDto
import com.t4kash.app.ui.model.CreateDeliveryCommentRequest
import com.t4kash.app.ui.model.CreateDeliveryRequest
import com.t4kash.app.ui.model.DeliveryDto
import com.t4kash.app.ui.model.PendingAttachment
import com.t4kash.app.ui.model.RequestDeliveryChangesRequest
import com.t4kash.app.ui.repository.MarketplaceRepository
import com.t4kash.app.ui.service.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class DeliveryActions(
    private val repository: MarketplaceRepository,
    private val scope: CoroutineScope,
    private val state: MarketplaceStateProvider,
    private val updateState: MarketplaceStateUpdater
) {
    fun load(jobId: Int, force: Boolean = false) {
        val current = state()
        val loadingId = current.managedJobId.takeIf {
            current.isLoadingDeliveries
        }
        if (
            !shouldLoadResource(
                requestedId = jobId,
                loadedId = current.loadedDeliveriesJobId,
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
                    deliveries = if (it.loadedDeliveriesJobId == jobId) {
                        it.deliveries
                    } else {
                        emptyList()
                    },
                    isLoadingDeliveries = true,
                    deliveriesError = null,
                    deliveryActionMessage = null
                )
            }
            when (val result = repository.loadDeliveries(jobId)) {
                is ApiResult.Success -> updateState {
                    if (it.managedJobId != jobId) {
                        it
                    } else {
                        it.copy(
                            loadedDeliveriesJobId = jobId,
                            deliveries = result.data,
                            isLoadingDeliveries = false
                        )
                    }
                }

                is ApiResult.Error -> updateState {
                    if (it.managedJobId != jobId) {
                        it
                    } else {
                        it.copy(
                            isLoadingDeliveries = false,
                            deliveriesError = result.message
                        )
                    }
                }
            }
        }
    }

    fun submit(
        jobId: Int,
        description: String,
        attachments: List<PendingAttachment>
    ) {
        scope.launch {
            updateState {
                it.copy(
                    isSendingDelivery = true,
                    deliveriesError = null,
                    deliveryActionMessage = null
                )
            }
            when (
                val result = repository.createDelivery(
                    jobId,
                    CreateDeliveryRequest(description.trim())
                )
            ) {
                is ApiResult.Success -> submitAttachments(
                    jobId = jobId,
                    delivery = result.data,
                    attachments = attachments
                )

                is ApiResult.Error -> updateState {
                    it.copy(
                        isSendingDelivery = false,
                        deliveriesError = result.message
                    )
                }
            }
        }
    }

    private suspend fun submitAttachments(
        jobId: Int,
        delivery: DeliveryDto,
        attachments: List<PendingAttachment>
    ) {
        updateState { current ->
            val existingDeliveries = if (
                current.loadedDeliveriesJobId == jobId
            ) {
                current.deliveries
            } else {
                emptyList()
            }
            current.copy(
                loadedDeliveriesJobId = jobId,
                managedJobId = jobId,
                deliveries = listOf(delivery) + existingDeliveries
            )
        }
        val uploaded = mutableListOf<AttachmentDto>()
        for (attachment in attachments) {
            when (
                val uploadResult = repository.uploadDeliveryAttachment(
                    delivery.idEntrega,
                    attachment
                )
            ) {
                is ApiResult.Success -> uploaded += uploadResult.data
                is ApiResult.Error -> {
                    updateJobAttachments(jobId, uploaded) {
                        it.copy(
                            isSendingDelivery = false,
                            deliveriesError =
                                "La entrega se registró, pero ${uploadResult.message}"
                        )
                    }
                    return
                }
            }
        }
        updateJobAttachments(jobId, uploaded) {
            it.copy(
                isSendingDelivery = false,
                deliveryActionMessage = if (attachments.isEmpty()) {
                    "Entrega enviada correctamente."
                } else {
                    "Entrega y archivos enviados correctamente."
                }
            )
        }
    }

    private fun updateJobAttachments(
        jobId: Int,
        uploaded: List<AttachmentDto>,
        transform: (MarketplaceUiState) -> MarketplaceUiState
    ) {
        updateState { current ->
            val existingAttachments = if (
                current.loadedJobAttachmentsJobId == jobId
            ) {
                current.jobAttachments
            } else {
                emptyList()
            }
            transform(
                current.copy(
                    loadedJobAttachmentsJobId = jobId,
                    jobAttachments = uploaded + existingAttachments
                )
            )
        }
    }

    fun approve(delivery: DeliveryDto) {
        scope.launch {
            val cashPayment = state().jobs.firstOrNull {
                it.idTrabajo == delivery.idTrabajo
            }?.pago?.metodoPago.equals("EFECTIVO", ignoreCase = true)
            updateState {
                it.copy(
                    approvingDeliveryId = delivery.idEntrega,
                    deliveriesError = null,
                    deliveryActionMessage = null
                )
            }
            when (val result = repository.approveDelivery(delivery.idEntrega)) {
                is ApiResult.Success -> updateState { current ->
                    current.copy(
                        approvingDeliveryId = null,
                        deliveries = current.deliveries.map {
                            if (it.idEntrega == result.data.idEntrega) {
                                result.data
                            } else {
                                it
                            }
                        },
                        jobs = current.jobs.map {
                            if (it.idTrabajo == result.data.idTrabajo) {
                                it.copy(
                                    estadoTrabajo = if (cashPayment) {
                                        "PAGO_EFECTIVO_PENDIENTE"
                                    } else {
                                        "FINALIZADO"
                                    }
                                )
                            } else {
                                it
                            }
                        },
                        deliveryActionMessage = if (cashPayment) {
                            "Entrega aprobada. Falta que el estudiante confirme el efectivo."
                        } else {
                            "Entrega aprobada. Trabajo finalizado."
                        }
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        approvingDeliveryId = null,
                        deliveriesError = result.message
                    )
                }
            }
        }
    }

    fun requestChanges(delivery: DeliveryDto, observation: String) {
        scope.launch {
            updateState {
                it.copy(
                    reviewingDeliveryId = delivery.idEntrega,
                    deliveriesError = null,
                    deliveryActionMessage = null
                )
            }
            when (
                val result = repository.requestDeliveryChanges(
                    delivery.idEntrega,
                    RequestDeliveryChangesRequest(observation.trim())
                )
            ) {
                is ApiResult.Success -> updateState { current ->
                    current.copy(
                        reviewingDeliveryId = null,
                        deliveries = current.deliveries.replaceDelivery(result.data),
                        deliveryActionMessage =
                            "Cambios solicitados. El estudiante ya puede enviar una nueva versión."
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        reviewingDeliveryId = null,
                        deliveriesError = result.message
                    )
                }
            }
        }
    }

    fun comment(delivery: DeliveryDto, comment: String) {
        scope.launch {
            updateState {
                it.copy(
                    commentingDeliveryId = delivery.idEntrega,
                    deliveriesError = null,
                    deliveryActionMessage = null
                )
            }
            when (
                val result = repository.commentDelivery(
                    delivery.idEntrega,
                    CreateDeliveryCommentRequest(comment.trim())
                )
            ) {
                is ApiResult.Success -> updateState { current ->
                    current.copy(
                        commentingDeliveryId = null,
                        deliveries = current.deliveries.replaceDelivery(result.data),
                        deliveryActionMessage = "Comentario registrado."
                    )
                }

                is ApiResult.Error -> updateState {
                    it.copy(
                        commentingDeliveryId = null,
                        deliveriesError = result.message
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        updateState {
            it.copy(
                deliveriesError = null,
                deliveryActionMessage = null
            )
        }
    }
}

private fun List<DeliveryDto>.replaceDelivery(updated: DeliveryDto): List<DeliveryDto> {
    return map { delivery ->
        if (delivery.idEntrega == updated.idEntrega) updated else delivery
    }
}
