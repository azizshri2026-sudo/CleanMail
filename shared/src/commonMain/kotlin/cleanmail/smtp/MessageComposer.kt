package cleanmail.smtp

import cleanmail.db.CleanMailDatabase
import cleanmail.db.toDbEntity
import cleanmail.models.*
import kotlinx.datetime.Clock

class MessageComposer(
    private val account: Account,
    private val db: CleanMailDatabase
) {
    private val client = SmtpClient(account)

    suspend fun send(message: OutgoingMessage): SmtpResult<Unit> {
        if (!client.isConnected) {
            client.connect().onFailure { return SmtpResult.Failure(it) }
        }
        val result = client.sendMessage(message)
        result.onSuccess {
            saveSentCopy(message)
        }
        return result
    }

    suspend fun saveDraft(message: OutgoingMessage) {
        val email = message.toEmail(account, isDraft = true)
        val sentFolder = db.folderQueries
            .selectByRole(account.id, FolderRole.DRAFTS.name)
            .executeAsOneOrNull()
        if (sentFolder != null) {
            db.emailQueries.insert(email.copy(folderId = sentFolder.id).toDbEntity())
        }
    }

    suspend fun disconnect() = client.disconnect()

    private fun saveSentCopy(message: OutgoingMessage) {
        val sentFolder = db.folderQueries
            .selectByRole(account.id, FolderRole.SENT.name)
            .executeAsOneOrNull() ?: return
        val email = message.toEmail(account, isDraft = false)
        db.emailQueries.insert(email.copy(folderId = sentFolder.id).toDbEntity())
    }
}

private fun OutgoingMessage.toEmail(account: Account, isDraft: Boolean): Email {
    val now = Clock.System.now()
    return Email(
        id = "local/${account.id}/${now.toEpochMilliseconds()}",
        accountId = account.id,
        folderId = "",
        uid = 0L,
        messageId = "",
        from = from,
        replyTo = replyTo,
        to = to,
        cc = cc,
        bcc = bcc,
        subject = subject,
        bodyText = bodyText,
        bodyHtml = bodyHtml,
        date = now,
        isRead = true,
        isDraft = isDraft,
        inReplyTo = inReplyTo,
        references = references
    )
}
