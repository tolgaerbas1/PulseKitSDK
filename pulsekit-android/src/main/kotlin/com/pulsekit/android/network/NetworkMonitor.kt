package com.pulsekit.android.network

import android.annotation.TargetApi
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors network connectivity and provides real-time updates.
 *
 * This class uses modern NetworkCallback API to monitor network changes
 * and provides connectivity information to the SDK.
 */
public class NetworkMonitor private constructor(
    context: Context,
) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val _isConnected = MutableStateFlow(false)
    private val _networkType = MutableStateFlow(NetworkType.NONE)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            updateNetworkStatus()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            updateNetworkStatus()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            updateNetworkStatus()
        }
    }

    init {
        startMonitoring()
    }

    /**
     * Flow of network connectivity status.
     */
    public val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /**
     * Flow of current network type.
     */
    public val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()

    /**
     * Get current network connectivity status.
     */
    public fun getCurrentStatus(): NetworkStatus {
        val connectivityManager = connectivityManager
        return when {
            connectivityManager == null -> disconnectedStatus()
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> getLegacyStatus(connectivityManager)
            else -> getModernStatus(connectivityManager)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun getModernStatus(connectivityManager: ConnectivityManager): NetworkStatus {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        return if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            val type = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }

            NetworkStatus(
                isConnected = true,
                networkType = type,
                isMetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED).not(),
                bandwidthDownstream = capabilities.linkDownstreamBandwidthKbps,
                bandwidthUpstream = capabilities.linkUpstreamBandwidthKbps,
            )
        } else {
            NetworkStatus(
                isConnected = false,
                networkType = NetworkType.NONE,
                isMetered = false,
                bandwidthDownstream = 0,
                bandwidthUpstream = 0,
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun getLegacyStatus(connectivityManager: ConnectivityManager): NetworkStatus {
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        if (activeNetworkInfo?.isConnected != true) {
            return disconnectedStatus()
        }

        val type = when (activeNetworkInfo.type) {
            ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
            ConnectivityManager.TYPE_MOBILE -> NetworkType.CELLULAR
            ConnectivityManager.TYPE_ETHERNET -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }

        return NetworkStatus(
            isConnected = true,
            networkType = type,
            isMetered = connectivityManager.isActiveNetworkMetered,
            bandwidthDownstream = 0,
            bandwidthUpstream = 0,
        )
    }

    private fun disconnectedStatus(): NetworkStatus = NetworkStatus(
        isConnected = false,
        networkType = NetworkType.NONE,
        isMetered = false,
        bandwidthDownstream = 0,
        bandwidthUpstream = 0,
    )

    private fun startMonitoring() {
        val connectivityManager = connectivityManager ?: return
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        runCatching {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        }.onFailure {
            _isConnected.value = false
            _networkType.value = NetworkType.NONE
            return
        }
        updateNetworkStatus()
    }

    private fun updateNetworkStatus() {
        val status = getCurrentStatus()
        _isConnected.value = status.isConnected
        _networkType.value = status.networkType
    }

    /**
     * Stop monitoring network changes.
     */
    public fun stopMonitoring() {
        connectivityManager?.let {
            runCatching { it.unregisterNetworkCallback(networkCallback) }
        }
    }

    public companion object {
        private var instance: NetworkMonitor? = null

        /**
         * Get or create the NetworkMonitor instance.
         */
        public fun getInstance(context: Context): NetworkMonitor {
            val appContext = context.applicationContext ?: context
            return instance ?: NetworkMonitor(appContext).also {
                instance = it
            }
        }

        /**
         * Cleanup the NetworkMonitor instance.
         */
        public fun cleanup() {
            instance?.stopMonitoring()
            instance = null
        }
    }
}

/**
 * Network connectivity status information.
 */
public data class NetworkStatus(
    public val isConnected: Boolean,
    public val networkType: NetworkType,
    public val isMetered: Boolean,
    public val bandwidthDownstream: Int,
    public val bandwidthUpstream: Int,
)

/**
 * Types of network connections.
 */
public enum class NetworkType {
    NONE,
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER,
}
