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
@Composable
fun HomeScreen(navController: NavHostController) {
    val isDark = isSystemInDarkTheme() // detect current system theme
    var devMessageVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current

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
                        if (duration >= 3000) {
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

                            devMessageVisible = false
                            navController.navigate("login")
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









//package com.chat.safeplay
//
//import android.content.Context
//import android.os.Build
//import android.os.VibrationEffect
//import android.os.Vibrator
//import android.os.VibratorManager
//import androidx.compose.foundation.background
//import androidx.compose.foundation.gestures.detectTapGestures
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavHostController
//import androidx.compose.ui.graphics.Color
//@Composable
//fun HomeScreen(navController: NavHostController) {
//    var devMessageVisible by remember { mutableStateOf(true) }
//    val context = LocalContext.current
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color.Black)
//            .pointerInput(Unit) {
//                detectTapGestures(
//                    onPress = {
//                        val pressTime = System.currentTimeMillis()
//                        try {
//                            // Wait for release or cancel
//                            awaitRelease()
//                        } catch (_: Exception) {}
//
//                        val duration = System.currentTimeMillis() - pressTime
//
//                        if (duration >= 3000) {
//                            // Vibrate when 3 seconds are over
//                            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                                val vibratorManager =
//                                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
//                                vibratorManager.defaultVibrator
//                            } else {
//                                @Suppress("DEPRECATION")
//                                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//                            }
//
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                                vibrator.vibrate(
//                                    VibrationEffect.createOneShot(
//                                        80, // vibration duration in ms
//                                        VibrationEffect.DEFAULT_AMPLITUDE
//                                    )
//                                )
//                            } else {
//                                @Suppress("DEPRECATION")
//                                vibrator.vibrate(80)
//                            }
//
//                            // Navigate after vibration
//                            devMessageVisible = false
//                            navController.navigate("login")
//                        }
//                    }
//                )
//            },
//        contentAlignment = Alignment.Center
//    ) {
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            if (devMessageVisible) {
//                Text(
//                    text = "🔧 App is still in development...",
//                    color = Color.White,
//                    fontSize = 20.sp
//                )
//                Spacer(modifier = Modifier.height(24.dp))
//            }
//            Button(onClick = { navController.navigate("gameSelection") }) {
//                Text("🎮 Play a Game While You Wait")
//            }
//        }
//    }
//}
//
//
//
//
//
//
//
//
