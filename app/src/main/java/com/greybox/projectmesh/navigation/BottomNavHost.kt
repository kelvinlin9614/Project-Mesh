package com.greybox.projectmesh.navigation

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.greybox.projectmesh.R
import com.greybox.projectmesh.ui.theme.AppTheme
import com.greybox.projectmesh.viewModel.SharedUriViewModel
import com.greybox.projectmesh.views.ChatScreen
import com.greybox.projectmesh.views.HomeScreen
import com.greybox.projectmesh.views.LogScreen
import com.greybox.projectmesh.views.NetworkScreen
import com.greybox.projectmesh.views.PingScreen
import com.greybox.projectmesh.views.ReceiveScreen
import com.greybox.projectmesh.views.SelectDestNodeScreen
import com.greybox.projectmesh.views.SendScreen
import com.greybox.projectmesh.views.SettingsScreen
import org.kodein.di.DI
import org.kodein.di.compose.withDI
import java.net.InetAddress

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun BottomNavApp(di: DI,
                 startDestination: String,
                 onThemeChange: (AppTheme) -> Unit,
                 onLanguageChange: (String) -> Unit,
                 onNavigateToScreen: (String) -> Unit,
                 onRestartServer: () -> Unit,
                 onDeviceNameChange: (String) -> Unit,
                 deviceName: String,
                 onAutoFinishChange: (Boolean) -> Unit,
                 onSaveToFolderChange: (String) -> Unit
) = withDI(di)
{
    val navController = rememberNavController()
    // Observe the current route directly through the back stack entry
    val currentRoute = navController.currentBackStackEntryFlow.collectAsState(initial = null)
    LaunchedEffect(currentRoute.value?.destination?.route) {
        if(currentRoute.value?.destination?.route == BottomNavItem.Settings.route){
            currentRoute.value?.destination?.route?.let { route ->
                onNavigateToScreen(route)
            }
        }
    }
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ){ innerPadding ->
        NavHost(navController, startDestination = startDestination, Modifier.padding(innerPadding))
        {
            composable(BottomNavItem.Home.route) {
                HomeScreen(deviceName = deviceName) }
            composable(BottomNavItem.Network.route) { NetworkScreen(
                onClickNetworkNode = { ip ->
                    navController.navigate("chatScreen/${ip}")
                }
            ) }
            composable("chatScreen/{ip}"){ entry ->
                val ip = entry.arguments?.getString("ip")
                    ?: throw IllegalArgumentException("Invalid address")
                ChatScreen(
                    virtualAddress = InetAddress.getByName(ip),
                    onClickButton = {
                        navController.navigate("pingScreen/${ip}")
                    }
                )
            }
            composable("pingScreen/{ip}"){ entry ->
                val ip = entry.arguments?.getString("ip")
                    ?: throw IllegalArgumentException("Invalid address")
                PingScreen(
                    virtualAddress = InetAddress.getByName(ip)
                )
            }
            composable(BottomNavItem.Send.route) {
                val activity = LocalContext.current as ComponentActivity
                val sharedUrisViewModel: SharedUriViewModel = viewModel(activity)
                SendScreen(
                    onSwitchToSelectDestNode = { uris ->
                        Log.d("uri_track_nav_send", "size: " + uris.size.toString())
                        Log.d("uri_track_nav_send", "List: $uris")
                        sharedUrisViewModel.setUris(uris)
                        navController.navigate("selectDestNode")
                    }
                )
            }
            composable("selectDestNode"){
                val activity = LocalContext.current as ComponentActivity
                val sharedUrisViewModel: SharedUriViewModel = viewModel(activity)
                val sendUris by sharedUrisViewModel.uris.collectAsState()
                Log.d("uri_track_nav_selectDestNode", "size: " + sendUris.size.toString())
                Log.d("uri_track_nav_selectDestNode", "List: $sendUris")
                SelectDestNodeScreen(
                    uris = sendUris,
                    popBackWhenDone = {navController.popBackStack()},
                )
            }
            composable(BottomNavItem.Receive.route) { ReceiveScreen(
                onAutoFinishChange = onAutoFinishChange
            ) }
            composable(BottomNavItem.Log.route) {
                LogScreen()
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(
                    onThemeChange = onThemeChange,
                    onLanguageChange = onLanguageChange,
                    onRestartServer = onRestartServer,
                    onDeviceNameChange = onDeviceNameChange,
                    onAutoFinishChange = onAutoFinishChange,
                    onSaveToFolderChange = onSaveToFolderChange
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        NavigationItem(BottomNavItem.Home.route, stringResource(id = R.string.home), BottomNavItem.Home.icon),
        NavigationItem(BottomNavItem.Network.route, stringResource(id = R.string.network), BottomNavItem.Network.icon),
        NavigationItem(BottomNavItem.Send.route, stringResource(id = R.string.send), BottomNavItem.Send.icon),
        NavigationItem(BottomNavItem.Receive.route, stringResource(id = R.string.receive), BottomNavItem.Receive.icon),
        NavigationItem(BottomNavItem.Log.route, stringResource(id = R.string.log), BottomNavItem.Log.icon),
        NavigationItem(BottomNavItem.Settings.route, stringResource(id = R.string.settings), BottomNavItem.Settings.icon)
    )
    NavigationBar {
        val currentRoute = navController.currentDestination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
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
