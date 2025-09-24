    package com.chat.safeplay

    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.compose.material3.Text
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.foundation.background
    import com.chat.safeplay.ui.theme.SafePlayTheme

    class GameActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                SafePlayTheme {
                    // Simple placeholder for game page
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray)
                    ) {
                        Text(text = "Game Screen - Coming Soon!", color = Color.White)
                    }
                }
            }
        }
    }
