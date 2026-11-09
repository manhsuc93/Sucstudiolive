# Đặc tả và trạng thái plugin native RtmpPublisher

> Trạng thái: đã có mã native Android/iOS và đường truyền RTMP; chưa có kết quả build CI hoặc kiểm thử trên thiết bị thật. Không xem đây là bản phát hành production-ready.

## Vì sao cần native
WebView không thể chuyển `MediaStream`/Canvas trực tiếp qua Capacitor bridge thành encoder native. Plugin tự quản lý camera, micro, encoder H.264/H.265, AAC và socket RTMP/RTMPS. Không dùng VPS, relay hoặc WHIP.

## API JavaScript
- `start(options)`: nhận RTMP URL, Stream Key, video/audio settings, camera facing và overlay; chỉ resolve sau status `NetStream.Publish.Start`.
- `stop()`: đóng stream, RTMP connection, camera và audio session.
- `updateOverlay(overlay)`: vẽ lại native bitmap chứa banner, ticker strip, scoreboard và logo trong lúc live.
- `getCapabilities()`: trả codec/RTMP capabilities của thiết bị.
- Event `rtmpStatus`: báo kết nối bị ngắt.

## Android
- Capacitor 7 plugin Kotlin.
- HaishinKit.kt 0.14.2; `CameraSource`, `AudioRecordSource`, `RtmpConnection`, `RtmpStream`.
- H.264/AAC mặc định; HEVC profile nếu MediaCodec có encoder phần cứng. Ingest server phải hỗ trợ Enhanced RTMP/HEVC.
- Overlay native bitmap được gắn vào `stream.screen`.

## iOS
- Capacitor 7 plugin Swift.
- HaishinKit.swift 1.9.1 qua CocoaPods; AVFoundation + VideoToolbox.
- H.264/AAC mặc định; HEVC khi VideoToolbox hỗ trợ phần cứng và máy chủ nhận Enhanced RTMP/HEVC.
- Overlay native bitmap dùng `ImageScreenObject` và offscreen video mixer.

## Giới hạn còn lại
- Native camera preview chưa được đặt chồng đúng vùng preview WebView. WebView camera/microphone được dừng lúc native capture bắt đầu; native camera vẫn phát hình lên RTMP.
- Ticker hiện là dải chữ tĩnh, chưa cuộn liên tục.
- Chưa chạy build Gradle/Xcode hoặc thử RTMP/RTMPS trên thiết bị thật.
- Cần xem log Codemagic, sửa lỗi biên dịch nếu có và kiểm thử stream thật trước khi phát hành.

## Build
`npm install` → `npm run web:sync` → `npx cap sync android` / `npx cap sync ios`.
