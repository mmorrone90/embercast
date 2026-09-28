package dev.firemirror.tv

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import java.net.Inet4Address

/** Watches LAN interfaces, even when the LAN has no internet access. */
class NetworkMonitor(context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var listener: ((String?) -> Unit)? = null

    fun start(onChange: (String?) -> Unit) {
        if (callback != null) return
        listener = onChange
        val observer = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = notifyChanged()
            override fun onLost(network: Network) = notifyChanged()
            override fun onLinkPropertiesChanged(network: Network, properties: LinkProperties) = notifyChanged()
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = notifyChanged()
        }
        // No INTERNET/VALIDATED requirement: isolated WiFi must work as well.
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()
        connectivity.registerNetworkCallback(request, observer)
        callback = observer
        notifyChanged()
    }

    fun stop() {
        callback?.let { connectivity.unregisterNetworkCallback(it) }
        callback = null
        listener = null
        main.removeCallbacksAndMessages(null)
    }

    @Suppress("DEPRECATION") // allNetworks supports LANs that are not the default internet route.
    fun localIpv4(): String? = connectivity.allNetworks
        .filter { network ->
            val caps = connectivity.getNetworkCapabilities(network)
            caps != null && !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        }
        .flatMap { connectivity.getLinkProperties(it)?.linkAddresses.orEmpty() }
        .map { it.address }
        .filterIsInstance<Inet4Address>()
        .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress && !it.isAnyLocalAddress }
        ?.hostAddress

    private fun notifyChanged() {
        main.post { listener?.invoke(localIpv4()) }
    }
}
