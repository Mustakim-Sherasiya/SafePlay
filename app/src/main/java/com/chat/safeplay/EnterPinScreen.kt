package com.chat.safeplay

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.firebase.firestore.SetOptions
import androidx.compose.ui.text.TextStyle
import com.google.firebase.firestore.BuildConfig


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

    var storedPin by rememberSaveable { mutableStateOf<String?>(null) }
    var pin by rememberSaveable { mutableStateOf("") }
    var showSubmitButton by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(true) }

    // 🔹 Biometric variables
    var biometricAttempted by remember { mutableStateOf(false) }
    var biometricFailedCount by remember { mutableStateOf(0) }
    var isBiometricEnabled by rememberSaveable { mutableStateOf(false) }
    var canReenableBiometric by rememberSaveable { mutableStateOf(false) }







//    var storedPin by remember { mutableStateOf<String?>(null) }
//    var pin by remember { mutableStateOf("") }
//    var showSubmitButton by remember { mutableStateOf(false) }
//    var isLoading by remember { mutableStateOf(true) }

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
                    // 🔹 Fetch biometricEnabled flag from Firestore (if exists)
                    val biometricFlag = document.getBoolean("biometricEnabled") ?: false
                    isBiometricEnabled = biometricFlag

                  // Also store locally for next login
                    val prefs = context.getSharedPreferences("safeplay_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("biometric_enabled", biometricFlag).apply()



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

    // 🔹 Show biometric prompt once after PIN is loaded
    Log.d("BIO_DEBUG", "Context type: ${context::class.java.simpleName}")

    // 🔹 Show biometric prompt once PIN is fetched and setting is ready
    LaunchedEffect(storedPin, isLoading) {
        if (storedPin != null && !isLoading && !biometricAttempted) {
            biometricAttempted = true
            Toast.makeText(context, "Checking biometric availability...", Toast.LENGTH_SHORT).show()

            val prefs = context.getSharedPreferences("safeplay_prefs", Context.MODE_PRIVATE)
            val isBiometricEnabled = prefs.getBoolean("biometric_enabled", false)
            Log.d("BIO_DEBUG", "BiometricEnabled (local prefs): $isBiometricEnabled")

            // ✅ Get a valid activity reference for BiometricPrompt
            // ✅ Get a valid FragmentActivity (Compose-safe)
            val activity = findFragmentActivity(context)
            if (activity == null) {
                Log.e("BIO_DEBUG", "❌ No FragmentActivity found for biometric prompt")
                Toast.makeText(context, "Biometric not supported on this screen", Toast.LENGTH_SHORT).show()
                return@LaunchedEffect
            }



            val biometricManager = BiometricManager.from(context)

            val canAuthenticate = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                biometricManager.canAuthenticate()
            }


            Log.d("BIO_DEBUG", "CanAuthenticate code: $canAuthenticate")

                  // ✅ Check if user can re-enable biometrics

            canReenableBiometric = (
                    canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS &&
                            !isBiometricEnabled
                    )



            // 🧠 Auto-disable biometric if user removed lock screen or fingerprints
            if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                Log.w("BIO_DEBUG", "Biometric disabled by system (code=$canAuthenticate). Turning off in SafePlay...")

                // 1️⃣ Turn off locally
                val prefs = context.getSharedPreferences("safeplay_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("biometric_enabled", false).apply()

                // 2️⃣ Turn off in Firestore
                user?.uid?.let { uid ->
                    firestore.collection("users").document(uid)
                        .set(mapOf("biometricEnabled" to false), SetOptions.merge())
                }

                // 3️⃣ Inform user once
                Toast.makeText(
                    context,
                    "Your device's screen lock or biometrics were removed. Biometric login disabled for safety.",
                    Toast.LENGTH_LONG
                ).show()

                // Stop further biometric prompt attempts
                return@LaunchedEffect
            }





            if (isBiometricEnabled) {
                try {
                    if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                        val executor = ContextCompat.getMainExecutor(context)

                        val biometricPrompt = BiometricPrompt(activity, executor,
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    super.onAuthenticationSucceeded(result)
                                    Toast.makeText(context, "Biometric success!", Toast.LENGTH_SHORT).show()
                                    onPinVerified()
                                    navController.navigate("UserDashboard") {
                                        popUpTo("enterPin") { inclusive = true }
                                    }
                                }

                                override fun onAuthenticationFailed() {
                                    super.onAuthenticationFailed()
                                    biometricFailedCount++
                                    if (biometricFailedCount >= 3) {
                                        Toast.makeText(
                                            context,
                                            "Biometric failed 3 times. Please enter your PIN.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                    super.onAuthenticationError(errorCode, errString)
                                    Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                                }
                            })

                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Unlock SafePlay")
                            .setSubtitle("Authenticate with fingerprint, face, or device PIN")
                            .setAllowedAuthenticators(
                                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                            )
                            .build()

                        Toast.makeText(context, "Launching biometric prompt...", Toast.LENGTH_SHORT).show()
                        biometricPrompt.authenticate(promptInfo)
                    } else {
                        // Fallback if biometrics not available or enrolled
                        Log.w("BIO_DEBUG", "Biometrics not available or not enrolled. code=$canAuthenticate")
                        Toast.makeText(context, "Biometric not available on this device. Use PIN.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("BIO_DEBUG", "❌ Biometric crash: ${e.message}", e)
                    Toast.makeText(context, "Biometric unavailable, please enter PIN.", Toast.LENGTH_LONG).show()
                }
            }
            else {
                Toast.makeText(
                    context,
                    "Biometric not available or disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }




//    LaunchedEffect(isLoading) {
//        if (!isLoading && !biometricAttempted) {
//            biometricAttempted = true
//            val activity = context as? FragmentActivity ?: return@LaunchedEffect
//
//            val biometricManager = BiometricManager.from(context)
//            val canAuthenticate = biometricManager.canAuthenticate(
//                BiometricManager.Authenticators.BIOMETRIC_STRONG or
//                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
//            )
//
//
//            // 👉 You can later replace this with a real setting from SharedPreferences
//            val prefs = context.getSharedPreferences("safeplay_prefs", Context.MODE_PRIVATE)
//            val isBiometricEnabled = prefs.getBoolean("biometric_enabled", false)
//
//            Log.d("BIO_DEBUG", "BiometricEnabled = $isBiometricEnabled")
//
//
//            if (isBiometricEnabled && canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
//                val executor = ContextCompat.getMainExecutor(context)
//
//                val biometricPrompt = BiometricPrompt(activity, executor,
//                    object : BiometricPrompt.AuthenticationCallback() {
//                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
//                            super.onAuthenticationSucceeded(result)
//                            Toast.makeText(context, "Biometric success!", Toast.LENGTH_SHORT).show()
//                            onPinVerified()
//                            navController.navigate("UserDashboard") {
//                                popUpTo("enterPin") { inclusive = true }
//                            }
//                        }
//
//                        override fun onAuthenticationFailed() {
//                            super.onAuthenticationFailed()
//                            biometricFailedCount++
//                            if (biometricFailedCount >= 3) {
//                                Toast.makeText(
//                                    context,
//                                    "Biometric failed 3 times. Please enter your PIN.",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            }
//                        }
//
//                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
//                            super.onAuthenticationError(errorCode, errString)
//                            Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
//                        }
//                    })
//
//                val promptInfo = BiometricPrompt.PromptInfo.Builder()
//                    .setTitle("Unlock SafePlay")
//                    .setSubtitle("Authenticate with fingerprint, face, or device PIN")
//                    .setAllowedAuthenticators(
//                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
//                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
//                    )
//                    .build()
//
//                biometricPrompt.authenticate(promptInfo)
//            }
//        }
//    }





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

//        if (storedPin != null) {
//            TextButton(onClick = {
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


        if (storedPin != null) {
            Spacer(modifier = Modifier.height(24.dp))

            // 🔹 Support contact email (includes user info and current PIN)
            ClickableText(
                text = AnnotatedString("Contact Support: safeplay@spysolution.in"),
                style = TextStyle(
                    color = Color(0xFF3B82F6),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                onClick = {
                    try {
                        val prefs = context.getSharedPreferences("safeplay_prefs", Context.MODE_PRIVATE)
                        val userUid = user?.uid ?: "Unknown"
                        val userEmail = user?.email ?: "Unknown"
                        val userPublicId = prefs.getString("public_id", "Unknown") ?: "Unknown"
                        val userPhone = prefs.getString("phone", "Unknown") ?: "Unknown"
                        val userPin = storedPin ?: "Not available"

                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "message/rfc822"
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("safeplay@spysolution.in"))
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "Help with PIN Login - SafePlay App"
                            )
                            putExtra(
                                Intent.EXTRA_TEXT,
                                """
                        Hello SafePlay Support,

                        I am facing an issue while entering my PIN. Please assist me with the following details:

                        (Describe your issue here or send mail as is.)

                        -------------------------
                        🧾 User Info:

                        UID: $userUid
                        Email: $userEmail
                        Public ID: $userPublicId
                        Phone: $userPhone
                        Current PIN: $userPin
                        -------------------------

                        Thank you,
                        SafePlay User
                        """.trimIndent()
                            )
                        }

                        context.startActivity(Intent.createChooser(intent, "Send email via..."))
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }





        // 🧩 Re-enable biometrics button (only if available again)
        // 🧩 Animated "Re-enable biometrics" button (fades in smoothly)
        AnimatedVisibility(
            visible = canReenableBiometric,
            enter = fadeIn(animationSpec = tween(durationMillis = 600)),
            exit = fadeOut(animationSpec = tween(durationMillis = 400))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val prefs = context.getSharedPreferences("safeplay_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("biometric_enabled", true).apply()

                        user?.uid?.let { uid ->
                            firestore.collection("users").document(uid)
                                .set(mapOf("biometricEnabled" to true), SetOptions.merge())
                        }

                        Toast.makeText(context, "Biometric login re-enabled!", Toast.LENGTH_SHORT).show()
                        canReenableBiometric = false // Hide the button after enabling
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Re-enable Biometric Login")
                }
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

private fun findFragmentActivity(context: Context): androidx.fragment.app.FragmentActivity? {
    var currentContext = context
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is androidx.fragment.app.FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
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
