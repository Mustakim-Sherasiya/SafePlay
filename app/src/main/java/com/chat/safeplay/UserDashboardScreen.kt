package com.chat.safeplay

import android.widget.Toast
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.res.painterResource
import com.chat.safeplay.R
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.chat.safeplay.profile.ProfileRoutes
import com.google.firebase.auth.FirebaseAuth
import com.chat.safeplay.chat.handler.ChatRoutes
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import android.util.Log



// replace the existing data class User { ... } with this
data class User(
    val uid: String = "",
    val email: String = "",
    val phone: String = "",
    val gender: String = "",
    val name: String? = null,
    val publicId: String? = null,
    val showDisplayName: Boolean? = null,
    val photoUrl: String? = null
)

private data class ConversationOverview(
    val convoId: String,
    val title: String,
    val lastMessage: String,
    val photoUrl: String?,
    val hasMessages: Boolean = true,
    val lastUpdatedMillis: Long? = null
)

private data class DashboardRow(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboardScreen(
    navController: NavHostController,
    currentUserUid: String
) {
    val context = LocalContext.current

    // Existing search state

    var foundUser by remember { mutableStateOf<User?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<String?>(null) }
    val db = FirebaseFirestore.getInstance()
    // --- add this at the top of UserDashboardScreen ---
    val auth = FirebaseAuth.getInstance()
    var photoUrl by remember { mutableStateOf<String?>(null) }


    // Conversations state (used by the conversation loader below)
    var conversations by remember { mutableStateOf<List<ConversationOverview>>(emptyList()) }
    var convosLoading by remember { mutableStateOf(true) }
    var convosError by remember { mutableStateOf<String?>(null) }


    val appUid = auth.currentUser?.uid
    if (appUid == null) {
        Toast.makeText(context, "Not signed in", Toast.LENGTH_LONG).show()
        return
    }

// Fetch the user's document and read publicId
    // Fetch the user's document and read publicId + photoUrl (store into state)
    db.collection("users").document(appUid).get()
        .addOnSuccessListener { doc ->
            val publicId = doc.getString("publicId")?.trim()?.ifBlank { null }
            val publicIdText = publicId ?: "(no publicId)"

            // read & sanitize photoUrl and save to state so UI updates
            val rawPhoto = doc.getString("photoUrl")
            photoUrl = rawPhoto?.trim()?.trim('"')?.ifBlank { null }

            // Debug toast (shows publicId and photoUrl presence)
           // val photoText = if (photoUrl != null) "photoUrl loaded" else "no photoUrl"
            Toast.makeText(
                context,

      //          "publicId: $publicIdText\n$photoText",

                "UID: $publicIdText",
                Toast.LENGTH_LONG
            ).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(
                context,
                "Failed loading profile: ${e.message ?: "unknown"}",
                Toast.LENGTH_LONG
            ).show()
        }


    // return the other participant id from a convoId like "idA_idB"
// picks the id that is not equal to current user's uid (appUid). If none matches, returns the second id.
    fun otherIdFromConvo(convoId: String, myUid: String?): String {
        if (convoId.isBlank()) return convoId
        val parts = convoId.split("_")
        if (parts.size != 2) return convoId // fallback: not a paired id -> treat as single id
        val a = parts[0]
        val b = parts[1]
        return when {
            myUid == null -> b // cannot decide, return second
            a == myUid -> b
            b == myUid -> a
            else -> b // neither matches (maybe convoId uses publicIds) -> return second part
        }
    }



    //  // Load user's photoUrl from Firestore
// NEW: load recent conversations where current user is a participant
    // Real-time listener for chats (replace the old LaunchedEffect block)
    // Robust DisposableEffect: load my publicId first, attach UID listener, fallback to publicId only if needed.
// Also surface errors via Toast so you can see why things fail (index/permission/etc).
    // Robust DisposableEffect: attach UID listener immediately, load publicId and attach publicId fallback if needed.
// returns a single onDispose at the end so it compiles correctly.
    DisposableEffect(auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            convosLoading = false
            convosError = "Not signed in"
            onDispose { /* nothing */ }
        } else {
            convosLoading = true
            convosError = null

            val listener = db.collection("chats")
                .whereArrayContains("participants", uid)
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .limit(25)
                .addSnapshotListener { snap, err ->
                    convosLoading = false
                    if (err != null) {
                        convosError = "Failed to load conversations: ${err.message ?: "unknown"}"
                        conversations = emptyList()
                        return@addSnapshotListener
                    }
                    if (snap != null && !snap.isEmpty) {
                        val list = snap.documents.map { doc ->
                            val rawLast = doc.getString("lastMessage")?.trim() ?: ""
                            val hasMsg = rawLast.isNotBlank()
                            val title = doc.getString("title") ?: doc.getString("displayName") ?: doc.id
                            val photo = doc.getString("photoUrl")?.trim()?.trim('"')?.ifBlank { null }
                            val lastUpdatedTs = doc.getTimestamp("lastUpdated")?.toDate()?.time
                            ConversationOverview(
                                convoId = doc.id,
                                title = title,
                                lastMessage = if (hasMsg) rawLast else "No messages yet",
                                photoUrl = photo,
                                hasMessages = hasMsg,
                                lastUpdatedMillis = lastUpdatedTs
                            )
                        }
                        conversations = list
                        convosError = null
                    } else {
                        conversations = emptyList()
                    }
                }

            onDispose {
                listener.remove()
            }
        }
    }







    // Function to search by publicId (UID shown in profile)
    fun searchUserByPublicId(publicId: String) {
        val queryId = publicId.trim()
        if (queryId.isEmpty()) {
            errorMessage = "Please enter a UID"
            foundUser = null
            return
        }

        isLoading = true
        errorMessage = null
        foundUser = null

        db.collection("users")
            .whereEqualTo("publicId", queryId)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                isLoading = false
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    val uid = doc.id

                    val currentUid = auth.currentUser?.uid
                    if (uid == currentUid) {
                        val msg = "Hey Buddy! 😎 , why search yourself ? 🤔 , There is no one to Match you HERE 🤩, You are unique in you 😉"

                        // Show Toast
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

                        // Show inline red text
                        errorMessage = msg

                        foundUser = null
                        return@addOnSuccessListener
                    }





//                    // Prevent searching yourself (RED TEXT)
//                    val currentUid = auth.currentUser?.uid
//                    if (uid == currentUid) {
//                        errorMessage = "Hey Buddy! 😎 ,why search yourself ? 🤔 ,There is no one to Match you HERE 🤩,You are unique in you 😉."
//                        foundUser = null
//                        return@addOnSuccessListener
//                    }



                    val email = doc.getString("email") ?: ""
                    val phone = doc.getString("phone") ?: ""
                    val name = doc.getString("name")            // nullable
                    val pId = doc.getString("publicId")         // nullable
                    val show = doc.getBoolean("showDisplayName")// nullable (true/false/null)
                    val photo = doc.getString("photoUrl")       // nullable
                    val gender = doc.getString("gender") ?: ""

                    foundUser = User(
                        uid = uid,
                        email = email,
                        phone = phone,
                        name = name,
                        publicId = pId,
                        photoUrl = photo,
                        gender = gender,
                        showDisplayName = show
                    )
                } else {
                    errorMessage = "No user found with id \"$queryId\""
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMessage = "Search failed: ${e.message ?: "unknown error"}"
            }

    }
    // add this inside UserDashboardScreen (near other helpers / state)
    fun computeDisplayName(user: User): String {
        val show = user.showDisplayName == true
        val name = user.name
        // prefer publicId if present, otherwise fallback to uid
        val idSource = user.publicId?.takeIf { it.isNotBlank() } ?: user.uid
        val suffix = if (idSource.length >= 2) idSource.takeLast(2) else idSource
        return if (show && !name.isNullOrBlank()) name else "User${suffix.uppercase()}"
    }


    // dropdown state for the right-side logo
    var profileMenuExpanded by remember { mutableStateOf(false) }

