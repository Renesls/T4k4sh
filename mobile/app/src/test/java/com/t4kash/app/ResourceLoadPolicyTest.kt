package com.t4kash.app.ui.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceLoadPolicyTest {

    @Test
    fun shouldLoadResource_skipsLoadedResource() {
        assertFalse(
            shouldLoadResource(
                requestedId = 7,
                loadedId = 7,
                loadingId = null,
                force = false
            )
        )
    }

    @Test
    fun shouldLoadResource_skipsDuplicateRequestInProgress() {
        assertFalse(
            shouldLoadResource(
                requestedId = 7,
                loadedId = null,
                loadingId = 7,
                force = true
            )
        )
    }

    @Test
    fun shouldLoadResource_loadsDifferentResource() {
        assertTrue(
            shouldLoadResource(
                requestedId = 8,
                loadedId = 7,
                loadingId = null,
                force = false
            )
        )
    }

    @Test
    fun shouldLoadResource_forceReloadsCachedResource() {
        assertTrue(
            shouldLoadResource(
                requestedId = 7,
                loadedId = 7,
                loadingId = null,
                force = true
            )
        )
    }
}
