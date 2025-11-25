package com.chat.safeplay.chat.handler

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.TileMode
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.drawBehind
import kotlin.math.min
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi





@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun rainbowGlowBrush(): Brush {
    val colors = listOf(
        Color(0xFFFF00FF),  // magenta
        Color(0xFFFF8A80),  // coral
        Color(0xFFFFFF00),  // yellow
        Color(0xFF80FF00),  // lime
        Color(0xFF00FFFF),  // cyan
        Color(0xFFB388FF)   // purple
    )

    val transition = rememberInfiniteTransition(label = "rainbowGlow")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbowShift"
    )

    return Brush.linearGradient(
        colors = colors,
        start = Offset(0f, shift),
        end = Offset(shift, 0f),
        tileMode = TileMode.Mirror
    )
}














@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MessageBubble(
    message: ChatUiMessage,
    isMine: Boolean,
    showAvatar: Boolean,                // retained but per your request avatar always shown from ChatScreen
    avatarUrl: String?,
    selected: Boolean,
    highlight: Boolean = false,
    onLongPress: () -> Unit,
    onTap: () -> Unit,
    onRetrySend: (() -> Unit)? = null,   // called when FAILED retry button pressed
    onReactionClick: ((emoji: String) -> Unit)? = null,
    onOpenReactions: (() -> Unit)? = null
) {
    // UI tuning variables
    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val avatarSize = 40.dp
    val maxBubbleWidth = 320.dp
    val radius = 16.dp
    val timestampColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)




    // 🔥 One-shot ripple when message becomes starredByMe = true
    val rippleAnim = remember { Animatable(0f) }

    LaunchedEffect(message.id, message.starredByMe) {
        if (message.starredByMe) {
            // restart ripple 0 → 1
            rippleAnim.snapTo(0f)
            rippleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1350,
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            // when un-starred, no ripple, just reset
            rippleAnim.snapTo(0f)
        }
    }







    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Left avatar (other user)
        if (!isMine && showAvatar) {
            AvatarImage(avatarUrl, avatarSize)
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Bubble content
        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            // 🔹 SafePlay Advanced Glow System (Neon + Halo + Circling + Reflection + Haptics)
            val infiniteGlow = rememberInfiniteTransition(label = "pulseGlow")
            val isDark = isSystemInDarkTheme()
            val context = LocalContext.current

// ⚡ Pulse brightness
            val pulseAlpha: Float by infiniteGlow.animateFloat(
                initialValue = 0.5f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1700, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )

// 🌠 Circling motion
            val lightAngle: Float by infiniteGlow.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 6000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "lightAngle"
            )

// 💫 Halo expansion
            val haloExpansion by animateDpAsState(
                targetValue = if (selected || highlight) 36.dp else 0.dp,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                label = "haloExpansion"
            )

// 🎨 Neon color palette (auto-adaptive)
            val primaryGlow = if (isDark) Color(0xFF00FFFF) else Color(0xFFAA33FF)
            val secondaryGlow = if (isDark) Color(0xFF4FC3F7) else Color(0xFFFF66CC)
            val deepAccent = if (isDark) Color(0xFF0077FF) else Color(0xFFE040FB)

// 🌈 Outer aura
            val outerAura = Brush.radialGradient(
                colors = listOf(
                    primaryGlow.copy(alpha = pulseAlpha * 0.55f),
                    secondaryGlow.copy(alpha = pulseAlpha * 0.3f),
                    Color.Transparent
                ),
                center = Offset(
                    x = 300f * kotlin.math.cos(Math.toRadians(lightAngle.toDouble())).toFloat(),
                    y = 300f * kotlin.math.sin(Math.toRadians(lightAngle.toDouble())).toFloat()
                ),
                radius = 900f + haloExpansion.value
            )

// 💥 Inner intense core
            val innerAura = Brush.radialGradient(
                colors = listOf(
                    deepAccent.copy(alpha = pulseAlpha * 0.7f),
                    Color.Transparent
                ),
                center = Offset.Zero,
                radius = 400f + haloExpansion.value
            )

// 🔁 Circulating rim light
            val rimLight = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    primaryGlow.copy(alpha = 0.4f),
                    secondaryGlow.copy(alpha = 0.3f),
                    Color.Transparent
                )
            )

