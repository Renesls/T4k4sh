package com.t4kash.app.ui.service

sealed interface ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>
    data class Error(val message: String) : ApiResult<Nothing>
}
