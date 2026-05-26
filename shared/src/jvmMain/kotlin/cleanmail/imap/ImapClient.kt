package cleanmail.imap

import cleanmail.models.*
import com.sun.mail.imap.IMAPFolder
import jakarta.mail.*
import jakarta.mail.FetchProfile
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.internet.InternetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import java.util.Properties
import jakarta.mail.event.MessageCountAdapter
import jakarta.mail.event.MessageCountEvent

actual class ImapClient actual constructor(private val account: Account) : IImapClient {

    private var store: Store? = null

    actual override val isConnected: Boolean
        get() = store?.isConnected == true

    actual override suspend fun connect(): ImapResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val session = Session.getInstance(buildProperties())
            val s = session.getStore(if (account.imapSecurity == SecurityType.SSL_TLS) "imaps" else "imap")
            s.connect(account.imapHost, account.imapPort, account.emailAddress, decryptPassword())
            store = s
        }.toImapResult()
    }

    actual override suspend fun disconnect() = withContext(Dispatchers.IO) {
        runCatching { store?.close() }
        store = null
    }

    actual override suspend fun listFolders(): ImapResult<List<cleanmail.models.Folder>> = withContext(Dispatchers.IO) {
        runCatching {
            requireStore().defaultFolder.list("*").map { it.toCleanFolder(account.id) }
        }.toImapResult()
    }

    actual override suspend fun selectFolder(folderPath: String): ImapResult<cleanmail.models.Folder> = withContext(Dispatchers.IO) {
        runCatching {
            val folder = requireStore().getFolder(folderPath)
            folder.open(Folder.READ_ONLY)
            folder.toCleanFolder(account.id)
        }.toImapResult()
    }

    actual override suspend fun fetchUids(folderPath: String, sinceUid: Long): ImapResult<List<Long>> = withContext(Dispatchers.IO) {
        runCatching {
            val folder = requireStore().getFolder(folderPath) as IMAPFolder
            if (!folder.isOpen) folder.open(Folder.READ_ONLY)
            val messages = if (sinceUid <= 1L) folder.messages
            else folder.getMessagesByUID(sinceUid, UIDFolder.LASTUID)
            messages.map { folder.getUID(it) }
        }.toImapResult()
    }

    actual override suspend fun fetchEmails(
        folderPath: String,
        uids: List<Long>,
        fetchBody: Boolean
    ): ImapResult<List<Email>> = withContext(Dispatchers.IO) {
        runCatching {
            val imapFolder = requireStore().getFolder(folderPath) as IMAPFolder
            if (!imapFolder.isOpen) imapFolder.open(Folder.READ_ONLY)
            val fp = FetchProfile().apply {
                add(FetchProfile.Item.ENVELOPE)
                add(FetchProfile.Item.FLAGS)
                if (fetchBody) add(FetchProfile.Item.CONTENT_INFO)
            }
            val messages = uids.mapNotNull { imapFolder.getMessageByUID(it) }
            imapFolder.fetch(messages.toTypedArray(), fp)
            messages.map { msg -> msg.toEmail(account.id, folderPath, imapFolder.getUID(msg)) }
        }.toImapResult()
    }

    actual override suspend fun fetchEmailBody(folderPath: String, uid: Long): ImapResult<Email> = withContext(Dispatchers.IO) {
        runCatching {
            val imapFolder = requireStore().getFolder(folderPath) as IMAPFolder
            if (!imapFolder.isOpen) imapFolder.open(Folder.READ_ONLY)
            val msg = imapFolder.getMessageByUID(uid) ?: error("UID $uid not found")
            msg.toEmail(account.id, folderPath, uid, fetchBody = true)
        }.toImapResult()
    }

    actual override suspend fun markRead(folderPath: String, uids: List<Long>) = setFlag(folderPath, uids, Flags.Flag.SEEN, true)
    actual override suspend fun markUnread(folderPath: String, uids: List<Long>) = setFlag(folderPath, uids, Flags.Flag.SEEN, false)
    actual override suspend fun markStarred(folderPath: String, uids: List<Long>, starred: Boolean) = setFlag(folderPath, uids, Flags.Flag.FLAGGED, starred)

    actual override suspend fun moveMessages(fromFolder: String, toFolder: String, uids: List<Long>): ImapResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val s = requireStore()
            val src = s.getFolder(fromFolder) as IMAPFolder
            val dst = s.getFolder(toFolder)
            if (!src.isOpen) src.open(Folder.READ_WRITE)
            val messages = uids.mapNotNull { src.getMessageByUID(it) }.toTypedArray()
            src.moveMessages(messages, dst)
        }.toImapResult()
    }

    actual override suspend fun deleteMessages(folderPath: String, uids: List<Long>) = setFlag(folderPath, uids, Flags.Flag.DELETED, true)

    actual override suspend fun expunge(folderPath: String): ImapResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val folder = requireStore().getFolder(folderPath)
            if (!folder.isOpen) folder.open(Folder.READ_WRITE)
            folder.expunge()
            Unit
        }.toImapResult()
    }

    actual override fun idle(folderPath: String): Flow<IdleEvent> = callbackFlow {
        val imapFolder = requireStore().getFolder(folderPath) as IMAPFolder
        if (!imapFolder.isOpen) imapFolder.open(Folder.READ_ONLY)

        val listener = object : MessageCountAdapter() {
            override fun messagesAdded(e: MessageCountEvent) { trySend(IdleEvent.NewMessages) }
            override fun messagesRemoved(e: MessageCountEvent) { trySend(IdleEvent.Expunged) }
        }
        imapFolder.addMessageCountListener(listener)

        try {
            while (!channel.isClosedForSend) {
                imapFolder.idle(true)
            }
        } catch (e: Exception) {
            trySend(IdleEvent.Error(e.message ?: "IDLE error"))
        } finally {
            imapFolder.removeMessageCountListener(listener)
        }

        awaitClose { runCatching { imapFolder.close(false) } }
    }

    private fun requireStore() = store ?: error("Not connected")
    private fun decryptPassword() = account.passwordEncrypted

    private fun buildProperties(): Properties = Properties().apply {
        val proto = if (account.imapSecurity == SecurityType.SSL_TLS) "imaps" else "imap"
        put("mail.$proto.host", account.imapHost)
        put("mail.$proto.port", account.imapPort.toString())
        put("mail.$proto.ssl.enable", (account.imapSecurity == SecurityType.SSL_TLS).toString())
        put("mail.$proto.starttls.enable", (account.imapSecurity == SecurityType.STARTTLS).toString())
        put("mail.$proto.starttls.required", (account.imapSecurity == SecurityType.STARTTLS).toString())
        put("mail.$proto.connectiontimeout", "15000")
        put("mail.$proto.timeout", "15000")
        ProxySocketHelper.proxyProperties(account.proxy).forEach { (k, v) -> put(k, v) }
    }

    private suspend fun setFlag(
        folderPath: String,
        uids: List<Long>,
        flag: Flags.Flag,
        value: Boolean
    ): ImapResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val f = requireStore().getFolder(folderPath) as IMAPFolder
            if (!f.isOpen) f.open(Folder.READ_WRITE)
            val messages = uids.mapNotNull { f.getMessageByUID(it) }.toTypedArray()
            f.setFlags(messages, Flags(flag), value)
        }.toImapResult()
    }
}

