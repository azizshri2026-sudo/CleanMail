package cleanmail.imap

import cleanmail.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.*

class FakeImapClient : IImapClient {

    var connectResult: ImapResult<Unit> = ImapResult.Success(Unit)
    var foldersResult: ImapResult<List<Folder>> = ImapResult.Success(emptyList())
    var uidsResult: ImapResult<List<Long>> = ImapResult.Success(emptyList())
    var emailsResult: ImapResult<List<Email>> = ImapResult.Success(emptyList())

    var connectCallCount = 0
    var listFoldersCallCount = 0
    var fetchUidsCallCount = 0
    var fetchEmailsCallCount = 0

    override var isConnected: Boolean = false

    override suspend fun connect(): ImapResult<Unit> {
        connectCallCount++
        if (connectResult is ImapResult.Success) isConnected = true
        return connectResult
    }

    override suspend fun disconnect() { isConnected = false }

    override suspend fun listFolders(): ImapResult<List<Folder>> {
        listFoldersCallCount++
        return foldersResult
    }

    override suspend fun selectFolder(folderPath: String): ImapResult<Folder> =
        ImapResult.Failure(ImapError.Unknown("not used in tests"))

    override suspend fun fetchUids(folderPath: String, sinceUid: Long): ImapResult<List<Long>> {
        fetchUidsCallCount++
        return uidsResult
    }

    override suspend fun fetchEmails(folderPath: String, uids: List<Long>, fetchBody: Boolean): ImapResult<List<Email>> {
        fetchEmailsCallCount++
        return emailsResult
    }

    override suspend fun fetchEmailBody(folderPath: String, uid: Long): ImapResult<Email> =
        ImapResult.Failure(ImapError.Unknown("not used in tests"))

    override suspend fun markRead(folderPath: String, uids: List<Long>) = ImapResult.Success(Unit)
    override suspend fun markUnread(folderPath: String, uids: List<Long>) = ImapResult.Success(Unit)
    override suspend fun markStarred(folderPath: String, uids: List<Long>, starred: Boolean) = ImapResult.Success(Unit)
    override suspend fun moveMessages(fromFolder: String, toFolder: String, uids: List<Long>) = ImapResult.Success(Unit)
    override suspend fun deleteMessages(folderPath: String, uids: List<Long>) = ImapResult.Success(Unit)
    override suspend fun expunge(folderPath: String) = ImapResult.Success(Unit)
    override fun idle(folderPath: String): Flow<IdleEvent> = emptyFlow()
}

class FakeSyncStorage : SyncStorage {
    val insertedEmails = mutableListOf<cleanmail.models.Email>()
    var uidNextMap = mutableMapOf<String, Long>()

    override fun getUidNext(accountId: String, folderPath: String): Long =
        uidNextMap["$accountId/$folderPath"] ?: 1L

    override fun insertEmails(emails: List<cleanmail.models.Email>) {
        insertedEmails.addAll(emails)
    }
}

class ImapSyncManagerTest {

    private val testAccount = Account(
        id = "acc-1",
        displayName = "Test Account",
        emailAddress = "test@example.com",
        authType = AuthType.PASSWORD,
        imapHost = "imap.example.com",
        imapPort = 993,
        imapSecurity = SecurityType.SSL_TLS,
        smtpHost = "smtp.example.com",
        smtpPort = 587,
        smtpSecurity = SecurityType.STARTTLS
    )

    private fun makeFolder(name: String, sync: Boolean = true) = Folder(
        id = "acc-1/$name",
        accountId = "acc-1",
        name = name,
        fullPath = name,
        role = FolderRole.INBOX,
        syncEnabled = sync
    )

    private fun makeEmails(count: Int, folderId: String = "INBOX") = (1..count).map { i ->
        Email(
            id = "email-$i",
            accountId = "acc-1",
            folderId = folderId,
            uid = i.toLong(),
            messageId = "<msg$i@test>",
            from = EmailAddress(address = "from@test.com"),
            to = listOf(EmailAddress(address = "test@example.com")),
            subject = "Email $i",
            date = Instant.fromEpochMilliseconds(0L)
        )
    }

    private fun manager(fake: FakeImapClient, storage: FakeSyncStorage = FakeSyncStorage()): ImapSyncManager =
        ImapSyncManager(testAccount, client = fake, storage = storage)

    // --- connect ---

    @Test fun fullSync_connectsWhenNotConnected() = runTest {
        val fake = FakeImapClient()
        fake.foldersResult = ImapResult.Success(listOf(makeFolder("INBOX")))
        manager(fake).fullSync()
        assertEquals(1, fake.connectCallCount)
    }

