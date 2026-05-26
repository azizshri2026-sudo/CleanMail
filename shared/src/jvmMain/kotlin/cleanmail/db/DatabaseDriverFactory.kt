package cleanmail.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbPath = resolveDbPath()
        val props = Properties().apply {
            setProperty("cipher", "sqlcipher")
            setProperty("key", KeystoreHelper.getDatabaseKey())
        }
        val driver = JdbcSqliteDriver("jdbc:sqlite:$dbPath", props)
        CleanMailDatabase.Schema.create(driver)
        return driver
    }

    private fun resolveDbPath(): String {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        return "$appData/CleanMail/cleanmail.db"
    }
}
