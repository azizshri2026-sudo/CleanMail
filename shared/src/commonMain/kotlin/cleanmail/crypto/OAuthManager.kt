package cleanmail.crypto

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

class OAuthManager(private val config: OAuthConfig) {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    data class AuthRequest(
        val url: String,
        val codeVerifier: String,
        val state: String
    )

    /**
     * Build PKCE authorization URL. Open this URL in a browser/WebView.
     */
    fun buildAuthRequest(): AuthRequest {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        val state = generateState()

        val url = URLBuilder(config.authorizationEndpoint).apply {
            parameters.append("response_type", "code")
            parameters.append("client_id", config.clientId)
            parameters.append("redirect_uri", config.redirectUri)
            parameters.append("scope", config.scopes.joinToString(" "))
            parameters.append("state", state)
            parameters.append("code_challenge", codeChallenge)
            parameters.append("code_challenge_method", "S256")
            parameters.append("access_type", "offline")
            parameters.append("prompt", "consent")
        }.buildString()

        return AuthRequest(url, codeVerifier, state)
    }

    /**
     * Exchange authorization code for tokens.
     */
    suspend fun exchangeCode(
        code: String,
        codeVerifier: String
    ): Result<OAuthTokens> = runCatching {
        val response: TokenResponse = httpClient.submitForm(
            url = config.tokenEndpoint,
            formParameters = parameters {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", config.redirectUri)
                append("client_id", config.clientId)
                if (config.clientSecret.isNotEmpty()) append("client_secret", config.clientSecret)
                append("code_verifier", codeVerifier)
            }
        ).body()
        response.toTokens()
    }

    /**
     * Refresh access token using stored refresh token.
     */
    suspend fun refreshToken(refreshToken: String): Result<OAuthTokens> = runCatching {
        val response: TokenResponse = httpClient.submitForm(
            url = config.tokenEndpoint,
            formParameters = parameters {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
                append("client_id", config.clientId)
                if (config.clientSecret.isNotEmpty()) append("client_secret", config.clientSecret)
            }
        ).body()
        response.toTokens()
    }

    fun close() = httpClient.close()

    // --- PKCE helpers ---

    private fun generateCodeVerifier(): String {
        val bytes = Random.nextBytes(32)
        return bytes.encodeBase64Url()
    }

    private fun generateCodeChallenge(verifier: String): String {
        // SHA-256 hash of verifier, base64url-encoded
        return sha256Base64Url(verifier)
    }

    private fun generateState(): String = Random.nextBytes(16).encodeBase64Url()
}

@Serializable
private data class TokenResponse(
    @SerialName("access_token")  val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in")    val expiresIn: Long = 3600,
    @SerialName("token_type")    val tokenType: String = "Bearer"
) {
    fun toTokens() = OAuthTokens(accessToken, refreshToken, expiresIn, tokenType)
}

// Implemented per-platform as expect/actual
expect fun ByteArray.encodeBase64Url(): String
expect fun sha256Base64Url(input: String): String
