package cleanmail.smtp

import cleanmail.models.EmailAddress

data class OutgoingAttachment(
    val filename: String,
    val mimeType: String,
    val data: ByteArray
) {
    override fun equals(other: Any?) = other is OutgoingAttachment && filename == other.filename
    override fun hashCode() = filename.hashCode()
}

data class OutgoingMessage(
    val from: EmailAddress,
    val to: List<EmailAddress>,
    val cc: List<EmailAddress> = emptyList(),
    val bcc: List<EmailAddress> = emptyList(),
    val replyTo: List<EmailAddress> = emptyList(),
    val subject: String,
    val bodyText: String = "",
    val bodyHtml: String = "",
    val attachments: List<OutgoingAttachment> = emptyList(),
    val inReplyTo: String = "",
    val references: List<String> = emptyList()
)
