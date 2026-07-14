package tw.taipei.veges.designsystem

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Refreshing<T>(val content: T) : UiState<T>
    data class Current<T>(val content: T) : UiState<T>
    data class Stale<T>(val content: T, val sourceDate: String) : UiState<T>
    data class Offline<T>(val content: T, val sourceDate: String?) : UiState<T>
    data class Unavailable(val reason: String) : UiState<Nothing>
    data class Error(val message: String, val cachedContent: Any? = null) : UiState<Nothing>
}
