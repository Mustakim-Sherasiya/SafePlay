package com.chat.safeplay.setting.manager



import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.chat.safeplay.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelayChatScreen(navController: NavController) {
    val context = LocalContext.current

    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser


    val isSystemInDarkTheme = isSystemInDarkTheme()

    var delayEnabled by remember { mutableStateOf(false) }

    var selectedDelay by remember { mutableStateOf(1) }

    // 🎥 SafePlay Live Video Logo (MP4)
    val videoUri = Uri.parse("asset:///safeplay_logo.mp4") // place video in assets folder

    // 🔹 Firestore state sync
    // 🔹 Real-time Firestore listener


    DisposableEffect(user?.uid) {
        var registration: ListenerRegistration? = null

        user?.uid?.let { uid ->
            val docRef = db.collection("users").document(uid)

            // 🟢 Start listening for live updates
            registration = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    // Update UI instantly when Firestore changes
                    delayEnabled = snapshot.getBoolean("delayChatEnabled") ?: false
                    selectedDelay = (snapshot.getLong("delayChatSeconds") ?: 1L).toInt()
                } else {
                    // Create missing fields for new user (OFF by default)
                    docRef.set(
                        mapOf(
                            "delayChatEnabled" to false,
                            "delayChatSeconds" to 1
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    delayEnabled = false
                    selectedDelay = 1
                }
            }
        }

        onDispose {
            registration?.remove() // stop listening when composable leaves
        }
    }





    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 🟢 Text first
                        Text(
                            "SafePlay",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.width(8.dp))

                        // 🟢 Then the live video logo
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.06f),
                            tonalElevation = 0.dp
                        ) {
                            VideoLogo(
                                resId = R.raw.delay_chat, // use your top-bar video file
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // 🔹 Title
            Text(
                text = "Delay Chat",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.width(8.dp))

            // 🔹 Note section
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Note:\nThe chat delay feature postpones sending a message for a set amount of time. " +
                            "You can cancel the message at any point before the delay ends. " +
                            "Default timer is 1 second. Maximum delay can be set to 3 seconds.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(35.dp))

            // 🔹 Delay Chat Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Delay Chat", style = MaterialTheme.typography.bodyLarge)

                Switch(
                    checked = delayEnabled,
                    onCheckedChange = { newState ->
                        delayEnabled = newState
                        user?.uid?.let { uid ->
                            val userRef = db.collection("users").document(uid)

                            if (newState) {
                                // ensure we save a valid 1..3 second value (default to 1)
                                val secondsToSave = selectedDelay.coerceIn(1, 3)
                                // reflect locally
                                selectedDelay = secondsToSave

                                // write both fields atomically
                                userRef.update(
                                    mapOf(
                                        "delayChatEnabled" to true,
                                        "delayChatSeconds" to secondsToSave
                                    )
                                )
                            } else {
                                userRef.update("delayChatEnabled", false)


                            }
                        }
                    }
                )



            }

            Spacer(modifier = Modifier.height(20.dp))

            // 🔹 Delay Timer Dropdown

            // 🔹 Show timer and message only when delay chat is ON
            if (delayEnabled) {
                Spacer(modifier = Modifier.height(20.dp))

                // 🔹 Delay Timer Dropdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Select delay timer ", style = MaterialTheme.typography.bodyLarge)

                    DelayDropdown(selectedDelay) { newValue ->
                        selectedDelay = newValue
                        user?.uid?.let { uid ->
                            db.collection("users").document(uid)
                                .update("delayChatSeconds", newValue)
                        }
                    }

                    Text(" second", style = MaterialTheme.typography.bodyLarge)
                }

                Spacer(modifier = Modifier.height(35.dp))

                // 🔹 Info text showing delay status
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "Chat messages will be delayed by $selectedDelay second${if (selectedDelay > 1) "s" else ""}.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }












//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .background(
//                        MaterialTheme.colorScheme.surfaceVariant,
//                        RoundedCornerShape(12.dp)
//                    )
//                    .padding(horizontal = 16.dp, vertical = 12.dp),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.Center
//            ) {
//                Text("Select delay timer ", style = MaterialTheme.typography.bodyLarge)
//
//                DelayDropdown(selectedDelay) { newValue ->
//                    selectedDelay = newValue
//                    user?.uid?.let { uid ->
//                        db.collection("users").document(uid)
//                            .update("delayChatSeconds", newValue)
//                    }
//                }
//
//
//                Text(" second", style = MaterialTheme.typography.bodyLarge)
//            }
//
//            // 🔹 Info text showing delay status
//            if (delayEnabled) {
//                Spacer(modifier = Modifier.height(35.dp))
//
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 4.dp),
//                    shape = RoundedCornerShape(12.dp),
//                    colors = CardDefaults.cardColors(
//                        containerColor = MaterialTheme.colorScheme.surfaceVariant
//                    )
//                ) {
//                    Text(
//                        text = "Chat messages will be delayed by $selectedDelay second${if (selectedDelay > 1) "s" else ""}.",
//                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
//                        modifier = Modifier.padding(14.dp),
//                        color = MaterialTheme.colorScheme.onSurface
//                    )
//                }
//            }



        }
    }
}

// 🔸 Dropdown for selecting delay time
@Composable
fun DelayDropdown(selectedValue: Int, onValueChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(1, 2, 3)

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(text = selectedValue.toString())
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { value ->
                DropdownMenuItem(
                    text = { Text("$value") },
                    onClick = {
                        onValueChange(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

// 🔸 Video Logo Player (looped & muted)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoLogoPlayer(videoUri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUri)
            setMediaItem(mediaItem)
            playWhenReady = true
            volume = 0f
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                useController = false
                player = exoPlayer
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = modifier
    )
}
