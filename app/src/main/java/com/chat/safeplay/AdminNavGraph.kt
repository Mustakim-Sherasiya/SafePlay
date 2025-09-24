

package com.chat.safeplay.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chat.safeplay.AdminPanelScreen
import com.chat.safeplay.AdminUserManagementScreen


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
    }
}
