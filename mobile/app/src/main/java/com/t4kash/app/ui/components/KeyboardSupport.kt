package com.t4kash.app.ui.components

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun Modifier.keepVisibleAboveKeyboard(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    var pendingRequest by remember { mutableStateOf<Job?>(null) }

    bringIntoViewRequester(requester)
        .onFocusChanged { focusState ->
            pendingRequest?.cancel()
            if (focusState.isFocused) {
                pendingRequest = scope.launch {
                    delay(KEYBOARD_ANIMATION_DELAY_MILLIS)
                    requester.bringIntoView()
                }
            } else {
                pendingRequest = null
            }
        }
}

private const val KEYBOARD_ANIMATION_DELAY_MILLIS = 180L
