package cleanmail.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    replyToId: String?,
    onBack: () -> Unit,
    vm: ComposeViewModel = viewModel(factory = ComposeViewModel.Factory(replyToId))
) {
    val state by vm.uiState.collectAsState()

    LaunchedEffect(state.isSent) {
        if (state.isSent) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (replyToId != null) "Reply" else "New Message") },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.saveDraft()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { vm.send() }) {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = state.to,
                onValueChange = vm::updateTo,
                label = { Text("To") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.cc,
                onValueChange = vm::updateCc,
                label = { Text("Cc") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.subject,
                onValueChange = vm::updateSubject,
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.bodyText,
                onValueChange = vm::updateBody,
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth().weight(1f),
                minLines = 6
            )
            state.error?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
