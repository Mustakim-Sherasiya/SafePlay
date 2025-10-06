package com.chat.safeplay

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import com.google.firebase.firestore.Source

@Composable
fun EnterPinScreen(
    navController: NavController,
    correctPin: String,
    pinLength: Int,
    autoSubmit: Boolean,
    onPinVerified: () -> Unit,
    onForgotPinClick: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    var storedPin by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var showSubmitButton by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch PIN from Firestore SERVER (force fresh read) and normalize it
    LaunchedEffect(user?.uid) {
        if (user?.uid != null) {
            val docRef = firestore.collection("users").document(user.uid)
            // Force server so we don't get a stale cached value
            docRef.get(Source.SERVER)
                .addOnSuccessListener { document ->
                    // Robust reading: handle string or numeric stored pin
                    val fetchedPin = document.getString("pin")
                        ?: document.getLong("pin")?.toString()
                        ?: document.get("pin")?.toString()

                    if (fetchedPin != null) {
                        storedPin = fetchedPin.trim()
                        Log.d("PIN_DEBUG", "Fetched PIN (server) length=${storedPin?.length}")
                    } else {
                        Log.d("PIN_DEBUG", "Fetched PIN is null or missing for user ${user.uid}")
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    Log.d("PIN_DEBUG", "Failed to fetch PIN from server: ${e.message}")
                    Toast.makeText(context, "Failed to load PIN data.", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
        } else {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter Your PIN", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Allow up to 6 digits (UI change only)
        OutlinedTextField(
            value = pin,
            onValueChange = {

                if (it.length <= pinLength && it.all { c -> c.isDigit() }) {
                    pin = it
                    // If autoSubmit is enabled, trigger verify once user typed the same length as stored PIN
                    if (autoSubmit && storedPin != null && pin.length == storedPin!!.length) {
                        verifyPin(pin, storedPin, navController, context, onPinVerified)
                    }
                    // If not autoSubmit, show submit button whenever user typed something
                    if (!autoSubmit) {
                        showSubmitButton = pin.isNotEmpty()
                    }
                }
            },
            label = { Text("PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        if (showSubmitButton) {
            Button(onClick = {
                verifyPin(pin, storedPin, navController, context, onPinVerified)
            }) {
                Text("Submit PIN")
            }
        }

        Spacer(Modifier.height(20.dp))

        if (storedPin != null) {
            TextButton(onClick = {
                onForgotPinClick()
                Toast.makeText(
                    context,
                    "Please contact support to reset your PIN.",
                    Toast.LENGTH_LONG
                ).show()
            }) {
                Text("Forgot PIN?")
            }
        }
    }
}

private fun verifyPin(
    enteredPin: String,
    storedPin: String?,
    navController: NavController,
    context: android.content.Context,
    onPinVerified: () -> Unit
) {
    if (storedPin == null) {
        Toast.makeText(context, "No PIN set. Please create one first.", Toast.LENGTH_SHORT).show()
        return
    }

    val normalizedEntered = enteredPin.trim()
    val normalizedStored = storedPin.trim()

    // Log for debugging (remove later)
    Log.d("PIN_DEBUG", "verifyPin -> entered='${normalizedEntered}' (${normalizedEntered.length}), stored='${normalizedStored}' (${normalizedStored.length})")

    if (normalizedEntered == normalizedStored) {
        Toast.makeText(context, "PIN correct! Logging in...", Toast.LENGTH_SHORT).show()
        onPinVerified()
        navController.navigate("UserDashboard") {
            popUpTo("enterPin") { inclusive = true }
        }
    } else {
        Toast.makeText(context, "Incorrect PIN. Try again.", Toast.LENGTH_SHORT).show()
    }
}






//package com.chat.safeplay
//
//import android.widget.Toast
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//
//@Composable
//fun EnterPinScreen(
//    navController: NavController,
//    correctPin: String,
//    pinLength: Int,
//    autoSubmit: Boolean,
//    onPinVerified: () -> Unit,
//    onForgotPinClick: () -> Unit
//) {
//    val context = LocalContext.current
//    val auth = FirebaseAuth.getInstance()
//    val firestore = FirebaseFirestore.getInstance()
//    val user = auth.currentUser
//
//    var storedPin by remember { mutableStateOf<String?>(null) }
//    var pin by remember { mutableStateOf("") }
//    var showSubmitButton by remember { mutableStateOf(false) }
//    var isLoading by remember { mutableStateOf(true) }
//
//    // Fetch PIN and autoSubmit from Firestore
//    LaunchedEffect(user?.uid) {
//        if (user?.uid != null) {
//            firestore.collection("users").document(user.uid).get()
//                .addOnSuccessListener { document ->
//                    val fetchedPin = document.getString("pin")
//                    if (fetchedPin != null) {
//                        storedPin = fetchedPin
//                    }
//                    isLoading = false
//                }
//                .addOnFailureListener {
//                    Toast.makeText(context, "Failed to load PIN data.", Toast.LENGTH_SHORT).show()
//                    isLoading = false
//                }
//        } else {
//            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
//            isLoading = false
//        }
//    }
//
//    if (isLoading) {
//        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//            CircularProgressIndicator()
//        }
//        return
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(24.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text("Enter Your PIN", style = MaterialTheme.typography.headlineMedium)
//        Spacer(Modifier.height(16.dp))
//
//        OutlinedTextField(
//            value = pin,
//            onValueChange = {
//
//                if (it.length <= pinLength && it.all { c -> c.isDigit() }) {
//
//
//
////                if (it.length <= 6 && it.all { c -> c.isDigit() }) {
//
//                    pin = it
//                    if (autoSubmit && pin.length == pinLength) {
//                        verifyPin(pin, storedPin, navController, context, onPinVerified)
//                    }
//                    if (!autoSubmit) {
//                        showSubmitButton = pin.length == pinLength
//                    }
//                }
//            },
//            label = { Text("PIN") },
//            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
//            singleLine = true,
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(Modifier.height(16.dp))
//
//        if (showSubmitButton) {
//            Button(onClick = {
//                verifyPin(pin, storedPin, navController, context, onPinVerified)
//            }) {
//                Text("Submit PIN")
//            }
//        }
//
//        Spacer(Modifier.height(20.dp))
//
//        if (storedPin != null) {
//            TextButton(onClick = {
//                // Call provided handler instead of password reset
//                onForgotPinClick()
//                Toast.makeText(
//                    context,
//                    "Please contact support to reset your PIN.",
//                    Toast.LENGTH_LONG
//                ).show()
//            }) {
//                Text("Forgot PIN?")
//            }
//        }
//    }
//}
//
//private fun verifyPin(
//    enteredPin: String,
//    storedPin: String?,
//    navController: NavController,
//    context: android.content.Context,
//    onPinVerified: () -> Unit
//) {
//    if (storedPin == null) {
//        Toast.makeText(context, "No PIN set. Please create one first.", Toast.LENGTH_SHORT).show()
//        return
//    }
//
//    if (enteredPin == storedPin) {
//        Toast.makeText(context, "PIN correct! Logging in...", Toast.LENGTH_SHORT).show()
//        onPinVerified()
//        navController.navigate("UserDashboard"){
//            popUpTo("enterPin") { inclusive = true }
//        }
//    } else {
//        Toast.makeText(context, "Incorrect PIN. Try again.", Toast.LENGTH_SHORT).show()
//    }
//}
