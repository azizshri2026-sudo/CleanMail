package cleanmail.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cleanmail.CleanMailApp
import cleanmail.db.toDomain
import cleanmail.imap.ImapSyncManager
import cleanmail.models.Account
import cleanmail.models.Email
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InboxUiState(
    val emails: List<Email> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null
)

class InboxViewModel(private val account: Account) : ViewModel() {

    private val db = CleanMailApp.instance.database
    private val syncManager = ImapSyncManager(account, db)

    private val _uiState = MutableStateFlow(InboxUiState(isLoading = true))
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        loadFromDb()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            runCatching { syncManager.fullSync() }
                .onSuccess { loadFromDb() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message, isSyncing = false) } }
        }
    }

    fun markRead(emailId: String) {
        viewModelScope.launch {
            db.emailQueries.markRead(emailId)
            loadFromDb()
        }
    }

    fun delete(email: Email) {
        viewModelScope.launch {
            db.emailQueries.softDelete(email.id)
            loadFromDb()
        }
    }

    private fun loadFromDb() {
        viewModelScope.launch {
            val inboxFolder = db.folderQueries
                .selectByRole(account.id, "INBOX")
                .executeAsOneOrNull()
            val emails = inboxFolder?.let {
                db.emailQueries
                    .selectByFolder(it.id, limit = 50, offset = 0)
                    .executeAsList()
                    .map { row -> row.toDomain() }
            } ?: emptyList()
            _uiState.update { it.copy(emails = emails, isLoading = false, isSyncing = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { syncManager.disconnect() }
    }
}
