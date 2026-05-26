package cleanmail.db

import android.content.Context
import cleanmail.crypto.CryptoManager

object KeystoreHelper {
    fun getDatabaseKey(@Suppress("UNUSED_PARAMETER") context: Context): ByteArray =
        CryptoManager.getDatabaseKey(CryptoManager.KEY_ALIAS_DB)
}