    @Test fun fullSync_skipsConnectWhenAlreadyConnected() = runTest {
        val fake = FakeImapClient().also { it.isConnected = true }
        fake.foldersResult = ImapResult.Success(listOf(makeFolder("INBOX")))
        manager(fake).fullSync()
        assertEquals(0, fake.connectCallCount)
    }

    @Test fun fullSync_returnsSummaryWithAccountId() = runTest {
        val fake = FakeImapClient()
        fake.foldersResult = ImapResult.Success(emptyList())
        val summary = manager(fake).fullSync()
        assertEquals("acc-1", summary.accountId)
    }

    // --- connection failure ---

    @Test fun fullSync_connectFailure_returnsEmptySummary() = runTest {
        val fake = FakeImapClient()
        fake.connectResult = ImapResult.Failure(ImapError.ConnectionFailed("Connection refused"))
        val summary = manager(fake).fullSync()
        assertEquals("acc-1", summary.accountId)
        assertTrue(summary.folders.isEmpty())
    }

    // --- folder filtering ---

    @Test fun fullSync_onlySyncsEnabledFolders() = runTest {
        val fake = FakeImapClient()
        fake.foldersResult = ImapResult.Success(listOf(
            makeFolder("INBOX", sync = true),
            makeFolder("Spam",  sync = false),
            makeFolder("Sent",  sync = true)
        ))
        fake.uidsResult = ImapResult.Success(emptyList())
        manager(fake).fullSync()
        assertEquals(2, fake.fetchUidsCallCount)
    }

    @Test fun fullSync_foldersFailure_returnsEmptySummary() = runTest {
        val fake = FakeImapClient()
        fake.foldersResult = ImapResult.Failure(ImapError.Unknown("List failed"))
        val summary = manager(fake).fullSync()
        assertTrue(summary.folders.isEmpty())
    }

    // --- uid / email fetch ---

    @Test fun fullSync_noNewUids_zeroFetchedCount() = runTest {
        val fake = FakeImapClient()
        fake.foldersResult = ImapResult.Success(listOf(makeFolder("INBOX")))
        fake.uidsResult = ImapResult.Success(emptyList())
        val summary = manager(fake).fullSync()
        assertEquals(0, summary.folders.first().fetched)
    }

    @Test fun fullSync_withEmails_returnsCorrectCount() = runTest {
        val fake = FakeImapClient()
        fake.foldersResult = ImapResult.Success(listOf(makeFolder("INBOX")))
        fake.uidsResult = ImapResult.Success(listOf(1L, 2L, 3L))
        fake.emailsResult = ImapResult.Success(makeEmails(3))
        val summary = manager(fake).fullSync()
        assertEquals(3, summary.folders.first().fetched)
    }

    @Test fun fullSync_emailsFetchFailure_zeroFetchedCount() = runTest {
        val fake = FakeImapClient()
        fake.foldersResult = ImapResult.Success(listOf(makeFolder("INBOX")))
        fake.uidsResult = ImapResult.Success(listOf(1L, 2L))
        fake.emailsResult = ImapResult.Failure(ImapError.Unknown("Fetch failed"))
        val summary = manager(fake).fullSync()
        assertEquals(0, summary.folders.first().fetched)
    }

    // --- storage ---

    @Test fun fullSync_persistsEmails_toStorage() = runTest {
        val fake = FakeImapClient()
        val storage = FakeSyncStorage()
        fake.foldersResult = ImapResult.Success(listOf(makeFolder("INBOX")))
        fake.uidsResult = ImapResult.Success(listOf(1L, 2L))
        fake.emailsResult = ImapResult.Success(makeEmails(2))
        manager(fake, storage).fullSync()
        assertEquals(2, storage.insertedEmails.size)
    }

    @Test fun syncFolder_usesStoredUidNext() = runTest {
        val fake = FakeImapClient()
        val storage = FakeSyncStorage().also { it.uidNextMap["acc-1/INBOX"] = 50L }
        var capturedSinceUid = 0L
        val capturingClient = object : IImapClient by fake {
            override val isConnected = true
            override suspend fun fetchUids(folderPath: String, sinceUid: Long): ImapResult<List<Long>> {
                capturedSinceUid = sinceUid
                return ImapResult.Success(emptyList())
            }
        }
        ImapSyncManager(testAccount, client = capturingClient, storage = storage)
            .syncFolder("INBOX")
        assertEquals(50L, capturedSinceUid)
    }

    @Test fun fullSync_durationMs_nonNegative() = runTest {
        val fake = FakeImapClient()
        fake.foldersResult = ImapResult.Success(emptyList())
        val summary = manager(fake).fullSync()
        assertTrue(summary.durationMs >= 0)
    }
}
