package cleanmail.imap

import cleanmail.models.Account
import cleanmail.models.Email
import cleanmail.models.Folder
import kotlinx.coroutines.flow.Flow

expect class ImapClient(account: Account) : IImapClient {

    override suspend fun connect(): ImapResult<Unit>

    override suspend fun disconnect()

    override suspend fun listFolders(): ImapResult<List<Folder>>

    override suspend fun selectFolder(folderPath: String): ImapResult<Folder>

    /**
     * Fetch UIDs in folder that are >= sinceUid.
     * sinceUid = 1 means full sync.
     */
    override suspend fun fetchUids(folderPath: String, sinceUid: Long): ImapResult<List<Long>>

    override suspend fun fetchEmails(
        folderPath: String,
        uids: List<Long>,
        fetchBody: Boolean
    ): ImapResult<List<Email>>

    override suspend fun fetchEmailBody(folderPath: String, uid: Long): ImapResult<Email>

    override suspend fun markRead(folderPath: String, uids: List<Long>): ImapResult<Unit>

    override suspend fun markUnread(folderPath: String, uids: List<Long>): ImapResult<Unit>

    override suspend fun markStarred(folderPath: String, uids: List<Long>, starred: Boolean): ImapResult<Unit>

    override suspend fun moveMessages(
        fromFolder: String,
        toFolder: String,
        uids: List<Long>
    ): ImapResult<Unit>

    override suspend fun deleteMessages(folderPath: String, uids: List<Long>): ImapResult<Unit>

    override suspend fun expunge(folderPath: String): ImapResult<Unit>

    /**
     * IMAP IDLE — emits events when server pushes changes.
     * Caller should cancel the coroutine to stop IDLE.
     */
    override fun idle(folderPath: String): Flow<IdleEvent>

    override val isConnected: Boolean
}
