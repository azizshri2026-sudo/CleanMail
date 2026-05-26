package cleanmail.db

import cleanmail.crypto.CryptoManager

object KeystoreHelper {
    fun getDatabaseKey(): String =
        CryptoManager.getDatabaseKey(CryptoManager.KEY_ALIAS_DB)
            .joinToString("") { "%02x".format(it) }
}
