package com.t4kash.app.ui.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPolicyTest {

    @Test
    fun shouldRefresh_returnsFalseWhileTargetIsLoading() {
        val policy = RefreshPolicy()

        assertFalse(
            policy.shouldRefresh(
                target = RefreshTarget.HOME,
                force = true,
                isLoading = true
            )
        )
    }

    @Test
    fun shouldRefresh_reusesRecentSuccessfulData() {
        var now = 1_000L
        val policy = RefreshPolicy(
            maxAgeMillis = 30_000L,
            currentTimeMillis = { now }
        )

        assertTrue(policy.shouldRefresh(RefreshTarget.HOME, false, false))
        policy.markSuccessful(RefreshTarget.HOME)
        now += 20_000L

        assertFalse(policy.shouldRefresh(RefreshTarget.HOME, false, false))
    }

    @Test
    fun shouldRefresh_refreshesStaleOrForcedData() {
        var now = 1_000L
        val policy = RefreshPolicy(
            maxAgeMillis = 30_000L,
            currentTimeMillis = { now }
        )
        policy.markSuccessful(RefreshTarget.JOBS)
        now += 30_000L

        assertTrue(policy.shouldRefresh(RefreshTarget.JOBS, false, false))

        policy.markSuccessful(RefreshTarget.JOBS)
        assertTrue(policy.shouldRefresh(RefreshTarget.JOBS, true, false))
    }

    @Test
    fun successfulRefreshes_areTrackedIndependently() {
        val policy = RefreshPolicy(currentTimeMillis = { 5_000L })
        policy.markSuccessful(RefreshTarget.HOME)

        assertFalse(policy.shouldRefresh(RefreshTarget.HOME, false, false))
        assertTrue(policy.shouldRefresh(RefreshTarget.JOBS, false, false))
    }
}
