package vn.livestudio.rtmp;

import android.Manifest;
import com.getcapacitor.*;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;

/** Capacitor 7 bridge shell. Does not claim to publish until HaishinKit is linked. */
@CapacitorPlugin(
    name = "RtmpPublisher",
    permissions = {
        @Permission(alias = "camera", strings = { Manifest.permission.CAMERA }),
        @Permission(alias = "microphone", strings = { Manifest.permission.RECORD_AUDIO })
    }
)
public class RtmpPublisherPlugin extends Plugin {
    @PluginMethod
    public void start(PluginCall call) {
        String url = call.getString("url", "");
        String key = call.getString("key", "");
        if (url == null || !(url.startsWith("rtmp://") || url.startsWith("rtmps://"))) {
            call.reject("URL phải bắt đầu bằng rtmp:// hoặc rtmps://", "INVALID_RTMP_URL");
            return;
        }
        if (key == null || key.trim().isEmpty()) {
            call.reject("Thiếu Stream Key", "MISSING_STREAM_KEY");
            return;
        }
        call.reject("Plugin native đã được nhận diện nhưng engine HaishinKit chưa được tích hợp. Chưa phát RTMP.", "RTMP_ENGINE_NOT_LINKED");
    }

    @PluginMethod
    public void stop(PluginCall call) {
        JSObject result = new JSObject();
        result.put("state", "stopped");
        call.resolve(result);
    }

    @PluginMethod
    public void updateOverlay(PluginCall call) {
        call.reject("Native overlay compositor chưa được tích hợp.", "OVERLAY_ENGINE_NOT_LINKED");
    }
}
