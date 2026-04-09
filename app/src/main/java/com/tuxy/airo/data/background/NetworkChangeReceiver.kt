package com.tuxy.airo.data.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

/**
 * BroadcastReceiver for detecting network connectivity changes.
 *
 * This receiver listens for [ConnectivityManager.CONNECTIVITY_ACTION] broadcasts
 * and triggers [FlightRefreshService] when the device gains network connectivity.
 *
 * Note: This is a fallback for older Android versions. On Android 7+,
 * [NetworkCallbackHandler] is the preferred approach as it's more reliable and
 * doesn't require manifest declaration.
 *
 * @see NetworkCallbackHandler Primary network change detection mechanism
 */
class NetworkChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val LOG_TAG = "NetworkChangeReceiver"

        /**
         * Checks if network is currently available.
         *
         * @param context Application context
         * @return true if device has internet connectivity
         */
        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        /**
         * Registers a network callback for connectivity changes.
         *
         * @param context Application context
         * @param callback NetworkCallback to receive events
         */
        fun registerNetworkCallback(
            context: Context,
            callback: ConnectivityManager.NetworkCallback
        ) {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
            Log.d(LOG_TAG, "Network callback registered")
        }

        /**
         * Unregisters a previously registered network callback.
         *
         * @param context Application context
         * @param callback NetworkCallback to unregister
         */
        fun unregisterNetworkCallback(
            context: Context,
            callback: ConnectivityManager.NetworkCallback
        ) {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                Log.d(LOG_TAG, "Network callback was not registered")
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(LOG_TAG, "onReceive called with action: ${intent.action}")

        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo

            if (networkInfo != null && networkInfo.isConnected) {
                Log.d(LOG_TAG, "Network connected, starting FlightRefreshService")
                val serviceIntent = Intent(context, FlightRefreshService::class.java).apply {
                    action = FlightRefreshService.ACTION_NETWORK_CHANGE
                }
                context.startForegroundService(serviceIntent)
            } else {
                Log.d(LOG_TAG, "Network disconnected")
            }
        }
    }
}

/**
 * Network callback handler for detecting connectivity changes on Android 7+.
 *
 * Uses the modern [ConnectivityManager.NetworkCallback] API which is more reliable
 * than broadcast receivers and doesn't require manifest declaration.
 *
 * @param context Application context for starting services
 */
class NetworkCallbackHandler(private val context: Context) : ConnectivityManager.NetworkCallback() {

    companion object {
        private const val LOG_TAG = "NetworkCallbackHandler"
    }

    /**
     * Called when a network becomes available.
     * Starts [FlightRefreshService] to refresh flight data.
     */
    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        Log.d(LOG_TAG, "Network available, starting FlightRefreshService")
        val serviceIntent = Intent(context, FlightRefreshService::class.java).apply {
            action = FlightRefreshService.ACTION_NETWORK_CHANGE
        }
        context.startForegroundService(serviceIntent)
    }

    /**
     * Called when the network is lost.
     */
    override fun onLost(network: Network) {
        super.onLost(network)
        Log.d(LOG_TAG, "Network lost")
    }

    /**
     * Called when network capabilities change.
     */
    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        val hasInternet =
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Log.d(LOG_TAG, "Network capabilities changed, hasInternet: $hasInternet")
    }
}