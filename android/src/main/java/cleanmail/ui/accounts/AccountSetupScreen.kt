package cleanmail.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cleanmail.CleanMailApp
import cleanmail.crypto.CryptoManager
import cleanmail.models.*
import cleanmail.db.toDbEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSetupScreen(
    accountId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val db    = CleanMailApp.instance.database
    val scope = rememberCoroutineScope()

    var displayName by remember { mutableStateOf("") }
    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var imapHost    by remember { mutableStateOf("") }
    var imapPort    by remember { mutableStateOf("993") }
    var smtpHost    by remember { mutableStateOf("") }
    var smtpPort    by remember { mutableStateOf("587") }
    var isSaving    by remember { mutableStateOf(false) }
    var error       by remember { mutableStateOf<String?>(null) }

    // Detect email domain and auto-fill hosts
    LaunchedEffect(email) {
        val domain = email.substringAfter("@").lowercase()
        when {
            domain.contains("gmail")                          -> { imapHost = "imap.gmail.com";         smtpHost = "smtp.gmail.com" }
            domain.contains("outlook") || domain.contains("hotmail") -> { imapHost = "outlook.office365.com"; smtpHost = "smtp.office365.com" }
            domain.contains("yahoo")                          -> { imapHost = "imap.mail.yahoo.com";    smtpHost = "smtp.mail.yahoo.com" }
            domain.isNotBlank()                               -> { imapHost = "imap.$domain";           smtpHost = "smtp.$domain" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (accountId == null) "Add Account" else "Edit Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Account", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(value = displayName, onValueChange = { displayName = it },
                label = { Text("Display name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = email, onValueChange = { email = it },
                label = { Text("Email address") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))

            OutlinedTextField(value = password, onValueChange = { password = it },
                label = { Text("Password / App password") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, visualTransformation = PasswordVisualTransformation())

            if (email.substringAfter("@").contains("gmail")) {
                Text(
                    "Для Gmail используй App Password: myaccount.google.com → Безопасность → Пароли приложений",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(Modifier.height(4.dp))
            Text("IMAP", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = imapHost, onValueChange = { imapHost = it },
                    label = { Text("Host") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = imapPort, onValueChange = { imapPort = it },
                    label = { Text("Port") }, modifier = Modifier.width(80.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }

            Text("SMTP", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = smtpHost, onValueChange = { smtpHost = it },
                    label = { Text("Host") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = smtpPort, onValueChange = { smtpPort = it },
                    label = { Text("Port") }, modifier = Modifier.width(80.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                     style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (email.isBlank() || imapHost.isBlank() || smtpHost.isBlank()) {
                        error = "Email, IMAP host and SMTP host are required"
                        return@Button
                    }
                    if (password.isBlank()) {
                        error = "Password required for password-based login"
                        return@Button
                    }
                    isSaving = true
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val encPass = CryptoManager.encrypt(password, CryptoManager.KEY_ALIAS_ACCOUNTS)
                            val account = Account(
                                id           = accountId ?: UUID.randomUUID().toString(),
                                displayName  = displayName.ifBlank { email },
                                emailAddress = email,
                                authType     = AuthType.PASSWORD,
                                imapHost     = imapHost,
                                imapPort     = imapPort.toIntOrNull() ?: 993,
                                imapSecurity = SecurityType.SSL_TLS,
                                smtpHost     = smtpHost,
                                smtpPort     = smtpPort.toIntOrNull() ?: 587,
                                smtpSecurity = SecurityType.STARTTLS,
                                passwordEncrypted = encPass
                            )
                            db.accountQueries.insert(account.toDbEntity())
                        }
                        onSaved()
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Save")
            }
        }
    }
}
