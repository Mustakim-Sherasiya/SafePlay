package com.chat.safeplay.admin.panel



import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

import com.chat.safeplay.model.User


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
                    email = doc.getString("email") ?: "",
                    phone = doc.getString("phone") ?: "",
                    name = doc.getString("name") ?: "",
                    publicId = doc.getString("publicId") ?: "",
                    showDisplayName = doc.getBoolean("showDisplayName") ?: false,
                    photoUrl = doc.getString("photoUrl") ?: "",
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
                title = { Text("Manage Users") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                errorMessage != null -> Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    items(userList) { user ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    navController.navigate("adminUserDetail/${user.uid}")
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // User photo or fallback
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        if (user.photoUrl.isNotEmpty()) user.photoUrl
                                        else "https://firebasestorage.googleapis.com/v0/b/safeplay-app/logo.png" // fallback
                                    ),
                                    contentDescription = "User Photo",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    val displayName = if (user.showDisplayName && user.name.isNotBlank())
                                        user.name else "User${user.publicId.takeLast(2)}"

                                    Text(displayName, fontWeight = FontWeight.Bold)
                                    Text("Email: ${user.email}")
                                    Text("Phone: ${user.phone}")
                                    Text("Public ID: ${user.publicId}")
                                    Text("Role: ${user.role}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
