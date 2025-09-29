package com.chat.safeplay.games

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

class SafeRunnerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameView = SafeRunnerSurfaceView(this)

        setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(factory = { gameView }, modifier = Modifier.fillMaxSize())
                    OverlayUI(gameView = gameView, onExit = { finish() })
                }
            }
        }
    }
}

@Composable
fun OverlayUI(gameView: SafeRunnerSurfaceView, onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onExit() }) { Text("Back") }
            Text("Score: ${gameView.score}", fontSize = 20.sp)
        }

        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            if (!gameView.running) {
                Button(onClick = { gameView.startGame() }) { Text("Start / Play Again") }
            }
        }
    }
}

class SafeRunnerSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
    private var thread: Thread? = null
    @Volatile var running = false

    private val paint = Paint()
    private var playerY = 0f
    private var jumpVelocity = 0f
    private val gravity = 3000f
    private val playerRadius = 50f
    private var playerX = 0f

    private val obstacles = mutableListOf<Obstacle>()
    private var spawnAccumulator = 0f
    private var speed = 600f
    var score = 0

    init { holder.addCallback(this) }

    override fun surfaceCreated(holder: SurfaceHolder) { playerX = width / 2f; resetGame() }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) { running = false }

    fun startGame() {
        resetGame()
        running = true
        thread = Thread(this)
        thread?.start()
    }

    private fun resetGame() {
        playerY = 0f
        jumpVelocity = 0f
        obstacles.clear()
        score = 0
        spawnAccumulator = 0f
        speed = 600f
    }

    override fun run() {
        var lastTime = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dt = (now - lastTime) / 1_000_000_000f
            lastTime = now

            update(dt)
            drawGame()
        }
    }

    private fun update(dt: Float) {
        // Jump physics always updates
        jumpVelocity += gravity * dt
        playerY += jumpVelocity * dt
        if (playerY > 0f) { playerY = 0f; jumpVelocity = 0f }

        // Move obstacles
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obs = iterator.next()
            obs.x -= speed * dt
            if (obs.x + obs.width < 0) { iterator.remove(); score += 10 }
        }

        // Spawn obstacles
        spawnAccumulator += dt
        if (spawnAccumulator > 0.8f) {
            spawnAccumulator = 0f
            val obsWidth = 100f
            val obsHeight = 100f
            obstacles.add(Obstacle(width.toFloat(), height * 0.78f - obsHeight, obsWidth, obsHeight))
        }

        // Collision detection only ends the game
        obstacles.forEach { obs ->
            val playerTop = height * 0.78f - playerRadius + playerY
            val playerLeft = playerX - playerRadius
            val playerRight = playerX + playerRadius
            val obsLeft = obs.x
            val obsRight = obs.x + obs.width
            val obsTop = obs.y
            val obsBottom = obs.y + obs.height

            if (playerRight > obsLeft && playerLeft < obsRight && playerTop + playerRadius > obsTop) {
                running = false
            }
        }
    }

    private fun drawGame() {
        val canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.BLACK)
            paint.color = Color.DKGRAY
            canvas.drawRect(0f, height * 0.78f, width.toFloat(), height.toFloat(), paint)

            paint.color = Color.CYAN
            canvas.drawCircle(playerX, height * 0.78f - playerRadius + playerY, playerRadius, paint)

            paint.color = Color.RED
            obstacles.forEach { obs -> canvas.drawRect(obs.x, obs.y, obs.x + obs.width, obs.y + obs.height, paint) }
        } finally { holder.unlockCanvasAndPost(canvas) }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            jumpVelocity = -1200f
        }
        return true
    }

    data class Obstacle(var x: Float, var y: Float, val width: Float, val height: Float)
}