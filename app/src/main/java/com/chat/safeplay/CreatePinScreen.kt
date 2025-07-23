package com.chat.safeplay

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CreatePinScreen(
    navController: NavController,
    onPinCreated: ((pin: String, pinLength: Int, autoSubmit: Boolean) -> Unit)? = null, // Optional callback
    userIdForPinReset: String? = null // Optional param for PIN reset mode
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    var pinLength by remember { mutableStateOf(4) }
    var autoSubmit by remember { mutableStateOf(true) }
    var pin by remember { mutableStateOf("") }
    var showSubmitButton by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Your PIN", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        // PIN length selector using a dropdown menu
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PIN Length:")
            Spacer(Modifier.width(8.dp))
            Box {
                OutlinedTextField(
                    value = pinLength.toString(),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .width(80.dp)
                        .clickable { dropdownExpanded = true },
                    label = { Text("Length") },
                    trailingIcon = {
                        Icon(
                            imageVector = if (dropdownExpanded)
                                Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown icon"
                        )
                    }
                )
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    listOf(4, 6).forEach { length ->
                        DropdownMenuItem(
                            text = { Text(length.toString()) },
                            onClick = {
                                pinLength = length
                                dropdownExpanded = false
                                // Clear PIN input on length change:
                                pin = ""
                                showSubmitButton = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Auto submit toggle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Auto Submit:")
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = autoSubmit,
                onCheckedChange = {
                    autoSubmit = it
                    showSubmitButton = !autoSubmit
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        // PIN input field
        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= pinLength && it.all { c -> c.isDigit() }) {
                    pin = it
                    if (autoSubmit && pin.length == pinLength) {
                        savePinAndNavigate(
                            pin,
                            pinLength,
                            autoSubmit,
                            userIdForPinReset ?: user?.uid,
                            firestore,
                            context,
                            navController,
                            userIdForPinReset != null // true if resetting PIN (save to Firestore but don't navigate to userDashboard)
                        )
                        onPinCreated?.invoke(pin, pinLength, autoSubmit)
                    }
                    if (!autoSubmit) {
                        showSubmitButton = pin.length == pinLength
                    }
                }
            },
            label = { Text("Enter PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        if (showSubmitButton) {
            Button(onClick = {
                savePinAndNavigate(
                    pin,
                    pinLength,
                    autoSubmit,
                    userIdForPinReset ?: user?.uid,
                    firestore,
                    context,
                    navController,
                    userIdForPinReset != null
                )
                onPinCreated?.invoke(pin, pinLength, autoSubmit)
            }) {
                Text("Submit PIN")
            }
        }
    }
}

private fun savePinAndNavigate(
    pin: String,
    pinLength: Int,
    autoSubmit: Boolean,
    userId: String?,
    firestore: FirebaseFirestore,
    context: android.content.Context,
    navController: NavController,
    isPinReset: Boolean = false
) {
    if (userId == null) {
        Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
        return
    }
    val userDocRef = firestore.collection("users").document(userId)

    val data = mapOf(
        "pin" to pin,
        "pinLength" to pinLength,
        "autoSubmit" to autoSubmit
    )

    userDocRef.set(data)
        .addOnSuccessListener {
            // Save PIN locally too
            PinStorageHelper.savePin(context, pin, pinLength, autoSubmit)

            Toast.makeText(
                context,
                if (isPinReset) "PIN reset successfully" else "PIN saved successfully",
                Toast.LENGTH_SHORT
            ).show()
            if (!isPinReset) {
                // Only navigate to userDashboard if this is normal flow (not PIN reset)
                navController.navigate("userDashboard") {
                    popUpTo("enterPin") { inclusive = true }
                }
            } else {
                // For PIN reset, just navigate back (e.g. to login or PIN reset menu)
                navController.popBackStack()
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(
                context,
                "Failed to save PIN: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
}