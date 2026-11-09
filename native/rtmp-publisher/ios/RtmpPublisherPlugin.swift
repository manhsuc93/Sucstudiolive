import Foundation
import AVFoundation
import VideoToolbox
import UIKit
import Capacitor
import HaishinKit

@objc(RtmpPublisherPlugin)
public class RtmpPublisherPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "RtmpPublisherPlugin"
    public let jsName = "RtmpPublisher"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "start", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stop", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "updateOverlay", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getCapabilities", returnType: CAPPluginReturnPromise)
    ]

    private var connection: RTMPConnection?
    private var stream: RTMPStream?
    private var pendingCall: CAPPluginCall?
    private var streamKey: String?
    private var activeCodec = "h264"
    private var overlayObject: ImageScreenObject?
    private var isStarting = false

    @objc func start(_ call: CAPPluginCall) {
        guard let url = call.getString("url"),
              let scheme = URLComponents(string: url)?.scheme?.lowercased(),
              scheme == "rtmp" || scheme == "rtmps" else {
            call.reject("URL phải bắt đầu bằng rtmp:// hoặc rtmps://", "INVALID_RTMP_URL")
            return
        }
        guard let key = call.getString("key"),
              !key.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            call.reject("Thiếu Stream Key", "MISSING_STREAM_KEY")
            return
        }
        guard !isStarting && connection == nil else {
            call.reject("Đang có phiên phát hoặc đang khởi động", "PUBLISHER_BUSY")
            return
        }

        let video = call.getObject("video") ?? [:]
        let codec = (video["codec"] as? String) ?? "h264"
        guard codec == "h264" || codec == "hevc" else {
            call.reject("Codec chỉ hỗ trợ h264 hoặc hevc", "UNSUPPORTED_CODEC")
            return
        }
        if codec == "hevc" && !VTIsHardwareEncodeSupported(kCMVideoCodecType_HEVC) {
            call.reject("Thiết bị không hỗ trợ mã hóa H.265 bằng phần cứng.", "HEVC_NOT_SUPPORTED")
            return
        }

        pendingCall = call
        streamKey = key
        activeCodec = codec
        isStarting = true

        AVCaptureDevice.requestAccess(for: .video) { [weak self] videoAllowed in
            AVCaptureDevice.requestAccess(for: .audio) { audioAllowed in
                DispatchQueue.main.async {
                    guard let self else { return }
                    guard videoAllowed && audioAllowed else {
                        self.fail("Bạn cần cấp quyền Camera và Micro để phát trực tiếp.", code: "PERMISSION_DENIED")
                        return
                    }
                    self.startEngine(url: url, call: call, video: video, codec: codec)
                }
            }
        }
    }

    private func startEngine(url: String, call: CAPPluginCall, video: [String: Any], codec: String) {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playAndRecord, mode: .videoRecording, options: [.defaultToSpeaker, .allowBluetooth])
            try session.setActive(true)

            guard let camera = AVCaptureDevice.default(
                .builtInWideAngleCamera,
                for: .video,
                position: (call.getString("cameraFacing") ?? "environment") == "user" ? .front : .back
            ) else {
                fail("Không tìm thấy camera phù hợp trên thiết bị.", code: "CAMERA_NOT_FOUND")
                return
            }
            guard let microphone = AVCaptureDevice.default(for: .audio) else {
                fail("Không tìm thấy micro trên thiết bị.", code: "MICROPHONE_NOT_FOUND")
                return
            }

            let conn = RTMPConnection()
            let rtmp = RTMPStream(connection: conn)
            let width = video["width"] as? Int ?? 1280
            let height = video["height"] as? Int ?? 720
            let fps = video["fps"] as? Int ?? 30
            let bitrate = video["bitrate"] as? Int ?? 2_500_000
            let profile = codec == "hevc"
                ? kVTProfileLevel_HEVC_Main_AutoLevel as String
                : kVTProfileLevel_H264_Baseline_3_1 as String

            rtmp.frameRate = fps
            rtmp.sessionPreset = width >= 1920 ? .hd1920x1080 : .hd1280x720
            rtmp.videoSettings = VideoCodecSettings(
                videoSize: CGSize(width: width, height: height),
                profileLevel: profile,
                bitRate: bitrate,
                maxKeyFrameIntervalDuration: 2,
                scalingMode: .trim,
                bitRateMode: .average,
                allowFrameReordering: nil,
                isHardwareEncoderEnabled: true
            )
            rtmp.audioSettings = AudioCodecSettings(bitRate: call.getObject("audio")?["bitrate"] as? Int ?? 128_000)
            rtmp.videoMixerSettings.mode = .offscreen
            rtmp.screen.startRunning()

            self.connection = conn
            self.stream = rtmp
            self.overlayObject = nil
            applyOverlay(call.getObject("overlay"), to: rtmp, width: width, height: height)

            conn.addEventListener(.rtmpStatus, selector: #selector(rtmpStatusHandler), observer: self)

            rtmp.attachAudio(microphone, automaticallyConfiguresApplicationAudioSession: false) { [weak self] _, error in
                guard let self else { return }
                if let error {
                    DispatchQueue.main.async { self.fail(error.localizedDescription, code: "MICROPHONE_ATTACH_FAILED") }
                    return
                }
                rtmp.attachCamera(camera, track: 0) { [weak self] _, error in
                    guard let self else { return }
                    DispatchQueue.main.async {
                        if let error {
                            self.fail(error.localizedDescription, code: "CAMERA_ATTACH_FAILED")
                            return
                        }
                        conn.connect(url)
                    }
                }
            }
        } catch {
            fail(error.localizedDescription, code: "NATIVE_START_FAILED")
        }
    }

    @objc private func rtmpStatusHandler(_ notification: Notification) {
        guard let event = notification.object as? Event else { return }
        let code = event.data["code"] as? String ?? ""
        if code == "NetConnection.Connect.Success" {
            guard let key = streamKey else {
                fail("Mất Stream Key trong phiên khởi động", code: "MISSING_STREAM_KEY")
                return
            }
            stream?.publish(key)
        } else if code == "NetStream.Publish.Start" {
            pendingCall?.resolve([
                "state": "live",
                "codec": activeCodec,
                "message": "Máy chủ RTMP đã chấp nhận publish \(activeCodec.uppercased())/AAC."
            ])
            pendingCall = nil
            isStarting = false
        } else if code.contains("Failed") || code.contains("Rejected") || code.contains("BadName") {
            fail("Máy chủ RTMP từ chối phiên phát: \(code)", code: "RTMP_PUBLISH_REJECTED")
        } else if code == "NetConnection.Connect.Closed" {
            if isStarting {
                fail("Máy chủ đã đóng kết nối RTMP trước khi bắt đầu phát.", code: "RTMP_CONNECTION_CLOSED")
            } else {
                notifyListeners("rtmpStatus", data: [
                    "state": "disconnected",
                    "code": code,
                    "message": "Kết nối RTMP đã bị đóng."
                ])
                cleanup(rejectPending: false)
            }
        }
    }

    @objc func stop(_ call: CAPPluginCall) {
        cleanup()
        call.resolve(["state": "stopped"])
    }

    @objc func updateOverlay(_ call: CAPPluginCall) {
        guard let stream else {
            call.reject("Chưa có phiên phát để cập nhật overlay.", "NOT_LIVE")
            return
        }
        let outputSize = stream.videoSettings.videoSize
        applyOverlay(call.data, to: stream, width: Int(outputSize.width), height: Int(outputSize.height))
        call.resolve()
    }

    @objc func getCapabilities(_ call: CAPPluginCall) {
        call.resolve([
            "h264": true,
            "hevc": VTIsHardwareEncodeSupported(kCMVideoCodecType_HEVC),
            "rtmp": true,
            "rtmps": true,
            "overlay": true,
            "platform": "ios"
        ])
    }

    private func applyOverlay(_ data: JSObject?, to stream: RTMPStream, width: Int, height: Int) {
        guard let data, let cgImage = renderOverlay(data, width: width, height: height) else { return }
        let image = overlayObject ?? ImageScreenObject()
        image.cgImage = cgImage
        image.size = .init(width: width, height: height)
        if overlayObject == nil {
            try? stream.screen.addChild(image)
            overlayObject = image
        }
    }

    private func renderOverlay(_ data: [String: Any], width: Int, height: Int) -> CGImage? {
        let size = CGSize(width: width, height: height)
        let renderer = UIGraphicsImageRenderer(size: size)
        let image = renderer.image { context in
            let cg = context.cgContext
            let portrait = height > width
            let logicalWidth: CGFloat = portrait ? 1080 : 1920
            let logicalHeight: CGFloat = portrait ? 1920 : 1080
            let sx = CGFloat(width) / logicalWidth
            let sy = CGFloat(height) / logicalHeight
            let scale = min(sx, sy)
            func rect(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat) -> CGRect {
                CGRect(x: x*sx, y: y*sy, width: w*sx, height: h*sy)
            }
            func drawText(_ value: String, _ point: CGPoint, _ fontSize: CGFloat, _ color: UIColor = .white) {
                let attrs: [NSAttributedString.Key: Any] = [
                    .font: UIFont.boldSystemFont(ofSize: fontSize*scale),
                    .foregroundColor: color
                ]
                (value as NSString).draw(at: CGPoint(x: point.x*sx, y: point.y*sy), withAttributes: attrs)
            }

            if let banner = data["banner"] as? [String: Any], (banner["enabled"] as? Bool) == true {
                let color = UIColor(hexString: banner["backgroundColor"] as? String ?? "#D02060D0")
                cg.setFillColor(color.cgColor)
                cg.fill(rect(40, 36, logicalWidth-80, 144))
                drawText(banner["title"] as? String ?? "LIVESTREAM TRỰC TIẾP", CGPoint(x: 70, y: 58), 44)
                drawText(banner["subtitle"] as? String ?? "", CGPoint(x: 70, y: 112), 27)
            }

            if let score = data["scoreboard"] as? [String: Any], (score["enabled"] as? Bool) == true {
                cg.setFillColor(UIColor.black.withAlphaComponent(0.82).cgColor)
                cg.fill(rect(logicalWidth*0.20, 205, logicalWidth*0.60, 110))
                let left = "\(score["teamA"] as? String ?? "ĐỘI A")  \(score["scoreA"] as? Int ?? 0)"
                let right = "\(score["teamB"] as? String ?? "ĐỘI B")  \(score["scoreB"] as? Int ?? 0)"
                drawText(left, CGPoint(x: 590, y: 240), 28)
                drawText(right, CGPoint(x: logicalWidth*0.52, y: 240), 28)
            }

            if let ticker = data["ticker"] as? [String: Any], (ticker["enabled"] as? Bool) == true {
                cg.setFillColor(UIColor.black.withAlphaComponent(0.82).cgColor)
                cg.fill(rect(0, logicalHeight-92, logicalWidth, 92))
                drawText(ticker["text"] as? String ?? "", CGPoint(x: 40, y: logicalHeight-70), 34)
            }

            if let logo = data["logo"] as? [String: Any],
               let dataURI = logo["dataUrl"] as? String,
               let comma = dataURI.firstIndex(of: ","),
               let bytes = Data(base64Encoded: String(dataURI[dataURI.index(after: comma)...])),
               let logoImage = UIImage(data: bytes) {
                let maxSide: CGFloat = 190*scale
                let ratio = min(maxSide/logoImage.size.width, maxSide/logoImage.size.height, 1)
                let w = logoImage.size.width*ratio
                let h = logoImage.size.height*ratio
                let position = logo["position"] as? String ?? "top-right"
                let x: CGFloat = position.contains("left") ? 36*sx : CGFloat(width)-w-36*sx
                let y: CGFloat = position.contains("bottom") ? CGFloat(height)-h-120*sy : 210*sy
                logoImage.draw(in: CGRect(x: x, y: y, width: w, height: h))
            }
        }
        return image.cgImage
    }

    private func cleanup(rejectPending: Bool = true) {
        stream?.close()
        connection?.close()
        stream = nil
        connection = nil
        overlayObject = nil
        streamKey = nil
        isStarting = false
        if rejectPending {
            pendingCall?.reject("Phiên phát đã bị dừng", "PUBLISH_CANCELLED")
        }
        pendingCall = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    private func fail(_ message: String, code: String) {
        let failedCall = pendingCall
        cleanup(rejectPending: false)
        failedCall?.reject(message, code)
    }

    deinit {
        stream?.close()
        connection?.close()
    }
}

private extension UIColor {
    convenience init(hexString: String) {
        var value = hexString.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        if value.count == 6 { value = "FF" + value }
        var number: UInt64 = 0
        Scanner(string: value).scanHexInt64(&number)
        self.init(
            red: CGFloat((number >> 16) & 0xff) / 255,
            green: CGFloat((number >> 8) & 0xff) / 255,
            blue: CGFloat(number & 0xff) / 255,
            alpha: CGFloat((number >> 24) & 0xff) / 255
        )
    }
}
