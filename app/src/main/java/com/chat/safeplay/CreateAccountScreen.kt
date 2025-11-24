package com.chat.safeplay

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth


@Composable
fun CreateAccountScreen(
    onCreateAccountClick: (String, String, String) -> Unit, // kept for compatibility
    onBackToLoginClick: () -> Unit
) {

    // ✅ Use rememberSaveable for all user inputs and view states
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isOtpSent by rememberSaveable { mutableStateOf(false) }
    var otpInput by rememberSaveable { mutableStateOf("") }
    var verificationId by rememberSaveable { mutableStateOf("") }
    var isProcessing by rememberSaveable { mutableStateOf(false) }



//    var email by remember { mutableStateOf("") }
//    var phoneNumber by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//    var confirmPassword by remember { mutableStateOf("") }
//    var passwordVisible by remember { mutableStateOf(false) }
//    var confirmPasswordVisible by remember { mutableStateOf(false) }
//    var errorMessage by remember { mutableStateOf<String?>(null) }
//
//    var isOtpSent by remember { mutableStateOf(false) }
//    var otpInput by remember { mutableStateOf("") }
//    var verificationId by remember { mutableStateOf("") }
//    var isProcessing by remember { mutableStateOf(false) } // Loading state

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding() // ✅ keeps UI above keyboard
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState) // ✅ enables scrolling
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Create Account", style = MaterialTheme.typography.headlineMedium)
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
                modifier = Modifier.fillMaxWidth(),
                enabled = !isOtpSent && !isProcessing
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = {
                    phoneNumber = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text("Phone Number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isOtpSent && !isProcessing
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon =
                        if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = icon, contentDescription = "Toggle password visibility")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isOtpSent && !isProcessing
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text("Confirm Password") },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon =
                        if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(imageVector = icon, contentDescription = "Toggle password visibility")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isOtpSent && !isProcessing
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                if (!isOtpSent) {
                    Button(
                        onClick = {
                            errorMessage = null

                            if (email.isBlank() || phoneNumber.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                                errorMessage = "Please fill in all fields"
                                return@Button
                            }
                            if (password != confirmPassword) {
                                errorMessage = "Passwords do not match"
                                return@Button
                            }

                            // Basic client-side phone formatting check can be added by you.
                            isProcessing = true

                            // sendPhoneOtp is in your helper file. It will call onCodeSent with the verificationId.
                            sendPhoneOtp(
                                phoneNumber,
                                context,
                                onCodeSent = { verId ->
                                    verificationId = verId
                                    isOtpSent = true
                                    isProcessing = false
                                    Toast.makeText(
                                        context,
                                        "OTP sent to your phone",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onFailure = { error ->
                                    isProcessing = false
                                    errorMessage = error
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = email.isNotBlank() && phoneNumber.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
                    ) {
                        Text("Create Account")
                    }
                } else {
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { otpInput = it },
                        label = { Text("Enter OTP") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (otpInput.length < 6) {
                                errorMessage = "Enter valid 6-digit OTP"
                                return@Button
                            }
                            errorMessage = null
                            isProcessing = true

                            // Call the linking-safe verifyPhoneOtp helper (replace that function in your helper file).
                            verifyPhoneOtp(
                                verificationId,
                                otpInput,
                                email.trim(),
                                password,
                                context,
                                onSuccess = {
                                    isProcessing = false
                                    Toast.makeText(
                                        context,
                                        "Account created — verification email sent. Check your inbox.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    // helper signs out already; ensure signed out locally
                                    FirebaseAuth.getInstance().signOut()

                                    // Optional: invoke the callback (kept for backward compatibility)
                                    onCreateAccountClick(email.trim(), phoneNumber.trim(), password)

                                    // Navigate back to login screen
                                    onBackToLoginClick()
                                },
                                onFailure = { error ->
                                    isProcessing = false
                                    errorMessage = error
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = otpInput.length == 6 && !isProcessing
                    ) {
                        Text("Verify OTP & Create Account")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBackToLoginClick) {
                Text("Back to Login")
            }
        }
    }
}






//package com.chat.safeplay
//
//import android.widget.Toast
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Visibility
//import androidx.compose.material.icons.filled.VisibilityOff
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.runtime.saveable.rememberSaveable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.text.input.PasswordVisualTransformation
//import androidx.compose.ui.text.input.VisualTransformation
//import androidx.compose.ui.unit.dp
//import com.google.firebase.auth.FirebaseAuth
//
//
//@Composable
//fun CreateAccountScreen(
//    onCreateAccountClick: (String, String, String) -> Unit, // kept for compatibility
//    onBackToLoginClick: () -> Unit
//) {
//
//    // ✅ Use rememberSaveable for all user inputs and view states
//    var email by rememberSaveable { mutableStateOf("") }
//    var phoneNumber by rememberSaveable { mutableStateOf("") }
//    var password by rememberSaveable { mutableStateOf("") }
//    var confirmPassword by rememberSaveable { mutableStateOf("") }
//
//    var passwordVisible by rememberSaveable { mutableStateOf(false) }
//    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
//
//    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
//    var isOtpSent by rememberSaveable { mutableStateOf(false) }
//    var otpInput by rememberSaveable { mutableStateOf("") }
//    var verificationId by rememberSaveable { mutableStateOf("") }
//    var isProcessing by rememberSaveable { mutableStateOf(false) }
//
//
//
////    var email by remember { mutableStateOf("") }
////    var phoneNumber by remember { mutableStateOf("") }
////    var password by remember { mutableStateOf("") }
////    var confirmPassword by remember { mutableStateOf("") }
////    var passwordVisible by remember { mutableStateOf(false) }
////    var confirmPasswordVisible by remember { mutableStateOf(false) }
////    var errorMessage by remember { mutableStateOf<String?>(null) }
////
////    var isOtpSent by remember { mutableStateOf(false) }
////    var otpInput by remember { mutableStateOf("") }
////    var verificationId by remember { mutableStateOf("") }
////    var isProcessing by remember { mutableStateOf(false) } // Loading state
//
//    val context = LocalContext.current
//    val scrollState = rememberScrollState()
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .imePadding() // ✅ keeps UI above keyboard
//    ) {
//        val scrollState = rememberScrollState()
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .verticalScroll(scrollState) // ✅ enables scrolling
//                .padding(24.dp),
//            verticalArrangement = Arrangement.Center,
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(text = "Create Account", style = MaterialTheme.typography.headlineMedium)
//            Spacer(modifier = Modifier.height(24.dp))
//
//            OutlinedTextField(
//                value = email,
//                onValueChange = {
//                    email = it
//                    if (errorMessage != null) errorMessage = null
//                },
//                label = { Text("Email") },
//                singleLine = true,
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
//                modifier = Modifier.fillMaxWidth(),
//                enabled = !isOtpSent && !isProcessing
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//
//            OutlinedTextField(
//                value = phoneNumber,
//                onValueChange = {
//                    phoneNumber = it
//                    if (errorMessage != null) errorMessage = null
//                },
//                label = { Text("Phone Number") },
//                singleLine = true,
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
//                modifier = Modifier.fillMaxWidth(),
//                enabled = !isOtpSent && !isProcessing
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//
//            OutlinedTextField(
//                value = password,
//                onValueChange = {
//                    password = it
//                    if (errorMessage != null) errorMessage = null
//                },
//                label = { Text("Password") },
//                singleLine = true,
//                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
//                trailingIcon = {
//                    val icon =
//                        if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
//                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
//                        Icon(imageVector = icon, contentDescription = "Toggle password visibility")
//                    }
//                },
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
//                modifier = Modifier.fillMaxWidth(),
//                enabled = !isOtpSent && !isProcessing
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//
//            OutlinedTextField(
//                value = confirmPassword,
//                onValueChange = {
//                    confirmPassword = it
//                    if (errorMessage != null) errorMessage = null
//                },
//                label = { Text("Confirm Password") },
//                singleLine = true,
//                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
//                trailingIcon = {
//                    val icon =
//                        if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
//                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
//                        Icon(imageVector = icon, contentDescription = "Toggle password visibility")
//                    }
//                },
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
//                modifier = Modifier.fillMaxWidth(),
//                enabled = !isOtpSent && !isProcessing
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//
//            if (errorMessage != null) {
//                Text(
//                    text = errorMessage ?: "",
//                    color = MaterialTheme.colorScheme.error,
//                    modifier = Modifier.padding(bottom = 16.dp)
//                )
//            }
//
//            if (isProcessing) {
//                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
//            } else {
//                if (!isOtpSent) {
//                    Button(
//                        onClick = {
//                            errorMessage = null
//
//                            if (email.isBlank() || phoneNumber.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
//                                errorMessage = "Please fill in all fields"
//                                return@Button
//                            }
//                            if (password != confirmPassword) {
//                                errorMessage = "Passwords do not match"
//                                return@Button
//                            }
//
//                            // Basic client-side phone formatting check can be added by you.
//                            isProcessing = true
//
//                            // sendPhoneOtp is in your helper file. It will call onCodeSent with the verificationId.
//                            sendPhoneOtp(
//                                phoneNumber,
//                                context,
//                                onCodeSent = { verId ->
//                                    verificationId = verId
//                                    isOtpSent = true
//                                    isProcessing = false
//                                    Toast.makeText(
//                                        context,
//                                        "OTP sent to your phone",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//                                },
//                                onFailure = { error ->
//                                    isProcessing = false
//                                    errorMessage = error
//                                }
//                            )
//                        },
//                        modifier = Modifier.fillMaxWidth(),
//                        enabled = email.isNotBlank() && phoneNumber.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
//                    ) {
//                        Text("Create Account")
//                    }
//                } else {
//                    OutlinedTextField(
//                        value = otpInput,
//                        onValueChange = { otpInput = it },
//                        label = { Text("Enter OTP") },
//                        singleLine = true,
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                        modifier = Modifier.fillMaxWidth()
//                    )
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    Button(
//                        onClick = {
//                            if (otpInput.length < 6) {
//                                errorMessage = "Enter valid 6-digit OTP"
//                                return@Button
//                            }
//                            errorMessage = null
//                            isProcessing = true
//
//                            // Call the linking-safe verifyPhoneOtp helper (replace that function in your helper file).
//                            verifyPhoneOtp(
//                                verificationId,
//                                otpInput,
//                                email.trim(),
//                                password,
//                                context,
//                                onSuccess = {
//                                    isProcessing = false
//                                    Toast.makeText(
//                                        context,
//                                        "Account created — verification email sent. Check your inbox.",
//                                        Toast.LENGTH_LONG
//                                    ).show()
//                                    // helper signs out already; ensure signed out locally
//                                    FirebaseAuth.getInstance().signOut()
//
//                                    // Optional: invoke the callback (kept for backward compatibility)
//                                    onCreateAccountClick(email.trim(), phoneNumber.trim(), password)
//
//                                    // Navigate back to login screen
//                                    onBackToLoginClick()
//                                },
//                                onFailure = { error ->
//                                    isProcessing = false
//                                    errorMessage = error
//                                }
//                            )
//                        },
//                        modifier = Modifier.fillMaxWidth(),
//                        enabled = otpInput.length == 6 && !isProcessing
//                    ) {
//                        Text("Verify OTP & Create Account")
//                    }
//                }
//            }
//            Spacer(modifier = Modifier.height(16.dp))
//            TextButton(onClick = onBackToLoginClick) {
//                Text("Back to Login")
//            }
//        }
//    }
//}
