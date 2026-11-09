package com.sucstudiolive.rtmp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * RTMP Publisher plugin.
 *
 * Native Android publishing:
 * - Camera capture
 * - Microphone capture
 * - H.264 / H.265 encoder selection
 * - AAC audio
 * - RTMP / RTMPS destination
 *
 * HaishinKit API calls must match the dependency version
 * configured in the Android project.
 */
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
class RtmpPublisherPlugin : Plugin() {

    companion object {
        private const val TAG = "RtmpPublisher"
    }

    private val lock = Any()

    private var pendingStart: PluginCall? = null
    private var isStarting = false
    private var isPublishing = false

    private var activeCodec = "h264"
    private var activeUrl: String? = null

    /*
     * Native RTMP resources.
     *
     * Connect these fields to the exact HaishinKit 0.14.2
     * classes and APIs used by your project.
     */
    private var rtmpConnection: Any? = null
    private var rtmpStream: Any? = null
    private var cameraSource: Any? = null

    private val stopping = AtomicBoolean(false)

    @PluginMethod
    fun start(call: PluginCall) {
        synchronized(lock) {
            if (isStarting || isPublishing) {
                call.reject("Một phiên phát RTMP đang hoạt động.")
                return
            }

            isStarting = true
            pendingStart = call
        }

        val url = call.getString("url")?.trim().orEmpty()
        val key = call.getString("key")?.trim().orEmpty()

        if (!isValidRtmpUrl(url)) {
            fail(call, "URL RTMP/RTMPS không hợp lệ.")
            return
        }

        if (key.isBlank()) {
            fail(call, "Stream Key không được để trống.")
            return
        }

        val video = call.getObject("video")
        val audio = call.getObject("audio")

        val codec = video?.optString("codec", "h264")
            ?.lowercase()
            ?: "h264"

        if (codec !in listOf("h264", "h265")) {
            fail(call, "Codec chỉ hỗ trợ h264 hoặc h265.")
            return
        }

        activeCodec = codec
        activeUrl = url

        if (!hasPermissions()) {
            requestPermissionForAliases(
                arrayOf("camera", "microphone"),
                call,
                "permissionsResult"
            )
            return
        }

        startAfterPermission(
            url = url,
            key = key,
            video = video,
            audio = audio,
            codec = codec,
            call = call
        )
    }

    @PermissionCallback
    private fun permissionsResult(call: PluginCall) {
        if (!hasPermissions()) {
            fail(
                call,
                "Ứng dụng cần quyền Camera và Micro để phát trực tiếp."
            )
            return
        }

        val video = call.getObject("video")
        val audio = call.getObject("audio")

        val codec = video?.optString("codec", "h264")
            ?.lowercase()
            ?: "h264"

        startAfterPermission(
            url = call.getString("url").orEmpty(),
            key = call.getString("key").orEmpty(),
            video = video,
            audio = audio,
            codec = codec,
            call = call
        )
    }

    private fun startAfterPermission(
        url: String,
        key: String,
        video: JSObject?,
        audio: JSObject?,
        codec: String,
        call: PluginCall
    ) {
        if (!hasPermissions()) {
            fail(call, "Thiếu quyền Camera hoặc Micro.")
            return
        }

        val width = video?.optInt("width", 1280) ?: 1280
        val height = video?.optInt("height", 720) ?: 720
        val fps = video?.optInt("fps", 30) ?: 30
        val videoBitrate = video?.optInt("bitrate", 2_500_000)
            ?: 2_500_000

        val audioBitrate = audio?.optInt("bitrate", 128_000)
            ?: 128_000

        val sampleRate = audio?.optInt("sampleRate", 44_100)
            ?: 44_100

        val facing = call.getString("cameraFacing", "back")
            ?: "back"

        if (width <= 0 || height <= 0 || fps <= 0) {
            fail(call, "Thông số video không hợp lệ.")
            return
        }

        if (videoBitrate <= 0 || audioBitrate <= 0) {
            fail(call, "Bitrate không hợp lệ.")
            return
        }

        /*
         * IMPORTANT:
         *
         * Insert the HaishinKit 0.14.2 initialization here.
         *
         * Expected pipeline:
         *
         * 1. Create RTMPConnection.
         * 2. Create RTMPStream(connection).
         * 3. Configure H.264 or HEVC encoder.
         * 4. Configure AAC audio encoder.
         * 5. Open CameraSource.
         * 6. Attach camera and microphone to RTMPStream.
         * 7. Connect to RTMP server.
         * 8. Publish stream key after connection succeeds.
         *
         * Do not report successful publishing before the
         * RTMP server confirms that publishing has started.
         */

        Log.i(
            TAG,
            "Preparing RTMP: codec=$codec, " +
                "resolution=${width}x$height, fps=$fps, " +
                "videoBitrate=$videoBitrate, " +
                "audioBitrate=$audioBitrate, " +
                "sampleRate=$sampleRate, camera=$facing"
        )

        fail(
            call,
            "Chưa nối phần khởi tạo HaishinKit vào phiên bản API của dự án."
        )
    }

    @PluginMethod
    fun stop(call: PluginCall) {
        synchronized(lock) {
            if (isStarting && !isPublishing) {
                pendingStart?.let {
                    pendingStart = null
                    it.reject("Phiên phát đã bị hủy.")
                }
            }
        }

        cleanupResources()

        synchronized(lock) {
            isStarting = false
            isPublishing = false
            activeUrl = null
        }

        val result = JSObject()
        result.put("success", true)
        result.put("message", "Đã dừng phát RTMP.")
        call.resolve(result)
    }

    @PluginMethod
    fun getStatus(call: PluginCall) {
        val result = JSObject()

        result.put("starting", isStarting)
        result.put("publishing", isPublishing)
        result.put("codec", activeCodec)
        result.put("url", activeUrl ?: "")

        call.resolve(result)
    }

    private fun hasPermissions(): Boolean {
        val context = context ?: return false

        val cameraGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        val microphoneGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        return cameraGranted && microphoneGranted
    }

    private fun isValidRtmpUrl(url: String): Boolean {
        return try {
            val uri = java.net.URI(url)

            (uri.scheme == "rtmp" || uri.scheme == "rtmps") &&
                !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    private fun fail(
        call: PluginCall,
        message: String
    ) {
        Log.e(TAG, message)

        cleanupResources()

        synchronized(lock) {
            if (pendingStart === call) {
                pendingStart = null
            }

            isStarting = false
            isPublishing = false
            activeUrl = null
        }

        call.reject(message)
    }

    private fun cleanupResources() {
        if (!stopping.compareAndSet(false, true)) {
            return
        }

        try {
            /*
             * Dispose stream, connection and camera separately.
             *
             * Replace these placeholders with the actual
             * HaishinKit dispose/close calls.
             */

            try {
                rtmpStream = null
            } catch (e: Exception) {
                Log.w(TAG, "Stream cleanup failed", e)
            }

            try {
                rtmpConnection = null
            } catch (e: Exception) {
                Log.w(TAG, "Connection cleanup failed", e)
            }

            try {
                cameraSource = null
            } catch (e: Exception) {
                Log.w(TAG, "Camera cleanup failed", e)
            }

        } finally {
            stopping.set(false)
        }
    }

    override fun handleOnDestroy() {
        cleanupResources()

        synchronized(lock) {
            pendingStart = null
            isStarting = false
            isPublishing = false
        }

        super.handleOnDestroy()
    }
}
