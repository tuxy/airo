package com.tuxy.airo.data.background

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

/**
 * Utility object for network-related operations.
 *
 * Provides helper methods for registering network callbacks and checking
 * network availability. Uses the newer [ConnectivityManager.NetworkCallback] API.
 */
object NetworkChangeReceiver {

    private const val LOG_TAG = "NetworkChangeReceiver"

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