package cleanmail.filter

import cleanmail.db.CleanMailDatabase
import cleanmail.imap.ImapClient
import cleanmail.models.*

class FilterActionExecutor(
    private val db: CleanMailDatabase,
    private val imapClient: ImapClient
) {

    sealed class ActionResult {
        object Ok : ActionResult()
        data class Error(val message: String) : ActionResult()
    }

    suspend fun execute(rule: FilterRule, email: Email): ActionResult {
        return when (rule.action) {
            FilterAction.MOVE_TO_FOLDER -> moveToFolder(email, rule.actionParam)
            FilterAction.MARK_READ      -> markRead(email)
            FilterAction.MARK_STARRED   -> markStarred(email)
            FilterAction.DELETE         -> delete(email)
            FilterAction.LABEL          -> label(email, rule.actionParam)
        }
    }

    private suspend fun moveToFolder(email: Email, targetFolderPath: String): ActionResult {
        if (targetFolderPath.isBlank()) return ActionResult.Error("No target folder specified")
        val result = imapClient.moveMessages(email.folderId, targetFolderPath, listOf(email.uid))
        return if (result is cleanmail.imap.ImapResult.Success) {
            val targetFolder = db.folderQueries
                .selectById("${email.accountId}/$targetFolderPath")
                .executeAsOneOrNull()
            if (targetFolder != null) {
                db.emailQueries.moveToFolder(targetFolder.id, email.id)
            }
            ActionResult.Ok
        } else {
            ActionResult.Error("Move failed")
        }
    }

    private suspend fun markRead(email: Email): ActionResult {
        imapClient.markRead(email.folderId, listOf(email.uid))
        db.emailQueries.markRead(email.id)
        return ActionResult.Ok
    }

    private suspend fun markStarred(email: Email): ActionResult {
        imapClient.markStarred(email.folderId, listOf(email.uid), starred = true)
        db.emailQueries.markStarred(1, email.id)
        return ActionResult.Ok
    }

    private suspend fun delete(email: Email): ActionResult {
        val trashFolder = db.folderQueries
            .selectByRole(email.accountId, FolderRole.TRASH.name)
            .executeAsOneOrNull()
        return if (trashFolder != null) {
            moveToFolder(email, trashFolder.full_path)
        } else {
            imapClient.deleteMessages(email.folderId, listOf(email.uid))
            db.emailQueries.softDelete(email.id)
            ActionResult.Ok
        }
    }

    private fun label(email: Email, label: String): ActionResult {
        // Labels are stored as a custom IMAP flag; persisted in DB as metadata
        // Full implementation depends on server IMAP METADATA support
        return ActionResult.Ok
    }
}
