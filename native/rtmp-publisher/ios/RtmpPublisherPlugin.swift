import Foundation
import Capacitor

/** Capacitor 7 bridge shell. Fails closed until HaishinKit is linked. */
@objc(RtmpPublisherPlugin)
public class RtmpPublisherPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "RtmpPublisherPlugin"
    public let jsName = "RtmpPublisher"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "start", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stop", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "updateOverlay", returnType: CAPPluginReturnPromise)
    ]

    @objc func start(_ call: CAPPluginCall) {
        let url = call.getString("url") ?? ""
        let key = call.getString("key") ?? ""
        guard url.hasPrefix("rtmp://") || url.hasPrefix("rtmps://") else {
            call.reject("URL phải bắt đầu bằng rtmp:// hoặc rtmps://", "INVALID_RTMP_URL")
            return
        }
        guard !key.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            call.reject("Thiếu Stream Key", "MISSING_STREAM_KEY")
            return
        }
        call.reject("Plugin native đã được nhận diện nhưng engine HaishinKit chưa được tích hợp. Chưa phát RTMP.", "RTMP_ENGINE_NOT_LINKED")
    }

    @objc func stop(_ call: CAPPluginCall) {
        call.resolve(["state": "stopped"])
    }

    @objc func updateOverlay(_ call: CAPPluginCall) {
        call.reject("Native overlay compositor chưa được tích hợp.", "OVERLAY_ENGINE_NOT_LINKED")
    }
}
