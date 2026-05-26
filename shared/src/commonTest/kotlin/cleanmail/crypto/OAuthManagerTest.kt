package cleanmail.crypto

import kotlin.test.*

class OAuthManagerTest {

    private val testConfig = OAuthConfig(
        clientId = "test-client-id",
        clientSecret = "",
        authorizationEndpoint = "https://accounts.google.com/o/oauth2/auth",
        tokenEndpoint = "https://oauth2.googleapis.com/token",
        redirectUri = "cleanmail://oauth",
        scopes = listOf("https://mail.google.com/")
    )

    private val manager = OAuthManager(testConfig)

    // --- buildAuthRequest ---

    @Test fun buildAuthRequest_returnsNonEmptyUrl() {
        val req = manager.buildAuthRequest()
        assertTrue(req.url.isNotBlank())
    }

    @Test fun buildAuthRequest_urlContainsClientId() {
        val req = manager.buildAuthRequest()
        assertTrue(req.url.contains("test-client-id"), "URL should contain client_id")
    }

    @Test fun buildAuthRequest_urlContainsCodeChallengeMethod() {
        val req = manager.buildAuthRequest()
        assertTrue(req.url.contains("code_challenge_method=S256"), "URL should use S256 challenge method")
    }

    @Test fun buildAuthRequest_urlContainsCodeChallenge() {
        val req = manager.buildAuthRequest()
        assertTrue(req.url.contains("code_challenge="), "URL should contain code_challenge")
    }

    @Test fun buildAuthRequest_urlContainsState() {
        val req = manager.buildAuthRequest()
        assertTrue(req.url.contains("state="), "URL should contain state parameter")
    }

    @Test fun buildAuthRequest_urlContainsRedirectUri() {
        val req = manager.buildAuthRequest()
        assertTrue(req.url.contains("redirect_uri="), "URL should contain redirect_uri")
    }

    @Test fun buildAuthRequest_urlContainsScope() {
        val req = manager.buildAuthRequest()
        assertTrue(req.url.contains("scope="), "URL should contain scope")
    }

    // --- PKCE: codeVerifier properties ---

    @Test fun codeVerifier_isNotEmpty() {
        val req = manager.buildAuthRequest()
        assertTrue(req.codeVerifier.isNotBlank())
    }

    @Test fun codeVerifier_minLength43() {
        // RFC 7636: code verifier length 43–128
        val req = manager.buildAuthRequest()
        assertTrue(req.codeVerifier.length >= 43, "code_verifier too short: ${req.codeVerifier.length}")
    }

    @Test fun codeVerifier_maxLength128() {
        val req = manager.buildAuthRequest()
        assertTrue(req.codeVerifier.length <= 128, "code_verifier too long: ${req.codeVerifier.length}")
    }

    @Test fun codeVerifier_onlyUrlSafeChars() {
        val req = manager.buildAuthRequest()
        val allowed = Regex("^[A-Za-z0-9\\-._~]+$")
        assertTrue(allowed.matches(req.codeVerifier),
            "code_verifier contains disallowed chars: ${req.codeVerifier}")
    }

    // --- PKCE: codeChallenge is deterministic for a given verifier ---

    @Test fun codeChallenge_deterministicFromVerifier() {
        // Given the same verifier, challenge must be same
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val c1 = sha256Base64Url(verifier)
        val c2 = sha256Base64Url(verifier)
        assertEquals(c1, c2)
    }

    @Test fun codeChallenge_knownVector() {
        // RFC 7636 Appendix B test vector
        // verifier: "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        // expected: "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val expected = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
        assertEquals(expected, sha256Base64Url(verifier))
    }

    @Test fun codeChallenge_differentVerifiers_differentChallenges() {
        val c1 = sha256Base64Url("verifier-aaaa")
        val c2 = sha256Base64Url("verifier-bbbb")
        assertNotEquals(c1, c2)
    }

    @Test fun codeChallenge_isBase64Url_noPlus_noSlash_noPadding() {
        val challenge = sha256Base64Url("some-test-verifier-value-longer-than-32chars-ok")
        // base64url: no +, no /, no =
        assertFalse(challenge.contains('+'), "challenge must not contain '+'")
        assertFalse(challenge.contains('/'), "challenge must not contain '/'")
        assertFalse(challenge.contains('='), "challenge must not contain '='")
    }

    // --- state uniqueness ---

    @Test fun state_uniquePerRequest() {
        val r1 = manager.buildAuthRequest()
        val r2 = manager.buildAuthRequest()
        assertNotEquals(r1.state, r2.state, "state must be different per request")
    }

    @Test fun state_notEmpty() {
        val req = manager.buildAuthRequest()
        assertTrue(req.state.isNotBlank())
    }

    // --- codeVerifier uniqueness ---

    @Test fun codeVerifier_uniquePerRequest() {
        val r1 = manager.buildAuthRequest()
        val r2 = manager.buildAuthRequest()
        assertNotEquals(r1.codeVerifier, r2.codeVerifier, "code_verifier must be unique per request")
    }

    // --- encodeBase64Url ---

    @Test fun encodeBase64Url_empty() {
        val result = ByteArray(0).encodeBase64Url()
        assertEquals("", result)
    }

    @Test fun encodeBase64Url_noUrlUnsafeChars() {
        repeat(20) {
            val bytes = ByteArray(32) { it.toByte() }
            val result = bytes.encodeBase64Url()
            assertFalse(result.contains('+'))
            assertFalse(result.contains('/'))
            assertFalse(result.contains('='))
        }
    }

    @Test fun encodeBase64Url_knownValue() {
        // [0, 0, 0] → base64: "AAAA", base64url: "AAAA"
        val result = byteArrayOf(0, 0, 0).encodeBase64Url()
        assertEquals("AAAA", result)
    }
}
