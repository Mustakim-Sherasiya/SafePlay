package com.chat.safeplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.chat.safeplay.ui.theme.SafePlayTheme
import kotlin.random.Random

class TapTheDotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafePlayTheme {
                TapTheDotGameScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapTheDotGameScreen(onBack: () -> Unit) {
    var score by remember { mutableStateOf(0) }

    val configuration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

    val scoreAreaHeight = with(LocalDensity.current) { 100.dp.toPx() }
    val bottomLineHeightPx = with(LocalDensity.current) { 40.dp.toPx() }  // ~1.5 cm from bottom

    val dotRadius = 50f  // Dot radius

    var dotPosition by remember {
        mutableStateOf(randomOffsetRestricted(screenWidthPx, screenHeightPx, scoreAreaHeight, bottomLineHeightPx))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        TopAppBar(
            title = { Text("Tap The Dot") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val distance = (tapOffset - dotPosition).getDistance()
                        if (distance <= dotRadius * 2) {
                            score++
                            dotPosition = randomOffsetRestricted(screenWidthPx, screenHeightPx, scoreAreaHeight, bottomLineHeightPx)
                        }
                    }
                }
        ) {
            // Draw the horizontal line near bottom
            Canvas(modifier = Modifier.fillMaxSize()) {
                // White horizontal line across screen at bottomLineHeightPx from bottom
                val yLine = size.height - bottomLineHeightPx
                drawLine(
                    color = Color.White,
                    start = Offset(0f, yLine),
                    end = Offset(size.width, yLine),
                    strokeWidth = 4f
                )

                // Draw the red dot above the line
                drawCircle(
                    color = Color.Red,
                    radius = dotRadius,
                    center = dotPosition
                )
            }

            Text(
                text = "Score: $score",
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
            )
        }
    }
}

// Random dot position but always above the white line and below score area
fun randomOffsetRestricted(
    screenWidth: Float,
    screenHeight: Float,
    topReservedHeight: Float,
    bottomLineHeightPx: Float
): Offset {
    val padding = 20f

    val usableHeight = screenHeight - topReservedHeight - bottomLineHeightPx - padding * 2
    val usableWidth = screenWidth - padding * 2

    val x = Random.nextFloat() * usableWidth + padding
    val y = topReservedHeight + Random.nextFloat() * usableHeight + padding

    return Offset(x, y)
}
