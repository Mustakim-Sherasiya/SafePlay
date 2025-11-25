
package com.chat.safeplay

import GameSelectionScreen
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

import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.Alignment
// Import your PIN reset nav graph here
import com.chat.safeplay.navigation.PinNavGraph
import com.google.firebase.firestore.FirebaseFirestore
import com.chat.safeplay.navigation.AdminNavGraph
import com.chat.safeplay.profile.ProfileRoutes
import com.chat.safeplay.profile.ProfileScreen
import com.chat.safeplay.chat.handler.ChatScreen
import com.chat.safeplay.setting.manager.SettingRoutes
import com.chat.safeplay.setting.manager.SettingsNavGraph
import androidx.compose.material3.ExperimentalMaterial3Api
import com.google.accompanist.navigation.animation.composable
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.chat.safeplay.star.messages.StarredMessagesScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.chat.safeplay.chat.handler.ChatRoutes


// Import your PIN storage helper (implement separately)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
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

    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        // 👇 Optional default transitions for all screens
        enterTransition = {
            fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300))
        }
    ) {
        composable("home") {
            HomeScreen(navController)
        }

        composable("gameSelection") {
            GameSelectionScreen(navController)
        }




//----------------------------- IF ANY ERROR COMES ANYWHERE REMOVE THIS UNDER BLOCK AND UN COMMENT ABOVE BLOCK-----------------------------//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun BeforeLoginNavGraph(
//    navController: NavHostController,
//    startDestination: String = "splash", // 👈 default start now splash
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
//        // 👇 Splash route first
//        composable("splash") {
//            SplashScreen(navController)
//        }
//
//        // 👇 Your normal app routes
//        composable("home") { HomeScreen(navController) }
//
//        composable("gameSelection") { GameSelectionScreen(navController) }
//

//------------------------------------------- TILL HERE   ----------------------------------------------------//

        composable("login") {
            LoginScreen(
                onLoginClick = { input, password ->
                    val auth = FirebaseAuth.getInstance()
                    val firestore = FirebaseFirestore.getInstance()

                    auth.signInWithEmailAndPassword(input, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null) {
                                    val uid = user.uid
                                    firestore.collection("users").document(uid).get()
                                        .addOnSuccessListener { document ->
                                            val role = document.getString("role")
                                            if (role == "admin") {
                                                // 🔥 Admin doesn't require email verification or PIN
                                                navController.navigate("adminGraph") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            } else {
                                                // ✅ Normal user must verify email
                                                //Auto login when logen in if not work then uncomment below line and delete this block

                                                if (user.isEmailVerified) {
                                                    LocalStorage.saveLogin(context.applicationContext, input, password)
                                                    // ✅ Force Firestore reload before navigating
                                                    val firestore = FirebaseFirestore.getInstance()
                                                    firestore.collection("users").document(user.uid).get()
                                                        .addOnSuccessListener { document ->
                                                            if (document.exists() && document.getString("pin") != null) {
                                                                // ✅ Pin already exists → Go to enterPin
                                                                navController.navigate("enterPin") {
                                                                    popUpTo("login") { inclusive = true }
                                                                }
                                                            } else {
                                                                // 🆕 No pin yet → Go to createPin
                                                                navController.navigate("createPin") {
                                                                    popUpTo("login") { inclusive = true }
                                                                }
                                                            }
                                                        }
                                                        .addOnFailureListener {
                                                            Toast.makeText(context, "Failed to fetch PIN info.", Toast.LENGTH_SHORT).show()
                                                            navController.navigate("enterPin") // fallback safe path
                                                        }
                                                }


//                                                if (user.isEmailVerified) {
//                                                    navController.navigate("enterPin") {
//                                                        popUpTo("login") { inclusive = true }
//                                                    }
//                                                }

                                                  else {
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
                                                                        "Please verify your email from your main through send Link, See SPAM FOLDER.",
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
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Failed to fetch user role.", Toast.LENGTH_SHORT).show()
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
                }
                ,
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
        composable("adminGraph") {
            AdminNavGraph(navController) // pass parentNavController if you want to navigate outside admin
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
                    // create user in Firebase Auth first
                    auth.createUserWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null) {
                                    // Step 1: reserve a unique short publicId for this user
                                    ensureUniquePublicId(onResult = { success, publicId ->
                                        if (!success || publicId == null) {
                                            // Failed to get a unique id — show error and delete auth user to avoid orphan account.
                                            FirebaseFirestore.getInstance() // ensure instance
                                            Toast.makeText(context, "Failed to generate user id. Try again.", Toast.LENGTH_LONG).show()
                                            // Clean up the created auth user to avoid auth-only accounts
                                            user.delete().addOnCompleteListener { /* ignore result */ }
                                            return@ensureUniquePublicId
                                        }

                                        // Step 2: Save user details in Firestore under users/{uid}
                                        val firestore = FirebaseFirestore.getInstance()
                                        val userData = hashMapOf(
                                            "email" to email.trim(),
                                            "phone" to phone.trim(),
                                            "role" to "user",
                                            "publicId" to publicId,
                                            "createdAt" to System.currentTimeMillis()
                                        )

                                        firestore.collection("users").document(user.uid)
                                            .set(userData)
                                            .addOnSuccessListener {
                                                // After saving → send verification email
                                                user.sendEmailVerification()
                                                    .addOnCompleteListener { emailTask ->
                                                        if (emailTask.isSuccessful) {
                                                            Toast.makeText(
                                                                context,
                                                                "Account created. Please verify your email.",
                                                                Toast.LENGTH_LONG
                                                            ).show()

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
                                            }
                                            .addOnFailureListener { e ->
                                                // Firestore save failed — clean up to avoid inconsistent state
                                                Toast.makeText(context, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()
                                                user.delete().addOnCompleteListener { /* ignore result */ }
                                            }
                                    })
                                } else {
                                    Toast.makeText(context, "User creation failed (null user).", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error: ${task.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                },
                onBackToLoginClick = {
                    navController.popBackStack()
                }
            )
        }

//        composable("createAccount") {
//            CreateAccountScreen(
//                onCreateAccountClick = { _, _, _ ->
//                    // 🔥 Account creation and Firestore write now happen in CreateAccountScreen via verifyPhoneOtp (AuthUtils)
//                    // No need to call createUserWithEmailAndPassword here again
//                },
//                onBackToLoginClick = {
//                    navController.popBackStack()
//                }
//            )
//        }







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


        // *** ADD PIN RESET NAV GRAPH ***
        composable("forgotPin") {
            PinNavGraph(navController = navController)

        }


        composable(
            route = "UserDashboard",
            enterTransition = {
                // When returning from Settings → Dashboard
                slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(400)) +
                        fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                // When leaving Dashboard → Settings
                slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(400)) +
                        fadeOut(animationSpec = tween(400))
            },
            popEnterTransition = {
                // When coming back (pop) into Dashboard
                slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(400)) +
                        fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                // When going out from Dashboard backwards (if needed)
                slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(400)) +
                        fadeOut(animationSpec = tween(400))
            }
        ) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val currentUserUid = currentUser?.uid ?: ""

            UserDashboardScreen(
                navController = navController,
                currentUserUid = currentUserUid
            )
        }

        composable(
            route = ProfileRoutes.PROFILE,
            enterTransition = {
                fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.9f, animationSpec = tween(400))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 1.05f, animationSpec = tween(400))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.9f, animationSpec = tween(400))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 1.05f, animationSpec = tween(400))
            }
        ) {
            ProfileScreen(navController = navController)
        }

        composable(
            route = SettingRoutes.SETTINGS,
            enterTransition = {
                // Opening Settings → slide in + fade
                slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(400)) +
                        fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                // Leaving Settings → slide out reverse
                slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(400)) +
                        fadeOut(animationSpec = tween(400))
            },
            popEnterTransition = {
                // Coming back to Settings (rare)
                slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(400)) +
                        fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                // Back to Dashboard
                slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(400)) +
                        fadeOut(animationSpec = tween(400))
            }
        ) {
            SettingsNavGraph(
                onBackToDashboard = {
                    navController.popBackStack("UserDashboard", false)
                }
            )
        }


