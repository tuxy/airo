package com.tuxy.airo.data.background

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log

/**
 * Network callback handler for detecting connectivity changes on N+.
 *
 * DOES NOT USE THE OLD BROADCAST: Uses the modern [ConnectivityManager.NetworkCallback] API
 * which doesn't require manifest declaration.
 *
 * @param context Application context for starting services
 */
class NetworkCallbackHandler(private val context: Context) : ConnectivityManager.NetworkCallback() {

    companion object {
        private const val LOG_TAG = "NetworkCallbackHandler"
    }

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        Log.d(LOG_TAG, "Network available, starting FlightRefreshService")
        val serviceIntent = Intent(context, FlightRefreshService::class.java).apply {
            action = FlightRefreshService.ACTION_NETWORK_CHANGE
        }
        context.startForegroundService(serviceIntent)
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        Log.d(LOG_TAG, "Network lost")
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        val hasInternet =
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Log.d(LOG_TAG, "Network capabilities changed, hasInternet: $hasInternet")
    }
}