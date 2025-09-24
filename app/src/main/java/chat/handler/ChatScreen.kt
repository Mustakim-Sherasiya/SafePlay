package com.chat.safeplay.chat.handler

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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    publicId: String,
    navController: androidx.navigation.NavController,
    vm: ChatViewModel = viewModel(factory = ChatViewModelFactory(publicId))
) {
    val messages by vm.messages.collectAsState()
    val selected by vm.selected.collectAsState()
    val otherUser by vm.otherUser.collectAsState()
    val typingState by vm.typingState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // Typing indicator row (small)
            if (otherIsTyping) {
                Text(
                    text = "$displayName is typing...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
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
                    val isMine = (msg.fromId == vm.myPublicId) || (msg.fromId == vm.myUid)

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
                                if (!success) Toast.makeText(context, "Retry failed: ${err ?: "unknown"}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onReactionClick = { emoji ->
                            vm.toggleReaction(msg.id, emoji) { success, err ->
                                if (!success) Toast.makeText(context, "Reaction failed: ${err ?: "unknown"}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenReactions = {
                            reactionTargetMessageId = msg.id
                            reactionPickerOpen = true
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Input bar + typing handling
            var input by remember { mutableStateOf("") }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
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
                                Toast.makeText(context, "Send failed: ${err ?: "unknown"}", Toast.LENGTH_SHORT).show()
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
                        if (input.isNotBlank()) Toast.makeText(context, "Cannot send only spaces", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
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
