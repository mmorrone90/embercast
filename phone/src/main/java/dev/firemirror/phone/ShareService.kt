package dev.firemirror.phone

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class ShareService : Service() {
    private data class CaptureSize(val width: Int, val height: Int)

    private val sharing = AtomicBoolean(false)
    @Volatile private var socket: Socket? = null
    @Volatile private var projection: MediaProjection? = null
    @Volatile private var encoder: MediaCodec? = null
    @Volatile private var virtualDisplay: VirtualDisplay? = null
    @Volatile private var inputSurface: Surface? = null
    @Volatile private var requestedSize: CaptureSize? = null
    @Volatile private var audioRecord: AudioRecord? = null
    @Volatile private var audioThread: Thread? = null
    @Volatile private var audioActive = false
    @Volatile private var phoneMuted = false
    private lateinit var phoneMediaVolume: PhoneMediaVolume

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        phoneMediaVolume = PhoneMediaVolume(this)
        phoneMediaVolume.restore()
        val notifications = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifications.createNotificationChannel(NotificationChannel(CHANNEL, "Screen sharing", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            finish("Sharing stopped")
            return START_NOT_STICKY
        }
        if (intent?.action != ACTION_START || !sharing.compareAndSet(false, true)) return START_NOT_STICKY
        running = true
        status = "Connecting to the TV"
        val notification = notification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else startForeground(NOTIFICATION_ID, notification)

        val host = intent.getStringExtra(EXTRA_HOST).orEmpty()
        val port = intent.getIntExtra(EXTRA_PORT, 7102)
        val result = intent.getIntExtra(EXTRA_RESULT, 0)
        val audioEnabled = intent.getBooleanExtra(EXTRA_AUDIO_ENABLED, false)
        @Suppress("DEPRECATION")
        val data = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
            else intent.getParcelableExtra(EXTRA_DATA)
        if (host.isBlank() || port !in 1..65535 || result == 0 || data == null) {
            finish("Screen sharing permission was not available")
            return START_NOT_STICKY
        }
        Thread({ share(host, port, result, data, audioEnabled) }, "PhoneMirrorSender").start()
        return START_NOT_STICKY
    }

    private fun share(host: String, port: Int, result: Int, data: Intent, audioEnabled: Boolean) {
        try {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            val initialSize = scaledSize(metrics.widthPixels, metrics.heightPixels)
            val connection = Socket()
            socket = connection
            connection.connect(InetSocketAddress(host, port), 8_000)
            connection.soTimeout = 35_000
            val output = DataOutputStream(connection.getOutputStream())
            val input = DataInputStream(connection.getInputStream())
            output.writeInt(PhoneMirrorPackets.MAGIC)
            output.writeInt(initialSize.width)
            output.writeInt(initialSize.height)
            output.flush()
            status = "Approve the phone on your TV"
            if (input.readUnsignedByte() != 1) {
                status = "The TV did not approve the connection"
                return
            }
            connection.soTimeout = 0

            val surface = createEncoder(initialSize)

            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val screen = projectionManager.getMediaProjection(result, data)
                ?: error("Screen capture permission expired")
            projection = screen
            screen.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { finish("Screen sharing ended") }
                override fun onCapturedContentResize(width: Int, height: Int) {
                    if (Build.VERSION.SDK_INT >= 34 && width > 0 && height > 0) {
                        requestedSize = scaledSize(width, height)
                    }
                }
            }, Handler(Looper.getMainLooper()))
            virtualDisplay = screen.createVirtualDisplay(
                "FireMirror", initialSize.width, initialSize.height, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null
            )
            if (audioEnabled) startPlaybackAudio(screen, output)
            status = sharingStatus()
            encodeLoop(output, initialSize, metrics.densityDpi)
        } catch (e: Exception) {
            if (sharing.get()) {
                Log.w(TAG, "Phone sharing failed", e)
                status = "Could not share: ${e.message ?: "connection lost"}"
            }
        } finally {
            finish(null)
        }
    }

    private fun scaledSize(width: Int, height: Int): CaptureSize {
        val scale = 960.0 / maxOf(width, height)
        return CaptureSize(
            ((width * scale).toInt() and -2).coerceAtLeast(64),
            ((height * scale).toInt() and -2).coerceAtLeast(64)
        )
    }

    private fun createEncoder(size: CaptureSize): Surface {
        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC, size.width, size.height
        ).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 2_500_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = codec.createInputSurface()
            codec.start()
            encoder = codec
            inputSurface = surface
            return surface
        } catch (e: Exception) {
            codec.release()
            throw e
        }
    }

    private fun startPlaybackAudio(screen: MediaProjection, output: DataOutputStream) {
        if (Build.VERSION.SDK_INT < 29 || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        try {
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(PhoneMirrorPackets.AUDIO_SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()
            val capture = AudioPlaybackCaptureConfiguration.Builder(screen)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()
            val minimum = AudioRecord.getMinBufferSize(
                PhoneMirrorPackets.AUDIO_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            require(minimum > 0) { "Audio capture format is not supported" }
            val record = AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(maxOf(minimum, PhoneMirrorPackets.AUDIO_CHUNK_BYTES * 4))
                .setAudioPlaybackCaptureConfig(capture)
                .build()
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                error("Audio capture could not start")
            }
            try {
                record.startRecording()
                check(record.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Audio capture was denied" }
                audioRecord = record
                PhoneMirrorPackets.audioFormat(output)
                audioActive = true
                phoneMuted = phoneMediaVolume.mute()
                audioThread = Thread({ sendAudio(record, output) }, "PhoneMirrorAudio").apply { start() }
            } catch (e: Exception) {
                runCatching { record.stop() }
                record.release()
                audioRecord = null
                audioActive = false
                phoneMuted = false
                phoneMediaVolume.restore()
                throw e
            }
        } catch (e: Exception) {
            Log.w(TAG, "Phone playback audio is unavailable", e)
        }
    }

    private fun sendAudio(record: AudioRecord, output: DataOutputStream) {
        val chunk = ByteArray(PhoneMirrorPackets.AUDIO_CHUNK_BYTES)
        var signalSeen = false
        var lastMuteCheck = 0L
        try {
            while (sharing.get()) {
                val count = record.read(chunk, 0, chunk.size, AudioRecord.READ_BLOCKING)
                if (count == 0) continue
                if (count < 0) {
                    Log.w(TAG, "Audio capture stopped: $count")
                    break
                }
                val now = SystemClock.elapsedRealtime()
                if (now - lastMuteCheck >= 500) {
                    val muted = phoneMediaVolume.mute()
                    if (muted != phoneMuted) {
                        phoneMuted = muted
                        status = sharingStatus()
                    }
                    lastMuteCheck = now
                }
                if (!signalSeen && (0 until count).any { chunk[it] != 0.toByte() }) {
                    signalSeen = true
                    Log.i(TAG, "Capturing non-silent app audio")
                }
                PhoneMirrorPackets.audio(output, System.nanoTime(), chunk, count)
            }
        } catch (e: Exception) {
            if (sharing.get()) Log.w(TAG, "Phone audio stream ended", e)
        } finally {
            audioActive = false
            phoneMediaVolume.restore()
            phoneMuted = false
            if (sharing.get()) {
                runCatching { PhoneMirrorPackets.audioEnded(output) }
                status = "Showing your screen on FireMirror; phone audio is unavailable"
            }
        }
    }

    private fun encodeLoop(output: DataOutputStream, initialSize: CaptureSize, densityDpi: Int) {
        val info = MediaCodec.BufferInfo()
        var currentSize = initialSize
        var codec = encoder ?: error("Screen encoder was not started")
        var config = ByteArray(0)
        var lastSizeCheck = 0L
        while (sharing.get()) {
            if (Build.VERSION.SDK_INT < 34 && System.currentTimeMillis() - lastSizeCheck >= 500) {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
                requestedSize = scaledSize(metrics.widthPixels, metrics.heightPixels)
                lastSizeCheck = System.currentTimeMillis()
            }
            val nextSize = requestedSize
            if (nextSize != null) {
                requestedSize = null
                if (nextSize != currentSize) {
                    val display = virtualDisplay ?: error("Screen projection ended")
                    display.setSurface(null)
                    codec.stop()
                    codec.release()
                    inputSurface?.release()
                    encoder = null
                    inputSurface = null
                    val surface = createEncoder(nextSize)
                    display.resize(nextSize.width, nextSize.height, densityDpi)
                    display.setSurface(surface)
                    PhoneMirrorPackets.resize(output, nextSize.width, nextSize.height)
                    codec = encoder ?: error("Screen encoder restart failed")
                    currentSize = nextSize
                    config = ByteArray(0)
                    status = sharingStatus()
                }
            }
            when (val index = codec.dequeueOutputBuffer(info, 10_000)) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val format = codec.outputFormat
                    config = listOf("csd-0", "csd-1").mapNotNull { key ->
                        format.getByteBuffer(key)?.let { buffer ->
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            AvcAnnexB.normalize(bytes)
                        }
                    }.fold(ByteArray(0)) { acc, bytes -> acc + bytes }
                }
                in 0..Int.MAX_VALUE -> {
                    try {
                        if (info.size <= 0) continue
                        val buffer = codec.getOutputBuffer(index) ?: continue
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        val raw = ByteArray(info.size)
                        buffer.get(raw)
                        val normalized = AvcAnnexB.normalize(raw) ?: continue
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            config = normalized
                            continue
                        }
                        val key = info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0
                        val packet = if (key && config.isNotEmpty()) config + normalized else normalized
                        PhoneMirrorPackets.video(output, info.presentationTimeUs * 1_000, packet)
                    } finally {
                        codec.releaseOutputBuffer(index, false)
                    }
                }
            }
        }
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, ShareService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("FireMirror is sharing your screen")
            .setContentText("Tap to return to FireMirror")
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stop)
            .setOngoing(true)
            .build()
    }

    private fun sharingStatus() = when {
        audioActive && phoneMuted -> "Showing your screen and app sound on FireMirror; phone media is silent"
        audioActive -> "Showing your screen and app sound on FireMirror; phone media may also play"
        else -> "Showing your screen on FireMirror; phone audio is unavailable"
    }

    private fun finish(message: String?) {
        if (!sharing.getAndSet(false)) return
        running = false
        if (message != null) status = message
        runCatching { socket?.close() }
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        runCatching { virtualDisplay?.release() }
        runCatching { projection?.stop() }
        runCatching { encoder?.stop() }
        runCatching { encoder?.release() }
        runCatching { inputSurface?.release() }
        socket = null
        virtualDisplay = null
        projection = null
        encoder = null
        inputSurface = null
        audioRecord = null
        audioThread = null
        audioActive = false
        phoneMediaVolume.restore()
        phoneMuted = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        finish("Sharing stopped")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ShareService"
        private const val CHANNEL = "firemirror_sharing"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "dev.firemirror.phone.START"
        const val ACTION_STOP = "dev.firemirror.phone.STOP"
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
        const val EXTRA_RESULT = "result"
        const val EXTRA_DATA = "data"
        const val EXTRA_AUDIO_ENABLED = "audio_enabled"
        @Volatile var status = "Ready to share"
        @Volatile var running = false
    }
}
