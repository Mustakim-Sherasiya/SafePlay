package com.chat.safeplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // Correct import
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chat.safeplay.ui.theme.SafePlayTheme
import com.google.firebase.auth.FirebaseAuth
import com.chat.safeplay.ui.theme.LaunchVideoOverlay




class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        enableEdgeToEdge() // ✅ ensures system bars draw edge-to-edge

        setContent {
            SafePlayTheme {

//                Box(modifier = Modifier.fillMaxSize()) {
//                    LaunchVideoOverlay()
//                    // your nav and scaffold below
//                }

                  //  LaunchVideoOverlay() // 👈 add this line // ANIMATION VIDEO AT START

                val navController = rememberNavController()
                val startDestination = "home" // Always start at home

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (
                            currentRoute != "adminGraph" &&
                            currentRoute != "profile" &&
                            currentRoute != "settings"&&
                            currentRoute?.startsWith("chat/") == false
                        ) {
                            BottomNavigationBar(navController)
                        }
                    }
                ) { innerPadding ->  // 👈 add this parameter
                    BeforeLoginNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        auth = auth,
                        modifier = Modifier // ✅ no padding applied
                    )
                }


            }
        }
    }
}







//--------------- NOT INCLUDED START ANIMATION VIDEO ----------------//
//package com.chat.safeplay
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge // Correct import
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material3.Scaffold
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Modifier
//import androidx.navigation.compose.currentBackStackEntryAsState
//import androidx.navigation.compose.rememberNavController
//import com.chat.safeplay.ui.theme.SafePlayTheme
//import com.google.firebase.auth.FirebaseAuth
//
//class MainActivity : ComponentActivity() {
//    private lateinit var auth: FirebaseAuth
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        auth = FirebaseAuth.getInstance()
//
//        enableEdgeToEdge() // ✅ ensures system bars draw edge-to-edge
//
//        setContent {
//            SafePlayTheme {
//                val navController = rememberNavController()
//                val startDestination = "home" // Always start at home
//
//                val navBackStackEntry by navController.currentBackStackEntryAsState()
//                val currentRoute = navBackStackEntry?.destination?.route
//
//                Scaffold(
//                    modifier = Modifier.fillMaxSize(),
//                    bottomBar = {
//                        if (
//                            currentRoute != "adminGraph" &&
//                            currentRoute != "profile" &&
//                            currentRoute != "settings"&&
//                            currentRoute?.startsWith("chat/") == false
//                        ) {
//                            BottomNavigationBar(navController)
//                        }
//                    }
//                ) { innerPadding ->  // 👈 add this parameter
//                    BeforeLoginNavGraph(
//                        navController = navController,
//                        startDestination = startDestination,
//                        auth = auth,
//                        modifier = Modifier // ✅ no padding applied
//                    )
//                }
//
//
//            }
//        }
//    }
//}
//
//
//
//
//

