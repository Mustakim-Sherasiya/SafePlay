package com.chat.safeplay.setting.manager

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardType.Companion.Uri
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.chat.safeplay.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.*






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordResetScreen(
    navController: NavHostController,
    sendResetEmail: (String, (Boolean, String?) -> Unit) -> Unit = { email, cb ->
        try {
            val auth = FirebaseAuth.getInstance()
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) cb(true, null)
                    else cb(false, task.exception?.message ?: "Failed")
                }
        } catch (e: Exception) {
            cb(false, e.message ?: "Error sending reset")
        }
    }
) {
    val ctx = LocalContext.current
    var email by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

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
                                resId = R.raw.pass_change, // use your top-bar video file
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent)
            )
        },

    containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .background(Color(0xFF121212)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Change Password",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Note:", color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "A password reset link will be sent to your email. If you haven't received it, please check your spam folder.",
                        color = Color(0xFFBEBEBE),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3A3A3A),
                    unfocusedBorderColor = Color(0xFF3A3A3A),
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (email.isBlank()) {
                        Toast.makeText(ctx, "Enter your email", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    loading = true
                    sendResetEmail(email) { success, errMsg ->
                        loading = false
                        if (success) {
                            Toast.makeText(
                                ctx,
                                "Reset link sent. Check your email.",
                                Toast.LENGTH_LONG
                            ).show()
                            email = "" // ✅ clear field after sending
                        } else {
                            Toast.makeText(
                                ctx,
                                "Failed: ${errMsg ?: "unknown"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sending...", color = Color.White)
                } else {
                    Text("Send Reset Link", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "If you don't get the email, check spam or try again after a few minutes.",
                color = Color(0xFF9A9A9A),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                shape = RoundedCornerShape(10.dp)
            ) {
                val context = LocalContext.current

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Need Help?",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "If you're facing any issue resetting your password, contact us at:",
                        color = Color(0xFFBEBEBE),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // ✅ Fixed clickable email link
                    val context = LocalContext.current
                    val auth = FirebaseAuth.getInstance()
                    val firestore = FirebaseFirestore.getInstance()

                    val currentUser = auth.currentUser
                    var userEmail by remember { mutableStateOf(currentUser?.email ?: "Unknown") }
                    var userPhone by remember { mutableStateOf("Unknown") }
                    var userPublicId by remember { mutableStateOf("Unknown") }
                    var userUid by remember { mutableStateOf(currentUser?.uid ?: "Unknown") }

// 🔹 Fetch extra info (publicId, phone) from Firestore
                    LaunchedEffect(currentUser?.uid) {
                        currentUser?.uid?.let { uid ->
                            firestore.collection("users").document(uid).get()
                                .addOnSuccessListener { doc ->
                                    userPhone = doc.getString("phone") ?: "Unknown"
                                    userPublicId = doc.getString("publicId") ?: "Unknown"
                                }
                        }
                    }

                    ClickableText(
                        text = AnnotatedString("safeplay.users.info@gmail.com"),
                        style = TextStyle(
                            color = Color(0xFF3B82F6),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "message/rfc822"
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("safeplay.users.info@gmail.com"))
                                    putExtra(Intent.EXTRA_SUBJECT, "Help with Password Reset - SafePlay App")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        """
                    Hello SafePlay Support,

                    I am facing an issue while resetting my password. Please assist me with the following details:
                    
                    (Describe your issue here or Send mail as is)

                    -------------------------
                    🧾 User Info:
                    UID: $userUid
                    Public ID: $userPublicId
                    Email: $userEmail
                    Phone: $userPhone
                    -------------------------

                    Thank you,
                    SafePlay User
                    """.trimIndent()
                                    )
                                }
                                context.startActivity(Intent.createChooser(intent, "Send email via..."))
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )





//                    ClickableText(
//                        text = AnnotatedString("safeplay.users.info@gmail.com"),
//                        style = TextStyle(
//                            color = Color(0xFF3B82F6),
//                            fontSize = 14.sp,
//                            fontWeight = FontWeight.Medium
//                        ),
//                        onClick = {
//                            try {
//                                val intent = Intent(Intent.ACTION_SENDTO).apply {
//                                    // ✅ This uses android.net.Uri
//                                    data = android.net.Uri.parse("mailto:safeplay.users.info@gmail.com")
//
//                                }
//                                context.startActivity(intent)
//                            } catch (e: Exception) {
//                                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                    )


                }
            }




        }
    }
}



