package dev.firemirror.tv

import android.content.Context
import android.media.MediaCodecList
import android.os.Build
import java.util.concurrent.Executors

object DeviceDiagnostics {
    // Enumerating vendor codecs can be slow. Do not do it on the main thread.
    val executor = Executors.newSingleThreadExecutor()

    fun report(context: Context): String = buildString {
        appendLine("FireMirror 0.1.0 · Phase 1")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Build: ${Build.DISPLAY}")
        appendLine("ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
        appendLine("Local IPv4: ${NetworkMonitor(context).localIpv4() ?: "unavailable"}")
        appendLine("Protocol: not installed")
        appendLine("Discovery: not advertised")
        appendLine("Video and audio: not connected")
        appendLine()
        appendLine("Available video decoders (not a playback test):")
        runCatching {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
                .filter { !it.isEncoder }
                .forEach { codec ->
                    val videoTypes = codec.supportedTypes.filter { it == "video/avc" || it == "video/hevc" }
                    if (videoTypes.isNotEmpty()) {
                        val kind = if (Build.VERSION.SDK_INT >= 29)
                            if (codec.isHardwareAccelerated) "hardware" else "software"
                        else "hardware status unavailable on this API"
                        appendLine("${codec.name}\n  ${videoTypes.joinToString()} · $kind")
                    }
                }
        }.onFailure { appendLine("Decoder query failed: ${it.javaClass.simpleName}") }
    }
}
