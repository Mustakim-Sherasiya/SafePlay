package games

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.chat.safeplay.ui.theme.SafePlayTheme

class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafePlayTheme {
                // Simple placeholder for game page
                Box(
                    modifier = Modifier.Companion
                        .fillMaxSize()
                        .background(Color.Companion.DarkGray)
                ) {
                    Text(text = "Game Screen - Coming Soon!", color = Color.Companion.White)
                }
            }
        }
    }
}