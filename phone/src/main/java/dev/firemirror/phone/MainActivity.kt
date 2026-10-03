package dev.firemirror.phone

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private data class Tv(val name: String, val host: String, val port: Int)

    private val devices = linkedMapOf<String, Tv>()
    private var chosen: Tv? = null
    private lateinit var manager: NsdManager
    private var discovery: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private lateinit var deviceList: LinearLayout
    private lateinit var status: TextView
    private lateinit var address: EditText
    private lateinit var connect: Button
    private lateinit var stop: Button
    private lateinit var testSound: Button
    private val handler = Handler(Looper.getMainLooper())
    private val updateStatus = object : Runnable {
        override fun run() {
            if (::status.isInitialized) status.text = ShareService.status
            if (::stop.isInitialized) stop.isEnabled = ShareService.running
            if (::testSound.isInitialized) testSound.isEnabled = ShareService.running && ShareService.status.startsWith("Showing")
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ShareService.running) PhoneMediaVolume(this).restore()
        window.statusBarColor = BACKGROUND
        window.navigationBarColor = BACKGROUND
        manager = getSystemService(Context.NSD_SERVICE) as NsdManager
        buildUi()
    }

    override fun onStart() {
        super.onStart()
        startDiscovery()
        handler.post(updateStatus)
    }

    override fun onStop() {
        handler.removeCallbacks(updateStatus)
        stopDiscovery()
        super.onStop()
    }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(36))
            setBackgroundColor(BACKGROUND)
        }
        val scroll = ScrollView(this).apply { addView(page) }
        setContentView(scroll)

        page.addView(label("FIREMIRROR", 13f, ACCENT, true).apply { letterSpacing = .18f })
        page.addView(label("Your phone.\nOn the big screen.", 34f, Color.WHITE, true).apply {
            setPadding(0, dp(20), 0, dp(10))
        })
        @Suppress("DEPRECATION")
        val version = packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
        page.addView(label("FireMirror Phone $version", 13f, MUTED, false))
        page.addView(label("Find your Fire TV on the same WiFi, then share your screen.", 16f, MUTED, false))

        page.addView(label("NEARBY FIRE TV", 12f, ACCENT, true).apply {
            setPadding(0, dp(34), 0, dp(12))
        })
        deviceList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(deviceList)
        renderDevices()

        page.addView(label("TV address", 15f, Color.WHITE, true).apply {
            setPadding(0, dp(26), 0, dp(8))
        })
        address = EditText(this).apply {
            setSingleLine(true)
            hint = "Enter the TV address if it is not found"
            setText(getPreferences(MODE_PRIVATE).getString("tv_address", ""))
            setTextColor(Color.WHITE)
            setHintTextColor(MUTED)
            textSize = 15f
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = rounded(PANEL, 14)
        }
        page.addView(address)

        connect = button("Mirror this phone", ACCENT, BACKGROUND).apply {
            setOnClickListener { beginShare() }
        }
        page.addView(connect, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(26) })
        stop = button("Stop sharing", PANEL, Color.WHITE).apply {
            isEnabled = false
            setOnClickListener {
                startService(Intent(this@MainActivity, ShareService::class.java).setAction(ShareService.ACTION_STOP))
            }
        }
        page.addView(stop, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })
        testSound = button("Test TV sound", PANEL, Color.WHITE).apply {
            isEnabled = false
            setOnClickListener { TestSound.play() }
        }
        page.addView(testSound, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })
        status = label("Waiting for a TV", 14f, MUTED, false).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(10))
        }
        page.addView(status)
        page.addView(label("The TV will ask you to approve the connection. Android will ask to capture your screen and supported app audio. While sharing app sound, your phone's media speaker is muted and its previous volume is restored when you stop. Some video apps block screen capture; if their picture is black on the TV, use their Fire TV app. Use a trusted WiFi network.", 13f, MUTED, false).apply {
            setPadding(0, dp(24), 0, 0)
        })
    }

    private fun beginShare() {
        val manual = address.text.toString().trim()
        val tv = if (manual.isNotEmpty()) Tv("FireMirror", manual, 7102) else chosen
        if (tv == null) {
            status.text = "Choose a nearby TV or enter its address."
            return
        }
        getPreferences(MODE_PRIVATE).edit().putString("tv_address", manual).apply()
        chosen = tv
        if (Build.VERSION.SDK_INT >= 29 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO)
        } else {
            requestScreenCapture()
        }
    }

    private fun requestScreenCapture() {
        val projection = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projection.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO) {
            if (grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
                status.text = "Audio permission declined. Sharing video only."
            }
            requestScreenCapture()
        }
    }

    @Deprecated("The platform result API is sufficient for this small standalone sender")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CAPTURE || resultCode != RESULT_OK || data == null) return
        val tv = chosen ?: return
        ShareService.status = "Waiting for approval on the TV"
        val intent = Intent(this, ShareService::class.java).apply {
            action = ShareService.ACTION_START
            putExtra(ShareService.EXTRA_HOST, tv.host)
            putExtra(ShareService.EXTRA_PORT, tv.port)
            putExtra(ShareService.EXTRA_RESULT, resultCode)
            putExtra(ShareService.EXTRA_DATA, data)
            putExtra(ShareService.EXTRA_AUDIO_ENABLED,
                Build.VERSION.SDK_INT >= 29 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        }
        startForegroundService(intent)
    }

    private fun startDiscovery() {
        if (discovery != null) return
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("firemirror_phone_discovery").apply {
            setReferenceCounted(false)
            acquire()
        }
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type: String) = Unit
            override fun onDiscoveryStopped(type: String) = Unit
            override fun onStartDiscoveryFailed(type: String, code: Int) = runOnUiThread {
                status.text = "Automatic discovery unavailable. Enter the TV address."
            }
            override fun onStopDiscoveryFailed(type: String, code: Int) = Unit
            override fun onServiceLost(info: NsdServiceInfo) = runOnUiThread {
                devices.remove(info.serviceName)
                if (chosen?.name == info.serviceName) chosen = null
                renderDevices()
            }
            override fun onServiceFound(info: NsdServiceInfo) {
                manager.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, code: Int) = Unit
                    override fun onServiceResolved(info: NsdServiceInfo) = runOnUiThread {
                        val host = info.host?.hostAddress ?: return@runOnUiThread
                        devices[info.serviceName] = Tv(info.serviceName, host, info.port)
                        if (chosen == null) chosen = devices[info.serviceName]
                        renderDevices()
                    }
                })
            }
        }
        discovery = listener
        try { manager.discoverServices("_firemirror._tcp", NsdManager.PROTOCOL_DNS_SD, listener) }
        catch (e: Exception) {
            discovery = null
            multicastLock?.release()
            multicastLock = null
            status.text = "Automatic discovery unavailable. Enter the TV address."
        }
    }

    private fun stopDiscovery() {
        discovery?.let { runCatching { manager.stopServiceDiscovery(it) } }
        discovery = null
        multicastLock?.let { runCatching { it.release() } }
        multicastLock = null
    }

    private fun renderDevices() {
        deviceList.removeAllViews()
        if (devices.isEmpty()) {
            deviceList.addView(label("Searching for FireMirror…", 15f, MUTED, false).apply {
                setPadding(dp(16), dp(20), dp(16), dp(20))
                background = rounded(PANEL, 16)
            })
            return
        }
        devices.values.forEach { tv ->
            val selected = chosen?.host == tv.host && chosen?.port == tv.port
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(14), dp(18), dp(14))
                background = rounded(if (selected) SELECTED else PANEL, 16)
                isClickable = true
                setOnClickListener {
                    chosen = tv
                    address.text.clear()
                    renderDevices()
                }
                addView(label("${if (selected) "● " else "○ "}${tv.name}", 18f, Color.WHITE, true))
                addView(label(tv.host, 13f, MUTED, false))
            }
            deviceList.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun button(value: String, backgroundColor: Int, textColor: Int) = Button(this).apply {
        text = value
        textSize = 16f
        isAllCaps = false
        setTextColor(textColor)
        background = rounded(backgroundColor, 16)
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (resources.displayMetrics.density * value).toInt()

    companion object {
        private const val REQUEST_CAPTURE = 20
        private const val REQUEST_AUDIO = 21
        private const val BACKGROUND = 0xFF0A1514.toInt()
        private const val PANEL = 0xFF1D302E.toInt()
        private const val SELECTED = 0xFF2F5550.toInt()
        private const val ACCENT = 0xFF6CDCCF.toInt()
        private const val MUTED = 0xFFADC1BD.toInt()
    }
}
