package cleanmail.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import net.sqlcipher.database.SupportFactory

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
        val passphrase = KeystoreHelper.getDatabaseKey(context)
        val factory = SupportFactory(passphrase)
        return AndroidSqliteDriver(
            schema = CleanMailDatabase.Schema,
            context = context,
            name = "cleanmail.db",
            factory = factory
        )
    }
}
