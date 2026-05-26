package cleanmail.db

import android.content.Context
import android.util.Base64
import cleanmail.crypto.CryptoManager
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeystoreHelper {

    private const val PREFS_NAME = "cleanmail_keystore"
    private const val KEY_DB_ENCRYPTED = "db_key_encrypted"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_SIZE = 12

    fun getDatabaseKey(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_DB_ENCRYPTED, null)

        return if (stored != null) {
            decryptStoredKey(stored)
        } else {
            val rawKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
            prefs.edit().putString(KEY_DB_ENCRYPTED, encryptKey(rawKey)).apply()
            rawKey
        }
    }

    private fun encryptKey(rawKey: ByteArray): String {
        CryptoManager.ensureKey(CryptoManager.KEY_ALIAS_DB)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, loadKeystoreKey())
        val combined = cipher.iv + cipher.doFinal(rawKey)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptStoredKey(stored: String): ByteArray {
        CryptoManager.ensureKey(CryptoManager.KEY_ALIAS_DB)
        val combined = Base64.decode(stored, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, IV_SIZE)
        val data = combined.copyOfRange(IV_SIZE, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, loadKeystoreKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(data)
    }

    private fun loadKeystoreKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (ks.getEntry(CryptoManager.KEY_ALIAS_DB, null) as KeyStore.SecretKeyEntry).secretKey
    }
}
