package cleanmail.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cleanmail.ui.accounts.AccountSetupScreen
import cleanmail.ui.compose.ComposeScreen
import cleanmail.ui.inbox.InboxScreen
import cleanmail.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Inbox    : Screen("inbox/{accountId}") {
        fun go(accountId: String) = "inbox/$accountId"
    }
    object Compose  : Screen("compose?replyToId={replyToId}") {
        fun go(replyToId: String? = null) =
            if (replyToId != null) "compose?replyToId=$replyToId" else "compose"
    }
    object Settings : Screen("settings")
    object AccountSetup : Screen("account_setup?accountId={accountId}") {
        fun go(accountId: String? = null) =
            if (accountId != null) "account_setup?accountId=$accountId" else "account_setup"
    }
}

@Composable
fun CleanMailNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "inbox/default") {

        composable(
            route = Screen.Inbox.route,
            arguments = listOf(navArgument("accountId") { defaultValue = "default" })
        ) { back ->
            InboxScreen(
                accountId = back.arguments?.getString("accountId") ?: "default",
                onCompose = { navController.navigate(Screen.Compose.go()) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onReply = { emailId -> navController.navigate(Screen.Compose.go(emailId)) },
                onSetupAccount = { navController.navigate(Screen.AccountSetup.go()) }
            )
        }

        composable(
            route = Screen.Compose.route,
            arguments = listOf(navArgument("replyToId") {
                nullable = true
                type = NavType.StringType
                defaultValue = null
            })
        ) { back ->
            ComposeScreen(
                replyToId = back.arguments?.getString("replyToId"),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onAddAccount = { navController.navigate(Screen.AccountSetup.go()) },
                onEditAccount = { id -> navController.navigate(Screen.AccountSetup.go(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AccountSetup.route,
            arguments = listOf(navArgument("accountId") {
                nullable = true
                type = NavType.StringType
                defaultValue = null
            })
        ) { back ->
            AccountSetupScreen(
                accountId = back.arguments?.getString("accountId"),
                onSaved = {
                    navController.navigate(Screen.Inbox.go("default")) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
