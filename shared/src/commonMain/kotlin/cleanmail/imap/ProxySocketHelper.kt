package cleanmail.imap

import cleanmail.models.ProxyConfig
import cleanmail.models.ProxyType

/**
 * Builds java.net.Proxy for use in JavaMail sessions.
 * Defined in commonMain as interface; actual socket creation is in platform code
 * since java.net is available on both Android and JVM targets.
 */
object ProxySocketHelper {

    fun buildJavaNetProxy(config: ProxyConfig): Any? {
        if (config.type == ProxyType.NONE) return null
        return config
    }

    fun proxyProperties(config: ProxyConfig): Map<String, String> {
        if (config.type == ProxyType.NONE) return emptyMap()
        return when (config.type) {
            ProxyType.SOCKS5 -> mapOf(
                "mail.imap.socks.host" to config.host,
                "mail.imap.socks.port" to config.port.toString(),
                "mail.imaps.socks.host" to config.host,
                "mail.imaps.socks.port" to config.port.toString()
            )
            ProxyType.HTTP -> mapOf(
                "mail.imap.proxy.host" to config.host,
                "mail.imap.proxy.port" to config.port.toString(),
                "mail.imaps.proxy.host" to config.host,
                "mail.imaps.proxy.port" to config.port.toString()
            )
            ProxyType.NONE -> emptyMap()
        }
    }
}
