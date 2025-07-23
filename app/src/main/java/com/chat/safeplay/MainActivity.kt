package com.chat.safeplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // Correct import
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.chat.safeplay.BeforeLoginNavGraph
import com.chat.safeplay.BottomNavigationBar
import com.chat.safeplay.ui.theme.SafePlayTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        enableEdgeToEdge() // Correct usage

        setContent {
            SafePlayTheme {
                val navController = rememberNavController()
                val startDestination = remember {
                    if (auth.currentUser != null) "home" else "login"
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BottomNavigationBar(navController) }
                ) { innerPadding ->
                    BeforeLoginNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        auth = auth,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}




//==========================================================================================
//-------------------------------------------------------------------------------------------
//package com.chat.safeplay
//
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.background
//import androidx.compose.foundation.gestures.detectTapGestures
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.*
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.*
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.*
//import com.chat.safeplay.ui.theme.SafePlayTheme
//import com.google.firebase.auth.FirebaseAuth
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Games
//import androidx.compose.material.icons.filled.Home
//
//class MainActivity : ComponentActivity() {
//    private lateinit var auth: FirebaseAuth
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        auth = FirebaseAuth.getInstance()
//
//        enableEdgeToEdge()
//        setContent {
//            SafePlayTheme {
//                val navController = rememberNavController()
//
//                // Decide start destination based on whether user is logged in
//                val startDestination = if (auth.currentUser != null) "home" else "login"
//
//                Scaffold(
//                    modifier = Modifier.fillMaxSize(),
//                    bottomBar = {
//                        NavigationBar {
//                            val navBackStackEntry by navController.currentBackStackEntryAsState()
//                            val currentRoute = navBackStackEntry?.destination?.route
//
//                            NavigationBarItem(
//                                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
//                                label = { Text("Home") },
//                                selected = currentRoute == "home",
//                                onClick = { navController.navigate("home") }
//                            )
//                            NavigationBarItem(
//                                icon = { Icon(Icons.Filled.Games, contentDescription = "Games") },
//                                label = { Text("Games") },
//                                selected = currentRoute == "gameSelection",
//                                onClick = { navController.navigate("gameSelection") }
//                            )
//                        }
//                    }
//                ) { innerPadding ->
//                    NavHost(
//                        navController = navController,
//                        startDestination = startDestination,
//                        modifier = Modifier.padding(innerPadding)
//                    ) {
//                        composable("home") { HomeScreen(navController) }
//                        composable("gameSelection") { GameSelectionScreen(navController) }
//
//                        composable("login") {
//                            LoginScreen(
//                                onLoginClick = { userInput, password ->
//                                    if (userInput.matches(Regex("^\\d{10,}$"))) {
//                                        Toast.makeText(this@MainActivity, "Use OTP for phone login", Toast.LENGTH_SHORT).show()
//                                    } else {
//                                        auth.signInWithEmailAndPassword(userInput, password)
//                                            .addOnCompleteListener(this@MainActivity) { task ->
//                                                if (task.isSuccessful) {
//                                                    navController.navigate("home") {
//                                                        popUpTo("login") { inclusive = true }
//                                                    }
//                                                } else {
//                                                    Toast.makeText(this@MainActivity, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
//                                                }
//                                            }
//                                    }
//                                },
//                                onCreateAccountClick = {
//                                    navController.navigate("createAccount")
//                                },
//                                onForgotPasswordClick = { email ->
//                                    auth.sendPasswordResetEmail(email)
//                                    Toast.makeText(this@MainActivity, "Reset link sent", Toast.LENGTH_SHORT).show()
//                                },
//                                navigateToPhoneOtpScreen = { phoneNumber ->
//                                    navController.navigate("phoneOtp/$phoneNumber")
//                                }
//                            )
//                        }
//
//                        composable("createAccount") {
//                            CreateAccountScreen(
//                                onCreateAccountClick = { email, phone, password ->
//                                    auth.createUserWithEmailAndPassword(email, password)
//                                        .addOnCompleteListener { task ->
//                                            if (task.isSuccessful) {
//                                                Toast.makeText(this@MainActivity, "Account created", Toast.LENGTH_SHORT).show()
//                                                navController.navigate("home")
//                                            } else {
//                                                Toast.makeText(this@MainActivity, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
//                                            }
//                                        }
//                                },
//                                onBackToLoginClick = {
//                                    navController.popBackStack()
//                                }
//                            )
//                        }
//
//                        composable("phoneOtp/{phoneNumber}") { backStackEntry ->
//                            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
//                            PhoneOtpScreen(phoneNumber = phoneNumber, navController = navController, auth = auth)
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//
//
//@Composable
//fun HomeScreen(navController: NavHostController) {
//    var devMessageVisible by remember { mutableStateOf(true) }
//    val context = LocalContext.current
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color.Black)
//            .pointerInput(Unit) {
//                detectTapGestures(
//                    onPress = {
//                        val pressTime = System.currentTimeMillis()
//                        try {
//                            awaitRelease()
//                        } catch (_: Exception) {
//                        }
//                        val duration = System.currentTimeMillis() - pressTime
//                        if (duration >= 3000) {
//                            devMessageVisible = false
//                            navController.navigate("login")
//                        }
//                    }
//                )
//            },
//        contentAlignment = Alignment.Center
//    ) {
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            if (devMessageVisible) {
//                Text(
//                    text = "🔧 App is still in development...",
//                    color = Color.White,
//                    fontSize = 20.sp
//                )
//                Spacer(modifier = Modifier.height(24.dp))
//            }
//            Button(onClick = { navController.navigate("gameSelection") }) {
//                Text("🎮 Play a Game While You Wait")
//            }
//        }
//    }
//}
//
//@Composable
//fun GameSelectionScreen(navController: NavHostController) {
//    val context = LocalContext.current
//
//    Column(
//        modifier = Modifier.fillMaxSize().padding(32.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text("🎮 Select a Game", fontSize = 24.sp, fontWeight = FontWeight.Bold)
//        Spacer(modifier = Modifier.height(32.dp))
//
//        Button(onClick = {
//            context.startActivity(Intent(context, TapTheDotActivity::class.java))
//        }) {
//            Text("🎯 Tap the Dot")
//        }
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Button(onClick = {
//            context.startActivity(Intent(context, ColorMemoryActivity::class.java))
//        }) {
//            Text("🔴 Color Memory")
//        }
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun HomeScreenPreview() {
//    SafePlayTheme {
//        HomeScreen(navController = rememberNavController())
//    }
//}