// ✨ Reflective surface overlay (bounces light onto text)
            val reflectiveOverlay = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.12f * pulseAlpha),
                    Color.Transparent
                )
            )

// 📳 Vibrate softly when first selected
            LaunchedEffect(selected) {
                if (selected) {
                    try {
                        val vibrator = context.getSystemService(android.os.Vibrator::class.java)
                        vibrator?.vibrate(
                            android.os.VibrationEffect.createOneShot(
                                35, // duration (ms)
                                android.os.VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } catch (_: Exception) {
                    }
                }
            }

// 💫 Glow + reflection box
            Box(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .combinedClickable(
                        onClick = { onTap() },
                        onLongClick = { onLongPress() }
                    )
                    // 🔊 Outward ripple glow when you star the message
                    .drawBehind {
                        val progress = rippleAnim.value
                        if (progress > 0f) {
                            val maxRadius = min(size.width, size.height) * 1.4f
                            val radiusPx = maxRadius * progress
                            val alpha = (1f - progress).coerceIn(0f, 1f)
                            val rippleColor = Color(0xFF00FFFF)

                            drawCircle(
                                color = rippleColor.copy(alpha = alpha * 0.6f),
                                radius = radiusPx,
                                center = center
                            )
                        }
                    }
                    // 🌈 Outer aura
                    .background(
                        brush = if (selected) outerAura else Brush.radialGradient(listOf(Color.Transparent, Color.Transparent)),
                        shape = RoundedCornerShape(radius)
                    )
                    // 💥 Inner glow
                    .background(
                        brush = if (selected) innerAura else Brush.radialGradient(listOf(Color.Transparent, Color.Transparent)),
                        shape = RoundedCornerShape(radius)
                    )

                    // 💫 Rim ring
                    .border(
                        width =
                            when {
                                selected || highlight -> 1.8.dp   // your normal selection glow
                                message.starredByMe -> 2.2.dp     // thicker rainbow glow
                                else -> 0.dp
                            },
                        brush =
                            when {
                                selected || highlight -> rimLight          // (existing cyan sweep)
                                message.starredByMe -> rainbowGlowBrush()  // 🌈 RAINBOW GLOW
                                else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            },
                        shape = RoundedCornerShape(radius)
                    )

                    // ✨ Reflective overlay that reacts to light pulse
                    .background(
                        brush = if (selected) reflectiveOverlay else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)),
                        shape = RoundedCornerShape(radius)
                    )
                    // 🌟 Halo expansion shadow
//                    .shadow(
//                        if (selected) 28.dp + haloExpansion else 0.dp,
//                        shape = RoundedCornerShape(radius),
//                        ambientColor = if (selected)
//                            primaryGlow.copy(alpha = 0.9f)
//                        else Color.Transparent,
//                        spotColor = if (selected)
//                            secondaryGlow.copy(alpha = 0.9f)
//                        else Color.Transparent
//                    )


                    .shadow(
                        when {
                            selected || highlight -> 28.dp + haloExpansion   // strong glow
                            message.starredByMe -> 18.dp                     // softer but visible for starred
                            else -> 0.dp
                        },
                        shape = RoundedCornerShape(radius),
                        ambientColor = when {
                            selected || highlight -> primaryGlow.copy(alpha = 0.9f)
                            message.starredByMe -> primaryGlow.copy(alpha = 0.45f)
                            else -> Color.Transparent
                        },
                        spotColor = when {
                            selected || highlight -> secondaryGlow.copy(alpha = 0.9f)
                            message.starredByMe -> secondaryGlow.copy(alpha = 0.45f)
                            else -> Color.Transparent
                        }
                    )




                    .animateContentSize()
                    .padding(2.dp)
            )












