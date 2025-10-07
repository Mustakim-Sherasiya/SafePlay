
package com.chat.safeplay.setting.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RawRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.chat.safeplay.R
import androidx.compose.material3.Text   // ✅ <-- add this line
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeChatBackgroundScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White

// 🔹 Upload progress states
    var uploadProgress by remember { mutableStateOf(0f) }
    var isUploading by remember { mutableStateOf(false) }

// 🔹 Load saved URI
    var customUri by remember { mutableStateOf(getSavedBackgroundUri(context)) }

// 🔹 Launcher for image/video picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(context, "Please log in first", Toast.LENGTH_SHORT).show()
                return@let
            }

            val db = FirebaseFirestore.getInstance()
            val storageRef = FirebaseStorage.getInstance().reference
            val userDoc = db.collection("users").document(uid)

            // 1️⃣ Fetch old background URL (to delete after success)
            userDoc.get().addOnSuccessListener { snapshot ->
                val oldUrl = snapshot.getString("backgroundUrl")

                // 2️⃣ Prepare new upload
                val newFileRef =
                    storageRef.child("chatBackgrounds/$uid/${System.currentTimeMillis()}.jpg")
                val uploadTask = newFileRef.putFile(selectedUri)

                // 3️⃣ Track progress visually
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress =
                        (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    isUploading = true
                    uploadProgress = (progress / 100f).toFloat()
                }

                // 4️⃣ Handle upload completion
                uploadTask.addOnSuccessListener {
                    isUploading = false
                    newFileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        // Save new background URL to Firestore
                        userDoc.update("backgroundUrl", downloadUri.toString())
                            .addOnSuccessListener {
                                customUri = downloadUri
                                saveBackgroundUri(context, downloadUri.toString())
                                Toast.makeText(
                                    context,
                                    "Background updated successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // 5️⃣ Delete old wallpaper if exists
                                if (!oldUrl.isNullOrEmpty()) {
                                    FirebaseStorage.getInstance().getReferenceFromUrl(oldUrl)
                                        .delete()
                                        .addOnSuccessListener {
                                            println("✅ Old wallpaper deleted successfully")
                                        }
                                        .addOnFailureListener { e ->
                                            println("⚠️ Failed to delete old wallpaper: ${e.message}")
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Failed to save to Firestore: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }.addOnFailureListener { e ->
                    isUploading = false
                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }




    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 🔹 SafePlay title (white text)
                        Text(
                            "SafePlay",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )


                        Spacer(Modifier.width(8.dp))

                        // 🔹 Animated logo inside circular Surface (same as your PIN screen)
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.06f),
                            tonalElevation = 0.dp
                        ) {
                            VideoLogo(
                                resId = R.raw.change_chat_background, // or change_chat_background if file exists
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFF121212) // matches SafePlay dark theme
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 🔹 Screen title
            Text(
                text = "Change Chat Background",
                style = MaterialTheme.typography.titleMedium,
                color = if (isDarkTheme) Color.White else Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AnimatedVisibility(visible = isUploading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Uploading background... ${(uploadProgress * 100).toInt()}%",
                        color = if (isDarkTheme) Color.White else Color.Black,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    AnimatedUploadBar(
                        progress = uploadProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }


// 🔹 🖼️ Then your current wallpaper preview (below the progress bar)
            if (customUri != null) {
                Text(
                    text = "Current Wallpaper",
                    color = if (isDarkTheme) Color.White else Color.Black,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    AsyncImage(
                        model = customUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }


            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 🟣 Default (theme) background item with live video
                item {
                    BackgroundVideoItem(
                        title = "Default",
                        videoRes = R.raw.default_wallpaper,
                        onClick = {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            if (uid == null) {
                                Toast.makeText(context, "Please log in first", Toast.LENGTH_SHORT).show()
                                return@BackgroundVideoItem
                            }

                            val db = FirebaseFirestore.getInstance()
                            val userDoc = db.collection("users").document(uid)

                            // 🔹 Fetch current background URL (if exists) to delete from storage
                            userDoc.get().addOnSuccessListener { snapshot ->
                                val oldUrl = snapshot.getString("backgroundUrl")

                                // 🔹 Delete the background URL field from Firestore
                                userDoc.update(mapOf("backgroundUrl" to FieldValue.delete()))
                                    .addOnSuccessListener {
                                        // 🔹 Clear local cache
                                        customUri = null
                                        clearSavedBackground(context)

                                        // 🔹 Remove old image from Firebase Storage if it existed
                                        if (!oldUrl.isNullOrEmpty()) {
                                            FirebaseStorage.getInstance().getReferenceFromUrl(oldUrl)
                                                .delete()
                                                .addOnSuccessListener {
                                                    println("✅ Old wallpaper deleted successfully")
                                                }
                                                .addOnFailureListener { e ->
                                                    println("⚠️ Failed to delete old wallpaper: ${e.message}")
                                                }
                                        }

                                        Toast.makeText(
                                            context,
                                            "Restored to default background",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(
                                            context,
                                            "Failed to reset: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                    )
                }


                // 🔵 Custom wallpaper item with live + icon video
                item {
                    BackgroundVideoItem(
                        title = "Custom wallpaper",
                        videoRes = R.raw.coustome_wallpaper, // 🔹 your animated "+" MP4 file
                        onClick = {
                            launcher.launch("image/*") // open gallery
                        }
                    )
                }
            }

        }
    }
}

@Composable
fun BackgroundPreviewItem(
    title: String,
    color: Color? = null,
    uri: Uri? = null,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier
                .size(140.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uri != null -> {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    color != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color)
                        )
                    }
                    else -> {
                        Icon(
                            painter = painterResource(R.drawable.safeplay_logo),
                            contentDescription = null,
                            tint = Color.Cyan,
                            modifier = Modifier
                                .size(60.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun BackgroundVideoItem(
    title: String,
    @RawRes videoRes: Int,
    onClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier
                .size(140.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F5F5)
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // ✅ Your live SafePlay MP4 video (looped, muted)
                VideoLogo(
                    resId = videoRes,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            title,
            color = if (isDarkTheme) Color.White else Color.Black,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedUploadBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    // 🟢 SafePlay accent color
    val accentColor = Color(0xFF00BCD4)

    // 🌈 Infinite shimmer + glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "uploadGlow")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerOffset"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            accentColor.copy(alpha = glowAlpha * 0.3f),
            accentColor.copy(alpha = 0.9f),
            accentColor.copy(alpha = glowAlpha * 0.3f)
        ),
        start = Offset(shimmerOffset - 200f, 0f),
        end = Offset(shimmerOffset + 200f, 0f)
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "uploadProgressAnim"
    )

    // 🔹 Shimmering glowing bar
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(
                Color.Gray.copy(alpha = 0.25f),
                RoundedCornerShape(50)
            )
    ) {
        Box(
            Modifier
                .fillMaxWidth(animatedProgress)
                .height(8.dp)
                .background(shimmerBrush, RoundedCornerShape(50))
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = accentColor.copy(alpha = glowAlpha * 0.6f),
                    spotColor = accentColor.copy(alpha = glowAlpha * 0.6f)
                )
        )
    }
}



