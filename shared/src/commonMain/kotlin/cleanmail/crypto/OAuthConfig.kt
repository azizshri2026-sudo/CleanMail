package cleanmail.crypto

import kotlinx.serialization.Serializable

@Serializable
data class OAuthConfig(
    val clientId: String,
    val clientSecret: String = "",
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val redirectUri: String,
    val scopes: List<String>
)

@Serializable
data class OAuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val tokenType: String = "Bearer"
)

object OAuthProviders {
    val gmail = OAuthConfig(
        clientId = "",
        authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth",
        tokenEndpoint = "https://oauth2.googleapis.com/token",
        redirectUri = "cleanmail://oauth",
        scopes = listOf(
            "https://mail.google.com/",
            "openid",
            "email",
            "profile"
        )
    )

    val outlook = OAuthConfig(
        clientId = "",
        authorizationEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
        tokenEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/token",
        redirectUri = "cleanmail://oauth",
        scopes = listOf(
            "https://outlook.office.com/IMAP.AccessAsUser.All",
            "https://outlook.office.com/SMTP.Send",
            "offline_access",
            "openid",
            "email"
        )
    )
}
