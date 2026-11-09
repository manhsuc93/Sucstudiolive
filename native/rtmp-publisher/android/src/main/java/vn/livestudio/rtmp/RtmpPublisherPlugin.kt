package vn.livestudio.rtmp

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaCodecList
import android.hardware.camera2.CameraCharacteristics
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log

import com.getcapacitor.*
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission

import com.haishinkit.codec.VideoCodecProfileLevel
import com.haishinkit.event.Event
import com.haishinkit.event.IEventListener
import com.haishinkit.media.source.AudioRecordSource
import com.haishinkit.media.source.CameraSource
import com.haishinkit.rtmp.RtmpConnection
import com.haishinkit.rtmp.RtmpStream
import com.haishinkit.screen.Image as HkImage

@CapacitorPlugin(
    name = "RtmpPublisher",
    permissions = [
        Permission(
            alias = "camera",
            strings = [Manifest.permission.CAMERA]
        ),
        Permission(
            alias = "microphone",
            strings = [Manifest.permission.RECORD_AUDIO]
        )
    ]
)
class RtmpPublisherPlugin : Plugin(), IEventListener {

    companion object {
        private const val TAG = "RtmpPublisher"
    }

    private val main = Handler(Looper.getMainLooper())

    private var connection: RtmpConnection? = null
    private var stream: RtmpStream? = null
    private var camera: CameraSource? = null
    private var overlayImage: HkImage? = null

    private var pendingStart: PluginCall? = null
    private var streamKey: String? = null

    @Volatile
    private var starting = false

    @Volatile
    private var publishing = false

    @Volatile
    private var activeCodec = "h264"

    private val stateLock = Any()

    // ---------------------------------------------------------
    // START PUBLISHING
    // ---------------------------------------------------------

    @PluginMethod
    fun start(call: PluginCall) {

        val url = call.getString("url")?.trim().orEmpty()
        val key = call.getString("key")?.trim().orEmpty()

        val video = call.getObject("video")
        val audio = call.getObject("audio")

        val requestedCodec = video
            ?.optString("codec", "h264")
            ?.lowercase()
            ?: "h264"

        // Accept both h265 and hevc names.
        val codec = when (requestedCodec) {
            "h264" -> "h264"
            "h265", "hevc" -> "hevc"
            else -> {
                call.reject(
                    "Codec chỉ hỗ trợ H.264 hoặc H.265.",
                    "UNSUPPORTED_CODEC"
                )
                return
            }
        }

        if (!url.startsWith("rtmp://") &&
            !url.startsWith("rtmps://")
        ) {
            call.reject(
                "URL phải bắt đầu bằng rtmp:// hoặc rtmps://",
                "INVALID_RTMP_URL"
            )
            return
        }

        if (key.isBlank()) {
            call.reject(
                "Thiếu Stream Key.",
                "MISSING_STREAM_KEY"
            )
            return
        }

        if (codec == "hevc" && !isHevcHardwareSupported()) {
            call.reject(
                "Thiết bị không có bộ mã hóa H.265 phần cứng.",
                "HEVC_NOT_SUPPORTED"
            )
            return
        }

        synchronized(stateLock) {
            if (starting || publishing || connection != null) {
                call.reject(
                    "Đang có một phiên phát RTMP hoạt động.",
                    "PUBLISHER_BUSY"
                )
                return
            }

            starting = true
            publishing = false
            pendingStart = call
            streamKey = key
            activeCodec = codec
        }

        if (!hasPermission("camera") ||
            !hasPermission("microphone")
        ) {
            requestPermissionForAliases(
                arrayOf("camera", "microphone"),
                call,
                "permissionsForStart"
            )
            return
        }

        startAfterPermission(
            url,
            key,
            video,
            audio,
            codec,
            call
        )
    }

    // ---------------------------------------------------------
    // PERMISSIONS
    // ---------------------------------------------------------

    @PermissionCallback
    private fun permissionsForStart(call: PluginCall) {

        if (!hasPermission("camera") ||
            !hasPermission("microphone")
        ) {
            fail(
                call,
                "Bạn cần cấp quyền Camera và Micro để phát trực tiếp.",
                "PERMISSION_DENIED"
            )
            return
        }

        val url = call.getString("url")?.trim().orEmpty()
        val key = call.getString("key")?.trim().orEmpty()

        val video = call.getObject("video")
        val audio = call.getObject("audio")

        val requestedCodec = video
            ?.optString("codec", "h264")
            ?.lowercase()
            ?: "h264"

        val codec = if (requestedCodec == "h265" ||
            requestedCodec == "hevc"
        ) "hevc" else "h264"

        startAfterPermission(
            url,
            key,
            video,
            audio,
            codec,
            call
        )
    }

    // ---------------------------------------------------------
    // CREATE CAMERA / AUDIO / RTMP STREAM
    // ---------------------------------------------------------

    private fun startAfterPermission(
        url: String,
        key: String,
        video: JSObject?,
        audio: JSObject?,
        codec: String,
        call: PluginCall
    ) {

        try {

            val width = (video?.getInteger("width") ?: 1280)
                .coerceIn(320, 1920)

            val height = (video?.getInteger("height") ?: 720)
                .coerceIn(240, 1920)

            val fps = (video?.getInteger("fps") ?: 30)
                .coerceIn(15, 60)

            val bitrate = (video?.getInteger("bitrate") ?: 2_500_000)
                .coerceIn(250_000, 12_000_000)

            val audioBitrate =
                (audio?.getInteger("bitrate") ?: 128_000)
                    .coerceIn(32_000, 320_000)

            val cameraFacing =
                call.getString("cameraFacing", "environment")
                    ?: "environment"

            val facing = if (cameraFacing == "user") {
                CameraCharacteristics.LENS_FACING_FRONT
            } else {
                CameraCharacteristics.LENS_FACING_BACK
            }

            // Create RTMP connection.
            val conn = RtmpConnection()

            // Create publishing stream.
            val rtmp = RtmpStream(context, conn)

            // Create native camera source.
            val cam = CameraSource(context).apply {
                open(facing)
            }

            // Attach microphone.
            rtmp.attachAudio(AudioRecordSource())

            // Attach camera.
            rtmp.attachVideo(cam)

            // Configure video.
            rtmp.videoSettings.width = width
            rtmp.videoSettings.height = height
            rtmp.videoSettings.bitrate = bitrate
            rtmp.videoSettings.IFrameInterval = 2

            // Configure H.264 / H.265 profile.
            rtmp.videoSettings.profileLevel =
                if (codec == "hevc") {
                    VideoCodecProfileLevel.HEVC_MAIN_3_1
                } else {
                    VideoCodecProfileLevel.H264_BASELINE_3_1
                }

            // Configure AAC audio bitrate.
            rtmp.audioSettings.bitrate = audioBitrate

            synchronized(stateLock) {
                connection = conn
                stream = rtmp
                camera = cam
                activeCodec = codec
                streamKey = key
            }

            // Create overlay graphics.
            val overlay = call.getObject("overlay")

            overlayImage = buildOverlayImage(
                overlay,
                width,
                height
            )?.also {
                it.frame.set(0, 0, width, height)
                rtmp.screen.addChild(it)
            }

            // Listen for RTMP events.
            conn.addEventListener(Event.RTMP_STATUS, this)
            conn.addEventListener(Event.IO_ERROR, this)

            Log.i(
                TAG,
                "Connecting RTMP: $url, " +
                    "codec=$codec, " +
                    "resolution=${width}x$height, " +
                    "fps=$fps, bitrate=$bitrate"
            )

            // Connect directly to the destination server.
            conn.connect(url)

        } catch (e: Exception) {

            Log.e(TAG, "RTMP initialization failed", e)

            fail(
                call,
                e.message ?: "Không thể khởi tạo bộ phát RTMP.",
                "NATIVE_START_FAILED"
            )
        }
    }

    // ---------------------------------------------------------
    // RTMP EVENTS
    // ---------------------------------------------------------
override fun handleEvent(event: Event) {
    val payload = try {
        EventUtils.toMap(event)
    } catch (_: Exception) {
        emptyMap<String, Any>()
    }

    val code = payload["code"]?.toString().orEmpty()
    val eventType = event.type // Phân biệt loại event (RTMP_STATUS hay IO_ERROR)

    main.post {
        if (eventType == Event.IO_ERROR || code.contains("Closed", true) || code.contains("Failed", true)) {
            // Xử lý khi mất kết nối hoặc lỗi IO
            val pending = synchronized(stateLock) { pendingStart }
            if (pending != null) {
                fail(pending, "Không thể kết nối đến máy chủ RTMP: $code", "RTMP_CONNECTION_FAILED")
            } else {
                val status = JSObject().apply {
                    put("state", "disconnected")
                    put("code", code)
                    put("message", "Kết nối RTMP đã bị gián đoạn.")
                }
                notifyListeners("rtmpStatus", status)
                cleanup(false)
            }
            return@post
        }

        when {
            // RTMP connection succeeded.
            code == RtmpConnection.Code.CONNECT_SUCCESS.rawValue -> {
                val key = streamKey
                if (key.isNullOrBlank()) {
                    pendingStart?.let { fail(it, "Không tìm thấy Stream Key.", "MISSING_STREAM_KEY") }
                } else {
                    try {
                        stream?.publish(key)
                        Log.i(TAG, "RTMP connected. Publishing stream.")
                    } catch (e: Exception) {
                        pendingStart?.let { fail(it, e.message ?: "Không thể bắt đầu publish.", "RTMP_PUBLISH_FAILED") }
                    }
                }
            }

            // Server accepted publishing.
            code.contains("NetStream.Publish.Start", ignoreCase = true) -> {
                val call = synchronized(stateLock) {
                    val current = pendingStart
                    pendingStart = null
                    starting = false
                    publishing = true
                    current
                }

                call?.let {
                    val result = JSObject().apply {
                        put("state", "live")
                        put("codec", activeCodec)
                        put("videoCodec", activeCodec)
                        put("audioCodec", "aac")
                        put("protocol", "rtmp")
                        put("message", "Máy chủ đã chấp nhận luồng phát.")
                    }
                    it.resolve(result)
                }

                val status = JSObject().apply {
                    put("state", "live")
                    put("codec", activeCodec)
                }
                notifyListeners("rtmpStatus", status)
                Log.i(TAG, "RTMP publishing started.")
            }

            // Server rejected or closed publishing.
            code.contains("Rejected", true) ||
            code.contains("BadName", true) ||
            code.contains("Publish.BadName", true) -> {
                val pending = synchronized(stateLock) { pendingStart }
                if (pending != null) {
                    fail(pending, "Máy chủ RTMP từ chối phiên phát: $code", "RTMP_PUBLISH_REJECTED")
                } else {
                    val status = JSObject().apply {
                        put("state", "disconnected")
                        put("code", code)
                        put("message", "Kết nối RTMP đã bị đóng.")
                    }
                    notifyListeners("rtmpStatus", status)
                    cleanup(false)
                }
            }
        }
    }
}


    @PluginMethod
    fun stop(call: PluginCall) {

        cleanup(true)

        val result = JSObject()

        result.put("state", "stopped")
        result.put("message", "Đã dừng phát RTMP.")

        call.resolve(result)
    }

    // ---------------------------------------------------------
    // UPDATE OVERLAY WHILE STREAMING
    // ---------------------------------------------------------

    @PluginMethod
    fun updateOverlay(call: PluginCall) {

        val currentStream = stream

        if (currentStream == null || !publishing) {
            call.reject(
                "Chưa có phiên phát RTMP để cập nhật overlay.",
                "NOT_LIVE"
            )
            return
        }

        try {

            val width = currentStream.videoSettings.width
            val height = currentStream.videoSettings.height

            val bitmap = renderOverlay(
                call.data,
                width,
                height
            )

            val image = overlayImage ?: HkImage().also {

                it.frame.set(0, 0, width, height)

                currentStream.screen.addChild(it)

                overlayImage = it
            }

            image.bitmap = bitmap

            call.resolve()

        } catch (e: Exception) {

            Log.e(TAG, "Overlay update failed", e)

            call.reject(
                "Không thể cập nhật overlay: ${e.message}",
                "OVERLAY_UPDATE_FAILED"
            )
        }
    }

    // ---------------------------------------------------------
    // STATUS
    // ---------------------------------------------------------

    @PluginMethod
    fun getStatus(call: PluginCall) {

        val result = JSObject()

        result.put("starting", starting)
        result.put("publishing", publishing)
        result.put("codec", activeCodec)
        result.put("audioCodec", "aac")

        call.resolve(result)
    }

    // ---------------------------------------------------------
    // DEVICE CAPABILITIES
    // ---------------------------------------------------------

    @PluginMethod
    fun getCapabilities(call: PluginCall) {

        val result = JSObject()

        result.put("h264", true)
        result.put("hevc", isHevcHardwareSupported())
        result.put("h265", isHevcHardwareSupported())

        result.put("aac", true)
        result.put("rtmp", true)
        result.put("rtmps", true)
        result.put("overlay", true)

        result.put("platform", "android")

        call.resolve(result)
    }

    // ---------------------------------------------------------
    // OVERLAY IMAGE
    // ---------------------------------------------------------

    private fun buildOverlayImage(
        overlay: JSObject?,
        width: Int,
        height: Int
    ): HkImage? {

        if (overlay == null) return null

        val bitmap = renderOverlay(
            overlay,
            width,
            height
        )

        return HkImage().apply {
            this.bitmap = bitmap
        }
    }

    private fun renderOverlay(
        data: JSObject,
        width: Int,
        height: Int
    ): Bitmap {

        val w = width.coerceAtLeast(16)
        val h = height.coerceAtLeast(16)

        val bitmap = Bitmap.createBitmap(
            w,
            h,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        val portrait = h > w

        val logicalWidth =
            if (portrait) 1080f else 1920f

        val logicalHeight =
            if (portrait) 1920f else 1080f

        val sx = w / logicalWidth
        val sy = h / logicalHeight

        val scale = minOf(sx, sy)

        fun rect(
            x: Float,
            y: Float,
            rw: Float,
            rh: Float
        ) = RectF(
            x * sx,
            y * sy,
            (x + rw) * sx,
            (y + rh) * sy
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 42f * scale
            color = Color.WHITE
        }

        // BANNER
        val banner = data.optJSONObject("banner")

        if (banner?.optBoolean("enabled", false) == true) {

            paint.color = parseColor(
                banner.optString(
                    "backgroundColor",
                    "#D02060D0"
                ),
                0xD02060D0.toInt()
            )

            canvas.drawRoundRect(
                rect(
                    40f,
                    36f,
                    logicalWidth - 80f,
                    144f
                ),
                22f * scale,
                22f * scale,
                paint
            )

            paint.color = Color.WHITE
            paint.textSize = 44f * scale

            canvas.drawText(
                banner.optString(
                    "title",
                    "LIVESTREAM TRỰC TIẾP"
                ),
                70f * sx,
                100f * sy,
                paint
            )

            paint.textSize = 27f * scale

            canvas.drawText(
                banner.optString("subtitle", ""),
                70f * sx,
                148f * sy,
                paint
            )
        }

        // SCOREBOARD
        val score = data.optJSONObject("scoreboard")

        if (score?.optBoolean("enabled", false) == true) {

            paint.color = 0xD9000000.toInt()

            canvas.drawRoundRect(
                rect(
                    logicalWidth * 0.20f,
                    205f,
                    logicalWidth * 0.60f,
                    110f
                ),
                18f * scale,
                18f * scale,
                paint
            )

            paint.color = Color.WHITE
            paint.textSize = 27f * scale

            val left =
                score.optString("teamA", "ĐỘI A") +
                    "  " +
                    score.optInt("scoreA", 0)

            val right =
                score.optString("teamB", "ĐỘI B") +
                    "  " +
                    score.optInt("scoreB", 0)

            canvas.drawText(
                left,
                logicalWidth * 0.23f * sx,
                268f * sy,
                paint
            )

            canvas.drawText(
                right,
                logicalWidth * 0.52f * sx,
                268f * sy,
                paint
            )
        }

        // TICKER
        val ticker = data.optJSONObject("ticker")

        if (ticker?.optBoolean("enabled", false) == true) {

            paint.color = 0xD9000000.toInt()

            canvas.drawRect(
                rect(
                    0f,
                    logicalHeight - 92f,
                    logicalWidth,
                    92f
                ),
                paint
            )

            paint.color = Color.WHITE
            paint.textSize = 34f * scale

            canvas.drawText(
                ticker.optString("text", ""),
                40f * sx,
                (logicalHeight - 35f) * sy,
                paint
            )
        }

        // LOGO
        val logo = data.optJSONObject("logo")

        val dataUri = logo
            ?.optString("dataUrl", "")
            .orEmpty()

        if (dataUri.startsWith("data:image/")) {

            try {

                val bytes = Base64.decode(
                    dataUri.substringAfter(','),
                    Base64.DEFAULT
                )

                val logoBitmap =
                    BitmapFactory.decodeByteArray(
                        bytes,
                        0,
                        bytes.size
                    )

                if (logoBitmap != null) {

                    val max = (190f * scale)
                        .toInt()
                        .coerceAtLeast(48)

                    val ratio = minOf(
                        max.toFloat() / logoBitmap.width,
                        max.toFloat() / logoBitmap.height,
                        1f
                    )

                    val logoWidth =
                        (logoBitmap.width * ratio).toInt()

                    val logoHeight =
                        (logoBitmap.height * ratio).toInt()

                    val position = logo.optString(
                        "position",
                        "top-right"
                    )

                    val x = if (position.contains("left")) {
                        36f * sx
                    } else {
                        w - logoWidth - 36f * sx
                    }

                    val y = if (position.contains("bottom")) {
                        h - logoHeight - 120f * sy
                    } else {
                        210f * sy
                    }

                    canvas.drawBitmap(
                        logoBitmap,
                        null,
                        RectF(
                            x,
                            y,
                            x + logoWidth,
                            y + logoHeight
                        ),
                        Paint(Paint.ANTI_ALIAS_FLAG)
                    )

                    logoBitmap.recycle()
                }

            } catch (e: Exception) {
                Log.w(TAG, "Logo rendering failed", e)
            }
        }

        return bitmap
    }

    // ---------------------------------------------------------
    // H.265 HARDWARE SUPPORT
    // ---------------------------------------------------------

    private fun isHevcHardwareSupported(): Boolean {

        return try {

            MediaCodecList(
                MediaCodecList.REGULAR_CODECS
            ).codecInfos.any { info ->

                info.isEncoder &&
                    info.supportedTypes.any {
                        it.equals(
                            "video/hevc",
                            ignoreCase = true
                        )
                    }
            }

        } catch (e: Exception) {

            Log.w(TAG, "HEVC capability check failed", e)

            false
        }
    }

    // ---------------------------------------------------------
    // COLOR
    // ---------------------------------------------------------

    private fun parseColor(
        value: String,
        fallback: Int
    ): Int {

        return try {
            Color.parseColor(value)
        } catch (_: Exception) {
            fallback
        }
    }

    // ---------------------------------------------------------
    // ERROR HANDLING
    // ---------------------------------------------------------

    private fun fail(
        call: PluginCall,
        message: String,
        code: String
    ) {

        Log.e(TAG, "$code: $message")

        synchronized(stateLock) {
            if (pendingStart === call) {
                pendingStart = null
            }

            starting = false
            publishing = false
        }

        cleanup(false)

        call.reject(message, code)
    }

    // ---------------------------------------------------------
    // RESOURCE CLEANUP
    // ---------------------------------------------------------

    private fun cleanup(
        rejectPending: Boolean
    ) {

        val pending = synchronized(stateLock) {

            val current = pendingStart

            pendingStart = null
            streamKey = null

            starting = false
            publishing = false

            current
        }

        // Remove listeners and close resources independently.
        try {
            connection?.removeEventListener(
                Event.RTMP_STATUS,
                this
            )
        } catch (e: Exception) {
            Log.w(TAG, "Remove RTMP listener failed", e)
        }

        try {
            connection?.removeEventListener(
                Event.IO_ERROR,
                this
            )
        } catch (e: Exception) {
            Log.w(TAG, "Remove IO listener failed", e)
        }

        try {
            stream?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Stream close failed", e)
        }

        try {
            stream?.dispose()
        } catch (e: Exception) {
            Log.w(TAG, "Stream dispose failed", e)
        }

        try {
            connection?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Connection close failed", e)
        }

        try {
            connection?.dispose()
        } catch (e: Exception) {
            Log.w(TAG, "Connection dispose failed", e)
        }

        try {
            camera?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Camera close failed", e)
        }

        try {
            overlayImage?.bitmap?.recycle()
        } catch (_: Exception) {
        }

        synchronized(stateLock) {
            connection = null
            stream = null
            camera = null
            overlayImage = null
        }

        if (rejectPending && pending != null) {
            pending.reject(
                "Phiên phát RTMP đã bị hủy.",
                "PUBLISH_CANCELLED"
            )
        }

        Log.i(TAG, "RTMP resources released.")
    }

    // ---------------------------------------------------------
    // DESTROY
    // ---------------------------------------------------------

    override fun handleOnDestroy() {

        cleanup(true)

        super.handleOnDestroy()
    }
}  
