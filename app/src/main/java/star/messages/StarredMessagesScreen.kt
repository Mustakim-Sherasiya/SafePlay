package com.chat.safeplay.star.messages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.chat.safeplay.R
import com.chat.safeplay.chat.handler.ChatRoutes
import com.chat.safeplay.chat.handler.ChatUiMessage
import com.chat.safeplay.setting.manager.VideoLogo
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredMessagesScreen(
    navController: NavController,
    vm: StarredMessagesViewModel = viewModel()
) {
    val starredMessages by vm.messages.collectAsState()

    // 🔁 Manage which video is currently playing
    var currentVideo by remember { mutableStateOf(R.raw.star_message1) }

    // 🔁 Timer effect to switch every 6 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            currentVideo = if (currentVideo == R.raw.star_message1) {
                R.raw.star_message2
            } else {
                R.raw.star_message1
            }
        }
    }

    // ⭐ local selection state (ids of starred messages)
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 🔙 Back button behavior: clear selection first, exit only if none selected
    BackHandler {
        if (selectedIds.isNotEmpty()) {
            selectedIds = emptySet()
        } else {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SafePlay",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.width(8.dp))

                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                            VideoLogo(
                                resId = currentVideo,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedIds.isNotEmpty()) {
                                selectedIds = emptySet()
                            } else {
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            if (starredMessages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Starred Message",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(starredMessages) { msg ->

                        val myKey = vm.starKey

                        // decide who the other user is (same logic you had)
                        val otherId = if (!myKey.isNullOrBlank()) {
                            when {
                                msg.fromId == myKey -> msg.toId ?: msg.fromId
                                msg.toId == myKey -> msg.fromId
                                else -> msg.fromId
                            }
                        } else {
                            msg.fromId
                        }

                        val isMine = !myKey.isNullOrBlank() && msg.fromId == myKey

                        val displayName = if (isMine) {
                            "You"
                        } else {
                            val sourceId = if (msg.fromId == myKey) msg.toId ?: msg.fromId else msg.fromId
                            val src = sourceId ?: "User"
                            val suffix = if (src.length >= 2) src.takeLast(2) else src
                            "User$suffix"
                        }

                        val isSelected = selectedIds.contains(msg.id)

                        StarredMessageItem(
                            message = msg,
                            displayName = displayName,
                            isMine = isMine,
                            isSelected = isSelected,
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    // in selection mode → toggle
                                    selectedIds = selectedIds.toggle(msg.id)
                                } else {
                                    // normal tap → open chat at that message
                                    if (!otherId.isNullOrBlank()) {
                                        navController.navigate(
                                            ChatRoutes.chatWith(otherId, msg.id)
                                        )
                                    }
                                }
                            },
                            onLongClick = {
                                selectedIds = selectedIds.toggle(msg.id)
                            },
                            onUnstarClick = {
                                vm.unstarMessages(listOf(msg.id))
                                selectedIds = selectedIds - msg.id
                            }
                        )
                    }
                }

                // 🔻 FAB to unstar selected messages (with star + slash)
                if (selectedIds.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = {
                            vm.unstarMessages(selectedIds.toList())
                            selectedIds = emptySet()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        UnstarIcon()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StarredMessageItem(
    message: ChatUiMessage,
    displayName: String,
    isMine: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onUnstarClick: () -> Unit
) {
    if (isSelected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    else
        MaterialTheme.colorScheme.surfaceVariant

    val bgColor =
        if (isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else
            MaterialTheme.colorScheme.surfaceVariant

    val borderColor =
        if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            Color.Transparent

    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (isSelected) 6.dp else 2.dp,
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            // 💡 soft glow + lift when selected
            .shadow(
                elevation = if (isSelected) 10.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarForStarred(
                name = displayName,
                isMine = isMine
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = niceStarTime(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (message.starredByMe) {
                IconButton(
                    onClick = onUnstarClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    UnstarIcon()
                }
            }
        }
    }
}


    @Composable
private fun UnstarIcon() {
    // ⭐ star with a red slash "/"
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = "Unstar",
            tint = Color.Yellow,
            modifier = Modifier.size(22.dp)
        )
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .padding(3.dp)
        ) {
            drawLine(
                color = Color.Red,
                start = Offset(0f, size.height),
                end = Offset(size.width, 0f),
                strokeWidth = size.minDimension * 0.12f
            )
        }
    }
}

@Composable
private fun AvatarForStarred(
    name: String,
    isMine: Boolean
) {
    val initials = remember(name) {
        name
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
    }

    val bg = if (isMine) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

private fun niceStarTime(tsMillis: Long): String {
    if (tsMillis <= 0L) return ""
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(tsMillis))
}

// small helper to toggle selection in a Set
private fun Set<String>.toggle(id: String): Set<String> {
    val m = this.toMutableSet()
    if (m.contains(id)) m.remove(id) else m.add(id)
    return m
}








//package com.chat.safeplay.star.messages
//
//import android.net.Uri
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.chat.safeplay.R
//import com.chat.safeplay.setting.manager.VideoLogo
//import kotlinx.coroutines.delay
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.layout.Arrangement
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.compose.runtime.collectAsState
//import com.chat.safeplay.chat.handler.ChatUiMessage
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.compose.runtime.collectAsState
//import com.chat.safeplay.chat.handler.ChatRoutes
//
//
//
//
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun StarredMessagesScreen(
//    navController: NavController,
//    vm: StarredMessagesViewModel = viewModel()
//) {
//    val context = LocalContext.current
//    val starredMessages by vm.messages.collectAsState()
//
//
//
//    // 🔁 Manage which video is currently playing
//    var currentVideo by remember { mutableStateOf(R.raw.star_message1) }
//
//    // 🔁 Timer effect to switch every 6 seconds
//    LaunchedEffect(Unit) {
//        while (true) {
//            delay(6000)  // 6 seconds
//            currentVideo = if (currentVideo == R.raw.star_message1) {
//                R.raw.star_message2  // your second video
//            } else {
//                R.raw.star_message1
//            }
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            SmallTopAppBar(
//                title = {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        // 🟢 SafePlay text
//                        Text(
//                            text = "SafePlay",
//                            color = MaterialTheme.colorScheme.onBackground,
//                            fontSize = 19.sp,
//                            fontWeight = FontWeight.SemiBold
//                        )
//
//                        Spacer(Modifier.width(8.dp))
//
//                        // 🟢 Animated MP4 logo that switches videos every 6s
//                        Surface(
//                            modifier = Modifier.size(36.dp),
//                            shape = CircleShape,
//                            color = Color.Transparent,
//                            tonalElevation = 0.dp
//                        ) {
//                            VideoLogo(
//                                resId = currentVideo,
//                                modifier = Modifier.fillMaxSize()
//                            )
//                        }
//                    }
//                },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(
//                            imageVector = Icons.Default.ArrowBack,
//                            contentDescription = "Back",
//                            tint = MaterialTheme.colorScheme.onBackground
//                        )
//                    }
//                },
//                colors = TopAppBarDefaults.smallTopAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.surface
//                )
//            )
//        },
//        containerColor = MaterialTheme.colorScheme.background
//    ) { padding ->
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//        ) {
//
//            if (starredMessages.isEmpty()) {
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(
//                        text = "No Starred Message",
//                        style = MaterialTheme.typography.bodyLarge,
//                        color = MaterialTheme.colorScheme.onBackground
//                    )
//                }
//            } else {
//                LazyColumn(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(horizontal = 12.dp, vertical = 8.dp),
//                    verticalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    items(starredMessages) { msg ->
//                        StarredMessageItem(
//                            message = msg,
//                            onClick = {
//                                val myKey = vm.starKey
//
//                                // decide who the other user is
//                                val otherId = if (!myKey.isNullOrBlank()) {
//                                    when {
//                                        msg.fromId == myKey -> msg.toId ?: msg.fromId
//                                        msg.toId == myKey -> msg.fromId
//                                        else -> msg.fromId
//                                    }
//                                } else {
//                                    msg.fromId
//                                }
//
//                                if (!otherId.isNullOrBlank()) {
//                                    navController.navigate(
//                                        ChatRoutes.chatWith(otherId, msg.id)
//                                    )
//                                }
//                            }
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun StarredMessageItem(
//    message: ChatUiMessage,
//    onClick: () -> Unit
//) {
//    Surface(
//        shape = RoundedCornerShape(16.dp),
//        tonalElevation = 2.dp,
//        color = MaterialTheme.colorScheme.surfaceVariant,
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onClick() }
//    ) {
//        Column(modifier = Modifier.padding(12.dp)) {
//            Text(
//                text = message.text,
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.onSurface
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//            Text(
//                text = niceStarTime(message.timestamp),
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
//            )
//        }
//    }
//}
//
//
//private fun niceStarTime(tsMillis: Long): String {
//    if (tsMillis <= 0L) return ""
//    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
//    return sdf.format(java.util.Date(tsMillis))
//}
//
