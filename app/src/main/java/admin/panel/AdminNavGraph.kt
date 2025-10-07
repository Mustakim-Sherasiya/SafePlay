//
//
//package com.chat.safeplay.navigation
//
//import androidx.compose.runtime.Composable
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import admin.panel.AdminUserDetailScreen
//import com.chat.safeplay.admin.panel.AdminUserManagementScreen
//import androidx.navigation.NavType
//import androidx.navigation.navArgument
//
//
//
//
//
//@Composable
//fun AdminNavGraph(navController: NavHostController) {
//    NavHost(
//        navController = navController,
//        startDestination = "adminPanel" // 👈 this is your main list screen
//    ) {
//        // 🟩 User List Screen
//        composable("adminUserList") {
//            AdminUserManagementScreen(navController = navController)
//        }
//
//        // 🟦 User Detail/Edit Screen
//        composable("adminUserDetail/{userId}") { backStackEntry ->
//            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
//            AdminUserDetailScreen(navController = navController, userId = userId)
//        }
//    }
//}

package com.chat.safeplay.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import admin.panel.AdminPanelScreen
import admin.panel.AdminUserDetailScreen
import com.chat.safeplay.admin.panel.AdminUserManagementScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNavGraph(parentNavController: NavHostController) {
    val adminNavController = rememberNavController()

    NavHost(navController = adminNavController, startDestination = "adminPanel") {
        composable("adminPanel") {
            AdminPanelScreen(adminNavController)
        }
        composable("adminUserManagement") {
            AdminUserManagementScreen(adminNavController)
        }
        //  🟦 User Detail/Edit Screen
        composable("adminUserDetail/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            // ✅ FIX: Pass the correct NavController
            AdminUserDetailScreen(navController = adminNavController, userId = userId)
        }
    }
}





