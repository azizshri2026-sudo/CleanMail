package cleanmail.imap

sealed class ImapResult<out T> {
    data class Success<T>(val value: T) : ImapResult<T>()
    data class Failure(val error: ImapError) : ImapResult<Nothing>()
}

sealed class ImapError {
    data class AuthFailed(val message: String) : ImapError()
    data class ConnectionFailed(val message: String) : ImapError()
    data class SslError(val message: String) : ImapError()
    data class Timeout(val message: String) : ImapError()
    data class Unknown(val message: String) : ImapError()
}

inline fun <T> ImapResult<T>.onSuccess(block: (T) -> Unit): ImapResult<T> {
    if (this is ImapResult.Success) block(value)
    return this
}

inline fun <T> ImapResult<T>.onFailure(block: (ImapError) -> Unit): ImapResult<T> {
    if (this is ImapResult.Failure) block(error)
    return this
}
