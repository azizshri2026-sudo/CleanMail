package cleanmail.imap

import cleanmail.db.CleanMailDatabase
import cleanmail.db.toDbEntity
import cleanmail.models.Email

interface SyncStorage {
    fun getUidNext(accountId: String, folderPath: String): Long
    fun insertEmails(emails: List<Email>)
}

class DatabaseSyncStorage(private val db: CleanMailDatabase) : SyncStorage {

    override fun getUidNext(accountId: String, folderPath: String): Long {
        val stored = db.folderQueries.selectById("$accountId/$folderPath").executeAsOneOrNull()
        return stored?.uid_next?.takeIf { it > 0L } ?: 1L
    }

    override fun insertEmails(emails: List<Email>) {
        db.transaction {
            emails.forEach { email ->
                db.emailQueries.insert(email.toDbEntity())
            }
        }
    }
}
