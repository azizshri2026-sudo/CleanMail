package cleanmail.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cleanmail.CleanMailApp
import cleanmail.db.toDomain
import cleanmail.models.Account
import cleanmail.smtp.MessageComposer
import cleanmail.smtp.OutgoingMessage
import cleanmail.smtp.SmtpResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ComposeUiState(
    val to: String = "",
    val cc: String = "",
    val subject: String = "",
    val bodyText: String = "",
    val isSending: Boolean = false,
    val isSent: Boolean = false,
    val error: String? = null
)

class ComposeViewModel(
    private val account: Account,
    replyToId: String? = null
) : ViewModel() {

    private val db = CleanMailApp.instance.database
    private val composer = MessageComposer(account, db)

    private val _uiState = MutableStateFlow(ComposeUiState())
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    init {
        if (replyToId != null) loadReplyContext(replyToId)
    }

    fun updateTo(value: String)      = _uiState.update { it.copy(to = value) }
    fun updateCc(value: String)      = _uiState.update { it.copy(cc = value) }
    fun updateSubject(value: String) = _uiState.update { it.copy(subject = value) }
    fun updateBody(value: String)    = _uiState.update { it.copy(bodyText = value) }

    fun send() {
        val s = _uiState.value
        if (s.to.isBlank()) {
            _uiState.update { it.copy(error = "Recipient required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            val msg = OutgoingMessage(
                from = cleanmail.models.EmailAddress(account.displayName, account.emailAddress),
                to = s.to.split(",").map { cleanmail.models.EmailAddress(address = it.trim()) },
                cc = s.cc.split(",").filter { it.isNotBlank() }.map { cleanmail.models.EmailAddress(address = it.trim()) },
                subject = s.subject,
                bodyText = s.bodyText
            )
            when (val result = composer.send(msg)) {
                is SmtpResult.Success -> _uiState.update { it.copy(isSent = true, isSending = false) }
                is SmtpResult.Failure -> _uiState.update { it.copy(error = result.error.toString(), isSending = false) }
            }
        }
    }

    fun saveDraft() {
        val s = _uiState.value
        viewModelScope.launch {
            composer.saveDraft(
                OutgoingMessage(
                    from = cleanmail.models.EmailAddress(account.displayName, account.emailAddress),
                    to = s.to.split(",").map { cleanmail.models.EmailAddress(address = it.trim()) },
                    subject = s.subject,
                    bodyText = s.bodyText
                )
            )
        }
    }

    private fun loadReplyContext(emailId: String) {
        viewModelScope.launch {
            val email = db.emailQueries.selectById(emailId).executeAsOneOrNull()?.toDomain() ?: return@launch
            _uiState.update {
                it.copy(
                    to = email.from.address,
                    subject = if (email.subject.startsWith("Re:")) email.subject else "Re: ${email.subject}",
                    bodyText = "\n\n--- ${email.from.name.ifBlank { email.from.address }} wrote:\n${email.bodyText}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { composer.disconnect() }
    }
}