//------------------------------   Working till sned mails not auto clear --------------//
//    // PasswordResetScreen.kt
//    package com.chat.safeplay.setting.manager
//
//
//    import android.widget.Toast
//    import androidx.compose.foundation.background
//    import androidx.compose.foundation.layout.*
//    import androidx.compose.foundation.shape.RoundedCornerShape
//    import androidx.compose.foundation.text.KeyboardOptions
//    import androidx.compose.material.icons.Icons
//    import androidx.compose.material.icons.filled.ArrowBack
//    import androidx.compose.material3.*
//    import androidx.compose.runtime.*
//    import androidx.compose.ui.Alignment
//    import androidx.compose.ui.Modifier
//    import androidx.compose.ui.draw.clip
//    import androidx.compose.ui.graphics.Color
//    import androidx.compose.ui.platform.LocalContext
//    import androidx.compose.ui.text.input.KeyboardType
//    import androidx.compose.ui.text.input.PasswordVisualTransformation
//    import androidx.compose.ui.unit.dp
//    import androidx.compose.ui.unit.sp
//    import androidx.navigation.NavHostController
//    import com.google.firebase.auth.FirebaseAuth
//
//    @OptIn(ExperimentalMaterial3Api::class)
//    @Composable
//    fun PasswordResetScreen(
//        navController: NavHostController,
//        sendResetEmail: (String, (Boolean, String?) -> Unit) -> Unit = { email, cb ->
//            // Default implementation: uses Firebase Auth if available
//            try {
//                val auth = FirebaseAuth.getInstance()
//                auth.sendPasswordResetEmail(email)
//                    .addOnCompleteListener { task ->
//                        if (task.isSuccessful) cb(true, null)
//                        else cb(false, task.exception?.message ?: "Failed")
//                    }
//            } catch (e: Exception) {
//                // If Firebase not present or fails, return error via callback
//                cb(false, e.message ?: "Error sending reset")
//            }
//        }
//    ) {
//        val ctx = LocalContext.current
//        var email by remember { mutableStateOf("") }
//        var loading by remember { mutableStateOf(false) }
//
//        Scaffold(
//            topBar = {
//                SmallTopAppBar(
//                    title = { Text(text = "Change Password", color = Color.White, fontSize = 18.sp) },
//                    navigationIcon = {
//                        IconButton(onClick = { navController.popBackStack() }) {
//                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
//                        }
//                    },
//                    colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent)
//                )
//            },
//            containerColor = Color(0xFF121212)
//        ) { padding ->
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(padding)
//                    .padding(horizontal = 20.dp, vertical = 16.dp)
//                    .background(Color(0xFF121212)),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Note box (dark rounded card)
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 8.dp),
//                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
//                    shape = RoundedCornerShape(8.dp)
//                ) {
//                    Column(modifier = Modifier.padding(16.dp)) {
//                        Text(
//                            text = "Note:",
//                            color = Color.White,
//                            fontSize = 14.sp
//                        )
//                        Spacer(modifier = Modifier.height(6.dp))
//                        Text(
//                            text = "A password reset link will be sent to your email. If you haven't received it, please check your spam folder.",
//                            color = Color(0xFFBEBEBE),
//                            fontSize = 13.sp
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(20.dp))
//
//                // Email input
//                OutlinedTextField(
//                    value = email,
//                    onValueChange = { email = it },
//                    label = { Text("Email") },
//                    singleLine = true,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(56.dp),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedBorderColor = Color(0xFF3A3A3A),
//                        unfocusedBorderColor = Color(0xFF3A3A3A),
//                        focusedContainerColor = Color(0xFF1E1E1E),
//                        unfocusedContainerColor = Color(0xFF1E1E1E),
//                        focusedTextColor = Color.White,
//                        unfocusedTextColor = Color.White,
//                        cursorColor = Color.White
//                    ),
//                    keyboardOptions = KeyboardOptions(
//                        keyboardType = KeyboardType.Email
//                    )
//                )
//
//
//                Spacer(modifier = Modifier.height(20.dp))
//
//                // Send button
//                Button(
//                    onClick = {
//                        if (email.isBlank()) {
//                            Toast.makeText(ctx, "Enter your email", Toast.LENGTH_SHORT).show()
//                            return@Button
//                        }
//                        loading = true
//                        // call the provided sendResetEmail (default uses Firebase)
//                        sendResetEmail(email) { success, errMsg ->
//                            loading = false
//                            if (success) {
//                                Toast.makeText(ctx, "Reset link sent. Check your email.", Toast.LENGTH_LONG).show()
//                                // optionally navigate back
//                                // navController.popBackStack()
//                            } else {
//                                Toast.makeText(ctx, "Failed: ${errMsg ?: "unknown"}", Toast.LENGTH_LONG).show()
//                            }
//                        }
//                    },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(48.dp)
//                        .clip(RoundedCornerShape(24.dp)),
//                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
//                ) {
//                    if (loading) {
//                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("Sending...", color = Color.White)
//                    } else {
//                        Text("Send Reset link", color = Color.White)
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Optional small helper text
//                Text(
//                    text = "If you don't get the email, check spam or try again after a few minutes.",
//                    color = Color(0xFF9A9A9A),
//                    fontSize = 12.sp,
//                    modifier = Modifier.padding(horizontal = 6.dp)
//                )
//            }
//        }
//    }
