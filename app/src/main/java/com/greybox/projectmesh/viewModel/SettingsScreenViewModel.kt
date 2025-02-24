package com.greybox.projectmesh.viewModel

import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.greybox.projectmesh.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.kodein.di.DI
import org.kodein.di.instance

class SettingsScreenViewModel(di: DI, savedStateHandle: SavedStateHandle): ViewModel() {
    // inject SharedPreferences
    private val settingPrefs: SharedPreferences by di.instance(tag = "settings")

    // Theme State
    private val _theme = MutableStateFlow(loadTheme())
    val theme: StateFlow<AppTheme> = _theme

    // Language State
    private val _lang = MutableStateFlow(loadLang())
    val lang: StateFlow<String> = _lang

    // Device Name State
    private val _deviceName = MutableStateFlow(loadDeviceName())
    val deviceName: StateFlow<String> = _deviceName

    // Auto Finish State
    private val _autoFinish = MutableStateFlow(loadAutoFinish())
    val autoFinish: StateFlow<Boolean> = _autoFinish

    // Save To Folder (directory path)
    private val _saveToFolder = MutableStateFlow(loadSaveToFolder())
    val saveToFolder: StateFlow<String> = _saveToFolder

    init {
        // Load the theme from SharedPreferences
        _theme.value = loadTheme()
        _lang.value = loadLang()
        _deviceName.value = loadDeviceName()
        _autoFinish.value = loadAutoFinish()
        _saveToFolder.value = loadSaveToFolder()
    }

    // Load theme from SharedPreferences
    private fun loadTheme(): AppTheme {
        val themeName = settingPrefs.getString(THEME_KEY, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        return AppTheme.valueOf(themeName)
    }

    // Store Selected Theme to SharedPreferences
    fun saveTheme(theme: AppTheme) {
        _theme.value = theme
        settingPrefs.edit().putString(THEME_KEY, theme.name).apply()
    }

    // Load Language from SharedPreferences
    private fun loadLang(): String {
        return settingPrefs.getString(LANGUAGE_KEY, "System") ?: "System"
    }

    // Store Selected Language to SharedPreferences
    fun saveLang(languageCode: String) {
        _lang.value = languageCode
        settingPrefs.edit().putString(LANGUAGE_KEY, languageCode).apply()
    }

    // Load Device Name from SharedPreferences
    private fun loadDeviceName(): String {
        return settingPrefs.getString(DEVICE_NAME_KEY, Build.MODEL) ?: Build.MODEL
    }

    // Store Device Name to SharedPreferences
    fun saveDeviceName(deviceName: String) {
        _deviceName.value = deviceName
        settingPrefs.edit().putString(DEVICE_NAME_KEY, deviceName).apply()
    }

    private fun loadAutoFinish(): Boolean {
        return settingPrefs.getBoolean(AUTO_FINISH_KEY, false)
    }

    fun saveAutoFinish(autoFinish: Boolean) {
        _autoFinish.value = autoFinish
        settingPrefs.edit().putBoolean(AUTO_FINISH_KEY, autoFinish).apply()
    }

    private fun loadSaveToFolder(): String {
        return settingPrefs.getString(
            SAVE_TO_FOLDER_KEY,
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/Project Mesh") ?:
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/Project Mesh"
    }

    fun saveSaveToFolder(saveToFolder: String) {
        _saveToFolder.value = saveToFolder
        settingPrefs.edit().putString(SAVE_TO_FOLDER_KEY, saveToFolder).apply()
    }

    fun updateConcurrencySettings(concurrencyKnown: Boolean, concurrencySupported: Boolean) {
        settingPrefs.edit()
            .putBoolean("concurrency_known", concurrencyKnown)
            .putBoolean("concurrency_supported", concurrencySupported)
            .apply()
    }


    companion object{
        private const val THEME_KEY = "app_theme"
        private const val LANGUAGE_KEY = "language"
        private const val DEVICE_NAME_KEY = "device_name"
        private const val AUTO_FINISH_KEY = "auto_finish"
        private const val SAVE_TO_FOLDER_KEY = "save_to_folder"
    }
}