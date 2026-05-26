package cleanmail.models

import kotlinx.serialization.Serializable

enum class ProxyType { NONE, SOCKS5, HTTP }

@Serializable
data class ProxyConfig(
    val type: ProxyType = ProxyType.NONE,
    val host: String = "",
    val port: Int = 1080,
    val username: String = "",
    val passwordEncrypted: String = "",
    val forceDnsThroughProxy: Boolean = true
)
