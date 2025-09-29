package games

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chat.safeplay.ui.theme.SafePlayTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

class ColorMemoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafePlayTheme {
                ColorMemoryGameScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorMemoryGameScreen(onBack: () -> Unit) {
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow)
    var sequence by remember { mutableStateOf(listOf<Int>()) }
    var playerIndex by remember { mutableStateOf(0) }
    var showingSequence by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var highlightedIndex by remember { mutableStateOf(-1) }

    fun startNewRound() {
        playerIndex = 0
        showingSequence = true
        sequence = sequence + Random.nextInt(colors.size)
    }

    LaunchedEffect(showingSequence) {
        if (showingSequence) {
            for (index in sequence) {
                highlightedIndex = index
                delay(700)
                highlightedIndex = -1
                delay(300)
            }
            showingSequence = false
        }
    }

    if (!gameOver && sequence.isEmpty()) {
        LaunchedEffect(Unit) { startNewRound() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("Color Memory") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text("Score: $score", fontSize = 24.sp, color = Color.White)
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            colors.forEachIndexed { index, color ->
                val displayColor = if (highlightedIndex == index) color else color.copy(alpha = 0.4f)

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(displayColor)
                        .clickable(enabled = !showingSequence && !gameOver) {
                            if (sequence[playerIndex] == index) {
                                playerIndex++
                                if (playerIndex == sequence.size) {
                                    score++
                                    startNewRound()
                                }
                            } else {
                                gameOver = true
                            }
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        if (gameOver) {
            Text("Game Over! Final Score: $score", color = Color.Red, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                sequence = emptyList()
                playerIndex = 0
                score = 0
                gameOver = false
            }) {
                Text("Play Again")
            }
        }

        if (showingSequence) {
            Text("Watch the sequence...", color = Color.LightGray)
        }
    }
}
