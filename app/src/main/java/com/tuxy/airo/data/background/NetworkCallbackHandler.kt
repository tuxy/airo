package com.tuxy.airo.data.background

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log

/**
 * Enqueues a one-time refresh when connectivity becomes available.
 *
 * It must not start a foreground service: `onAvailable` can run while the app
 * is backgrounded, and Android 12+ forbids starting an FGS from the background.
 * WorkManager handles the network constraint and process lifetime for us.
 */
class NetworkCallbackHandler(private val context: Context) : ConnectivityManager.NetworkCallback() {

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        Log.d(LOG_TAG, "Network available; enqueueing a one-time refresh")
        FlightWorkScheduler.enqueueOneTime(context)
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        Log.d(LOG_TAG, "Network lost")
    }

    override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities,
    ) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        val hasInternet =
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Log.d(LOG_TAG, "Network capabilities changed, hasInternet: $hasInternet")
    }

    private companion object {
        const val LOG_TAG = "NetworkCallbackHandler"
    }
}
