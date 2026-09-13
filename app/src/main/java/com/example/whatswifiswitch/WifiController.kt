package com.example.whatswifiswitch

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Handles turning WiFi on/off and connecting to the configured network.
 *
 * Note: Starting with Android 10 (API 29), apps are no longer allowed to
 * silently enable/disable WiFi (this is an OS-level privacy restriction,
 * not something this app can bypass without root). On those versions we
 * fall back to opening the quick WiFi panel so the user can tap it once.
 * Connecting to the saved network, however, can still happen automatically
 * via WifiNetworkSuggestion once WiFi is on.
 */
object WifiController {

    private const val TAG = "WifiController"

    fun turnOnAndConnect(context: Context, networks: List<WifiNetwork>) {
        if (networks.isEmpty()) return

        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Pre-Android 10: we can toggle wifi and connect directly.
            // Use the first saved network that's in range; simplest
            // approach is to register all of them and let Android pick.
            wifiManager.isWifiEnabled = true
            networks.forEach { legacyAddNetwork(wifiManager, it.ssid, it.password) }
            wifiManager.reconnect()
        } else {
            // Android 10+: can't force-enable wifi silently. Suggest every
            // saved network (Android auto-connects to whichever is in
            // range once wifi is on) and open the quick panel so the user
            // only needs one tap.
            suggestNetworks(context, networks)
            if (!wifiManager.isWifiEnabled) {
                openWifiPanel(context)
            }
        }
    }

    fun turnOff(context: Context) {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = false
        } else {
            openWifiPanel(context)
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyAddNetwork(wifiManager: WifiManager, ssid: String, password: String) {
        val config = WifiConfiguration().apply {
            SSID = "\"$ssid\""
            preSharedKey = "\"$password\""
        }

        val netId = wifiManager.addNetwork(config)
        if (netId != -1) {
            wifiManager.enableNetwork(netId, false)
        } else {
            Log.w(TAG, "Failed to add network config for SSID $ssid")
        }
    }

    private fun suggestNetworks(context: Context, networks: List<WifiNetwork>) {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        val suggestions = networks.map {
            WifiNetworkSuggestion.Builder()
                .setSsid(it.ssid)
                .setWpa2Passphrase(it.password)
                .build()
        }

        val result = wifiManager.addNetworkSuggestions(suggestions)
        if (result != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS &&
            result != WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE
        ) {
            Log.w(TAG, "Network suggestions failed with status $result")
        }
    }

    private fun openWifiPanel(context: Context) {
        val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not open wifi panel: ${e.message}")
        }
    }
}
