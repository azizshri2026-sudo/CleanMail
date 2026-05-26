package cleanmail.oauth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class OAuthCallbackResult(
    val code: String,
    val state: String,
    val provider: String  // "google" | "outlook"
)

object OAuthCallbackBus {
    private val _events = MutableSharedFlow<OAuthCallbackResult>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun post(result: OAuthCallbackResult) {
        _events.tryEmit(result)
    }
}
