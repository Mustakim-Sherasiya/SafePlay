package com.chat.safeplay.chat.handler

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chat.safeplay.setting.manager.getSavedBackgroundUri
import com.chat.safeplay.setting.manager.saveBackgroundUri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    publicId: String,
    navController: androidx.navigation.NavController,
    vm: ChatViewModel = viewModel(factory = ChatViewModelFactory(publicId))
) {
    val messages by vm.messages.collectAsState()
    val pending by vm.pendingMessages.collectAsState()
    val selected by vm.selected.collectAsState()
    val otherUser by vm.otherUser.collectAsState()
    val typingState by vm.typingState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    var customBackgroundUri by remember { mutableStateOf<Uri?>(null) }


    val uid = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(uid) {
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val url = doc.getString("backgroundUrl")
                    if (!url.isNullOrBlank()) {
                        customBackgroundUri = Uri.parse(url)
                        saveBackgroundUri(context, url) // cache locally for faster next load
                    } else {
                        customBackgroundUri = getSavedBackgroundUri(context)
                    }
                }
                .addOnFailureListener {
                    customBackgroundUri = getSavedBackgroundUri(context)
                }
        } else {
            customBackgroundUri = getSavedBackgroundUri(context)
        }
    }

    val listState = rememberLazyListState()

    // auto-clear selection and typing when this Composable is disposed (navigated away)
    DisposableEffect(Unit) {
        onDispose {
            vm.clearSelection()
            vm.setTyping(false)
        }
    }





    // UI state for edit / delete / reactions dialogs
    var editDialogOpen by remember { mutableStateOf(false) }
    var deleteConfirmOpen by remember { mutableStateOf(false) }
    var reactionPickerOpen by remember { mutableStateOf(false) }
    var reactionTargetMessageId by remember { mutableStateOf<String?>(null) }
    var editMessageId by remember { mutableStateOf<String?>(null) }
    var editDraft by remember { mutableStateOf("") }

    // typing debounce job
    var typingJob by remember { mutableStateOf<Job?>(null) }

    // load earlier throttle
    var loadEarlierTriggeredAt by remember { mutableStateOf(0L) }

    // Scroll to bottom when new messages added (auto-scroll)
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    // Mark messages read when messages arrive / become visible
    LaunchedEffect(messages) {
        val lastTs = messages.lastOrNull()?.timestamp ?: 0L
        if (lastTs > 0L) {
            vm.markMessagesReadUpTo(lastTs)
        }
    }

    // Pagination: when user scrolls to top, load earlier (throttle to 1s)
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex == 0) {
            val now = System.currentTimeMillis()
            if (now - loadEarlierTriggeredAt > 1000L) {
                loadEarlierTriggeredAt = now
                vm.loadEarlierMessages()
            }
        }
    }

    // Compute a friendly display name
    val displayName = remember(otherUser) {
        val suffix = if (otherUser.publicId.length >= 2) otherUser.publicId.takeLast(2) else otherUser.publicId
        if (otherUser.showDisplayName && !otherUser.name.isNullOrBlank()) otherUser.name!! else "User$suffix"
    }

    // Typing indicator text: if any other uid (not mine) has typing = true
    val otherIsTyping = remember(typingState, vm.myUid) {
        typingState.any { it.key != vm.myUid && it.value }
    }

    Scaffold(
        topBar = {
            if (selected.isEmpty()) {
                SmallTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = rememberAsyncImagePainter(otherUser.photoUrl ?: ""),
                                contentDescription = "Other avatar",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            // clear typing & selection on back
                            vm.setTyping(false)
                            vm.clearSelection()
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }

                    }
                )
            } else {
                SmallTopAppBar(
                    title = { Text("${selected.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { vm.clearSelection() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        // Delete selected
                        IconButton(onClick = { deleteConfirmOpen = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete selected")
                        }
                        // Star selected
                        IconButton(onClick = {
                            vm.toggleStarSelected()
                            Toast.makeText(context, "Star toggled", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Outlined.Star, contentDescription = "Star selected")
                        }
                        // Edit when exactly one selected
                        if (selected.size == 1) {
                            IconButton(onClick = {
                                val id = selected.first()
                                val msg = messages.find { it.id == id }
                                if (msg != null) {
                                    editMessageId = id
                                    editDraft = msg.text
                                    editDialogOpen = true
                                } else {
                                    Toast.makeText(context, "Message not found", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit selected")
                            }
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (customBackgroundUri == null)
                        if (isDarkTheme) Color.Black else Color.White
                    else
                        Color.Transparent
                )
        ) {
            // background image if user selected one
            customBackgroundUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                )
            }

            // main chat content column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

// ------------------- SHOWING TYPING INDICATOR -------------------//
                // Typing indicator row (small)
                if (otherIsTyping) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (customBackgroundUri != null)
                                    Color.Black.copy(alpha = 0.35f)
                                else
                                    Color.Transparent
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$displayName is typing...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .shadow(2.dp, ambientColor = Color.Black.copy(alpha = 0.6f)) // ✅ subtle glow here
                        )
                    }
                }



                // Messages list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    state = listState
                ) {
                    itemsIndexed(messages) { _, msg ->
                        val isMine =
                            msg.fromId == vm.myUid || msg.fromId == vm.myPublicId.ifBlank { vm.myUid }


                        MessageBubble(
                            message = msg,
                            isMine = isMine,
                            showAvatar = true, // keep avatar always shown
                            avatarUrl = if (isMine) vm.myPhotoUrl else otherUser.photoUrl,
                            selected = selected.contains(msg.id),
                            onLongPress = {
                                // try to toggle selection as before
                                vm.toggleSelect(msg.id)
                            },
                            onTap = {
                                if (selected.isNotEmpty()) vm.toggleSelect(msg.id)
                            },
                            onRetrySend = {
                                // retry using message.text
                                vm.retrySend(msg.text) { success, err ->
                                    if (!success) Toast.makeText(
                                        context,
                                        "Retry failed: ${err ?: "unknown"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onReactionClick = { emoji ->
                                vm.toggleReaction(msg.id, emoji) { success, err ->
                                    if (!success) Toast.makeText(
                                        context,
                                        "Reaction failed: ${err ?: "unknown"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onOpenReactions = {
                                reactionTargetMessageId = msg.id
                                reactionPickerOpen = true
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    // 🕒 Pending delayed messages (shown as sending soon)
                    items(
                        items = pending.values.toList(),
                        key = { it.localId } // each pending message has a unique ID
                    ) { pendingMsg: PendingMessage ->
                        PendingMessageBubble(
                            pendingMsg = pendingMsg,
                            onCancel = { vm.cancelPendingMessage(pendingMsg.localId) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }



                    item { Spacer(modifier = Modifier.height(8.dp)) }

                }

                // Input bar + typing handling
                var input by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            if (customBackgroundUri != null)
                                Color.Black.copy(alpha = 0.4f)   // 🔹 darker overlay when wallpaper active
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )

                        .padding(start = 16.dp, end = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = input,
                        onValueChange = { new ->
                            input = new
                            // typing debounce: send typing=true immediately, then reset to false after 1500ms of inactivity
                            typingJob?.cancel()
                            vm.setTyping(true)
                            typingJob = scope.launch {
                                delay(1500)
                                vm.setTyping(false)
                            }
                        },
                        placeholder = { Text("Message") },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (input.isNotBlank() && input.trim().isNotEmpty()) {
                            val textToSend = input.trim()
                            // stop typing indicator
                            typingJob?.cancel()
                            vm.setTyping(false)

                            vm.sendMessage(textToSend) { success, err ->
                                if (!success) {
                                    Toast.makeText(
                                        context,
                                        "Send failed: ${err ?: "unknown"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    // success feedback
                                }
                            }
                            input = ""
                            scope.launch {
                                if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                            }
                        } else {
                            // prevent sending spaces
                            if (input.isNotBlank()) Toast.makeText(
                                context,
                                "Cannot send only spaces",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }

    // --- Edit dialog (single message edit) ---
    if (editDialogOpen && editMessageId != null) {
        AlertDialog(
            onDismissRequest = { editDialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    val id = editMessageId!!
                    val newText = editDraft
                    vm.editMessage(id, newText) { success, err ->
                        if (!success) {
                            Toast.makeText(context, "Edit failed: ${err ?: "unknown"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    editDialogOpen = false
                    vm.clearSelection()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editDialogOpen = false }) { Text("Cancel") }
            },
            title = { Text("Edit message") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editDraft,
                        onValueChange = { editDraft = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Profanity will be filtered automatically.")
                }
            }
        )
    }

    // --- Delete confirmation dialog (for selection) ---
    if (deleteConfirmOpen) {
        AlertDialog(
            onDismissRequest = { deleteConfirmOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    if (selected.isNotEmpty()) {
                        vm.deleteSelected()
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                    }
                    deleteConfirmOpen = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmOpen = false }) { Text("Cancel") }
            },
            title = { Text("Delete messages") },
            text = { Text("Delete ${selected.size} selected message(s)? This cannot be undone.") }
        )
    }

    // --- Reaction picker dialog ---
    if (reactionPickerOpen && reactionTargetMessageId != null) {
        val emojis = listOf("👍", "❤️", "😂", "🎉", "🔥", "😮", "😢")
        AlertDialog(
            onDismissRequest = { reactionPickerOpen = false; reactionTargetMessageId = null },
            confirmButton = {
                TextButton(onClick = {
                    reactionPickerOpen = false
                    reactionTargetMessageId = null
                }) { Text("Close") }
            },
            title = { Text("React") },
            text = {
                // simple chunked grid (no FlowRow dependency)
                val chunked = emojis.chunked(4)
                Column {
                    chunked.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { emoji ->
                                Text(
                                    text = emoji,
                                    modifier = Modifier
                                        .clickable {
                                            val id = reactionTargetMessageId
                                            if (!id.isNullOrBlank()) {
                                                vm.toggleReaction(id, emoji) { success, err ->
                                                    if (!success) Toast.makeText(context, "Reaction failed: ${err ?: "unknown"}", Toast.LENGTH_SHORT).show()
                                                }
                                                reactionPickerOpen = false
                                                reactionTargetMessageId = null
                                            }
                                        }
                                        .padding(8.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

            }
        )
    }

    // When user presses back during dialog, clear typing presence
    BackHandler {
        vm.setTyping(false)
        vm.clearSelection()
        navController.popBackStack()
    }

}



@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PendingMessageBubble(
    pendingMsg: PendingMessage,
    onCancel: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val totalSeconds = pendingMsg.remainingSeconds.coerceAtLeast(1)
    val remaining = pendingMsg.remainingSeconds.coerceIn(0, totalSeconds)
    val progressTarget = 1f - ((remaining - 1f) / totalSeconds.toFloat())

    // 🎞 Smooth continuous bar shrink
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = (1000L * totalSeconds).toInt(),
            easing = LinearEasing
        ),
        label = "SmoothProgressDynamic"
    )

    // 🟢 SafePlay colors
    val accentColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val textColor = MaterialTheme.colorScheme.onSurface

    // 🌈 Glow + shimmer infinite transitions
    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")

    // Soft breathing glow
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Horizontal shimmer (moving gradient)
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse // 👈 shimmer bounces back
        ),
        label = "shimmerOffset"
    )

    // Gradient shimmer brush
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            accentColor.copy(alpha = glowAlpha * 0.3f),
            accentColor.copy(alpha = 0.9f),
            accentColor.copy(alpha = glowAlpha * 0.3f)
        ),
        start = Offset(shimmerOffset - 200f, 0f),
        end = Offset(shimmerOffset + 200f, 0f)
    )

    // 🌙 Vanish on cancel or time out
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(pendingMsg.remainingSeconds) {
        if (pendingMsg.remainingSeconds <= 0) visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.9f),
        exit = fadeOut(tween(250)) + shrinkVertically(tween(300))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .background(bgColor, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            // 📝 Message preview text
            Text(
                text = pendingMsg.text,
                color = textColor,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // 🔹 Animated shimmer progress bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        RoundedCornerShape(50)
                    )
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(6.dp)
                        .background(shimmerBrush, RoundedCornerShape(50))
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = accentColor.copy(alpha = glowAlpha * 0.6f),
                            spotColor = accentColor.copy(alpha = glowAlpha * 0.6f)
                        )
                )
            }

            // Countdown + cancel button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sending in ${pendingMsg.remainingSeconds}s...",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = {
                        // ✅ trigger gentle vibration
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                        // vanish instantly
                        visible = false
                        onCancel()
                    }
                )
                {
                    Text("✖ Cancel", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }
    }
}


//@OptIn(ExperimentalAnimationApi::class)
//@Composable
//fun PendingMessageBubble(
//    pendingMsg: PendingMessage,
//    onCancel: () -> Unit
//) {
//    val totalSeconds = 3f
//    val remaining = pendingMsg.remainingSeconds.coerceIn(0, 3)
//    val progressTarget = 1f - ((remaining - 1) / totalSeconds)
//
//    // 🎞 Smooth animation for progress line
//    val animatedProgress by animateFloatAsState(
//        targetValue = progressTarget.coerceIn(0f, 1f),
//        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
//        label = "SmoothProgress"
//    )
//
//    // 🔹 Fade + scale animation
//    var visible by remember { mutableStateOf(true) }
//    LaunchedEffect(pendingMsg.remainingSeconds) {
//        if (pendingMsg.remainingSeconds <= 0) visible = false
//    }
//
//    // 🟢 Get dynamic accent color from theme
//    val accentColor = MaterialTheme.colorScheme.primary
//    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
//    val textColor = MaterialTheme.colorScheme.onSurface
//
//    AnimatedVisibility(
//        visible = visible,
//        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f),
//        exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 12.dp)
//                .background(bgColor, RoundedCornerShape(16.dp))
//                .padding(12.dp)
//        ) {
//            // 📝 Message text
//            Text(
//                text = pendingMsg.text,
//                color = textColor,
//                fontSize = 16.sp,
//                modifier = Modifier.padding(bottom = 6.dp)
//            )
//
//            // 🔹 Animated accent progress bar
//            Box(
//                Modifier
//                    .fillMaxWidth()
//                    .height(5.dp)
//                    .background(
//                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
//                        RoundedCornerShape(50)
//                    )
//            ) {
//                Box(
//                    Modifier
//                        .fillMaxWidth(animatedProgress)
//                        .height(5.dp)
//                        .background(accentColor, RoundedCornerShape(50))
//                )
//            }
//
//            // ⏳ Countdown and cancel
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(top = 6.dp),
//                horizontalArrangement = Arrangement.End,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "Sending in ${pendingMsg.remainingSeconds}s...",
//                    color = textColor.copy(alpha = 0.7f),
//                    fontSize = 13.sp,
//                    modifier = Modifier.weight(1f)
//                )
//                TextButton(onClick = {
//                    visible = false
//                    onCancel()
//                }) {
//                    Text("✖ Cancel", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
//                }
//            }
//        }
//    }
//}
//
//
//
//






//--------------------------------------------------------------------------------------------------------------



//package com.chat.safeplay.chat.handler
//
//import android.widget.Toast
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.Send
//import androidx.compose.material.icons.outlined.Delete
//import androidx.compose.material.icons.outlined.Star
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import coil.compose.rememberAsyncImagePainter
//import kotlinx.coroutines.launch
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ChatScreen(
//    publicId: String,
//    navController: androidx.navigation.NavController,
//    vm: ChatViewModel = viewModel(factory = ChatViewModelFactory(publicId))
//) {
//    val messages by vm.messages.collectAsState()
//    val selected by vm.selected.collectAsState()
//    val otherUser by vm.otherUser.collectAsState()
//    val scope = rememberCoroutineScope()
//    val context = LocalContext.current
//    val listState = rememberLazyListState()
//
//    // auto-scroll to bottom when new messages arrive
//    LaunchedEffect(messages.size) {
//        if (messages.isNotEmpty()) {
//            listState.animateScrollToItem(messages.lastIndex)
//        }
//    }
//
//    // compute display name
//    val displayName = remember(otherUser) {
//        val suffix = if (otherUser.publicId.length >= 2) {
//            otherUser.publicId.takeLast(2)
//        } else otherUser.publicId
//        if (otherUser.showDisplayName && !otherUser.name.isNullOrBlank()) {
//            otherUser.name!!
//        } else {
//            "User$suffix"
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            if (selected.isEmpty()) {
//                SmallTopAppBar(
//                    title = {
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            Image(
//                                painter = rememberAsyncImagePainter(otherUser.photoUrl ?: ""),
//                                contentDescription = "Other avatar",
//                                modifier = Modifier
//                                    .size(36.dp)
//                                    .clip(CircleShape)
//                            )
//                            Spacer(modifier = Modifier.width(8.dp))
//                            Text(
//                                text = displayName,
//                                maxLines = 1,
//                                overflow = TextOverflow.Ellipsis
//                            )
//                        }
//                    },
//                    navigationIcon = {
//                        IconButton(onClick = { navController.popBackStack() }) {
//                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                        }
//                    }
//                )
//            } else {
//                SmallTopAppBar(
//                    title = { Text("${selected.size} selected") },
//                    navigationIcon = {
//                        IconButton(onClick = { vm.clearSelection() }) {
//                            Icon(Icons.Default.ArrowBack, contentDescription = "Clear selection")
//                        }
//                    },
//                    actions = {
//                        IconButton(onClick = {
//                            vm.deleteSelected()
//                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
//                        }) {
//                            Icon(Icons.Outlined.Delete, contentDescription = "Delete selected")
//                        }
//                        IconButton(onClick = {
//                            vm.toggleStarSelected()
//                            Toast.makeText(context, "Star toggled", Toast.LENGTH_SHORT).show()
//                        }) {
//                            Icon(Icons.Outlined.Star, contentDescription = "Star selected")
//                        }
//                    }
//                )
//            }
//        },
//        containerColor = MaterialTheme.colorScheme.background
//    ) { innerPadding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//        ) {
//            // Messages list
//            // --- Debug row (temporary, helps debugging) ---
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 12.dp, vertical = 6.dp),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text(
//                    text = "myUid=${vm.myUid.takeIf { it.isNotBlank() } ?: "?"}  myPub=${vm.myPublicId.ifBlank { "?" }}",
//                    style = MaterialTheme.typography.bodySmall,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//                Text(
//                    text = "other=${otherUser.publicId}",
//                    style = MaterialTheme.typography.bodySmall
//                )
//            }
//
//// Messages list
//            LazyColumn(
//                modifier = Modifier
//                    .weight(1f)
//                    .fillMaxWidth()
//                    .padding(horizontal = 12.dp, vertical = 8.dp),
//                state = listState
//            ) {
//                itemsIndexed(messages) { _, msg ->
//                    // robust "isMine" check: message.fromId may be publicId OR uid
//                    val isMine = (msg.fromId == vm.myPublicId) || (msg.fromId == vm.myUid)
//
//                    MessageBubble(
//                        message = msg,
//                        isMine = isMine,
//                        showAvatar = true,
//                        avatarUrl = if (isMine) vm.myPhotoUrl else otherUser.photoUrl,
//                        selected = selected.contains(msg.id),
//                        onLongPress = { vm.toggleSelect(msg.id) },
//                        onTap = { if (selected.isNotEmpty()) vm.toggleSelect(msg.id) }
//                    )
//                    Spacer(modifier = Modifier.height(6.dp))
//                }
//            }
//
//
//            // Input bar
//            var input by remember { mutableStateOf("") }
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 12.dp, vertical = 10.dp)
//                    .clip(RoundedCornerShape(28.dp))
//                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
//                    .padding(start = 16.dp, end = 9.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                TextField(
//                    value = input,
//                    onValueChange = { input = it },
//                    placeholder = { Text("Message") },
//                    modifier = Modifier
//                        .weight(1f)
//                        .height(54.dp),
//                    colors = TextFieldDefaults.textFieldColors(
//                        containerColor = Color.Transparent,
//                        focusedIndicatorColor = Color.Transparent,
//                        unfocusedIndicatorColor = Color.Transparent,
//                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
//                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
//                        cursorColor = MaterialTheme.colorScheme.primary
//                    ),
//                    singleLine = true
//                )
//                IconButton(onClick = {
//                    if (input.isNotBlank()) {
//                        vm.sendMessage(input) { success, err ->
//                            if (!success) {
//                                Toast.makeText(context, "Send failed: ${err ?: "unknown"}", Toast.LENGTH_SHORT).show()
//                            } else {
//                                // clear input and scroll on success
//                                input = ""
//                                scope.launch {
//                                    if (messages.isNotEmpty()) {
//                                        listState.animateScrollToItem(messages.lastIndex)
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }) {
//                    Icon(Icons.Default.Send, contentDescription = "Send")
//                }
//            }
//        }
//    }
//}
//
