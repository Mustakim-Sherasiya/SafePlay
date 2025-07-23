package com.chat.safeplay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    lastSentTimeMillis: Long,
    onUpdateLastSentTime: (Long) -> Unit,
    onResetClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Cooldown duration in milliseconds
    val cooldownMillis = 60_000L
    val now = System.currentTimeMillis()
    val remainingTimeMillis = (lastSentTimeMillis + cooldownMillis - now).coerceAtLeast(0)

    // Track countdown seconds
    var countdownSeconds by remember { mutableStateOf((remainingTimeMillis / 1000).toInt()) }

    // Countdown timer effect
    LaunchedEffect(key1 = remainingTimeMillis) {
        if (remainingTimeMillis > 0) {
            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds -= 1
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Forgot Password", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (errorMessage != null) errorMessage = null
            },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Show Send or Resend button based on cooldown
        if (remainingTimeMillis > 0) {
            Text(
                text = "You can resend reset link in $countdownSeconds seconds",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = { /* Disabled during cooldown */ },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Reset Link")
            }
        } else {
            Button(
                onClick = {
                    if (email.isBlank()) {
                        errorMessage = "Please enter your email"
                    } else {
                        errorMessage = null
                        onResetClick(email.trim())
                        // Update cooldown start time immediately to block rapid resend taps
                        onUpdateLastSentTime(System.currentTimeMillis())
                        countdownSeconds = 60
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank()
            ) {
                Text("Send Reset Link")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBackClick) {
            Text("Back to Login")
        }
    }
}
