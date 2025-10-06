package com.chat.safeplay.ui.theme



import com.chat.safeplay.R

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun LaunchVideoOverlay() {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.safeplay_intro}")
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            volume = 0f
        }
    }

    LaunchedEffect(Unit) {
        delay(4000)
        visible = false
        exoPlayer.release()
    }

    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black), // 👈 visible background
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = {
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = true
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            )

            // 👇 Add a test text to confirm overlay is visible
            Text(
                text = "Overlay Active",
                fontSize = 22.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
