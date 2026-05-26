package cleanmail.imap

import cleanmail.db.CleanMailDatabase
import cleanmail.models.Account
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ImapSyncManager(
    private val account: Account,
    private val client: IImapClient,
    private val storage: SyncStorage
) {
    constructor(account: Account, db: CleanMailDatabase) : this(
        account,
        client  = ImapClient(account),
        storage = DatabaseSyncStorage(db)
    )

    private var idleJob: Job? = null

    suspend fun fullSync(): SyncSummary {
        val start = currentTimeMs()
        if (!client.isConnected) {
            client.connect().onFailure {
                return SyncSummary(account.id, emptyList(), currentTimeMs() - start)
            }
        }

        val folders = client.listFolders()
            .let { if (it is ImapResult.Success) it.value else return SyncSummary(account.id, emptyList(), 0L) }
            .filter { it.syncEnabled }

        val results = folders.map { folder -> syncFolder(folder.fullPath) }

        return SyncSummary(account.id, results, currentTimeMs() - start)
    }

    suspend fun syncFolder(folderPath: String): FolderSyncResult {
        val sinceUid = storage.getUidNext(account.id, folderPath)

        val uids = client.fetchUids(folderPath, sinceUid)
            .let { if (it is ImapResult.Success) it.value else return FolderSyncResult(folderPath, 0, 0, 0) }

        if (uids.isEmpty()) return FolderSyncResult(folderPath, 0, 0, 0)

        val emails = client.fetchEmails(folderPath, uids, fetchBody = false)
            .let { if (it is ImapResult.Success) it.value else return FolderSyncResult(folderPath, 0, 0, 0) }

        storage.insertEmails(emails)

        return FolderSyncResult(folderPath, emails.size, 0, 0)
    }

    fun startIdle(folderPath: String, scope: CoroutineScope): Flow<IdleEvent> = flow {
        client.idle(folderPath).collect { event ->
            when (event) {
                is IdleEvent.NewMessages -> {
                    syncFolder(folderPath)
                    emit(event)
                }
                else -> emit(event)
            }
        }
    }

    suspend fun disconnect() = client.disconnect()

    private fun currentTimeMs(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
}
