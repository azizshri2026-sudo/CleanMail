package cleanmail

import android.app.Application
import cleanmail.crypto.CryptoManager
import cleanmail.db.CleanMailDatabase
import cleanmail.db.DatabaseDriverFactory

class CleanMailApp : Application() {

    lateinit var database: CleanMailDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        CryptoManager.ensureKey(CryptoManager.KEY_ALIAS_DB)
        CryptoManager.ensureKey(CryptoManager.KEY_ALIAS_ACCOUNTS)
        CryptoManager.ensureKey(CryptoManager.KEY_ALIAS_OAUTH)
        database = CleanMailDatabase(DatabaseDriverFactory(this).createDriver())
        instance = this
    }

    companion object {
        lateinit var instance: CleanMailApp
            private set
    }
}
