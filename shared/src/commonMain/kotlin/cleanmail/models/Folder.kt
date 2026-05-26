package cleanmail.models

import kotlinx.serialization.Serializable

enum class FolderRole { INBOX, SENT, DRAFTS, TRASH, SPAM, ARCHIVE, CUSTOM }

@Serializable
data class Folder(
    val id: String,
    val accountId: String,
    val name: String,
    val fullPath: String,
    val role: FolderRole = FolderRole.CUSTOM,
    val unreadCount: Int = 0,
    val totalCount: Int = 0,
    val uidValidity: Long = 0L,
    val uidNext: Long = 0L,
    val syncEnabled: Boolean = true
)
