package com.greybox.projectmesh.util

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import org.kodein.di.compose.localDI
import org.kodein.di.instance

//@Composable
//fun getLatestConcurrencySupportAnswer(
//    concurrencyKnown: Boolean,
//    concurrencySupported: Boolean
//): Boolean {
//    val di = localDI()
//    val settingPref: SharedPreferences by di.instance(tag="settings")
//    return concurrencyKnown && concurrencySupported
//}

/**
 * Tries to check STA+AP concurrency support:
 * 1) If Android 11+ -> use the official API.
 * 2) If Android 9/10 -> try reflection to find 'isDualModeSupported()'.
 * 3) If reflection fails -> prompt the user to do a manual check.
 */
fun checkStaApConcurrency(
    context: Context,
    onResult: (Boolean?) -> Unit
) {
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        ?: return onResult(false)  // No WifiManager? Return false or handle gracefully.

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // On Android 11+ (API 30+), we can use the official API
        onResult(wifiManager.isStaApConcurrencySupported)
    } else {
        // Attempt reflection on Android 9 or 10
        try {
            val method = wifiManager.javaClass.getMethod("isDualModeSupported")
            val result = method.invoke(wifiManager) as? Boolean ?: false
            if(result){
                showSupportDialog(context)
            }
            else{
                showNotSupportDialog(context)
            }
            onResult(result)
        } catch (e: NoSuchMethodException) {
            // The hidden method doesn't exist on this device/ROM
            showManualConcurrencyDialog(context, onResult)
        } catch (e: Exception) {
            // Reflection failed for some other reason
            showManualConcurrencyDialog(context, onResult)
        }
    }
}

/**
 * Shows a dialog explaining how to test concurrency manually,
 * and get the user’s response.
 */
private fun showManualConcurrencyDialog(
    context: Context,
    onResult: (Boolean?) -> Unit
) {
    AlertDialog.Builder(context)
        .setTitle("Manual Test Required")
        .setMessage(
            "We were unable to determine if your device supports " +
                    "Wi-Fi + Hotspot concurrency. Please help us verify this feature using your " +
                    "device's built-in Wi-Fi and Hotspot (Not this app) by following these steps:\n\n" +
                    "1) Connect to a Wi-Fi network.\n" +
                    "2) Enable your hotspot.\n" +
                    "3) Check if Wi-Fi remains connected or if it’s disabled."
        )
        .setPositiveButton("Wi-Fi stayed connected") { _, _ ->
            // User indicate concurrency works
            onResult(true)
        }
        .setNegativeButton("Wi-Fi turned off") { _, _ ->
            // User indicate concurrency does not work
            onResult(false)
        }
        .setNeutralButton("Cancel") { _, _ ->
            // User canceled without answer
            onResult(null)
        }
        .show()
}

private fun showSupportDialog(context: Context) {
    AlertDialog.Builder(context)
        .setTitle("Great!")
        .setMessage("Your device supports Wi-Fi + Hotspot Concurrency")
        .setPositiveButton("Confirm!"){ _, _ -> }
        .show()
}

private fun showNotSupportDialog(context: Context) {
    AlertDialog.Builder(context)
        .setTitle("Unfortunately, Your device does not Support Wi-Fi + Hotspot Concurrency")
        .setMessage("If you believe your device should support this feature, " +
                "Please tap Reset button on Settings page.")
        .setPositiveButton("Confirm!"){ _, _ ->}
        .show()
}
