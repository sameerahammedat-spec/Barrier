package com.rushgames.barrier.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rushgames.barrier.ui.screens.blocklists.BlocklistDetailScreen
import com.rushgames.barrier.ui.screens.blocklists.BlocklistsScreen
import com.rushgames.barrier.ui.screens.blocklists.CreateBlocklistScreen
import com.rushgames.barrier.ui.screens.permissions.PermissionsScreen
import com.rushgames.barrier.ui.screens.protection.ProtectionCenterScreen
import com.rushgames.barrier.ui.screens.sessions.CreateSessionScreen
import com.rushgames.barrier.ui.screens.sessions.SessionDetailScreen
import com.rushgames.barrier.ui.screens.sessions.SessionsScreen
import com.rushgames.barrier.ui.screens.settings.SettingsScreen
import com.rushgames.barrier.ui.screens.sounds.SoundsScreen
import com.rushgames.barrier.ui.screens.today.TodayScreen

@Composable
fun BarrierNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Today.route,
        modifier = modifier
    ) {
        composable(Screen.Today.route) {
            TodayScreen(
                onStartSession = { navController.navigate(Screen.CreateSession.route) },
                onNavigateSessions = { navController.navigate(Screen.Sessions.route) },
                onNavigatePermissions = { navController.navigate(Screen.PermissionsSetup.route) }
            )
        }
        composable(Screen.Sessions.route) {
            SessionsScreen(
                onCreateSession = { navController.navigate(Screen.CreateSession.route) },
                onSessionClick = { id -> navController.navigate(Screen.SessionDetail.withId(id)) }
            )
        }
        composable(Screen.CreateSession.route) {
            CreateSessionScreen(
                onBack = { navController.popBackStack() },
                onSessionCreated = { navController.popBackStack() }
            )
        }
        composable(
            Screen.SessionDetail.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            SessionDetailScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Blocklists.route) {
            BlocklistsScreen(
                onCreateBlocklist = { navController.navigate(Screen.CreateBlocklist.route) },
                onBlocklistClick = { id -> navController.navigate(Screen.BlocklistDetail.withId(id)) }
            )
        }
        composable(Screen.CreateBlocklist.route) {
            CreateBlocklistScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.BlocklistDetail.route,
            arguments = listOf(navArgument("blocklistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val blocklistId = backStackEntry.arguments?.getLong("blocklistId") ?: return@composable
            BlocklistDetailScreen(
                blocklistId = blocklistId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Sounds.route) {
            SoundsScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigatePermissions = { navController.navigate(Screen.PermissionsSetup.route) },
                onNavigateProtection = { navController.navigate(Screen.ProtectionCenter.route) }
            )
        }
        composable(Screen.PermissionsSetup.route) {
            PermissionsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ProtectionCenter.route) {
            ProtectionCenterScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
