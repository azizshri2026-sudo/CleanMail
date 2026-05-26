package cleanmail.crypto

/**
 * Platform-specific AES-GCM encrypt/decrypt backed by the OS secure keystore.
 * Keys never leave the secure enclave on supported hardware.
 */
expect object CryptoManager {

    /**
     * Encrypt [plaintext] and return Base64-encoded ciphertext (IV prepended).
     * [keyAlias] identifies which key in the keystore to use.
     */
    fun encrypt(plaintext: String, keyAlias: String): String

    /**
     * Decrypt Base64-encoded [ciphertext] (IV prepended) using [keyAlias].
     */
    fun decrypt(ciphertext: String, keyAlias: String): String

    /**
     * Derive a 32-byte database key for SQLCipher from the keystore.
     * Returned as raw ByteArray — never logged or serialized.
     */
    fun getDatabaseKey(keyAlias: String = KEY_ALIAS_DB): ByteArray

    /**
     * Generate (or retrieve existing) key for [keyAlias].
     */
    fun ensureKey(keyAlias: String)

    val KEY_ALIAS_DB: String
    val KEY_ALIAS_ACCOUNTS: String
    val KEY_ALIAS_OAUTH: String
}
