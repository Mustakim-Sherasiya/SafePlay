
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
fun ResetPinSecurityQuestionScreen(
    userId: String,
    securityQuestion: String,
    onAnswerVerified: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var answerInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Answer your security question", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        Text(securityQuestion, style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = answerInput,
            onValueChange = { answerInput = it },
            label = { Text("Answer") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                isLoading = true
                firestore.collection("users").document(userId).get()
                    .addOnSuccessListener { doc ->
                        val correctAnswer = doc.getString("securityAnswer")
                        if (correctAnswer != null && answerInput.trim().equals(correctAnswer.trim(), ignoreCase = true)) {
                            onAnswerVerified()
                        } else {
                            Toast.makeText(context, "Incorrect answer, try again.", Toast.LENGTH_SHORT).show()
                        }
                        isLoading = false
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to verify answer", Toast.LENGTH_SHORT).show()
                        isLoading = false
                    }
            },
            enabled = answerInput.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Verifying..." else "Verify")
        }
    }
}
