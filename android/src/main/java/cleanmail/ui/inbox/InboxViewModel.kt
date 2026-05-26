package cleanmail.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cleanmail.CleanMailApp
import cleanmail.db.toDomain
import cleanmail.models.Account
import cleanmail.models.Email
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InboxUiState(
    val emails: List<Email> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val noAccounts: Boolean = false
)

class InboxViewModel(private val accountId: String) : ViewModel() {

    private val db = CleanMailApp.instance.database
    private var account: Account? = null

    private val _uiState = MutableStateFlow(InboxUiState(isLoading = true))
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val accounts = db.accountQueries.selectAll().executeAsList()
            if (accounts.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, noAccounts = true) }
                return@launch
            }
            account = accounts.firstOrNull { it.id == accountId }?.toDomain()
                ?: accounts.first().toDomain()
            loadFromDb()
        }
    }

    fun refresh() {
        val acc = account ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            runCatching {
                cleanmail.imap.ImapSyncManager(acc, db).fullSync()
            }
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
        val acc = account ?: return
        viewModelScope.launch {
            val inboxFolder = db.folderQueries
                .selectByRole(acc.id, "INBOX")
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

    class Factory(private val accountId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InboxViewModel(accountId) as T
    }
}
