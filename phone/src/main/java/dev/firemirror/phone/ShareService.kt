package dev.firemirror.phone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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
    private val sharing = AtomicBoolean(false)
    @Volatile private var socket: Socket? = null
    @Volatile private var projection: MediaProjection? = null
    @Volatile private var encoder: MediaCodec? = null
    @Volatile private var virtualDisplay: VirtualDisplay? = null
    @Volatile private var inputSurface: Surface? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
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
        @Suppress("DEPRECATION")
        val data = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
            else intent.getParcelableExtra(EXTRA_DATA)
        if (host.isBlank() || port !in 1..65535 || result == 0 || data == null) {
            finish("Screen sharing permission was not available")
            return START_NOT_STICKY
        }
        Thread({ share(host, port, result, data) }, "PhoneMirrorSender").start()
        return START_NOT_STICKY
    }

    private fun share(host: String, port: Int, result: Int, data: Intent) {
        try {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            val scale = 960.0 / maxOf(metrics.widthPixels, metrics.heightPixels)
            val width = ((metrics.widthPixels * scale).toInt() and -2).coerceAtLeast(64)
            val height = ((metrics.heightPixels * scale).toInt() and -2).coerceAtLeast(64)
            val connection = Socket()
            socket = connection
            connection.connect(InetSocketAddress(host, port), 8_000)
            connection.soTimeout = 35_000
            val output = DataOutputStream(connection.getOutputStream())
            val input = DataInputStream(connection.getInputStream())
            output.writeInt(MAGIC)
            output.writeInt(width)
            output.writeInt(height)
            output.flush()
            status = "Approve the phone on your TV"
            if (input.readUnsignedByte() != 1) {
                status = "The TV did not approve the connection"
                return
            }
            connection.soTimeout = 0

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 2_500_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder = codec
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = codec.createInputSurface()
            inputSurface = surface
            codec.start()

            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val screen = projectionManager.getMediaProjection(result, data)
                ?: error("Screen capture permission expired")
            projection = screen
            screen.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { finish("Screen sharing ended") }
            }, Handler(Looper.getMainLooper()))
            virtualDisplay = screen.createVirtualDisplay(
                "FireMirror", width, height, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null
            )
            status = "Showing your phone on FireMirror"
            encodeLoop(codec, output)
        } catch (e: Exception) {
            if (sharing.get()) {
                Log.w(TAG, "Phone sharing failed", e)
                status = "Could not share: ${e.message ?: "connection lost"}"
            }
        } finally {
            finish(null)
        }
    }

    private fun encodeLoop(codec: MediaCodec, output: DataOutputStream) {
        val info = MediaCodec.BufferInfo()
        var config = ByteArray(0)
        while (sharing.get()) {
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
                        output.writeInt(packet.size)
                        output.writeLong(info.presentationTimeUs * 1_000)
                        output.write(packet)
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

    private fun finish(message: String?) {
        if (!sharing.getAndSet(false)) return
        running = false
        if (message != null) status = message
        runCatching { socket?.close() }
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
        private const val MAGIC = 0x464D5031
        const val ACTION_START = "dev.firemirror.phone.START"
        const val ACTION_STOP = "dev.firemirror.phone.STOP"
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
        const val EXTRA_RESULT = "result"
        const val EXTRA_DATA = "data"
        @Volatile var status = "Ready to share"
        @Volatile var running = false
    }
}
