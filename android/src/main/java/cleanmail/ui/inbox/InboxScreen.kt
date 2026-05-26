package cleanmail.ui.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cleanmail.models.Email
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    accountId: String,
    onCompose: () -> Unit,
    onSettings: () -> Unit,
    onReply: (String) -> Unit,
    onSetupAccount: () -> Unit = {},
    vm: InboxViewModel = viewModel(factory = InboxViewModel.Factory(accountId))
) {
    val state by vm.uiState.collectAsState()

    if (state.noAccounts) {
        LaunchedEffect(Unit) { onSetupAccount() }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inbox") },
                actions = {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { vm.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync")
                        }
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCompose) {
                Icon(Icons.Default.Edit, contentDescription = "Compose")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> ErrorView(state.error!!) { vm.refresh() }
                state.emails.isEmpty() -> EmptyInbox()
                else -> EmailList(
                    emails = state.emails,
                    onMarkRead = { vm.markRead(it.id) },
                    onDelete = { vm.delete(it) },
                    onReply = { onReply(it.id) }
                )
            }
        }
    }
}

@Composable
private fun EmailList(
    emails: List<Email>,
    onMarkRead: (Email) -> Unit,
    onDelete: (Email) -> Unit,
    onReply: (Email) -> Unit
) {
    LazyColumn {
        items(emails, key = { it.id }) { email ->
            EmailRow(
                email = email,
                onMarkRead = { onMarkRead(email) },
                onDelete = { onDelete(email) },
                onReply = { onReply(email) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun EmailRow(
    email: Email,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
    onReply: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        modifier = Modifier.clickable { expanded = !expanded },
        headlineContent = {
            Text(
                text = email.subject,
                fontWeight = if (email.isRead) FontWeight.Normal else FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = email.bodyText.take(80).replace('\n', ' '),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        },
        overlineContent = {
            Text(
                text = email.from.name.ifBlank { email.from.address },
                style = MaterialTheme.typography.labelSmall
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = email.date.toLocalDateTime(TimeZone.currentSystemDefault())
                        .let { "${it.hour}:${it.minute.toString().padStart(2, '0')}" },
                    style = MaterialTheme.typography.labelSmall
                )
                if (email.isStarred) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        },
        leadingContent = {
            if (!email.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .padding(top = 4.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxSize()
                    ) {}
                }
            }
        }
    )
    if (expanded) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onReply)   { Text("Reply") }
            TextButton(onClick = onMarkRead) { Text("Mark read") }
            TextButton(onClick = onDelete)  { Text("Delete") }
        }
    }
}

@Composable
private fun EmptyInbox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No messages", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}