//            // 🔹 SafePlay Advanced Glow System (Neon + Halo Expansion + Circling Light)
//            val infiniteGlow = rememberInfiniteTransition(label = "pulseGlow")
//            val isDark = isSystemInDarkTheme()
//
//// ⚡ Pulsing brightness (0.5 → 1.3)
//            val pulseAlpha: Float by infiniteGlow.animateFloat(
//                initialValue = 0.5f,
//                targetValue = 1.3f,
//                animationSpec = infiniteRepeatable(
//                    animation = tween(durationMillis = 1700, easing = LinearEasing),
//                    repeatMode = RepeatMode.Reverse
//                ),
//                label = "pulseAlpha"
//            )
//
//// 🌠 Circling light angle (rotates endlessly)
//            val lightAngle: Float by infiniteGlow.animateFloat(
//                initialValue = 0f,
//                targetValue = 360f,
//                animationSpec = infiniteRepeatable(
//                    animation = tween(durationMillis = 6000, easing = LinearEasing),
//                    repeatMode = RepeatMode.Restart
//                ),
//                label = "lightAngle"
//            )
//
//// 💫 Halo expansion when selected
//            val haloExpansion by animateDpAsState(
//                targetValue = if (selected) 36.dp else 0.dp,
//                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
//                label = "haloExpansion"
//            )
//
//// 🌈 Theme-adaptive neon tones
//            val primaryGlow = if (isDark) Color(0xFF00FFFF) else Color(0xFFAA33FF)
//            val secondaryGlow = if (isDark) Color(0xFF4FC3F7) else Color(0xFFFF66CC)
//            val deepAccent = if (isDark) Color(0xFF0077FF) else Color(0xFFE040FB)
//
//// 🌟 Outer aura (strong)
//            val outerAura = Brush.radialGradient(
//                colors = listOf(
//                    primaryGlow.copy(alpha = pulseAlpha * 0.55f),
//                    secondaryGlow.copy(alpha = pulseAlpha * 0.3f),
//                    Color.Transparent
//                ),
//                center = Offset(
//                    x = 300f * kotlin.math.cos(Math.toRadians(lightAngle.toDouble())).toFloat(),
//                    y = 300f * kotlin.math.sin(Math.toRadians(lightAngle.toDouble())).toFloat()
//                ),
//                radius = 900f + haloExpansion.value
//            )
//
//// 💥 Inner intense core glow
//            val innerAura = Brush.radialGradient(
//                colors = listOf(
//                    deepAccent.copy(alpha = pulseAlpha * 0.7f),
//                    Color.Transparent
//                ),
//                center = Offset.Zero,
//                radius = 400f + haloExpansion.value
//            )
//
//// 🔁 Circulating rim light (adds motion even where no glow)
//            val rimLight = Brush.sweepGradient(
//                colors = listOf(
//                    Color.Transparent,
//                    primaryGlow.copy(alpha = 0.35f),
//                    secondaryGlow.copy(alpha = 0.25f),
//                    Color.Transparent
//                )
//            )
//
//            Box(
//                modifier = Modifier
//                    .widthIn(max = maxBubbleWidth)
//                    .pointerInput(Unit) {
//                        detectTapGestures(
//                            onLongPress = { onLongPress() },
//                            onTap = { onTap() }
//                        )
//                    }
//                    // 🌈 Outer aura
//                    .background(
//                        brush = if (selected) outerAura else Brush.radialGradient(listOf(Color.Transparent, Color.Transparent)),
//                        shape = RoundedCornerShape(radius)
//                    )
//                    // 💥 Inner core aura
//                    .background(
//                        brush = if (selected) innerAura else Brush.radialGradient(listOf(Color.Transparent, Color.Transparent)),
//                        shape = RoundedCornerShape(radius)
//                    )
//                    // 🌌 Subtle rim light — keeps moving around the bubble
//                    .border(
//                        width = if (selected) 1.5.dp else 0.dp,
//                        brush = rimLight,
//                        shape = RoundedCornerShape(radius)
//                    )
//                    // 🌟 Shadow halo that expands when selected
//                    .shadow(
//                        if (selected) 28.dp + haloExpansion else 0.dp,
//                        shape = RoundedCornerShape(radius),
//                        ambientColor = if (selected)
//                            primaryGlow.copy(alpha = 0.8f)
//                        else Color.Transparent,
//                        spotColor = if (selected)
//                            secondaryGlow.copy(alpha = 0.8f)
//                        else Color.Transparent
//                    )
//                    .animateContentSize()
//                    .padding(2.dp)
//            )





            {
                val elevation = if (selected) 8.dp else 2.dp
                Surface(
                    tonalElevation = elevation,
                    shape = RoundedCornerShape(radius),
                    color = bubbleColor
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // message text + edited badge inline
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            if (message.edited) {
                                Text(
                                    text = "edited",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = timestampColor,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // timestamp + status / ticks row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // formatted timestamp
                            Text(
                                text = niceTimestamp(message.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = timestampColor
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {


                                // ⭐ if this message is starred by me
                                // 🌈 Multi-color glowing star if this message is starred by me
                                if (message.starredByMe) {
                                    GlowingStarIcon(
                                        modifier = Modifier.padding(end = 6.dp),
                                        size = 16.dp
                                    )
                                }


                                // reactions preview (condensed)
                                if (message.reactions.isNotEmpty()) {
                                    ReactionPreview(
                                        reactions = message.reactions,
                                        onClick = { onOpenReactions?.invoke() }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                // delivered / read ticks (only for messages I sent)
                                if (isMine) {
                                    when {
                                        message.read -> Icon(
                                            Icons.Default.DoneAll,
                                            contentDescription = "Read",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )

                                        message.delivered -> Icon(
                                            Icons.Default.DoneAll,
                                            contentDescription = "Delivered",
                                            tint = timestampColor,
                                            modifier = Modifier.size(16.dp)
                                        )

                                        else -> Icon(
                                            Icons.Default.Done,
                                            contentDescription = "Sent",
                                            tint = timestampColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                // status indicator (SENDING/SENT/FAILED) for optimistic UI
                                when (message.status) {
                                    MessageStatus.SENDING -> Icon(
                                        Icons.Default.HourglassEmpty,
                                        contentDescription = "Sending",
                                        modifier = Modifier.size(16.dp)
                                    )

                                    MessageStatus.SENT -> {
                                        /* nothing extra — ticks show delivered/read */
                                    }

                                    MessageStatus.FAILED -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Error,
                                                contentDescription = "Failed",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            if (onRetrySend != null) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Retry",
                                                    modifier = Modifier
                                                        .clickable { onRetrySend() }
                                                        .padding(4.dp),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }

            // Reaction row (buttons)
            if (onReactionClick != null || onOpenReactions != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    // small set of quick emoji shortcuts
                    val quick = listOf("👍", "❤️", "😂", "🎉", "🔥")
                    quick.forEach { emoji ->
                        val count = message.reactions[emoji]?.size ?: 0
                        ReactionButton(emoji = emoji, count = count, onClick = { onReactionClick?.invoke(emoji) })
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    // open full picker
                    if (onOpenReactions != null) {
                        Text(
                            text = "Add",
                            modifier = Modifier
                                .clickable { onOpenReactions.invoke() }
                                .padding(6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Right avatar (my avatar) if mine
        if (isMine && showAvatar) {
            Spacer(modifier = Modifier.width(8.dp))
            AvatarImage(avatarUrl, avatarSize)
        }
    }
}

@Composable
private fun AvatarImage(avatarUrl: String?, size: Dp) {
    if (avatarUrl.isNullOrBlank()) {
        // simple placeholder circle with initials or '?'
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("?", style = MaterialTheme.typography.bodySmall)
        }
    } else {
        Image(
            painter = rememberAsyncImagePainter(avatarUrl),
            contentDescription = "User avatar",
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.surfaceTint, CircleShape)
        )
    }
}

@Composable
private fun ReactionButton(emoji: String, count: Int, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .height(28.dp)
            .wrapContentWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(modifier = Modifier.padding(start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, style = MaterialTheme.typography.bodySmall)
            if (count > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = count.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun ReactionPreview(reactions: Map<String, List<String>>, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        reactions.entries.take(4).forEach { (emoji, list) ->
            Text(text = emoji, modifier = Modifier.padding(end = 4.dp))
            if (list.isNotEmpty()) {
                Text(text = list.size.toString(), style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

@Composable
private fun GlowingStarIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    // 🌈 colors the star will cycle through
    val colors = listOf(
        Color(0xFFFFD700), // gold
        Color(0xFFFF8A80), // soft red
        Color(0xFF80D8FF), // cyan
        Color(0xFFB388FF), // purple
        Color(0xFFA7FFEB)  // mint
    )

    val transition = rememberInfiniteTransition(label = "starGlowTransition")

    // animate a float from 0 → colors.size continuously
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = colors.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starGlowPhase"
    )

    val index = phase.toInt() % colors.size
    val nextIndex = (index + 1) % colors.size
    val fraction = phase - phase.toInt()

    // interpolate between current color and next color
    val tintColor = androidx.compose.ui.graphics.lerp(
        colors[index],
        colors[nextIndex],
        fraction
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                ambientColor = tintColor.copy(alpha = 0.8f),
                spotColor = tintColor.copy(alpha = 0.8f)
            )
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = "Starred",
            tint = tintColor,
            modifier = Modifier.size(size)
        )
    }
}


private fun niceTimestamp(tsMillis: Long): String {
    if (tsMillis <= 0L) return ""
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = tsMillis }

    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
            && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == then.get(Calendar.YEAR)
            && yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)

    return when {
        sameDay -> {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            sdf.format(Date(tsMillis))
        }
        isYesterday -> {
            val sdf = SimpleDateFormat("'Yesterday' hh:mm a", Locale.getDefault())
            sdf.format(Date(tsMillis))
        }
        else -> {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            sdf.format(Date(tsMillis))
        }
    }
}








//package com.chat.safeplay.chat.handler
//
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.gestures.detectTapGestures
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Error
//import androidx.compose.material.icons.filled.HourglassEmpty
//import androidx.compose.material.icons.filled.Refresh
//import androidx.compose.material.icons.filled.Send
//import androidx.compose.material.icons.filled.Done
//import androidx.compose.material.icons.filled.DoneAll
//import androidx.compose.material.icons.outlined.Star
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import coil.compose.rememberAsyncImagePainter
//import java.text.SimpleDateFormat
//import java.util.*
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.graphics.Color
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.animation.animateContentSize
//import androidx.compose.animation.core.*
//import androidx.compose.ui.draw.shadow
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Brush
//
//
//@Composable
//fun MessageBubble(
//    message: ChatUiMessage,
//    isMine: Boolean,
//    showAvatar: Boolean,                // retained but per your request avatar always shown from ChatScreen
//    avatarUrl: String?,
//    selected: Boolean,
//    onLongPress: () -> Unit,
//    onTap: () -> Unit,
//    onRetrySend: (() -> Unit)? = null,   // called when FAILED retry button pressed
//    onReactionClick: ((emoji: String) -> Unit)? = null,
//    onOpenReactions: (() -> Unit)? = null
//) {
//    // UI tuning variables
//    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
//    val textColor = MaterialTheme.colorScheme.onSurface
//    val avatarSize = 40.dp
//    val maxBubbleWidth = 320.dp
//    val radius = 16.dp
//    val timestampColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
//
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 4.dp, horizontal = 8.dp),
//        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
//        verticalAlignment = Alignment.Bottom
//    ) {
//        // Left avatar (other user)
//        if (!isMine && showAvatar) {
//            AvatarImage(avatarUrl, avatarSize)
//            Spacer(modifier = Modifier.width(8.dp))
//        }
//
//        // Bubble content
//        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
//
//            // 🔹 Animated SafePlay Glow + Aura Light
//            val infiniteGlow = rememberInfiniteTransition(label = "pulseGlow")
//
//// breathing pulse intensity (0.3 ↔ 0.9)
//            val pulseAlpha: Float by infiniteGlow.animateFloat(
//                initialValue = 0.3f,
//                targetValue = 0.9f,
//                animationSpec = infiniteRepeatable(
//                    animation = tween(durationMillis = 1500, easing = LinearEasing),
//                    repeatMode = RepeatMode.Reverse
//                ),
//                label = "pulseAlpha"
//            )
//
//// shifting aura offset (subtle shimmer motion)
//            val shimmerOffset: Float by infiniteGlow.animateFloat(
//                initialValue = -100f,
//                targetValue = 100f,
//                animationSpec = infiniteRepeatable(
//                    animation = tween(durationMillis = 2500, easing = LinearEasing),
//                    repeatMode = RepeatMode.Reverse
//                ),
//                label = "shimmerOffset"
//            )
//
//// ✅ create dynamic gradient glow brush (light aura)
//            val auraBrush = Brush.radialGradient(
//                colors = listOf(
//                    MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha * 0.25f),
//                    MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha * 0.12f),
//                    Color.Transparent
//                ),
//                center = Offset(0f + shimmerOffset, 0f),
//                radius = 600f
//            )
//
//            Box(
//                modifier = Modifier
//                    .widthIn(max = maxBubbleWidth)
//                    .pointerInput(Unit) {
//                        detectTapGestures(
//                            onLongPress = { onLongPress() },
//                            onTap = { onTap() }
//                        )
//                    }
//                    // ✅ Soft glow highlight when selected
//                    .background(
//                        if (selected)
//                            MaterialTheme.colorScheme.primary.copy(alpha = (pulseAlpha * 0.3f))
//                        else
//                            Color.Transparent,
//                        shape = RoundedCornerShape(radius)
//                    )
//                    .shadow(
//                        if (selected) 12.dp else 0.dp,
//                        shape = RoundedCornerShape(radius),
//                        ambientColor = if (selected)
//                            MaterialTheme.colorScheme.primary.copy(alpha = (pulseAlpha * 0.5f))
//                        else Color.Transparent,
//                        spotColor = if (selected)
//                            MaterialTheme.colorScheme.primary.copy(alpha = (pulseAlpha * 0.5f))
//                        else Color.Transparent
//                    )
//                    .animateContentSize()
//                    .padding(2.dp)
//            )
//
//
//            {
//                val elevation = if (selected) 8.dp else 2.dp
//                Surface(
//                    tonalElevation = elevation,
//                    shape = RoundedCornerShape(radius),
//                    color = bubbleColor
//                ) {
//                    Column(modifier = Modifier.padding(12.dp)) {
//                        // message text + edited badge inline
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            Text(
//                                text = message.text,
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = textColor,
//                                modifier = Modifier.padding(end = 6.dp)
//                            )
//                            if (message.edited) {
//                                Text(
//                                    text = "edited",
//                                    style = MaterialTheme.typography.labelSmall,
//                                    color = timestampColor,
//                                    modifier = Modifier.padding(start = 4.dp)
//                                )
//                            }
//                        }
//
//                        Spacer(modifier = Modifier.height(6.dp))
//
//                        // timestamp + status / ticks row
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween,
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            // formatted timestamp
//                            Text(
//                                text = niceTimestamp(message.timestamp),
//                                style = MaterialTheme.typography.bodySmall,
//                                color = timestampColor
//                            )
//
//                            Row(verticalAlignment = Alignment.CenterVertically) {
//                                // reactions preview (condensed)
//                                if (message.reactions.isNotEmpty()) {
//                                    ReactionPreview(
//                                        reactions = message.reactions,
//                                        onClick = { onOpenReactions?.invoke() }
//                                    )
//                                    Spacer(modifier = Modifier.width(8.dp))
//                                }
//
//                                // delivered / read ticks (only for messages I sent)
//                                if (isMine) {
//                                    // prefer read > delivered > single tick
//                                    when {
//                                        message.read -> Icon(Icons.Default.DoneAll, contentDescription = "Read", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
//                                        message.delivered -> Icon(Icons.Default.DoneAll, contentDescription = "Delivered", tint = timestampColor, modifier = Modifier.size(16.dp))
//                                        else -> Icon(Icons.Default.Done, contentDescription = "Sent", tint = timestampColor, modifier = Modifier.size(16.dp))
//                                    }
//                                    Spacer(modifier = Modifier.width(6.dp))
//                                }
//
//                                // status indicator (SENDING/SENT/FAILED) for optimistic UI
//                                when (message.status) {
//                                    MessageStatus.SENDING -> Icon(Icons.Default.HourglassEmpty, contentDescription = "Sending", modifier = Modifier.size(16.dp))
//                                    MessageStatus.SENT -> { /* nothing extra — ticks show delivered/read */ }
//                                    MessageStatus.FAILED -> {
//                                        Row(verticalAlignment = Alignment.CenterVertically) {
//                                            Icon(Icons.Default.Error, contentDescription = "Failed", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
//                                            if (onRetrySend != null) {
//                                                Spacer(modifier = Modifier.width(6.dp))
//                                                Text(
//                                                    text = "Retry",
//                                                    modifier = Modifier
//                                                        .clickable { onRetrySend() }
//                                                        .padding(4.dp),
//                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
//                                                    color = MaterialTheme.colorScheme.primary
//                                                )
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            // Reaction row (buttons)
//            if (onReactionClick != null || onOpenReactions != null) {
//                Spacer(modifier = Modifier.height(6.dp))
//                Row(modifier = Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
//                    // small set of quick emoji shortcuts
//                    val quick = listOf("👍", "❤️", "😂", "🎉", "🔥")
//                    quick.forEach { emoji ->
//                        val count = message.reactions[emoji]?.size ?: 0
//                        ReactionButton(emoji = emoji, count = count, onClick = { onReactionClick?.invoke(emoji) })
//                        Spacer(modifier = Modifier.width(6.dp))
//                    }
//                    // open full picker
//                    if (onOpenReactions != null) {
//                        Text(
//                            text = "Add",
//                            modifier = Modifier
//                                .clickable { onOpenReactions.invoke() }
//                                .padding(6.dp),
//                            style = MaterialTheme.typography.labelSmall,
//                            color = MaterialTheme.colorScheme.primary
//                        )
//                    }
//                }
//            }
//        }
//
//        // Right avatar (my avatar) if mine
//        if (isMine && showAvatar) {
//            Spacer(modifier = Modifier.width(8.dp))
//            AvatarImage(avatarUrl, avatarSize)
//        }
//    }
//}
//
//@Composable
//private fun AvatarImage(avatarUrl: String?, size: Dp) {
//    if (avatarUrl.isNullOrBlank()) {
//        // simple placeholder circle with initials or '?'
//        Box(
//            modifier = Modifier
//                .size(size)
//                .clip(CircleShape)
//                .background(MaterialTheme.colorScheme.surfaceVariant),
//            contentAlignment = Alignment.Center
//        ) {
//            Text("?", style = MaterialTheme.typography.bodySmall)
//        }
//    } else {
//        Image(
//            painter = rememberAsyncImagePainter(avatarUrl),
//            contentDescription = "User avatar",
//            modifier = Modifier
//                .size(size)
//                .clip(CircleShape)
//                .border(1.dp, MaterialTheme.colorScheme.surfaceTint, CircleShape)
//        )
//    }
//}
//
//@Composable
//private fun ReactionButton(emoji: String, count: Int, onClick: () -> Unit) {
//    Surface(
//        shape = RoundedCornerShape(12.dp),
//        tonalElevation = 1.dp,
//        modifier = Modifier
//            .height(28.dp)
//            .wrapContentWidth()
//            .clickable { onClick() },
//        color = MaterialTheme.colorScheme.surface
//    ) {
//        Row(modifier = Modifier.padding(start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
//            Text(text = emoji, style = MaterialTheme.typography.bodySmall)
//            if (count > 0) {
//                Spacer(modifier = Modifier.width(6.dp))
//                Text(text = count.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
//            }
//        }
//    }
//}
//
//@Composable
//private fun ReactionPreview(reactions: Map<String, List<String>>, onClick: () -> Unit) {
//    Row(
//        modifier = Modifier
//            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
//            .padding(horizontal = 6.dp, vertical = 2.dp)
//            .clickable { onClick() },
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        reactions.entries.take(4).forEach { (emoji, list) ->
//            Text(text = emoji, modifier = Modifier.padding(end = 4.dp))
//            if (list.isNotEmpty()) {
//                Text(text = list.size.toString(), style = MaterialTheme.typography.labelSmall)
//                Spacer(modifier = Modifier.width(6.dp))
//            }
//        }
//    }
//}
//
//private fun niceTimestamp(tsMillis: Long): String {
//    if (tsMillis <= 0L) return ""
//    val now = Calendar.getInstance()
//    val then = Calendar.getInstance().apply { timeInMillis = tsMillis }
//
//    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
//            && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
//
//    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
//    val isYesterday = yesterday.get(Calendar.YEAR) == then.get(Calendar.YEAR)
//            && yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
//
//    return when {
//        sameDay -> {
//            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
//            sdf.format(Date(tsMillis))
//        }
//        isYesterday -> {
//            val sdf = SimpleDateFormat("'Yesterday' hh:mm a", Locale.getDefault())
//            sdf.format(Date(tsMillis))
//        }
//        else -> {
//            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
//            sdf.format(Date(tsMillis))
//        }
//    }
//}
