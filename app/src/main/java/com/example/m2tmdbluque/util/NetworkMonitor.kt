package com.example.m2tmdbluque.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    private var callback: ConnectivityManager.NetworkCallback? = null

    fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun start(onChanged: (online: Boolean) -> Unit) {
        stop()
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onChanged(true)
            override fun onLost(network: Network) = onChanged(isOnline())
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                onChanged(
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                )
            }
        }
        callback = networkCallback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        onChanged(isOnline())
    }

    fun stop() {
        callback?.let {
            runCatching { connectivityManager.unregisterNetworkCallback(it) }
        }
        callback = null
    }
}
