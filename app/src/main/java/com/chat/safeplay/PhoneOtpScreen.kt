package com.chat.safeplay

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

@Composable
fun PhoneOtpScreen(
    phoneNumber: String,  // Expecting this to already be like "+919664802805"
    navController: NavController,
    auth: FirebaseAuth
) {
    val context = LocalContext.current
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("Sending OTP...") }

    LaunchedEffect(Unit) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)  // ✅ Do NOT add +91 again here
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(context as ComponentActivity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    auth.signInWithCredential(credential).addOnCompleteListener {
                        if (it.isSuccessful) {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            message = "Error: ${it.exception?.message}"
                        }
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    message = "Failed to send OTP: ${e.message}"
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    message = "OTP sent to $phoneNumber"
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Verify Phone: $phoneNumber", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = otpCode,
            onValueChange = { otpCode = it },
            label = { Text("Enter OTP") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                verificationId?.let {
                    val credential = PhoneAuthProvider.getCredential(it, otpCode)
                    auth.signInWithCredential(credential).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            message = "Invalid OTP: ${task.exception?.message}"
                        }
                    }
                } ?: run { message = "OTP not yet sent" }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = otpCode.length == 6
        ) {
            Text("Verify")
        }

        Spacer(Modifier.height(16.dp))
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}