private fun Folder.toCleanFolder(accountId: String): cleanmail.models.Folder {
    val role = when (fullName.lowercase()) {
        "inbox" -> FolderRole.INBOX
        "sent", "sent items", "sent messages" -> FolderRole.SENT
        "drafts", "draft" -> FolderRole.DRAFTS
        "trash", "deleted", "deleted items" -> FolderRole.TRASH
        "spam", "junk" -> FolderRole.SPAM
        "archive", "all mail" -> FolderRole.ARCHIVE
        else -> FolderRole.CUSTOM
    }
    return cleanmail.models.Folder(
        id = "$accountId/$fullName",
        accountId = accountId,
        name = name,
        fullPath = fullName,
        role = role,
        unreadCount = if (isOpen) unreadMessageCount else 0,
        totalCount = if (isOpen) messageCount else 0
    )
}

private fun Message.toEmail(
    accountId: String,
    folderId: String,
    uid: Long,
    fetchBody: Boolean = false
): Email {
    val from = (this.from?.firstOrNull() as? InternetAddress)
        ?.let { EmailAddress(it.personal ?: "", it.address) }
        ?: EmailAddress(address = "")
    return Email(
        id = "$folderId/$uid",
        accountId = accountId,
        folderId = folderId,
        uid = uid,
        messageId = getHeader("Message-ID")?.firstOrNull() ?: "",
        from = from,
        to = (getRecipients(Message.RecipientType.TO) ?: emptyArray()).mapIA(),
        cc = (getRecipients(Message.RecipientType.CC) ?: emptyArray()).mapIA(),
        subject = subject ?: "(no subject)",
        bodyText = if (fetchBody) extractText(content) else "",
        bodyHtml = if (fetchBody) extractHtml(content) else "",
        date = Instant.fromEpochMilliseconds(sentDate?.time ?: 0L),
        isRead = flags.contains(Flags.Flag.SEEN),
        isStarred = flags.contains(Flags.Flag.FLAGGED),
        isDeleted = flags.contains(Flags.Flag.DELETED),
        isDraft = flags.contains(Flags.Flag.DRAFT),
        inReplyTo = getHeader("In-Reply-To")?.firstOrNull() ?: ""
    )
}

private fun Array<Address>.mapIA() =
    filterIsInstance<InternetAddress>().map { EmailAddress(it.personal ?: "", it.address) }

private fun extractText(content: Any?): String = when (content) {
    is String -> content
    is Multipart -> (0 until content.count).mapNotNull { content.getBodyPart(it) }
        .firstOrNull { it.contentType.startsWith("text/plain") }?.content as? String ?: ""
    else -> ""
}

private fun extractHtml(content: Any?): String = when (content) {
    is Multipart -> (0 until content.count).mapNotNull { content.getBodyPart(it) }
        .firstOrNull { it.contentType.startsWith("text/html") }?.content as? String ?: ""
    else -> ""
}

private fun <T> Result<T>.toImapResult(): ImapResult<T> = fold(
    onSuccess = { ImapResult.Success(it) },
    onFailure = { e ->
        val msg = e.message ?: ""
        ImapResult.Failure(when {
            msg.contains("authentication", ignoreCase = true) -> ImapError.AuthFailed(msg)
            msg.contains("SSL", ignoreCase = true) || msg.contains("TLS", ignoreCase = true) -> ImapError.SslError(msg)
            msg.contains("timeout", ignoreCase = true) -> ImapError.Timeout(msg)
            msg.contains("connect", ignoreCase = true) -> ImapError.ConnectionFailed(msg)
            else -> ImapError.Unknown(msg)
        })
    }
)
