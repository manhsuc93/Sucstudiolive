# Trạng thái chuyển Sucstudiolive sang RTMP trực tiếp

## Đã có trong gói này
- Giao diện RTMP URL + Stream Key.
- Bỏ luồng WHIP và máy chủ relay khỏi gói chuyển tiếp.
- JavaScript gọi `Capacitor.Plugins.RtmpPublisher.start()` / `stop()` theo hợp đồng native.
- Gửi cấu hình H.264/AAC, camera trước/sau và trạng thái overlay (banner, ticker, scoreboard, logo) sang plugin.
- Không dùng trạng thái camera/micro WebView để giả định rằng native capture đã sẵn sàng.
- Đặc tả API, quyền truy cập, compositor và checklist kiểm thử ở `native/README-RTMP-PLUGIN-VI.md`.

## Chưa hoàn thành
- Chưa có implementation Capacitor plugin Android/iOS.
- Chưa thêm dự án native `android/` hoặc `ios/`.
- Chưa có native camera preview, encoder, RTMP socket hoặc native overlay compositor.
- Chưa build APK/IPA và chưa kiểm thử RTMP trên thiết bị thật.

**Vì vậy gói này chưa thể phát RTMP trực tiếp.** Trên web/PWA, nút phát sẽ báo thiếu plugin thay vì giả lập trạng thái live.

## Hướng native
Dùng HaishinKit.kt cho Android và HaishinKit.swift cho iOS. Kiểm tra phiên bản HaishinKit.swift tương thích Xcode trước khi khóa dependency; upstream ghi 2.1.x cần Xcode 16.4+, còn 2.2.x/2.3.x cần Xcode 26+.

## Bổ sung ở bước tiếp theo
- Đã thêm bộ khung Capacitor plugin cho Android (Java) và iOS (Swift) trong `native/rtmp-publisher/`.
- Plugin shell kiểm tra định dạng URL/Stream Key và trả lỗi rõ ràng `RTMP_ENGINE_NOT_LINKED`; không giả lập trạng thái live.
- `stop()` an toàn dạng no-op; `updateOverlay()` báo chưa có compositor.
- Chưa thêm HaishinKit runtime, chưa có camera capture/encoder/socket RTMP thực tế, chưa thể build APK/IPA.
