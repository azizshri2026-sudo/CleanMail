package cleanmail.imap

data class FolderSyncResult(
    val folderPath: String,
    val fetched: Int,
    val updated: Int,
    val deleted: Int
)

data class SyncSummary(
    val accountId: String,
    val folders: List<FolderSyncResult>,
    val durationMs: Long
)

sealed class IdleEvent {
    object NewMessages : IdleEvent()
    object FlagsChanged : IdleEvent()
    object Expunged : IdleEvent()
    data class Error(val message: String) : IdleEvent()
}
