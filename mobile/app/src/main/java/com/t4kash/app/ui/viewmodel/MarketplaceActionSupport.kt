package com.t4kash.app.ui.viewmodel

internal typealias MarketplaceStateProvider = () -> MarketplaceUiState
internal typealias MarketplaceStateUpdater =
    ((MarketplaceUiState) -> MarketplaceUiState) -> Unit

internal fun shouldLoadResource(
    requestedId: Int,
    loadedId: Int?,
    loadingId: Int?,
    force: Boolean
): Boolean {
    if (loadingId == requestedId) {
        return false
    }
    return force || loadedId != requestedId
}
