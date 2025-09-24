package com.chat.safeplay.profile

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.chat.safeplay.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf(TextFieldValue("Loading...")) }
    var gender by remember { mutableStateOf("Loading...") }
    var about by remember { mutableStateOf(TextFieldValue("Loading....")) }
    var showDisplayName by remember { mutableStateOf(false) }
    var publicId by remember { mutableStateOf("Loading...") }
    var photoMenuExpanded by remember { mutableStateOf(false) }



    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { currentUid ->
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && snapshot.exists()) {
                        publicId = snapshot.getString("publicId") ?: currentUid
                    }
                }
        }
    }
    // Load saved data from Firestore when screen opens
    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { currentUid ->
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && snapshot.exists()) {
                        profilePhotoUrl = snapshot.getString("photoUrl")
                        name = TextFieldValue(snapshot.getString("name") ?: "")
                        gender = snapshot.getString("gender") ?: ""
                        about = TextFieldValue(snapshot.getString("about") ?: "")
                        showDisplayName = snapshot.getBoolean("showDisplayName") ?: true
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
        }
    }


    // Image picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val currentUid = auth.currentUser?.uid
            if (currentUid == null) {
                Toast.makeText(context, "Not authenticated", Toast.LENGTH_SHORT).show()
                return@let
            }

            try {
                // get mime type if available
                val contentType = context.contentResolver.getType(selectedUri) ?: "image/jpeg"
                val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                    .setContentType(contentType)
                    .build()

                val ref = storage.reference.child("profile_photos/$currentUid/profile.jpg")
                val uploadTask = ref.putFile(selectedUri, metadata)

                // optional: show progress to user
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val bytesTransferred = taskSnapshot.bytesTransferred
                    val totalBytes = taskSnapshot.totalByteCount
                    val progress = if (totalBytes > 0) (100.0 * bytesTransferred / totalBytes).toInt() else 0
                    // lightweight feedback — replace with Snackbar/ProgressBar in your UI if you want
                    Toast.makeText(context, "Uploading $progress%", Toast.LENGTH_SHORT).show()
                }

                uploadTask
                    .addOnSuccessListener {
                        // get download url and save to Firestore
                        ref.downloadUrl.addOnSuccessListener { downloadUri ->
                            profilePhotoUrl = downloadUri.toString()
                            db.collection("users").document(currentUid)
                                .set(mapOf("photoUrl" to profilePhotoUrl), SetOptions.merge())
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Profile photo updated", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Saved URL failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }.addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to get download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Upload error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    // editing state
    var editingName by remember { mutableStateOf(false) }
    var nameBuffer by remember { mutableStateOf(name) } // TextFieldValue

    var editingAbout by remember { mutableStateOf(false) }
    var aboutBuffer by remember { mutableStateOf(about) } // TextFieldValue

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Circular container for the live video logo
                        Surface(
                            modifier = Modifier.size(45.dp),        // outer circle size
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.06f), // subtle background for contrast
                            tonalElevation = 0.dp
                        ) {
                            // Keep some padding so the video doesn't touch the edge
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                // VideoLogo should be small and fill the inner box
                                VideoLogo(
                                    resId = R.raw.small_live_logo,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Text(
                            "SafePlay",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF0F0F0F)
    ) {innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(160.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    if (!profilePhotoUrl.isNullOrEmpty()) {
                        // Show uploaded profile photo
                        Image(
                            painter = rememberAsyncImagePainter(profilePhotoUrl),
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Show SafePlay logo fallback
                        Image(
                            painter = painterResource(id = R.drawable.safeplay_logo),
                            contentDescription = "Default Profile",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-8).dp, y = (-4).dp)
                ) {
                    IconButton(
                        onClick = { photoMenuExpanded = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1F1F1F), CircleShape)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                    }

                    DropdownMenu(
                        expanded = photoMenuExpanded,
                        onDismissRequest = { photoMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Change Photo") },
                            onClick = {
                                photoMenuExpanded = false
                                launcher.launch("image/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove Photo") },
                            onClick = {
                                photoMenuExpanded = false
                                val currentUid = auth.currentUser?.uid ?: return@DropdownMenuItem

                                val ref = storage.reference.child("profile_photos/$currentUid/profile.jpg")
                                ref.delete()
                                    .addOnSuccessListener {
                                        db.collection("users").document(currentUid)
                                            .set(mapOf("photoUrl" to null), SetOptions.merge())
                                        profilePhotoUrl = null
                                        Toast.makeText(context, "Profile photo removed", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        )
                    }
                }

            }





            // small space between avatar and the display name
            Spacer(modifier = Modifier.height(8.dp))

            // ---------------- Display name (either user name or generated UserXX) ----------------
            val computedDisplayName: String = remember(name.text, publicId, showDisplayName) {
                if (showDisplayName && name.text.isNotBlank()) {
                    name.text
                } else {
                    // fallback: last 2 chars of publicId (or uid)
                    val id = publicId.ifBlank { auth.currentUser?.uid ?: "" }
                    val suffix = if (id.length >= 2) id.takeLast(2) else id
                    "User${suffix.uppercase()}"
                }
            }

            Text(
                text = computedDisplayName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 6.dp, bottom = 6.dp)
                    .clickable {
                        // reuse your existing edit flow: open name editor
                        nameBuffer = name
                        editingName = true
                    }
            )
            // -------------------------------------------------------------------------------------




            // small extra spacing before content card
            Spacer(modifier = Modifier.height(12.dp))



            // Content card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = MaterialTheme.shapes.medium,
                color = Color(0xFF1D1D1D),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // UID
                    ProfileRow(
                        logoRes = R.raw.uid_logo,
                        content = {
                            Column {
                                Text("UID", color = Color.LightGray, fontSize = 12.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(publicId, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    )
                    Divider(color = Color(0xFF2A2A2A), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // Name
                    ProfileRow(
                        logoRes = R.raw.name_logo,
                        content = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Name", color = Color.LightGray, fontSize = 12.sp)
                                        Spacer(Modifier.height(2.dp))
                                        if (editingName) {
                                            OutlinedTextField(
                                                value = nameBuffer,
                                                onValueChange = { nameBuffer = it },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                                    focusedBorderColor = Color.Gray,
                                                    unfocusedBorderColor = Color.Transparent,
                                                    containerColor = Color.Transparent,
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    cursorColor = Color.White
                                                )
                                            )
                                        } else {
                                            Text(name.text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    if (editingName) {
                                        Button(onClick = {
                                            val currentUid = auth.currentUser?.uid ?: return@Button
                                            name = nameBuffer
                                            db.collection("users").document(currentUid)
                                                .set(mapOf("name" to name.text), SetOptions.merge())
                                            editingName = false
                                        }) { Text("Save") }
                                    } else {
                                        IconButton(onClick = {
                                            nameBuffer = name
                                            editingName = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit name", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    )
                    Divider(color = Color(0xFF2A2A2A), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // Display name toggle
                    ProfileRow(
                        logoRes = R.raw.toggle_logo,
                        content = {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Display name", color = Color.LightGray, fontSize = 12.sp)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        if (showDisplayName) "Display name is ON" else "Display name is OFF",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                                Switch(checked = showDisplayName, onCheckedChange = {
                                    showDisplayName = it
                                    auth.currentUser?.uid?.let { currentUid ->
                                        db.collection("users").document(currentUid)
                                            .set(mapOf("showDisplayName" to it), SetOptions.merge())
                                    }
                                })
                            }
                        }
                    )
                    Divider(color = Color(0xFF2A2A2A), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // Gender
                    ProfileRow(
                        logoRes = R.raw.gender_logo,
                        content = {
                            var expanded by remember { mutableStateOf(false) }
                            Column {
                                Text("Gender", color = Color.LightGray, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = gender,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = { expanded = !expanded }) {
                                            Icon(
                                                painterResource(id = R.drawable.ic_expand),
                                                contentDescription = "Expand",
                                                tint = Color.White
                                            )
                                        }
                                    },
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = Color.Gray,
                                        unfocusedBorderColor = Color.Transparent,
                                        containerColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    listOf("Male", "Female", "Other").forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                gender = option
                                                expanded = false
                                                auth.currentUser?.uid?.let { currentUid ->
                                                    db.collection("users").document(currentUid)
                                                        .set(mapOf("gender" to option), SetOptions.merge())
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    Divider(color = Color(0xFF2A2A2A), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // About
                    ProfileRow(
                        logoRes = R.raw.about_logo,
                        content = {
                            Column {
                                Text("About", color = Color.LightGray, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                                if (editingAbout) {
                                    OutlinedTextField(
                                        value = aboutBuffer,
                                        onValueChange = { aboutBuffer = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = Color.Gray,
                                            unfocusedBorderColor = Color.Transparent,
                                            containerColor = Color.Transparent,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = Color.White
                                        )
                                    )
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        Button(onClick = {
                                            about = aboutBuffer
                                            auth.currentUser?.uid?.let { currentUid ->
                                                db.collection("users").document(currentUid)
                                                    .set(mapOf("about" to about.text), SetOptions.merge())
                                            }
                                            editingAbout = false
                                        }) { Text("Save") }
                                    }
                                } else {
                                    Text(
                                        about.text,
                                        color = Color.White,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                aboutBuffer = about
                                                editingAbout = true
                                            }
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Reusable row **/

@Composable
fun ProfileRow(logoRes: Int, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF2A2A2A),
                modifier = Modifier.size(40.dp)
            ) {
                VideoLogo(resId = logoRes, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}


/** Video logo **/
@Composable
fun VideoLogo(resId: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaUri = "android.resource://${context.packageName}/$resId"
            setMediaItem(MediaItem.fromUri(mediaUri))
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        factory = {
            PlayerView(context).apply {
                useController = false
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.player = player
            }
        },
        modifier = modifier
    )
}














//package com.chat.safeplay.profile
//
//import android.net.Uri
//import android.view.ViewGroup
//import android.widget.FrameLayout
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.Edit
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.input.TextFieldValue
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.viewinterop.AndroidView
//import coil.compose.rememberAsyncImagePainter
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.storage.FirebaseStorage
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.SetOptions
//import com.chat.safeplay.R
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.DisposableEffect
//import androidx.media3.common.MediaItem
//import androidx.media3.exoplayer.ExoPlayer
//import androidx.media3.ui.PlayerView
//import androidx.navigation.NavHostController
//import android.widget.Toast
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.text.font.FontWeight
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ProfileScreen(navController: NavHostController) {
//    val context = LocalContext.current
//    val auth = FirebaseAuth.getInstance()
//    val db = FirebaseFirestore.getInstance()
//    val storage = FirebaseStorage.getInstance()
//
//    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
//    var name by remember { mutableStateOf(TextFieldValue("Muju")) }
//    var gender by remember { mutableStateOf("Male/Female") }
//    var about by remember { mutableStateOf(TextFieldValue("SafePlay is a app where you can play safely while chatting with other person.")) }
//    var showDisplayName by remember { mutableStateOf(true) }
//    val uid = auth.currentUser?.uid ?: "A1B2C3" // fallback shown in screenshot
//
//    // Image picker
//    val launcher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent()
//    ) { uri: Uri? ->
//        uri?.let { selectedUri ->
//            val currentUid = auth.currentUser?.uid
//            if (currentUid == null) {
//                Toast.makeText(context, "Not authenticated", Toast.LENGTH_SHORT).show()
//                return@let
//            }
//            val ref = storage.reference.child("profile_photos/$currentUid.jpg")
//            ref.putFile(selectedUri).addOnSuccessListener {
//                ref.downloadUrl.addOnSuccessListener { downloadUri ->
//                    profilePhotoUrl = downloadUri.toString()
//                    db.collection("users").document(currentUid)
//                        .set(mapOf("photoUrl" to profilePhotoUrl), SetOptions.merge())
//                }
//            }.addOnFailureListener {
//                Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    // editing state for name and about
//    var editingName by remember { mutableStateOf(false) }
//    var nameBuffer by remember { mutableStateOf(name.text) }
//    var editingAbout by remember { mutableStateOf(false) }
//    var aboutBuffer by remember { mutableStateOf(about.text) }
//
//    // Use Scaffold so the top app bar doesn't push content with extra gap
//    Scaffold(
//        topBar = {
//            SmallTopAppBar(
//                title = {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        // small live logo near title (use a small video resource or drawable)
//                        VideoLogo(resId = R.raw.small_live_logo, modifier = Modifier.size(22.dp))
//                        Spacer(Modifier.width(8.dp))
//                        Text("SafePlay", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
//                    }
//                },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
//                    }
//                },
//                actions = {
//                    // optional action (empty)
//                },
//                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent)
//            )
//        },
//        containerColor = Color(0xFF0F0F0F) // dark background kept
//    ) { innerPadding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding), // ensures content starts just below the app bar with no extra gap
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Top
//        ) {
//
//            // center big profile avatar with big white circle behind (like screenshot)
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 8.dp), // no top padding here — app bar spacing handled by innerPadding
//                contentAlignment = Alignment.Center
//            ) {
//                // White circular background
//                Surface(
//                    modifier = Modifier.size(160.dp),
//                    shape = CircleShape,
//                    color = Color.White,
//                    shadowElevation = 4.dp
//                ) {
//                    // profile image inside white circle
//                    Image(
//                        painter = rememberAsyncImagePainter(profilePhotoUrl ?: "https://your-default-app-logo.png"),
//                        contentDescription = "Profile Photo",
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .clip(CircleShape)
//                            .clickable { launcher.launch("image/*") },
//                        contentScale = ContentScale.Crop
//                    )
//                }
//
//                // small edit icon overlay bottom-right
//                IconButton(
//                    onClick = { launcher.launch("image/*") },
//                    modifier = Modifier
//                        .align(Alignment.BottomEnd)
//                        .offset(x = (-8).dp, y = (-4).dp)
//                        .size(36.dp)
//                        .background(Color(0xFF1F1F1F), CircleShape)
//                ) {
//                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
//                }
//            }
//
//            // Content card that contains the fields (dark gray card)
//            Surface(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 12.dp),
//                shape = MaterialTheme.shapes.medium,
//                color = Color(0xFF1D1D1D),
//                tonalElevation = 2.dp
//            ) {
//                Column(modifier = Modifier.padding(12.dp)) {
//                    // UID row (row style matches screenshot)
//                    ProfileRow(
//                        logoRes = R.raw.uid_logo,
//                        content = {
//                            Column {
//                                Text("UID", color = Color.LightGray, fontSize = 12.sp)
//                                Spacer(Modifier.height(2.dp))
//                                Text(uid, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
//                            }
//                        }
//                    )
//                    Divider(color = Color(0xFF2A2A2A), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
//
//                    // Name row (editable)
//                    ProfileRow(
//                        logoRes = R.raw.name_logo,
//                        content = {
//                            Column {
//                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
//                                    Column(modifier = Modifier.weight(1f)) {
//                                        Text("Name", color = Color.LightGray, fontSize = 12.sp)
//                                        Spacer(Modifier.height(2.dp))
//                                        if (editingName) {
//                                            OutlinedTextField(
//                                                value = TextFieldValue(nameBuffer),
//                                                onValueChange = { nameBuffer = it.text },
//                                                singleLine = true,
//                                                modifier = Modifier.fillMaxWidth(),
//                                                colors = TextFieldDefaults.outlinedTextFieldColors(
//                                                    focusedBorderColor = Color.Gray,
//                                                    unfocusedBorderColor = Color.Transparent,
//                                                    containerColor = Color.Transparent,
//                                                    focusedTextColor = Color.White,
//                                                    unfocusedTextColor = Color.White,
//                                                    cursorColor = Color.White
//                                                )
//
//                                            )
//
//                                        } else {
//                                            Text(name.text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
//                                        }
//                                    }
//                                    Spacer(Modifier.width(8.dp))
//                                    if (editingName) {
//                                        Button(onClick = {
//                                            val currentUid = auth.currentUser?.uid ?: return@Button
//                                            name = TextFieldValue(nameBuffer)
//                                            db.collection("users").document(currentUid)
//                                                .set(mapOf("name" to name.text), SetOptions.merge())
//                                            editingName = false
//                                        }) {
//                                            Text("Save")
//                                        }
//                                    } else {
//                                        IconButton(onClick = { editingName = true }) {
//                                            Icon(Icons.Default.Edit, contentDescription = "Edit name", tint = Color.White)
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    )
//                    Divider(color = Color(0xFF2A2A2A), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
//
//                    // Display name toggle row
//                    ProfileRow(
//                        logoRes = R.raw.toggle_logo,
//                        content = {
//                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
//                                Column(modifier = Modifier.weight(1f)) {
//                                    Text("Display name", color = Color.LightGray, fontSize = 12.sp)
//                                    Spacer(Modifier.height(2.dp))
//                                    Text(if (showDisplayName) "Display name is ON" else "Display name is OFF", color = Color.White, fontSize = 14.sp)
//                                }
//                                Switch(checked = showDisplayName, onCheckedChange = {
//                                    showDisplayName = it
//                                    auth.currentUser?.uid?.let { currentUid ->
//                                        db.collection("users").document(currentUid)
//                                            .set(mapOf("showDisplayName" to it), SetOptions.merge())
//                                    }
//                                })
//                            }
//                        }
//                    )
//                    Divider(color = Color(0xFF2A2A2A), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
//
//                    // Gender row (dropdown)
//                    ProfileRow(
//                        logoRes = R.raw.gender_logo,
//                        content = {
//                            var expanded by remember { mutableStateOf(false) }
//                            Column {
//                                Text("Gender", color = Color.LightGray, fontSize = 12.sp)
//                                Spacer(Modifier.height(4.dp))
//                                OutlinedTextField(
//                                    value = gender,
//                                    onValueChange = {},
//                                    readOnly = true,
//                                    trailingIcon = {
//                                        IconButton(onClick = { expanded = !expanded }) {
//                                            Icon(
//                                                painterResource(id = R.drawable.ic_expand),
//                                                contentDescription = "Expand",
//                                                tint = Color.White
//                                            )
//                                        }
//                                    },
//                                    colors = TextFieldDefaults.outlinedTextFieldColors(
//                                        focusedBorderColor = Color.Gray,
//                                        unfocusedBorderColor = Color.Transparent,
//                                        containerColor = Color.Transparent,
//                                        focusedTextColor = Color.White,
//                                        unfocusedTextColor = Color.White,
//                                        cursorColor = Color.White
//                                    ),
//                                    modifier = Modifier.fillMaxWidth()
//                                )
//                                DropdownMenu(
//                                    expanded = expanded,
//                                    onDismissRequest = { expanded = false }
//                                ) {
//                                    listOf("Male", "Female", "Other").forEach { option ->
//                                        DropdownMenuItem(
//                                            text = { Text(option) },
//                                            onClick = {
//                                                gender = option
//                                                expanded = false
//                                                // save to Firestore
//                                                FirebaseAuth.getInstance().currentUser?.uid?.let { currentUid ->
//                                                    FirebaseFirestore.getInstance()
//                                                        .collection("users")
//                                                        .document(currentUid)
//                                                        .set(mapOf("gender" to option), SetOptions.merge())
//                                                }
//                                            }
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    )
//
//                    Divider(color = Color(0xFF2A2A2A), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
//
//                    // About row (editable multi-line)
//                    ProfileRow(
//                        logoRes = R.raw.about_logo,
//                        content = {
//                            Column {
//                                Text("About", color = Color.LightGray, fontSize = 12.sp)
//                                Spacer(Modifier.height(4.dp))
//                                if (editingAbout) {
//                                    OutlinedTextField(
//                                        value = TextFieldValue(aboutBuffer),
//                                        onValueChange = { aboutBuffer = it.text },
//                                        modifier = Modifier.fillMaxWidth(),
//                                        colors = TextFieldDefaults.outlinedTextFieldColors(
//                                            focusedBorderColor = Color.Gray,
//                                            unfocusedBorderColor = Color.Transparent,
//                                            containerColor = Color.Transparent,
//                                            focusedTextColor = Color.White,
//                                            unfocusedTextColor = Color.White,
//                                            cursorColor = Color.White
//                                        )
//                                    )
//                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
//                                        Button(onClick = {
//                                            about = TextFieldValue(aboutBuffer)
//                                            auth.currentUser?.uid?.let { currentUid ->
//                                                db.collection("users").document(currentUid)
//                                                    .set(mapOf("about" to about.text), SetOptions.merge())
//                                            }
//                                            editingAbout = false
//                                        }) {
//                                            Text("Save")
//                                        }
//                                    }
//                                } else {
//                                    Text(
//                                        about.text,
//                                        color = Color.White,
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .clickable { editingAbout = true }
//                                    )
//                                }
//                            }
//                        }
//                    )
//
//                    Spacer(modifier = Modifier.height(12.dp))
//                }
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//        }
//    }
//}
//
///** Reusable row with left animated logo and right content **/
//@Composable
//fun ProfileRow(logoRes: Int, content: @Composable () -> Unit) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        // left logo box
//        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
//            VideoLogo(resId = logoRes, modifier = Modifier.size(36.dp))
//        }
//        Spacer(Modifier.width(10.dp))
//        Box(modifier = Modifier.weight(1f)) {
//            content()
//        }
//    }
//}
//
///** Safer VideoLogo that releases ExoPlayer when disposed (keeps your current approach) **/
//@Composable
//fun VideoLogo(resId: Int, modifier: Modifier = Modifier) {
//    val context = LocalContext.current
//    val player = remember {
//        ExoPlayer.Builder(context).build().apply {
//            val mediaUri = "android.resource://${context.packageName}/$resId"
//            setMediaItem(MediaItem.fromUri(mediaUri))
//            playWhenReady = true
//            repeatMode = ExoPlayer.REPEAT_MODE_ALL
//            prepare()
//        }
//    }
//
//    DisposableEffect(player) {
//        onDispose { player.release() }
//    }
//
//    AndroidView(
//        factory = {
//            PlayerView(context).apply {
//                useController = false
//                layoutParams = FrameLayout.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.MATCH_PARENT
//                )
//                this.player = player
//            }
//        },
//        modifier = modifier
//    )
//}
//