// *** ADD STARRED MESSAGES NAV GRAPH ***
        composable(
            route = "starredMessages",
            enterTransition = {
                // From Dashboard → Starred Messages
                slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(400)) +
                        fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                // Leaving Starred Messages → Dashboard
                slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(400)) +
                        fadeOut(animationSpec = tween(400))
            },
            popEnterTransition = {
                // Coming back into Starred Messages (pop)
                slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(400)) +
                        fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                // Back navigation → Dashboard
                slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(400)) +
                        fadeOut(animationSpec = tween(400))
            }
        ) {
            StarredMessagesScreen(navController = navController)
        }




//
//        composable(ProfileRoutes.STARRED) {
//            StarredScreen(navController = navController) // placeholder for now
//        }




//        composable(ChatRoutes.CHAT_WITH) { backStackEntry ->
//            val publicId = backStackEntry.arguments?.getString("publicId") ?: return@composable
//            ChatScreen(
//                publicId = publicId,
//                navController = navController
//            )
//        }



        composable(
            route = ChatRoutes.CHAT_WITH,
            arguments = listOf(
                navArgument("publicId") { type = NavType.StringType },
                navArgument("focusId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val publicId = backStackEntry.arguments?.getString("publicId") ?: return@composable
            val focusId = backStackEntry.arguments?.getString("focusId")

            ChatScreen(
                publicId = publicId,
                navController = navController,
                focusMessageId = focusId   // 👈 this is the key
            )
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




