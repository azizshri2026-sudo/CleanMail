package cleanmail.crypto

import java.security.MessageDigest
import java.util.Base64

actual fun ByteArray.encodeBase64Url(): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(this)

actual fun sha256Base64Url(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(input.toByteArray(Charsets.US_ASCII))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
}
