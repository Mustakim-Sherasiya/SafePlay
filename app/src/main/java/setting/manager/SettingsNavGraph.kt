package com.chat.safeplay.setting.manager

import androidx.compose.runtime.Composable


import androidx.navigation.compose.rememberNavController
import com.chat.safeplay.setting.manager.SettingsScreen  // ✅ correct import
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import androidx.compose.animation.*
import androidx.compose.animation.core.tween


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsNavGraph(
    onBackToDashboard: () -> Unit = {}
) {
    val navController = rememberNavController()

    AnimatedNavHost(
        navController = navController,
        startDestination = SettingRoutes.SETTINGS,




        // 🎬 Smooth SafePlay-style transition
        enterTransition = {
            slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(400)) +
                    fadeIn(animationSpec = tween(400))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(400)) +
                    fadeOut(animationSpec = tween(400))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(400)) +
                    fadeIn(animationSpec = tween(400))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(400)) +
                    fadeOut(animationSpec = tween(400))
        }




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

                onDelayChat = {  navController.navigate(SettingRoutes.DELAY_CHAT)},


                onRequestAccountDeletion = { navController.navigate(SettingRoutes.ACCOUNT_DELETION)},

                navController = navController,
                onBackToDashboard = onBackToDashboard
            )
        }

        composable(SettingRoutes.PASSWORD_RESET) {
            PasswordResetScreen(navController = navController)
        }

        composable(SettingRoutes.PIN_CHANGE) {
            PinChangeScreen(navController)
        }

        composable(SettingRoutes.CHAT_BACKGROUND) {
            ChangeChatBackgroundScreen(navController = navController)
        }

        composable(SettingRoutes.DELAY_CHAT) {
            DelayChatScreen(navController)
        }

        composable(SettingRoutes.ACCOUNT_DELETION) {
            AccountDeletionScreen(navController)
        }

    }
}