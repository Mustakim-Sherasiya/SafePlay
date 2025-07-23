package com.chat.safeplay.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.chat.safeplay.CreatePinScreen
import com.chat.safeplay.forgotpin.ForgotPinScreen
import com.chat.safeplay.forgotpin.ResetPinSecurityQuestionScreen


@Composable
fun PinNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "forgotPin") {

        composable("forgotPin") {
            ForgotPinScreen(
                onSecurityQuestionFetched = { userId, question ->
                    // URL-encode question if it can have spaces or special chars
                    val encodedQuestion = java.net.URLEncoder.encode(question, "UTF-8")
                    navController.navigate("resetSecurityQuestion/$userId/$encodedQuestion")
                }
            )
        }

        composable(
            route = "resetSecurityQuestion/{userId}/{question}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("question") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val encodedQuestion = backStackEntry.arguments?.getString("question") ?: ""
            val question = java.net.URLDecoder.decode(encodedQuestion, "UTF-8")
            ResetPinSecurityQuestionScreen(
                userId = userId,
                securityQuestion = question,
                onAnswerVerified = {
                    navController.navigate("createPinForReset/$userId") {
                        popUpTo("resetSecurityQuestion/$userId/$encodedQuestion") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "createPinForReset/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            CreatePinScreen(
                navController = navController,
                userIdForPinReset = userId,
                onPinCreated = { pin, pinLength, autoSubmit ->
                    // After saving the PIN, navigate back to forgotPin or login screen
                    navController.popBackStack("forgotPin", false)
                }
            )
        }
    }
}
