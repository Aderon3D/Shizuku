package moe.shizuku.manager.core.platform.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.ext.SdkExtensions
import android.util.Log
import androidx.annotation.RequiresApi
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.isValidPort
import moe.shizuku.manager.core.extensions.resultOf
import moe.shizuku.manager.core.platform.services.systemService
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket

@RequiresApi(Build.VERSION_CODES.R)
class AdbMdns(context: Context) {

    private val nsdManager: NsdManager by systemService(context)

    val connectFlow: Flow<Int> = portFlow(ServiceType.CONNECT.value)
    val pairingFlow: Flow<Int> = portFlow(ServiceType.PAIRING.value)

    private fun portFlow(serviceType: String): Flow<Int> = callbackFlow {
        var currentServiceName: String? = null

        val listener = createDiscoveryListener(
            onServiceResolved = { resolvedService ->
                if (resolvedService.isValidService) {
                    currentServiceName = resolvedService.serviceName
                    trySend(resolvedService.port)
                }
            },
            onServiceLost = { serviceInfo ->
                if (serviceInfo.serviceName == currentServiceName) {
                    trySend(-1)
                }
            },
            onStartDiscoveryFailed = { close(it) },
            onStopped = { close() }
        )

        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)

        awaitClose {
            nsdManager.stopServiceDiscovery(listener)
        }
    }

    private fun createDiscoveryListener(
        onServiceResolved: (NsdServiceInfo) -> Unit,
        onServiceLost: (NsdServiceInfo) -> Unit,
        onStartDiscoveryFailed: (Throwable) -> Unit,
        onStopped: () -> Unit
    ) = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "onStartDiscoveryFailed: $errorCode")
            onStartDiscoveryFailed(RuntimeException("Start discovery failed: $errorCode"))
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "onStopDiscoveryFailed: $errorCode")
        }

        override fun onDiscoveryStarted(serviceType: String) {
            Log.d(TAG, "onDiscoveryStarted: $serviceType")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.d(TAG, "onDiscoveryStopped: $serviceType")
            onStopped()
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            Log.i(TAG, "onServiceFound: $serviceInfo")

            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    Log.e(TAG, "onResolveFailed: $errorCode")
                }

                override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                    Log.i(TAG, "onServiceResolved: $resolvedService")
                    onServiceResolved(resolvedService)
                }
            })
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            Log.i(TAG, "onServiceLost: $serviceInfo")
            onServiceLost(serviceInfo)
        }
    }

    private val NsdServiceInfo.isValidService: Boolean
        get() = this.hasValidHostAddress
                && this.port.isPortAvailable
                && this.port.isValidPort

    private val NsdServiceInfo.hasValidHostAddress: Boolean
        get() = resultOf {
            val serviceAddrs = this.hostAddressesCompat

            val localAddrs = NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { it.inetAddresses.asSequence() }

            localAddrs.any { it in serviceAddrs }
        }.getOrElse { if (it is IOException) false else throw it }

    private val NsdServiceInfo.hostAddressesCompat: Set<InetAddress>
        get() = if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 7) {
            hostAddresses.toSet()
        } else {
            @Suppress("DEPRECATION")
            setOfNotNull(host)
        }

    private val Int.isPortAvailable: Boolean
        get() = resultOf {
            ServerSocket().use {
                it.bind(InetSocketAddress("127.0.0.1", this))
            }
        }.fold(
            success = { false },
            failure = { if (it is IOException) true else throw it }
        )

    private enum class ServiceType(val value: String) {
        CONNECT("_adb-tls-connect._tcp"),
        PAIRING("_adb-tls-pairing._tcp")
    }
}