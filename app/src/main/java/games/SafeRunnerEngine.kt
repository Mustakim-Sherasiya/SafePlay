package com.chat.safeplay.games

import kotlin.random.Random

data class Obstacle(var x: Float, val lane: Int, val width: Float, val height: Float)

class SafeRunnerEngine(
    private val laneCount: Int,
    private val screenWidth: Float,
    private val screenHeight: Float
) {
    var playerLane = 1
    var playerY = 0f
    var isJumping = false
    var jumpVelocity = 0f

    var score = 0
    var gameOver = false

    private val obstaclesList = mutableListOf<Obstacle>()
    val obstacles: List<Obstacle> get() = obstaclesList

    private var spawnAccumulator = 0f
    private var timeElapsed = 0f
    private val baseSpeed = 600f

    fun update(dt: Float) {
        if (gameOver) return

        timeElapsed += dt

        // Move obstacles
        val speed = baseSpeed * (1f + timeElapsed / 10f)
        obstaclesList.forEach { it.x -= speed * dt }

        // Spawn new obstacles
        spawnAccumulator += dt
        val spawnInterval = maxOf(0.6f - timeElapsed / 60f, 0.28f)
        if (spawnAccumulator >= spawnInterval) {
            spawnAccumulator = 0f
            val lane = Random.nextInt(0, laneCount)
            val obsWidth = (screenWidth * 0.08f).coerceAtLeast(48f)
            val obsHeight = (screenHeight * 0.11f).coerceAtMost(200f)
            obstaclesList.add(Obstacle(screenWidth + obsWidth + Random.nextFloat() * 200f, lane, obsWidth, obsHeight))
        }

        // Jump physics
        if (isJumping) {
            val gravity = 3000f
            jumpVelocity += gravity * dt
            playerY += jumpVelocity * dt
            if (playerY >= 0f) {
                playerY = 0f
                isJumping = false
                jumpVelocity = 0f
            }
        }

        // Remove offscreen obstacles and increase score
        val iterator = obstaclesList.iterator()
        while (iterator.hasNext()) {
            val obs = iterator.next()
            if (obs.x + obs.width < 0) {
                iterator.remove()
                score += 10
            }
        }
    }

    fun jump() {
        if (!isJumping) {
            isJumping = true
            jumpVelocity = -1200f
        }
    }

    fun moveLeft() {
        playerLane = maxOf(0, playerLane - 1)
    }

    fun moveRight() {
        playerLane = minOf(laneCount - 1, playerLane + 1)
    }

    fun reset() {
        playerLane = 1
        playerY = 0f
        isJumping = false
        jumpVelocity = 0f
        score = 0
        gameOver = false
        obstaclesList.clear()
        spawnAccumulator = 0f
        timeElapsed = 0f
    }
}