//    val rows = remember {
//        listOf(
//            DashboardRow("HoYoverse", "Traveler! Version 6.0 is now live in Teyvat!", Icons.Filled.SportsEsports),
//            DashboardRow("Google", "we noticed a new sign-in to your Google", Icons.Filled.Language),
//            DashboardRow("Amazone", "Your Amazon order #123-4567890 is out", Icons.Filled.ShoppingCart),
//            DashboardRow("Microsoft", "Your Microsoft 365 subscription is sched-", Icons.Filled.Work),
//            DashboardRow("Facebook", "Hi, we thought you'd like to look back on", Icons.Filled.Person)
//        )
//    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Header card (app title + profile + rounded search)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // TOP ROW: title left, profile button right (with dropdown)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Card(
                                    shape = CircleShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // left small video logo (Live video)
                                        LiveVideoLogo(videoResId = R.raw.logo)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("SafePlay", style = MaterialTheme.typography.titleMedium)
                            }

                            // Right: profile button showing user photo or SafePlay logo
                            Box {
                                FilledIconButton(
                                    onClick = { profileMenuExpanded = true },
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors()
                                ) {
                                    if (!photoUrl.isNullOrEmpty()) {
                                        // show user's profile image
                                        Image(
                                            painter = rememberAsyncImagePainter(model = photoUrl),
                                            contentDescription = "Profile Photo",
                                            modifier = Modifier
                                                .size(55.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // fallback to SafePlay logo
                                        Image(
                                            painter = painterResource(id = R.drawable.safeplay_logo),
                                            contentDescription = "Default Profile",
                                            modifier = Modifier
                                                .size(55.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = profileMenuExpanded,
                                    onDismissRequest = { profileMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Profile") },
                                        onClick = {
                                            profileMenuExpanded = false
                                            navController.navigate(ProfileRoutes.PROFILE) {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Settings") },
                                        onClick = {
                                            profileMenuExpanded = false
                                            Toast.makeText(context, "Settings clicked", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Starred") },
                                        onClick = {
                                            profileMenuExpanded = false
                                            Toast.makeText(context, "Starred clicked", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    Divider()
                                    DropdownMenuItem(
                                        text = { Text("Logout") },
                                        onClick = {
                                            profileMenuExpanded = false
                                            FirebaseAuth.getInstance().signOut()
                                            Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }

                        }

                        Spacer(Modifier.height(10.dp))

                        // SEARCH INPUT (below the top row)
                        TextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            placeholder = { Text("Enter UID") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(45.dp))
                        )

                        Spacer(Modifier.height(8.dp))

                        // SEARCH BUTTON
                        Button(
                            onClick = { searchUserByPublicId(searchInput.trim()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Search User")
                        }

                        // SHOW RESULT
                        if (isLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        errorMessage?.let {
                            Text(
                                text = it,
                                color = Color(0xFFFF9800), // Orange
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                            )
                        }

                        foundUser?.let { user ->
                            // compute display name:
                            val displayName = remember(user) {
                                val nameFromDb = user.name?.trim()
                                val show = user.showDisplayName ?: false

                                if (show && !nameFromDb.isNullOrBlank()) {
                                    nameFromDb
                                } else {
                                    // fallback to last 2 chars of publicId (or uid if publicId is null/blank)
                                    val id = user.publicId?.takeIf { it.isNotBlank() } ?: user.uid
                                    val suffix = if (id.length >= 2) id.takeLast(2) else id
                                    "User${suffix.uppercase()}"
                                }
                            }
//----------------------------- load and clean up photoUrl -------------------
                            // clean up photoUrl (remove surrounding quotes if any)
//
//                            val rawUrl = user.photoUrl
//                            val photoUrl = rawUrl?.trim()?.trim('"')?.ifBlank { null }




                            Card(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .fillMaxWidth()
                                    .clickable {
                                        val pid = user.publicId ?: user.uid
                                        navController.navigate(ChatRoutes.chatWith(pid))
                                    },
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    androidx.compose.foundation.Image( // fallback simple Image if you don't use AsyncImage
                                        painter = if (!user.photoUrl.isNullOrEmpty())
                                            rememberAsyncImagePainter(user.photoUrl)
                                        else
                                            painterResource(id = R.drawable.safeplay_logo),
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )



                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(displayName, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.height(4.dp))
                                        Text("gender: ${user.gender}", fontSize = 13.sp, color = Color.Gray)
                                        // Text("Phone: ${user.phone}", fontSize = 13.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }



                        Spacer(Modifier.height(8.dp))

                        // Search button (Firestore)
//                        androidx.compose.material3.Button(
//                            onClick = {
//                                if (searchInput.length < 10) {
//                                    errorMessage = "Enter full phone number"
//                                    foundUser = null
//                                    return@Button
//                                }
//                                errorMessage = null
//                                isLoading = true
//                                val firestore = FirebaseFirestore.getInstance()
//                                firestore.collection("users")
//                                    .whereEqualTo("phone", searchInput)
//                                    .limit(2)
//                                    .get()
//                                    .addOnSuccessListener { snap ->
//                                        isLoading = false
//                                        if (!snap.isEmpty) {
//                                            if (snap.size() > 1) {
//                                                errorMessage = "Multiple users found for this number"
//                                                foundUser = null
//                                                return@addOnSuccessListener
//                                            }
//                                            val doc = snap.documents.first()
//                                            val user = doc.toObject(User::class.java)
//                                            if (user != null && user.uid != currentUserUid) {
//                                                foundUser = user
//                                            } else {
//                                                errorMessage = if (user?.uid == currentUserUid) "Cannot chat with yourself" else "Invalid user record"
//                                                foundUser = null
//                                            }
//                                        } else {
//                                            errorMessage = "No user found with this phone number"
//                                            foundUser = null
//                                        }
//                                    }
//                                    .addOnFailureListener { e ->
//                                        isLoading = false
//                                        errorMessage = "Error searching user: ${e.message}"
//                                        foundUser = null
//                                    }
//
//                            },
//                            enabled = !isLoading,
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Text(if (isLoading) "Searching..." else "Search")
//                        }
                    }
                }
            }

            // Found user card (tap to open chat)
//            item {
//                errorMessage?.let {
//                    Text(
//                        it,
//                        color = MaterialTheme.colorScheme.error,
//                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
//                    )
//                }
//            }
//
//            item {
//                foundUser?.let { user ->
//                    Card(
//                        modifier = Modifier
//                            .padding(horizontal = 12.dp, vertical = 4.dp)
//                            .fillMaxWidth()
//                            .clickable {
//                                navController.navigate("chat/${user.uid}")
//                            },
//                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//                    ) {
//                        Column(modifier = Modifier.padding(16.dp)) {
//                            Text("Email: ${user.email}")
//                            Text("Phone: ${user.phone}")
//                        }
//                    }
//                }
//            }

            // Alerts list (static sample)
            // Recent Conversations (show real conversations; fallback to static rows if none)
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Recent Chats",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // show a loading indicator while conversations load
            if (convosLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    }
                }
            }



            // show any loading error
            convosError?.let { err ->
                item {
                    Text(
                        text = err,
                        color = Color.Red,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }




            // If we have conversations, render them



            if (conversations.isNotEmpty()) {
                items(conversations.size) { idx ->
                    val conv = conversations[idx]
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .fillMaxWidth()
                            .clickable {
                                // compute the other participant id from the convoId (works for uid_uid or publicId_publicId)
                                val otherId = otherIdFromConvo(conv.convoId, appUid)
                                if (!conv.hasMessages) {
                                    Toast.makeText(context, "No messages yet — start the conversation", Toast.LENGTH_SHORT).show()
                                }
                                // navigate to chat screen for the other participant
                                navController.navigate(ChatRoutes.chatWith(otherId))
                            }
                        ,
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.Image(
                                painter = if (!conv.photoUrl.isNullOrEmpty())
                                    rememberAsyncImagePainter(conv.photoUrl)
                                else
                                    painterResource(id = R.drawable.safeplay_logo),
                                contentDescription = "Convo Photo",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(conv.title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    conv.lastMessage,
                                    fontSize = 13.sp,
                                    color = if (conv.hasMessages) Color.Gray else Color(0xFF9E9E9E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            conv.lastUpdatedMillis?.let { millis ->
                                val timeText = android.text.format.DateFormat.format(
                                    "dd MMM, HH:mm",
                                    java.util.Date(millis)
                                ).toString()
                                Text(timeText, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            } else {
                // fallback to static alerts when there are no conversations
                // No conversations -> show an empty state message and CTA to start a new chat
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Show user's profile photo if available, otherwise fallback to SafePlay logo
                        Image(
                            painter = if (!photoUrl.isNullOrEmpty())
                                rememberAsyncImagePainter(model = photoUrl)
                            else
                                painterResource(id = R.drawable.safeplay_logo),
                            contentDescription = "User Logo",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "No conversations yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Start a new chat by searching a user UID above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(12.dp))
//                        Button(
//                            onClick = {
//                                // Scroll to search box, or navigate to user search screen
//                            },
//                            enabled = true
//                        ) {
//                            Text("Start Chat")
//                        }
                    }
                }

            }

            item { Spacer(Modifier.height(8.dp)) }

        }
    }
}

/**
 * Optional small preview (remove if not needed)
 */
@Composable
fun LogoPreview() {
    FilledIconButton(
        onClick = { /* … */ },
        shape = CircleShape,
        modifier = Modifier.size(60.dp),
        colors = IconButtonDefaults.filledIconButtonColors()
    ) {
        Image(
            painter = painterResource(id = R.drawable.safeplay_logo),
            contentDescription = "Profile",
            modifier = Modifier.size(70.dp),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * LiveVideoLogo: plays a short video from raw resources inside a small PlayerView.
 * Place a file app/src/main/res/raw/logo.mp4 (or .webm etc.) so R.raw.logo exists.
 */
@Composable
fun LiveVideoLogo(@RawRes videoResId: Int, modifier: Modifier = Modifier.size(36.dp)) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = "android.resource://${context.packageName}/$videoResId"
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                player = exoPlayer
            }
        }
    )
}
