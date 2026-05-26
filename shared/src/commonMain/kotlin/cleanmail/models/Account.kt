package cleanmail.models

import kotlinx.serialization.Serializable

enum class AuthType { PASSWORD, OAUTH2 }
enum class SecurityType { SSL_TLS, STARTTLS, NONE }

@Serializable
data class Account(
    val id: String,
    val displayName: String,
    val emailAddress: String,
    val authType: AuthType,

    // IMAP
    val imapHost: String,
    val imapPort: Int = 993,
    val imapSecurity: SecurityType = SecurityType.SSL_TLS,

    // SMTP
    val smtpHost: String,
    val smtpPort: Int = 587,
    val smtpSecurity: SecurityType = SecurityType.STARTTLS,

    // Credentials — хранятся зашифрованными, расшифровка через платформенный keystore
    val usernameEncrypted: String = "",
    val passwordEncrypted: String = "",
    val oauthTokenEncrypted: String = "",
    val oauthRefreshTokenEncrypted: String = "",

    val proxy: ProxyConfig = ProxyConfig(),

    val syncIntervalMinutes: Int = 15,
    val isActive: Boolean = true
)
