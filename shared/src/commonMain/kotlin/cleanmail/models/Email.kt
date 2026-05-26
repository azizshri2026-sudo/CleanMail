package cleanmail.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class EmailAddress(
    val name: String = "",
    val address: String
)

@Serializable
data class Attachment(
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val contentId: String = "",
    val isInline: Boolean = false,
    val localPath: String = ""
)

@Serializable
data class Email(
    val id: String,
    val accountId: String,
    val folderId: String,
    val uid: Long,
    val messageId: String,

    val from: EmailAddress,
    val replyTo: List<EmailAddress> = emptyList(),
    val to: List<EmailAddress>,
    val cc: List<EmailAddress> = emptyList(),
    val bcc: List<EmailAddress> = emptyList(),

    val subject: String,
    val bodyText: String = "",
    val bodyHtml: String = "",
    val attachments: List<Attachment> = emptyList(),

    val date: Instant,
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val isDeleted: Boolean = false,
    val isDraft: Boolean = false,

    val threadId: String = "",
    val inReplyTo: String = "",
    val references: List<String> = emptyList()
)
