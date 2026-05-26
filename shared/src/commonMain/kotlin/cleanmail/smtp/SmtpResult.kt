package cleanmail.smtp

sealed class SmtpResult<out T> {
    data class Success<T>(val value: T) : SmtpResult<T>()
    data class Failure(val error: SmtpError) : SmtpResult<Nothing>()
}

sealed class SmtpError {
    data class AuthFailed(val message: String) : SmtpError()
    data class ConnectionFailed(val message: String) : SmtpError()
    data class SslError(val message: String) : SmtpError()
    data class RecipientRejected(val address: String, val message: String) : SmtpError()
    data class MessageTooLarge(val message: String) : SmtpError()
    data class Timeout(val message: String) : SmtpError()
    data class Unknown(val message: String) : SmtpError()
}

inline fun <T> SmtpResult<T>.onSuccess(block: (T) -> Unit): SmtpResult<T> {
    if (this is SmtpResult.Success) block(value)
    return this
}

inline fun <T> SmtpResult<T>.onFailure(block: (SmtpError) -> Unit): SmtpResult<T> {
    if (this is SmtpResult.Failure) block(error)
    return this
}
