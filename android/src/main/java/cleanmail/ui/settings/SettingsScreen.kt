package cleanmail.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cleanmail.CleanMailApp
import cleanmail.models.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onAddAccount: () -> Unit,
    onEditAccount: (String) -> Unit,
    onBack: () -> Unit
) {
    val db = CleanMailApp.instance.database
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            accounts = db.accountQueries.selectAll().executeAsList().map { row ->
                Account(
                    id = row.id,
                    displayName = row.display_name,
                    emailAddress = row.email_address,
                    authType = cleanmail.models.AuthType.valueOf(row.auth_type),
                    imapHost = row.imap_host,
                    imapPort = row.imap_port.toInt(),
                    imapSecurity = cleanmail.models.SecurityType.valueOf(row.imap_security),
                    smtpHost = row.smtp_host,
                    smtpPort = row.smtp_port.toInt(),
                    smtpSecurity = cleanmail.models.SecurityType.valueOf(row.smtp_security),
                    syncIntervalMinutes = row.sync_interval_minutes.toInt(),
                    isActive = row.is_active == 1L
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAddAccount) {
                        Icon(Icons.Default.Add, contentDescription = "Add account")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                ListItem(headlineContent = { Text("Accounts", style = MaterialTheme.typography.titleSmall) })
                HorizontalDivider()
            }
            items(accounts) { account ->
                ListItem(
                    modifier = Modifier.clickable { onEditAccount(account.id) },
                    headlineContent = { Text(account.displayName) },
                    supportingContent = { Text(account.emailAddress) },
                    trailingContent = {
                        Text(
                            if (account.isActive) "Active" else "Inactive",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
                HorizontalDivider()
            }
            item {
                Spacer(Modifier.height(24.dp))
                ListItem(headlineContent = { Text("Privacy", style = MaterialTheme.typography.titleSmall) })
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("No analytics") },
                    supportingContent = { Text("CleanMail never collects or transmits usage data") }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Encrypted storage") },
                    supportingContent = { Text("All emails and credentials stored with AES-256-GCM") }
                )
                HorizontalDivider()
            }
        }
    }
}
