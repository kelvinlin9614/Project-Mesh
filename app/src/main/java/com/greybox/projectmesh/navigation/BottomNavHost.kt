package com.greybox.projectmesh.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        NavigationItem(BottomNavItem.Home.route, BottomNavItem.Home.title, BottomNavItem.Home.icon),
        NavigationItem(BottomNavItem.Network.route, BottomNavItem.Network.title, BottomNavItem.Network.icon),
        NavigationItem(BottomNavItem.Send.route, BottomNavItem.Send.title, BottomNavItem.Send.icon),
        NavigationItem(BottomNavItem.Receive.route, BottomNavItem.Receive.title, BottomNavItem.Receive.icon),
        NavigationItem(BottomNavItem.Info.route, BottomNavItem.Info.title, BottomNavItem.Info.icon)
    )
    NavigationBar {
        val currentRoute = navController.currentDestination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Avoid multiple copies of the same destination when reselecting the same item
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                        // Avoid multiple copies of the same destination when reselecting the same item
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
