package com.greybox.projectmesh

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.greybox.projectmesh.debug.CrashHandler
import com.greybox.projectmesh.debug.CrashScreenActivity
import com.greybox.projectmesh.navigation.BottomNavApp
import com.greybox.projectmesh.navigation.BottomNavItem
import com.greybox.projectmesh.navigation.BottomNavigationBar
import com.greybox.projectmesh.server.AppServer
import com.greybox.projectmesh.ui.theme.AppTheme
import com.greybox.projectmesh.ui.theme.ProjectMeshTheme
import com.greybox.projectmesh.viewModel.SharedUriViewModel
import com.greybox.projectmesh.views.ChatScreen
import com.greybox.projectmesh.views.HomeScreen
import com.greybox.projectmesh.views.SettingsScreen
import com.greybox.projectmesh.views.NetworkScreen
import com.greybox.projectmesh.views.PingScreen
import com.greybox.projectmesh.views.ReceiveScreen
import com.greybox.projectmesh.views.SelectDestNodeScreen
import com.greybox.projectmesh.views.SendScreen
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.compose.withDI
import org.kodein.di.instance
import java.io.File
import java.util.Locale
import java.net.InetAddress
import com.greybox.projectmesh.views.RequestPermissionsScreen

class MainActivity : ComponentActivity(), DIAware {
    override val di by closestDI()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // crash screen
        CrashHandler.init(applicationContext,CrashScreenActivity::class.java)
        val settingPref: SharedPreferences by di.instance(tag="settings")
        val appServer: AppServer by di.instance()
        // check if the default directory exist (Download/Project Mesh)
        ensureDefaultDirectory()
        setContent {
            // Check if the app was launched from a notification
            val launchedFromNotification = intent?.getBooleanExtra("from_notification", false) ?: false
            // Request all permission in order
            RequestPermissionsScreen(skipPermissions = launchedFromNotification)
            var appTheme by rememberSaveable {
                mutableStateOf(AppTheme.valueOf(
                    settingPref.getString("app_theme", AppTheme.SYSTEM.name) ?:
                    AppTheme.SYSTEM.name))
            }
            var languageCode by rememberSaveable {
                mutableStateOf(settingPref.getString(
                    "language", "en") ?: "en")
            }
            var restartServerKey by rememberSaveable {mutableStateOf(0)}
            var deviceName by rememberSaveable {
                mutableStateOf(settingPref.getString("device_name", Build.MODEL) ?: Build.MODEL)
            }

            var autoFinish by rememberSaveable {
                mutableStateOf(settingPref.getBoolean("auto_finish", false))
            }

            var saveToFolder by rememberSaveable {
                mutableStateOf(
                    settingPref.getString("save_to_folder", null)
                        ?: "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/Project Mesh"
                )
            }

            // State to trigger recomposition when locale changes
            var localeState by rememberSaveable { mutableStateOf(Locale.getDefault()) }

            // Remember the current screen across recompositions
            var currentScreen by rememberSaveable { mutableStateOf(BottomNavItem.Home.route) }
            LaunchedEffect(intent?.getStringExtra("navigateTo")) {
                if (intent?.getStringExtra("navigateTo") == BottomNavItem.Receive.route) {
                    currentScreen = BottomNavItem.Receive.route
                }
            }
            LaunchedEffect(restartServerKey) {
                if (restartServerKey > 0){
                    appServer.restart()
                    Toast.makeText(this@MainActivity, "Server restart complete", Toast.LENGTH_SHORT).show()
                }
            }
            // Observe language changes and apply locale
            LaunchedEffect(languageCode) {
                localeState = updateLocale(languageCode)
            }
            key(localeState) {
                ProjectMeshTheme(appTheme = appTheme) {
                    BottomNavApp(
                        di,
                        startDestination = currentScreen,
                        onThemeChange = { selectedTheme -> appTheme = selectedTheme},
                        onLanguageChange = { selectedLanguage ->  languageCode = selectedLanguage},
                        onNavigateToScreen = {screen ->
                            currentScreen = screen },
                        onRestartServer = {restartServerKey++},
                        onDeviceNameChange = {deviceName = it},
                        deviceName = deviceName,
                        onAutoFinishChange = {autoFinish = it},
                        onSaveToFolderChange = {saveToFolder = it}
                    )
                }
            }
        }
    }

    private fun ensureDefaultDirectory() {
        val defaultDirectory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Project Mesh"
        )
        if (!defaultDirectory.exists()) {
            // Create the directory if it doesn't exist
            if (defaultDirectory.mkdirs()) {
                Log.d("DirectoryCheck", "Default directory created: ${defaultDirectory.absolutePath}")
            }
            else {
                Log.e("DirectoryCheck", "Failed to create default directory: ${defaultDirectory.absolutePath}")
            }
        }
        else {
            Log.d("DirectoryCheck", "Default directory already exists: ${defaultDirectory.absolutePath}")
        }
    }

    private fun updateLocale(languageCode: String): Locale {
        val locale = Locale(languageCode)
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        return locale
    }
}