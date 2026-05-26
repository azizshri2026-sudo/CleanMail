package cleanmail.db

import cleanmail.models.EmailAddress
import cleanmail.models.Email as DomainEmail
import cleanmail.models.Attachment
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun DomainEmail.toDbEntity(): cleanmail.db.Email = cleanmail.db.Email(
    id = id,
    account_id = accountId,
    folder_id = folderId,
    uid = uid,
    message_id = messageId,
    from_name = from.name,
    from_address = from.address,
    reply_to_json = Json.encodeToString(replyTo),
    to_json = Json.encodeToString(this.to),
    cc_json = Json.encodeToString(cc),
    bcc_json = Json.encodeToString(bcc),
    subject = subject,
    body_text = bodyText,
    body_html = bodyHtml,
    attachments_json = Json.encodeToString(attachments),
    date_epoch_ms = date.toEpochMilliseconds(),
    is_read = if (isRead) 1L else 0L,
    is_starred = if (isStarred) 1L else 0L,
    is_deleted = if (isDeleted) 1L else 0L,
    is_draft = if (isDraft) 1L else 0L,
    thread_id = threadId,
    in_reply_to = inReplyTo,
    references_json = Json.encodeToString(references)
)

fun cleanmail.db.Email.toDomain(): DomainEmail = DomainEmail(
    id = id,
    accountId = account_id,
    folderId = folder_id,
    uid = uid,
    messageId = message_id,
    from = EmailAddress(from_name, from_address),
    replyTo = Json.decodeFromString(reply_to_json),
    to = Json.decodeFromString(to_json),
    cc = Json.decodeFromString(cc_json),
    bcc = Json.decodeFromString(bcc_json),
    subject = subject,
    bodyText = body_text,
    bodyHtml = body_html,
    attachments = Json.decodeFromString(attachments_json),
    date = Instant.fromEpochMilliseconds(date_epoch_ms),
    isRead = is_read == 1L,
    isStarred = is_starred == 1L,
    isDeleted = is_deleted == 1L,
    isDraft = is_draft == 1L,
    threadId = thread_id,
    inReplyTo = in_reply_to,
    references = Json.decodeFromString(references_json)
)
