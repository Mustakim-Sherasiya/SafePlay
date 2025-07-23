package com.safeplay.auth

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chat.safeplay.sendPhoneOtp
import com.chat.safeplay.verifyPhoneOtp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

@Composable
fun SignupScreen(navController: NavController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneOtp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Create Account", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone (+91...)") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            sendPhoneOtp(
                phone, context,
                onCodeSent = TODO()
            ) { id ->
                verificationId = id
                isOtpSent = true
            }
        }) {
            Text("Send OTP to Phone")
        }

        if (isOtpSent) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = phoneOtp,
                onValueChange = { phoneOtp = it },
                label = { Text("Enter Phone OTP") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                verifyPhoneOtp(
                    verificationId, phoneOtp, email, password, context,
                    onSuccess = TODO(),
                    onFailure = TODO()
                )
            }) {
                Text("Verify & Create Account")
            }
        }
    }
}
