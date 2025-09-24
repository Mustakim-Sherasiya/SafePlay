package com.chat.safeplay.chat.handler

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

@Composable
fun MessageBubble(
    message: ChatUiMessage,
    isMine: Boolean,
    showAvatar: Boolean,                // retained but per your request avatar always shown from ChatScreen
    avatarUrl: String?,
    selected: Boolean,
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
            Box(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { onLongPress() },
                            onTap = { onTap() }
                        )
                    }
            ) {
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
                                    // prefer read > delivered > single tick
                                    when {
                                        message.read -> Icon(Icons.Default.DoneAll, contentDescription = "Read", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        message.delivered -> Icon(Icons.Default.DoneAll, contentDescription = "Delivered", tint = timestampColor, modifier = Modifier.size(16.dp))
                                        else -> Icon(Icons.Default.Done, contentDescription = "Sent", tint = timestampColor, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                // status indicator (SENDING/SENT/FAILED) for optimistic UI
                                when (message.status) {
                                    MessageStatus.SENDING -> Icon(Icons.Default.HourglassEmpty, contentDescription = "Sending", modifier = Modifier.size(16.dp))
                                    MessageStatus.SENT -> { /* nothing extra — ticks show delivered/read */ }
                                    MessageStatus.FAILED -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Error, contentDescription = "Failed", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
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
//import androidx.compose.foundation.gestures.detectTapGestures
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.outlined.Star
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import coil.compose.rememberAsyncImagePainter
//import java.text.SimpleDateFormat
//import java.util.*
//import androidx.compose.ui.unit.Dp
//
//@Composable
//fun MessageBubble(
//    message: ChatUiMessage,
//    isMine: Boolean,
//    showAvatar: Boolean,
//    avatarUrl: String?,
//    selected: Boolean,
//    onLongPress: () -> Unit,
//    onTap: () -> Unit
//) {
//    val bubbleColor =
//        if (isMine) MaterialTheme.colorScheme.primaryContainer
//        else MaterialTheme.colorScheme.surfaceVariant
//    val avatarSize = 40.dp
//
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 4.dp, horizontal = 8.dp),
//        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
//        verticalAlignment = Alignment.Bottom
//    ) {
//        if (!isMine && showAvatar) {
//            // Other person's avatar (left side)
//            AvatarImage(avatarUrl, avatarSize)
//            Spacer(modifier = Modifier.width(6.dp))
//        }
//
//        // Bubble content
//        Box(
//            modifier = Modifier
//                .widthIn(max = 280.dp)
//                .pointerInput(Unit) {
//                    detectTapGestures(
//                        onLongPress = { onLongPress() },
//                        onTap = { onTap() }
//                    )
//                }
//        ) {
//            val elevation = if (selected) 8.dp else 2.dp
//            Surface(
//                tonalElevation = elevation,
//                shape = RoundedCornerShape(16.dp),
//                color = bubbleColor
//            ) {
//                Column(modifier = Modifier.padding(12.dp)) {
//                    Text(
//                        text = message.text,
//                        style = MaterialTheme.typography.bodyMedium
//                    )
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = formatTime(message.timestamp),
//                            style = MaterialTheme.typography.bodySmall
//                        )
//                        if (message.starredByMe) {
//                            Icon(
//                                imageVector = Icons.Outlined.Star,
//                                contentDescription = "Starred",
//                                modifier = Modifier.size(16.dp)
//                            )
//                        }
//                    }
//                }
//            }
//        }
//
//        if (isMine) {
//            Spacer(modifier = Modifier.width(6.dp))
//            // My avatar (right side)
//            AvatarImage(avatarUrl, avatarSize)
//        }
//    }
//}
//
//@Composable
//private fun AvatarImage(avatarUrl: String?, size: Dp) {
//    if (avatarUrl.isNullOrBlank()) {
//        // Fallback placeholder
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
//        )
//    }
//}
//
//private fun formatTime(ts: Long): String {
//    return try {
//        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
//        sdf.format(Date(ts))
//    } catch (e: Exception) {
//        ""
//    }
//}
