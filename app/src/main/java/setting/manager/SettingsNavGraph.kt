package com.chat.safeplay.setting.manager

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chat.safeplay.setting.manager.SettingsScreen  // ✅ correct import


@Composable
fun SettingsNavGraph(
    onBackToDashboard: () -> Unit = {} // ✅ receives callback from main nav
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SettingRoutes.SETTINGS
    ) {
        composable(SettingRoutes.SETTINGS) {
            SettingsScreen(
                onOpenPasswordManager = {
                    navController.navigate(SettingRoutes.PASSWORD_RESET)
                },
                onRequestPinChange = {
                    navController.navigate(SettingRoutes.PIN_CHANGE)
                },
                onChangeChatBackground = {
                    navController.navigate(SettingRoutes.CHAT_BACKGROUND)

                },
                onDelayChat = {

                },
                onRequestAccountDeletion = {

                },
                navController = navController,

                // ✅ Pass the callback down to SettingsScreen
                onBackToDashboard = onBackToDashboard
            )
        }

        composable(SettingRoutes.PASSWORD_RESET) {
            PasswordResetScreen(navController = navController)
        }
        composable("settings_pin_change") {
            PinChangeScreen(navController)
        }
        composable(SettingRoutes.CHAT_BACKGROUND) {
            ChangeChatBackgroundScreen(navController = navController)
        }

    }
}
