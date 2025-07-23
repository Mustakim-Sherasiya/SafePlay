package com.chat.safeplay



import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun GameSelectionScreen(navController: NavHostController) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎮 Select a Game", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            context.startActivity(Intent(context, TapTheDotActivity::class.java))
        }) {
            Text("🎯 Tap the Dot")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            context.startActivity(Intent(context, ColorMemoryActivity::class.java))
        }) {
            Text("🔴 Color Memory")
        }
    }
}
