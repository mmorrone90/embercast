package dev.firemirror.tv

import android.os.Bundle
import android.view.ViewGroup
import dev.firemirror.tv.databinding.ActivityDiagnosticsBinding

class DiagnosticsActivity : TvActivity() {
    private lateinit var binding: ActivityDiagnosticsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostics)
        binding = ActivityDiagnosticsBinding.bind(findViewById<ViewGroup>(android.R.id.content).getChildAt(0))
        binding.backButton.setOnClickListener { finish() }
        binding.refreshButton.setOnClickListener { refresh() }
        refresh()
    }

    private fun refresh() {
        binding.refreshButton.isEnabled = false
        val context = applicationContext
        DeviceDiagnostics.executor.execute {
            val report = runCatching { DeviceDiagnostics.report(context) }
                .getOrElse { "Diagnostics unavailable: ${it.javaClass.simpleName}" }
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    binding.report.text = report
                    binding.refreshButton.isEnabled = true
                }
            }
        }
    }
}
