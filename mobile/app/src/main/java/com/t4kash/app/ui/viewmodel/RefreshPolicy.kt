package com.t4kash.app.ui.viewmodel

internal enum class RefreshTarget {
    HOME,
    JOBS
}

internal class RefreshPolicy(
    private val maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    private val lastSuccessfulRefresh = mutableMapOf<RefreshTarget, Long>()

    fun shouldRefresh(
        target: RefreshTarget,
        force: Boolean,
        isLoading: Boolean
    ): Boolean {
        if (isLoading) {
            return false
        }
        if (force) {
            return true
        }
        val lastRefresh = lastSuccessfulRefresh[target] ?: return true
        return currentTimeMillis() - lastRefresh >= maxAgeMillis
    }

    fun markSuccessful(target: RefreshTarget) {
        lastSuccessfulRefresh[target] = currentTimeMillis()
    }

    private companion object {
        const val DEFAULT_MAX_AGE_MILLIS = 30_000L
    }
}
