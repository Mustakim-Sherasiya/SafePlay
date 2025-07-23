package com.chat.safeplay

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.Alignment
// Import your PIN reset nav graph here
import com.chat.safeplay.navigation.PinNavGraph
import com.google.firebase.firestore.FirebaseFirestore

// Import your PIN storage helper (implement separately)

@Composable
fun BeforeLoginNavGraph(
    navController: NavHostController,
    startDestination: String,
    auth: FirebaseAuth,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lastVerificationEmailSentTime = remember { mutableStateOf(0L) }
    val lastResetEmailSentTime = remember { mutableStateOf(0L) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("home") { HomeScreen(navController) }

        composable("gameSelection") { GameSelectionScreen(navController) }

        composable("login") {
            LoginScreen(
                onLoginClick = { input, password ->
                    val auth = FirebaseAuth.getInstance()
                    auth.signInWithEmailAndPassword(input, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null) {
                                    if (user.isEmailVerified) {
                                        // Always go to enterPin screen; Firestore PIN will be fetched there
                                        navController.navigate("enterPin") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        val now = System.currentTimeMillis()
                                        if (now - lastVerificationEmailSentTime.value > 60_000) {
                                            user.sendEmailVerification()
                                                .addOnCompleteListener { emailTask ->
                                                    if (emailTask.isSuccessful) {
                                                        Toast.makeText(
                                                            context,
                                                            "Verification email sent. Please check your inbox.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        lastVerificationEmailSentTime.value = now
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Failed to send verification email.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                        } else {
                                            val secondsLeft = (60_000 - (now - lastVerificationEmailSentTime.value)) / 1000
                                            Toast.makeText(
                                                context,
                                                "Please check your email. You can resend verification in $secondsLeft seconds.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        auth.signOut()
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Login failed: ${task.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                },
                onCreateAccountClick = {
                    navController.navigate("createAccount")
                },
                onForgotPasswordClick = { emailOrPhone ->
                    navController.navigate("forgotPassword")
                },
                navigateToPhoneOtpScreen = { phoneNumber ->
                    navController.navigate("phoneOtp/$phoneNumber")
                }
            )
        }



        composable("forgotPassword") {
            val context = LocalContext.current
            ForgotPasswordScreen(
                lastSentTimeMillis = lastResetEmailSentTime.value,
                onUpdateLastSentTime = { newTime -> lastResetEmailSentTime.value = newTime },
                onResetClick = { email ->
                    val auth = FirebaseAuth.getInstance()
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Password reset email sent. Check your inbox.", Toast.LENGTH_LONG).show()
                                lastResetEmailSentTime.value = System.currentTimeMillis()
                            } else {
                                Toast.makeText(context, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("createAccount") {
            CreateAccountScreen(
                onCreateAccountClick = { email, phone, password ->
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                user?.sendEmailVerification()
                                    ?.addOnCompleteListener { emailTask ->
                                        if (emailTask.isSuccessful) {
                                            Toast.makeText(
                                                context,
                                                "Account created. Please verify your email.",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            // Clear any cached PIN here
                                            PinStorageHelper.clearPin(context)

                                            auth.signOut()

                                            navController.navigate("login") {
                                                popUpTo("createAccount") { inclusive = true }
                                            }
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to send verification email.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error: ${task.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                },
                onBackToLoginClick = {
                    navController.popBackStack()
                }
            )
        }



        composable("phoneOtp/{phoneNumber}") { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            PhoneOtpScreen(phoneNumber = phoneNumber, navController = navController, auth = auth)
        }

        // NEW ROUTE for creating PIN after first login
        composable("createPin") {
            CreatePinScreen(
                navController = navController,  // <--- FIXED HERE by adding navController param
                onPinCreated = { pin, pinLength, autoSubmit ->
                    PinStorageHelper.savePin(context, pin, pinLength, autoSubmit)
                    Toast.makeText(context, "PIN set successfully", Toast.LENGTH_SHORT).show()
                    navController.navigate("home") {
                        popUpTo("createPin") { inclusive = true }
                    }
                }
            )
        }

        // NEW ROUTE for entering PIN on app start
        composable("enterPin") {
            val context = LocalContext.current
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser

            var isLoading by remember { mutableStateOf(true) }
            var hasPin by remember { mutableStateOf(false) }
            var pinFromFirestore by remember { mutableStateOf<String?>(null) }
            var pinLengthFromFirestore by remember { mutableStateOf(4) }
            var autoSubmitFromFirestore by remember { mutableStateOf(true) }

            LaunchedEffect(user?.uid) {
                if (user?.uid != null) {
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("users").document(user.uid).get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val fetchedPin = document.getString("pin")
                                val fetchedPinLength = document.getLong("pinLength")?.toInt() ?: 4
                                val fetchedAutoSubmit = document.getBoolean("autoSubmit") ?: true
                                if (fetchedPin != null) {
                                    hasPin = true
                                    pinFromFirestore = fetchedPin
                                    pinLengthFromFirestore = fetchedPinLength
                                    autoSubmitFromFirestore = fetchedAutoSubmit
                                }
                            }
                            isLoading = false
                        }
                        .addOnFailureListener {
                            isLoading = false
                        }
                } else {
                    isLoading = false
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (!hasPin) {
                    // No PIN in Firestore → Navigate to createPin
                    LaunchedEffect(Unit) {
                        navController.navigate("createPin") {
                            popUpTo("enterPin") { inclusive = true }
                        }
                    }
                } else {
                    // PIN found in Firestore → Go to EnterPin screen
                    EnterPinScreen(
                        correctPin = pinFromFirestore ?: "",
                        pinLength = pinLengthFromFirestore,
                        autoSubmit = autoSubmitFromFirestore,
                        onPinVerified = {
                            navController.navigate("userDashboard") {
                                popUpTo("enterPin") { inclusive = true }
                            }
                        },
                        onForgotPinClick = {
                            navController.navigate("forgotPin")
                        },
                        navController = navController
                    )
                }

            }

        }

        // ... your existing composable("enterPin") { ... }

        composable("UserDashboard") {
            UserDashboardScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo("UserDashboard") { inclusive = true }
                    }
                },
                onChatSelected = { selectedUser ->
                    Toast.makeText(context, "Chat with ${selectedUser.name} tapped", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // *** ADD PIN RESET NAV GRAPH ***
        composable("forgotPin") {
            PinNavGraph(navController = navController)
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = { navController.navigate("home") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Games, contentDescription = "Games") },
            label = { Text("Games") },
            selected = currentRoute == "gameSelection",
            onClick = { navController.navigate("gameSelection") }
        )
    }
}












//--------------------------------------------------------------------------------------------------------
//=========================================================================================================
//package com.chat.safeplay
//
//import android.widget.Toast
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Games
//import androidx.compose.material.icons.filled.Home
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.currentBackStackEntryAsState
//import com.google.firebase.auth.FirebaseAuth
//
//// Import your PIN storage helper (implement separately)
//
//@Composable
//fun BeforeLoginNavGraph(
//    navController: NavHostController,
//    startDestination: String,
//    auth: FirebaseAuth,
//    modifier: Modifier = Modifier
//) {
//    val context = LocalContext.current
//    val lastVerificationEmailSentTime = remember { mutableStateOf(0L) }
//    val lastResetEmailSentTime = remember { mutableStateOf(0L) }
//
//    NavHost(
//        navController = navController,
//        startDestination = startDestination,
//        modifier = modifier
//    ) {
//        composable("home") { HomeScreen(navController) }
//
//        composable("gameSelection") { GameSelectionScreen(navController) }
//
//        composable("login") {
//            LoginScreen(
//                onLoginClick = { input, password ->
//                    val auth = FirebaseAuth.getInstance()
//                    auth.signInWithEmailAndPassword(input, password)
//                        .addOnCompleteListener { task ->
//                            if (task.isSuccessful) {
//                                val user = auth.currentUser
//                                if (user != null) {
//                                    if (user.isEmailVerified) {
//                                        val savedPin = PinStorageHelper.getPin(context)
//                                        if (savedPin == null) {
//                                            navController.navigate("createPin") {
//                                                popUpTo("login") { inclusive = true }
//                                            }
//                                        } else {
//                                            navController.navigate("enterPin") { // ✅ go to PIN screen
//                                                popUpTo("login") { inclusive = true }
//                                            }
//                                        }
//                                    } else {
//                                        val now = System.currentTimeMillis()
//                                        if (now - lastVerificationEmailSentTime.value > 60_000) {
//                                            user.sendEmailVerification()
//                                                .addOnCompleteListener { emailTask ->
//                                                    if (emailTask.isSuccessful) {
//                                                        Toast.makeText(context, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show()
//                                                        lastVerificationEmailSentTime.value = now
//                                                    } else {
//                                                        Toast.makeText(context, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
//                                                    }
//                                                }
//                                        } else {
//                                            val secondsLeft = (60_000 - (now - lastVerificationEmailSentTime.value)) / 1000
//                                            Toast.makeText(context, "Please check your email. You can resend verification in $secondsLeft seconds.", Toast.LENGTH_LONG).show()
//                                        }
//                                        auth.signOut()
//                                    }
//                                }
//                            } else {
//                                Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
//                            }
//                        }
//                },
//                onCreateAccountClick = {
//                    navController.navigate("createAccount")
//                },
//                onForgotPasswordClick = { emailOrPhone ->
//                    navController.navigate("forgotPassword")
//                },
//                navigateToPhoneOtpScreen = { phoneNumber ->
//                    navController.navigate("phoneOtp/$phoneNumber")
//                }
//            )
//        }
//
//
//        composable("forgotPassword") {
//            val context = LocalContext.current
//            ForgotPasswordScreen(
//                lastSentTimeMillis = lastResetEmailSentTime.value,
//                onUpdateLastSentTime = { newTime -> lastResetEmailSentTime.value = newTime },
//                onResetClick = { email ->
//                    val auth = FirebaseAuth.getInstance()
//                    auth.sendPasswordResetEmail(email)
//                        .addOnCompleteListener { task ->
//                            if (task.isSuccessful) {
//                                Toast.makeText(context, "Password reset email sent. Check your inbox.", Toast.LENGTH_LONG).show()
//                                lastResetEmailSentTime.value = System.currentTimeMillis()
//                            } else {
//                                Toast.makeText(context, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
//                            }
//                        }
//                },
//                onBackClick = {
//                    navController.popBackStack()
//                }
//            )
//        }
//
//        composable("createAccount") {
//            CreateAccountScreen(
//                onCreateAccountClick = { email, phone, password ->
//                    auth.createUserWithEmailAndPassword(email, password)
//                        .addOnCompleteListener { task ->
//                            if (task.isSuccessful) {
//                                val user = auth.currentUser
//                                user?.sendEmailVerification()
//                                    ?.addOnCompleteListener { emailTask ->
//                                        if (emailTask.isSuccessful) {
//                                            Toast.makeText(
//                                                context,
//                                                "Account created. Please verify your email.",
//                                                Toast.LENGTH_LONG
//                                            ).show()
//
//                                            auth.signOut()
//
//                                            navController.navigate("login") {
//                                                popUpTo("createAccount") { inclusive = true }
//                                            }
//                                        } else {
//                                            Toast.makeText(
//                                                context,
//                                                "Failed to send verification email.",
//                                                Toast.LENGTH_SHORT
//                                            ).show()
//                                        }
//                                    }
//                            } else {
//                                Toast.makeText(
//                                    context,
//                                    "Error: ${task.exception?.message}",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            }
//                        }
//                },
//                onBackToLoginClick = {
//                    navController.popBackStack()
//                }
//            )
//        }
//
//        composable("phoneOtp/{phoneNumber}") { backStackEntry ->
//            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
//            PhoneOtpScreen(phoneNumber = phoneNumber, navController = navController, auth = auth)
//        }
//
//        // NEW ROUTE for creating PIN after first login
//        composable("createPin") {
//            CreatePinScreen(
//                navController = navController,  // <--- FIXED HERE by adding navController param
//                onPinCreated = { pin, pinLength, autoSubmit ->
//                    PinStorageHelper.savePin(context, pin, pinLength, autoSubmit)
//                    Toast.makeText(context, "PIN set successfully", Toast.LENGTH_SHORT).show()
//                    navController.navigate("home") {
//                        popUpTo("createPin") { inclusive = true }
//                    }
//                }
//            )
//        }
//
//        // NEW ROUTE for entering PIN on app start
//        composable("enterPin") {
//            val savedPin = PinStorageHelper.getPin(context)
//            val savedPinLength = PinStorageHelper.getPinLength(context)
//            val savedAutoSubmit = PinStorageHelper.getAutoSubmit(context)
//
//            if (savedPin == null) {
//                LaunchedEffect(Unit) {
//                    navController.navigate("createPin") {
//                        popUpTo("enterPin") { inclusive = true }
//                    }
//                }
//            } else {
//                EnterPinScreen(
//                    correctPin = savedPin,
//                    pinLength = savedPinLength,
//                    autoSubmit = savedAutoSubmit,
//                    onPinVerified = {
//                        navController.navigate("UserDashboard") {
//                            popUpTo("enterPin") { inclusive = true }
//                        }
//                    },
//                    onForgotPinClick = {
//                        Toast.makeText(
//                            context,
//                            "Reset PIN link sent to your email.",
//                            Toast.LENGTH_LONG
//                        ).show()
//                        // TODO: Implement real PIN reset email logic here
//                    },
//                    navController = navController  // ✅ Fixed!
//                )
//
//            }
//        }
//        // ... your existing composable("enterPin") { ... }
//
//        composable("UserDashboard") {
//            UserDashboardScreen(
//                onLogout = {
//                    FirebaseAuth.getInstance().signOut()
//                    navController.navigate("login") {
//                        popUpTo("UserDashboard") { inclusive = true }
//                    }
//                },
//                onChatSelected = { selectedUser ->
//                    Toast.makeText(context, "Chat with ${selectedUser.name} tapped", Toast.LENGTH_SHORT).show()
//                }
//            )
//        }
//
//
//    }
//}
//
//@Composable
//fun BottomNavigationBar(navController: NavHostController) {
//    val navBackStackEntry by navController.currentBackStackEntryAsState()
//    val currentRoute = navBackStackEntry?.destination?.route
//
//    NavigationBar {
//        NavigationBarItem(
//            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
//            label = { Text("Home") },
//            selected = currentRoute == "home",
//            onClick = { navController.navigate("home") }
//        )
//        NavigationBarItem(
//            icon = { Icon(Icons.Filled.Games, contentDescription = "Games") },
//            label = { Text("Games") },
//            selected = currentRoute == "gameSelection",
//            onClick = { navController.navigate("gameSelection") }
//        )
//    }
//}

















