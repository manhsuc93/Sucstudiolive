# Đặc tả plugin native RtmpPublisher (Capacitor 7)

> Trạng thái: đặc tả tích hợp, CHƯA phải plugin native chạy được. Không được phát hành APK/IPA với tuyên bố RTMP đã hoạt động cho đến khi hoàn tất implementation và test thiết bị.

## Vì sao cần native

WebView không thể chuyển `MediaStream`/Canvas trực tiếp qua Capacitor bridge thành encoder native. Plugin phải tự quản lý camera, micro, H.264, AAC và socket RTMP/RTMPS. Không dùng VPS, MediaMTX, FFmpeg relay hay WHIP.

## API JavaScript mà app đang gọi

### `start(options)`

```ts
{
  url: string;             // rtmp:// hoặc rtmps:// ingest URL, KHÔNG gồm stream key
  key: string;             // stream key; không log, không lưu plaintext
  video: {
    codec: 'h264'; width: number; height: number;
    fps: 30; bitrate: number;
  };
  audio: {
    codec: 'aac'; bitrate: 128000;
    sampleRate: 44100; channels: 2;
  };
  cameraFacing: 'user' | 'environment';
  requireNativeCapture: true;
  overlay: {
    banner: { enabled: boolean; title: string; subtitle: string; backgroundColor: string };
    ticker: { enabled: boolean; text: string };
    scoreboard: { enabled: boolean; teamA: string; teamB: string; scoreA: number; scoreB: number };
    logo: null | { dataUrl: string; position: string };
  };
}
```

`start()` chỉ resolve sau khi native camera/micro đã mở, encoder chạy và RTMP publish được server chấp nhận. Nếu chỉ tạo session hoặc đang kết nối thì phải tiếp tục chờ; không được báo `live` sớm. Reject với mã lỗi rõ ràng nếu thiếu quyền, camera/micro bận, URL/key sai, codec không hỗ trợ hoặc server từ chối.

### `stop()`

Dừng publish, đóng RTMP socket, encoder, camera, audio session và giải phóng tài nguyên. Phải idempotent.

### `updateOverlay(options)` (cần bổ sung khi implement)

Cập nhật banner, ticker, scoreboard và logo trong khi live mà không khởi động lại phiên. Tất cả overlay phải được vẽ trong native compositor trước khi encode; overlay HTML chỉ nằm trong WebView không tự xuất hiện trên video RTMP.

## Yêu cầu native

### Android

- Capacitor 7 `Plugin` class `RtmpPublisherPlugin`, expose `start`, `stop`, `updateOverlay`.
- HaishinKit.kt: module `haishinkit` + `rtmp`, repository JitPack; thư viện yêu cầu Android 6.0/API 23+ theo tài liệu upstream.
- Runtime permissions: CAMERA và RECORD_AUDIO; INTERNET trong manifest.
- Capture camera/mic, H.264/AAC encode, preview native; giữ session đúng lifecycle; xử lý permission denial và app background.

### iOS

- Capacitor plugin Swift `RtmpPublisherPlugin`, expose cùng API.
- HaishinKit.swift qua Swift Package Manager; cấu hình `NSCameraUsageDescription`, `NSMicrophoneUsageDescription` và `AVAudioSession`.
- Cần chọn phiên bản tương thích Xcode thực tế: nhánh HaishinKit 2.1.x yêu cầu Xcode 16.4+/Swift 6; 2.2.x/2.3.x yêu cầu Xcode 26+ theo README upstream.
- Xử lý camera permission, audio session interruptions, app inactive/background và dừng an toàn.

## Compositor / preview

Bản web hiện có canvas preview và vẽ logo/banner/chữ chạy/bảng điểm. Canvas đó không tự truyền qua bridge. Plugin native phải dựng preview native và compositing các dữ liệu overlay ở trên vào frame trước H.264 encode. Cần đồng bộ `updateOverlay` từ các thao tác chỉnh sửa trong UI. Không tuyên bố overlay đã xuất hiện trong livestream cho tới khi kiểm thử bằng luồng nhận RTMP thực tế.

## Kiểm thử bắt buộc trước khi phát hành

1. Android và iPhone thật, camera trước/sau, micro, đổi hướng dọc/ngang.
2. RTMP và RTMPS; stream key sai; mất mạng; mạng đổi Wi-Fi/4G; server disconnect.
3. 720p và 1080p; bitrate; nhiệt độ/CPU; đồng bộ tiếng-hình; reconnect.
4. Logo/banner/ticker/scoreboard trên luồng nhận thực tế.
5. App vào nền, khóa màn hình, cuộc gọi đến, quyền bị từ chối; xác nhận dừng hoặc tiếp tục đúng chính sách nền tảng.
6. Không ghi stream key vào log, analytics hoặc file cấu hình.

## Thư viện upstream

- Android: https://github.com/HaishinKit/HaishinKit.kt
- Apple: https://github.com/HaishinKit/HaishinKit.swift
