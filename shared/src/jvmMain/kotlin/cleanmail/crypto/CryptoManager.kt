package cleanmail.crypto

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual object CryptoManager {

    actual const val KEY_ALIAS_DB       = "cleanmail_db_key"
    actual const val KEY_ALIAS_ACCOUNTS = "cleanmail_accounts_key"
    actual const val KEY_ALIAS_OAUTH    = "cleanmail_oauth_key"

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_SIZE = 12
    private const val KEYSTORE_TYPE = "PKCS12"

    private val keystoreFile: File by lazy {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        File("$appData/CleanMail/.keystore.p12").also { it.parentFile?.mkdirs() }
    }

    // Derived from machine-specific data — not stored anywhere
    private val keystorePassword: CharArray by lazy { deriveKeystorePassword() }

    actual fun ensureKey(keyAlias: String) {
        val ks = loadKeystore()
        if (ks.containsAlias(keyAlias)) return

        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val entry = KeyStore.SecretKeyEntry(key)
        ks.setEntry(keyAlias, entry, KeyStore.PasswordProtection(keystorePassword))
        saveKeystore(ks)
    }

    actual fun encrypt(plaintext: String, keyAlias: String): String {
        ensureKey(keyAlias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, loadKey(keyAlias))
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + ciphertext)
    }

    actual fun decrypt(ciphertext: String, keyAlias: String): String {
        ensureKey(keyAlias)
        val combined = Base64.getDecoder().decode(ciphertext)
        val iv = combined.copyOfRange(0, IV_SIZE)
        val data = combined.copyOfRange(IV_SIZE, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, loadKey(keyAlias), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(data).toString(Charsets.UTF_8)
    }

    actual fun getDatabaseKey(keyAlias: String): ByteArray {
        ensureKey(keyAlias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, loadKey(keyAlias))
        val seed = "cleanmail_db_passphrase_v1".toByteArray(Charsets.UTF_8)
        return cipher.doFinal(seed).copyOf(32)
    }

    private fun loadKey(keyAlias: String): SecretKey {
        val ks = loadKeystore()
        return (ks.getEntry(keyAlias, KeyStore.PasswordProtection(keystorePassword)) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun loadKeystore(): KeyStore {
        val ks = KeyStore.getInstance(KEYSTORE_TYPE)
        if (keystoreFile.exists()) {
            keystoreFile.inputStream().use { ks.load(it, keystorePassword) }
        } else {
            ks.load(null, keystorePassword)
        }
        return ks
    }

    private fun saveKeystore(ks: KeyStore) {
        keystoreFile.outputStream().use { ks.store(it, keystorePassword) }
        // Restrict file permissions on non-Windows platforms
        runCatching {
            Files.setPosixFilePermissions(
                keystoreFile.toPath(),
                PosixFilePermissions.fromString("rw-------")
            )
        }
    }

    /**
     * Derives a keystore password from machine-unique data.
     * Uses username + OS name + a fixed salt — not cryptographically strong,
     * but prevents casual file copying. On Windows, wrap with DPAPI for stronger binding.
     */
    private fun deriveKeystorePassword(): CharArray {
        val raw = "${System.getProperty("user.name")}:${System.getProperty("os.name")}:cleanmail_v1"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return Base64.getEncoder().encodeToString(digest.digest(raw.toByteArray())).toCharArray()
    }
}
