package com.rushgames.barrier.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Today : Screen("today")
    object Sessions : Screen("sessions")
    object Blocklists : Screen("blocklists")
    object Sounds : Screen("sounds")
    object Settings : Screen("settings")

    // Nested
    object SessionDetail : Screen("sessions/{sessionId}") {
        fun withId(id: Long) = "sessions/$id"
    }
    object CreateSession : Screen("sessions/create")
    object BlocklistDetail : Screen("blocklists/{blocklistId}") {
        fun withId(id: Long) = "blocklists/$id"
    }
    object CreateBlocklist : Screen("blocklists/create")
    object PermissionsSetup : Screen("permissions")
    object ProtectionCenter : Screen("protection_center")
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Today.route,
        label = "Today",
        icon = Icons.Outlined.Shield,
        selectedIcon = Icons.Outlined.Shield
    ),
    BottomNavItem(
        route = Screen.Sessions.route,
        label = "Sessions",
        icon = Icons.Outlined.PlayCircle,
        selectedIcon = Icons.Outlined.PlayCircle
    ),
    BottomNavItem(
        route = Screen.Blocklists.route,
        label = "Blocklists",
        icon = Icons.Outlined.Block,
        selectedIcon = Icons.Outlined.Block
    ),
    BottomNavItem(
        route = Screen.Sounds.route,
        label = "Sounds",
        icon = Icons.Outlined.MusicNote,
        selectedIcon = Icons.Outlined.MusicNote
    ),
    BottomNavItem(
        route = Screen.Settings.route,
        label = "Settings",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Outlined.Settings
    )
)
