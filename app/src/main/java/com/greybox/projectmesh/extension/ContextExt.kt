package com.greybox.projectmesh.extension

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.ustadmobile.meshrabiya.MeshrabiyaConstants

/*
context is a class that provides access to application-specific resources and classes.
This File contains several context related extension functions that will use in this app.
 */

/**
 * On Android 13+ we can use the NEARBY_WIFI_DEVICES permission instead of the location permission.
 * On earlier versions, we need fine location permission
 */
val NEARBY_WIFI_PERMISSION_NAME = if(Build.VERSION.SDK_INT >= 33){
    Manifest.permission.NEARBY_WIFI_DEVICES
}else {
    Manifest.permission.ACCESS_FINE_LOCATION
}

// check if the app has the nearby wifi devices permission
fun Context.hasNearbyWifiDevicesOrLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this, NEARBY_WIFI_PERMISSION_NAME
    ) == PackageManager.PERMISSION_GRANTED
}

// check if the app has the bluetooth connect permission
fun Context.hasBluetoothConnectPermission(): Boolean {
    return if(Build.VERSION.SDK_INT >= 31) {
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }else {
        true
    }
}

// create a DataStore instance that Meshrabiya can use to remember networks
val Context.networkDataStore: DataStore<Preferences> by preferencesDataStore(name = "meshr_settings")

// Check if the device supports WiFi STA/AP Concurrency
fun Context.hasStaApConcurrency(): Boolean {
    return Build.VERSION.SDK_INT >= 30 && getSystemService(WifiManager::class.java).isStaApConcurrencySupported
}

fun Context.deviceInfo(): String {
    val wifiManager = getSystemService(WifiManager::class.java)
    val hasStaConcurrency = Build.VERSION.SDK_INT >= 31 &&
            wifiManager.isStaConcurrencyForLocalOnlyConnectionsSupported
    val hasStaApConcurrency = Build.VERSION.SDK_INT >= 30 &&
            wifiManager.isStaApConcurrencySupported
    val hasWifiAwareSupport = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)

    return buildString {
        append("Meshrabiya: Version :${MeshrabiyaConstants.VERSION}\n")
        append("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        append("Device: ${Build.MANUFACTURER} - ${Build.MODEL}\n")
        append("5Ghz supported: ${wifiManager.is5GHzBandSupported}\n")
        append("Local-only station concurrency: $hasStaConcurrency\n")
        append("Station-AP concurrency: $hasStaApConcurrency\n")
        append("WifiAware support: $hasWifiAwareSupport\n")
    }
}