package cleanmail.imap

import cleanmail.models.Email
import cleanmail.models.Folder
import kotlinx.coroutines.flow.Flow

interface IImapClient {
    val isConnected: Boolean
    suspend fun connect(): ImapResult<Unit>
    suspend fun disconnect()
    suspend fun listFolders(): ImapResult<List<Folder>>
    suspend fun selectFolder(folderPath: String): ImapResult<Folder>
    suspend fun fetchUids(folderPath: String, sinceUid: Long): ImapResult<List<Long>>
    suspend fun fetchEmails(folderPath: String, uids: List<Long>, fetchBody: Boolean = false): ImapResult<List<Email>>
    suspend fun fetchEmailBody(folderPath: String, uid: Long): ImapResult<Email>
    suspend fun markRead(folderPath: String, uids: List<Long>): ImapResult<Unit>
    suspend fun markUnread(folderPath: String, uids: List<Long>): ImapResult<Unit>
    suspend fun markStarred(folderPath: String, uids: List<Long>, starred: Boolean): ImapResult<Unit>
    suspend fun moveMessages(fromFolder: String, toFolder: String, uids: List<Long>): ImapResult<Unit>
    suspend fun deleteMessages(folderPath: String, uids: List<Long>): ImapResult<Unit>
    suspend fun expunge(folderPath: String): ImapResult<Unit>
    fun idle(folderPath: String): Flow<IdleEvent>
}
