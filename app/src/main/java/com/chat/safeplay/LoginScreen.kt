package com.chat.safeplay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.core.util.PatternsCompat

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onCreateAccountClick: () -> Unit,
    onForgotPasswordClick: (String) -> Unit,
    navigateToPhoneOtpScreen: (String) -> Unit
) {
    var userInput by remember { mutableStateOf("") }  // Email or Phone
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var submitAttempted by remember { mutableStateOf(false) }
    var userInputError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }  // Loading state

    fun isValidPhone(input: String): Boolean {
        val cleaned = input.replace("\\s".toRegex(), "")
        return cleaned.matches(Regex("^[+]?[0-9]{10,13}$"))
    }

    fun formatPhoneNumber(input: String): String {
        val cleaned = input.replace("\\s".toRegex(), "")
        return if (cleaned.startsWith("+")) cleaned else "+91$cleaned"
    }

    fun validateInputs(): Boolean {
        var valid = true
        val trimmedInput = userInput.trim()
        userInputError = null

        if (trimmedInput.isEmpty()) {
            userInputError = "Email or phone required"
            valid = false
        } else if (!isValidPhone(trimmedInput) && !PatternsCompat.EMAIL_ADDRESS.matcher(trimmedInput).matches()) {
            userInputError = "Invalid email or phone"
            valid = false
        }

        passwordError = if (password.isBlank()) {
            valid = false
            "Password required"
        } else null

        return valid
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to SafePlay", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = userInput,
            onValueChange = {
                userInput = it
                if (submitAttempted) userInputError = null
            },
            label = { Text("Email") }, // or Phone Number
            singleLine = true,
            isError = userInputError != null,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Email
            ),
            modifier = Modifier.fillMaxWidth()
        )
        userInputError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (submitAttempted) passwordError = null
            },
            label = { Text("Password") },
            singleLine = true,
            isError = passwordError != null,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = icon, contentDescription = null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        passwordError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                submitAttempted = true
                if (validateInputs()) {
                    loading = true
                    val input = userInput.trim().replace("\\s".toRegex(), "")
                    if (isValidPhone(input)) {
                        val formattedPhone = formatPhoneNumber(input)
                        navigateToPhoneOtpScreen(formattedPhone)
                        loading = false // Reset loading here or after navigation finishes
                    } else {
                        onLoginClick(input, password)
                        loading = false // Reset loading here or after login finishes
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading && userInput.isNotBlank() && password.isNotBlank()
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onCreateAccountClick) {
                Text("Create Account")
            }
            TextButton(onClick = { onForgotPasswordClick(userInput.trim()) }) {
                Text("Forgot Password?")
            }
        }
    }
}








//package com.chat.safeplay
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.text.input.PasswordVisualTransformation
//import androidx.compose.ui.text.input.VisualTransformation
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.Alignment
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Visibility
//import androidx.compose.material.icons.filled.VisibilityOff
//import androidx.core.util.PatternsCompat
//
//@Composable
//fun LoginScreen(
//    onLoginClick: (String, String) -> Unit,
//    onCreateAccountClick: () -> Unit,
//    onForgotPasswordClick: (String) -> Unit,
//    navigateToPhoneOtpScreen: (String) -> Unit
//)
// {
//    var userInput by remember { mutableStateOf("") }  // Email or Phone
//    var password by remember { mutableStateOf("") }
//    var passwordVisible by remember { mutableStateOf(false) }
//
//    var submitAttempted by remember { mutableStateOf(false) }
//    var userInputError by remember { mutableStateOf<String?>(null) }
//    var passwordError by remember { mutableStateOf<String?>(null) }
//
//    fun isValidPhone(input: String): Boolean {
//        val cleaned = input.replace("\\s".toRegex(), "")
//        return cleaned.matches(Regex("^[+]?[0-9]{10,13}$"))
//    }
//
//    fun formatPhoneNumber(input: String): String {
//        val cleaned = input.replace("\\s".toRegex(), "")
//        return if (cleaned.startsWith("+")) cleaned else "+91$cleaned"
//    }
//
//    fun validateInputs(): Boolean {
//        var valid = true
//        val trimmedInput = userInput.trim()
//        userInputError = null
//
//        if (trimmedInput.isEmpty()) {
//            userInputError = "Email or phone required"
//            valid = false
//        } else if (!isValidPhone(trimmedInput) && !PatternsCompat.EMAIL_ADDRESS.matcher(trimmedInput).matches()) {
//            userInputError = "Invalid email or phone"
//            valid = false
//        }
//
//        passwordError = if (password.isBlank()) {
//            valid = false
//            "Password required"
//        } else null
//
//        return valid
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(24.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text("Welcome to SafePlay", style = MaterialTheme.typography.headlineMedium)
//        Spacer(modifier = Modifier.height(24.dp))
//
//        OutlinedTextField(
//            value = userInput,
//            onValueChange = {
//                userInput = it
//                if (submitAttempted) userInputError = null
//            },
//            label = { Text("Email") },// or Phone Number
//            singleLine = true,
//            isError = userInputError != null,
//            keyboardOptions = KeyboardOptions.Default.copy(
//                keyboardType = KeyboardType.Email
//            ),
//            modifier = Modifier.fillMaxWidth()
//        )
//        userInputError?.let {
//            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        OutlinedTextField(
//            value = password,
//            onValueChange = {
//                password = it
//                if (submitAttempted) passwordError = null
//            },
//            label = { Text("Password") },
//            singleLine = true,
//            isError = passwordError != null,
//            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
//            trailingIcon = {
//                val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
//                IconButton(onClick = { passwordVisible = !passwordVisible }) {
//                    Icon(imageVector = icon, contentDescription = null)
//                }
//            },
//            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
//            modifier = Modifier.fillMaxWidth()
//        )
//        passwordError?.let {
//            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
//        }
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        Button(
//            onClick = {
//                submitAttempted = true
//                if (validateInputs()) {
//                    val input = userInput.trim().replace("\\s".toRegex(), "")
//                    if (isValidPhone(input)) {
//                        val formattedPhone = formatPhoneNumber(input)
//                        navigateToPhoneOtpScreen(formattedPhone)
//                    } else {
//                        onLoginClick(input, password)
//                    }
//                }
//            },
//            modifier = Modifier.fillMaxWidth(),
//            enabled = userInput.isNotBlank() && password.isNotBlank()
//        ) {
//            Text("Login")
//        }
//
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
//            TextButton(onClick = onCreateAccountClick) {
//                Text("Create Account")
//            }
//            TextButton(onClick = { onForgotPasswordClick(userInput.trim()) }) {
//                Text("Forgot Password?")
//            }
//        }
//    }
//}