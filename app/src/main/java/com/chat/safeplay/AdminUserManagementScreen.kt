package com.chat.safeplay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.Alignment
import androidx.navigation.NavHostController
import com.chat.safeplay.model.User  // Import the User data class here

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    var userList by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val snapshot = firestore.collection("users").get().await()
            val users = snapshot.documents.map { doc ->
                User(
                    uid = doc.id,
                    email = doc.getString("email") ?: "No Email",
                    phone = doc.getString("phone") ?: "",
                    role = doc.getString("role") ?: "user"
                )
            }
            userList = users
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Unknown error"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Manage Users") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage != null -> {
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn {
                        items(userList) { user ->
                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* Add user details or edit actions later */ },
                                headlineContent = { Text(user.email) },
                                supportingContent = { Text("Role: ${user.role}") }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}
