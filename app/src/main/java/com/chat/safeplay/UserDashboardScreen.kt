package com.chat.safeplay

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

data class ChatUser(val name: String, val lastMessage: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboardScreen(
    onLogout: () -> Unit,
    onChatSelected: (ChatUser) -> Unit
) {
    val context = LocalContext.current
    val chatUsers = remember {
        listOf(
            ChatUser("Alice", "Hey! Are you coming today?"),
            ChatUser("Bob", "Meeting postponed to 4 PM"),
            ChatUser("Charlie", "I'll share the file shortly."),
            ChatUser("Daisy", "That was funny! 😂")
        )
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("SafePlay Chat") },
                actions = {
                    IconButton(onClick = {
                        onLogout()
                        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            items(chatUsers) { user ->
                ChatUserItem(user = user, onClick = { onChatSelected(user) })
            }
        }
    }
}

@Composable
fun ChatUserItem(user: ChatUser, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(text = user.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = user.lastMessage,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )
        Divider(modifier = Modifier.padding(top = 12.dp))
    }
}
