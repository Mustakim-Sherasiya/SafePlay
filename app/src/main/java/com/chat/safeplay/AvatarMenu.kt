package com.chat.safeplay

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Logout
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.chat.safeplay.profile.ProfileRoutes
import com.chat.safeplay.setting.manager.SettingRoutes

@OptIn(UnstableApi::class)
@Composable
fun AvatarMenu(
    navController: NavHostController,
    onLogoutDone: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val tag = "AvatarMenu"

    Surface {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(8.dp)
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.Person, contentDescription = "Avatar")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Profile Menu Item
                DropdownMenuItem(
                    text = { Text("Profile") },
                    onClick = {
                        // close menu first to avoid interaction issues
                        expanded = false

                        // Optional: log current route for debugging
                        Log.d(tag, "Navigating to profile. Current route: ${navController.currentBackStackEntry?.destination?.route}")

                        // navigate
                        navController.navigate(ProfileRoutes.PROFILE) {
                            launchSingleTop = true
                        }
                    }
                )
                // ✅ Settings
//                DropdownMenuItem(
//                    text = { Text("Settings") },
//                    onClick = {
//                        expanded = false
//                        navController.navigate(ProfileRoutes.SETTINGS) {
//                            launchSingleTop = true
//                        }
//                    },
//                    leadingIcon = {
//                        Icon(Icons.Default.Settings, contentDescription = "Settings")
//                    }
//                )
//
//                // ✅ Starred
//                DropdownMenuItem(
//                    text = { Text("Starred") },
//                    onClick = {
//                        expanded = false
//                        navController.navigate(ProfileRoutes.STARRED) {
//                            launchSingleTop = true
//                        }
//                    },
//                    leadingIcon = {
//                        Icon(Icons.Default.Star, contentDescription = "Starred")
//                    }
//                )

                // ✅ Logout
                DropdownMenuItem(
                    text = { Text("Logout") },
                    onClick = {
                        expanded = false
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("userDashboard") { inclusive = true }
                        }
                        onLogoutDone?.invoke()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                )
            }
        }
    }
}
