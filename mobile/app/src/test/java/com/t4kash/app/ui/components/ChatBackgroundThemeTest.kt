package com.t4kash.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatBackgroundThemeTest {
    @Test
    fun storedValueRestoresSelectedTheme() {
        assertEquals(
            ChatBackgroundTheme.WAVES,
            ChatBackgroundTheme.fromStorage("waves")
        )
    }

    @Test
    fun unknownValueUsesBrandTheme() {
        assertEquals(
            ChatBackgroundTheme.T4KASH,
            ChatBackgroundTheme.fromStorage("unknown")
        )
    }
}
