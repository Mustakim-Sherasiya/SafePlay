package com.chat.safeplay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
    var devMessageVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val pressTime = System.currentTimeMillis()
                        try { awaitRelease() } catch (_: Exception) {}
                        val duration = System.currentTimeMillis() - pressTime
                        if (duration >= 3000) {
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
                    color = Color.White,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            Button(onClick = { navController.navigate("gameSelection") }) {
                Text("🎮 Play a Game While You Wait")
            }
        }
    }
}
