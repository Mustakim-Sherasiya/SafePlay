package com.chat.safeplay.setting.manager

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.chat.safeplay.R
import com.chat.safeplay.setting.manager.VideoLogo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.SetOptions
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.saveable.rememberSaveable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinChangeScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    val colorScheme = MaterialTheme.colorScheme

    var oldPin by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }

    var oldPinVisible by rememberSaveable { mutableStateOf(false) }
    var newPinVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPinVisible by rememberSaveable { mutableStateOf(false) }

    var storedPin by rememberSaveable { mutableStateOf<String?>(null) }
    var currentPinLength by rememberSaveable { mutableStateOf(4) } // default
    var newPinLength by rememberSaveable { mutableStateOf(4) }

    var biometricEnabled by rememberSaveable { mutableStateOf(false) }



//    var oldPin by remember { mutableStateOf("") }
//    var newPin by remember { mutableStateOf("") }
//    var confirmPin by remember { mutableStateOf("") }
//
//    var oldPinVisible by remember { mutableStateOf(false) }
//    var newPinVisible by remember { mutableStateOf(false) }
//    var confirmPinVisible by remember { mutableStateOf(false) }
//
//    var storedPin by remember { mutableStateOf<String?>(null) }
//    var currentPinLength by remember { mutableStateOf(4) } // default
//    var newPinLength by remember { mutableStateOf(4) }
//    // 🔹 Biometric toggle state
//    var biometricEnabled by remember { mutableStateOf(false) }


    // 🔹 Fetch current user's PIN and its length
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid)
                .get(Source.SERVER)
                .addOnSuccessListener { doc ->
                    val fetchedPin = doc.getString("pin")
                        ?: doc.getLong("pin")?.toString()
                        ?: doc.get("pin")?.toString()
                    val fetchedLength = (doc.getLong("pinLength") ?: fetchedPin?.length?.toLong() ?: 4).toInt()
                    if (fetchedPin != null) {
                        storedPin = fetchedPin.trim()
                        currentPinLength = fetchedLength
                        newPinLength = fetchedLength
                    }
                    // Fetch biometric setting (true/false) from Firestore if it exists
                    val biometricFlag = doc.getBoolean("biometricEnabled") ?: false
                    biometricEnabled = biometricFlag



                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load PIN info", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "SafePlay",
                            color = colorScheme.onBackground,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = colorScheme.onBackground.copy(alpha = 0.06f),
                            tonalElevation = 0.dp
                        ) {
                            VideoLogo(
                                resId = R.raw.pin_change,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = colorScheme.background
    ) { innerPadding ->
        // ✅ Add this line here — INSIDE the Scaffold lambda
        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding() // keeps content above keyboard
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(scrollState) // ✅ now recognized
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Change PIN",
                    color = colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(20.dp))

                // 🔹 Old PIN (restricted to current length)
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = {
                        if (it.length <= currentPinLength && it.all { c -> c.isDigit() }) oldPin =
                            it
                    },
                    label = { Text("Old PIN ($currentPinLength digits)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    visualTransformation = if (oldPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { oldPinVisible = !oldPinVisible }) {
                            Icon(
                                imageVector = if (oldPinVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 🔹 Select new PIN length (only 4 or 6)
                Text("Select New PIN Length", color = colorScheme.onBackground)
                Spacer(Modifier.height(6.dp))

                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("$newPinLength digits")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf(4, 6).forEach { length ->
                            DropdownMenuItem(
                                text = { Text("$length digits") },
                                onClick = {
                                    newPinLength = length
                                    newPin = ""
                                    confirmPin = ""
                                    expanded = false
                                    Toast.makeText(
                                        context,
                                        "Enter $length-digit PIN",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 🔹 New PIN field
                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        // Block invalid lengths and show helpful toasts
                        if (it.all { c -> c.isDigit() }) {
                            if (it.length <= newPinLength) {
                                newPin = it
                            } else if (newPinLength == 4 && it.length > 4) {
                                Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT)
                                    .show()
                            } else if (newPinLength == 6 && it.length in 5..5) {
                                Toast.makeText(context, "Enter 6-digit PIN", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    },
                    label = { Text("New PIN ($newPinLength digits)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    visualTransformation = if (newPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newPinVisible = !newPinVisible }) {
                            Icon(
                                imageVector = if (newPinVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 🔹 Confirm New PIN
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= newPinLength && it.all { c -> c.isDigit() }) confirmPin =
                            it
                    },
                    label = { Text("Confirm New PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    visualTransformation = if (confirmPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPinVisible = !confirmPinVisible }) {
                            Icon(
                                imageVector = if (confirmPinVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )


                // 🔹 Toggle for Biometric Authentication (between Change PIN and Need Help)
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Use Biometric for Login",
                        color = colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { enabled ->
                            biometricEnabled = enabled

                            // 🔹 Save to Firestore
                            currentUser?.uid?.let { uid ->
                                val updateData = mapOf("biometricEnabled" to enabled)
                                firestore.collection("users").document(uid)
                                    .set(updateData, SetOptions.merge())
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            context,
                                            if (enabled) "Biometric login enabled" else "Biometric login disabled",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            context,
                                            "Failed to update biometric setting",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }

                            // 🔹 Save locally too (for instant use in EnterPinScreen)
                            val prefs =
                                context.getSharedPreferences("safeplay_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("biometric_enabled", enabled).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF4CAF50),
                            uncheckedThumbColor = Color.Gray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))






                Spacer(modifier = Modifier.height(24.dp))

                // 🔘 Change PIN button
                Button(
                    onClick = {
                        if (storedPin == null) {
                            Toast.makeText(
                                context,
                                "Could not verify current PIN",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        if (oldPin != storedPin) {
                            Toast.makeText(context, "Old PIN is incorrect", Toast.LENGTH_SHORT)
                                .show()
                            return@Button
                        }

                        if (newPin != confirmPin) {
                            Toast.makeText(context, "New PINs do not match", Toast.LENGTH_SHORT)
                                .show()
                            return@Button
                        }

                        if (newPin.length != newPinLength) {
                            Toast.makeText(
                                context,
                                "PIN must be exactly $newPinLength digits",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        firestore.collection("users").document(currentUser!!.uid)
                            .set(
                                mapOf(
                                    "pin" to newPin,
                                    "pinLength" to newPinLength
                                ),
                                SetOptions.merge()
                            )
                            .addOnSuccessListener {
                                vibrateSuccess(context)
                                Toast.makeText(
                                    context,
                                    "PIN updated successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navController.popBackStack()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error updating PIN", Toast.LENGTH_SHORT)
                                    .show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Change PIN")
                }

                Spacer(modifier = Modifier.height(36.dp))
                SupportCard(context)
            }
        }
    }
}

@Composable
fun SupportCard(context: Context) {
    val colorScheme = MaterialTheme.colorScheme
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
                text = "If you're facing any issue resetting or changing PIN, contact us at:",
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
                text = AnnotatedString("safeplay@spysolution.in"),
                style = TextStyle(
                    color = Color(0xFF3B82F6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "message/rfc822"
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("safeplay@spysolution.in"))
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "Help with PIN Change  - SafePlay App"
                            )
                            putExtra(
                                Intent.EXTRA_TEXT,
                                """
                    Hello SafePlay Support,

                    I am facing an issue while Changing my PIN . Please assist me with the following details:
                    
                    (Describe your issue here or Send mail as is it)

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

        }
    }
}

@Suppress("MissingPermission")
private fun vibrateSuccess(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(80)
    }
}











//package com.safeplay.auth.pin
//
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.os.VibrationEffect
//import android.os.Vibrator
//import android.widget.Toast
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.Visibility
//import androidx.compose.material.icons.filled.VisibilityOff
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.PasswordVisualTransformation
//import androidx.compose.ui.text.input.VisualTransformation
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.chat.safeplay.setting.manager.VideoLogo
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.Source
//import com.google.firebase.firestore.SetOptions
//import com.chat.safeplay.R
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PinChangeScreen(navController: NavController) {
//    val context = LocalContext.current
//    val auth = FirebaseAuth.getInstance()
//    val firestore = FirebaseFirestore.getInstance()
//    val currentUser = auth.currentUser
//
//    var oldPin by remember { mutableStateOf("") }
//    var newPin by remember { mutableStateOf("") }
//    var confirmPin by remember { mutableStateOf("") }
//    var storedPin by remember { mutableStateOf<String?>(null) }
//
//    var oldPinVisible by remember { mutableStateOf(false) }
//    var newPinVisible by remember { mutableStateOf(false) }
//    var confirmPinVisible by remember { mutableStateOf(false) }
//
//    var pinLength by remember { mutableStateOf(4) } // Default 4 if user not found
//
//    // 🔹 Fetch stored PIN and detect its length
//    LaunchedEffect(currentUser?.uid) {
//        currentUser?.uid?.let { uid ->
//            firestore.collection("users").document(uid)
//                .get(Source.SERVER)
//                .addOnSuccessListener { doc ->
//                    val fetchedPin = doc.getString("pin")
//                        ?: doc.getLong("pin")?.toString()
//                        ?: doc.get("pin")?.toString()
//
//                    if (fetchedPin != null) {
//                        storedPin = fetchedPin.trim()
//                        pinLength = storedPin!!.length
//                    }
//                }
//                .addOnFailureListener {
//                    Toast.makeText(context, "Failed to load PIN info", Toast.LENGTH_SHORT).show()
//                }
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            SmallTopAppBar(
//                title = {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Text(
//                            "SafePlay",
//                            color = MaterialTheme.colorScheme.onBackground,
//                            fontSize = 19.sp,
//                            fontWeight = FontWeight.SemiBold
//                        )
//                        Spacer(Modifier.width(8.dp))
//                        Surface(
//                            modifier = Modifier.size(36.dp),
//                            shape = CircleShape,
//                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
//                            tonalElevation = 0.dp
//                        ) {
//                            VideoLogo(
//                                resId = R.raw.pass_change,
//                                modifier = Modifier.fillMaxSize()
//                            )
//                        }
//                    }
//                },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(
//                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                            contentDescription = "Back",
//                            tint = MaterialTheme.colorScheme.onBackground
//                        )
//                    }
//                },
//                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent)
//            )
//        },
//        containerColor = MaterialTheme.colorScheme.background
//    ) { innerPadding ->
//        Column(
//            modifier = Modifier
//                .padding(innerPadding)
//                .fillMaxSize()
//                .padding(horizontal = 24.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Spacer(modifier = Modifier.height(20.dp))
//            Text(
//                text = "Change PIN",
//                color = MaterialTheme.colorScheme.onBackground,
//                fontSize = 18.sp,
//                fontWeight = FontWeight.SemiBold
//            )
//            Spacer(modifier = Modifier.height(20.dp))
//
//            // 🔹 Old PIN
//            OutlinedTextField(
//                value = oldPin,
//                onValueChange = {
//                    if (it.length <= pinLength && it.all { c -> c.isDigit() }) oldPin = it
//                },
//                label = { Text("Old PIN ($pinLength digits)") },
//                singleLine = true,
//                visualTransformation = if (oldPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
//                trailingIcon = {
//                    IconButton(onClick = { oldPinVisible = !oldPinVisible }) {
//                        Icon(
//                            imageVector = if (oldPinVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
//                            contentDescription = null
//                        )
//                    }
//                },
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // 🔹 New PIN
//            OutlinedTextField(
//                value = newPin,
//                onValueChange = {
//                    if (it.length <= pinLength && it.all { c -> c.isDigit() }) newPin = it
//                },
//                label = { Text("New PIN ($pinLength digits)") },
//                singleLine = true,
//                visualTransformation = if (newPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
//                trailingIcon = {
//                    IconButton(onClick = { newPinVisible = !newPinVisible }) {
//                        Icon(
//                            imageVector = if (newPinVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
//                            contentDescription = null
//                        )
//                    }
//                },
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // 🔹 Confirm New PIN
//            OutlinedTextField(
//                value = confirmPin,
//                onValueChange = {
//                    if (it.length <= pinLength && it.all { c -> c.isDigit() }) confirmPin = it
//                },
//                label = { Text("Confirm New PIN") },
//                singleLine = true,
//                visualTransformation = if (confirmPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
//                trailingIcon = {
//                    IconButton(onClick = { confirmPinVisible = !confirmPinVisible }) {
//                        Icon(
//                            imageVector = if (confirmPinVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
//                            contentDescription = null
//                        )
//                    }
//                },
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            Button(
//                onClick = {
//                    if (storedPin == null) {
//                        Toast.makeText(context, "Could not verify current PIN", Toast.LENGTH_SHORT).show()
//                        return@Button
//                    }
//
//                    if (oldPin != storedPin) {
//                        Toast.makeText(context, "Old PIN is incorrect", Toast.LENGTH_SHORT).show()
//                        return@Button
//                    }
//
//                    if (newPin != confirmPin) {
//                        Toast.makeText(context, "New PIN and confirmation do not match", Toast.LENGTH_SHORT).show()
//                        return@Button
//                    }
//
//                    firestore.collection("users").document(currentUser!!.uid)
//                        .set(mapOf("pin" to newPin), SetOptions.merge())
//                        .addOnSuccessListener {
//                            vibrateSuccess(context)
//                            Toast.makeText(context, "PIN updated successfully", Toast.LENGTH_SHORT).show()
//                            navController.popBackStack()
//                        }
//                        .addOnFailureListener {
//                            Toast.makeText(context, "Error updating PIN", Toast.LENGTH_SHORT).show()
//                        }
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(50.dp),
//                shape = RoundedCornerShape(12.dp)
//            ) {
//                Text("Change PIN")
//            }
//
//            Spacer(modifier = Modifier.height(36.dp))
//            SupportCard(context)
//        }
//    }
//}
//
//@Composable
//fun SupportCard(context: Context) {
//    val colorScheme = MaterialTheme.colorScheme
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable {
//                val intent = Intent(Intent.ACTION_SEND).apply {
//                    type = "message/rfc822"
//                    putExtra(Intent.EXTRA_EMAIL, arrayOf("safeplay.users.info@gmail.com"))
//                    putExtra(Intent.EXTRA_SUBJECT, "SafePlay PIN Change Help")
//                    putExtra(
//                        Intent.EXTRA_TEXT, """
//                        Hello SafePlay Support,
//
//                        I need help changing my PIN.
//
//                        Thank you,
//                        SafePlay User
//                        """.trimIndent()
//                    )
//                }
//                context.startActivity(Intent.createChooser(intent, "Contact SafePlay Support"))
//            },
//        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
//        shape = RoundedCornerShape(16.dp)
//    ) {
//        Box(
//            modifier = Modifier.padding(20.dp),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                "Need help? Contact SafePlay Support",
//                style = MaterialTheme.typography.bodyMedium,
//                color = colorScheme.onSurface
//            )
//        }
//    }
//}
//
//@Suppress("MissingPermission")
//private fun vibrateSuccess(context: Context) {
//    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
//    } else {
//        @Suppress("DEPRECATION")
//        vibrator.vibrate(80)
//    }
//}
//
