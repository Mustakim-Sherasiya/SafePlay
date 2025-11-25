package com.chat.safeplay

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.ui.graphics.Color
import com.chat.safeplay.LocalStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore



@Composable
fun HomeScreen(navController: NavHostController) {
    val isDark = isSystemInDarkTheme() // detect current system theme
    var devMessageVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val appContext = context.applicationContext


    // Background and text color adapt automatically
    val backgroundColor = if (isDark) Color.Black else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val pressTime = System.currentTimeMillis()
                        try {
                            awaitRelease()
                        } catch (_: Exception) {}

                        val duration = System.currentTimeMillis() - pressTime
                        if (duration >= 2000) {
                            if (!LocalStorage.isLoggedIn(appContext)) {
                                FirebaseAuth.getInstance().signOut()
                            }

                            // Vibrate after 3 seconds
                            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val vibratorManager =
                                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                vibratorManager.defaultVibrator
                            } else {
                                @Suppress("DEPRECATION")
                                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(
                                    VibrationEffect.createOneShot(
                                        80,
                                        VibrationEffect.DEFAULT_AMPLITUDE
                                    )
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(80)
                            }

                            devMessageVisible = false  // ✅ keep this

                            val auth = FirebaseAuth.getInstance()
                            val firestore = FirebaseFirestore.getInstance()

                            if (LocalStorage.isLoggedIn(appContext)) {
                                val email = LocalStorage.getEmail(appContext)
                                val password = LocalStorage.getPassword(appContext)


                                if (auth.currentUser == null && email != null && password != null) {
                                    // 🔁 Re-login silently using saved credentials
                                    auth.signInWithEmailAndPassword(email, password)
                                        .addOnSuccessListener {
                                            val user = auth.currentUser
                                            if (user != null) {
                                                firestore.collection("users").document(user.uid).get()
                                                    .addOnSuccessListener { doc ->
                                                        val hasPin = doc.getString("pin") != null
                                                        if (hasPin) {
                                                            navController.navigate("enterPin")
                                                        } else {
                                                            navController.navigate("createPin")
                                                        }
                                                    }
                                                    .addOnFailureListener {
                                                        navController.navigate("enterPin") // fallback
                                                    }
                                            } else {
                                                navController.navigate("login")
                                            }
                                        }
                                        .addOnFailureListener {
                                            navController.navigate("login")
                                        }
                                } else {
                                    val user = auth.currentUser
                                    if (user != null) {
                                        firestore.collection("users").document(user.uid).get()
                                            .addOnSuccessListener { doc ->
                                                val hasPin = doc.getString("pin") != null
                                                if (hasPin) {
                                                    navController.navigate("enterPin")
                                                } else {
                                                    navController.navigate("createPin")
                                                }
                                            }
                                            .addOnFailureListener {
                                                navController.navigate("enterPin")
                                            }
                                    } else {
                                        navController.navigate("login")
                                    }
                                }
                            } else {
                                navController.navigate("login")
                            }

                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (devMessageVisible) {
                Text(
                    text = "🔧 App is still in development...",
                    color = textColor,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            Button(
                onClick = { navController.navigate("gameSelection") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color.DarkGray else Color.LightGray,
                    contentColor = textColor
                )
            ) {
                Text("🎮 Play a Game While You Wait")
            }
        }
    }
}



