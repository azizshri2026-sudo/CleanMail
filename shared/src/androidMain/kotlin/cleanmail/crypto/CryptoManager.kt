package cleanmail.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
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

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val ALGORITHM  = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING    = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_SIZE = 12

    actual fun ensureKey(keyAlias: String) {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (ks.containsAlias(keyAlias)) return

        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()

        KeyGenerator.getInstance(ALGORITHM, KEYSTORE_PROVIDER)
            .apply { init(spec) }
            .generateKey()
    }

    actual fun encrypt(plaintext: String, keyAlias: String): String {
        ensureKey(keyAlias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, loadKey(keyAlias))
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + ciphertext
        return Base64.getEncoder().encodeToString(combined)
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
        // Encrypt a fixed sentinel with the keystore key; use resulting bytes as DB passphrase.
        // This ties the DB key to the Android Keystore entry — changes if key is deleted.
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, loadKey(keyAlias))
        val seed = "cleanmail_db_passphrase_v1".toByteArray(Charsets.UTF_8)
        return cipher.doFinal(seed).copyOf(32)
    }

    private fun loadKey(keyAlias: String): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return (ks.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
    }
}
