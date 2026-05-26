package cleanmail.ui.accounts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cleanmail.CleanMailApp
import cleanmail.crypto.CryptoManager
import cleanmail.crypto.OAuthManager
import cleanmail.crypto.OAuthProviders
import cleanmail.models.*
import cleanmail.oauth.OAuthCallbackBus
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
    val db      = CleanMailApp.instance.database
    val scope   = rememberCoroutineScope()
    val context = LocalContext.current

    var displayName by remember { mutableStateOf("") }
    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var imapHost    by remember { mutableStateOf("") }
    var imapPort    by remember { mutableStateOf("993") }
    var smtpHost    by remember { mutableStateOf("") }
    var smtpPort    by remember { mutableStateOf("587") }
    var isSaving    by remember { mutableStateOf(false) }
    var error       by remember { mutableStateOf<String?>(null) }
    var authType    by remember { mutableStateOf(AuthType.PASSWORD) }

    // Pending PKCE state for the in-flight OAuth request
    var pendingOAuthVerifier by remember { mutableStateOf<String?>(null) }
    var pendingOAuthState    by remember { mutableStateOf<String?>(null) }
    var pendingOAuthProvider by remember { mutableStateOf<String?>(null) }

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

    // Collect OAuth callback from MainActivity
    LaunchedEffect(Unit) {
        OAuthCallbackBus.events.collect { result ->
            val verifier  = pendingOAuthVerifier ?: return@collect
            val expected  = pendingOAuthState    ?: return@collect
            val provider  = pendingOAuthProvider ?: return@collect

            // Reject if state mismatch (CSRF protection)
            if (result.state != expected) {
                error = "OAuth state mismatch — possible CSRF attack"
                pendingOAuthVerifier = null
                pendingOAuthState    = null
                pendingOAuthProvider = null
                return@collect
            }

            isSaving = true
            error    = null
            scope.launch {
                try {
                    val config  = if (provider == "google") OAuthProviders.gmail else OAuthProviders.outlook
                    val manager = OAuthManager(config)
                    val tokens  = manager.exchangeCode(result.code, verifier).getOrThrow()
                    manager.close()

                    withContext(Dispatchers.IO) {
                        val encAccess  = CryptoManager.encrypt(tokens.accessToken,  CryptoManager.KEY_ALIAS_OAUTH)
                        val encRefresh = CryptoManager.encrypt(tokens.refreshToken, CryptoManager.KEY_ALIAS_OAUTH)
                        val account = Account(
                            id                      = accountId ?: UUID.randomUUID().toString(),
                            displayName             = displayName.ifBlank { email },
                            emailAddress            = email,
                            authType                = AuthType.OAUTH2,
                            imapHost                = imapHost,
                            imapPort                = imapPort.toIntOrNull() ?: 993,
                            imapSecurity            = SecurityType.SSL_TLS,
                            smtpHost                = smtpHost,
                            smtpPort                = smtpPort.toIntOrNull() ?: 587,
                            smtpSecurity            = SecurityType.STARTTLS,
                            oauthTokenEncrypted     = encAccess,
                            oauthRefreshTokenEncrypted = encRefresh
                        )
                        db.accountQueries.insert(account.toDbEntity())
                    }
                    onSaved()
                } catch (e: Exception) {
                    error = e.message ?: "OAuth exchange failed"
                } finally {
                    isSaving = false
                    pendingOAuthVerifier = null
                    pendingOAuthState    = null
                    pendingOAuthProvider = null
                }
            }
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

            // --- OAuth sign-in buttons ---
            val googleVisible  = email.isBlank() || email.substringAfter("@").contains("gmail")
            val outlookVisible = email.isBlank() || email.substringAfter("@").let { it.contains("outlook") || it.contains("hotmail") || it.contains("microsoft") }

            if (googleVisible || outlookVisible) {
                Text("Sign in with", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (googleVisible) {
                        OutlinedButton(
                            onClick = {
                                val config = OAuthProviders.gmail.copy(clientId = "<YOUR_GOOGLE_CLIENT_ID>")
                                val req    = OAuthManager(config).buildAuthRequest()
                                pendingOAuthVerifier = req.codeVerifier
                                pendingOAuthState    = req.state
                                pendingOAuthProvider = "google"
                                authType = AuthType.OAUTH2
                                // Append provider hint so MainActivity can route the callback
                                val urlWithProvider = "${req.url}&provider=google"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlWithProvider)))
                            }
                        ) { Text("Google") }
                    }
                    if (outlookVisible) {
                        OutlinedButton(
                            onClick = {
                                val config = OAuthProviders.outlook.copy(clientId = "<YOUR_AZURE_CLIENT_ID>")
                                val req    = OAuthManager(config).buildAuthRequest()
                                pendingOAuthVerifier = req.codeVerifier
                                pendingOAuthState    = req.state
                                pendingOAuthProvider = "outlook"
                                authType = AuthType.OAUTH2
                                val urlWithProvider = "${req.url}&provider=outlook"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlWithProvider)))
                            }
                        ) { Text("Outlook") }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("— or use password —",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }

            OutlinedTextField(value = password, onValueChange = { password = it },
                label = { Text("Password / App password") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, visualTransformation = PasswordVisualTransformation())

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
