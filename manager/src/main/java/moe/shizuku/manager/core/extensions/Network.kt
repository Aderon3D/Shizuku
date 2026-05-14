package moe.shizuku.manager.core.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import moe.shizuku.manager.core.platform.services.systemService

private val Context.connectivityManager: ConnectivityManager by systemService()

val Context.isWifiConnected: Boolean
    get() {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }