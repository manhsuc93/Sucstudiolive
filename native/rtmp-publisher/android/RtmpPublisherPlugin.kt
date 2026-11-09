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
        Permission(alias = "camera", strings = [Manifest.permission.CAMERA]),
        Permission(alias = "microphone", strings = [Manifest.permission.RECORD_AUDIO])
    ]
)
class RtmpPublisherPlugin : Plugin(), IEventListener {
    private var connection: RtmpConnection? = null
    private var stream: RtmpStream? = null
    private var camera: CameraSource? = null
    private var overlayImage: HkImage? = null
    private var pendingStart: PluginCall? = null
    private var streamKey: String? = null
    private var starting = false
    private var activeCodec = "h264"
    private val main = Handler(Looper.getMainLooper())

    @PluginMethod
    fun start(call: PluginCall) {
        val url = call.getString("url")?.trim().orEmpty()
        val key = call.getString("key")?.trim().orEmpty()
        val video = call.getObject("video")
        val audio = call.getObject("audio")
        val codec = video?.optString("codec", "h264") ?: "h264"

        if (!(url.startsWith("rtmp://") || url.startsWith("rtmps://"))) {
            call.reject("URL phải bắt đầu bằng rtmp:// hoặc rtmps://", "INVALID_RTMP_URL"); return
        }
        if (key.isBlank()) { call.reject("Thiếu Stream Key", "MISSING_STREAM_KEY"); return }
        if (codec != "h264" && codec != "hevc") {
            call.reject("Codec chỉ hỗ trợ H.264 hoặc H.265.", "UNSUPPORTED_CODEC"); return
        }
        if (codec == "hevc" && !isHevcHardwareSupported()) {
            call.reject("Thiết bị Android không có bộ mã hóa H.265 phần cứng tương thích.", "HEVC_NOT_SUPPORTED"); return
        }
        if (starting || connection != null) {
            call.reject("Đang có phiên phát hoặc đang khởi động", "PUBLISHER_BUSY"); return
        }

        pendingStart = call
        streamKey = key
        starting = true
        if (!hasPermission("camera") || !hasPermission("microphone")) {
            requestPermissionForAliases(arrayOf("camera", "microphone"), call, "permissionsForStart")
            return
        }
        startAfterPermission(url, key, video, audio, call)
    }

    @PermissionCallback
    private fun permissionsForStart(call: PluginCall) {
        val url = call.getString("url")?.trim().orEmpty()
        val key = call.getString("key")?.trim().orEmpty()
        if (!hasPermission("camera") || !hasPermission("microphone")) {
            fail(call, "Bạn cần cấp quyền Camera và Micro để phát trực tiếp.", "PERMISSION_DENIED")
            return
        }
        startAfterPermission(url, key, call.getObject("video"), call.getObject("audio"), call)
    }

    private fun startAfterPermission(
        url: String, key: String, video: JSObject?, audio: JSObject?, call: PluginCall
    ) {
        try {
            val width = video?.getInteger("width") ?: 1280
            val height = video?.getInteger("height") ?: 720
            val bitrate = video?.getInteger("bitrate") ?: 2_500_000
            val audioBitrate = audio?.getInteger("bitrate") ?: 128_000
            val conn = RtmpConnection()
            val rtmp = RtmpStream(conn)
            val camFacing = call.getString("cameraFacing", "environment")
            val facing = if (camFacing == "user") CameraCharacteristics.LENS_FACING_FRONT
                         else CameraCharacteristics.LENS_FACING_BACK
            val cam = CameraSource(context).apply { open(facing) }

            rtmp.attachAudio(AudioRecordSource())
            rtmp.attachVideo(cam)
            rtmp.videoSettings.width = width
            rtmp.videoSettings.height = height
            rtmp.videoSettings.bitrate = bitrate
            rtmp.videoSettings.IFrameInterval = 2
            rtmp.videoSettings.profileLevel = if (codec == "hevc") {
                VideoCodecProfileLevel.HEVC_MAIN_3_1
            } else {
                VideoCodecProfileLevel.H264_BASELINE_3_1
            }
            rtmp.audioSettings.bitrate = audioBitrate

            connection = conn
            stream = rtmp
            camera = cam
            activeCodec = codec
            overlayImage = buildOverlayImage(call.getObject("overlay"), width, height)?.also {
                it.frame.set(0, 0, width, height)
                rtmp.screen.addChild(it)
            }

            pendingStart = call
            streamKey = key
            conn.addEventListener(Event.RTMP_STATUS, this)
            conn.addEventListener(Event.IO_ERROR, this)
            conn.connect(url)
        } catch (e: Exception) {
            Log.e("RtmpPublisher", "Native RTMP start failed", e)
            fail(call, e.message ?: "Không thể khởi tạo bộ phát RTMP", "NATIVE_START_FAILED")
        }
    }

    override fun handleEvent(event: Event) {
        val payload = try { EventUtils.toMap(event) } catch (_: Exception) { emptyMap<String, Any>() }
        val code = payload["code"]?.toString().orEmpty()
        main.post {
            when {
                code == RtmpConnection.Code.CONNECT_SUCCESS.rawValue -> {
                    val key = streamKey
                    if (key.isNullOrBlank()) {
                        pendingStart?.let { fail(it, "Mất Stream Key trong phiên khởi động", "MISSING_STREAM_KEY") }
                    } else {
                        stream?.publish(key)
                    }
                }
                code.contains("NetStream.Publish.Start", ignoreCase = true) -> {
                    pendingStart?.let {
                        val result = JSObject()
                        result.put("state", "live")
                        result.put("codec", activeCodec)
                        result.put("message", "Máy chủ RTMP đã chấp nhận publish H.264/AAC.")
                        it.resolve(result)
                    }
                    pendingStart = null
                    starting = false
                }
                code.contains("Failed", ignoreCase = true) ||
                    code.contains("Rejected", ignoreCase = true) ||
                    code.contains("BadName", ignoreCase = true) ||
                    code.contains("Connect.Closed", ignoreCase = true) -> {
                    if (pendingStart != null) {
                        pendingStart?.let { fail(it, "Máy chủ RTMP từ chối hoặc đóng phiên phát: $code", "RTMP_PUBLISH_REJECTED") }
                    } else {
                        val status = JSObject()
                        status.put("state", "disconnected")
                        status.put("code", code)
                        status.put("message", "Kết nối RTMP đã bị đóng.")
                        notifyListeners("rtmpStatus", status)
                        cleanup()
                    }
                }
            }
        }
    }

    @PluginMethod
    fun stop(call: PluginCall) {
        cleanup()
        val result = JSObject()
        result.put("state", "stopped")
        call.resolve(result)
    }

    @PluginMethod
    fun updateOverlay(call: PluginCall) {
        val currentStream = stream
        if (currentStream == null) {
            call.reject("Chưa có phiên phát để cập nhật overlay.", "NOT_LIVE")
            return
        }
        try {
            val bitmap = renderOverlay(call.data, currentStream.videoSettings.width, currentStream.videoSettings.height)
            val image = overlayImage ?: HkImage().also {
                it.frame.set(0, 0, currentStream.videoSettings.width, currentStream.videoSettings.height)
                currentStream.screen.addChild(it)
                overlayImage = it
            }
            image.bitmap = bitmap
            call.resolve()
        } catch (e: Exception) {
            call.reject("Không thể cập nhật overlay: ${e.message}", "OVERLAY_UPDATE_FAILED")
        }
    }

    @PluginMethod
    fun getCapabilities(call: PluginCall) {
        val result = JSObject()
        result.put("h264", true)
        result.put("hevc", isHevcHardwareSupported())
        result.put("rtmp", true)
        result.put("rtmps", true)
        result.put("overlay", true)
        result.put("platform", "android")
        call.resolve(result)
    }

    private fun buildOverlayImage(overlay: JSObject?, width: Int, height: Int): HkImage? {
        if (overlay == null) return null
        val bitmap = renderOverlay(overlay, width, height)
        val image = HkImage()
        image.bitmap = bitmap
        return image
    }

    private fun renderOverlay(data: JSObject, width: Int, height: Int): Bitmap {
        val w = width.coerceAtLeast(16)
        val h = height.coerceAtLeast(16)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val portrait = h > w
        val logicalWidth = if (portrait) 1080f else 1920f
        val logicalHeight = if (portrait) 1920f else 1080f
        val sx = w / logicalWidth
        val sy = h / logicalHeight
        val scale = minOf(sx, sy)
        fun rect(x: Float, y: Float, rw: Float, rh: Float) =
            RectF(x*sx, y*sy, (x+rw)*sx, (y+rh)*sy)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 42f*scale; color = Color.WHITE }

        val banner = data.optJSONObject("banner")
        if (banner?.optBoolean("enabled", false) == true) {
            paint.color = parseColor(banner.optString("backgroundColor", "#D02060D0"), 0xD02060D0.toInt())
            canvas.drawRoundRect(rect(40f, 36f, logicalWidth-80f, 144f), 22f*scale, 22f*scale, paint)
            paint.color = Color.WHITE
            paint.textSize = 44f*scale
            canvas.drawText(banner.optString("title", "LIVESTREAM TRỰC TIẾP"), 70f*sx, 100f*sy, paint)
            paint.textSize = 27f*scale
            canvas.drawText(banner.optString("subtitle", ""), 70f*sx, 148f*sy, paint)
        }

        val score = data.optJSONObject("scoreboard")
        if (score?.optBoolean("enabled", false) == true) {
            paint.color = 0xD9000000.toInt()
            canvas.drawRoundRect(rect(logicalWidth*0.20f, 205f, logicalWidth*0.60f, 110f), 18f*scale, 18f*scale, paint)
            paint.color = Color.WHITE
            paint.textSize = 27f*scale
            val left = score.optString("teamA", "ĐỘI A") + "  " + score.optInt("scoreA", 0)
            val right = score.optString("teamB", "ĐỘI B") + "  " + score.optInt("scoreB", 0)
            canvas.drawText(left, logicalWidth*0.23f*sx, 268f*sy, paint)
            canvas.drawText(right, logicalWidth*0.52f*sx, 268f*sy, paint)
        }

        val ticker = data.optJSONObject("ticker")
        if (ticker?.optBoolean("enabled", false) == true) {
            paint.color = 0xD9000000.toInt()
            canvas.drawRect(rect(0f, logicalHeight-92f, logicalWidth, 92f), paint)
            paint.color = Color.WHITE
            paint.textSize = 34f*scale
            canvas.drawText(ticker.optString("text", ""), 40f*sx, (logicalHeight-35f)*sy, paint)
        }

        val logo = data.optJSONObject("logo")
        val dataUri = logo?.optString("dataUrl", "").orEmpty()
        if (dataUri.startsWith("data:image/")) {
            val bytes = Base64.decode(dataUri.substringAfter(','), Base64.DEFAULT)
            val logoBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (logoBitmap != null) {
                val max = (190f*scale).toInt().coerceAtLeast(48)
                val ratio = minOf(max.toFloat()/logoBitmap.width, max.toFloat()/logoBitmap.height, 1f)
                val lw = (logoBitmap.width*ratio).toInt()
                val lh = (logoBitmap.height*ratio).toInt()
                val pos = logo.optString("position", "top-right")
                val x = if (pos.contains("left")) 36f*sx else w-lw-36f*sx
                val y = if (pos.contains("bottom")) h-lh-120f*sy else 210f*sy
                canvas.drawBitmap(logoBitmap, null, RectF(x,y,x+lw,y+lh), Paint(Paint.ANTI_ALIAS_FLAG))
                logoBitmap.recycle()
            }
        }
        return bitmap
    }

    private fun isHevcHardwareSupported(): Boolean {
        return try {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
                info.isEncoder && info.supportedTypes.any { it.equals("video/hevc", ignoreCase = true) }
            }
        } catch (_: Exception) { false }
    }

    private fun parseColor(value: String, fallback: Int): Int =
        try { Color.parseColor(value) } catch (_: Exception) { fallback }

    private fun cleanup() {
        try { connection?.close(); connection?.dispose(); stream?.dispose(); camera?.close() }
        catch (e: Exception) { Log.w("RtmpPublisher", "cleanup", e) }
        connection = null
        stream = null
        camera = null
        overlayImage = null
        streamKey = null
        starting = false
        pendingStart?.reject("Phiên phát đã bị dừng", "PUBLISH_CANCELLED")
        pendingStart = null
    }

    private fun fail(call: PluginCall, message: String, code: String) {
        cleanup()
        call.reject(message, code)
    }

    override fun handleOnDestroy() {
        cleanup()
        super.handleOnDestroy()
    }
}
