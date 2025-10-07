
package admin.panel

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.chat.safeplay.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun AdminUserDetailScreen(
    navController: NavHostController,
    userId: String
)
 {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch user details
    LaunchedEffect(userId) {
        try {
            val snapshot = firestore.collection("users").document(userId).get().await()
            if (snapshot.exists()) {
                user = snapshot.toObject(User::class.java)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Edit User") },
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

                user == null -> Text(
                    text = "User not found.",
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> {
                    var name by remember { mutableStateOf(user!!.name) }
                    var email by remember { mutableStateOf(user!!.email) }
                    var phone by remember { mutableStateOf(user!!.phone) }
                    var role by remember { mutableStateOf(user!!.role) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false // usually email shouldn't be edited
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = role,
                            onValueChange = { role = it },
                            label = { Text("Role") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val updatedUser = user!!.copy(
                                    name = name,
                                    phone = phone,
                                    role = role
                                )
                                firestore.collection("users").document(userId)
                                    .update(
                                        mapOf(
                                            "name" to updatedUser.name,
                                            "phone" to updatedUser.phone,
                                            "role" to updatedUser.role
                                        )
                                    )
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            context,
                                            "User updated successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        navController.popBackStack()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            context,
                                            "Update failed: ${it.localizedMessage}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}
