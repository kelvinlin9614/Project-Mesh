package com.greybox.projectmesh.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector


sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector){
    data object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    data object Network : BottomNavItem("network", "Network", Icons.Default.Wifi)
    data object Send : BottomNavItem("send", "Send", Icons.AutoMirrored.Filled.Send)
    data object Receive : BottomNavItem("receive", "Receive", Icons.Default.Download)
    data object Log: BottomNavItem("log", "Log", Icons.Default.History)
    data object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)
}