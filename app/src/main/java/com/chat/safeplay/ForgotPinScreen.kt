

package com.chat.safeplay.forgotpin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ForgotPinScreen(
    onSecurityQuestionFetched: (userId: String, question: String) -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var emailOrPhone by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter your registered Email or Phone", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = emailOrPhone,
            onValueChange = { emailOrPhone = it },
            label = { Text("Email or Phone") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isLoading = true
                // Query Firestore to find user by email or phone and get security question
                firestore.collection("users")
                    .whereEqualTo("email", emailOrPhone)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (querySnapshot.isEmpty) {
                            // Try phone
                            firestore.collection("users")
                                .whereEqualTo("phone", emailOrPhone)
                                .get()
                                .addOnSuccessListener { phoneSnapshot ->
                                    if (phoneSnapshot.isEmpty) {
                                        Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                                        isLoading = false
                                    } else {
                                        val doc = phoneSnapshot.documents[0]
                                        val question = doc.getString("securityQuestion")
                                        if (question.isNullOrEmpty()) {
                                            Toast.makeText(context, "No security question set", Toast.LENGTH_SHORT).show()
                                        } else {
                                            onSecurityQuestionFetched(doc.id, question)
                                        }
                                        isLoading = false
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                                    isLoading = false
                                }
                        } else {
                            val doc = querySnapshot.documents[0]
                            val question = doc.getString("securityQuestion")
                            if (question.isNullOrEmpty()) {
                                Toast.makeText(context, "No security question set", Toast.LENGTH_SHORT).show()
                            } else {
                                onSecurityQuestionFetched(doc.id, question)
                            }
                            isLoading = false
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                        isLoading = false
                    }
            },
            enabled = emailOrPhone.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Loading..." else "Next")
        }
    }
}
