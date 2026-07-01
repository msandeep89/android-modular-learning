package com.sandeep.newsreader.core.common

// Sealed class wrapping API responses — every feature module uses this.
// Forces callers to handle all three states explicitly.
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// UiState is what ViewModels expose to Fragments via StateFlow.
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// Convenience: convert a Result to a UiState
fun <T> Result<T>.toUiState(): UiState<T> = when (this) {
    is Result.Loading  -> UiState.Loading
    is Result.Success  -> UiState.Success(data)
    is Result.Error    -> UiState.Error(message)
}
