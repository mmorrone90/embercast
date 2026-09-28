package dev.firemirror.tv

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.util.Log
import dev.firemirror.tv.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : TvActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: ReceiverPreferences
    private lateinit var network: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.bind(findViewById<ViewGroup>(android.R.id.content).getChildAt(0))
        preferences = ReceiverPreferences(this)
        network = NetworkMonitor(this)
        binding.settingsButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.diagnosticsButton.setOnClickListener { startActivity(Intent(this, DiagnosticsActivity::class.java)) }
        render(ReceiverSnapshot())
    }

    override fun onStart() {
        super.onStart()
        if (preferences.diagnosticLogging) Log.d(TAG, "Main screen started; receiver unavailable")
        binding.receiverName.text = preferences.receiverName
        binding.instructions.text = getString(R.string.mac_instructions, preferences.receiverName)
        network.start { ip ->
            binding.localIp.text = ip ?: getString(R.string.network_unavailable)
            if (preferences.diagnosticLogging) Log.d(TAG, "LAN available=${ip != null}; protocol unavailable")
        }
    }

    override fun onStop() {
        network.stop()
        if (preferences.diagnosticLogging) Log.d(TAG, "Main screen stopped")
        super.onStop()
    }

    private fun render(snapshot: ReceiverSnapshot) {
        binding.connectionStatus.setText(snapshot.state.label)
        binding.connectedDevice.text = snapshot.deviceName ?: getString(R.string.not_connected)
        binding.resolution.text = if (snapshot.width != null && snapshot.height != null)
            "${snapshot.width} × ${snapshot.height}" else getString(R.string.no_stream)
        binding.fps.text = snapshot.framesPerSecond?.let { String.format(Locale.ROOT, "%.1f FPS", it) }
            ?: getString(R.string.no_stream)
    }

    companion object { const val TAG = "FireMirror" }
}
